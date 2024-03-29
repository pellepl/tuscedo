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
  static final int FLAG_DANGLING = (1 << 0); // if no sub match, this is a leaf
  static final int FLAG_WILD_MANY = (1 << 1); // range match, 0 or more
  static final int FLAG_WILD_ONE = (1 << 2); // range match, once
  static final int FLAG_WILD_NOT = (1 << 3); // range match, negated
  static final int FLAG_USER_BIT = 4;
  static final int FLAG_USERL = (1 << FLAG_USER_BIT); // user match set low bit
  static final int FLAG_USERH = (1 << (FLAG_USER_BIT+1)); // user match set high bit
  static final int FLAG_COMP = (1 << 6); // compound symbol

  static final int FLAG_UNIQUE_PATH_MASK = ~FLAG_DANGLING;

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
  static final int MAP_COMP = (1 << 3);
  static final int MAP_USER_BIT = 4;
  static final int MAP_USER0 = (1 << MAP_USER_BIT);
  static final int MAP_USER1 = (1 << (MAP_USER_BIT+1));
  static final int MAP_USER2 = (1 << (MAP_USER_BIT+2));
  static final int MAP_USER3 = (1 << (MAP_USER_BIT+3));

  ByteNodes root;
  ByteNodes curNodes;
  int path[];
  byte buffer[];
  int pathIx;
  int bufIx;
  boolean compoundPrev = false;
  boolean compoundDangling = false;
  byte rbuffer[];
  int rix, wix;
  Emitter emitter;
  
  boolean dbg = false;
  
  public int offset;
  
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
    rbuffer = new byte[maxSymLen+1];
    offset = 0;
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
    rix = wix = 0;
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
    markMapCharacters(chars, matchFlag);
  }

  public void defineCompoundChars(String chars) {
    markMapCharacters(chars, MAP_COMP);
  }
  
  public void markMapCharacters(String chars, int flag) {
    for (int i = 0; i < flagmap.length; i++) {
      flagmap[i] &= ~flag;
    }
    for (int i = 0; i < chars.length(); i++) {
      char c = chars.charAt(i);
      flagmap[c & 0xff] |= flag;
    }
  }

  protected void internalReset() {
    compoundDangling = false;
    compoundPrev = false; // TODO?
    pathIx = 0;
    bufIx = 0;
  }

  protected boolean isNum(int b) {
    return (flagmap[b] & MAP_NUM) != 0;
  }

  protected boolean isSpc(int b) {
    return (flagmap[b] & MAP_SPC) != 0;
  }

  protected boolean isXarg(int b) {
    return (flagmap[b] & MAP_XARG) != 0;
  }

  protected boolean isComp(int b) {
    return (flagmap[b] & MAP_COMP) != 0;
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
    createSymbol(root, sym, symid, 0);
  }

  /**
   * A compound symbol must not be neighboring other compound characters.
   * E.g. if a-z are compound characters, and "if" is a compound symbol,
   * then "what if then" will match, but "whatif then" will not match.
   * Otherwise equals to addSymbol.
   * @param sym
   * @param symid
   */
  public void addSymbolCompound(String sym, int symid) {
    if (root == null) {
      root = new ByteNodes();
    }
    createSymbol(root, sym, symid, FLAG_COMP);
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
      // First prioritize the non wildcard ones,
      // then prioritize the single wildcards,
      // finally prioritize the zero or many wildcards.
      // This is because if a wildcard precedes an actual token,
      // the wildcard will be chosen before the token, where
      // the token is more more likely to be a terminal.
      b.ids = new byte[b.lids.size()];
      b.flags = new byte[b.lids.size()];
      b.children = new ByteNodes[b.lids.size()];
      b.symids = new int[b.lids.size()];
      b.closed = new boolean[b.lids.size()];
      int entryIx = 0;
      // terminals
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
      s += (char) (b.ids[path[i]]);
      s += (b.flags[path[i]] & FLAG_COMP) != 0 ? "C" : "";
      s += " ";
      b = b.children[path[i]];
    }
    return s;
  }

  void rbufAdd(byte b) {
    rbuffer[wix++] = b;
    if (wix >= rbuffer.length) wix = 0;
    if (wix == rix) {
      rix++;
      if (rix >= rbuffer.length) rix = 0;
    }
  }
  
  int rbufLen() {
    if (rix > wix) 
      return rbuffer.length - (rix - wix);
    else {
      return wix - rix;
    }
  }
  
  void rbufRewind(int len) {
    if (len > rbufLen()) 
      throw new ArrayIndexOutOfBoundsException("rewind rbuf " + len + ", while having only " + rbufLen());
    wix -= len;
    if (wix < 0) wix += rbuffer.length;
  }
  
  int rbufPeek(int len) {
    if (len > rbufLen()) return -1;
    int ix = wix - len;
    if (ix < 0) ix += rbuffer.length;
    return rbuffer[ix];
  }
  
  
  /**
   * Parses given byte.
   * 
   * @param b
   */
  public void feed(int b) {
    offset++;
    boolean reparse;
    if (b >= 0) rbufAdd((byte)b);
    if (rbufLen() > 1) { 
      compoundPrev = isComp(rbufPeek(2));
      if (dbg) System.out.println("  prevchar '" + (char)rbufPeek(2) + "', comp " + compoundPrev);
    }
    do {
      reparse = false;
      int branchIx = pathIx == 0 ? 0 : path[pathIx - 1];
      ByteNodes branches = pathIx == 0 ? root : curNodes.children[branchIx];

      if (b < 0) {
        // finalizer byte, emit what we have buffered
        if (pathIx > 0 && (curNodes.flags[branchIx] & FLAG_DANGLING) != 0) {
          // finalized on a maybe compound dangling symbol, emit symbol
          emitter.symbol(buffer, bufIx, curNodes.symids[branchIx], offset - bufIx - 1);
        } else if (bufIx > 0) {
          // just plain data, emit data
          emitter.data(buffer, bufIx, offset - bufIx);
        }
        curNodes = null;
        reset();
        return;
      }
      
      if (compoundDangling) {
        compoundDangling = false;
        if (!isComp(b)) {
          emitter.symbol(buffer, bufIx, curNodes.symids[branchIx], offset - bufIx - 1);
          curNodes = null;
          internalReset();
          reparse = true;
          continue;
        } else {
          // TODO close compound path and retry 
        }
      }

      // store current byte
      buffer[bufIx++] = (byte) b;

      // see if current byte matches anything in current branch
      int matchIx = -1;
      boolean matchWildMany = false;
      boolean compound = false;
      if (dbg) System.out.print("'" + (char) b + "' pathIx:" + pathIx + " cur:"
          + curNodes + " brn:" + branches + " [" + printPath() + "] ");
      for (int i = 0; branches != null && i < branches.ids.length; i++) {
        if (branches.closed[i]) continue;
        byte id = branches.ids[i];
        byte flags = branches.flags[i];
        boolean wildone = (flags & FLAG_WILD_ONE) != 0;
        boolean wildmany = (flags & FLAG_WILD_MANY) != 0;
        boolean wildnot = (flags & FLAG_WILD_NOT) != 0;
        boolean comp = (flags & FLAG_COMP) != 0;
        // convert FLAG_USERH | FLAG_USERL to MAP_USERx
        int userFlagMap = 1 << (((flags & (FLAG_USERH | FLAG_USERL)) >> FLAG_USER_BIT) + MAP_USER_BIT);
        boolean match = (!wildone && !wildmany && id == b)
            || (wildone || wildmany)
            && (id == CHAR_WILDCARD || 
                id == CHAR_WILDNUM && (wildnot && !isNum(b) || !wildnot && isNum(b)) ||
                id == CHAR_WILDSPC && (wildnot && !isSpc(b)  || !wildnot && isSpc(b)) || 
                id == CHAR_WILDUSR && (wildnot && !isUserSet(b, userFlagMap) || !wildnot && isUserSet(b, userFlagMap)));
        if (bufIx == 1 && comp && compoundPrev) match = false;
        if (match) {
          // got match
          if (dbg) System.out.print("match " + (char) id + (wildone ? "?" : "") + (wildmany ? "*" : ""));
          matchIx = i;
          path[pathIx] = i;
          matchWildMany = wildmany;
          compound = comp;
          if (!matchWildMany) {
            curNodes = branches;
          }
          break;
        }
      } // per branch child id

      if (dbg) {
        if (matchIx < 0) System.out.print("no match");
        System.out.println("  buf:'" + new String(buffer, 0, bufIx) + "'");
      }

      if (!matchWildMany) {
        pathIx++;
      }

      if (!matchWildMany && matchIx >= 0 && branches.children[matchIx] == null) {
        // matched out to a non-compound leaf, emit symbol
        if (!compound) {
          curNodes = null;
          emitter.symbol(buffer, bufIx, branches.symids[matchIx], offset - bufIx);
          internalReset();
        } else {
          compoundDangling = true;
          if (dbg) System.out.println("COMPOUND DANGLING");
          break;
        }
      } else {
        if (matchIx < 0) {
          // no match
          if (curNodes != null
              && (curNodes.flags[branchIx] & FLAG_DANGLING) != 0 && bufIx > 0) {
            // intermittent leaf node, emit symbol
            emitter.symbol(buffer, bufIx - 1, curNodes.symids[branchIx], offset - bufIx);
            internalReset();
            reparse = true;
            curNodes = null;
          } else {
            if (bufIx == 1) {
              // no match on this byte, emit data
              curNodes = null;
              emitter.data(buffer, 1, offset - 1);
              internalReset();
            } else if (bufIx > 1) {
              // branch broken, reparse buffered data
              int closedIx = branchIx;
              ByteNodes closedEntryNodes = curNodes;
              // optimise: if this is a leaf on a straight path then search up to nearest 
              // bifurcation and close that node instead so we minimise retraversal
              while (closedEntryNodes.parent != null && 
                     closedEntryNodes.parent.parent != null && // never close root 
                     closedEntryNodes.parent.children.length == 1) {
                closedEntryNodes = closedEntryNodes.parent;
                closedIx = 0;
              }
              int len = bufIx;
              internalReset();
              curNodes = null;
              int ooffset = offset;
              closedEntryNodes.closed[closedIx] = true;
              if (dbg) {
                System.out.print("refeed "+ len + " [" + new String(buffer, 0, len)  + "], close "); printTreeNode(closedEntryNodes, closedIx);
                System.out.println();
              }
              compoundPrev = rbufLen() < len+1 ? false : isComp(rbufPeek(len+1));
              if (dbg) System.out.println("  rewind " + len + " to char " + (char)rbufPeek(len+1) + ", comp " + compoundPrev);
              offset -= len;
              rbufRewind(len);
              feed(buffer, 0, len);
              closedEntryNodes.closed[closedIx] = false;
              offset = ooffset;
              if (dbg) {
                System.out.print("refed  " + len + ",reopen "); printTreeNode(closedEntryNodes, closedIx);
                System.out.println();
              }
              break;
            }
          }
        } else {
          // match in midst of branch
          if (dbg) System.out.println("  '" + (char)b + "' match, path " + printPath());
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

  protected void createSymbol(ByteNodes cur, String sym, int symid, int xtraFlags) {
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
      byte nodeflags = (byte)xtraFlags;
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
            nodeflags |= (userIx << FLAG_USER_BIT) & (FLAG_USERH | FLAG_USERL);
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
          createSymbol(cur, sym.substring(symix+1), symid, xtraFlags);
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
          createSymbol(cur, sym.substring(symix+1), symid, xtraFlags);
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
      if (id == nodeid && (nodeflags & FLAG_UNIQUE_PATH_MASK) == (flags & FLAG_UNIQUE_PATH_MASK)) {
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
          // current branch reached a leaf, but this symbol keeps on - mark
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
      id += (char)((int)'0' + (int)((x & (FLAG_USERL | FLAG_USERH) >> FLAG_USER_BIT))); 
    }
    if ((bn.flags[i] & (FLAG_WILD_ONE | FLAG_WILD_MANY)) != 0)
      id += ")";

    String post = "";
    if ((bn.flags[i] & FLAG_WILD_ONE) != 0)
      post += "?";
    else if ((bn.flags[i] & FLAG_WILD_MANY) != 0)
      post += "*";
    if ((bn.flags[i] & FLAG_COMP) != 0)
      post += "C";
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
    void data(byte data[], int len, int offset);

    void symbol(byte symdata[], int len, int sym, int offset);
  } // interface Emitter

  public static void main(String[] args) {
    Lexer.Emitter emitter = new Lexer.Emitter() {
      @Override
      public void data(byte data[], int len, int offset) {
        System.out.print(new String(data, 0, len));
      }

      @Override
      public void symbol(byte data[], int len, int sym, int offset) {
        System.out.print("[" + new String(data, 0, len) + ":" + sym + "]");
      }
    };
    Lexer p = new Lexer(emitter, 256);
    p.defineCompoundChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");
    p.addSymbol("a*%b", 10);
    p.addSymbol("a1234567b", 11);
    p.addSymbolCompound("a12345", 22);
    p.compile();
    p.printTree();
    p.feed((
        //"a12345678b a1234567b a12341111118b " +
        "a12345 a123a12345 a12345 a12345"
        ).getBytes());
    p.flush();
  }
}
