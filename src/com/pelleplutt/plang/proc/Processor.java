package com.pelleplutt.plang.proc;

import java.io.PrintStream;

public class Processor implements ByteCode {
  static final int TINT = 0;
  static final int TFLOAT = 1;
  static final int TSTR = 2;
  static final int TRANGE = 3;
  static final int TCODE = 4;
  static final int TREF = 5;
  
  M[] memory;
  byte[] code;
  int sp;
  int pc;
  int fp;
  boolean zero;
  boolean minus;
  
  public Processor(int memorySize, byte[] code) {
    sp = memorySize - 1;
    pc = 0;
    fp = sp;
    memory = new M[memorySize];
    for (int i = 0; i < memorySize; i++) memory[i] = new M();
    this.code = code;
  }
  
  public void disasm(PrintStream out, int pc, int len) {
    while (len > 0) {
      out.println(String.format("0x%08x %s", pc, disasm(pc)));
      int instr = (int)(code[pc] & 0xff);
      int step = ISIZE[instr];
      if (step <= 0) step = 1;
      pc += step;
      len -= step;
    }
  }

  public String disasm(int pc) {
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
    
    case IADI:
      sb.append("add_im  ");
      sb.append(String.format("0x%02x", codetoi(pc, 1) + 1));
      break;
    case ISUI:
      sb.append("sub_im  ");
      sb.append(String.format("0x%02x", codetoi(pc, 1) + 1));
      break;
    case IPUI:
      sb.append("push_im ");
      sb.append(String.format("0x%02x", codetoi(pc, 1)));
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
      
    case IPOP:
      sb.append("pop     ");
      break;
    case IDUP:
      sb.append("dup     ");
      break;
    case IROT:
      sb.append("rot     ");
      break;
    case ICPY:
      sb.append("cpy     ");
      sb.append(String.format("sp[0x%02x]", codetoi(pc, 1) + 1));
      break;
    case ISTR:
      sb.append("stor    ");
      break;
    case ISTI:
      sb.append("stor_im ");
      sb.append(String.format("mem[0x%06x]", codetoi(pc, 3)));
      break;
    case ILD :
      sb.append("load    ");
      break;
    case ILDI:
      sb.append("load_im ");
      sb.append(String.format("mem[0x%06x]", codetoi(pc, 3)));
      break;
    case ISTF:
      sb.append("stor_fp ");
      sb.append(String.format("fp[0x%02x]", codetoi(pc, 1)));
      break;
    case ILDF:
      sb.append("load_fp ");
      sb.append(String.format("fp[0x%02x]", codetoi(pc, 1)));
      break;

    case IALL:
      sb.append("allo_sp ");
      sb.append(String.format("%d", codetoi(pc, 1) + 1));
      break;
    case IFRE:
      sb.append("free_sp ");
      sb.append(String.format("%d", codetoi(pc, 1) + 1));
      break;

    case ICAL: 
      sb.append("call    ");
      break;
    case IRET: 
      sb.append("return  ");
      break;
    case IJMP: 
      sb.append("jump    ");
      break;
    case IJMPEQ: 
      sb.append("jump_eq ");
      break;
    case IJMPNE: 
      sb.append("jump_ne ");
      break;
    case IJMPGT: 
      sb.append("jump_gt ");
      break;
    case IJMPGE: 
      sb.append("jump_ge ");
      break;
    case IJMPLT: 
      sb.append("jump_lt ");
      break;
    case IJMPLE: 
      sb.append("jump_le ");
      break;
    case IBRA: 
      sb.append("bra     ");
      sb.append(String.format("%d", codetos(pc, 3)));
      break;
    case IBRAEQ: 
      sb.append("bra_eq  ");
      break;
    case IBRANE: 
      sb.append("bra_ne  ");
      break;
    case IBRAGT: 
      sb.append("bra_gt  ");
      break;
    case IBRAGE: 
      sb.append("bra_ge  ");
      break;
    case IBRALT: 
      sb.append("bra_lt  ");
      break;
    case IBRALE: 
      sb.append("bra_le  ");
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
  }
  
  M peek(int a) {
    return memory[a];
  }
  
  int codetoi(int addr, int bytes) {
    int x = 0;
    for (int i = 0; i < bytes; i++) {
      x <<= 8;
      x |= (((int)code[addr++]) & 0xff);
    }
    return x;
  }

  int codetos(int addr, int bytes) {
    int x = codetoi(addr, bytes);
    x <<= 8*(4-bytes);
    x >>= 8*(4-bytes);
    return x;
  }
  
  void push(M m) {
    poke(sp--, m);
  }
    
  void push(int x) {
    memory[sp].type = TINT;
    memory[sp--].i = x;
  }
    
  void push(float x) {
    memory[sp].type = TFLOAT;
    memory[sp--].f = x;
  }
    
  void push(String x) {
    memory[sp].type = TSTR;
    memory[sp--].str = x;
  }
    
  void pushRef(M x) {
    memory[sp].type = TREF;
    memory[sp--].ref = x;
  }
    
  M peekStack(int r) {
    return memory[sp + r + 1];
  }
  
  void dup() {
    push(peekStack(0));
  }
  
  void rot() {
    M m1 = pop();
    M m2 = pop();
    push(m1);
    push(m2);
  }
  
  void cpy() {
    push(peekStack(codetoi(pc++, 1) + 1));
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
    int addr = codetoi(pc, 3);
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
    int addr = codetoi(pc, 3);
    pc += 3;
    push(peek(addr));
  }
  
  void stf() {
    int rel = codetoi(pc++, 1);
    poke(fp - rel, pop());
  }
  
  void ldf() {
    int rel = codetoi(pc++, 1);
    push(peek(fp - rel));
  }
  
  void all() {
    int m = codetoi(pc++, 1) + 1;
    sp -= m;
  }
  
  void fre() {
    int m = codetoi(pc++, 1) + 1;
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
  }

  void sub() {
    M e2 = pop();
    M e1 = pop();
    if (e1.type == e2.type) {
      if (e1.type == TINT) {
        int r = e1.i - e2.i;
        status(r);
        push(r);
      }
      else if (e1.type == TFLOAT) {
        float r = e1.f - e2.f;
        status(r);
        push(r);
      }
      else if (e1.type == TSTR) {
        String r = e1.str;
        int pos = r.lastIndexOf(e2.str);
        if (pos >= 0) {
          r = r.substring(0, pos);
        }
        status(r);
        push(r);
      }
    }
    else if (e1.type == TFLOAT && e2.type == TINT) {
      float r = e1.f - e2.i;
      status(r);
      push(r);
    }
    else if (e2.type == TFLOAT && e1.type == TINT) {
      float r = e1.i - e2.f;
      status(r);
      push(r);
    }
  }
  
  void adi() {
    push(codetoi(pc++, 1) + 1);
    add();
  }

  void sui() {
    push(codetoi(pc++, 1) + 1);
    sub();
  }
  
  void pui() {
    push(codetoi(pc++, 1));
  }

  void adq(int x) {
    push(x);
    add();
  }

  void suq(int x) {
    push(x);
    sub();
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
    }
  }
  
  void cal() {
    push(fp);
    fp = sp;
    // TODO
  }
  
  void ret() {
    // TODO
    sp = fp;
    fp = pop().i;
  }

  public void exec() {
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
      break;
    case ISHL:
      break;
    case ISHR:
      break;
    case IAND:
      break;
    case IOR :
      break;
    case IXOR:
      break;
    case ICMP:
      break;
    case ICMN:
      break;
    case INOT:
      break;
    case INEG:
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

    case IPOP:
      pop();
      break;
    case IDUP:
      dup();
      break;
    case IROT:
      rot();
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
    case ILDF :
      ldf();
      break;
      
    case IALL:
      all();
      break;
    case IFRE:
      fre();
      break;

    case ICAL: 
      cal();
      break;
    case IRET: 
      ret();
      break;
    case IJMP: 
      break;
    case IBRA: 
      break;
    default:
      throw new Error("unknown instruction");
    }
  }
  
  class M {
    byte type;
    M ref;
    String str;
    int i;
    float f;
    public String toString() {
      switch(type) {
      case TINT:
        return "i"+ i;
      case TFLOAT:
        return "g"+ f;
      case TRANGE:
        return "r";
      case TSTR:
        return "s\'" + str + "'";
      case TCODE:
        return "c";
      case TREF:
        return "ref";
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
  
  String getProc() {
    return "pc:" + pc + " sp:" + (memory.length - sp - 1) + " fp:" + fp + " " + 
        (zero ? "Z" : "z") + (minus ? "M" : "m");
  }

  public static void main(String[] args) {
    byte code[] = {
        (byte)IALL, 4,
        (byte)IPUI, 5,
        (byte)IPUI, 6,
        (byte)IADI, 7,
        (byte)IMUL,
        (byte)IPUI, 10,
        (byte)IMUL,
        (byte)IDUP,
        (byte)ISTI, 0,0,8,
        (byte)IFRE, 5,
        (byte)ILDI, 0,0,8,
        (byte)ILDI, 0,0,8,
        (byte)ISUB,
        (byte)INOP,
        (byte)INOP,
      
    };
    Processor p = new Processor(1024, code);
    while (p.pc < code.length) {
      System.out.print(p.disasm(p.pc) + "\t\t");
      System.out.println(p.getProc() + "   " + p.getStack());
      p.exec();
    }
  }
}