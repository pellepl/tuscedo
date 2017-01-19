package com.pelleplutt.plang;

import java.util.Map;

import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.Source.SourceString;
import com.pelleplutt.plang.proc.ExtCall;



// DONE:   ambiguity: modulename / variablename when x.key() for x - always prefer variable name in frontend
// DONE:   add 'return' to functions and anon if not explicitly set by programmer
// DONE:   add number of call arguments to stack frame, ie push pc+fp and also argc
// DONE:   in frontend, reverse argument list in order to be able to handle varargs. Fix backend for this also
// DONE:   use of variables before declaration within scope should not work, confusing 
// DONE:   ranges 
// DONE:   partialarr = oldarr[[3,4,5]], partialarr = oldarr[3#5]
// DONE:   a=[]; for(i in 0#10) {a['x'+i]='y'+i;} // inverts key/val
// DONE:   if (a) println('a true'); else println('a false');
// DONE:   { globalscope = 1; { localscope = 2; anon = { return localscope * 4; }; println(anon()); } }
// DONE:   a = 1>2;
// DONE:   arr = arrb[{if ($0 > 4) return $0; else return nil;}]; // removes all elements below 4
// DONE:   arr = arrb[{return $0*2;}]; // multiplies all elements by 2
// DONE:   arr[[1,2,3]] = 4
// DONE:   map = map[{if $0 ...etc}]
// DONE:  "r = ['a':1,'b':2,'c':3];\n" +
//        "r.b = r;\n" +
//        "println(r.b.b['b'].c);\n" +
// FIXME:  goto
// FIXME:  handle 'global' keyword
// FIXME:  on $0..999, in StructAnalyser, replace these by internal variables so we do not need to check range all the time



public class Compiler {
  static Source src;
  static int stringix = 0;
  
  public static Executable compile(Map<String, ExtCall> extDefs, int ramOffs, int constOffs, String ...sources) {
    Source srcs[] = new Source[sources.length];
    int i = 0;
    for (String s : sources) {
      srcs[i++] = new Source.SourceString("<string" + (stringix++) + ">", s);
    }
    return compile(extDefs, ramOffs, constOffs, srcs);
  }

  public static Executable compile(Map<String, ExtCall> extDefs, int ramOffs, int constOffs, Source ...sources) {
    IntermediateRepresentation ir = null;
    for (Source osrc : sources) {
      String src = osrc.getSource();
      Compiler.src = osrc;
      //System.out.println("* 1. build tree");
      //AST.dbg = true;
      ASTNodeBlok e = AST.buildTree(src);
      //System.out.println(e);
      
      //System.out.println("* 2. optimise tree");
      ASTOptimiser.optimise(e);
      //System.out.println(e);
  
      //System.out.println("* 3. check grammar");
      //Grammar.dbg = true;
      Grammar.check(e);
  
      //System.out.println("* 4. structural analysis");
      //StructAnalysis.dbg = true;
      StructAnalysis.analyse(e, ir);
      
      //System.out.println("* 5. intermediate codegen");
      //CodeGenFront.dbg = true;
      //CodeGenFront.dbgUnwind = true;
      ir = CodeGenFront.genIR(e, ir, osrc);
  
      //System.out.println("* 6. backend codegen");
      //CodeGenBack.dbg = true;
      CodeGenBack.compile(ir);
      
      ir.accumulateGlobals();
    }
    TAC.dbgResolveRefs = true;

    //System.out.println("* link");
    //Linker.dbg = true;
    Executable exe = Linker.link(ir, ramOffs, constOffs, extDefs, true);
    
    //System.out.println(".. all ok, " + exe.machineCode.length + " bytes of code, pc start @ 0x" + Integer.toHexString(exe.getPCStart()));
    
    return exe;
  }

  public static Source getSource() {
    return src;
  }
  
  Map<String, ExtCall> extDefs;
  Linker linker;
  public Compiler(Map<String, ExtCall> extDefs, int ramOffs, int constOffs) {
    this.extDefs = extDefs;
    linker = new Linker(ramOffs, constOffs);
  }
  
  IntermediateRepresentation ir = null;
  public Executable compileIncrementally(String src) {
    return compileIncrementally(new SourceString("<string" + (stringix++) + ">", src));
  }
  public Executable compileIncrementally(Source src) {
    Executable exe = null;
    try {
      ASTNodeBlok e = AST.buildTree(src.getSource());
      ASTOptimiser.optimise(e);
      Grammar.check(e);
      StructAnalysis.analyse(e, ir);
      ir = CodeGenFront.genIR(e, ir, src);
      CodeGenBack.compile(ir);
      ir.accumulateGlobals();
      exe = linker.link(ir, extDefs, true);
    } finally {
      if (ir != null) ir.clearModules();
    }
    return exe;
  }
}
