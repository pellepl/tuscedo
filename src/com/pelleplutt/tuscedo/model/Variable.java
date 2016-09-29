package com.pelleplutt.tuscedo.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Variable {
  public static final String INPUTSTREAM = "in"; 
  public static final String OUTPUTSTREAM = "out"; 
  
  Map<String, Variable> children;
  final String name;
  Object value;
  
  public Variable(String name, Object value) {
    this.name = name;
    this.value = value;
  }
  
  public void set(String name, Variable v) {
    if (children == null) {
      children = new HashMap<String, Variable>();
    }
    children.put(name, v);
  }
  
  public Object getValue() {
    return value;
  }
  
  public void setValue(Object value) {
    this.value = value;
  }

  public Variable getChild(String name) {
    if (children == null || children.isEmpty()) {
      return null;
    }
    Variable v = children.get(name);
    return v;
  }
  
  public String[] getChildNames() {
    if (children == null) return null;
    Set<String> s = children.keySet();
    return s.toArray(new String[s.size()]);
  }
}
