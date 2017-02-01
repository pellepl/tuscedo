package com.pelleplutt.tuscedo;

public class RegexId {

  public static final char TOK_ANY_CHAR = '.';
  public static final char TOK_ZERO_OR_ONE = '?';
  public static final char TOK_ZERO_OR_MANY = '*';
  public static final char TOK_ONE_OR_MANY = '+';
  public static final char TOK_OR = '|';
  public static final char TOK_BRACKETO = '[';
  public static final char TOK_BRACKETC = ']';
  public static final char TOK_PARENO = '(';
  public static final char TOK_PARENC = ')';
  public static final char TOK_ESC = '\\';
  
  public RegexId() {
  }
  
  public void addRegex(String r, int id) {
    parseRegex(r);
  }
  
  void parseRegex(String r) {
    StringBuilder tok = new StringBuilder();
    boolean esc = false;
    for (int i = 0; i < r.length(); i++) {
      char c = r.charAt(i);
      
      if (esc) {
        switch (c) {
        case TOK_ANY_CHAR:
        case TOK_ZERO_OR_ONE:
        case TOK_ZERO_OR_MANY:
        case TOK_ONE_OR_MANY:
        case TOK_OR:
        case TOK_BRACKETO:
        case TOK_BRACKETC:
        case TOK_PARENO:
        case TOK_PARENC:
        case TOK_ESC:
          tok.append(c);
          break;
        case 'n':
          tok.append("\n");
          break;
        case 'r':
          tok.append("\r");
          break;
        case 't':
          tok.append("\t");
          break;
        case 'b':
          tok.append("\b");
          break;
        case 'f':
          tok.append("\f");
          break;
        case '"':
          tok.append("\"");
          break;
        case '\'':
          tok.append("\'");
          break;
        default:
          throw new Error("bad escape character '" + c + "'");
        }
        continue;
      }
      
      switch (c) {
      case TOK_ANY_CHAR:
        break;
      case TOK_ZERO_OR_ONE:
        break;
      case TOK_ZERO_OR_MANY:
        break;
      case TOK_ONE_OR_MANY:
        break;
      case TOK_OR:
        break;
      case TOK_BRACKETO:
        break;
      case TOK_BRACKETC:
        break;
      case TOK_PARENO:
        break;
      case TOK_PARENC:
        break;
      case TOK_ESC:
        esc = true;
        break;
      default:
        break;
      }
    }
  }
  
  public interface Hit {
    void match(String r, int id);
  }
  
  class Node {
    
  }

}
