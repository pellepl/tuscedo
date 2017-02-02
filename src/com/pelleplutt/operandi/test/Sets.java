package com.pelleplutt.operandi.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.pelleplutt.operandi.AST;
import com.pelleplutt.operandi.CodeGenBack;
import com.pelleplutt.operandi.CodeGenFront;
import com.pelleplutt.operandi.Linker;
import com.pelleplutt.operandi.proc.Processor;

public class Sets {
  @Before
  public void setUp() {
    AST.dbg = false;
    CodeGenFront.dbg = false;
    CodeGenBack.dbg = false;
    Linker.dbg = false;
    Processor.silence = false;
  }

  @Test
  public void arrMod1() {
    String sA;
    sA = 
      "l = [1];\n"+
      "l[0] += 1;\n"+
      "return l;\n" +
      "";
    assertEquals("[2]", Processor.compileAndRun(false, false, sA).asString()); 
  }
  @Test
  public void arrMod2() {
    String sA;
    sA = 
      "l = [1];\n"+
      "l[0]++;\n"+
      "return l;\n" +
      "";
    assertEquals("[2]", Processor.compileAndRun(false, false, sA).asString()); 
  }
  @Test
  public void arrMod3() {
    String sA;
    sA = 
      "l = [1];\n"+
      "++l[0];\n"+
      "return l;\n" +
      "";
    assertEquals("[2]", Processor.compileAndRun(false, false, sA).asString()); 
  }
  @Test
  public void matMod1() {
    String sA;
    sA = 
      "l = [[1,1]];\n"+
      "l[0][1] += 1;\n"+
      "return l;\n" +
      "";
    assertEquals("[[1, 2]]", Processor.compileAndRun(false, false, sA).asString()); 
  }
  @Test
  public void matMod2() {
    String sA;
    sA = 
        "l = [[1,1]];\n"+
        "l[0][1]++;\n"+
        "return l;\n" +
        "";
      assertEquals("[[1, 2]]", Processor.compileAndRun(false, false, sA).asString()); 
  }
  @Test
  public void matMod3() {
    String sA;
    sA = 
        "l = [[1,1]];\n"+
        "++l[0][1];\n"+
        "return l;\n" +
        "";
      assertEquals("[[1, 2]]", Processor.compileAndRun(false, false, sA).asString()); 
  }
}
