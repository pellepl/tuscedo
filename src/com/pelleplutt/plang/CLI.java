package com.pelleplutt.plang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.pelleplutt.plang.proc.ExtCall;
import com.pelleplutt.plang.proc.Processor;
import com.pelleplutt.plang.proc.ProcessorError;
import com.pelleplutt.plang.proc.ProcessorError.ProcessorFinishedError;

public class CLI {
  
  Map<String, ExtCall> extDefs = new HashMap<String, ExtCall>();
  
  void run() throws IOException {
    extDefs.put("println", new ExtCall() {
      public Processor.M exe(Processor.M[] memory, Processor.M[] args) {
        if (args == null || args.length == 0) {
          System.out.println();
        } else {
          for (int i = 0; i < args.length; i++) {
            System.out.print(args[i].asString() + (i < args.length-1 ? " " : ""));
          }
        }
        System.out.println();
        return null;
      }
    });
    extDefs.put("print", new ExtCall() {
      public Processor.M exe(Processor.M[] memory, Processor.M[] args) {
        if (args == null || args.length == 0) {
        } else {
          for (int i = 0; i < args.length; i++) {
            System.out.print(args[i].asString() + (i < args.length-1 ? " " : ""));
          }
        }
        return null;
      }
    });
    extDefs.put("halt", new ExtCall() {
      public Processor.M exe(Processor.M[] memory, Processor.M[] args) {
        throw new ProcessorError("halt");
      }
    });

    Compiler compiler = new Compiler(extDefs, 0x0000, 0x4000);
    Processor p = new Processor(0x10000);

    BufferedReader c = new BufferedReader(new InputStreamReader(System.in));
    String line;
    Executable e;
    while ((line = c.readLine()) != null) {
      try {
        e = compiler.compileIncrementally(line);
      } catch (CompilerError ce) {
        String s = line;
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
        p.dbgRun = true;
        for (; i < 10000000*1+800000; i++) {
          p.step();
        }
      } catch (ProcessorFinishedError pfe) {}
      catch (ProcessorError pe) {
        System.out.println("**********************************************");
        System.out.println(String.format("Exception at pc 0x%06x", p.getPC()));
        System.out.println(p.getProcInfo());
        System.out.println(pe.getMessage());
        System.out.println("**********************************************");
        System.out.println("DISASM");
        p.disasm(System.out, "   ", p.getPC(), 8);
        System.out.println("STACK");
        p.printStack(System.out, "   ", 16);
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
