package com.pelleplutt.operandi.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.pelleplutt.operandi.proc.Processor;

public class BasicEval2 {
  int a = 2; int b = 3;

  @Test
  public void testMath() {
    assertEquals(Processor.compileAndRun("a=2;b=3;return a+b;").i, a+b);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b+a;").i, b+a);
    assertEquals(Processor.compileAndRun("a=2;b=3;return a-b;").i, a-b);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b-a;").i, b-a);
    assertEquals(Processor.compileAndRun("a=2;b=3;return a*b;").i, a*b);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b*a;").i, b*a);
    assertEquals(Processor.compileAndRun("a=2;b=3;return a/b;").i, a/b);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b/a;").i, b/a);
    assertEquals(Processor.compileAndRun("a=2;b=3;return a%b;").i, a%b);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b%a;").i, b%a);
  }
  @Test
  public void testLogical() {
    assertEquals(Processor.compileAndRun("a=2;b=3;return a&b;").i, a&b);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b&a;").i, b&a);
    assertEquals(Processor.compileAndRun("a=2;b=3;return a|b;").i, a|b);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b|a;").i, b|a);
    assertEquals(Processor.compileAndRun("a=2;b=3;return a^b;").i, a^b);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b^a;").i, b^a);
    assertEquals(Processor.compileAndRun("a=2;b=3;return a>>b;").i, a>>b);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b>>a;").i, b>>a);
    assertEquals(Processor.compileAndRun("a=2;b=3;return a<<b;").i, a<<b);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b<<a;").i, b<<a);
    assertEquals(Processor.compileAndRun("a=2;b=3;return ~b;").i, ~b);
    assertEquals(Processor.compileAndRun("a=2;b=3;return ~a;").i, ~a);
  }
  @Test
  public void testConditional() {
    assertEquals(Processor.compileAndRun("a=2;b=3;return a==b;").i, a==b?1:0);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b==a;").i, b==a?1:0);
    assertEquals(Processor.compileAndRun("a=2;b=3;return a!=b;").i, a!=b?1:0);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b!=a;").i, b!=a?1:0);
    assertEquals(Processor.compileAndRun("a=2;b=3;return a<b;").i, a<b?1:0);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b<a;").i, b<a?1:0);
    assertEquals(Processor.compileAndRun("a=2;b=3;return a>b;").i, a>b?1:0);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b>a;").i, b>a?1:0);
    assertEquals(Processor.compileAndRun("a=2;b=3;return a<=b;").i, a<=b?1:0);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b<=a;").i, b<=a?1:0);
    assertEquals(Processor.compileAndRun("a=2;b=3;return a>=b;").i, a>=b?1:0);
    assertEquals(Processor.compileAndRun("a=2;b=3;return b>=a;").i, b>=a?1:0);
    assertEquals(Processor.compileAndRun("a=2;b=3;return !b;").i, 0);
    assertEquals(Processor.compileAndRun("a=2;b=3;return !a;").i, 0);
  }
}
