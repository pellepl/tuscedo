package com.pelleplutt.plang;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.ASTNode.ASTNodeFuncCall;
import com.pelleplutt.plang.ASTNode.ASTNodeNumeric;
import com.pelleplutt.plang.ASTNode.ASTNodeOp;
import com.pelleplutt.plang.ASTNode.ASTNodeString;
import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;
import com.pelleplutt.tuscedo.Lexer;

public class AST implements Lexer.Emitter {
  Lexer lexer;
  boolean dbg = false;
  static int __id = 0;
  final static int OP_COMMENTMULTI = __id++;
  final static int OP_COMMENTLINE  = __id++;
  final static int OP_SPACES       = __id++;
  
  final static int OP_QUOTE1       = __id++;
  final static int OP_QUOTE2       = __id++;
  final static int OP_BRACEO       = __id++;
  final static int OP_BRACEC       = __id++;
  final static int OP_BRACKETO     = __id++;
  final static int OP_BRACKETC     = __id++;
  final static int OP_PARENO       = __id++;
  final static int OP_PARENC       = __id++;
  
  final static int OP_MODULE       = __id++;
  final static int OP_LABEL        = __id++;
  final static int OP_FUNCDEF      = __id++;
  final static int OP_WHILE        = __id++;
  final static int OP_IN           = __id++;
  final static int OP_FOR          = __id++;
  final static int OP_ELSE         = __id++;
  final static int OP_IF           = __id++;
  final static int OP_BREAK        = __id++;
  final static int OP_CONTINUE     = __id++;
  final static int OP_RETURN       = __id++;
  final static int OP_GOTO         = __id++;
  
  final static int OP_EQ           = __id++;
  final static int OP_EQ2          = __id++;
  final static int OP_GT           = __id++;
  final static int OP_LT           = __id++;
  final static int OP_GE           = __id++;
  final static int OP_LE           = __id++;
  final static int OP_NEQ          = __id++;
  final static int OP_HASH         = __id++;
  final static int OP_AND          = __id++;
  final static int OP_ANDEQ        = __id++;
  final static int OP_OR           = __id++;
  final static int OP_OREQ         = __id++;
  final static int OP_XOR          = __id++;
  final static int OP_XOREQ        = __id++;
  final static int OP_SHLEFT       = __id++;
  final static int OP_SHLEFTEQ     = __id++;
  final static int OP_SHRIGHT      = __id++;
  final static int OP_SHRIGHTEQ    = __id++;
  final static int OP_NOTEQ        = __id++;
  final static int OP_PLUS         = __id++;
  final static int OP_PLUSEQ       = __id++;
  final static int OP_MINUS        = __id++;
  final static int OP_MINUSEQ      = __id++;
  final static int OP_DIV          = __id++;
  final static int OP_DIVEQ        = __id++;
  final static int OP_MOD          = __id++;
  final static int OP_MODEQ        = __id++;
  final static int OP_MUL          = __id++;
  final static int OP_MULEQ        = __id++;
  final static int OP_PLUS2        = __id++;
  final static int OP_MINUS2       = __id++;
  final static int OP_DOT          = __id++;
  final static int OP_NOT          = __id++;
  final static int OP_GLOBAL       = __id++;
  
  final static int OP_SEMI         = __id++;
  final static int OP_COMMA        = __id++;
//  final static int OP_NUMERIC0     = __id++;
  final static int OP_NUMERICI     = __id++;
  final static int OP_NUMERICD     = __id++;
  final static int OP_NUMERICH1    = __id++;
  final static int OP_NUMERICH2    = __id++;
  final static int OP_NUMERICB1    = __id++;
  final static int OP_NUMERICB2    = __id++;
  final static int OP_NIL          = __id++;
  final static int OP_SYMBOL       = __id++;
  
  // non lexeme tokens
  final static int _OP_FINAL       = __id++;
  final static int OP_ARRAY        = __id++;
  final static int OP_MINUS_UNARY  = __id++;
  final static int OP_PLUS_UNARY   = __id++;
  final static int OP_FINALIZER    = -1;
  final static int OP_BLOK         = -2;
  final static int OP_CALL         = -3;
  final static int OP_RANGE        = -4;
  
  public final static Op[] OPS = {
      new Op("/\\**?\\*/", OP_COMMENTMULTI),
      new Op("//*?\n", OP_COMMENTLINE),
      new Op("*_", OP_SPACES),
      
      new Op("\"*?\"", OP_QUOTE1),
      new Op("'*?'", OP_QUOTE2),
      new Op("{", OP_BRACEO),
      new Op("}", OP_BRACEC),
      new Op("[", OP_BRACKETO),
      new Op("]", OP_BRACKETC),
      new Op("(", OP_PARENO),
      new Op(")", OP_PARENC),
      
      new Op("module", OP_MODULE, 1),
      new Op(":", OP_LABEL, 1),
      new Op("func", OP_FUNCDEF, 1),
      new Op("while", OP_WHILE, 2),
      new Op("in", OP_IN, 1),
      new Op("for", OP_FOR, 4),
      new Op("else", OP_ELSE, 1),
      new Op("if", OP_IF, 2), //, Op.LEFT_ASSOCIATIVITY),
      new Op("break", OP_BREAK),
      new Op("continue", OP_CONTINUE),
      new Op("return", OP_RETURN, 1),
      new Op("goto", OP_GOTO, 1),
      
      new Op("=", OP_EQ, 2, Op.LEFT_ASSOCIATIVITY),
      new Op("==", OP_EQ2, 2),
      new Op(">", OP_GT, 2),
      new Op("<", OP_LT, 2),
      new Op(">=", OP_GE, 2),
      new Op("<=", OP_LE, 2),
      new Op("\\!=", OP_NEQ, 2),
      new Op("#", OP_HASH, 2),
      new Op("&", OP_AND, 2),
      new Op("&=", OP_ANDEQ, 2),
      new Op("|", OP_OR, 2),
      new Op("|=", OP_OREQ, 2),
      new Op("\\^", OP_XOR, 2),
      new Op("\\^=", OP_XOREQ, 2),
      new Op("<<", OP_SHLEFT, 2),
      new Op("<<=", OP_SHLEFTEQ, 2),
      new Op(">>", OP_SHRIGHT, 2),
      new Op(">>=", OP_SHRIGHTEQ, 2),
      new Op("~=", OP_NOTEQ, 2),
      new Op("+", OP_PLUS, 2),
      new Op("+=", OP_PLUSEQ, 2),
      new Op("-", OP_MINUS, 2),
      new Op("-=", OP_MINUSEQ, 2),
      new Op("/", OP_DIV, 2),
      new Op("/=", OP_DIVEQ, 2),
      new Op("\\%", OP_MOD, 2),
      new Op("\\%=", OP_MODEQ, 2),
      new Op("\\*", OP_MUL, 2),
      new Op("\\*=", OP_MULEQ, 2),
      new Op("++", OP_PLUS2, 1), 
      new Op("--", OP_MINUS2, 1),
      new Op(".", OP_DOT, 2),
      new Op("~", OP_NOT, 1),
      new Op("global", OP_GLOBAL, 1),
      
      new Op(";", OP_SEMI),
      new Op(",", OP_COMMA),
//      new Op("ZeRo", OP_NUMERIC0),
      new Op("*%", OP_NUMERICI),
      new Op("*%.*%", OP_NUMERICD),
      new Op("0x*^1", OP_NUMERICH1),
      new Op("0X*^1", OP_NUMERICH2),
      new Op("0b*^2", OP_NUMERICB1),
      new Op("0B*^2", OP_NUMERICB2),
      new Op("nil", OP_NIL),
      new Op("*^0", OP_SYMBOL),
      
      new Op("<@>", _OP_FINAL),
      
      new Op("arr", OP_ARRAY, 2),
      new Op("U-", OP_MINUS_UNARY, 1),
      new Op("U+", OP_PLUS_UNARY, 1),
  };

  Stack<ASTNode> exprs = new Stack<ASTNode>();
  Stack<Op> opers = new Stack<Op>();
  Stack<Integer> blcks = new Stack<Integer>();
  int callid;
  int prevTokix = -1;
  int prevPrevTokix = -1;
  
  public AST() {
    lexer = new Lexer(this, 65536);
    for (int i = 0; i < _OP_FINAL; i++) {
      if (i >= OP_FUNCDEF && i <= OP_GOTO ||
          i == OP_GLOBAL) {
        lexer.addSymbolCompound(OPS[i].str, OPS[i].id);
      } else {
        lexer.addSymbol(OPS[i].str, OPS[i].id);
      }
    }
    lexer.compile();
    lexer.defineUserSet("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_", 0);
    lexer.defineUserSet("abcdefABCDEF0123456789", 1);
    lexer.defineUserSet("01", 2);
    lexer.defineCompoundChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");
  }
  
  static public ASTNodeBlok buildTree(String s) {
    AST ast = new AST();
    ast.callid = 0;
    byte tst[] = s.getBytes();
    for (byte b : tst) {
      ast.lexer.feed(b);
    }
    ast.lexer.flush();
    ast.onOperator(OP_FINALIZER);
    ASTNodeBlok e = new ASTNodeBlok();
    e.operands = ast.exprs;
    return e;
  }
  
  int getCallId() {
    return callid++;
  }
  
  
  
  // impl Parser.Emitter
  
  @Override
  public void data(byte[] data, int len) {
    if (dbg) System.out.println("GARBAGE:<" + new String(data, 0, len) + ">");
  }

  @Override
  public void symbol(byte[] symdata, int len, int tokix) {
    if (tokix != OP_SPACES) {
      if (dbg) System.out.println("SYMBOL: <" + new String(symdata, 0, len) + "> " + tokix);
      if (dbg) System.out.println("        >> EX:" + exprs);
      if (dbg) System.out.println("        >> OP:" + opersString());
    }
    
    if (tokix == OP_COMMENTLINE || tokix == OP_COMMENTMULTI) {
      if (dbg) System.out.println(new String(symdata, 0, len));
    } else if (tokix == OP_SPACES) {
      //if (dbg) System.out.println("");
      return;
    } 
    
    else if (tokix == OP_NUMERICD) {
      try {
        onNumber(Double.parseDouble(new String(symdata, 0, len)), true);
      } catch (Throwable t) {t.printStackTrace();}
    } else if (tokix == OP_NUMERICI) {
      try {
        onNumber(Integer.parseInt(new String(symdata, 0, len)), false);
      } catch (Throwable t) {t.printStackTrace();}
    } else if (tokix == OP_NUMERICH1 || tokix== OP_NUMERICH2) {
      try {
        onNumber(Integer.parseInt(new String(symdata, 2, len-2), 16), false);
      } catch (Throwable t) {t.printStackTrace();}
    } else if (tokix == OP_NUMERICB1 || tokix== OP_NUMERICB2) {
      try {
        onNumber(Integer.parseInt(new String(symdata, 2, len-2), 2), false);
      } catch (Throwable t) {t.printStackTrace();}
    } 
    
    else if (tokix == OP_SYMBOL) {
      onSymbol(new String(symdata, 0, len));
    }
    
    else if (tokix == OP_QUOTE1 || tokix == OP_QUOTE2) {
      onString(new String(symdata, 1, len-2));
    }
    
    else if (tokix == OP_LABEL) {
      onLabel(tokix);
    }
    
    else if (tokix == OP_PARENO) {
      onParenthesisOpen(tokix);
    }
    else if (tokix == OP_PARENC) {
      onParenthesisClose(tokix);
    }

    else if (tokix == OP_BRACKETO) {
      onBracketOpen(tokix);
    }
    else if (tokix == OP_BRACKETC) {
      onBracketClose(tokix);
    }

    else if (tokix == OP_BRACEO) {
      onBraceOpen(tokix);
    }
    else if (tokix == OP_BRACEC) {
      onBraceClose(tokix);
    }

    else if (tokix == OP_COMMA) {
      onComma(tokix);
    }
    else if (tokix == OP_SEMI) {
      onSemi(tokix);
    }

    else if (tokix == OP_MINUS) {
      if (prevTokix == OP_SYMBOL || prevTokix == OP_QUOTE1 || prevTokix == OP_QUOTE2 ||  
          prevTokix == OP_NUMERICD || prevTokix == OP_NUMERICH1 ||
          prevTokix == OP_NUMERICH2 || prevTokix == OP_NUMERICI ||
          prevTokix == OP_NUMERICB1 || prevTokix == OP_NUMERICB2 ||
          prevTokix == OP_PARENC || prevTokix == OP_BRACKETC) {
        // binary minus
        onOperator(tokix);
      } else {
        // unary minus
        onOperator(OP_MINUS_UNARY);
      }
    } else if (tokix == OP_PLUS) {
      if (prevTokix == OP_SYMBOL || prevTokix == OP_QUOTE1 || prevTokix == OP_QUOTE2 ||  
          prevTokix == OP_NUMERICD || prevTokix == OP_NUMERICH1 ||
          prevTokix == OP_NUMERICH2 || prevTokix == OP_NUMERICI ||
          prevTokix == OP_NUMERICB1 || prevTokix == OP_NUMERICB2 ||
          prevTokix == OP_PARENC || prevTokix == OP_BRACKETC) {
        // binary plus
        onOperator(tokix);
      } else {
        // unary plus
        onOperator(OP_PLUS_UNARY);
      }
    }

    else {
      onOperator(tokix);
    }
    prevPrevTokix = prevTokix;
    prevTokix = tokix;

    if (dbg) System.out.println("        << EX:" + exprs);
    if (dbg) System.out.println("        << OP:" + opersString());

  }
  
  
  
  void onNumber(double number, boolean f) {
    if (dbg) System.out.println("   epush num " + number);
    exprs.push(new ASTNodeNumeric(number, f));
  }
  
  void onString(String str) {
    if (dbg) System.out.println("   epush str " + str);
    exprs.push(new ASTNodeString(str));
  }
  
  void onSymbol(String symbol) {
    if (dbg) System.out.println("   epush sym " + symbol);
    exprs.push(new ASTNodeSymbol(symbol));
  }
  
  void onLabel(int tokix) {
    // collapse all before label
    ASTNode a = exprs.pop();
    collapseStack(OP_FINALIZER, OP_BRACEO);
    // collapse label
    exprs.push(a);
    onOperator(tokix);
    collapseStack(OP_FINALIZER, OP_BRACEO);
  }
  
  void onParenthesisOpen(int tokix) {
    if (prevTokix == OP_SYMBOL) {
      if (prevPrevTokix == OP_FUNCDEF) {
        // function definition
        //TODO
      } else {
        // function call
        ASTNodeSymbol funcName = (ASTNodeSymbol)exprs.pop();
//        if (dbg) System.out.println("      collapse all keywords (until :)");
//        collapseStack(OP_LABEL); // collapse all keywords, thus the label prio
        int callid = getCallId();
        ASTNodeFuncCall callnode = new ASTNodeFuncCall(funcName.symbol, callid);
        OpCall callop = new OpCall(funcName.symbol, callid);
        opers.push(callop);
        if (dbg) System.out.println("      opush pareno " + opString(tokix) + ", funccall " + callnode);
        exprs.push(callnode);
      }
    } else {
      // simple parenthesis
      if (dbg) System.out.println("      opush pareno " + opString(tokix));
      opers.push(OPS[tokix]);
    }
  }
  
  void onParenthesisClose(int tokix) {
    if (!opers.isEmpty() && opers.peek().id == OP_PARENO) {
      // empty paren (), push empty block
      exprs.push(new ASTNodeBlok());
    }
    int endedAtTokix = collapseStack(tokix, OP_PARENO, OP_CALL);
    if (endedAtTokix == OP_PARENO) {
      if (dbg) System.out.println("      opop  " + opers.peek());
      opers.pop(); // pop off the (
    } else if (endedAtTokix == OP_CALL) {
      OpCall call = (OpCall)opers.pop();
      // collect arguments to func call
      List<ASTNode> args = new ArrayList<ASTNode>();
      while (!exprs.isEmpty()) {
        ASTNode a = exprs.pop();
        if (a instanceof ASTNodeFuncCall && ((ASTNodeFuncCall)a).callid == call.callid) {
          ((ASTNodeFuncCall)a).setArguments(args);
          exprs.push(a);
          break;
        } else {
          args.add(0, a);
        }
      }
    } else {
      throw new CompilerError("missing left parenthesis");
    }
  }
  
  void onBracketOpen(int tokix) {
    if (dbg) System.out.println("      opush bracketo " + opString(tokix));
    opers.push(OPS[tokix]);
  }
  
  void onBracketClose(int tokix) {
    int endedAtTokix = collapseStack(OP_FINALIZER, OP_BRACKETO);
    if (endedAtTokix == OP_BRACKETO) {
      if (dbg) System.out.println("      opop  " + opers.peek());
      opers.pop(); // pop off the [
      ASTNode e2 = exprs.pop();
      ASTNode e1 = exprs.pop();
      exprs.push(new ASTNodeOp(OP_ARRAY, e1, e2));
    } else {
      throw new CompilerError("missing left bracket");
    }
  }
  
  void onBraceOpen(int tokix) {
    if (dbg) System.out.println("      epush " + opString(tokix));
    opers.push(OPS[tokix]);
    blcks.push(exprs.size());
  }
  
  void onBraceClose(int tokix) {
    // wrap all expressions within starting brace and this brace
    if (blcks.isEmpty()) {
      throw new Error("'" + OPS[OP_BRACEC] + "' missing '" + OPS[OP_BRACEO] + "'");
    }
    int blckSize = blcks.pop();
    List<ASTNode> arguments = new ArrayList<ASTNode>();
    while (exprs.size() > blckSize) {
      arguments.add(0, exprs.pop());
    }
    ASTNode result = new ASTNodeBlok(arguments.toArray(new ASTNode[arguments.size()]));
    if (dbg) System.out.println("      epush " + result);
    exprs.push(result);
    if (collapseStack(OP_FINALIZER, OP_BRACEO) != OP_BRACEO) {
      throw new CompilerError("missing left brace");
    };
    if (dbg) System.out.println("      opop  " + opers.peek());
    opers.pop(); // pop off the {
    if (!opers.isEmpty() && (
        opers.peek().id == OP_FOR || opers.peek().id == OP_WHILE || opers.peek().id == OP_IF ||
        opers.peek().id == OP_ELSE)) {
      collapseStack(opers.peek().id);
    }
  }
  
  void onComma(int tokix) {
    int etokix = collapseStack(OP_FINALIZER, OP_PARENO, OP_CALL, OP_BRACEO);
    if (etokix != OP_CALL && etokix != OP_BRACEO && etokix != OP_PARENO) {
      throw new CompilerError("missing delimiter (parenthesis or brace)");
    }
  }
  
  void onSemi(int tokix) {
    // check if empty semi, if so push empty ASTNodeBlok on ex stack
    if (prevTokix == OP_PARENO || prevTokix == OP_BRACEO || prevTokix == OP_SEMI) {
      exprs.push(new ASTNodeBlok());
    } else {
      collapseStack(tokix, OP_PARENO, OP_CALL, OP_BRACEO);
    }
  }
  
  void onStatemendEnd() {
    onOperator(OP_FINALIZER);
  }
  
  void onOperator(int tokix) {
    if (tokix == OP_FINALIZER) {
      if (dbg) System.out.println("   hndle tok finalizer");
    } else {
      if (dbg) System.out.println("   hndle tok '" + OPS[tokix] + 
          "' ass:" + (OPS[tokix].associativity == Op.LEFT_ASSOCIATIVITY ? "L" : "R"));
    }
    
    do {
      if (tokix != OP_FINALIZER) {
        collapseStack(tokix);
        if (dbg) System.out.println("      epush " + opString(tokix));
        opers.push(OPS[tokix]);
        break;
      } 
      
      if (tokix == OP_FINALIZER) {
        if (dbg) System.out.println("      collapse finalize");
        collapseStack(OP_FINALIZER);
        break;
      }
    } while (false);
    
  }
  
  int collapseStack(int tokix, int... untilTokixs) {
    boolean until = untilTokixs != null && untilTokixs.length > 0; 
    if (until) {
      if (dbg) {
        System.out.print("      collapse until ");
        StringBuilder sb = new StringBuilder();
        for (int utokix : untilTokixs) {
          sb.append(opString(utokix) + " ");
        }
        System.out.println(sb);
      }
    }
    while (!opers.isEmpty()) {
      int topTokix = opers.isEmpty() ? OP_FINALIZER : opers.peek().id;
      boolean ass = opers.peek().associativity;
      if (until) {
        for (int untilTokid : untilTokixs) {
          if (topTokix == untilTokid) {
            if (dbg) System.out.println("      collapse end, found " + opString(topTokix));
            return topTokix;
          }
        }
      } else {
        if (topTokix < tokix || 
            ass == Op.LEFT_ASSOCIATIVITY && topTokix == tokix) {
          return topTokix;
        } else {
          if (dbg) System.out.print("      " + opString(topTokix) + " precedes " + opString(tokix) + " (ass:" +
            (ass == Op.LEFT_ASSOCIATIVITY ? "L)" : "R)"));
        }
      }

      Op operator = opers.pop();
      List<ASTNode> arguments = new ArrayList<ASTNode>(operator.operands);
      for (int i = 0; i < operator.operands; i++) {
        if (exprs.isEmpty()) {
          throw new CompilerError("'" + operator + "' missing operators, got " + i + ", but expected " + operator.operands);
        }
        ASTNode e = exprs.pop();
        if (operator.id == OP_FOR && e.op == OP_IN) {
          // for / in, skip one arg
          i++;
        }
        arguments.add(0, e);
      }
      ASTNode result = new ASTNodeOp(operator.id, arguments.toArray(new ASTNode[arguments.size()]));
      if (result.op == OP_ELSE) {
        // handle else speically, look for preceding if and enter the 
        // else op as the if's third operand
        if (dbg) System.out.println("      epush " + result + " collapse, else");
        if (exprs.isEmpty() || exprs.peek().op != OP_IF) {
          throw new CompilerError("else without if", result);
        }
        ASTNodeOp ifop = (ASTNodeOp)exprs.peek();
        while (ifop.operands.size() == 3) {
          ASTNodeOp elseop = (ASTNodeOp)ifop.operands.get(2);
          if (elseop.operands.get(0).op == OP_IF) {
            ifop = (ASTNodeOp)elseop.operands.get(0);
          } else {
            throw new CompilerError("else on else, expected else if", elseop);
          }
        }
        ifop.operands.add(result);
      } else {
        if (dbg) System.out.println("      epush " + result + " collapse");
        exprs.push(result);
      }
    }
    return OP_FINALIZER;
  }
  

  String opersString() {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    for (int i = 0; i < opers.size(); i++) {
      sb.append(opers.get(i).str);
      if (i < opers.size() - 1) sb.append(", ");
    }
    sb.append(']');
    return sb.toString();
  }
  
  static String opString(int tokix) {
    if (tokix >= 0/* && tokix < _OP_FINAL*/) 
      return OPS[tokix].toString();
    else if (tokix == OP_CALL) {
      return "call";
    } else if (tokix == OP_BLOK) {
      return "blok";
    } else {
      return Integer.toString(tokix);
    }
  }
  
  static boolean isOperator(int op) {
    return isConditionalOperator(op) ||
        isLogicalOperator(op) ||
        isAdditiveOperator(op) ||
        isMultiplicativeOperator(op);
  }
  
  static boolean isAssignOperator(int op) {
    return op == OP_ANDEQ || op == OP_NOTEQ || op == OP_OREQ ||
        op == OP_SHLEFTEQ || op == OP_SHRIGHTEQ || op == OP_XOREQ ||
        op == OP_MINUSEQ || op == OP_PLUSEQ || op == OP_DIVEQ ||
        op == OP_MODEQ || op == OP_MULEQ;
  }
  
  static boolean isUnaryOperator(int op) {
    return op == OP_NOT || op == OP_MINUS_UNARY || op == OP_PLUS_UNARY ||
        op == OP_PLUS2 || op == OP_MINUS2;
  }
  
  static boolean isConditionalOperator(int op) {
    return op == OP_EQ2 ||
        op == OP_GE || op == OP_GT || op == OP_LE || op == OP_LT || op == OP_NEQ;
 
  }
  
  static boolean isLogicalOperator(int op) {
    return op == OP_AND || op == OP_ANDEQ || op == OP_NOT || op == OP_NOTEQ || 
        op == OP_OR || op == OP_OREQ || op == OP_SHLEFT || op == OP_SHLEFTEQ || 
        op == OP_SHRIGHT || op == OP_SHRIGHTEQ || op == OP_XOR || op == OP_XOREQ;
  }
  
  static boolean isAdditiveOperator(int op) {
    return op == OP_MINUS || op == OP_MINUSEQ || op == OP_PLUS || op == OP_PLUSEQ ||
        op == OP_MINUS_UNARY || op == OP_PLUS_UNARY || op == OP_PLUS2 || op == OP_MINUS2;
  }
  
  static boolean isMultiplicativeOperator(int op) {
    return op == OP_DIV || op == OP_DIVEQ || op == OP_MOD || op == OP_MODEQ || op == OP_MUL || op == OP_MULEQ; 
  }
  /////////////////////////////////////////////////////////////////////////////
  // Operator nodes
  //
  
  static class Op {
    String str; 
    int id;
    int operands; 
    boolean associativity = RIGHT_ASSOCIATIVITY;
    public static final boolean LEFT_ASSOCIATIVITY = true;
    public static final boolean RIGHT_ASSOCIATIVITY = false;
    public Op(String s, int i, int operands, boolean ass) {
      str=s; id=i; this.operands=operands; this.associativity = ass;
    }
    public Op(String s, int i, int operands) {
      str=s; id=i; this.operands=operands;
    }
    public Op(String s, int i) {
      str=s; id=i; operands=0;
    }
    public String toString() {
      if (id == OP_QUOTE1 || id == OP_QUOTE2) {
        return "string";
      } else if (id == OP_NUMERICI) {
        return "nint";
      } else if (id == OP_NUMERICD) {
        return "ndec";
      } else if (id == OP_NUMERICH1 || id == OP_NUMERICH2) {
        return "ninth";
      } else if (id == OP_NUMERICB1 || id == OP_NUMERICB2) {
        return "nintb";
      } else if (id == OP_SYMBOL) {
        return "symbol";
      } else if (id == OP_PLUS_UNARY) {
        return "u+";
      } else if (id == OP_MINUS_UNARY) {
        return "u-";
      }
      return str.startsWith("\\") ? str.substring(1) : str;
    }
  }

  static class OpCall extends Op{
    int callid;
    public OpCall(String s, int callid) {
      super(s, OP_CALL);
      this.callid = callid;
    }
    public String toString() {
      return "<" + super.toString() + ">";
    }
  }
}

