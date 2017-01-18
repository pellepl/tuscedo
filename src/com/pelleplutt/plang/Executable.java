package com.pelleplutt.plang;

import java.util.List;
import java.util.Map;

import com.pelleplutt.plang.proc.ExtCall;
import com.pelleplutt.plang.proc.Processor.M;

public class Executable {
  byte[] machineCode;
  Map<Integer, M> constants;
  Map<Integer, ExtCall> extLinks;
  List<Module> dbgModules;
  int pcStart;
  
  
  public Executable(int pcStart, byte[] machineCode, Map<Integer, M> constants, Map<Integer, ExtCall> extLinks, List<Module> list) {
    this.pcStart = pcStart;
    this.machineCode = machineCode;
    this.constants = constants;
    this.extLinks = extLinks;
    this.dbgModules = list;
  }
  public int getPCStart() {
    return pcStart;
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
  public String getSrcDebugInfoNearest(int pc) {
    return getSrcDebugInfoNearest(pc, true);
  }
  public String getSrcDebugInfoNearest(int pc, boolean showPrecise) {
    if (dbgModules == null) return null;
    for (Module m : dbgModules) {
      for (ModuleFragment frag : m.frags) {
        int offs = frag.executableOffset;
        int len = frag.getMachineCodeLength();
        if (pc >= offs && pc < offs + len) {
          Source source = frag.getSource();
          if (source == null) return null;
          ModuleFragment.SrcRef srcref = frag.getDebugInfoSourceNearest(pc-offs);
          if (srcref == null) return null;
          String line = source.getCSource().substring(srcref.lineOffset, srcref.lineOffset + srcref.lineLen);
          String prefix = source.getName() + "@" + srcref.line + ":";
          String mark = "";
          for (int i = 0; i < prefix.length() + srcref.symOffs - srcref.lineOffset; i++) mark += " ";
          for (int i = 0; i < Math.min(srcref.symLen, line.length() - (srcref.symOffs - srcref.lineOffset)); i++) mark += "~";
          return prefix + line + (showPrecise ? System.getProperty("line.separator") + mark : "");
        }
      }
    }
    return null;
  }
  public String getFunctionName(int pc) {
    if (dbgModules == null) return null;
    for (Module m : dbgModules) {
      for (ModuleFragment frag : m.frags) {
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
}
