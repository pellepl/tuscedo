package com.pelleplutt.plang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.proc.ExtCall;
import com.pelleplutt.plang.proc.Processor;
import com.pelleplutt.plang.proc.ProcessorError;
import com.pelleplutt.plang.proc.ProcessorError.ProcessorFinishedError;

public class Compiler {
  static String src;
  public static Executable compile(Map<String, ExtCall> extDefs, String ...sources) {
    List<Module> allMods = new ArrayList<Module>();
    for (String src : sources) {
      Compiler.src = src;
      //System.out.println("* build tree");
      AST.dbg = true;
      ASTNodeBlok e = AST.buildTree(src);
      //System.out.println(e);
      
      System.out.println("* optimise tree");
      ASTOptimiser.optimise(e);
      System.out.println(e);
  
      System.out.println("* check grammar");
      //Grammar.dbg = true;
      Grammar.check(e);
  
      System.out.println("* structural analysis");
      //StructAnalysis.dbg = true;
      StructAnalysis.analyse(e);
      
      System.out.println("* intermediate codegen");
      //CodeGenFront.dbg = true;
      List<Module> mods = CodeGenFront.genIR(e);
  
      System.out.println("* backend codegen");
      //CodeGenBack.dbg = true;
      CodeGenBack.compile(mods);
      
      allMods.addAll(mods);
    }
    TAC.dbgResolveRefs = true;

    System.out.println("* link");
    Linker.dbg = true;
    Executable exe = Linker.link(allMods, 0x000100, 0x000000, extDefs, true);
    
    System.out.println(".. all ok, " + exe.machineCode.length + " bytes of code");
    
    return exe;
  }
  
  public static void main(String[] args) {
    Map<String, ExtCall> extDefs = new HashMap<String, ExtCall>();
    extDefs.put("outln", new ExtCall() {
      public Processor.M exe(Processor.M[] memory, Processor.M[] args) {
        System.out.println(args[0].asString());
        return null;
      }
    });
    extDefs.put("out", new ExtCall() {
      public Processor.M exe(Processor.M[] memory, Processor.M[] args) {
        System.out.print(args[0].asString());
        return null;
      }
    });
    extDefs.put("cos", new ExtCall() {
      public Processor.M exe(Processor.M[] memory, Processor.M[] args) {
        return new Processor.M((float)Math.cos(args[0].f));
      }
    });
    extDefs.put("halt", new ExtCall() {
      public Processor.M exe(Processor.M[] memory, Processor.M[] args) {
        throw new ProcessorError("halt");
      }
    });
    extDefs.put("argcheckext", new ExtCall() {
      public Processor.M exe(Processor.M[] memory, Processor.M[] args) {
        System.out.println("1:" + args[0].asString());
        System.out.println("2:" + args[1].asString());
        System.out.println("3:" + args[2].asString());
        return null;
      }
    });
    
    String src = 
        "module mandel;\n" +
        "outln('*************');\n" + 
        "outln('program start');\n" + 
        "mul = 100;\n" +
        "step = 0.4;\n" +

        "func calcIterations(x, y) {\n" +
        "  zr = x; zi = y; i = 0;\n" +
        "  while (++i < mul) {\n" +
        "    zr2 = zr*zr; zi2 = zi*zi;\n" +
        "    if (zr2 + zi2 > 2*2) break;\n" + //return i;\n" +
        "    zrt = zr;\n" +
        "    zr = zr2 - zi2 + x;\n" +
        "    zi = 2*zrt*zi + y;\n" +
        "  }" +
        "  return i;\n" +
        "}" +
        
        "func fib(n) {\n" +
        "  if (n == 0) return 0;\n" +
        "  else if (n == 1) return 1;\n" + 
        "  else return fib(n - 1) + fib(n - 2);\n"  +
        "}\n" +

        "output = '';\n" + 
//TODO        "for (y in -1.0 # step # 1.0) {" +
        "for (y = -1.0; y < 1.0; y += step) {" +
        "  for (x = -1.6; x < 0.4; x += step/2) {\n" +
        "    iters = calcIterations(x,y);\n" +
        "    if (iters & 1 == 0) output = output + '0';\n" +
        "    else                output = output + '1';\n" +
        "  }\n" +
        "  output = output + '\n';\n" + 
        "}\n" +
        "outln('cos(0.1):' + cos(0.1));\n" +
        "out('mandel\n' + output);\n" +
        "anon = {outln('calling anon');return 'hello anon ';};\n" +
        "mojja = fib;\n" +
        "outln('fibo='+mojja(12));\n" +
        "outln(mandel.anon + ':' + anon());\n" +
        "outln('sisterglobal:' + otherglobal);\n" +
        "outln('friendglobal:' + walnut.otherglobal);\n" +
        "sisfun = sisterfunc();\n" +
        "outln('sisterfunc return value:'  + sisfun);\n" + 
        "outln('calling it:' + sisfun());\n" +
        "false = 0;\n" +
        "true = !false;\n" +
        "outln('true: ' + true);\n" +
        "outln('false:' + false);\n" +
        "if (true) walnut.friendfunc();\n" +
        "if (!true) halt();\n" +
        "list = [1,2,3,4];\n" +
        "outln(list[3]);\n" +
//TODO        "hashmap.somekey(); // == hashmap["somekey"]()
//TODO        "othermodule.hashmap.somekey();  // == othermodule.hashmap["somekey"]()
//TODO        "othermodule.hashmap.somekey.somesubkey();  // == othermodule.hashmap["somekey"]["somesubkey"]()
//TODO        "othermodule.hashmap.somekey.somesubkey = 123;  // == othermodule.hashmap["somekey"]["somesubkey"] = 123
        ""
        ;
    
    // DONE:   ambiguity: modulename / variablename when x.key() for x - always prefer variable name in frontend
    // FIXME:  add 'return' to functions and anon if not explicitly set by programmer
    // FIXME:  add number of call arguments to stack frame, ie push pc+fp and also argc
    // DONE:   in frontend, reverse argument list in order to be able to handle varargs. Fix backend for this also
    
    String siblingsrc = 
        "module mandel;"+
        "otherglobal = 'variable sister';\n" + 
        "func sisterfunc() {" +
        "  return {outln('sisterfunc anon func called');return 'it worked';};\n" +
        "}" +
        "func argfunc(a,b,c) {" +
        "  outln('1st:' + a);\n" +
        "  outln('2nd:' + b);\n" +
        "  outln('3rd:' + c);\n" +
        "  return;\n" +
        "}";
    String othersrc = 
         "module walnut; otherglobal = 'variable friend';\n" + 
         "l = [1,2,3];\n" +
         "l2 = [1, [a], b];\n" +
         "outln(l[0]);\n" +
         "func friendfunc() {" +
         "  outln('friendfunc called ' + mandel.anon);\n" +
         "  return;\n" +
         "}";

//    src = "a = 0; for (a; a < 10; a += 1) { outln(a); }";
//    othersrc = "";
//    siblingsrc = "";
    
    Executable e = null;
    try {
      e = Compiler.compile(extDefs, othersrc, siblingsrc, src);
    } catch (CompilerError ce) {
      String s = Compiler.getSource();
      int strstart = ce.getStringStart();
      int strend = ce.getStringEnd();
      if (strstart > 0) {
        int ps = Math.max(0, strstart - 50);
        int pe = Math.min(s.length(), strend + 50);
        System.out.println(ce.getMessage());
        System.out.println("... " + s.substring(ps, strstart) + 
            " -->" + s.substring(strstart, strend) + "<-- " +
            s.substring(strend, pe) + " ...");
      }
      throw ce;
    }
    Processor p = new Processor(0x10000, e);
    //Processor.dbgRun = true;
    //Processor.dbgMem = true;
    int i = 0;
    try {
      for (; i < 100000000; i++) {
        p.step();
      }
    } catch (ProcessorFinishedError pfe) {}
    System.out.println(i + " instructions executed");
  }

  private static String getSource() {
    return src;
  }
}
