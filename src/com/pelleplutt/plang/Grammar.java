package com.pelleplutt.plang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Grammar {
  static final String GRAMMAR_DEF =
          "numi:   OP_NUMERIC0 | OP_NUMERICI\n" +
          "numd:   OP_NUMERICD\n" +
          "numih:  OP_NUMERICH1 | OP_NUMERICH2\n" +
          "num:    numi | numd | numih\n" +
  "";
  
  Map<String, List<String>> defMap = new HashMap<String, List<String>>();
  
  void build() throws IOException {
    BufferedReader reader = new BufferedReader(new StringReader(GRAMMAR_DEF));
    String def;
    while ((def = reader.readLine()) != null) {
      parse(def);
    }
    compile();
  }
 
  void parse(String def) {
    String key = null;
    int op = -1;
    String subKey = null;
    String[] defsub = def.split("[\\s+\\|]");
    for (String sub : defsub) {
      if (sub.trim().length() == 0) continue;
      if (sub.endsWith(":")) {
        key = sub.substring(0, sub.length()-1);
      } else {
        addDef(key, sub);
      }
    }
  }
 
  void addDef(String key, String def) {
    List<String> defs = defMap.get(key);
    if (defs == null) {
      defs = new ArrayList<String>();
      defMap.put(key, defs);
    }
    defs.add(def);
  }
 
  int getOpNumber(String def) {
    int id = Integer.MIN_VALUE;
    try {
      Field f = AST.class.getDeclaredField(def);
      if (f != null) {
        id = f.getInt(null);
      }
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
      e.printStackTrace();
    }
    if (id == Integer.MIN_VALUE) {
      throw new Error("Grammar, opnumber for " + def + " not found");
    }
    return id;
  }
  
  void compile() {
    Set<String> keys = defMap.keySet();
    for (String key : keys) {
      List<String> defs = defMap.get(key);
      
    }
  }
  
  public static void check(List<ASTNode> exprs) {
    Grammar g = new Grammar();
    try {
      g.build();
    } catch (IOException ioe) {}
    //checkRecurse(exprs);
  }
  
  static void checkRecurse(List<ASTNode> exprs) {
    for (ASTNode a : exprs) {
      if (a.op != AST.OP_COMP) {
        System.out.print("[" + a.op + (a.op >= 0 ? (" " + AST.OPS[a.op].toString()) : "") + "] ");
      }
      if (a.operands != null) {
        checkRecurse(a.operands);
      }
    }
    System.out.println();
  }

}
