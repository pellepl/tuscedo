package com.pelleplutt.operandi.proc;

import com.pelleplutt.operandi.proc.Processor.M;

public abstract class ExtCall {
  public void doexe(Processor p, M[] args) {
    M ret = null;
    try {
      ret = exe(p, args);
    } catch (Throwable t) {
      t.printStackTrace();
      throw new ProcessorError(t.getMessage(), t);
    }
    p.push(ret == null ? p.nilM : ret);
    p.retv();
  }
  public abstract M exe(Processor p, M[] args);
}
