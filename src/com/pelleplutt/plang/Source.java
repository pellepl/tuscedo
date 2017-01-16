package com.pelleplutt.plang;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.pelleplutt.util.AppSystem;

public abstract class Source {
  String csrc;
  public abstract String getName();
  public abstract String getSource();
  List<Integer> lstart;
  public String getCSource() {
    if (csrc == null) {
      csrc = getSource();
      index();
    }
    return csrc;
  }
  
  void index() {
    lstart = new ArrayList<Integer>();
    int offs = 0;
    int nl;
    while ((nl = csrc.indexOf('\n', offs)) >= 0) {
      lstart.add(offs);
      offs = nl+1;
    }
    lstart.add(csrc.length());
  }
  
  Object[] lineinfo = new Object[3];
  /**
   * return[0] = linenbr
   * return[1] = linestring
   * return[2] = lineoffset
   * @param offset
   * @return
   */
  public Object[] getLine(int offset) {
    if (csrc == null) {
      getCSource();
    }
    if (offset < 0 || offset > csrc.length()) return null;
    int line = 0;
    while (line < lstart.size() && offset >= lstart.get(line)) {
      line++;
    }
    if (line >= lstart.size()) return null;
    int olstart = line == 0 ? 0 : lstart.get(line-1);
    int olend = lstart.get(line);
    lineinfo[0] = line;
    lineinfo[1] = csrc.substring(olstart, olend-1);
    lineinfo[2] = olstart; 
    return lineinfo;
  }
  
  public static class SourceString extends Source {
    String name, source;
    public SourceString(String name, String source) {
      this.name = name;
      this.source = source;
    }
    @Override
    public String getName() {
      return name;
    }
    @Override
    public String getSource() {
      return source;
    }
  }
  public static class SourceFile extends Source {
    String name, source;
    public SourceFile(File file, String source) {
      this.name = file.getPath();
      this.source = AppSystem.readFile(file);
    }
    @Override
    public String getName() {
      return name;
    }
    @Override
    public String getSource() {
      return source;
    }
  }
}
