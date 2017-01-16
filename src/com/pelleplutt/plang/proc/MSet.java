package com.pelleplutt.plang.proc;

import com.pelleplutt.plang.proc.Processor.M;

public interface MSet {
  public static final int TARR = 10;
  public static final int TMAP = 11;
  public static final int TTUP = 12;

  public int size();
  public void add(M m);
  public void insert(int ix, M m);
  public void set(M mix, M m);
  public void put(Object key, M m);
  public M get(M m);
  public M get(int ix);
  public void remove(M m);
  public M getElement(int ix);
  public int getType();
}
