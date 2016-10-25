package com.pelleplutt.plang;

public interface ByteCode {
  static final int ICOND_AL = 0x0;
  static final int ICOND_EQ = 0x1;
  static final int ICOND_NE = 0x2;
  static final int ICOND_GT = 0x3;
  static final int ICOND_GE = 0x4;
  static final int ICOND_LT = 0x5;
  static final int ICOND_LE = 0x6;
  
  static final int INOP  = 0x00;
  
  static final int IADD  = 0x01;
  static final int ISUB  = 0x02;
  static final int IMUL  = 0x03;
  static final int IDIV  = 0x04;
  static final int IREM  = 0x05;
  static final int ISHL  = 0x06;
  static final int ISHR  = 0x07;
  static final int IAND  = 0x08;
  static final int IOR   = 0x09;
  static final int IXOR  = 0x0a;
  static final int ICMP  = 0x0b;
  static final int ICMN  = 0x0c;
  static final int INOT  = 0x0d;
  static final int INEG  = 0x0e;

  static final int IADI  = 0x11; // add immediate // xx+1
  static final int ISUI  = 0x12; // sub immediate // xx+1
  static final int IPUI  = 0x13; // push immediate // xx
  
  static final int IPOP  = 0x20; 
  static final int IDUP  = 0x21; 
  static final int IROT  = 0x22;
  static final int ICPY  = 0x23; // copy stack entry to top //xx + 1
  static final int ISTR  = 0x24; // pop to memory
  static final int ISTI  = 0x25; // pop to memory immediate //xxxxxx
  static final int ILD   = 0x26; // push from memory
  static final int ILDI  = 0x27; // push from memory immediate //xxxxxx

  static final int IALL  = 0x28; // allocate on stack //xx+1
  static final int IFRE  = 0x29; // deallocate from stack //xx+1

  static final int ICAL  = 0xe0; 
  static final int IRET  = 0xe8; 
  static final int IJMP  = 0xf0; 
  static final int IJMPEQ  = 0xf1; 
  static final int IJMPNE  = 0xf2; 
  static final int IJMPGT  = 0xf3; 
  static final int IJMPGE  = 0xf4; 
  static final int IJMPLT  = 0xf5; 
  static final int IJMPLE  = 0xf6; 
  static final int IBRA  = 0xf8; 
  static final int IBRAEQ  = 0xf9; 
  static final int IBRANE  = 0xfa; 
  static final int IBRAGT  = 0xfb; 
  static final int IBRAGE  = 0xfc; 
  static final int IBRALT  = 0xfd; 
  static final int IBRALE  = 0xfe; 
  
  
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
