package com.pelleplutt.tuscedo;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple symbol lexer.
 * 
 * Example of use:
 * 
 * <pre>
 * Lexer.Emitter emitter = new Lexer.Emitter() {
 *   &#064;Override
 *   public void data(byte data[], int len) {
 *     System.out.print(new String(data, 0, len));
 *   }
 * 
 *   &#064;Override
 *   public void symbol(byte data[], int len, int sym) {
 *     System.out.print("[" + new String(data, 0, len) + "]");
 *   }
 * };
 * Lexer p = new Lexer(emitter, 256);
 * p.addSymbol("ba*?a");
 * p.addSymbol("num*%;");
 * p.addSymbol("umber");
 * p.addSymbol("foo");
 * p.compile();
 * p.feed("bane banana num123; number123; foobar".getBytes());
 * p.flush();
 * </pre>
 * 
 * This will output:
 * 
 * <pre>
 * [bane ba]nana [num123;] n[umber]123; [foo]bar
 * </pre>
 * 
 * @author petera
 */
public class Lexer {
  static final int FLAG_DANGLING = (1 << 0); // if no sub match, this was a leaf
  static final int FLAG_WILD_MANY = (1 << 1); // range match, 0 or more
  static final int FLAG_WILD_ONE = (1 << 2); // range match, once
  static final int FLAG_WILD_NOT = (1 << 3); // range match, negated
  static final int FLAG_USER0 = (1 << 4); // user match set 1
  static final int FLAG_USER1 = (1 << 5); // user match set 2
  static final int FLAG_USER2 = (1 << 6); // user match set 3
  static final int FLAG_USER3 = (1 << 7); // user match set 4

  static final int FLAG_MASK = ~FLAG_DANGLING;

  static final char CHAR_MATCHMANY = '*';
  static final char CHAR_WILDCARD = '?';
  static final char CHAR_WILDNUM = '%';
  static final char CHAR_WILDSPC = '_';
  static final char CHAR_WILDNOT = '!';
  static final char CHAR_WILDUSR = '^';
  static final String WILDCARD_TOKENS = CHAR_WILDCARD + ", " + CHAR_WILDNUM + ", "
      + CHAR_WILDSPC + ", " + CHAR_WILDNOT + ", " + CHAR_WILDUSR;

  static final int MAP_NUM = (1 << 0);
  static final int MAP_SPC = (1 << 1);
  static final int MAP_XARG = (1 << 2);
  static final int MAP_USER0 = (1 << 4);
  static final int MAP_USER1 = (1 << 5);
  static final int MAP_USER2 = (1 << 6);
  static final int MAP_USER3 = (1 << 7);

  ByteNodes root;
  ByteNodes curNodes;
  int path[];
  byte buffer[];
  int pathIx;
  int bufIx;
  Emitter emitter;

  byte[] flagmap = new byte[256];

  /**
   * Creates a lexer with given emitter handling atmost maxSymLen symbol
   * lengths, including wildcards.
   * 
   * @param emitter
   * @param maxSymLen
   */
  public Lexer(Emitter emitter, int maxSymLen) {
    this.emitter = emitter;
    path = new int[maxSymLen];
    buffer = new byte[maxSymLen];
    for (int i = 0; i < flagmap.length; i++) {
      boolean isNum = i >= '0' && i <= '9';
      boolean isSpc = i == ' ' || i == '\t' || i == '\r' || i == '\n';
      boolean isXarg = isNum || i == ';';
      flagmap[i] = (byte) ((isNum ? MAP_NUM : 0) | (isSpc ? MAP_SPC : 0) | (isXarg ? MAP_XARG
          : 0));
    }
  }

  /**
   * Sets the emitter for this lexer
   * 
   * @param emitter
   *          emitter for this lexer
   */
  public void setEmitter(Emitter emitter) {
    this.emitter = emitter;
  }

  /**
   * Resets the lexers state
   */
  public void reset() {
    internalReset();
  }
  
  public void defineUserSet(String chars, int matchSet) {
    int matchFlag;
    switch (matchSet) {
    case 0: matchFlag = MAP_USER0; break;
    case 1: matchFlag = MAP_USER1; break;
    case 2: matchFlag = MAP_USER2; break;
    case 3: matchFlag = MAP_USER3; break;
    default: throw new Error("Bad match set (0-3)");
    }
    for (int i = 0; i < flagmap.length; i++) {
      flagmap[i] &= ~matchFlag;
    }
    for (int i = 0; i < chars.length(); i++) {
      char c = chars.charAt(i);
      flagmap[c & 0xff] |= matchFlag;
    }
    
  }

  protected void internalReset() {
    pathIx = 0;
    bufIx = 0;
  }

  protected boolean isNum(int b) {
    return (flagmap[b] & MAP_NUM) != 0;
  }

  protected boolean isSpc(int b) {
    return (flagmap[b] & MAP_SPC) != 0;
  }

  protected boolean isNspc(int b) {
    return (flagmap[b] & MAP_SPC) == 0;
  }

  protected boolean isXarg(int b) {
    return (flagmap[b] & MAP_XARG) != 0;
  }

  protected boolean isUserSet(int b, int mask) {
    return (flagmap[b] & mask) != 0;
  }

  /**
   * Transfers this compiled model tree to destination lexer.
   * 
   * @param dst
   */
  public void transferModel(Lexer dst) {
    dst.root = this.root;
    dst.flagmap = this.flagmap; 
    dst.reset();
  }

  /**
   * Flushes unparsed buffered data and resets lexer
   */
  public void flush() {
    feed(-1);
  }

  /**
   * Parses given byte buffer.
   * 
   * @param buf
   */
  public void feed(byte buf[]) {
    for (byte b : buf) {
      feed(b);
    }
  }

  /**
   * Parses given byte buffer at given range.
   * 
   * @param buf
   * @param offs
   * @param len
   */
  public void feed(byte buf[], int offs, int len) {
    for (int i = offs; i < offs + len; i++) {
      feed(buf[i]);
    }
  }

  /**
   * Adds a symbol as string.
   * The bytes from string will be extracted and used as symbol data.
   * 
   * Special characters are qustionmark (?) for any character, percent (%) for 
   * integer characters, underscore (_) for space characters, caret and integer 
   * 0 to 3 (^[0-3]) for user defined sets.
   * 
   * Function defineUserSet specifies the characters comprised by given set.
   * 
   * Escape character is backslash (\). For actually putting the questionmark
   * byte into a symbol, one specifies "sym\?bol" in the symbol string. 
   * 
   * One wildcard character will match one byte in given sequence when lexing.
   * To match zero or many characters, precede the wildcard with an asterisk, 
   * for example "sym*?bol". To match negated set, precede wildcard with an 
   * exclamation mark. E.g. to match zero or many non-integer characters, 
   * specify "*!%" as symbol.
   * 
   * To match everything but "abcde", define user set 0 as "abcde", and specify
   * symbol "*!^0". Here, '*' means zero or many, '!' means match negated, and
   * '^0' means match characters defined in user set 0.
   * 
   * @param sym     The symbol string
   * @param symid   The id that will be passed thru emitter for matched strings
   */
  public void addSymbol(String sym, int symid) {
    if (root == null) {
      root = new ByteNodes();
    }
    createSymbol(root, sym, symid);
  }

  /**
   * Compiles all symbols.
   */
  public void compile() {
    compile(root);
  }

  protected void compile(ByteNodes b) {
    if (b.lids != null && b.lids.size() > 0) {
      // Here, we sort the symbols.
      // First prioritize the non wildcard ones (definit),
      // then prioritize the single wildcards,
      // finally prioritize the zero or many wildcards.
      // This is because if a wildcard precedes a definit,
      // the wildcard will be chosen before the definit, where
      // the definit is more defined than a wildcard.
      b.ids = new byte[b.lids.size()];
      b.flags = new byte[b.lids.size()];
      b.children = new ByteNodes[b.lids.size()];
      b.symids = new int[b.lids.size()];
      b.closed = new boolean[b.lids.size()];
      int entryIx = 0;
      // definits
      for (int i = 0; i < b.ids.length; i++) {
        if ((b.lflags.get(i) & (FLAG_WILD_ONE | FLAG_WILD_MANY)) == 0) {
          b.ids[entryIx] = b.lids.get(i);
          b.flags[entryIx] = b.lflags.get(i);
          ByteNodes ch = b.lchildren.get(i);
          b.children[entryIx] = (ch instanceof NullNodes) ? null : ch;
          b.symids[entryIx] = b.lsymids.get(i);
          entryIx++;
        }
      }
      // single wildcards
      for (int i = 0; i < b.ids.length; i++) {
        if ((b.lflags.get(i) & FLAG_WILD_ONE) != 0) {
          b.ids[entryIx] = b.lids.get(i);
          b.flags[entryIx] = b.lflags.get(i);
          ByteNodes ch = b.lchildren.get(i);
          b.children[entryIx] = (ch instanceof NullNodes) ? null : ch;
          b.symids[entryIx] = b.lsymids.get(i);
          entryIx++;
        }
      }
      // zero or many wildcards
      for (int i = 0; i < b.ids.length; i++) {
        if ((b.lflags.get(i) & FLAG_WILD_MANY) != 0) {
          b.ids[entryIx] = b.lids.get(i);
          b.flags[entryIx] = b.lflags.get(i);
          ByteNodes ch = b.lchildren.get(i);
          b.children[entryIx] = (ch instanceof NullNodes) ? null : ch;
          b.symids[entryIx] = b.lsymids.get(i);
          entryIx++;
        }
      }
      // set parents
      for (int i = 0; i < b.ids.length; i++) {
        if (b.children[i] != null) b.children[i].parent = b;
      }
    }
    // remove all worker lists
    b.lids = null;
    b.lflags = null;
    b.lchildren = null;
    b.lsymids = null;
    if (b.children != null) {
      for (ByteNodes cb : b.children) {
        if (cb != null)
          compile(cb);
      }
    }
  }

  String printPath() {
    String s = "";
    ByteNodes b = root;
    for (int i = 0; i < pathIx; i++) {
      s += (char) (b.ids[path[i]]) + " ";
      b = b.children[path[i]];
    }
    return s;
  }

  /**
   * Parses given byte.
   * 
   * @param b
   */
  public void feed(int b) {
    boolean reparse;
    do {
      reparse = false;
      int branchIx = pathIx == 0 ? 0 : path[pathIx - 1];
      ByteNodes branches = pathIx == 0 ? root : curNodes.children[branchIx];

      if (b < 0) {
        // finalizer byte, emit what we have buffered
        if (pathIx > 0 && (curNodes.flags[branchIx] & FLAG_DANGLING) != 0) {
          // finalized on a dangling symbol, emit symbol
          emitter.symbol(buffer, bufIx, curNodes.symids[branchIx]);
        } else if (bufIx > 0) {
          // just plain data, emit data
          emitter.data(buffer, bufIx);
        }
        curNodes = null;
        reset();
        return;
      }

      // store current byte
      buffer[bufIx++] = (byte) b;

      // see if current byte matches anything in current branch
      int matchIx = -1;
      boolean matchWildMany = false;
//      System.out.print("'" + (char) b + "' pathIx:" + pathIx + " cur:"
//          + curNodes + " brn:" + branches + " [" + printPath() + "] ");
      for (int i = 0; i < branches.ids.length; i++) {
        if (branches.closed[i]) continue;
        byte id = branches.ids[i];
        byte flags = branches.flags[i];
        boolean wildone = (flags & FLAG_WILD_ONE) != 0;
        boolean wildmany = (flags & FLAG_WILD_MANY) != 0;
        boolean wildnot = (flags & FLAG_WILD_NOT) != 0;
        boolean match = (!wildone && !wildmany && id == b)
            || (wildone || wildmany)
            && (id == CHAR_WILDCARD || 
                id == CHAR_WILDNUM && (wildnot && !isNum(b) || !wildnot && isNum(b)) ||
                id == CHAR_WILDSPC && (wildnot && !isSpc(b)  || !wildnot && isSpc(b)) || 
                id == CHAR_WILDUSR && (wildnot && !isUserSet(b, flags&0xf0) || !wildnot && isUserSet(b, flags&0xf0)));
        if (match) {
          // got match
//          System.out.print("match " + (char) id + (wildone ? "?" : "")
//              + (wildmany ? "*" : ""));
          matchIx = i;
          path[pathIx] = i;
          matchWildMany = wildmany;
          if (!matchWildMany) {
            curNodes = branches;
            if (pathIx == 0) {
              // symComp = (flags & FLAG_COMPOUND) != 0;
            }
          }
          break;
        }
      } // per branch child id

//      if (matchIx < 0) System.out.print("no match");
//      System.out.print("  buf:'" + new String(buffer, 0, bufIx) + "'");
//      System.out.println();

      if (!matchWildMany) {
        pathIx++;
      }

      if (!matchWildMany && matchIx >= 0 && branches.children[matchIx] == null) {
        // matched out to a leaf, emit symbol
        curNodes = null;
        emitter.symbol(buffer, bufIx, branches.symids[matchIx]);
        internalReset();
      } else {
        if (matchIx < 0) {
          // no match
          if (curNodes != null
              && (curNodes.flags[branchIx] & FLAG_DANGLING) != 0 && bufIx > 0) {
            // intermittent leaf node, emit symbol
            emitter.symbol(buffer, bufIx - 1, curNodes.symids[branchIx]);
            reset();
            reparse = true;
            curNodes = null;
          } else {
            if (bufIx == 1) {
              // no match on this byte, emit data
              curNodes = null;
              emitter.data(buffer, 1);
              reset();
            } else if (bufIx > 1) {
              // branch broken, reparse buffered data
              int closedIx = branchIx;
              ByteNodes closedEntryNodes = curNodes;
              // optimise: if this is a leaf on a non-bifurcated branch then  search up to 
              // bifurcation and close that node instead so we minimise retraversal
              while (closedEntryNodes.parent != null && 
                     closedEntryNodes.parent.parent != null && // never close root 
                     closedEntryNodes.parent.children.length == 1) {
                closedEntryNodes = closedEntryNodes.parent;
                closedIx = 0;
              }
              int len = bufIx;
              reset();
              curNodes = null;
              closedEntryNodes.closed[closedIx] = true;
              //System.out.print("refeed "+ len + " [" + new String(buffer, 0, len)  + "], close "); printTreeNode(closedEntryNodes, closedIx);
              //System.out.println();
              feed(buffer, 0, len);
              closedEntryNodes.closed[closedIx] = false;
              //System.out.print("refed  " + len + ",reopen "); printTreeNode(closedEntryNodes, closedIx);
              //System.out.println();
              break;
            }
          }
        } else {
          // match in midst of branch
          //System.out.println("'" + (char)b + "' match, path " + printPath());
        }
      }
    } while (reparse);
  }

  protected void checkSpecialChar(char c) {
    if (!(c == '\\' || c == CHAR_MATCHMANY || checkEscChar(c))) {
      throw new Error("Bad escape token '" + c + "', only \\" + ", "
          + CHAR_MATCHMANY + ", " + WILDCARD_TOKENS
          + " allowed");
    }
  }

  protected boolean checkEscChar(char c) {
    return (c == CHAR_WILDCARD || c == CHAR_WILDNUM || c == CHAR_WILDSPC
        || c == CHAR_WILDNOT || c == CHAR_WILDUSR);
  }

  protected int makePathNum(int[] path, int len) {
    int n = 0;
    for (int i = 0; i < len; i++) {
      n = (n * 31) ^ (path[i] + ' ');
    }
    return n;
  }

  protected void createSymbol(ByteNodes cur, String sym, int symid) {
    int len = sym.length();
    boolean esc = false;
    boolean many = false;
    boolean negated = false;
    boolean user = false;
    int userIx = -1;

    // per character in symbol string
    for (int symix = 0; symix < len; symix++) {
      boolean isLast = (symix + 1) >= len;
      // check escape sequences, get nodeid
      char c = sym.charAt(symix);
      byte nodeid = (byte)(c & 0xff);
      if (esc) {
        checkSpecialChar(c);
      } else if (c == '\\') {
        esc = true;
        continue;
      } else if (!many && c == CHAR_MATCHMANY) {
        many = true;
        continue;
      } else if (!negated && c == CHAR_WILDNOT) {
        negated = true;
        continue;
      } else if (!user && c == CHAR_WILDUSR) {
        user = true;
        continue;
      } else if (user) {
        if (c >= '0' && c <= '3') {
          userIx = c - '0';
        } else {
          throw new Error("User token must be followed by 0,1,2 or 3");
        }
        c = CHAR_WILDUSR;
        nodeid = (byte)(c & 0xff);
      }
      
      // make flags
      byte nodeflags = 0;
      if (!esc) {
        if (checkEscChar(c)) {
          if (many) {
            nodeflags = FLAG_WILD_MANY;
          } else {
            nodeflags = FLAG_WILD_ONE;            
          }
          if (negated) {
            nodeflags |= FLAG_WILD_NOT;
          }
          if (user) {
            switch (userIx) {
            case 0: nodeflags |= FLAG_USER0; break;
            case 1: nodeflags |= FLAG_USER1; break;
            case 2: nodeflags |= FLAG_USER2; break;
            case 3: nodeflags |= FLAG_USER3; break;
            }
          }
          
        }
      }
      
      if ((nodeflags & FLAG_WILD_MANY) != 0) {
        
        // e.g. "a*?b" 
        // =>
        //        ____b___
        //       /        \
        // O-a->O-*?->O-b->X 
        //           ^ \
        //           |  ?
        //           \_/
        //       
        // =>
        // case A          case B
        // O-a->O-b->X  ,  O-a->O-?->O-b->X
        //                          ^ \
        //                          |  ?
        //                          \_/
        
        // case A
        if (!isLast) {
          // if stuff is following the wildcard, add this directly under parent
          // (case no wildcard match)
          createSymbol(cur, sym.substring(symix+1), symid);
        }
        // case B
        // add the first single char wildcard match
        cur = addToken(cur, c, nodeid, 
            (byte)((nodeflags & ~FLAG_WILD_MANY) | FLAG_WILD_ONE | (isLast ? FLAG_DANGLING : 0)), 
            false, symid);
        // then, under the single char wildcard match, add zero or many match 
        addToken(cur, c, nodeid, nodeflags, isLast, symid);
        if (!isLast) {
          // if stuff is following the wildcard, add this directly under parent
          createSymbol(cur, sym.substring(symix+1), symid);
        }
        return;
      }
      
      cur = addToken(cur, c, nodeid, nodeflags, isLast, symid);
      if (cur == null) {
        return;
      }
      
      esc = false;
      many = false;
      negated = false;
      user = false;
    }
  }

  protected ByteNodes addToken(ByteNodes cur, char c, byte nodeid,
      byte nodeflags, boolean isLast, int symid) {
    if (cur.lids == null) {
      cur.lids = new ArrayList<Byte>();
      cur.lflags = new ArrayList<Byte>();
      cur.lchildren = new ArrayList<ByteNodes>();
      cur.lsymids = new ArrayList<Integer>();
    }

    // see if there already is a matching id in current branch
    boolean match = false;
    int nodeix;
    for (nodeix = 0; nodeix < cur.lids.size(); nodeix++) {
      byte id = cur.lids.get(nodeix);
      byte flags = cur.lflags.get(nodeix);
      if (id == nodeid && (nodeflags & FLAG_MASK) == (flags & FLAG_MASK)) {
        match = true;
        break;
      }
    }

    if (!match) {

      // no match, add a new node
      nodeix = -1;
      cur.lids.add(nodeid);
      cur.lflags.add(nodeflags);
      if (!isLast) {
        // more symbol stuff, add new children
        ByteNodes newChildren = new ByteNodes();
        cur.lchildren.add(newChildren);
        cur.lsymids.add((nodeflags & FLAG_DANGLING) != 0 ? symid : 0);
        //cur.lsymids.add(symid);
        return newChildren;
      } else {
        // no more symbol stuff, fill with nullmarker and assign symbol id
        cur.lchildren.add(new NullNodes());
        cur.lsymids.add(symid);
        return null;
      }

    } else {

      // got match, have a path already
      if (isLast) {
        if (!(cur.lchildren.get(nodeix) instanceof NullNodes)) {
          // this symbol has no children, but current branch keeps on - mark
          // dangling
          cur.lflags.set(nodeix,
              (byte) (cur.lflags.get(nodeix) | FLAG_DANGLING));
          cur.lsymids.set(nodeix, symid);
        }
        return null;
      } else {
        if (cur.lchildren.get(nodeix) instanceof NullNodes) {
          // current branch has no children, but this symbol keeps on - mark
          // dangling
          cur.lflags.set(nodeix,
              (byte) (cur.lflags.get(nodeix) | FLAG_DANGLING));
          ByteNodes newChildren = new ByteNodes();
          cur.lchildren.set(nodeix, newChildren);
          return newChildren;
        } else {
          // full match, keep on
          return cur.lchildren.get(nodeix);
        }
      }
    }
  }

  /**
   * Debug purposes
   */
  public void printTree() {
    printTree(root, 0);
  }

  protected void printTree(ByteNodes bn, int depth) {
    if (bn == null)
      return;
    if (bn.ids == null)
      return;
    for (int i = 0; i < bn.ids.length; i++) {
      for (int p = 0; p < depth; p++) {
        System.out.print("   ");
      }
      printTreeNode(bn, i);
      System.out.println();
      printTree(bn.children[i], depth + 1);
    }
  }

  protected void printTreeNode(ByteNodes bn, int i) {
    String pre = "[";
    String id = "";
    if ((bn.flags[i] & (FLAG_WILD_ONE | FLAG_WILD_MANY)) != 0)
      id += "(";
    if ((bn.flags[i] & FLAG_WILD_NOT) != 0)
        id += "!";
    id += "" + (char) bn.ids[i];
    if ((bn.flags[i] & (FLAG_WILD_ONE | FLAG_WILD_MANY)) != 0 && bn.ids[i] == CHAR_WILDUSR) {
      int x = bn.flags[i] & 0xf0;
      if (x == FLAG_USER0) id += "0";
      if (x == FLAG_USER1) id += "1";
      if (x == FLAG_USER2) id += "2";
      if (x == FLAG_USER3) id += "3";
    }
    if ((bn.flags[i] & (FLAG_WILD_ONE | FLAG_WILD_MANY)) != 0)
      id += ")";

    String post = "";
    if ((bn.flags[i] & FLAG_WILD_ONE) != 0)
      post += "?";
    else if ((bn.flags[i] & FLAG_WILD_MANY) != 0)
      post += "*";
    if ((bn.flags[i] & FLAG_DANGLING) != 0)
      post += ":";
    else if (bn.children[i] != null)
      post += ">";
    else
      post += "]";
    if (bn.closed[i]) post +="X";
    post += bn.symids[i] == 0 ? "" : (" -> " + bn.symids[i]);
    System.out.print(pre + id + post);
  }

  protected class ByteNodes {
    ByteNodes parent;
    byte ids[];
    byte flags[];
    ByteNodes children[];
    int symids[];
    boolean closed[];
    List<Byte> lids;
    List<Byte> lflags;
    List<ByteNodes> lchildren;
    List<Integer> lsymids;

    public String toString() {
      String s = "";
      for (int i = 0; i < ids.length; i++) {
        s += (char) (ids[i] & 0xff);
      }
      return s;
    }
  } // class ByteNodes

  protected class NullNodes extends ByteNodes {
  } // class NullNodes

  public interface Emitter {
    void data(byte data[], int len);

    void symbol(byte symdata[], int len, int sym);
  } // interface Emitter

  public static void main(String[] args) {
    Lexer.Emitter emitter = new Lexer.Emitter() {
      @Override
      public void data(byte data[], int len) {
        System.out.print(new String(data, 0, len));
      }

      @Override
      public void symbol(byte data[], int len, int sym) {
        System.out.print("[" + new String(data, 0, len) + ":" + sym + "]");
      }
    };
    Lexer p = new Lexer(emitter, 256);
    p.addSymbol("a*%b", 10);
    p.addSymbol("a1234567b", 11);
    p.compile();
    p.printTree();
    p.feed("a12345678b a1234567b a12341111118b".getBytes());
    p.flush();

  }
}
