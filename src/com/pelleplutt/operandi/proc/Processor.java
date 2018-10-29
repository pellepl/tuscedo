package com.pelleplutt.operandi.proc;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.operandi.AST;
import com.pelleplutt.operandi.CodeGenBack;
import com.pelleplutt.operandi.CodeGenFront;
import com.pelleplutt.operandi.Compiler;
import com.pelleplutt.operandi.CompilerError;
import com.pelleplutt.operandi.Executable;
import com.pelleplutt.operandi.Grammar;
import com.pelleplutt.operandi.Linker;
import com.pelleplutt.operandi.Source;
import com.pelleplutt.operandi.StructAnalysis;
import com.pelleplutt.operandi.proc.ProcessorError.ProcessorBreakpointError;
import com.pelleplutt.operandi.proc.ProcessorError.ProcessorFinishedError;

public class Processor implements ByteCode {
  public static final int VERSION = 0x00000002;
  
  public static final int TNIL = 0;
  public static final int TINT = 1;
  public static final int TFLOAT = 2;
  public static final int TSTR = 3;
  public static final int TFUNC = 4;
  public static final int TANON = 5;
  public static final int TSET = 6; // must be last, MSet.TYPES must be bigger than TSET 
  
  static final int TO_CHAR = -1;
  
  public static boolean dbgMem = false;
  public static boolean dbgRun = false;
  public static boolean dbgRunSrc = false;
  public static boolean silence = false;
  
  public static final String TNAME[] = {
    "nil", "int", "float", "string", "func", "anon", "set"
  };
  
  public final static int IFUNC_SET_VISITOR_IX = 0;
  final static int[] IFUNC_OFFS = new int[1];

  final byte[] code_internal_funcs;

  M[] memory;
  byte[] code;
  int sp;
  int pc;
  M me;
  M me_banked;
  int oldpc;
  int fp;
  boolean zero;
  boolean minus;
  Map<Integer, ExtCall> extLinks;
  Executable exe;
  M nilM = new M();
  M zeroM = new M(0);
  String[] args;
  int nestedIRQ;
  IRQHandler irqHandler;
  public volatile int user;
  
  public Processor(int memorySize) {
    nilM.type = TNIL;
    memory = new M[memorySize];
    for (int i = 0; i < memorySize; i++) {
      memory[i] = new M();
      memory[i].type = TNIL;
    }
    ByteArrayOutputStream ifcbuf = new ByteArrayOutputStream();
    byte[] ifc;
    try {
      IFUNC_OFFS[IFUNC_SET_VISITOR_IX] = ifcbuf.size();
      ifc = assemble(IFUNC_SET_VISITOR_ASM);
      ifcbuf.write(ifc);
    } catch (IOException ignore) {}
    code_internal_funcs = ifcbuf.toByteArray();
  }
  
  public Processor(int memorySize, Executable exe) {
    this(memorySize);
    setExe(exe);
  }
  
  public void setIRQHandler(IRQHandler l) {
    irqHandler = l;
  }
  
  public Executable getExecutable() {
    return exe;
  }
  
  public M[] getMemory() {
    return memory;
  }
  
  public void reset() {
    sp = memory.length - 1;
    if (args == null) args = new String[0];
    for (int i = args.length-1; i >= 0; i--) {
      push(args[i]);
    }
    push(args.length);
    push(nilM); // me
    push(0xffffffff); // pc
    push(0xffffffff); // fp
    fp = sp;
    me = null;
    zero = false;
    minus = false;
    pc = exe == null ? 0 : exe.getPCStart();
    nestedIRQ = 0;
  }
  
  public void resetAndCallAddress(int addr, List<M> args, M me) {
    sp = memory.length - 1;
    if (args == null) args = new ArrayList<M>();
    for (int i = args.size()-1; i >= 0; i--) {
      push(args.get(i));
    }
    push(args.size());
    push(me == null ? nilM : me); // me
    push(0xffffffff); // pc
    push(0xffffffff); // fp
    fp = sp;
    me = null;
    zero = false;
    minus = false;
    pc = addr;
  }
  
  public void setExe(Executable exe, String ...args) {
    this.exe = exe;
    this.args = args;
    pc = exe.getPCStart();
    this.code = exe.getMachineCode();
    this.extLinks = exe.getExternalLinkMap();
    //exe.wipeDebugInfo();
    Map<Integer, M> consts = exe.getConstants();
    for (int addr : consts.keySet()) {
      M m = consts.get(addr);
      poke(addr, m);
    }
    reset();
  }
  
  public void setMemory(int addr, M m) {
    poke(addr, m);
  }
  
  
  public void step() {
    try {
      if (dbgRun)         stepInstr(System.out);
      else if (dbgRunSrc) stepSrc(System.out);
      else                stepProc();
    } catch (Throwable t) {
      if (t instanceof ProcessorError) throw t;
      else {
        t.printStackTrace();
        throw new ProcessorError(t.getMessage());
      }
    }
  }
  
  public void stepInstr(PrintStream out) {
    String procInfo = getProcInfo();
    String disasm;
    if ((pc & 0xff800000) != 0) {
      disasm = Assembler.disasm(code_internal_funcs, pc - 0xff000000);
    } else {
      disasm = Assembler.disasm(code, pc);
    }
    String dbgComment = exe.getInstrDebugInfo(pc);
    out.println(String.format("%s      %-32s  %s", procInfo, disasm, (dbgComment == null ? "" : ("; " + dbgComment))));
    stepProc();
    String stack = getStack();
    out.println(stack);
  }
  
  public String stepInstr() {
    stepProc();
    String procInfo = getProcInfo();
    String disasm;
    if ((pc & 0xff800000) != 0) {
      disasm = Assembler.disasm(code_internal_funcs, pc - 0xff000000);
    } else {
      disasm = Assembler.disasm(code, pc);
    }
    String dbgComment = exe.getInstrDebugInfo(pc);
    String dbgRes = String.format("%s      %-32s  %s", procInfo, disasm, (dbgComment == null ? "" : ("; " + dbgComment)));
    return dbgRes;
  }
  
  String lastSrcLine = null;
  
  public void stepSrc(PrintStream out) {
    String d = exe.getSrcDebugInfoPrecise(pc);
    if (d != null) {
      lastSrcLine = d;
      if (out != null) out.println(d);
    }
    do {
      stepProc();
      d = exe.getSrcDebugInfoPrecise(pc);
    } while (d == null || lastSrcLine.equals(d));
  }

  public String stepSrc() {
    stepProc();
    return exe.getSrcDebugInfoPrecise(pc);
  }
  
  public void raiseInterrupt(int newPC) {
    push(getSR());
    pushFrameAndJump(newPC, true);
    if ((newPC & 0x800000) == 0x800000) {
      ExtCall ec = extLinks.get(pc);
      if (ec == null) throw new ProcessorError(String.format("bad external call 0x%06x", pc));
      ec.doexe(this, null); 
    }
  }
  
  public void dumpError(ProcessorError pe, PrintStream out) {
    out.println("**********************************************");
    out.println(String.format("Exception at pc 0x%06x", getPC()));
    out.println(getProcInfo());
    out.println(pe.getMessage());
    out.println("**********************************************");
    String func = getExecutable().getFunctionName(getPC());
    if (func != null) {
      out.println("in context " + func);
    }
    String dbg = getExecutable().getSrcDebugInfoNearest(getPC());
    if (dbg != null) {
      out.println(dbg);
    }
    unwindStackTrace(out);
    out.println("DISASM");
    Assembler.disasm(out, "   ", getExecutable().getMachineCode(), getPC(), 8);
    out.println("STACK");
    printStack(out, "   ", 16);
  }
  

  public static void addCommonExtdefs(Map<String, ExtCall> extDefs) {
    addCommonExtdefs(extDefs, System.in, System.out);
  }
  public static void addCommonExtdefs(Map<String, ExtCall> extDefs, final InputStream in, final PrintStream out) {
    extDefs.put("println", new EC_println(out));
    extDefs.put("print", new EC_print(out));
    extDefs.put("rand", new EC_rand());
    extDefs.put("randseed", new EC_randseed());
    extDefs.put("cpy", new EC_cpy());
    extDefs.put("byte", new EC_byte());
    extDefs.put("strstr", new EC_strstr());
    extDefs.put("strstrr", new EC_strstrr());
    extDefs.put("lines", new EC_lines());
    extDefs.put("atoi", new EC_atoi());
    extDefs.put("__dbg", new EC_dbg(out));
    extDefs.put("__dumpstack", new EC_dumpstack());
    extDefs.put("__const", new EC_const());
    extDefs.put("__mem", new EC_mem());
    extDefs.put("__sp", new EC_sp());
    extDefs.put("__fp", new EC_fp());
    extDefs.put("__pc", new EC_pc());
  }
  
  public void printStack(PrintStream out, String pre, int maxEntries) {
    for (int i = sp-1; i < sp+maxEntries; i++) {
      if (i >= memory.length) break;
      if (pre != null) out.print(pre);
      out.println(String.format("0x%06x %-8s %s", i, TNAME[memory[i].type], memory[i].asString()));
    }
  }
  
  public String getProcInfo() {
    boolean irq = fp != 0xffffffff && ((fp & 0x80000000) != 0);
    return String.format("pc:0x%06x  sp:0x%06x  fp:0x%06x%c sr:", pc, sp, fp & ~0x80000000, irq?'I':' ') + 
        (zero ? "Z" : "z") + (minus ? "M" : "m");
  }

  public int getPC() {
    return oldpc;
  }
  public int getSP() {
    return sp;
  }
  public int getFP() {
    return fp;
  }
  public int getSR() {
    return (zero ? (1<<0) : 0) | (minus ? (1<<1) : 0);
  }
  public void setSR(int x) {
    zero = (x & (1<<0)) != 0;
    minus = (x & (1<<1)) != 0;
  }
  public M getMe() {
    return me;
  }
  public int getNestedIRQ() {
    return nestedIRQ;
  }


  public void unwindStackTrace(PrintStream out) {
    int fp = this.fp;
    int pc = this.oldpc;
    int sp = this.sp;
    M me = this.me;
    int maxHops = 40;
    boolean irq = false;
    while(pc != 0xffffffff && fp != 0xffffffff) {
      int nxtFP = fp != 0xffffffff ? peek((fp&~0x80000000)+FRAME_0_FP).i : 0;
      irq = nxtFP != 0xffffffff && (nxtFP & 0x80000000) != 0;
      out.println(String.format("PC:0x%08x FP:0x%08x SP:0x%08x%s", 
          pc, fp, sp, irq ? " IRQ":""));
      fp &= ~0x80000000;
      int argc = peek(fp + FRAME_3_ARGC).i;
      String func = exe.getFunctionName(pc);
      if (func != null) {
        out.print(func);
      } else {
        out.print(String.format("@ 0x%08x", pc));
      }
      if (!irq) {
        out.print("( ");
        for (int a = 0; a < argc; a++) {
          M arg = peek(fp + FRAME_SIZE + 1 + a);
          out.print(arg.asString() + " ");
        }
        out.println(") me:" + (me != null ? me.asString() : ""));
      } else {
        out.println();
      }
      String dbg = exe.getSrcDebugInfoNearest(pc, false);
      if (dbg != null) {
        out.println(dbg);
      }
      sp = fp;
      if (sp == 0xffffffff) break;
      fp = peek(sp+FRAME_0_FP).i;
      pc = peek(sp+FRAME_1_PC).i;
      me = peek(sp+FRAME_2_ME);
      out.println();
      if (maxHops-- == 0) {
        out.println("*** stacktrace abort, too long ***");
        break;
      }
    }
  }


  
  
  
  
  
  M pop() {
    sp++;
    return memory[sp];
  }
  
  void poke(int a, M m) {
    memory[a].type = m.type;
    memory[a].i = m.i;
    memory[a].f = m.f;
    memory[a].str = m.str;
    memory[a].ref = m.ref;
    if (dbgMem) System.out.println(String.format("poke 0x%06x %s" , a, m));
  }
  
  M peek(int a) {
    if (dbgMem) System.out.println(String.format("peek 0x%06x %s" , a, memory[a]));
    return memory[a];
  }
  
  int pcodetoi(int addr, int bytes) {
    byte c[];
    if ((addr & 0xff80000)!=0) {
      addr -= 0xff000000;
      c = code_internal_funcs;
    } else {
      c = code;
    }
    return codetoi(c, addr, bytes);
  }
  
  int pcodetos(int addr, int bytes) {
    byte c[];
    if ((addr & 0xff80000)!=0) {
      addr -= 0xff000000;
      c = code_internal_funcs;
    } else {
      c = code;
    }
    return codetos(c, addr, bytes);
  }
  
  public static int codetoi(byte[] code, int addr, int bytes) {
    int x = 0;
    for (int i = 0; i < bytes; i++) {
      x <<= 8;
      x |= (((int)code[addr++]) & 0xff);
    }
    return x;
  }

  public static int codetos(byte[] code, int addr, int bytes) {
    int x = codetoi(code, addr, bytes);
    x <<= 8*(4-bytes);
    x >>= 8*(4-bytes);
    return x;
  }
  
  void push(M m) {
    if (m == null) m = nilM;
    poke(sp--, m);
  }
    
  void push(int x) {
    memory[sp].type = TINT;
    memory[sp].str = null;
    memory[sp].ref = null;
    memory[sp--].i = x;
  }
    
  void push(float x) {
    memory[sp].type = TFLOAT;
    memory[sp].str = null;
    memory[sp].ref = null;
    memory[sp--].f = x;
  }
    
  void push(char x) {
    push(Character.toString(x));
  }
  void push(String x) {
    memory[sp].type = TSTR;
    memory[sp].ref = null;
    memory[sp--].str = x;
  }
    
  void push(MSet set) {
    memory[sp].type = TSET;
    memory[sp].ref = set;
    memory[sp--].str = null;
  }
    
  M peekStack(int r) {
    return memory[sp + r + 1];
  }
  
  void dup() {
    push(peekStack(0));
  }
  
  M tmpM = new M();
  void swap() {
    M m1 = pop();
    M m2 = tmpM.copy(pop());
    push(m1);
    push(m2);
  }
  
  void cpy() {
    push(peekStack(pcodetos(pc++, 1)));
  }
  
  void stor() {
    M m = pop();
    M addr = pop();
    if (addr.type == TINT) {
      poke(addr.i, m);
    } else {
      throw new Error("cannot access non int adress");
    }
  }
  
  void stor_im() {
    M m = pop();
    int addr = pcodetoi(pc, 3);
    pc += 3;
    poke(addr, m);
  }
  
  void load() {
    M addr = pop();
    if (addr.type == TINT) {
      push(peek(addr.i));
    } else {
      throw new Error("cannot access non int adress");
    }
  }
  
  void load_im() {
    int addr = pcodetoi(pc, 3);
    pc += 3;
    push(peek(addr));
  }
  
  void stor_fp() {
    int rel = pcodetos(pc++, 1);
    poke((fp & ~0x80000000) - rel, pop());
  }
  
  void load_fp() {
    int rel = pcodetos(pc++, 1);
    push(peek((fp & ~0x80000000) - rel));
  }
  
  void sp_incr() {
    int m = pcodetoi(pc++, 1) + 1;
    sp -= m;
  }
  
  void sp_decr() {
    int m = pcodetoi(pc++, 1) + 1;
    for (int i = 0; i < m; i++) {
      poke(sp + i, nilM);
    }
    sp += m;
  }
  
  void push_pc() {
    push(oldpc);
  }
  
  void push_sp() {
    push(sp);
  }
  
  void push_fp(boolean interrupt) {
    if (interrupt) {
      nestedIRQ++;
      push(fp | 0x80000000);
    } else {
      push(fp);
    }
  }
  
  void pop_fp() {
    fp = pop().i;
  }
  
  void push_sr() {
    push( getSR() );
  }
  
  void pop_sr() {
    setSR( pop().i );
  }
  
  void set_cre() {
    int elements = pop().asInt();
    if (elements == -1) {
      // create arg array
      int fp = this.fp & ~0x80000000;
      MMemList list = new MMemList(memory, 
          fp+FRAME_SIZE+1, fp+FRAME_SIZE+1+memory[fp+FRAME_3_ARGC].asInt());
      push(list);
    } else {
      MListMap list = new MListMap();
      for (int i = 0; i < elements; i++) {
        M m = new M();
        m.copy(peek(sp + elements - i));
        list.add(m);
      }
      sp += elements;
      push(list);
    }
  }
  
  void arr_cre() {
    int addr = pcodetoi( pc, 3);
    pc += 3;
    int elements = pop().asInt();
    MListMap set = new MListMap();
    for (int i = 0; i < elements; i++) {
      M m = new M();
      m.copy(peek(addr++));
      set.add(m);
    }
    push(set);
  }
  
  void tup_cre() {
    MListMap tup = new MListMap();
    M mval = pop();
    M mkey = pop();
    tup.makeTup(new M().copy(mkey), new M().copy(mval));
    push(tup);
  }
  
  void set_drf() {
    M mix = pop();
    M mset = pop();
    if (mset.type == TSET) {
      if (mix.type == TSET) {
        derefSetArgSet(mset.ref, mix.ref);
      } else if (mix.type == TANON) {
        derefSetArgAnon(mset.ref, mix);
      } else {
        push((mset.ref).get(mix));
      }
    } else if (mset.type == TSTR) {
      if (mix.type == TSET) {
        derefStringArgSet(mset.str, mix.ref);
      } else if (mix.type == TANON) {
        derefStringArgAnon(mset.str, mix);
      } else {
        int ix = mix.asInt();
        if (ix < 0) ix = mset.str.length() + ix;
        push(mset.str.charAt(ix));
      }
    } else if (mset.type == TINT) {
      int res = 0;
      if (mix.type == TSET) {
        MSet drf = mix.ref;
        int len = drf.size();
        for (int i = 0; i < len; i++) {
          M mdrf = drf.getElement(i);
          if (mdrf.type == TFLOAT || mdrf.type == TINT) {
            res = (res << 1) | ( (mset.i & (1 << mdrf.asInt())) >>> mdrf.asInt() );
          }
        }
      } else if (mix.type == TANON) {
        throw new ProcessorError("cannot dereference integers with mutators");
      } else {
        int ix = mix.asInt();
        if (ix < 0) ix = 32 + ix;
        res = (mset.i & (1<<ix))>>>ix;
      }
      push(res);
    } else {
      throw new ProcessorError("cannot dereference type " + TNAME[mset.type]);
    }
  }
  
  void derefSetArgSet(MSet set, MSet drf) {
    MListMap res = new MListMap();
    int len = drf.size();
    for (int i = 0; i < len; i++) {
      M mdrf = drf.getElement(i);
      M m = new M();
      if (mdrf.type == TFLOAT || mdrf.type == TINT) {
        m.copy(set.getElement(mdrf.asInt()));
      } else {
        m.copy(set.get(mdrf));
      }
      res.add(m);
    }
    push(res);
  }
  
  void derefSetArgAnon(MSet set, M adrf) {
    push(adrf);
    push(set);
    push(2);
    // the assembled function IFUNC_SET_VISITOR_ASM
    push(0xff000000 | IFUNC_OFFS[IFUNC_SET_VISITOR_IX]);
    call();
  }
  
  void derefStringArgAnon(String string, M adrf) {
    push(adrf);
    push(string);
    push(2);
    // the assembled function IFUNC_SET_VISITOR_ASM
    push(0xff000000 | IFUNC_OFFS[IFUNC_SET_VISITOR_IX]);
    call();
  }
  
  void derefStringArgSet(String str, MSet drf) {
    StringBuilder sb = new StringBuilder();
    int len = drf.size();
    for (int i = 0; i < len; i++) {
      M mdrf = drf.getElement(i);
      if (mdrf.type == TFLOAT || mdrf.type == TINT) {
        sb.append(str.charAt(mdrf.asInt()));
      }
    }
    push(sb.toString());
  }
  
  void set_wr(M mset, M mix, M mval) {
    if (mset.type == TSET) {
      if (mval.type == TNIL) {
        mset.ref.remove(mix);
      } else {
        M m = new M();
        m.copy(mval);
        mset.ref.set(mix, m);
      }
      push(mset);
    } else if (mset.type == TINT) {
      int res = mset.i;
      int ix = mix.asInt();
      if (ix < 0) ix = 32+ix;
      if (mval.type == TNIL) {
        int mask = (1<<ix)-1;
        int lo = res & mask;
        int hi = (res >>> 1) & ~mask;
        res = hi | lo;
      } else {
        if (mval.asInt() > 0) {
          res |= (1 << ix);
        } else {
          res &= ~(1 << ix);
        }
      }
      push(res);
    } else if (mset.type == TSTR) {
      if (mval.type == TNIL) {
        int ix = mix.asInt();
        int len = mset.str.length();
        if (ix < 0) ix = len + ix;
        mset.str = 
            (ix > 0 ? mset.str.substring(0, ix-1) : "") +  
            (ix+1 < len ? mset.str.substring(ix+1) : "");
      } else {
        int ix = mix.asInt();
        int len = mset.str.length();
        if (ix < 0) ix = len + ix;
        mset.str = 
            (ix > 0 ? mset.str.substring(0, ix-1) : "") +  
            mval.asString() + 
            (ix+1 < len ? mset.str.substring(ix+1) : "");
      }
      push(mset);
    } else {
      throw new ProcessorError("cannot write entries in type " + TNAME[mset.type]);
    }
  }
    
  void set_wr() {
    M mval = pop();
    M mix = pop();
    M mset = pop();
    if (mix.type == TSET) {
      MSet ixset = mix.ref;
      int len = ixset.size();
      for (int i = 0; i < len; i++) {
        M dmix = ixset.get(i);
        set_wr(mset, dmix, mval);
        if (i < len - 1) {
          mset = pop();
        }
      }
    } else {
      set_wr(mset, mix, mval);
    }
  }
  
  void map_add() {
    M mkey = pop();
    M mval = pop();
    M mmap = peek(sp+1);
    if (mmap.type == TSET) {
      M mk = new M();
      mk.copy(mkey);
      M mv = new M();
      mv.copy(mval);
      mmap.ref.put(mk.getRaw(), mv);
    } else {
      throw new ProcessorError("cannot add tuples to type " + TNAME[mmap.type]);
    }
  }
  
  void set_sz() {
    M e = pop();
    if (e.type == TSET) {
      push(e.ref.size());
    } else if (e.type == TSTR) {
      push(e.str.length());
    } else {
      push(0);
    }
  }
  
  void set_rd() {
    int ix = pop().asInt();
    M mset = pop();
    if (mset.type == TSET) {
      push(mset.ref.get(ix));
    } else if (mset.type == TSTR) {
      push(mset.str.charAt(ix));
     } else {
      throw new ProcessorError("cannot get sub element of type " + TNAME[mset.type]);
    }
  }
  
  void rng(int def) {
    MRange range;
    if (def == 2) {
      M mto = pop();
      M mfrom = pop();
      range = new MRange(mfrom, mto);
    } else {
      M mto = pop();
      M mstep = pop();
      M mfrom = pop();
      range = new MRange(mfrom, mstep, mto);
    }
    push(range);
  }
  
  void status(int x) {
    zero = x == 0;
    minus = x < 0;
  }
  void status(float x) {
    zero = x == 0;
    minus = x < 0;
  }
  void status(String x) {
    zero = x == null || x.isEmpty();
    minus = false;
  }
    
  void add() {
    M e2 = pop();
    M e1 = pop();
    if (e1.type == e2.type) {
      if (e1.type == TINT) {
        int r = e1.i + e2.i;
        status(r);
        push(r);
      }
      else if (e1.type == TFLOAT) {
        float r = e1.f + e2.f;
        status(r);
        push(r);
      }
      else if (e1.type == TSTR) {
        String r = e1.str + e2.str;
        status(r);
        push(r);
      } else if (e1.type == TSET) {
        M me1 = new M();
        me1.copy(e1);
        M me2 = new M();
        me2.copy(e2);
        me1.ref.add(me2);
        push(me1);
      } else {
        throw new ProcessorError("cannot add type " + TNAME[e1.type]);
      }
    }
    else if (e1.type == TFLOAT && e2.type == TINT) {
      float r = e1.f + e2.i;
      status(r);
      push(r);
    }
    else if (e2.type == TFLOAT && e1.type == TINT) {
      float r = e1.i + e2.f;
      status(r);
      push(r);
    }
    else if (e1.type == TSET) {
      M m = new M();
      m.copy(e2);
      e1.ref.add(m);
      push(e1);
    }
    else if (e2.type == TSET) {
      M m = new M();
      m.copy(e1);
      e2.ref.insert(0, m);
      push(e2);
    }
    else if (e1.type == TSTR || e2.type == TSTR) {
      String r = (e1.type == TNIL ? "" : e1.asString()) + (e2.type == TNIL ? "" : e2.asString());
      status(r);
      push(r);
    } else {
      throw new ProcessorError("cannot add types " + TNAME[e1.type] + " and " + TNAME[e2.type]);
    }
  }

  void sub() {
    sub_i(true, true, pop(), pop());
  }
  void sub(boolean push, M e2, M e1) {
    sub_i(push, true, e2, e1);
  }
  void sub_i(boolean push, boolean naturalOrder, M e2, M e1) {
    if (!naturalOrder) {
      M tmp = e1;
      e1 = e2;
      e2 = tmp;
    }
    if (e1.type == e2.type) {
      if (e1.type == TINT || e1.type == TFUNC || e1.type == TANON) {
        int r = e1.i - e2.i;
        status(r);
        if (push) push(r);
      }
      else if (e1.type == TFLOAT) {
        float r = e1.f - e2.f;
        status(r);
        if (push) push(r);
      }
      else if (e1.type == TSTR) {
        if (!push) {
          // comparison
          if (e1.str.length() != e2.str.length()) {
            status(e1.str.length() - e2.str.length());
          } else {
            boolean equal = true;
            for (int i = 0; i < e1.str.length(); i++) {
              if (e1.str.charAt(i) != e2.str.charAt(i)) {
                status(e1.str.charAt(i) - e2.str.charAt(i));
                equal = false;
                break;
              }
            }
            if (equal) status(0);
          }
        } else {
          // operation
          String r = e1.str;
          int pos = r.lastIndexOf(e2.str);
          if (pos >= 0) {
            r = r.substring(0, pos) + r.substring(pos + e2.str.length());
          }
          status(r);
          push(r);
        }
      } else if (e1.type == TNIL && !push) {
        status(0);
      } else if (e1.type == TSET && !push) {
        int diff = e1.ref.size() - e2.ref.size();
        if (diff == 0 && e1.ref != e2.ref) {
          diff = 1;
        }
        status(diff);
      } else {
        throw new ProcessorError("cannot subtract type " + TNAME[e1.type]);
      }
    }
    else if (e1.type == TNIL && !push) {
      status(-1);
    }
    else if (e2.type == TNIL && !push) {
      status(1);
    }
    else if (e1.type == TFLOAT && e2.type == TINT) {
      float r = e1.f - e2.i;
      status(r);
      if (push) push(r);
    }
    else if (e2.type == TFLOAT && e1.type == TINT) {
      float r = e1.i - e2.f;
      status(r);
      if (push) push(r);
    } 
    else if (e1.type == TSTR && e2.type == TINT) {
      float r = e1.asInt() - e2.i;
      status(r);
      if (push) push(r);
    }
    else if (e2.type == TSTR && e1.type == TINT) {
      float r = e1.i - e2.asInt();
      status(r);
      if (push) push(r);
    } 
    else if (e1.type == TSTR && e2.type == TFLOAT) {
      float r = e1.asFloat() - e2.f;
      status(r);
      if (push) push(r);
    }
    else if (e2.type == TSTR && e1.type == TFLOAT) {
      float r = e1.f - e2.asFloat();
      status(r);
      if (push) push(r);
    } 
    else if (!push && (e1.type == TSET || e2.type == TSET)) {
      status(e1.type == TSET ? 1 : -1);
    }
    else {
      throw new ProcessorError("cannot subtract types " + TNAME[e1.type] + " and " + TNAME[e2.type]);
    }
  }
  
  void cmp() {
    sub(false, pop(), pop());
  }
  
  void cmpn() {
    sub_i(false, false, pop(), pop());
  }
  
  void cmp_0() {
    sub(false, zeroM, pop());
  }
  
  void cmn_0() {
    sub_i(false, false, zeroM, pop());
  }
  
  void not() {
    M e1 = pop();
    if (e1.type == TINT) {
      push(~e1.i);
    } else {
      throw new ProcessorError("cannot invert type " + TNAME[e1.type]);
    }
  }
  
  void neg() {
    M e1 = pop();
    if (e1.type == TINT) {
      push(-e1.i);
    } else if (e1.type == TFLOAT) {
      push(-e1.f);
    } else {
      throw new ProcessorError("cannot negate type " + TNAME[e1.type]);
    }
  }
  
  void lnot() {
    M e1 = pop();
    if (e1.type == TINT) {
      push(e1.i == 0 ? 1 : 0);
    } else {
      throw new ProcessorError("cannot logical not type " + TNAME[e1.type]);
    }
  }
  
  void add_im() {
    push(pcodetoi( pc++, 1) + 1);
    add();
  }

  void sub_im() {
    push(pcodetoi( pc++, 1) + 1);
    sub();
  }
  
  void push_s() {
    push(pcodetos( pc++, 1));
  }

  void push_u() {
    push(pcodetoi( pc++, 1)+128);
  }

  void push_im_int(int i) {
    push(i);
  }

  void push_nil() {
    push(nilM);
  }

  M m_me = new M();
  void def_me() {
    m_me.copy(peekStack(0));
    me_banked = m_me;
  }

  void push_me() {
    if (me == null) {
      push(nilM);
    } else {
      push(me);
    }
  }

  void udef_me() {
    me_banked = null;
  }

  void add_q(int x) {
    push(x);
    add();
  }

  void sub_q(int x) {
    push(x);
    sub();
  }
  void cast(int type) {
    M m = pop();
    switch (type) {
    case TINT:
      switch (m.type) {
      case TFUNC:
      case TANON:
      case TINT: break;
      case TFLOAT: m.i = (int)m.f; break;
      case TNIL: m.i = 0; break;
      case TSTR: m.i = Integer.parseInt(m.str.trim()); break;
      case TSET: break;
      }
      break;
    case TFLOAT:
      switch (m.type) {
      case TFLOAT: break;
      case TFUNC:
      case TANON:
      case TINT: m.f = m.i; break;
      case TNIL: m.f = 0; break;
      case TSTR: m.f = Float.parseFloat(m.str.trim()); break;
      case TSET: break;
      }
      break;
    case TO_CHAR:
      switch (m.type) {
      case TFLOAT: m.str = "" + (char)((int)m.f); break;
      case TANON:
      case TFUNC: break;
      case TINT: m.str = "" + (char)(m.i); break;
      case TNIL: break;
      case TSTR: break;
      case TSET: break;
      }
      break;
    case TSTR:
      m.str = m.asString();
      break;
    }
    m.type = (byte)(type == TO_CHAR ? TSTR : type);
    push(m);
  }
  
  void get_typ() {
    M m = pop();
    int t = m.type;
    if (t == TSET) {
      t = m.ref.getType();
    }
    push(t);
  }
  
  void mul() {
    M e2 = pop();
    M e1 = pop();
    if (e1.type == e2.type) {
      if (e1.type == TINT) {
        int r = e1.i * e2.i;
        status(r);
        push(r);
      }
      else if (e1.type == TFLOAT) {
        float r = e1.f * e2.f;
        status(r);
        push(r);
      } else {
        throw new ProcessorError("cannot multiply type " + TNAME[e1.type]);
      }
    }
    else if (e1.type == TFLOAT && e2.type == TINT) {
      float r = e1.f * e2.i;
      status(r);
      push(r);
    }
    else if (e2.type == TFLOAT && e1.type == TINT) {
      float r = e1.i * e2.f;
      status(r);
      push(r);
    } else {
      throw new ProcessorError("cannot multiply types " + TNAME[e1.type] + " and " + TNAME[e2.type]);
    }
  }

  void div() {
    M e2 = pop();
    M e1 = pop();
    if (e1.type == e2.type) {
      if (e1.type == TINT) {
        int r = e1.i / e2.i;
        status(r);
        push(r);
      }
      else if (e1.type == TFLOAT) {
        float r = e1.f / e2.f;
        status(r);
        push(r);
      } else {
        throw new ProcessorError("cannot divide type " + TNAME[e1.type]);
      }
    }
    else if (e1.type == TFLOAT && e2.type == TINT) {
      float r = e1.f / e2.i;
      status(r);
      push(r);
    }
    else if (e2.type == TFLOAT && e1.type == TINT) {
      float r = e1.i / e2.f;
      status(r);
      push(r);
    } else {
      throw new ProcessorError("cannot divide types " + TNAME[e1.type] + " and " + TNAME[e2.type]);
    }
  }
  
  void rem() {
    M e2 = pop();
    M e1 = pop();
    if (e1.type == e2.type) {
      if (e1.type == TINT) {
        int r = e1.i % e2.i;
        status(r);
        push(r);
      }
      else if (e1.type == TFLOAT) {
        float r = e1.f % e2.f;
        status(r);
        push(r);
      } else {
        throw new ProcessorError("cannot modulo type " + TNAME[e1.type]);
      }
    }
    else if (e1.type == TFLOAT && e2.type == TINT) {
      float r = e1.f % e2.i;
      status(r);
      push(r);
    }
    else if (e2.type == TFLOAT && e1.type == TINT) {
      float r = e1.i % e2.f;
      status(r);
      push(r);
    } else {
      throw new ProcessorError("cannot modulo types " + TNAME[e1.type] + " and " + TNAME[e2.type]);
    }
  }
  
  void shiftl() {
    M e2 = pop();
    M e1 = pop();
    if (e1.type == e2.type && e1.type == TINT) {
      int r = e1.i << e2.i;
      status(r);
      push(r);
    } else {
      throw new ProcessorError("cannot shift types " + TNAME[e1.type] + " and " + TNAME[e2.type]);
    }
  }
  
  void shiftr() {
    M e2 = pop();
    M e1 = pop();
    if (e1.type == e2.type && e1.type == TINT) {
      int r = e1.i >>> e2.i;
      status(r);
      push(r);
    } else {
      throw new ProcessorError("cannot shift types " + TNAME[e1.type] + " and " + TNAME[e2.type]);
    }
  }
  
  void and() {
    M e2 = pop();
    M e1 = pop();
    if (e1.type == e2.type && e1.type == TINT) {
      int r = e1.i & e2.i;
      status(r);
      push(r);
    } else {
      throw new ProcessorError("cannot and types " + TNAME[e1.type] + " and " + TNAME[e2.type]);
    }
  }
  
  void or() {
    M e2 = pop();
    M e1 = pop();
    if (e1.type == e2.type && e1.type == TINT) {
      int r = e1.i | e2.i;
      status(r);
      push(r);
    } else {
      throw new ProcessorError("cannot or types " + TNAME[e1.type] + " and " + TNAME[e2.type]);
    }
  }
  
  void xor() {
    M e2 = pop();
    M e1 = pop();
    if (e1.type == e2.type && e1.type == TINT) {
      int r = e1.i ^ e2.i;
      status(r);
      push(r);
    } else {
      throw new ProcessorError("cannot xor types " + TNAME[e1.type] + " and " + TNAME[e2.type]);
    }
  }
  
  void call() {
    M addr = pop();
    if (addr.type != TINT && addr.type != TFUNC && addr.type != TANON) {
      throw new ProcessorError("calling bad type " + TNAME[addr.type]);
    }
    if (addr.type == TFUNC || addr.type == TINT) {
      int newPC = addr.i;
      pushFrameAndJump(newPC, false);
      me = me_banked;
      int argc = peek(sp+FRAME_3_ARGC).i;
      if ((newPC & 0x800000) == 0x800000) {
        ExtCall ec = extLinks.get(pc);
        if (ec == null) throw new ProcessorError(String.format("bad external call 0x%06x", pc));
        ec.doexe(this, getArgs(fp, argc)); 
      }
    } else if (addr.type == TANON) {
      MSet vars = addr.ref;
      int newPC = addr.i;
      pushFrameAndJump(newPC, false);
      me = me_banked;
      
      // put ((MSet)addr.ref) on stack (adsVars)
      int len = vars.size();
      for (int i = 0; i < len; i++) {
        push(vars.get(i));
      }
    }
  }
  
  void call_im() {
    int newPC = pcodetos(pc, 3);
    pc += 3;
    pushFrameAndJump(newPC, false);
    me = me_banked;
    int argc = peek(sp+FRAME_3_ARGC).i;
    if ((newPC & 0x800000) == 0x800000) {
      ExtCall ec = extLinks.get(pc);
      if (ec == null) throw new ProcessorError(String.format("bad external call 0x%06x", pc));
      ec.doexe(this, getArgs(fp, argc)); 
    }
  }
  
  void call_r() {
    int rel = pcodetos(pc, 3) - 4;
    pc += 3;
    pushFrameAndJump(pc + rel, false);
    me = me_banked;
  }
  
  void pushFrameAndJump(int newPC, boolean interrupt) {
    push(me);
    push(pc);
    push_fp(interrupt);
    fp = sp;
    pc = newPC;
  }
  
  void ano_cre() {
    // TODO check types
    M addr = pop();
    M locals = pop();
    M ret = new M(addr.i, (MSet)locals.ref);
    push(ret);
  }
  
  void ret() {
    if (fp == 0xffffffff) throw new ProcessorError.ProcessorStackError();
    boolean leftInterrupt = popFrame();
    if (pc == 0xffffffff) throw new ProcessorError.ProcessorFinishedError(nilM);
    if (!leftInterrupt) {
      int argc = pop().i;
      sp += argc;
    }
  }
  
  void retv() {
    M t = new M().copy(pop());
    if (fp == 0xffffffff) throw new ProcessorError.ProcessorStackError();
    boolean leftInterrupt = popFrame();
    if (pc == 0xffffffff) throw new ProcessorError.ProcessorFinishedError(t);
    if (!leftInterrupt) {
      int argc = pop().i;
      sp += argc;
      push(t);
    }
  }
  
  boolean popFrame() {
    if (fp == 0xffffffff) throw new ProcessorError.ProcessorStackError();
    boolean leftInterrupt = (fp & 0x80000000) == 0; 
    sp = fp & ~0x80000000;
    pop_fp();
    pc = pop().i;
    me = pop();
    if (leftInterrupt) {
      leftInterrupt = (fp & 0x80000000) != 0;
      if (leftInterrupt) {
        fp &= ~0x80000000;
        pop_sr();
        nestedIRQ--;
        if (irqHandler != null) irqHandler.leftIRQ(oldpc);
      }
    }

    return leftInterrupt;
  }
  
  void in() {
    M mset = pop();
    M mval = pop();
    zero = false;
    if (mset.type == TSET) {
      MSet set = mset.ref;
      int len = set.size();
      if (set.getType() == MSet.TMAP) {
        for (int i = 0; i < len; i++) {
          M element = set.getElement(i).ref.get(0); // get key
          sub_i(false, true, element, mval);
          if (zero) return;
        }
      } else {
        for (int i = 0; i < len; i++) {
          M element = set.getElement(i);
          sub_i(false, true, element, mval);
          if (zero) return;
        }
      }
    } else if (mset.type == TSTR) {
      zero = mset.str.contains(mval.asString());
    } else {
      throw new ProcessorError("cannot check containment of type " + TNAME[mset.type]);
    }
  }
  
  void push_cond(int icond) {
    int val = 0;
    switch (icond) {
    case ICOND_EQ:
      if (zero) val = 1; break;
    case ICOND_NE:
      if (!zero) val = 1; break;
    case ICOND_GE:
      if (zero || !minus) val = 1; break;
    case ICOND_GT:
      if (!zero && !minus) val = 1; break;
    case ICOND_LE:
      if (zero || minus) val = 1; break;
    case ICOND_LT:
      if (!zero && minus) val = 1; break;
    }
    push(val);
  }
  
  void jump(int icond) {
    int dst = pcodetoi( pc, 3);
    pc += 3;
    switch (icond) {
    case ICOND_AL:
      pc = dst; break;
    case ICOND_EQ:
      if (zero) pc = dst; break;
    case ICOND_NE:
      if (!zero) pc = dst; break;
    case ICOND_GE:
      if (zero || !minus) pc = dst; break;
    case ICOND_GT:
      if (!zero && !minus) pc = dst; break;
    case ICOND_LE:
      if (zero || minus) pc = dst; break;
    case ICOND_LT:
      if (!zero && minus) pc = dst; break;
    }
  }
  
  void bra(int icond) {
    int rel = pcodetos( pc, 3) - 1 - 3;
    pc += 3;
    switch (icond) {
    case ICOND_AL:
      pc += rel; break;
    case ICOND_EQ:
      if (zero) pc += rel; break;
    case ICOND_NE:
      if (!zero) pc += rel; break;
    case ICOND_GE:
      if (zero || !minus) pc += rel; break;
    case ICOND_GT:
      if (!zero && !minus) pc += rel; break;
    case ICOND_LE:
      if (zero || minus) pc += rel; break;
    case ICOND_LT:
      if (!zero && minus) pc += rel; break;
    }
  }

  void stepProc() {
    if (irqHandler != null) irqHandler.step(pc);
    oldpc = pc;
    int instr;
    if (sp < exe.getStackTop()) {
      throw new ProcessorError("stack overflow");
    }
    if ((pc & 0xff800000) == 0) {
      instr = (int)(code[pc] & 0xff);
      if (pc < 0 || pc >= code.length) {
        throw new ProcessorError(String.format("bad instruction address 0x%08x", pc));
      }
    }
    else {
      instr = (int)(code_internal_funcs[pc - 0xff000000] & 0xff);
    }
    pc++;
    switch (instr) {
    case INOP:
      break;
    case IADD:
      add();
      break;
    case ISUB:
      sub();
      break;
    case IMUL:
      mul();
      break;
    case IDIV:
      div();
      break;
    case IREM:
      rem();
      break;
    case ISHIFTL:
      shiftl();
      break;
    case ISHIFTR:
      shiftr();
      break;
    case IAND:
      and();
      break;
    case IOR :
      or();
      break;
    case IXOR:
      xor();
      break;
    case INOT:
      not();
      break;
    case INEG:
      neg();
      break;
    case ILNOT:
      lnot();
      break;
    case ICMP:
      cmp();
      break;
    case ICMPN:
      cmpn();
      break;
      
    case ICMP_0:
      cmp_0();
      break;
    case ICMN_0:
      cmn_0();
      break;
    case IADD_IM:
      add_im();
      break;
    case ISUB_IM:
      sub_im();
      break;
    case IPUSH_S:
      push_s();
      break;
    case IPUSH_U:
      push_u();
      break;
    case IPUSH_0:
      push_im_int(0);
      break;
    case IPUSH_1:
      push_im_int(1);
      break;
    case IPUSH_2:
      push_im_int(2);
      break;
    case IPUSH_3:
      push_im_int(3);
      break;
    case IPUSH_4:
      push_im_int(4);
      break;
    case IPUSH_NIL:
      push_nil();
      break;
    case IDEF_ME:
      def_me();
      break;
    case IPUSH_ME:
      push_me();
      break;
    case IUDEF_ME:
      udef_me();
      break;
    
    case IADD_Q1:
      add_q(1);
      break;
    case IADD_Q2:
      add_q(2);
      break;
    case IADD_Q3:
      add_q(3);
      break;
    case IADD_Q4:
      add_q(4);
      break;
    case IADD_Q5:
      add_q(5);
      break;
    case IADD_Q6:
      add_q(6);
      break;
    case IADD_Q7:
      add_q(7);
      break;
    case IADD_Q8:
      add_q(8);
      break;
    case ISUB_Q1:
      sub_q(1);
      break;
    case ISUB_Q2:
      sub_q(2);
      break;
    case ISUB_Q3:
      sub_q(3);
      break;
    case ISUB_Q4:
      sub_q(4);
      break;
    case ISUB_Q5:
      sub_q(5);
      break;
    case ISUB_Q6:
      sub_q(6);
      break;
    case ISUB_Q7:
      sub_q(7);
      break;
    case ISUB_Q8:
      sub_q(8);
      break;

    case ICAST_I:
      cast(TINT);
      break;
    case ICAST_F:
      cast(TFLOAT);
      break;
    case ICAST_S:
      cast(TSTR);
      break;
    case ICAST_CH:
      cast(TO_CHAR);
      break;
    case IGET_TYP:
      get_typ();
      break;

    case IPOP:
      pop();
      break;
    case IDUP:
      dup();
      break;
    case ISWAP:
      swap();
      break;
    case ICPY:
      cpy();
      break;
    case ISTOR:
      stor();
      break;
    case ISTOR_IM:
      stor_im();
      break;
    case ILOAD :
      load();
      break;
    case ILOAD_IM:
      load_im();
      break;
    case ISTOR_FP:
      stor_fp();
      break;
    case ILOAD_FP:
      load_fp();
      break;
      
    case ISP_INCR:
      sp_incr();
      break;
    case ISP_DECR:
      sp_decr();
      break;

    case IPUSH_PC:
      push_pc();
      break;
    case IPUSH_SP:
      push_sp();
      break;
    case IPUSH_FP:
      push_fp(false);
      break;
    case IPUSH_SR:
      push_sr();
      break;
      
      
    case ISET_CRE:
      set_cre();
      break;
    case IARR_CRE:
      arr_cre();
      break;
    case ITUP_CRE:
      tup_cre();
      break;
    case ISET_DRF:
      set_drf();
      break;
    case ISET_WR:
      set_wr();
      break;
    case IARR_ADD:
      break;
    case IMAP_ADD:
      map_add();
      break;
    case ISET_DEL:
      // TODO
      break;
    case IARR_INS:
      // TODO
      break;
    case ISET_SZ:
      set_sz();
      break;
    case ISET_RD:
      set_rd();
      break;
    case IRNG2:
      rng(2);
      break;
    case IRNG3:
      rng(3);
      break;

    case ICALL: 
      call();
      break;
    case ICALL_IM: 
      call_im();
      break;
    case ICALL_R: 
      call_r();
      break;
    case IANO_CRE: 
      ano_cre();
      break;
    case IRET: 
      ret();
      break;
    case IRETV: 
      retv();
      break;
    case IPUSH_EQ: 
      push_cond(ICOND_EQ);
      break;
    case IPUSH_NE: 
      push_cond(ICOND_NE);
      break;
    case IPUSH_GE: 
      push_cond(ICOND_GE);
      break;
    case IPUSH_GT: 
      push_cond(ICOND_GT);
      break;
    case IPUSH_LE: 
      push_cond(ICOND_LE);
      break;
    case IPUSH_LT: 
      push_cond(ICOND_LT);
      break;
    case IIN: 
      in();
      break;
    case IJUMP: 
      jump(ICOND_AL);
      break;
    case IJUMP_EQ: 
      jump(ICOND_EQ);
      break;
    case IJUMP_NE: 
      jump(ICOND_NE);
      break;
    case IJUMP_GE: 
      jump(ICOND_GE);
      break;
    case IJUMP_GT: 
      jump(ICOND_GT);
      break;
    case IJUMP_LE: 
      jump(ICOND_LE);
      break;
    case IJUMP_LT: 
      jump(ICOND_LT);
      break;
    case IBRA: 
      bra(ICOND_AL);
      break;
    case IBRA_EQ: 
      bra(ICOND_EQ);
      break;
    case IBRA_NE: 
      bra(ICOND_NE);
      break;
    case IBRA_GE: 
      bra(ICOND_GE);
      break;
    case IBRA_GT: 
      bra(ICOND_GT);
      break;
    case IBRA_LE: 
      bra(ICOND_LE);
      break;
    case IBRA_LT: 
      bra(ICOND_LT);
      break;
      
    case IBKPT: 
      throw new ProcessorBreakpointError();
      
    default:
      throw new Error(String.format("unknown instruction 0x%02x", instr));
    }
  }
  
  M[] getArgs(int fp, int args) {
    fp &= ~0x80000000;
    M argv[] = new M[args];
    for (int i = 0; i < args; i++) {
      argv[args - i - 1] = memory[fp+FRAME_SIZE+(args - i)];
    }
    return argv;
  }
  
  public static class M {
    public byte type;
    public MSet ref;
    public String str;
    public int i;
    public float f;

    public M copy(M m) {
      type = m.type;
      ref = m.ref;
      str = m.str;
      i = m.i;
      f = m.f;
      return this;
    }
    public M() {type = TINT;}
    public M(int x) { type = TINT; i = x; }
    public M(float x) { type = TFLOAT; f = x; }
    public M(String x) { type = TSTR; str = x; }
    public M(int addr, MSet locals) { type = TANON; i = addr; ref = locals; }
    public M(Object o) {
      if (o instanceof Integer) {
        i = ((Integer) o).intValue();
        type = TINT;
      }
      else if (o instanceof Boolean) {
        i = ((Boolean) o).booleanValue() ? 1 : 0;
        type = TINT;
      }
      else if (o instanceof Float) {
        f = ((Float) o).floatValue();
        type = TFLOAT;
      }
      else if (o instanceof String) {
        str = ((String) o);
        type = TSTR;
      } 
      else if (o instanceof MSet) {
        ref = (MSet)o;
        type = TSET;
      } 
      else {
        throw new ProcessorError("bad memory class " + o.getClass().getSimpleName());
      }
    }
    public M(byte[] data) {
      type = TSET;
      MListMap l = new MListMap();
      for (byte b : data) l.add(new M((int)(b) & 0xff));
      ref = l;
    }
    public String asString() {
      switch(type) {
      case TNIL:
        return "nil";
      case TINT:
        return ""+ i;
      case TFLOAT:
        return ""+ f;
      case TSTR:
        return str;
      case TFUNC:
        return String.format("->0x%06x", i);
      case TANON:
        return String.format(":>0x%06x", i);
      case TSET:
        return ref.toString();
      default:
        return "?" + type;
      }
    }

    public float asFloat() {
      switch(type) {
      case TINT:
        return (float)i;
      case TFLOAT:
        return f;
      case TSTR:
        try { return Float.parseFloat(str.trim()); } catch (Throwable t) {}
        return Float.NaN;
      default:
        return Float.NaN;
      }
    }

    public int asInt() {
      switch(type) {
      case TINT:
        return i;
      case TFLOAT:
        return (int)f;
      case TSTR:
        try { return Integer.parseInt(str.trim()); } catch (Throwable t) {}
        return 0;
      default:
        return 0;
      }
    }

    public String toString() {
      switch(type) {
      case TNIL:
        return "("+ asString() + ")";
      case TINT:
        return "i"+ asString();
      case TFLOAT:
        return "f"+ asString();
      case TSTR:
        return "s\'" + asString() + "'";
      case TFUNC:
        return "c" + asString();
      case TANON:
        return "C" + asString();
      case TSET:
        return "t" + asString();
      default:
        return "?" + type;
      }
    }
    
    public Object getRaw() {
      switch(type) {
      case TNIL:
        return null;
      case TINT:
        return i;
      case TFLOAT:
        return f;
      case TSTR:
        return str;
      case TFUNC:
        return this;
      case TANON:
        return this;
      case TSET:
        return ref;
      default:
        return null;
      }
    }
  }
  
  String getStack() {
    StringBuilder sb = new StringBuilder("{ ");
    int ix = sp+1;
    int fp = this.fp;
    int argc = 0;
    while (ix < memory.length) {
      if (ix == fp+1) {
        int args = memory[ix + FRAME_3_ARGC - 1].i;
        M me = memory[ix + FRAME_2_ME - 1];
        String meStr = me.type == TNIL ? "" : (" me:" + me.toString());
        sb.append("<<<FRAME a" + args +  meStr + ">>>  ");
        fp = memory[ix + FRAME_0_FP -1].i;
        ix += FRAME_SIZE;
        argc = args;
      } else {
        if (argc > 0) {
          argc--;
          sb.append('A');
        }
        sb.append(memory[ix] + "  ");
        ix++;
      }
    }
    sb.append('}');
    return sb.toString();
  }
  
  static byte[] assemble(String s) {
    return Assembler.assemble(s);
  }
  
  static final String IFUNC_SET_VISITOR_ASM =
      //func setVisitor(set, visitor) {
      //  res = (if isstr(set) nil else t[]);
      //  for (i in set) {
      //    mutation = visitor(i, <ix_nbr>);
      //    if (mutation != nil) res += mutation;
      //  }
      //  return res;
      //}
      "//.main.func.setVisitor:   \n"+
      "  sp_incr 2                \n"+
      "  load_fp $fp[-5]          \n"+
      "  get_typ                  \n"+
      "  push_3                   \n"+
      "  cmp                      \n"+
      "  bra_ne L1_notstr         \n"+
      "  push_nil                 \n"+
      "  bra L1_init              \n"+
      "L1_notstr:                 \n"+
      "  push_0                   \n"+
      "  set_cre                  \n"+
      "L1_init:                   \n"+
      "  stor_fp $fp[0]           \n"+
      "  sp_incr 3                \n"+
      "  load_fp $fp[-5]          \n"+
      "  stor_fp $fp[4]           \n"+
      "  push_0                   \n"+
      "  stor_fp $fp[3]           \n"+
      "L1_floop:                  \n"+
      "  load_fp $fp[3]           \n"+
      "  load_fp $fp[4]           \n"+
      "  set_sz                   \n"+
      "  cmp                      \n"+
      "  bra_ge L1_fexit          \n"+
      "  load_fp $fp[3]           \n"+ // add ix_nbr
      "  load_fp $fp[4]           \n"+
      "  load_fp $fp[3]           \n"+
      "  set_rd                   \n"+
      "  dup                      \n"+
      "  stor_fp $fp[1]           \n"+
      "  udef_me                  \n"+
      "  push_2                   \n"+ // 2 params: entry, ix_nbr
      "  load_fp $fp[-6]          \n"+
      "  call                     \n"+
      "  dup                      \n"+
      "  stor_fp $fp[2]           \n"+
      "  push_nil                 \n"+
      "  cmp                      \n"+
      "  bra_eq L2_ifend          \n"+
      "  load_fp $fp[0]           \n"+
      "  load_fp $fp[2]           \n"+
      "  add                      \n"+
      "  stor_fp $fp[0]           \n"+
      "L2_ifend:                  \n"+
      "  load_fp $fp[3]           \n"+
      "  add_q1                   \n"+
      "  stor_fp $fp[3]           \n"+
      "  bra L1_floop             \n"+
      "L1_fexit:                  \n"+
      "  sp_decr 3                \n"+
      "  load_fp $fp[0]           \n"+
      "  retv                     \n"+
      "  "  ;

  static long regA = 0x20070515, regB = 0x20090129, regC = 0x20140315;
  static int calcRand() {
    // https://www.schneier.com/academic/archives/1994/09/pseudo-random_sequen.html
    regA = (((((regA>>31)^(regA>>6)^(regA>>4)^(regA>>2)^(regA<<1)^regA)
        & 0x00000001)<<31) | regA>>1);
    regB = ((((regB>>30)^(regB>>2)) & 0x00000001)<<30) | (regB>>1);
    regC = ((((regC>>28)^(regC>>1)) & 0x00000001)<<28) | (regC>>1);
    return (int)(regA ^ regB ^ regC);
  }
  static void randSeed(int seed) {
    if (seed == 0x19760401) seed = 0;
    regA = regB = regC = (0x19760401 ^ seed);
  }
  
  
  static void setDbg(List<String> a, boolean ena) {
    for (String s:a) {
      boolean all = s.equals("*") || s.equals("all");
      if (s.equals("run") || all) Processor.dbgRun = ena;
      if (s.equals("mem") || all) Processor.dbgMem = ena;
      if (s.equals("ast") || all) AST.dbg = ena;
      if (s.equals("gra") || all) Grammar.dbg = ena;
      if (s.equals("str") || all) StructAnalysis.dbg = ena;
      if (s.equals("fro") || all) CodeGenFront.dbg = ena;
      if (s.equals("frogra") || all) CodeGenFront.dbgDot = ena;
      if (s.equals("bak") || all) CodeGenBack.dbg = ena;
      if (s.equals("lin") || all) Linker.dbg = ena;
    }
  }
  
  static class EC_pc extends ExtCall {
    public Processor.M exe(Processor p, Processor.M[] args) {
      return new M(p.oldpc);
    }
  }
  static class EC_sp extends ExtCall {
    public Processor.M exe(Processor p, Processor.M[] args) {
      return new M(p.sp);
    }
  }
  static class EC_fp extends ExtCall {
    public Processor.M exe(Processor p, Processor.M[] args) {
      return new M(p.fp);
    }
  }

  static class EC_dbg extends ExtCall {
    final PrintStream out;
    public EC_dbg(PrintStream out) { this.out = out; }
    public Processor.M exe(Processor p, Processor.M[] args) {
      if (args == null || args.length == 0) {
        out.println("  run : " + Processor.dbgRun);
        out.println("  mem : " + Processor.dbgMem);
        out.println("  ast : " + AST.dbg);
        out.println("  gra : " + Grammar.dbg);
        out.println("  str : " + StructAnalysis.dbg);
        out.println("  fro : " + CodeGenFront.dbg);
        out.println("  bak : " + CodeGenBack.dbg);
        out.println("  lin : " + Linker.dbg);
        out.println("  frogra : " + CodeGenFront.dbgDot);
      } else {
        List<String> areas = new ArrayList<String>();
        for (M marg : args) {
          String cmd = marg.str.toLowerCase();
          if (marg.type == TSTR && (cmd.equals("on") || cmd.equals("1")) || marg.type == TINT && marg.i != 0) {
            setDbg(areas, true);
            areas.clear();
          } else if (marg.type == TSTR && (cmd.equals("off") || cmd.equals("0")) || marg.type == TINT && marg.i == 0) {
            setDbg(areas, false);
            areas.clear();
          } else {
            areas.add(cmd);
          }
        }
      }
      return null;
    }
  } 
  static class EC_dumpstack extends ExtCall {
    public Processor.M exe(Processor p, Processor.M[] args) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      String data = null;
      try {
        PrintStream ps = new PrintStream(baos, true, "UTF-8");
        p.unwindStackTrace(ps);
        data = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        ps.close();
      } catch (Exception e) {}
      return new M(data);
    }
  } 
  static class EC_const extends ExtCall {
    public Processor.M exe(Processor p, Processor.M[] args) {
      if (p.exe.getConstants().keySet().isEmpty()) return null;
      int min = Integer.MAX_VALUE;
      int max = 0;
      for (int a : p.exe.getConstants().keySet()) {
        min = Math.min(a, min);
        max = Math.max(a, max);
      }
      M m = new M();
      m.ref = new MMemList(p.memory, min, max + 1);
      m.type = TSET;
      return m;
    }
  }
  static class EC_mem extends ExtCall {
    public Processor.M exe(Processor p, Processor.M[] args) {
      M m = new M();
      m.ref = new MMemList(p.memory, 0, p.memory.length);
      m.type = TSET;
      return m;
    }
  }
  static class EC_println extends ExtCall {
    final PrintStream out;
    public EC_println(PrintStream out) { this.out = out; }
    public Processor.M exe(Processor p, Processor.M[] args) {
      if (args == null || args.length == 0) {
        out.println();
      } else {
        for (int i = 0; i < args.length; i++) {
          out.print(args[i].asString() + (i < args.length-1 ? " " : ""));
        }
      }
      out.println();
      return null;
    }
  }
  static class EC_print extends ExtCall {
    final PrintStream out;
    public EC_print(PrintStream out) { this.out = out; }
    public Processor.M exe(Processor p, Processor.M[] args) {
      if (args == null || args.length == 0) {
      } else {
        for (int i = 0; i < args.length; i++) {
          out.print(args[i].asString() + (i < args.length-1 ? " " : ""));
        }
      }
      return null;
    }
  }
  static class EC_rand extends ExtCall {
    public Processor.M exe(Processor p, Processor.M[] args) {
      return new M(calcRand());
    }
  }
  static class EC_randseed extends ExtCall {
    public Processor.M exe(Processor p, Processor.M[] args) {
      if (args == null || args.length == 0) {
      } else {
        randSeed(args[0].asInt());
      }
      return null;
    }
  }
  static class EC_cpy extends ExtCall {
    public Processor.M exe(Processor p, Processor.M[] args) {
      if (args == null || args.length == 0) {
        return null;
      } else {
        M res = new M();
        M src = args[0];
        if (src.type != TSET) {
          res.copy(src);
        } else {
          res.type = TSET;
          res.ref = src.ref.copyShallow();
        }
        return res;
      }
    }
  }
  static class EC_byte extends ExtCall {
    public Processor.M exe(Processor p, Processor.M[] args) {
      if (args == null || args.length == 0) {
        return null;
      } else {
        M res = new M();
        res.type = TINT;
        M src = args[0];
        if (src.type == TINT || src.type == TFLOAT) {
          res.i = src.asInt() & 0xff;
        } else if (src.type == TSTR) {
          res.i = (src.str.length() == 1 ? src.str.charAt(0) : src.asInt()) & 0xff ;
        }
        return res;
      }
    }
  }
  static class EC_strstr extends ExtCall {
    public Processor.M exe(Processor p, Processor.M[] args) {
      if (args == null || args.length < 2 || args.length > 3) {
        return null;
      } else {
        M res = new M();
        res.type = TINT;
        M str = args[0];
        M pat = args[1];
        int from = 0;
        if (args.length == 3) {
          from = args[2].asInt();
        }
        if (str.type == TSTR) {
          res.i = str.asString().indexOf(pat.asString(), from);
        } else {
          res.i = -1;
        }
        return res;
      }
    }
  }
  static class EC_strstrr extends ExtCall {
    public Processor.M exe(Processor p, Processor.M[] args) {
      if (args == null || args.length < 2 || args.length > 3) {
        return null;
      } else {
        M res = new M();
        res.type = TINT;
        M str = args[0];
        M pat = args[1];
        int from = 0;
        if (args.length == 3) {
          from = args[2].asInt();
        }
        if (str.type == TSTR) {
          res.i = str.asString().lastIndexOf(pat.asString(), from);
        } else {
          res.i = -1;
        }
        return res;
      }
    }
  }
  
  static class EC_lines extends ExtCall {
    public Processor.M exe(Processor p, Processor.M[] args) {
      if (args == null || args.length != 1) {
        return null;
      } else {
        MListMap mlist = new MListMap();
        mlist.makeArr();
        String str = args[0].asString();
        BufferedReader bufReader = new BufferedReader(new StringReader(str));
        String line;
        try {
          while((line = bufReader.readLine()) != null) {
            mlist.add(new M(line));
          }
        } catch (IOException ignore) {}
        return new M(mlist);
      }
    }
  }

  static class EC_atoi extends ExtCall {
    public Processor.M exe(Processor p, Processor.M[] args) {
      if (args == null || args.length < 1) {
        return null;
      } else {
        float res = 0;
        try {
          if (args.length == 1) {
            res = Float.parseFloat(args[0].asString());
          } else {
            res = Integer.parseInt(args[0].asString(), args[1].asInt());
          }
        } catch (Exception ignore) {}
        return new M(res);
      }
    }
  }

  public static M compileAndRun(String... sources) {
    return compileAndRun(0x0000, 0x4000, null, false, false, sources);
  }

  public static M compileAndRun(boolean dbgRun, boolean dbgMem, String... sources) {
    return compileAndRun(0x0000, 0x4000, null, dbgRun, dbgMem, sources);
  }

  public static M compileAndRun(int ramOffs, int constOffs, Map<String, ExtCall> extDefs, 
      boolean dbgRun, boolean dbgMem, String... sources) {
    if (extDefs == null) {
      extDefs = new HashMap<String, ExtCall>();
    }
    Processor.addCommonExtdefs(extDefs);
    M ret = null;
    Executable e = null;
    try {
      e = Compiler.compileOnce(extDefs, ramOffs, constOffs, sources);
    } catch (CompilerError ce) {
      Source src = Compiler.getSource();
      String s = src.getCSource();
      int strstart = Math.min(s.length(), Math.max(0, ce.getStringStart()));
      int strend = Math.min(s.length(), Math.max(0, ce.getStringEnd()));
      if (strstart > 0) {
        int ps = Math.min(s.length(), Math.max(0, strstart - 50));
        int pe = Math.max(0, Math.min(s.length(), strend + 50));
        if (!silence) System.out.println(ce.getMessage());
        if (!silence) System.out.println("... " + s.substring(ps, strstart) + 
            " -->" + s.substring(strstart, strend) + "<-- " +
            s.substring(strend, pe) + " ...");
      }
      throw ce;
    }
    Processor p = new Processor(0x10000, e);
    Processor.dbgRun = dbgRun;
    Processor.dbgMem = dbgMem;
    int i = 0;
    try {
      for (; i < 10000000*2; i++) {
        p.step();
      }
      throw new ProcessorError("processor hanged");
    } catch (ProcessorFinishedError pfe) {
      //System.out.println("processor end, retval " + pfe.getRet());
      ret = pfe.getRet();
    }
    catch (ProcessorError pe) {
      if (!silence) {
        p.dumpError(pe, System.out);
      }
      throw pe;
    }
    
    
    // TODO remove
    System.out.println(p.getSP());
    // TODO remove
    
    return ret;
  }
}
