package com.pelleplutt.plang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.pelleplutt.plang.proc.ExtCall;
import com.pelleplutt.plang.proc.Processor;
import com.pelleplutt.plang.proc.Processor.M;
import com.pelleplutt.plang.proc.ProcessorError;
import com.pelleplutt.plang.proc.ProcessorError.ProcessorFinishedError;

public class CLI {
  
  Map<String, ExtCall> extDefs = new HashMap<String, ExtCall>();
  
  void run() throws IOException {
    extDefs.put("println", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
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
      public Processor.M exe(Processor p, Processor.M[] args) {
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
      public Processor.M exe(Processor p, Processor.M[] args) {
        throw new ProcessorError("halt");
      }
    });
    extDefs.put("__const", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SortedSet<Integer> asort = new TreeSet<Integer>();
        asort.addAll(p.getExecutable().getConstants().keySet());
        for (int addr : asort) {
          System.out.println(String.format("  0x%06x  %s", addr, p.getExecutable().getConstants().get(addr).toString()));
        }
        return null;
      }
    });
    extDefs.put("__stack", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          for (int addr = p.getSP(); addr < p.getMemory().length; addr++) {
            System.out.println(String.format("  0x%06x  %s", addr, p.getMemory()[addr].toString()));
          }
        } else {
          for (int addr = p.getMemory().length - args[0].i; addr < p.getMemory().length; addr++) {
            System.out.println(String.format("  0x%06x  %s", addr, p.getMemory()[addr].toString()));
          }
        }
        return null;
      }
    });
    extDefs.put("__mem", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          System.out.println(String.format("  0x%06x--0x%06x", 0, p.getMemory().length));
        } else {
          int start = args[0].i;
          int addr = start;
          int len = args.length < 2 ? 1 : args[1].i;
          while (addr < p.getMemory().length && addr < start + len) {
            System.out.println(String.format("  0x%06x  %s", addr, p.getMemory()[addr].toString()));
            addr++;
          }
        }
        return null;
      }
    });

    extDefs.put("__dbg", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          System.out.println("  run : " + Processor.dbgRun);
          System.out.println("  mem : " + Processor.dbgMem);
          System.out.println("  ast : " + AST.dbg);
          System.out.println("  gra : " + Grammar.dbg);
          System.out.println("  str : " + StructAnalysis.dbg);
          System.out.println("  fro : " + CodeGenFront.dbg);
          System.out.println("  bak : " + CodeGenBack.dbg);
          System.out.println("  lin : " + Linker.dbg);
        } else {
          List<String> areas = new ArrayList<String>();
          for (M marg : args) {
            String cmd = marg.str.toLowerCase();
            if (cmd.equals("on") || cmd.equals("1")) {
              setDbg(areas, true);
              areas.clear();
            } else if (cmd.equals("off") || cmd.equals("0")) {
              setDbg(areas, false);
              areas.clear();
            } else {
              areas.add(cmd);
            }
          }
        }
        return null;
      }
    });

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
  
  static void setDbg(List<String> a, boolean ena) {
    for (String s:a) {
      if (s.equals("run") || s.equals("*")) Processor.dbgRun = ena;
      if (s.equals("mem") || s.equals("*")) Processor.dbgMem = ena;
      if (s.equals("ast") || s.equals("*")) AST.dbg = ena;
      if (s.equals("gra") || s.equals("*")) Grammar.dbg = ena;
      if (s.equals("str") || s.equals("*")) StructAnalysis.dbg = ena;
      if (s.equals("fro") || s.equals("*")) CodeGenFront.dbg = ena;
      if (s.equals("bak") || s.equals("*")) CodeGenBack.dbg = ena;
      if (s.equals("lin") || s.equals("*")) Linker.dbg = ena;
    }
  }
  
  public static void main(String[] args) throws IOException {
    CLI cli = new CLI();
    cli.run();
  }
}
