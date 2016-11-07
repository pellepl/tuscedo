package com.pelleplutt.plang;

import static com.pelleplutt.plang.AST.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.ASTNode.ASTNodeRange;
import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;

// TODO handle 'global' keyword
// TODO handle 'func' keyword
// TODO figure out modules
public class StructAnalysis {
  public Stack<Scope> scopeStack;
  String module = ".main";
  int blockId;
  
  public StructAnalysis() {
  }
  
  String getBlockId() {
    return ("." + Integer.toString(blockId++));
  }
  
  void structAnalyse(ASTNodeBlok e) {
    scopeStack = new Stack<Scope>();
    rec(e, scopeStack, null, false, false);
  }

  void rec(ASTNode e, Stack<Scope> scopeStack, ASTNode parent, boolean operator, boolean loop) {
    if (e.op == OP_MODULE) {
      module = ((ASTNodeSymbol)e.operands.get(0)).symbol;
    } 
    else if (e.op == OP_BLOK) {
      ASTNodeBlok be = (ASTNodeBlok)e;
      Scope s = new Scope(be);
      String blockId = getBlockId();
      if (!scopeStack.isEmpty()) {
        be.parent = scopeStack.peek().block;
      }
      scopeStack.push(s);
      if (e.operands != null) {
        for (ASTNode e2 : e.operands) {
          rec(e2, scopeStack, e, false, loop);
        }
      }
      Scope scope = scopeStack.pop();
      be.setAnnotation(scope.symMap, scopeStack.size(), blockId, module, ASTNodeBlok.MAIN);
    }
    else if (e.op == OP_SYMBOL && !operator) {
      defVar(scopeStack, ((ASTNodeSymbol)e).symbol, (ASTNodeSymbol)e);
    } 
    else if (e.op == OP_SYMBOL && operator) {
      // do naught
    } 
    else if (e.op == OP_EQ) {
      ASTNode asignee = e.operands.get(0);
      ASTNode tnode = e.operands.get(1);
      String var = getVariableName(asignee);
      defVarIfUndef(scopeStack, var, (ASTNodeSymbol)asignee);
      if (tnode instanceof ASTNodeBlok) {
        // anonymous scope
        ASTNodeBlok be = (ASTNodeBlok)tnode;
        Scope globalScope = scopeStack.get(0);
        be.parent = globalScope.block;
        
        String blockId = getBlockId() + "A";
        Stack<Scope> anonScopeStack = new Stack<Scope>();
        anonScopeStack.push(globalScope);
        Scope anonScope = new Scope(globalScope.block);
        anonScopeStack.push(anonScope);
        System.out.println(">>> branch off anon");
        if (tnode.operands != null) {
          for (ASTNode e2 : tnode.operands) {
            rec(e2, anonScopeStack, e, false, false);
          }
        }
        be.setAnnotation(anonScope.symMap, anonScopeStack.size(), blockId, module, ASTNodeBlok.ANON);
        System.out.println("<<< branch back anon");
      } else {
        if (e.operands != null) {
          for (ASTNode e2 : e.operands) {
            rec(e2, scopeStack, e, true, loop);
          }
        }
      }
    } else if (AST.isOperator(e.op)) {
      if (e.operands != null) {
        for (ASTNode e2 : e.operands) {
          rec(e2, scopeStack, e, true, loop);
        }
      }
    } else if (e.op == OP_HASH) {
      // check hash construct (error not covered by grammar) and replace by a range node
      if (e.operands.get(0).op == OP_HASH) {
        ASTNode sub = e.operands.get(0);
        if (sub.operands.get(0).op == OP_HASH) {
          throw new CompilerError("cannot nest ranges ", e);
        }
        ASTNode range = new ASTNodeRange(sub.operands.get(0), sub.operands.get(1), e.operands.get(1)); 
        parent.operands.set(parent.operands.indexOf(e), range);
        rec(range, scopeStack, parent, true, loop);
      } else {
        ASTNode range = new ASTNodeRange(e.operands.get(0), e.operands.get(1));
        parent.operands.set(parent.operands.indexOf(e), range);
        rec(range, scopeStack, parent, true, loop);
      }
    }
    else if (e.op == OP_CALL) {
      if (e.operands != null) {
        for (ASTNode e2 : e.operands) {
          rec(e2, scopeStack, e, true, false);
        }
      }
    }
    else if (e.op == OP_FOR) {
      if (e.operands.size() == 4) {
        // for (x;y;z) w
        rec(e.operands.get(0), scopeStack, e, false, loop);
        rec(e.operands.get(1), scopeStack, e, true, loop);
        rec(e.operands.get(2), scopeStack, e, true, loop);
        rec(e.operands.get(3), scopeStack, e, false, true);
      } else {
        // for (x in y) w
        rec(e.operands.get(0), scopeStack, e, false, loop);
        rec(e.operands.get(1), scopeStack, e, true, loop);
        rec(e.operands.get(2), scopeStack, e, false, true);
      }
    }
    else if (e.op == OP_WHILE) {
      rec(e.operands.get(0), scopeStack, e, false, loop);
      rec(e.operands.get(1), scopeStack, e, true, true);
    }
    else if (e.op == OP_BREAK) {
      if (!loop) throw new CompilerError("break without loop", e);
    }
    else if (e.op == OP_CONTINUE) {
      if (!loop) throw new CompilerError("continue without loop", e);
    }
    else {
      if (e.operands != null) {
        for (ASTNode e2 : e.operands) {
          rec(e2, scopeStack, e, operator, loop);
        }
      }
    }
 }
  
  public static void analyse(ASTNodeBlok e) {
    StructAnalysis cg = new StructAnalysis();
    System.out.println("  * analyse variables");
    cg.structAnalyse(e);
  }
  
  String getVariableName(ASTNode e) {
    if (e.op == OP_SYMBOL) {
      return ((ASTNodeSymbol)e).symbol;
    } else if (e.op == OP_ARRAY) {
      return getVariableName(e.operands.get(0));
    }
    return null;
  }

  // define variable for scope if not already reachable
  void defVarIfUndef(Stack<Scope> scopeStack, String sym, ASTNodeSymbol esym) {
    for (int i = scopeStack.size()-1; i >= 0; i--) {
      Scope s = scopeStack.get(i);
      if (s.symMap.containsKey(sym)) {
        // is defined already
        return;
      };
    }
    scopeStack.peek().symMap.put(sym, esym);
    System.out.println("  + '" + sym + "' " + (scopeStack.size() == 1 ? "GLOBAL" : "LOCAL"));
  }
  
  // define variable for scope, if same name already reachable this will shadow ancestor
  void defVar(Stack<Scope> scopeStack, String sym, ASTNodeSymbol esym) {
    boolean shadow = isVarDefAbove(scopeStack, sym);
    scopeStack.peek().symMap.put(sym, esym);
    System.out.println("  + '" + sym + "' " + (scopeStack.size() == 1 ? "GLOBAL" : "LOCAL") + (shadow ? " SHADOW" : ""));
  }
  
  // see if a variable is reachable from this scope and ancestors
  boolean isVarDef(Stack<Scope> scopeStack, String sym) {
    for (int i = scopeStack.size()-1; i >= 0; i--) {
      Scope s = scopeStack.get(i);
      if (s.symMap.containsKey(sym)) return true;
    }
    return false;
  }
  
  // see if a variable is reachable in ancestors only
  boolean isVarDefAbove(Stack<Scope> scopeStack, String sym) {
    for (int i = scopeStack.size()-2; i >= 0; i--) {
      Scope s = scopeStack.get(i);
      if (s.symMap.containsKey(sym)) return true;
    }
    return false;
  }
  
  class Scope {
    Map<String, ASTNodeSymbol> symMap;
    ASTNodeBlok block;
    public Scope(ASTNodeBlok e) {
      this.block = e;
      symMap = new HashMap<String, ASTNodeSymbol>();
    }
    public String toString() {
      StringBuilder sb = new StringBuilder();
      for (String key : symMap.keySet()) {
        sb.append(key + "  ");
      }
      return sb.toString();
    }
  }
}