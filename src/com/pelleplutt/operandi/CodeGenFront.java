package com.pelleplutt.operandi;

import static com.pelleplutt.operandi.AST.OP_ADECL;
import static com.pelleplutt.operandi.AST.OP_ADEREF;
import static com.pelleplutt.operandi.AST.OP_AND;
import static com.pelleplutt.operandi.AST.OP_ANDEQ;
import static com.pelleplutt.operandi.AST.OP_BKPT;
import static com.pelleplutt.operandi.AST.OP_BLOK;
import static com.pelleplutt.operandi.AST.OP_BREAK;
import static com.pelleplutt.operandi.AST.OP_CALL;
import static com.pelleplutt.operandi.AST.OP_CONTINUE;
import static com.pelleplutt.operandi.AST.OP_DIV;
import static com.pelleplutt.operandi.AST.OP_DIVEQ;
import static com.pelleplutt.operandi.AST.OP_DOT;
import static com.pelleplutt.operandi.AST.OP_EQ;
import static com.pelleplutt.operandi.AST.OP_FOR;
import static com.pelleplutt.operandi.AST.OP_FUNCDEF;
import static com.pelleplutt.operandi.AST.OP_GOTO;
import static com.pelleplutt.operandi.AST.OP_GLOBAL;
import static com.pelleplutt.operandi.AST.OP_IF;
import static com.pelleplutt.operandi.AST.OP_MINUS;
import static com.pelleplutt.operandi.AST.OP_MINUSEQ;
import static com.pelleplutt.operandi.AST.OP_MOD;
import static com.pelleplutt.operandi.AST.OP_MODEQ;
import static com.pelleplutt.operandi.AST.OP_MODULE;
import static com.pelleplutt.operandi.AST.OP_MUL;
import static com.pelleplutt.operandi.AST.OP_MULEQ;
import static com.pelleplutt.operandi.AST.OP_NIL;
import static com.pelleplutt.operandi.AST.OP_NUMERICB1;
import static com.pelleplutt.operandi.AST.OP_NUMERICB2;
import static com.pelleplutt.operandi.AST.OP_NUMERICD;
import static com.pelleplutt.operandi.AST.OP_NUMERICH1;
import static com.pelleplutt.operandi.AST.OP_NUMERICH2;
import static com.pelleplutt.operandi.AST.OP_NUMERICI;
import static com.pelleplutt.operandi.AST.OP_OR;
import static com.pelleplutt.operandi.AST.OP_OREQ;
import static com.pelleplutt.operandi.AST.OP_PLUS;
import static com.pelleplutt.operandi.AST.OP_PLUSEQ;
import static com.pelleplutt.operandi.AST.OP_QUOTE1;
import static com.pelleplutt.operandi.AST.OP_QUOTE2;
import static com.pelleplutt.operandi.AST.OP_RANGE;
import static com.pelleplutt.operandi.AST.OP_RETURN;
import static com.pelleplutt.operandi.AST.OP_SHLEFT;
import static com.pelleplutt.operandi.AST.OP_SHLEFTEQ;
import static com.pelleplutt.operandi.AST.OP_SHRIGHT;
import static com.pelleplutt.operandi.AST.OP_SHRIGHTEQ;
import static com.pelleplutt.operandi.AST.OP_SPACES;
import static com.pelleplutt.operandi.AST.OP_SYMBOL;
import static com.pelleplutt.operandi.AST.OP_TUPLE;
import static com.pelleplutt.operandi.AST.OP_WHILE;
import static com.pelleplutt.operandi.AST.OP_XOR;
import static com.pelleplutt.operandi.AST.OP_XOREQ;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.pelleplutt.operandi.ASTNode.ASTNodeBlok;
import com.pelleplutt.operandi.ASTNode.ASTNodeFuncCall;
import com.pelleplutt.operandi.ASTNode.ASTNodeNumeric;
import com.pelleplutt.operandi.ASTNode.ASTNodeRange;
import com.pelleplutt.operandi.ASTNode.ASTNodeString;
import com.pelleplutt.operandi.ASTNode.ASTNodeSymbol;
import com.pelleplutt.operandi.TAC.TACAlloc;
import com.pelleplutt.operandi.TAC.TACArgNbr;
import com.pelleplutt.operandi.TAC.TACArgc;
import com.pelleplutt.operandi.TAC.TACArgv;
import com.pelleplutt.operandi.TAC.TACAssign;
import com.pelleplutt.operandi.TAC.TACBkpt;
import com.pelleplutt.operandi.TAC.TACCall;
import com.pelleplutt.operandi.TAC.TACCode;
import com.pelleplutt.operandi.TAC.TACDefineMe;
import com.pelleplutt.operandi.TAC.TACFloat;
import com.pelleplutt.operandi.TAC.TACFree;
import com.pelleplutt.operandi.TAC.TACFuncArg;
import com.pelleplutt.operandi.TAC.TACGetMe;
import com.pelleplutt.operandi.TAC.TACGoto;
import com.pelleplutt.operandi.TAC.TACGotoCond;
import com.pelleplutt.operandi.TAC.TACInt;
import com.pelleplutt.operandi.TAC.TACLabel;
import com.pelleplutt.operandi.TAC.TACNil;
import com.pelleplutt.operandi.TAC.TACOp;
import com.pelleplutt.operandi.TAC.TACReturn;
import com.pelleplutt.operandi.TAC.TACSetDeref;
import com.pelleplutt.operandi.TAC.TACSetRead;
import com.pelleplutt.operandi.TAC.TACString;
import com.pelleplutt.operandi.TAC.TACUnaryOp;
import com.pelleplutt.operandi.TAC.TACUndefineMe;
import com.pelleplutt.operandi.TAC.TACUnresolved;
import com.pelleplutt.operandi.TAC.TACVar;

public class CodeGenFront {
  public static boolean dbg = false;
  public static boolean dbgDot = false;
  static int label = 0;
  static int anonIx = 0;
  List<FrontFragment> ffrags = new ArrayList<FrontFragment>();
  FrontFragment ffrag;
  IntermediateRepresentation irep;
  
  public static IntermediateRepresentation genIR(ASTNodeBlok e, IntermediateRepresentation ir, Source src) {
    if (ir == null) {
      ir = new IntermediateRepresentation();
    }
    CodeGenFront cg = new CodeGenFront();
    cg.irep = ir;
    List<Module> mods = cg.doIntermediateCode(e, src);
    ir.getModules().addAll(mods);
    return ir;
  }
  
  public CodeGenFront() {
  }
  
  public List<Module> doIntermediateCode(ASTNodeBlok eblk, Source src) {
    genIR(eblk, eblk, new Info());
    if (dbg) printIR(System.out);
    genCFG(ffrags);
    if (dbgDot) printDot(System.out);
    List<Module> res = gather(src);
    return res;
  }
  
  List<Module> gather(Source src) {
    Map<String, Module> mmap = new HashMap<String, Module>();
    for (FrontFragment ffrag : ffrags) {
      Module module = mmap.get(ffrag.module);
      if (module == null) {
        module = new Module();
        module.id = ffrag.module;
        mmap.put(ffrag.module, module);
      }
      ModuleFragment frag = new ModuleFragment(ffrag.defNode, src);
      frag.fragname = ffrag.name;
      frag.type = ffrag.type;
      frag.modname = module.id;
      for (Block block : ffrag.blocks) {
        frag.addTACBlock(block.ir);
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
    out.println("CONTEXT " + ffrag.module + "" + ffrag.name);
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
  
  // dot -Tps gtest.gv -o gtest.ps && okular gtest.ps 
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
  
  boolean isAboveTraceCall(Info info, int steps) {
    if (info.trace.size() < steps + 1) return false;
    return info.trace.get(info.trace.size()-1-steps) == OP_CALL;
  }

  void clearAboveTraceCall(Info info, int steps) {
    info.trace.set(info.trace.size()-1-steps, OP_SPACES);
  }

  boolean isFirstTraceAssign(Info info) {
    return info.trace.get(0) == OP_EQ;
  }

// unwinds dotted symbols (a.b), dereferenced symbols (a[b]), and function names (a.b() or a[b]() or a())
// n.b. fixup assignment a.b.c[e] = 4;
//      fixup calls, define me a.b.c(); me = a.b
//      fixup combinations a.b[c]().d()[e] = 4;
  public static boolean dbgUnwind = false;
  TAC genIRUnwindSymbol(ASTNode e, ASTNodeBlok parentEblk, Info info) {
    info.trace.add(e.op);
    if (dbgUnwind) {
      System.out.print("UNWIND [");
      for (int op : info.trace) {
        System.out.print(AST.opString(op) + " ");
      }
      System.out.println("] " +  e + "  ");
    }
    if (e.op == OP_CALL) {
      
      ASTNodeFuncCall callNode = (ASTNodeFuncCall)e;
      String args[] = new String[e.operands.size()];
      for (int i = args.length-1; i >= 0; i--) {
        TAC argVal = genIR(e.operands.get(i), parentEblk, new Info());
        TAC arg = new TACFuncArg(e, argVal);
        setReferenced(argVal);
        if (dbgUnwind) System.out.println("   + funcarg  " + arg + " func " + callNode);
        add(arg);
      }
      

      TACCall tcall = null;
      if (callNode.callByOperation) {
        // call by operation
        TAC addrGen = genIRUnwindSymbol(callNode.callAddrOp, parentEblk, info);
        setReferenced(addrGen);
        if (dbgUnwind) System.out.println("   + funcaddr " + addrGen + " func " + callNode);
        add(addrGen);
        tcall = new TACCall((ASTNodeFuncCall)e, args.length, parentEblk.module, null, null);
        info.trace.pop();
        return tcall;
      } else {
        // call by symbol
        ASTNodeSymbol callSym = callNode.name;
        ASTNodeBlok declBlok = getScopeIfDef(parentEblk, callSym);
        TACVar tvar = null;
        if (declBlok != null) {
          // found a variable with the call name, so presume we're calling a function variable
          tvar = new TACVar(e, ((ASTNodeFuncCall)e).name.symbol, declBlok.getModule(), null, declBlok.getScopeId());
        }
        tcall = new TACCall((ASTNodeFuncCall)e, args.length, parentEblk.module, null, tvar);
        if (tvar == null) tcall.funcAddrInVar = false;
        info.trace.pop();
        return tcall;
      }

    } else if (e.op == OP_DOT) {
      
      ASTNode edottee = e.operands.get(0);
      ASTNode edotter = e.operands.get(1);
      TAC tdottee;

      // reached bottom of path?
      if (edottee.op == OP_SYMBOL && edotter.op == OP_SYMBOL) {
        // first, try if initial symbol is a reachable variable. If so, prefer local
        ASTNodeBlok declBlok = getScopeIfDef(parentEblk, (ASTNodeSymbol)edottee);
        if (declBlok != null) {
          // it was, so consider this a hash map
          // keep going
        } else {
          // initial symbol unreachable, thus <module>.<var>
          // add unresolved for first two path entries <module>.<variable>
          TAC tunres = new TACUnresolved(e, ((ASTNodeSymbol)edottee).symbol, ((ASTNodeSymbol)edotter).symbol); 
          // <module>.<map>....
          if (isAboveTraceCall(info, 2)) {
            // on call, set unresolved to "me"
            if (dbgUnwind) System.out.println("   + defme2");
            add(new TACDefineMe(e));
            clearAboveTraceCall(info, 2); // unmark this call so we do not get 2 defmes
          }
          info.trace.pop();
          return tunres;
        }
      }

      // recurse further down to path, depth first
      tdottee = genIRUnwindSymbol(edottee, parentEblk, info);
      if (dbgUnwind) System.out.println("   + dotee " + tdottee);
      add(tdottee);
      setReferenced(tdottee);
      if (isAboveTraceCall(info, 1)) {
        if (dbgUnwind) System.out.println("   + defme3");
        add(new TACDefineMe(e));
      }
      
      TAC tdotter = new TACString(edotter, ((ASTNodeSymbol)edotter).symbol);
      setReferenced(tdotter);
      TAC tderef = new TACSetDeref(e, tdottee, tdotter);

      if (edottee.op != OP_DOT && edottee.op != OP_ADEREF) {
        // last dereference
        if (!isFirstTraceAssign(info)) {
          if (dbgUnwind) System.out.println("   + dotderef " + tderef);
          add(tderef);
        } else {
          // on assignment, do not add last read dereference, as backend will add write to that dereference
          setReferenced(tderef);
        }
      } else {
        setReferenced(tderef);
      }
      
      info.trace.pop();
      return tderef;      
    
    } else if (e.op == OP_ADEREF) {
    
      ASTNode ederefee = e.operands.get(0);
      ASTNode ederefer = e.operands.get(1);
      TAC tderefee;

      // recurse further down to path, depth first
      tderefee = genIRUnwindSymbol(ederefee, parentEblk, info);
      if (dbgUnwind) System.out.println("   + arree " + tderefee);
      add(tderefee);
      setReferenced(tderefee);
      if (isAboveTraceCall(info, 1)) {
        if (dbgUnwind) System.out.println("   + defme4");
        add(new TACDefineMe(e));
      }
      
      TAC tderefer = genIR(ederefer, parentEblk, new Info());
      setReferenced(tderefer);
      TAC tderef = new TACSetDeref(e, tderefee, tderefer);

      if (ederefee.op != OP_DOT && ederefee.op != OP_ADEREF) {
        // last dereference
        if (!isFirstTraceAssign(info)) {
          if (dbgUnwind) System.out.println("   + arrderef " + tderef);
          add(tderef);
        } else {
          // on assignment, do not add last read dereference, as backend will add write to that dereference
          setReferenced(tderef);
        }
      } else {
        setReferenced(tderef);
      }
      
      info.trace.pop();
      return tderef;      
      
    } else if (e.op == OP_SYMBOL || e.op == OP_ADECL || e.op == OP_RANGE || AST.isString(e.op)) {
      
      TAC tsym = genIR(e, parentEblk, info);
      info.trace.pop();
      return tsym;

    }
    
    throw new CompilerError("not implemented " + e, e);
  }
  
  TAC genIRAssignment(ASTNode assignee, ASTNodeBlok parentEblk, Info info) {
    if (assignee.op == OP_SYMBOL) {
      TAC ret = genIR(assignee, parentEblk, info);
      return ret;
    }
    else if (assignee.op == OP_ADEREF || assignee.op == OP_DOT) {
      return genIRUnwindSymbol(assignee, parentEblk, new Info(OP_EQ));
    }
    else if (AST.isNumber(assignee.op) || AST.isString(assignee.op) ||
        assignee.op == OP_CALL) {
      return null; // do nufin
    }
    else {
      throw new CompilerError("fatal, unknown assignee " + assignee.getClass().getSimpleName(), assignee);
    }
  }
  
  //
  // construct three address code intermediate representation
  //
  TAC genIR(ASTNode e, ASTNodeBlok parentEblk, Info info) {
    if (e.op == OP_BLOK) {
      ASTNodeBlok eblk = (ASTNodeBlok)e;
      FrontFragment oldFrag = ffrag;
      FrontFragment newFrag = null;
      
      if (eblk.type == ASTNodeBlok.TYPE_ANON) {
        newFrag = new FrontFragment(eblk, oldFrag.module, ".anon" + (anonIx++));
        newFrag.type = ASTNode.ASTNodeBlok.TYPE_ANON;
        ffrags.add(newFrag);
        ffrag = newFrag;
      } else if (eblk.type == ASTNodeBlok.TYPE_FUNC) {
        newFrag = new FrontFragment(eblk, oldFrag.module, ".func" + ((ASTNodeBlok)eblk).id);
        newFrag.type = ASTNode.ASTNodeBlok.TYPE_FUNC;
        ffrags.add(newFrag);
        ffrag = newFrag;
      } else if (ffrag == null) {
        // first context, must be globals
        ffrag = new FrontFragment(eblk, eblk.module == null ? ".MAIN" : eblk.module, ".main");
        ffrag.type = ASTNode.ASTNodeBlok.TYPE_MAIN;
        ffrags.add(ffrag);
      }
      
      if (eblk.getScopeLevel() == 0) {
        irep.addGlobalVariables(eblk.module, eblk.getVariables());
      }
      
      boolean doStackAllocation = (eblk.gotUnhandledVariables() &&
                                   // no variable stack allocation for top scopes, these are global vars
                                   eblk.getScopeLevel() > 0);
      if (doStackAllocation) {
        eblk.setVariablesHandled();
        TACAlloc talloc = new TACAlloc(eblk, eblk.getModule(), eblk.getScopeId(), eblk.type == ASTNodeBlok.TYPE_FUNC);
        add(talloc);
      }
      for (ASTNode e2 : e.operands) {
        genIR(e2, (ASTNodeBlok)e, info);
      }
      if (doStackAllocation) {
        // TODO what if programmer has a return statement? then we add free after return, stooooh-pidh!
        TACFree tfree = new TACFree(eblk, eblk.getModule(), eblk.getScopeId());
        add(tfree);
      }
      
      if (eblk.type == ASTNodeBlok.TYPE_ANON || eblk.type == ASTNodeBlok.TYPE_FUNC) {
        // add 'return' if not set by programmer for TYPE_ANON and TYPE_FUNC
        if (ffrag.ir.size() == 0 || !(ffrag.ir.get(ffrag.ir.size()-1) instanceof TACReturn)) {
          add(new TACReturn(e, new TACNil(e))); 
        }
        
      }
      ffrag = oldFrag;
      if (eblk.type == ASTNodeBlok.TYPE_ANON) {
        // create external variable list that the anonymous scope reaches out to
        List<TACVar> adsVars = new ArrayList<TACVar>();
        for (ASTNodeSymbol esym : eblk.getAnonymousDefinedScopeVariables()) {
          ASTNodeBlok declBlok = getScopeIfDef(parentEblk, esym);
          if (declBlok == null) throw new CompilerError("fatal", e);
          adsVars.add(new TACVar(esym, esym.symbol, declBlok.getModule(), null, declBlok.getScopeId()));
        }
        return new TACCode(e, newFrag, adsVars, ASTNodeBlok.TYPE_ANON);
      }
      return null;
    } 
    
    else if (isNum(e.op)) {
      ASTNodeNumeric enm = ((ASTNodeNumeric)e);
      return enm.frac ? new TACFloat(enm, (float)enm.value) : new TACInt(enm, (int)enm.value);
    }
    
    else if (isStr(e.op)) {
      return new TACString(e, ((ASTNodeString)e).string);
    }
    
    else if (e.op == OP_SYMBOL) {
      if (((ASTNodeSymbol)e).symbol.charAt(0) == '$') {
        return createInbuiltSymbol(e, ((ASTNodeSymbol)e).symbol.substring(1));
      }
      if ("me".equals(((ASTNodeSymbol)e).symbol)) {
        return new TACGetMe(e);
      }
      ASTNodeBlok declBlok = getScopeIfDef(parentEblk, (ASTNodeSymbol)e);
      if (declBlok != null) {
        return new TACVar(e, ((ASTNodeSymbol)e).symbol, declBlok.getModule(), null, declBlok.getScopeId());
      } else {
        if (dbg) System.out.println("  sym " + e + " is unresolved");
        return new TACUnresolved((ASTNodeSymbol)e, parentEblk);
      }
    }
    
    else if (e.op == OP_GLOBAL) {
      return null;
    }
    
    else if (e.op == OP_ADEREF) {
      TAC r = genIRUnwindSymbol(e, parentEblk, info);
      if (r.referenced) add(r);
      return r; 
    }
    
    else if (e.op == OP_DOT) {
      TAC r = genIRUnwindSymbol(e, parentEblk, info);
      if (r.referenced) add(r);
      return r; 
    }
    
    else if (e.op == OP_NIL) {
      return new TACNil(e);
    }
    
    else if (e.op == OP_EQ) {
      TAC assignee = genIRAssignment(e.operands.get(0), parentEblk, info);
      info.assignment++;
      TAC assignment = genIR(e.operands.get(1), parentEblk, info);
      info.assignment--;
      setReferenced(assignment);
      TAC op = new TACAssign(e, e.op, assignee, assignment); 
      add(op);
      return op;
    } 
    
    else if (AST.isOperator(e.op)) {
      if (!AST.isAssignOperator(e.op)) {
        if (AST.isUnaryOperator(e.op) || e.op == OP_CALL) {
          genIRAssignment(e.operands.get(0), parentEblk, info);
          TAC operand = genIR(e.operands.get(0), parentEblk, info);  
          setReferenced(operand);
          TAC op = new TACUnaryOp(e, e.op, operand);
          add(op);
          return op;
        } else {
          TAC left = genIR(e.operands.get(0), parentEblk, info);  
          TAC right = genIR(e.operands.get(1), parentEblk, info);  
          setReferenced(left);
          setReferenced(right);
          TAC op = new TACOp(e, e.op, left, right);
          add(op);
          return op;
        }
      } else {
        TAC assignee = genIRAssignment(e.operands.get(0), parentEblk, info);
        setReferenced(assignee);
        TAC assignOperandLeft = genIR(e.operands.get(0), parentEblk, info);
        setReferenced(assignOperandLeft);
        TAC assignOperandRight = genIR(e.operands.get(1), parentEblk, info);  
        setReferenced(assignOperandRight);
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
        TAC operation = new TACOp(e, opCode, assignOperandLeft, assignOperandRight);
        add(operation);
        TAC assignment = new TACAssign(e, OP_EQ, assignee, operation); 
        add(assignment);
        return assignment;
      }
    }
    
    else if (e.op == OP_IF) {
      boolean hasElse = e.operands.size() == 3;
      String label = genLabel();
      TACLabel lExit = new TACLabel(e, label+"_ifend");
      if (!hasElse) {
        TAC cond = genIR(e.operands.get(0), parentEblk, info);
        if (AST.isUnaryOperator(cond.op) || cond.op == OP_CALL) setReferenced(cond);
        TAC iffalsegoto = new TACGotoCond(e, cond, lExit, false);
        add(iffalsegoto);
        newBlock();
        TAC cres = genIR(e.operands.get(1), parentEblk, info);
        add(cres);
        // on a = (if b c else d); it should be referenced
        if (info.assignment > 0) {
          setReferenced(cres);
        }
        newBlock();
        add(lExit);
        return null;
      } else {
        TACLabel lElse = new TACLabel(e, label+"_ifelse");
        TAC cond = genIR(e.operands.get(0), parentEblk, info);
        if (AST.isUnaryOperator(cond.op) || cond.op == OP_CALL) setReferenced(cond);
        TAC iffalsegoto = new TACGotoCond(e, cond, lElse, false);
        add(iffalsegoto);
        newBlock();
        TAC cres = genIR(e.operands.get(1), parentEblk, info);
        add(cres);
        // on a = (if b c else d); it should be referenced
        if (info.assignment > 0) {
          setReferenced(cres);
        }
        TAC gotoExit = new TACGoto(e, lExit);
        add(gotoExit);
        newBlock();
        add(lElse);
        TAC celse = genIR(e.operands.get(2).operands.get(0), parentEblk, info);
        add(celse);
        // on a = (if b c else d); it should be referenced
        if (info.assignment > 0) {
          setReferenced(celse);
        }
        newBlock();
        add(lExit);
        return iffalsegoto;
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
        genIR(e.operands.get(0), parentEblk, info);
        newBlock();
        add(lLoop);
        // conditional : y
        TAC cond = genIR(e.operands.get(1), parentEblk, info);
        if (AST.isUnaryOperator(cond.op) || cond.op == OP_CALL) setReferenced(cond);
        TAC iffalsegoto = new TACGotoCond(e, cond, lExit, false);
        add(iffalsegoto);
        newBlock();
        pushLoop(lCont, lExit);
        // loop : w
        genIR(e.operands.get(3), parentEblk, info);
        boolean contCalled = loopWasContinued();
        popLoop();
        if (contCalled) {
          newBlock();
          add(lCont);
        }
        // evaluate : z
        genIR(e.operands.get(2), parentEblk, info);
        TAC gotoLoop = new TACGoto(e, lLoop);
        add(gotoLoop);
        newBlock();
        add(lExit);

        if (doStackAllocation) {
          add(new TACFree(eblk, eblk.getModule(), eblk.getScopeId()));
        }
        
      } else {
        //for (x in y) {w}
        String innerScope = getInnerScope();
        ASTNode ex = e.operands.get(0).operands.get(0); // for >X< in  y   w
        ASTNode ey = e.operands.get(0).operands.get(1); // for  x  in >Y<  w
        ASTNode ew = e.operands.get(1);                 // for  x  in  y  >W<
        if (ew.op == OP_BLOK) {
          ((ASTNodeBlok)ew).setVariablesHandled();
        }
        TACAlloc talloc = new TACAlloc(ew, innerScope, parentEblk);
        add(talloc);
        String label = genLabel();

        TACLabel lLoop = new TACLabel(e, label+"_floop");
        TACLabel lExit = new TACLabel(e, label+"_fexit");
        TACLabel lCont = new TACLabel(e, label+"_fcont");

        TACVar _set = new TACVar(parentEblk, TACAlloc.varSet, talloc.module, null, talloc.scope + innerScope); 
        TACVar _iter = new TACVar(parentEblk, TACAlloc.varIterator, talloc.module, null, talloc.scope + innerScope); 
        
        TAC assignee = genIRAssignment(ex, parentEblk, info);
        
        // inital .set = y, .iter = 0
        // .set = y 
        TAC setAssignment = genIR(ey, parentEblk, info);
        setReferenced(setAssignment);
        add(new TACAssign(
            ey,                                                           // ASTNode
            OP_EQ,                                                        // op
            _set,                                                         // inner set variable
            setAssignment));                                              // the set itself
        // .iter = 0 
        add(new TACAssign(
            ex,                                                           // ASTNode
            OP_EQ,                                                        // op
            _iter,                                                        // inner iterator variable
            new TACInt(ey, 0)));                                          // zero
        
        newBlock();
        add(lLoop);

        // conditional : y (.iter < len(.set))
        TACFuncArg _set_arg = new TACFuncArg(e, _set);
        add(_set_arg);
        setReferenced(_set_arg);
        TAC _len_set = new TACCall(e, "len", 1, talloc.module, null);
        add(_len_set);
        setReferenced(_len_set);
        TAC cond = new TACOp(e, AST.OP_LT, _iter, _len_set);
        if (AST.isUnaryOperator(cond.op) || cond.op == OP_CALL) setReferenced(cond);
        add(cond);
        TACGotoCond iffalsegoto = new TACGotoCond(e, cond, lExit, false);
        iffalsegoto.condOp = AST.OP_LT;
        add(iffalsegoto);
        newBlock();
        pushLoop(lCont, lExit);

        // loop : x = .set[.iter]
        add(_set);
        setReferenced(_set);
        TAC _set_iter = new TACSetRead(ex, _set, _iter);
        add(_set_iter);
        setReferenced(_set_iter);
        add(new TACAssign(
            ex,                                                           // ASTNode
            OP_EQ,                                                        // op
            assignee,                                                     // x
            _set_iter));                                                  // .set[.iter]

        // loop : w
        genIR(ew, parentEblk, info);
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
        
        TACFree tfree = new TACFree(talloc);
        add(tfree);

      }
      return null;
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
      TAC cond = genIR(e.operands.get(0), parentEblk, info);
      if (AST.isUnaryOperator(cond.op) || cond.op == OP_CALL) setReferenced(cond);
      TAC iffalsegoto = new TACGotoCond(e, cond, lExit, false);
      add(iffalsegoto);
      newBlock();
      pushLoop(lLoop, lExit);
      genIR(e.operands.get(1), parentEblk, info);
      popLoop();
      TAC gotoLoop = new TACGoto(e, lLoop);
      add(gotoLoop);
      newBlock();
      add(lExit);
      
      if (doStackAllocation) {
        add(new TACFree(eblk, eblk.getModule(), eblk.getScopeId()));
      }
      return null;
    }
    
    else if (e.op == OP_BREAK) {
      TACLabel lExit = getLoopBreakLabel();
      add(new TACGoto(e, lExit));
      newBlock();
      return null;
    }
    
    else if (e.op == OP_CONTINUE) {
      TACLabel lCont = getLoopContinueLabel();
      add(new TACGoto(e, lCont));
      newBlock();
      return null;
    }
    
    else if (e.op == OP_GOTO) {
      TACLabel dest = new TACLabel(e.operands.get(0), ((ASTNodeSymbol)e.operands.get(0)).symbol);
      TAC gotoDest = new TACGoto(e, dest);
      add(gotoDest);
      newBlock();
      return null;
    }
    
    else if (e.op == OP_CALL) {
      if (!(e instanceof ASTNodeFuncCall)) throw new CompilerError("bad call construct", e);
      ASTNodeFuncCall ecall = (ASTNodeFuncCall)e;
      String args[] = new String[e.operands.size()];
      for (int i = args.length-1; i >= 0; i--) {
        TAC argVal = genIR(e.operands.get(i), parentEblk, info);
        TAC arg = new TACFuncArg(e, argVal);
        setReferenced(argVal);
        add(arg);
      }
      
      TACCall tcall = null;
      if (ecall.callByOperation) {
        // call by operation
        TAC addrGen = genIRUnwindSymbol(ecall.callAddrOp, parentEblk, new Info(OP_CALL));
        setReferenced(addrGen);
        add(addrGen);
        tcall = new TACCall((ASTNodeFuncCall)e, args.length, parentEblk.module, null, null);
        add(tcall);
      } else {
        // call by symbol
        ASTNodeSymbol callSym = ecall.name;
        ASTNodeBlok declBlok = getScopeIfDef(parentEblk, callSym);
        TACVar tvar = null;
        if (declBlok != null) {
          // found a variable with the call name, so presume we're calling a function variable
          tvar = new TACVar(e, ((ASTNodeFuncCall)e).name.symbol, declBlok.getModule(), null, declBlok.getScopeId());
        }
        tcall = new TACCall((ASTNodeFuncCall)e, args.length, parentEblk.module, null, tvar);
        if (tvar == null) tcall.funcAddrInVar = false;
        add(new TACUndefineMe(e));
        add(tcall);
      }
      return tcall;
    }

    else if (e.op == OP_FUNCDEF) {
      genIR(e.operands.get(0), parentEblk, info);
      return null;
    }
    
    else if (e.op == OP_RETURN) {
      TACReturn ret;
      if (e.operands != null && e.operands.size() > 0) {
        TAC retVal = genIR(e.operands.get(0), parentEblk, info);
        setReferenced(retVal);
        ret = new TACReturn(e, retVal);
      } else {
        ret = new TACReturn(e);
      }
      add(ret);
      newBlock();
      return null;
    }
    
    else if (e.op == OP_BKPT) {
      add(new TACBkpt(e));
      return null;
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
          TAC.TACMapTuple tuple = (TAC.TACMapTuple)genIR(e2, parentEblk, info);
          tuple.isLast = i == len-1;
          add(tuple);
        }
      } else {
        if (adecle.onlyPrimitives) {
          TAC.TACArrInit tarr = new TAC.TACArrInit(adecle); 
          for (ASTNode e2 : adecle.operands) {
            TAC arrentry = genIR(e2, parentEblk, info);
            tarr.entries.add(arrentry);
          }
          set = tarr;
          add(set);
        } else {
          for (ASTNode e2 : adecle.operands) {
            TAC entry = genIR(e2, parentEblk, info);
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
      TAC key = genIR(e.operands.get(0), parentEblk, info);
      TAC val = genIR(e.operands.get(1), parentEblk, info);
      setReferenced(key);
      setReferenced(val);
      TAC tuple = new TAC.TACMapTuple(e, key, val);
      return tuple;
    }
    
    else if (e.op == OP_RANGE) {
      TAC r;
      if (e.operands.size() == 2) {
        TAC efrom = genIR(e.operands.get(0), parentEblk, info); 
        add(efrom);
        setReferenced(efrom);
        TAC eto = genIR(e.operands.get(1), parentEblk, info);
        add(eto);
        setReferenced(eto);
        r = new TAC.TACRange((ASTNodeRange)e, efrom, eto); 
      } else {
        TAC efrom = genIR(e.operands.get(0), parentEblk, info); 
        add(efrom);
        setReferenced(efrom);
        TAC estep = genIR(e.operands.get(1), parentEblk, info); 
        add(estep);
        setReferenced(estep);
        TAC eto = genIR(e.operands.get(2), parentEblk, info);
        add(eto);
        setReferenced(eto);
        r = new TAC.TACRange((ASTNodeRange)e, efrom, estep, eto); 
      }
      add(r);
      return r;
    }
    else if (e.op == OP_MODULE) {
      return null;
    }
    else {
      throw new CompilerError("not implemented " + AST.opString(e.op), e);
    }
    // TODO moar?
  }
  
  void add(TAC t) {
    if (t == null) return;
    if (t.added) return;
    if (ffrag.block == null) {
      ffrag.block = new Block();
      ffrag.blocks.add(ffrag.block);
    }
    ffrag.block.add(t);
    ffrag.ir.add(t);
    t.ffrag = ffrag;
    t.added = true;
//    System.out.println("*********************** TAC " + t + " added");
//    printContext(System.out, t.ffrag);
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
  
  boolean loopWasBroken() {
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
  
  TAC createInbuiltSymbol(ASTNode e, String isym) {
    boolean isNumeric = true;
    int num = -1;
    try {num = Integer.parseInt(isym); } catch(Throwable t) {isNumeric=false;}
         if (isym.equals("argc")) return new TACArgc(e);
    else if (isym.equals("argv")) return new TACArgv(e);
    else if (isNumeric)           return new TACArgNbr(e, num);
    else throw new CompilerError("not implemented $" + isym);
  }
  
  ASTNodeBlok getScopeIfDef(ASTNodeBlok eblk, ASTNodeSymbol sym) {
    if (sym.symbol.charAt(0) == '$' || "me".equals(sym.symbol)) return eblk;  // handle $argc, $argv, $<num>, me
    int ix = 0;
    int whenceSymNbr = sym.symNbr;
    ASTNodeBlok veblk = eblk;
    while (veblk != null) {
      if (dbg) System.out.println((ix++) + " looking for " + sym + " in vars:" + veblk.getVariables() + " args:" + veblk.getArguments() + " ads: " +veblk.getAnonymousDefinedScopeVariables());// + ": " + veblk);
      if (dbg) System.out.println("  declared in this scope: " + veblk.declaresVariableInThisScope(sym));
      if (dbg) System.out.println("  is declared previously: " +  veblk.isSymbolDeclaredBefore(sym, whenceSymNbr));
      if ((veblk.getArguments() != null && veblk.getArguments().contains(sym)) ||
          (veblk.declaresVariableInThisScope(sym) && veblk.isSymbolDeclaredBefore(sym, whenceSymNbr)) ||
          (veblk.getAnonymousDefinedScopeVariables() != null && veblk.getAnonymousDefinedScopeVariables().contains(sym))) {
        if (dbg) System.out.println("  found");
        return veblk;
      }
      whenceSymNbr = veblk.symNbr;
      veblk = veblk.parentBlock;
    }
    List<ASTNodeSymbol> modGlobs = null;
    if (irep != null && (modGlobs = irep.getGlobalVariables(eblk.getModule())) != null) {
      if (dbg) System.out.print("  is " + sym + " in globals " + modGlobs + "?");
      if (modGlobs.contains(sym)) {
        if (dbg) System.out.println("  is declared in previous source");
        ASTNodeBlok declBlok = new ASTNodeBlok(); 
        declBlok.module = eblk.getModule();
        declBlok.id = ".0"; // global scope
        return declBlok;
      }
      else if (dbg) System.out.print(" false");
    } else {
      if (dbg) System.out.println("  no globals in mod " + eblk.getModule() + " modGlobs:" + modGlobs);
    }
    if (dbg) System.out.println("  NOT FOUND");
    return null;
  }


  //
  // IR context (e.g. main, func def, anonymous)
  //
  
  class FrontFragment {
    ASTNode defNode;
    String module, name;
    Block block; 
    List<Block> blocks = new ArrayList<Block>();
    List<TAC> ir = new ArrayList<TAC>();
    Stack<Loop> loopStack = new Stack<Loop>();
    int type; // ASTNode.ASTNodeBlok.TYPE_*
    
    public FrontFragment(ASTNode defNode, String module, String name) {
      this.defNode = defNode;
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
  
  class Info {
    public int assignment;
    Stack<Integer> trace = new Stack<Integer>();
    public Info() {}
    public Info(int op) {trace.push(op);}
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
