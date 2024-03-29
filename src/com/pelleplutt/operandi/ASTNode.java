package com.pelleplutt.operandi;

import static com.pelleplutt.operandi.AST.OP_AND;
import static com.pelleplutt.operandi.AST.OP_BNOT;
import static com.pelleplutt.operandi.AST.OP_DIV;
import static com.pelleplutt.operandi.AST.OP_EQ;
import static com.pelleplutt.operandi.AST.OP_EQ2;
import static com.pelleplutt.operandi.AST.OP_GE;
import static com.pelleplutt.operandi.AST.OP_GT;
import static com.pelleplutt.operandi.AST.OP_LE;
import static com.pelleplutt.operandi.AST.OP_LT;
import static com.pelleplutt.operandi.AST.OP_MINUS;
import static com.pelleplutt.operandi.AST.OP_MUL;
import static com.pelleplutt.operandi.AST.OP_NEQ;
import static com.pelleplutt.operandi.AST.OP_OR;
import static com.pelleplutt.operandi.AST.OP_PLUS;
import static com.pelleplutt.operandi.AST.OP_RANGE;
import static com.pelleplutt.operandi.AST.OP_SHLEFT;
import static com.pelleplutt.operandi.AST.OP_SHRIGHT;
import static com.pelleplutt.operandi.AST.OP_XOR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ASTNode {
  int op;
  List<ASTNode> operands;
  public int stroffset = -1;
  public int strlen = -1;

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
  
  public void copyDebugInfo(ASTNode e) {
    this.stroffset = e.stroffset;
    this.strlen = e.strlen;
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
    if (op == OP_BNOT) return "not";
    if (op == OP_PLUS) return "+";
    if (op == OP_MINUS) return "-";
    if (op == OP_MUL) return "*";
    if (op == OP_DIV) return "/";
    return null;
  }
  
  public static class ASTNodeBlok extends ASTNode {
    // map of symbols declared in this block, and at what index they are declared in this block
    private Map<ASTNodeSymbol, Integer> symList;
    private List<ASTNodeSymbol> argList;
    private List<ASTNodeSymbol> anonDefScopeSymList;
    int scopeLevel;
    String id;
    String module;
    ASTNodeBlok parentBlock;
    int type;
    boolean variablesHandled;
    public int symNbr;
    public static final int TYPE_MAIN = 0;
    public static final int TYPE_FUNC = 1;
    public static final int TYPE_ANON = 2;
    public ASTNodeBlok(ASTNode... operands) {
      super(AST.OP_BLOK, operands);
    }
    public void setAnnotation(Map<ASTNodeSymbol, Integer> symList, int scopeLevel, String id, String module, int type) {
      this.symList = symList;
      this.scopeLevel = scopeLevel;
      this.id = id;
      this.module = module;
      this.type = type;
    }
    public void setAnnotationFunction(Map<ASTNodeSymbol, Integer> symList, List<ASTNodeSymbol> argList, int scopeLevel, String id, String module) {
      this.symList = symList;
      this.argList = argList;
      this.scopeLevel = scopeLevel;
      this.id = id;
      this.module = module;
      this.type = ASTNodeBlok.TYPE_FUNC;
    }
    public void setAnnotationAnonymous(Map<ASTNodeSymbol, Integer> symList, int scopeLevel, String id, String module, List<ASTNodeSymbol> anonDefScopeSymList) {
      this.symList = symList;
      this.scopeLevel = scopeLevel;
      this.id = id;
      this.module = module;
      this.anonDefScopeSymList = new ArrayList<ASTNodeSymbol>(anonDefScopeSymList); 
      this.type = ASTNodeBlok.TYPE_ANON;
    }
    public boolean declaresVariableInThisScope(ASTNodeSymbol sym) {
      return symList.containsKey(sym);
    }
    public boolean isSymbolDeclaredBefore(ASTNodeSymbol sym, int whenceSymNbr) {
      Integer symNbr = symList.get(sym);
      if (symNbr == null) return false;
      //if (CodeGenFront.dbg) System.out.println("       " + sym.symbol + "  " + symNbr.intValue() + " <= " + whenceSymNbr + "?");
      return symNbr.intValue() <= whenceSymNbr; 
    }
    public List<ASTNodeSymbol> getVariables() {
      if (symList == null) return null;
      List<ASTNodeSymbol> vl = new ArrayList<ASTNodeSymbol>();
      vl.addAll(symList.keySet());
      return vl;
    }
    public List<ASTNodeSymbol> getArguments() {
      return this.argList;
    }
    public List<ASTNodeSymbol> getAnonymousDefinedScopeVariables() {
      return this.anonDefScopeSymList;
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
      return !variablesHandled && (
                !symList.isEmpty() || 
                (argList != null && !argList.isEmpty()) ||
                (anonDefScopeSymList != null && !anonDefScopeSymList.isEmpty())
            );
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
      if (argList != null && !argList.isEmpty()) {
        sb.append(" ARGS:");
        for (int i = 0; i < argList.size(); i++) {
          sb.append(argList.get(i).toString());
          if (i < argList.size() - 1)
            sb.append(',');
        }
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
    // the symbol string
    String symbol;
    // if this node is declaring this symbol
    public boolean declare;
    // each time a symbol is encountered in a block, it is assigned a unique increasing number
    public int symNbr;

    public ASTNodeSymbol(String s) {
      super(AST.OP_SYMBOL);
      symbol = s;
    }

    public String toString() {
      return symbol + (declare ? "*" : "");
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
    ASTNodeSymbol name;
    ASTNode callAddrOp;
    // if false, call function denoted by name. if false, call funcion address generated
    // by callAddrOp
    boolean callByOperation;
    int callid;

    public ASTNodeFuncCall(ASTNodeSymbol sym, int callid, ASTNode... operands) {
      super(AST.OP_CALL, operands);
      this.callid = callid;
      callByOperation = false;
      name = sym;
    }
    
    public ASTNodeFuncCall(ASTNode callAddr, int callid, ASTNode... operands) {
      super(AST.OP_CALL, operands);
      this.callid = callid;
      callByOperation = true;
      callAddrOp = callAddr;
    }
    
    public void setArguments(List<ASTNode> args) {
      operands = args;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("<" + (!callByOperation ? name : "FUNCADDR:"+callAddrOp) + ">");
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
  
  public static class ASTNodeArrDecl extends ASTNode {
    int arrid;
    public boolean onlyPrimitives;
    public ASTNodeArrDecl(int id) {
      super(AST.OP_ADECL);
      this.operands = new ArrayList<ASTNode>();
      this.arrid = id;
    }

    public boolean isEmpty() {
      return operands == null || operands.isEmpty();
    }
    
    public boolean containsTuples() {
      boolean gotTuple = false;
      boolean gotEntry = false;
      for (ASTNode e : operands) {
        if (e.op == AST.OP_TUPLE) {
          gotTuple = true;
          if (gotEntry) throw new CompilerError("cannot mix tuples and entries", e);
        }
        else {
          gotEntry = true;
          if (gotTuple) throw new CompilerError("cannot mix tuples and entries", e);
        }
      }
      return gotTuple;
    }

    public String toString() {
      return "arrdeclr" + operands;
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
