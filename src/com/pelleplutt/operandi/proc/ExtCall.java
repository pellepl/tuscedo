package com.pelleplutt.operandi.proc;

import com.pelleplutt.operandi.proc.Processor.M;

public interface ExtCall {
  M exe(Processor p, M[] args);
}
