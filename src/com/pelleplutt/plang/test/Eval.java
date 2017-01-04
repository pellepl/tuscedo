package com.pelleplutt.plang.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.pelleplutt.plang.proc.Processor;

public class Eval {

  @Test
  public void test() {
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
  }
}
