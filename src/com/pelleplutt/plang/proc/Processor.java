package com.pelleplutt.plang.proc;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.plang.Executable;
import com.pelleplutt.plang.proc.ProcessorError.ProcessorBreakpointError;

public class Processor implements ByteCode {
  public static final int TNIL = 0;
  public static final int TINT = 1;
  public static final int TFLOAT = 2;
  public static final int TSTR = 3;
  public static final int TRANGE = 4;
  public static final int TCODE = 5;
  public static final int TARR = 6;
  public static final int TMAP = 7;
  public static final int TREF = 8;
  
  static final int TO_CHAR = -1;
  
  public static boolean dbgMem = false;
  public static boolean dbgRun = false;
  
  public static final String TNAME[] = {
    "nil", "int", "float", "string", "range", "code", "arr", "map", "ref"
  };
  
  M[] memory;
  byte[] code;
  int sp;
  int pc;
  int me;
  int me_banked;
  int oldpc;
  int fp;
  boolean zero;
  boolean minus;
  Map<Integer, ExtCall> extLinks;
  Executable exe;
  M nilM = new M();
  M zeroM = new M(0);
  String[] args;
  
  public Processor(int memorySize) {
    nilM.type = TNIL;
    memory = new M[memorySize];
    for (int i = 0; i < memorySize; i++) memory[i] = new M();
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
    push(-1); // pc
    push(-1); // fp
    fp = sp;
    me = -1;
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
  
  public void disasm(PrintStream out, String pre, int pc, int len) {
    disasm(out, pre, code, pc, len); 
  }

  public static void disasm(PrintStream out, byte[] code, int pc, int len) {
    disasm(out, null, code, pc, len);
  }
  public static void disasm(PrintStream out, String pre, byte[] code, int pc, int len) {
    while (len > 0 && pc < code.length) {
      if (pre != null) out.print(pre);
      out.println(String.format("0x%06x %s", pc, disasm(code, pc)));
      int instr = (int)(code[pc] & 0xff);
      int step = ISIZE[instr];
      if (step <= 0) step = 1;
      pc += step;
      len -= step;
    }
  }

  public static String disasm(byte[] code, int pc) {
    if (pc < 0 || pc >= code.length) {
      return String.format("BADADDR 0x%06x", pc);
    }
    int instr = (int)(code[pc++] & 0xff);
    StringBuilder sb = new StringBuilder();
    switch (instr) {
    case INOP:
      sb.append("nop     ");
      break;
    case IADD:
      sb.append("add     ");
      break;
    case ISUB:
      sb.append("sub     ");
      break;
    case IMUL:
      sb.append("mul     ");
      break;
    case IDIV:
      sb.append("div     ");
      break;
    case IREM:
      sb.append("rem     ");
      break;
    case ISHIFTL:
      sb.append("shiftl  ");
      break;
    case ISHIFTR:
      sb.append("shiftr  ");
      break;
    case IAND:
      sb.append("and     ");
      break;
    case IOR :
      sb.append("or      ");
      break;
    case IXOR:
      sb.append("xor     ");
      break;
    case ICMP:
      sb.append("cmp     ");
      break;
    case ICMPN:
      sb.append("cmpn    ");
      break;
    case INOT:
      sb.append("not     ");
      break;
    case INEG:
      sb.append("neg     ");
      break;
    case ILNOT:
      sb.append("lnot    ");
      break;
    
    case ICMP_0:
      sb.append("cmp_0   ");
      break;
    case ICMN_0:
      sb.append("cmpn_0  ");
      break;
    case IADD_IM:
      sb.append("add_im  ");
      sb.append(String.format("0x%02x", codetoi(code, pc, 1) + 1));
      break;
    case ISUB_IM:
      sb.append("sub_im  ");
      sb.append(String.format("0x%02x", codetoi(code, pc, 1) + 1));
      break;
    case IPUSH_S:
      sb.append("push_s  ");
      sb.append(String.format("%d", codetos(code, pc, 1)));
      break;
    case IPUSH_U:
      sb.append("push_u  ");
      sb.append(String.format("%d", codetoi(code, pc, 1)+128));
      break;
    case IPUSH_NIL:
      sb.append("push_nil");
      break;
    case IPUSH_0:
      sb.append("push_0  ");
      break;
    case IPUSH_1:
      sb.append("push_1  ");
      break;
    case IPUSH_2:
      sb.append("push_2  ");
      break;
    case IPUSH_3:
      sb.append("push_3  ");
      break;
    case IPUSH_4:
      sb.append("push_4  ");
      break;
    case IDEF_ME:
      sb.append("def_me  ");
      break;
    case IPUSH_ME:
      sb.append("push_me ");
      break;
    case IUDEF_ME:
      sb.append("udef_me ");
      break;

    case IADD_Q1:
      sb.append("add_q1  ");
      break;
    case IADD_Q2:
      sb.append("add_q2  ");
      break;
    case IADQ3:
      sb.append("add_q3  ");
      break;
    case IADQ4:
      sb.append("add_q4  ");
      break;
    case IADD_Q5:
      sb.append("add_q5  ");
      break;
    case IADD_Q6:
      sb.append("add_q6  ");
      break;
    case IADD_Q7:
      sb.append("add_q7  ");
      break;
    case IADD_Q8:
      sb.append("add_q8  ");
      break;
    case ISUB_Q1:
      sb.append("sub_q1  ");
      break;
    case ISUB_Q2:
      sb.append("sub_q2  ");
      break;
    case ISUB_Q3:
      sb.append("sub_q3  ");
      break;
    case ISUB_Q4:
      sb.append("sub_q4  ");
      break;
    case ISUB_Q5:
      sb.append("sub_q5  ");
      break;
    case ISUB_Q6:
      sb.append("sub_q6  ");
      break;
    case ISUB_Q7:
      sb.append("sub_q7  ");
      break;
    case ISUB_Q8:
      sb.append("sub_q8  ");
      break;
      
    case ICAST_I:
      sb.append("cast_I  ");
      break;
    case ICAST_F:
      sb.append("cast_F  ");
      break;
    case ICAST_S:
      sb.append("cast_S  ");
      break;
    case ICAST_CH:
      sb.append("cast_ch ");
      break;
      
    case IPOP:
      sb.append("pop     ");
      break;
    case IDUP:
      sb.append("dup     ");
      break;
    case ISWAP:
      sb.append("swap    ");
      break;
    case ICPY:
      sb.append("cpy     ");
      sb.append(String.format("$sp[%4d]", codetos(code, pc, 1)));
      break;
    case ISTOR:
      sb.append("stor    ");
      break;
    case ISTOR_IM:
      sb.append("stor_im ");
      sb.append(String.format("mem[0x%06x]", codetoi(code, pc, 3)));
      break;
    case ILOAD :
      sb.append("load    ");
      break;
    case ILOAD_IM:
      sb.append("load_im ");
      sb.append(String.format("mem[0x%06x]", codetoi(code, pc, 3)));
      break;
    case ISTOR_FP:
      sb.append("stor_fp ");
      sb.append(String.format("$fp[%4d]", codetos(code, pc, 1)));
      break;
    case ILOAD_FP:
      sb.append("load_fp ");
      sb.append(String.format("$fp[%4d]", codetos(code, pc, 1)));
      break;

    case ISP_INCR:
      sb.append("sp_incr ");
      sb.append(String.format("%d", codetoi(code, pc, 1) + 1));
      break;
    case ISP_DECR:
      sb.append("sp_decr ");
      sb.append(String.format("%d", codetoi(code, pc, 1) + 1));
      break;

    case ISET_CRE:
      sb.append("set_cre ");
      break;
    case IARR_CRE:
      sb.append("arr_cre ");
      sb.append(String.format("mem[0x%06x]", codetoi(code, pc, 3)));
      break;
    case ISET_DRF:
      sb.append("set_drf ");
      break;
    case ISET_WR :
      sb.append("set_wr  ");
      break;
    case IARR_ADD:
      sb.append("arr_add ");
      break;
    case IMAP_ADD:
      sb.append("map_add ");
      break;
    case ISET_DEL:
      sb.append("set_del ");
      break;
    case IARR_INS:
      sb.append("arr_ins ");
      break;
    case ISET_SZ:
      sb.append("set_sz  ");
      break;
    case ISET_RD:
      sb.append("set_rd  ");
      break;
    case IRNG2:
      sb.append("rng2    ");
      break;
    case IRNG3:
      sb.append("rng3    ");
      break;

    case ICALL: 
      sb.append("call    ");
      break;
    case ICALL_IM: 
      sb.append("call_im ");
      sb.append(String.format("0x%06x", codetoi(code, pc, 3)));
      break;
    case IRET: 
      sb.append("ret     ");
      break;
    case IRETV: 
      sb.append("retv    ");
      break;
    case IJUMP: 
      sb.append("jump    ");
      sb.append(String.format("%d", codetoi(code, pc, 3)));
      break;
    case IJUMP_EQ: 
      sb.append("jump_eq ");
      sb.append(String.format("%d", codetoi(code, pc, 3)));
      break;
    case IJUMP_NE: 
      sb.append("jump_ne ");
      sb.append(String.format("%d", codetoi(code, pc, 3)));
      break;
    case IJUMP_GT: 
      sb.append("jump_gt ");
      sb.append(String.format("%d", codetoi(code, pc, 3)));
      break;
    case IJUMP_GE: 
      sb.append("jump_ge ");
      sb.append(String.format("%d", codetoi(code, pc, 3)));
      break;
    case IJUMP_LT: 
      sb.append("jump_lt ");
      sb.append(String.format("%d", codetoi(code, pc, 3)));
      break;
    case IJUMP_LE: 
      sb.append("jump_le ");
      sb.append(String.format("%d", codetoi(code, pc, 3)));
      break;
    case IBRA: 
      sb.append("bra     ");
      sb.append(String.format("%d (0x%06x)", codetos(code, pc, 3), pc + codetos(code, pc, 3)-1));
      break;
    case IBRA_EQ: 
      sb.append("bra_eq  ");
      sb.append(String.format("%d (0x%06x)", codetos(code, pc, 3), pc + codetos(code, pc, 3)-1));
      break;
    case IBRA_NE: 
      sb.append("bra_ne  ");
      sb.append(String.format("%d (0x%06x)", codetos(code, pc, 3), pc + codetos(code, pc, 3)-1));
      break;
    case IBRA_GT: 
      sb.append("bra_gt  ");
      sb.append(String.format("%d (0x%06x)", codetos(code, pc, 3), pc + codetos(code, pc, 3)-1));
      break;
    case IBRA_GE: 
      sb.append("bra_ge  ");
      sb.append(String.format("%d (0x%06x)", codetos(code, pc, 3), pc + codetos(code, pc, 3)-1));
      break;
    case IBRA_LT: 
      sb.append("bra_lt  ");
      sb.append(String.format("%d (0x%06x)", codetos(code, pc, 3), pc + codetos(code, pc, 3)-1));
      break;
    case IBRA_LE: 
      sb.append("bra_le  ");
      sb.append(String.format("%d (0x%06x)", codetos(code, pc, 3), pc + codetos(code, pc, 3)-1));
      break;
    case IBKPT: 
      sb.append("bkpt    ");
      break;
    default:
      sb.append("???     ");
      break;
    }
    return sb.toString();
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
  
  static int codetoi(byte[] code, int addr, int bytes) {
    int x = 0;
    for (int i = 0; i < bytes; i++) {
      x <<= 8;
      x |= (((int)code[addr++]) & 0xff);
    }
    return x;
  }

  static int codetos(byte[] code, int addr, int bytes) {
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
    
  void push(List<M> list) {
    memory[sp].type = TARR;
    memory[sp].ref = list;
    memory[sp--].str = null;
  }
    
  void pushRef(M x) {
    memory[sp].type = TREF;
    memory[sp].str = null;
    memory[sp--].ref = x;
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
    push(peekStack(codetos(code, pc++, 1)));
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
    int addr = codetoi(code, pc, 3);
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
    int addr = codetoi(code, pc, 3);
    pc += 3;
    push(peek(addr));
  }
  
  void stor_fp() {
    int rel = codetos(code, pc++, 1);
    poke(fp - rel, pop());
  }
  
  void load_fp() {
    int rel = codetos(code, pc++, 1);
    push(peek(fp - rel));
  }
  
  void sp_incr() {
    int m = codetoi(code, pc++, 1) + 1;
    for (int i = 0; i < m; i++) {
      poke(sp - i, nilM);
    }
    sp -= m;
  }
  
  void sp_decr() {
    int m = codetoi(code, pc++, 1) + 1;
    sp += m;
  }
  
  void set_cre() {
    int elements = pop().asInt();
    if (elements == -1) {
      // create arg array
      MemoryList list = new MemoryList(memory, 
          fp+FRAME_SIZE+1, fp+FRAME_SIZE+1+memory[fp+FRAME_3_ARGC].asInt());
      push(list);
    } else {
      List<M> list = new ArrayList<M>();
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
    int addr = codetoi(code, pc, 3);
    pc += 3;
    int elements = pop().asInt();
    List<M> list = new ArrayList<M>();
    for (int i = 0; i < elements; i++) {
      M m = new M();
      m.copy(peek(addr++));
      list.add(m);
    }
    push(list);
  }
  
  @SuppressWarnings("unchecked")
  void set_drf() {
    M mix = pop();
    M mset = pop();
    if (mset.type == TARR) {
      if (mix.type == TARR) {
        derefListArgList((List<M>)mset.ref, (List<M>)mix.ref);
      } else if (mix.type == TRANGE) {
        derefListArgRange((List<M>)mset.ref, (Range)mix.ref);
      } else {
        push(((List<M>)mset.ref).get(mix.asInt()));
      }
    } else if (mset.type == TSTR) {
      if (mix.type == TARR) {
        derefStringArgList(mset.str, (List<M>)mix.ref);
      } else if (mix.type == TRANGE) {
        derefStringArgRange(mset.str, (Range)mix.ref);
      } else {
        push(mset.str.charAt(mix.asInt()));
      }
    } else if (mset.type == TMAP) {
      if (mix.type == TARR) {
        derefMapArgList((Map<Object,M>)mset.ref, (List<M>)mix.ref);
      } else if (mix.type == TRANGE) {
        derefMapArgRange((Map<Object,M>)mset.ref, (Range)mix.ref);
      } else {
        push(((Map<Object,M>)mset.ref).get(mix.getRaw()));
      }
    } else if (mset.type == TRANGE) {
      if (mix.type == TARR) {
        derefRangeArgList((Range)mset.ref, (List<M>)mix.ref);
      } else if (mix.type == TRANGE) {
        derefRangeArgRange((Range)mset.ref, (Range)mix.ref);
      }else {
        if (((Range)mset.ref).type == TINT) {
          push((int)((Range)mset.ref).get(mix.asInt()));
        }
        else if (((Range)mset.ref).type == TFLOAT) {
          push((float)((Range)mset.ref).get(mix.asInt()));
        }
      }
    } else {
      throw new ProcessorError("cannot dereference type " + TNAME[mset.type]);
    }
  }
  
  void derefListArgList(List<M> set, List<M> drf) {
    List<M> list = new ArrayList<M>();
    for (M mdrf : drf) {
      if (mdrf.type == TFLOAT || mdrf.type == TINT) {
        M m = new M();
        m.copy(set.get(mdrf.asInt()));
        list.add(m);
      }
    }
    push(list);
  }
  void derefListArgRange(List<M> set, Range drf) {
    List<M> list = new ArrayList<M>();
    int len = drf.size();
    for (int i = 0; i < len; i++) {
      float d = drf.get(i);
      M m = new M();
      m.copy(set.get((int)d));
      list.add(m);
    }
    push(list);
  }
  
  void derefStringArgList(String str, List<M> drf) {
    StringBuilder sb = new StringBuilder();
    for (M mdrf : drf) {
      if (mdrf.type == TFLOAT || mdrf.type == TINT) {
        sb.append(str.charAt(mdrf.asInt()));
      }
    }
    push(sb.toString());
  }
  void derefStringArgRange(String str, Range drf) {
    StringBuilder sb = new StringBuilder();
    int len = drf.size();
    for (int i = 0; i < len; i++) {
      float d = drf.get(i);
      sb.append(str.charAt((int)d));
    }
    push(sb.toString());
  }
  
  void derefMapArgList(Map<Object,M> set, List<M> drf) {
    List<M> list = new ArrayList<M>();
    for (M mdrf : drf) {
      if (mdrf.type == TFLOAT || mdrf.type == TINT) {
        M m = new M();
        m.copy(set.get(mdrf.getRaw()));
        list.add(m);
      }
    }
    push(list);
  }
  void derefMapArgRange(Map<Object,M> set, Range drf) {
    List<M> list = new ArrayList<M>();
    Object keys[] = set.keySet().toArray();
    int len = drf.size();
    for (int i = 0; i < len; i++) {
      float d = drf.get(i);
      Object key = keys[(int)d];
      M mval = set.get(key);
      if (mval == null) continue;
      M mkey = new M(key);
      Map<Object,M> res = new HashMap<Object,M>();
      res.put("key", mkey);
      res.put("val", mval);
      M mres = new M();
      mres.type = TMAP;
      mres.ref = res;
      list.add(mres);
    }
    push(list);
  }  
  
  void derefRangeArgList(Range set, List<M> drf) {
    List<M> list = new ArrayList<M>();
    if (set.type == TINT) {
      for (M mdrf : drf) {
        if (mdrf.type == TFLOAT || mdrf.type == TINT) {
          list.add(new M((int)set.get(mdrf.asInt())));
        }
      }
    }
    else if (set.type == TFLOAT) {
      for (M mdrf : drf) {
        if (mdrf.type == TFLOAT || mdrf.type == TINT) {
          list.add(new M((float)set.get(mdrf.asInt())));
        }
      }
    }
    push(list);
  }
  void derefRangeArgRange(Range set, Range drf) {
    List<M> list = new ArrayList<M>();
    int len = drf.size();
    if (set.type == TINT) {
      for (int i = 0; i < len; i++) {
        float d = drf.get(i);
        list.add(new M((int)set.get((int)d)));
      }
    }
    else if (set.type == TFLOAT) {
      for (int i = 0; i < len; i++) {
        float d = drf.get(i);
        list.add(new M((float)set.get((int)d)));
      }
    }
    push(list);
  }
  
  @SuppressWarnings("unchecked")
  void set_wr() {
    // TODO handle set list ie arr[[1,2,3]] = 3
    // TODO handle set range ie arr[0#2] = 3
    M mix = pop();
    M mval = pop();
    M mset = pop();
    if (mset.type == TARR) {
      if (mval.type == TNIL) {
        ((List<M>)mset.ref).remove(mix.asInt());
      } else {
        M m = new M();
        m.copy(mval);
        ((List<M>)mset.ref).set(mix.asInt(), m);
      }
    } else if (mset.type == TMAP) {
      if (mval.type == TNIL) {
        ((Map<Object, M>)mset.ref).remove(mix.getRaw());
      } else {
        M m = new M();
        m.copy(mval);
        ((Map<Object, M>)mset.ref).put(mix.getRaw(), m);
      }
    } else if (mset.type == TSTR) {
      if (mval.type == TNIL) {
        int ix = mix.asInt();
        int len = mset.str.length();
        mset.str = 
            (ix > 0 ? mset.str.substring(0, ix-1) : "") +  
            (ix+1 < len ? mset.str.substring(ix+1) : "");
      } else {
        int ix = mix.asInt();
        int len = mset.str.length();
        mset.str = 
            (ix > 0 ? mset.str.substring(0, ix-1) : "") +  
            mval.asString() + 
            (ix+1 < len ? mset.str.substring(ix+1) : "");
      }
    } else {
      throw new ProcessorError("cannot write entries in type " + TNAME[mset.type]);
    }
  }
  
  @SuppressWarnings("unchecked")
  void map_add() {
    M mkey = pop();
    M mval = pop();
    M mmap = peek(sp+1);
    if (mmap.type == TARR) {
      if (!((List<M>)mmap.ref).isEmpty()) {
        throw new ProcessorError("cannot add tuples to a list");
      }
      mmap.ref = new HashMap<Object, M>();
      mmap.type = TMAP;
    }
    if (mmap.type == TMAP) {
      M mk = new M();
      mk.copy(mkey);
      M mv = new M();
      mv.copy(mval);
      ((HashMap<Object,M>)mmap.ref).put(mk.getRaw(), mv);
    } else {
      throw new ProcessorError("cannot add tuples to type " + TNAME[mmap.type]);
    }
  }
  
  @SuppressWarnings("unchecked")
  void set_sz() {
    M e = pop();
    if (e.type == TARR) {
      push(((List<M>)e.ref).size());
    } else if (e.type == TMAP) {
      push(((Map<Object,M>)e.ref).size());
    } else if (e.type == TSTR) {
      push(e.str.length());
    } else if (e.type == TRANGE) {
      push(((Range)e.ref).size());
    } else {
      push(0);
    }
  }
  
  @SuppressWarnings("unchecked")
  void set_rd() {
    int ix = pop().asInt();
    M mset = pop();
    if (mset.type == TARR) {
      push(((List<M>)mset.ref).get(ix));
    } else if (mset.type == TMAP) {
      Map<Object,M> map = (Map<Object,M>)mset.ref;
      Object key = map.keySet().toArray()[ix];
      M mval = map.get(key);
      if (mval == null) mval = nilM;
      M mkey = new M(key);
      Map<Object,M> res = new HashMap<Object,M>();
      res.put("key", mkey);
      res.put("val", mval);
      M mres = new M();
      mres.type = TMAP;
      mres.ref = res;
      push(mres);
    } else if (mset.type == TSTR) {
      push(mset.str.charAt(ix));
    } else if (mset.type == TRANGE) {
      if (((Range)mset.ref).type == TINT) {
        push((int)((Range)mset.ref).get(ix));
      }
      else if (((Range)mset.ref).type == TFLOAT) {
        push((float)((Range)mset.ref).get(ix));
      } else {
        throw new ProcessorError("fatal: range of bad type " + TNAME[((Range)mset.ref).type]);
      }
     } else {
      throw new ProcessorError("cannot get sub element of type " + TNAME[mset.type]);
    }
  }
  
  void rng(int def) {
    Range range;
    if (def == 2) {
      M mto = pop();
      M mfrom = pop();
      range = new Range(mfrom, mto);
    } else {
      M mto = pop();
      M mstep = pop();
      M mfrom = pop();
      range = new Range(mfrom, mstep, mto);
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
    
  @SuppressWarnings("unchecked")
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
    else if (e1.type == TARR) {
      M m = new M();
      m.copy(e2);
      ((List<M>)e1.ref).add(m);
      push(e1);
    }
    else if (e2.type == TARR) {
      M m = new M();
      m.copy(e1);
      ((List<M>)e2.ref).add(0, m);
      push(e2);
    }
    else if (e1.type == TSTR || e2.type == TSTR) {
      String r = e1.asString() + e2.asString();
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
      if (e1.type == TINT || e1.type == TCODE) {
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
        String r = e1.str;
        int pos = r.lastIndexOf(e2.str);
        if (pos >= 0) {
          r = r.substring(0, pos) + r.substring(pos + e2.str.length());
        }
        status(r);
        if (push) push(r);
      } else {
        throw new ProcessorError("cannot subtract type " + TNAME[e1.type]);
      }
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
    push(codetoi(code, pc++, 1) + 1);
    add();
  }

  void sub_im() {
    push(codetoi(code, pc++, 1) + 1);
    sub();
  }
  
  void push_s() {
    push(codetos(code, pc++, 1));
  }

  void push_u() {
    push(codetoi(code, pc++, 1)+128);
  }

  void push_im_int(int i) {
    push(i);
  }

  void push_nil() {
    push(nilM);
  }

  void def_me() {
    me_banked = peekStack(0).i;
  }

  void push_me() {
    if (me == -1) {
      push(nilM);
    } else {
      push(memory[me]);
    }
  }

  void udef_me() {
    me_banked = -1;
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
      case TCODE:
      case TINT: break;
      case TFLOAT: m.i = (int)m.f; break;
      case TNIL: m.i = 0; break;
      case TSTR: m.i = m.str.length() == 1 ? m.str.charAt(0) : Integer.parseInt(m.str); break;
      case TRANGE: break;
      case TARR: break;
      case TMAP: break;
      case TREF: break;
      }
      break;
    case TFLOAT:
      switch (m.type) {
      case TFLOAT: break;
      case TCODE:
      case TINT: m.f = m.i; break;
      case TNIL: m.f = 0; break;
      case TSTR: m.f = Float.parseFloat(m.str); break;
      case TRANGE: break;
      case TARR: break;
      case TMAP: break;
      case TREF: break;
      }
      break;
    case TO_CHAR:
      switch (m.type) {
      case TFLOAT: m.str = "" + (char)((int)m.f); break;
      case TCODE: break;
      case TINT: m.str = "" + (char)(m.i); break;
      case TNIL: break;
      case TSTR: break;
      case TRANGE: break;
      case TARR: break;
      case TMAP: break;
      case TREF: break;
      }
      break;
    case TSTR:
      m.str = m.asString();
      break;
    }
    m.type = (byte)(type == TO_CHAR ? TSTR : type);
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
    int a = addr.i;
    if (addr.type != TINT && addr.type != TCODE) {
      throw new ProcessorError("calling bad type " + TNAME[addr.type]);
    }
    push(me);
    push(pc);
    push(fp);
    int args = peek(sp+FRAME_3_ARGC).i;
    fp = sp;
    pc = a;
    me = me_banked;
    if ((pc & 0xff0000) == 0xff0000) {
      ExtCall ec = extLinks.get(pc);
      if (ec == null) throw new ProcessorError(String.format("bad external call 0x%06x", pc));
      M ret = ec.exe(this, getArgs(fp, args)); 
      push(ret == null ? nilM : ret);
      retv();
    }
  }
  
  void call_im() {
    push(me);
    push(pc+3);
    push(fp);
    int args = peek(sp+FRAME_3_ARGC).i;
    fp = sp;
    pc = codetos(code, pc, 3);
    me = me_banked;
    if ((pc & 0xff0000) == 0xff0000) {
      ExtCall ec = extLinks.get(pc);
      if (ec == null) throw new ProcessorError(String.format("bad external call 0x%06x", pc));
      M ret = ec.exe(this, getArgs(fp, args)); 
      push(ret == null ? nilM : ret);
      retv();
    }
  }
  
  void ret() {
    sp = fp;
    if (fp < 0 || fp >= memory.length-1) throw new ProcessorError.ProcessorFinishedError("abnormal exit");
    fp = pop().i;
    pc = pop().i;
    me = pop().i;
    if (pc < 0 || pc >= memory.length-1) throw new ProcessorError.ProcessorFinishedError("normal exit");
    int argc = pop().i;
    sp += argc;
  }
  
  void retv() {
    M t = pop();
    sp = fp;
    if (fp < 0 || fp >= memory.length-1) throw new ProcessorError.ProcessorFinishedError("abnormal exit");
    fp = pop().i;
    pc = pop().i;
    me = pop().i;
    if (pc < 0 || pc >= memory.length-1) throw new ProcessorError.ProcessorFinishedError("normal exit");
    int argc = pop().i;
    sp += argc;
    push(t);
  }
  
  void jump(int icond) {
    int dst = codetoi(code, pc, 3);
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
    int rel = codetos(code, pc, 3) - 1 - 3;
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
    String disasm = disasm(code, pc);
    String dbgComment = exe.getDebugInfo(pc);
    out.println(String.format("%s      %-32s  %s", procInfo, disasm, (dbgComment == null ? "" : ("; " + dbgComment))));
    stepProc();
    String stack = getStack();
    out.println(stack);
  }

  void stepProc() {
    oldpc = pc;
    int instr = (int)(code[pc++] & 0xff);
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
    case IADQ3:
      add_q(3);
      break;
    case IADQ4:
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
    case IRET: 
      ret();
      break;
    case IRETV: 
      retv();
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
    public M(M x) { type = TREF; ref = x; }
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
      else if (o instanceof Range) {
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
      case TCODE:
        return String.format("->0x%08x", i);
      case TARR:
      {
        StringBuilder sb = new StringBuilder("[");
        @SuppressWarnings("unchecked")
        List<M> l = (List<M>)ref;
        final int sz = l.size();
        if (sz > 6) {
          for (int i = 0; i < 3; i++) {
            sb.append(l.get(i).asString() + ", ");
          }
          sb.append("(" + (sz - 3) + " more entries)");
        } else {
          for (int i = 0; i < sz; i++) {
            sb.append(l.get(i).asString());
            if (i < sz-1) sb.append(", ");
          }
        }
        sb.append("]");
        return sb.toString();
      }
      case TMAP:
        StringBuilder sb = new StringBuilder("[");
        @SuppressWarnings("unchecked")
        Map<Object, M> m = (Map<Object, M>)ref;
        final int sz = m.size();
        Object keys[] = m.keySet().toArray();
        if (sz > 6) {
          for (int i = 0; i < 3; i++) {
            sb.append(keys[i] + ":" + m.get(keys[i]).asString() + ", ");
          }
          sb.append("(" + (sz - 3) + " more entries)");
        } else {
          for (int i = 0; i < sz; i++) {
            sb.append(keys[i] + ":" + m.get(keys[i]).asString());
            if (i < sz-1) sb.append(", ");
          }
        }
        sb.append("]");
        return sb.toString();
      case TRANGE:
        return ((Range)ref).toString();
      case TREF:
        return ref instanceof M ? ((M)ref).asString() : ref.toString();
      default:
        return "?";
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
      case TCODE:
        return "c" + asString();
      case TARR:
        return "l" + asString();
      case TMAP:
        return "m" + asString();
      case TREF:
        return "ref" + asString();
      default:
        return "?";
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
      case TCODE:
        return this;
      case TARR:
        return ref;
      case TMAP:
        return ref;
      case TREF:
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
    return String.format("pc:0x%08x  sp:0x%06x  fp:0x%06x  me:0x%06x(0x%06x) sr:", pc, sp, fp, me & 0xffffff, me_banked & 0xffffff) + 
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

}