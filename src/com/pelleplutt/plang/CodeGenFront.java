package com.pelleplutt.plang;

import static com.pelleplutt.plang.AST.OP_BKPT;
import static com.pelleplutt.plang.AST.OP_BLOK;
import static com.pelleplutt.plang.AST.OP_BREAK;
import static com.pelleplutt.plang.AST.OP_CALL;
import static com.pelleplutt.plang.AST.OP_CONTINUE;
import static com.pelleplutt.plang.AST.OP_EQ;
import static com.pelleplutt.plang.AST.OP_FOR;
import static com.pelleplutt.plang.AST.OP_FUNCDEF;
import static com.pelleplutt.plang.AST.OP_GOTO;
import static com.pelleplutt.plang.AST.OP_IF;
import static com.pelleplutt.plang.AST.OP_NIL;
import static com.pelleplutt.plang.AST.OP_NUMERICB1;
import static com.pelleplutt.plang.AST.OP_NUMERICB2;
import static com.pelleplutt.plang.AST.OP_NUMERICD;
import static com.pelleplutt.plang.AST.OP_NUMERICH1;
import static com.pelleplutt.plang.AST.OP_NUMERICH2;
import static com.pelleplutt.plang.AST.OP_NUMERICI;
import static com.pelleplutt.plang.AST.OP_PLUS;
import static com.pelleplutt.plang.AST.OP_QUOTE1;
import static com.pelleplutt.plang.AST.OP_QUOTE2;
import static com.pelleplutt.plang.AST.OP_RETURN;
import static com.pelleplutt.plang.AST.OP_SYMBOL;
import static com.pelleplutt.plang.AST.OP_WHILE;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.ASTNode.ASTNodeFuncCall;
import com.pelleplutt.plang.ASTNode.ASTNodeNumeric;
import com.pelleplutt.plang.ASTNode.ASTNodeString;
import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;
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

public class CodeGenFront {
  static boolean dbg = false;
  int code = 0;
  int label = 0;
  int anonIx = 0;
  List<Context> ctxs = new ArrayList<Context>();
  Context ctx;
  
  public static List<Module> genIR(ASTNodeBlok e) {
    CodeGenFront cg = new CodeGenFront();
    return cg.doIntermediateCode(e);
  }
  
  public CodeGenFront() {
  }
  
  public List<Module> doIntermediateCode(ASTNodeBlok eblk) {
    genIR(eblk, eblk);
    if (dbg) printIR(System.out);
    genCFG(ctxs);
    //printDot(System.out);
    List<Module> res = gather();
    return res;
  }
  
  List<Module> gather() {
    Map<String, Module> mmap = new HashMap<String, Module>();
    for (Context ctx : ctxs) {
      Module m = mmap.get(ctx.module);
      if (m == null) {
        m = new Module();
        m.id = ctx.module;
        mmap.put(ctx.module, m);
      }
      ModuleFragment frag = new ModuleFragment();
      frag.name = ctx.name;
      frag.gvars = ctx.gvars;
      frag.module = m;
      frag.type = ctx.type;
      for (Block block : ctx.blocks) {
        frag.tacs.add(new ArrayList<TAC>(block.ir));
      }
      m.frags.add(frag);
      m.gvars.addAll(frag.gvars);
    }
    List<Module> res = new ArrayList<Module>();
    res.addAll(mmap.values());
    return res;
  }
  
  void printBlock(PrintStream out, Context ctx, Block block, String nl) {
    for (TAC t : block.ir) {
      if (t instanceof TACLabel) {
        out.print(t + nl);
      } else {
        out.print("   " + ctx.ir.indexOf(t) + ":\t"+t+nl);
      }
    }
  }
  
  void printContext(PrintStream out, Context ctx) {
    int bix = 0;
    System.out.println("CONTEXT " + ctx.module + "" + ctx.name);
    for (Block block : ctx.blocks) {
      out.println("======================================== " + bix);
      bix++;
      printBlock(out, ctx, block, System.getProperty("line.separator"));
    }
    out.println();
  }
  
  void printIR(PrintStream out) {
    for (Context ctx : ctxs) {
      printContext(out, ctx);
    }
  }
  
  void printDot(PrintStream out) {
    out.println("digraph G {");
    int ctxIx = 0;
    for (Context ctx : ctxs) {
      String ctxId = ctxIx + "";
      out.println(ctxId + " [ fontsize=6 label=\"" + ctx.module + "\"];");
      for (Block block : ctx.blocks) {
        String nodeId = ctxIx + "." + block.blockId;
        out.print(nodeId + " [ fontsize=6 shape=box label=\"");
        printBlock(out, ctx, block, "\\l");
        out.println("\"];");
      }
      ctxIx++;
    }

    ctxIx = 0;
    for (Context ctx : ctxs) {
      String ctxId = ctxIx + "";
      out.println(ctxId + "->" + ctxIx + "." + ctx.blocks.get(0).blockId + ";");
      for (Block block : ctx.blocks) {
        String nodeId = ctxIx + "." + block.blockId;
        for (Block eblock : block.exits) {
          String enodeId = ctxIx + "." + eblock.blockId;
          out.println(nodeId + "->" + enodeId + ";");
        }
      }
      ctxIx++;
    }
    out.print("}");
  }
  
  //
  // construct control flow graph DAG from blocks
  //
  
  void genCFG(List<Context> ctxs) {
    for (Context c : ctxs) {
      for (int bix = 0; bix < c.blocks.size(); bix++) {
        Block b = c.blocks.get(bix);
        Block nb = bix < c.blocks.size() - 1 ? c.blocks.get(bix+1) : null;
        TAC t = b.get(b.size()-1);

        if (t instanceof TACGoto) {
          TACLabel l = ((TACGoto)t).label;
          Block ob = c.getBlock(l);
          b.exits.add(ob);
          ob.entries.add(b);
        } else if (t instanceof TACGotoCond) {
          TACLabel l = ((TACGotoCond)t).label;
          Block ob = c.getBlock(l);
          b.exits.add(ob);
          ob.entries.add(b);
          if (nb != null) {
            b.exits.add(nb);
            nb.entries.add(b);
          }
        } else {
          if (nb != null) {
            b.exits.add(nb);
            nb.entries.add(b);
          }
        }
      }
    }
  }
  
  //
  // construct three address code intermediate representation
  //
  
  TAC genIR(ASTNode e, ASTNodeBlok parentEblk) {
    if (e.op == OP_BLOK) {
      ASTNodeBlok eblk = (ASTNodeBlok)e;
      Context oldCtx = ctx;
      Context newctx = null;
      
      if (eblk.type == ASTNodeBlok.TYPE_ANON) {
        newctx = new Context(oldCtx.module, ".anon" + (anonIx++));
        newctx.type = ASTNode.ASTNodeBlok.TYPE_ANON;
        ctxs.add(newctx);
        ctx = newctx;
      } else if (eblk.type == ASTNodeBlok.TYPE_FUNC) {
        newctx = new Context(oldCtx.module, ".func" + ((ASTNodeBlok)eblk).id);
        newctx.type = ASTNode.ASTNodeBlok.TYPE_FUNC;
        ctxs.add(newctx);
        ctx = newctx;
      } else if (ctx == null) {
        // first context, must be globals
        ctx = new Context(eblk.module == null ? ".MAIN" : eblk.module, ".main");
        ctx.type = ASTNode.ASTNodeBlok.TYPE_MAIN;
        ctxs.add(ctx);
        if (eblk.symList != null) {
          // collect the global variables
          for (ASTNode esym : eblk.symList) {
            ASTNodeSymbol sym = (ASTNodeSymbol)esym;
            ctx.gvars.add(new TACVar(sym, eblk));
          }
        }
      }
      boolean doStackAllocation = (eblk.type != ASTNodeBlok.TYPE_MAIN && eblk.gotUnhandledVariables())||
                                  (eblk.gotUnhandledVariables() && 
                                   eblk.getScopeLevel() > 0); // no variable stack allocation for top scopes, these are global vars
      if (doStackAllocation) {
        eblk.setVariablesHandled();
        add(new TACAlloc(eblk, eblk.type == ASTNodeBlok.TYPE_FUNC));
      }
      for (ASTNode e2 : e.operands) {
        genIR(e2, (ASTNodeBlok)e);
      }
      if (doStackAllocation && eblk.type == ASTNodeBlok.TYPE_MAIN) {
        add(new TACFree(eblk));
      }
      ctx = oldCtx;
      if (eblk.type == ASTNodeBlok.TYPE_ANON) {
        return new TACCode(e, newctx);
      }
    } 
    
    else if (isNum(e.op)) {
      ASTNodeNumeric enm = ((ASTNodeNumeric)e);
      return enm.frac ? new TACFloat(enm, (float)enm.value) : new TACInt(enm, (int)enm.value);
    }
    
    else if (isStr(e.op)) {
      return new TACString(e, ((ASTNodeString)e).string);
    }
    
    else if (e.op == OP_SYMBOL) {
      return new TACVar((ASTNodeSymbol)e, getScope(parentEblk, (ASTNodeSymbol)e));
    }
    
    else if (e.op == OP_NIL) {
      return new TACNil(e);
    }
    
    else if (e.op == OP_EQ) {
      TAC assignee = genIR(e.operands.get(0), parentEblk);
      TAC assignment = genIR(e.operands.get(1), parentEblk);
      setReferenced(assignment);
      TAC op = new TACAssign(e, e.op, assignee, assignment); 

      add(op);
      return op;
    } 
    
    else if (AST.isOperator(e.op) && !AST.isAssignOperator(e.op)) {
      TAC left = genIR(e.operands.get(0), parentEblk);  
      TAC right = genIR(e.operands.get(1), parentEblk);  
      setReferenced(left);
      setReferenced(right);
      TAC op = new TACOp(e, e.op, left, right);
      add(op);
      return op;
    }
    
    else if (e.op == OP_IF) {
      boolean hasElse = e.operands.size() == 3;
      String label = genLabel();
      TACLabel lExit = new TACLabel(e, label+"_ifend");
      if (!hasElse) {
        TAC cond = genIR(e.operands.get(0), parentEblk);
        setReferenced(cond);
        TAC iffalsegoto = new TACGotoCond(e, cond, lExit, false);
        add(iffalsegoto);
        newBlock();
        genIR(e.operands.get(1), parentEblk);
        newBlock();
        add(lExit);
      } else {
        TACLabel lElse = new TACLabel(e, label+"_ifelse");
        TAC cond = genIR(e.operands.get(0), parentEblk);
        setReferenced(cond);
        TAC iffalsegoto = new TACGotoCond(e, cond, lElse, false);
        add(iffalsegoto);
        newBlock();
        genIR(e.operands.get(1), parentEblk);
        TAC gotoExit = new TACGoto(e, lExit);
        add(gotoExit);
        newBlock();
        add(lElse);
        genIR(e.operands.get(2).operands.get(0), parentEblk);
        newBlock();
        add(lExit);
      }
    }
    
    else if (e.op == OP_FOR) {
      if (e.operands.size() == 4) {
        //for (x; y; z) {w}
        String label = genLabel();
        ASTNodeBlok eblk = null;
        if (e.operands.get(3).op == OP_BLOK) {
          eblk = (ASTNodeBlok)e.operands.get(3);
        }

        TACLabel lLoop = new TACLabel(e, label+"_floop");
        TACLabel lExit = new TACLabel(e, label+"_fexit");
        TACLabel lCont = new TACLabel(e, label+"_fcont");
        
        boolean doStackAllocation = eblk != null && eblk.gotUnhandledVariables();
        if (doStackAllocation) {
          add(new TACAlloc(eblk));
          eblk.setVariablesHandled();
        }
        
        genIR(e.operands.get(0), parentEblk);
        newBlock();
        add(lLoop);
        TAC cond = genIR(e.operands.get(1), parentEblk);
        setReferenced(cond);
        TAC iffalsegoto = new TACGotoCond(e, cond, lExit, false);
        add(iffalsegoto);
        newBlock();
        pushLoop(lCont, lExit);
        genIR(e.operands.get(3), parentEblk);
        boolean contCalled = loopWasContinued();
        popLoop();
        if (contCalled) {
          newBlock();
          add(lCont);
        }
        genIR(e.operands.get(2), parentEblk);
        TAC gotoLoop = new TACGoto(e, lLoop);
        add(gotoLoop);
        newBlock();
        add(lExit);

        if (doStackAllocation) {
          add(new TACFree(eblk));
        }
        
      } else {
        //for (x in y) {w}
        // TODO
      }
    }
    
    else if (e.op == OP_WHILE) {
      ASTNodeBlok eblk = null;
      if (e.operands.get(1).op == OP_BLOK) {
        eblk = (ASTNodeBlok)e.operands.get(1);
      }

      String label = genLabel();
      TACLabel lLoop = new TACLabel(e, label+"_wloop");
      TACLabel lExit = new TACLabel(e, label+"_wexit");
      
      boolean doStackAllocation = eblk != null && eblk.gotUnhandledVariables();
      if (doStackAllocation) {
        add(new TACAlloc(eblk));
        eblk.setVariablesHandled();
      }

      newBlock();
      add(lLoop);
      TAC cond = genIR(e.operands.get(0), parentEblk);
      setReferenced(cond);
      TAC iffalsegoto = new TACGotoCond(e, cond, lExit, false);
      add(iffalsegoto);
      newBlock();
      pushLoop(lLoop, lExit);
      genIR(e.operands.get(1), parentEblk);
      popLoop();
      TAC gotoLoop = new TACGoto(e, lLoop);
      add(gotoLoop);
      newBlock();
      add(lExit);
      
      if (doStackAllocation) {
        add(new TACFree(eblk));
      }
    }
    
    else if (e.op == OP_BREAK) {
      TACLabel lExit = getLoopBreakLabel();
      add(new TACGoto(e, lExit));
      newBlock();
    }
    
    else if (e.op == OP_CONTINUE) {
      TACLabel lCont = getLoopContinueLabel();
      add(new TACGoto(e, lCont));
      newBlock();
    }
    
    else if (e.op == OP_GOTO) {
      TACLabel dest = new TACLabel(e.operands.get(0), ((ASTNodeSymbol)e.operands.get(0)).symbol);
      TAC gotoDest = new TACGoto(e, dest);
      add(gotoDest);
      newBlock();
    }
    
    else if (e.op == OP_CALL) {
      String args[] = new String[e.operands.size()];
      for (int i = 0; i < args.length; i++) {
        TAC argVal = genIR(e.operands.get(i), parentEblk);
        TAC arg = new TACArg(e, argVal);
        setReferenced(argVal);
        add(arg);
      }
      TAC call = new TACCall(e, ((ASTNodeFuncCall)e).name, args.length);
      add(call);
      return call;
    }

    else if (e.op == OP_FUNCDEF) {
      genIR(e.operands.get(0), parentEblk);
    }
    
    else if (e.op == OP_RETURN) {
      TACReturn ret;
      if (e.operands != null && e.operands.size() > 0) {
        TAC retVal = genIR(e.operands.get(0), parentEblk);
        setReferenced(retVal);
        ret = new TACReturn(e, retVal);
      } else {
        ret = new TACReturn(e);
      }
      add(ret);
      newBlock();
    }
    
    else if (e.op == OP_BKPT) {
      add(new TACBkpt(e));
    }
    
    // TODO more
    
    return null;
  }
  
  void add(TAC t) {
    if (ctx.block == null) {
      ctx.block = new Block();
      ctx.blocks.add(ctx.block);
    }
    ctx.block.add(t);
    ctx.ir.add(t);
    t.ctx = ctx;
  }
  
  void newBlock() {
    if (ctx.block != null && ctx.block.size() == 1 && ctx.block.get(0) instanceof TACLabel) {
      return;
    }
    ctx.block = null;
  }
  
  void pushLoop(TACLabel loop, TACLabel exit) {
    ctx.loopStack.push(new Loop(loop, exit));
  }
  
  void popLoop() {
    ctx.loopStack.pop();
  }
  
  TACLabel getLoopBreakLabel() {
    Loop l = ctx.loopStack.peek();
    l.wasBroken = true;
    return l.exit;
  }
  
  TACLabel getLoopContinueLabel() {
    Loop l = ctx.loopStack.peek();
    l.wasContinued = true;
    return l.loop;
  }
  
  boolean loopWasContinued() {
    return ctx.loopStack.peek().wasContinued;
  }
  
  boolean loopWasBraked() {
    return ctx.loopStack.peek().wasBroken;
  }
  
  String genTmp() {
    code++;
    return "t" + code;
  }
  
  String genLabel() {
    label++;
    return ".L" + label;
  }
  
  boolean isNum(int op) {
    return op == OP_NUMERICI || op == OP_NUMERICD || 
          op == OP_NUMERICH1 || op == OP_NUMERICH2 ||  
          op == OP_NUMERICB1 || op == OP_NUMERICB2;
  }
  
  boolean isStr(int op) {
    return op == OP_QUOTE1 || op == OP_QUOTE2; 
  }
  
  boolean isDef(int op) {
    return op == OP_SYMBOL || isStr(op) || isNum(op); 
  }
  
  void setReferenced(TAC tac) {
    tac.referenced = true;
  }
  
  ASTNodeBlok getScope(ASTNodeBlok eblk, ASTNodeSymbol sym) {
    while (eblk != null) {
      if (eblk.getVariables().contains(sym) ||
          (eblk.getArguments() != null && eblk.getArguments().contains(sym))) {
        return eblk;
      }
      eblk = eblk.parentBlock;
    }
    throw new CompilerError("variable '" + sym + "' not found");
  }

  static String varMapString(List<ASTNodeSymbol> vars) {
    StringBuilder sb = new StringBuilder();
    sb.append("[ ");
    for (ASTNodeSymbol var : vars) {
      sb.append(var.symbol + " ");
    }
    sb.append(']');
    return sb.toString();
  }
  
  //
  // IR context (e.g. main, func def, anonymous)
  //
  
  class Context {
    String module, name;
    Block block; 
    List<Block> blocks = new ArrayList<Block>();
    List<TAC> ir = new ArrayList<TAC>();
    Stack<Loop> loopStack = new Stack<Loop>();
    List<TACVar> gvars = new ArrayList<TACVar>();
    int type; // ASTNode.ASTNodeBlok.TYPE_*
    
    public Context(String module, String name) {
      this.module = module;
      this.name = name;
    }
    public Block getBlock(TACLabel t) {
      for (Block block : blocks) {
        for (TACLabel ot : block.labels) {
          if (t.label.equals(ot.label)) return block;
        }
      }
      throw new Error("cannot find block labeled " + t);
    }
  }
  
  //
  // Block in a context
  //
  
  static int __blockId = 0;
  class Block {
    int blockId;
    List<TACLabel> labels = new ArrayList<TACLabel>();
    List<TAC> ir = new ArrayList<TAC>();
    List<Block> entries = new ArrayList<Block>();
    List<Block> exits = new ArrayList<Block>();
    public Block() { blockId = __blockId++; }
    public void add(TAC t) {ir.add(t); if (t instanceof TACLabel) labels.add((TACLabel)t);}
    public TAC get(int i) {return ir.get(i);}
    public int size() {return ir.size();}
  }
  
  //
  // Loop info (for / while)
  //
  
  class Loop {
    TACLabel loop, exit;
    boolean wasContinued;
    boolean wasBroken;
    public Loop(TACLabel loop, TACLabel exit) {
      this.loop = loop;
      this.exit = exit;
    }
  }
}
