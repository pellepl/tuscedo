package com.pelleplutt.plang;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.ModuleFragment.Link;
import com.pelleplutt.plang.ModuleFragment.LinkArrayInitializer;
import com.pelleplutt.plang.ModuleFragment.LinkCall;
import com.pelleplutt.plang.ModuleFragment.LinkConst;
import com.pelleplutt.plang.ModuleFragment.LinkGlobal;
import com.pelleplutt.plang.ModuleFragment.LinkUnresolved;
import com.pelleplutt.plang.TAC.TACArrInit;
import com.pelleplutt.plang.TAC.TACCode;
import com.pelleplutt.plang.TAC.TACFloat;
import com.pelleplutt.plang.TAC.TACInt;
import com.pelleplutt.plang.TAC.TACString;
import com.pelleplutt.plang.TAC.TACUnresolved;
import com.pelleplutt.plang.TAC.TACVar;
import com.pelleplutt.plang.proc.Assembler;
import com.pelleplutt.plang.proc.ByteCode;
import com.pelleplutt.plang.proc.ExtCall;
import com.pelleplutt.plang.proc.Processor;
import com.pelleplutt.plang.proc.Processor.M;

public class Linker implements ByteCode {
  public static boolean dbg =  false;
  int extFunc = -1;
  int codeOffset = 0;
  static int mainFragIx = 0;
  int ramOffset, constOffset;
  int symbolVarOffset = -1, symbolConstOffset = -1;
  List<ModuleFragment> fragments;
  Map<TAC, Integer> globalLUT = new HashMap<TAC, Integer>();
  Map<String, Integer> fragLUT = new HashMap<String, Integer>();
  List<Byte> code = new ArrayList<Byte>();
  Map<String, Integer> extCalls = new HashMap<String, Integer>();
  Map<String, ExtCall> extDefs;
  Map<Integer, ExtCall> extLinks = new HashMap<Integer, ExtCall>();
  
  public static Executable link(IntermediateRepresentation ir, int ramOffset, int constOffset) {
    return link(ir, ramOffset, constOffset, null, false);
  }
  
  public static Executable link(IntermediateRepresentation ir, int ramOffset, int constOffset,
      Map<String, ExtCall> extDefs) {
    return link(ir, ramOffset, constOffset, extDefs, false);
  }
  
  public static Executable link(IntermediateRepresentation ir, int ramOffset, int constOffset,
      Map<String, ExtCall> extDefs, boolean keepDbg) {
    Linker l = new Linker(ramOffset, constOffset);
    return l.link(ir, extDefs, keepDbg);
  }
  
  public Executable link(IntermediateRepresentation ir, Map<String, ExtCall> extDefs, boolean keepDbg) {
    List<Module> modules = ir.getModules();
    
    fragments = new ArrayList<ModuleFragment>();
    extCalls = new HashMap<String, Integer>();
    this.extDefs = extDefs;
    
    int ocodeOffset = codeOffset;
    int osymbolVarOffset = symbolVarOffset;
    int osymbolConstOffset = symbolConstOffset;

    try {
      int pcStart = linkAll(modules);
      if (dbg) printLinkedCode(System.out);
      
      Map<Integer, M> constants = getConstants();
      
      return new Executable(pcStart, getMachineCode(), constants, extLinks, keepDbg ? modules : null);
    } catch (Throwable t) {
      codeOffset = ocodeOffset;
      symbolVarOffset = osymbolVarOffset;
      symbolConstOffset = osymbolConstOffset;
      throw t;
    }
  }

  public int linkAll(List<Module> modules) {
    // collect fragments, functions and anonymous first
    for (Module m : modules) {
      for (ModuleFragment frag : m.frags) {
        if (frag.type != ASTNode.ASTNodeBlok.TYPE_MAIN) {
          fragments.add(frag);
        }
      }
    }
    
    ModuleFragment firstMainFrag = null;
    int pcStart = -1;
    // collect fragments, global fragments secondly
    ModuleFragment lastMainFrag = null;
    for (Module m : modules) {
      for (ModuleFragment frag : m.frags) {
        if (frag.type == ASTNode.ASTNodeBlok.TYPE_MAIN) {
          frag.fragname += "_MAIN" + mainFragIx++;
          fragments.add(frag);
          if (firstMainFrag == null) firstMainFrag = frag;
          lastMainFrag = frag;
        }
      }
    }
    if (lastMainFrag != null) {
      lastMainFrag.addCode("main return", IRET);
    }
    
    // allocate code
    for (ModuleFragment frag : fragments) {
      if (frag == firstMainFrag) pcStart = codeOffset;
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
    if (symbolConstOffset == -1) {
      symbolConstOffset = constOffset;
    }
    
    for (ModuleFragment frag : fragments) {
      for (Link l : frag.links) {
        if (l instanceof LinkConst) {
          LinkConst lc = (LinkConst)l;
          if (!globalLUT.containsKey(lc.cnst)) {
            globalLUT.put(lc.cnst, symbolConstOffset++);
          }
        } 
        else if (l instanceof LinkUnresolved) {
          LinkUnresolved lu = (LinkUnresolved)l;
          String callName = lu.sym.module + ".func." + lu.sym.name;
          if (fragLUT.containsKey(callName)) {
            // this is a function reference, add address as constant
            TACCode funcRef = new TACCode(lu.sym.getNode(), fragLUT.get(callName), ASTNodeBlok.TYPE_FUNC);
            if (!globalLUT.containsKey(funcRef)) {
              globalLUT.put(funcRef, symbolConstOffset++);
            }
          } 
        }
        else if (l instanceof LinkArrayInitializer) {
          LinkArrayInitializer lai = (LinkArrayInitializer)l;
          globalLUT.put(lai.arr, symbolConstOffset);
          symbolConstOffset += lai.arr.entries.size();
        }
      }
    }
    
    // collect all global variables
    if (symbolVarOffset == - 1) {
      if (ramOffset != constOffset) {
        symbolVarOffset = ramOffset;
      } else {
        symbolVarOffset = symbolConstOffset;
      }
    }
    
    for (ModuleFragment frag : fragments) {
      for (Link l : frag.links) {
        if (l instanceof LinkGlobal) {
          LinkGlobal lg = (LinkGlobal)l;
          if (!globalLUT.containsKey(lg.var)) {
            globalLUT.put(lg.var, symbolVarOffset++);
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

    for (Module m : modules) {
      m.linked = true;
    }
    
    return pcStart;
  }
  
  public void linkFragments() {
    for (ModuleFragment frag : fragments) {
      if (dbg) System.out.println("    .. " + frag.modname + frag.fragname);
      linkFragConst(frag);
      linkFragArrayInitializers(frag);
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
  
  // TODO perhaps do something clever with the module name here
  //      would be good to know if the module was actually declared or not
  public void linkFragFunc(ModuleFragment frag) {
    for (Link l : frag.links) {
      if (l instanceof LinkCall) {
        LinkCall lc = (LinkCall)l;
        int srcvar = lc.pc;
        String funcFragName = lc.call.module + ".func." + lc.call.func;
        if (fragLUT.containsKey(funcFragName)) {
          // got a defined address for this function
          int codeOffset = fragLUT.get(funcFragName);
          frag.write(srcvar + 1, codeOffset, 3);
        } else {
          String extCallId = (lc.call.declaredModule == null ? "" : (lc.call.declaredModule + ".")) + lc.call.func;
          int codeOffset;
          if (extCalls.containsKey(extCallId)) {
            // already have a negative address for unresolved (external?) call
            codeOffset = extCalls.get(extCallId);
          } else {
            // assign a new negative address for unresolved (external?) call
            codeOffset = extFunc--;
            extCalls.put(extCallId, codeOffset);
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
        
        TACVar refVar = new TACVar(tu.getNode(), tu.name, tu.module, null, ".0"); // '.0', must be a global scope
        if (dbg) System.out.print("       " + lu);
        if (dbg) System.out.print(", as variable " + refVar);
        if (globalLUT.containsKey(refVar)) {
          // this is a global variable reference
          frag.write(srcvar + 1, globalLUT.get(refVar), 3);
          if (dbg) System.out.println(": found");
          continue;
        }
        String callName = frag.modname + ".func." + tu.name;
        if (dbg) System.out.print(", as function " + callName);
        if (fragLUT.containsKey(callName)) {
          // this is a function reference
          TACCode funcRef = new TACCode(tu.getNode(), fragLUT.get(callName), ASTNodeBlok.TYPE_FUNC);
          frag.write(srcvar + 1, globalLUT.get(funcRef), 3);
          if (dbg) System.out.println(": found");
          continue;
        }
        if (dbg) System.out.println(": failed");
        throw new CompilerError("unresolved reference to " + lu.sym.name, lu.sym.getNode());
      } // all LinkUnresolveds 
    }
  }
  
  public void linkFragArrayInitializers(ModuleFragment frag) {
    for (Link l : frag.links) {
      if (l instanceof LinkArrayInitializer) {
        LinkArrayInitializer lai = (LinkArrayInitializer)l;
        int srcvar = lai.pc;
        frag.write(srcvar + 1, globalLUT.get(lai.arr), 3);
      } 
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

  M makeConstPrimitive(TAC t) {
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
      m.type = Processor.TFUNC;
    }
    else throw new CompilerError("constant type not primitive " + t.getClass().getSimpleName());

    return m;
  }
  
  Map<Integer, M> getConstants() {
    Map<Integer, M> constants = new HashMap<Integer, M>();
    for (TAC t : globalLUT.keySet()) {
      if (!(t instanceof TACVar)) {
        int addr = globalLUT.get(t);
        M m = null;
        if (t instanceof TACArrInit) {
          List<TAC> arr = ((TACArrInit)t).entries;
          for (TAC taentry : arr) {
            constants.put(addr++, makeConstPrimitive(taentry));
          }
        } else {
          m = makeConstPrimitive(t);
          constants.put(addr, m);
        }
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
        String disasm = String.format("0x%08x %s", pc, Assembler.disasm(mc, pc)); 
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
