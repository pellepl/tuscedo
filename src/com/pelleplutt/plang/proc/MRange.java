package com.pelleplutt.plang.proc;

import com.pelleplutt.plang.CompilerError;
import com.pelleplutt.plang.proc.Processor.M;

public class MRange implements MSet {
  byte type;
  float start;
  float end;
  float step;
  Processor.M m = new M();
  
  public MRange(M start, M end) {
    type = checkType(start, null, end);
    this.start = start.asFloat();
    this.end = end.asFloat();
    if (this.start < this.end) {
      this.step = 1.0f;
    } else {
      this.step = -1.0f;
    }
    if (this.start == this.end) {
      throw new CompilerError("zero range");
    }
    this.m.type = type;
  }

  public MRange(M start, M step, M end) {
    type = checkType(start, step, end);
    this.start = start.asFloat();
    this.step = step.asFloat();
    this.end = end.asFloat();
    if (this.start == this.end) {
      throw new CompilerError("zero range");
    }
    if (this.step == 0) {
      throw new CompilerError("range step cannot be zero");
    }
    if (this.start < this.end && this.step < 0 ||
        this.start > this.end && this.step > 0) {
      throw new CompilerError("range step sign is invalid");
    }
    this.m.type = type;
  }
  
  public int size() {
    int l = (int)Math.ceil(Math.abs((end-start) / step));
    if (l * step + start == end) l++;
    return l;
  }
  
  public M get(M m) {
    int ix = m.asInt();
    return get(ix);
  }
  
  public M get(int ix) {
    if (type == Processor.TINT) {
      m.i = (int)(start + ((float)ix*step));
    } else {
      m.f = start + ((float)ix*step);
    }
    return m;
  }

  public M getElement(int ix) {
    return get(ix);
  }
  
  public static byte checkType(M start, M step, M end) {
    boolean f = false;
    if (start.type == Processor.TFLOAT) {
      f = true;
    } else if (start.type != Processor.TINT) {
      throw new CompilerError("ranges cannot contain type " + Processor.TNAME[start.type]);
    }
    if (end.type == Processor.TFLOAT) {
      f = true;
    } else if (end.type != Processor.TINT) {
      throw new CompilerError("ranges cannot contain type " + Processor.TNAME[end.type]);
    }
    if (step != null) {
      if (step.type == Processor.TFLOAT) {
        f = true;
      } else if (step.type != Processor.TINT) {
        throw new CompilerError("ranges cannot contain type " + Processor.TNAME[step.type]);
      }
    }
    return (byte)(f ? Processor.TFLOAT : Processor.TINT);
  }
  
  public void add(M m) {throw new ProcessorError("ranges are immutable");}
  public void insert(int ix, M m) {throw new ProcessorError("ranges are immutable");}
  public void set(M mix, M m) {throw new ProcessorError("ranges are immutable");}
  public void set(Object key, M m) {throw new ProcessorError("ranges are immutable");}
  public void remove(M m) {throw new ProcessorError("ranges are immutable");}

  public String toString() {
    return start + "#" + step + "#" + end;
  }
  
}
