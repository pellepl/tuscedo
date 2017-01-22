package com.pelleplutt.operandi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.pelleplutt.operandi.proc.ExtCall;
import com.pelleplutt.operandi.proc.Processor;
import com.pelleplutt.operandi.proc.ProcessorError;
import com.pelleplutt.operandi.proc.Processor.M;
import com.pelleplutt.operandi.proc.ProcessorError.ProcessorFinishedError;

public class CLI {
  
  Map<String, ExtCall> extDefs = new HashMap<String, ExtCall>();
  
  void run() throws IOException {
    Processor.addCommonExtdefs(extDefs);

    Compiler comp = new Compiler(extDefs, 0x4000, 0x0000);
    Processor proc = new Processor(0x10000);

    BufferedReader c = new BufferedReader(new InputStreamReader(System.in));
    String line;
    Executable exe = null;
    Executable pexe = null;
    StringBuilder input = new StringBuilder();
    while ((line = c.readLine()) != null) {
      if (line.endsWith("\\")) {
        input.append(line.substring(0, line.length()-1) + "\n");
        continue;
      } else {
        input.append(line + "\n");
      }
      String src = input.toString();
      input = new StringBuilder();
      try {
        exe = comp.compileIncrementally(src, pexe);
        pexe = exe;
      } catch (CompilerError ce) {
        comp.printCompilerError(System.out, Compiler.getSource(), ce);
        continue;
      }
      int i = 0;
      proc.setExe(exe);
      try {
        for (; i < 10000000*1+800000; i++) {
          proc.step();
        }
      } catch (ProcessorFinishedError pfe) {
        M m = pfe.getRet();
        if (m != null && m.type != Processor.TNIL) {
          System.out.println("processor end, retval " + m);
        }
      }
      catch (ProcessorError pe) {
        proc.dumpError(pe, System.out);
        pe.printStackTrace(System.err);
      } finally {
        proc.reset();
      }
    }
  }

  public static void main(String[] args) throws IOException {
    CLI cli = new CLI();
    cli.run();
  }
}
