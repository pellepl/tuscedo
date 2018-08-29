package com.pelleplutt.operandi;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.pelleplutt.operandi.ASTNode.ASTNodeArrDecl;
import com.pelleplutt.operandi.ASTNode.ASTNodeBlok;
import com.pelleplutt.operandi.ASTNode.ASTNodeFuncCall;
import com.pelleplutt.operandi.ASTNode.ASTNodeFuncDef;
import com.pelleplutt.operandi.ASTNode.ASTNodeNumeric;
import com.pelleplutt.operandi.ASTNode.ASTNodeOp;
import com.pelleplutt.operandi.ASTNode.ASTNodeString;
import com.pelleplutt.operandi.ASTNode.ASTNodeSymbol;
import com.pelleplutt.tuscedo.Lexer;

public class AST implements Lexer.Emitter {
  Lexer lexer;
  public static boolean dbg = false;
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
  final static int OP_ELSE         = __id++;
  final static int OP_IF           = __id++;
  final static int OP_WHILE        = __id++;
  final static int OP_IN           = __id++;
  final static int OP_FOR          = __id++;
  final static int OP_BREAK        = __id++;
  final static int OP_CONTINUE     = __id++;
  final static int OP_RETURN       = __id++;
  final static int OP_GOTO         = __id++;
  
  final static int OP_EQ           = __id++;
  final static int OP_HASH         = __id++;
  final static int OP_OREQ         = __id++;
  final static int OP_XOREQ        = __id++;
  final static int OP_ANDEQ        = __id++;
  final static int OP_SHLEFTEQ     = __id++;
  final static int OP_SHRIGHTEQ    = __id++;
  final static int OP_PLUSEQ       = __id++;
  final static int OP_MINUSEQ      = __id++;
  final static int OP_MODEQ        = __id++;
  final static int OP_DIVEQ        = __id++;
  final static int OP_MULEQ        = __id++;
  final static int OP_OR           = __id++;
  final static int OP_XOR          = __id++;
  final static int OP_AND          = __id++;
  final static int OP_SHLEFT       = __id++;
  final static int OP_SHRIGHT      = __id++;
  final static int OP_EQ2          = __id++;
  final static int OP_GT           = __id++;
  final static int OP_LT           = __id++;
  final static int OP_GE           = __id++;
  final static int OP_LE           = __id++;
  final static int OP_NEQ          = __id++;
  final static int OP_PLUS         = __id++;
  final static int OP_MINUS        = __id++;
  final static int OP_MOD          = __id++;
  final static int OP_DIV          = __id++;
  final static int OP_MUL          = __id++;
  final static int OP_PLUS2        = __id++;
  final static int OP_MINUS2       = __id++;
  final static int OP_DOT          = __id++;
  final static int OP_BNOT         = __id++;
  final static int OP_LNOT         = __id++;
  final static int OP_GLOBAL       = __id++;
  
  final static int OP_SEMI         = __id++;
  final static int OP_COMMA        = __id++;
  final static int OP_NUMERICI     = __id++;
  final static int OP_NUMERICD     = __id++;
  final static int OP_NUMERICH1    = __id++;
  final static int OP_NUMERICH2    = __id++;
  final static int OP_NUMERICB1    = __id++;
  final static int OP_NUMERICB2    = __id++;
  final static int OP_NIL          = __id++;
  final static int OP_BKPT         = __id++;
  final static int OP_SYMBOL       = __id++;
  final static int OP_ARGC         = __id++;
  final static int OP_ARGV         = __id++;
  final static int OP_ARG          = __id++;
  final static int OP_ME           = __id++;
  
  // non lexeme tokens
  final static int _OP_FINAL       = __id++;
  final static int OP_ADECL        = __id++;
  final static int OP_ADEREF       = __id++;
  final static int OP_TUPLE        = __id++;
  final static int OP_MINUS_UNARY  = __id++;
  final static int OP_PLUS_UNARY   = __id++;
  final static int OP_POSTINC      = __id++;
  final static int OP_PREINC       = __id++;
  final static int OP_POSTDEC      = __id++;
  final static int OP_PREDEC       = __id++;
  
  final static int OP_FINALIZER    = -1;
  final static int OP_BLOK         = -2;
  final static int OP_CALL         = -3;
  final static int OP_FOR_IN       = -4;
  final static int OP_RANGE        = -5;
  final static int OP_INBUILT_FN   = -6;
  
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
      new Op("else", OP_ELSE, 1),
      new Op("if", OP_IF, 2),
      new Op("while", OP_WHILE, 2),
      new Op("in", OP_IN, 2),
      new Op("for", OP_FOR, 4),
      new Op("break", OP_BREAK),
      new Op("continue", OP_CONTINUE),
      new Op("return", OP_RETURN, 1),
      new Op("goto", OP_GOTO, 1),
      
      new Op("=", OP_EQ, 2, Op.LEFT_ASSOCIATIVITY),
      new Op("#", OP_HASH, 2),
      new Op("|=", OP_OREQ, 2),
      new Op("\\^=", OP_XOREQ, 2),
      new Op("&=", OP_ANDEQ, 2),
      new Op("<<=", OP_SHLEFTEQ, 2),
      new Op(">>=", OP_SHRIGHTEQ, 2),
      new Op("+=", OP_PLUSEQ, 2),
      new Op("-=", OP_MINUSEQ, 2),
      new Op("\\%=", OP_MODEQ, 2),
      new Op("/=", OP_DIVEQ, 2),
      new Op("\\*=", OP_MULEQ, 2),
      new Op("|", OP_OR, 2),
      new Op("\\^", OP_XOR, 2),
      new Op("&", OP_AND, 2),
      new Op("<<", OP_SHLEFT, 2),
      new Op(">>", OP_SHRIGHT, 2),
      new Op("==", OP_EQ2, 2),
      new Op(">", OP_GT, 2),
      new Op("<", OP_LT, 2),
      new Op(">=", OP_GE, 2),
      new Op("<=", OP_LE, 2),
      new Op("\\!=", OP_NEQ, 2),
      new Op("+", OP_PLUS, 2),
      new Op("-", OP_MINUS, 2),
      new Op("\\%", OP_MOD, 2),
      new Op("/", OP_DIV, 2),
      new Op("\\*", OP_MUL, 2),
      new Op("++", OP_PLUS2, 1), 
      new Op("--", OP_MINUS2, 1),
      new Op(".", OP_DOT, 2),
      new Op("~", OP_BNOT, 1),
      new Op("\\!", OP_LNOT, 1),
      new Op("global", OP_GLOBAL, 1),
      
      new Op(";", OP_SEMI),
      new Op(",", OP_COMMA),
      new Op("*%", OP_NUMERICI),
      new Op("*%.*%", OP_NUMERICD),
      new Op("0x*^1", OP_NUMERICH1),
      new Op("0X*^1", OP_NUMERICH2),
      new Op("0b*^2", OP_NUMERICB1),
      new Op("0B*^2", OP_NUMERICB2),
      new Op("nil", OP_NIL),
      new Op("\\_\\_BKPT", OP_BKPT),
      new Op("*^0", OP_SYMBOL),
      new Op("$argc", OP_ARGC),
      new Op("$argv", OP_ARGV),
      new Op("$*%", OP_ARG),
      new Op("me", OP_ME),
      
      new Op("<@>", _OP_FINAL),
      
      new Op("setdeclr", OP_ADECL, 2, OP_BRACKETO),
      new Op("deref", OP_ADEREF, 2, OP_BRACKETO),
      new Op("tuple", OP_TUPLE, 2, OP_LABEL),
      new Op("U-", OP_MINUS_UNARY, 1, OP_MINUS),
      new Op("U+", OP_PLUS_UNARY, 1, OP_PLUS),
      new Op("++U", OP_POSTINC, 1, OP_PLUS),
      new Op("U++", OP_PREINC, 1, OP_PLUS),
      new Op("--U", OP_POSTDEC, 1, OP_MINUS),
      new Op("U--", OP_PREDEC, 1, OP_MINUS),
  };

  Stack<ASTNode> exprs = new Stack<ASTNode>();
  Stack<Op> opers = new Stack<Op>();
  // stack of current blocks
  Stack<Integer> blcks = new Stack<Integer>();
  // stack of current block symbol number
  Stack<Integer> blkSymNbrs = new Stack<Integer>(); 
  int callid;
  int arrdeclid;
  // symbol counter for current block
  int blkSymCounter;
  int prevTokix = -1;
  int prevPrevTokix = -1;
  int stroffset, strlen;
  
  public AST() {
    lexer = new Lexer(this, 65536);
    for (int i = 0; i < _OP_FINAL; i++) {
      if (i >= OP_FUNCDEF && i <= OP_GOTO ||
          i == OP_GLOBAL || i == OP_ME || i == OP_MODULE) {
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
    ast.arrdeclid = 0;
    ast.blkSymCounter = 0;
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

  int globoffs, globlen;
  void pushExpr(ASTNode e) {
    if (e != null) {
      e.stroffset = stroffset;
      e.strlen = strlen;
      exprs.push(e);
      collateNodeDbgInfo(e);
    }
  }
  
  void collateNodeDbgInfo(ASTNode e) {
    int minOffs = Integer.MAX_VALUE, maxOffs = Integer.MIN_VALUE;
    Stack<ASTNode> rece = new Stack<ASTNode>();
    rece.add(e);
    while (!rece.isEmpty()) {
      ASTNode ev = rece.pop();
      if (ev.strlen > 0 && ev.stroffset > 0) {
        minOffs = Math.min(minOffs, ev.stroffset);
        maxOffs = Math.max(maxOffs, ev.stroffset + ev.strlen);
      }
      if (ev.op == OP_CALL && ev instanceof ASTNodeFuncCall) {
        ASTNodeFuncCall evCall = (ASTNodeFuncCall)ev;
        rece.push(evCall.callByOperation ? evCall.callAddrOp : evCall.name);
      }
      if (ev.operands != null) {
        for (ASTNode e2 : ev.operands) {
          rece.push(e2);
        }
      }
    }
    e.stroffset = minOffs;
    e.strlen = maxOffs - minOffs;
  }
  
  // impl Parser.Emitter
  
  @Override
  public void data(byte[] data, int len, int offset) {
    if (dbg) System.out.println("GARBAGE:<" + new String(data, 0, len) + ">");
  }

  @Override
  public void symbol(byte[] symdata, int len, int tokix, int offset) {
    stroffset = offset;
    strlen = len;
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
        onNumber((int)Long.parseLong(new String(symdata, 2, len-2), 16), false);
      } catch (Throwable t) {t.printStackTrace();}
    } else if (tokix == OP_NUMERICB1 || tokix== OP_NUMERICB2) {
      try {
        onNumber(Integer.parseInt(new String(symdata, 2, len-2), 2), false);
      } catch (Throwable t) {t.printStackTrace();}
    } 
    
    else if (tokix == OP_SYMBOL 
        || tokix == OP_ARGC || tokix == OP_ARGV || tokix == OP_ARG 
        || tokix == OP_ME) {
      onSymbol(new String(symdata, 0, len));
    }
    
    else if (tokix == OP_QUOTE1 || tokix == OP_QUOTE2) {
      onString(new String(symdata, 1, len-2));
    }
    
    else if (tokix == OP_LABEL) {
      onLabelOrTuple(tokix);
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
      if (isArithmeticOperand(prevTokix)) {
        // binary minus
        onOperator(tokix);
      } else {
        // unary minus
        onOperator(OP_MINUS_UNARY);
      }
    } else if (tokix == OP_PLUS) {
      if (isArithmeticOperand(prevTokix)) {
        // binary plus
        onOperator(tokix);
      } else {
        // unary plus
        onOperator(OP_PLUS_UNARY);
      }
    } else if (tokix == OP_PLUS2 || tokix == OP_MINUS2) {
      if (isArithmeticOperand(prevTokix)) {
        // postinc/dec
        onOperator(tokix == OP_PLUS2 ? OP_POSTINC : OP_POSTDEC);
      } else {
        // preinc/dec
        onOperator(tokix == OP_PLUS2 ? OP_PREINC : OP_PREDEC);
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
    pushExpr(new ASTNodeNumeric(number, f));
  }
  
  void onString(String str) {
    if (dbg) System.out.println("   epush str " + str);
    pushExpr(new ASTNodeString(str));
  }
  
  void onSymbol(String symbol) {
    if (dbg) System.out.println("   epush sym " + symbol);
    ASTNodeSymbol esym = new ASTNodeSymbol(symbol);
    esym.symNbr = blkSymCounter++; // assign symbol number for this symbol in this block
    pushExpr(esym);
  }
  
  void onLabelOrTuple(int tokix) {
    if (prevTokix == OP_SYMBOL && (
        prevTokix == OP_FINALIZER || 
        prevPrevTokix == OP_FINALIZER || 
        prevPrevTokix == OP_SEMI ||
        prevPrevTokix == OP_BRACEO ||
        prevPrevTokix == OP_BRACEC)) {
      // label
      // collapse all before label
      ASTNode a = exprs.pop();
      collapseStack(OP_FINALIZER, OP_BRACEO);
      // collapse label
      pushExpr(a);
      onOperator(tokix);
      collapseStack(OP_FINALIZER, OP_BRACEO);
    } else {
      onOperator(OP_TUPLE);
    }
  }
  
  void onParenthesisOpen(int tokix) {
    if (!exprs.isEmpty() && prevTokix != OP_FOR && prevTokix != OP_IF && prevTokix != OP_RETURN && 
          (exprs.peek().op == OP_ADEREF && prevTokix == OP_BRACKETC ||
          exprs.peek().op == OP_CALL && !opers.isEmpty() && opers.peek().id != OP_CALL || 
          prevPrevTokix == OP_DOT)) {
      if (prevPrevTokix == OP_DOT) collapseStack(OP_DOT);

      // function call, addr by op
      int callid = getCallId();
      ASTNodeFuncCall callnode = new ASTNodeFuncCall(exprs.pop(), callid);
      OpCall callop = new OpCall("<call addr on stack>", callid);
      opers.push(callop);
      if (dbg) System.out.println("      opush pareno " + opString(tokix) + ", funccallbyop " + callnode);
      pushExpr(callnode);
    } 
    else if (prevTokix == OP_SYMBOL && prevPrevTokix != OP_DOT) {
      if (prevPrevTokix == OP_FUNCDEF) {
        // function definition
        ASTNodeSymbol funcName = (ASTNodeSymbol)exprs.pop();
        ASTNodeFuncDef defnode = new ASTNodeFuncDef(funcName.symbol);
        if (dbg) System.out.println("      opush pareno " + opString(tokix) + ", funcdef " + defnode);
        pushExpr(defnode);
      } else {
        // function call, plain symbol
        ASTNodeSymbol funcName = (ASTNodeSymbol)exprs.pop();
        int callid = getCallId();
        ASTNodeFuncCall callnode = new ASTNodeFuncCall(funcName, callid);
        OpCall callop = new OpCall(funcName.symbol, callid);
        opers.push(callop);
        if (dbg) System.out.println("      opush pareno " + opString(tokix) + ", funccallbysym " + callnode);
        pushExpr(callnode);
      }
    } 
    else {
      // simple parenthesis
      if (dbg) System.out.println("      opush pareno " + opString(tokix));
      opers.push(OPS[tokix]);
    }
  }
  
  void onParenthesisClose(int tokix) {
    // TODO
    // cannot have this for case "(1)"
    //if (!opers.isEmpty() && opers.peek().id == OP_PARENO) {
    //  // empty paren (), push empty block
    //  pushExpr(new ASTNodeBlok());
    //}
    int endedAtTokix = collapseStack(tokix, OP_PARENO, OP_CALL, OP_FUNCDEF);
    if (endedAtTokix == OP_PARENO) {
      if (dbg) System.out.println("      opop  " + opers.peek());
      opers.pop(); // pop off the (
    } else if (endedAtTokix == OP_CALL) {
      OpCall call = (OpCall)opers.pop();
      // collect arguments to func call
      List<ASTNode> args = new ArrayList<ASTNode>();
      boolean checkFuncFound = false;
      while (!exprs.isEmpty()) {
        ASTNode a = exprs.pop();
        if (a instanceof ASTNodeFuncCall && ((ASTNodeFuncCall)a).callid == call.callid) {
          ((ASTNodeFuncCall)a).setArguments(args);
          pushExpr(a);
          checkFuncFound = true;
          break;
        } else {
          args.add(0, a);
        }
      }
      if (!checkFuncFound) {
        throw new CompilerError("fatal, collected args but no func", exprs.isEmpty()? null : exprs.peek());
      }
    } else if (endedAtTokix == OP_FUNCDEF) {
      // collect arguments to func def
      List<ASTNode> args = new ArrayList<ASTNode>();
      boolean checkFuncFound = false;
      while (!exprs.isEmpty()) {
        ASTNode a = exprs.pop();
        if (a instanceof ASTNodeFuncDef) {
          ((ASTNodeFuncDef)a).setArguments(args);
          pushExpr(a);
          checkFuncFound = true;
          break;
        } else {
          args.add(0, a);
        }
      }
      if (!checkFuncFound) {
        throw new CompilerError("fatal, collected args but no func def", exprs.isEmpty()? null : exprs.peek());
      }
    } else {
      throw new CompilerError("missing left parenthesis", exprs.isEmpty()? null : exprs.peek());
    }
  }
  
  void onBracketOpen(int tokix) {
    if (dbg) System.out.println("      opush bracketo " + opString(tokix));
    if (prevTokix != OP_COMMA && ( 
        prevTokix == OP_PARENC || 
        prevTokix == OP_BRACKETC || 
        prevTokix == OP_SYMBOL || 
        prevTokix == OP_ARGV || 
        prevTokix == OP_ARG || 
        prevTokix == OP_ME || 
        isString(prevTokix) || 
        (!exprs.isEmpty() && ( 
            (exprs.peek().op == OP_CALL && prevTokix != OP_PARENO) ||
            (exprs.peek().op == OP_ADECL)
            )
          )
        )) {
      // deref
      collapseStack(OP_DOT); // TODO - is this ok???
      opers.push(OPS[OP_ADEREF]);
    } else if (
        isOperator(prevTokix) ||
        prevTokix == OP_EQ ||
        prevTokix == OP_PARENO || 
        prevTokix == OP_BRACEO || 
        prevTokix == OP_BRACEC || 
        prevTokix == OP_BRACKETO || 
        prevTokix == OP_COMMA || 
        prevTokix == OP_SEMI || 
        prevTokix == OP_RETURN || 
        prevTokix == OP_LABEL) {
      // declare
      opers.push(new OpArrDecl(arrdeclid++));
    } else {
      if (exprs.isEmpty()) {
        throw new CompilerError("syntax error", null);
      } else {
        throw new CompilerError("unhandled operation (" + opString(prevTokix) + ")", exprs.peek());
      }
    }
  }
  
  void onBracketClose(int tokix) {
    if (prevTokix == OP_BRACKETO) {
      Op op = opers.pop();
      if (op.id == OP_ADEREF) {
        if (!exprs.isEmpty() && exprs.peek().op == OP_SYMBOL) {
          ASTNode symbol = exprs.pop();
          ASTNode emptyarr = new ASTNodeArrDecl(arrdeclid++);
          ASTNodeOp assign = new ASTNodeOp(OP_EQ, symbol, emptyarr);
          pushExpr(assign);
        } else {
          throw new CompilerError("Array declaration must be preceded by symbol only", exprs.peek());
        }
      } else {
        pushExpr(new ASTNodeArrDecl(((OpArrDecl)op).arrid));
      }
    } else {
      int endedAtTokix = collapseStack(OP_FINALIZER, OP_ADEREF, OP_ADECL);
      if (endedAtTokix == OP_ADEREF) {
        if (dbg) System.out.println("      opop  " + opers.peek());
        opers.pop(); // pop off the [ aderef
        ASTNode e2 = exprs.pop();
        ASTNode e1 = exprs.pop();
        pushExpr(new ASTNodeOp(OP_ADEREF, e1, e2));
      } else if (endedAtTokix == OP_ADECL) {
        if (dbg) System.out.println("      opop  " + opers.peek());
        handleArrayDeclaration();
        opers.pop(); // pop off the [ adecl
      } else {
        throw new CompilerError("missing left bracket", exprs.peek());
      }
    }
  }
  
  void onBraceOpen(int tokix) {
    if (dbg) System.out.println("      epush " + opString(tokix));
    opers.push(OPS[tokix]);
    blcks.push(exprs.size());
    blkSymCounter++;
    blkSymNbrs.push(blkSymCounter); // push current symbol counter, and restart counter for new block
    blkSymCounter = 0;
  }
  
  void onBraceClose(int tokix) {
    // wrap all expressions within starting brace and this brace
    if (blcks.isEmpty()) {
      throw new CompilerError("'" + OPS[OP_BRACEC] + "' missing '" + OPS[OP_BRACEO] + "'", stroffset, strlen);
    }
    blkSymCounter = blkSymNbrs.pop(); // restore symbol counter when returning to previous block
    int blckSize = blcks.pop();
    List<ASTNode> arguments = new ArrayList<ASTNode>();
    while (exprs.size() > blckSize) {
      arguments.add(0, exprs.pop());
    }
    ASTNodeBlok result = new ASTNodeBlok(arguments.toArray(new ASTNode[arguments.size()]));
    result.symNbr = blkSymCounter; // assign the symbol number to this block
    if (dbg) System.out.println("      epush " + result);
    pushExpr(result);
    if (collapseStack(OP_FINALIZER, OP_BRACEO) != OP_BRACEO) {
    };
    if (dbg) System.out.println("      opop  " + opers.peek());
    opers.pop(); // pop off the {
    if (!opers.isEmpty()) {
      if (opers.peek().id == OP_FOR || opers.peek().id == OP_WHILE || opers.peek().id == OP_IF ||
          opers.peek().id == OP_ELSE) {
        if (dbg) System.out.println("      found control stmnt " + AST.opString(opers.peek().id) + ", collapse to this");
        // if collapsing to "if", then check any preceding "else"
        boolean isIf = opers.peek().id == OP_IF;
        collapseStack(opers.peek().id);
        if (isIf && !opers.isEmpty() && opers.peek().id == OP_ELSE) {
          collapseStack(opers.peek().id);
        }
      } else if (opers.peek().id == OP_FUNCDEF) {
        opers.pop(); // pop off the "func"
        exprs.get(exprs.size()-2).operands.add(exprs.pop());
      }
    }
  }
  
  void onComma(int tokix) {
    int etokix = collapseStack(OP_FINALIZER, OP_PARENO, OP_CALL, OP_FUNCDEF, OP_BRACEO, OP_ADECL, OP_ADEREF);
    if (etokix != OP_CALL && etokix != OP_BRACEO && etokix != OP_PARENO && 
        etokix != OP_FUNCDEF && etokix != OP_ADECL && etokix != OP_ADEREF) {
      throw new CompilerError("missing delimiter (parenthesis, brace, or bracket)");
    } else if (etokix == OP_ADECL) {
      handleArrayDeclaration();
    }
  }
  
  void handleArrayDeclaration() {
    ASTNode e = null;
    if (exprs.size() >= 2) e = exprs.get(exprs.size()-2);
    if (e != null && e.op == OP_ADECL && ((ASTNodeArrDecl)e).arrid == ((OpArrDecl)opers.peek()).arrid) {
      ASTNodeArrDecl adecle = (ASTNodeArrDecl)exprs.get(exprs.size()-2);
      adecle.operands.add(exprs.pop());
    } else {
      ASTNodeArrDecl adecle = new ASTNodeArrDecl(((OpArrDecl)opers.peek()).arrid);
      adecle.operands.add(exprs.pop());
      pushExpr(adecle);
    }
  }
  
  void onSemi(int tokix) {
    // check if empty semi, if so push empty ASTNodeBlok on ex stack
    if (prevTokix == OP_PARENO || prevTokix == OP_BRACEO || prevTokix == OP_SEMI) {
      pushExpr(new ASTNodeBlok());
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
      int topPrio = topTokix < 0 ? -1 : OPS[topTokix].prio;
      int prio = tokix < 0 ? -1 : OPS[tokix].prio;
      boolean ass = opers.peek().associativity;
      if (until) {
        for (int untilTokid : untilTokixs) {
          if (topTokix == untilTokid) {
            if (dbg) System.out.println("      collapse end, found " + opString(topTokix));
            return topTokix;
          }
        }
      } else {
        if (topPrio < prio || 
            ass == Op.LEFT_ASSOCIATIVITY && topPrio == prio) {
          return topTokix;
        } else {
          if (dbg) System.out.print("      " + opString(topTokix)+":"+topPrio + " precedes " + opString(tokix) +":"+prio+ " (ass:" +
            (ass == Op.LEFT_ASSOCIATIVITY ? "L)" : "R)"));
        }
      }

      Op operator = opers.pop();
      List<ASTNode> arguments = new ArrayList<ASTNode>(operator.operands);
      for (int i = 0; i < operator.operands; i++) {
        if (exprs.isEmpty()) {
          throw new CompilerError("'" + operator + "' missing operators, got " + i + ", but expected " + operator.operands, 
              stroffset, stroffset+strlen);
        }
        ASTNode e = exprs.pop();
        if (operator.id == OP_FOR && e.op == OP_IN) {
          // for / in, skip two args
          i += 2;
        }
        arguments.add(0, e);
      }
      ASTNode result = new ASTNodeOp(operator.id, arguments.toArray(new ASTNode[arguments.size()]));
      if (result.op == OP_ELSE) {
        // handle else specially, look for preceding if and enter the 
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
        pushExpr(result);
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
    } else if (tokix == OP_FUNCDEF) {
      return "def";
    } else if (tokix == OP_BLOK) {
      return "blok";
    } else {
      return Integer.toString(tokix);
    }
  }
  
  public static boolean isArithmeticOperand(int op) {
    return op == OP_SYMBOL || op == OP_ARG || op == OP_ARGC || op == OP_ARGV || op == OP_DOT || 
        isString(op) || isNumber(op) ||
        op == OP_PARENC || op == OP_BRACKETC || op == OP_PLUS2 || op == OP_MINUS2;
  }
  
  public static boolean isNumber(int op) {
    return op == OP_NUMERICB1 || op == OP_NUMERICB2 || op == OP_NUMERICD || 
        op == OP_NUMERICH1  || op == OP_NUMERICH2 || op == OP_NUMERICI;
  }
  
  public static boolean isString(int op) {
    return op == OP_QUOTE1 || op == OP_QUOTE2;
  }
  
  static boolean isOperator(int op) {
    return isConditionalOperator(op) ||
        isLogicalOperator(op) ||
        isAdditiveOperator(op) ||
        isMultiplicativeOperator(op);
  }
  
  public static boolean isAssignOperator(int op) {
    return op == OP_ANDEQ || op == OP_OREQ ||
        op == OP_SHLEFTEQ || op == OP_SHRIGHTEQ || op == OP_XOREQ ||
        op == OP_MINUSEQ || op == OP_PLUSEQ || op == OP_DIVEQ ||
        op == OP_MODEQ || op == OP_MULEQ;
  }
  
  public static boolean isUnaryOperator(int op) {
    return op == OP_BNOT || op == OP_LNOT || op == OP_MINUS_UNARY || op == OP_PLUS_UNARY ||
        op == OP_POSTINC || op == OP_PREINC || op == OP_POSTDEC || op == OP_PREDEC;
  }
  
  static boolean isConditionalOperator(int op) {
    return op == OP_EQ2 ||
        op == OP_GE || op == OP_GT || op == OP_LE || op == OP_LT || op == OP_NEQ ||
        op == OP_IN;
 
  }
  
  static boolean isLogicalOperator(int op) {
    return op == OP_AND || op == OP_ANDEQ || 
        op == OP_BNOT ||  
        op == OP_LNOT ||
        op == OP_OR || op == OP_OREQ || op == OP_SHLEFT || op == OP_SHLEFTEQ || 
        op == OP_SHRIGHT || op == OP_SHRIGHTEQ || op == OP_XOR || op == OP_XOREQ;
  }
  
  static boolean isAdditiveOperator(int op) {
    return op == OP_MINUS || op == OP_MINUSEQ || op == OP_PLUS || op == OP_PLUSEQ ||
        op == OP_MINUS_UNARY || op == OP_PLUS_UNARY || 
        op == OP_POSTINC || op == OP_PREINC  || op == OP_POSTDEC || op == OP_PREDEC;
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
    int prio;
    int operands; 
    boolean associativity = RIGHT_ASSOCIATIVITY;
    public static final boolean LEFT_ASSOCIATIVITY = true;
    public static final boolean RIGHT_ASSOCIATIVITY = false;
    public Op(String s, int i, int operands, boolean ass) {
      str=s; id=i; prio=i; this.operands=operands; this.associativity = ass;
    }
    public Op(String s, int i, int operands) {
      str=s; id=i; prio=i; this.operands=operands;
    }
    public Op(String s, int i, int operands, int prio) {
      str=s; id=i; this.operands=operands; this.prio = prio;
    }
    public Op(String s, int i) {
      str=s; id=i; prio=i; operands=0;
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
  static class OpArrDecl extends Op{
    int arrid;
    public OpArrDecl(int id) {
      super("arrdecl"+id, OP_ADECL, OPS[OP_ADECL].operands, OPS[OP_ADECL].prio);
      this.arrid = id;
    }
    public String toString() {
      return "<" + super.toString() + ">";
    }
  }
  
}

