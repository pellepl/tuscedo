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
  
  
  public Executable(byte[] machineCode, Map<Integer, M> constants, Map<Integer, ExtCall> extLinks, List<Module> list) {
    this.machineCode = machineCode;
    this.constants = constants;
    this.extLinks = extLinks;
    this.dbgModules = list;
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
  public String getDebugInfo(int pc) {
    if (dbgModules == null) return null;
    int rpc = 0;
    for (Module m : dbgModules) {
      for (ModuleFragment frag : m.frags) {
        int len = frag.getMachineCodeLength();
        if (pc >= rpc && pc < rpc + len) {
          return frag.commentDbg(pc - rpc);
        }
        rpc += len;
      }
    }
    return null;
  }
}
