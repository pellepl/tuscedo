package com.pelleplutt.plang.proc;

import java.io.PrintStream;
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
  public static final int TLIST = 6;
  public static final int TMAP = 7;
  public static final int TREF = 8;
  
  public static boolean dbgMem = false;
  public static boolean dbgRun = false;
  
  static final String TSTRING[] = {
    "nil", "int", "float", "string", "range", "code", "list", "map", "reference"
  };
  
  M[] memory;
  byte[] code;
  int sp;
  int pc;
  int fp;
  boolean zero;
  boolean minus;
  Map<Integer, ExtCall> extLinks;
  Executable exe;
  M nilM = new M();
  
  public Processor(int memorySize, Executable exe) {
    this.exe = exe;
    sp = memorySize - 1;
    pc = 0;
    fp = sp;
    nilM.type = TNIL;
    memory = new M[memorySize];
    for (int i = 0; i < memorySize; i++) memory[i] = new M();
    this.code = exe.getMachineCode();
    this.extLinks = exe.getExternalLinkMap();
    Map<Integer, M> consts = exe.getConstants();
    for (int addr : consts.keySet()) {
      M m = consts.get(addr);
      poke(addr, m);
    }
  }
  
  public static void disasm(PrintStream out, byte[] code, int pc, int len) {
    while (len > 0) {
      out.println(String.format("0x%06x %s", pc, disasm(code, pc)));
      int instr = (int)(code[pc] & 0xff);
      int step = ISIZE[instr];
      if (step <= 0) step = 1;
      pc += step;
      len -= step;
    }
  }

  public static String disasm(byte[] code, int pc) {
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
    case ISHL:
      sb.append("shiftl  ");
      break;
    case ISHR:
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
    case ICMN:
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
    
    case IADI:
      sb.append("add_im  ");
      sb.append(String.format("0x%02x", codetoi(code, pc, 1) + 1));
      break;
    case ISUI:
      sb.append("sub_im  ");
      sb.append(String.format("0x%02x", codetoi(code, pc, 1) + 1));
      break;
    case IPUI:
      sb.append("push_im ");
      sb.append(String.format("0x%02x", codetoi(code, pc, 1)));
      break;
    case IPU0:
      sb.append("push_nil");
      break;
    case IPUC:
      sb.append("push_c  ");
      sb.append(String.format("0x%06x", codetoi(code, pc, 3)));
      break;

    case IADQ1:
      sb.append("add_q1  ");
      break;
    case IADQ2:
      sb.append("add_q2  ");
      break;
    case IADQ3:
      sb.append("add_q3  ");
      break;
    case IADQ4:
      sb.append("add_q4  ");
      break;
    case IADQ5:
      sb.append("add_q5  ");
      break;
    case IADQ6:
      sb.append("add_q6  ");
      break;
    case IADQ7:
      sb.append("add_q7  ");
      break;
    case IADQ8:
      sb.append("add_q8  ");
      break;
    case ISUQ1:
      sb.append("sub_q1  ");
      break;
    case ISUQ2:
      sb.append("sub_q2  ");
      break;
    case ISUQ3:
      sb.append("sub_q3  ");
      break;
    case ISUQ4:
      sb.append("sub_q4  ");
      break;
    case ISUQ5:
      sb.append("sub_q5  ");
      break;
    case ISUQ6:
      sb.append("sub_q6  ");
      break;
    case ISUQ7:
      sb.append("sub_q7  ");
      break;
    case ISUQ8:
      sb.append("sub_q8  ");
      break;
      
    case ITOI:
      sb.append("cast_I  ");
      break;
    case ITOF:
      sb.append("cast_F  ");
      break;
    case ITOS:
      sb.append("cast_S  ");
      break;
      
    case IPOP:
      sb.append("pop     ");
      break;
    case IDUP:
      sb.append("dup     ");
      break;
    case ISWP:
      sb.append("swap    ");
      break;
    case ICPY:
      sb.append("cpy     ");
      sb.append(String.format("$sp[%4d]", codetos(code, pc, 1)));
      break;
    case ISTR:
      sb.append("stor    ");
      break;
    case ISTI:
      sb.append("stor_im ");
      sb.append(String.format("mem[0x%06x]", codetoi(code, pc, 3)));
      break;
    case ILD :
      sb.append("load    ");
      break;
    case ILDI:
      sb.append("load_im ");
      sb.append(String.format("mem[0x%06x]", codetoi(code, pc, 3)));
      break;
    case ISTF:
      sb.append("stor_fp ");
      sb.append(String.format("$fp[%4d]", codetos(code, pc, 1)));
      break;
    case ILDF:
      sb.append("load_fp ");
      sb.append(String.format("$fp[%4d]", codetos(code, pc, 1)));
      break;

    case ISPI:
      sb.append("sp_incr ");
      sb.append(String.format("%d", codetoi(code, pc, 1) + 1));
      break;
    case ISPD:
      sb.append("sp_decr ");
      sb.append(String.format("%d", codetoi(code, pc, 1) + 1));
      break;

    case IIXRD:
      sb.append("arr_rd  ");
      break;
    case IIXWR :
      sb.append("arr_wr  ");
      break;
    case IIXADD:
      sb.append("arr_add ");
      break;
    case IIXDEL:
      sb.append("arr_del ");
      break;
    case IIXINS:
      sb.append("arr_ins ");
      break;
    case IIXSZ:
      sb.append("arr_sz  ");
      break;

    case ICAL: 
      sb.append("call    ");
      break;
    case ICALI: 
      sb.append("call_im ");
      sb.append(String.format("0x%06x", codetoi(code, pc, 3)));
      break;
    case IRET: 
      sb.append("return  ");
      break;
    case IRETV: 
      sb.append("returnv ");
      break;
    case IJMP: 
      sb.append("jump    ");
      sb.append(String.format("%d", codetoi(code, pc, 3)));
      break;
    case IJMPEQ: 
      sb.append("jump_eq ");
      sb.append(String.format("%d", codetoi(code, pc, 3)));
      break;
    case IJMPNE: 
      sb.append("jump_ne ");
      sb.append(String.format("%d", codetoi(code, pc, 3)));
      break;
    case IJMPGT: 
      sb.append("jump_gt ");
      sb.append(String.format("%d", codetoi(code, pc, 3)));
      break;
    case IJMPGE: 
      sb.append("jump_ge ");
      sb.append(String.format("%d", codetoi(code, pc, 3)));
      break;
    case IJMPLT: 
      sb.append("jump_lt ");
      sb.append(String.format("%d", codetoi(code, pc, 3)));
      break;
    case IJMPLE: 
      sb.append("jump_le ");
      sb.append(String.format("%d", codetoi(code, pc, 3)));
      break;
    case IBRA: 
      sb.append("bra     ");
      sb.append(String.format("%d (0x%06x)", codetos(code, pc, 3), pc + codetos(code, pc, 3)-1));
      break;
    case IBRAEQ: 
      sb.append("bra_eq  ");
      sb.append(String.format("%d (0x%06x)", codetos(code, pc, 3), pc + codetos(code, pc, 3)-1));
      break;
    case IBRANE: 
      sb.append("bra_ne  ");
      sb.append(String.format("%d (0x%06x)", codetos(code, pc, 3), pc + codetos(code, pc, 3)-1));
      break;
    case IBRAGT: 
      sb.append("bra_gt  ");
      sb.append(String.format("%d (0x%06x)", codetos(code, pc, 3), pc + codetos(code, pc, 3)-1));
      break;
    case IBRAGE: 
      sb.append("bra_ge  ");
      sb.append(String.format("%d (0x%06x)", codetos(code, pc, 3), pc + codetos(code, pc, 3)-1));
      break;
    case IBRALT: 
      sb.append("bra_lt  ");
      sb.append(String.format("%d (0x%06x)", codetos(code, pc, 3), pc + codetos(code, pc, 3)-1));
      break;
    case IBRALE: 
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
    
  void push(String x) {
    memory[sp].type = TSTR;
    memory[sp].ref = null;
    memory[sp--].str = x;
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
  void swp() {
    M m1 = pop();
    M m2 = tmpM.copy(pop());
    push(m1);
    push(m2);
  }
  
  void cpy() {
    push(peekStack(codetos(code, pc++, 1)));
  }
  
  void str() {
    M m = pop();
    M addr = pop();
    if (addr.type == TINT) {
      poke(addr.i, m);
    } else {
      throw new Error("cannot access non int adress");
    }
  }
  
  void sti() {
    M m = pop();
    int addr = codetoi(code, pc, 3);
    pc += 3;
    poke(addr, m);
  }
  
  void ld() {
    M addr = pop();
    if (addr.type == TINT) {
      push(peek(addr.i));
    } else {
      throw new Error("cannot access non int adress");
    }
  }
  
  void ldi() {
    int addr = codetoi(code, pc, 3);
    pc += 3;
    push(peek(addr));
  }
  
  void stf() {
    int rel = codetos(code, pc++, 1);
    poke(fp - rel, pop());
  }
  
  void ldf() {
    int rel = codetos(code, pc++, 1);
    push(peek(fp - rel));
  }
  
  void spi() {
    int m = codetoi(code, pc++, 1) + 1;
    sp -= m;
  }
  
  void spd() {
    int m = codetoi(code, pc++, 1) + 1;
    sp += m;
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
      } else {
        throw new ProcessorError("cannot add type " + TSTRING[e1.type]);
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
    else if (e1.type == TSTR || e2.type == TSTR) {
      String r = e1.asString() + e2.asString();
      status(r);
      push(r);
    } else {
      throw new ProcessorError("cannot add types " + TSTRING[e1.type] + " and " + TSTRING[e2.type]);
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
      if (e1.type == TINT) {
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
          r = r.substring(0, pos);
        }
        status(r);
        if (push) push(r);
      } else {
        throw new ProcessorError("cannot subtract type " + TSTRING[e1.type]);
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
    } else {
      throw new ProcessorError("cannot subtract types " + TSTRING[e1.type] + " and " + TSTRING[e2.type]);
    }
  }
  
  void cmp() {
    sub(false);
  }
  
  void cmn() {
    sub(false, false);
  }
  
  void not() {
    M e1 = pop();
    if (e1.type == TINT) {
      push(~e1.i);
    } else {
      throw new ProcessorError("cannot NOT type " + TSTRING[e1.type]);
    }
  }
  
  void neg() {
    M e1 = pop();
    if (e1.type == TINT) {
      push(-e1.i);
    } else if (e1.type == TFLOAT) {
      push(-e1.f);
    } else {
      throw new ProcessorError("cannot negate type " + TSTRING[e1.type]);
    }
  }
  
  void lnot() {
    M e1 = pop();
    if (e1.type == TINT) {
      push(e1.i == 0 ? 1 : 0);
    } else {
      throw new ProcessorError("cannot logical not type " + TSTRING[e1.type]);
    }
  }
  
  void adi() {
    push(codetoi(code, pc++, 1) + 1);
    add();
  }

  void sui() {
    push(codetoi(code, pc++, 1) + 1);
    sub();
  }
  
  void pui() {
    push(codetoi(code, pc++, 1));
  }

  void pu0() {
    push(nilM);
  }

  void puc() {
    push(codetoi(code, pc, 3));
    pc += 3;
  }

  void adq(int x) {
    push(x);
    add();
  }

  void suq(int x) {
    push(x);
    sub();
  }
  
  void to(int type) {
    M m = peekStack(0);
    switch (type) {
    case TINT:
      switch (m.type) {
      case TCODE:
      case TINT: break;
      case TFLOAT: m.i = (int)m.f; break;
      case TNIL: m.i = 0; break;
      case TSTR: m.i = Integer.parseInt(m.str); break;
      case TLIST: break;
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
      case TLIST: break;
      case TMAP: break;
      case TREF: break;
      }
      break;
    case TSTR:
      m.str = m.asString();
      break;
    }
    m.type = (byte)type;
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
        throw new ProcessorError("cannot multiply type " + TSTRING[e1.type]);
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
      throw new ProcessorError("cannot multiply types " + TSTRING[e1.type] + " and " + TSTRING[e2.type]);
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
        throw new ProcessorError("cannot divide type " + TSTRING[e1.type]);
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
      throw new ProcessorError("cannot divide types " + TSTRING[e1.type] + " and " + TSTRING[e2.type]);
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
        throw new ProcessorError("cannot modulo type " + TSTRING[e1.type]);
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
      throw new ProcessorError("cannot modulo types " + TSTRING[e1.type] + " and " + TSTRING[e2.type]);
    }
  }
  
  void shl() {
    M e2 = pop();
    M e1 = pop();
    if (e1.type == e2.type && e1.type == TINT) {
      int r = e1.i << e2.i;
      status(r);
      push(r);
    } else {
      throw new ProcessorError("cannot shift types " + TSTRING[e1.type] + " and " + TSTRING[e2.type]);
    }
  }
  
  void shr() {
    M e2 = pop();
    M e1 = pop();
    if (e1.type == e2.type && e1.type == TINT) {
      int r = e1.i >> e2.i;
      status(r);
      push(r);
    } else {
      throw new ProcessorError("cannot shift types " + TSTRING[e1.type] + " and " + TSTRING[e2.type]);
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
      throw new ProcessorError("cannot and types " + TSTRING[e1.type] + " and " + TSTRING[e2.type]);
    }
  }
  
  void or() {
    M e2 = pop();
    M e1 = pop();
    if (e1.type == e2.type && e1.type == TINT) {
      int r = e1.i & e2.i;
      status(r);
      push(r);
    } else {
      throw new ProcessorError("cannot or types " + TSTRING[e1.type] + " and " + TSTRING[e2.type]);
    }
  }
  
  void xor() {
    M e2 = pop();
    M e1 = pop();
    if (e1.type == e2.type && e1.type == TINT) {
      int r = e1.i & e2.i;
      status(r);
      push(r);
    } else {
      throw new ProcessorError("cannot xor types " + TSTRING[e1.type] + " and " + TSTRING[e2.type]);
    }
  }
  
  void cal() {
    M addr = pop();
    int a = addr.i;
    if (addr.type != TINT && addr.type != TCODE) {
      throw new ProcessorError("calling bad type " + TSTRING[addr.type]);
    }
    push(pc);
    push(fp);
    fp = sp;
    pc = a;
    if ((pc & 0xff0000) == 0xff0000) {
      ExtCall ec = extLinks.get(pc);
      if (ec == null) throw new ProcessorError(String.format("bad external call 0x%06x", pc));
      M ret = ec.exe(memory, sp, fp); 
      push(ret == null ? nilM : ret);
      retv();
    }
  }
  
  void cali() {
    push(pc+3);
    push(fp);
    fp = sp;
    pc = codetos(code, pc, 3);
    if ((pc & 0xff0000) == 0xff0000) {
      ExtCall ec = extLinks.get(pc);
      if (ec == null) throw new ProcessorError(String.format("bad external call 0x%06x", pc));
      M ret = ec.exe(memory, sp, fp); 
      push(ret == null ? nilM : ret);
      retv();
    }
  }
  
  void ret() {
    if (fp >= memory.length-1) throw new ProcessorError.ProcessorFinishedError();
    sp = fp;
    fp = pop().i;
    pc = pop().i;
  }
  
  void retv() {
    if (fp >= memory.length-1) throw new ProcessorError.ProcessorFinishedError();
    M t = pop();
    sp = fp;
    fp = pop().i;
    pc = pop().i;
    push(t);
  }
  
  void jmp(int icond) {
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
    if (dbgRun) stepDebug(System.out);
    else        stepProc();
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
    case ISHL:
      shl();
      break;
    case ISHR:
      shr();
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
    case ICMP:
      cmp();
      break;
    case ICMN:
      cmn();
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
      
    case IADI:
      adi();
      break;
    case ISUI:
      sui();
      break;
    case IPUI:
      pui();
      break;
    case IPU0:
      pu0();
      break;
    case IPUC:
      puc();
      break;
    
    case IADQ1:
      adq(1);
      break;
    case IADQ2:
      adq(2);
      break;
    case IADQ3:
      adq(3);
      break;
    case IADQ4:
      adq(4);
      break;
    case IADQ5:
      adq(5);
      break;
    case IADQ6:
      adq(6);
      break;
    case IADQ7:
      adq(7);
      break;
    case IADQ8:
      adq(8);
      break;
    case ISUQ1:
      suq(1);
      break;
    case ISUQ2:
      suq(2);
      break;
    case ISUQ3:
      suq(3);
      break;
    case ISUQ4:
      suq(4);
      break;
    case ISUQ5:
      suq(5);
      break;
    case ISUQ6:
      suq(6);
      break;
    case ISUQ7:
      suq(7);
      break;
    case ISUQ8:
      suq(8);
      break;

    case ITOI:
      to(TINT);
      break;
    case ITOF:
      to(TFLOAT);
      break;
    case ITOS:
      to(TSTR);
      break;

    case IPOP:
      pop();
      break;
    case IDUP:
      dup();
      break;
    case ISWP:
      swp();
      break;
    case ICPY:
      cpy();
      break;
    case ISTR:
      str();
      break;
    case ISTI:
      sti();
      break;
    case ILD :
      ld();
      break;
    case ILDI:
      ldi();
      break;
    case ISTF:
      stf();
      break;
    case ILDF:
      ldf();
      break;
      
    case ISPI:
      spi();
      break;
    case ISPD:
      spd();
      break;

    case IIXRD:
      break;
    case IIXWR:
      break;
    case IIXADD:
      break;
    case IIXDEL:
      break;
    case IIXINS:
      break;
    case IIXSZ:
      break;

    case ICAL: 
      cal();
      break;
    case ICALI: 
      cali();
      break;
    case IRET: 
      ret();
      break;
    case IRETV: 
      retv();
      break;
    case IJMP: 
      jmp(ICOND_AL);
      break;
    case IJMPEQ: 
      jmp(ICOND_EQ);
      break;
    case IJMPNE: 
      jmp(ICOND_NE);
      break;
    case IJMPGE: 
      jmp(ICOND_GE);
      break;
    case IJMPGT: 
      jmp(ICOND_GT);
      break;
    case IJMPLE: 
      jmp(ICOND_LE);
      break;
    case IJMPLT: 
      jmp(ICOND_LT);
      break;
    case IBRA: 
      bra(ICOND_AL);
      break;
    case IBRAEQ: 
      bra(ICOND_EQ);
      break;
    case IBRANE: 
      bra(ICOND_NE);
      break;
    case IBRAGE: 
      bra(ICOND_GE);
      break;
    case IBRAGT: 
      bra(ICOND_GT);
      break;
    case IBRALE: 
      bra(ICOND_LE);
      break;
    case IBRALT: 
      bra(ICOND_LT);
      break;
    case IBKPT: 
      throw new ProcessorBreakpointError();
    default:
      throw new Error(String.format("unknown instruction 0x%02x", instr));
    }
  }
  
  public static class M {
    public byte type;
    public M ref;
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
    public String asString() {
      switch(type) {
      case TNIL:
        return "nil";
      case TINT:
        return ""+ i;
      case TFLOAT:
        return ""+ f;
      case TRANGE:
        return "";
      case TSTR:
        return str;
      case TCODE:
        return String.format("->0x%08x", i);
      case TLIST:
      case TMAP:
        return "TODO"; // TODO
      case TREF:
        return ref.asString();
      default:
        return "?";
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
      case TLIST:
        return "l" + asString();
      case TMAP:
        return "m" + asString();
      case TREF:
        return "ref" + asString();
      default:
        return "?";
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
  
  String getProcInfo() {
    return String.format("pc:0x%08x  sp:0x%06x  fp:%06x  sr:", pc, sp, fp) + 
        (zero ? "Z" : "z") + (minus ? "M" : "m");
  }
}