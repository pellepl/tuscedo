package com.pelleplutt.plang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.plang.TAC.TACLabel;
import com.pelleplutt.plang.TAC.TACVar;

public class ModuleFragment {
  Module module;
  String name;
  // three address codes
  List<TAC> frags = new ArrayList<TAC>();
  // machine codes
  List<Byte> code = new ArrayList<Byte>();
  // labels / fragment machine code offset
  Map<TACLabel, Integer> labels = new HashMap<TACLabel, Integer>();
  // fragment machine code offset / unresolved references
  Map<Integer, Link> links = new HashMap<Integer, Link>();
  // global variables for this fragment
  List<TACVar> gvars = new ArrayList<TACVar>();
  //fragment machine code offset / string comment
  Map<Integer, String> dbgcomments = new HashMap<Integer, String>();
 
  public void addCode(int i, Integer... codes) {
    code.add((byte)i);
    if (codes != null) {
      for (int c : codes) {
        code.add((byte)c);
      }
    }
  }
  public void addCode(String comment, int i, Integer... codes) {
    dbgcomments.put(code.size(), comment);
    code.add((byte)i);
    if (codes != null) {
      for (int c : codes) {
        code.add((byte)c);
      }
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
  public String commentDbg(int codeIx) {
    return dbgcomments.get(codeIx);
  }
  
  public static  abstract class Link {}

  public static class LinkVar extends Link {
    TACVar var;
    public LinkVar(TACVar var) {
      this.var = var;
    }
  }
  public static class LinkConst extends Link {
    TAC c;
    public LinkConst(TAC c) {
      this.c = c;
    }
  }
  public static class LinkGoto extends Link {
    TACLabel l;
    public LinkGoto(TACLabel l) {
      this.l = l;
    }
  }
  
  //public static class LinkFunc extends Link {
  //public static class LinkLabel extends Link {

}
