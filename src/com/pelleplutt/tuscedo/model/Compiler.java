package com.pelleplutt.tuscedo.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.pelleplutt.tuscedo.Lexer;

public class Compiler implements Lexer.Emitter{
  Lexer lexer;
  static class Sym {String str; int id; public Sym(String s, int i) {str=s; id=i;}public String toString(){return str;}}
  static int __id = 0;
  final static int _SYM_FINALIZER = -1;
  final static int SYM_COMMENTMULTI = __id++;
  final static int SYM_COMMENTLINE  = __id++;
  final static int SYM_SPACES   = __id++;
  final static int SYM_QUOTE1   = __id++;
  final static int SYM_QUOTE2   = __id++;
  final static int SYM_PARENO   = __id++;
  final static int SYM_PARENC   = __id++;
  final static int SYM_WHILE    = __id++;
  final static int SYM_FOR      = __id++;
  final static int SYM_IF       = __id++;
  final static int SYM_ELSE     = __id++;
  final static int SYM_BRACKETO = __id++;
  final static int SYM_BRACKETC = __id++;
  final static int SYM_EQ       = __id++;
  final static int SYM_EQ2      = __id++;
  final static int SYM_GT       = __id++;
  final static int SYM_LT       = __id++;
  final static int SYM_GE       = __id++;
  final static int SYM_LE       = __id++;
  final static int SYM_NEQ      = __id++;
  final static int SYM_PLUS     = __id++;
  final static int SYM_MINUS    = __id++;
  final static int SYM_DIV      = __id++;
  final static int SYM_MOD      = __id++;
  final static int SYM_MUL      = __id++;
  final static int SYM_PLUS2    = __id++;
  final static int SYM_MINUS2   = __id++;
  final static int SYM_DOT      = __id++;
  final static int SYM_SEMI     = __id++;
  final static int SYM_NUMERIC  = __id++;
  final static int SYM_NUMERIC2 = __id++;
  
  final static Sym[] SYMS = {
      new Sym("/\\**?\\*/", SYM_COMMENTMULTI),
      new Sym("//*?\n", SYM_COMMENTLINE),
      new Sym("*^", SYM_SPACES),
      new Sym("\"*?\"", SYM_QUOTE1),
      new Sym("'*?'", SYM_QUOTE2),
      new Sym("(", SYM_PARENO),
      new Sym(")", SYM_PARENC),
      new Sym("while", SYM_WHILE),
      new Sym("for", SYM_FOR),
      new Sym("if", SYM_IF),
      new Sym("else", SYM_ELSE),
      new Sym("{", SYM_BRACKETO),
      new Sym("}", SYM_BRACKETC),
      new Sym("=", SYM_EQ),
      new Sym("==", SYM_EQ2),
      new Sym(">", SYM_GT),
      new Sym("<", SYM_LT),
      new Sym(">=", SYM_GE),
      new Sym("<=", SYM_LE),
      new Sym("\\!=", SYM_NEQ),
      new Sym("+", SYM_PLUS),
      new Sym("-", SYM_MINUS),
      new Sym("/", SYM_DIV),
      new Sym("\\%", SYM_MOD),
      new Sym("\\*", SYM_MUL),
      new Sym("++", SYM_PLUS2), 
      new Sym("--", SYM_MINUS2),
      new Sym(".", SYM_DOT),
      new Sym(";", SYM_SEMI),
      new Sym("*%", SYM_NUMERIC),
      new Sym("*%.*%", SYM_NUMERIC2),
  };

  public Compiler() {
    lexer = new Lexer(this, 65536);
    for (Sym s : SYMS) {
      lexer.addSymbol(s.str, s.id);
    }
    lexer.compile();
    lexer.printTree();
  }
  
  // impl Parser.Emitter

  static Stack<ASTNode> exprs = new Stack<ASTNode>();
  static Stack<Integer> opers = new Stack<Integer>();
  
  static class ASTNode {
    int op;
    List<ASTNode> operands;
    
    public ASTNode(int op) {
      this.op = op;
    }
    
    public ASTNode(int op, ASTNode ...operands) {
      this.op = op;
      this.operands = new ArrayList<ASTNode>();
      for (ASTNode n : operands) {
        this.operands.add(n);
      }
    }
    
    public String toString() {
      StringBuilder sb = new StringBuilder();
      if (operands != null) {
        // operation
        sb.append(SYMS[op]);
        sb.append('{');
        for (int i = 0; i < operands.size(); i++) {
          sb.append(operands.get(i).toString());
          if (i < operands.size() - 1) sb.append(',');
        }
        sb.append('}');
      } else {
        // numeric
        sb.append(op);
      }
      return sb.toString();
    }
  }
  
  
  @Override
  public void data(byte[] data, int len) {
    //System.out.print(new String(data, 0, len));
//    try {
//      onExpression(Integer.valueOf(new String(data, 0, len).trim()));
//    } catch (Throwable t) {}
  }

  @Override
  public void symbol(byte[] symdata, int len, int sym) {
    //System.out.print("[" + new String(symdata, 0, len) + ":" + idSym.get(sym) + "]");
    if (sym == SYM_NUMERIC || sym == SYM_NUMERIC2) {
      try {
        System.out.println("NUMERIC " + new String(symdata, 0, len));
        onNumber((int)Double.parseDouble((new String(symdata, 0, len).trim())));
      } catch (Throwable t) {t.printStackTrace();}
    } else if (sym == SYM_COMMENTLINE || sym == SYM_COMMENTMULTI) {
      System.out.println(new String(symdata, 0, len));
    } else if (sym == SYM_SPACES) {
      System.out.println("");
    } else {
      onSymbol(sym);
    } 
  }
  
  static String opersString() {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    for (int i = 0; i < opers.size(); i++) {
      sb.append(SYMS[opers.get(i)]);
      if (i < opers.size() - 1) sb.append(", ");
    }
    sb.append(']');
    return sb.toString();
  }
  
  static void onSymbol(int symIx) {
    if (symIx < 0) {
      System.out.println("   hndle symb finalizer");
    } else {
      System.out.println("   hndle symb " + SYMS[symIx]);
    }
    System.out.println("        >> EX:" + exprs);
    System.out.println("        >> OP:" + opersString());
    if (symIx == SYM_PARENO) {
      System.out.println("     opush pareno");
      opers.push(symIx);
    }
    else {
      while (!opers.isEmpty()) {
        boolean popParen = (symIx == SYM_PARENC && opers.peek() != SYM_PARENO);
        boolean popPrio = (opers.peek() >= symIx);
        if (!popParen && !popPrio) break;
        
        if (symIx >= 0) {
          if (popParen) System.out.print("     pare [");
          if (popPrio)  System.out.print("     prio [");
          System.out.print(SYMS[opers.peek()] + 
              " >= " + SYMS[symIx] + "], opop " + SYMS[opers.peek()] + ", ");
        }
        int operator = opers.pop();
        ASTNode e2 = exprs.pop(); // TODO here we should pop number of expressions
        ASTNode e1 = exprs.pop(); //      required by operator
        System.out.println("   epush " + e1 + " " + SYMS[operator] + " " + e2);
        exprs.push(new ASTNode(operator, e1, e2));
      }
      if (symIx == SYM_PARENC) {
        System.out.println("     parc -> opop the oparen " + SYMS[opers.peek()]);
        opers.pop(); // pop off the (
      } else if (symIx != _SYM_FINALIZER) {
        System.out.println("     opush " + SYMS[symIx]);
        opers.push(symIx);
      }
    }
    System.out.println("        << EX:" + exprs);
    System.out.println("        << OP:" + opersString());
  }
  
  static void onNumber(int number) {
    System.out.println("   epush expr " + number);
    exprs.push(new ASTNode(number));
  }
  
  public static void main(String[] args) {
    Compiler c = new Compiler();
    byte tst[] = 
        (
        "//cool test\n"+
        "10 - 2.3 + .3 * ((40 + 50) * 60) / 70\n" +
            "/* now the parsing \n" +
            "   is finished     */"
        ).getBytes();
    for (byte b : tst) {
      c.lexer.feed(b);
    }
    c.lexer.flush();
    onSymbol(_SYM_FINALIZER);
    System.out.println(exprs);
    System.out.println(opersString());
  }
  
}