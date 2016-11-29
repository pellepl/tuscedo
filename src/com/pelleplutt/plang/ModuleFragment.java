package com.pelleplutt.plang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.plang.TAC.TACArrInit;
import com.pelleplutt.plang.TAC.TACCall;
import com.pelleplutt.plang.TAC.TACLabel;
import com.pelleplutt.plang.TAC.TACUnresolved;
import com.pelleplutt.plang.TAC.TACVar;
import com.pelleplutt.plang.proc.Processor;

public class ModuleFragment {
  String modname;
  String fragname;
  // three address codes, block organised
  List<List<TAC>> tacs = new ArrayList<List<TAC>>();
  // executable offset
  int exeOffset;
  // machine codes
  List<Byte> code = new ArrayList<Byte>();
  // labels / fragment machine code offset
  Map<TACLabel, Integer> labels = new HashMap<TACLabel, Integer>();
  // unresolved references
  List<Link> links = new ArrayList<Link>();
  // fragment machine code offset / string comment
  Map<Integer, String> dbgcomments = new HashMap<Integer, String>();
  // ASTNode.ASTNodeBlok.TYPE_*
  int type;
  // locals
  Map<TACVar, Integer> locals;
  
  public ModuleFragment() {
    
  }
 
  public void addCode(String comment, int i, Integer... codes) {
    if (comment != null) dbgcomments.put(code.size(), comment);
    code.add((byte)i);
    if (codes != null) {
      for (int c : codes) {
        code.add((byte)c);
      }
    }
    if (CodeGenBack.dbg) {
      byte[] mc = new byte[i + (codes == null ? 0 : codes.length)];
      int x = 0;
      mc[x++] = (byte)i;
      if (codes != null) {
        for (int c : codes) {
          mc[x++] = (byte)c;
        }
      }
      System.out.println(String.format("        %-32s// %s", Processor.disasm(mc, 0), comment));
    }
  }
  
  public void write(int pc, int data, int len) {
    for (int i = 0; i < len; i++) {
      code.set(pc+i, (byte)((data >> ((len - i - 1)*8)) & 0xff));
    }
  }
  
  public int getPC() {
    return code.size();
  }
  
  public byte[] getMachineCode() {
    byte b[] = new byte[code.size()];
    for (int i = 0; i < code.size(); i++) {
      b[i] = code.get(i);
    }
    return b;
  }
  
  public int getMachineCodeLength() {
    return code.size();
  }
  
  public String commentDbg(int codeIx) {
    return dbgcomments.get(codeIx);
  }

  
  public static  abstract class Link {
    int pc;
    public Link(int pc) {this.pc = pc;};
  }

  public static class LinkGlobal extends Link {
    TACVar var;
    public LinkGlobal(int pc, TACVar var) {
      super(pc);
      this.var = var;
    }
    public String toString() {return String.format("link var %s @ 0x%06x", var.toString(), pc);}
  }
  public static class LinkUnresolved extends Link {
    TACUnresolved sym;
    public LinkUnresolved(int pc, TACUnresolved sym) {
      super(pc);
      this.sym = sym;
    }
    public String toString() {return String.format("link unresolved %s @ 0x%06x", sym.toString(), pc);}
  }
  public static class LinkConst extends Link {
    TAC cnst;
    public LinkConst(int pc, TAC c) {
      super(pc);
      this.cnst = c;
    }
    public String toString() {return String.format("link const %s @ 0x%06x", cnst.toString(), pc);}
  }
  public static class LinkGoto extends Link {
    TACLabel label;
    public LinkGoto(int pc, TACLabel l) {
      super(pc);
      this.label = l;
    }
  }
  public static class LinkCall extends Link {
    TACCall call;
    public LinkCall(int pc, TACCall c) {
      super(pc);
      this.call = c;
    }
    public String toString() {return String.format("link func %s @ 0x%06x", call.toString(), pc);}
  }
  public static class LinkArrayInitializer extends Link {
    TACArrInit arr;
    public LinkArrayInitializer(int pc, TACArrInit arr) {
      super(pc);
      this.arr = arr;
    }
    public String toString() {return String.format("link const array %d entries @ 0x%06x", arr.entries.size(), pc);}
  }
}
