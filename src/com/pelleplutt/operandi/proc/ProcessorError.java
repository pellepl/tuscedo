package com.pelleplutt.operandi.proc;

import com.pelleplutt.operandi.proc.Processor.M;

public class ProcessorError extends Error {

  public ProcessorError() {
    // TODO Auto-generated constructor stub
  }

  public ProcessorError(String message) {
    super(message);
    // TODO Auto-generated constructor stub
  }

  public ProcessorError(Throwable cause) {
    super(cause);
    // TODO Auto-generated constructor stub
  }

  public ProcessorError(String message, Throwable cause) {
    super(message, cause);
    // TODO Auto-generated constructor stub
  }

  public ProcessorError(String message, Throwable cause,
      boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
    // TODO Auto-generated constructor stub
  }

  public static class ProcessorFinishedError extends ProcessorError {
    M ret;
    public ProcessorFinishedError(M ret) {
      super("execution stopped");
      this.ret = ret;
    }
    
    public M getRet() {
      return ret;
    }
  }
  public static class ProcessorStackError extends ProcessorError {
    public ProcessorStackError() { super("bad stack contents at return"); }
  }
  public static class ProcessorBreakpointError extends ProcessorError {
    public ProcessorBreakpointError() { super("breakpoint hit"); }
  }
}
