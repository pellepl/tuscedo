package com.pelleplutt.operandi;

import java.util.List;
import java.util.Map;

import com.pelleplutt.operandi.proc.ExtCall;
import com.pelleplutt.operandi.proc.Processor.M;

public class Executable {
  byte[] machineCode;
  Map<Integer, M> constants;
  Map<Integer, ExtCall> extLinks;
  List<Module> dbgModules;
  int pcStart;
  int stackTop;
  
  public Executable(int pcStart, int stackTop, byte[] machineCode, Map<Integer, M> constants, Map<Integer, ExtCall> extLinks, List<Module> list) {
    this.pcStart = pcStart;
    this.machineCode = machineCode;
    this.constants = constants;
    this.extLinks = extLinks;
    this.dbgModules = list;
    this.stackTop = stackTop;
  }
  public int getPCStart() {
    return pcStart;
  }
  public int getStackTop() {
    return stackTop;
  }
  public byte[] getMachineCode() {
    return machineCode;
  }
  public Map<Integer, M> getConstants() {
    return constants;
  }
  public Map<Integer, ExtCall> getExternalLinkMap() {
    return extLinks;
  }
  public String getSrcDebugInfoPrecise(int pc) {
    if (dbgModules == null) return null;
    return Linker.getSrcDebugInfoPrecise(pc, dbgModules);
  }
  public String getSrcDebugInfoNearest(int pc) {
    return getSrcDebugInfoNearest(pc, true);
  }
  public String getSrcDebugInfoNearest(int pc, boolean showPrecise) {
    if (dbgModules == null) return null;
    return Linker.getSrcDbgInfoNearest(pc, true, showPrecise, dbgModules);
  }
  public String getFunctionName(int pc) {
    //System.out.println(String.format("get func for %08x", pc) + " " + dbgModules);
    if (dbgModules == null) return null;
    for (Module m : dbgModules) {
      for (ModuleFragment frag : m.frags) {
        //System.out.println(String.format("  %s%s %08x--%08x", frag.modname, frag.fragname, frag.executableOffset, frag.executableOffset + frag.getMachineCodeLength()));
        int offs = frag.executableOffset;
        int len = frag.getMachineCodeLength();
        if (pc >= offs && pc < offs + len) {
          return frag.modname + frag.fragname;
        }
      }
    }
    return null;
  }
  public String getInstrDebugInfo(int pc) {
    if (dbgModules == null) return null;
    for (Module m : dbgModules) {
      for (ModuleFragment frag : m.frags) {
        int offs = frag.executableOffset;
        int len = frag.getMachineCodeLength();
        if (pc >= offs && pc < offs + len) {
          return frag.instructionDbg(pc - offs);
        }
      }
    }
    return null;
  }
  public void wipeDebugInfo() {
    dbgModules = null;
  }
  public void mergeDebugInfo(Executable prevExe) {
    dbgModules.addAll(prevExe.dbgModules);
  }
}
