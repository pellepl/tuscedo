package com.pelleplutt.operandi;

public class CompilerError extends Error {
  public final ASTNode e;
  int min = Integer.MAX_VALUE;
  int max = -1;
  public CompilerError(String message) {
    super(message);
    e = null;
  }
  public CompilerError(String message, ASTNode e) {
    super(message);
    this.e = e;
    if (e != null) {
      recurse(e);
    }
  }
  public CompilerError(String message, int min, int max) {
    super(message);
    this.e = null;
    this.min = min;
    this.max = max;
  }
  public int getStringStart() {
    return (max < 0) ? -1 : min;
  }
  public int getStringEnd() {
    return (max < 0) ? -1 : max;
  }
  void recurse(ASTNode e) {
    if (e.stroffset >= 0 && e.stroffset < min) min = e.stroffset;
    if (e.stroffset >= 0 && e.stroffset + e.strlen > max) max = e.stroffset + e.strlen;
    if (e.operands != null) {
      for (ASTNode e2 : e.operands) {
        if (e2.op != AST.OP_BLOK) {
          recurse(e2);
        }
      }
    }
  }
}
