package com.pelleplutt.plang;

import com.pelleplutt.plang.ASTNode.ASTNodeBlok;
import com.pelleplutt.plang.ASTNode.ASTNodeNumeric;
import com.pelleplutt.plang.ASTNode.ASTNodeOp;
import com.pelleplutt.plang.ASTNode.ASTNodeString;
import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;

public class ASTOptimiser {
  public static void optimise(ASTNodeBlok a) {
    // optimise constants
    System.out.println("  * constant folding");
    constFolding(a, null);
    optimiseDeadCode(a);
  }
  
  public static void constFolding(ASTNode a, ASTNode parent) {
    if (a.operands == null || a.operands.isEmpty()) return;
    for (ASTNode child : a.operands) {
      constFolding(child, a);
    }
    
    // 0*x or x*0
    if (a instanceof ASTNodeOp && a.op == AST.OP_MUL) {
      for (ASTNode arg : a.operands) {
        if (arg instanceof ASTNodeNumeric && ((ASTNodeNumeric)arg).value == 0) {
          ASTNode rNode = new ASTNodeNumeric(0, false);
          parent.operands.set(parent.operands.indexOf(a), rNode);
          return;
        }
      }
    }
    // 0/x or 0%x
    if (a instanceof ASTNodeOp && (a.op == AST.OP_DIV || a.op == AST.OP_MOD)) {
      ASTNode arg = a.operands.get(0);
      if (arg instanceof ASTNodeNumeric && ((ASTNodeNumeric)arg).value == 0) {
        ASTNode rNode = new ASTNodeNumeric(0, false);
        parent.operands.set(parent.operands.indexOf(a), rNode);
        return;
      }
    }
    
    // x/1
    if (a instanceof ASTNodeOp && (a.op == AST.OP_DIV)) {
      ASTNode arg = a.operands.get(1);
      if (arg instanceof ASTNodeNumeric && ((ASTNodeNumeric)arg).value == 1.0) {
        ASTNode rNode = a.operands.get(0);
        parent.operands.set(parent.operands.indexOf(a), rNode);
        return;
      }
    }
    
    // x-x
    if (a instanceof ASTNodeOp && a.op == AST.OP_MINUS) {
      ASTNode arg1 = a.operands.get(0);
      ASTNode arg2 = a.operands.get(1);
      if (arg1 instanceof ASTNodeSymbol && arg2 instanceof ASTNodeSymbol && 
          ((ASTNodeSymbol)arg1).symbol.equals(((ASTNodeSymbol)arg2).symbol)) {
        ASTNode rNode = new ASTNodeNumeric(0, false);
        parent.operands.set(parent.operands.indexOf(a), rNode);
        return;
      }
    }
    
    // x-0
    if (a instanceof ASTNodeOp && a.op == AST.OP_MINUS) {
      ASTNode arg = a.operands.get(1);
      if (arg instanceof ASTNodeNumeric && ((ASTNodeNumeric)arg).value == 0) {
        ASTNode rNode = a.operands.get(0);
        parent.operands.set(parent.operands.indexOf(a), rNode);
        return;
      }
    }
    
    // x+0 or 0+x
    if (a instanceof ASTNodeOp && a.op == AST.OP_PLUS) {
      ASTNode arg1 = a.operands.get(0);
      ASTNode arg2 = a.operands.get(1);
      if (arg1 instanceof ASTNodeNumeric && ((ASTNodeNumeric)arg1).value == 0) {
        ASTNode rNode = a.operands.get(1);
        parent.operands.set(parent.operands.indexOf(a), rNode);
        return;
      } else if (arg2 instanceof ASTNodeNumeric && ((ASTNodeNumeric)arg2).value == 0) {
        ASTNode rNode = a.operands.get(0);
        parent.operands.set(parent.operands.indexOf(a), rNode);
        return;
      }
    }
    
    boolean opNumericChildsOnly = false;
    if (a instanceof ASTNodeOp && a.operands != null && !a.operands.isEmpty()) {
      ASTNodeOp opNode = (ASTNodeOp)a;
      opNumericChildsOnly = true;
      for (ASTNode arg : opNode.operands) {
        if (!(arg instanceof ASTNodeNumeric)) {
          opNumericChildsOnly = false;
          break;
        }
      }
    }
    
    if (opNumericChildsOnly) {
      ASTNodeOp opNode = (ASTNodeOp)a;
      ASTNodeNumeric rNode;
      if (opNode.op == AST.OP_AND) {
        ASTNodeNumeric e1 = (ASTNodeNumeric)opNode.operands.get(0);
        ASTNodeNumeric e2 = (ASTNodeNumeric)opNode.operands.get(1);
        if (e1.frac || e2.frac) throw new CompilerError("Can only AND integers");
        int r = (int)e1.value & (int)e2.value; 
        rNode = new ASTNodeNumeric(r, false);
        parent.operands.set(parent.operands.indexOf(a), rNode);
      }
      if (opNode.op == AST.OP_DIV) {
        ASTNodeNumeric e1 = (ASTNodeNumeric)opNode.operands.get(0);
        ASTNodeNumeric e2 = (ASTNodeNumeric)opNode.operands.get(1);
        double r = e1.value / e2.value; 
        rNode = new ASTNodeNumeric(r, e1.frac | e2.frac);
        parent.operands.set(parent.operands.indexOf(a), rNode);
      }
      if (opNode.op == AST.OP_EQ2) {
        ASTNodeNumeric e1 = (ASTNodeNumeric)opNode.operands.get(0);
        ASTNodeNumeric e2 = (ASTNodeNumeric)opNode.operands.get(1);
        int r = (e1.value == e2.value) ? 1 : 0; 
        rNode = new ASTNodeNumeric(r, false);
        parent.operands.set(parent.operands.indexOf(a), rNode);
      }
      if (opNode.op == AST.OP_MINUS) {
        ASTNodeNumeric e1 = (ASTNodeNumeric)opNode.operands.get(0);
        ASTNodeNumeric e2 = (ASTNodeNumeric)opNode.operands.get(1);
        double r = e1.value - e2.value; 
        rNode = new ASTNodeNumeric(r, e1.frac | e2.frac);
        parent.operands.set(parent.operands.indexOf(a), rNode);
      }
      if (opNode.op == AST.OP_MINUS_UNARY) {
        ASTNodeNumeric e1 = (ASTNodeNumeric)opNode.operands.get(0);
        double r = -e1.value; 
        rNode = new ASTNodeNumeric(r, e1.frac);
        parent.operands.set(parent.operands.indexOf(a), rNode);
      }
      if (opNode.op == AST.OP_MOD) {
        ASTNodeNumeric e1 = (ASTNodeNumeric)opNode.operands.get(0);
        ASTNodeNumeric e2 = (ASTNodeNumeric)opNode.operands.get(1);
        double r = e1.value % e2.value; 
        rNode = new ASTNodeNumeric(r, e1.frac | e2.frac);
        parent.operands.set(parent.operands.indexOf(a), rNode);
      }
      if (opNode.op == AST.OP_MUL) {
        ASTNodeNumeric e1 = (ASTNodeNumeric)opNode.operands.get(0);
        ASTNodeNumeric e2 = (ASTNodeNumeric)opNode.operands.get(1);
        double r = e1.value * e2.value; 
        rNode = new ASTNodeNumeric(r, e1.frac | e2.frac);
        parent.operands.set(parent.operands.indexOf(a), rNode);
      }
      if (opNode.op == AST.OP_OR) {
        ASTNodeNumeric e1 = (ASTNodeNumeric)opNode.operands.get(0);
        ASTNodeNumeric e2 = (ASTNodeNumeric)opNode.operands.get(1);
        if (e1.frac || e2.frac) throw new CompilerError("Can only OR integers");
        int r = (int)e1.value | (int)e2.value; 
        rNode = new ASTNodeNumeric(r, false);
        parent.operands.set(parent.operands.indexOf(a), rNode);
      }
      if (opNode.op == AST.OP_PLUS) {
        ASTNodeNumeric e1 = (ASTNodeNumeric)opNode.operands.get(0);
        ASTNodeNumeric e2 = (ASTNodeNumeric)opNode.operands.get(1);
        double r = e1.value + e2.value; 
        rNode = new ASTNodeNumeric(r, e1.frac | e2.frac);
        parent.operands.set(parent.operands.indexOf(a), rNode);
      }
      if (opNode.op == AST.OP_PLUS_UNARY) {
        ASTNodeNumeric e1 = (ASTNodeNumeric)opNode.operands.get(0);
        double r = e1.value; 
        rNode = new ASTNodeNumeric(r, e1.frac);
        parent.operands.set(parent.operands.indexOf(a), rNode);
      }
      if (opNode.op == AST.OP_SHLEFT) {
        ASTNodeNumeric e1 = (ASTNodeNumeric)opNode.operands.get(0);
        ASTNodeNumeric e2 = (ASTNodeNumeric)opNode.operands.get(1);
        if (e1.frac || e2.frac) throw new CompilerError("Can only shift integers");
        int r = (int)e1.value << (int)e2.value; 
        rNode = new ASTNodeNumeric(r, false);
        parent.operands.set(parent.operands.indexOf(a), rNode);
      }
      if (opNode.op == AST.OP_SHRIGHT) {
        ASTNodeNumeric e1 = (ASTNodeNumeric)opNode.operands.get(0);
        ASTNodeNumeric e2 = (ASTNodeNumeric)opNode.operands.get(1);
        if (e1.frac || e2.frac) throw new CompilerError("Can only shift integers");
        int r = (int)e1.value >>> (int)e2.value; 
        rNode = new ASTNodeNumeric(r, false);
        parent.operands.set(parent.operands.indexOf(a), rNode);
      }
      if (opNode.op == AST.OP_XOR) {
        ASTNodeNumeric e1 = (ASTNodeNumeric)opNode.operands.get(0);
        ASTNodeNumeric e2 = (ASTNodeNumeric)opNode.operands.get(1);
        if (e1.frac || e2.frac) throw new CompilerError("Can only XOR integers");
        int r = (int)e1.value ^ (int)e2.value; 
        rNode = new ASTNodeNumeric(r, false);
        parent.operands.set(parent.operands.indexOf(a), rNode);
      }
    }
    else {
      boolean opStringChildsOnly = false;
      if (a instanceof ASTNodeOp && a.operands != null && !a.operands.isEmpty()) {
        ASTNodeOp opNode = (ASTNodeOp)a;
        opStringChildsOnly = true;
        for (ASTNode arg : opNode.operands) {
          if (!(arg instanceof ASTNodeString)) {
            opStringChildsOnly = false;
            break;
          }
        }
      }
      
      if (opStringChildsOnly) {
        ASTNodeOp opNode = (ASTNodeOp)a;
        ASTNodeString rNode;
        if (opNode.op == AST.OP_PLUS) {
          ASTNodeString e1 = (ASTNodeString)opNode.operands.get(0);
          ASTNodeString e2 = (ASTNodeString)opNode.operands.get(1);
          String r = e1.string + e2.string; 
          rNode = new ASTNodeString(r);
          parent.operands.set(parent.operands.indexOf(a), rNode);
        }
      }
    }

  }

  
  static final int S_NONE = 0;
  static final int S_IFNEVER = 1;
  static final int S_IFALWAYS = 2;
  
  public static void optimiseDeadCode(ASTNode parent) {
// TODO if/else redefined fix
//    if (parent.operands == null) return;
//  
//    int prevState = S_NONE;
//    for (int i = 0; i < parent.operands.size(); i++) {
//      ASTNode a = parent.operands.get(i);
//      if (a.op == AST.OP_IF) {
//        ASTNode branch = a.operands.get(0);
//        if (branch instanceof ASTNodeNumeric) {
//          if (((ASTNodeNumeric)branch).value != 0) {
//            int ix = parent.operands.indexOf(a);
//            parent.operands.set(ix, a.operands.get(1));
//            prevState = S_IFALWAYS;
//          } else {
//            parent.operands.remove(a);
//            i--;
//            prevState = S_IFNEVER;
//          }
//        }
//      }
//      else if (a.op == AST.OP_ELSEIF) {
//        if (prevState == S_IFNEVER) {
//          a.op = AST.OP_IF;
//          prevState = S_NONE;
//        } else if (prevState == S_IFALWAYS) {
//          parent.operands.remove(a);
//          i--;
//          prevState = S_NONE;
//          continue;
//        }
//        ASTNode branch = a.operands.get(0);
//        if (branch instanceof ASTNodeNumeric) {
//          if (((ASTNodeNumeric)branch).value != 0) {
//            int ix = parent.operands.indexOf(a);
//            parent.operands.set(ix, a.operands.get(1));
//            prevState = S_IFALWAYS;
//          } else {
//            parent.operands.remove(a);
//            i--;
//            prevState = S_IFNEVER;
//          }
//        }
//      }
//      else if (a.op == AST.OP_ELSE) {
//        if (prevState == S_IFNEVER) {
//          int ix = parent.operands.indexOf(a);
//          parent.operands.set(ix, a.operands.get(0));
//        } else if (prevState == S_IFALWAYS) {
//          parent.operands.remove(a);
//          i--;
//        }
//        prevState = S_NONE;
//      } 
//      else {
//        prevState = S_NONE;
//      }
//    }
//    for (ASTNode a : parent.operands) {
//      optimiseDeadCode(a);
//    }
  }
  

}
