package com.pelleplutt.operandi.proc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.pelleplutt.operandi.proc.Processor.M;
public class MListMap implements MSet {
  public int type;
  public List<M> arr;
  public Map<Object,M> map;
  public M[] tup;
  public MListMap() {
    type = Processor.TNIL;
  }
  public MListMap(Map<String, String> inmap) {
    type = TMAP;
    map = new HashMap<Object, M>();
    Iterator<Entry<String, String>> it = inmap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, String> pair = it.next();
      put(pair.getKey().toString(), new M(pair.getValue().toString()));
    }
  }
  public void makeArr() {
    type = TARR;
    arr = new ArrayList<M>();
  }
  public void makeTup() {
    type = TTUP;
    tup = new M[2];
  }
  public void makeTup(M key, M val) {
    makeTup();
    tup[0] = key;
    tup[1] = val;
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
      makeMap();
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
  
  @Override
  public MSet copyShallow() {
    MListMap ml = new MListMap();
    int len = size();
    if (getType() != TMAP) {
      for (int i = 0; i < len; i++) {
        ml.add(new M().copy(get(i)));
      }
    } else {
      ml.makeMap();
      Object keys[] = map.keySet().toArray();
      for (int i = 0; i < len; i++) {
        ml.map.put(keys[i], new M().copy(map.get(keys[i])));
      }
    }
    ml.type = getType();
    return ml;
  }
  
  static final int MAX_DEPTH = 3;
  static final int MAX_ELEMENTS = 1000;
  static public int stringifyAppend(MSet m, StringBuilder s, int els, int depth) {
    if (depth <= MAX_DEPTH && els < MAX_ELEMENTS) {
      if (m.getType() == Processor.TNIL) {
        s.append("{[}]");
      } else if (m.getType() == TMAP) {
        s.append('{');
        for (int i = 0; els < MAX_ELEMENTS && i < m.size(); i++) {
          els++;
          M tup = m.getElement(i);
          s.append(tup.ref.get(0));
          M v = tup.ref.get(1);
          s.append(':');
          if (v.type == Processor.TSET) {
            if (depth >= MAX_DEPTH) {
              s.append("...");
              break;
            } else {
              els = stringifyAppend(v.ref, s, els, depth + 1);
            }
          } else if (v.getRaw() == null) {
            s.append("(nil)");
          } else {
            s.append(v.getRaw().toString());
          }
          if (i < m.size() - 1) s.append(", ");
        }
        if (els >= MAX_ELEMENTS) s.append("...");
        s.append('}');
      } else if (m.getType() == TARR) {
        s.append('[');
        String lastApp = null; int iters = 0;
        for (int i = 0; els < MAX_ELEMENTS && i < m.size(); i++) {
          M v = m.get(i);
          if (v.type == Processor.TSET) {
            if (depth >= MAX_DEPTH) {
              s.append("...");
              break;
            } else {
              els = stringifyAppend(v.ref, s, els, depth + 1);
            }
          } else {
            String append;
            if (v.getRaw() == null) {
              append = "(nil)";
            } else {
              append = v.getRaw().toString();
            }
            
            if (append.equals(lastApp)) {
              iters++;
              if (iters > 2) {
                continue;
              }
            } else {
              if (iters > 2) {
                s.append("(" + (iters - 2) + " more)");
                if (i < m.size()- 1) s.append(", ");
              }
              iters = 0;
            }
            els++;
            
            s.append(append);
            lastApp = append;
          }
          if (i < m.size()- 1) s.append(", ");
        }
        if (iters > 2) {
          s.append("(" + (iters - 2) + " more)");
        }
        if (els >= MAX_ELEMENTS) s.append("...");
        s.append(']');
      } else if (m.getType() == TTUP) {
        els+=2;
        s.append('(');
        M v = m.get(1);
        s.append(m.get(0));
        s.append(':');
        if (v.type == Processor.TSET) {
          if (depth >= MAX_DEPTH) {
            s.append("...");
          } else {
            stringifyAppend(v.ref, s, els, depth + 1);
          }
        } else {
          s.append(v.getRaw());
        }
        s.append(')');
      }
    }
    
    return els;
  }
  
  public String toString() {
    StringBuilder s = new StringBuilder();
    stringifyAppend(this, s, 0, 0);
    return s.toString();
  }
}
