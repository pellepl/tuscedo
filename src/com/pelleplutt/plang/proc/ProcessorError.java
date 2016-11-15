package com.pelleplutt.plang.proc;

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
  }
  public static class ProcessorBreakpointError extends ProcessorError {
  }
}
