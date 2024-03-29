package com.pelleplutt.operandi.proc;

public interface ByteCode {
  static final int VERSION = 0x00000001;
  
  static final int ICOND_AL = 0x0;
  static final int ICOND_EQ = 0x1;
  static final int ICOND_NE = 0x2;
  static final int ICOND_GT = 0x3;
  static final int ICOND_GE = 0x4;
  static final int ICOND_LT = 0x5;
  static final int ICOND_LE = 0x6;
  
  static final int INOP     = 0x00;
  
  static final int IADD     = 0x01; // add                           push(pop() + pop())
  static final int ISUB     = 0x02; // sub                           push(pop() - pop())
  static final int IMUL     = 0x03; // mul                           push(pop() * pop())
  static final int IDIV     = 0x04; // div                           push(pop() / pop())
  static final int IREM     = 0x05; // rem                           push(pop() % pop())
  static final int ISHIFTL  = 0x06; // shift left                    push(pop() << pop())
  static final int ISHIFTR  = 0x07; // shift right                   push(pop() >> pop())
  static final int IAND     = 0x08; // and                           push(pop() & pop())
  static final int IOR      = 0x09; // or                            push(pop() | pop())
  static final int IXOR     = 0x0a; // xor                           push(pop() ^ pop())
  static final int INOT     = 0x0b; // not (binary)                  push(~pop())
  static final int INEG     = 0x0c; // negate                        push(-pop())
  static final int ILNOT    = 0x0d; // not (logical)                 push(pop() != 0 ? 1 : 0)
  static final int ICMP     = 0x0e; // compare                       pop() - pop()    [updates $sr_zero and $sr_minus]
  static final int ICMPN    = 0x0f; // compare neg                   -(pop() - pop()) [updates $sr_zero and $sr_minus]

  static final int ICMP_0   = 0x10; // compare 0                     pop() - 0        [updates $sr_zero and $sr_minus]
  static final int ICMN_0   = 0x11; // compare neg 0                 0 - pop())       [updates $sr_zero and $sr_minus]
  static final int IADD_IM  = 0x12; // add immediate                 push(pop() + xx+1)
  static final int ISUB_IM  = 0x13; // sub immediate                 push(pop() - (xx+1))
  static final int IPUSH_S  = 0x14; // push signed immediate         push(ss)
  static final int IPUSH_U  = 0x15; // push unsigned immediate       push(xx+128)
  static final int IPUSH_NIL= 0x16; // push nil                      push(nil)
  static final int IPUSH_0  = 0x17; // push 0                        push(0)
  static final int IPUSH_1  = 0x18; // push 1                        push(1)
  static final int IPUSH_2  = 0x19; // push 2                        push(2)
  static final int IPUSH_3  = 0x1a; // push 3                        push(3)
  static final int IPUSH_4  = 0x1b; // push 4                        push(4)
  static final int IDEF_ME  = 0x1c; // set me reg                    $me = peek()
  static final int IPUSH_ME = 0x1d; // push me reg                   push($me)
  static final int IUDEF_ME = 0x1e; // undef me reg                  $me = -1
  
  static final int IPOP     = 0x20; 
  static final int IDUP     = 0x21; 
  static final int ISWAP    = 0x22;
  static final int ICPY     = 0x23; // copy stack entry to top       push($sp[ss])
  static final int ISTOR    = 0x24; // pop to memory                 mem[pop()]=pop()
  static final int ISTOR_IM = 0x25; // pop to memory immediate       mem[xxxxxx]=pop()
  static final int ILOAD    = 0x26; // push from memory              push(mem[pop()])
  static final int ILOAD_IM = 0x27; // push from memory immediate    push(mem[xxxxxx])
  static final int ISTOR_FP = 0x28; // store to stack rel fp         $fp[ss]=pop()
  static final int ILOAD_FP = 0x29; // load from stack rel fp        push($fp[ss])
  static final int ISP_INCR = 0x2a; // allocate on stack             $sp += xx+1
  static final int ISP_DECR = 0x2b; // deallocate from stack         $sp -= xx+1
  static final int IPUSH_PC = 0x2c; // push PC reg                   push($pc)
  static final int IPUSH_SP = 0x2d; // push SP reg                   push($sp)
  static final int IPUSH_FP = 0x2e; // push FP reg                   push($fp)
  static final int IPUSH_SR = 0x2f; // push SR reg                   push($sr)  
  
  static final int IADD_Q1  = 0x30; // add quick                     push(pop() + 1)
  static final int IADD_Q2  = 0x31; // add quick                     push(pop() + 2)
  static final int IADD_Q3  = 0x32; // add quick                     push(pop() + 3)
  static final int IADD_Q4  = 0x33; // add quick                     push(pop() + 4)
  static final int IADD_Q5  = 0x34; // add quick                     push(pop() + 5)
  static final int IADD_Q6  = 0x35; // add quick                     push(pop() + 6)
  static final int IADD_Q7  = 0x36; // add quick                     push(pop() + 7)
  static final int IADD_Q8  = 0x37; // add quick                     push(pop() + 8)
  static final int ISUB_Q1  = 0x38; // sub quick                     push(pop() - 1)
  static final int ISUB_Q2  = 0x39; // sub quick                     push(pop() - 2)
  static final int ISUB_Q3  = 0x3a; // sub quick                     push(pop() - 3)
  static final int ISUB_Q4  = 0x3b; // sub quick                     push(pop() - 4)
  static final int ISUB_Q5  = 0x3c; // sub quick                     push(pop() - 5)
  static final int ISUB_Q6  = 0x3d; // sub quick                     push(pop() - 6)
  static final int ISUB_Q7  = 0x3e; // sub quick                     push(pop() - 7)
  static final int ISUB_Q8  = 0x3f; // sub quick                     push(pop() - 8)

  static final int ICAST_I  = 0x40; // cast to int
  static final int ICAST_F  = 0x41; // cast to float
  static final int ICAST_S  = 0x42; // cast to string
  static final int ICAST_CH = 0x43; // cast to char

  static final int IGET_TYP = 0x48; // get top stack element type    push(typeof(pop()))
  
  static final int ISET_CRE = 0x50; // create set                    sz=pop(); while(sz--){l.add($sp[-sz]);};push(l);
  static final int IARR_CRE = 0x51; // create array                  addr=xxxxxx; sz=pop(); while(sz--){l.add(mem[addr++]);};push(l);
  static final int ISET_DRF = 0x52; // set dereference               ix=pop(); l=pop(); push(l[ix]);
  static final int ISET_WR  = 0x53; // set write index               v=pop(); ix=pop(); l=pop(); l[ix]=v; push(l);
  static final int IARR_ADD = 0x54; // array add                     v=pop(); l=pop(); l.add(v);
  static final int IMAP_ADD = 0x55; // map add tuple                 k=pop(); v=pop(); peek().add(k,v);
  static final int ISET_DEL = 0x56; // set remove                    ix=pop(); l=pop(); l.del(ix);
  static final int IARR_INS = 0x57; // array insert                  v=pop(); ix=pop(); l=pop(); l.ins(ix,v);
  static final int ISET_SZ  = 0x58; // read set size                 l=pop();push(l.size)
  static final int ISET_RD  = 0x59; // read set entry                ix=pop(); s=pop(); push(s[ix]);
  static final int IRNG2    = 0x5a; // range(from,to)                push(range(pop(to), pop(from));
  static final int IRNG3    = 0x5b; // range(from,step,to)           push(range(pop(to), pop(step), pop(from));
  static final int ITUP_CRE = 0x5c; // create tuple                  push(tuple(pop(),pop()));
  static final int IIN      = 0x5d; // check if x in y               set=pop(); elem=pop(); $sr_zero=(elem in set); [updates $sr_zero]

  static final int ICALL    = 0x60; // call function                 (argc on stack) a=pop(); push($pc); push($fp); $fp=sp; $pc=a
  static final int ICALL_IM = 0x61; // call function immediate       (argc on stack) push($pc+3); push($fp); $fp=sp; $pc=xxxxxx
  static final int ICALL_R  = 0x62; // call function relative pc     (argc on stack) push($pc+3); push($fp); $fp=sp; $pc=$pc+ssssss
  static final int IANO_CRE = 0x63; // create anonymous function     locals=pop(); addr=pop(); push(anon{addr, locals});
  static final int IRET     = 0x66; // return                        $sp=$fp; $fp=pop(); $pc=pop(); argc=pop(); $sp-=argc;
  static final int IRETV    = 0x67; // return val                    t=pop(); $sp=$fp; $fp=pop(); $pc=pop(); argc=pop(); $sp-=argc; push(t);
  
  static final int IPUSH_EQ = 0x69; // push condition                if ($sr_zero) push(1) else push(0); 
  static final int IPUSH_NE = 0x6a; 
  static final int IPUSH_GT = 0x6b; 
  static final int IPUSH_GE = 0x6c; 
  static final int IPUSH_LT = 0x6d; 
  static final int IPUSH_LE = 0x6e; 

  static final int IJUMP    = 0x70; // jump                          $pc = xxxxxx 
  static final int IJUMP_EQ = 0x71; 
  static final int IJUMP_NE = 0x72; 
  static final int IJUMP_GT = 0x73; 
  static final int IJUMP_GE = 0x74; 
  static final int IJUMP_LT = 0x75; 
  static final int IJUMP_LE = 0x76; 
  static final int IBRA     = 0x78; // branch                        $pc = $pc + ssssss  
  static final int IBRA_EQ  = 0x79; 
  static final int IBRA_NE  = 0x7a; 
  static final int IBRA_GT  = 0x7b; 
  static final int IBRA_GE  = 0x7c; 
  static final int IBRA_LT  = 0x7d; 
  static final int IBRA_LE  = 0x7e; 
  static final int IBKPT    = 0x7f; // breakpoint 
  
  
  static final int UD = -1;  
  static final int ISIZE[] = {
    // x0 x1 x2 x3 x4 x5 x6 x7 x8 x9 xa xb xc xd xe xf
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, //0x
        1, 1, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1,UD, //1x
        1, 1, 1, 2, 1, 4, 1, 4, 2, 2, 2, 2, 1, 1, 1, 1, //2x
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, //3x
        1, 1, 1, 1,UD,UD,UD,UD, 1,UD,UD,UD,UD,UD,UD,UD, //4x
        1, 4, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,UD,UD, //5x
        1, 4, 4, 1,UD,UD, 1, 1,UD, 1, 1, 1, 1, 1, 1,UD, //6x
        4, 4, 4, 4, 4, 4, 4,UD, 4, 4, 4, 4, 4, 4, 4, 1, //7x
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //8x
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //9x
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //ax
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //bx
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //cx
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //dx
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //ex
       UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD,UD, //fx
  };
  

  /**
   * Stack when call by address
   * ARG..
   * ARG1
   * ARG0
   * ARGC
   * ---> call
   */
  /**
   * Stack when call by stack address
   * ARG..
   * ARG1
   * ARG0
   * ARGC
   * FUNCADDR
   * ---> call
   */
  /**
   * Stack when calling anonymous
   * ARG..
   * ARG1
   * ARG0
   * ARGC
   * ANONADDR
   * ---> call
   */
  /**
   * Stack in call, regular
   * ARG...
   * ARG1
   * ARG0
   * ARGC
   * ME
   * PC
   * FP
   */
  /**
   * Stack in call, anonymous
   * ARG...
   * ARG1
   * ARG0
   * ARGC
   * ME
   * PC
   * FP
   * (anonymous definition scope list)
   */
  
  static final int FRAME_0_FP = 1;
  static final int FRAME_1_PC = 2;
  static final int FRAME_2_ME = 3;
  static final int FRAME_3_ARGC = 4;
  static final int FRAME_SIZE = 4;
  
  static final String INBUILTFN_TOSTR = "str";
  static final String INBUILTFN_TOINT = "int";
  static final String INBUILTFN_TOFLOAT = "float";
  static final String INBUILTFN_TOCHAR = "char";
  static final String INBUILTFN_ISNIL = "isnil";
  static final String INBUILTFN_ISINT = "isint";
  static final String INBUILTFN_ISFLOAT = "isfloat";
  static final String INBUILTFN_ISSTR = "isstr";
  static final String INBUILTFN_ISFUNC = "isfunc";
  static final String INBUILTFN_ISANON = "isanon";
  static final String INBUILTFN_ISSET = "isset";
  static final String INBUILTFN_ISMAP = "ismap";
  static final String INBUILTFN_ISARR = "isarr";
  static final String INBUILTFN_ISTUP = "istup";
  static final String INBUILTFN_TYPEOF = "type";
  static final String INBUILTFN_LEN = "len";
  
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
