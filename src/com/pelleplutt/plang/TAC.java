package com.pelleplutt.plang;

import java.util.ArrayList;
import java.util.List;

import com.pelleplutt.plang.ASTNode.ASTNodeArrDecl;
import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.ASTNode.ASTNodeCompoundSymbol;
import com.pelleplutt.plang.ASTNode.ASTNodeFuncCall;
import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;
import com.pelleplutt.plang.CodeGenFront.FrontFragment;

public abstract class TAC {
  private ASTNode e;
  boolean referenced;
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
    } else {
      if (dbgResolveRefs) return "(" + t.toString() + ")";
      return "["+Integer.toString(ffrag.ir.indexOf(t)) +"]";
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
      // TODO associativity
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
    String scopeId;
    public TACVar(ASTNode e, String symbol, String module, String scopeId) {
      super(e); this.symbol = symbol; this.module = module; this.scopeId = scopeId;
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
    public TACCode(ASTNode e, FrontFragment ffrag) {
      super(e); this.ffrag = ffrag;
    }
    public TACCode(ASTNode e, int address) {
      super(e); this.addr = address;
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
    public String toString() {return ffrag != null ? ffrag.module + ffrag.name : String.format("0x%08x", addr);}
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
    public String toString() {return "ARR " + entries + " entries";}
  }
  public static class TACArrInit extends TAC {
    List<TAC> entries = new ArrayList<TAC>();
    public TACArrInit(ASTNodeArrDecl e) {
      super((ASTNode)e);
    }
    public String toString() {return "ARRINIT " + entries;}
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
    public String toString() {return "ENTRY " + ref(entry);}
  }
  public static class TACMapTuple extends TAC {
    TAC key; TAC val;
    public boolean isLast;
    public TACMapTuple(ASTNode e, TAC key, TAC val) {
      super(e); this.key = key; this.val = val; 
    }
    public String toString() {return "TUPLE " + ref(key) + ":" + ref(val) + (isLast ? " last" : "");}
  }
  public static class TACSetDeref extends TAC {
    TAC set, derefVal;
    public TACSetDeref(ASTNode e, TAC set, TAC derefVal) {
      super(e); this.set = set; this.derefVal = derefVal;
    }
    public String toString() {return "DEREF (" + ref(set) + "<" + ref(derefVal) + ">)";}
  }
  public static class TACSetRead extends TAC {
    TAC set, derefIx;
    public TACSetRead(ASTNode e, TAC set, TAC derefIx) {
      super(e); this.set = set; this.derefIx = derefIx;
    }
    public String toString() {return "READDIX (" + ref(set) + "<" + ref(derefIx) + ">)";}
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
    List<String> vars = new ArrayList<String>();
    List<String> args = new ArrayList<String>();
    String module, scope;
    boolean funcEntry;
    
    public TACAlloc(ASTNodeBlok e, String module, String scope) {
      super(e);
      this.module = module;
      this.scope = scope;
      if (e.getVariables() != null) {
        for (ASTNodeSymbol sym : e.getVariables()) {
          vars.add(sym.symbol);
        }
      }
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
    }

    // for each constructor (for x in y)
    public TACAlloc(ASTNode e, ASTNodeBlok eblk) {
      super(e instanceof ASTNodeBlok ? e : eblk);
      if (e instanceof ASTNodeBlok) {
        eblk = (ASTNodeBlok)e;
        if (eblk.getVariables() != null) {
          for (ASTNodeSymbol sym : eblk.getVariables()) {
            vars.add(sym.symbol);
          }
        }
      }
      this.module = eblk.getModule();
      this.scope = eblk.getScopeId();
      vars.add(varIterator);
      vars.add(varSet);
    }
    
    public String toString() {return "ALLO " + vars + (funcEntry ? " FUNC" + args : "");}
  }

  public static class TACFree extends TAC {
    List<String> vars = new ArrayList<String>();
    String module, scope;
    public TACFree(ASTNodeBlok e, String module, String scope) {
      super(e);
      this.module = module;
      this.scope = scope;
      if (e.getVariables() != null) {
        for (ASTNodeSymbol sym : e.getVariables()) {
          vars.add(sym.symbol);
        }
      }
    }
    public TACFree(TACAlloc x) {
      super(x.getNode());
      this.module = x.module;
      this.scope = x.scope;
      this.vars = x.vars;
    }
    public String toString() {return "FREE " + vars;}
  }
  
  public static class TACArg extends TAC {
    TAC arg;
    public TACArg(ASTNode e, TAC arg) {
      super(e); this.arg = arg;
    }
    public String toString() {return "ARG " + ref(arg);}
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
    String func;
    int args;
    TACVar var;
    boolean link;
    // if true, call by name or variable, else func addr is presumed to be on stack
    boolean callByName;
    public TACCall(ASTNodeFuncCall e, int args, String module, TACVar var) {
      super(e); 
      this.args = args; this.var = var; this.module = module; callByName = e.callByName;
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
    public TACCall(ASTNode e, String funcName, int args, String module) {
      super(e); 
      this.args = args; this.var = null; this.module = module; callByName = true; link = true;
      this.func = funcName;
    }
    public String toString() {
      String id;
      ASTNodeFuncCall efc = null;
      if (getNode() instanceof ASTNodeFuncCall) efc = (ASTNodeFuncCall)getNode(); 
      if (efc == null || efc.callByName) {
        String funcName = func;
        String moduleName = ((module != null ? (":" + module + ".func") : " (resolve)"));
        id = funcName+moduleName;
      } else {
        id = efc.callAddrOp.toString();
      }
        
      return "CALL <" + id +"> " + args +" args";}
  }
  
  public static class TACBkpt extends TAC {
    public TACBkpt(ASTNode e) {
      super(e);
    }
  }
}
