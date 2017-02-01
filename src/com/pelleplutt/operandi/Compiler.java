package com.pelleplutt.operandi;

import java.io.PrintStream;
import java.util.Map;

import com.pelleplutt.operandi.ASTNode.ASTNodeBlok;
import com.pelleplutt.operandi.Source.SourceString;
import com.pelleplutt.operandi.proc.ExtCall;



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
// DONE:   i = ["f":println]; // println external func
// DONE:   handle 'global' keyword
// FIXME:  goto
// FIXME:  on $0..999, in StructAnalyser, replace these by internal variables so we do not need to check range all the time



public class Compiler {
  static final int VERSION = 0x00000001;
  
  static Source src;
  static int stringix = 0;
  Map<String, ExtCall> extDefs;
  Linker linker;
  IntermediateRepresentation ir = new IntermediateRepresentation();
  
  public static Executable compileOnce(Map<String, ExtCall> extDefs, int ramOffs, int constOffs, String ...sources) {
    Source srcs[] = new Source[sources.length];
    int i = 0;
    for (String s : sources) {
      srcs[i++] = new Source.SourceString("<string" + (stringix++) + ">", s);
    }
    return compileOnce(extDefs, ramOffs, constOffs, srcs);
  }
  public Executable compileIncrementally(String src, Executable exe) {
    return compileIncrementally(new SourceString("<string" + (stringix++) + ">", src), exe);
  }
  public static Executable compileOnce(Map<String, ExtCall> extDefs, int ramOffs, int constOffs, Source ...sources) {
    return new Compiler(extDefs, ramOffs, constOffs).compile(sources);
  }
  public Executable compileIncrementally(Source src, Executable prevExe) {
    Executable exe = null;
    try {
      Compiler.src = src;
      ASTNodeBlok e = AST.buildTree(src.getSource());
      ASTOptimiser.optimise(e);
      Grammar.check(e);
      StructAnalysis.analyse(e, ir);
      ir = CodeGenFront.genIR(e, ir, src);
      CodeGenBack.compile(ir);
      ir.accumulateGlobals();
      linker.wipeRunOnceCode(prevExe);
      exe = linker.link(ir, extDefs, true, prevExe);
    } finally {
      if (ir != null) ir.clearModules();
    }
    return exe;
  }
  
  public Executable compile(Source ...sources) {
    for (Source osrc : sources) {
      String src = osrc.getSource();
      Compiler.src = osrc;
      ASTNodeBlok e = AST.buildTree(src);
      ASTOptimiser.optimise(e);
      Grammar.check(e);
      StructAnalysis.analyse(e, ir);
      ir = CodeGenFront.genIR(e, ir, osrc);
      CodeGenBack.compile(ir);
      ir.accumulateGlobals();
    }
    TAC.dbgResolveRefs = true;

    Executable exe = linker.link(ir, extDefs, true);

    return exe;
  }
  
  public void injectGlobalVariable(String module, String varName) {
    if (module == null) module = ".main";
    ir.injectGlobalVariable(module, varName);
    linker.injectGlobalVariable(module, varName);
  }

  public static Source getSource() {
    return src;
  }
  
  public Compiler(Map<String, ExtCall> extDefs, int ramOffs, int constOffs) {
    this.extDefs = extDefs;
    linker = new Linker(ramOffs, constOffs);
  }
  
  public void printCompilerError(PrintStream out, Source src, CompilerError ce) {
    int strstart = ce.getStringStart();
    int strend = ce.getStringEnd();
    String s = src.getCSource();
    Object[] srcinfo = src.getLine(strstart);
    String location = src.getName() + (srcinfo != null ? ("@" + srcinfo[0]) : "");
    out.println(location + " " + ce.getMessage());
    if (srcinfo != null) {
      String line = (String)srcinfo[1];
      int lineNbr = (Integer)srcinfo[0];
      int lineLen = line.length();
      int lineOffset = (Integer)srcinfo[2];
      String prefix = lineNbr + ": ";
      out.println(prefix + line);
      int lineMarkOffs = strstart - lineOffset;
      for (int i = 0; i < prefix.length() + lineMarkOffs; i++) {
        out.print(" ");
      }
      for (int i = 0; i < Math.min(lineOffset - lineLen, strend - strstart); i++) {
        out.print("~");
      }
      out.println();
    } else {
      if (strstart > 0) {
        int ps = Math.max(0, strstart - 50);
        int pe = Math.min(s.length(), strend + 50);
        out.println("... " + s.substring(ps, strstart) + 
            " -->" + s.substring(strstart, strend) + "<-- " +
            s.substring(strend, pe) + " ...");
      }

    }

  }
  
  public Linker getLinker() {
    return linker;
  }
}
