package com.pelleplutt.plang;

import java.util.ArrayList;
import java.util.List;
import static com.pelleplutt.plang.AST.*;

public abstract class ASTNode {
  int op;
  List<ASTNode> operands;

  public ASTNode(int op) {
    this.op = op;
  }

  public ASTNode(int op, ASTNode... operands) {
    this.op = op;
    this.operands = new ArrayList<ASTNode>();
    for (ASTNode n : operands) {
      this.operands.add(n);
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (operands != null) {
      // operation
      if (op >= 0) {
        sb.append(AST.OPS[op]);
      } else {
        sb.append(op);
      }
      sb.append('[');
      for (int i = 0; i < operands.size(); i++) {
        sb.append(operands.get(i).toString());
        if (i < operands.size() - 1)
          sb.append(',');
      }
      sb.append(']');
    } else {
      // numeric
      sb.append(op);
    }
    return sb.toString();
  }
  
  public String threeAddrOp() {
    if (op == OP_EQ) return "=";
    if (op == OP_EQ2) return "==";
    if (op == OP_GT) return ">";
    if (op == OP_GE) return ">=";
    if (op == OP_LT) return "<";
    if (op == OP_LE) return "<=";
    if (op == OP_NEQ) return "!=";
    if (op == OP_AND) return "and";
    if (op == OP_OR) return "or";
    if (op == OP_XOR) return "xor";
    if (op == OP_SHLEFT) return ">>";
    if (op == OP_SHRIGHT) return "<<";
    if (op == OP_NOT) return "not";
    if (op == OP_PLUS) return "+";
    if (op == OP_MINUS) return "-";
    if (op == OP_MUL) return "*";
    if (op == OP_DIV) return "/";
    return null;
  }
  
  public static class ASTNodeBlok extends ASTNode {
    public ASTNodeBlok(ASTNode... operands) {
      super(AST.OP_BLOK, operands);
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      // operation
      sb.append("");
      sb.append('{');
      for (int i = 0; i < operands.size(); i++) {
        sb.append(operands.get(i).toString());
        if (i < operands.size() - 1)
          sb.append(',');
      }
      sb.append('}');
      return sb.toString();
    }
  }

  public static class ASTNodeOp extends ASTNode {
    public ASTNodeOp(int op, ASTNode... operands) {
      super(op, operands);
    }
  }

  public static class ASTNodeDelim extends ASTNode {
    public ASTNodeDelim(int op) {
      super(op);
    }
  }

  public static class ASTNodeNumeric extends ASTNode {
    double value;
    boolean frac;

    public ASTNodeNumeric(double v, boolean f) {
      super(f ? AST.OP_NUMERICD : AST.OP_NUMERICI);
      value = v;
      this.frac = f;
    }

    public String toString() {
      if (frac) {
        return Double.toString(value);
      } else {
        return Integer.toString((int) value);
      }
    }
  }

  public static class ASTNodeSymbol extends ASTNode {
    String symbol;

    public ASTNodeSymbol(String s) {
      super(AST.OP_SYMBOL);
      symbol = s;
    }

    public String toString() {
      return symbol;
    }
  }

  public static class ASTNodeFuncDef extends ASTNode {
    String name;

    public ASTNodeFuncDef(String s, ASTNode... operands) {
      super(AST.OP_FUNCDEF, operands);
      name = s;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("def:" + name);
      sb.append('(');
      for (int i = 0; i < operands.size(); i++) {
        sb.append(operands.get(i).toString());
        if (i < operands.size() - 1)
          sb.append(',');
      }
      sb.append(')');
      return sb.toString();
    }
  }

  public static class ASTNodeFuncCall extends ASTNode {
    String name;
    int callid;

    public ASTNodeFuncCall(String s, int callid, ASTNode... operands) {
      super(AST.OP_CALL, operands);
      this.callid = callid;
      name = s;
    }
    
    public void setArguments(List<ASTNode> args) {
      operands = args;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("<" + name + ">");
      sb.append('(');
      for (int i = 0; i < operands.size(); i++) {
        sb.append(operands.get(i).toString());
        if (i < operands.size() - 1)
          sb.append(',');
      }
      sb.append(')');
      return sb.toString();
    }
  }

  public static class ASTNodeString extends ASTNode {
    String string;

    public ASTNodeString(String s) {
      super(AST.OP_QUOTE1);
      string = s;
    }

    public String toString() {
      return "\"" + string + "\"";
    }
  }

}

