package com.pelleplutt.plang.proc;

import com.pelleplutt.plang.proc.Processor.M;

public class MMemList implements MSet {
  final M[] memory;
  final int start, end, sign, len, istart, iend;
  public MMemList(M[] memory, int start, int end) {
    this.memory = memory;
    this.start = start;
    this.end = end;
    sign = end < start ? -1 : 1;
    len = (end - start) * sign;
    if (sign > 0) {
      istart = start;
      iend = end;
    } else {
      istart = end+1;
      iend = start+1;
    }
    //System.out.println(String.format("memory list @ 0x%06x--0x%06x, size %d", start, end, len));
  }
  
  @Override
  public int size() {
    return len;
  }

  @Override
  public M get(int index) {
    if (index >= len) return null;
    return memory[start + index * sign];
  }
  @Override
  public void add(M e) {
    throw new ProcessorError("cannot modify arg vector");
  }
  @Override
  public void insert(int ix, M e) {
    throw new ProcessorError("cannot modify arg vector");
  }
  @Override
  public void set(M mix, M m) {
    throw new ProcessorError("cannot modify arg vector");
  }
  @Override
  public void set(Object key, M m) {
    throw new ProcessorError("cannot modify arg vector");
  }

  @Override
  public M get(M m) {
    return get(m.asInt());
  }

  @Override
  public void remove(M m) {
    throw new ProcessorError("cannot modify arg vector");
  }

  @Override
  public M getElement(int ix) {
    return get(ix);
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder("[");
    int len = size();
    for (int i = 0; i < len; i++) {
      sb.append(get(i));
      if (i < len-1) sb.append(", ");
    }
    sb.append("]");
    return sb.toString(); 
  }
}