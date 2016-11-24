package com.pelleplutt.plang;

import java.util.ArrayList;
import java.util.List;

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
        t instanceof TACArrayDeref) {
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
  
  public static class TACArrayEntry extends TAC {
    TAC entry;
    public TACArrayEntry(ASTNode e, TAC arg) {
      super(e); this.entry = arg;
    }
    public String toString() {return "ENTRY " + ref(entry);}
  }
  public static class TACArray extends TAC {
    int entries;
    public TACArray(ASTNode e, int entries) {
      super(e); this.entries = entries;
    }
    public String toString() {return "ARR " + entries + " entries";}
  }
  public static class TACArrayDeref extends TAC {
    TAC arr, derefVal;
    public TACArrayDeref(ASTNode e, TAC arr, TAC derefVal) {
      super(e); this.arr = arr; this.derefVal = derefVal;
    }
    public String toString() {return "DEREF " + ref(arr) + "<" + ref(derefVal) + ">";}
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
    boolean callByName;
    public TACCall(ASTNodeFuncCall e, int args, String module, TACVar var) {
      super(e); 
      this.args = args; this.var = var; this.module = module; this.callByName = e.callByName;
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
    public String toString() {
      String id;
      ASTNodeFuncCall e = (ASTNodeFuncCall)getNode(); 
      if (e.callByName) {
        String funcName = func;
        String moduleName = ((module != null ? (":" + module + ".func") : " (resolve)"));
        id = funcName+moduleName;
      } else {
        id = e.callAddrOp.toString();
      }
        
      return "CALL <" + id +"> " + args +" args";}
  }
  
  public static class TACBkpt extends TAC {
    public TACBkpt(ASTNode e) {
      super(e);
    }
  }
}
