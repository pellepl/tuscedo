package com.pelleplutt.operandi;

import java.util.ArrayList;
import java.util.List;

public class Module {
  String id;
  List<ModuleFragment> frags = new ArrayList<ModuleFragment>();
  boolean compiled;
  
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
