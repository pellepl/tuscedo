package com.pelleplutt.plang;

import static com.pelleplutt.plang.AST.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.ASTNode.ASTNodeFuncCall;
import com.pelleplutt.plang.ASTNode.ASTNodeNumeric;
import com.pelleplutt.plang.ASTNode.ASTNodeString;
import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;

public class CodeGenFront {
  int code = 0;
  int label = 0;
  int anonIx = 0;
  List<Context> ctxs = new ArrayList<Context>();
  Context ctx;
  
  public CodeGenFront() {
    ctx = new Context(".main");
    ctxs.add(ctx);
  }
  
  public void gen(ASTNodeBlok e) {
    genrec(e, e);
    printIR();
  }
  
  void printIR() {
    for (Context ctx : ctxs) {
      int bix = 0;
      System.out.println("CONTEXT " + ctx.id);
      for (List<TAC> block : ctx.blocks) {
        System.out.println("======================================== " + bix);
        bix++;
        for (TAC t : block) {
          if (t instanceof TACLabel) {
            System.out.println(t);
          } else {
            System.out.println("   " + ctx.ir.indexOf(t) + ":\t"+t);
          }
        }
      }
      System.out.println();
    }
  }
  
  void printDot() {
     
  }
  
  void add(TAC t) {
    if (ctx.block == null) {
      ctx.block = new ArrayList<TAC>();
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
  TACLabel loopBreak() {
    return ctx.loopStack.peek().exit;
  }
  TACLabel loopContinue() {
    return ctx.loopStack.peek().loop;
  }
  
  TAC genrec(ASTNode e, ASTNodeBlok blok) {
    if (e.op == OP_BLOK) {
      ASTNodeBlok be = (ASTNodeBlok)e;
      Context oldCtx = ctx;
      Context newctx = null;
      if (be.type == ASTNodeBlok.ANON) {
         newctx = new Context(".anon" + (anonIx++));
        ctxs.add(newctx);
        ctx = newctx;
      } 
      if (be.symMap.size() > 0) {
        add(new TACAlloc(be, be.symMap));
      }
      for (ASTNode e2 : e.operands) {
        genrec(e2, (ASTNodeBlok)e);
      }
      if (be.symMap.size() > 0) {
        add(new TACFree(be, be.symMap));
      }
      ctx = oldCtx;
      if (be.type == ASTNodeBlok.ANON) {
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
      return new TACSym(e, ((ASTNodeSymbol)e).symbol, getScope(blok, ((ASTNodeSymbol)e).symbol));
    }
    
    else if (e.op == OP_EQ) {
      TAC assignee = genrec(e.operands.get(0), blok);
      TAC assignment = genrec(e.operands.get(1), blok);

      TAC op = new TACOp(e, e.op, assignee, assignment); 
      add(op);
      return op;
    } else if (AST.isOperator(e.op) && !AST.isAssignOperator(e.op)) { 
      TAC op = new TACOp(e, e.op, 
          genrec(e.operands.get(0), blok), 
          genrec(e.operands.get(1), blok));
      add(op);
      return op;
    }
    else if (e.op == OP_IF) {
      boolean hasElse = e.operands.size() == 3;
      String label = genLabel();
      TACLabel lExit = new TACLabel(e, label+"_ifend");
      if (!hasElse) {
        TAC cond = genrec(e.operands.get(0), blok);
        TAC iffalsegoto = new TACGotoCond(e, cond, lExit, false);
        add(iffalsegoto);
        newBlock();
        genrec(e.operands.get(1), blok);
        newBlock();
        add(lExit);
      } else {
        TACLabel lElse = new TACLabel(e, label+"_ifelse");
        TAC cond = genrec(e.operands.get(0), blok);
        TAC iffalsegoto = new TACGotoCond(e, cond, lElse, false);
        add(iffalsegoto);
        newBlock();
        genrec(e.operands.get(1), blok);
        TAC gotoExit = new TACGoto(e, lExit);
        add(gotoExit);
        newBlock();
        add(lElse);
        genrec(e.operands.get(2).operands.get(0), blok);
        newBlock();
        add(lExit);
      }
    }    
    else if (e.op == OP_FOR) {
      if (e.operands.size() == 4) {
        //for (x; y; z) {w}
        String label = genLabel();
        TACLabel lLoop = new TACLabel(e, label+"_floop");
        TACLabel lExit = new TACLabel(e, label+"_fexit");
        genrec(e.operands.get(0), blok);
        newBlock();
        add(lLoop);
        TAC cond = genrec(e.operands.get(1), blok);
        TAC iffalsegoto = new TACGotoCond(e, cond, lExit, false);
        add(iffalsegoto);
        newBlock();
        pushLoop(lLoop, lExit);
        genrec(e.operands.get(3), blok);
        popLoop();
        genrec(e.operands.get(2), blok);
        TAC gotoLoop = new TACGoto(e, lLoop);
        add(gotoLoop);
        newBlock();
        add(lExit);
      } else {
        //for (x in y) {w}
        // TODO
      }
    }
    else if (e.op == OP_WHILE) {
      String label = genLabel();
      TACLabel lLoop = new TACLabel(e, label+"_wloop");
      TACLabel lExit = new TACLabel(e, label+"_wexit");
      newBlock();
      add(lLoop);
      TAC cond = genrec(e.operands.get(0), blok);
      TAC iffalsegoto = new TACGotoCond(e, cond, lExit, false);
      add(iffalsegoto);
      newBlock();
      pushLoop(lLoop, lExit);
      genrec(e.operands.get(1), blok);
      popLoop();
      TAC gotoLoop = new TACGoto(e, lLoop);
      add(gotoLoop);
      newBlock();
      add(lExit);
    }
    else if (e.op == OP_BREAK) {
      TACLabel lExit = loopBreak();
      add(new TACGoto(e, lExit));
      newBlock();
    }
    else if (e.op == OP_CONTINUE) {
      TACLabel lCont = loopContinue();
      add(new TACGoto(e, lCont));
      newBlock();
    }
    else if (e.op == OP_CALL) {
      String args[] = new String[e.operands.size()];
      for (int i = 0; i < args.length; i++) {
        TAC arg = new TACArg(e, genrec(e.operands.get(i), blok));
        add(arg);
      }
      TAC call = new TACCall(e, ((ASTNodeFuncCall)e).name, args.length);
      add(call);
      return call;
    }

    
    
    return null;
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
  
  String getScope(ASTNodeBlok be, String sym) {
    while (be != null) {
      if (be.getVariables().containsKey(sym)) {
        return be.getModule() + "$" + be.getId();
      }
      be = be.parent;
    }
    throw new CompilerError("variable '" + sym + "' not found");
  }
  
  static public void check(ASTNodeBlok e) {
    CodeGenFront cg = new CodeGenFront();
    cg.gen(e);
  }
  
  String varMapString(Map<String, ASTNode> vars) {
    StringBuilder sb = new StringBuilder();
    sb.append("[ ");
    for (String var : vars.keySet()) {
      sb.append(var + " ");
    }
    sb.append(']');
    return sb.toString();
  }
  
  class Context {
    String id;
    List<TAC> block; 
    List<List<TAC>> blocks = new ArrayList<List<TAC>>();
    List<TAC> ir = new ArrayList<TAC>();
    Stack<Loop> loopStack = new Stack<Loop>();
    public Context(String id) {
      this.id = id;
    }
  }
  
  class Loop {
    TACLabel loop, exit;
    public Loop(TACLabel loop, TACLabel exit) {
      this.loop = loop;
      this.exit = exit;
    }
  }
  
  //
  // 3 address code
  //
  
  abstract class TAC {
    ASTNode e;
    Context ctx;
    String ref(TAC t){
      if (t instanceof TACSym || t instanceof TACString || 
          t instanceof TACInt || t instanceof TACFloat ||
          t instanceof TACCode) {
        return t.toString();
      } else {
        return "["+Integer.toString(ctx.ir.indexOf(t)) +"]";
      }
    }
  }
  class TACOp extends TAC {
    int op;
    TAC left, right;
    public TACOp(ASTNode e, int op, TAC left, TAC right) {
      this.e = e; this.op = op; this.left = left; this.right = right;
    }
    public String toString() {return ref(left) + " " + AST.opString(op) + " " + ref(right);}
  }
  class TACSym extends TAC {
    String symbol;
    String scope;
    public TACSym(ASTNode e, String sym, String scope) {
      this.e = e; this.symbol = sym;this.scope = scope;
    }
    public String toString() {return symbol + "___" + scope;}
  }
  class TACInt extends TAC {
    int x;
    public TACInt(ASTNode e, int x) {
      this.e = e; this.x = x;
    }
    public String toString() {return Integer.toString(x);}
  }
  class TACString extends TAC {
    String x;
    public TACString(ASTNode e, String x) {
      this.e = e; this.x = x;
    }
    public String toString() {return "'" + x + "'";}
  }
  class TACCode extends TAC {
    Context ctx;
    public TACCode(ASTNode e, Context ctx) {
      this.e = e; this.ctx = ctx;
    }
    public String toString() {return ctx.id;}
  }
  class TACFloat extends TAC {
    float x;
    public TACFloat(ASTNode e, float x) {
      this.e = e; this.x = x;
    }
    public String toString() {return Float.toString(x);}
  }
  class TACGoto extends TAC {
    TACLabel label;
    public TACGoto(ASTNode e, TACLabel label) {
      this.e = e; this.label = label;
    }
    public String toString() {return "GOTO " + label.label;}
  }
  class TACGotoCond extends TAC {
    TACLabel label;
    TAC cond;
    boolean positive;
    public TACGotoCond(ASTNode e, TAC cond, TACLabel label, boolean condPositive) {
      this.e = e; this.cond = cond; this.label = label; this.positive = condPositive;
    }
    public String toString() {return (positive ? "IF " : "IFNOT ") + ref(cond) + " GOTO " + label.label;}
  }
  class TACLabel extends TAC {
    String label;
    public TACLabel(ASTNode e, String label) {
      this.e = e; this.label = label;
    }
    public String toString() {return label +":";}
  }
  class TACAlloc extends TAC {
    Map<String, ASTNode> vars;
    public TACAlloc(ASTNode e, Map<String, ASTNode> vars) {
      this.e = e; this.vars = vars;
    }
    public String toString() {return "ALLO " + varMapString(vars);}
  }
  class TACFree extends TAC {
    Map<String, ASTNode> vars;
    public TACFree(ASTNode e,     Map<String, ASTNode> vars) {
      this.e = e; this.vars = vars;
    }
    public String toString() {return "FREE " + varMapString(vars);}
  }
  class TACArg extends TAC {
    TAC arg;
    public TACArg(ASTNode e, TAC arg) {
      this.e = e; this.arg = arg;
    }
    public String toString() {return "ARG " + ref(arg);}
  }
  class TACCall extends TAC {
    String func;
    int args;
    public TACCall(ASTNode e, String func, int args) {
      this.e = e; this.func = func; this.args = args;
    }
    public String toString() {return "CALL <" + func + "> " + args +" args";}
  }
}
