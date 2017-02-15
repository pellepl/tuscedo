package com.pelleplutt.operandi.proc;

public interface IRQHandler {
  void leftIRQ(int pc);
  void step(int pc);
}
