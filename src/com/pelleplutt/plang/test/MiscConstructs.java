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
        "if (dd < 0.48) return 'fail lo dmin/(dmin+dmax)=' + dd;\n" +
        "if (dd > 0.52) return 'fail hi dmin/(dmin+dmax)=' + dd;\n" +
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
        "void = arr[{sum += $0;}];\n" +
        "if (len(arr) == 256 & sum == 0) return 'OK';\n" +
        "return 'fail';\n" +
        "\n";
    assertEquals("OK", Processor.compileAndRun(false, false, sA).str); 
  }
}
