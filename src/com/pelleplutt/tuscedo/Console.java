package com.pelleplutt.tuscedo;

public interface Console {
  void stdout(String s);

  void stdout(byte b);

  void stdout(byte b[], int len);

  void stderr(String s);

  void stderr(byte b);

  void stderr(byte b[], int len);

  void reset();

  void close();
}
