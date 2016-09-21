package com.pelleplutt.tuscedo;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple symbol parser.
 * 
 * Example of use:
 * <pre>
 * Parser.Emitter emitter = new Parser.Emitter() {
 *   @Override
 *   public void data(byte data[], int len) {
 *     System.out.print(new String(data, 0, len));
 *   }
 *   @Override
 *   public void symbol(byte data[], int len, int sym) {
 *     System.out.print("[" + new String(data, 0, len)+"]");
 *   }
 * };
 * Parser p = new Parser(emitter, 256);
 * p.addSymbol("ba*a");
 * p.addSymbol("num%;");
 * p.addSymbol("umber");
 * p.addSymbol("foo");
 * p.compile();
 * p.parse("bane banana num123; number123; foobar".getBytes());
 * p.flush();
 * </pre>
 * This will output:
 * <pre>
 * [bane ba]nana [num123;] n[umber]123; [foo]bar
 * </pre>
 * 
 * @author petera
 */
public class Parser {
  static final int FLAG_DANGLING = (1<<0);  // if no sub match, this was a leaf
  static final int FLAG_WILDCARD = (1<<1);  // match any char
  static final int FLAG_WILDNUM =  (1<<2);  // match any number 0-9
  static final int FLAG_WILDNSPC = (1<<3);  // match anything but space, tab, \r, \n
  static final int FLAG_WILDXARG = (1<<4);  // match any number 0-9 and ;

  static final char CHAR_WILDCARD = '*';
  static final char CHAR_WILDNUM = '%';
  static final char CHAR_WILDNSPC = '_';
  static final char CHAR_WILDXARG = '£';
  
  ByteNodes broot;
  int path[];
  ByteNodes curMap;
  
  Node root;
  Node nodePath[];
  
  byte buffer[];
  int pathIx;
  int bufIx;
  
  Emitter emitter;
  
  /**
   * Creates a parser with given emitter handling atmost maxSymLen
   * symbol lengths, including wildcards.
   * @param emitter
   * @param maxSymLen
   */
  public Parser(Emitter emitter, int maxSymLen) {
    this.emitter = emitter;
    path = new int[maxSymLen];
    root = new Node();
    nodePath = new Node[maxSymLen];
    buffer = new byte[maxSymLen];
  }
  
  /**
   * Sets the emitter for this parser
   * @param emitter  emitter for this parser
   */
  public void setEmitter(Emitter emitter) {
    this.emitter = emitter;
  }
  
  /**
   * Resets the parsers state
   */
  public void reset() {
    pathIx = 0;
    bufIx = 0;
    nodePath[0] = root;
  }
  
  /**
   * Transfers this compiled model tree to destination parser.
   * @param dst
   */
  public void transferModel(Parser dst) {
    dst.broot = this.broot;
    dst.reset();
  }
  
  public int getSymbolID(String sym) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int len = sym.length();
    boolean esc = false;
    for (int i = 0; i < len; i++) {
      char c = sym.charAt(i);
      if (esc) {
        checkEscChar(c);
      } else if (c == '\\') {
        esc = true;
        continue;
      }
      if (c == CHAR_WILDCARD) {
        baos.write(esc ? CHAR_WILDCARD : '0');
      } else if (!esc && c == CHAR_WILDNUM) {
        baos.write(esc ? CHAR_WILDNUM : '0');
      } else if (!esc && c == CHAR_WILDNSPC) {
        baos.write(esc ? CHAR_WILDNSPC : '0');
      } else if (!esc && c == CHAR_WILDXARG) {
        baos.write(esc ? CHAR_WILDXARG : '0');
      } else {
        baos.write((byte)c);
      }
      esc = false;
    }
    return getSymbolID(baos.toByteArray());
  }
  
  public int getSymbolID(byte[] sym) {
    Emitter e = emitter;
    __symbolID = -1;
    try {
      emitter = symbolRetreiverEmitter;
      parse(sym);
    } finally {
      emitter = e;
    }
    
    return __symbolID;
  }
  
  /**
   * Flushes unparsed buffered data and resets parser 
   */
  public void flush() {
    if (bufIx > 0) {
      emitter.data(buffer, bufIx);
    }
    reset();
  }
  
  /**
   * Parses given byte buffer.
   * @param buf
   */
  public void parse(byte buf[]) {
    for (byte b : buf) {
      parse(b);
    }
  }
  
  /**
   * Parses given byte buffer at given range.
   * @param buf
   * @param offs
   * @param len
   */
  public void parse(byte buf[], int offs, int len) {
    for (int i = offs; i < offs+len; i++) {
      parse(buf[i]);
    }
  }
  
  /**
   * Adds a symbol as string. The bytes from string will be extracted and
   * used as symbol data. Special characters are asterisk (*) for wildcard,
   * percent (%) for integer wildcard, underscore (_) for non-space wildcard,
   * pound (£) for integers and semicolon wildcard. 
   * Escape character is \. For actually putting the asterisk byte into the symbol, 
   * one specifies "sym\\*bol" in the symbol string.
   * Unended wildcards are not allowed as a match would go on forever. E.g.
   * "foo*" is not valid, but "foo*bar" is.
   * @param sym
   */
  public void addSymbol(String sym) {
    addRoot(createSymbol(null, sym));
  }
  
  /**
   * Adds a symbol as a byte array. This way wildcards are not supported.
   * @param sym
   */
  public void addSymbol(byte[] sym) {
    addRoot(createSymbol(null, sym));
  }
  
  /**
   * Compiles all symbols.
   */
  public void compile() {
    compile(true);
  }

  /**
   * Parses given byte.
   * @param b
   */
  public void parse(byte b) {
    boolean reparse;
    do {
      reparse = false;
      ByteNodes current = pathIx == 0 ? broot : curMap;
      ByteNodes branches = pathIx == 0 ? broot : curMap.children[path[pathIx-1]];
      boolean dangling = false;
      boolean wildcard = false;
      boolean wildnum = false;
      boolean wildnspc = false;
      boolean wildxarg = false;
      if (pathIx > 0) {
        byte flags = current.flags[path[pathIx-1]]; 
        dangling = (flags & FLAG_DANGLING) != 0;
        wildcard = (flags & FLAG_WILDCARD) != 0;
        wildnum = (flags & FLAG_WILDNUM) != 0;
        wildnspc = (flags & FLAG_WILDNSPC) != 0;
        wildxarg = (flags & FLAG_WILDXARG) != 0;
      }
      buffer[bufIx++] = b;
      
      int matchIx = -1;
      for (int i = 0; i < branches.ids.length; i++) {
        byte id = branches.ids[i];
        if (id == b) {
          path[pathIx] = i;
          curMap = branches;
          matchIx = i;
          break;
        }
      }
      pathIx++;
      if (matchIx >= 0 && branches.children[matchIx] == null) {
        emitter.symbol(buffer, bufIx, makePathNum(path, pathIx));
        pathIx = 0;
        bufIx = 0;
      } else {
        if (matchIx < 0) {
          if (dangling) {
            emitter.symbol(buffer, bufIx-1, makePathNum(path, pathIx-1));
            reparse = true;
            pathIx = 0;
            bufIx = 0;
          } else if (wildcard) {
            pathIx--;
          } else if (wildnum && b >= '0' && b <= '9') {
            pathIx--;
          } else if (wildnspc && b != ' ' && b != '\t' && b != '\n' && b != '\r') {
            pathIx--;
          } else if (wildxarg && (b >= '0' && b <= '9' || b == ';')) {
            pathIx--;
          } else {
            if (bufIx > 1) {
              int len = bufIx - 1;
              emitter.data(buffer, 1);
              pathIx = 0;
              bufIx = 0;
              parse(buffer, 1, len);
              break;
            } else if (bufIx == 1) {
              emitter.data(buffer, 1);
            }
            pathIx = 0;
            bufIx = 0;
          }
        }
      }
    } while (reparse);
  }
  
  /**
   * Debug purposes
   */
  protected void parseNode(byte buf[]) {
    root.id = '?';
    for (byte b : buf) {
      parseNode(b);
    }
  }
  
  /**
   * Debug purposes
   */
  protected void parseNode(byte buf[], int offs, int len) {
    root.id = '?';
    for (int i = offs; i < offs+len; i++) {
      parseNode(buf[i]);
    }
  }
  
  /**
   * Debug purposes
   */
  protected void parseNode(byte b) {
    boolean reparse;
    try {
    do {
      List<Node> branches = nodePath[pathIx].children;
      boolean dangling = nodePath[pathIx].dangling;
      boolean wildcard = nodePath[pathIx].wildcard;
      boolean wildnum = nodePath[pathIx].wildnum;
      boolean wildnspc = nodePath[pathIx].wildnspc;
      boolean wildxarg = nodePath[pathIx].wildxarg;
      reparse = false;
      Node match = null;
      buffer[bufIx++] = b;
      pathIx++;
      for (Node n : branches) {
        if (n.id == b) {
          nodePath[pathIx] = n;
          match = n;
          break;
        }
      }
      System.out.print((pathIx-1) + "," + (bufIx-1) + " " + (char)b);
      System.out.print(" C:" + nodePath[pathIx-1]); 
      System.out.print(" M:" + (match == null ? "   " : match)); 
      System.out.print((dangling ? " dang":"     ")); 
      System.out.print((wildcard ? " wild":"     ")); 
      System.out.print((wildnum ? " wnum":"     ")); 
      System.out.print((wildnspc ? " wnsp":"     ")); 
      System.out.print((wildxarg ? " wxrg":"     ")); 
      System.out.print(" ");
      if (match != null && match.children == null) {
        System.out.print("ELEAF    ");
        emitter.symbol(buffer, bufIx, makePathNum(nodePath, pathIx));
        pathIx = 0;
        bufIx = 0;
      } else {
        if (match == null) {
          if (dangling) {
            System.out.print("EDANG    ");
            emitter.symbol(buffer, bufIx-1, makePathNum(nodePath, pathIx-1));
            reparse = true;
            pathIx = 0;
            bufIx = 0;
          } else if (wildcard) {
            System.out.print("WILD     ");
            pathIx--;
          } else if (wildnum && b >= '0' && b <= '9') {
            System.out.print("WNUM     ");
            pathIx--;
          } else if (wildnspc && b != ' ' && b != '\t' && b != '\n' && b != '\r') {
            System.out.print("WNSPC   ");
            pathIx--;
          } else if (wildxarg && (b >= '0' && b <= '9' || b == ';')) {
            System.out.print("WXARG    ");
            pathIx--;
          } else {
            if (bufIx > 1) {
              int len = bufIx - 1;
              System.out.print("DATA '" + new String(""+(char)buffer[0]) + "' ");
              emitter.data(buffer, 1);
              System.out.println("\n>>> REPARS: " + new String(buffer, 1, len));
              pathIx = 0;
              bufIx = 0;
              parseNode(buffer, 1, len);
              System.out.println("<<< REPARS: " + new String(buffer, 1, len));
              break;
            } else if (bufIx == 1) {
              System.out.print("DATA '" + new String(buffer, 0, bufIx > 1 ? bufIx - 1 : bufIx) + "' ");
              emitter.data(buffer, 1);
            }
            pathIx = 0;
            bufIx = 0;
          }
        } else {
          System.out.print("MIDSYM   ");
        }
      }
      System.out.println(" reparse:" + reparse + " | ");
    } while (reparse);
    } catch (Throwable t) {
      //System.out.println("DUMP:");
      //System.out.println("pathIx:" + pathIx);
      //System.out.println("bufIx: " + bufIx);
      //System.out.println("byte:  " + (char)b);
      //System.out.print("path:  ");
      //for (Node n : nodePath) {
      //  System.out.print(n == null ? "nul":n.toString());
      //}
      //System.out.println();
      
      throw t;
    }
  }
  
  protected void checkEscChar(char c) {
    if (!(c == '\\' || 
        c == CHAR_WILDCARD || 
        c == CHAR_WILDNUM || 
        c == CHAR_WILDNSPC || 
        c == CHAR_WILDXARG)) {
      throw new Error("Bad escape sym " + c+ ", only \\" 
          + ", " + CHAR_WILDCARD 
          + ", " + CHAR_WILDNUM 
          + ", " + CHAR_WILDNSPC 
          + ", " + CHAR_WILDXARG +  
          " allowed");
    }
  }
  
  protected int makePathNum(Node[] path, int len) {
    int n = 0;
    for (int i = 0; i < len; i++) {
      n = (n*31) ^ (path[i].id);
    }
    return n;
  }
  
  protected int makePathNum(int[] path, int len) {
    int n = 0;
    for (int i = 0; i < len; i++) {
      n = (n*31) ^ (path[i]+1);
    }
    return n;
  }
  
  protected void addRoot(Node n) {
    root.addChild(n);
  }
  
  // Find nodes with same starting conditions and merges children
  protected void prune(List<Node> nodes, String path) {
    for (int masterIx = 0; masterIx < nodes.size(); masterIx++) {
      Node master = nodes.get(masterIx);
      if (master.children == null) {
        master.dangling = true;
      }
      if (master.children == null && 
          (master.wildcard || master.wildnspc || master.wildnum || master.wildxarg)) {
        throw new Error("Wildcard symbols must have a specified end");
      }

      for (int ix = masterIx+1; ix < nodes.size(); ix++) {
        Node n = nodes.get(ix);
        if (master.id == n.id){
          master.wildcard |= n.wildcard;
          master.wildnspc |= n.wildnspc;
          master.wildnum  |= n.wildnum;
          master.wildxarg |= n.wildxarg;
          nodes.remove(ix);
          ix--;
          if (n.children == null) {
            if (master.dangling) {
              throw new Error("Symbol collision, " + path + (char)(n.id));
            }
            master.dangling = true;
          } else {
            master.addChildren(n.children);
          }
        }
      }
    }
    for (Node n : nodes) {
      if (n.children != null) prune(n.children, path + (char)n.id);
    }
  }
  
  // Builds arrays instead of node trees 
  protected ByteNodes optimise(List<Node> nodes) {
    ByteNodes bn = new ByteNodes();
    optimiseBytify(bn, nodes);
    bn.children = new ByteNodes[nodes.size()];
    for (int i = 0; i < nodes.size(); i++) {
      Node n = nodes.get(i);
      if (n.children != null) {
        bn.children[i] = optimise(n.children);
      }
    }
    return bn;
  }
  
  protected void optimiseBytify(ByteNodes bn, List<Node> nodes) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (Node n : nodes) {
      baos.write(n.id  & 0xff);
    }
    bn.ids = baos.toByteArray();
    bn.flags = new byte[bn.ids.length];
    for (int i = 0; i < bn.ids.length; i++) {
      Node n = nodes.get(i);
      bn.flags[i] = (byte)(0
          | (n.dangling  ? FLAG_DANGLING : 0)
          | (n.wildcard  ? FLAG_WILDCARD : 0)
          | (n.wildnum   ? FLAG_WILDNUM  : 0)
          | (n.wildnspc  ? FLAG_WILDNSPC : 0)
          | (n.wildxarg  ? FLAG_WILDXARG : 0)
          );
    }
  }
  
  protected void compile(boolean skipNodes) {
    prune(root.children, "");
    nodePath[0] = root;
    broot = optimise(root.children);
    if (skipNodes) {
      root = null;
      nodePath = null;
    }
  }
  
  protected Node createSymbol(Node parent, Object sym) {
    Node ret = null;
    int len;
    if (sym instanceof String) {
      len = ((String)sym).length();
    } else if (sym instanceof byte[]) {
      len = ((byte[])sym).length;
    } else {
      throw new Error("Unhandled symbol type");
    }
    boolean esc = false;
    for (int i = 0; i < len; i++) {
      byte b = 0;
      char c = '_';
      if (sym instanceof String) {
        c = ((String)sym).charAt(i);
        b = (byte)(c & 0xff);
        if (esc) {
          checkEscChar(c);
        } else if (c == '\\') {
          esc = true;
          continue;
        }
      } else if (sym instanceof byte[]) {
        b = ((byte[])sym)[i];
        esc = false;
      }
      if (!esc && c == CHAR_WILDCARD) {
        parent.wildcard = true;
      } else if (!esc && c == CHAR_WILDNUM) {
        parent.wildnum = true;
      } else if (!esc && c == CHAR_WILDNSPC) {
        parent.wildnspc = true;
      } else if (!esc && c == CHAR_WILDXARG) {
        parent.wildxarg = true;
      } else {
        Node n = new Node();
        if (ret == null) ret = n;
        n.id = (byte)b;
        if (parent != null) {
          parent.addChild(n);
        }
        parent = n;
      }
      esc = false;
    }
    return ret;
  }
  
  protected void printNodeTree() {
    printNodeTree(root.children, 0);
  }
  protected void printNodeTree(List<Node> nodes, int depth) {
    if (nodes == null) return;
    for (Node n : nodes) {
      for (int i = 0; i < depth; i++) {
        System.out.print("   ");
      }
      System.out.println(n + " ");
      if (n.children != null) printNodeTree(n.children, depth + 1);
    }
  }
  
  /**
   * Debug purposes
   */
  public void printTree() {
    printTree(broot, 0);
  }
  protected void printTree(ByteNodes bn, int depth) {
    if (bn == null) return;
    for (int i = 0; i < bn.ids.length; i++) {
      byte id = bn.ids[i];
      for (int p = 0; p < depth; p++) {
        System.out.print("   ");
      }
      ByteNodes children = bn.children[i];
      System.out.println("[" + (char)id + 
          (children == null ? "]" : 
            ((bn.flags[i] & FLAG_DANGLING) != 0 ? ":" : 
            ((bn.flags[i] & FLAG_WILDCARD) != 0? CHAR_WILDCARD : 
            ((bn.flags[i] & FLAG_WILDNUM)  != 0? CHAR_WILDNUM  : 
            ((bn.flags[i] & FLAG_WILDNSPC) != 0? CHAR_WILDNSPC : 
            ((bn.flags[i] & FLAG_WILDXARG) != 0? CHAR_WILDXARG : 
                ">")))))));
      printTree(bn.children[i], depth+1);
    }
  }
  
  protected class ByteNodes {
    byte ids[];
    byte flags[];
    ByteNodes children[];
    public String toString() {
      String s = "";
      for (int i = 0; i < ids.length; i++) {
        s += (char)(ids[i] & 0xff);
      }
      return s;
    }
  } // class ByteNodes
  
  protected class Node {
    List<Node> children;
    byte id;
    boolean dangling;
    boolean wildcard;
    boolean wildnum;
    boolean wildnspc;
    boolean wildxarg;
    
    public void addChild(Node n) {
      if (children == null) children = new ArrayList<Node>();
      children.add(n);
    }
    public void addChildren(List<Node> n) {
      if (n == null) return;
      if (children == null) children = new ArrayList<Node>();
      children.addAll(n);
    }
    public String toString() {
      return "[" + (char)id + (children == null ? "]" : 
        (dangling ? ":" : 
        (wildcard ? CHAR_WILDCARD : 
        (wildnum  ? CHAR_WILDNUM  : 
        (wildnspc ? CHAR_WILDNSPC : 
        (wildxarg ? CHAR_WILDXARG : 
              ">"))))));
    }
  } // class Node
  
  public interface Emitter {
    void data(byte data[], int len);
    void symbol(byte symdata[], int len, int sym);
  } // interface Emitter
  
  int __symbolID;
  protected Emitter symbolRetreiverEmitter = new Emitter() {

    @Override
    public void data(byte[] data, int len) {}

    @Override
    public void symbol(byte[] symdata, int len, int sym) {
      __symbolID = sym;
    }
  };
/*  
  public static void main(String[] args) {
    final StringBuilder sb = new StringBuilder();
    Parser p = new Parser(new Emitter() {
      @Override
      public void data(byte[] data, int len) {
        sb.append(new String(data, 0, len));
      }
      @Override
      public void symbol(byte[] symdata, int len, int sym) {
        sb.append("[" + new String(symdata, 0, len) + "]");
      }
      
    }, 256);
    
    p.addSymbol("A%a");
    p.addSymbol("A£b");
    p.compile(false);
    p.printNodeTree();
    p.printTree();
    String s = "STARTA123aA123;123bSTOP";
    p.parseNode(s.getBytes());
    p.flush();
    System.out.println(sb);
  }
*/
}