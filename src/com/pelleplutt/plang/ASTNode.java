package com.pelleplutt.plang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    List<ASTNodeSymbol> symList;
    List<ASTNodeSymbol> argList;
    int scopeLevel;
    String id;
    String module;
    ASTNodeBlok parentBlock;
    int type;
    boolean variablesHandled;
    public static final int TYPE_MAIN = 0;
    public static final int TYPE_FUNC = 1;
    public static final int TYPE_ANON = 2;
    public ASTNodeBlok(ASTNode... operands) {
      super(AST.OP_BLOK, operands);
    }
    public void setAnnotation(List<ASTNodeSymbol> symList, int scopeLevel, String id, String module, int type) {
      this.symList = symList;
      this.scopeLevel = scopeLevel;
      this.id = id;
      this.module = module;
      this.type = type;
    }
    public void setAnnotation(List<ASTNodeSymbol> symList, List<ASTNodeSymbol> argList, int scopeLevel, String id, String module, int type) {
      this.symList = symList;
      this.argList = argList;
      this.scopeLevel = scopeLevel;
      this.id = id;
      this.module = module;
      this.type = type;
    }
    public List<ASTNodeSymbol> getVariables() {
      return this.symList;
    }
    public List<ASTNodeSymbol> getArguments() {
      return this.argList;
    }
    public String getModule() {
      return this.module;
    }
    public int getScopeLevel(){
      return scopeLevel;
    }
    public String getScopeId() {
      return this.id;
    }
    public boolean gotUnhandledVariables() {
      return !variablesHandled && (!symList.isEmpty() || (argList != null && !argList.isEmpty()));
    }
    public boolean isVariablesHandled() {
      return variablesHandled;
    }
    public void setVariablesHandled() {
      variablesHandled = true;
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
    
    public boolean equals(Object o) {
      if (o instanceof String)
        return ((String)o).equals(symbol);
      else if (o instanceof ASTNodeSymbol) 
        return ((ASTNodeSymbol)o).symbol.equals(symbol);
      else
        return false;
    }
    
    public int hashCode() {
      return symbol.hashCode();
    }
    
  }

  public static class ASTNodeFuncDef extends ASTNode {
    String name;
    public List<ASTNode> arguments;

    public ASTNodeFuncDef(String s, ASTNode... operands) {
      super(AST.OP_FUNCDEF, operands);
      name = s;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(name);
      sb.append('(');
      for (int i = 0; arguments != null && i < arguments.size(); i++) {
        sb.append(arguments.get(i).toString());
        if (i < arguments.size() - 1)
          sb.append(',');
      }
      sb.append("):");
      sb.append(super.toString());
      return sb.toString();
    }
    public void setArguments(List<ASTNode> args) {
      arguments = args;
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
  
  public static class ASTNodeRange extends ASTNode {
    public ASTNodeRange(ASTNode from, ASTNode step, ASTNode to) {
      super(OP_RANGE, from, step, to);
    }
    public ASTNodeRange(ASTNode from, ASTNode to) {
      super(OP_RANGE, from, to);
    }
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("range<");
      if (operands.size() == 2) {
        sb.append(operands.get(0));
        sb.append(".." + operands.get(1));
      } else {
        sb.append(operands.get(0));
        sb.append(".." + operands.get(1));
        sb.append(".." + operands.get(2));
      }
      sb.append('>');
      return sb.toString();
    }
  }
}
