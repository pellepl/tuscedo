package com.pelleplutt.tuscedo;

public interface ProcessHandler extends InputCapable {
  public void linkToProcess(ProcessGroup p);
  public boolean isLinkedToProcess();
  public void unlinkFromProcess();
  public void sendToBack();
  public void kill();
  public ProcessGroup getLinkedProcess();
}
