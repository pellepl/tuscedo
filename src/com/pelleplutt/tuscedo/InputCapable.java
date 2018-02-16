package com.pelleplutt.tuscedo;

public interface InputCapable {
  public void closeStdin();
  public void sendToStdIn(String line);
  public void sendToStdIn(byte b);
  public void sendToStdIn(byte b[]);
  public void sendToStdIn(byte b[], int offs, int len);
}
