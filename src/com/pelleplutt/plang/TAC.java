package com.pelleplutt.plang;

import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;
import com.pelleplutt.plang.CodeGenFront.Context;

public abstract class TAC {
  ASTNode e;
  Context ctx;
  String ref(TAC t){
    if (t instanceof TACVar || t instanceof TACString || 
        t instanceof TACInt || t instanceof TACFloat ||
        t instanceof TACCode) {
      return t.toString();
    } else {
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
    public String toString() {return esym.symbol + "[" + declaringBlock.getModule() + 
                                     declaringBlock.getScopeId() + "]";}
  }
  public static class TACInt extends TAC {
    int x;
    public TACInt(ASTNode e, int x) {
      this.e = e; this.x = x;
    }
    public String toString() {return Integer.toString(x);}
  }
  public static class TACString extends TAC {
    String x;
    public TACString(ASTNode e, String x) {
      this.e = e; this.x = x;
    }
    public String toString() {return "'" + x + "'";}
  }
  public static class TACCode extends TAC {
    Context ctx;
    public TACCode(ASTNode e, Context ctx) {
      this.e = e; this.ctx = ctx;
    }
    public String toString() {return ctx.module;}
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
    public TACAlloc(ASTNodeBlok e) {
      this.e = e;
      this.blok = e;
    }
    public String toString() {return "ALLO " + CodeGenFront.varMapString(((ASTNodeBlok)e).symMap);}
  }
  public static class TACFree extends TAC {
    ASTNodeBlok blok;
    public TACFree(ASTNodeBlok e) {
      this.e = e;
      this.blok = e;
    }
    public String toString() {return "FREE " + CodeGenFront.varMapString(((ASTNodeBlok)e).symMap);}
  }
  public static class TACArg extends TAC {
    TAC arg;
    public TACArg(ASTNode e, TAC arg) {
      this.e = e; this.arg = arg;
    }
    public String toString() {return "ARG " + ref(arg);}
  }
  public static class TACCall extends TAC {
    String func;
    int args;
    public TACCall(ASTNode e, String func, int args) {
      this.e = e; this.func = func; this.args = args;
    }
    public String toString() {return "CALL <" + func + "> " + args +" args";}
  }
}
