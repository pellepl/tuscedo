package com.pelleplutt.operandi.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.pelleplutt.operandi.AST;
import com.pelleplutt.operandi.CodeGenBack;
import com.pelleplutt.operandi.CodeGenFront;
import com.pelleplutt.operandi.Linker;
import com.pelleplutt.operandi.proc.Processor;

public class Anonymous {
  @Before
  public void setUp() {
    AST.dbg = false;
    CodeGenFront.dbg = false;
    CodeGenBack.dbg = false;
    Linker.dbg = false;
    Processor.silence = false;
  }

  @Test
  public void testAnon1() {
    String sA;
    sA = 
      "func create_anon_adder(x) {\n"+
      "  f = {\n"+
      "    return $0 + x;\n"+
      "  };"+
      "  return f;\n"+
      "}\n"+
      "adder1 = create_anon_adder(10);\n"+
      "adder2 = create_anon_adder(20);\n"+
      "s = '';\n"+
      "s = adder1(1) + ' ' + adder2(2);\n"+
      "return s;\n" +
      "";
    assertEquals("11 22", Processor.compileAndRun(false, false, sA).asString()); 
  }

  @Test
  public void testAnon2() {
    String sA;
    sA = 
      "func create_anon_adders(offset) {\n"+
      "  fs[];\n"+
      "  for (i in 1#10) {;\n"+
      "    f = {\n"+
      "      return offset + $0 + i;\n"+
      "    };"+
      "    fs += f;\n"+
      "  };"+
      "  return fs;\n"+
      "}\n"+
      "adders = create_anon_adders(100);\n"+
      "s = '';\n"+
      "for (f in adders) {\n"+
      "  s += f(5);"+
      "  s += ' ';"+
      "};"+
      "return s;\n" +
      "";
    assertEquals("106 107 108 109 110 111 112 113 114 115 ", Processor.compileAndRun(false, false, sA).asString()); 
  }
  @Test
  public void testAnon3() {
    String sA;
    sA = 
      "globoffs = 10;\n" +
      "func create_anon_adder_gen(offset) {\n"+
      "  fs = {\n"+
      "    return {\n"+
      "      return offset + $0 + globoffs;\n"+
      "    };\n"+
      "  };\n"+
      "  return fs;\n"+
      "}\n"+
      "adder_gen100 = create_anon_adder_gen(100);\n"+
      "adder_gen200 = create_anon_adder_gen(200);\n"+
      "adder_100 = adder_gen100();\n"+
      "adder_200 = adder_gen200();\n"+
      "s = '';\n"+
      "s += adder_100(1) + ' ' + adder_200(1) + ' ';\n"+
      "globoffs = 20;\n" +
      "s += adder_100(2) + ' ' + adder_200(2) + ' ';\n"+
      "return s;\n" +
      "";
    assertEquals("111 211 122 222 ", Processor.compileAndRun(false, false, sA).asString()); 
  }
}
