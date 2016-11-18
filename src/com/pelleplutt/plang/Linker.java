package com.pelleplutt.plang;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.plang.ModuleFragment.Link;
import com.pelleplutt.plang.ModuleFragment.LinkCall;
import com.pelleplutt.plang.ModuleFragment.LinkConst;
import com.pelleplutt.plang.ModuleFragment.LinkUnresolved;
import com.pelleplutt.plang.ModuleFragment.LinkGlobal;
import com.pelleplutt.plang.TAC.TACCode;
import com.pelleplutt.plang.TAC.TACFloat;
import com.pelleplutt.plang.TAC.TACInt;
import com.pelleplutt.plang.TAC.TACString;
import com.pelleplutt.plang.TAC.TACUnresolved;
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
  List<ModuleFragment> fragments;
  Map<TAC, Integer> globalLUT = new HashMap<TAC, Integer>();
  Map<String, Integer> fragLUT = new HashMap<String, Integer>();
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
    l.fragments = new ArrayList<ModuleFragment>();
    l.extDefs = extDefs;
    l.linkAll(modules);
    
    if (dbg) l.printLinkedCode(System.out);
    
    Map<Integer, M> constants = l.getConstants();
    
    return new Executable(l.getMachineCode(), constants, l.extLinks, keepDbg ? modules : null);
  }

  public void linkAll(List<Module> modules) {
    // collect fragments, global fragments first
    int mainIx = 0;
    ModuleFragment lastMainFrag = null;
    for (Module m : modules) {
      for (ModuleFragment frag : m.frags) {
        if (frag.type == ASTNode.ASTNodeBlok.TYPE_MAIN) {
          frag.fragname += "$" + mainIx++;
          fragments.add(frag);
          lastMainFrag = frag;
        }
      }
    }
    if (lastMainFrag != null) {
      lastMainFrag.addCode("main return", IRET);
    }
    // collect fragments, functions and anonymous second
    for (Module m : modules) {
      for (ModuleFragment frag : m.frags) {
        if (frag.type != ASTNode.ASTNodeBlok.TYPE_MAIN) {
          fragments.add(frag);
        }
      }
    }
    
    // allocate code
    int codeOffset = 0;
    for (ModuleFragment frag : fragments) {
      String fragId = frag.modname + frag.fragname;
      if (!fragLUT.containsKey(fragId)) {
        if (dbg) System.out.println(String.format("  %-32s @ 0x%08x:%d", 
            fragId, codeOffset, frag.code.size()));
        fragLUT.put(fragId, codeOffset);
      } else {
        throw new CompilerError("duplicate definition of " + fragId, frag.tacs.get(0).get(0).getNode());
      }
      codeOffset += frag.code.size();
    }
    
    // collect all constants
    symbolOffset = constOffset;
    for (ModuleFragment frag : fragments) {
      for (Link l : frag.links) {
        if (l instanceof LinkConst) {
          LinkConst lc = (LinkConst)l;
          if (!globalLUT.containsKey(lc.cnst)) {
            globalLUT.put(lc.cnst, symbolOffset++);
          }
        } 
        else if (l instanceof LinkUnresolved) {
          LinkUnresolved lu = (LinkUnresolved)l;
          String callName = lu.sym.module + ".func." + lu.sym.name;
          if (fragLUT.containsKey(callName)) {
            // this is a function reference, add address as constant
            TACCode funcRef = new TACCode(lu.sym.getNode(), fragLUT.get(callName));
            if (!globalLUT.containsKey(funcRef)) {
              globalLUT.put(funcRef, symbolOffset++);
            }
          } 
        }
      }
    }
    
    // collect all global variables
    if (ramOffset != constOffset) {
      symbolOffset = ramOffset;
    }
    for (ModuleFragment frag : fragments) {
      for (Link l : frag.links) {
        if (l instanceof LinkGlobal) {
          LinkGlobal lg = (LinkGlobal)l;
          if (!globalLUT.containsKey(lg.var)) {
            globalLUT.put(lg.var, symbolOffset++);
          }
        } 
      }
    }
    
    // resolve links
    if (dbg) System.out.println("  * link fragments");
    linkFragments();
    
    // sum all code
    collectCode();
    
    // resolve external definitions
    for (String linkName : extCalls.keySet()) {
      int extOffs = extCalls.get(linkName);
      ExtCall extCall = extDefs.get(linkName);
      if (extCall == null) {
        throw new CompilerError("unresolved reference to " + linkName);
      }
      extLinks.put(extOffs, extCall);
    }
  }
  
  public void linkFragments() {
    for (ModuleFragment frag : fragments) {
      if (dbg) System.out.println("    .. " + frag.modname + frag.fragname);
      linkFragConst(frag);
      linkFragGlobals(frag);
      linkFragFunc(frag);
      linkFragUnresolved(frag);
    }
  }
  
  public void linkFragGlobals(ModuleFragment frag) {
    for (Link l : frag.links) {
      if (l instanceof LinkGlobal) {
        LinkGlobal lvar = (LinkGlobal)l;
        int srcvar = lvar.pc;
        frag.write(srcvar + 1, globalLUT.get(lvar.var), 3);
      } 
    }
  }
  
  public void linkFragConst(ModuleFragment frag) {
    for (Link l : frag.links) {
      if (l instanceof LinkConst) {
        LinkConst lc = (LinkConst)l;
        int srcvar = lc.pc;
        frag.write(srcvar + 1, globalLUT.get(lc.cnst), 3);
      } 
    }
  }
  
  public void linkFragFunc(ModuleFragment frag) {
    for (Link l : frag.links) {
      if (l instanceof LinkCall) {
        LinkCall lc = (LinkCall)l;
        int srcvar = lc.pc;
        String funcFragName = lc.call.module + ".func." + lc.call.func;
        if (fragLUT.containsKey(funcFragName)) {
          int codeOffset = fragLUT.get(funcFragName);
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
  
  public void linkFragUnresolved(ModuleFragment frag) {
    for (Link l : frag.links) {
      if (l instanceof LinkUnresolved) {
        LinkUnresolved lu = (LinkUnresolved)l;
        TACUnresolved tu = lu.sym;
        int srcvar = lu.pc;
        
        TACVar refVar = new TACVar(tu.getNode(), tu.name, tu.module, ".0"); // '.0', must be a global scope
        if (dbg) System.out.print("       " + lu);
        if (dbg) System.out.print(", as variable " + refVar);
        if (globalLUT.containsKey(refVar)){
          // this is a global variable reference
          frag.write(srcvar, ILDI, 1);
          frag.write(srcvar + 1, globalLUT.get(refVar), 3);
          if (dbg) System.out.println(": found");
          continue;
        }
        String callName = frag.modname + ".func." + tu.name;
        if (dbg) System.out.print(", as function " + callName);
        if (fragLUT.containsKey(callName)) {
          // this is a function reference
          TACCode funcRef = new TACCode(tu.getNode(), fragLUT.get(callName));
          frag.write(srcvar, ILDI, 1);
          frag.write(srcvar + 1, globalLUT.get(funcRef), 3);
          if (dbg) System.out.println(": found");
          continue;
        }
        if (dbg) System.out.println(": failed");
        throw new CompilerError("unresolved reference to " + lu.sym.name, lu.sym.getNode());
      } // all LinkUnresolveds 
    }
  }
  
  public void collectCode() {
    for (ModuleFragment frag : fragments) {
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
    for (TAC t : globalLUT.keySet()) {
      if (!(t instanceof TACVar)) {
        int addr = globalLUT.get(t);
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
          if (((TACCode)t).ffrag != null) {
            m = new M(fragLUT.get(((TACCode)t).ffrag.module + ((TACCode)t).ffrag.name));
          } else {
            m = new M(((TACCode)t).addr);
          }
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
    for (TAC t : globalLUT.keySet()) {
      if (t instanceof TACVar) {
        System.out.println(String.format("    0x%06x %s", globalLUT.get(t), t));
      }
    }
    System.out.println("  * const");
    for (TAC t : globalLUT.keySet()) {
      if (!(t instanceof TACVar)) {
        System.out.println(String.format("    0x%06x %-8s %s", globalLUT.get(t), 
            t.getClass().getSimpleName().substring(3).toLowerCase(), t));
      }
    }
    System.out.println("  * ext defs");
    for (String linkName : extDefs.keySet()) {
      if (extCalls.containsKey(linkName)) {
        int extOffs = extCalls.get(linkName);
        ExtCall extCall = extLinks.get(extOffs);
        System.out.println(String.format("    0x%06x  %-20s  %s", extOffs & 0xffffff, 
            linkName, extCall));
      }
    }
    
    
    byte[] mc = getMachineCode();
    int pc = 0;
    for (ModuleFragment frag : fragments) {
      out.println();
      out.println(frag.modname + frag.fragname);
      int len = frag.code.size();
      int fragoffs = fragLUT.get(frag.modname + frag.fragname);
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
