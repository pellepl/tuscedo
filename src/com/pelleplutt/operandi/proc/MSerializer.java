package com.pelleplutt.operandi.proc;

import java.util.*;

public class MSerializer {
  public static final char CNIL = '0'; 
  public static final char CINT = 'i'; 
  public static final char CFLOAT = 'f'; 
  public static final char CSTR = 's'; 
  public static final char CSTRS = ';'; 
  public static final char CFUNC = '>'; 
  public static final char CANON = '?'; 
  
  public static final char CSET_ARR = '['; 
  public static final char CSET_ARRS = ']'; 
  public static final char CSET_MAP = '{'; 
  public static final char CSET_MAPS = '}'; 
  public static final char CSET_TUP = '('; 
  public static final char CSET_TUPS = ')';
  public static final char CSET_DELIM = ',';
  public static final char CSET_ASSIGN = ':';
  
  static public String serialize(Processor.M m) {
    Set<MSet> serializedSets = new HashSet<MSet>();
    return serialize(m, serializedSets);
  }

  static String serialize(Processor.M m, Set<MSet> sets) {
    switch (m.type) {
    case Processor.TNIL: return CNIL+"";
    case Processor.TINT: return CINT + Integer.toString(m.i);
    case Processor.TFLOAT: return CFLOAT + Float.toString(m.f);
    case Processor.TSTR: return CSTR + m.str.replace(""+CSTRS, "\\"+CSTRS) + CSTRS;
    case Processor.TFUNC: return CFUNC + String.format("%06x", m.i);
    case Processor.TANON: return CANON + String.format("%06x", m.i);
    case Processor.TSET: {
      StringBuilder sb = new StringBuilder();
      serialize(m.ref, sb, sets);
      return sb.toString();
    }
    }
    return null;
  }
  
  static void serialize(MSet set, StringBuilder sb, Set<MSet> sets) {
    if (sets.contains(set)) {
      sb.append(CNIL);
      //throw new RuntimeException("cannot serialize cyclic graphs");
      return;
    }
    sets.add(set);
    if (set.getType() == MSet.TARR) {
      sb.append(CSET_ARR);
      serializeArr(set, sb, sets);
      sb.append(CSET_ARRS);
    }
    else if (set.getType() == MSet.TMAP) {
      sb.append(CSET_MAP);
      serializeMap(set, sb, sets);
      sb.append(CSET_MAPS);
    }
    else if (set.getType() == MSet.TTUP) {
      sb.append(CSET_TUP);
      serializeTup(set, sb, sets);
      sb.append(CSET_TUPS);
    }
    sets.remove(set);
  }
  
  static void serializeArr(MSet set, StringBuilder sb, Set<MSet> sets) {
    int s = set.size();
    for (int i = 0; i < s; i++) {
      if (i > 0) sb.append(CSET_DELIM);
      sb.append(serialize(set.get(i), sets));
    }
  }
  static void serializeMap(MSet set, StringBuilder sb, Set<MSet> sets) {
    int s = set.size();
    for (int i = 0; i < s; i++) {
      if (i > 0) sb.append(CSET_DELIM);
      sb.append(serialize(set.get(i).ref.get(0), sets));
      sb.append(CSET_ASSIGN);
      sb.append(serialize(set.get(i).ref.get(1), sets));
    }
  }
  static void serializeTup(MSet set, StringBuilder sb, Set<MSet> sets) {
    sb.append(serialize(set.get(0), sets));
    sb.append(CSET_ASSIGN);
    sb.append(serialize(set.get(1), sets));
  }
  
  static class Des {
    String s;
    int ix;
    public Des(String s, int ix) {
      this.s = s; this.ix = ix;
    }
  }
  static public Processor.M deserialize(String s) {
    return deserialize(new Des(s,0));
  }
  
  static int nextPrimitive(Des des) {
    int end = -1;
    String s = des.s;
    int ix = des.ix;
    int len = s.length();
    
    // skip whitespaces
    for (; ix < len; ix++) {
      char c = s.charAt(ix);
      if (c != ' ' && c != '\r' && c != '\n' && c != '\t') {
        break;
      }
    }
    des.ix = ix;
    
    if (s.charAt(ix) == CSTR) {
      // string
      boolean skip = false;
      for (int i = ix+1; i < len; i++) {
        if (s.charAt(i) == '\\' && !skip) {
          skip = true;
        } else if (s.charAt(i) == CSTRS) {
          if (!skip) {
            end = i+1;
            break;
          } else {
            skip = false;
          }
        } else {
          skip = false;
        }
      }
    } else {
      // non-string
      for (int i = ix; i < len; i++) {
        char c = s.charAt(i);
        if (c == CSET_ARR || c == CSET_MAP || c == CSET_TUP || 
            c == CSET_ARRS || c == CSET_MAPS || c == CSET_TUPS || 
            c == CSET_DELIM || c == CSET_ASSIGN) {
          end = i;
          break;
        }
      }
    }
    if (end < 0) end = len;
    return end;
  }

  static Processor.M deserialize(Des des) {
    Processor.M m = new Processor.M();
    int nix = nextPrimitive(des);
    char c = des.s.charAt(des.ix);
    switch (c) {
    case CNIL: 
      m.type = Processor.TNIL;
      des.ix = nix;
      break;
    case CINT: 
      m.type = Processor.TINT;
      m.i = Integer.parseInt(des.s.substring(des.ix+1,nix));
      des.ix = nix;
      break;
    case CFLOAT: 
      m.type = Processor.TFLOAT; 
      m.f = Float.parseFloat(des.s.substring(des.ix+1,nix));
      des.ix = nix;
      break;
    case CSTR: 
      m.type = Processor.TSTR; 
      m.str = des.s.substring(des.ix+1,nix-1).replace("\\"+CSTRS, ""+CSTRS); 
      des.ix = nix;
      break;
    case CFUNC: 
      m.type = Processor.TFUNC; 
      m.i = (int)Long.parseLong(des.s.substring(des.ix+1,nix), 16);
      des.ix = nix;
      break;
    case CANON: 
      m.type = Processor.TANON;
      m.i = (int)Long.parseLong(des.s.substring(des.ix+1,nix), 16);
      des.ix = nix;
      break;
    case CSET_ARR: deserializeArr(des, m); break;
    case CSET_MAP: deserializeMap(des, m); break;
    case CSET_TUP: deserializeTup(des, m); break;
    default: throw new RuntimeException("unexpected deserialization char '" + c + "'");
    }
    return m;
  }
  
  
  static void deserializeArr(Des des, Processor.M m) {
    m.type = Processor.TSET;
    MListMap set = new MListMap();
    set.makeArr();
    des.ix++;
    int len = des.s.length();
    while (des.ix < len) {
      set.add(deserialize(des));
      if (des.s.charAt(des.ix) == CSET_DELIM) des.ix++;
      else if (des.s.charAt(des.ix) == CSET_ARRS) {
        des.ix++;
        break;
      }
      else throw new RuntimeException("unexpected array deserialization char '" + des.s.charAt(des.ix) + "'");

    }
    m.ref = set;
  }
  static void deserializeMap(Des des, Processor.M m) {
    m.type = Processor.TSET;
    MListMap set = new MListMap();
    set.makeMap();
    des.ix++;
    int len = des.s.length();
    while (des.ix < len) {
      Processor.M key = deserialize(des);
      if (des.s.charAt(des.ix) == CSET_ASSIGN) des.ix++;
      else throw new RuntimeException("unexpected map deserialization assign char '" + des.s.charAt(des.ix) + "'");
      Processor.M val = deserialize(des);
      set.put(key.getRaw(), val);
      if (des.s.charAt(des.ix) == CSET_DELIM) des.ix++;
      else if (des.s.charAt(des.ix) == CSET_MAPS) {
        des.ix++;
        break;
      }
      else throw new RuntimeException("unexpected map deserialization char '" + des.s.charAt(des.ix) + "'");
    }
    m.ref = set;
  }
  static void deserializeTup(Des des, Processor.M m) {
    m.type = Processor.TSET;
    MListMap set = new MListMap();
    des.ix++;
    Processor.M key = deserialize(des);
    if (des.s.charAt(des.ix) == CSET_ASSIGN) des.ix++;
    else throw new RuntimeException("unexpected tuple assign deserialization char '" + des.s.charAt(des.ix) + "'");
    Processor.M val = deserialize(des);
    if (des.s.charAt(des.ix) == CSET_TUPS) {
      des.ix++;
    }
    else throw new RuntimeException("unexpected tuple deserialization char '" + des.s.charAt(des.ix) + "'");
    set.makeTup(key,val);
    m.ref = set;
  }
}
