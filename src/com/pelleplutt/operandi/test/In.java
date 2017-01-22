package com.pelleplutt.operandi.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.pelleplutt.operandi.AST;
import com.pelleplutt.operandi.CodeGenBack;
import com.pelleplutt.operandi.CodeGenFront;
import com.pelleplutt.operandi.Linker;
import com.pelleplutt.operandi.proc.Processor;

public class In {
  @Before
  public void setUp() {
    AST.dbg = false;
    CodeGenFront.dbg = false;
    CodeGenBack.dbg = false;
    Linker.dbg = false;
    Processor.silence = false;
  }

  @Test
  public void testIn1() {
    String sA;
    sA = 
        "a = 0#10;\n" +
        "res = '';\n" +
        "if (1 in a) res += 'OK'; else res += 'FAIL';\n" +
        "if ((1 in a) & (4 in a)) res += 'OK'; else res += 'FAIL';\n" +
        "if ((11 in a) | (4 in a)) res += 'OK'; else res += 'FAIL';\n" +
        "if (0 in a) res += 'OK'; else res += 'FAIL';\n" +
        "if (-1 in a) res += 'OK'; else res += 'FAIL';\n" +
        "return res;\n" + 
        "";
    assertEquals("OKOKOKOKFAIL", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testIn2() {
    String sA;
    sA = 
        "a = [0,1,2,3,4,5,6,7,8,9];\n" +
        "res = '';\n" +
        "if (1 in a) res += 'OK'; else res += 'FAIL';\n" +
        "if ((1 in a) & (4 in a)) res += 'OK'; else res += 'FAIL';\n" +
        "if ((11 in a) | (4 in a)) res += 'OK'; else res += 'FAIL';\n" +
        "if (0 in a) res += 'OK'; else res += 'FAIL';\n" +
        "if (-1 in a) res += 'OK'; else res += 'FAIL';\n" +
        "return res;\n" + 
        "";
    assertEquals("OKOKOKOKFAIL", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testIn3() {
    String sA;
    sA = 
        "a = [0:'a',1:'b',2:'c',3:'d',4:'e',5:'f',6:'g',7:'h',8:'i',9:'j'];\n" +
        "res = '';\n" +
        "if (1 in a) res += 'OK'; else res += 'FAIL';\n" +
        "if ((1 in a) & (4 in a)) res += 'OK'; else res += 'FAIL';\n" +
        "if ((11 in a) | (4 in a)) res += 'OK'; else res += 'FAIL';\n" +
        "if (0 in a) res += 'OK'; else res += 'FAIL';\n" +
        "if (-1 in a) res += 'OK'; else res += 'FAIL';\n" +
        "return res;\n" + 
        "";
    assertEquals("OKOKOKOKFAIL", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testIn4() {
    String sA;
    sA = 
        "a = '0123456789';\n" +
        "res = '';\n" +
        "if (1 in a) res += 'OK'; else res += 'FAIL';\n" +
        "if ((1 in a) & (4 in a)) res += 'OK'; else res += 'FAIL';\n" +
        "if ((11 in a) | (4 in a)) res += 'OK'; else res += 'FAIL';\n" +
        "if (0 in a) res += 'OK'; else res += 'FAIL';\n" +
        "if (-1 in a) res += 'OK'; else res += 'FAIL';\n" +
        "return res;\n" + 
        "";
    assertEquals("OKOKOKOKFAIL", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testIn5() {
    String sA;
    sA = 
        "a = '0123456789';\n" +
        "res = '';\n" +
        "t1 = (1 in a);\n"+
        "t2 = (1 in a) & (4 in a);\n"+
        "t3 = (11 in a) | (4 in a);\n"+
        "t4 = (0 in a);\n"+
        "t5 = (-1 in a);\n"+
        "if t1 res += 'OK'; else res += 'FAIL';\n" +
        "if t2 res += 'OK'; else res += 'FAIL';\n" +
        "if t3 res += 'OK'; else res += 'FAIL';\n" +
        "if t4 res += 'OK'; else res += 'FAIL';\n" +
        "if t5 res += 'OK'; else res += 'FAIL';\n" +
        "return res;\n" + 
        "";
    assertEquals("OKOKOKOKFAIL", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testIn6() {
    String sA;
    sA = 
        "a = ['name':'a', 'age':12];\n" +
        "b = ['name':'b', 'age':23];\n" +
        "c = ['name':'c', 'age':34];\n" +
        "d = ['name':'d', 'age':45];\n" +
        "persons = [a,b,c,d];\n" +
        "res = '';\n" +
        "if (a in persons) res += 'OK'; else res += 'FAIL';\n" +
        "if (b in persons) res += 'OK'; else res += 'FAIL';\n" +
        "if (c in persons) res += 'OK'; else res += 'FAIL';\n" +
        "if (d in persons) res += 'OK'; else res += 'FAIL';\n" +
        "if ('clown' in persons) res += 'OK'; else res += 'FAIL';\n" +
        "return res;\n" + 
        "";
    assertEquals("OKOKOKOKFAIL", Processor.compileAndRun(false, false, sA).str); 
  }
}
