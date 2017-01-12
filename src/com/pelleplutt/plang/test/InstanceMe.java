package com.pelleplutt.plang.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.pelleplutt.plang.AST;
import com.pelleplutt.plang.CodeGenBack;
import com.pelleplutt.plang.CodeGenFront;
import com.pelleplutt.plang.CompilerError;
import com.pelleplutt.plang.Linker;
import com.pelleplutt.plang.proc.Processor;

public class InstanceMe {
  @Before
  public void setUp() {
    CodeGenFront.dbg = false;
    CodeGenBack.dbg = false;
    Linker.dbg = false;
    Processor.silence = false;
  }

  @Test
  public void testInstMe() {
    String sA;
    sA = 
        "func inst(name) {\n"+
        "  inst = [];\n" +
        "  inst.name = name;\n" +
        "  inst.age = 0;\n" +
        "  inst.set_age = set_age;\n" +
        "  return inst;\n" +
        "}\n" +
        "func set_age(age) {\n"+
        "  me.age = age;\n" +
        "}\n" +
        "eve = inst('eve');\n" +
        "ada = inst('ada');\n" +
        "ada.set_age(20);\n"+
        "ada2 = ada;\n"+
        "eve.set_age(10)\n" +
        "ada2.set_age(30);\n" +
        "return eve.name + ',' + eve.age + ' and ' + ada.name + ',' + ada.age;";
    assertEquals("eve,10 and ada,30", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testInstMeSub() {
    String sA;
    sA = 
        "func inst(name) {\n"+
        "  inst = [];\n" +
        "  inst.name = name;\n" +
        "  inst.age = 0;\n" +
        "  inst.children = [];\n"+
        "  inst.set_age = set_age;\n" +
        "  inst.list_family = list_family;\n" +
        "  inst.get_child = get_child;\n" +
        "  inst.add_child = add_child;\n" +
        "  return inst;\n" +
        "}\n" +
        "func set_age(age) {\n"+
        "  me.age = age;\n" +
        "}\n" +
        "func get_child(ix) {\n"+
        "  return me.children[ix];\n" +
        "}\n" +
        "func add_child(c) {\n"+
        "  me.children += c;\n" +
        "}\n" +
        "func list_family() {\n"+
        "  res = me.name + ':' + me.age + ' ';\n" +
        "  for (c in me.children) {;\n" +
        "    res += c.list_family();\n" +
        "  };\n" +
        "  return res;\n" +
        "}\n" +
        "eve = inst('eve');\n" +
        "eve.add_child(inst('ada'));\n" +
        "eve.add_child(inst('ina'));\n" +
        "eve.set_age(50);\n" +
        "eve.get_child(0).set_age(25);\n" +
        "eve.get_child(1).set_age(22);\n" +
        "eve.get_child(0).add_child(inst('ori'));\n" +
        "eve.get_child(0).get_child(0).set_age(1);\n" +
        "return eve.list_family();";
    assertEquals("eve:50 ada:25 ori:1 ina:22 ", Processor.compileAndRun(false, false, sA).str); 
  }
}
