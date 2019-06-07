package com.pelleplutt.operandi;

import java.io.PrintStream;
import java.util.Map;

import com.pelleplutt.operandi.ASTNode.ASTNodeBlok;
import com.pelleplutt.operandi.Source.SourceString;
import com.pelleplutt.operandi.proc.ExtCall;



// DONE:   ambiguity: modulename / variablename when x.key() for x - always prefer variable name in frontend
// DONE:   add 'return' to functions and anon if not explicitly set by programmer
// DONE:   add number of call arguments to stack frame, ie push pc+fp and also argc
// DONE:   in frontend, reverse argument list in order to be able to handle varargs. Fix backend for this also
// DONE:   use of variables before declaration within scope should not work, confusing 
// DONE:   ranges 
// DONE:   partialarr = oldarr[[3,4,5]], partialarr = oldarr[3#5]
// DONE:   a=[]; for(i in 0#10) {a['x'+i]='y'+i;} // inverts key/val
// DONE:   if (a) println('a true'); else println('a false');
// DONE:   { globalscope = 1; { localscope = 2; anon = { return localscope * 4; }; println(anon()); } }
// DONE:   a = 1>2;
// DONE:   arr = arrb[{if ($0 > 4) return $0; else return nil;}]; // removes all elements below 4
// DONE:   arr = arrb[{return $0*2;}]; // multiplies all elements by 2
// DONE:   arr[[1,2,3]] = 4
// DONE:   map = map[{if $0 ...etc}]
// DONE:  "r = ['a':1,'b':2,'c':3];\n" +
//        "r.b = r;\n" +
//        "println(r.b.b['b'].c);\n" +
// DONE:   i = ["f":println]; // println external func
// DONE:   handle 'global' keyword
// FIXME:  goto
// FIXME:  on $0..999, in StructAnalyser, replace these by internal variables so we do not need to check range all the time



public class Compiler {
  public static final int VERSION = 0x00000001;
  
  static Source src;
  static int stringix = 0;
  Map<String, ExtCall> extDefs;
  Linker linker;
  IntermediateRepresentation ir = new IntermediateRepresentation();
  
  public static Executable compileOnce(Map<String, ExtCall> extDefs, int ramOffs, int constOffs, String ...sources) {
    Source srcs[] = new Source[sources.length];
    int i = 0;
    for (String s : sources) {
      srcs[i++] = new Source.SourceString("<string" + (stringix++) + ">", s);
    }
    return compileOnce(extDefs, ramOffs, constOffs, srcs);
  }
  public Executable compileIncrementally(String src, Executable exe) {
    return compileIncrementally(new SourceString("<string" + (stringix++) + ">", src), exe);
  }
  public static Executable compileOnce(Map<String, ExtCall> extDefs, int ramOffs, int constOffs, Source ...sources) {
    return new Compiler(extDefs, ramOffs, constOffs).compile(sources);
  }
  public Executable compileIncrementally(Source src, Executable prevExe) {
    Executable exe = null;
    try {
      Compiler.src = src;
      ASTNodeBlok e = AST.buildTree(src.getSource());
      ASTOptimiser.optimise(e);
      Grammar.check(e);
      StructAnalysis.analyse(e, ir);
      ir = CodeGenFront.genIR(e, ir, src);
      CodeGenBack.compile(ir);
      ir.accumulateGlobals();
      linker.wipeRunOnceCode(prevExe);
      exe = linker.link(ir, extDefs, true, prevExe);
    } finally {
      if (ir != null) ir.clearModules();
    }
    return exe;
  }
  
  public Executable compile(Source ...sources) {
    for (Source osrc : sources) {
      String src = osrc.getSource();
      Compiler.src = osrc;
      ASTNodeBlok e = AST.buildTree(src);
      ASTOptimiser.optimise(e);
      Grammar.check(e);
      StructAnalysis.analyse(e, ir);
      ir = CodeGenFront.genIR(e, ir, osrc);
      CodeGenBack.compile(ir);
      ir.accumulateGlobals();
    }
    TAC.dbgResolveRefs = true;

    Executable exe = linker.link(ir, extDefs, true);

    return exe;
  }
  
  public void injectGlobalVariable(String module, String varName) {
    if (module == null) module = ".main";
    ir.injectGlobalVariable(module, varName);
    linker.injectGlobalVariable(module, varName);
  }

  public static Source getSource() {
    return src;
  }
  
  public Compiler(Map<String, ExtCall> extDefs, int ramOffs, int constOffs) {
    this.extDefs = extDefs;
    linker = new Linker(ramOffs, constOffs);
  }
  
  public void printCompilerError(PrintStream out, Source src, CompilerError ce) {
    int strstart = ce.getStringStart();
    int strend = ce.getStringEnd();
    String s = src.getCSource();
    Object[] srcinfo = src.getLine(strstart);
    String location = src.getName() + (srcinfo != null ? ("@" + srcinfo[0]) : "");
    out.println(location + " " + ce.getMessage());
    if (srcinfo != null) {
      String line = (String)srcinfo[1];
      int lineNbr = (Integer)srcinfo[0];
      int lineLen = line.length();
      int lineOffset = (Integer)srcinfo[2];
      String prefix = lineNbr + ": ";
      out.println(prefix + line);
      int lineMarkOffs = strstart - lineOffset;
      for (int i = 0; i < prefix.length() + lineMarkOffs; i++) {
        out.print(" ");
      }
      for (int i = 0; i < Math.min(lineOffset - lineLen, strend - strstart); i++) {
        out.print("~");
      }
      out.println();
    } else {
      if (strstart > 0) {
        int ps = Math.max(0, strstart - 50);
        int pe = Math.min(s.length(), strend + 50);
        out.println("... " + s.substring(ps, strstart) + 
            " -->" + s.substring(strstart, strend) + "<-- " +
            s.substring(strend, pe) + " ...");
      }

    }

  }
  
  public Linker getLinker() {
    return linker;
  }

  // from https://stackoverflow.com/questions/3537706/how-to-unescape-a-java-string-literal-in-java
  // how I love this person!
  public final static String stringify(String oldstr) {
      /*
       * In contrast to fixing Java's broken regex charclasses,
       * this one need be no bigger, as unescaping shrinks the string
       * here, where in the other one, it grows it.
       */

      StringBuffer newstr = new StringBuffer(oldstr.length());

      boolean saw_backslash = false;

      for (int i = 0; i < oldstr.length(); i++) {
          int cp = oldstr.codePointAt(i);
          if (oldstr.codePointAt(i) > Character.MAX_VALUE) {
              i++; /****WE HATES UTF-16! WE HATES IT FOREVERSES!!!****/
          }

          if (!saw_backslash) {
              if (cp == '\\') {
                  saw_backslash = true;
              } else {
                  newstr.append(Character.toChars(cp));
              }
              continue; /* switch */
          }

          if (cp == '\\') {
              saw_backslash = false;
              newstr.append('\\');
              continue; /* switch */
          }

          switch (cp) {

              case 'r':  newstr.append('\r');
                         break; /* switch */

              case 'n':  newstr.append('\n');
                         break; /* switch */

              case 'f':  newstr.append('\f');
                         break; /* switch */

              /* PASS a \b THROUGH!! */
              case 'b':  newstr.append("\\b");
                         break; /* switch */

              case 't':  newstr.append('\t');
                         break; /* switch */

              case 'a':  newstr.append('\007');
                         break; /* switch */

              case 'e':  newstr.append('\033');
                         break; /* switch */

              /*
               * A "control" character is what you get when you xor its
               * codepoint with '@'==64.  This only makes sense for ASCII,
               * and may not yield a "control" character after all.
               *
               * Strange but true: "\c{" is ";", "\c}" is "=", etc.
               */
              case 'c':   {
                  if (++i == oldstr.length()) { break; /*("trailing \\c");*/ }
                  cp = oldstr.codePointAt(i);
                  /*
                   * don't need to grok surrogates, as next line blows them up
                   */
                  if (cp > 0x7f) { break; /*die("expected ASCII after \\c");*/ }
                  newstr.append(Character.toChars(cp ^ 64));
                  break; /* switch */
              }

              case '8':
              case '9': break; /*die("illegal octal digit");*/
                        /* NOTREACHED */

      /*
       * may be 0 to 2 octal digits following this one
       * so back up one for fallthrough to next case;
       * unread this digit and fall through to next case.
       */
              case '1':
              case '2':
              case '3':
              case '4':
              case '5':
              case '6':
              case '7': --i;
                        /* FALLTHROUGH */

              /*
               * Can have 0, 1, or 2 octal digits following a 0
               * this permits larger values than octal 377, up to
               * octal 777.
               */
              case '0': {
                  if (i+1 == oldstr.length()) {
                      /* found \0 at end of string */
                      newstr.append(Character.toChars(0));
                      break; /* switch */
                  }
                  i++;
                  int digits = 0;
                  int j;
                  for (j = 0; j <= 2; j++) {
                      if (i+j == oldstr.length()) {
                          break; /* for */
                      }
                      /* safe because will unread surrogate */
                      int ch = oldstr.charAt(i+j);
                      if (ch < '0' || ch > '7') {
                          break; /* for */
                      }
                      digits++;
                  }
                  if (digits == 0) {
                      --i;
                      newstr.append('\0');
                      break; /* switch */
                  }
                  int value = 0;
                  try {
                      value = Integer.parseInt(
                                  oldstr.substring(i, i+digits), 8);
                  } catch (NumberFormatException nfe) {
                    break; /*die("invalid octal value for \\0 escape");*/
                  }
                  newstr.append(Character.toChars(value));
                  i += digits-1;
                  break; /* switch */
              } /* end case '0' */

              case 'x':  {
                  if (i+2 > oldstr.length()) {
                    break; /*die("string too short for \\x escape");*/
                  }
                  i++;
                  boolean saw_brace = false;
                  if (oldstr.charAt(i) == '{') {
                          /* ^^^^^^ ok to ignore surrogates here */
                      i++;
                      saw_brace = true;
                  }
                  int j;
                  for (j = 0; j < 8; j++) {

                      if (!saw_brace && j == 2) {
                          break;  /* for */
                      }

                      /*
                       * ASCII test also catches surrogates
                       */
                      int ch = oldstr.charAt(i+j);
                      if (ch > 127) {
                        break; /*die("illegal non-ASCII hex digit in \\x escape");*/
                      }

                      if (saw_brace && ch == '}') { break; /* for */ }

                      if (! ( (ch >= '0' && ch <= '9')
                                  ||
                              (ch >= 'a' && ch <= 'f')
                                  ||
                              (ch >= 'A' && ch <= 'F')
                            )
                         )
                      {
                        break; /*die(String.format(
                              "illegal hex digit #%d '%c' in \\x", ch, ch));*/
                      }

                  }
                  if (j == 0) { break; /*die("empty braces in \\x{} escape");*/ }
                  int value = 0;
                  try {
                      value = Integer.parseInt(oldstr.substring(i, i+j), 16);
                  } catch (NumberFormatException nfe) {
                    break; /*die("invalid hex value for \\x escape");*/
                  }
                  newstr.append(Character.toChars(value));
                  if (saw_brace) { j++; }
                  i += j-1;
                  break; /* switch */
              }

              case 'u': {
                  if (i+4 > oldstr.length()) {
                    break; /*die("string too short for \\u escape");*/
                  }
                  i++;
                  int j;
                  for (j = 0; j < 4; j++) {
                      /* this also handles the surrogate issue */
                      if (oldstr.charAt(i+j) > 127) {
                        break; /*die("illegal non-ASCII hex digit in \\u escape");*/
                      }
                  }
                  int value = 0;
                  try {
                      value = Integer.parseInt( oldstr.substring(i, i+j), 16);
                  } catch (NumberFormatException nfe) {
                    break; /*die("invalid hex value for \\u escape");*/
                  }
                  newstr.append(Character.toChars(value));
                  i += j-1;
                  break; /* switch */
              }

              case 'U': {
                  if (i+8 > oldstr.length()) {
                    break; /*die("string too short for \\U escape");*/
                  }
                  i++;
                  int j;
                  for (j = 0; j < 8; j++) {
                      /* this also handles the surrogate issue */
                      if (oldstr.charAt(i+j) > 127) {
                        break; /*die("illegal non-ASCII hex digit in \\U escape");*/
                      }
                  }
                  int value = 0;
                  try {
                      value = Integer.parseInt(oldstr.substring(i, i+j), 16);
                  } catch (NumberFormatException nfe) {
                    break; /*die("invalid hex value for \\U escape");*/
                  }
                  newstr.append(Character.toChars(value));
                  i += j-1;
                  break; /* switch */
              }

              default:   newstr.append('\\');
                         newstr.append(Character.toChars(cp));
             /*
              * say(String.format(
              *       "DEFAULT unrecognized escape %c passed through",
              *       cp));
              */
                         break; /* switch */

          }
          saw_backslash = false;
      }

      return newstr.toString();
  }
}
