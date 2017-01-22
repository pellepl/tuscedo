package com.pelleplutt.operandi.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.pelleplutt.operandi.CodeGenBack;
import com.pelleplutt.operandi.CodeGenFront;
import com.pelleplutt.operandi.Linker;
import com.pelleplutt.operandi.proc.Processor;

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
        "return eve.list_family();\n";
    assertEquals("eve:50 ada:25 ori:1 ina:22 ", Processor.compileAndRun(false, false, sA).str); 
  }

  @Test
  public void testInstMeTreeADFCombination() {
    String sA;
    sA = 
        "func inst(name) {\n"+
        "  inst = [];\n" +
        "  inst.name = name;\n" +
        "  inst.set_subs = set_subs;\n"+
        "  inst.get_sub = get_sub;\n"+
        "  return inst;\n" +
        "}\n" +
        "func set_subs(subs) {\n"+
        "  for (sub in subs) {\n" +
        "    if (sub.name == me.name) continue;\n" +
        "    me[sub.name] = sub;\n" +
        "  }\n" +
        "}\n" +
        "func get_sub(name) {\n"+
        "  return me[name];\n"+
        "}\n" +
        "a = inst('a');\n" +
        "b = inst('b');\n" +
        "c = inst('c');\n" +
        "all = [a,b,c];\n" +
        "for (x in all) x.set_subs(all);\n" +
        "res = '';\n";
    
    // S = {a,b,c}
    // make combinations of {x of S} <.|[]|get_sub()> {y of S-x} <.|[]|get_sub()> {z of S-y} <.|[]|get_sub()> name
    String instancenames = "abc";
    String code = "";
    int cycles = 3*1 * 2*3 * 2*3 * 1*3;
    for (int i = 0; i < cycles; i++) {
      int curInst = i / (2*3 * 2*3 * 1*3);
      char resName = '?'; 
      String ass ="tmp = " + instancenames.charAt(curInst);
      int combo = (i - curInst * (2*3 * 2*3 * 1*3)) / 3;
      for (int l = 0; l < 2; l++) {
        int nxtInst = (combo/3) % 2;
        int deref = combo % 3;
        if (nxtInst == curInst) nxtInst++;
        curInst = nxtInst;
        char inst = instancenames.charAt(curInst);
        if (deref == 0) {
          ass += "." + inst;
        } else if (deref == 1) {
          ass += "['" + inst + "']";
        } else {
          ass += ".get_sub('" + inst + "')";
        }
        combo /= 2*3;
        resName = inst;
      }
      int deref = i % 3;
      if (deref == 0) {
        ass += ".name;\n";
      } else if (deref == 1) {
        ass += "['name'];\n";
      } else {
        ass += ".get_sub('name');\n";
      }
      
      code +=  ass + "if (tmp != '" + resName + "') res += 'fail ' + tmp + ' exp " + resName + "\n';\n"; 
    }
    sA += code + 
        "return res;\n"+
        "";
    assertEquals("", Processor.compileAndRun(false, false, sA).str); 
  }
}
