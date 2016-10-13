package com.pelleplutt.plang;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.pelleplutt.plang.ASTNode.ASTNodeExpr;
import com.pelleplutt.plang.ASTNode.ASTNodeFuncCall;
import com.pelleplutt.plang.ASTNode.ASTNodeNumeric;
import com.pelleplutt.plang.ASTNode.ASTNodeOp;
import com.pelleplutt.plang.ASTNode.ASTNodeString;
import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;
import com.pelleplutt.tuscedo.Lexer;

public class AST implements Lexer.Emitter {
  Lexer lexer;
  boolean dbg = true;
  static int __id = 0;
  final static int OP_COMMENTMULTI = __id++;
  final static int OP_COMMENTLINE  = __id++;
  final static int OP_SPACES       = __id++;
  final static int OP_QUOTE1       = __id++;
  final static int OP_QUOTE2       = __id++;
  final static int OP_BRACEO       = __id++;
  final static int OP_BRACKETO     = __id++;
  final static int OP_BRACKETC     = __id++;
  final static int OP_PARENO       = __id++;
  final static int OP_PARENC       = __id++;
  final static int OP_LABEL        = __id++;
  final static int OP_FUNCDEF      = __id++;
  final static int OP_WHILE        = __id++;
  final static int OP_IN           = __id++;
  final static int OP_FOR          = __id++;
  final static int OP_ELSE         = __id++;
  final static int OP_ELSEIF       = __id++;
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
  final static int OP_SEMI         = __id++;
  final static int OP_COMMA        = __id++;
  final static int OP_NUMERIC0     = __id++;
  final static int OP_NUMERICI     = __id++;
  final static int OP_NUMERICD     = __id++;
  final static int OP_NUMERICH1    = __id++;
  final static int OP_NUMERICH2    = __id++;
  final static int OP_SYMBOL       = __id++;
  final static int OP_BRACEC       = __id++;
  final static int _OP_FINAL       = __id++;
  final static int OP_FINALIZER    = -1;
  final static int OP_COMP         = -2;
  final static int OP_CALL         = -3;
  
  public final static Op[] OPS = {
      new Op("/\\**?\\*/", OP_COMMENTMULTI),
      new Op("//*?\n", OP_COMMENTLINE),
      new Op("*_", OP_SPACES),
      new Op("\"*?\"", OP_QUOTE1),
      new Op("'*?'", OP_QUOTE2),
      new Op("{", OP_BRACEO),
      new Op("[", OP_BRACKETO),
      new Op("]", OP_BRACKETC),
      new Op("(", OP_PARENO),
      new Op(")", OP_PARENC),
      new Op(":", OP_LABEL, 1),
      new Op("func", OP_FUNCDEF, 1),
      new Op("while", OP_WHILE, 2),
      new Op("in", OP_IN, 1),
      new Op("for", OP_FOR, 4),
      new Op("else", OP_ELSE, 1),
      new Op("elseif", OP_ELSEIF, 2),
      new Op("if", OP_IF, 2),
      new Op("break", OP_BREAK),
      new Op("continue", OP_CONTINUE),
      new Op("goto", OP_GOTO, 1),
      new Op("return", OP_RETURN, 1),
      new Op("=", OP_EQ, 2),
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
      new Op(";", OP_SEMI),
      new Op(",", OP_COMMA),
      new Op("0", OP_NUMERIC0),
      new Op("*%", OP_NUMERICI),
      new Op("*%.*%", OP_NUMERICD),
      new Op("0x*^1", OP_NUMERICH1),
      new Op("0X*^1", OP_NUMERICH2),
      new Op("*^0", OP_SYMBOL),
      new Op("}", OP_BRACEC),
      new Op("-", _OP_FINAL),
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
      lexer.addSymbol(OPS[i].str, OPS[i].id);
    }
    lexer.compile();
    lexer.defineUserSet("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_", 0);
    lexer.defineUserSet("abcdefABCDEF0123456789", 1);
    //lexer.printTree();
  }
  
  public void buildTree(String s) {
    callid = 0;
    byte tst[] = s.getBytes();
    for (byte b : tst) {
      lexer.feed(b);
    }
    lexer.flush();
    onOperator(OP_FINALIZER);
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
    if (dbg) System.out.println("SYMBOL: <" + new String(symdata, 0, len) + "> " + tokix);
    if (dbg) System.out.println("        >> EX:" + exprs);
    if (dbg) System.out.println("        >> OP:" + opersString());
    
    if (tokix == OP_COMMENTLINE || tokix == OP_COMMENTMULTI) {
      if (dbg) System.out.println(new String(symdata, 0, len));
    } else if (tokix == OP_SPACES) {
      if (dbg) System.out.println("");
    } 
    
    else if (tokix == OP_NUMERICD) {
      try {
        onNumber(Double.parseDouble(new String(symdata, 0, len)), true);
      } catch (Throwable t) {t.printStackTrace();}
    } else if (tokix == OP_NUMERICI || tokix == OP_NUMERIC0) {
      try {
        onNumber(Integer.parseInt(new String(symdata, 0, len)), false);
      } catch (Throwable t) {t.printStackTrace();}
    } else if (tokix == OP_NUMERICH1 || tokix== OP_NUMERICH2) {
      try {
        onNumber(Integer.parseInt(new String(symdata, 2, len-2), 16), false);
      } catch (Throwable t) {t.printStackTrace();}
    } 
    
    else if (tokix == OP_SYMBOL) {
      onSymbol(new String(symdata, 0, len));
    }
    
    else if (tokix == OP_QUOTE1 || tokix == OP_QUOTE2) {
      onString(new String(symdata, 1, len-2));
    }
    
    else if (tokix == OP_LABEL) {
      onOperator(tokix);
      onStatemendEnd();
    }
    
    else if (tokix == OP_PARENO) {
      onParenthesisOpen(tokix);
    }
    else if (tokix == OP_PARENC) {
      onParenthesisClose(tokix);
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
  
  void onParenthesisOpen(int tokix) {
    if (prevTokix == OP_SYMBOL) {
      if (prevPrevTokix == OP_FUNCDEF) {
        // function definition
      } else {
        // function call
        ASTNodeSymbol funcName = (ASTNodeSymbol)exprs.pop();
        int callid = getCallId();
        ASTNodeFuncCall call = new ASTNodeFuncCall(funcName.symbol, callid);
        OpCall op = new OpCall(funcName.symbol, callid);
        opers.push(op);
        if (dbg) System.out.println("      opush pareno " + opString(tokix) + ", funccall " + call);
        exprs.push(call);
      }
    } else {
      // simple parenthesis
      if (dbg) System.out.println("      opush pareno " + opString(tokix));
      opers.push(OPS[tokix]);
    }
  }
  
  void onParenthesisClose(int tokix) {
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
          a.operands = args;
          exprs.push(a);
          break;
        } else {
          args.add(0, a);
        }
      }
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
    ASTNode result = new ASTNodeExpr(arguments.toArray(new ASTNode[arguments.size()]));
    if (dbg) System.out.println("      opop  " + opers.peek());
    opers.pop(); // pop off the {
    if (dbg) System.out.println("      epush " + result);
    exprs.push(result);
    collapseStack(OP_FINALIZER, OP_BRACEO);
  }
  
  void onComma(int tokix) {
    collapseStack(tokix, OP_PARENO, OP_CALL, OP_BRACEO);
  }
  
  void onSemi(int tokix) {
    collapseStack(tokix, OP_PARENO, OP_CALL, OP_BRACEO);
  }
  
  void onStatemendEnd() {
    onOperator(OP_FINALIZER);
  }
  
  void onOperator(int tokix) {
    if (tokix == OP_FINALIZER) {
      if (dbg) System.out.println("   hndle tok finalizer");
    } else {
      if (dbg) System.out.println("   hndle tok " + OPS[tokix]);
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
      if (until) {
        for (int untilTokid : untilTokixs) {
          if (topTokix == untilTokid) {
            if (dbg) System.out.println("      collapse end, found " + opString(topTokix));
            return topTokix;
          }
        }
      } else {
        if (topTokix < tokix) {
          return topTokix;
        } else {
          if (dbg) System.out.print("      " + opString(topTokix) + " < " + opString(tokix));
        }
      }

      Op operator = opers.pop();
      List<ASTNode> arguments = new ArrayList<ASTNode>(operator.operands);
      for (int i = 0; i < operator.operands; i++) {
        if (exprs.isEmpty()) {
          throw new CompilerError("'" + operator + "' missing operators, got " + i + " of " + operator.operands);
        }
        ASTNode e = exprs.pop();
        if (operator.id == OP_FOR && e.op == OP_IN) {
          // for / in, skip one arg
          i++;
        }
        arguments.add(0, e);
      }
      ASTNode result = new ASTNodeOp(operator.id, arguments.toArray(new ASTNode[arguments.size()]));
      if (dbg) System.out.println("      epush " + result + " collapse");
      exprs.push(result);
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
  
  String opString(int tokix) {
    if (tokix >= 0 && tokix < _OP_FINAL) 
      return OPS[tokix].str;
    else if (tokix == OP_CALL) {
      return "call";
    } else {
      return Integer.toString(tokix);
    }
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // Operator nodes
  //
  
  static class Op {
    String str; int id; int operands; 
    public Op(String s, int i, int operands) {
      str=s; id=i; this.operands=operands;
    }
    public Op(String s, int i) {
      str=s; id=i; operands=0;
    }
    public String toString() {
      if (id == OP_QUOTE1 || id == OP_QUOTE2) {
        return "STR";
      } else if (id == OP_NUMERIC0 || id == OP_NUMERICI) {
        return "NUMI";
      } else if (id == OP_NUMERICD) {
        return "NUMD";
      } else if (id == OP_NUMERICH1 || id == OP_NUMERICH2) {
        return "NUMIh";
      } else if (id == OP_SYMBOL) {
        return "SYM";
      }
      return str.replace('\\', ' ');
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

  /*
   *     // handle opening parenthesis
    if (tokix == OP_PARENO) {
      // check if this is a function call
      // TODO
//      if (!exprs.isEmpty() && exprs.peek() instanceof ASTNodeSymbol && 
//          (opers.isEmpty() || opers.peek().id != OP_FUNCDEF)) {
      if (prevPrevTokix != OP_FUNCDEF && prevTokix == OP_SYMBOL) {
        ASTNodeSymbol funcName = (ASTNodeSymbol)exprs.pop();
        ASTNodeFuncCall call = new ASTNodeFuncCall(funcName.symbol);
        OpCall op = new OpCall(funcName.symbol);
        opers.push(op);
        if (dbg) System.out.println("     opush func " + op);
        exprs.push(call);
        opers.push(OPS[tokix]);
      } else
      {
        if (dbg) System.out.println("     opush pareno");
        opers.push(OPS[tokix]);
      }
    }
    // handle opening bracket
    else if (tokix == OP_BRACKETO) {
      if (dbg) System.out.println("     opush bracketo");
      opers.push(OPS[tokix]);
    }
    // handle opening brace
    else if (tokix == OP_BRACEO) {
      if (dbg) System.out.println("     bpush braceo");
      opers.push(OPS[tokix]);
      blcks.push(exprs.size());
    }
    // handle closing brace
    else if (tokix == OP_BRACEC) {
      // wrap all expressions within starting brace and this brace
      if (blcks.isEmpty()) {
        throw new Error("'" + OPS[OP_BRACEC] + "' missing '" + OPS[OP_BRACEO] + "'");
      }
      int blckSize = blcks.pop();
      List<ASTNode> arguments = new ArrayList<ASTNode>();
      while (exprs.size() > blckSize) {
        arguments.add(0, exprs.pop());
      }
      ASTNode result = new ASTNodeExpr(arguments.toArray(new ASTNode[arguments.size()]));
      if (dbg) System.out.println("     brkc -> opop the braceo " + opers.peek());
      opers.pop(); // pop off the {
      // check if this end brace defines the body of a function
      if (!exprs.isEmpty() && exprs.peek() instanceof ASTNodeFuncDef) {
        ASTNodeFuncDef f = (ASTNodeFuncDef)exprs.peek();
        f.operands.add(result);
      } else {
        if (dbg) System.out.println("   epush " + result);
        exprs.push(result);
      }
      onOperator(_OP_FINALIZER);
    }
    // handle all other operators
    else {
      // check if stack is to be collapsed
      while (!opers.isEmpty()) {
        boolean popBrack = (tokix == OP_BRACKETC && opers.peek().id != OP_BRACKETO);
        boolean popParen = (tokix == OP_PARENC && opers.peek().id != OP_PARENO);
        boolean popPrio = (opers.peek().id >= tokix);
        if (!popBrack && !popParen && !popPrio) {
          break;
        }
        
        if (tokix >= 0) {
          if (popParen) if (dbg) System.out.print("     pare [");
          if (popPrio)  if (dbg) System.out.print("     prio [");
          if (dbg) System.out.print(opers.peek() + 
              " >= " + OPS[tokix] + "], opop " + opers.peek() + ", ");
        }
        if (tokix == _OP_FINALIZER && 
            (opers.peek().id == OP_BRACEO || 
             opers.peek().id == OP_BRACKETO || 
             opers.peek().id == OP_PARENO)) {
          break;
        }
        
        Op operator = opers.pop();
        List<ASTNode> arguments = new ArrayList<ASTNode>(operator.operands);
        for (int i = 0; i < operator.operands; i++) {
          if (exprs.isEmpty()) {
            throw new CompilerError("'" + operator + "' missing operators, got " + i + " of " + operator.operands);
          }
          ASTNode e = exprs.pop();
          if (operator.id == OP_FOR && e.op == OP_IN) {
            // for / in, skip one arg
            i++;
          }
          arguments.add(0, e);
        }
        ASTNode result = new ASTNodeOp(operator.id, arguments.toArray(new ASTNode[arguments.size()]));
        if (dbg) System.out.println("   epush " + result);
        exprs.push(result);
      }
      if (tokix == _OP_FINALIZER) {
        ASTNodeExpr e = new ASTNodeExpr();
        while (!exprs.isEmpty() && !(exprs.peek() instanceof ASTNodeExpr)) {
          e.operands.add(exprs.pop());
        }
        exprs.push(e);
      }
      
      // handle closing parenthesis
      if (tokix == OP_PARENC) {
        if (dbg) System.out.println("     parc -> opop the pareno " + opers.peek());
        opers.pop(); // pop off the (
        // check if this is the end of argument definition in a funcdef
        if (!opers.isEmpty() && opers.peek().id == OP_FUNCDEF) {
          opers.pop(); // pop off the func
          List<ASTNode> arguments = new ArrayList<ASTNode>();
          while (!exprs.isEmpty() && (exprs.peek() instanceof ASTNodeSymbol)) {
            arguments.add(0, exprs.pop());
          }
          if (arguments.isEmpty()) {
            throw new Error("Expected function name");
          }
          ASTNode funcName = arguments.remove(0);
          if (!(funcName instanceof ASTNodeSymbol)) {
            throw new Error("Expected function name");
          }
          ASTNode result = new ASTNodeFuncDef(((ASTNodeSymbol)funcName).symbol);
          result.operands = arguments;
          exprs.push(result);
        }
        // check if this is the end of argument definition in a funccall
        // TODO 
//        else if (!opers.isEmpty() && opers.peek().id == OP_CALL) {
//          opers.pop(); // pop off the func
//          List<ASTNode> arguments = new ArrayList<ASTNode>();
//          while (!exprs.isEmpty() && !(exprs.peek() instanceof ASTNodeFuncCall)) {
//            arguments.add(0, exprs.pop());
//          }
//          if (arguments.isEmpty()) {
//            throw new CompilerError("Expected function call");
//          }
//          ASTNode funcCall = exprs.isEmpty() ? null : exprs.peek();
//          if (!(funcCall instanceof ASTNodeFuncCall)) {
//            throw new CompilerError("Expected function call");
//          }
//          funcCall.operands = arguments;
//        }
      } 
      // handle closing bracket
      else if (tokix == OP_BRACKETC) {
        if (dbg) System.out.println("     brkc -> opop the bracko " + opers.peek());
        opers.pop(); // pop off the [
      } 
      // handle non finalizer
      else if (tokix != _OP_FINALIZER) {
        if (dbg) System.out.println("     opush " + OPS[tokix]);
        opers.push(OPS[tokix]);
      }
    }
*/
}

