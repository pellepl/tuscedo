package com.pelleplutt.plang;

import static com.pelleplutt.plang.AST.OP_AND;
import static com.pelleplutt.plang.AST.OP_CALL;
import static com.pelleplutt.plang.AST.OP_DIV;
import static com.pelleplutt.plang.AST.OP_EQ2;
import static com.pelleplutt.plang.AST.OP_GE;
import static com.pelleplutt.plang.AST.OP_GT;
import static com.pelleplutt.plang.AST.OP_LE;
import static com.pelleplutt.plang.AST.OP_LT;
import static com.pelleplutt.plang.AST.OP_MINUS;
import static com.pelleplutt.plang.AST.OP_MUL;
import static com.pelleplutt.plang.AST.OP_NEQ;
import static com.pelleplutt.plang.AST.OP_OR;
import static com.pelleplutt.plang.AST.OP_PLUS;
import static com.pelleplutt.plang.AST.OP_SHLEFT;
import static com.pelleplutt.plang.AST.OP_SHRIGHT;
import static com.pelleplutt.plang.AST.OP_SYMBOL;
import static com.pelleplutt.plang.AST.OP_XOR;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;
import com.pelleplutt.plang.ModuleFragment.Link;
import com.pelleplutt.plang.ModuleFragment.LinkGoto;
import com.pelleplutt.plang.TAC.TACAlloc;
import com.pelleplutt.plang.TAC.TACArg;
import com.pelleplutt.plang.TAC.TACAssign;
import com.pelleplutt.plang.TAC.TACBkpt;
import com.pelleplutt.plang.TAC.TACCall;
import com.pelleplutt.plang.TAC.TACCode;
import com.pelleplutt.plang.TAC.TACFloat;
import com.pelleplutt.plang.TAC.TACFree;
import com.pelleplutt.plang.TAC.TACGoto;
import com.pelleplutt.plang.TAC.TACGotoCond;
import com.pelleplutt.plang.TAC.TACInt;
import com.pelleplutt.plang.TAC.TACLabel;
import com.pelleplutt.plang.TAC.TACNil;
import com.pelleplutt.plang.TAC.TACOp;
import com.pelleplutt.plang.TAC.TACReturn;
import com.pelleplutt.plang.TAC.TACString;
import com.pelleplutt.plang.TAC.TACVar;
import com.pelleplutt.plang.proc.ByteCode;
import com.pelleplutt.plang.proc.Processor;

public class CodeGenBack implements ByteCode {
  static boolean dbg = false;
  int sp;
  int fp;
  
  public static void compile(List<Module> modules) {
    TAC.dbgResolveRefs = true;
    CodeGenBack cg = new CodeGenBack();
    for (Module m : modules) {
      System.out.println("  * compile module " + m.id);
      cg.compileMod(m);
    }
  }
  
  void compileMod(Module m) {
    for (ModuleFragment frag : m.frags) {
      sp = 0;
      fp = 0;
      System.out.println("  * compile frgmnt " + m.id + frag.name);
      compileFrag(frag);
      byte mc[] = frag.getMachineCode();
      
      if (dbg) {
        PrintStream out = System.out;
        int pc = 0;
        int len = mc.length;
        while (len > 0) {
          String disasm = String.format("0x%08x %s", pc, Processor.disasm(mc, pc)); 
          out.print(disasm);
          String com = frag.commentDbg(pc);
          if (com != null) {
            for (int i = 0; i < (34 - disasm.length()) + 2; i++) out.print(" ");
            out.print("// " + com);
          }
          out.println();
          int instr = (int)(mc[pc] & 0xff);
          int step = ISIZE[instr];
          if (step <= 0) step = 1;
          pc += step;
          len -= step;
        }
      }
    }
  }
  
  
  void addCode(ModuleFragment frag, String comment, int i, Integer... codes) {
    frag.addCode(comment, i, codes);
  }
  void addCode(ModuleFragment frag, int i, Integer... codes) {
    addCode(frag, null, i, codes);
  }
    
  void compileFrag(ModuleFragment frag) {
    // translate to machine code
    Map<TACVar, Integer> locals = new HashMap<TACVar, Integer>();
    frag.locals = locals;
    for (int i = 0; i < frag.tacs.size(); i++) {
      List<TAC> tacBlock = frag.tacs.get(i);
      for (TAC tac : tacBlock) {
        compileTAC(tac, frag);
      }
    }
    if (frag.type == ASTNode.ASTNodeBlok.TYPE_MAIN) {
      addCode(frag, IRET);
    }
    // resolve fragment branches and labels
    for (Link l : frag.links) {
      if (l instanceof LinkGoto) {
        LinkGoto lgoto = (LinkGoto)l;
        int srcgoto = lgoto.pc;
        TACLabel ldstgoto = lgoto.label;
        int dstgoto = frag.labels.get(ldstgoto);
        int branchDelta = dstgoto - srcgoto;
        frag.write(srcgoto + 1, branchDelta, 3);
      } 
    }
  }
  
  void compileTAC(TAC tac, ModuleFragment frag) {
    if (dbg) System.out.println("    " + tac);

    if (tac instanceof TACAlloc) {
      TACAlloc all = (TACAlloc)tac;
      // point out argument variables
      if (all.blok.getArguments() != null) {
        int argc = all.blok.getArguments().size();
        for (ASTNodeSymbol esym : all.blok.getArguments()) {
          frag.locals.put(new TACVar(esym, all.blok), -argc-2); // -2 for pushed frame (pc, fp)
          argc--;
        }
      }
      // allocate stack variables
      if (!all.blok.symList.isEmpty()) {
        int fpoffset = sp - fp;
        for (ASTNodeSymbol esym : all.blok.symList) {
          frag.locals.put(new TACVar(esym, all.blok), fpoffset++);
        }
        sp += all.blok.symList.size();
        addCode(frag, stackInfo() + all.toString(), ISPI, all.blok.symList.size()-1);
      }
    }
    else if (tac instanceof TACFree) {
      // free stack variables
      TACFree fre = (TACFree)tac;
      if (!fre.blok.symList.isEmpty()) {
        for (ASTNodeSymbol esym : fre.blok.symList) {
          frag.locals.remove(new TACVar(esym, fre.blok));
        }
  
        sp -= fre.blok.symList.size();
        addCode(frag, stackInfo() + tac.toString(), ISPD, fre.blok.symList.size()-1);
      }
    }
    else if (tac instanceof TACVar || tac instanceof TACFloat || 
        tac instanceof TACString || tac instanceof TACCode) {
      if (tac.referenced) {
        pushValue(tac, frag);
      }
    }
    else if (tac instanceof TACAssign) {
      TACAssign op = (TACAssign)tac;
      TACVar assignee = (TACVar)op.left;
      TAC assignment = (TAC)op.right;
      if (frag.locals.containsKey(assignee)) {
        int fpoffset = frag.locals.get(assignee);
        pushValue(assignment, frag);
        if (op.referenced) {
          sp++;
          addCode(frag, stackInfo() + op.toString(), IDUP);
        }
        sp--;
        addCode(frag, stackInfo() + op.toString(), ISTF, fpoffset);
      } else {
        pushValue(assignment, frag);
        if (op.referenced) {
          sp++;
          addCode(frag, stackInfo() + op.toString(), IDUP);
        }
        frag.links.add(new ModuleFragment.LinkVar(frag.getPC(), assignee));
        sp--;
        addCode(frag, stackInfo() + op.toString(), ISTI, 0,0,0);
      }
    }
    else if (tac instanceof TACOp) {
      if (AST.isUnaryOperator(((TACOp) tac).op)) {
        compileUnaryOp((TACOp)tac, frag);
      } else {
        compileBinaryOp((TACOp)tac, frag);
      }
    }
    else if (tac instanceof TACLabel) {
      frag.labels.put((TACLabel)tac, frag.getPC());
    }
    else if (tac instanceof TACGoto) {
      frag.links.add(new ModuleFragment.LinkGoto(frag.getPC(), ((TACGoto)tac).label));
      addCode(frag, stackInfo() + "->" + ((TACGoto)tac).label.toString(), IBRA, 0xff,0xff,0xff);
    }
    else if (tac instanceof TACGotoCond) {
      TACGotoCond c = (TACGotoCond)tac;
      pushValue(c.cond, frag);
      boolean inverseCond = !c.positive; 
      int instr = IBRA;
      if (c.cond.e.op == OP_EQ2) instr = inverseCond ? IBRANE : IBRAEQ;
      else if (c.cond.e.op == OP_NEQ) instr = inverseCond ? IBRAEQ : IBRANE;
      else if (c.cond.e.op == OP_GE) instr = inverseCond ? IBRALT : IBRAGE;
      else if (c.cond.e.op == OP_LT) instr = inverseCond ? IBRAGE : IBRALT;
      else if (c.cond.e.op == OP_GT) instr = inverseCond ? IBRALE : IBRAGT;
      else if (c.cond.e.op == OP_LE) instr = inverseCond ? IBRAGT : IBRALE;
      else if (c.cond.e.op == OP_CALL || 
               c.cond.e.op == OP_SYMBOL || 
               AST.isNumber(c.cond.e.op) || 
               AST.isString(c.cond.e.op)) {
        pushValue(new TACInt(c.cond.e, 0), frag);
        instr = IBRAEQ;
      }
      else throw new CompilerError("bad condition " + c.cond + " for " + c +  ", is " + AST.opString(c.cond.e.op));
      sp -= 2;
      addCode(frag, stackInfo() + tac.toString(), ICMP);
      frag.links.add(new ModuleFragment.LinkGoto(frag.getPC(), c.label));
      addCode(frag, stackInfo() + "->" + c.label.toString(), instr, 0xff,0xff,0xff);
    }
    else if (tac instanceof TACCall) {
      TACCall call = (TACCall)tac;
      frag.links.add(new ModuleFragment.LinkCall(frag.getPC(), call));
      sp++;
      addCode(frag, stackInfo() + "<" + call.func + ", " + call.args + " args>", ICALI, 0x03,0x00,0x00);
      if (call.referenced && call.args == 0) {
        // no args, using return value - do naught
      } else {
        sp -= call.args + 1;
        addCode(frag, stackInfo() + "pop arguments and return val", ISPD, call.args-1 + 1); // +1 for the return value, is now at sp[-call.args]
        if (call.referenced) {
          // keep returnvalue if referenced
          sp++;
          addCode(frag, stackInfo() + "move return val to sp top", ICPY, -(call.args+1));
        }
      }
    }
    else if (tac instanceof TACReturn) {
      TACReturn ret = (TACReturn)tac;
      TAC retVal = ret.ret;
      if (retVal == null) {
        sp++;
        addCode(frag, stackInfo() + "return nil", IPU0);
      } else {
        pushValue(retVal, frag);
      }
      sp--;
      addCode(frag, stackInfo(), IRETV);
    }
    else if (tac instanceof TACArg) {
      TACArg a = (TACArg)tac;
      pushValue(a.arg, frag);
    }
    else if (tac instanceof TACBkpt) {
      addCode(frag, stackInfo() + "breakpoint", IBKPT);
    }
    else {
      System.out.println("unknown " + tac);
    }
  }
  
  void compileUnaryOp(TACOp tac, ModuleFragment frag) {
    // TODO
  }
  
  void compileBinaryOp(TACOp tac, ModuleFragment frag) {
    if (tac.op == OP_PLUS) {
      TAC l = tac.left;
      TAC r = tac.right;
      if (l instanceof TACInt && ((TACInt)l).x < 9) {
        pushValue(r, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), IADQ1 + (((TACInt)l).x - 1));
      } else if (r instanceof TACInt && ((TACInt)r).x < 9) {
        pushValue(l, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), IADQ1 + (((TACInt)r).x - 1));
      } else if (l instanceof TACInt && ((TACInt)l).x < 256) {
        pushValue(r, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), IADI, ((TACInt)l).x - 1);
      } else if (r instanceof TACInt && ((TACInt)r).x < 256) {
        pushValue(l, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), IADI, ((TACInt)r).x - 1);
      } else {
        pushValues(l, r, frag);
        sp = sp - 2 + 1;
        addCode(frag, stackInfo() + tac.toString(), IADD);
      }
    }
    else if (tac.op == OP_MINUS) {
      TAC l = tac.left;
      TAC r = tac.right;
      if (l instanceof TACInt && ((TACInt)l).x < 9) {
        pushValue(r, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), ISUQ1 + (((TACInt)l).x - 1));
      } else if (r instanceof TACInt && ((TACInt)r).x < 9) {
        pushValue(l, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), ISUQ1 + (((TACInt)r).x - 1));
      } else if (l instanceof TACInt && ((TACInt)l).x < 256) {
        pushValue(r, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), ISUI, ((TACInt)l).x - 1);
      } else if (r instanceof TACInt && ((TACInt)r).x < 256) {
        pushValue(l, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), ISUI, ((TACInt)r).x - 1);
      } else {
        pushValues(l, r, frag);
        sp = sp - 2 + 1;
        addCode(frag, stackInfo() + tac.toString(), ISUB);
      }
    }
    else if (tac.op == OP_MUL) {
      pushValues(tac.left, tac.right, frag);
      sp = sp - 2 + 1;
      addCode(frag, stackInfo() + tac.toString(), IMUL);
    }
    else if (tac.op == OP_DIV) {
      pushValues(tac.left, tac.right, frag);
      sp = sp - 2 + 1;
      addCode(frag, stackInfo() + tac.toString(), IDIV);
    }
    else if (tac.op == OP_AND) {
      pushValues(tac.left, tac.right, frag);
      sp = sp - 2 + 1;
      addCode(frag, stackInfo() + tac.toString(), IAND);
    }
    else if (tac.op == OP_OR) {
      pushValues(tac.left, tac.right, frag);
      sp = sp - 2 + 1;
      addCode(frag, stackInfo() + tac.toString(), IOR);
    }
    else if (tac.op == OP_XOR) {
      pushValues(tac.left, tac.right, frag);
      sp = sp - 2 + 1;
      addCode(frag, stackInfo() + tac.toString(), IXOR);
    }
    else if (tac.op == OP_SHLEFT) {
      pushValues(tac.left, tac.right, frag);
      sp = sp - 2 + 1;
      addCode(frag, stackInfo() + tac.toString(), ISHL);
    }
    else if (tac.op == OP_SHRIGHT) {
      pushValues(tac.left, tac.right, frag);
      sp = sp - 2 + 1;
      addCode(frag, stackInfo() + tac.toString(), ISHR);
    }
    else if (AST.isConditionalOperator(tac.op)) {
      pushValues(tac.left, tac.right, frag);
    }
    // TODO moar
    else {
      System.out.println("unknown op " + tac);
    }
  }
  
  void pushValues(TAC left, TAC right, ModuleFragment frag) {
    boolean lOnStack = pushValue(left, frag);
    boolean rOnStack = pushValue(right, frag);
    if (rOnStack && !lOnStack) {
      addCode(frag, IROT);
    }
  }
  
  boolean pushValue(TAC a, ModuleFragment frag) {
    if (a instanceof TACNil) {
      sp++;
      addCode(frag, stackInfo() + a.toString(), IPU0);
    } 
    else if (a instanceof TACInt) {
      if (((TACInt)a).x >= -128 && ((TACInt)a).x <= 127) {
        // push immediate
        sp++;
        addCode(frag, stackInfo() + a.toString() + " (constimm)", IPUI, ((TACInt)a).x);
      } else {
        frag.links.add(new ModuleFragment.LinkConst(frag.getPC(), a));
        sp++;
        addCode(frag, stackInfo() + a.toString() + " (const)", ILDI, 1,0,0);
      }
    } 
    else if (a instanceof TACVar) {
      if (frag.locals.containsKey(a)) {
        int fpoffset = frag.locals.get(a);
        sp++;
        addCode(frag, stackInfo() + a.toString() + " (local)", ILDF, fpoffset);
      } else {
        frag.links.add(new ModuleFragment.LinkVar(frag.getPC(), (TACVar)a));
        sp++;
        addCode(frag, stackInfo() + a.toString() + " (var)", ILDI, 0,0,0);
      }
    }
    else if (a instanceof TACFloat || a instanceof TACString || a instanceof TACCode) {
      // TODO RANGE?
      frag.links.add(new ModuleFragment.LinkConst(frag.getPC(), a));
      sp++;
      addCode(frag, stackInfo() + a.toString() + " (const)", ILDI, 2,0,0);
    } 
//    else if (a instanceof TACOp) {
//      // supposed to be on stack TODO
//      //addCode(frag, stackInfo() + "STACKREF:" + a.toString(), INOP);
//      //System.out.println("push val ref " + a);
//    }
    else {
      return true;
    }
    return false;
  }
  
  String stackInfo() {
    return "sp=" + sp + "\t";
  }
}
