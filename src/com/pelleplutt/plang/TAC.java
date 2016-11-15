package com.pelleplutt.plang;

import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;
import com.pelleplutt.plang.CodeGenFront.Context;

public abstract class TAC {
  ASTNode e;
  Context ctx;
  boolean referenced;

  static boolean dbgResolveRefs = false;
  
  String ref(TAC t){
    if (t instanceof TACVar || t instanceof TACString || 
        t instanceof TACInt || t instanceof TACFloat ||
        t instanceof TACCode) {
      return t.toString();
    } else {
      if (dbgResolveRefs) return "(" + t.toString() + ")";
      return "["+Integer.toString(ctx.ir.indexOf(t)) +"]";
    }
  }
  public static class TACOp extends TAC {
    int op;
    TAC left, right;
    public TACOp(ASTNode e, int op, TAC left, TAC right) {
      this.e = e; this.op = op; this.left = left; this.right = right;
    }
    public String toString() {return ref(left) + " " + AST.opString(op) + " " + ref(right);}
  }
  public static class TACAssign extends TACOp {
    public TACAssign(ASTNode e, int op, TAC left, TAC right) {
      super(e, op, left, right);
    }
  }
  public static class TACVar extends TAC {
    ASTNodeSymbol esym;
    ASTNodeBlok declaringBlock;
    public TACVar(ASTNodeSymbol e, ASTNodeBlok declaringBlock) {
      this.e = e; this.esym= e; this.declaringBlock = declaringBlock;
    }
    public boolean equals(Object o) {
      if (o instanceof TACVar) {
        TACVar t = (TACVar)o;
        return esym.symbol.equals(t.esym.symbol) &&
            declaringBlock.getModule().equals(t.declaringBlock.getModule()) &&
            declaringBlock.getScopeId() == t.declaringBlock.getScopeId();
      } else return false;
    }
    public int hashCode() {
      return esym.symbol.hashCode();
    }
    public String toString() {return esym.symbol + ":" + declaringBlock.getModule() + 
                                     declaringBlock.getScopeId();}
  }
  public static class TACNil extends TAC {
    public TACNil(ASTNode e) {
      this.e = e;
    }
    public boolean equals(Object o) {
      return (o instanceof TACNil);
    }
    public int hashCode() {
      return 0;
    }
    public String toString() {return "(nil)";}
  }
  public static class TACInt extends TAC {
    int x;
    public TACInt(ASTNode e, int x) {
      this.e = e; this.x = x;
    }
    public boolean equals(Object o) {
      if (o instanceof TACInt) {
        TACInt t = (TACInt)o;
        return x == t.x;
      } else return false;
    }
    public int hashCode() {
      return x;
    }
    public String toString() {return Integer.toString(x);}
  }
  public static class TACString extends TAC {
    String x;
    public TACString(ASTNode e, String x) {
      this.e = e; this.x = x;
    }
    public boolean equals(Object o) {
      if (o instanceof TACString) {
        TACString t = (TACString)o;
        return x.equals(t.x);
      } else return false;
    }
    public int hashCode() {
      return x.hashCode();
    }
    public String toString() {return "'" + x + "'";}
  }
  public static class TACCode extends TAC {
    Context ctx;
    public TACCode(ASTNode e, Context ctx) {
      this.e = e; this.ctx = ctx;
    }
    public boolean equals(Object o) {
      if (o instanceof TACCode) {
        TACCode t = (TACCode)o;
        return ctx.module.equals(t.ctx.module) && ctx.name.equals(t.ctx.name);
      } else return false;
    }
    public int hashCode() {
      return ctx.module.hashCode() ^ ctx.name.hashCode();
    }
    public String toString() {return ctx.module + ctx.name;}
  }
  public static class TACFloat extends TAC {
    float x;
    public TACFloat(ASTNode e, float x) {
      this.e = e; this.x = x;
    }
    public String toString() {return Float.toString(x);}
  }
  public static class TACGoto extends TAC {
    TACLabel label;
    public TACGoto(ASTNode e, TACLabel label) {
      this.e = e; this.label = label;
    }
    public String toString() {return "GOTO " + label.label;}
  }
  public static class TACGotoCond extends TAC {
    TACLabel label;
    TAC cond;
    boolean positive;
    public TACGotoCond(ASTNode e, TAC cond, TACLabel label, boolean condPositive) {
      this.e = e; this.cond = cond; this.label = label; this.positive = condPositive;
    }
    public String toString() {return (positive ? "IF " : "IFNOT ") + ref(cond) + " GOTO " + label.label;}
  }
  public static class TACLabel extends TAC {
    String label;
    public TACLabel(ASTNode e, String label) {
      this.e = e; this.label = label;
    }
    public String toString() {return label +":";}
  }
  public static class TACAlloc extends TAC {
    ASTNodeBlok blok;
    boolean funcEntry;
    public TACAlloc(ASTNodeBlok e) {
      this.e = e;
      this.blok = e;
    }
    public TACAlloc(ASTNodeBlok e,boolean funcEntry) {
      this.e = e;
      this.blok = e;
      this.funcEntry = funcEntry;
    }
    public String toString() {return "ALLO " + CodeGenFront.varMapString(((ASTNodeBlok)e).symList) + (funcEntry ? " FUNC" : "");}
  }
  public static class TACFree extends TAC {
    ASTNodeBlok blok;
    public TACFree(ASTNodeBlok e) {
      this.e = e;
      this.blok = e;
    }
    public String toString() {return "FREE " + CodeGenFront.varMapString(((ASTNodeBlok)e).symList);}
  }
  public static class TACArg extends TAC {
    TAC arg;
    public TACArg(ASTNode e, TAC arg) {
      this.e = e; this.arg = arg;
    }
    public String toString() {return "ARG " + ref(arg);}
  }
  public static class TACReturn extends TAC {
    TAC ret;
    public TACReturn(ASTNode e) {
      this.e = e;
    }
    public TACReturn(ASTNode e, TAC ret) {
      this.e = e; this.ret = ret;
    }
    public String toString() {return "RET " + (ret == null ? "" : ref(ret));}
  }
  public static class TACCall extends TAC {
    String func;
    int args;
    public TACCall(ASTNode e, String func, int args) {
      this.e = e; this.func = func; this.args = args;
    }
    public String toString() {return "CALL <" + func + "> " + args +" args";}
  }
  public static class TACBkpt extends TAC {
    public TACBkpt(ASTNode e) {
      this.e = e;
    }
  }
}
