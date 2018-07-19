package com.pelleplutt.operandi.proc;

import java.util.Map;

import com.pelleplutt.operandi.proc.Processor.M;

public class MMapRef implements MSet {
  Map<Object, String> jm;
  public MMapRef(Map<Object, String> jm) {
    this.jm = jm;
  }

  @Override
  public int size() {
    return jm.size();
  }

  @Override
  public void add(M m) {
    MSet tuple = (MSet)m.ref;
    jm.put(tuple.get(0).getRaw(), tuple.get(1).asString());
  }

  @Override
  public void insert(int ix, M m) {
  }

  @Override
  public void set(M mix, M m) {
    jm.put(mix.getRaw(), m.asString());
  }

  @Override
  public void put(Object key, M m) {
    jm.put(key, m.asString());
  }

  @Override
  public M get(M m) {
    return new M(jm.get(m.getRaw()));
  }

  @Override
  public M get(int ix) {
    return getElement(ix);
  }

  @Override
  public void remove(M m) {
    jm.remove(m.getRaw());
  }

  @Override
  public M getElement(int ix) {
    Object keys[] = jm.keySet().toArray();
    if (ix < 0) ix = keys.length + ix;
    Object key = keys[ix];
    if (jm.get(key) == null) return null;
    M mval = new M(jm.get(key));
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
    return TMAP;
  }

  @Override
  public MSet copyShallow() {
    MListMap ml = new MListMap();
    int len = size();
    ml.makeMap();
    Object keys[] = jm.keySet().toArray();
    for (int i = 0; i < len; i++) {
      ml.map.put(keys[i], new M().copy(new M(jm.get(keys[i]))));
    }
    ml.type = getType();
    return ml;
  }

}
