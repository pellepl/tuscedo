package com.pelleplutt.plang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.pelleplutt.plang.proc.Assembler;
import com.pelleplutt.plang.proc.ExtCall;
import com.pelleplutt.plang.proc.Processor;
import com.pelleplutt.plang.proc.ProcessorError;
import com.pelleplutt.plang.proc.Processor.M;
import com.pelleplutt.plang.proc.ProcessorError.ProcessorFinishedError;

public class CLI {
  
  Map<String, ExtCall> extDefs = new HashMap<String, ExtCall>();
  
  void run() throws IOException {
    Processor.addCommonExtdefs(extDefs);

    Compiler compiler = new Compiler(extDefs, 0x0000, 0x4000);
    Processor p = new Processor(0x10000);

    BufferedReader c = new BufferedReader(new InputStreamReader(System.in));
    String line;
    Executable e;
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
        e = compiler.compileIncrementally(src);
      } catch (CompilerError ce) {
        String s = src;
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
        continue;
      }
      int i = 0;
      p.setExe(e);
      try {
        for (; i < 10000000*1+800000; i++) {
          p.step();
        }
      } catch (ProcessorFinishedError pfe) {
        M m = pfe.getRet();
        if (m != null && m.type != Processor.TNIL) {
          System.out.println("processor end, retval " + m);
        }
      }
      catch (ProcessorError pe) {
        p.dumpError(pe, System.out);
        pe.printStackTrace(System.err);
      } finally {
        p.reset();
      }
    }
  }

  public static void main(String[] args) throws IOException {
    CLI cli = new CLI();
    cli.run();
  }
}
