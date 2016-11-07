package com.pelleplutt.plang;

import java.util.List;

import com.pelleplutt.plang.ASTNode.ASTNodeBlok;

public class Compiler {
  public static void compile(String s) {
    System.out.println("* build tree");
    ASTNodeBlok e = AST.buildTree(s);
    System.out.println(e.operands);
    
    System.out.println("* optimise tree");
    ASTOptimiser.optimise(e);
    System.out.println(e.operands);

    System.out.println("* check grammar");
    Grammar.check(e);

    System.out.println("* structural analysis");
    StructAnalysis.analyse(e);
    
    System.out.println("* intermediate codegen");
    List<Module> mods = CodeGenFront.genIR(e);
    System.out.println(mods);

    System.out.println("* backend codegen");
    CodeGenBack.compile(mods);
  }
  
  public static void main(String[] args) {
    Compiler.compile(
    "module mymod;\n" +
    "a = 1;\n" +
    "b = 2;\n" +
    "c = a;\n" +
    "c = (a + b)*3;\n" +
    "for (i = 0; i < 10; i=i+1) {\n" +
    "  tmp = a;\n" +
    "  a = b*0.5;\n" +
    "  b = tmp*2.5;\n" +
    "  if (b > 666) continue;\n" +
    "  if (b < 333) break;\n" +
    "  c = c + (i*c) + 2*a + 3.5*b;\n" +
    "}" +
    "if (c >= 1000) {\n" +
    "  i = 0;\n" +
    "  while ((i = i + 1) < 5) {" +
    "    b;\n" +
    "    b = c*100; \n" +
    "    print('c is huge (' + c + ')');\n" + 
    "  }\n" +
    "} else if (c < 100) {\n" +
    "  print('c is small (' + c + ')');\n" + 
    "} else {\n" +
    "  print('c is med (' + c + ')');\n" +
    "}\n" +
    "q = {" +
    "  for (tmp = 0; tmp < 5; tmp = tmp + 1) print(c*tmp);" +
    "};" +
    "print('almost done');" +
    "for (x in 0#1#100) print(x);" +
 ""
);
  }
}
