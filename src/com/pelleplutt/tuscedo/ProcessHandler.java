package com.pelleplutt.tuscedo;

public interface ProcessHandler {
  public void linkToProcess(ProcessGroup p);
  public boolean isLinkedToProcess();
  public void closeStdin();
  public int unlinkFromProcess();
  public void sendToStdIn(String line);
}
