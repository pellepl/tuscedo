package com.pelleplutt.tuscedo;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.pelleplutt.plang.Compiler;
import com.pelleplutt.plang.CompilerError;
import com.pelleplutt.plang.Executable;
import com.pelleplutt.plang.Source;
import com.pelleplutt.plang.proc.ExtCall;
import com.pelleplutt.plang.proc.Processor;
import com.pelleplutt.plang.proc.Processor.M;
import com.pelleplutt.plang.proc.ProcessorError;
import com.pelleplutt.plang.proc.ProcessorError.ProcessorFinishedError;
import com.pelleplutt.tuscedo.ui.WorkArea;
import com.pelleplutt.util.AppSystem;

public class PlangScript {

  Executable exe, pexe;
  Compiler comp;
  Processor proc;
  Map<String, ExtCall> extDefs = new HashMap<String, ExtCall>();
  volatile WorkArea currentWA;
  WorkArea.View runningView;

  public PlangScript() {
    proc = new Processor(0x10000);
    reset();
  }

  public void reset() {
    exe = pexe = null;
    Processor.addCommonExtdefs(extDefs);
    extDefs.put("println", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          currentWA.appendViewText(currentWA.getCurrentView(), "\n", WorkArea.STYLE_BASH_OUT);
        } else {
          for (int i = 0; i < args.length; i++) {
            currentWA.appendViewText(currentWA.getCurrentView(), args[i].asString() + (i < args.length-1 ? " " : ""), 
                WorkArea.STYLE_BASH_OUT);
          }
        }
        currentWA.appendViewText(currentWA.getCurrentView(), "\n", WorkArea.STYLE_BASH_OUT);
        return null;
      }
    });
    extDefs.put("print", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
        } else {
          for (int i = 0; i < args.length; i++) {
            currentWA.appendViewText(currentWA.getCurrentView(), args[i].asString() + (i < args.length-1 ? " " : ""), 
                WorkArea.STYLE_BASH_OUT);
          }
        }
        return null;
      }
    });
    comp = new Compiler(extDefs, 0x4000, 0x0000);
    proc.reset();
  }

  public void runScript(WorkArea wa, String s) {
    currentWA = wa;
    WorkArea.View view = wa.getCurrentView();
    proc.reset();
    try {
      Source src = new Source.SourceString("cli", s);
      exe = comp.compileIncrementally(src, pexe);
      pexe = exe;
    } catch (CompilerError ce) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos);
      comp.printCompilerError(ps, Compiler.getSource(), ce);
      String err = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      AppSystem.closeSilently(ps);
      wa.appendViewText(view, err, WorkArea.STYLE_BASH_ERR);
      return;
    }
    int i = 0;
    proc.setExe(exe);
    try {
      for (; i < 10000000; i++) {
        proc.step();
      }
    } catch (ProcessorFinishedError pfe) {
      M m = pfe.getRet();
      if (m != null && m.type != Processor.TNIL) {
        wa.appendViewText(view, "script returned " + m.asString() + "\n", WorkArea.STYLE_BASH_OUT);
      }
    } catch (ProcessorError pe) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos);
      proc.dumpError(pe, ps);
      String err = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      AppSystem.closeSilently(ps);
      wa.appendViewText(view, err, WorkArea.STYLE_BASH_ERR);
    }
  }
}
