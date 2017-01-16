package com.pelleplutt.plang.proc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import com.pelleplutt.plang.CompilerError;

public class Assembler implements ByteCode {
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
  
  public static byte[] assemble(String s) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Map<String, Integer> labels = new HashMap<String, Integer>();
    Map<Integer, String> labelRefs = new HashMap<Integer, String>();
    String lines[] = s.split("\n");
    for (String line:lines) {
      if (line == null) continue;
      String l = line;
      int commentOffs = l.indexOf("//");
      if (commentOffs >= 0) {
        l = l.substring(0, commentOffs);  
      }
      l = l.toLowerCase().trim();
      if (l.endsWith(":")) {
        labels.put(l.substring(0, l.length()-1), baos.size());
      } else {
        byte code[] = Assembler.asmInstr(l, baos.size(), labelRefs);
        if (code != null) {
          try { baos.write(code); } catch (IOException e) { e.printStackTrace(); }
        }
      }
    }
    byte[] d = baos.toByteArray();
    
    // resolve label references
    for (int labelInstrAddr : labelRefs.keySet()) {
      String label = labelRefs.get(labelInstrAddr);
      int jmp = 0;
      int instr = (int)(d[labelInstrAddr] & 0xff); 
      if (instr >= IBRA && instr <= IBRA_LE) {
        // branch, relative
        jmp = labels.get(label) - labelInstrAddr;
      } else if (instr >= IJUMP && instr <= IJUMP_LE ||
          instr == ICALL_IM) {
        // jump | call, absolute
        jmp = labels.get(label);
      } else {
        throw new CompilerError("unknown label instruction");
      }
      d[labelInstrAddr+1] = (byte)(jmp>>16);
      d[labelInstrAddr+2] = (byte)(jmp>>8);
      d[labelInstrAddr+3] = (byte)(jmp);
    }
    
    return d;
  }

  
  public static byte[] asmInstr(String line, int addr, Map<Integer, String> labelRefs) {
  	String[] tokens = line.split("\\s+");
  	if (tokens.length == 0) return null;
  	if (tokens[0].length() == 0) return null;
  	String op = tokens[0];
  	ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
  	try {
	  	if (op.equals("nop")) {
	  		baos.write(INOP);
	  	}
	  	else if (op.equals("add")) {
	  		baos.write(IADD);
	  	}
	  	else if (op.equals("sub")) {
	  		baos.write(ISUB);
	  	}
	  	else if (op.equals("mul")) {
	  		baos.write(IMUL);
	  	}
	  	else if (op.equals("div")) {
	  		baos.write(IDIV);
	  	}
	  	else if (op.equals("rem")) {
	  		baos.write(IREM);
	  	}
	  	else if (op.equals("shiftl")) {
	  		baos.write(ISHIFTL);
	  	}
	  	else if (op.equals("shiftr")) {
	  		baos.write(ISHIFTR);
	  	}
	  	else if (op.equals("and")) {
	  		baos.write(IAND);
	  	}
	  	else if (op.equals("or")) {
	  		baos.write(IOR);
	  	}
	  	else if (op.equals("xor")) {
	  		baos.write(IXOR);
	  	}
	  	else if (op.equals("cmp")) {
	  		baos.write(ICMP);
	  	}
	  	else if (op.equals("cmpn")) {
	  		baos.write(ICMPN);
	  	}
	  	else if (op.equals("not")) {
	  		baos.write(INOT);
	  	}
	  	else if (op.equals("neg")) {
	  		baos.write(INEG);
	  	}
	  	else if (op.equals("lnot")) {
	  		baos.write(ILNOT);
	  	}
	  	else if (op.equals("cmp_0")) {
	  		baos.write(ICMP_0);
	  	}
	  	else if (op.equals("cmpn_0")) {
	  		baos.write(ICMN_0);
	  	}
	  	else if (op.equals("add_im")) {
	  		baos.write(IADD_IM);
	  		baos.write(utobytes(tokens[1], 1, 1));
	  	}
	  	else if (op.equals("sub_im")) {
	  		baos.write(ISUB_IM);
	  		baos.write(utobytes(tokens[1], 1, 1));
	  	}
	  	else if (op.equals("push_s")) {
	  		baos.write(IPUSH_S);
	  		baos.write(stobytes(tokens[1], 1));
	  	}
	  	else if (op.equals("push_u")) {
	  		baos.write(IPUSH_U);
	  		baos.write(utobytes(tokens[1], 1, 128));
	  	}
	  	else if (op.equals("push_nil")) {
	  		baos.write(IPUSH_NIL);
	  	}
	  	else if (op.equals("push_0")) {
	  		baos.write(IPUSH_0);
	  	}
	  	else if (op.equals("push_1")) {
	  		baos.write(IPUSH_1);
	  	}
	  	else if (op.equals("push_2")) {
	  		baos.write(IPUSH_2);
	  	}
	  	else if (op.equals("push_3")) {
	  		baos.write(IPUSH_3);
	  	}
	  	else if (op.equals("push_4")) {
	  		baos.write(IPUSH_4);
	  	}
	  	else if (op.equals("def_me")) {
	  		baos.write(IDEF_ME);
	  	}
	  	else if (op.equals("push_me")) {
	  		baos.write(IPUSH_ME);
	  	}
	  	else if (op.equals("udef_me")) {
	  		baos.write(IUDEF_ME);
	  	}
	  	else if (op.equals("add_q1")) {
	  		baos.write(IADD_Q1);
	  	}
	  	else if (op.equals("add_q2")) {
	  		baos.write(IADD_Q2);
	  	}
	  	else if (op.equals("add_q3")) {
	  		baos.write(IADD_Q3);
	  	}
	  	else if (op.equals("add_q4")) {
	  		baos.write(IADD_Q4);
	  	}
	  	else if (op.equals("add_q5")) {
	  		baos.write(IADD_Q5);
	  	}
	  	else if (op.equals("add_q6")) {
	  		baos.write(IADD_Q6);
	  	}
	  	else if (op.equals("add_q7")) {
	  		baos.write(IADD_Q7);
	  	}
	  	else if (op.equals("add_q8")) {
	  		baos.write(IADD_Q8);
	  	}
	  	else if (op.equals("sub_q1")) {
	  		baos.write(ISUB_Q1);
	  	}
	  	else if (op.equals("sub_q2")) {
	  		baos.write(ISUB_Q2);
	  	}
	  	else if (op.equals("sub_q3")) {
	  		baos.write(ISUB_Q3);
	  	}
	  	else if (op.equals("sub_q4")) {
	  		baos.write(ISUB_Q4);
	  	}
	  	else if (op.equals("sub_q5")) {
	  		baos.write(ISUB_Q5);
	  	}
	  	else if (op.equals("sub_q6")) {
	  		baos.write(ISUB_Q6);
	  	}
	  	else if (op.equals("sub_q7")) {
	  		baos.write(ISUB_Q7);
	  	}
	  	else if (op.equals("sub_q8")) {
	  		baos.write(ISUB_Q8);
	  	}
	  	else if (op.equals("cast_I")) {
	  		baos.write(ICAST_I);
	  	}
	  	else if (op.equals("cast_F")) {
	  		baos.write(ICAST_F);
	  	}
	  	else if (op.equals("cast_S")) {
	  		baos.write(ICAST_S);
	  	}
      else if (op.equals("cast_ch")) {
        baos.write(ICAST_CH);
      }
      else if (op.equals("get_typ")) {
        baos.write(IGET_TYP);
      }
	  	else if (op.equals("pop")) {
	  		baos.write(IPOP);
	  	}
	  	else if (op.equals("dup")) {
	  		baos.write(IDUP);
	  	}
	  	else if (op.equals("swap")) {
	  		baos.write(ISWAP);
	  	}
	  	else if (op.equals("cpy")) {
	  		baos.write(ICPY);
	  		baos.write(stobytesreg("$sp", tokens[1], 1));
	  	}
	  	else if (op.equals("stor")) {
	  		baos.write(ISTOR);
	  	}
	  	else if (op.equals("stor_im")) {
	  		baos.write(ISTOR_IM);
	  		baos.write(stobytesreg("mem", tokens[1], 3));
	  	}
	  	else if (op.equals("load")) {
	  		baos.write(ILOAD);
	  	}
	  	else if (op.equals("load_im")) {
	  		baos.write(ILOAD_IM);
	  		baos.write(utobytesreg("mem", tokens[1], 3));
	  	}
	  	else if (op.equals("stor_fp")) {
	  		baos.write(ISTOR_FP);
	  		baos.write(stobytesreg("$fp", tokens[1], 1));
	  	}
	  	else if (op.equals("load_fp")) {
	  		baos.write(ILOAD_FP);
	  		baos.write(stobytesreg("$fp", tokens[1], 1));
	  	}
	  	else if (op.equals("sp_incr")) {
	  		baos.write(ISP_INCR);
	  		baos.write(stobytes(tokens[1], 1, 1));
	  	}
	  	else if (op.equals("sp_decr")) {
	  		baos.write(ISP_DECR);
	  		baos.write(stobytes(tokens[1], 1, 1));
	  	}
	  	else if (op.equals("set_cre")) {
	  		baos.write(ISET_CRE);
	  	}
	  	else if (op.equals("arr_cre")) {
	  		baos.write(IARR_CRE);
	  		baos.write(stobytesreg("mem", tokens[1], 3));
	  	}
	  	else if (op.equals("set_drf")) {
	  		baos.write(ISET_DRF);
	  	}
	  	else if (op.equals("set_wr")) {
	  		baos.write(ISET_WR);
	  	}
	  	else if (op.equals("arr_add")) {
	  		baos.write(IARR_ADD);
	  	}
	  	else if (op.equals("map_add")) {
	  		baos.write(IMAP_ADD);
	  	}
	  	else if (op.equals("set_del")) {
	  		baos.write(ISET_DEL);
	  	}
	  	else if (op.equals("arr_ins")) {
	  		baos.write(IARR_INS);
	  	}
	  	else if (op.equals("set_sz")) {
	  		baos.write(ISET_SZ);
	  	}
	  	else if (op.equals("set_rd")) {
	  		baos.write(ISET_RD);
	  	}
	  	else if (op.equals("rng2")) {
	  		baos.write(IRNG2);
	  	}
	  	else if (op.equals("rng3")) {
	  		baos.write(IRNG3);
	  	}
	  	else if (op.equals("call")) {
	  		baos.write(ICALL);
	  	}
	  	else if (op.equals("call_im")) {
	  		baos.write(ICALL_IM);
	  		baos.write(utobytesl(tokens[1], 3, addr, labelRefs));
	  	}
	  	else if (op.equals("ano_cre")) {
	  		baos.write(IANO_CRE);
	  	}
	  	else if (op.equals("ret")) {
	  		baos.write(IRET);
	  	}
	  	else if (op.equals("retv")) {
	  		baos.write(IRETV);
	  	}
      else if (op.equals("push_eq")) {
        baos.write(IPUSH_EQ);
      }
      else if (op.equals("push_ne")) {
        baos.write(IPUSH_NE);
      }
      else if (op.equals("push_gt")) {
        baos.write(IPUSH_GT);
      }
      else if (op.equals("push_ge")) {
        baos.write(IPUSH_GE);
      }
      else if (op.equals("push_lt")) {
        baos.write(IPUSH_LT);
      }
      else if (op.equals("push_le")) {
        baos.write(IPUSH_LE);
      }
      else if (op.equals("in")) {
        baos.write(IIN);
      }
	  	else if (op.equals("jump")) {
	  		baos.write(IJUMP);
	  		baos.write(utobytesl(tokens[1], 3, addr, labelRefs));
	  	}
	  	else if (op.equals("jump_eq")) {
	  		baos.write(IJUMP_EQ);
	  		baos.write(utobytesl(tokens[1], 3, addr, labelRefs));
	  	}
	  	else if (op.equals("jump_ne")) {
	  		baos.write(IJUMP_NE);
	  		baos.write(utobytesl(tokens[1], 3, addr, labelRefs));
	  	}
	  	else if (op.equals("jump_gt")) {
	  		baos.write(IJUMP_GT);
	  		baos.write(utobytesl(tokens[1], 3, addr, labelRefs));
	  	}
	  	else if (op.equals("jump_ge")) {
	  		baos.write(IJUMP_GE);
	  		baos.write(utobytesl(tokens[1], 3, addr, labelRefs));
	  	}
	  	else if (op.equals("jump_lt")) {
	  		baos.write(IJUMP_LT);
	  		baos.write(utobytesl(tokens[1], 3, addr, labelRefs));
	  	}
	  	else if (op.equals("jump_le")) {
	  		baos.write(IJUMP_LE);
	  		baos.write(utobytesl(tokens[1], 3, addr, labelRefs));
	  	}
	  	else if (op.equals("bra")) {
	  		baos.write(IBRA);
	  		baos.write(stobytesl(tokens[1], 3, addr, labelRefs));
	  	}
	  	else if (op.equals("bra_eq")) {
	  		baos.write(IBRA_EQ);
	  		baos.write(stobytesl(tokens[1], 3, addr, labelRefs));
	  	}
	  	else if (op.equals("bra_ne")) {
	  		baos.write(IBRA_NE);
	  		baos.write(stobytesl(tokens[1], 3, addr, labelRefs));
	  	}
	  	else if (op.equals("bra_gt")) {
	  		baos.write(IBRA_GT);
	  		baos.write(stobytesl(tokens[1], 3, addr, labelRefs));
	  	}
	  	else if (op.equals("bra_ge")) {
	  		baos.write(IBRA_GE);
	  		baos.write(stobytesl(tokens[1], 3, addr, labelRefs));
	  	}
	  	else if (op.equals("bra_lt")) {
	  		baos.write(IBRA_LT);
	  		baos.write(stobytesl(tokens[1], 3, addr, labelRefs));
	  	}
	  	else if (op.equals("bra_le")) {
	  		baos.write(IBRA_LE);
	  		baos.write(stobytesl(tokens[1], 3, addr, labelRefs));
	  	}
	  	else if (op.equals("bkpt")) {
	  		baos.write(IBKPT);
	  		baos.write(stobytesl(tokens[1], 3, addr, labelRefs));
	  	} else {
	  		throw new CompilerError("unknown instruction '" + tokens[0] + "'");
	  	}
  	} catch (IOException ioe) {
  	}
  	return baos.toByteArray();
  }
  
  static byte[] strtobytes(String s, int len, int offs, boolean signed) {
  	s = s.toLowerCase().trim();
  	int x;
  	if (s.startsWith("0x")) {
  		x = Integer.parseInt(s.substring(2), 16);
  	} else if (s.startsWith("0b")) {
  		x = Integer.parseInt(s.substring(2), 2);
  	} else {
  		x = Integer.parseInt(s, 10);
  	}
  	x -= offs;
  	if (!signed && x < 0) throw new CompilerError("range overflow");
  	byte b[] = new byte[len];
  	for (int i = 0; i < len; i++) {
  		b[i] = (byte)((x >>> (len - i - 1) * 8) & 0xff);
  	}
  	return b;
  }
  static byte[] utobytes(String s, int len, int offs) {
  	return strtobytes(s, len, offs, false);
  }

  static byte[] stobytes(String s, int len, int offs) {
  	return strtobytes(s, len, offs, true);
  }
  
  static byte[] utobytes(String s, int len) {
    return strtobytes(s, len, 0, false);
  }

  static byte[] stobytes(String s, int len) {
    return strtobytes(s, len, 0, true);
  }
  
  static byte[] utobytesl(String s, int len, int addr, Map<Integer, String> labelRefs) {
    try {
      return strtobytes(s, len, 0, false);
    } catch (Throwable t) {
      labelRefs.put(addr, s);
      return new byte[len];
    }
  }

  static byte[] stobytesl(String s, int len, int addr, Map<Integer, String> labelRefs) {
    try {
      return strtobytes(s, len, 0, true);
    } catch (Throwable t) {
      labelRefs.put(addr, s);
      return new byte[len];
    }
  }
  
  static String checkreg(String reg, String s) {
  	if (!s.startsWith(reg)) throw new CompilerError("expected '" + reg + "'");
  	s = s.substring(reg.length());
  	if (!s.startsWith("[") && !s.endsWith("]")) throw new CompilerError("expected brackets around '" + s + "'");
  	return s.substring(1, s.length()-1);
  }
  
  static byte[] stobytesreg(String reg, String s, int len, int offs) {
  	return strtobytes(checkreg(reg, s), len, offs, true);
  }

  static byte[] utobytesreg(String reg, String s, int len, int offs) {
  	return strtobytes(checkreg(reg, s), len, offs, false);
  }

  static byte[] stobytesreg(String reg, String s, int len) {
  	return strtobytes(checkreg(reg, s), len, 0, true);
  }

  static byte[] utobytesreg(String reg, String s, int len) {
  	return strtobytes(checkreg(reg, s), len, 0, false);
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
      sb.append(String.format("0x%02x", Processor.codetoi(code, pc, 1) + 1));
      break;
    case ISUB_IM:
      sb.append("sub_im  ");
      sb.append(String.format("0x%02x", Processor.codetoi(code, pc, 1) + 1));
      break;
    case IPUSH_S:
      sb.append("push_s  ");
      sb.append(String.format("%d", Processor.codetos(code, pc, 1)));
      break;
    case IPUSH_U:
      sb.append("push_u  ");
      sb.append(String.format("%d", Processor.codetoi(code, pc, 1)+128));
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
    case IADD_Q3:
      sb.append("add_q3  ");
      break;
    case IADD_Q4:
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
    case IGET_TYP:
      sb.append("get_typ ");
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
      sb.append(String.format("$sp[%4d]", Processor.codetos(code, pc, 1)));
      break;
    case ISTOR:
      sb.append("stor    ");
      break;
    case ISTOR_IM:
      sb.append("stor_im ");
      sb.append(String.format("mem[0x%06x]", Processor.codetoi(code, pc, 3)));
      break;
    case ILOAD :
      sb.append("load    ");
      break;
    case ILOAD_IM:
      sb.append("load_im ");
      sb.append(String.format("mem[0x%06x]", Processor.codetoi(code, pc, 3)));
      break;
    case ISTOR_FP:
      sb.append("stor_fp ");
      sb.append(String.format("$fp[%4d]", Processor.codetos(code, pc, 1)));
      break;
    case ILOAD_FP:
      sb.append("load_fp ");
      sb.append(String.format("$fp[%4d]", Processor.codetos(code, pc, 1)));
      break;

    case ISP_INCR:
      sb.append("sp_incr ");
      sb.append(String.format("%d", Processor.codetoi(code, pc, 1) + 1));
      break;
    case ISP_DECR:
      sb.append("sp_decr ");
      sb.append(String.format("%d", Processor.codetoi(code, pc, 1) + 1));
      break;

    case ISET_CRE:
      sb.append("set_cre ");
      break;
    case IARR_CRE:
      sb.append("arr_cre ");
      sb.append(String.format("mem[0x%06x]", Processor.codetoi(code, pc, 3)));
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
      sb.append(String.format("0x%06x", Processor.codetoi(code, pc, 3)));
      break;
    case IANO_CRE: 
      sb.append("ano_cre ");
      break;
    case IRET: 
      sb.append("ret     ");
      break;
    case IRETV: 
      sb.append("retv    ");
      break;
    case IPUSH_EQ: 
      sb.append("push_eq ");
      break;
    case IPUSH_NE: 
      sb.append("push_ne ");
      break;
    case IPUSH_GT: 
      sb.append("push_gt ");
      break;
    case IPUSH_GE: 
      sb.append("push_ge ");
      break;
    case IPUSH_LT: 
      sb.append("push_lt ");
      break;
    case IPUSH_LE: 
      sb.append("push_le ");
      break;
    case IIN: 
      sb.append("in      ");
      break;
    case IJUMP: 
      sb.append("jump    ");
      sb.append(String.format("%d", Processor.codetoi(code, pc, 3)));
      break;
    case IJUMP_EQ: 
      sb.append("jump_eq ");
      sb.append(String.format("%d", Processor.codetoi(code, pc, 3)));
      break;
    case IJUMP_NE: 
      sb.append("jump_ne ");
      sb.append(String.format("%d", Processor.codetoi(code, pc, 3)));
      break;
    case IJUMP_GT: 
      sb.append("jump_gt ");
      sb.append(String.format("%d", Processor.codetoi(code, pc, 3)));
      break;
    case IJUMP_GE: 
      sb.append("jump_ge ");
      sb.append(String.format("%d", Processor.codetoi(code, pc, 3)));
      break;
    case IJUMP_LT: 
      sb.append("jump_lt ");
      sb.append(String.format("%d", Processor.codetoi(code, pc, 3)));
      break;
    case IJUMP_LE: 
      sb.append("jump_le ");
      sb.append(String.format("%d", Processor.codetoi(code, pc, 3)));
      break;
    case IBRA: 
      sb.append("bra     ");
      sb.append(String.format("%d (0x%06x)", Processor.codetos(code, pc, 3), pc + Processor.codetos(code, pc, 3)-1));
      break;
    case IBRA_EQ: 
      sb.append("bra_eq  ");
      sb.append(String.format("%d (0x%06x)", Processor.codetos(code, pc, 3), pc + Processor.codetos(code, pc, 3)-1));
      break;
    case IBRA_NE: 
      sb.append("bra_ne  ");
      sb.append(String.format("%d (0x%06x)", Processor.codetos(code, pc, 3), pc + Processor.codetos(code, pc, 3)-1));
      break;
    case IBRA_GT: 
      sb.append("bra_gt  ");
      sb.append(String.format("%d (0x%06x)", Processor.codetos(code, pc, 3), pc + Processor.codetos(code, pc, 3)-1));
      break;
    case IBRA_GE: 
      sb.append("bra_ge  ");
      sb.append(String.format("%d (0x%06x)", Processor.codetos(code, pc, 3), pc + Processor.codetos(code, pc, 3)-1));
      break;
    case IBRA_LT: 
      sb.append("bra_lt  ");
      sb.append(String.format("%d (0x%06x)", Processor.codetos(code, pc, 3), pc + Processor.codetos(code, pc, 3)-1));
      break;
    case IBRA_LE: 
      sb.append("bra_le  ");
      sb.append(String.format("%d (0x%06x)", Processor.codetos(code, pc, 3), pc + Processor.codetos(code, pc, 3)-1));
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
  

}
