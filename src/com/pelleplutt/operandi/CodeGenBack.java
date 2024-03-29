package com.pelleplutt.operandi;

import static com.pelleplutt.operandi.AST.OP_AND;
import static com.pelleplutt.operandi.AST.OP_BNOT;
import static com.pelleplutt.operandi.AST.OP_CALL;
import static com.pelleplutt.operandi.AST.OP_DIV;
import static com.pelleplutt.operandi.AST.OP_EQ;
import static com.pelleplutt.operandi.AST.OP_EQ2;
import static com.pelleplutt.operandi.AST.OP_GE;
import static com.pelleplutt.operandi.AST.OP_GT;
import static com.pelleplutt.operandi.AST.OP_IN;
import static com.pelleplutt.operandi.AST.OP_LE;
import static com.pelleplutt.operandi.AST.OP_LNOT;
import static com.pelleplutt.operandi.AST.OP_LT;
import static com.pelleplutt.operandi.AST.OP_MINUS;
import static com.pelleplutt.operandi.AST.OP_MINUS_UNARY;
import static com.pelleplutt.operandi.AST.OP_MOD;
import static com.pelleplutt.operandi.AST.OP_MUL;
import static com.pelleplutt.operandi.AST.OP_NEQ;
import static com.pelleplutt.operandi.AST.OP_OR;
import static com.pelleplutt.operandi.AST.OP_PLUS;
import static com.pelleplutt.operandi.AST.OP_PLUS_UNARY;
import static com.pelleplutt.operandi.AST.OP_POSTDEC;
import static com.pelleplutt.operandi.AST.OP_POSTINC;
import static com.pelleplutt.operandi.AST.OP_PREDEC;
import static com.pelleplutt.operandi.AST.OP_PREINC;
import static com.pelleplutt.operandi.AST.OP_SHLEFT;
import static com.pelleplutt.operandi.AST.OP_SHRIGHT;
import static com.pelleplutt.operandi.AST.OP_SYMBOL;
import static com.pelleplutt.operandi.AST.OP_XOR;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;

import com.pelleplutt.operandi.ASTNode.ASTNodeBlok;
import com.pelleplutt.operandi.ModuleFragment.Link;
import com.pelleplutt.operandi.ModuleFragment.LinkGoto;
import com.pelleplutt.operandi.TAC.TACAlloc;
import com.pelleplutt.operandi.TAC.TACArgNbr;
import com.pelleplutt.operandi.TAC.TACArgc;
import com.pelleplutt.operandi.TAC.TACArgv;
import com.pelleplutt.operandi.TAC.TACArrEntry;
import com.pelleplutt.operandi.TAC.TACArrInit;
import com.pelleplutt.operandi.TAC.TACAssign;
import com.pelleplutt.operandi.TAC.TACBkpt;
import com.pelleplutt.operandi.TAC.TACCall;
import com.pelleplutt.operandi.TAC.TACCode;
import com.pelleplutt.operandi.TAC.TACDefineMe;
import com.pelleplutt.operandi.TAC.TACFloat;
import com.pelleplutt.operandi.TAC.TACFree;
import com.pelleplutt.operandi.TAC.TACFuncArg;
import com.pelleplutt.operandi.TAC.TACGetMe;
import com.pelleplutt.operandi.TAC.TACGoto;
import com.pelleplutt.operandi.TAC.TACGotoCond;
import com.pelleplutt.operandi.TAC.TACInt;
import com.pelleplutt.operandi.TAC.TACLabel;
import com.pelleplutt.operandi.TAC.TACMap;
import com.pelleplutt.operandi.TAC.TACMapTuple;
import com.pelleplutt.operandi.TAC.TACNil;
import com.pelleplutt.operandi.TAC.TACOp;
import com.pelleplutt.operandi.TAC.TACRange;
import com.pelleplutt.operandi.TAC.TACRangeLen;
import com.pelleplutt.operandi.TAC.TACReturn;
import com.pelleplutt.operandi.TAC.TACSet;
import com.pelleplutt.operandi.TAC.TACSetDeref;
import com.pelleplutt.operandi.TAC.TACSetRead;
import com.pelleplutt.operandi.TAC.TACString;
import com.pelleplutt.operandi.TAC.TACUnaryOp;
import com.pelleplutt.operandi.TAC.TACUndefineMe;
import com.pelleplutt.operandi.TAC.TACUnresolved;
import com.pelleplutt.operandi.TAC.TACVar;
import com.pelleplutt.operandi.proc.Assembler;
import com.pelleplutt.operandi.proc.ByteCode;
import com.pelleplutt.operandi.proc.MSet;
import com.pelleplutt.operandi.proc.Processor;

public class CodeGenBack implements ByteCode {
  public static boolean dbg = false;
  int sp;
  int fp;
  
  public static void compile(IntermediateRepresentation ir) {
    List<Module> modules = ir.getModules();
    CodeGenBack cg = new CodeGenBack();
    for (Module m : modules) {
      if (m.compiled) continue;
      if (dbg) System.out.println("  * compile module " + m.id);
      cg.compileMod(m);
      m.compiled = true;
    }
  }
  
  void compileMod(Module m) {
    for (ModuleFragment frag : m.frags) {
      sp = 0;
      fp = 0;
      if (dbg) System.out.println("  * compile frgmnt " + m.id + frag.fragname);
      compileFrag(frag);
      byte mc[] = frag.getMachineCode();
      
      if (dbg) {
        PrintStream out = System.out;
        int pc = 0;
        int len = mc.length;
        while (len > 0) {
          String disasm = String.format("0x%08x %s", pc, Assembler.disasm(mc, pc)); 
          out.print(disasm);
          String com = frag.instructionDbg(pc);
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
  
  ASTNode curNode;
  void addCode(ModuleFragment frag, String comment, int i, Integer... codes) {
    frag.addCode(comment, curNode, i, codes);
  }
  void addCode(ModuleFragment frag, int i, Integer... codes) {
    addCode(frag, null, i, codes);
  }
    
  void compileFrag(ModuleFragment frag) {
    // translate to machine code
    frag.locals = new HashMap<TACVar, Integer>();
    for (int i = 0; i < frag.getTACBlocks().size(); i++) {
      List<TAC> tacBlock = frag.getTACBlocks().get(i);
      for (TAC tac : tacBlock) {
        compileTAC(tac, frag);
        //peepholeOptimise(frag);
        // TODO
        // update comments and links and what not -- phew!!
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
  
  void generateIsType(TACCall call, ModuleFragment frag, String type, int typeInt){
    sp++;
    addCode(frag, stackInfo() + "is" + type, IGET_TYP);
    pushNumber(frag, typeInt, type + " type");
    sp -= 2;
    addCode(frag, stackInfo(), ICMP);
    sp++;
    if (typeInt == Processor.TSET) {
      addCode(frag, stackInfo(), IPUSH_GT); // if stack type > TSET
    } else {
      addCode(frag, stackInfo(), IPUSH_EQ);
    }
  }
  
  boolean generateInbuiltFunction(TACCall call, ModuleFragment frag) {
    if (dbg) System.out.println("    GEN_INBUILT_FUNC");
    if (call.func.equals(INBUILTFN_TOSTR)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      addCode(frag, stackInfo() + "tostr", ICAST_S);
      return true;
    } 
    else if (call.func.equals(INBUILTFN_TOINT)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      addCode(frag, stackInfo() + "toint", ICAST_I);
      return true;
    } 
    else if (call.func.equals(INBUILTFN_TOFLOAT)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      addCode(frag, stackInfo() + "tofloat", ICAST_F);
      return true;
    } 
    else if (call.func.equals(INBUILTFN_TOCHAR)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      addCode(frag, stackInfo() + "tochar", ICAST_CH);
      return true;
    }  
    else if (call.func.equals(INBUILTFN_ISNIL)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      generateIsType(call, frag, "nil", Processor.TNIL);
      return true;
    }  
    else if (call.func.equals(INBUILTFN_ISINT)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      generateIsType(call, frag, "int", Processor.TINT);
      return true;
    }  
    else if (call.func.equals(INBUILTFN_ISFLOAT)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      generateIsType(call, frag, "float", Processor.TFLOAT);
      return true;
    }  
    else if (call.func.equals(INBUILTFN_ISSTR)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      generateIsType(call, frag, "str", Processor.TSTR);
      return true;
    }  
    else if (call.func.equals(INBUILTFN_ISFUNC)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      generateIsType(call, frag, "func", Processor.TFUNC);
      return true;
    }  
    else if (call.func.equals(INBUILTFN_ISANON)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      generateIsType(call, frag, "anon", Processor.TANON);
      return true;
    }  
    else if (call.func.equals(INBUILTFN_ISSET)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      generateIsType(call, frag, "set", Processor.TSET);
      return true;
    }  
    else if (call.func.equals(INBUILTFN_ISMAP)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      generateIsType(call, frag, "map", MSet.TMAP);
      return true;
    }  
    else if (call.func.equals(INBUILTFN_ISARR)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      generateIsType(call, frag, "arr", MSet.TARR);
      return true;
    }  
    else if (call.func.equals(INBUILTFN_ISTUP)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      generateIsType(call, frag, "tup", MSet.TTUP);
      return true;
    }  
    else if (call.func.equals(INBUILTFN_TYPEOF)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      sp = sp - 1 + 1;
      addCode(frag, stackInfo() + "typeof", IGET_TYP);
      return true;
    }  
    else if (call.func.equals(INBUILTFN_LEN)) {
      if (call.args != 1) throw new CompilerError("bad number of arguments", call.getNode());
      sp = sp - 1 + 1;
      addCode(frag, stackInfo() + "length", ISET_SZ);
      return true;
    }
    return false;
  }
  
  void compileTAC(TAC tac, ModuleFragment frag) {
    if (dbg) System.out.println("  " + (tac.referenced ? "r " : "  ") + tac);
    curNode = tac.getNode();

    
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
      if (!all.getArguments().isEmpty()) {
        int argix = 0;
        for (String sym : all.getArguments()) {
          frag.locals.put(new TACVar(tac.getNode(), sym, all.module, null, all.scope), -argix-FRAME_SIZE-1);
          argix++;
        }
      }
      // allocate stack variables
      if (all.variablesOnStack() + all.countADSVars() > 0) {
        int fpoffset = sp - fp;
        for (String sym : all.getAnonymousDefinedScopeVariables()) {
          TACVar v = new TACVar(tac.getNode(), sym, all.module, null, all.scope);
          frag.locals.put(v, fpoffset++);
        }
        for (String sym : all.getLocalVariables()) {
          TACVar v = new TACVar(tac.getNode(), sym, all.module, null, all.scope);
          frag.locals.put(v, fpoffset++);
        }
        for (String sym : all.getInternalVariables()) {
          TACVar v = new TACVar(tac.getNode(), sym, all.module, null, all.scope + all.tScope);
          frag.locals.put(v, fpoffset++);
        }
        sp += all.countADSVars();
        if (all.variablesOnStack() > 0) {
          sp += all.variablesOnStack();
          addCode(frag, stackInfo() + all.toString(), ISP_INCR, all.variablesOnStack()-1);
        }
      }
    }
    else if (tac instanceof TACFree) {
      // free stack variables
      TACFree fre = (TACFree)tac;
      if (fre.variablesOnStack() > 0) {
        for (String sym : fre.getAnonymousDefinedScopeVariables()) {
          frag.locals.remove(new TACVar(tac.getNode(), sym, fre.module, null, fre.scope));
        }
        for (String esym : fre.getLocalVariables()) {
          frag.locals.remove(new TACVar(tac.getNode(), esym, fre.module, null, fre.scope));
        }
        for (String esym : fre.getInternalVariables()) {
          frag.locals.remove(new TACVar(tac.getNode(), esym, fre.module, null, fre.scope + fre.tScope));
        }
        sp -= fre.variablesOnStack();
        addCode(frag, stackInfo() + tac.toString(), ISP_DECR, fre.variablesOnStack()-1);
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
      emitAssignment(op, op.left, op.right, op.referenced, frag);
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
      boolean cmp0 = false;
      TACGotoCond c = (TACGotoCond)tac;
      boolean inverseCond = !c.positive; 
      int braInstr = IBRA;
      if (c.condOp == OP_EQ2) braInstr = inverseCond ? IBRA_NE : IBRA_EQ;
      else if (c.condOp == OP_NEQ) braInstr = inverseCond ? IBRA_EQ : IBRA_NE;
      else if (c.condOp == OP_GE) braInstr = inverseCond ? IBRA_LT : IBRA_GE;
      else if (c.condOp == OP_LT) braInstr = inverseCond ? IBRA_GE : IBRA_LT;
      else if (c.condOp == OP_GT) braInstr = inverseCond ? IBRA_LE : IBRA_GT;
      else if (c.condOp == OP_LE) braInstr = inverseCond ? IBRA_GT : IBRA_LE;
      else if (c.condOp == OP_CALL || 
               c.condOp == OP_SYMBOL || 
               c.condOp == OP_EQ || 
               AST.isAssignOperator(c.condOp) ||
               AST.isNumber(c.condOp) || 
               AST.isString(c.condOp) ||
               AST.isAdditiveOperator(c.condOp) ||
               AST.isMultiplicativeOperator(c.condOp) ||
               AST.isLogicalOperator(c.condOp) ||
               AST.isUnaryOperator(c.condOp)
               ) {
        cmp0 = true;
        braInstr = IBRA_EQ;
      } else if (c.condOp == OP_IN) {
        cmp0 = false;
        braInstr = IBRA_NE;
      }
      else {
        throw new CompilerError("bad condition '" + c.cond + "' for '" + c +  "', is '" + AST.opString(c.condOp)+"'", c.getNode());
      }
      if (cmp0) {
        pushValue(c.cond, frag);
        sp--;
        addCode(frag, stackInfo() + tac.toString(), ICMP_0);
      }
      frag.links.add(new ModuleFragment.LinkGoto(frag.getPC(), c.label));
      addCode(frag, stackInfo() + "->" + c.label.toString(), braInstr, 0xff,0xff,0xff);
    }
    else if (tac instanceof TACCall) {
      emitCall((TACCall)tac, frag);
    }
    else if (tac instanceof TACReturn) {
      TACReturn ret = (TACReturn)tac;
      if (ret.emptyReturn) {
        sp++;
        addCode(frag, stackInfo() + "return nil", IPUSH_NIL);
      } else {
        pushValue(ret.ret, frag);
      }
      sp--;
      addCode(frag, stackInfo(), IRETV);
    }
    else if (tac instanceof TACFuncArg) {
      TACFuncArg a = (TACFuncArg)tac;
      pushValue(a.arg, frag);
    }
    else if (tac instanceof TACBkpt) {
      addCode(frag, stackInfo() + "breakpoint", IBKPT);
    }
    else if (tac instanceof TACSet) {
      TACSet a = (TACSet)tac;
      pushValue(new TACInt(a.getNode(), a.entries), frag);
      sp -= a.entries;
      addCode(frag, stackInfo(), ISET_CRE);
    }
    else if (tac instanceof TACArrInit) {
      TACArrInit a = (TACArrInit)tac;
      pushValue(new TACInt(a.getNode(), a.entries.size()), frag);
      frag.links.add(new ModuleFragment.LinkArrayInitializer(frag.getPC(), a));
      addCode(frag, stackInfo() + a, IARR_CRE, 4,0,0);
    }
    else if (tac instanceof TACArrEntry) {
      TACArrEntry a = (TACArrEntry)tac;
      pushValue(a.entry, frag);
    }
    else if (tac instanceof TACMap) {
      TACMap a = (TACMap)tac;
      sp++;
      addCode(frag, stackInfo(), IPUSH_0);
      addCode(frag, stackInfo() + a, ISET_CRE);
    }
    else if (tac instanceof TACMapTuple) {
      TACMapTuple tup = (TACMapTuple)tac;
      pushValue(tup.val, frag);
      pushValue(tup.key, frag);
      sp -= 2;
      addCode(frag, stackInfo() + tac, IMAP_ADD);
    }
    else if (tac instanceof TACSetDeref) {
      TACSetDeref a = (TACSetDeref)tac;
      pushValue(a.derefVal, frag);
      sp = sp - 2 + 1;
      addCode(frag, stackInfo() + a, ISET_DRF);
      if (!a.referenced) {
        sp--;
        addCode(frag, stackInfo() + "derefed val not used", IPOP);
      }
        
    }
    else if (tac instanceof TACSetRead) {
      TACSetRead a = (TACSetRead)tac;
      pushValue(a.derefIx, frag);
      sp = sp - 2 + 1;
      addCode(frag, stackInfo() + a, ISET_RD);
    }
    else if (tac instanceof TACRange) {
      TACRange a = (TACRange)tac;
      if (a.stepDefined) {
        sp = sp - 3 + 1;
        addCode(frag, stackInfo() + a, IRNG3);
      } else {
        sp = sp - 2 + 1;
        addCode(frag, stackInfo() + a, IRNG2);
      }
    }
    else if (tac instanceof TACInt) {
      pushValue(tac, frag);
    }
    else if (tac instanceof TACArgv) {
      pushValue(tac, frag);
    }
    else if (tac instanceof TACArgc) {
      pushValue(tac, frag);
    }
    else if (tac instanceof TACArgNbr) {
      pushValue(tac, frag);
    }
    else if (tac instanceof TACRangeLen) {
      pushValue(tac, frag);
    }
    else if (tac instanceof TACNil) {
      if (tac.referenced) pushValue(tac, frag);
    }
    else if (tac instanceof TACDefineMe) {
      addCode(frag, stackInfo() + "define me (banked)", IDEF_ME);
    }
    else if (tac instanceof TACUndefineMe) {
      addCode(frag, stackInfo() + "undefine me (banked)", IUDEF_ME);
    }
    else if (tac instanceof TACGetMe) {
      sp++;
      addCode(frag, stackInfo() + "get me", IPUSH_ME);
    }

    else {
      throw new CompilerError("not implemented " + tac + " " + tac.getClass().getSimpleName() + " "+ AST.opString(tac.getNode().op), tac.getNode());
    }
  }
  
  void compileUnaryOp(TACUnaryOp tac, ModuleFragment frag) {
    if (dbg) System.out.println("    COMPILE UNARY");
    if (tac.op == OP_PREINC) {
      pushValue(tac.operand, frag);
      sp = sp - 1 + 1;
      addCode(frag, stackInfo() + tac.toString(), IADD_Q1);
      emitAssignment(tac, tac.operand, null, tac.referenced, frag);
    } 
    else if (tac.op == OP_PREDEC) {
      pushValue(tac.operand, frag);
      sp = sp - 1 + 1;
      addCode(frag, stackInfo() + tac.toString(), ISUB_Q1);
      emitAssignment(tac, tac.operand, null, tac.referenced, frag);
    } 
    else if (tac.op == OP_POSTINC) {
      pushValue(tac.operand, frag);
      if (tac.referenced) {
        sp++;
        addCode(frag, IDUP);
      }
      sp = sp - 1 + 1;
      addCode(frag, stackInfo() + tac.toString(), IADD_Q1);
      // false instead of tac.referenced, DUP already emitted
      emitAssignment(tac, tac.operand, null, false, frag);
    } 
    else if (tac.op == OP_POSTDEC) {
      pushValue(tac.operand, frag);
      if (tac.referenced) {
        sp++;
        addCode(frag, IDUP);
      }
      sp = sp - 1 + 1;
      addCode(frag, stackInfo() + tac.toString(), ISUB_Q1);
      // false instead of tac.referenced, DUP already emitted
      emitAssignment(tac, tac.operand, null, false, frag);
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
    if (dbg) System.out.println("    COMPILE BINARY");
    if (tac.op == OP_PLUS) {
      TAC l = tac.left;
      TAC r = tac.right;
      if (l instanceof TACInt && ((TACInt)l).x < 9 && ((TACInt)l).x > 0) {
        pushValue(r, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), IADD_Q1 + (((TACInt)l).x - 1));
      } else if (r instanceof TACInt && ((TACInt)r).x < 9 && ((TACInt)r).x > 0) {
        pushValue(l, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), IADD_Q1 + (((TACInt)r).x - 1));
      } else if (l instanceof TACInt && ((TACInt)l).x < 256 && ((TACInt)l).x > 0) {
        pushValue(r, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), IADD_IM, ((TACInt)l).x - 1);
      } else if (r instanceof TACInt && ((TACInt)r).x < 256 && ((TACInt)r).x > 0) {
        pushValue(l, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), IADD_IM, ((TACInt)r).x - 1);
      } else {
        pushValues(l, r, frag);
        sp = sp - 2 + 1;
        addCode(frag, stackInfo() + tac.toString(), IADD);
      }
    }
    else if (tac.op == OP_MINUS) {
      TAC l = tac.left;
      TAC r = tac.right;
      if (l instanceof TACInt && ((TACInt)l).x < 9 && ((TACInt)l).x > 0) {
        pushValue(r, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), INEG);
        addCode(frag, stackInfo() + tac.toString(), IADD_Q1 + (((TACInt)l).x - 1));
      } else if (r instanceof TACInt && ((TACInt)r).x < 9 && ((TACInt)r).x > 0) {
        pushValue(l, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), ISUB_Q1 + (((TACInt)r).x - 1));
      } else if (l instanceof TACInt && ((TACInt)l).x < 256  && ((TACInt)l).x > 0) {
        pushValue(r, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), INEG);
        addCode(frag, stackInfo() + tac.toString(), IADD_IM, ((TACInt)l).x - 1);
      } else if (r instanceof TACInt && ((TACInt)r).x < 256 && ((TACInt)r).x > 0) {
        pushValue(l, frag);
        sp = sp - 1 + 1;
        addCode(frag, stackInfo() + tac.toString(), ISUB_IM, ((TACInt)r).x - 1);
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
      addCode(frag, stackInfo() + tac.toString(), ISHIFTL);
    }
    else if (tac.op == OP_SHRIGHT) {
      pushValues(tac.left, tac.right, frag);
      sp = sp - 2 + 1;
      addCode(frag, stackInfo() + tac.toString(), ISHIFTR);
    }
    else if (AST.isConditionalOperator(tac.op)) {
      pushValues(tac.left, tac.right, frag);
      sp = sp - 2;
      if (tac.op == OP_IN) {
        addCode(frag, stackInfo() + tac.toString(), IIN);
      } else {
        addCode(frag, stackInfo() + tac.toString(), ICMP);
      }
      TACOp tacop = (TACOp)tac;
      if (tacop.referenced) {
        // push 1 if conditional is true, push 0 if conditional is false
        sp++;
        int instr = IPUSH_0;
        if (tacop.op == OP_EQ2) instr = IPUSH_EQ;
        else if (tacop.op == OP_NEQ) instr = IPUSH_NE;
        else if (tacop.op == OP_GE) instr = IPUSH_GE;
        else if (tacop.op == OP_LT) instr = IPUSH_LT;
        else if (tacop.op == OP_GT) instr = IPUSH_GT;
        else if (tacop.op == OP_LE) instr = IPUSH_LE;
        else if (tacop.op == OP_IN) instr = IPUSH_EQ;
        addCode(frag, stackInfo() + "push cond " + tac, instr);
      }
    }
    // TODO moar
    else {
      throw new CompilerError("not implemented " + tac, tac.getNode());
    }
  }
  
  // if assignment is already on stack, set assignment argument to null
  void emitAssignment(TAC op, TAC assignee, TAC assignment, boolean referenced, ModuleFragment frag) {
    if (dbg) System.out.println("    EMIT ASSIGNMENT");
    if (assignee instanceof TACVar) {
      if (frag.locals.containsKey(assignee)) {
        // local value
        int fpoffset = frag.locals.get(assignee);
        pushValue(assignment, frag);
        if (referenced) {
          sp++;
          addCode(frag, stackInfo() + op.toString(), IDUP);
        }
        sp--;
        addCode(frag, stackInfo() + op.toString() + " (local)", ISTOR_FP, fpoffset);
      } else {
        // global value
        pushValue(assignment, frag);
        if (referenced) {
          sp++;
          addCode(frag, stackInfo() + op.toString(), IDUP);
        }
        frag.links.add(new ModuleFragment.LinkGlobal(frag.getPC(), (TACVar)assignee));
        sp--;
        addCode(frag, stackInfo() + op.toString() + " (global)", ISTOR_IM, 0,0,0);
      }
    } else if (assignee instanceof TACSetDeref) {
      TACSetDeref de = (TACSetDeref)assignee;
      pushValues(de.derefVal, assignment, frag);
      sp += -3 + 1;
      addCode(frag, stackInfo() + op.toString(), ISET_WR);
      if (referenced) {
        sp++;
        addCode(frag, stackInfo() + op.toString(), IDUP);
      }
      if (de.set instanceof TACSetDeref || de.set instanceof TACArgNbr || 
          de.set instanceof TACGetMe || de.set instanceof TACCall) {
        sp--;
        addCode(frag, stackInfo() + "nested deref write", IPOP);
      } else {
        emitAssignment(op, de.set, null, false, frag);
      }
    } else if (assignee instanceof TACUnresolved) {
      pushValue(assignment, frag);
      frag.links.add(new ModuleFragment.LinkUnresolved(frag.getPC(), (TACUnresolved)assignee));
      sp--;
      addCode(frag, stackInfo() + assignee.toString() + " (link)", ISTOR_IM, INOP,INOP,INOP);
    } else {
      throw new CompilerError("not implemented " + assignee.getClass().getName(), op.getNode());
    }
  }
  
  void emitCall(TACCall call, ModuleFragment frag) {
    if (dbg) System.out.println("    EMIT CALL");
    if (!call.funcNameDefined) {
      // func address is pushed by previous operation
      // push nbr of args
      pushNumber(frag, call.args, "argc, replaced by retval");
      addCode(frag, ISWAP);
      // func address is on stack
      sp = sp - 1          // read call address
          - 1 - call.args  // return, pop args and argc
          + 1;             // retval
      addCode(frag, stackInfo() + "<_stack_addr_, " + call.args + " args>", ICALL);
    } else if (call.funcAddrInVar) {
      // func is a variable func pointer, no need for linking
      // push nbr of args
      pushNumber(frag, call.args, "argc, replaced by retval");
      // func is a local variable
      pushValue(call.var, frag);
      sp = sp - 1          // read call address
          - 1 - call.args  // return, pop args and argc
          + 1;             // retval
      addCode(frag, stackInfo() + "<" + call.func + ", " + call.args + " args>", ICALL);
    } else {
      // func is a function name, needs linking
      // push nbr of args
      if (generateInbuiltFunction(call, frag)) return;
      pushNumber(frag, call.args, "argc, replaced by retval");
      frag.links.add(new ModuleFragment.LinkCall(frag.getPC(), call));
      sp = sp 
          - 1 - call.args  // return, pop args and argc
          + 1;             // retval
      addCode(frag, stackInfo() + "<" + call.func + ", " + call.args + " args>", ICALL_R, 0x03,0x00,0x00);
    }
    if (!call.referenced) {
      sp--;
      addCode(frag, stackInfo() + "unused retval", IPOP);
    }

  }
  
  void pushValues(TAC left, TAC right, ModuleFragment frag) {
    if (dbg) System.out.println("    PUSH VALUES");
    boolean lOnStack = pushValue(left, frag);
    boolean rOnStack = pushValue(right, frag);
    if (rOnStack && !lOnStack) {
      addCode(frag, ISWAP);
    }
  }
  
  void pushNumber(ModuleFragment frag, int num, String comment) {
    if (dbg) System.out.println("    PUSH NUM");
    sp++;
    if (num >= 0 && num <= 4) {
      addCode(frag, comment != null ? (stackInfo() + comment) : null, IPUSH_0 + num);
    } else if (num >= -128 && num <= 127) {
      addCode(frag, comment != null ? (stackInfo() + comment) : null, IPUSH_S, num);
    } else if (num >= 128 && num <= 255+128) {
      addCode(frag, comment != null ? (stackInfo() + comment) : null, IPUSH_U, num-128);
    } else if (num <= -128 && num >= -(255+128)) {
      addCode(frag, comment != null ? (stackInfo() + comment) : null, IPUSH_U, (-num)-128, INEG);
    } else {
      throw new CompilerError("not implemented");
    }

  }
  
  boolean pushValue(TAC a, ModuleFragment frag) {
    if (dbg) System.out.println("    PUSH VAL " + a);
    if (a instanceof TACNil) {
      sp++;
      addCode(frag, stackInfo() + a.toString(), IPUSH_NIL);
    } 
    else if (a instanceof TACInt) {
      if (((TACInt)a).x >= -(255+128) && ((TACInt)a).x <= 255+128) {
        pushNumber(frag, ((TACInt)a).x, a.toString());
      } else {
        frag.links.add(new ModuleFragment.LinkConst(frag.getPC(), a));
        sp++;
        addCode(frag, stackInfo() + a.toString() + " (const)", ILOAD_IM, 1,0,0);
      }
    } 
    else if (a instanceof TACVar) {
      if (frag.locals.containsKey(a)) {
        // local variable
        int fpoffset = frag.locals.get(a);
        sp++;
        addCode(frag, stackInfo() + a.toString() + " (local)", ILOAD_FP, fpoffset);
      } else {
        // global variable
        frag.links.add(new ModuleFragment.LinkGlobal(frag.getPC(), (TACVar)a));
        sp++;
        addCode(frag, stackInfo() + a.toString() + " (global)", ILOAD_IM, 0,0,0);
      }
    }
    else if (a instanceof TACFloat || a instanceof TACString) {
      frag.links.add(new ModuleFragment.LinkConst(frag.getPC(), a));
      sp++;
      addCode(frag, stackInfo() + a.toString() + " (const)", ILOAD_IM, 2,0,0);
    } else if (a instanceof TACCode) {
      TACCode tcode = (TACCode)a;
      if (tcode.type == ASTNodeBlok.TYPE_ANON) {
        // copy the locals in this fragment referenced by the anonymous into a set
        for (int i = 0; i < tcode.adsVars.size(); i++) {
          TACVar adsVar = tcode.adsVars.get(i);
          if (frag.locals.containsKey(adsVar)) {
            // local value
            int fpoffset = frag.locals.get(adsVar);
            sp++;
            addCode(frag, stackInfo() + adsVar.toString() + " (local anon)", ILOAD_FP, fpoffset);
          } else {
//            // global variable
//            frag.links.add(new ModuleFragment.LinkGlobal(frag.getPC(), adsVar));
//            sp++;
//            addCode(frag, stackInfo() + a.toString() + " (global anon)", ILOAD_IM, 0,0,0);
            throw new CompilerError("fatal", a.getNode());
          }
        }
        pushValue(new TACInt(a.getNode(), tcode.adsVars.size()), frag);
        sp -= tcode.adsVars.size();
        addCode(frag, stackInfo(), ISET_CRE);
      }
      frag.links.add(new ModuleFragment.LinkConst(frag.getPC(), a));
      sp++;
      addCode(frag, stackInfo() + a.toString() + " (const)", ILOAD_IM, 2,0,0);
      if (tcode.type == ASTNodeBlok.TYPE_ANON) {
        sp = sp - 2 + 1;
        addCode(frag, stackInfo() + a.toString() + " (anon call def)", IANO_CRE);
      }
    }
    else if (a instanceof TACGetMe) {
      sp++;
      addCode(frag, stackInfo() + "get me", IPUSH_ME);
    } 
    else if (a instanceof TACRange) {
      // TODO RANGE?
      //throw new CompilerError("not implemented" , a.getNode());
    }
    else if (a instanceof TACUnresolved) {
      frag.links.add(new ModuleFragment.LinkUnresolved(frag.getPC(), (TACUnresolved)a));
      sp++;
      addCode(frag, stackInfo() + a.toString() + " (link)", ILOAD_IM, INOP,INOP,INOP);
    }
    else if (a instanceof TACArgc) {
      sp++;
      addCode(frag, stackInfo() + a, ILOAD_FP, -FRAME_3_ARGC);
    }
    else if (a instanceof TACArgNbr) {
      int argnbr = ((TACArgNbr)a).arg;
      pushNumber(frag, argnbr, "argnbr " + argnbr);
      sp++;
      addCode(frag, stackInfo() + "argc", ILOAD_FP, -FRAME_3_ARGC);
      sp -= 2;
      addCode(frag, stackInfo() + argnbr + " >= argc", ICMP);
      addCode(frag, stackInfo() + "ok", IBRA_LT, 0,0,4+1+4);
      sp++;
      addCode(frag, stackInfo() + "beyond argc", IPUSH_NIL);
      sp--;
      addCode(frag, stackInfo(), IBRA, 0,0,4+2);
      sp++;
      addCode(frag, stackInfo() + "get arg " + argnbr, ILOAD_FP, -FRAME_SIZE - 1 - argnbr);
    }
    else if (a instanceof TACArgv) {
      pushNumber(frag, -1, "create argv array");
      addCode(frag, stackInfo(), ISET_CRE);
    }
    else if (a instanceof TACMapTuple) {
      pushValue(((TACMapTuple) a).key, frag);
      pushValue(((TACMapTuple) a).val, frag);
      sp--;
      addCode(frag, stackInfo() + a, ITUP_CRE);
    }
    else if (a instanceof TACRangeLen) {
      sp = sp + 1;
      addCode(frag, stackInfo() + "dereferenced set", ICPY, ((TACRangeLen)a).getDerefeeStackPos());
      sp = sp - 1 + 1;
      addCode(frag, stackInfo() + "length-1", ISET_SZ);
      sp = sp - 1 + 1;
      addCode(frag, stackInfo() + "-1", ISUB_Q1);
    }
    else if (a instanceof TACOp) {
      // already on stack
      return true;
    }
    else {
      return true;
    }
    return false;
  }
  
  String stackInfo() {
    return "sp=" + sp + "\t";
  }
}
