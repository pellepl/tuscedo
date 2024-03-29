package com.pelleplutt.operandi.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.pelleplutt.operandi.AST;
import com.pelleplutt.operandi.CodeGenBack;
import com.pelleplutt.operandi.CodeGenFront;
import com.pelleplutt.operandi.Linker;
import com.pelleplutt.operandi.proc.Processor;

public class Loops {
  @Before
  public void setUp() {
    AST.dbg = false;
    CodeGenFront.dbg = false;
    CodeGenBack.dbg = false;
    Linker.dbg = false;
    Processor.silence = false;
  }

  @Test
  public void testFor1() {
    String sA;
    sA = 
        "a = 0#100;\n" +
        "b = [];\n" +
        "for (x in a) {\n" +
        "  if (x > 50) break;\n" +
        "  if (x & 0xf) continue;\n" +
        "  b += x;\n" +
        "}\n" +
        "return str(b);\n" + 
        "";
    assertEquals("[0, 16, 32, 48]", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testFor2() {
    String sA;
    sA = 
        "a = 0#100;\n" +
        "b = [];\n" +
        "for (i = 0; i < len(a); i++) {\n" +
        "  x = a[i];\n" +
        "  if (x > 50) break;\n" +
        "  if (x & 0xf) continue;\n" +
        "  b += x;\n" +
        "}\n" +
        "return str(b);\n" + 
        "";
    assertEquals("[0, 16, 32, 48]", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testWhileSimple() {
    String sA;
    sA = 
        "res = '';\n" +
        "i = 10;\n" +
        "while (i--) {\n" +
        "  res += i;\n" +
        "}\n" +
        "return res;\n" + 
        "";
    assertEquals("9876543210", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testWhileSimple2() {
    String sA;
    sA = 
        "res = '';\n" +
        "func w(x) { \n" + 
        "  while (x) {\n" +
        "    res += --x;\n" +
        "  }\n" +
        "}\n" +
        "w(10);\n" +
        "return res;\n" + 
        "";
    assertEquals("9876543210", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testWhile() {
    String sA;
    sA = 
        "a = 0#100;\n" +
        "b = [];\n" +
        "i = 0;\n" +
        "while (i < len(a)) {\n" +
        "  x = a[i++];\n" +
        "  if (x > 50) break;\n" +
        "  if (x & 0xf) continue;\n" +
        "  b += x;\n" +
        "}\n" +
        "return str(b);\n" + 
        "";
    assertEquals("[0, 16, 32, 48]", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testForNest() {
    String sA;
    sA = 
        "a = 0#100;\n" +
        "b = [];\n" +
        "for (j in 0#10) {\n" +
        "  if (j >= 6) break;\n" +
        "  b += j;\n" +
        "  if (j > 2) continue;\n" +
        "  for (x in a) {\n" +
        "    if (x > 50) break;\n" +
        "    if (x & 0xf) continue;\n" +
        "    b += x;\n" +
        "  }\n" +
        "}\n" +
        "return str(b);\n" + 
        "";
    assertEquals("[0, 0, 16, 32, 48, 1, 0, 16, 32, 48, 2, 0, 16, 32, 48, 3, 4, 5]", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testForWhile() {
    String sA;
    sA = 
        "a = 0#100;\n" +
        "b = [];\n" +
        "for (j in 0#10) {\n" +
        "  if (j >= 6) break;\n" +
        "  b += j;\n" +
        "  if (j > 2) continue;\n" +
        "  i = 0;\n" +
        "  while (i < len(a)) {\n" +
        "    x = a[i++];\n" +
        "    if (x > 50) break;\n" +
        "    if (x & 0xf) continue;\n" +
        "    b += x;\n" +
        "  }\n" +
        "}\n" +
        "return str(b);\n" + 
        "";
    assertEquals("[0, 0, 16, 32, 48, 1, 0, 16, 32, 48, 2, 0, 16, 32, 48, 3, 4, 5]", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testWhileNest() {
    String sA;
    sA = 
        "a = 0#100;\n" +
        "b = [];\n" +
        "j = 0;\n" +
        "while (j <= 10) {\n" +
        "  if (j >= 6) break;\n" +
        "  b += j;\n" +
        "  j++;\n" +
        "  if (j > 2+1) continue;\n" +
        "  i = 0;\n" +
        "  while (i < len(a)) {\n" +
        "    x = a[i++];\n" +
        "    if (x > 50) break;\n" +
        "    if (x & 0xf) continue;\n" +
        "    b += x;\n" +
        "  }\n" +
        "}\n" +
        "return str(b);\n" + 
        "";
    assertEquals("[0, 0, 16, 32, 48, 1, 0, 16, 32, 48, 2, 0, 16, 32, 48, 3, 4, 5]", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testWhileFor() {
    String sA;
    sA = 
        "a = 0#100;\n" +
        "b = [];\n" +
        "j = 0;\n" +
        "while (j <= 10) {\n" +
        "  if (j >= 6) break;\n" +
        "  b += j;\n" +
        "  j++;\n" +
        "  if (j > 2+1) continue;\n" +
        "  for (x in a) {\n" +
        "    if (x > 50) break;\n" +
        "    if (x & 0xf) continue;\n" +
        "    b += x;\n" +
        "  }\n" +
        "}\n" +
        "return str(b);\n" + 
        "";
    assertEquals("[0, 0, 16, 32, 48, 1, 0, 16, 32, 48, 2, 0, 16, 32, 48, 3, 4, 5]", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testWhileFunc() {
    String sA;
    sA = 
        "a = 10;\n" +
        "func have_more() {\n" +
        "  return a > 0;" +
        "}\n" +
        "func get_next() {\n" +
        "  return a--;" +
        "}\n" +
        "s;\n" +
        "while(have_more()) {\n" +
        "  q = get_next();\n" +
        "  s += q + ' ';\n" +
        "}\n" +
        "return s;\n" + 
        "";
    CodeGenBack.dbg = true;
    assertEquals("10 9 8 7 6 5 4 3 2 1 ", Processor.compileAndRun(false, false, sA).str); 
  }
}
