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
  }
  
  public static void main(String[] args) {
    Compiler.compile(
        "for (i in 1#100+4#1001) {\n"+
        "  if (a*0 == 9) goto blaj;" +
        "  elseif (a == 9) {\n" +
        "    q = q*2;\n"+
        "    w = 3*(q+w);\n"+
        "    goto blaj2;" +
        "  } else break;" +
        "}\n"+
        "blaj:\n"+
        "for (b=10; a < 10; a++) {\n" +
        "  q = (x+y)*8;\n" + 
        "  print(q);\n" + 
        "}\n"+
        "blaj2:\n"+
        "a = a + 1;\n"+
        "b = a * 10;"+
        
//        "for (a=0;a<9;a++) {}\n" +
//        "for (a in 0#2#100) {}\n" +
//        "print('here be dragons' + '\\n', 10*(12+5), debug);"+
        
//        "1+2*3-(4*(7+8)+5)" +
 ""
);
  }
}
