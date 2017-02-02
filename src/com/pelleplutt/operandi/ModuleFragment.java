package com.pelleplutt.operandi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.operandi.ASTNode.ASTNodeBlok;
import com.pelleplutt.operandi.TAC.TACArrInit;
import com.pelleplutt.operandi.TAC.TACCall;
import com.pelleplutt.operandi.TAC.TACLabel;
import com.pelleplutt.operandi.TAC.TACUnresolved;
import com.pelleplutt.operandi.TAC.TACVar;
import com.pelleplutt.operandi.proc.Assembler;

public class ModuleFragment {
  String modname;
  String fragname;
  ASTNode defNode;
  // source code for this fragment
  Source source;
  // three address codes, block organised
  private List<List<TAC>> tacs = new ArrayList<List<TAC>>();
  // machine codes
  List<Byte> code = new ArrayList<Byte>();
  // labels / fragment machine code offset
  Map<TACLabel, Integer> labels = new HashMap<TACLabel, Integer>();
  // unresolved references
  List<Link> links = new ArrayList<Link>();
  // ASTNode.ASTNodeBlok.TYPE_*
  int type;
  // local variables defined in this fragment and their FP offset
  Map<TACVar, Integer> locals;
  // Linked fragment id
  public String fragId;
  // Linked executable offset
  public int executableOffset;
  // fragment machine code offset / string comment
  Map<Integer, String> dbgcomments = new HashMap<Integer, String>();
  // fragment machine code offset / (source location reference)
  List<Integer> FRAGMCsrcref = new ArrayList<Integer>();
  // (fragment machine code offset) / source location reference
  List<SrcRef> fragmcSRCREF = new ArrayList<SrcRef>();
  Map<Integer, SrcRef> fragmcsrcrefMap = new HashMap<Integer, SrcRef>();
  
  public ModuleFragment(ASTNode defNode, Source src) {
    this.defNode = defNode;
    source = src;
  }
  
  public List<List<TAC>> getTACBlocks() {
    return tacs;
  }
 
  public void addTACBlock(List<TAC> ir) {
    tacs.add(new ArrayList<TAC>(ir));
  }
  
  ASTNode prevNode = null;
  public void addCode(String comment, ASTNode node, int bytecode, Integer... bytecodeext) {
    if (comment != null) dbgcomments.put(code.size(), comment);
    Object[] srcinfo = null;
    if (!(node instanceof ASTNodeBlok)) {
      if (node != prevNode && node != null) {
        srcinfo = source.getLine(node.stroffset);
        if (srcinfo != null) {
          FRAGMCsrcref.add(new Integer(code.size()));
          SrcRef srcref = new SrcRef();
          srcref.line = (Integer)srcinfo[0];
          srcref.lineLen = ((String)srcinfo[1]).length();
          srcref.lineOffset = (Integer)srcinfo[2];
          srcref.symLen = node.strlen;
          srcref.symOffs = node.stroffset;
          fragmcSRCREF.add(srcref);
          fragmcsrcrefMap.put(new Integer(code.size()), srcref);
        }
      }
    }
    code.add((byte)bytecode);
    if (bytecodeext != null) {
      for (int c : bytecodeext) {
        code.add((byte)c);
      }
    }
    if (CodeGenBack.dbg) {
      if (!(node instanceof ASTNodeBlok)) {
        if (node != prevNode && node != null) {
          if (srcinfo != null) {
            String srcline = (String)srcinfo[1];
            String prefix = String.format("%s@%-4d: ", source.getName(), srcinfo[0]);
            System.out.println(prefix + srcline);
            int linemarkix = node.stroffset - (Integer)srcinfo[2];
            int linemarklen = node.strlen;
            for (int i = 0; i < linemarkix + prefix.length(); i++) System.out.print(" ");
            for (int i = 0; i < Math.min(linemarklen, srcline.length()); i++) System.out.print("-");
            System.out.println();
          }
        }
      }
      byte[] mc = new byte[1 + (bytecodeext == null ? 0 : bytecodeext.length)];
      int x = 0;
      mc[x++] = (byte)bytecode;
      if (bytecodeext != null) {
        for (int c : bytecodeext) {
          mc[x++] = (byte)c;
        }
      }
      System.out.println(String.format("        %-32s// %s", Assembler.disasm(mc, 0), comment));
    }
    prevNode = node;
  }
  
  public void write(int pc, int data, int len) {
    for (int i = 0; i < len; i++) {
      int d = (data >> ((len - i - 1)*8)) & 0xff;
      code.set(pc+i, (byte)(d));
    }
  }
  
  public int getPC() {
    return code.size();
  }
  
  public Source getSource() {
    return source;
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
  
  public String instructionDbg(int codeIx) {
    return dbgcomments != null ? dbgcomments.get(codeIx) : null;
  }

  public SrcRef getDebugInfoSourceNearest(int codeIx) {
    int i = 0;
    while (i < FRAGMCsrcref.size()) {
      if (codeIx <= FRAGMCsrcref.get(i)) {
        return fragmcSRCREF.get(i);
      }
      i++;
    }
    return null;
  }
  
  public SrcRef getDebugInfoSourcePrecise(int codeIx) {
    return fragmcsrcrefMap.get(codeIx);
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
  public static class SrcRef {
    int line, lineLen, lineOffset, symOffs, symLen;
  }
}
