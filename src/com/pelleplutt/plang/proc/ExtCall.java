package com.pelleplutt.plang.proc;

import com.pelleplutt.plang.proc.Processor.M;

public interface ExtCall {
  M exe(M[] memory, M[] args);
}
