package com.pelleplutt.plang;

import java.util.List;

import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.ASTNode.ASTNodeFuncCall;
import com.pelleplutt.plang.ASTNode.ASTNodeNumeric;
import com.pelleplutt.plang.ASTNode.ASTNodeString;
import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;

import static com.pelleplutt.plang.AST.*;

public class CodeGenFront {
  int code = 0;
  int label = 0;
  
  public CodeGenFront() {
  }

  
  public void gen(List<ASTNode> exprs) {
    ASTNodeBlok eblok = new ASTNodeBlok();
    eblok.operands = exprs;
    genrec(eblok);
  }
  
  String genrec(ASTNode e) {
    if (e.op == OP_BLOK) {
      System.out.println("\tBEGIN");
      for (ASTNode e2 : e.operands) {
        genrec(e2);
      }
      System.out.println("\tEND");
    } 
    else if (isNum(e.op)) {
      return Double.toString(((ASTNodeNumeric)e).value);
    } 
    else if (isStr(e.op)) {
      return "'" + ((ASTNodeString)e).string + "'";
    }
    else if (e.op == OP_SYMBOL) {
      return ((ASTNodeSymbol)e).symbol;
    }
    
    else if (e.op == OP_EQ || 
        e.op == OP_PLUS || e.op == OP_MINUS ||
        e.op == OP_MUL || e.op == OP_DIV || e.op == OP_MOD ||
        e.op == OP_AND || e.op == OP_OR || e.op == OP_XOR ||
        e.op == OP_EQ2 || e.op == OP_GT || e.op == OP_GE ||
        e.op == OP_LT || e.op == OP_LE || e.op == OP_NEQ
        ) {
      String t1 = genrec(e.operands.get(0));
      String t2 = genrec(e.operands.get(1));
      String t = genTmp();
      System.out.println(t + "\t" + t1 + " " + e.threeAddrOp() + " " + t2);
      return t;
    }
    else if (e.op == OP_FOR) {
      if (e.operands.size() == 4) {
        //for (x; y; z) {w}
        String labelForloop = genLabel() + "_forloop";
        String labelForexit = genLabel() + "_forexit";
        genrec(e.operands.get(0));
        System.out.println(labelForloop + ":");
        String cond = genrec(e.operands.get(1));
        System.out.println("\tIFFALSE " + cond + " GOTO " + labelForexit);
        genrec(e.operands.get(3));
        genrec(e.operands.get(2));
        System.out.println("\tGOTO " + labelForloop);
        System.out.println(labelForexit + ":");
      } else {
        //for (x in y) {w}
        
      }
    }

    
    else if (e.op == OP_CALL) {
      String args[] = new String[e.operands.size()];
      for (int i = 0; i < args.length; i++) {
        System.out.println("\tARG " + genrec(e.operands.get(i)));
      }
      String tmp = genTmp();
      System.out.println(tmp + "\tCALL <" + ((ASTNodeFuncCall)e).name + "> " + args.length + " args");
      return tmp;
    }

    
    
    return null;
  }
  
  String genTmp() {
    code++;
    return "t" + code;
  }
  
  String genLabel() {
    label++;
    return "_L" + label;
  }
  
  boolean isNum(int op) {
    return op == OP_NUMERICI || op == OP_NUMERICD || 
          op == OP_NUMERICH1 || op == OP_NUMERICH2 ||  
          op == OP_NUMERICB1 || op == OP_NUMERICB2;
  }
  
  boolean isStr(int op) {
    return op == OP_QUOTE1 || op == OP_QUOTE2; 
  }
  
  boolean opsAllDef(ASTNode e) {
    if (e.operands == null) return true;
    for (ASTNode e2 : e.operands) {
      if (!isDef(e2.op)) return false;
    }
    return true;
  }
  
  boolean isDef(int op) {
    return op == OP_SYMBOL || isStr(op) || isNum(op); 
  }
  
  static public void check(List<ASTNode> exprs) {
    CodeGenFront cg = new CodeGenFront();
    cg.gen(exprs);
  }

}
