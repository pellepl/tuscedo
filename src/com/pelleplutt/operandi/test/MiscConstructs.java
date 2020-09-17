package com.pelleplutt.operandi.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.pelleplutt.operandi.AST;
import com.pelleplutt.operandi.CodeGenBack;
import com.pelleplutt.operandi.CodeGenFront;
import com.pelleplutt.operandi.Linker;
import com.pelleplutt.operandi.proc.Processor;

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
  public void oddConstruct0() {
    String sA;
    sA = 
        "a[];\n" +
        "a[{println $0;}];\n" +
        "return 'OK';";
    assertEquals("OK", Processor.compileAndRun(false, false, sA).str); 
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
  @Test
  public void testRand() {
    String sA;
    int iters = 100000;
    sA = 
        "histcount = [];\n" +
        "iters = float(" + iters + ");\n" +
        "for (i in 0#iters-1) {\n" +
        "  r = str(rand() & 0xff);\n" +
        "  if (histcount[r] == nil) histcount[r] = 0;\n" +
        "  histcount[r]++;\n" +
        "}\n" +
        "min = iters; max = 0; sum = 0;\n"+
        "histcount[{\n" + 
        "  v = $0.val;\n" +
        "  if (v < min) min = v;\n" +
        "  if (v > max) max = v;\n" +
        "  sum += v;\n" +
        "}];\n" +
        "if (sum != iters) return 'fail sum=' + sum;\n" +
        "avg = sum / len(histcount);\n" +
        "dmin = (avg-min) * 256 / iters;\n" +
        "dmax = (max-avg) * 256 / iters;\n" +
        "if (dmin > 1.0) return 'fail dmin=' + dmin;\n" +
        "if (dmax > 1.0) return 'fail dmax=' + dmax;\n" +
        "dd = dmin/(dmin+dmax);\n" +
        "if (dd < 0.46) return 'fail lo dmin/(dmin+dmax)=' + dd;\n" +
        "if (dd > 0.54) return 'fail hi dmin/(dmin+dmax)=' + dd;\n" +
        "return 'OK';\n" +
        "\n";
    assertEquals("OK", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testArrFill() {
    String sA;
    sA = 
        "arr = [];\n" +
        "for (i in 0#255) arr += 0;\n" +
        "sum = 0;\n" +
        "arr[{sum += $0;}];\n" +
        "if (len(arr) == 256 & sum == 0) return 'OK';\n" +
        "return 'fail';\n" +
        "\n";
    assertEquals("OK", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testMapExt() {
    String sA;
    sA = 
        "func inner() {return 0;}\n" +
        "i = ['ext':println, 'inner':inner];\n" +
        "return 'OK';\n" +
        "\n";
    assertEquals("OK", Processor.compileAndRun(false, false, sA).str); 
  }
  @Test
  public void testRangeMutator() {
    String sA;
    sA = 
        "return (0#4)[{return $0*2;}];\n" +
        "\n";
    assertEquals("[0, 2, 4, 6, 8]", Processor.compileAndRun(false, false, sA).asString()); 
  }
  
  @Test
  public void testIfElse() {
    String sA;
    sA = 
      "func nop() {}\n" +
      "l = [1,2,nil,[5,4,3,2,1,0],5,'end'];\n" +
      "s='';\n" +
      "for (x in l) {\n" +
      "  if (isset(x) & len(x) > 4)\n" +
      "  {\n" +  
      "    s += 'BIG'; \n" +
      "  }\n" +
      "  else if (!isnil(x))  \n" +
      "  {\n" +
      "    s += str(x);\n" +
      "  }\n" +
      "  else \n" +
      "  {\n" +
      "    nop();\n" +
      "  }\n" +
      "}\n" +
      "return s;\n" +
      "";
    assertEquals("12BIG5end", Processor.compileAndRun(false, false, sA).asString()); 
  }

  @Test
  public void testIfElse2() {
    String sA;
    sA = 
      "func nop() {}\n" +
      "l = [1,2,nil,[5,4,3,2,1,0],5,'end'];\n" +
      "s='';\n" +
      "for (x in l) {\n" +
      "  if (isset(x) & len(x) > 4)\n" +
      "    s += 'BIG'; \n" +
      "  else if (!isnil(x))  \n" +
      "    s += str(x);\n" +
      "  else \n" +
      "    nop();\n" +
      "}\n" +
      "return s;\n" +
      "";
    assertEquals("12BIG5end", Processor.compileAndRun(false, false, sA).asString()); 
  }

  @Test
  public void testIfElse3() {
    String sA;
    sA = 
      "l = 1#10;\n"+
      "s = 0.0;\n"+
      "for (a in l) {\n" +
      "  if (a < 5) {\n"+
      "    if (a > 1 & a <= 3) {\n"+
      "      s += a*3;\n"+
      "    } else if (a==4) {\n"+
      "      s /= 2.0;\n"+
      "    } else {\n"+
      "      s -= a;\n"+
      "    }\n"+
      "  } else if (a == 10) {\n"+
      "    s *= 3.14;\n"+
      "  } else {\n"+
      "    if (a in [5,7,9]) {\n"+
      "      if (a == 7) {\n"+
      "        s *= 100; \n"+
      "      } else {\n" +
      "        s += a*12.5;\n"+
      "      }\n"+
      "    } else {\n"+
      "      b = a * 12 - s/2.0;\n"+
      "      s += b / s;\n"+
      "    }\n"+
      "  }\n"+
      "}\n" +
      "return s;\n" +
      "";
    assertEquals(22343, Processor.compileAndRun(false, false, sA).asInt()); 
  }


  @Test
  public void testArgNil() {
    String sA;
    sA = 
      "func f(a,b) {return str(a) + str(b);}\n" +
      "return f() + '/' + f('a') + '/' + f('a','b') + '/' + f('a','b','c');\n" +
      "";
    assertEquals("nilnil/anil/ab/ab", Processor.compileAndRun(false, false, sA).asString()); 
  }


}
