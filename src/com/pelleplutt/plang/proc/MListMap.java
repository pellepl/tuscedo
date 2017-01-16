package com.pelleplutt.plang.proc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.plang.proc.Processor.M;
public class MListMap implements MSet {
  public int type;
  public List<M> arr;
  public Map<Object,M> map;
  public M[] tup;
  public MListMap() {
    type = Processor.TNIL;
  }
  public void makeArr() {
    type = TARR;
    arr = new ArrayList<M>();
  }
  public void makeTup() {
    type = TTUP;
    tup = new M[2];
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
    } else if (type == TTUP) {
      return 2;
    } else {
      return 0;
    }
  }
  public void add(M m) {
    if (size() == 0) {
      if (m.type == Processor.TSET && ((MSet)m.ref).getType() == TTUP) {
      	makeMap();
      } else {
      	makeArr();
      }
    }
    if (type == TARR) {
    	arr.add(m);
    } else if (type == TMAP) {
      if (m.type != Processor.TSET || ((MSet)m.ref).getType() != TTUP) {
      	throw new ProcessorError("can only add tuples to maps");
      }
      MSet tuple = (MSet)m.ref;
      map.put(tuple.get(0).getRaw(), tuple.get(1));
    }
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
    } else if (type == TTUP) {
      if (mix.type == Processor.TINT) {
        tup[mix.asInt()] = new M().copy(m);
      } else if (mix.type == Processor.TSTR) {
      	if ("key".equals(mix.str)) {
	      	tup[0] = new M().copy(m);
	      } else if ("val".equals(mix.str)) {
	      	tup[1] = new M().copy(m);
	      }
      }
    } else if (type == TMAP) {
      map.put(mix.getRaw(), m);
    } else {
      
    }
  }
  public void put(Object key, M m) {
    if (size() == 0) {
      makeMap();
    }
    if (type == TARR) {
      if (key instanceof Integer) {
        arr.set(((Integer)key).intValue(), m);
      }
    } else if (type == TTUP) {
      if (key instanceof Integer) {
      	tup[((Integer)key).intValue()] = new M().copy(m);
      } else if ("key".equals(key)) {
      	tup[0] = new M().copy(m);
      } else if ("val".equals(key)) {
      	tup[1] = new M().copy(m);
      }
    } else if (type == TMAP) {
      map.put(key, m);
    } else {
      
    }
  }
  public M get(M m) {
    if (type == TARR) {
      int ix = m.asInt();
      if (ix < 0) ix = arr.size() + ix;
      return arr.get(ix);
    } else if (type == TTUP) {
      if (m.type == Processor.TINT) {
        return tup[m.asInt()];
      } else if (m.type == Processor.TSTR) {
      	if ("key".equals(m.str)) {
          return tup[0];
	      } else if ("val".equals(m.str)) {
	        return tup[1];
	      } else {
	      	throw new ProcessorError("can only dereference tuples by 0, 1, 'key', or 'val'");
	      }
      }
    } else if (type == TMAP) {
      return map.get(m.getRaw());
    }
    return null;
  }
  public M get(int ix) {
    if (type == TARR) {
      if (ix < 0) ix = arr.size() + ix;
    	return arr.get(ix);
    } else if (type == TTUP) {
    	return tup[ix];
    } else if (type == TMAP) {
      return getElement(ix);
    } else {
      return null;
    }
  }
  public void remove(M m) {
    if (type == TARR) {
      int ix = m.asInt();
      if (ix < 0) ix = arr.size() + ix;
      arr.remove(ix);
    } else if (type == TMAP) {
      map.remove(m.getRaw());
    } else {
      
    }
  }
  public M getElement(int ix) {
    if (type == TARR || type == TTUP) {
      if (ix < 0) ix = arr.size() + ix;
      return arr.get(ix);
    } else if (type == TMAP) {
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
    } else {
      return null;
    }
  }
  
  public int getType() {
  	return this.type;
  }
  
  protected void stringifyAppend(MListMap m, StringBuilder s, int depth) {
    if (depth <= 3) {
      if (m.type == Processor.TNIL) {
        s.append("{[}]");
      } else if (m.type == TMAP) {
        Object keys[] = m.map.keySet().toArray();
        s.append('{');
        for (int i = 0; i < keys.length; i++) {
          M v = m.map.get(keys[i]);
          s.append(keys[i]);
          s.append(':');
          if (v.type == Processor.TSET && v.ref instanceof MListMap) {
            if (depth >= 3) {
              s.append("...");
              break;
            } else {
              stringifyAppend((MListMap)v.ref, s, depth + 1);
            }
          } else {
            s.append(v.getRaw());
          }
          if (i < keys.length - 1) s.append(", ");
        }
        s.append('}');
      } else if (m.type == TARR) {
        s.append('[');
        for (int i = 0; i < m.arr.size(); i++) {
          M v = m.arr.get(i);
          if (v.type == Processor.TSET && v.ref instanceof MListMap) {
            if (depth >= 3) {
              s.append("...");
              break;
            } else {
              stringifyAppend((MListMap)v.ref, s, depth + 1);
            }
          } else {
            s.append(v.getRaw());
          }
          if (i < m.arr.size()- 1) s.append(", ");
        }
        s.append(']');
      } else if (m.type == TTUP) {
        s.append('(');
        M v = m.tup[1];
        s.append(m.tup[0]);
        s.append(':');
        if (v.type == Processor.TSET && v.ref instanceof MListMap) {
          if (depth >= 3) {
            s.append("...");
          } else {
            stringifyAppend((MListMap)v.ref, s, depth + 1);
          }
        } else {
          s.append(v.getRaw());
        }
        s.append(')');
      }
    }
  }
  
  public String toString() {
    StringBuilder s = new StringBuilder();
    stringifyAppend(this, s, 0);
    return s.toString();
  }
}
