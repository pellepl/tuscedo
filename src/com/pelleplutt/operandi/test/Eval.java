package com.pelleplutt.operandi.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.pelleplutt.operandi.proc.Processor;

public class Eval {

  @Test
  public void testEval() {
    int a = 2; int b = 3; int c = 4; int d = 5; int e = 6;
    assertEquals(Processor.compileAndRun("a=2;b=3;c=4;d=5;e=6;return a+b*c-d/a%e;").i, 
        a+b*c-d/a%e);
    assertEquals(Processor.compileAndRun("a=2;b=3;c=4;d=5;e=6;return a+(b*c-d)/a%e;").i, 
        a+(b*c-d)/a%e);
    assertEquals(Processor.compileAndRun("a=2;b=3;c=4;d=5;e=6;return ((a+b)*c-d)/a%e;").i, 
        ((a+b)*c-d)/a%e);
    assertEquals(Processor.compileAndRun("a=2;b=3;c=4;d=5;e=6;return a+b*c;").i, 
        a+b*c);
    assertEquals(Processor.compileAndRun("a=2;b=3;c=4;d=5;e=6;return a|b&c;").i, 
        a|b&c);
    assertEquals(Processor.compileAndRun("a=2;b=3;c=4;d=5;e=6;return a|b&c^d;").i, 
        a|b&c^d);
    assertEquals(Processor.compileAndRun("a=2;b=3;c=4;d=5;e=6;return a&b|c^d&a<<e;").i, 
        a&b|c^d&a<<e);
    assertEquals(Processor.compileAndRun("a=2;b=3;c=4;d=5;e=6;return (a&b)|(c^d)&(a<<e);").i, 
        (a&b)|(c^d)&(a<<e));
    assertEquals(Processor.compileAndRun("a=2;b=3;c=4;d=5;e=6;return ((a&(b|c)^d)&a)<<e;").i, 
        ((a&(b|c)^d)&a)<<e);
    assertEquals(Processor.compileAndRun("x=1;y=2;a = x>y;return a;").i, 0);
    assertEquals(Processor.compileAndRun("x=1;y=2;a = x<y;return a;").i, 1);

  }
  @Test
  public void testEvalCond() {
    String sA;
    sA = 
        "a = 1; b = 2;\n" +
        "if (a == b) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 0);
    sA = 
        "a = 2; b = 2;\n" +
        "if (a == b) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 1);
    sA = 
        "a = 2; b = 2;\n" +
        "if (a != b) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 0);
    sA = 
        "a = 1; b = 2;\n" +
        "if (a != b) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 1);
    sA = 
        "a = 2; b = 2;\n" +
        "if (a > b) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 0);
    sA = 
        "a = 2; b = 1;\n" +
        "if (a > b) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 1);
    sA = 
        "a = 2; b = 2;\n" +
        "if (a < b) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 0);
    sA = 
        "a = 1; b = 2;\n" +
        "if (a < b) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 1);
    sA = 
        "a = 2; b = 1;\n" +
        "if (a <= b) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 0);
    sA = 
        "a = 1; b = 2;\n" +
        "if (a <= b) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 1);
    sA = 
        "a = 2; b = 2;\n" +
        "if (a <= b) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 1);
    sA = 
        "a = 1; b = 2;\n" +
        "if (a >= b) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 0);
    sA = 
        "a = 2; b = 1;\n" +
        "if (a >= b) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 1);
    sA = 
        "a = 2; b = 2;\n" +
        "if (a >= b) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 1);
  }
  @Test
  public void testEvalAnd() {
    String sA;
    sA = 
        "a = b = 1;\n" +
        "if (a > 0 & b > 0) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 1);
    sA = 
        "a = b = 1;\n" +
        "if (a < 1 & b < 1) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 0);
    sA = 
        "a = b = 1;\n" +
        "if (a < 1 & b > 0) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 0);
    sA = 
        "a = b = 1;\n" +
        "if (a > 0 & b < 1) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 0);
  }
  @Test
  public void testEvalOr() {
    String sA;
    sA = 
        "a = b = 1;\n" +
        "if (a > 0 | b > 0) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 1);
    sA = 
        "a = b = 1;\n" +
        "if (a < 1 | b < 1) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 0);
    sA = 
        "a = b = 1;\n" +
        "if (a < 1 | b > 0) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 1);
    sA = 
        "a = b = 1;\n" +
        "if (a > 0 | b < 1) return 1;\n" +
        "return 0;\n" +
        "";
    assertEquals(Processor.compileAndRun(sA).i, 1);
  }
}
