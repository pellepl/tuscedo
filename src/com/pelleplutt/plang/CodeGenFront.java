package com.pelleplutt.plang;

import static com.pelleplutt.plang.AST.OP_ADECL;
import static com.pelleplutt.plang.AST.OP_ADEREF;
import static com.pelleplutt.plang.AST.OP_AND;
import static com.pelleplutt.plang.AST.OP_ANDEQ;
import static com.pelleplutt.plang.AST.OP_BKPT;
import static com.pelleplutt.plang.AST.OP_BLOK;
import static com.pelleplutt.plang.AST.OP_BREAK;
import static com.pelleplutt.plang.AST.OP_CALL;
import static com.pelleplutt.plang.AST.OP_CONTINUE;
import static com.pelleplutt.plang.AST.OP_DIV;
import static com.pelleplutt.plang.AST.OP_DIVEQ;
import static com.pelleplutt.plang.AST.OP_DOT;
import static com.pelleplutt.plang.AST.OP_EQ;
import static com.pelleplutt.plang.AST.OP_FOR;
import static com.pelleplutt.plang.AST.OP_FUNCDEF;
import static com.pelleplutt.plang.AST.OP_GOTO;
import static com.pelleplutt.plang.AST.OP_IF;
import static com.pelleplutt.plang.AST.OP_MINUS;
import static com.pelleplutt.plang.AST.OP_MINUSEQ;
import static com.pelleplutt.plang.AST.OP_MOD;
import static com.pelleplutt.plang.AST.OP_MODEQ;
import static com.pelleplutt.plang.AST.OP_MUL;
import static com.pelleplutt.plang.AST.OP_MULEQ;
import static com.pelleplutt.plang.AST.OP_NIL;
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
import static com.pelleplutt.plang.AST.OP_RANGE;
import static com.pelleplutt.plang.AST.OP_RETURN;
import static com.pelleplutt.plang.AST.OP_SHLEFT;
import static com.pelleplutt.plang.AST.OP_SHLEFTEQ;
import static com.pelleplutt.plang.AST.OP_SHRIGHT;
import static com.pelleplutt.plang.AST.OP_SHRIGHTEQ;
import static com.pelleplutt.plang.AST.OP_SYMBOL;
import static com.pelleplutt.plang.AST.OP_TUPLE;
import static com.pelleplutt.plang.AST.OP_WHILE;
import static com.pelleplutt.plang.AST.OP_XOR;
import static com.pelleplutt.plang.AST.OP_XOREQ;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.pelleplutt.plang.ASTNode.ASTNodeArrSymbol;
import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.ASTNode.ASTNodeCompoundSymbol;
import com.pelleplutt.plang.ASTNode.ASTNodeFuncCall;
import com.pelleplutt.plang.ASTNode.ASTNodeNumeric;
import com.pelleplutt.plang.ASTNode.ASTNodeString;
import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;
import com.pelleplutt.plang.TAC.TACAlloc;
import com.pelleplutt.plang.TAC.TACArg;
import com.pelleplutt.plang.TAC.TACAssign;
import com.pelleplutt.plang.TAC.TACBkpt;
import com.pelleplutt.plang.TAC.TACCall;
import com.pelleplutt.plang.TAC.TACCode;
import com.pelleplutt.plang.TAC.TACFloat;
import com.pelleplutt.plang.TAC.TACFree;
import com.pelleplutt.plang.TAC.TACGoto;
import com.pelleplutt.plang.TAC.TACGotoCond;
import com.pelleplutt.plang.TAC.TACInt;
import com.pelleplutt.plang.TAC.TACLabel;
import com.pelleplutt.plang.TAC.TACNil;
import com.pelleplutt.plang.TAC.TACOp;
import com.pelleplutt.plang.TAC.TACReturn;
import com.pelleplutt.plang.TAC.TACSetDeref;
import com.pelleplutt.plang.TAC.TACSetRead;
import com.pelleplutt.plang.TAC.TACString;
import com.pelleplutt.plang.TAC.TACUnaryOp;
import com.pelleplutt.plang.TAC.TACUnresolved;
import com.pelleplutt.plang.TAC.TACVar;

public class CodeGenFront {
  static boolean dbg = false;
  static int label = 0;
  static int anonIx = 0;
  List<FrontFragment> ffrags = new ArrayList<FrontFragment>();
  FrontFragment ffrag;
  
  public static List<Module> genIR(ASTNodeBlok e) {
    CodeGenFront cg = new CodeGenFront();
    return cg.doIntermediateCode(e);
  }
  
  public CodeGenFront() {
  }
  
  public List<Module> doIntermediateCode(ASTNodeBlok eblk) {
    genIR(eblk, eblk);
    if (dbg) printIR(System.out);
    genCFG(ffrags);
    //printDot(System.out);
    List<Module> res = gather();
    return res;
  }
  
  List<Module> gather() {
    Map<String, Module> mmap = new HashMap<String, Module>();
    for (FrontFragment ffrag : ffrags) {
      Module module = mmap.get(ffrag.module);
      if (module == null) {
        module = new Module();
        module.id = ffrag.module;
        mmap.put(ffrag.module, module);
      }
      ModuleFragment frag = new ModuleFragment();
      frag.fragname = ffrag.name;
      frag.type = ffrag.type;
      frag.modname = module.id;
      for (Block block : ffrag.blocks) {
        frag.tacs.add(new ArrayList<TAC>(block.ir));
      }
      module.frags.add(frag);
    }
    List<Module> res = new ArrayList<Module>();
    res.addAll(mmap.values());
    return res;
  }
  
  void printBlock(PrintStream out, FrontFragment ffrag, Block block, String nl) {
    for (TAC t : block.ir) {
      if (t instanceof TACLabel) {
        out.print(t + nl);
      } else {
        out.print("   " + ffrag.ir.indexOf(t) + ":\t"+t+nl);
      }
    }
  }
  
  void printContext(PrintStream out, FrontFragment ffrag) {
    int bix = 0;
    System.out.println("CONTEXT " + ffrag.module + "" + ffrag.name);
    for (Block block : ffrag.blocks) {
      out.println("======================================== " + bix);
      bix++;
      printBlock(out, ffrag, block, System.getProperty("line.separator"));
    }
    out.println();
  }
  
  void printIR(PrintStream out) {
    for (FrontFragment ffrag : ffrags) {
      printContext(out, ffrag);
    }
  }
  
  void printDot(PrintStream out) {
    out.println("digraph G {");
    int ffragIx = 0;
    for (FrontFragment ffrag : ffrags) {
      String ffragId = ffragIx + "";
      out.println(ffragId + " [ fontsize=6 label=\"" + ffrag.module + "\"];");
      for (Block block : ffrag.blocks) {
        String nodeId = ffragIx + "." + block.blockId;
        out.print(nodeId + " [ fontsize=6 shape=box label=\"");
        printBlock(out, ffrag, block, "\\l");
        out.println("\"];");
      }
      ffragIx++;
    }

    ffragIx = 0;
    for (FrontFragment ffrag : ffrags) {
      String ffragId = ffragIx + "";
      out.println(ffragId + "->" + ffragIx + "." + ffrag.blocks.get(0).blockId + ";");
      for (Block block : ffrag.blocks) {
        String nodeId = ffragIx + "." + block.blockId;
        for (Block eblock : block.exits) {
          String enodeId = ffragIx + "." + eblock.blockId;
          out.println(nodeId + "->" + enodeId + ";");
        }
      }
      ffragIx++;
    }
    out.print("}");
  }
  
  //
  // construct control flow graph DAG from blocks
  //
  
  void genCFG(List<FrontFragment> ffrags) {
    for (FrontFragment ffrag : ffrags) {
      for (int bix = 0; bix < ffrag.blocks.size(); bix++) {
        Block b = ffrag.blocks.get(bix);
        Block nb = bix < ffrag.blocks.size() - 1 ? ffrag.blocks.get(bix+1) : null;
        TAC last = b.get(b.size()-1);

        if (last instanceof TACGoto) {
          TACLabel l = ((TACGoto)last).label;
          Block ob = ffrag.getBlock(l);
          b.exits.add(ob);
          ob.entries.add(b);
        } else if (last instanceof TACGotoCond) {
          TACLabel l = ((TACGotoCond)last).label;
          Block ob = ffrag.getBlock(l);
          b.exits.add(ob);
          ob.entries.add(b);
          if (nb != null) {
            b.exits.add(nb);
            nb.entries.add(b);
          }
        } else if (last instanceof TACReturn) {
          // leaf
        } else {
          if (nb != null) {
            b.exits.add(nb);
            nb.entries.add(b);
          }
        }
      }
    }
  }
  
  static int __innerScope = 0;
  String getInnerScope() {
    return ".$" + (__innerScope++);
  }
  
  TAC genIRUnwindArrOp(ASTNode e, ASTNodeBlok parentEblk, boolean assignOp) {
    ASTNodeArrSymbol ce = new ASTNodeArrSymbol(e);
    int len = ce.path.size();
    TAC tarr = genIR(ce.path.get(0), parentEblk);
    add(tarr);
    setReferenced(tarr);
    for (int i = 1; i < len; i++) {
      TAC derefVal = genIR(ce.path.get(i), parentEblk);
      setReferenced(derefVal);
      TAC deref = new TACSetDeref(e, tarr, derefVal);
      setReferenced(deref);
      if (assignOp) {
        if (i < len-1) {
          add(deref);
        }
        // do not add last dot path, as this is returned as the assignee and will be emitted thare
      } else {
        add(deref);
      }
      tarr = deref;
    }
    return tarr;
  }
  
  TAC genIRUnwindDotOp(ASTNode e, ASTNodeBlok parentEblk, boolean assignOp) {
    // cases:
    // <map>.<key> or
    // <module>.<var> or
    // <module>.<map>.<key>
    // x.y      - if x unknown => x module, y var
    //            if x known   => x map,    y key
    // x.y.z... - if x unknown => x module, y map, z... keys
    //            if x known   => x map,    y,z... keys
    
    // a.b.c...
    ASTNodeCompoundSymbol ce = new ASTNodeCompoundSymbol(e);
    int len = ce.dots.size();
    // first, try if initial symbol is a reachable variable. If so, prefer local
    ASTNodeBlok declBlok = getScopeIfDef(parentEblk, (ASTNodeSymbol)ce.dots.get(0));
    if (declBlok != null) {
      // it was, so consider this a hash map - start unwinding it
      // get first symbol...
      TAC tmap = genIR(ce.dots.get(0), parentEblk);
      add(tmap);
      setReferenced(tmap);
      // ...then unwind the dereferences...
      for (int i = 1; i < len; i++) {
        TACString derefval = new TACString(ce.dots.get(i), ((ASTNodeSymbol)ce.dots.get(i)).symbol);
        TAC deref = new TACSetDeref(e, tmap, derefval);
        if (assignOp) {
          if (i < len-1) {
            add(deref);
          }
          // do not add last dot path, as this is returned as the assignee and will be emitted thare
        } else {
          add(deref);
        }
        tmap = deref;
      }
      return tmap;
    } else {
      // <module>.<map>
      // add unresolved for first two path entries <module>.<variable>
      TAC tmap = new TACUnresolved(e, ce.dots.get(0).symbol, ce.dots.get(1).symbol); 
      if (len <= 2) {
        return tmap;
      }
      // <module>.<map>.<key>....
      add(tmap);
      setReferenced(tmap);
      for (int i = 2; i < len; i++) {
        TACString derefval = new TACString(ce.dots.get(i), ((ASTNodeSymbol)ce.dots.get(i)).symbol);
        TAC deref = new TACSetDeref(e, tmap, derefval);
        if (assignOp) {
          if (i < len-1) {
            add(deref);
          }
          // do not add last dot path, as this is returned as the assignee and will be emitted thare
        } else {
          add(deref);
        }
        tmap = deref;
      }
      return tmap;
    }
  }
  
  class SymbolHolder {
    TAC arrSymbol;
  }
  
  TAC genIRAssignment(ASTNode e, ASTNodeBlok parentEblk, int level, SymbolHolder sh) {
    if (e.op == OP_SYMBOL) {
      TAC ret = genIR(e, parentEblk);
      sh.arrSymbol = ret;
      return ret;
    }
    else if (e.op == OP_ADEREF) {
      return genIRUnwindArrOp(e, parentEblk, true);
    } else if (e.op == OP_DOT) {
      return genIRUnwindDotOp(e, parentEblk, true);
    } else {
      throw new CompilerError("fatal, unknown assignee", e);
    }
  }
  
  //
  // construct three address code intermediate representation
  //
  TAC genIR(ASTNode e, ASTNodeBlok parentEblk) {
    if (e.op == OP_BLOK) {
      ASTNodeBlok eblk = (ASTNodeBlok)e;
      FrontFragment oldFrag = ffrag;
      FrontFragment newFrag = null;
      
      if (eblk.type == ASTNodeBlok.TYPE_ANON) {
        newFrag = new FrontFragment(oldFrag.module, ".anon" + (anonIx++));
        newFrag.type = ASTNode.ASTNodeBlok.TYPE_ANON;
        ffrags.add(newFrag);
        ffrag = newFrag;
      } else if (eblk.type == ASTNodeBlok.TYPE_FUNC) {
        newFrag = new FrontFragment(oldFrag.module, ".func" + ((ASTNodeBlok)eblk).id);
        newFrag.type = ASTNode.ASTNodeBlok.TYPE_FUNC;
        ffrags.add(newFrag);
        ffrag = newFrag;
      } else if (ffrag == null) {
        // first context, must be globals
        ffrag = new FrontFragment(eblk.module == null ? ".MAIN" : eblk.module, ".main");
        ffrag.type = ASTNode.ASTNodeBlok.TYPE_MAIN;
        ffrags.add(ffrag);
//        if (eblk.symList != null) {
//          // collect the global variables
//          for (ASTNode esym : eblk.symList) {
//            ASTNodeSymbol sym = (ASTNodeSymbol)esym;
//            ffrag.gvars.add(new TACVar(sym, sym.symbol, eblk.getModule(), eblk.getScopeId()));
//          }
//        }
      }
      boolean doStackAllocation = 
                                  (eblk.gotUnhandledVariables() && 
                                   eblk.getScopeLevel() > 0); // no variable stack allocation for top scopes, these are global vars
      if (doStackAllocation) {
        eblk.setVariablesHandled();
        add(new TACAlloc(eblk, eblk.getModule(), eblk.getScopeId(), eblk.type == ASTNodeBlok.TYPE_FUNC));
      }
      for (ASTNode e2 : e.operands) {
        genIR(e2, (ASTNodeBlok)e);
      }
      if (doStackAllocation && eblk.type == ASTNodeBlok.TYPE_MAIN) {
        add(new TACFree(eblk, eblk.getModule(), eblk.getScopeId()));
      }
      
      if (eblk.type == ASTNodeBlok.TYPE_ANON || eblk.type == ASTNodeBlok.TYPE_FUNC) {
        // add 'return' if not set by programmer for TYPE_ANON and TYPE_FUNC
        if (!(ffrag.ir.get(ffrag.ir.size()-1) instanceof TACReturn)) {
          add(new TACReturn(e, new TACNil(e))); 
        }
        
      }
      ffrag = oldFrag;
      if (eblk.type == ASTNodeBlok.TYPE_ANON) {
        return new TACCode(e, newFrag);
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
      ASTNodeBlok declBlok = getScopeIfDef(parentEblk, (ASTNodeSymbol)e);
      if (declBlok != null) {
        return new TACVar(e, ((ASTNodeSymbol)e).symbol, declBlok.getModule(), declBlok.getScopeId());
      } else {
        return new TACUnresolved((ASTNodeSymbol)e, parentEblk);
      }
    }
    
    else if (e.op == OP_DOT) {
      return genIRUnwindDotOp(e, parentEblk, false);
    }
    
    else if (e.op == OP_NIL) {
      return new TACNil(e);
    }
    
    else if (e.op == OP_EQ) {
      TAC assignee = genIRAssignment(e.operands.get(0), parentEblk, 0, new SymbolHolder());
      TAC assignment = genIR(e.operands.get(1), parentEblk);
      setReferenced(assignment);
      TAC op = new TACAssign(e, e.op, assignee, assignment); 
      add(op);
      return op;
    } 
    
    else if (AST.isOperator(e.op)) {
      if (!AST.isAssignOperator(e.op)) {
        if (AST.isUnaryOperator(e.op)) {
          TAC operand = genIR(e.operands.get(0), parentEblk);  
          setReferenced(operand);
          TAC op = new TACUnaryOp(e, e.op, operand);
          add(op);
          return op;
        } else {
          TAC left = genIR(e.operands.get(0), parentEblk);  
          TAC right = genIR(e.operands.get(1), parentEblk);  
          setReferenced(left);
          setReferenced(right);
          TAC op = new TACOp(e, e.op, left, right);
          add(op);
          return op;
        }
      } else {
        TAC assignee = genIRAssignment(e.operands.get(0), parentEblk, 0, new SymbolHolder());
        setReferenced(assignee);
        TAC right = genIR(e.operands.get(1), parentEblk);  
        if (right != null) setReferenced(right);
        int opCode;
        if      (e.op == OP_ANDEQ) opCode = OP_AND; 
        else if (e.op == OP_DIVEQ) opCode = OP_DIV; 
        else if (e.op == OP_MINUSEQ) opCode = OP_MINUS; 
        else if (e.op == OP_MODEQ) opCode = OP_MOD; 
        else if (e.op == OP_MULEQ) opCode = OP_MUL; 
        else if (e.op == OP_OREQ) opCode = OP_OR; 
        else if (e.op == OP_PLUSEQ) opCode = OP_PLUS; 
        else if (e.op == OP_SHLEFTEQ) opCode = OP_SHLEFT; 
        else if (e.op == OP_SHRIGHTEQ) opCode = OP_SHRIGHT; 
        else if (e.op == OP_XOREQ) opCode = OP_XOR; 
        else throw new CompilerError("unknown assign operator " + AST.opString(e.op), e);
        TAC assignment = new TACOp(e, opCode, assignee, right);
        add(assignment);
        TAC assOp = new TACAssign(e, OP_EQ, assignee, assignment); 
        add(assOp);
        return assOp;
      }
    }
    
    else if (e.op == OP_IF) {
      boolean hasElse = e.operands.size() == 3;
      String label = genLabel();
      TACLabel lExit = new TACLabel(e, label+"_ifend");
      if (!hasElse) {
        TAC cond = genIR(e.operands.get(0), parentEblk);
        setReferenced(cond);
        TAC iffalsegoto = new TACGotoCond(e, cond, lExit, false);
        add(iffalsegoto);
        newBlock();
        genIR(e.operands.get(1), parentEblk);
        newBlock();
        add(lExit);
      } else {
        TACLabel lElse = new TACLabel(e, label+"_ifelse");
        TAC cond = genIR(e.operands.get(0), parentEblk);
        setReferenced(cond);
        TAC iffalsegoto = new TACGotoCond(e, cond, lElse, false);
        add(iffalsegoto);
        newBlock();
        genIR(e.operands.get(1), parentEblk);
        TAC gotoExit = new TACGoto(e, lExit);
        add(gotoExit);
        newBlock();
        add(lElse);
        genIR(e.operands.get(2).operands.get(0), parentEblk);
        newBlock();
        add(lExit);
      }
    }
    
    else if (e.op == OP_FOR) {
      if (e.operands.size() == 4) {
        //for (x; y; z) {w}
        String label = genLabel();
        ASTNodeBlok eblk = null;
        if (e.operands.get(3).op == OP_BLOK) {
          eblk = (ASTNodeBlok)e.operands.get(3);
        }

        TACLabel lLoop = new TACLabel(e, label+"_floop");
        TACLabel lExit = new TACLabel(e, label+"_fexit");
        TACLabel lCont = new TACLabel(e, label+"_fcont");
        
        boolean doStackAllocation = eblk != null && eblk.gotUnhandledVariables();
        if (doStackAllocation) {
          add(new TACAlloc(eblk, eblk.getModule(), eblk.getScopeId()));
          eblk.setVariablesHandled();
        }
        
        // initial : x
        genIR(e.operands.get(0), parentEblk);
        newBlock();
        add(lLoop);
        // conditional : y
        TAC cond = genIR(e.operands.get(1), parentEblk);
        setReferenced(cond);
        TAC iffalsegoto = new TACGotoCond(e, cond, lExit, false);
        add(iffalsegoto);
        newBlock();
        pushLoop(lCont, lExit);
        // loop : w
        genIR(e.operands.get(3), parentEblk);
        boolean contCalled = loopWasContinued();
        popLoop();
        if (contCalled) {
          newBlock();
          add(lCont);
        }
        // evaluate : z
        genIR(e.operands.get(2), parentEblk);
        TAC gotoLoop = new TACGoto(e, lLoop);
        add(gotoLoop);
        newBlock();
        add(lExit);

        if (doStackAllocation) {
          add(new TACFree(eblk, eblk.getModule(), eblk.getScopeId()));
        }
        
      } else {
        //for (x in y) {w}
        TACAlloc talloc = new TACAlloc(e.operands.get(2), parentEblk); 
        add(talloc);
        String label = genLabel();

        TACLabel lLoop = new TACLabel(e, label+"_floop");
        TACLabel lExit = new TACLabel(e, label+"_fexit");
        TACLabel lCont = new TACLabel(e, label+"_fcont");

        String innerScope = getInnerScope();
        TACVar _set = new TACVar(parentEblk, TACAlloc.varSet, talloc.module, talloc.scope + innerScope); 
        TACVar _iter = new TACVar(parentEblk, TACAlloc.varIterator, talloc.module, talloc.scope + innerScope); 
        TACVar assignee = new TACVar(e.operands.get(0), ((ASTNodeSymbol)e.operands.get(0)).symbol, 
            parentEblk.getModule(), parentEblk.getScopeId());
        
        // inital .set = y, .iter = 0
        // .set = y 
        add(new TACAssign(
            e.operands.get(1),                                            // ASTNode
            OP_EQ,                                                        // op
            _set,                                                         // inner set variable
            genIR(e.operands.get(1).operands.get(0), parentEblk)));       // the set itself
        // .iter = 0 
        add(new TACAssign(
            e.operands.get(1),                                            // ASTNode
            OP_EQ,                                                        // op
            _iter,                                                        // inner iterator variable
            new TACInt(e.operands.get(1).operands.get(0), 0)));           // zero
        
        newBlock();
        add(lLoop);

        // conditional : y (.iter < len(.set))
        TACArg _set_arg = new TACArg(e, _set);
        add(_set_arg);
        setReferenced(_set_arg);
        TAC _len_set = new TACCall(e, "len", 1, talloc.module);
        add(_len_set);
        setReferenced(_len_set);
        TAC cond = new TACOp(e, AST.OP_LT, _iter, _len_set);
        add(cond);
        setReferenced(cond);
        TACGotoCond iffalsegoto = new TACGotoCond(e, cond, lExit, false);
        iffalsegoto.condOp = AST.OP_LT;
        add(iffalsegoto);
        newBlock();
        pushLoop(lCont, lExit);

        // loop : x = .set[.iter]
        add(_set);
        setReferenced(_set);
        TAC _set_iter = new TACSetRead(e.operands.get(2), _set, _iter);
        add(_set_iter);
        setReferenced(_set_iter);
        add(new TACAssign(
            e.operands.get(0),                                            // ASTNode
            OP_EQ,                                                        // op
            assignee,                                                     // x
            _set_iter));                                                  // .set[.iter]

        // loop : w
        genIR(e.operands.get(2), parentEblk);
        boolean contCalled = loopWasContinued();
        popLoop();
        if (contCalled) {
          newBlock();
          add(lCont);
        }

        // continue
        TAC _iter_add = new TACUnaryOp(e, AST.OP_POSTINC, _iter);
        add(_iter_add);

        TAC gotoLoop = new TACGoto(e, lLoop);
        add(gotoLoop);
        newBlock();
        add(lExit);
        
        add(new TACFree(talloc));
      }
    }
    
    else if (e.op == OP_WHILE) {
      ASTNodeBlok eblk = null;
      if (e.operands.get(1).op == OP_BLOK) {
        eblk = (ASTNodeBlok)e.operands.get(1);
      }

      String label = genLabel();
      TACLabel lLoop = new TACLabel(e, label+"_wloop");
      TACLabel lExit = new TACLabel(e, label+"_wexit");
      
      boolean doStackAllocation = eblk != null && eblk.gotUnhandledVariables();
      if (doStackAllocation) {
        add(new TACAlloc(eblk, eblk.getModule(), eblk.getScopeId()));
        eblk.setVariablesHandled();
      }

      newBlock();
      add(lLoop);
      TAC cond = genIR(e.operands.get(0), parentEblk);
      setReferenced(cond);
      TAC iffalsegoto = new TACGotoCond(e, cond, lExit, false);
      add(iffalsegoto);
      newBlock();
      pushLoop(lLoop, lExit);
      genIR(e.operands.get(1), parentEblk);
      popLoop();
      TAC gotoLoop = new TACGoto(e, lLoop);
      add(gotoLoop);
      newBlock();
      add(lExit);
      
      if (doStackAllocation) {
        add(new TACFree(eblk, eblk.getModule(), eblk.getScopeId()));
      }
    }
    
    else if (e.op == OP_BREAK) {
      TACLabel lExit = getLoopBreakLabel();
      add(new TACGoto(e, lExit));
      newBlock();
    }
    
    else if (e.op == OP_CONTINUE) {
      TACLabel lCont = getLoopContinueLabel();
      add(new TACGoto(e, lCont));
      newBlock();
    }
    
    else if (e.op == OP_GOTO) {
      TACLabel dest = new TACLabel(e.operands.get(0), ((ASTNodeSymbol)e.operands.get(0)).symbol);
      TAC gotoDest = new TACGoto(e, dest);
      add(gotoDest);
      newBlock();
    }
    
    else if (e.op == OP_CALL) {
      ASTNodeFuncCall callNode = (ASTNodeFuncCall)e;
      String args[] = new String[e.operands.size()];
      for (int i = args.length-1; i >= 0; i--) {
        TAC argVal = genIR(e.operands.get(i), parentEblk);
        TAC arg = new TACArg(e, argVal);
        setReferenced(argVal);
        add(arg);
      }
      
      TACCall call = null;
      if (callNode.callByName) {
        ASTNodeSymbol callSym = callNode.name;
        if (callSym instanceof ASTNodeCompoundSymbol) {
          // call by dotted symbol
          ASTNodeCompoundSymbol ce = (ASTNodeCompoundSymbol)callSym;
          // first, try if initial symbol is a reachable variable. If so, prefer local
          ASTNodeBlok declBlok = getScopeIfDef(parentEblk, (ASTNodeSymbol)ce.dots.get(0));
          if (declBlok != null) {
            // it was, so consider this calling a hash map function <mapvar>.<key>.<key>...()
            // hashmap func call unwind
            // get first symbol <mapvar>...
            TAC tmap = genIR(ce.dots.get(0), parentEblk);
            add(tmap);
            setReferenced(tmap);
            // ...then unwind the dereferences...
            for (int i = 1; i < ce.dots.size(); i++) {
              TACString derefval = new TACString(ce.dots.get(i), ((ASTNodeSymbol)ce.dots.get(i)).symbol);
              TAC deref = new TACSetDeref(e, tmap, derefval);
              add(deref);
              tmap = deref;
            }
            // ...then do the call
            call = new TACCall((ASTNodeFuncCall)e, args.length, parentEblk.module, null);
            call.callByName = false;
            add(call);
          } else {
            // no reachable variable for first entry in dot path, this needs to be linked
            if (ce.dots.size() == 2) {
              // only a.b, so presume this is <module>.<function>
              call = new TACCall((ASTNodeFuncCall)e, args.length, ce.dots.get(0).symbol, null);
              add(call);
              call.link = true;
            } else {
              // a.b.c..., so presume this is a hashmap call <module>.<mapvar>.<key>....()
              TAC tmap = new TACUnresolved(e, ce.dots.get(0).symbol, ce.dots.get(1).symbol); 
              add(tmap);
              setReferenced(tmap);
              for (int i = 2; i < ce.dots.size(); i++) {
                TACString derefval = new TACString(ce.dots.get(i), ((ASTNodeSymbol)ce.dots.get(i)).symbol);
                TAC deref = new TACSetDeref(e, tmap, derefval);
                add(deref);
                tmap = deref;
              }
              call = new TACCall((ASTNodeFuncCall)e, args.length, ce.dots.get(0).symbol, null);
              add(call);
              call.link = true;
              // do not call by name but by stack top address as we've dereferenced
              // external map
              call.callByName = false; 
            }
          }
        } else {
          // call by symbol
          ASTNodeBlok scopeBlock = getScopeIfDef(parentEblk, callSym);
          TACVar var = null;
          if (scopeBlock != null) {
            // found a variable with the call name, so presume we're calling a function variable
            var = new TACVar(e, ((ASTNodeFuncCall)e).name.symbol, scopeBlock.getModule(), scopeBlock.getScopeId());
          }
          call = new TACCall((ASTNodeFuncCall)e, args.length, parentEblk.module, var);
          if (var == null) call.link = true;
          add(call);
        }
      } else {
        // call by op
        TAC addrGen = genIR(callNode.callAddrOp, parentEblk);
        setReferenced(addrGen);
        call = new TACCall((ASTNodeFuncCall)e, args.length, parentEblk.module, null);
        add(call);
      }
      return call;
    }

    else if (e.op == OP_FUNCDEF) {
      genIR(e.operands.get(0), parentEblk);
    }
    
    else if (e.op == OP_RETURN) {
      TACReturn ret;
      if (e.operands != null && e.operands.size() > 0) {
        TAC retVal = genIR(e.operands.get(0), parentEblk);
        setReferenced(retVal);
        ret = new TACReturn(e, retVal);
      } else {
        ret = new TACReturn(e);
      }
      add(ret);
      newBlock();
    }
    
    else if (e.op == OP_BKPT) {
      add(new TACBkpt(e));
    }
    
    else if (e.op == OP_ADEREF) {
      return genIRUnwindArrOp(e, parentEblk, false);
    }
    
    else if (e.op == OP_ADECL) {
      TAC set;
      ASTNode.ASTNodeArrDecl adecle = (ASTNode.ASTNodeArrDecl)e;
      boolean isMap = adecle.isEmpty() || adecle.containsTuples();
      if (isMap) {
        set = new TAC.TACMap(e, adecle.operands.size()); 
        add(set);
        int len = adecle.operands.size();
        for (int i = 0; i < len; i++) {
          ASTNode e2 = adecle.operands.get(i);
          TAC.TACMapTuple tuple = (TAC.TACMapTuple)genIR(e2, parentEblk);
          tuple.isLast = i == len-1;
          add(tuple);
        }
      } else {
        if (adecle.onlyPrimitives) {
          TAC.TACArrInit tarr = new TAC.TACArrInit(adecle); 
          for (ASTNode e2 : adecle.operands) {
            TAC arrentry = genIR(e2, parentEblk);
            tarr.entries.add(arrentry);
          }
          set = tarr;
          add(set);
        } else {
          for (ASTNode e2 : adecle.operands) {
            TAC entry = genIR(e2, parentEblk);
            TAC entryT = new TAC.TACArrEntry(e, entry);
            setReferenced(entry);
            add(entryT);
          }
          set = new TAC.TACSet(e, adecle.operands.size()); 
          add(set);
        }
      }

      return set;
    }
    
    else if (e.op == OP_TUPLE) {
      TAC key = genIR(e.operands.get(0), parentEblk);
      TAC val = genIR(e.operands.get(1), parentEblk);
      setReferenced(key);
      setReferenced(val);
      TAC tuple = new TAC.TACMapTuple(e, key, val);
      return tuple;
    }
    
    else if (e.op == OP_RANGE) {
      // TODO range
      throw new CompilerError("not implemented", e);
    }
    
    // TODO moar
    
    return null;
  }
  
  void add(TAC t) {
    if (ffrag.block == null) {
      ffrag.block = new Block();
      ffrag.blocks.add(ffrag.block);
    }
    ffrag.block.add(t);
    ffrag.ir.add(t);
    t.ffrag = ffrag;
  }
  
  void newBlock() {
    if (ffrag.block != null && ffrag.block.size() == 1 && ffrag.block.get(0) instanceof TACLabel) {
      return;
    }
    ffrag.block = null;
  }
  
  void pushLoop(TACLabel loop, TACLabel exit) {
    ffrag.loopStack.push(new Loop(loop, exit));
  }
  
  void popLoop() {
    ffrag.loopStack.pop();
  }
  
  TACLabel getLoopBreakLabel() {
    Loop l = ffrag.loopStack.peek();
    l.wasBroken = true;
    return l.exit;
  }
  
  TACLabel getLoopContinueLabel() {
    Loop l = ffrag.loopStack.peek();
    l.wasContinued = true;
    return l.loop;
  }
  
  boolean loopWasContinued() {
    return ffrag.loopStack.peek().wasContinued;
  }
  
  boolean loopWasBraked() {
    return ffrag.loopStack.peek().wasBroken;
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
  
  void setReferenced(TAC tac) {
    if (tac != null) tac.referenced = true;
  }
  
  ASTNodeBlok getScopeIfDef(ASTNodeBlok eblk, ASTNodeSymbol sym) {
    int whenceSymNbr = sym.symNbr;
    while (eblk != null) {
//      System.out.println("looking for " + sym + " in " + eblk.getVariables());
//      System.out.println("  declared here  " + eblk.declaresVariableInThisScope(sym));
//      System.out.println("  is declared b4 " +  eblk.isSymbolDeclared(sym, whenceSymNbr));
      if ((eblk.getArguments() != null && eblk.getArguments().contains(sym)) ||
          (eblk.declaresVariableInThisScope(sym) && eblk.isSymbolDeclared(sym, whenceSymNbr))) {
        return eblk;
      }
      whenceSymNbr = eblk.symNbr;
      eblk = eblk.parentBlock;
    }
    return null;
  }


  //
  // IR context (e.g. main, func def, anonymous)
  //
  
  class FrontFragment {
    String module, name;
    Block block; 
    List<Block> blocks = new ArrayList<Block>();
    List<TAC> ir = new ArrayList<TAC>();
    Stack<Loop> loopStack = new Stack<Loop>();
    //List<TACVar> gvars = new ArrayList<TACVar>();
    int type; // ASTNode.ASTNodeBlok.TYPE_*
    
    public FrontFragment(String module, String name) {
      this.module = module;
      this.name = name;
    }
    public Block getBlock(TACLabel t) {
      for (Block block : blocks) {
        for (TACLabel ot : block.labels) {
          if (t.label.equals(ot.label)) return block;
        }
      }
      throw new Error("cannot find block labeled " + t);
    }
  }
  
  //
  // Block in a context
  //
  
  static int __blockId = 0;
  class Block {
    int blockId;
    List<TACLabel> labels = new ArrayList<TACLabel>();
    List<TAC> ir = new ArrayList<TAC>();
    List<Block> entries = new ArrayList<Block>();
    List<Block> exits = new ArrayList<Block>();
    public Block() { blockId = __blockId++; }
    public void add(TAC t) {ir.add(t); if (t instanceof TACLabel) labels.add((TACLabel)t);}
    public TAC get(int i) {return ir.get(i);}
    public int size() {return ir.size();}
  }
  
  //
  // Loop info (for / while)
  //
  
  class Loop {
    TACLabel loop, exit;
    boolean wasContinued;
    boolean wasBroken;
    public Loop(TACLabel loop, TACLabel exit) {
      this.loop = loop;
      this.exit = exit;
    }
  }
}
