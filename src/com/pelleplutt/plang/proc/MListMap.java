package com.pelleplutt.plang.proc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.plang.proc.Processor.M;

public class MListMap implements MSet {
  public static final int TARR = 10;
  public static final int TMAP = 11;
  public int type;
  public List<M> arr;
  public Map<Object,M> map;
  public MListMap() {
    type = Processor.TNIL;
  }
  public void makeArr() {
    type = TARR;
    arr = new ArrayList<M>();
  }
  public void makeMap() {
    type = TMAP;
    map = new HashMap<Object, M>();
  }
  public int size() {
    if (type == TARR) {
      return arr.size();
    } else if (type == TMAP) {
      return map.size();
    } else {
      return 0;
    }
  }
  public void add(M m) {
    if (size() == 0) {
      makeArr();
    }
    arr.add(m);
  }
  public void insert(int ix, M m) {
    if (size() == 0) {
      makeArr();
    }
    arr.add(ix, m);
  }
  public void set(M mix, M m) {
    if (size() == 0) {
      if (mix.type == Processor.TINT) {
        makeArr();
      } else {
        makeMap();
      }
    }
    if (type == TARR) {
      arr.set(mix.asInt(), m);
    } else if (type == TMAP) {
      map.put(mix.getRaw(), m);
    } else {
      
    }
  }
  public void set(Object key, M m) {
    if (size() == 0) {
      if (key instanceof Integer) {
        makeArr();
      } else {
        makeMap();
      }
    }
    if (type == TARR) {
      if (key instanceof Integer) {
        arr.set(((Integer)key).intValue(), m);
      }
    } else if (type == TMAP) {
      map.put(key, m);
    } else {
      
    }
  }
  public M get(M m) {
    if (type == TARR) {
      return arr.get(m.asInt());
    } else if (type == TMAP) {
      return map.get(m.getRaw());
    } else {
      return null;
    }
  }
  public M get(int ix) {
    if (type == TARR) {
      return arr.get(ix);
    } else if (type == TMAP) {
      return getElement(ix);
    } else {
      return null;
    }
  }
  public void remove(M m) {
    if (type == TARR) {
      arr.remove(m.asInt());
    } else if (type == TMAP) {
      map.remove(m.getRaw());
    } else {
      
    }
  }
  public M getElement(int ix) {
    if (type == TARR) {
      return arr.get(ix);
    } else if (type == TMAP) {
      Object keys[] = map.keySet().toArray();
      Object key = keys[ix];
      M mval = map.get(key);
      if (mval == null) return null;
      M mkey = new M(key);
      MListMap res = new MListMap();
      res.set("key", mkey);
      res.set("val", mval);
      M mres = new M();
      mres.type = Processor.TSET;
      mres.ref = res;
      return mres;
    } else {
      return null;
    }
  }
  
  public String toString() {
    if (type == Processor.TNIL) return "[]";
    else if (type == TMAP) return map.toString();
    else if (type == TARR) return arr.toString();
    else return null;
  }
}
