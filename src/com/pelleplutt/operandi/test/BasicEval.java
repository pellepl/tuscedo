package com.pelleplutt.operandi.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.pelleplutt.operandi.proc.Processor;

public class BasicEval {

  @Test
  public void testMath() {
    assertEquals(Processor.compileAndRun("return 2+3;").i, 2+3);
    assertEquals(Processor.compileAndRun("return 3+2;").i, 3+2);
    assertEquals(Processor.compileAndRun("return 2-3;").i, 2-3);
    assertEquals(Processor.compileAndRun("return 3-2;").i, 3-2);
    assertEquals(Processor.compileAndRun("return 2*3;").i, 2*3);
    assertEquals(Processor.compileAndRun("return 3*2;").i, 3*2);
    assertEquals(Processor.compileAndRun("return 2/3;").i, 2/3);
    assertEquals(Processor.compileAndRun("return 3/2;").i, 3/2);
    assertEquals(Processor.compileAndRun("return 2%3;").i, 2%3);
    assertEquals(Processor.compileAndRun("return 3%2;").i, 3%2);
  }
  @Test
  public void testLogical() {
    assertEquals(Processor.compileAndRun("return 2&3;").i, 2&3);
    assertEquals(Processor.compileAndRun("return 3&2;").i, 3&2);
    assertEquals(Processor.compileAndRun("return 2|3;").i, 2|3);
    assertEquals(Processor.compileAndRun("return 3|2;").i, 3|2);
    assertEquals(Processor.compileAndRun("return 2^3;").i, 2^3);
    assertEquals(Processor.compileAndRun("return 3^2;").i, 3^2);
    assertEquals(Processor.compileAndRun("return 2>>3;").i, 2>>3);
    assertEquals(Processor.compileAndRun("return 3>>2;").i, 3>>2);
    assertEquals(Processor.compileAndRun("return 2<<3;").i, 2<<3);
    assertEquals(Processor.compileAndRun("return 3<<2;").i, 3<<2);
    assertEquals(Processor.compileAndRun("return ~3;").i, ~3);
    assertEquals(Processor.compileAndRun("return ~2;").i, ~2);
  }
  @Test
  public void testConditional() {
    int x2 = 2;
    int x3 = 3;
    assertEquals(Processor.compileAndRun("return 2==3;").i, x2==x3?1:0);
    assertEquals(Processor.compileAndRun("return 3==2;").i, x3==x2?1:0);
    assertEquals(Processor.compileAndRun("return 2!=3;").i, x2!=x3?1:0);
    assertEquals(Processor.compileAndRun("return 3!=2;").i, x3!=x2?1:0);
    assertEquals(Processor.compileAndRun("return 2<3;").i, x2<x3?1:0);
    assertEquals(Processor.compileAndRun("return 3<2;").i, x3<x2?1:0);
    assertEquals(Processor.compileAndRun("return 2>3;").i, x2>x3?1:0);
    assertEquals(Processor.compileAndRun("return 3>2;").i, x3>x2?1:0);
    assertEquals(Processor.compileAndRun("return 2<=3;").i, x2<=x3?1:0);
    assertEquals(Processor.compileAndRun("return 3<=2;").i, x3<=x2?1:0);
    assertEquals(Processor.compileAndRun("return 2>=3;").i, x2>=x3?1:0);
    assertEquals(Processor.compileAndRun("return 3>=2;").i, x3>=x2?1:0);
    assertEquals(Processor.compileAndRun("return !3;").i, 0);
    assertEquals(Processor.compileAndRun("return !2;").i, 0);
  }

}
