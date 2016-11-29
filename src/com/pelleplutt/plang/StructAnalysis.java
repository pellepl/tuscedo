package com.pelleplutt.plang;

import static com.pelleplutt.plang.AST.OP_ADECL;
import static com.pelleplutt.plang.AST.OP_ADEREF;
import static com.pelleplutt.plang.AST.OP_BLOK;
import static com.pelleplutt.plang.AST.OP_BREAK;
import static com.pelleplutt.plang.AST.OP_CALL;
import static com.pelleplutt.plang.AST.OP_CONTINUE;
import static com.pelleplutt.plang.AST.OP_DOT;
import static com.pelleplutt.plang.AST.OP_ELSE;
import static com.pelleplutt.plang.AST.OP_EQ;
import static com.pelleplutt.plang.AST.OP_FOR;
import static com.pelleplutt.plang.AST.OP_FUNCDEF;
import static com.pelleplutt.plang.AST.OP_HASH;
import static com.pelleplutt.plang.AST.OP_IF;
import static com.pelleplutt.plang.AST.OP_MODULE;
import static com.pelleplutt.plang.AST.OP_RETURN;
import static com.pelleplutt.plang.AST.OP_SYMBOL;
import static com.pelleplutt.plang.AST.OP_TUPLE;
import static com.pelleplutt.plang.AST.OP_WHILE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.pelleplutt.plang.ASTNode.ASTNodeArrDecl;
import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.ASTNode.ASTNodeFuncDef;
import com.pelleplutt.plang.ASTNode.ASTNodeNumeric;
import com.pelleplutt.plang.ASTNode.ASTNodeRange;
import com.pelleplutt.plang.ASTNode.ASTNodeString;
import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;

public class StructAnalysis {
  static boolean dbg = false;
  public ScopeStack mainScopeStack;
  String module = ".main";
  int blockId;
  
  public StructAnalysis() {
  }
  
  String getBlockId() {
    return ("." + Integer.toString(blockId++));
  }
  
  void structAnalyse(ASTNodeBlok e) {
    mainScopeStack = new ScopeStack();
    analyseRecurse(e, mainScopeStack, null, false, false);
  }
  

  void analyseRecurse(ASTNode e, ScopeStack scopeStack, ASTNode parentNode, boolean operator, boolean loop) {
    // System.out.println("*** anaylyseRec " + e);
    // System.out.println("    scopeStack:" + scopeStack.id + " " + scopeStack);
    if (e.op == OP_MODULE) {
      module = ((ASTNodeSymbol)e.operands.get(0)).symbol;
      if (scopeStack.size() > 1) {
        throw new CompilerError("cannot declare module '"+module+"' within a scope", e);
      }
      
    } 
    else if (e.op == OP_BLOK) {
      ASTNodeBlok eblok = (ASTNodeBlok)e;
      //if (dbg) System.out.println("ENTER eblk " + e);
      Scope newScope = new Scope(eblok);
      String blockId = getBlockId();
      if (!scopeStack.isEmpty()) {
        eblok.parentBlock = scopeStack.peek().block;
        // System.out.println("     PRENT " + scopeStack.peek() + " block:" + scopeStack.peek().block);
      }
      scopeStack.push(newScope);
      if (eblok.operands != null) {
        for (ASTNode e2 : eblok.operands) {
          analyseRecurse(e2, scopeStack, eblok, false, loop);
        }
      }
      Scope scope = scopeStack.pop();
      eblok.setAnnotation(scope.symVars, scopeStack.size(), blockId, module, ASTNodeBlok.TYPE_MAIN);
      //if (dbg) System.out.println("LEAVE eblk " + eblok + " got symbols " + scope.symList);
    }
    else if (e.op == OP_SYMBOL && !operator) {
      defVar(scopeStack, (ASTNodeSymbol)e);
    } 
    else if (e.op == OP_SYMBOL && operator) {
      // nothung
    }
    else if (e.op == OP_RETURN) {
      if (e.operands!=null && e.operands.size()>0) {
        analyseRecurse(e.operands.get(0), scopeStack, parentNode, true, loop);
        if (e.operands.get(0) instanceof ASTNodeBlok) {
          newAnonymousScope((ASTNodeBlok)e.operands.get(0), e);
        }
      }
    }
    else if (e.op == OP_FUNCDEF) {
      // funcdef scope
      // System.out.println("ENTER func " + e);
      ASTNodeFuncDef fe = (ASTNodeFuncDef)e;
      Scope globalScope = mainScopeStack.get(0);
      ASTNodeBlok be = (ASTNodeBlok)fe.operands.get(0);
      be.parentBlock = globalScope.block;
      
      String blockId = "." + fe.name;
      ScopeStack funcScopeStack = new ScopeStack();
      funcScopeStack.push(globalScope);
      Scope funcScope = new Scope(be);
      funcScopeStack.push(funcScope);
      // add arguments to arglist
      for (ASTNode earg: fe.arguments) {
        ASTNodeSymbol symarg = (ASTNodeSymbol)earg;
        funcScope.symArgs.add(symarg);
      }
      if (dbg) System.out.println(">>> branch off funcdef " + blockId  + " args:" + funcScope.symArgs);
      if (be.operands != null) {
        for (ASTNode e2 : be.operands) {
          analyseRecurse(e2, funcScopeStack, e, false, false);
        }
      }
      be.setAnnotation(funcScope.symVars, funcScope.symArgs, funcScopeStack.size(), blockId, module, 
          ASTNodeBlok.TYPE_FUNC);
      if (dbg) System.out.println("<<< branch back funcdef " + blockId);
      // System.out.println("LEAVE func " + fe + " got symbols " + funcScope.symList + " and args " + funcScope.symArgs);
    } 
    else if (e.op == OP_EQ) {
      ASTNode asignee = e.operands.get(0);
      ASTNode tnode = e.operands.get(1);
      if (asignee.op != OP_DOT) {
        ASTNodeSymbol var = getVariableName(asignee);
        defVarIfUndef(scopeStack, (ASTNodeSymbol)var);
      }
        
      if (tnode instanceof ASTNodeBlok) {
        // anonymous scope
        newAnonymousScope((ASTNodeBlok)tnode, e);
      } else {
        if (e.operands != null) {
          for (ASTNode e2 : e.operands) {
            analyseRecurse(e2, scopeStack, e, true, loop);
          }
        }
      }
    } else if (AST.isOperator(e.op)) {
      if (AST.isAssignOperator(e.op)) {
        analyseRecurse(e.operands.get(0), scopeStack, e, true, loop);
        if (e.operands.get(1) instanceof ASTNodeBlok) {
          newAnonymousScope((ASTNodeBlok)e.operands.get(1), e);
        } else {
          analyseRecurse(e.operands.get(1), scopeStack, e, true, loop);
        }
      } else {
        if (e.operands != null) {
          for (ASTNode e2 : e.operands) {
            analyseRecurse(e2, scopeStack, e, true, loop);
          }
        }
      }
    } else if (e.op == OP_HASH) {
      // check hash construct (error not covered by grammar) and replace by a range node
      if (e.operands.get(0).op == OP_HASH) {
        ASTNode sub = e.operands.get(0);
        if (sub.operands.get(0).op == OP_HASH) {
          throw new CompilerError("cannot nest ranges", e);
        }
        ASTNode range = new ASTNodeRange(sub.operands.get(0), sub.operands.get(1), e.operands.get(1)); 
        parentNode.operands.set(parentNode.operands.indexOf(e), range);
        analyseRecurse(range, scopeStack, parentNode, true, loop);
      } else {
        ASTNode range = new ASTNodeRange(e.operands.get(0), e.operands.get(1));
        parentNode.operands.set(parentNode.operands.indexOf(e), range);
        analyseRecurse(range, scopeStack, parentNode, true, loop);
      }
    }
    else if (e.op == OP_CALL) {
      if (e.operands != null) {
        for (ASTNode e2 : e.operands) {
          analyseRecurse(e2, scopeStack, e, true, false);
        }
      }
    }
    else if (e.op == OP_FOR) {
      if (e.operands.size() == 4) {
        // for (x;y;z) w
        analyseRecurse(e.operands.get(0), scopeStack, e, false, loop);
        analyseRecurse(e.operands.get(1), scopeStack, e, true, loop);
        analyseRecurse(e.operands.get(2), scopeStack, e, true, loop);
        analyseRecurse(e.operands.get(3), scopeStack, e, false, true);
      } else {
        // for (x in y) w
        analyseRecurse(e.operands.get(0), scopeStack, e, false, loop);
        analyseRecurse(e.operands.get(1), scopeStack, e, true, loop);
        analyseRecurse(e.operands.get(2), scopeStack, e, false, true);
      }
    }
    else if (e.op == OP_WHILE) {
      analyseRecurse(e.operands.get(0), scopeStack, e, false, loop);
      analyseRecurse(e.operands.get(1), scopeStack, e, true, true);
    }
    else if (e.op == OP_BREAK) {
      if (!loop) throw new CompilerError("break without loop", e);
    }
    else if (e.op == OP_CONTINUE) {
      if (!loop) throw new CompilerError("continue without loop", e);
    }
    else if (e.op == OP_IF) {
      boolean hasElse = e.operands.size() == 3;
      analyseRecurse(e.operands.get(0), scopeStack, e, false, loop);
      analyseRecurse(e.operands.get(1), scopeStack, e, true, loop);
      if (hasElse) {
        analyseRecurse(e.operands.get(2), scopeStack, e, true, loop);
      }
    }
    else if (e.op == OP_ELSE) {
      analyseRecurse(e.operands.get(0), scopeStack, e, true, loop);
    }
    else if (e.op == OP_ADECL) {
      boolean onlyPrimitives = true;
      if (e.operands == null || e.operands.isEmpty()) {
        onlyPrimitives = false;
      } else {
        for (ASTNode e2 : e.operands) {
          if (!(e2 instanceof ASTNodeNumeric || e2 instanceof ASTNodeString || e2 instanceof ASTNodeBlok)) {
            onlyPrimitives = false;
          }
          if (e2 instanceof ASTNodeBlok) {
            newAnonymousScope((ASTNodeBlok)e2, e);
          } else {
            analyseRecurse(e2, scopeStack, e, operator, loop);
          }
        }
      }
      ((ASTNodeArrDecl)e).onlyPrimitives = onlyPrimitives;
    }    
    else if (e.op == OP_TUPLE) {
      // key
      analyseRecurse(e.operands.get(0), scopeStack, e, operator, loop);
      //val
      ASTNode val = e.operands.get(1);
      if (val instanceof ASTNodeBlok) {
        newAnonymousScope((ASTNodeBlok)val, e);
      } else {
        analyseRecurse(val, scopeStack, e, operator, loop);
      }
    }
    else {
      if (e.operands != null) {
        for (ASTNode e2 : e.operands) {
          analyseRecurse(e2, scopeStack, e, operator, loop);
        }
      }
    }
  }
  
  void newAnonymousScope(ASTNodeBlok be, ASTNode parent) {
    Scope globalScope = mainScopeStack.get(0);
    be.parentBlock = globalScope.block;
    
    String blockId = getBlockId() + "A";
    ScopeStack anonScopeStack = new ScopeStack();
    anonScopeStack.push(globalScope);
    Scope anonScope = new Scope(globalScope.block);
    anonScopeStack.push(anonScope);
    if (dbg) System.out.println(">>> branch off anon");
    if (be.operands != null) {
      for (ASTNode e2 : be.operands) {
        analyseRecurse(e2, anonScopeStack, parent, false, false);
      }
    }
    be.setAnnotation(anonScope.symVars, anonScopeStack.size(), blockId, module, ASTNodeBlok.TYPE_ANON);
    if (dbg) System.out.println("<<< branch back anon");
  }
  
  public static void analyse(ASTNodeBlok e) {
    StructAnalysis cg = new StructAnalysis();
    System.out.println("  * analyse variables");
    cg.structAnalyse(e);
  }
  
  ASTNodeSymbol getVariableName(ASTNode e) {
    if (e.op == OP_SYMBOL) {
      return ((ASTNodeSymbol)e);
    } else if (e.op == OP_ADEREF || e.op == OP_DOT) {
      return getVariableName(e.operands.get(0));
    }
    return null;
  }

  // define variable for scope if not already reachable
  void defVarIfUndef(ScopeStack scopeStack, ASTNodeSymbol esym) {
    if (esym == null) throw new CompilerError("fatal");
    for (int i = scopeStack.size()-1; i >= 0; i--) {
      Scope s = scopeStack.get(i);
      if (s.symVars.keySet().contains(esym)) {
        // is defined already
        return;
      };
      if (s.symArgs.contains(esym)) {
        // is defined already
        return;
      };
    }
    scopeStack.peek().symVars.put(esym, esym.symNbr);
    esym.declare = true;
    if (dbg) System.out.println("  + '" + esym.symbol + "' " + (scopeStack.size() == 1 ? "GLOBAL" : "LOCAL"));
  }
  
  // define variable for scope, if same name already reachable this will shadow ancestor
  void defVar(ScopeStack scopeStack, ASTNodeSymbol esym) {
    boolean shadow = isVarDefAbove(scopeStack, esym);
    scopeStack.peek().symVars.put(esym, esym.symNbr);
    esym.declare = true;
    if (dbg) System.out.println("  + '" + esym.symbol + "' " + (scopeStack.size() == 1 ? "GLOBAL" : "LOCAL") + (shadow ? " SHADOW" : ""));
  }
  
  // see if a variable is reachable from this scope and ancestors
  boolean isVarDef(ScopeStack scopeStack, ASTNodeSymbol esym) {
    for (int i = scopeStack.size()-1; i >= 0; i--) {
      Scope s = scopeStack.get(i);
      if (s.symVars.keySet().contains(esym)) return true;
      if (s.symArgs.contains(esym)) return true;
    }
    return false;
  }
  
  // see if a variable is reachable in ancestors only
  boolean isVarDefAbove(ScopeStack scopeStack, ASTNodeSymbol sym) {
    for (int i = scopeStack.size()-2; i >= 0; i--) {
      Scope s = scopeStack.get(i);
      if (s.symVars.keySet().contains(sym)) return true;
      if (s.symArgs.contains(sym)) return true;
    }
    return false;
  }
  
  class Scope {
    Map<ASTNodeSymbol, Integer> symVars;
    List<ASTNodeSymbol> symArgs;
    ASTNodeBlok block;
    public Scope(ASTNodeBlok e) {
      this.block = e;
      symVars = new HashMap<ASTNodeSymbol, Integer>();
      symArgs = new ArrayList<ASTNodeSymbol>();
    }
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('(');
      for (ASTNodeSymbol esym : symVars.keySet()) {
        sb.append("@" + esym.symbol + " ");
      }
      if (symArgs != null) {
        for (ASTNodeSymbol esym : symArgs) {
          sb.append("$" + esym.symbol + " ");
        }
      }
      sb.append(')');
      return sb.toString();
    }
  }
  
  static class ScopeStack extends Stack<Scope> {
    static int __id = 0;
    int id = __id++;
  }
}