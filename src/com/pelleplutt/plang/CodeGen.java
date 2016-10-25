package com.pelleplutt.plang;

import static com.pelleplutt.plang.AST.OP_AND;
import static com.pelleplutt.plang.AST.OP_ANDEQ;
import static com.pelleplutt.plang.AST.OP_ARRAY;
import static com.pelleplutt.plang.AST.OP_BLOK;
import static com.pelleplutt.plang.AST.OP_CALL;
import static com.pelleplutt.plang.AST.OP_DIV;
import static com.pelleplutt.plang.AST.OP_DIVEQ;
import static com.pelleplutt.plang.AST.OP_EQ;
import static com.pelleplutt.plang.AST.OP_EQ2;
import static com.pelleplutt.plang.AST.OP_FOR;
import static com.pelleplutt.plang.AST.OP_GE;
import static com.pelleplutt.plang.AST.OP_GT;
import static com.pelleplutt.plang.AST.OP_HASH;
import static com.pelleplutt.plang.AST.OP_IN;
import static com.pelleplutt.plang.AST.OP_LE;
import static com.pelleplutt.plang.AST.OP_LT;
import static com.pelleplutt.plang.AST.OP_MINUS;
import static com.pelleplutt.plang.AST.OP_MINUSEQ;
import static com.pelleplutt.plang.AST.OP_MOD;
import static com.pelleplutt.plang.AST.OP_MODEQ;
import static com.pelleplutt.plang.AST.OP_MUL;
import static com.pelleplutt.plang.AST.OP_MULEQ;
import static com.pelleplutt.plang.AST.OP_NEQ;
import static com.pelleplutt.plang.AST.OP_NOT;
import static com.pelleplutt.plang.AST.OP_NOTEQ;
import static com.pelleplutt.plang.AST.OP_NUMERICB1;
import static com.pelleplutt.plang.AST.OP_NUMERICB2;
import static com.pelleplutt.plang.AST.OP_NUMERICD;
import static com.pelleplutt.plang.AST.OP_NUMERICH1;
import static com.pelleplutt.plang.AST.OP_NUMERICH2;
import static com.pelleplutt.plang.AST.OP_NUMERICI;
import static com.pelleplutt.plang.AST.OP_OR;
import static com.pelleplutt.plang.AST.OP_OREQ;
import static com.pelleplutt.plang.AST.OP_PLUS;
import static com.pelleplutt.plang.AST.OP_PLUSEQ;
import static com.pelleplutt.plang.AST.OP_QUOTE1;
import static com.pelleplutt.plang.AST.OP_QUOTE2;
import static com.pelleplutt.plang.AST.OP_SHLEFT;
import static com.pelleplutt.plang.AST.OP_SHLEFTEQ;
import static com.pelleplutt.plang.AST.OP_SHRIGHT;
import static com.pelleplutt.plang.AST.OP_SHRIGHTEQ;
import static com.pelleplutt.plang.AST.OP_SYMBOL;
import static com.pelleplutt.plang.AST.OP_XOR;
import static com.pelleplutt.plang.AST.OP_XOREQ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.ASTNode.ASTNodeNumeric;
import com.pelleplutt.plang.ASTNode.ASTNodeString;
import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;

public class CodeGen {
  static final int TINT = 0;
  static final int TFLOAT = 1;
  static final int TSTRING = 2;
  static final int TRANGE = 3;
  static final int TCODE = 4;
  static final String TYPENAMES[] = {
    "INT",
    "FLOAT",
    "STRING",
    "RANGE",
    "CODE"
  };
  
  public CodeGen() {
  }
  
  public Stack<Scope> scopeStack;
  public Scope topScope;
  public List<Scope> anonTopScopes = new ArrayList<Scope>(); 
  
  void var(List<ASTNode> exprs) {
    scopeStack = new Stack<Scope>();
    ASTNodeBlok eblok = new ASTNodeBlok();
    eblok.operands = exprs;
    varAnalyse(eblok, scopeStack, false);
    System.out.println("main scope:");
    System.out.println(topScope);
    System.out.println("anon scope:");
    for (Scope s : anonTopScopes) {
      System.out.println(s);
    }
  }
  
  void errorType(ASTNode e, int type1, int type2) {
    throw new CompilerError("bad types " + TYPENAMES[type1] + ", " + TYPENAMES[type2] + " for " + e);
  }
  
  void errorType(ASTNode e, int type1) {
    throw new CompilerError("bad type " + TYPENAMES[type1] + " for " + e);
  }
  
  void errorSym(ASTNode e, String sym) {
    throw new CompilerError("symbol '" + sym + "' undefined for " + e);
  }
  
  int varAnalyse(ASTNode e, Stack<Scope> scopeStack, boolean operator) {
    //System.out.println(e);
    if (e.op == OP_BLOK) {
      System.out.println("{");
      Scope s = new Scope();
      if (topScope == null) topScope = s;
      scopeStack.push(s);
      if (e.operands != null) {
        for (ASTNode e2 : e.operands) {
          varAnalyse(e2, scopeStack, false);
        }
      }
      System.out.println("}");
      
      Scope scope = scopeStack.pop();
      System.out.println("         " + scope);
      return - 1;
    }
    
    if (e.op == OP_SYMBOL && !operator) {
      defLocal(scopeStack, ((ASTNodeSymbol)e).symbol, TINT);
      return TINT;
    } 
    else 
    if (e.op == OP_SYMBOL && operator) {
      return getType(e, scopeStack, ((ASTNodeSymbol)e).symbol);
    }
    else if (e.op == OP_NUMERICB1 || e.op == OP_NUMERICB2 ||
        e.op == OP_NUMERICH1 || e.op == OP_NUMERICH2 || e.op == OP_NUMERICI) {
      return TINT;
    }
    else if (e.op == OP_NUMERICD) {
      return TFLOAT;
    }
    else if (e.op == OP_QUOTE1 || e.op == OP_QUOTE2) {
      return TSTRING;
    }
    else if (e.op == OP_EQ) {
      ASTNode asignee = e.operands.get(0);
      ASTNode tnode = e.operands.get(1);
      String sym = getSymbol(asignee);
      System.out.println("sym " + e + " assign");
      if (tnode instanceof ASTNodeNumeric) {
        defIfUndef(scopeStack, sym, ((ASTNodeNumeric)tnode).frac ? TFLOAT : TINT);
        return ((ASTNodeNumeric)tnode).frac ? TFLOAT : TINT;
      } else if (tnode instanceof ASTNodeString) {
        defIfUndef(scopeStack, sym, TSTRING);
        return TSTRING;
      } else if (tnode instanceof ASTNodeSymbol) {
        defIfUndef(scopeStack, sym, getType(e, scopeStack, ((ASTNodeSymbol)tnode).symbol));
        return getType(e, scopeStack, ((ASTNodeSymbol)tnode).symbol);
      } else if (tnode instanceof ASTNodeBlok) {
        // TODO handle this somehow, what if anon block redefs a variable?
        //      what if the anon block never runs? 
        defIfUndef(scopeStack, sym, TCODE);
        Scope top = new Scope(scopeStack.get(0));
        anonTopScopes.add(top);
        Stack<Scope> anonScopeStack = new Stack<Scope>();
        anonScopeStack.push(top);
        System.out.println(">>> branch off anon");
        varAnalyse(tnode, anonScopeStack, false);
        System.out.println("<<< branch back anon");
        return TCODE;
      } else {
        int t = varAnalyse(tnode, scopeStack, true);
        defIfUndef(scopeStack, sym, t);
        return t;
      }
    }
    else if (e.op == OP_PLUS || e.op == OP_MINUS) {
      int type1 = varAnalyse(e.operands.get(0), scopeStack, true);
      int type2 = varAnalyse(e.operands.get(1), scopeStack, true);
      if (type1 == type2 && type1 != TCODE) 
        return type1;
      else if (type1 == TSTRING && (type2 == TINT || type2 == TFLOAT) || 
          type2 == TSTRING && (type1 == TINT || type1 == TFLOAT))
        return TSTRING;
      else if (type1 == TFLOAT && type2 == TINT || type2 == TFLOAT && type1 == TINT)
        return TFLOAT;
      errorType(e, type1, type2);
    }
    else if (e.op == OP_MUL || e.op == OP_DIV || e.op == OP_MOD) {
      int type1 = varAnalyse(e.operands.get(0), scopeStack, true);
      int type2 = varAnalyse(e.operands.get(1), scopeStack, true);
      if (type1 == type2 && type1 != TCODE && type1 != TSTRING) 
        return type1;
      else if (e.op == OP_MUL && (type1 == TSTRING && type2 == TINT || 
                                  type2 == TSTRING && type1 == TINT))
        return TSTRING;
      else if (type1 == TFLOAT && type2 == TINT || type2 == TFLOAT && type1 == TINT)
        return TFLOAT;
      errorType(e, type1, type2);
    }
    else if (e.op == OP_AND || e.op == OP_OR || e.op == OP_XOR || e.op == OP_SHLEFT ||
        e.op == OP_SHRIGHT) {
      int type1 = varAnalyse(e.operands.get(0), scopeStack, true);
      int type2 = varAnalyse(e.operands.get(1), scopeStack, true);
      if (type1 == type2 && type1 == TINT) 
        return type1;
      errorType(e, type1, type2);
    }
    else if (e.op == OP_NOT) {
      int type1 = varAnalyse(e.operands.get(0), scopeStack, true);
      if (type1 == TINT) 
        return type1;
      errorType(e, type1);
    }
    else if (e.op == OP_PLUSEQ || e.op == OP_MINUSEQ) {
      String sym = ((ASTNodeSymbol)e.operands.get(0)).symbol;
      int type1 = getType(e, scopeStack, sym);
      int type2 = varAnalyse(e.operands.get(1), scopeStack, true);
      int type = -1;
      if (type1 == type2 && type1 != TCODE) 
        type = type1;
      else if (type1 == TSTRING && (type2 == TINT || type2 == TFLOAT) || 
          type2 == TSTRING && (type1 == TINT || type1 == TFLOAT))
        type = TSTRING;
      else if (type1 == TFLOAT && type2 == TINT || type2 == TFLOAT && type1 == TINT)
        type = TFLOAT;
      else 
        errorType(e, type1, type2);
      defIfUndef(scopeStack, sym, type);

      return type;
    }
    else if (e.op == OP_MULEQ) {
    }
    else if (e.op == OP_DIVEQ) {
    }
    else if (e.op == OP_MODEQ) {
    }
    else if (e.op == OP_ANDEQ) {
    }
    else if (e.op == OP_OREQ) {
    }
    else if (e.op == OP_XOREQ) {
    }
    else if (e.op == OP_SHLEFTEQ) {
    }
    else if (e.op == OP_SHRIGHTEQ) {
    }
    else if (e.op == OP_NOTEQ) {
    }
    else if (e.op == OP_EQ2) {
    }
    else if (e.op == OP_NEQ) {
    }
    else if (e.op == OP_GT) {
    }
    else if (e.op == OP_GE) {
    }
    else if (e.op == OP_LT) {
    }
    else if (e.op == OP_LE) {
    }
    else if (e.op == OP_CALL) {
    }
    else if (e.op == OP_FOR) {
      if (e.operands.get(1).op == OP_IN) {
        int type = varAnalyse(e.operands.get(1), scopeStack, false);
        String sym = ((ASTNodeSymbol)e.operands.get(0)).symbol;
        defIfUndef(scopeStack, sym, type);
        varAnalyse(e.operands.get(2), scopeStack, false);
        return type;
      } else {
        // TODO
      }
    }
    else if (e.op == OP_IN) {
      return varAnalyse(e.operands.get(0), scopeStack, true);
    }
    else if (e.op == OP_HASH) {
      int type1 = varAnalyse(e.operands.get(0), scopeStack, true);
      int type2 = varAnalyse(e.operands.get(1), scopeStack, true);
      if (type1 == type2 && type1 != TCODE && (type1 == TINT || type1 == TFLOAT)) 
        return type1;
      else if (type1 == TFLOAT && type2 == TINT || type2 == TFLOAT && type1 == TINT)
        return TFLOAT;
      errorType(e, type1, type2);
    }

    else {
      if (e.operands != null) {
        for (ASTNode e2 : e.operands) {
          varAnalyse(e2, scopeStack, operator);
        }
      }
      return -1;
    }
    return -1;
  }
  
  public static void check(List<ASTNode> exprs) {
    CodeGen cg = new CodeGen();
    System.out.println("  * analyse variables");
    cg.var(exprs);
  }
  
  String getSymbol(ASTNode e) {
    if (e.op == OP_SYMBOL) {
      return ((ASTNodeSymbol)e).symbol;
    } else if (e.op == OP_ARRAY) {
      return getSymbol(e.operands.get(0));
    } else {
      errorType(e, OP_SYMBOL);
      return null;
    }
  }

  void defIfUndef(Stack<Scope> scopeStack, String sym, int type) {
    for (int i = scopeStack.size()-1; i >= 0; i--) {
      Scope s = scopeStack.get(i);
      if (s.symMap.containsKey(sym)) {
        // is defined already, so redef
        int typeold = s.symMap.get(sym);
        System.out.println("  ! '" + sym + "' retype  " + TYPENAMES[type] + ", was " + TYPENAMES[typeold]);
        s.symMap.put(sym, type);
        return;
      };
    }
    scopeStack.peek().symMap.put(sym, type);
    System.out.println("  + '" + sym + "' settype " + TYPENAMES[type]);
  }
  
  void defLocal(Stack<Scope> scopeStack, String sym, int type) {
    scopeStack.peek().symMap.put(sym, type);
    System.out.println("  + '" + sym + "' LOCAL settype " + TYPENAMES[type]);
  }
  
  boolean isDef(Stack<Scope> scopeStack, String sym) {
    for (int i = scopeStack.size()-1; i >= 0; i--) {
      Scope s = scopeStack.get(i);
      if (s.symMap.containsKey(sym)) return true;
    }
    return false;
  }
  
  int getType(ASTNode e, Stack<Scope> scopeStack, String sym) {
    for (int i = scopeStack.size()-1; i >= 0; i--) {
      Scope s = scopeStack.get(i);
      if (s.symMap.containsKey(sym)) return s.symMap.get(sym);
    }
    errorSym(e, sym);
    return -1;
  }
  
  class Scope {
    Map<String, Integer> symMap;
    public Scope() {
      symMap = new HashMap<String, Integer>();
    }
    public Scope(Scope s) {
      symMap = new HashMap<String, Integer>();
      symMap.putAll(s.symMap);
    }
    public String toString() {
      StringBuilder sb = new StringBuilder();
      for (String key : symMap.keySet()) {
        sb.append(key + ":" + TYPENAMES[symMap.get(key)] + "  ");
      }
      return sb.toString();
    }
  }
}