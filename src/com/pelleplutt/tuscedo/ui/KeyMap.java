package com.pelleplutt.tuscedo.ui;

import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KeyMap {
  public int keyCode;
  public int modifiers;
  static Map<String, KeyMap> map = new HashMap<String, KeyMap>();
  
  public static void set(String action, String keyDef) {
    KeyMap km = fromString(keyDef);
    if (km != null) {
      map.put(action, km);
    } else {
      map.remove(action);
    }
  }
  
  public static Set<String> getActions() {
    return map.keySet();
  }
  
  public static KeyMap getKeyDef(String action) {
    return map.get(action);
  }
  
  public static void load(String path) throws IOException {
    FileInputStream fstream = new FileInputStream(path);
    BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

    String l;
    try {
      while ((l = br.readLine()) != null) {
        l = l.trim();
        if (l.trim().startsWith("#")) continue;
        String[] defs = l.split("(\\s*=\\s*)");
        if (l.length() == 2) {
          set(defs[0].trim(), defs[1].trim());
        }
      }
    } finally {
      br.close();
    }
  }
  
  public static void save(String path, String comment) throws IOException {
    PrintWriter fw = new PrintWriter(new FileWriter(path));
    try {
      fw.println("# " + comment);
      for (String k : map.keySet()) {
        fw.println(k + "=" + map.get(k).toString());
      }
    } finally {
      fw.close();
    }
  }
  
  public String toString() {
    Field[] fields = KeyEvent.class.getFields();
    String name = "";
    if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
      name += "shift + ";
    }
    if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
      name += "ctrl + ";
    }
    if ((modifiers & KeyEvent.ALT_DOWN_MASK) != 0) {
      name += "alt + ";
    }
    if ((modifiers & KeyEvent.ALT_GRAPH_DOWN_MASK) != 0) {
      name += "altgr + ";
    }

    try {
      for (Field f : fields) {
        if (f.getType() == int.class && f.getName().startsWith("VK_")
            && !f.isAccessible() && f.getInt(null) == keyCode) {
          name += f.getName().substring("VK_".length()).toUpperCase();
          break;
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }

    return name;
  }

  public static KeyMap fromString(String s) {
    String[] strings = s.split("(\\s*\\+\\s*)");
    if (strings == null || strings.length == 0 || strings[0] == null) {
      return null;
    }
    KeyMap km = new KeyMap();
    km.keyCode = 0;
    
    for (String def : strings) {
      if (def == null) continue;
      def = def.toLowerCase();
      if (def.equals("shift")) {
        km.modifiers |= KeyEvent.SHIFT_DOWN_MASK;
      }
      else if (def.equals("ctrl")) {
        km.modifiers |= KeyEvent.CTRL_DOWN_MASK;
      }
      else if (def.equals("alt")) {
        km.modifiers |= KeyEvent.ALT_DOWN_MASK;
      }
      else if (def.equals("altgr")) {
        km.modifiers |= KeyEvent.ALT_GRAPH_DOWN_MASK;
      } else {
        if (km.keyCode != 0) {
          return null;
        }
        String fieldName = "VK_" + def.toUpperCase();
        Field[] fields = KeyEvent.class.getFields();
        try {
          for (Field f : fields) {
            if (f.getType() == int.class && f.getName().equals(fieldName)
                && !f.isAccessible()) {
              km.keyCode = f.getInt(null);
              break;
            }
          }
        } catch (Throwable t) {
          throw new Error("Could not parse key define '" + def + "'");
        }
      }
    }
    
    if (km.keyCode == 0) {
      throw new Error("No key defined in '" + s+ "'");
    }
    
    return km;
  }
}