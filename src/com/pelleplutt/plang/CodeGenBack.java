package com.pelleplutt.plang;

import static com.pelleplutt.plang.AST.OP_DIV;
import static com.pelleplutt.plang.AST.OP_EQ;
import static com.pelleplutt.plang.AST.OP_MINUS;
import static com.pelleplutt.plang.AST.OP_MUL;
import static com.pelleplutt.plang.AST.OP_PLUS;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pelleplutt.plang.ASTNode.ASTNodeSymbol;
import com.pelleplutt.plang.TAC.TACAlloc;
import com.pelleplutt.plang.TAC.TACCode;
import com.pelleplutt.plang.TAC.TACFloat;
import com.pelleplutt.plang.TAC.TACFree;
import com.pelleplutt.plang.TAC.TACGoto;
import com.pelleplutt.plang.TAC.TACGotoCond;
import com.pelleplutt.plang.TAC.TACInt;
import com.pelleplutt.plang.TAC.TACLabel;
import com.pelleplutt.plang.TAC.TACOp;
import com.pelleplutt.plang.TAC.TACString;
import com.pelleplutt.plang.TAC.TACVar;
import com.pelleplutt.plang.proc.ByteCode;
import com.pelleplutt.plang.proc.Processor;

public class CodeGenBack implements ByteCode {
  int sp;
  int fp;
  Map<TACVar, Integer> stackVars = new HashMap<TACVar, Integer>();
  
  public static void compile(List<Module> modules) {
    CodeGenBack cg = new CodeGenBack();
    for (Module m : modules) {
      System.out.println("  * compile module " + m.id);
      cg.compileMod(m);
    }
  }
  
  void compileMod(Module m) {
    for (ModuleFragment frag : m.frags) {
      sp = 0;
      fp = 0;
      System.out.println("  * compile frgmnt " + m.id + " " + frag.name);
      compileFrag(frag);
      byte mc[] = frag.getMachineCode();
      
      Processor p = new Processor(0, mc);
      PrintStream out = System.out;
      int pc = 0;
      int len = mc.length;
      while (len > 0) {
        String disasm = String.format("0x%08x %s", pc, p.disasm(pc)); 
        out.print(disasm);
        String com = frag.commentDbg(pc);
        if (com != null) {
          for (int i = 0; i < (34 - disasm.length()) + 2; i++) out.print(" ");
          out.print("// " + com);
        }
        out.println();
        int instr = (int)(mc[pc] & 0xff);
        int step = ISIZE[instr];
        if (step <= 0) step = 1;
        pc += step;
        len -= step;
      }

      //p.disasm(System.out, 0, mc.length);
    }
  }
  
  void compileFrag(ModuleFragment frag) {
    for (int i = 0; i < frag.frags.size(); i++) {
      TAC tac = frag.frags.get(i);
      compileTAC(tac, frag);
    }
  }
  
  void compileTAC(TAC tac, ModuleFragment frag) {
    if (tac instanceof TACAlloc) {
      // allocate stack variables
      TACAlloc all = (TACAlloc)tac;
      int spBaseOffset = sp;
      for (String varSym : all.blok.symMap.keySet()) {
        ASTNodeSymbol esym = all.blok.symMap.get(varSym);
        stackVars.put(new TACVar(esym, all.blok), spBaseOffset);
      }
      sp += all.blok.symMap.size();
      frag.addCode(all.toString() + stackInfo(), IALL, all.blok.symMap.size()-1);
    }
    else if (tac instanceof TACFree) {
      // free stack variables
      TACFree fre = (TACFree)tac;
      for (String varSym : fre.blok.symMap.keySet()) {
        ASTNodeSymbol esym = fre.blok.symMap.get(varSym);
        stackVars.remove(new TACVar(esym, fre.blok));
      }

      sp -= fre.blok.symMap.size();
      frag.addCode(tac.toString() + stackInfo(), IFRE, fre.blok.symMap.size()-1);
    }
    else if (tac instanceof TACOp) {
      compileOp((TACOp)tac, frag);
    }
    else if (tac instanceof TACLabel) {
      frag.labels.put((TACLabel)tac, frag.getPC());
    }
    else if (tac instanceof TACGoto) {
      frag.links.put(frag.getPC(), new ModuleFragment.LinkGoto(((TACGoto)tac).label));
      frag.addCode(((TACGoto)tac).label.toString() + stackInfo(), IBRA, 0xff,0xff,0xff);
    }
    else if (tac instanceof TACGotoCond) {
      TACGotoCond c = (TACGotoCond)tac;
      pushValue(c.cond, frag);
      if (c.positive) {
        frag.addCode(tac.toString() + stackInfo(), ICMP);
      } else {
        frag.addCode(tac.toString() + stackInfo(), ICMN);
      }
      frag.links.put(frag.getPC(), new ModuleFragment.LinkGoto(c.label));
      frag.addCode(c.label.toString() + stackInfo(), IBRA, 0xff,0xff,0xff);
    }
  }
  
  void compileOp(TACOp tac, ModuleFragment frag) {
    if (tac.op == OP_EQ) {
      TACVar assignee = (TACVar)tac.left;
      TAC assignment = (TAC)tac.right;
      if (stackVars.containsKey(assignee)) {
        int baseSpOffset = stackVars.get(assignee);
        int offs = sp - baseSpOffset;
        pushValue(assignment, frag);
        sp--;
        frag.addCode(tac.toString() + stackInfo(), ISTF, offs);
      } else {
        pushValue(assignment, frag);
        frag.links.put(frag.getPC(), new ModuleFragment.LinkVar(assignee));
        sp--;
        frag.addCode(tac.toString() + stackInfo(), ISTI, 0,0,0);
      }
    }
    if (tac.op == OP_PLUS) {
      pushValue(tac.left, frag);
      pushValue(tac.right, frag);
      sp = sp - 2 + 1;
      frag.addCode(tac.toString() + stackInfo(), IADD);
    }
    if (tac.op == OP_MINUS) {
      pushValue(tac.left, frag);
      pushValue(tac.right, frag);
      sp = sp - 2 + 1;
      frag.addCode(tac.toString() + stackInfo(), ISUB);
    }
    if (tac.op == OP_MUL) {
      pushValue(tac.left, frag);
      pushValue(tac.right, frag);
      sp = sp - 2 + 1;
      frag.addCode(tac.toString() + stackInfo(), IMUL);
    }
    if (tac.op == OP_DIV) {
      pushValue(tac.left, frag);
      pushValue(tac.right, frag);
      sp = sp - 2 + 1;
      frag.addCode(tac.toString() + stackInfo(), IDIV);
    }
  }
  
  void pushValue(TAC a, ModuleFragment frag) {
    
    if (a instanceof TACInt) {
      if (((TACInt)a).x >= -128 && ((TACInt)a).x <= 127) {
        // push immediate
        sp++;
        frag.addCode(a.toString() + stackInfo(), IPUI, ((TACInt)a).x);
      } else {
        frag.links.put(frag.getPC(), new ModuleFragment.LinkConst(a));
        sp++;
        frag.addCode(a.toString() + stackInfo(), ILDI, 1,0,0);
      }
    } else if (a instanceof TACVar) {
      if (stackVars.containsKey(a)) {
        int baseSpOffset = stackVars.get(a);
        int offs = sp - baseSpOffset;
        sp++;
        frag.addCode(a.toString() + stackInfo(), ILDF, offs);
      } else {
        frag.links.put(frag.getPC(), new ModuleFragment.LinkVar((TACVar)a));
        sp++;
        frag.addCode(a.toString() + stackInfo(), ILDI, 0,0,0);
      }
    }
    // TODO
    else if (a instanceof TACFloat || a instanceof TACString || a instanceof TACCode) {
      // TODO RANGE?
      frag.links.put(frag.getPC(), new ModuleFragment.LinkConst(a));
      sp++;
      frag.addCode(a.toString() + stackInfo(), ILDI, 2,0,0);
    } else if (a instanceof TACOp) {
      // supposed to be on stack
    }
      
  }
  
  String stackInfo() {
    return "\t\tsp=" + sp + " fp=" + fp;
  }
}
