package com.pelleplutt.plang.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.pelleplutt.plang.AST;
import com.pelleplutt.plang.CodeGenBack;
import com.pelleplutt.plang.CodeGenFront;
import com.pelleplutt.plang.Linker;
import com.pelleplutt.plang.proc.Processor;

public class MiscConstructs {
  @Before
  public void setUp() {
    AST.dbg = false;
    CodeGenFront.dbg = false;
    CodeGenBack.dbg = false;
    Linker.dbg = false;
    Processor.silence = false;
  }

  @Test
  public void oddConstruct1() {
    String sA;
    sA = 
        "res;\n" +
        "func set_res(x) { res = x; }\n" +
        "set_res(('abcdef'[0#4])[3#1]);\n" +
        "return res;";
    assertEquals("dcb", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void oddConstruct2() {
    String sA;
    sA = 
        "func getarr(l) { l2 = l; l2[2] = 9; return l2; };\n" +
        "modarr = getarr([5,4,3,2,1]);\n" +
        "res = str(modarr);\n" +
        "iniarr = [5,4,3,2,1];\n" +
        "res += str(getarr(iniarr)[0]);\n" +
        "res += str(getarr(iniarr)[2]);\n" +
        "res += str(getarr(iniarr)[4]);\n" +
        "return res;";
    assertEquals("[5, 4, 9, 2, 1]591", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void oddConstruct3() {
    String sA, sB;
    sA = 
        "module A;\n" +
        "fmap=['call':{B.res += 'called A.fmap.call';}];\n" +
        "\n";
    sB = 
        "module B;\n" +
        "res;\n" +
        "func add(x,y) {return x+y;}\n" +
        "A.fmap.call();\n" +
        "A.fmap.call = B.add;\n" +
        "B.res += A.fmap.call(2,3);\n" +
        "return res;\n" +
        "\n";
    assertEquals("called A.fmap.call5", Processor.compileAndRun(false, false, sA, sB).str); 
  }
}
