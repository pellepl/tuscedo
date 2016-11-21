package com.pelleplutt.plang.proc;

import com.pelleplutt.plang.CompilerError;
import com.pelleplutt.plang.proc.Processor.M;

public class Range {
  byte type;
  float start;
  float end;
  float step;
  
  public Range(M start, M end) {
    type = checkType(start, null, end);
    this.start = start.asFloat();
    this.end = end.asFloat();
    if (this.start < this.end) {
      this.step = 1.0f;
    } else {
      this.step = -1.0f;
    }
  }

  public Range(M start, M step, M end) {
    this.start = start.asFloat();
    this.step = step.asFloat();
    this.end = end.asFloat();
    if (this.start < this.end && this.step < 0 ||
        this.start > this.end && this.step > 0) {
      throw new CompilerError("range step sign is invalid");
    }
  }
  
  public static byte checkType(M start, M step, M end) {
    boolean f = false;
    if (start.type == Processor.TFLOAT) {
      f = true;
    } else if (start.type != Processor.TINT) {
      throw new CompilerError("ranges cannot contain type " + Processor.TSTRING[start.type]);
    }
    if (end.type == Processor.TFLOAT) {
      f = true;
    } else if (end.type != Processor.TINT) {
      throw new CompilerError("ranges cannot contain type " + Processor.TSTRING[end.type]);
    }
    if (step != null) {
      if (step.type == Processor.TFLOAT) {
        f = true;
      } else if (step.type != Processor.TINT) {
        throw new CompilerError("ranges cannot contain type " + Processor.TSTRING[step.type]);
      }
    }
    return (byte)(f ? Processor.TFLOAT : Processor.TINT);
  }

}
