package com.pelleplutt.plang.proc;

import com.pelleplutt.plang.proc.Processor.M;

public interface ExtCall {
  M exe(Processor p, M[] args);
}
