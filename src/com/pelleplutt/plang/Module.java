package com.pelleplutt.plang;

import java.util.ArrayList;
import java.util.List;

import com.pelleplutt.plang.TAC.TACVar;

public class Module {
  String id;
  List<ModuleFragment> frags = new ArrayList<ModuleFragment>();
  
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(id + "[ ");
    for (ModuleFragment f : frags) {
      sb.append(f.fragname + " ");
    }
    sb.append("]");
    return sb.toString();
  }
}
