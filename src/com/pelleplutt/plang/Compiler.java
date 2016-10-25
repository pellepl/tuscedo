package com.pelleplutt.plang;

public class Compiler {
  public static void compile(String s) {
    System.out.println("* build tree");
    AST ast = new AST();
    ast.buildTree(s);
    System.out.println(ast.exprs);
    
    System.out.println("* optimise tree");
    ASTOptimiser.optimise(ast.exprs);
    System.out.println(ast.exprs);

    System.out.println("* check grammar");
    Grammar.check(ast.exprs);

    System.out.println("* codegen");
    //CodeGen.check(ast.exprs);
    CodeGenFront.check(ast.exprs);
  }
  
  public static void main(String[] args) {
    Compiler.compile(
    "a = 1;\n" +
    "b = 2;\n" +
    "c = (a + b)*3;\n" +
    "for (i = 0; i < 10; i=i+1) {\n" +
    "  tmp = a;\n" +
    "  a = b*0.5;\n" +
    "  b = tmp*2.5;\n" +
    "  c = c + (i*c) + 2*a + 3.5*b;\n" +
    "}" +
    "if (c >= 1000) {\n" +
    "  print('c is huge (' + c + ')');\n" + 
    "} else if (c < 100) {\n" +
    "  print('c is small (' + c + ')');\n" + 
    "} else {\n" +
    "  print('c is med (' + c + ')');\n" +
    "}\n" +
    "print('res:', niceify(c));\n" +
    //"print('res:' + niceify(c));\n" + // TODO FIX
 ""
);
  }
}
