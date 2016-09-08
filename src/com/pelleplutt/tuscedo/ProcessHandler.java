package com.pelleplutt.tuscedo;

public interface ProcessHandler {
  public void linkToProcess(ProcessGroup p);
  public boolean isLinkedToProcess();
  public void closeStdin();
  public void unlinkFromProcess();
  public void sendToStdIn(String line);
  public void sendToBack();
  public void kill();
  public ProcessGroup getLinkedProcess();
}
