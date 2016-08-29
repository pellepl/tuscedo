package com.pelleplutt.tuscedo;

import java.io.InputStream;
import java.io.OutputStream;

public interface ProcessHandler {
  void linkToProcess(Process p);
  boolean isLinkedToProcess();
  int unlinkFromProcess();
  void closeStdin();
  InputStream stdout();
  InputStream stderr();
  OutputStream stdin();
}
