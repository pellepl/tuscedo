package com.pelleplutt.plang;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.proc.ExtCall;
import com.pelleplutt.plang.proc.Processor;
import com.pelleplutt.plang.proc.ProcessorError.ProcessorFinishedError;

public class Compiler {
  public static Executable compile(String s, Map<String, ExtCall> extDefs) {
    System.out.println("* build tree");
    //AST.dbg = true;
    ASTNodeBlok e = AST.buildTree(s);
    
    System.out.println("* optimise tree");
    ASTOptimiser.optimise(e);

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

    System.out.println("* link");
    Linker.dbg = true;
    Executable exe = Linker.link(mods, 0x000100, 0x000000, extDefs, true);
    
    System.out.println(".. all ok, " + exe.machineCode.length + " bytes of code");
    
    return exe;
  }
  
  public static void main(String[] args) {
    Map<String, ExtCall> extDefs = new HashMap<String, ExtCall>();
    extDefs.put("outln", new ExtCall() {
      public Processor.M exe(Processor.M[] memory, int sp, int fp) {
        System.out.println(memory[fp+3].asString());
        return null;
      }
    });
    extDefs.put("out", new ExtCall() {
      public Processor.M exe(Processor.M[] memory, int sp, int fp) {
        System.out.print(memory[fp+3].asString());
        return null;
      }
    });
    
    String src = 
        "module mandel;\n" +
      
        "mul = 100;" +
        "step = 0.4;" +

        "func calcIterations(x, y) {" +
        "  zr = x; zi = y; i = 0;" +
//TODO        "  while (i++ <= mul) {" +
        "  while ((i = i + 1) <= mul) {" +
        "    zr2 = zr*zr; zi2 = zi*zi;" +
        "    if (zr2 + zi2 > 2*2) break;" + //return i;" +
        "    zrt = zr;" +
        "    zr = zr2 - zi2 + x;" +
        "    zi = 2*zrt*zi + y;" +
        "  }" +
        "  return i;" +
        "}" +
        
        "func fib(n) {" +
        "  if (n == 0) return 0;" +
        "  else if (n == 1) return 1;" + 
        "  else return fib(n - 1) + fib(n - 2);"  +
        "}" +

//TODO        "for (y in -1.0 # step # 1.0) {" +
//TODO        "for (y = -1.0; y < 1.0; y += step) {" +
        "output = '';" + 
        "for (y = -1.0; y < 1.0; y = y + step) {" +
        "  for (x = -1.6; x < 0.4; x = x + step/2) {" +
        "    iters = calcIterations(x,y);" +
        "    if (iters & 1 == 0) output = output + '0';" +
        "    else                output = output + '1';" +
        "  }" +
        "  output = output + '\n';" + 
        "}" +
        "out(output);" +
        "outln('fibo10='+fib(12));" +
        ""
        ;
    
    Processor.dbgRun = false;
    Processor.dbgMem = false;
    Executable e = Compiler.compile(src, extDefs);
    Processor p = new Processor(0x10000, e);
    int i = 0;
    try {
      for (; i < 100000000; i++) {
        p.step();
      }
    } catch (ProcessorFinishedError pfe) {}
    System.out.println(i + " instructions executed");
  }
}
