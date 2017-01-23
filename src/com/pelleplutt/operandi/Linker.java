package com.pelleplutt.operandi;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.operandi.ASTNode.ASTNodeBlok;
import com.pelleplutt.operandi.ModuleFragment.Link;
import com.pelleplutt.operandi.ModuleFragment.LinkArrayInitializer;
import com.pelleplutt.operandi.ModuleFragment.LinkCall;
import com.pelleplutt.operandi.ModuleFragment.LinkConst;
import com.pelleplutt.operandi.ModuleFragment.LinkGlobal;
import com.pelleplutt.operandi.ModuleFragment.LinkUnresolved;
import com.pelleplutt.operandi.TAC.TACArrInit;
import com.pelleplutt.operandi.TAC.TACCode;
import com.pelleplutt.operandi.TAC.TACFloat;
import com.pelleplutt.operandi.TAC.TACInt;
import com.pelleplutt.operandi.TAC.TACString;
import com.pelleplutt.operandi.TAC.TACUnresolved;
import com.pelleplutt.operandi.TAC.TACVar;
import com.pelleplutt.operandi.proc.Assembler;
import com.pelleplutt.operandi.proc.ByteCode;
import com.pelleplutt.operandi.proc.ExtCall;
import com.pelleplutt.operandi.proc.Processor;
import com.pelleplutt.operandi.proc.Processor.M;

public class Linker implements ByteCode {
  public static boolean dbg =  false;
  int extFunc = -1;
  int codeOffset = 0;
  static int mainFragIx = 0;
  int ramOffset, constOffset;
  int symbolVarOffset = -1, symbolConstOffset = -1;
  List<ModuleFragment> fragments;
  Map<TAC, Integer> globalAddrLUT = new HashMap<TAC, Integer>();
  Map<String, Integer> fragAddrLUT = new HashMap<String, Integer>();
  Map<String, Integer> extCallsAddrLut = new HashMap<String, Integer>();
  List<Byte> code = new ArrayList<Byte>();
  Map<String, ExtCall> extDefs;
  Map<Integer, ExtCall> extLinks = new HashMap<Integer, ExtCall>();
  Executable incrementalPreviousExe;
  
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
    return link(ir, extDefs, keepDbg, null);
  }
  public Executable link(IntermediateRepresentation ir, Map<String, ExtCall> extDefs, 
      boolean keepDbg, Executable incrementalPreviousExe) {
    List<Module> modules = ir.getModules();
    
    fragments = new ArrayList<ModuleFragment>();
    this.extDefs = extDefs;
    
    int ocodeOffset = codeOffset;
    int osymbolVarOffset = symbolVarOffset;
    int osymbolConstOffset = symbolConstOffset;
    this.incrementalPreviousExe = incrementalPreviousExe;

    try {
      int pcStart = linkAll(modules);
      if (dbg) printLinkedCode(System.out);
      
      Map<Integer, M> constants = getConstants();

      Executable exe = new Executable(pcStart, symbolVarOffset, getMachineCode(), constants, extLinks, 
          keepDbg ? new ArrayList<Module>(modules) : null);
      if (incrementalPreviousExe != null && keepDbg) {
        exe.mergeDebugInfo(incrementalPreviousExe);
      }
      
      return exe; 
    } catch (Throwable t) {
      codeOffset = ocodeOffset;
      symbolVarOffset = osymbolVarOffset;
      symbolConstOffset = osymbolConstOffset;
      throw t;
    }
  }
  
  /**
   * Injects a global variable into linker
   * @param module module name, may be null for unnamed module
   * @param name variable name
   * @return memory address for variable
   */
  public int injectGlobalVariable(String module, String name) {
    if (symbolVarOffset == - 1) {
      if (ramOffset != constOffset) {
        symbolVarOffset = ramOffset;
      } else {
        symbolVarOffset = symbolConstOffset;
      }
    }
    
    String modname = module == null ? ".main" : module;
    TACVar var = new TACVar(null, name, modname, modname, ".0");
    
    int symAddr; 
    if (!globalAddrLUT.containsKey(var)) {
      symAddr = symbolVarOffset;
      globalAddrLUT.put(var, symbolVarOffset++);
    } else {
      symAddr = globalAddrLUT.get(var);
    }
    return symAddr;
  }

  public int lookupFunction(String func) {
    if (fragAddrLUT.containsKey(func)) {
      return fragAddrLUT.get(func);
    } else if (extDefs.containsKey(func)){
      return getExtCallAddress(func);
    }
    throw new Error("function " + func + " not found");
  }

  /**
   * Links given modules. Returns entry pc.
   * Returns 
   * @param modules
   * @return entry pc.
   */
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
      lastMainFrag.addCode("main return", null, IRET);
    }
    
    // allocate code
    for (ModuleFragment frag : fragments) {
      if (frag == firstMainFrag) pcStart = codeOffset;
      String fragId = frag.modname + frag.fragname;
      if (!fragAddrLUT.containsKey(fragId)) {
        if (dbg) System.out.println(String.format("  %-32s @ 0x%08x:%d", 
            fragId, codeOffset, frag.code.size()));
        fragAddrLUT.put(fragId, codeOffset);
        frag.fragId = fragId;
        frag.executableOffset = codeOffset;
      } else {
        int addr = fragAddrLUT.get(fragId);
        String otherplace = null;
        otherplace = Linker.getSrcDbgInfoNearest(addr, false, false, modules);
        if (otherplace == null && incrementalPreviousExe != null) {
          otherplace = Linker.getSrcDbgInfoNearest(addr, false, false, incrementalPreviousExe.dbgModules);
        }
        throw new CompilerError("duplicate definition of " + frag.fragname +
            (otherplace != null ? (" (the other is declared in " + otherplace) +")" : ""),
            /*((ASTNodeBlok)frag.defNode)); */ frag.getTACBlocks().get(0).get(0).getNode());
      }
      frag.defNode = null; // remove reference to tree
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
          if (!globalAddrLUT.containsKey(lc.cnst)) {
            globalAddrLUT.put(lc.cnst, symbolConstOffset++);
          }
        } 
        else if (l instanceof LinkUnresolved) {
          LinkUnresolved lu = (LinkUnresolved)l;
          String callName = lu.sym.module + ".func." + lu.sym.name;
          if (fragAddrLUT.containsKey(callName)) {
            // this is a function reference, add address as constant
            TACCode funcRef = new TACCode(lu.sym.getNode(), fragAddrLUT.get(callName), ASTNodeBlok.TYPE_FUNC);
            if (!globalAddrLUT.containsKey(funcRef)) {
              globalAddrLUT.put(funcRef, symbolConstOffset++);
            }
          } 
        }
        else if (l instanceof LinkArrayInitializer) {
          LinkArrayInitializer lai = (LinkArrayInitializer)l;
          globalAddrLUT.put(lai.arr, symbolConstOffset);
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
          if (!globalAddrLUT.containsKey(lg.var)) {
            globalAddrLUT.put(lg.var, symbolVarOffset++);
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
    for (String linkName : extCallsAddrLut.keySet()) {
      int extOffs = extCallsAddrLut.get(linkName);
      ExtCall extCall = extDefs.get(linkName);
      if (extCall == null) {
        throw new CompilerError("unresolved reference to " + linkName);
      }
      extLinks.put(extOffs, extCall);
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
        frag.write(srcvar + 1, globalAddrLUT.get(lvar.var), 3);
      } 
    }
  }
  
  public void linkFragConst(ModuleFragment frag) {
    for (Link l : frag.links) {
      if (l instanceof LinkConst) {
        LinkConst lc = (LinkConst)l;
        int srcvar = lc.pc;
        frag.write(srcvar + 1, globalAddrLUT.get(lc.cnst), 3);
      } 
    }
  }
  
  public void linkFragFunc(ModuleFragment frag) {
    for (Link l : frag.links) {
      if (l instanceof LinkCall) {
        LinkCall lc = (LinkCall)l;
        int srcvar = lc.pc;
        String funcFragName = lc.call.module + ".func." + lc.call.func;
        if (fragAddrLUT.containsKey(funcFragName)) {
          // got a defined address for this function
          int codeOffset = fragAddrLUT.get(funcFragName);
          frag.write(srcvar + 1, codeOffset, 3);
        } else {
          String extCallId = (lc.call.declaredModule == null ? "" : (lc.call.declaredModule + ".")) + lc.call.func;
          int codeOffset = getExtCallAddress(extCallId);
          frag.write(srcvar + 1, codeOffset, 3);
        }
      } 
    }
  }
  
  int getExtCallAddress(String extCallId) {
    int codeOffset;
    if (extCallsAddrLut.containsKey(extCallId)) {
      // already have a negative address for external call
      codeOffset = extCallsAddrLut.get(extCallId);
    } else {
      // assign a new negative address for external call
      codeOffset = extFunc--;
      extCallsAddrLut.put(extCallId, codeOffset);
      extLinks.put(codeOffset, extDefs.get(extCallId));
    }
    return codeOffset;
  }
  
  public void linkFragUnresolved(ModuleFragment frag) {
    for (Link l : frag.links) {
      if (l instanceof LinkUnresolved) {
        LinkUnresolved lu = (LinkUnresolved)l;
        TACUnresolved tu = lu.sym;
        int srcvar = lu.pc;
        
        TACVar refVar = new TACVar(tu.getNode(), tu.name, tu.module, null, ".0"); // '.0', unresolved may only point to global scope
        if (dbg) System.out.print("       " + lu);
        if (dbg) System.out.print(", as variable '" + refVar +"'");
        if (globalAddrLUT.containsKey(refVar)) {
          // this is a global variable reference
          frag.write(srcvar + 1, globalAddrLUT.get(refVar), 3);
          if (dbg) System.out.println(": found");
          continue;
        }
        String callName = tu.module + ".func." + tu.name;
        if (dbg) System.out.print(", as function '" + callName +"'");
        if (fragAddrLUT.containsKey(callName)) {
          // this is a function reference
          TACCode funcRef = new TACCode(tu.getNode(), fragAddrLUT.get(callName), ASTNodeBlok.TYPE_FUNC);
          frag.write(srcvar + 1, globalAddrLUT.get(funcRef), 3);
          if (dbg) System.out.println(": found");
          continue;
        }
        String extName;
        if (tu.module.equals(".main")) extName = tu.name;
        else extName = tu.module + "." + tu.name;
        if (dbg) System.out.print(", as external '" + extName +"'");
        if (extDefs.containsKey(extName)) {
          // this is an external function reference
          // add ext funcref addr to constants
          TACCode funcRef = new TACCode(
              lu.sym.getNode(), 
              getExtCallAddress(extName), 
              ASTNodeBlok.TYPE_FUNC);
          int refConstAddr;
          if (!globalAddrLUT.containsKey(funcRef)) {
            refConstAddr = symbolConstOffset;
            globalAddrLUT.put(funcRef, symbolConstOffset++);
          } else {
            refConstAddr = globalAddrLUT.get(funcRef);
          }

          frag.write(srcvar + 1, refConstAddr, 3);
          if (dbg) System.out.println(": found");
          continue;
        }
        if (dbg) System.out.println(": failed");
        throw new CompilerError("unresolved reference to " + lu.sym.module + "." + lu.sym.name, lu.sym.getNode());
      } // all LinkUnresolveds 
    }
  }
  
  public void linkFragArrayInitializers(ModuleFragment frag) {
    for (Link l : frag.links) {
      if (l instanceof LinkArrayInitializer) {
        LinkArrayInitializer lai = (LinkArrayInitializer)l;
        int srcvar = lai.pc;
        frag.write(srcvar + 1, globalAddrLUT.get(lai.arr), 3);
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
  
  public void wipeRunOnceCode(Executable exe) {
    if (exe == null) return;
    while (code.size() > exe.pcStart) {
      code.remove(exe.pcStart);
    }
    codeOffset = exe.pcStart;
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
        m = new M(fragAddrLUT.get(((TACCode)t).ffrag.module + ((TACCode)t).ffrag.name));
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
    for (TAC t : globalAddrLUT.keySet()) {
      if (!(t instanceof TACVar)) {
        int addr = globalAddrLUT.get(t);
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
    for (TAC t : globalAddrLUT.keySet()) {
      if (t instanceof TACVar) {
        System.out.println(String.format("    0x%06x %s", globalAddrLUT.get(t), t));
      }
    }
    System.out.println("  * const");
    for (TAC t : globalAddrLUT.keySet()) {
      if (!(t instanceof TACVar)) {
        System.out.println(String.format("    0x%06x %-8s %s", globalAddrLUT.get(t), 
            t.getClass().getSimpleName().substring(3).toLowerCase(), t));
      }
    }
    System.out.println("  * ext defs");
    for (String linkName : extDefs.keySet()) {
      if (extCallsAddrLut.containsKey(linkName)) {
        int extOffs = extCallsAddrLut.get(linkName);
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
      int fragoffs = fragAddrLUT.get(frag.modname + frag.fragname);
      Source source = frag.getSource();
      int lastLine = -1;
      while (len > 0) {
        ModuleFragment.SrcRef srcref = frag.getDebugInfoSourcePrecise(pc-fragoffs);
        if (srcref != null) {
          if (lastLine != srcref.line) {
            String line = source.getCSource().substring(srcref.lineOffset, srcref.lineOffset + srcref.lineLen);
            System.out.println(source.getName() + "@" + srcref.line + ":" + line);
          }
          lastLine = srcref.line;
        }
        String disasm = String.format("  0x%08x %s", pc, Assembler.disasm(mc, pc)); 
        out.print(disasm);
        String com = frag.instructionDbg(pc-fragoffs);
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

  public static String getSrcDbgInfoNearest(int pc, boolean showCode, boolean showPrecise,
      List<Module> dbgModules) {
    for (Module m : dbgModules) {
      for (ModuleFragment frag : m.frags) {
        int offs = frag.executableOffset;
        int len = frag.getMachineCodeLength();
        if (pc >= offs && pc < offs + len) {
          Source source = frag.getSource();
          if (source == null) return null;
          ModuleFragment.SrcRef srcref = frag.getDebugInfoSourceNearest(pc-offs);
          if (srcref == null) return null;
          String res = null;
          String line = source.getCSource().substring(srcref.lineOffset, srcref.lineOffset + srcref.lineLen);
          String prefix = source.getName() + "@" + srcref.line;
          if (showCode) {
            prefix += ":";
            res = prefix + line;
            if (showPrecise) {
              String mark = "";
              for (int i = 0; i < prefix.length() + srcref.symOffs - srcref.lineOffset; i++) mark += " ";
              for (int i = 0; i < Math.min(srcref.symLen, line.length() - (srcref.symOffs - srcref.lineOffset)); i++) mark += "~";
              res += System.getProperty("line.separator") + mark;
            }
          } else {
            res = prefix;
          }
          return res;
        }
      }
    }
    return null;
  }

  public static String getSrcDebugInfoPrecise(int pc, List<Module> dbgModules) {
    for (Module m : dbgModules) {
      for (ModuleFragment frag : m.frags) {
        int offs = frag.executableOffset;
        int len = frag.getMachineCodeLength();
        if (pc >= offs && pc < offs + len) {
          Source source = frag.getSource();
          if (source == null) return null;
          ModuleFragment.SrcRef srcref = frag.getDebugInfoSourcePrecise(pc-offs);
          if (srcref == null) return null;
          String line = source.getCSource().substring(srcref.lineOffset, srcref.lineOffset + srcref.lineLen);
          return source.getName() + "@" + srcref.line + ":" + line;
        }
      }
    }
    return null;
  }
}
