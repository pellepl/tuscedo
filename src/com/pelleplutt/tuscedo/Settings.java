package com.pelleplutt.tuscedo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.pelleplutt.Essential;
import com.pelleplutt.util.Log;

public class Settings {
  private static Settings _inst;
  
  public final static String EX_LIST = "foo.list";
  public final static String EX_INT = "another.int";
  
  public final static String SERIAL_NEWLINE_STRING = "serialnl.string";
  public final static String SCRIPT_PREFIX_STRING = "scriptprefix.string";
  public final static String BASH_PREFIX_STRING = "bashprefix.string";
  public final static String BASH_CHAIN_STRING = "bashchain.string";
  public final static String BASH_OUTPUT_STRING = "bashoutput.string";
  public final static String BASH_APPEND_STRING = "bashappend.string";
  public final static String BASH_ERR_OUTPUT_STRING = "bashoutputerr.string";
  public final static String BASH_ERR_APPEND_STRING = "bashappenderr.string";
  public final static String BASH_INPUT_STRING = "bashinput.string";
  public final static String BASH_PIPE_STRING = "bashpipe.string";
  public final static String BASH_CONNECTION_STREAM_STRING = "bashconn.string";
  public final static String BASH_LAST_RET_STRING = "bashlastret.string";
  public final static String BASH_BACKGROUND_STRING = "bashbackground.string";

  public static final int MAX_LIST_ENTRIES = 9;
  
  Properties props = new Properties();
  File settingsFile = new File(System.getProperty("user.home") + File.separator + 
      Essential.userSettingPath + File.separator + Essential.settingsFile);
  File settingsPath = settingsFile.getParentFile();
  
  public static Settings inst() {
    if (_inst == null) {
      _inst = new Settings();
    }
    return _inst;
  }
  
  void defaultSettings() {
    props.setProperty(EX_LIST+".0", "def");
    props.setProperty(EX_INT, "0");
    
    props.setProperty(SCRIPT_PREFIX_STRING, "@");
    props.setProperty(BASH_PREFIX_STRING, "$");
    props.setProperty(BASH_CHAIN_STRING, "&&");
    props.setProperty(BASH_OUTPUT_STRING, ">");
    props.setProperty(BASH_APPEND_STRING, ">>");
    props.setProperty(BASH_ERR_OUTPUT_STRING, "2>");
    props.setProperty(BASH_ERR_APPEND_STRING, "2>>");
    props.setProperty(BASH_INPUT_STRING, "<");
    props.setProperty(BASH_PIPE_STRING, "|");
    props.setProperty(BASH_CONNECTION_STREAM_STRING, "@CONN");
    props.setProperty(BASH_LAST_RET_STRING, "$?");
    props.setProperty(BASH_BACKGROUND_STRING, "&");
    props.setProperty(SERIAL_NEWLINE_STRING, "\n");
  }
  
  public void saveSettings() {
    try {
      if (!settingsFile.exists()) settingsFile.createNewFile();
      FileWriter fw = new FileWriter(settingsFile); 
      props.store(fw, Essential.name + " v" + Essential.vMaj + "." + Essential.vMin + "." + Essential.vMic);
      fw.close();
    } catch (IOException ioe) {
      Log.printStackTrace(ioe);
    }
  }
  
  void loadSettings() {
    if (!settingsFile.exists()) {
      settingsPath.mkdirs();
      defaultSettings();
      saveSettings();
    } else {
      try {
        props.load(new FileReader(settingsFile));
      } catch (IOException e) {
        Log.printStackTrace(e);
      }
    }
  }
  
  private Settings() {
    loadSettings();
  }
  
  public String string(String s) {
    if (s.endsWith(".list")) s += ".0";
    String ss = props.getProperty(s);
    return ss == null ? "" : props.getProperty(s);
  }
  
  public void setString(String key, String s) {
    Log.println(key+"="+s);
    props.setProperty(key, s);
  }
  
  public int integer(String s) {
    return Integer.parseInt(props.getProperty(s));
  }
  
  public void setInt(String key, int i) {
    Log.println(key+"="+i);
    props.setProperty(key, new Integer(i).toString());
  }
  
  public void listAdd(String key, String s) {
    if (s.equals(props.getProperty(key + ".0"))) return;
    
    for (int i = 0; i <= MAX_LIST_ENTRIES; i++) {
      if (s.equals(props.getProperty(key + "." + Integer.toString(i)))) {
        for (int j = i; j > 0; j--) {
          String preVal = props.getProperty(key + "." + Integer.toString(j-1));
          props.setProperty(key + "." + Integer.toString(j), preVal);
        }
        props.setProperty(key + "." + 0, s);
        return;
      }
    }
    
    int ix = MAX_LIST_ENTRIES;
    String ixVal;
    while (ix > 0) {
      ixVal = props.getProperty(key + "." + Integer.toString(ix-1));
      if (ixVal != null) {
        props.setProperty(key + "." + Integer.toString(ix), ixVal);
      }
      ix--;
    }
    props.setProperty(key + "." + 0, s);
  }
  
  public String[] list(String key) {
    int ix = 0;
    List<String> vals = new ArrayList<String>();
    String ixVal;
    while (ix <= MAX_LIST_ENTRIES && (ixVal = props.getProperty(key + "." + Integer.toString(ix))) != null) {
      vals.add(ixVal);
      ix++;
    }
    
    return vals.size() == 0 ? new String[0] : vals.toArray(new String[vals.size()]);
  }

  public String[] getKeys() {
    Set<Object> keys = props.keySet();
    String[] s = new String[keys.size()];
    int i = 0;
    for (Object o : keys) {
      String tkey = o.toString();
      String type = tkey.substring(tkey.lastIndexOf("."));
      if (type.charAt(0) >= '0' && type.charAt(0) <= '9') {
        continue;
      }
      String key = tkey.substring(0, tkey.lastIndexOf("."));
      s[i++] = key;
    }
    return s;
  }
  
  public Object keyValue(String key) {
    Object o = null;
    Set<Object> keys = props.keySet();
    for (Object k : keys) {
      String tkey = k.toString();
      if (key.equals(tkey.substring(0, tkey.lastIndexOf(".")))) {
        if (tkey.endsWith(".string")) {
          o = (String)props.getProperty(tkey, "");
        } else if (tkey.endsWith(".int")) {
          o = new Integer(Integer.parseInt(props.getProperty(tkey, "0")));
        } else if (tkey.endsWith(".list")) {
          // TODO
          throw new Error("not implemented");
        }
        break;
      }
    }
    return o;
  }

  public void setKeyValue(String key, String val) {
    Set<Object> keys = props.keySet();
    for (Object k : keys) {
      String tkey = k.toString();
      if (key.equals(tkey.substring(0, tkey.lastIndexOf(".")))) {
        if (tkey.endsWith(".string")) {
          setString(tkey, val);
        } else if (tkey.endsWith(".int")) {
          setInt(tkey, Integer.parseInt(val));
        } else if (tkey.endsWith(".list")) {
          // TODO
          throw new Error("not implemented");
        }
        return;
      }
    }
    props.put(key + ".string", val);
  }
}
