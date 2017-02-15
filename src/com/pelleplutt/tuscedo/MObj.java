package com.pelleplutt.tuscedo;

import java.util.HashMap;
import java.util.Map;

import com.pelleplutt.operandi.proc.MListMap;
import com.pelleplutt.operandi.proc.MSet;
import com.pelleplutt.operandi.proc.Processor;
import com.pelleplutt.operandi.proc.Processor.M;
import com.pelleplutt.operandi.proc.ProcessorError;
import com.pelleplutt.tuscedo.ui.UIWorkArea;

public abstract class MObj implements MSet {
  UIWorkArea workarea;
  Map<String, M> map = new HashMap<String, M>();
  String type;
  
  public MObj(UIWorkArea wa, com.pelleplutt.operandi.Compiler comp, String type) {
    this.workarea = wa;
    init(wa, comp);
    this.type = type;
  }
  
  public void addFunc(String mapName, String funcName, com.pelleplutt.operandi.Compiler comp) {
    M funcVal = new Processor.M(comp.getLinker().lookupFunctionAddress(funcName));
    funcVal.type = Processor.TFUNC;
    map.put(mapName, funcVal);
  }
  
  public abstract void init(UIWorkArea wa, com.pelleplutt.operandi.Compiler comp);

  public void addVar(String varName, String varVal) {
    M var = new Processor.M(varVal);
    map.put(varName, var);
  }

  public void putIntern(String key, M m) {
    map.put(key, m);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public M get(M m) {
    return map.get(m.getRaw());
  }

  @Override
  public M get(int ix) {
    return getElement(ix);
  }

  @Override
  public M getElement(int ix) {
    Object keys[] = map.keySet().toArray();
    if (ix < 0) ix = keys.length + ix;
    Object key = keys[ix];
    M mval = map.get(key);
    if (mval == null) return null;
    MListMap res = new MListMap();
    res.makeTup();
    res.tup[0] = new M(key);
    res.tup[1] = mval;
    M mres = new M();
    mres.type = Processor.TSET;
    mres.ref = res;
    return mres;
  }

  @Override
  public int getType() {
    return MSet.TMAP;
  }

  @Override
  public void put(Object key, M m) {
    throw new ProcessorError(type + " object cannot be modified");
  }

  @Override
  public void remove(M m) {
    throw new ProcessorError(type + " object cannot be modified");
  }

  @Override
  public MSet copyShallow() {
    throw new ProcessorError(type + " object cannot be copied");
  }

  @Override
  public void insert(int ix, M m) {
    throw new ProcessorError(type + " object cannot be modified");
    
  }

  @Override
  public void add(M m) {
    throw new ProcessorError(type + " object cannot be modified");
  }

  @Override
  public void set(M mix, M m) {
    throw new ProcessorError(type + " object cannot be modified");
  }
  
  @Override
  public String toString() {
    return map.keySet().toString();
  }
}
