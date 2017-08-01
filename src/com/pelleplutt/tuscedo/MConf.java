package com.pelleplutt.tuscedo;

import com.pelleplutt.operandi.Compiler;
import com.pelleplutt.operandi.proc.Processor.M;
import com.pelleplutt.operandi.proc.ProcessorError;
import com.pelleplutt.tuscedo.ui.UIWorkArea;

public class MConf extends MObj {
  public MConf(UIWorkArea wa, Compiler comp) {
    super(wa, comp, "conf");
    Settings settings = Settings.inst();
    String[] keys = settings.getKeys();
    for (String k : keys) {
      Object v = settings.keyValue(k);
      putIntern(k, new M(v == null ? "" : v));
    }
  }

  @Override
  public M get(M m) {
    System.out.println("get setting " + m.asString());
    Object v = Settings.inst().keyValue(m.asString());
    return v == null ? null : new M(v);
  }

  @Override
  public M get(int ix) {
    throw new ProcessorError(type + " object cannot be indexed");
  }

  @Override
  public M getElement(int ix) {
    throw new ProcessorError(type + " object cannot be iterated");
  }

  @Override
  public void put(Object key, M m) {
    Settings.inst().setKeyValue(key.toString(), m.asString());
  }
  
  @Override
  public void set(M mix, M m) {
    Settings.inst().setKeyValue(mix.asString(), m.asString());
  }

  public void init(UIWorkArea wa, com.pelleplutt.operandi.Compiler comp) {
    this.workarea = wa;
  }
}
