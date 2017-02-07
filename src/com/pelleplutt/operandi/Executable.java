package com.pelleplutt.operandi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.pelleplutt.operandi.proc.ByteCode;
import com.pelleplutt.operandi.proc.ExtCall;
import com.pelleplutt.operandi.proc.Processor;
import com.pelleplutt.operandi.proc.Processor.M;

public class Executable {
  public static final int OPERANDI_ID = 0x76070914;
  int vByteCode;
  int vProcessor;
  int vCompiler;
  byte[] machineCode;
  Map<Integer, M> constants;
  Map<Integer, ExtCall> extLinks;
  List<Module> dbgModules;
  int pcStart;
  int stackTop; // or, address of last global varriable
  
  private Executable() {
  }
  
  public Executable(int pcStart, int stackTop, byte[] machineCode, Map<Integer, M> constants, Map<Integer, ExtCall> extLinks, List<Module> list) {
    this.vByteCode= ByteCode.VERSION;
    this.vProcessor = Processor.VERSION;
    this.vCompiler = Compiler.VERSION;
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
  public int getAddressOfFunction(String func) {
    if (dbgModules == null) return -1;
    for (Module m : dbgModules) {
      for (ModuleFragment frag : m.frags) {
        if (func.equals(frag.modname + frag.fragname)) {
          return frag.executableOffset;
        }
      }
    }
    return -1;
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
  
  public void write(OutputStream o) throws IOException {
    write32(o, OPERANDI_ID);
    write32(o, this.vByteCode);
    write32(o, this.vProcessor);
    write32(o, this.vCompiler);
    write32(o, pcStart);
    write32(o, stackTop);
    write32(o, machineCode.length);
    o.write(machineCode);
    write32(o, constants.size());
    for (Entry<Integer, M> e : constants.entrySet()) {
      write32(o, e.getKey());
      writeM(o, e.getValue());
    }
    write32(o, extLinks.size());
    for (Entry<Integer, ExtCall> e : extLinks.entrySet()) {
      write32(o, e.getKey());
      writeStr(o, e.getValue().getClass().getName());
    }
    o.flush();
  }
  
  public static Executable read(InputStream i, Collection<ExtCall> extCalls) throws IOException {
    Executable e = new Executable();
    int id = read32(i);
    if (id != OPERANDI_ID) {
      throw new Error("not an operandi file");
    }
    e.vByteCode = read32(i);
    e.vProcessor = read32(i);
    e.vCompiler = read32(i);
    e.pcStart = read32(i);
    e.stackTop = read32(i);
    int mcLen = read32(i);
    byte mc[] = new byte[mcLen];
    i.read(mc);
    e.machineCode = mc;
    int constLen = read32(i);
    e.constants = new HashMap<Integer, M>(constLen);
    for (int x = 0; x < constLen; x++) {
      int addr = read32(i);
      M val = readM(i);
      e.constants.put(addr, val);
    }
    int extLen = read32(i);
    e.extLinks = new HashMap<Integer, ExtCall>(extLen);
    for (int x = 0; x < extLen; x++) {
      int addr = read32(i);
      String className = readStr(i);
      boolean found = false;
      for (ExtCall ec : extCalls) {
        if (ec.getClass().getName().equals(className)) {
          e.extLinks.put(addr, ec);
          found = true;
          break;
        }
      }
      if (!found) throw new Error("need external call " + className);
    }
    return e;
  }
  
  public static final void writeM(OutputStream o, M m) throws IOException {
    write8(o, m.type);
    switch (m.type) {
    case Processor.TINT:
      write32(o, m.i);
      break;
    case Processor.TFLOAT:
      write32(o, Float.floatToIntBits(m.f));
      break;
    case Processor.TFUNC:
      write32(o, m.i);
      break;
    case Processor.TANON:
      write32(o, m.i);
      break;
    case Processor.TSTR:
      writeStr(o, m.str);
      break;
    case Processor.TNIL:
      break;
    default:
      throw new Error("cannot write M type " + m.type);
    }
  }
  
  public static final M readM(InputStream i) throws IOException {
    byte t = (byte)read8(i);
    M m = null;
    switch (t) {
    case Processor.TINT:
      m = new M(read32(i));
      break;
    case Processor.TFLOAT:
      m = new M(Float.intBitsToFloat(read32(i)));
      break;
    case Processor.TFUNC:
      m = new M(read32(i));
      break;
    case Processor.TANON:
      m = new M(read32(i));
      break;
    case Processor.TSTR:
      m = new M(readStr(i));
      break;
    case Processor.TNIL:
      break;
    default:
      throw new Error("cannot read M type " + t);
    }
    m.type = t;
    return m;
  }

  public static final void writeStr(OutputStream o, String s)  throws IOException {
    byte[] b = s.getBytes("UTF8");
    write32(o, b.length);
    o.write(b);
  }
  
  public static final String readStr(InputStream i) throws IOException {
    int blen = read32(i);
    byte bstr[] = new byte[blen];
    i.read(bstr);
    return new String(bstr, "UTF8");
  }
  
  public static final void write8(OutputStream o, int x) throws IOException {
    o.write((byte)(x));
  }
  
  public static final int read8(InputStream i)  throws IOException {
    int x = ((i.read() & 0xff));
    return x;
  }

  public static final void write32(OutputStream o, int x) throws IOException {
    o.write((byte)(x>>24));
    o.write((byte)(x>>16));
    o.write((byte)(x>>8));
    o.write((byte)(x));
  }
  
  public static final int read32(InputStream i)  throws IOException {
    int x = 
        ((i.read() & 0xff) << 24) |
        ((i.read() & 0xff) << 16) |
        ((i.read() & 0xff) << 8) |
        ((i.read() & 0xff));
    return x;
  }

}
