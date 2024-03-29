package com.pelleplutt.operandi;

import static com.pelleplutt.operandi.AST.OP_ADECL;
import static com.pelleplutt.operandi.AST.OP_ADEREF;
import static com.pelleplutt.operandi.AST.OP_BLOK;
import static com.pelleplutt.operandi.AST.OP_BREAK;
import static com.pelleplutt.operandi.AST.OP_CALL;
import static com.pelleplutt.operandi.AST.OP_CONTINUE;
import static com.pelleplutt.operandi.AST.OP_DOT;
import static com.pelleplutt.operandi.AST.OP_ELSE;
import static com.pelleplutt.operandi.AST.OP_EQ;
import static com.pelleplutt.operandi.AST.OP_FOR;
import static com.pelleplutt.operandi.AST.OP_FOR_IN;
import static com.pelleplutt.operandi.AST.OP_FUNCDEF;
import static com.pelleplutt.operandi.AST.OP_GLOBAL;
import static com.pelleplutt.operandi.AST.OP_HASH;
import static com.pelleplutt.operandi.AST.OP_IF;
import static com.pelleplutt.operandi.AST.OP_IN;
import static com.pelleplutt.operandi.AST.OP_MODULE;
import static com.pelleplutt.operandi.AST.OP_NIL;
import static com.pelleplutt.operandi.AST.OP_RETURN;
import static com.pelleplutt.operandi.AST.OP_SYMBOL;
import static com.pelleplutt.operandi.AST.OP_TUPLE;
import static com.pelleplutt.operandi.AST.OP_WHILE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.pelleplutt.operandi.ASTNode.ASTNodeArrDecl;
import com.pelleplutt.operandi.ASTNode.ASTNodeBlok;
import com.pelleplutt.operandi.ASTNode.ASTNodeFuncDef;
import com.pelleplutt.operandi.ASTNode.ASTNodeNumeric;
import com.pelleplutt.operandi.ASTNode.ASTNodeOp;
import com.pelleplutt.operandi.ASTNode.ASTNodeRange;
import com.pelleplutt.operandi.ASTNode.ASTNodeString;
import com.pelleplutt.operandi.ASTNode.ASTNodeSymbol;

/**
 * Does a contextual analysis to find errors not covered in grammar 
 * and collect variable information (e.g. scope information) for future 
 * passes.
 * @author petera
 */
public class StructAnalysis {
  public static boolean dbg = false;
  Stack<Scope> mainScopeStack;
  Stack<Fragment> fragmentStack;
  
  int prevOp, prevPrevOp, _prevOp;
  
  String module = ".main";
  int blockId;
  IntermediateRepresentation irep;
  
  public StructAnalysis() {
    prevOp = prevPrevOp = AST._OP_FINAL;
  }
  
  String getBlockId() {
    return ("." + Integer.toString(blockId++));
  }
  
  void structAnalyse(ASTNodeBlok e) {
    mainScopeStack = new Stack<Scope>();
    fragmentStack = new Stack<Fragment>(); 
    analyseRecurse(e, mainScopeStack, null, false, false);
  }
  
  void analyseRecurse(ASTNode e, Stack<Scope> scopeStack, ASTNode parentNode, boolean operator, boolean loop) {
    prevPrevOp = prevOp;
    prevOp = _prevOp;
    _prevOp = e.op;
    //System.out.println("*** anaylyseRec oper:" + operator + " loop:" + loop + "   " + e + ":" + AST.opString(e.op));
    //System.out.println("    scopeStack:" + scopeStack.id + " " + scopeStack);
    if (e.op == OP_MODULE) {
      module = ((ASTNodeSymbol)e.operands.get(0)).symbol;
      if (scopeStack.size() > 1) {
        throw new CompilerError("cannot declare module '"+module+"' within a scope", e);
      }
    }
    else if (e.op == OP_BLOK) {
      ASTNodeBlok eblok = (ASTNodeBlok)e;
      Scope newScope = new Scope(eblok);
      String blockId = getBlockId();
      if (!scopeStack.isEmpty()) {
        eblok.parentBlock = scopeStack.peek().block;
        // System.out.println("     PRENT " + scopeStack.peek() + " block:" + scopeStack.peek().block);
      }
      if (dbg) System.out.println("ENTER eblk " + e);
      if (dbg) System.out.println("      prnt " + eblok.parentBlock);
      scopeStack.push(newScope);
      if (eblok.operands != null) {
        for (ASTNode e2 : eblok.operands) {
          analyseRecurse(e2, scopeStack, eblok, false, loop);
        }
      }
      Scope scope = scopeStack.pop();
      eblok.setAnnotation(scope.symVars, scopeStack.size(), blockId, module, ASTNodeBlok.TYPE_MAIN);
      if (dbg) System.out.println("LEAVE eblk " + eblok + " got symbols " + scope.symVars);
    }
    else if (e.op == OP_GLOBAL) {
      if (!(prevOp == OP_BLOK || prevOp == OP_FUNCDEF || prevPrevOp == OP_GLOBAL && prevOp == OP_SYMBOL)) {
//        System.out.println(AST.opString(prevPrevOp) +  "  " 
//            + AST.opString(prevOp) +  "  " +
//            AST.opString(e.op)); 
        throw new CompilerError("global only allowed in scope start", e);
      }
      analyseRecurse(e.operands.get(0), scopeStack, e, true, loop);
    }
    else if (e.op == OP_SYMBOL && !operator) {
      if (((ASTNodeSymbol)e).symbol.charAt(0) != '$') {
        defVar(scopeStack, (ASTNodeSymbol)e, parentNode);
      }
    } 
    else if (e.op == OP_SYMBOL && operator) {
      ASTNodeSymbol esym = (ASTNodeSymbol)e;
      if (parentNode.op == OP_GLOBAL) {
        defVarIfUndefGlobal(scopeStack, esym, parentNode);
      }
      else if (!fragmentStack.isEmpty()) {
        // within an anonymous scope, check if this is a variable referring
        // to outside
        if (!fragmentStack.peek().anonDefLocals.contains(esym) &&
            !isLocalVarDef(scopeStack, esym) &&
            isExternalVarReachableDef(esym)) {
          fragmentStack.peek().anonDefLocals.add(esym);
        }
      }
    }
    else if (e.op == OP_RETURN) {
      if (e.operands!=null && e.operands.size()>0) {
        if (e.operands.get(0) instanceof ASTNodeBlok) {
          newAnonymousScope((ASTNodeBlok)e.operands.get(0), scopeStack, e);
        } else {
          analyseRecurse(e.operands.get(0), scopeStack, parentNode, true, loop);
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
      Stack<Scope> funcScopeStack = new Stack<Scope>();
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
      be.setAnnotationFunction(funcScope.symVars, funcScope.symArgs, funcScopeStack.size(), blockId, module);
      if (dbg) System.out.println("<<< branch back funcdef " + blockId);
      // System.out.println("LEAVE func " + fe + " got symbols " + funcScope.symList + " and args " + funcScope.symArgs);
    } 
    else if (e.op == OP_EQ) {
      ASTNode assignee = e.operands.get(0);
      ASTNode tnode = e.operands.get(1);
      
      if (assignee.op == OP_SYMBOL && ((ASTNodeSymbol)assignee).symbol.charAt(0) == '$') {
        throw new CompilerError("cannot assign argument variables", e);
      }
      
      if (assignee.op != OP_DOT && assignee.op != OP_ADEREF) {
        ASTNodeSymbol var = getVariableName(assignee);
        if (var.symbol.charAt(0) == '$') {
          throw new CompilerError("cannot assign argument variables", e);
        }
        defVarIfUndef(scopeStack, (ASTNodeSymbol)var, e);
      }
        
      if (tnode instanceof ASTNodeBlok) {
        // anonymous scope
        newAnonymousScope((ASTNodeBlok)tnode, scopeStack, e);
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
          newAnonymousScope((ASTNodeBlok)e.operands.get(1), scopeStack, e);
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
          if (e2 instanceof ASTNodeBlok) {
            newAnonymousScope((ASTNodeBlok)e2, scopeStack, e);
          } else {
            analyseRecurse(e2, scopeStack, e, true, false);
          }
        }
      }
    }
    else if (e.op == OP_FOR) {
      if (e.operands.size() == 4) {
        // for (x;y;z) w
        analyseRecurse(e.operands.get(0), scopeStack, e, false, loop); // for (x;y;z) <-- may declare x -> false
        analyseRecurse(e.operands.get(1), scopeStack, e, true, loop);
        analyseRecurse(e.operands.get(2), scopeStack, e, true, loop);
        analyseRecurse(e.operands.get(3), scopeStack, e, false, true);
      } else if (e.operands.size() == 2) {
        // for (x in y) w
        if (e.operands.get(0).op != OP_IN) throw new CompilerError("expected 'in'", e.operands.get(0));
        e.operands.get(0).op = OP_FOR_IN; // rewrite to a OP_FOR_IN operaation
        analyseRecurse(e.operands.get(0), scopeStack, e, true, loop);  // for X IN Y w
        analyseRecurse(e.operands.get(1), scopeStack, e, false, true); // for x in y W
      } else {
        throw new CompilerError("invalid for construct", e);
      }
    }
    else if (e.op == OP_FOR_IN) {
      if (e.operands.get(0).op != OP_SYMBOL) throw new CompilerError("expected symbol", e.operands.get(0));
      analyseRecurse(e.operands.get(0), scopeStack, e, false, loop); // X in y
      analyseRecurse(e.operands.get(1), scopeStack, e, true, loop);  // x in Y
    }
    else if (e.op == OP_WHILE) {
      analyseRecurse(e.operands.get(0), scopeStack, e, true, loop); // while (x) <-- does not declare x -> true
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
            newAnonymousScope((ASTNodeBlok)e2, scopeStack, e);
          } else {
            analyseRecurse(e2, scopeStack, e, operator, loop);
          }
        }
      }
      if (!(e instanceof ASTNodeArrDecl)) throw new CompilerError("malformed array", e);
      ((ASTNodeArrDecl)e).onlyPrimitives = onlyPrimitives;
    }    
    else if (e.op == OP_TUPLE) {
      // key
      analyseRecurse(e.operands.get(0), scopeStack, e, operator, loop);
      //val
      ASTNode val = e.operands.get(1);
      if (val instanceof ASTNodeBlok) {
        newAnonymousScope((ASTNodeBlok)val, scopeStack, e);
      } else {
        analyseRecurse(val, scopeStack, e, operator, loop);
      }
    }
    else if (e.op == OP_ADEREF) {
      analyseRecurse(e.operands.get(0), scopeStack, e, operator, loop);
      if (e.operands.get(1).op == OP_BLOK) {
        newAnonymousScope((ASTNodeBlok)e.operands.get(1), scopeStack, e);
      } else if (e.operands.get(1).op == OP_RETURN) {
        ASTNodeBlok eblok = new ASTNodeBlok();
        eblok.operands.add(e.operands.get(1));
        e.operands.set(1, eblok);
        newAnonymousScope(eblok, scopeStack, e);
      } else if (AST.isConditionalOperator(e.operands.get(1).op)) {
      	// convert rel op A to { if A return $0; else return nul; } 
      	ASTNodeBlok eblok = new ASTNodeBlok();
      	ASTNode eif = new ASTNodeOp(OP_IF);
      	ASTNode ereturn$0 = new ASTNodeOp(OP_RETURN);
      	ereturn$0.operands.add(new ASTNodeSymbol("$0"));
      	ASTNode eelse = new ASTNodeOp(OP_ELSE);
      	ASTNode ereturnnil = new ASTNodeOp(OP_RETURN);
      	ereturnnil.operands.add(new ASTNodeOp(OP_NIL));
      	eelse.operands.add(ereturnnil);
      	eif.operands.add(e.operands.get(1));
      	eif.operands.add(ereturn$0);
      	eif.operands.add(eelse);
      	eblok.operands.add(eif);
      	e.operands.set(1, eblok);
        newAnonymousScope(eblok, scopeStack, e);
      } else {
        analyseRecurse(e.operands.get(1), scopeStack, e, operator, loop);
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
  
  void newAnonymousScope(ASTNodeBlok be, Stack<Scope> definingScopeStack, ASTNode parent) {
    prevPrevOp = prevOp;
    prevOp = _prevOp;
    _prevOp = be.op;
    
    // If 2nd level anon within a 1st level anon refers an outer variable not in the 1st level anon,
    // this variable must be copied also to the 1st anon
    // Ex:
    // func outer(a1,b1) {
    //   inner1 = {        // inner1 refers outer:a1 directly, and outer:b1 indirectly from inner2
    //     println(a1);
    //     inner2 = {      // inner2 refers outer:b1 via inner1:b1
    //       println(b1);
    //     };
    //     return inner2;
    //   };
    // }
    
    fragmentStack.push(new Fragment(definingScopeStack));

    Scope globalScope = mainScopeStack.get(0);
    be.parentBlock = globalScope.block;
    
    String blockId = getBlockId() + "A";
    Stack<Scope> anonScopeStack = new Stack<Scope>();
    anonScopeStack.push(globalScope);
    Scope anonScope = new Scope(be);
    anonScopeStack.push(anonScope);
    if (dbg) System.out.println("ENTER anon eblk " + be);
    if (dbg) System.out.println("           prnt " + parent);
    if (be.operands != null) {
      for (ASTNode e2 : be.operands) {
        analyseRecurse(e2, anonScopeStack, parent, false, false);
      }
    }
    Fragment f = fragmentStack.pop();
    be.setAnnotationAnonymous(anonScope.symVars, anonScopeStack.size(), blockId, module, f.anonDefLocals);
    if (dbg) System.out.println("LEAVE anon eblk " + be + " got symbols " + anonScope.symVars);
  }
  
  public static void analyse(ASTNodeBlok e, IntermediateRepresentation ir) {
    StructAnalysis cg = new StructAnalysis();
    if (dbg) System.out.println("  * analyse variables");
    cg.irep = ir;
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
  
  boolean isExternalVarReachableDef(ASTNodeSymbol esym) {
    // structure: by logic, the first frag in fragmentStack must be
    // the function or the main frag defining the chain of anonymous 
    // functions. The first scope in all frags is the global scope.
    
    // first, try finding it in wrapping fragments
    int fragDescIx;
    boolean found = false;
    for (fragDescIx = fragmentStack.size() - 1; !found && fragDescIx >= 0; fragDescIx--) {
      Fragment f = fragmentStack.get(fragDescIx);
      if (f.anonDefLocals.contains(esym) || isLocalVarDef(f.scopeStack, esym)) {
        found = true;
      }
    }
    if (!found) return false;
    // secondly, populate the external reachable variable through all
    // layers of fragments
    for (int fragAscIx = fragDescIx+1; fragAscIx < fragmentStack.size()-1; fragAscIx++) {
      Fragment f = fragmentStack.get(fragAscIx);
      if (!f.anonDefLocals.contains(esym)) f.anonDefLocals.add(esym);
    }
    return true;
  }

  // define variable for scope if not already reachable
  void defVarIfUndef(Stack<Scope> scopeStack, ASTNodeSymbol esym, ASTNode definer) {
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
      List<ASTNodeSymbol> modGlobs = null;
      if (irep != null && (modGlobs = irep.getGlobalVariables(module)) != null) {
        if (modGlobs.contains(esym)) {
          return;
        }
      }
    }
    scopeStack.peek().symVars.put(esym, esym.symNbr);
    if (dbg) System.out.println("  + '" + esym.symbol + "' " + (scopeStack.size() == 1 ? "GLOBAL" : "LOCAL") +
        " in " + definer);
    esym.declare = true;
  }
  
  // define variable for global scope if not already defined
  void defVarIfUndefGlobal(Stack<Scope> scopeStack, ASTNodeSymbol esym, ASTNode definer) {
    if (esym == null) throw new CompilerError("fatal");
    if (scopeStack.get(0).symVars.keySet().contains(esym)) {
      // is defined already
      return;
    };
    scopeStack.get(0).symVars.put(esym, esym.symNbr);
    if (dbg) System.out.println("  + '" + esym.symbol + "' " + "GLOBAL" +
        " in " + definer);
    esym.declare = true;
  }
  
  // define variable for scope, if same name already reachable this will shadow ancestor
  void defVar(Stack<Scope> scopeStack, ASTNodeSymbol esym, ASTNode definer) {
    boolean shadow = isVarDefAbove(scopeStack, esym);
    if (scopeStack.peek().symVars.get(esym) == null) {
      scopeStack.peek().symVars.put(esym, esym.symNbr);
      if (dbg) System.out.println("  + '" + esym.symbol + "' " + (scopeStack.size() == 1 ? "GLOBAL" : "LOCAL") + (shadow ? " SHADOW" : "")+
          " in " + definer);
      esym.declare = true;
    }
  }
  
  // see if a variable is reachable from this scope and ancestors
  boolean isVarDef(Stack<Scope> scopeStack, ASTNodeSymbol esym) {
    for (int i = scopeStack.size()-1; i >= 0; i--) {
      Scope s = scopeStack.get(i);
      if (s.symVars.keySet().contains(esym)) return true;
      if (s.symArgs.contains(esym)) return true;
    }
    return false;
  }
  
  // see if a variable is reachable from this scope and ancestors, but not global scope
  boolean isLocalVarDef(Stack<Scope> scopeStack, ASTNodeSymbol esym) {
    for (int i = scopeStack.size()-1; i > 0; i--) {
      Scope s = scopeStack.get(i);
      if (s.symVars.keySet().contains(esym)) return true;
      if (s.symArgs.contains(esym)) return true;
    }
    return false;
  }
  
  // see if a variable is reachable in ancestors only
  boolean isVarDefAbove(Stack<Scope> scopeStack, ASTNodeSymbol sym) {
    for (int i = scopeStack.size()-2; i >= 0; i--) {
      Scope s = scopeStack.get(i);
      if (s.symVars.keySet().contains(sym)) return true;
      if (s.symArgs.contains(sym)) return true;
    }
    return false;
  }
  
  class Scope {
    // local variables of this scope / declaring symbol index
    Map<ASTNodeSymbol, Integer> symVars;
    // function arguments
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
  
  class Fragment {
    // scope stack of defining scope
    Stack<Scope> scopeStack; 
    // external referring variables in this anonymous scope
    List<ASTNodeSymbol> anonDefLocals = new ArrayList<ASTNodeSymbol>();
    public Fragment(Stack<Scope> definingScopeStack) {
      scopeStack = definingScopeStack;
    }
  }
}