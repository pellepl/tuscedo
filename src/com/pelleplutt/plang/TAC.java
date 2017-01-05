package com.pelleplutt.plang;

import java.util.ArrayList;
import java.util.List;

import com.pelleplutt.plang.ASTNode.ASTNodeArrDecl;
import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.ASTNode.ASTNodeCompoundSymbol;
import com.pelleplutt.plang.ASTNode.ASTNodeFuncCall;
import com.pelleplutt.plang.ASTNode.ASTNodeRange;
import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;
import com.pelleplutt.plang.CodeGenFront.FrontFragment;

public abstract class TAC {
  private ASTNode e;
  boolean referenced;
  boolean added;
  FrontFragment ffrag; // only needed for debug when listing the IR

  static boolean dbgResolveRefs = false;
  
  TAC(ASTNode e) {
    this.e = e;
  }
  
  public ASTNode getNode() {
    return e;
  }
  
  String ref(TAC t){
    if (t instanceof TACVar || t instanceof TACString || 
        t instanceof TACInt || t instanceof TACFloat ||
        t instanceof TACCode || t instanceof TACUnresolved ||
        t instanceof TACSetDeref || t instanceof TACMapTuple) {
      return t.toString();
    } else if (ffrag != null && ffrag.ir != null) {
      if (dbgResolveRefs) return "(" + t.toString() + ")";
      return "["+Integer.toString(ffrag.ir.indexOf(t)) +"]";
    } else {
      return t.toString();
    }
  }
  
  public static class TACOp extends TAC {
    int op;
    TAC left, right;
    public TACOp(ASTNode e, int op, TAC left, TAC right) {
      super(e); this.op = op; this.left = left; this.right = right;
    }
    public String toString() {return ref(left) + " " + AST.opString(op) + " " + ref(right);}
  }
  
  public static class TACUnaryOp extends TAC {
    int op;
    TAC operand;
    public TACUnaryOp(ASTNode e, int op, TAC operand) {
      super(e); this.op = op; this.operand = operand;
    }
    public String toString() {return AST.opString(op) + ref(operand);}
  }
  
  public static class TACAssign extends TACOp {
    public TACAssign(ASTNode e, int op, TAC left, TAC right) {
      super(e, op, left, right);
    }
  }
  
  public static class TACVar extends TAC {
    String symbol;
    String module;
    String declaredModule;
    String scopeId;
    public TACVar(ASTNode e, String symbol, String module, String declaredModule, String scopeId) {
      super(e); this.symbol = symbol; this.module = module; this.declaredModule = declaredModule; this.scopeId = scopeId;
    }
    public boolean equals(Object o) {
      if (o instanceof TACVar) {
        TACVar t = (TACVar)o;
        return symbol.equals(t.symbol) && module.equals(t.module) && scopeId.equals(t.scopeId);
      } else return false;
    }
    public int hashCode() { return symbol.hashCode(); }
    public String toString() { return symbol + ":" + module + scopeId; } 
  }
  
  public static class TACUnresolved extends TAC {
    String name;
    String module;
    public TACUnresolved(ASTNodeSymbol e, ASTNodeBlok referringBlock) {
      super(e); name = e.symbol; module = referringBlock.getModule();
    }
    public TACUnresolved(ASTNode e, String module, String name) {
      super(e); this.module = module; this.name = name;
    }
    public String toString() { return module + "." + name + "$L"; } 
  }
  
  public static class TACNil extends TAC {
    public TACNil(ASTNode e) {
      super(e);
    }
    public boolean equals(Object o) {
      return (o instanceof TACNil);
    }
    public int hashCode() {
      return 0;
    }
    public String toString() {return "(nil)";}
  }
  
  public static class TACInt extends TAC {
    int x;
    public TACInt(ASTNode e, int x) {
      super(e); this.x = x;
    }
    public boolean equals(Object o) {
      if (o instanceof TACInt) {
        TACInt t = (TACInt)o;
        return x == t.x;
      } else return false;
    }
    public int hashCode() {
      return x;
    }
    public String toString() {return Integer.toString(x);}
  }
  
  public static class TACString extends TAC {
    String x;
    public TACString(ASTNode e, String x) {
      super(e); this.x = x;
    }
    public boolean equals(Object o) {
      if (o instanceof TACString) {
        TACString t = (TACString)o;
        return x.equals(t.x);
      } else return false;
    }
    public int hashCode() {
      return x.hashCode();
    }
    public String toString() {return "'" + x + "'";}
  }
  
  public static class TACCode extends TAC {
    FrontFragment ffrag;
    int addr;
    int type;
    List<TACVar> adsVars;
    public TACCode(ASTNode e, FrontFragment ffrag, int type) {
      super(e); this.ffrag = ffrag; this.type = type;
    }
    public TACCode(ASTNode e, FrontFragment ffrag, List<TACVar> adsVars, int type) {
      super(e); this.ffrag = ffrag; this.type = type; this.adsVars = adsVars;
    }
    public TACCode(ASTNode e, int address, int type) {
      super(e); this.addr = address; this.type = type;
    }
    
    public boolean equals(Object o) {
      if (o instanceof TACCode) {
        TACCode t = (TACCode)o;
        return (ffrag != null && ffrag.module.equals(t.ffrag.module) && ffrag.name.equals(t.ffrag.name)) ||
               (ffrag == null && addr == t.addr);
      } else return false;
    }
    public int hashCode() {
      return ffrag != null ? ffrag.module.hashCode() ^ ffrag.name.hashCode() : addr;
    }
    public String toString() {return ffrag != null ? ffrag.module + ffrag.name : String.format("0x%06x", addr);}
  }
  
  public static class TACFloat extends TAC {
    float x;
    public TACFloat(ASTNode e, float x) {
      super(e); this.x = x;
    }
    public String toString() {return Float.toString(x);}
  }
  
  public static class TACSet extends TAC {
    int entries;
    public TACSet(ASTNode e, int entryCount) {
      super(e); this.entries = entryCount;
    }
    public String toString() {return "SET " + entries + " entries";}
  }
  public static class TACArrInit extends TAC {
    List<TAC> entries = new ArrayList<TAC>();
    public TACArrInit(ASTNodeArrDecl e) {
      super((ASTNode)e);
    }
    public String toString() {return "AINIT " + entries;}
  }
  public static class TACMap extends TAC {
    int tuples;
    public TACMap(ASTNode e, int tuples) {
      super(e); this.tuples = tuples;
    }
    public String toString() {return "MAP " + tuples + " tuples";}
  }
  public static class TACArrEntry extends TAC {
    TAC entry;
    public TACArrEntry(ASTNode e, TAC arg) {
      super(e); this.entry = arg;
    }
    public String toString() {return "AENTRY " + ref(entry);}
  }
  public static class TACMapTuple extends TAC {
    TAC key; TAC val;
    public boolean isLast;
    public TACMapTuple(ASTNode e, TAC key, TAC val) {
      super(e); this.key = key; this.val = val; 
    }
    public String toString() {return "MTUPLE " + ref(key) + ":" + ref(val) + (isLast ? " last" : "");}
  }
  public static class TACSetDeref extends TAC {
    TAC set, derefVal;
    public TACSetDeref(ASTNode e, TAC set, TAC derefVal) {
      super(e); this.set = set; this.derefVal = derefVal;
    }
    public String toString() {return "SDEREF (" + ref(set) + "<" + ref(derefVal) + ">)";}
  }
  public static class TACSetRead extends TAC {
    TAC set, derefIx;
    public TACSetRead(ASTNode e, TAC set, TAC derefIx) {
      super(e); this.set = set; this.derefIx = derefIx;
    }
    public String toString() {return "SREADIX (" + ref(set) + "<" + ref(derefIx) + ">)";}
  }
  
  public static class TACGoto extends TAC {
    TACLabel label;
    public TACGoto(ASTNode e, TACLabel label) {
      super(e); this.label = label;
    }
    public String toString() {return "GOTO " + label.label;}
  }
  
  public static class TACGotoCond extends TAC {
    int condOp;
    TACLabel label;
    TAC cond;
    boolean positive;
    public TACGotoCond(ASTNode e, TAC cond, TACLabel label, boolean condPositive) {
      super(e); this.condOp = cond.getNode().op; this.cond = cond; this.label = label; this.positive = condPositive;
    }
    public String toString() {return (positive ? "IF " : "IFNOT ") + ref(cond) + " GOTO " + label.label;}
  }
  
  public static class TACLabel extends TAC {
    String label;
    public TACLabel(ASTNode e, String label) {
      super(e); this.label = label;
    }
    public String toString() {return label +":";}
  }
  
  public static class TACAlloc extends TAC {
    public static final String varIterator = ".iter";
    public static final String varSet = ".set";
    List<String> adsVars = new ArrayList<String>();
    List<String> vars = new ArrayList<String>();
    List<String> tvars = new ArrayList<String>();
    List<String> args = new ArrayList<String>();
    String module, scope, tScope;
    boolean funcEntry;
    
    public TACAlloc(ASTNodeBlok e, String module, String scope) {
      this(e, module, scope, false);
    }
    
    public TACAlloc(ASTNodeBlok e, String module, String scope, boolean funcEntry) {
      super(e);
      this.module = module;
      this.scope = scope;
      this.funcEntry = funcEntry;
      if (e.getVariables() != null) {
        for (ASTNodeSymbol sym : e.getVariables()) {
          vars.add(sym.symbol);
        }
      }
      if (e.getArguments() != null) {
        for (ASTNodeSymbol sym : e.getArguments()) {
          args.add(sym.symbol);
        }
      }
      if (e.getAnonymousDefinedScopeVariables() != null) {
        for (ASTNodeSymbol sym : e.getAnonymousDefinedScopeVariables()) {
          adsVars.add(sym.symbol);
        }
      }
    }

    // for each constructor (for x in y)
    public TACAlloc(ASTNode loopCode, String tScope, ASTNodeBlok parentEblk) {
      super(loopCode instanceof ASTNodeBlok ? loopCode : parentEblk);
      if (loopCode instanceof ASTNodeBlok) {
        parentEblk = (ASTNodeBlok)loopCode;
        if (parentEblk.getVariables() != null) {
          for (ASTNodeSymbol sym : parentEblk.getVariables()) {
            vars.add(sym.symbol);
          }
        }
      }
      this.module = parentEblk.getModule();
      this.scope = parentEblk.getScopeId();
      this.tScope = tScope;
      tvars.add(varIterator);
      tvars.add(varSet);
    }

    public int variablesOnStack() {
      return vars.size() + tvars.size();
    }
    
    public int countADSVars() {
      return adsVars.size();
    }
    
    public String toString() {return "ALLO " + vars + (!tvars.isEmpty() ? " " + tvars : "") +
        (!adsVars.isEmpty() ? " ADS:" + adsVars : "") +
        (funcEntry ? " FUNC" + args : "");}
  }

  public static class TACFree extends TAC {
    List<String> adsVars = new ArrayList<String>();
    List<String> vars = new ArrayList<String>();
    List<String> tvars = new ArrayList<String>();
    String module, scope, tScope;
    public TACFree(ASTNodeBlok e, String module, String scope) {
      super(e);
      this.module = module;
      this.scope = scope;
      if (e.getVariables() != null) {
        for (ASTNodeSymbol sym : e.getVariables()) {
          vars.add(sym.symbol);
        }
      }
      if (e.getAnonymousDefinedScopeVariables() != null) {
        for (ASTNodeSymbol sym : e.getAnonymousDefinedScopeVariables()) {
          adsVars.add(sym.symbol);
        }
      }
    }
    public TACFree(TACAlloc x) {
      super(x.getNode());
      this.module = x.module;
      this.scope = x.scope;
      this.tScope = x.tScope;
      this.vars = x.vars;
      this.tvars = x.tvars;
      this.adsVars = x.adsVars;
    }
    public int variablesOnStack() {
      return vars.size() + tvars.size() + adsVars.size();
    }
    public String toString() {return "FREE " + vars + (!tvars.isEmpty() ? " " + tvars : "")+ (!adsVars.isEmpty() ? " " + adsVars : "");}
  }
  
  public static class TACFuncArg extends TAC {
    TAC arg;
    public TACFuncArg(ASTNode e, TAC arg) {
      super(e); this.arg = arg;
    }
    public String toString() {return "FARG " + ref(arg);}
  }
  public static class TACReturn extends TAC {
    TAC ret;
    boolean emptyReturn;
    public TACReturn(ASTNode e) {
      super(e);
      emptyReturn = true;
    }
    public TACReturn(ASTNode e, TAC ret) {
      super(e); this.ret = ret;
      emptyReturn = ret == null;
    }
    public String toString() {return "RET " + (emptyReturn ? "" : ref(ret));}
  }
  
  public static class TACCall extends TAC {
    String module;
    String declaredModule;
    String func;
    int args;
    // if true, call by name or variable, else func addr is presumed to be on stack
    boolean funcNameDefined;
    // if true, func address is a variable, denoted in var
    boolean funcAddrInVar;
    // if funcAddrInVar is true, this is the variable func pointer
    TACVar var;
    
    public TACCall(ASTNodeFuncCall e, int args, String module, String declaredModule, TACVar var) {
      super(e); 
      this.args = args; this.var = var; this.module = module; this.declaredModule = declaredModule;
      funcNameDefined = !e.callByOperation;
      funcAddrInVar = var != null;
      if (e.name instanceof ASTNodeCompoundSymbol) {
        ASTNodeCompoundSymbol ce = (ASTNodeCompoundSymbol)e.name;
        this.func = ce.dots.get(1).symbol;  
      } else {
        if (e.name == null) {
          // func address on stack
        } else {
          this.func = e.name.symbol;
        }
      }
    }
    public TACCall(ASTNode e, String funcName, int args, String module, String declaredModule) {
      super(e); 
      this.args = args; this.var = null; this.module = module; this.declaredModule = declaredModule;
      funcNameDefined = true;
      funcAddrInVar = false;
      this.func = funcName;
    }
    public String toString() {
      String id;
      ASTNodeFuncCall efc = null;
      if (getNode() instanceof ASTNodeFuncCall) efc = (ASTNodeFuncCall)getNode(); 
      if (efc == null || !efc.callByOperation) {
        String funcName = func;
        String moduleName = ((module != null ? (":" + module + ".func") : " (resolve)"));
        id = funcName+moduleName;
      } else {
        id = efc.callAddrOp.toString();
      }
        
      return "CALL <" + id +"> " + args +" args";}
  }
  
  public static class TACRange extends TAC {
    boolean stepDefined;
    TAC from, step, to;
    private TACRange(ASTNodeRange e, boolean stepDefined) {
      super(e);
      this.stepDefined = stepDefined; 
    }
    public TACRange(ASTNodeRange e, TAC from, TAC to) {
      this(e, false);
      this.from = from;
      this.to = to;
    }
    public TACRange(ASTNodeRange e, TAC from, TAC step, TAC to) {
      this(e, true);
      this.from = from;
      this.step = step;
      this.to = to;
    }
    public String toString() {return "RANGE";}
  }
  
  public static class TACBkpt extends TAC {
    public TACBkpt(ASTNode e) {
      super(e);
    }
  }
  public static class TACArgc extends TAC {
    public TACArgc(ASTNode e) {
      super(e);
    }
    public String toString() {return "ARGC";}
  }
  public static class TACArgv extends TAC {
    public TACArgv(ASTNode e) {
      super(e);
    }
    public String toString() {return "ARGV";}
  }
  public static class TACArgNbr extends TAC {
    int arg;
    public TACArgNbr(ASTNode e, int arg) {
      super(e);
      this.arg = arg;
    }
    public String toString() {return "ARG#"+arg;}
  }
  public static class TACDefineMe extends TAC {
    public TACDefineMe(ASTNode e) {
      super(e);
    }
    public String toString() {return "ME_SET";}
  }
  public static class TACUndefineMe extends TAC {
    public TACUndefineMe(ASTNode e) {
      super(e);
    }
    public String toString() {return "ME_CLEAR";}
  }
  public static class TACGetMe extends TAC {
    public TACGetMe(ASTNode e) {
      super(e);
    }
    public String toString() {return "ME_GET";}
  }
}
