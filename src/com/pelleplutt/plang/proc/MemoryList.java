package com.pelleplutt.plang.proc;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.pelleplutt.plang.proc.Processor.M;

public class MemoryList implements List<M> {
  final M[] memory;
  final int start, end, sign, len, istart, iend;
  public MemoryList(M[] memory, int start, int end) {
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
  public boolean isEmpty() {
    return len == 0;
  }

  @Override
  public boolean contains(Object o) {
    for (int i = istart; i < iend; i++) {
      if (memory[i].equals(o)) return true;
    }
    return false;
  }

  @Override
  public M get(int index) {
    if (index >= len) return null;
    return memory[start + index * sign];
  }
  
  @Override
  public Iterator<M> iterator() {
    throw new ProcessorError("not implemented");
  }

  @Override
  public Object[] toArray() {
    throw new ProcessorError("not implemented");
  }

  @Override
  public <T> T[] toArray(T[] a) {
    throw new ProcessorError("not implemented");
  }

  @Override
  public boolean add(M e) {
    throw new ProcessorError("cannot modify arg vector");
  }

  @Override
  public boolean remove(Object o) {
    throw new ProcessorError("cannot modify arg vector");
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    throw new ProcessorError("not implemented");
  }

  @Override
  public boolean addAll(Collection<? extends M> c) {
    throw new ProcessorError("cannot modify arg vector");
  }

  @Override
  public boolean addAll(int index, Collection<? extends M> c) {
    throw new ProcessorError("cannot modify arg vector");
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new ProcessorError("cannot modify arg vector");
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new ProcessorError("cannot modify arg vector");
  }

  @Override
  public void clear() {
    throw new ProcessorError("cannot modify arg vector");
  }


  @Override
  public M set(int index, M element) {
    throw new ProcessorError("cannot modify arg vector");
  }

  @Override
  public void add(int index, M element) {
    throw new ProcessorError("cannot modify arg vector");
  }

  @Override
  public M remove(int index) {
    throw new ProcessorError("cannot modify arg vector");
  }

  @Override
  public int indexOf(Object o) {
    throw new ProcessorError("not implemented");
  }

  @Override
  public int lastIndexOf(Object o) {
    throw new ProcessorError("not implemented");
  }

  @Override
  public ListIterator<M> listIterator() {
    throw new ProcessorError("not implemented");
  }

  @Override
  public ListIterator<M> listIterator(int index) {
    throw new ProcessorError("not implemented");
  }

  @Override
  public List<M> subList(int fromIndex, int toIndex) {
    throw new ProcessorError("not implemented");
  }

}
