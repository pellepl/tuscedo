package com.pelleplutt.plang;

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
    fp = -1;
    memory = new M[memorySize];
    for (int i = 0; i < memorySize; i++) memory[i] = new M();
    this.code = code;
  }
  
  String disasm(int pc) {
    int instr = (int)(code[pc++] & 0xff);
    StringBuilder sb = new StringBuilder();
    switch (instr) {
    case INOP:  // 0x00
      sb.append("nop");
      break;
    case IADD:  // 0x01
      sb.append("add");
      break;
    case ISUB:  // 0x02
      sb.append("sub");
      break;
    case IMUL:  // 0x03
      sb.append("mul");
      break;
    case IDIV:  // 0x04
      sb.append("div");
      break;
    case IREM:  // 0x05
      sb.append("rem");
      break;
    case ISHL:  // 0x06
      sb.append("shl");
      break;
    case ISHR:  // 0x07
      sb.append("shr");
      break;
    case IAND:  // 0x08
      sb.append("and");
      break;
    case IOR :  // 0x09
      sb.append("or ");
      break;
    case IXOR:  // 0x0a
      sb.append("xor");
      break;
    case ICMP:  // 0x0b
      sb.append("cmp");
      break;
    case ICMN:  // 0x0c
      sb.append("cmn");
      break;
    case INOT:  // 0x0d
      sb.append("not");
      break;
    case INEG:  // 0x0e
      sb.append("neg");
      break;
    
    case IADI:
      sb.append("adi ");
      sb.append(String.format("0x%02x", codetoi(pc, 1) + 1));
      break;
    case ISUI:
      sb.append("sui ");
      sb.append(String.format("0x%02x", codetoi(pc, 1) + 1));
      break;
    case IPUI:
      sb.append("pui ");
      sb.append(String.format("0x%02x", codetoi(pc, 1)));
      break;

    case IPOP:  // 0x20
      sb.append("pop");
      break;
    case IDUP:  // 0x21
      sb.append("dup");
      break;
    case IROT:  // 0x22
      sb.append("rot");
      break;
    case ICPY:  // 0x23 // copy stack entry to top //xx + 1
      sb.append("cpy ");
      sb.append(String.format("sp[0x%02x]", codetoi(pc, 1) + 1));
      break;
    case ISTR:  // 0x24 // pop to memory
      sb.append("str");
      break;
    case ISTI:  // 0x25 // pop to memory immediate
      sb.append("sti ");
      sb.append(String.format("mem[0x%06x]", codetoi(pc, 3)));
      break;
    case ILD :  // 0x26 // push from memory
      sb.append("ld ");
      break;
    case ILDI:  // 0x27 // push from memory immediate
      sb.append("ldi ");
      sb.append(String.format("mem[0x%06x]", codetoi(pc, 3)));
      break;

    case IALL:
      sb.append("all ");
      sb.append(String.format("%d", codetoi(pc, 1) + 1));
      break;
    case IFRE:
      sb.append("fre ");
      sb.append(String.format("%d", codetoi(pc, 1) + 1));
      break;

    case ICAL:  // 0xe0 
      sb.append("cal");
      break;
    case IRET:  // 0xe8 
      sb.append("ret");
      break;
    case IJMP:  // 0xf0 
      sb.append("jmp");
      break;
    case IJMPEQ: 
      sb.append("jmp eq");
      break;
    case IJMPNE: 
      sb.append("jmp ne");
      break;
    case IJMPGT: 
      sb.append("jmp gt");
      break;
    case IJMPGE: 
      sb.append("jmp ge");
      break;
    case IJMPLT: 
      sb.append("jmp lt");
      break;
    case IJMPLE: 
      sb.append("jmp le");
      break;
    case IBRA:  // 0xf8 
      sb.append("bra");
      break;
    case IBRAEQ: 
      sb.append("bra eq");
      break;
    case IBRANE: 
      sb.append("bra ne");
      break;
    case IBRAGT: 
      sb.append("bra gt");
      break;
    case IBRAGE: 
      sb.append("bra ge");
      break;
    case IBRALT: 
      sb.append("bra lt");
      break;
    case IBRALE: 
      sb.append("bra le");
      break;
    default:
      sb.append("???");
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

  public void exec() {
    int instr = (int)(code[pc++] & 0xff);
    switch (instr) {
    case INOP:  // 0x00
      break;
    case IADD:  // 0x01
      add();
      break;
    case ISUB:  // 0x02
      sub();
      break;
    case IMUL:  // 0x03
      mul();
      break;
    case IDIV:  // 0x04
      div();
      break;
    case IREM:  // 0x05
      break;
    case ISHL:  // 0x06
      break;
    case ISHR:  // 0x07
      break;
    case IAND:  // 0x08
      break;
    case IOR :  // 0x09
      break;
    case IXOR:  // 0x0a
      break;
    case ICMP:  // 0x0b
      break;
    case ICMN:  // 0x0c
      break;
    case INOT:  // 0x0d
      break;
    case INEG:  // 0x0e
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
    
    case IPOP:  // 0x20
      pop();
      break;
    case IDUP:  // 0x21
      dup();
      break;
    case IROT:  // 0x22
      rot();
      break;
    case ICPY:  // 0x23 // copy stack entry to top //xx + 1
      cpy();
      break;
    case ISTR:  // 0x24 // pop to memory
      str();
      break;
    case ISTI:  // 0x25 // pop to memory immediate
      sti();
      break;
    case ILD :  // 0x26 // push from memory
      ld();
      break;
    case ILDI:  // 0x27 // push from memory immediate
      ldi();
      break;
      
    case IALL:  // 0x28 // allocate on stack //xx+1
      all();
      break;
    case IFRE:  // 0x28 // deallocate from stack //xx+1
      fre();
      break;

    case ICAL:  // 0xe0 
      break;
    case IRET:  // 0xe8 
      break;
    case IJMP:  // 0xf0 // last 3 bits conditional 
      break;
    case IBRA:  // 0xf8 // last 3 bits conditional 
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
