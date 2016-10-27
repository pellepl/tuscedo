package com.pelleplutt.plang;

public class CompilerError extends Error {

  public CompilerError(String message) {
    super(message);
  }
  public CompilerError(String message, ASTNode e) {
    super(message);
  }
}
