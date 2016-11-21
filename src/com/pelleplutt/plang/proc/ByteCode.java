package com.pelleplutt.plang.proc;

public interface ByteCode {
  static final int ICOND_AL = 0x0;
  static final int ICOND_EQ = 0x1;
  static final int ICOND_NE = 0x2;
  static final int ICOND_GT = 0x3;
  static final int ICOND_GE = 0x4;
  static final int ICOND_LT = 0x5;
  static final int ICOND_LE = 0x6;
  
  static final int INOP  = 0x00;
  
  static final int IADD  = 0x01; // add                           push(pop() + pop())
  static final int ISUB  = 0x02; // sub                           push(pop() - pop())
  static final int IMUL  = 0x03; // mul                           push(pop() * pop())
  static final int IDIV  = 0x04; // div                           push(pop() / pop())
  static final int IREM  = 0x05; // rem                           push(pop() % pop())
  static final int ISHL  = 0x06; // shift left                    push(pop() << pop())
  static final int ISHR  = 0x07; // shift right                   push(pop() >> pop())
  static final int IAND  = 0x08; // and                           push(pop() & pop())
  static final int IOR   = 0x09; // or                            push(pop() | pop())
  static final int IXOR  = 0x0a; // xor                           push(pop() ^ pop())
  static final int INOT  = 0x0b; // not (binary)                  push(~pop())
  static final int INEG  = 0x0c; // negate                        push(-pop())
  static final int ILNOT = 0x0d; // not (logical)                 push(pop() != 0 ? 1 : 0)
  static final int ICMP  = 0x0e; // compare                       pop() - pop()
  static final int ICMN  = 0x0f; // compare neg                   -(pop() - pop())

  static final int ICMP0 = 0x10; // compare 0                     pop() - 0
  static final int ICMN0 = 0x11; // compare neg 0                 0 - pop())
  static final int IADI  = 0x12; // add immediate                 push(pop() + xx+1)
  static final int ISUI  = 0x13; // sub immediate                 push(pop() - (xx+1))
  static final int IPUI  = 0x14; // push signed immediate         push(xx)
  static final int IPUNIL= 0x15; // push nil                      push(nil)
  static final int IPU0  = 0x16; // push 0                        push(0)
  static final int IPU1  = 0x17; // push 1                        push(1)
  static final int IPU2  = 0x18; // push 2                        push(2)
  static final int IPU3  = 0x19; // push 3                        push(3)
  static final int IPU4  = 0x1a; // push 4                        push(4)
  static final int IPUC  = 0x1b; // push constant                 push(xxxxxx)
  
  static final int IPOP  = 0x20; 
  static final int IDUP  = 0x21; 
  static final int ISWP  = 0x22;
  static final int ICPY  = 0x23; // copy stack entry to top       push($sp[ss])
  static final int ISTR  = 0x24; // pop to memory                 mem[pop()]=pop()
  static final int ISTI  = 0x25; // pop to memory immediate       mem[xxxxxx]=pop()
  static final int ILD   = 0x26; // push from memory              push(mem[pop()])
  static final int ILDI  = 0x27; // push from memory immediate    push(mem[xxxxxx])
  static final int ISTF  = 0x28; // store to stack rel fp         $fp[ss]=pop()
  static final int ILDF  = 0x29; // load from stack rel fp        push($fp[ss])

  static final int ISPI  = 0x2c; // allocate on stack             $sp += xx+1
  static final int ISPD  = 0x2d; // deallocate from stack         $sp -= xx+1

  static final int IADQ1 = 0x30; // add quick                     push(pop() + 1)
  static final int IADQ2 = 0x31; // add quick                     push(pop() + 2)
  static final int IADQ3 = 0x32; // add quick                     push(pop() + 3)
  static final int IADQ4 = 0x33; // add quick                     push(pop() + 4)
  static final int IADQ5 = 0x34; // add quick                     push(pop() + 5)
  static final int IADQ6 = 0x35; // add quick                     push(pop() + 6)
  static final int IADQ7 = 0x36; // add quick                     push(pop() + 7)
  static final int IADQ8 = 0x37; // add quick                     push(pop() + 8)
  static final int ISUQ1 = 0x38; // sub quick                     push(pop() - 1)
  static final int ISUQ2 = 0x39; // sub quick                     push(pop() - 2)
  static final int ISUQ3 = 0x3a; // sub quick                     push(pop() - 3)
  static final int ISUQ4 = 0x3b; // sub quick                     push(pop() - 4)
  static final int ISUQ5 = 0x3c; // sub quick                     push(pop() - 5)
  static final int ISUQ6 = 0x3d; // sub quick                     push(pop() - 6)
  static final int ISUQ7 = 0x3e; // sub quick                     push(pop() - 7)
  static final int ISUQ8 = 0x3f; // sub quick                     push(pop() - 8)

  static final int ITOI  = 0x40; // cast to int
  static final int ITOF  = 0x41; // cast to float
  static final int ITOS  = 0x42; // cast to string
  
  static final int IIXRD = 0x50; // list read index               push(pop(list).list_get(pop(ix)))
  static final int IIXWR = 0x51; // list write index              pop(list).set(pop(ix)) = pop()
  static final int IIXADD= 0x52; // list add                      pop(list).add(pop(ix))
  static final int IIXDEL= 0x53; // list remove                   pop(list).del(pop(ix))
  static final int IIXINS= 0x54; // list insert                   pop(list).ins(pop(ix)) = pop()
  static final int IIXSZ = 0x55; // read list size                push(pop(list).size)

  static final int ICAL  = 0xe0; // call function                 a=pop(); push($pc+3); push($fp); $fp=sp; $pc=a
  static final int ICALI = 0xe1; // call function immediate       push($pc+3); push($fp); $fp=sp; $pc=xxxxxx
  static final int IRET  = 0xe8; // return                        $sp=$fp; $fp=pop(); $pc=pop(); argc=pop(); $sp-=argc;
  static final int IRETV = 0xe9; // return val                    t=pop(); $sp=$fp; $fp=pop(); $pc=pop(); argc=pop(); $sp-=argc; push(t);
  
  static final int IJMP  = 0xf0; // jump                          $pc = xxxxxx 
  static final int IJMPEQ= 0xf1; 
  static final int IJMPNE= 0xf2; 
  static final int IJMPGT= 0xf3; 
  static final int IJMPGE= 0xf4; 
  static final int IJMPLT= 0xf5; 
  static final int IJMPLE= 0xf6; 
  static final int IBRA  = 0xf8; // branch                        $pc = $pc + ssssss  
  static final int IBRAEQ= 0xf9; 
  static final int IBRANE= 0xfa; 
  static final int IBRAGT= 0xfb; 
  static final int IBRAGE= 0xfc; 
  static final int IBRALT= 0xfd; 
  static final int IBRALE= 0xfe; 
  static final int IBKPT = 0xff; // breakpoint 
  
  
  static final int UD = -1;  
  static final int ISIZE[] = {
      //0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, //00
       UD,UD, 2, 2, 2, 1, 1, 1, 1, 1, 1, 4,UD,UD,UD,UD, //10
        1, 1, 1, 2, 1, 4, 1, 4, 2, 2,UD,UD, 2, 2,UD,UD, //20
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, //30
        1, 1, 1,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //40
        1, 1, 1, 1, 1, 1,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //50
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //60
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //70
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //80
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //90
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //a0
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //b0
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //c0
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //d0
        1, 4,UD,UD,UD,UD,UD,UD, 1,UD,UD,UD,UD,UD,UD,UD, //e0
        4, 4, 4, 4, 4, 4, 4,UD, 4, 4, 4, 4, 4, 4, 4, 0, //f0
 };
  
  
  /*
  

  static final int ICOND_AL = 0x0;
  static final int ICOND_EQ = 0x1; // Z == 1
  static final int ICOND_NE = 0x2; // Z == 0
  static final int ICOND_HS = 0x3; // C == 1
  static final int ICOND_LO = 0x4; // C == 0
  static final int ICOND_MI = 0x5; // N == 1
  static final int ICOND_PL = 0x6; // N == 0
  static final int ICOND_VS = 0x7; // V == 1
  static final int ICOND_VC = 0x8; // V == 0
  static final int ICOND_HI = 0x9; // C==1 & Z == 0
  static final int ICOND_LS = 0xa; // C==0 | Z == 1
  static final int ICOND_GE = 0xb; // N == V
  static final int ICOND_LT = 0xc; // N != V
  static final int ICOND_GT = 0xd; // Z == 0 & N == V
  static final int ICOND_LE = 0xe; // Z == 1 & N != V 
  static final int ICOND_IT = 0xf; // I == 1
  
  static final int INOP  = 0x0000;
  
  // [op:8 reg_h:4 reg_l:4]
  static final int IMOV  = 0x0100; // nibbles 3,4 are reg operands (h = l) 
  static final int IADD  = 0x0200; // nibbles 3,4 are reg operands (h = h + l)
  static final int ISUB  = 0x0300; // nibbles 3,4 are reg operands (h = h - l)
  static final int INEG  = 0x0300; // nibbles 3,4 are same, hl = ~(hl)
  static final int IMUL  = 0x0400; // nibbles 3,4 are reg operands (h = h * l)
  static final int IDIV  = 0x0500; // nibbles 3,4 are reg operands (h = h / l)
  static final int IREM  = 0x0600; // nibbles 3,4 are reg operands (h = h % l)
  static final int ISHL  = 0x0700; // nibbles 3,4 are reg operands (h = h << l)
  static final int ISHR  = 0x0800; // nibbles 3,4 are reg operands (h = h >> l)
  static final int IAND  = 0x0900; // nibbles 3,4 are reg operands (h = h & l)
  static final int IOR   = 0x0a00; // nibbles 3,4 are reg operands (h = h | l)
  static final int IXOR  = 0x0b00; // nibbles 3,4 are reg operands (h = h ^ l)
  static final int INOT  = 0x0b00; // nibbles 3,4 are same, hl = -(hl)
  static final int ICMP  = 0x0c00; // nibbles 3,4 are reg operands (h - l)
  static final int ICMN  = 0x0d00; // nibbles 3,4 are reg operands (l - h)

  // [op:8 reg_h:4 const:4]
  static final int ILDQ  = 0x1000; // reg nibble 3 = nibble 4
  static final int ILDQN = 0x1100; // reg nibble 3 = -nibble 4
  
  // [op:12 reg:4]
  static final int IPSH  = 0x2000; // push reg (nibble 4)
  static final int IPOP  = 0x2010; // pop reg (nibble 4)

  // [op:8 reg1:4 reg2:4]
  static final int IPSH2 = 0x2100; // push regs (push nibble 3, push nibble 4)
  static final int IPOP2 = 0x2200; // pop regs (pop nibble 4, pop nibble 3)
  
  // [op:4 cond:4 offs/addr:8] ([offs/addr:16])
  static final int IBRA  = 0x3000; // nibble 2 is conditional, nibbles 3,4 are bitpacked offset
  static final int IJMP  = 0x4000; // nibble 2 is conditional, nibbles 3,4 are bitpacked address
  
  // [op:4 src_reg:4 dst_reg:4 offs:4] ([offs:16])
  static final int ISTRI = 0x5000; // nibble 2 is src reg, nibble 3 is addr reg, nibble 4 is bitpacked offset  
  // [op:4 src_reg:4 dst_reg:4 offs_reg:4]
  static final int ISTR  = 0x6000; // nibble 2 is src reg, nibble 3 is dst reg, nibble 4 is offset register  
  // [op:4 dst_reg:4 addr:8] ([addr:16])
  static final int ILDI  = 0x7000; // nibble 2 is dst reg, nibbles 3,4 are bitpacked address  
  // [op:4 dst_reg:4 src_reg:4 offs_reg:4]
  static final int ILD   = 0x8000; // nibble 2 is dst reg, nibble 3 is addr reg, nibble 4 is offset register  
  */

}
