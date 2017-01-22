package com.pelleplutt.operandi.proc;

import com.pelleplutt.operandi.proc.Processor.M;

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
  public M get(int ix) {
    if (ix < 0) ix = len + ix;
    if (ix >= len) return null;
    return memory[start + ix * sign];
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
  public void put(Object key, M m) {
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
    if (ix < 0) ix = len + ix;
    return get(ix);
  }
  @Override
  public int getType() {
  	return TARR;
  }
  
  @Override
  public MSet copyShallow() {
    MListMap ml = new MListMap();
    ml.type = TARR;
    for (int i = 0; i < len; i++) {
      ml.add(new M().copy(get(i)));
    }
    return ml;
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
