package com.pelleplutt.plang;

import static com.pelleplutt.plang.AST.*;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;

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
import com.pelleplutt.plang.TAC.TACUnaryOp;
import com.pelleplutt.plang.TAC.TACUnresolved;
import com.pelleplutt.plang.TAC.TACVar;
import com.pelleplutt.plang.proc.ByteCode;
import com.pelleplutt.plang.proc.Processor;

public class CodeGenBack implements ByteCode {
  static boolean dbg = false;
  int sp;
  int fp;
  
  public static void compile(List<Module> modules) {
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
      System.out.println("  * compile frgmnt " + m.id + frag.fragname);
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
    frag.locals = new HashMap<TACVar, Integer>();
    for (int i = 0; i < frag.tacs.size(); i++) {
      List<TAC> tacBlock = frag.tacs.get(i);
      for (TAC tac : tacBlock) {
        compileTAC(tac, frag);
        //peepholeOptimise(frag);
        // TODO
        // update comments and links and what now -- phew!!
      }
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
      // TODO check arg count - do we want this?
//      if (all.funcEntry) {
//        sp++;
//        addCode(frag, stackInfo() + "given argc", ICPY, 2);
//        sp++;
//        addCode(frag, stackInfo() + "expected argc", IPUI, all.args.size());
//        sp -= 2;
//        addCode(frag, stackInfo(), ICMP);
//        addCode(frag, IBRAEQ, 0,0,5);
//        addCode(frag, IBKPT); // TODO not bkpt, but raise exception or something
//      }
      
      // point out argument variables
      if (!all.args.isEmpty()) {
        int argix = 0;
        for (String sym : all.args) {
          frag.locals.put(new TACVar(tac.getNode(), sym, all.module, all.scope), -argix-4); // -4 for pushed frame (args, pc, fp)
          argix++;
        }
      }
      // allocate stack variables
      if (!all.vars.isEmpty()) {
        int fpoffset = sp - fp;
        for (String sym : all.vars) {
          frag.locals.put(new TACVar(tac.getNode(), sym, all.module, all.scope), fpoffset++);
        }
        sp += all.vars.size();
        addCode(frag, stackInfo() + all.toString(), ISPI, all.vars.size()-1);
      }
    }
    else if (tac instanceof TACFree) {
      // free stack variables
      TACFree fre = (TACFree)tac;
      if (!fre.vars.isEmpty()) {
        for (String esym : fre.vars) {
          frag.locals.remove(new TACVar(tac.getNode(), esym, fre.module, fre.scope));
        }
        sp -= fre.vars.size();
        addCode(frag, stackInfo() + tac.toString(), ISPD, fre.vars.size()-1);
      }
    }
    else if (tac instanceof TACVar || tac instanceof TACFloat || 
        tac instanceof TACString || tac instanceof TACCode || tac instanceof TACUnresolved) {
      if (tac.referenced) {
        pushValue(tac, frag);
      }
    }
    else if (tac instanceof TACAssign) {
      TACAssign op = (TACAssign)tac;
      emitAssignment(op, (TACVar)op.left, op.right, op.referenced, frag);
    }
    else if (tac instanceof TACUnaryOp) {
      compileUnaryOp((TACUnaryOp)tac, frag);
    }
    else if (tac instanceof TACOp) {
      compileBinaryOp((TACOp)tac, frag);
    }
    else if (tac instanceof TACLabel) {
      frag.labels.put((TACLabel)tac, frag.getPC());
    }
    else if (tac instanceof TACGoto) {
      frag.links.add(new ModuleFragment.LinkGoto(frag.getPC(), ((TACGoto)tac).label));
      addCode(frag, stackInfo() + "->" + ((TACGoto)tac).label.toString(), IBRA, 0xff,0xff,0xff);
    }
    else if (tac instanceof TACGotoCond) {
      int cmpInstr = ICMP;
      TACGotoCond c = (TACGotoCond)tac;
      pushValue(c.cond, frag);
      boolean inverseCond = !c.positive; 
      int braInstr = IBRA;
      if (c.condOp == OP_EQ2) braInstr = inverseCond ? IBRANE : IBRAEQ;
      else if (c.condOp == OP_NEQ) braInstr = inverseCond ? IBRAEQ : IBRANE;
      else if (c.condOp == OP_GE) braInstr = inverseCond ? IBRALT : IBRAGE;
      else if (c.condOp == OP_LT) braInstr = inverseCond ? IBRAGE : IBRALT;
      else if (c.condOp == OP_GT) braInstr = inverseCond ? IBRALE : IBRAGT;
      else if (c.condOp == OP_LE) braInstr = inverseCond ? IBRAGT : IBRALE;
      else if (c.condOp == OP_CALL || 
               c.condOp == OP_SYMBOL || 
               c.condOp == OP_EQ || AST.isAssignOperator(c.condOp) ||
               AST.isNumber(c.condOp) || 
               AST.isString(c.condOp) ||
               AST.isUnaryOperator(c.condOp)
               ) {
        //pushValue(new TACInt(c.cond.getNode(), 0), frag);
        cmpInstr = ICMP0;
        braInstr = IBRAEQ;
      }
      else {
        throw new CompilerError("bad condition '" + c.cond + "' for '" + c +  "', is '" + AST.opString(c.condOp)+"'", c.getNode());
      }
      sp -= cmpInstr == ICMP0 ? 1 : 2;
      addCode(frag, stackInfo() + tac.toString(), cmpInstr);
      frag.links.add(new ModuleFragment.LinkGoto(frag.getPC(), c.label));
      addCode(frag, stackInfo() + "->" + c.label.toString(), braInstr, 0xff,0xff,0xff);
    }
    else if (tac instanceof TACCall) {
      TACCall call = (TACCall)tac;
      // push nbr of args
      pushNumber(frag, call.args, "argc, replaced by retval");
      if (!call.link) {
        // func is a local variable
        pushValue(call.var, frag);
        sp = sp - 1          // read call address
             - 1 - call.args // return, pop args and argc
             + 1;            // retval
        addCode(frag, stackInfo() + "<" + call.func + ", " + call.args + " args>", ICAL);
      } else {
        // func is a function name
        frag.links.add(new ModuleFragment.LinkCall(frag.getPC(), call));
        sp = sp 
            - 1 - call.args // return, pop args and argc
            + 1;            // retval
        addCode(frag, stackInfo() + "<" + call.func + ", " + call.args + " args>", ICALI, 0x03,0x00,0x00);
      }
      if (!call.referenced) {
        sp--;
        addCode(frag, stackInfo() + "unused retval", IPOP);
      }
    }
    else if (tac instanceof TACReturn) {
      TACReturn ret = (TACReturn)tac;
      if (ret.emptyReturn) {
        sp++;
        addCode(frag, stackInfo() + "return nil", IPUNIL);
      } else {
        pushValue(ret.ret, frag);
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
      throw new Error("not implemented " + tac);
    }
  }
  
  void compileUnaryOp(TACUnaryOp tac, ModuleFragment frag) {
    if (tac.op == OP_PREINC) {
      pushValue(tac.operand, frag);
      sp = sp - 1 + 1;
      addCode(frag, stackInfo() + tac.toString(), IADQ1);
      if (tac.operand instanceof TACVar) {
        emitAssignment(tac, (TACVar)tac.operand, null, tac.referenced, frag);
      }
    } 
    else if (tac.op == OP_PREDEC) {
      pushValue(tac.operand, frag);
      sp = sp - 1 + 1;
      addCode(frag, stackInfo() + tac.toString(), ISUQ1);
      if (tac.operand instanceof TACVar) {
        emitAssignment(tac, (TACVar)tac.operand, null, tac.referenced, frag);
      }
    } 
    else if (tac.op == OP_POSTINC) {
      pushValue(tac.operand, frag);
      if (tac.referenced) {
        sp++;
        addCode(frag, IDUP);
      }
      sp = sp - 1 + 1;
      addCode(frag, stackInfo() + tac.toString(), IADQ1);
      if (tac.operand instanceof TACVar) {
        emitAssignment(tac, (TACVar)tac.operand, null, false, frag);
      }
    } 
    else if (tac.op == OP_POSTDEC) {
      pushValue(tac.operand, frag);
      if (tac.referenced) {
        sp++;
        addCode(frag, IDUP);
      }
      sp = sp - 1 + 1;
      addCode(frag, stackInfo() + tac.toString(), ISUQ1);
      if (tac.operand instanceof TACVar) {
        emitAssignment(tac, (TACVar)tac.operand, null, false, frag);
      }
    } 
    else if (tac.op == OP_LNOT) {
      pushValue(tac.operand, frag);
      sp = sp - 1 + 1;
      addCode(frag, stackInfo() + tac.toString(), ILNOT);
    } 
    else if (tac.op == OP_BNOT) {
      pushValue(tac.operand, frag);
      sp = sp - 1 + 1;
      addCode(frag, stackInfo() + tac.toString(), INOT);
    } 
    else if (tac.op == OP_MINUS_UNARY) {
      pushValue(tac.operand, frag);
      sp = sp - 1 + 1;
      addCode(frag, stackInfo() + tac.toString(), INEG);
    } 
    else if (tac.op == OP_PLUS_UNARY) {
    }
    else {
      throw new Error("not implemented " + tac);
    }
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
    else if (tac.op == OP_MOD) {
      pushValues(tac.left, tac.right, frag);
      sp = sp - 2 + 1;
      addCode(frag, stackInfo() + tac.toString(), IREM);
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
      throw new Error("not implemented " + tac);
    }
  }
  
  // if assignment is already on stack, set assignment argument to null
  void emitAssignment(TAC op, TACVar assignee, TAC assignment, boolean referenced, ModuleFragment frag) {
    if (frag.locals.containsKey(assignee)) {
      // local value
      int fpoffset = frag.locals.get(assignee);
      pushValue(assignment, frag);
      if (referenced) {
        sp++;
        addCode(frag, stackInfo() + op.toString(), IDUP);
      }
      sp--;
      addCode(frag, stackInfo() + op.toString(), ISTF, fpoffset);
    } else {
      // global value
      pushValue(assignment, frag);
      if (referenced) {
        sp++;
        addCode(frag, stackInfo() + op.toString(), IDUP);
      }
      frag.links.add(new ModuleFragment.LinkGlobal(frag.getPC(), assignee));
      sp--;
      addCode(frag, stackInfo() + op.toString(), ISTI, 0,0,0);
    }
  }
  
  void pushValues(TAC left, TAC right, ModuleFragment frag) {
    boolean lOnStack = pushValue(left, frag);
    boolean rOnStack = pushValue(right, frag);
    if (rOnStack && !lOnStack) {
      addCode(frag, ISWP);
    }
  }
  
  void pushNumber(ModuleFragment frag, int num, String comment) {
    sp++;
    if (num >= 0 && num < 4) {
      addCode(frag, comment != null ? (stackInfo() + comment) : null, IPU0 + num);
    } else if (num >= -128 && num <= 127) {
      addCode(frag, comment != null ? (stackInfo() + comment) : null, IPUI, num);
    } else {
      throw new CompilerError("not implemented");
    }

  }
  
  boolean pushValue(TAC a, ModuleFragment frag) {
    if (a instanceof TACNil) {
      sp++;
      addCode(frag, stackInfo() + a.toString(), IPUNIL);
    } 
    else if (a instanceof TACInt) {
      if (((TACInt)a).x >= 0 && ((TACInt)a).x < 4) {
        sp++;
        addCode(frag, stackInfo() + a.toString(), IPU0 + ((TACInt)a).x);
      } else if (((TACInt)a).x >= -128 && ((TACInt)a).x <= 127) {
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
        // local variable
        int fpoffset = frag.locals.get(a);
        sp++;
        addCode(frag, stackInfo() + a.toString() + " (local)", ILDF, fpoffset);
      } else {
        // global variable
        frag.links.add(new ModuleFragment.LinkGlobal(frag.getPC(), (TACVar)a));
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
    else if (a instanceof TACUnresolved) {
      frag.links.add(new ModuleFragment.LinkUnresolved(frag.getPC(), (TACUnresolved)a));
      sp++;
      addCode(frag, stackInfo() + a.toString() + " (link)", IPUNIL, INOP,INOP,INOP);
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
