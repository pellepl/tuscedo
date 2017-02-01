package com.pelleplutt.tuscedo;

import java.util.HashMap;
import java.util.Map;

import com.pelleplutt.operandi.proc.MListMap;
import com.pelleplutt.operandi.proc.MSet;
import com.pelleplutt.operandi.proc.Processor;
import com.pelleplutt.operandi.proc.Processor.M;
import com.pelleplutt.operandi.proc.ProcessorError;
import com.pelleplutt.tuscedo.ui.WorkArea;

public class MSerial implements MSet {
  WorkArea workarea;
  Map<String, M> map = new HashMap<String, M>();
  
  public MSerial(WorkArea wa, com.pelleplutt.operandi.Compiler comp) {
    this.workarea = wa;
    addFunc("info", OperandiScript.FN_SERIAL_INFO, comp);
    addFunc("disconnect", OperandiScript.FN_SERIAL_DISCONNECT, comp);
    addFunc("connect", OperandiScript.FN_SERIAL_CONNECT, comp);
    addFunc("tx", OperandiScript.FN_SERIAL_TX, comp);
    addFunc("on_rx", OperandiScript.FN_SERIAL_ON_RX, comp);
    addFunc("clear_on_rx", OperandiScript.FN_SERIAL_ON_RX_CLEAR, comp);
    addFunc("list_on_rx", OperandiScript.FN_SERIAL_ON_RX_LIST, comp);
  }
  
  void addFunc(String mapName, String funcName, com.pelleplutt.operandi.Compiler comp) {
    M funcVal = new Processor.M(comp.getLinker().lookupFunctionAddress(funcName));
    funcVal.type = Processor.TFUNC;
    map.put(mapName, funcVal);
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
    throw new ProcessorError("serial object cannot be modified");
  }

  @Override
  public void remove(M m) {
    throw new ProcessorError("serial object cannot be modified");
  }

  @Override
  public MSet copyShallow() {
    throw new ProcessorError("serial object cannot be copied");
  }

  @Override
  public void insert(int ix, M m) {
    throw new ProcessorError("serial object cannot be modified");
    
  }

  @Override
  public void add(M m) {
    throw new ProcessorError("serial object cannot be modified");
  }

  @Override
  public void set(M mix, M m) {
    throw new ProcessorError("serial object cannot be modified");
  }
  
  @Override
  public String toString() {
    return map.keySet().toString();
  }
}
