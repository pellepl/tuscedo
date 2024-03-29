package com.pelleplutt.operandi.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.pelleplutt.operandi.CodeGenBack;
import com.pelleplutt.operandi.CodeGenFront;
import com.pelleplutt.operandi.CompilerError;
import com.pelleplutt.operandi.Linker;
import com.pelleplutt.operandi.proc.Processor;

public class Scope {
  final String sGlobalA1 = 
      "module a;\n" +
      "globfail = 1;\n";
  final String sGlobalA2 = 
      "module a;\n" +
      "res1 = globfail;\n" +
      "globfail = 2;\n" +
      "res2 = globfail;\n" +
      "return str(res1) + str(res2);\n";

  @Before
  public void setUp() {
    CodeGenFront.dbg = false;
    CodeGenBack.dbg = false;
    Linker.dbg = false;
    Processor.silence = true;
  }

  @Test
  public void testDeclarationGlobal1() {
    assertEquals("nil2", Processor.compileAndRun(sGlobalA2).str); 
  }
  @Test
  public void testDeclarationGlobal2() {
    assertEquals("12", Processor.compileAndRun(sGlobalA1, sGlobalA2).str); 
  }
  @Test
  public void testDeclarationGlobal3() {
    assertEquals("nil2", Processor.compileAndRun(sGlobalA2, sGlobalA1).str); 
  }
  
  @Test(expected=CompilerError.class)
  public void testDeclarationLocal() {
    Processor.silence = true;
    String sA;
    sA = 
        "module a;\n" +
        "{\n"+
        "  locfail = loclater + 3;\n" +
        "  loclater = 2;\n" +
        "  println('testDeclarationLocal.locfail:'+locfail);\n" +
        "}\n";
    Processor.compileAndRun(sA); 
  }
  @Test
  public void testDeclarationGlobalLocal() {
    String sA;
    sA = 
        "module a;\n" +
        "{\n"+
        "  locfail = str(globlater) + str(3);\n" +
        "  return locfail;\n" +
        "}\n" +
        "globlater = 2;\n";
    assertEquals("nil3", Processor.compileAndRun(sA).str); 
  }
  @Test
  public void testShadow() {
    String sA;
    sA = 
        "module a;\n" +
        "aglob = 'Ag';\n" +
        "aret = 'overwritten';\n" +
        "ashadow = 'Asg';\n" +
        "{\n" +
        "  ashadow;\n" +
        "  ashadow = 'Asl';\n" +
        "  ascop = aglob + ashadow;\n" +
        "  aret = ascop;\n" +
        "}\n" +
        "return a.aret + ashadow;\n";
    assertEquals("AgAslAsg", Processor.compileAndRun(sA).str); 
  }
  @Test
  public void testModular() {
    String sA, sB;
    sA = 
        "module a;\n" +
        "aglob = 'Ag';\n" +
        "aret = 'overwritten';\n" +
        "{\n" +
        "  ascop = aglob + b.bglob;\n" +
        "  aret = ascop;\n" +
        "}\n" +
        "return a.aret;\n";
    sB = 
        "module b;\n" +
        "bglob = 'Bg';\n";
    assertEquals("AgBg", Processor.compileAndRun(sB, sA).str); 
  }
  @Test
  public void testModularShadow1() {
    String sA, sB;
    sA = 
        "module a;\n" +
        "aglob = 'Ag';\n" +
        "aret = 'overwritten';\n" +
        "ashadow = 'Asg';\n" +
        "{\n" +
        "  ashadow;\n" +
        "  ashadow = 'Asl';\n" +
        "  ascop = aglob + b.bglob + ashadow;\n" +
        "  aret = ascop;\n" +
        "}\n" +
        "return a.aret + ashadow;\n";
    sB = 
        "module b;\n" +
        "bglob = 'Bg';\n";
    assertEquals("AgBgAslAsg", Processor.compileAndRun(sB, sA).str); 
  }
  @Test
  public void testModularShadow2() {
    String sA, sB;
    sA = 
        "module a;\n" +
        "aglob = 'Ag';\n" +
        "aret = 'overwritten';\n" +
        "ashadow = 'Asg';\n" +
        "{\n" +
        "  ashadow = 'Asl';\n" +
        "  ascop = aglob + b.bglob + ashadow;\n" +
        "  aret = ascop;\n" +
        "}\n" +
        "return a.aret + ashadow;\n";
    sB = 
        "module b;\n" +
        "bglob = 'Bg';\n";
    assertEquals("AgBgAslAsl", Processor.compileAndRun(sB, sA).str); 
  }
  @Test
  public void testModularMap() {
    String sA, sB;
    sA = 
        "module a;\n" +
        "map=['one':1, 'ten':10, 'hundred':100];\n" +
        "bmap = b.map;\n" +
        "map['b'] = bmap;\n" +
        "return map.one + a.map.ten + map.hundred + b.map.one + bmap.ten + map.b.hundred;\n";
    sB = 
        "module b;\n" +
        "map=['one':1000, 'ten':10000, 'hundred':100000];\n";
    assertEquals(111111, Processor.compileAndRun(sB, sA).i); 
  }
  @Test
  public void testModularPreference() {
    String sA, sB;
    sA = 
        "module a;\n" +
        "res1 = b.map.value; // from module 'b', variable 'map', entry 'value'\n" +
        "b = ['map':['value':'froma']];\n" +
        "res2 = b.map.value; // from this module, variable 'b', entry 'map', subentry 'value'\n" +
        "return res1 + res2;\n";
    sB = 
        "module b;\n" +
        "map=['value':'fromb'];\n";
    assertEquals("frombfroma", Processor.compileAndRun(sB, sA).str); 
  }
  @Test
  public void testModularFunc() {
    String sA, sB;
    sA = 
        "module a;\n" +
        "func genericname() {return 1;}\n" +
        "c = ['genericname':{return 100;}];\n" +
        "return genericname() + b.genericname() + c.genericname() + b.d.genericname();\n";
    sB = 
        "module b;\n" +
        "d = ['genericname':{return 1000;}];\n" +
        "func genericname() {return 10;}\n";
    assertEquals(1111, Processor.compileAndRun(sB, sA).i); 
  }
  @Test
  public void testGlobalInner() {
    String sA, sB;
    sA = 
        "module b;\n" +
        "res = str(a.fglob);\n" +
        "a.fn(123);\n" +
        "res += a.fglob;\n" +
        "return res;\n" +
        "\n";
    sB = 
        "module a;\n" +
        "func fn(x) {\n" +
        "  global fglob;\n" +
        "  fglob = x;\n" +
        "}\n";
    assertEquals("nil123", Processor.compileAndRun(sB, sA).str); 
  }
  @Test(expected=CompilerError.class)
  public void testGlobalInnerErr1() {
    String sA;
    sA = 
        "module b;\n" +
        "res = 23;\n" +
        "global qwe;" +
        "return res;\n" +
        "\n";
    Processor.compileAndRun(sA); 
  }
  @Test(expected=CompilerError.class)
  public void testGlobalInnerErr2() {
    String sA;
    sA = 
        "module b;\n" +
        "func fn() {\n" +
        "  res = 23;\n" +
        "  global qwe;" +
        "  return res;\n" +
        "}\n" +
        "\n";
    Processor.compileAndRun(sA); 
  }
  @Test(expected=CompilerError.class)
  public void testGlobalInnerErr3() {
    String sA;
    sA = 
        "module b;\n" +
        "a = 0;\n" +
        " if (a) {\n" +
        "  res = 23;\n" +
        "  global qwe;" +
        "  return res;\n" +
        "}\n" +
        "\n";
    Processor.compileAndRun(sA); 
  }
  @Test(expected=CompilerError.class)
  public void testGlobalInnerErr4() {
    String sA;
    sA = 
        "module b;\n" +
        "q = {\n" +
        "  res = 23;\n" +
        "  global qwe;" +
        "  return res;\n" +
        "}\n" +
        "\n";
    Processor.compileAndRun(sA); 
  }
  @Test(expected=CompilerError.class)
  public void testGlobalInnerErr5() {
    String sA;
    sA = 
        "module b;\n" +
        "q = [1,2,3,4][{\n" +
        "  res = 23;\n" +
        "  global qwe;" +
        "  return res;\n" +
        "}];\n" +
        "\n";
    Processor.compileAndRun(sA); 
  }
  @Test
  public void testGlobalInnerMul() {
    String sA;
    sA = 
        "module b;\n" +
        "[1,2,3,4][{\n" +
        "  global g1;" +
        "  global g2;" +
        "  g1 = $0;\n" +
        "  g2 = $1*2;\n" +
        "}];\n" +
        "return g1 + ',' + g2;\n" +
        "\n";
    assertEquals("4,6", Processor.compileAndRun(sA).str); 
  }
}
