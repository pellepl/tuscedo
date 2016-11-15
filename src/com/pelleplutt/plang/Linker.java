package com.pelleplutt.plang;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.plang.ModuleFragment.Link;
import com.pelleplutt.plang.ModuleFragment.LinkCall;
import com.pelleplutt.plang.ModuleFragment.LinkConst;
import com.pelleplutt.plang.ModuleFragment.LinkVar;
import com.pelleplutt.plang.TAC.TACCode;
import com.pelleplutt.plang.TAC.TACFloat;
import com.pelleplutt.plang.TAC.TACInt;
import com.pelleplutt.plang.TAC.TACString;
import com.pelleplutt.plang.TAC.TACVar;
import com.pelleplutt.plang.proc.ByteCode;
import com.pelleplutt.plang.proc.ExtCall;
import com.pelleplutt.plang.proc.Processor;
import com.pelleplutt.plang.proc.Processor.M;

public class Linker implements ByteCode {
  static boolean dbg =  false;
  int extFunc = -1;
  int symbolOffset = 0;
  int ramOffset, constOffset;
  List<Module> modules;
  Map<TAC, Integer> symbolOffsets = new HashMap<TAC, Integer>();
  Map<String, Integer> fragOffsets = new HashMap<String, Integer>();
  List<Byte> code = new ArrayList<Byte>();
  Map<String, Integer> extCalls = new HashMap<String, Integer>();
  Map<String, ExtCall> extDefs;
  Map<Integer, ExtCall> extLinks = new HashMap<Integer, ExtCall>();
  
  public static Executable link(List<Module> modules, int ramOffset, int constOffset) {
    return link(modules, ramOffset, constOffset, null, false);
  }
  
  public static Executable link(List<Module> modules, int ramOffset, int constOffset,
      Map<String, ExtCall> extDefs) {
    return link(modules, ramOffset, constOffset, extDefs, false);
  }
  
  public static Executable link(List<Module> modules, int ramOffset, int constOffset,
      Map<String, ExtCall> extDefs, boolean keepDbg) {
    Linker l = new Linker(ramOffset, constOffset);
    l.modules = modules;
    l.extDefs = extDefs;
    l.linkAll(modules);
    
    if (dbg) l.printLinkedCode(System.out);
    
    Map<Integer, M> constants = l.getConstants();
    
    return new Executable(l.getMachineCode(), constants, l.extLinks, keepDbg ? modules : null);
  }

  public void linkAll(List<Module> modules) {
    // allocate all code
    int codeOffset = 0;
    for (Module m : modules) {
      for (ModuleFragment frag : m.frags) {
        if (dbg) System.out.println(String.format("  %-32s @ 0x%08x:%d", 
            frag.module.id + frag.name, codeOffset,
            frag.code.size()));
        fragOffsets.put(frag.module.id + frag.name, codeOffset);
        codeOffset += frag.code.size();
      }
    }
    
    // collect all constants
    symbolOffset = constOffset;
    for (Module m : modules) {
      for (ModuleFragment frag : m.frags) {
        for (Link l : frag.links) {
          if (l instanceof LinkConst) {
            LinkConst lc = (LinkConst)l;
            if (!symbolOffsets.containsKey(lc.cnst)) {
              symbolOffsets.put(lc.cnst, symbolOffset++);
            }
          } 
        }
      }
    }
    
    // collect all global variables
    if (ramOffset != constOffset) {
      symbolOffset = ramOffset;
    }
    for (Module m : modules) {
      for (ModuleFragment frag : m.frags) {
        for (Link l : frag.links) {
          if (l instanceof LinkVar) {
            LinkVar lv = (LinkVar)l;
            if (!symbolOffsets.containsKey(lv.var)) {
              symbolOffsets.put(lv.var, symbolOffset++);
            }
          } 
        }
      }
    }
    
    // link all modules
    for (Module m : modules) {
      if (dbg) System.out.println("  * link module " + m.id);
      linkMod(m);
    }
    
    // sum all code
    for (Module m : modules) {
      collectCode(m);
    }
    
    // resolve external definitions
    for (String linkName : extCalls.keySet()) {
      int extOffs = extCalls.get(linkName);
      ExtCall extCall = extDefs.get(linkName);
      if (extCall == null) throw new CompilerError("unresolved reference to " + linkName);
      extLinks.put(extOffs, extCall);
    }
  }
  
  public void linkMod(Module m) {
    for (ModuleFragment frag : m.frags) {
      linkFragConst(frag);
    }
    for (ModuleFragment frag : m.frags) {
      linkFragVar(frag);
    }
    for (ModuleFragment frag : m.frags) {
      linkFragFunc(frag);
    }
  }
  
  public void linkFragVar(ModuleFragment frag) {
    for (Link l : frag.links) {
      if (l instanceof LinkVar) {
        LinkVar lvar = (LinkVar)l;
        int srcvar = lvar.pc;
        frag.write(srcvar + 1, symbolOffsets.get(lvar.var), 3);
      } 
    }
  }
  
  public void linkFragConst(ModuleFragment frag) {
    for (Link l : frag.links) {
      if (l instanceof LinkConst) {
        LinkConst lc = (LinkConst)l;
        int srcvar = lc.pc;
        frag.write(srcvar + 1, symbolOffsets.get(lc.cnst), 3);
      } 
    }
  }
  
  public void linkFragFunc(ModuleFragment frag) {
    for (Link l : frag.links) {
      if (l instanceof LinkCall) {
        LinkCall lc = (LinkCall)l;
        int srcvar = lc.pc;
        String fragName = frag.module.id + ".func." + lc.call.func;
        if (fragOffsets.containsKey(fragName)) {
          int codeOffset = fragOffsets.get(fragName);
          frag.write(srcvar + 1, codeOffset, 3);
        } else {
          int codeOffset;
          if (extCalls.containsKey(lc.call.func)) {
            codeOffset = extCalls.get(lc.call.func);
          } else {
            codeOffset = extFunc--;
            extCalls.put(lc.call.func, codeOffset);
          }
          frag.write(srcvar + 1, codeOffset, 3);
        }
      } 
    }
  }
  
  public void collectCode(Module m) {
    for (ModuleFragment frag : m.frags) {
      code.addAll(frag.code);
    }
  }

  
  public byte[] getMachineCode() {
    byte mc[] = new byte[code.size()];
    for (int i = 0; i < mc.length; i++) {
      mc[i] = code.get(i);
    }
    return mc;
  }

  
  Map<Integer, M> getConstants() {
    Map<Integer, M> constants = new HashMap<Integer, M>();
    for (TAC t : symbolOffsets.keySet()) {
      if (!(t instanceof TACVar)) {
        int addr = symbolOffsets.get(t);
        M m = null;
        if (t instanceof TACInt) {
          m = new M(((TACInt)t).x);
        }
        else if (t instanceof TACFloat) {
          m = new M(((TACFloat)t).x);
        }
        else if (t instanceof TACString) {
          m = new M(((TACString)t).x);
        }
        else if (t instanceof TACCode) {
          m = new M(fragOffsets.get(((TACCode)t).ctx.module + ((TACCode)t).ctx.name));
          m.type = Processor.TCODE;
        }
        else throw new CompilerError("unhandled constant type " + t.getClass().getSimpleName());
        constants.put(addr, m);
      }
    }
    return constants;
  }

  
  public Linker(int ramOffset, int constOffset) {
    this.ramOffset = ramOffset;
    this.constOffset = constOffset;
  }
  
  public void printLinkedCode(PrintStream out) {
    
    System.out.println("  * ram");
    for (TAC t : symbolOffsets.keySet()) {
      if (t instanceof TACVar) {
        System.out.println(String.format("    0x%06x %s", symbolOffsets.get(t), t));
      }
    }
    System.out.println("  * const");
    for (TAC t : symbolOffsets.keySet()) {
      if (!(t instanceof TACVar)) {
        System.out.println(String.format("    0x%06x %-8s %s", symbolOffsets.get(t), 
            t.getClass().getSimpleName().substring(3).toLowerCase(), t));
      }
    }
    System.out.println("  * ext defs");
    for (String linkName : extDefs.keySet()) {
      if (extCalls.containsKey(linkName)) {
        int extOffs = extCalls.get(linkName);
        ExtCall extCall = extLinks.get(extOffs);
        System.out.println(String.format("    0x%06x  %-30s  %-64s", extOffs, 
            linkName, extCall));
      }
    }
    
    
    byte[] mc = getMachineCode();
    int pc = 0;
    for (Module m : modules) {
      for (ModuleFragment frag : m.frags) {
        int len = frag.code.size();
        int fragoffs = fragOffsets.get(frag.module.id + frag.name);
        while (len > 0) {
          String disasm = String.format("0x%08x %s", pc, Processor.disasm(mc, pc)); 
          out.print(disasm);
          String com = frag.commentDbg(pc-fragoffs);
          if (com != null) {
            for (int i = 0; i < (34 - disasm.length()) + 2; i++) out.print(" ");
            out.print("// " + com);
          }
          out.println();
          int instr = (int)(mc[pc] & 0xff);
          int step = ISIZE[instr];
          if (step <= 0) step = 1;
          pc += step;
          len -= step;
        }
      }
    }
  }
}
