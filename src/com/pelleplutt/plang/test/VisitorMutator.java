package com.pelleplutt.plang.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.pelleplutt.plang.CodeGenBack;
import com.pelleplutt.plang.CodeGenFront;
import com.pelleplutt.plang.Linker;
import com.pelleplutt.plang.proc.Processor;
import com.pelleplutt.plang.proc.ProcessorError;

public class VisitorMutator {

  @Before
  public void setUp() {
    CodeGenFront.dbg = false;
    CodeGenBack.dbg = false;
    Linker.dbg = false;
    Processor.silence = false;
  }
  
  @Test
  public void testList1() {
    String sA;
    sA = 
        "l = [1,2,3,'four','five','six',7,8];\n"+
        "l2 = l[return $0];\n"+
        "if (len(l2) != len(l)) return 0;\n"+
        "for (i in 0#len(l)-1) {\n"+
        "  if (l[i] != l2[i])\n"+
        "    return 0;\n"+
        "}\n"+
        "return 1;\n";
    assertEquals(1, Processor.compileAndRun(sA).i); 
  }
  @Test
  public void testList2() {
    String sA;
    sA = 
        "l = [1,2,3,4,5,6,7,8];\n"+
        "l = l[($0 & 1) == 0];\n" +
        "return str(l);\n";
    assertEquals("[2, 4, 6, 8]", Processor.compileAndRun(sA).str); 
  }
  @Test
  public void testList3() {
    String sA;
    sA = 
        "l = [1,2,3,4,5,6,7,8];\n"+
        "l = l[return $0 * 2];\n" +
        "prod = 1;\n" +
        "l = l[{if ($0 < 10) prod *= $0;}];\n" +
        "return prod;\n";
    assertEquals(2*4*6*8, Processor.compileAndRun(sA).i); 
  }
  @Test
  public void testList4() {
    String sA;
    sA = 
        "l = [3,1,2,4];\n"+
        "l = l[return ['one', 'two', 'three', 'four'][$0-1]];\n" +
        "return str(l);\n";
    assertEquals("[three, one, two, four]", Processor.compileAndRun(sA).str); 
  }
  @Test
  public void testString1() {
    String sA;
    sA = 
        "s = 'pelleplutt';\n"+
        "s = s[{if ($0 != 'l') return $0;}];\n" +
        "return s;\n";
    assertEquals("peeputt", Processor.compileAndRun(sA).str); 
  }
  @Test
  public void testString2() {
    String sA;
    sA = 
        "s = 'pelleplutt';\n"+
        "lower2upper = int('A')-int('a');\n" +
        "s = s[return char(int($0)+lower2upper)];\n" +
        "return s;\n";
    assertEquals("PELLEPLUTT", Processor.compileAndRun(sA).str); 
  }
  @Test
  public void testMap1() {
    String sA;
    sA = 
        "m = ['a':1, 'b':2, 'c':3, 'd':4];\n"+
        "m2 = m[return $0];\n"+
        "if (len(m2) != len(m)) return 0;\n"+
        "for (i in m) {\n"+
        "  if (m2[i.key] != i.val)\n"+
        "    return 0;\n"+
        "}\n"+
        "return 1;\n";
    assertEquals(1, Processor.compileAndRun(sA).i); 
  }
  @Test
  public void testMap2() {
    String sA;
    sA = 
        "m = ['a':1, 'b':2, 'c':3, 'd':4];\n"+
        "m2 = m[{if ($0.val > 2) return $0;}];\n"+
        "res = '';\n"+
        "for (i in m2) res += i.key;\n"+
        "return res;\n";
    String res = Processor.compileAndRun(sA).str;
    assertEquals(res.equals("cd") || res.equals("dc"), true); 
  }
  @Test
  public void testMap3() {
    String sA;
    sA = 
        "m = ['a':1, 'b':2, 'c':3, 'd':4];\n"+
        "m2 = m[{if ($0.key < 'c') $0.val *= 10; return $0;}];\n"+
        "return m2.a+m2.b+m2.c+m2.d;\n";
    assertEquals(10+20+3+4, Processor.compileAndRun(sA).i); 
  }
  @Test(expected=ProcessorError.class)
  public void testInt() {
    Processor.silence = true;
    String sA;
    sA = 
        "i = 0x01234567;\n"+
        "i2 = i[{if ($0 != 0) return $0;}];\n"+
        "return i2;\n";
      Processor.compileAndRun(sA); 
  }
}
