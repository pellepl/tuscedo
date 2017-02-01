package com.pelleplutt.operandi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.operandi.ASTNode.ASTNodeBlok;

public class Grammar {
  static final String GRAMMAR_DEF =
          "numi:          OP_NUMERICI\n" +
          "numd:          OP_NUMERICD\n" +
          "numih:         OP_NUMERICH1 OP_NUMERICH2\n" +
          "numib:         OP_NUMERICB1 OP_NUMERICB2\n" +
          "nil:           OP_NIL\n" +
          "sym:           OP_SYMBOL\n" +
          "str:           OP_QUOTE1 OP_QUOTE2\n" +
          "rel_op:        OP_EQ2 OP_GT OP_LT OP_GE OP_LE OP_NEQ OP_IN\n" +
          "expr_op_add:   OP_PLUS OP_MINUS\n" +
          "expr_op_mul:   OP_MUL OP_DIV OP_MOD\n" +
          "expr_op_log:   OP_AND OP_OR OP_XOR OP_SHLEFT OP_SHRIGHT\n" +
          "expr_op_bin:   expr_op_log expr_op_add expr_op_mul\n" +
          "expr_op_una:   OP_MINUS_UNARY OP_PLUS_UNARY OP_POSTINC OP_PREINC OP_POSTDEC OP_PREDEC OP_BNOT OP_LNOT\n" + 
          "assign:        OP_EQ\n"+
          "assign_op_add: OP_PLUSEQ OP_MINUSEQ\n" +
          "assign_op_mul: OP_DIVEQ OP_MODEQ OP_MULEQ\n" +
          "assign_op_log: OP_ANDEQ OP_OREQ OP_XOREQ OP_SHLEFTEQ OP_SHRIGHTEQ\n"+
          "assign_op:     assign_op_log assign_op_add assign_op_mul\n" +
          "dot:           OP_DOT\n" +
          "in:            OP_IN\n" +
          "range:         OP_HASH\n" +
          "call:          OP_CALL\n" +
          "blok:          OP_BLOK\n" + 
          "if:            OP_IF\n" +
          "else:          OP_ELSE\n" +
          "goto:          OP_GOTO\n" +
          "for:           OP_FOR\n" +
          "while:         OP_WHILE\n" +
          "break:         OP_BREAK\n" +
          "continue:      OP_CONTINUE\n" +
          "label:         OP_LABEL\n" +
          "module:        OP_MODULE\n" +
          "global:        OP_GLOBAL\n" +
          "funcdef:       OP_FUNCDEF\n" +
          "return:        OP_RETURN\n" +
          "bkpt:          OP_BKPT\n" +
          "arrdecl:       OP_ADECL\n" +
          "arrderef:      OP_ADEREF\n" +
          "label:         OP_LABEL\n" + 
          "tuple:         OP_TUPLE\n" +
          
          "cond_op:       if else\n" +
          "expr:          expr_op_bin expr_op_una\n" +
          "num:           numi numd numih numib\n" +
          "val:           num sym call expr rel_op dot arrdecl arrderef str tuple\n" +
          "arg:           val str code range nil\n" +
          "maparg:        arg tuple\n" +
          "op:            expr assign_op\n" +
          "jmp:           goto for while break continue goto label\n" +
          "stat:          assign global op call\n" + 
          "oper:          stat jmp cond_op\n" + 
          "condition:     rel_op val expr assign_op assign\n" +
          "code:          nil blok oper sym jmp module funcdef return bkpt arrderef\n" + 
  "";
  static final String GRAMMAR_RULES = 
          "label:         sym\n" +
          "tuple:         op | call | val | str | range | nil | assign | blok | rel_op , op | call | val | str | range | nil | assign | blok | rel_op\n" +
          "dot:           sym | dot | arrderef | call , sym\n" +
          "arrderef:      sym | dot | arrderef | call | arrdecl | str | range , val | str | range | blok | rel_op | op | return\n" +
          "assign:        sym | arrderef | dot , op | call | val | str | range | nil | assign | blok | rel_op | if\n" +
          "arrdecl:       maparg*\n" +
          "return:        op | val | str | range | nil | assign | blok | rel_op\n" +
          "assign_op:     sym | arrderef | dot , op | call | val | assign | rel_op\n" +
          "assign_op_add: sym | arrderef | dot , str\n" +
          "OP_PLUSEQ:     sym | arrderef | dot , blok\n" +
          "range:         val | range , val\n" +
          "global:        sym\n" +
          "module:        sym\n" +
          "rel_op:        val | assign | nil , val | assign | nil\n" +
          "expr_op_add:   str | val , str | val\n" +
          "expr_op_bin:   val , val\n" +
          "expr_op_una:   val\n" +
          "in:            range | sym | call | dot | arrdecl | arrderef\n" +
          "goto:          sym\n" +
          "call:          arg*\n" +
          "blok:          code*\n" +
          "for:           stat | sym , condition , stat , code\n" +
          "for:           in , code\n" +
          "in:            sym | call | dot | arrdecl | arrderef | str | range , sym | call | dot | arrdecl | arrderef | str | range\n" +
          "while:         condition , code\n" +
          "if:            condition , code | val\n" + 
          "if:            condition , code | val , else\n" + 
          "else:          code | val\n" + 
          "funcdef:       blok\n" + 
  "";
      
  Map<String, List<String>> defMap = new HashMap<String, List<String>>();
  Map<String, List<Integer>> idMap = new HashMap<String, List<Integer>>();
  // contains the rules for each operator.
  // operator = list of rules. If more than one rule per operator, any rule may apply (OR).
  // For each rule, there is a list of OperandAccepts. Each entry in the list denotes list of possible
  // operators for given operand index == list index.
  Map<Integer, List<Rule>> ruleMap = new HashMap<Integer, List<Rule>>();
  public static boolean dbg = false;
  
  void build() throws IOException {
    // read all definitions
    BufferedReader reader = new BufferedReader(new StringReader(GRAMMAR_DEF));
    String def;
    while ((def = reader.readLine()) != null) {
      parseDefs(def);
    }
    
    // resolve all definitions
    resolveDefs();
    
    // read all rules
    reader = new BufferedReader(new StringReader(GRAMMAR_RULES));
    String rule;
    while ((rule = reader.readLine()) != null) {
      parseRules(rule);
    }
    defMap = null;
    idMap = null;
  }
 
  void parseDefs(String def) {
    String key = null;
    String[] defsub = def.split("[\\s+]");
    List<String> seqDef = new ArrayList<String>();
    for (String sub : defsub) {
      sub = sub.trim();
      if (sub.length() == 0) continue;
      if (sub.endsWith(":")) {
        key = sub.substring(0, sub.length()-1);
      } else {
        seqDef.add(sub);
      }
    }
    if (seqDef.size() > 0) addDef(key, seqDef);
  }
 
  void addDef(String key, List<String> def) {
    List<String> defs = defMap.get(key);
    if (defs == null) {
      defs = new ArrayList<String>();
      defMap.put(key, defs);
    }
    defs.addAll(def);
  }
  
  void resolveDefs() {
    for (String k : defMap.keySet()) {
      List<Integer> ids = lookupOperatorIDs(k);
      idMap.put(k, ids);
    }
  }
  
  List<Integer> lookupOperatorIDs(String def) {
    List<Integer> l = new ArrayList<Integer>();
    lookupOperatorRecurse(def, l); 
    return l;
  }
  
  void lookupOperatorRecurse(String def, List<Integer> r) {
    if (def.startsWith("OP_")) {
      int op = getOpNumber(def);
      if (!r.contains(op)) {
        r.add(op);
      }
    } else {
      List<String> subDef = defMap.get(def);
      if (subDef == null) {
        System.out.println('"' + def + "\" undef");
      } else {
        for (String sdef : subDef) {
          lookupOperatorRecurse(sdef, r);
        }
      }
    }
  }
  
  void parseRules(String rule) {
    String key = null;
    String[] rulesub = rule.split("[\\s+]");
    List<OperandAccept> ruleDef = new ArrayList<OperandAccept>();
    OperandAccept oa = new OperandAccept();
    for (String sub : rulesub) {
      sub = sub.trim();
      if (sub.length() == 0) continue;
      if (sub.endsWith(":")) {
        key = sub.substring(0, sub.length()-1);
      } else if (sub.equals(",")) {
        if (oa.ops.size() > 0) ruleDef.add(oa); 
        oa = new OperandAccept();
      } else if (!sub.equals("|")) {
        if (sub.endsWith("*")) {
          oa.zeroOrMany = true;
          sub = sub.substring(0, sub.length()-1);
        }
        List<Integer> ops = idMap.get(sub); 
        for (int op : ops) {
          if (!oa.ops.contains(op))
          oa.ops.add(op);
        }
      }
    }
    if (oa.ops.size() > 0) ruleDef.add(oa);
    addRule(key, ruleDef);
  }
 
  void addRule(String key, List<OperandAccept> ruleDef) {
    List<Integer> ruleKeys = lookupOperatorIDs(key);
    for (int k : ruleKeys) {
      List<Rule> r = ruleMap.get(k); 
      if (r == null) {
        r = new ArrayList<Rule>();
        ruleMap.put(k, r);
      }
      Rule rule = new Rule(ruleDef);
      r.add(rule);
    }
  }
  
  int getOpNumber(String def) {
    int id = Integer.MIN_VALUE;
    try {
      Field f = AST.class.getDeclaredField(def);
      if (f != null) {
        id = f.getInt(null);
      }
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
      e.printStackTrace();
    }
    if (id == Integer.MIN_VALUE) {
      throw new Error("Grammar, opnumber for " + def + " not found");
    }
    return id;
  }
  
  void checkNode(ASTNode e) {
    if (e.op >= 0 && AST.OPS[e.op].operands == 0) return;
    List<Rule> rules = ruleMap.get(e.op);
    if (rules == null) {
      System.out.println("warning: no rule for op " + AST.opString(e.op));
      return;
    }

    // special case, no operands
    if (e.operands == null || e.operands.size() == 0) {
      for (Rule rule : rules) {
        if (rule.approvedOpSequence.size() == 0 || 
            (rule.approvedOpSequence.size() == 1 && rule.approvedOpSequence.get(0).zeroOrMany)) {
          // there is a rule allowing no operands
          return;
        }
      }
      throw new CompilerError("Missing operands for " + e, e);
    }
    
    // run thru all rules and see if everything matches
    if (dbg) System.out.println("CHEK: " + e);
    boolean ruleMatch = false;
    ASTNode failingOperand = null;
    for (Rule rule : rules) {
      if (dbg) System.out.println("  RULE: " + rule);
      int nodeOpIx = 0;
      int ruleOpIx = 0;
      int nodeOpLen = e.operands.size();
      int ruleOpLen = rule.approvedOpSequence.size();
      while (nodeOpIx < nodeOpLen && ruleOpIx < ruleOpLen) {
        OperandAccept oa = rule.approvedOpSequence.get(ruleOpIx);
        ASTNode opNode = e.operands.get(nodeOpIx);

        if (oa.matches(opNode.op)) {
          if (!oa.zeroOrMany) {
            ruleOpIx++;
          }
          nodeOpIx++;
        } else {
          if (oa.zeroOrMany) {
            ruleOpIx++;
          } else {
            if (dbg) System.out.println("  FAIL @ " + opNode);
            failingOperand = opNode;
            break; // rule fail
          }
        }
      }
      ruleMatch = 
          (nodeOpIx == nodeOpLen && 
          (ruleOpIx == ruleOpLen || 
            (ruleOpIx == ruleOpLen - 1 && rule.approvedOpSequence.get(ruleOpIx).zeroOrMany)
          ));
      if (ruleMatch) 
        break;
      else 
        if (dbg) System.out.println("  FAIL ops:" + nodeOpIx + " opLen:" + nodeOpLen + " ruleOps:" + ruleOpIx + " ruleLen:" + ruleOpLen);
    } // per rule
    if (!ruleMatch) {
      throw new CompilerError("Bad operands for " + e, failingOperand == null ? e:failingOperand);
    } else {
      if (dbg) System.out.println("  PASS");
    }
  }
  
  void traverseNode(ASTNode e) {
    checkNode(e);
    if (e.operands != null) {
      for (ASTNode e2 : e.operands) {
        traverseNode(e2);
      }
    }
  }
  
  public static void check(ASTNodeBlok e) {
    Grammar g = new Grammar();
    try {
      g.build();
      g.traverseNode(e);
    } catch (IOException ioe) {}
  }
  
  class Rule {
    List<OperandAccept> approvedOpSequence;
    public Rule(List<OperandAccept> oas) {
      approvedOpSequence = oas;
    }
    public void add(List<Integer> r) {
      OperandAccept oa = new OperandAccept();
      oa.ops = r;
      approvedOpSequence.add(oa);
    }
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('{');
      for (int i = 0; i < approvedOpSequence.size(); i++) {
        OperandAccept oa = approvedOpSequence.get(i);
        sb.append(oa.toString());
        if (i < approvedOpSequence.size()-1) sb.append(',');
      }
      sb.append('}');
      return sb.toString();
    }
  }
  
  class OperandAccept {
    boolean zeroOrMany;
    List<Integer> ops;
    public OperandAccept() {
      ops = new ArrayList<Integer>();
    }
    public boolean matches(int op) {
      return ops.contains(op);
    }
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('[');
      for (int i = 0; i < ops.size(); i++) {
        int op = ops.get(i);
        sb.append(AST.opString(op));
        if (i < ops.size()-1) sb.append(' ');
      }
      sb.append(']');
      if (zeroOrMany) sb.append('*');
      return sb.toString();
    }
  }
}
