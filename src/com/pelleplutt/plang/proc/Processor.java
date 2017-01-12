package com.pelleplutt.plang.proc;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.pelleplutt.plang.AST;
import com.pelleplutt.plang.CodeGenBack;
import com.pelleplutt.plang.CodeGenFront;
import com.pelleplutt.plang.Compiler;
import com.pelleplutt.plang.CompilerError;
import com.pelleplutt.plang.Executable;
import com.pelleplutt.plang.Grammar;
import com.pelleplutt.plang.Linker;
import com.pelleplutt.plang.StructAnalysis;
import com.pelleplutt.plang.proc.ProcessorError.ProcessorBreakpointError;
import com.pelleplutt.plang.proc.ProcessorError.ProcessorFinishedError;

public class Processor implements ByteCode {
  public static final int TNIL = 0;
  public static final int TINT = 1;
  public static final int TFLOAT = 2;
  public static final int TSTR = 3;
  public static final int TRANGE = 4;
  public static final int TFUNC = 5;
  public static final int TANON = 6;
  public static final int TSET = 7;
  
  static final int TO_CHAR = -1;
  
  public static boolean dbgMem = false;
  public static boolean dbgRun = false;
  public static boolean silence = false;
  
  public static final String TNAME[] = {
    "nil", "int", "float", "string", "range", "func", "anon", "set"
  };
  
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
  final byte[] code_internal_func_set_visitor;
  
  public Processor(int memorySize) {
    nilM.type = TNIL;
    memory = new M[memorySize];
    for (int i = 0; i < memorySize; i++) {
      memory[i] = new M();
      memory[i].type = TNIL;
    }
    code_internal_func_set_visitor = assemble(INTERNAL_FUNC_SET_VISITOR_ASM);
  }
  
  public Processor(int memorySize, Executable exe) {
    this(memorySize);
    setExe(exe);
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
  }

  
  public void setExe(Executable exe, String ...args) {
    this.exe = exe;
    this.args = args;
    pc = exe.getPCStart();
    this.code = exe.getMachineCode();
    this.extLinks = exe.getExternalLinkMap();
    Map<Integer, M> consts = exe.getConstants();
    for (int addr : consts.keySet()) {
      M m = consts.get(addr);
      poke(addr, m);
    }
    reset();
  }
  
  M pop() {
    return memory[++sp];
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
      c = code_internal_func_set_visitor;
    } else {
      c = code;
    }
    return codetoi(c, addr, bytes);
  }
  
  int pcodetos(int addr, int bytes) {
    byte c[];
    if ((addr & 0xff80000)!=0) {
      addr -= 0xff000000;
      c = code_internal_func_set_visitor;
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
    push(peekStack(pcodetos( pc++, 1)));
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
    int addr = pcodetoi( pc, 3);
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
    int addr = pcodetoi( pc, 3);
    pc += 3;
    push(peek(addr));
  }
  
  void stor_fp() {
    int rel = pcodetos( pc++, 1);
    poke(fp - rel, pop());
  }
  
  void load_fp() {
    int rel = pcodetos( pc++, 1);
    push(peek(fp - rel));
  }
  
  void sp_incr() {
    int m = pcodetoi( pc++, 1) + 1;
    for (int i = 0; i < m; i++) {
      poke(sp - i, nilM);
    }
    sp -= m;
  }
  
  void sp_decr() {
    int m = pcodetoi( pc++, 1) + 1;
    sp += m;
  }
  
  void set_cre() {
    int elements = pop().asInt();
    if (elements == -1) {
      // create arg array
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
  
  void set_drf() {
    M mix = pop();
    M mset = pop();
    if (mset.type == TSET || mset.type == TRANGE) {
      if (mix.type == TSET || mix.type == TRANGE) {
        derefSetArgSet((MSet)mset.ref, (MSet)mix.ref);
      } else if (mix.type == TANON) {
        derefSetArgAnon((MSet)mset.ref, mix);
      } else {
        push(((MSet)mset.ref).get(mix));
      }
    } else if (mset.type == TSTR) {
      if (mix.type == TSET || mix.type == TRANGE) {
        derefStringArgSet(mset.str, (MSet)mix.ref);
      } else if (mix.type == TANON) {
        derefStringArgAnon(mset.str, mix);
      } else {
        int ix = mix.asInt();
        if (ix < 0) ix = mset.str.length() + ix;
        push(mset.str.charAt(ix));
      }
    } else if (mset.type == TINT) {
      int res = 0;
      if (mix.type == TSET || mix.type == TRANGE) {
        MSet drf = (MSet)mix.ref;
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
    push(0xff000000); // the assembled function INTERNAL_FUNC_SET_VISITOR_ASM
    call();
  }
  
  void derefStringArgAnon(String string, M adrf) {
    push(adrf);
    push(string);
    push(2);
    push(0xff000000); // the assembled function INTERNAL_FUNC_SET_VISITOR_ASM
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
        ((MSet)mset.ref).remove(mix);
      } else {
        M m = new M();
        m.copy(mval);
        ((MSet)mset.ref).set(mix, m);
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
    if (mix.type == TSET || mix.type == TRANGE) {
      MSet ixset = (MSet)mix.ref;
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
      ((MSet)mmap.ref).set(mk.getRaw(), mv);
    } else {
      throw new ProcessorError("cannot add tuples to type " + TNAME[mmap.type]);
    }
  }
  
  void set_sz() {
    M e = pop();
    if (e.type == TSET) {
      push(((MSet)e.ref).size());
    } else if (e.type == TSTR) {
      push(e.str.length());
    } else if (e.type == TRANGE) {
      push(((MRange)e.ref).size());
    } else {
      push(0);
    }
  }
  
  void set_rd() {
    int ix = pop().asInt();
    M mset = pop();
    if (mset.type == TSET || mset.type == TRANGE) {
      push(((MSet)mset.ref).get(ix));
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
    M mr = new M();
    mr.ref = range;
    mr.type = TRANGE;
    push(mr);
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
        ((MSet)me1.ref).add(me2);
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
      ((MSet)e1.ref).add(m);
      push(e1);
    }
    else if (e2.type == TSET) {
      M m = new M();
      m.copy(e1);
      ((MSet)e2.ref).insert(0, m);
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
    sub(true, true);
  }
  void sub(boolean push) {
    sub(push, true);
  }
  void sub(boolean push, boolean naturalOrder) {
    M e2 = pop();
    M e1 = pop();
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
    else {
      throw new ProcessorError("cannot subtract types " + TNAME[e1.type] + " and " + TNAME[e2.type]);
    }
  }
  
  void cmp() {
    sub(false);
  }
  
  void cmpn() {
    sub(false, false);
  }
  
  void cmp_0() {
    push(zeroM);
    sub(false);
  }
  
  void cmn_0() {
    push(zeroM);
    sub(false, false);
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

  void def_me() {
    M m = new M();
    m.copy(peekStack(0));
    me_banked = m;
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
    M m = peekStack(0);
    switch (type) {
    case TINT:
      switch (m.type) {
      case TFUNC:
      case TANON:
      case TINT: break;
      case TFLOAT: m.i = (int)m.f; break;
      case TNIL: m.i = 0; break;
      case TSTR: m.i = m.str.length() == 1 ? m.str.charAt(0) : Integer.parseInt(m.str); break;
      case TRANGE: break;
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
      case TSTR: m.f = Float.parseFloat(m.str); break;
      case TRANGE: break;
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
      case TRANGE: break;
      case TSET: break;
      }
      break;
    case TSTR:
      m.str = m.asString();
      break;
    }
    m.type = (byte)(type == TO_CHAR ? TSTR : type);
  }
  
  void get_typ() {
    M m = pop();
    push(m.type);
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
      int r = e1.i >> e2.i;
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
      int a = addr.i;
      push(me);
      push(pc);
      push(fp);
      int args = peek(sp+FRAME_3_ARGC).i;
      fp = sp;
      pc = a;
      me = me_banked;
      if ((pc & 0x800000) == 0x800000) {
        ExtCall ec = extLinks.get(pc);
        if (ec == null) throw new ProcessorError(String.format("bad external call 0x%06x", pc));
        M ret = ec.exe(this, getArgs(fp, args)); 
        push(ret == null ? nilM : ret);
        retv();
      }
    } else if (addr.type == TANON) {
      MSet vars = ((MSet)addr.ref);
      int a = addr.i;
      push(me);
      push(pc);
      push(fp);
      fp = sp;
      pc = a;
      me = me_banked;
      
      // put ((MSet)addr.ref) on stack (adsVars)
      int len = vars.size();
      for (int i = 0; i < len; i++) {
        push(vars.get(i));
      }
    }
  }
  
  void call_im() {
    push(me);
    push(pc+3);
    push(fp);
    int args = peek(sp+FRAME_3_ARGC).i;
    fp = sp;
    pc = pcodetos( pc, 3);
    me = me_banked;
    if ((pc & 0x800000) == 0x800000) {
      ExtCall ec = extLinks.get(pc);
      if (ec == null) throw new ProcessorError(String.format("bad external call 0x%06x", pc));
      M ret = ec.exe(this, getArgs(fp, args)); 
      push(ret == null ? nilM : ret);
      retv();
    }
  }
  
  void ano_cre() {
    // TODO check types
    M addr = pop();
    M locals = pop();
    M ret = new M(addr.i, (MSet)locals.ref);
    push(ret);
  }
  
  void ret() {
    sp = fp;
    if (fp == 0xffffffff) throw new ProcessorError.ProcessorStackError();
    fp = pop().i;
    pc = pop().i;
    me = pop();
    if (pc == 0xffffffff) throw new ProcessorError.ProcessorFinishedError(nilM);
    int argc = pop().i;
    sp += argc;
  }
  
  void retv() {
    M t = new M().copy(pop());
    sp = fp;
    if (fp == 0xffffffff) throw new ProcessorError.ProcessorStackError();
    fp = pop().i;
    pc = pop().i;
    me = pop();
    if (pc == 0xffffffff) throw new ProcessorError.ProcessorFinishedError(t);
    int argc = pop().i;
    sp += argc;
    push(t);
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
  
  public void step() {
    try {
      if (dbgRun) stepDebug(System.out);
      else        stepProc();
    } catch (Throwable t) {
      if (t instanceof ProcessorError) throw t;
      else {
        t.printStackTrace();
        throw new ProcessorError(t.getMessage());
      }
    }
  }
  
  void stepDebug(PrintStream out) {
    String procInfo = getProcInfo();
    String disasm;
    if ((pc & 0xff800000) != 0) {
      disasm = Assembler.disasm(code_internal_func_set_visitor, pc - 0xff000000);
    } else {
      disasm = Assembler.disasm(code, pc);
    }
    String dbgComment = exe.getDebugInfo(pc);
    out.println(String.format("%s      %-32s  %s", procInfo, disasm, (dbgComment == null ? "" : ("; " + dbgComment))));
    stepProc();
    String stack = getStack();
    out.println(stack);
  }

  void stepProc() {
    oldpc = pc;
    int instr;
    if ((pc & 0xff800000) == 0) 
      instr = (int)(code[pc] & 0xff);
    else
      instr = (int)(code_internal_func_set_visitor[pc - 0xff000000] & 0xff);
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

    case ISET_CRE:
      set_cre();
      break;
    case IARR_CRE:
      arr_cre();
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
      break;
    case IARR_INS:
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
    M argv[] = new M[args];
    for (int i = 0; i < args; i++) {
      argv[args - i - 1] = memory[fp+FRAME_SIZE+(args - i)];
    }
    return argv;
  }
  
  public static class M {
    public byte type;
    public Object ref;
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
      else if (o instanceof Float) {
        f = ((Float) o).floatValue();
        type = TFLOAT;
      }
      else if (o instanceof String) {
        str = ((String) o);
        type = TSTR;
      } 
      else if (o instanceof MRange) {
        ref = o;
        type = TRANGE;
      } 
      else {
        throw new ProcessorError("bad memory class " + o.getClass().getSimpleName());
      }
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
        return String.format(":>0x%06x", i) + ref;
      case TSET:
        return ((MSet)ref).toString();
      case TRANGE:
        return ((MRange)ref).toString();
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
      case TRANGE:
        return "r"+ asString();
      case TSTR:
        return "s\'" + asString() + "'";
      case TFUNC:
        return "c" + asString();
      case TANON:
        return "C" + asString();
      case TSET:
        return "a" + asString();
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
      case TRANGE:
        return ref;
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
    for (int i = sp+1; i < memory.length; i++) {
      sb.append(memory[i] + " ");
    }
    sb.append('}');
    return sb.toString();
  }
  
  public void printStack(PrintStream out, String pre, int maxEntries) {
    for (int i = sp-1; i < sp+maxEntries; i++) {
      if (i >= memory.length) break;
      if (pre != null) out.print(pre);
      out.println(String.format("0x%06x %-8s %s", i, TNAME[memory[i].type], memory[i].asString()));
    }
  }
  
  public String getProcInfo() {
    return String.format("pc:0x%06x  sp:0x%06x  fp:0x%06x  sr:", pc, sp, fp) + 
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
  
  static byte[] assemble(String s) {
    return Assembler.assemble(s);
  }

  static final String INTERNAL_FUNC_SET_VISITOR_ASM =
      //func setVisitor(set, visitor) {
      //  res[];
      //  for (i in set) {
      //    mutation = visitor(i);
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
      "  load_fp $fp[4]           \n"+
      "  load_fp $fp[3]           \n"+
      "  set_rd                   \n"+
      "  dup                      \n"+
      "  stor_fp $fp[1]           \n"+
      "  udef_me                  \n"+
      "  push_1                   \n"+
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
  
  
  public static void addCommonExtdefs(Map<String, ExtCall> extDefs) {
    addCommonExtdefs(extDefs, System.in, System.out);
  }
  public static void addCommonExtdefs(Map<String, ExtCall> extDefs, final InputStream in, final PrintStream out) {
    extDefs.put("__dbg", new ExtCall() {
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
    });
    extDefs.put("println", new ExtCall() {
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
    });
    extDefs.put("print", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
        } else {
          for (int i = 0; i < args.length; i++) {
            out.print(args[i].asString() + (i < args.length-1 ? " " : ""));
          }
        }
        return null;
      }
    });
    extDefs.put("rand", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        return new M(calcRand());
      }
    });
    extDefs.put("randseed", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
        } else {
          randSeed(args[0].asInt());
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
          out.println(String.format("  0x%06x  %s", addr, p.getExecutable().getConstants().get(addr).toString()));
        }
        return null;
      }
    });
    extDefs.put("__stack", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          for (int addr = p.getSP(); addr < p.getMemory().length; addr++) {
            out.println(String.format("  0x%06x  %s", addr, p.getMemory()[addr].toString()));
          }
        } else {
          for (int addr = p.getMemory().length - args[0].i; addr < p.getMemory().length; addr++) {
            out.println(String.format("  0x%06x  %s", addr, p.getMemory()[addr].toString()));
          }
        }
        return null;
      }
    });
    extDefs.put("__mem", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          out.println(String.format("  0x%06x--0x%06x", 0, p.getMemory().length));
        } else {
          int start = args[0].i;
          int addr = start;
          int len = args.length < 2 ? 1 : args[1].i;
          while (addr < p.getMemory().length && addr < start + len) {
            out.println(String.format("  0x%06x  %s", addr, p.getMemory()[addr].toString()));
            addr++;
          }
        }
        return null;
      }
    });
  }
  
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
      e = Compiler.compile(extDefs, ramOffs, constOffs, sources);
    } catch (CompilerError ce) {
      String s = Compiler.getSource();
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
        System.out.println("**********************************************");
        System.out.println(String.format("Exception at pc 0x%06x", p.getPC()));
        System.out.println(p.getProcInfo());
        System.out.println(pe.getMessage());
        System.out.println("**********************************************");
        System.out.println("DISASM");
        Assembler.disasm(System.out, "   ", p.getExecutable().getMachineCode(), p.getPC(), 8);
        System.out.println("STACK");
        p.printStack(System.out, "   ", 16);
      }
      throw pe;
    }
    return ret;
  }
}
