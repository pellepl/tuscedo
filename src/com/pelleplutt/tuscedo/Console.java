package com.pelleplutt.tuscedo;

public interface Console {
  /** Called given meta info */
  void stdout(String s);
  /** Called given process stdout data */
  void stdout(byte b);
  /** Called given process stdout data */
  void stdout(byte b[], int len);
  /** Called given meta info */
  void stderr(String s);
  /** Called given process stderr data */
  void stderr(byte b);
  /** Called given process stderr data */
  void stderr(byte b[], int len);
  /** Called when process ends or goes to background */
  void reset();
  /** Called when application closes */
  void close();
}
