package com.pelleplutt.tuscedo;

/**
 * Feed this with xterm console output. Controls will be emitted to the
 * given XtermHandler implementation.
 * 
 * @author petera
 */
public abstract class XtermStream implements Lexer.Emitter {
  
  public static final int TEXT_DEFAULT = 0;
  public static final int TEXT_BLACK = 1;
  public static final int TEXT_RED = 2;
  public static final int TEXT_GREEN = 3;
  public static final int TEXT_YELLOW = 4;
  public static final int TEXT_BLUE = 5;
  public static final int TEXT_MAGENTA = 6;
  public static final int TEXT_CYAN = 7;
  public static final int TEXT_WHITE = 8;
  public static final int TEXT_BRED = 9;
  public static final int TEXT_BGREEN = 10;
  public static final int TEXT_BYELLOW = 11;
  public static final int TEXT_BBLUE = 12;
  public static final int TEXT_BMAGENTA = 13;
  public static final int TEXT_BCYAN = 14;
  public static final int TEXT_BWHITE = 15;

  
  static Lexer model = null;
  
  private static final int PARSE_CHAR = 0;
  private static final int PARSE_CTRL = 1;

  private static final int S7C1T = 0;
  private static final int S8C1T = 1;
  
  private static final int CTRL_IND   = 0;
  private static final int CTRL_NEL   = 1;
  private static final int CTRL_HTS   = 2;
  private static final int CTRL_RI    = 3;
  private static final int CTRL_SS2   = 4;
  private static final int CTRL_SS3   = 5;
  private static final int CTRL_DCS   = 6;
  private static final int CTRL_SPA   = 7;
  private static final int CTRL_EPA   = 8;
  private static final int CTRL_SOS   = 9;
  private static final int CTRL_DECID = 10;
  private static final int CTRL_CSI   = 11;
  private static final int CTRL_ST    = 12;
  private static final int CTRL_OSC   = 13;
  private static final int CTRL_PM    = 14;
  private static final int CTRL_APC   = 15;
  
  private static final String ESC = "\u001b";

  private static final String CTRL_7BIT[] = {
      ESC + 'D',   // IND
      ESC + 'E',   // NEL
      ESC + 'H',   // HTS
      ESC + 'M',   // RI
      ESC + 'N',   // SS2
      ESC + 'O',   // SS3
      ESC + 'P',   // DCS
      ESC + 'V',   // SPA
      ESC + 'W',   // EPA
      ESC + 'X',   // SOS
      ESC + 'Z',   // DECID
      ESC + '[',   // CSI
      ESC + '\\',  // ST
      ESC + ']',   // OSC
      ESC + "\\^",   // PM
      ESC + "\\_", // APC
  };
  
  private static final String CTRL_8BIT[] = {
      "\u0084", // IND
      "\u0085", // NEL
      "\u0088", // HTS
      "\u008d", // RI
      "\u008e", // SS2
      "\u008f", // SS3
      "\u0090", // DCS
      "\u0096", // SPA
      "\u0097", // EPA
      "\u0098", // SOS
      "\u009a", // DECID
      "\u009b", // CSI
      "\u009c", // ST
      "\u009d", // OSC
      "\u009e", // PM
      "\u009f", // APC
  };
  
  static class Sym {
    String sym;
    int id;
    public Sym(String s, int i) {
      sym = s; id = i;
    }
  }
  
  static final int X_BS = 1;
  static final int X_RI = 2;
  static final int X_S7C1T = 3;
  static final int X_S8C1T = 4;
  static final int X_DECSC = 5; 
  static final int X_DECRC = 6; 
  static final int X_SGR  = 7;
  static final int X_CUP  = 8;
  static final int X_VPA  = 9;
  static final int X_CUU  = 10;
  static final int X_CUD  = 11;
  static final int X_CUF  = 12;
  static final int X_CUB  = 13;
  static final int X_CNL  = 14;
  static final int X_CPL  = 15;
  static final int X_CHA  = 16;
  static final int X_CHT  = 17;
  static final int X_EL  = 18;
  static final int X_DCH  = 19;
  static final int X_ED  = 20;
  static final int X_DECSTBM  = 21;
  static final int X_SU  = 22;
  static final int X_SD  = 23;
  static final int X_DECSET  = 24;
  static final int X_DECRST  = 25;
  static final int X_CHARSET0   = 26;
  static final int X_CHARSET1   = 27;
  
  private static final Sym CTRL_XTERM[] = {
      new Sym("\b", X_BS),             //Backspace
      new Sym(ESC + 'M', X_RI),        // RI

      new Sym(ESC + ' ' + 'F', X_S7C1T),  //7-bit controls (S7C1T).
      new Sym(ESC + ' ' + 'G', X_S8C1T),  //8-bit controls (S8C1T).
      new Sym(ESC + ' ' + 'L', -1),  //Set ANSI conformance level 1 (dpANS X3.134.1).
      new Sym(ESC + ' ' + 'M', -1),  //Set ANSI conformance level 2 (dpANS X3.134.1).
      new Sym(ESC + ' ' + 'N', -1),  //Set ANSI conformance level 3 (dpANS X3.134.1).
      new Sym(ESC + '#' + '3', -1),   //DEC double-height line, top half (DECDHL).
      new Sym(ESC + '#' + '4', -1),   //DEC double-height line, bottom half (DECDHL).
      new Sym(ESC + '#' + '5', -1),   //DEC single-width line (DECSWL).
      new Sym(ESC + '#' + '6', -1),   //DEC double-width line (DECDWL).
      new Sym(ESC + '#' + '8', -1),   //DEC Screen Alignment Test (DECALN).
      new Sym(ESC + "*^0" + '@', -1),   //Select default character set.  That is ISO 8859-1 (ISO 2022).
      new Sym(ESC + "*^0" + 'G', -1),   //Select UTF-8 character set (ISO 2022).
      /**
      C = 0  -> DEC Special Character and Line Drawing Set.
      C = <  -> DEC Supplementary (VT200).
      C = % 5  -> DEC Supplementary Graphics (VT300).
      C = >  -> DEC Technical (VT300).
      C = A  -> United Kingdom (UK).
      C = B  -> United States (USASCII).
      C = 4  -> Dutch.
      C = C  or 5  -> Finnish.
      C = R  or f  -> French.
      C = Q  or 9  -> French Canadian (VT200, VT300).
      C = K  -> German.
      C = Y  -> Italian.
      C = ` , E  or 6  -> Norwegian/Danish.
      C = % 6  -> Portuguese (VT300).
      C = Z  -> Spanish.
      C = H  or 7  -> Swedish.
      C = =  -> Swiss.
      */
      new Sym(ESC + '(' + '0', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + '<', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + "\\%" + '5', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + '>', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + 'A', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + 'B', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + '4', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + 'C', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + '5', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + 'R', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + 'f', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + 'Q', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + '9', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + 'K', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + 'Y', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + '\'', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + 'E', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + '6', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + "\\%" + '6', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + 'Z', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + 'H', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + '7', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      new Sym(ESC + '(' + '=', X_CHARSET0),   //Designate G0 Character Set (ISO 2022, VT100).
      
      new Sym(ESC + ')' + '0', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + '<', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + "\\%" + '5', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + '>', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + 'A', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + 'B', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + '4', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + 'C', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + '5', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + 'R', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + 'f', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + 'Q', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + '9', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + 'K', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + 'Y', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + '\'', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + 'E', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + '6', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + "\\%" + '6', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + 'Z', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + 'H', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + '7', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      new Sym(ESC + ')' + '=', X_CHARSET1),   //Designate G1 Character Set (ISO 2022, VT100).
      
      new Sym(ESC + "\\*" + 'C', -1),   //Designate G2 Character Set (ISO 2022, VT220).
      new Sym(ESC + '+' + 'C', -1),   //Designate G3 Character Set (ISO 2022, VT220).
      new Sym(ESC + '-' + 'C', -1),   //Designate G1 Character Set (VT300).
      new Sym(ESC + '.' + 'C', -1),   //Designate G2 Character Set (VT300).
      new Sym(ESC + '/' + 'C', -1),   //Designate G3 Character Set (VT300).
      new Sym(ESC + '6', -1),   //Back Index (DECBI), VT420 and up.
      new Sym(ESC + '7', X_DECSC),   //Save Cursor (DECSC).
      new Sym(ESC + '8', X_DECRC),   //Restore Cursor (DECRC).
      new Sym(ESC + '9', -1),   //Forward Index (DECFI), VT420 and up.
      new Sym(ESC + '=', -1),   //Application Keypad (DECKPAM).
      new Sym(ESC + '>', -1),   //Normal Keypad (DECKPNM).
      new Sym(ESC + 'F', -1),   //Cursor to lower left corner of screen.  This is enabled by the
                //hpLowerleftBugCompat resource.
      new Sym(ESC + 'c', -1),   //Full Reset (RIS).
      new Sym(ESC + 'l', -1),   //Memory Lock (per HP terminals).  Locks memory above the cursor.
      new Sym(ESC + 'm', -1),   //Memory Unlock (per HP terminals).
      new Sym(ESC + 'n', -1),   //Invoke the G2 Character Set as GL (LS2).
      new Sym(ESC + 'o', -1),   //Invoke the G3 Character Set as GL (LS3).
      new Sym(ESC + '|', -1),   //Invoke the G3 Character Set as GR (LS3R).
      new Sym(ESC + '}', -1),   //Invoke the G2 Character Set as GR (LS2R).
      new Sym(ESC + '~', -1),   //Invoke the G1 Character Set as GR (LS1R).
      
      // Device Control funcs TODO
      new Sym(CTRL_7BIT[CTRL_DCS] + "\\*" + CTRL_7BIT[CTRL_ST], -1),  
      // CSI funcs TODO
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '@', -1), //Insert Ps (Blank) Character(s) (default = 1) (ICH).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'A', X_CUU), //Cursor Up Ps Times (default = 1) (CUU).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'B', X_CUD), //Cursor Down Ps Times (default = 1) (CUD).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'C', X_CUF), //Cursor Forward Ps Times (default = 1) (CUF).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'D', X_CUB), //Cursor Backward Ps Times (default = 1) (CUB).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'E', X_CNL), //Cursor Next Line Ps Times (default = 1) (CNL).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'F', X_CPL), //Cursor Preceding Line Ps Times (default = 1) (CPL).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'G', X_CHA), //Cursor Character Absolute  [column] (default = [row,1]) (CHA).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'H', X_CUP), //Cursor Position [row;column] (default = [1,1]) (CUP).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'I', X_CHT), //Cursor Forward Tabulation Ps tab stops (default = 1) (CHT).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'J', X_ED), //Erase in Display (ED).
        /**
                    Ps = 0  -> Erase Below (default).
                    Ps = 1  -> Erase Above.
                    Ps = 2  -> Erase All.
                    Ps = 3  -> Erase Saved Lines (xterm).
                   */
      new Sym(CTRL_7BIT[CTRL_CSI] + "\\?" + "*^0" + 'J', -1), //Erase in Display (DECSED).
            /**
                    Ps = 0  -> Selective Erase Below (default).
                    Ps = 1  -> Selective Erase Above.
                    Ps = 2  -> Selective Erase All.
                    */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'K', X_EL), //Erase in Line (EL).
        /**
                    Ps = 0  -> Erase to Right (default).
                    Ps = 1  -> Erase to Left.
                    Ps = 2  -> Erase All.
                    */
      new Sym(CTRL_7BIT[CTRL_CSI] + "\\?" + "*^0" + 'K', -1), //Erase in Line (DECSEL).
            /**
                    Ps = 0  -> Selective Erase to Right (default).
                    Ps = 1  -> Selective Erase to Left.
                    Ps = 2  -> Selective Erase All.
                    */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'L', -1), //Insert Ps Line(s) (default = 1) (IL).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'M', -1), //Delete Ps Line(s) (default = 1) (DL).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'P', X_DCH), //Delete Ps Character(s) (default = 1) (DCH).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'S', X_SU), //Scroll up Ps lines (default = 1) (SU).
      new Sym(CTRL_7BIT[CTRL_CSI] + "\\?" + "*^0" + 'S', -1), //If configured to support either Sixel Graphics or ReGIS Graph-
                            //ics, xterm accepts a three-parameter control sequence, where
                            //Pi, Pa and Pv are the item, action and value.
        /**
                    Pi = 1  -> item (color registers)
                    Pa = 1  -> read the number of color registers
                    Pa = 2  -> reset the number of color registers
                    Pa = 3  -> set the number of color registers to the value Pv
                  The control sequence returns a response using the same form:
  
                     new Sym(CTRL_7BIT[CTRL_CSI] + ? Pi; Ps; Pv S
  
                  where Ps is the status:
                    Ps = 0  -> success
                    Ps = 3  -> failure
                    */
      /*new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'T',*/ //Scroll down Ps lines (default = 1) (SD).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'T', X_SD), //Initiate highlight mouse tracking.  Parameters are
                                     //[func;startx;starty;firstrow;lastrow].  See the section Mouse Tracking.
      new Sym(CTRL_7BIT[CTRL_CSI] + '>' + "*^0" + 'T', -1), //Reset one or more features of the title modes to the default
        //value.  Normally, "reset" disables the feature.  It is possi-
        //ble to disable the ability to reset features by compiling a
        //different default for the title modes into xterm.
        /**
                    Ps = 0  -> Do not set window/icon labels using hexadecimal.
                    Ps = 1  -> Do not query window/icon labels using hexadeci-
                  mal.
                    Ps = 2  -> Do not set window/icon labels using UTF-8.
                    Ps = 3  -> Do not query window/icon labels using UTF-8.
                  (See discussion of "Title Modes").
                  */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'X', -1), //Erase Ps Character(s) (default = 1) (ECH).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'Z', -1), //Cursor Backward Tabulation Ps tab stops (default = 1) (CBT).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '`', -1), //Character Position Absolute  [column] (default = [row,1]) (HPA).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'a', -1), //Character Position Relative  [columns] (default = [row,col+1]) (HPR).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'b', -1), //Repeat the preceding graphic character Ps times (REP).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'c', -1), //Send Device Attributes (Primary DA).
        /**
                    Ps = 0  or omitted -> request attributes from terminal.  The
                  response depends on the decTerminalID resource setting.
                    -> CSI ? 1 ; 2 c  ("VT100 with Advanced Video Option")
                    -> CSI ? 1 ; 0 c  ("VT101 with No Options")
                    -> CSI ? 6 c  ("VT102")
                    -> CSI ? 6 2 ; Psc  ("VT220")
                    -> CSI ? 6 3 ; Psc  ("VT320")
                    -> CSI ? 6 4 ; Psc  ("VT420")
                  The VT100-style response parameters do not mean anything by
                  themselves.  VT220 (and higher) parameters do, telling the
                  host what features the terminal supports:
                    Ps = 1  -> 132-columns.
                    Ps = 2  -> Printer.
                    Ps = 3  -> ReGIS graphics.
                    Ps = 4  -> Sixel graphics.
                    Ps = 6  -> Selective erase.
                    Ps = 8  -> User-defined keys.
                    Ps = 9  -> National Replacement Character sets.
                    Ps = 1 5  -> Technical characters.
                    Ps = 1 8  -> User windows.
                    Ps = 2 1  -> Horizontal scrolling.
                    Ps = 2 2  -> ANSI color, e.g., VT525.
                    Ps = 2 9  -> ANSI text locator (i.e., DEC Locator mode).
                    */
      new Sym(CTRL_7BIT[CTRL_CSI] + '>' + "*^0" + 'c', -1), //Send Device Attributes (Secondary DA).
        /**
                    Ps = 0  or omitted -> request the terminal's identification
                  code.  The response depends on the decTerminalID resource set-
                  ting.  It should apply only to VT220 and up, but xterm extends
                  this to VT100.
                    -> CSI  > Pp ; Pv ; Pc c
                  where Pp denotes the terminal type
                    Pp = 0  -> "VT100".
                    Pp = 1  -> "VT220".
                    Pp = 2  -> "VT240".
                    Pp = 1 8 -> "VT330".
                    Pp = 1 9 -> "VT340".
                    Pp = 2 4 -> "VT320".
                    Pp = 4 1 -> "VT420".
                    Pp = 6 1 -> "VT510".
                    Pp = 6 4 -> "VT520".
                    Pp = 6 5 -> "VT525".
                  and Pv is the firmware version (for xterm, this was originally
                  the XFree86 patch number, starting with 95).  In a DEC termi-
                  nal, Pc indicates the ROM cartridge registration number and is
                  always zero.
                  */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'd', X_VPA), //Line Position Absolute  [row] (default = [1,column]) (VPA).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'e', -1), //Line Position Relative  [rows] (default = [row+1,column]) (VPR).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'f', -1), //Horizontal and Vertical Position [row;column] (default = [1,1]) (HVP).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'g', -1), //Tab Clear (TBC).
        /**
                    Ps = 0  -> Clear Current Column (default).
                    Ps = 3  -> Clear All.
                  */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'h', -1), //Set Mode (SM).
            /**
                    Ps = 2  -> Keyboard Action Mode (AM).
                    Ps = 4  -> Insert Mode (IRM).
                    Ps = 1 2  -> Send/receive (SRM).
                    Ps = 2 0  -> Automatic Newline (LNM).
                  */
      new Sym(CTRL_7BIT[CTRL_CSI] + "\\?" + "*^0" + 'h', X_DECSET), //DEC Private Mode Set (DECSET).
            /**
                    Ps = 1  -> Application Cursor Keys (DECCKM).
                    Ps = 2  -> Designate USASCII for character sets G0-G3
                  (DECANM), and set VT100 mode.
                    Ps = 3  -> 132 Column Mode (DECCOLM).
                    Ps = 4  -> Smooth (Slow) Scroll (DECSCLM).
                    Ps = 5  -> Reverse Video (DECSCNM).
                    Ps = 6  -> Origin Mode (DECOM).
                    Ps = 7  -> Wraparound Mode (DECAWM).
                    Ps = 8  -> Auto-repeat Keys (DECARM).
                    Ps = 9  -> Send Mouse X & Y on button press.  See the sec-
                  tion Mouse Tracking.  This is the X10 xterm mouse protocol.
                    Ps = 1 0  -> Show toolbar (rxvt).
                    Ps = 1 2  -> Start Blinking Cursor (att610).
                    Ps = 1 8  -> Print form feed (DECPFF).
                    Ps = 1 9  -> Set print extent to full screen (DECPEX).
                    Ps = 2 5  -> Show Cursor (DECTCEM).
                    Ps = 3 0  -> Show scrollbar (rxvt).
                    Ps = 3 5  -> Enable font-shifting functions (rxvt).
                    Ps = 3 8  -> Enter Tektronix Mode (DECTEK).
                    Ps = 4 0  -> Allow 80 -> 132 Mode.
                    Ps = 4 1  -> more(1) fix (see curses resource).
                    Ps = 4 2  -> Enable National Replacement Character sets
                  (DECNRCM).
                    Ps = 4 4  -> Turn On Margin Bell.
                    Ps = 4 5  -> Reverse-wraparound Mode.
                    Ps = 4 6  -> Start Logging.  This is normally disabled by a
                  compile-time option.
                    Ps = 4 7  -> Use Alternate Screen Buffer.  (This may be dis-
                  abled by the titeInhibit resource).
                    Ps = 6 6  -> Application keypad (DECNKM).
                    Ps = 6 7  -> Backarrow key sends backspace (DECBKM).
                    Ps = 6 9  -> Enable left and right margin mode (DECLRMM),
                  VT420 and up.
                    Ps = 9 5  -> Do not clear screen when DECCOLM is set/reset
                  (DECNCSM), VT510 and up.
                    Ps = 1 0 0 0  -> Send Mouse X & Y on button press and
                  release.  See the section Mouse Tracking.  This is the X11
                  xterm mouse protocol.
                    Ps = 1 0 0 1  -> Use Hilite Mouse Tracking.
                    Ps = 1 0 0 2  -> Use Cell Motion Mouse Tracking.
                    Ps = 1 0 0 3  -> Use All Motion Mouse Tracking.
                    Ps = 1 0 0 4  -> Send FocusIn/FocusOut events.
                    Ps = 1 0 0 5  -> Enable UTF-8 Mouse Mode.
                    Ps = 1 0 0 6  -> Enable SGR Mouse Mode.
                    Ps = 1 0 0 7  -> Enable Alternate Scroll Mode.
                    Ps = 1 0 1 0  -> Scroll to bottom on tty output (rxvt).
                    Ps = 1 0 1 1  -> Scroll to bottom on key press (rxvt).
                    Ps = 1 0 1 5  -> Enable urxvt Mouse Mode.
                    Ps = 1 0 3 4  -> Interpret "meta" key, sets eighth bit.
                  (enables the eightBitInput resource).
                    Ps = 1 0 3 5  -> Enable special modifiers for Alt and Num-
                  Lock keys.  (This enables the numLock resource).
                    Ps = 1 0 3 6  -> Send ESC   when Meta modifies a key.  (This
                  enables the metaSendsEscape resource).
                    Ps = 1 0 3 7  -> Send DEL from the editing-keypad Delete
                  key.
                    Ps = 1 0 3 9  -> Send ESC  when Alt modifies a key.  (This
                  enables the altSendsEscape resource).
                    Ps = 1 0 4 0  -> Keep selection even if not highlighted.
                  (This enables the keepSelection resource).
                    Ps = 1 0 4 1  -> Use the CLIPBOARD selection.  (This enables
                  the selectToClipboard resource).
                    Ps = 1 0 4 2  -> Enable Urgency window manager hint when
                  Control-G is received.  (This enables the bellIsUrgent
                  resource).
                    Ps = 1 0 4 3  -> Enable raising of the window when Control-G
                  is received.  (enables the popOnBell resource).
                    Ps = 1 0 4 4  -> Reuse the most recent data copied to CLIP-
                  BOARD.  (This enables the keepClipboard resource).
                    Ps = 1 0 4 7  -> Use Alternate Screen Buffer.  (This may be
                  disabled by the titeInhibit resource).
                    Ps = 1 0 4 8  -> Save cursor as in DECSC.  (This may be dis-
                  abled by the titeInhibit resource).
                    Ps = 1 0 4 9  -> Save cursor as in DECSC and use Alternate
                  Screen Buffer, clearing it first.  (This may be disabled by
                  the titeInhibit resource).  This combines the effects of the 1
                  0 4 7  and 1 0 4 8  modes.  Use this with terminfo-based
                  applications rather than the 4 7  mode.
                    Ps = 1 0 5 0  -> Set terminfo/termcap function-key mode.
                    Ps = 1 0 5 1  -> Set Sun function-key mode.
                    Ps = 1 0 5 2  -> Set HP function-key mode.
                    Ps = 1 0 5 3  -> Set SCO function-key mode.
                    Ps = 1 0 6 0  -> Set legacy keyboard emulation (X11R6).
                    Ps = 1 0 6 1  -> Set VT220 keyboard emulation.
                    Ps = 2 0 0 4  -> Set bracketed paste mode.
                  */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'i', -1), //Media Copy (MC).
        /**
                    Ps = 0  -> Print screen (default).
                    Ps = 4  -> Turn off printer controller mode.
                    Ps = 5  -> Turn on printer controller mode.
                    Ps = 1  0  -> HTML screen dump.
                    Ps = 1  1  -> SVG screen dump.
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + "\\?" + "*^0" + 'i', -1), //Media Copy (MC, DEC-specific).
            /**
                    Ps = 1  -> Print line containing cursor.
                    Ps = 4  -> Turn off autoprint mode.
                    Ps = 5  -> Turn on autoprint mode.
                    Ps = 1  0  -> Print composed display, ignores DECPEX.
                    Ps = 1  1  -> Print all pages.
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'l', -1), //Reset Mode (RM).
        /**
                    Ps = 2  -> Keyboard Action Mode (AM).
                    Ps = 4  -> Replace Mode (IRM).
                    Ps = 1 2  -> Send/receive (SRM).
                    Ps = 2 0  -> Normal Linefeed (LNM).
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + "\\?" + "*^0" + 'l', X_DECRST), //DEC Private Mode Reset (DECRST).
            /**
                    Ps = 1  -> Normal Cursor Keys (DECCKM).
                    Ps = 2  -> Designate VT52 mode (DECANM).
                    Ps = 3  -> 80 Column Mode (DECCOLM).
                    Ps = 4  -> Jump (Fast) Scroll (DECSCLM).
                    Ps = 5  -> Normal Video (DECSCNM).
                    Ps = 6  -> Normal Cursor Mode (DECOM).
                    Ps = 7  -> No Wraparound Mode (DECAWM).
                    Ps = 8  -> No Auto-repeat Keys (DECARM).
                    Ps = 9  -> Don't send Mouse X & Y on button press.
                    Ps = 1 0  -> Hide toolbar (rxvt).
                    Ps = 1 2  -> Stop Blinking Cursor (att610).
                    Ps = 1 8  -> Don't print form feed (DECPFF).
                    Ps = 1 9  -> Limit print to scrolling region (DECPEX).
                    Ps = 2 5  -> Hide Cursor (DECTCEM).
                    Ps = 3 0  -> Don't show scrollbar (rxvt).
                    Ps = 3 5  -> Disable font-shifting functions (rxvt).
                    Ps = 4 0  -> Disallow 80 -> 132 Mode.
                    Ps = 4 1  -> No more(1) fix (see curses resource).
                    Ps = 4 2  -> Disable National Replacement Character sets
                  (DECNRCM).
                    Ps = 4 4  -> Turn Off Margin Bell.
                    Ps = 4 5  -> No Reverse-wraparound Mode.
                    Ps = 4 6  -> Stop Logging.  (This is normally disabled by a
                  compile-time option).
                    Ps = 4 7  -> Use Normal Screen Buffer.
                    Ps = 6 6  -> Numeric keypad (DECNKM).
                    Ps = 6 7  -> Backarrow key sends delete (DECBKM).
                    Ps = 6 9  -> Disable left and right margin mode (DECLRMM),
                  VT420 and up.
                    Ps = 9 5  -> Clear screen when DECCOLM is set/reset (DEC-
                  NCSM), VT510 and up.
                    Ps = 1 0 0 0  -> Don't send Mouse X & Y on button press and
                  release.  See the section Mouse Tracking.
                    Ps = 1 0 0 1  -> Don't use Hilite Mouse Tracking.
                    Ps = 1 0 0 2  -> Don't use Cell Motion Mouse Tracking.
                    Ps = 1 0 0 3  -> Don't use All Motion Mouse Tracking.
                    Ps = 1 0 0 4  -> Don't send FocusIn/FocusOut events.
                    Ps = 1 0 0 5  -> Disable UTF-8 Mouse Mode.
                    Ps = 1 0 0 6  -> Disable SGR Mouse Mode.
                    Ps = 1 0 0 7  -> Disable Alternate Scroll Mode.
                    Ps = 1 0 1 0  -> Don't scroll to bottom on tty output
                  (rxvt).
                    Ps = 1 0 1 1  -> Don't scroll to bottom on key press (rxvt).
                    Ps = 1 0 1 5  -> Disable urxvt Mouse Mode.
                    Ps = 1 0 3 4  -> Don't interpret "meta" key.  (This disables
                  the eightBitInput resource).
                    Ps = 1 0 3 5  -> Disable special modifiers for Alt and Num-
                  Lock keys.  (This disables the numLock resource).
                    Ps = 1 0 3 6  -> Don't send ESC  when Meta modifies a key.
                  (This disables the metaSendsEscape resource).
                    Ps = 1 0 3 7  -> Send VT220 Remove from the editing-keypad
                  Delete key.
                    Ps = 1 0 3 9  -> Don't send ESC  when Alt modifies a key.
                  (This disables the altSendsEscape resource).
                    Ps = 1 0 4 0  -> Do not keep selection when not highlighted.
                  (This disables the keepSelection resource).
                    Ps = 1 0 4 1  -> Use the PRIMARY selection.  (This disables
                  the selectToClipboard resource).
                    Ps = 1 0 4 2  -> Disable Urgency window manager hint when
                  Control-G is received.  (This disables the bellIsUrgent
                  resource).
                    Ps = 1 0 4 3  -> Disable raising of the window when Control-
                  G is received.  (This disables the popOnBell resource).
                    Ps = 1 0 4 7  -> Use Normal Screen Buffer, clearing screen
                  first if in the Alternate Screen.  (This may be disabled by
                  the titeInhibit resource).
                    Ps = 1 0 4 8  -> Restore cursor as in DECRC.  (This may be
                  disabled by the titeInhibit resource).
                    Ps = 1 0 4 9  -> Use Normal Screen Buffer and restore cursor
                  as in DECRC.  (This may be disabled by the titeInhibit
                  resource).  This combines the effects of the 1 0 4 7  and 1 0
                  4 8  modes.  Use this with terminfo-based applications rather
                  than the 4 7  mode.
                    Ps = 1 0 5 0  -> Reset terminfo/termcap function-key mode.
                    Ps = 1 0 5 1  -> Reset Sun function-key mode.
                    Ps = 1 0 5 2  -> Reset HP function-key mode.
                    Ps = 1 0 5 3  -> Reset SCO function-key mode.
                    Ps = 1 0 6 0  -> Reset legacy keyboard emulation (X11R6).
                    Ps = 1 0 6 1  -> Reset keyboard emulation to Sun/PC style.
                    Ps = 2 0 0 4  -> Reset bracketed paste mode.
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'm', X_SGR), //Character Attributes (SGR).
        /**
                    Ps = 0  -> Normal (default).
                    Ps = 1  -> Bold.
                    Ps = 2  -> Faint, decreased intensity (ISO 6429).
                    Ps = 3  -> Italicized (ISO 6429).
                    Ps = 4  -> Underlined.
                    Ps = 5  -> Blink (appears as Bold).
                    Ps = 7  -> Inverse.
                    Ps = 8  -> Invisible, i.e., hidden (VT300).
                    Ps = 9  -> Crossed-out characters (ISO 6429).
                    Ps = 2 1  -> Doubly-underlined (ISO 6429).
                    Ps = 2 2  -> Normal (neither bold nor faint).
                    Ps = 2 3  -> Not italicized (ISO 6429).
                    Ps = 2 4  -> Not underlined.
                    Ps = 2 5  -> Steady (not blinking).
                    Ps = 2 7  -> Positive (not inverse).
                    Ps = 2 8  -> Visible, i.e., not hidden (VT300).
                    Ps = 2 9  -> Not crossed-out (ISO 6429).
                    Ps = 3 0  -> Set foreground color to Black.
                    Ps = 3 1  -> Set foreground color to Red.
                    Ps = 3 2  -> Set foreground color to Green.
                    Ps = 3 3  -> Set foreground color to Yellow.
                    Ps = 3 4  -> Set foreground color to Blue.
                    Ps = 3 5  -> Set foreground color to Magenta.
                    Ps = 3 6  -> Set foreground color to Cyan.
                    Ps = 3 7  -> Set foreground color to White.
                    Ps = 3 9  -> Set foreground color to default (original).
                    Ps = 4 0  -> Set background color to Black.
                    Ps = 4 1  -> Set background color to Red.
                    Ps = 4 2  -> Set background color to Green.
                    Ps = 4 3  -> Set background color to Yellow.
                    Ps = 4 4  -> Set background color to Blue.
                    Ps = 4 5  -> Set background color to Magenta.
                    Ps = 4 6  -> Set background color to Cyan.
                    Ps = 4 7  -> Set background color to White.
                    Ps = 4 9  -> Set background color to default (original).
  
                  If 16-color support is compiled, the following apply.  Assume
                  that xterm's resources are set so that the ISO color codes are
                  the first 8 of a set of 16.  Then the aixterm colors are the
                  bright versions of the ISO colors:
                    Ps = 9 0  -> Set foreground color to Black.
                    Ps = 9 1  -> Set foreground color to Red.
                    Ps = 9 2  -> Set foreground color to Green.
                    Ps = 9 3  -> Set foreground color to Yellow.
                    Ps = 9 4  -> Set foreground color to Blue.
                    Ps = 9 5  -> Set foreground color to Magenta.
                    Ps = 9 6  -> Set foreground color to Cyan.
                    Ps = 9 7  -> Set foreground color to White.
                    Ps = 1 0 0  -> Set background color to Black.
                    Ps = 1 0 1  -> Set background color to Red.
                    Ps = 1 0 2  -> Set background color to Green.
                    Ps = 1 0 3  -> Set background color to Yellow.
                    Ps = 1 0 4  -> Set background color to Blue.
                    Ps = 1 0 5  -> Set background color to Magenta.
                    Ps = 1 0 6  -> Set background color to Cyan.
                    Ps = 1 0 7  -> Set background color to White.
  
                  If xterm is compiled with the 16-color support disabled, it
                  supports the following, from rxvt:
                    Ps = 1 0 0  -> Set foreground and background color to
                  default.
  
                  Xterm maintains a color palette whose entries are identified
                  by an index beginning with zero.  If 88- or 256-color support
                  is compiled, the following apply:
                  o All parameters are decimal integers.
                  o RGB values range from zero (0) to 255.
                  o ISO-8613-3 can be interpreted in more than one way; xterm
                    allows the semicolons in this control to be replaced by
                    colons (but after the first colon, colons must be used).
  
                  These ISO-8613-3 controls are supported:
                    Pm = 3 8 ; 2 ; Pr; Pg; Pb -> Set foreground color to the
                  closest match in xterm's palette for the given RGB Pr/Pg/Pb.
                    Pm = 3 8 ; 5 ; Ps -> Set foreground color to Ps.
                    Pm = 4 8 ; 2 ; Pr; Pg; Pb -> Set background color to the
                  closest match in xterm's palette for the given RGB Pr/Pg/Pb.
                    Pm = 4 8 ; 5 ; Ps -> Set background color to Ps.
  
                    */
      new Sym(CTRL_7BIT[CTRL_CSI] + '>' + "*^0" + 'm', -1), //Set or reset resource-values used by xterm to decide whether
                        //to construct escape sequences holding information about the
                        //modifiers pressed with a given key.  The first parameter iden-
                        //tifies the resource to set/reset.  The second parameter is the
                        //value to assign to the resource.  If the second parameter is
                        //omitted, the resource is reset to its initial value.
        /**
                    Ps = 0  -> modifyKeyboard.
                    Ps = 1  -> modifyCursorKeys.
                    Ps = 2  -> modifyFunctionKeys.
                    Ps = 4  -> modifyOtherKeys.
                  If no parameters are given, all resources are reset to their
                  initial values.
                  */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'n', -1), //Device Status Report (DSR).
        /**
                    Ps = 5  -> Status Report.
                  Result ("OK") is CSI 0 n
                    Ps = 6  -> Report Cursor Position (CPR) [row;column].
                  Result is CSI r ; c R
  
                  Note: it is possible for this sequence to be sent by a func-
                  tion key.  For example, with the default keyboard configura-
                  tion the shifted F1 key may send (with shift-, control-, alt-
                  modifiers)
                  new Sym(CTRL_7BIT[CTRL_CSI] + 1  ; 2  R , or
                  new Sym(CTRL_7BIT[CTRL_CSI] + 1  ; 5  R , or
                  new Sym(CTRL_7BIT[CTRL_CSI] + 1  ; 6  R , etc.
                  The second parameter encodes the modifiers; values range from
                  2 to 16.  See the section PC-Style Function Keys for the
                  codes.  The modifyFunctionKeys and modifyKeyboard resources
                  can change the form of the string sent from the modified F1
                  key.
  
                  */
      new Sym(CTRL_7BIT[CTRL_CSI] + '>' + "*^0" + 'n', -1), //
        /**
                  Disable modifiers which may be enabled via the CSI > Ps; Ps m
                  sequence.  This corresponds to a resource value of "-1", which
                  cannot be set with the other sequence.  The parameter identi-
                  fies the resource to be disabled:
                    Ps = 0  -> modifyKeyboard.
                    Ps = 1  -> modifyCursorKeys.
                    Ps = 2  -> modifyFunctionKeys.
                    Ps = 4  -> modifyOtherKeys.
                  If the parameter is omitted, modifyFunctionKeys is disabled.
                  When modifyFunctionKeys is disabled, xterm uses the modifier
                  keys to make an extended sequence of functions rather than
                  adding a parameter to each function key to denote the modi-
                  fiers.
                  */
      new Sym(CTRL_7BIT[CTRL_CSI] + "\\?" + "*^0" + 'n', -1), //Device Status Report (DSR, DEC-specific).
            /**
                    Ps = 6  -> Report Cursor Position (DECXCPR) [row;column] as
                CTRL_7BIT[CTRL_CSI] + ? r ; c R (assumes the default page, i.e., "1").
                    Ps = 1 5  -> Report Printer status as CSI ? 1 0 n  (ready).
                  or CSI ? 1 1 n  (not ready).
                    Ps = 2 5  -> Report UDK status as CSI ? 2 0 n  (unlocked) or
                (locked).
                    Ps = 2 6  -> Report Keyboard status as
                CTRL_7BIT[CTRL_CSI] + ? 2 7 ; 1 ; 0 ; 0 n  (North American).
                  The last two parameters apply to VT400 & up, and denote key-
                  board ready and LK01 respectively.
                    Ps = 5 3  -> Report Locator status as CSI ? 5 3 n  Locator
                  available, if compiled-in, or CSI ? 5 0 n  No Locator, if not.
                    Ps = 5 5  -> Report Locator status as CSI ? 5 3 n  Locator
                  available, if compiled-in, or CSI ? 5 0 n  No Locator, if not.
                    Ps = 5 6  -> Report Locator type as CSI ? 5 7 ; 1 n  Mouse,
                  if compiled-in, or CSI ? 5 7 ; 0 n  Cannot identify, if not.
                    Ps = 6 2  -> Report macro space (DECMSR) as CSI Pn \* {
                    Ps = 6 3  -> Report memory checksum (DECCKSR) as DCS Pt ! x
                  x x x ST
                      Pt is the request id (from an optional parameter to the
                  request).
                      The x's are hexadecimal digits 0-9 and A-F.
                    Ps = 7 5  -> Report data integrity as CSI ? 7 0 n  (ready,
                  no errors)
                    Ps = 8 5  -> Report multi-session configuration as CSI ? 8 3
                  n  (not configured for multiple-session operation).
                  */
      new Sym(CTRL_7BIT[CTRL_CSI] + '>' + "*^0" + 'p', -1), //
            /**
                  Set resource value pointerMode.  This is used by xterm to
                  decide whether to hide the pointer cursor as the user types.
                  Valid values for the parameter:
                    Ps = 0  -> never hide the pointer.
                    Ps = 1  -> hide if the mouse tracking mode is not enabled.
                    Ps = 2  -> always hide the pointer, except when leaving the
                  window.
                    Ps = 3  -> always hide the pointer, even if leaving/entering
                  the window.  If no parameter is given, xterm uses the default,
                  which is 1 .
                  */
      new Sym(CTRL_7BIT[CTRL_CSI] + "\\!" + 'p', -1), //Soft terminal reset (DECSTR).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '$' + 'p', -1), //
        /**
                  Request ANSI mode (DECRQM).  For VT300 and up, reply is
                  CTRL_7BIT[CTRL_CSI] + "*^0"; Pm$ y
                  where Ps is the mode number as in RM, and Pm is the mode
                  value:
                    0 - not recognized
                    1 - set
                    2 - reset
                    3 - permanently set
                    4 - permanently reset
                    */
      new Sym(CTRL_7BIT[CTRL_CSI] + "\\?" + "*^0"+ '$' + 'p', -1), //
            /**
                  Request DEC private mode (DECRQM).  For VT300 and up, reply is
                  new Sym(CTRL_7BIT[CTRL_CSI] + ? Ps; Pm$ y
                  where Ps is the mode number as in DECSET, Pm is the mode value
                  as in the ANSI DECRQM.
                  */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '"' + 'p', -1), //
        /**
                  Set conformance level (DECSCL).  Valid values for the first
                  parameter:
                    Ps = 6 1  -> VT100.
                    Ps = 6 2  -> VT200.
                    Ps = 6 3  -> VT300.
                  Valid values for the second parameter:
                    Ps = 0  -> 8-bit controls.
                    Ps = 1  -> 7-bit controls (always set for VT100).
                    Ps = 2  -> 8-bit controls.
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'q', -1), //Load LEDs (DECLL).
        /**
                    Ps = 0  -> Clear all LEDS (default).
                    Ps = 1  -> Light Num Lock.
                    Ps = 2  -> Light Caps Lock.
                    Ps = 3  -> Light Scroll Lock.
                    Ps = 2  1  -> Extinguish Num Lock.
                    Ps = 2  2  -> Extinguish Caps Lock.
                    Ps = 2  3  -> Extinguish Scroll Lock.
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + ' ' + 'q', -1), // Set cursor style (DECSCUSR, VT520).
        /**
                    Ps = 0  -> blinking block.
                    Ps = 1  -> blinking block (default).
                    Ps = 2  -> steady block.
                    Ps = 3  -> blinking underline.
                    Ps = 4  -> steady underline.
                    Ps = 5  -> blinking bar (xterm).
                    Ps = 6  -> steady bar (xterm).
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '"' + 'q', -1), //
        /**
                  Select character protection attribute (DECSCA).  Valid values
                  for the parameter:
                    Ps = 0  -> DECSED and DECSEL can erase (default).
                    Ps = 1  -> DECSED and DECSEL cannot erase.
                    Ps = 2  -> DECSED and DECSEL can erase.
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'r', X_DECSTBM), //
        /**
                  Set Scrolling Region [top;bottom] (default = full size of win-
                  dow) (DECSTBM).
                  */
      new Sym(CTRL_7BIT[CTRL_CSI] + "\\?" + "*^0" + 'r', -1), //
      /**
                  Restore DEC Private Mode Values.  The value of Ps previously
                  saved is restored.  Ps values are the same as for DECSET.
                  */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '$' + 'r', -1), //
        /**
                  Change Attributes in Rectangular Area (DECCARA), VT400 and up.
                    Pt; Pl; Pb; Pr denotes the rectangle.
                    Ps denotes the SGR attributes to change: 0, 1, 4, 5, 7.
                    */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 's', -1), // Set left and right margins (DECSLRM), available only when DECLRMM is enabled (VT420 and up).
      /*new Sym(CTRL_7BIT[CTRL_CSI] + 's',*/ // Save cursor (ANSI.SYS), available only when DECLRMM is dis- abled.
      new Sym(CTRL_7BIT[CTRL_CSI] + "\\?" + "*^0" + 's', -1), // Save DEC Private Mode Values.  Ps values are the same as for DECSET.
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 't', -1), //
        /**
                  Window manipulation (from dtterm, as well as extensions).
                  These controls may be disabled using the allowWindowOps
                  resource.  Valid values for the first (and any additional
                  parameters) are:
                    Ps = 1  -> De-iconify window.
                    Ps = 2  -> Iconify window.
                    Ps = 3  ;  x ;  y -> Move window to [x, y].
                    Ps = 4  ;  height ;  width -> Resize the xterm window to
                  given height and width in pixels.  Omitted parameters reuse
                  the current height or width.  Zero parameters use the dis-
                  play's height or width.
                    Ps = 5  -> Raise the xterm window to the front of the stack-
                  ing order.
                    Ps = 6  -> Lower the xterm window to the bottom of the
                  stacking order.
                    Ps = 7  -> Refresh the xterm window.
                    Ps = 8  ;  height ;  width -> Resize the text area to given
                  height and width in characters.  Omitted parameters reuse the
                  current height or width.  Zero parameters use the display's
                  height or width.
                    Ps = 9  ;  0  -> Restore maximized window.
                    Ps = 9  ;  1  -> Maximize window (i.e., resize to screen
                  size).
                    Ps = 9  ;  2  -> Maximize window vertically.
                    Ps = 9  ;  3  -> Maximize window horizontally.
                    Ps = 1 0  ;  0  -> Undo full-screen mode.
                    Ps = 1 0  ;  1  -> Change to full-screen.
                    Ps = 1 0  ;  2  -> Toggle full-screen.
                    Ps = 1 1  -> Report xterm window state.  If the xterm window
                  is open (non-iconified), it returns CSI 1 t .  If the xterm
                  window is iconified, it returns CSI 2 t .
                    Ps = 1 3  -> Report xterm window position.
                  Result is CSI 3 ; x ; y t
                    Ps = 1 4  -> Report xterm window in pixels.
                  Result is CSI  4  ;  height ;  width t
                    Ps = 1 8  -> Report the size of the text area in characters.
                  Result is CSI  8  ;  height ;  width t
                    Ps = 1 9  -> Report the size of the screen in characters.
                  Result is CSI  9  ;  height ;  width t
                    Ps = 2 0  -> Report xterm window's icon label.
                  Result is OSC  L  label ST
                    Ps = 2 1  -> Report xterm window's title.
                  Result is OSC  l  label ST
                    Ps = 2 2  ;  0  -> Save xterm icon and window title on
                  stack.
                    Ps = 2 2  ;  1  -> Save xterm icon title on stack.
                    Ps = 2 2  ;  2  -> Save xterm window title on stack.
                    Ps = 2 3  ;  0  -> Restore xterm icon and window title from
                  stack.
                    Ps = 2 3  ;  1  -> Restore xterm icon title from stack.
                    Ps = 2 3  ;  2  -> Restore xterm window title from stack.
                    Ps >= 2 4  -> Resize to Ps lines (DECSLPP).
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '$' + 't', -1), // Reverse Attributes in Rectangular Area (DECRARA), VT400 and up.
        /**
                    Pt; Pl; Pb; Pr denotes the rectangle.
                    Ps denotes the attributes to reverse, i.e.,  1, 4, 5, 7.
                    */
      new Sym(CTRL_7BIT[CTRL_CSI] + '>' + "*^0" + 't', -1), // Set one or more features of the title modes.  Each parameter enables a single feature.
        /**
                    Ps = 0  -> Set window/icon labels using hexadecimal.
                    Ps = 1  -> Query window/icon labels using hexadecimal.
                    Ps = 2  -> Set window/icon labels using UTF-8.
                    Ps = 3  -> Query window/icon labels using UTF-8.  (See dis-
                  cussion of "Title Modes")
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + ' ' + 't', -1), // Set warning-bell volume (DECSWBV, VT520).
        /**
                    Ps = 0  or 1  -> off.
                    Ps = 2 , 3  or 4  -> low.
                    Ps = 5 , 6 , 7 , or 8  -> high.
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + 'u', -1), //Restore cursor (ANSI.SYS).
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + ' ' + 'u', -1), //Set margin-bell volume (DECSMBV, VT520).
        /**
                    Ps = 1  -> off.
                    Ps = 2 , 3  or 4  -> low.
                    Ps = 0 , 5 , 6 , 7 , or 8  -> high.
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '$' + 'v', -1), //Copy Rectangular Area (DECCRA, VT400 and up).
        /**
                    Pt; Pl; Pb; Pr denotes the rectangle.
                    Pp denotes the source page.
                    Pt; Pl denotes the target location.
                    Pp denotes the target page.
                    */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '\'' + 'w', -1), //Enable Filter Rectangle (DECEFR), VT420 and up.
        /**
                  Parameters are [top;left;bottom;right].
                  Defines the coordinates of a filter rectangle and activates
                  it.  Anytime the locator is detected outside of the filter
                  rectangle, an outside rectangle event is generated and the
                  rectangle is disabled.  Filter rectangles are always treated
                  as "one-shot" events.  Any parameters that are omitted default
                  to the current locator position.  If all parameters are omit-
                  ted, any locator motion will be reported.  DECELR always can-
                  cels any prevous rectangle definition.
                  */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + 'x', -1), //Request Terminal Parameters (DECREQTPARM).
        /**
                  if Ps is a "0" (default) or "1", and xterm is emulating VT100,
                  the control sequence elicits a response of the same form whose
                  parameters describe the terminal:
                    Ps -> the given Ps incremented by 2.
                    Pn = 1  <- no parity.
                    Pn = 1  <- eight bits.
                    Pn = 1  <- 2  8  transmit 38.4k baud.
                    Pn = 1  <- 2  8  receive 38.4k baud.
                    Pn = 1  <- clock multiplier.
                    Pn = 0  <- STP flags.
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + "\\*" + 'x', -1), //Select Attribute Change Extent (DECSACE).
        /**
                    Ps = 0  -> from start to end position, wrapped.
                    Ps = 1  -> from start to end position, wrapped.
                    Ps = 2  -> rectangle (exact).
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" +"\\*" + 'y', -1), //Request Checksum of Rectangular Area (DECRQCRA), VT420 and up.
        /**
                  Response is
                  DCS Pi ! x x x x ST
                    Pi is the request id.
                    Pg is the page number.
                    Pt; Pl; Pb; Pr denotes the rectangle.
                    The x's are hexadecimal digits 0-9 and A-F.
                    */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '$' + 'x', -1), //Fill Rectangular Area (DECFRA), VT420 and up.
        /**
                    Pc is the character to use.
                    Pt; Pl; Pb; Pr denotes the rectangle.
                    */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '\'' + 'z', -1), //Enable Locator Reporting (DECELR).
        /**
                  Valid values for the first parameter:
                    Ps = 0  -> Locator disabled (default).
                    Ps = 1  -> Locator enabled.
                    Ps = 2  -> Locator enabled for one report, then disabled.
                  The second parameter specifies the coordinate unit for locator
                  reports.
                  Valid values for the second parameter:
                    Pu = 0  <- or omitted -> default to character cells.
                    Pu = 1  <- device physical pixels.
                    Pu = 2  <- character cells.
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '$' + 'z', -1), //Erase Rectangular Area (DECERA), VT400 and up.
        /**
                    Pt; Pl; Pb; Pr denotes the rectangle.
                    */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '\'' + '{', -1), //Select Locator Events (DECSLE).
        /**
                  Valid values for the first (and any additional parameters)
                  are:
                    Ps = 0  -> only respond to explicit host requests (DECRQLP).
                               (This is default).  It also cancels any filter
                               rectangle.
                    Ps = 1  -> report button down transitions.
                    Ps = 2  -> do not report button down transitions.
                    Ps = 3  -> report button up transitions.
                    Ps = 4  -> do not report button up transitions.
                        */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '$' + '{', -1), //Selective Erase Rectangular Area (DECSERA), VT400 and up.
          /**
                    Pt; Pl; Pb; Pr denotes the rectangle.
                    */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '\'' + '|', -1), //Request Locator Position (DECRQLP).
        /**
                  Valid values for the parameter are:
                    Ps = 0 , 1 or omitted -> transmit a single DECLRP locator
                  report.
  
                  If Locator Reporting has been enabled by a DECELR, xterm will
                  respond with a DECLRP Locator Report.  This report is also
                  generated on button up and down events if they have been
                  enabled with a DECSLE, or when the locator is detected outside
                  of a filter rectangle, if filter rectangles have been enabled
                  with a DECEFR.
  
                    -> CSI Pe ; Pb ; Pr ; Pc ; Pp &  w
  
                  Parameters are [event;button;row;column;page].
                  Valid values for the event:
                    Pe = 0  -> locator unavailable - no other parameters sent.
                    Pe = 1  -> request - xterm received a DECRQLP.
                    Pe = 2  -> left button down.
                    Pe = 3  -> left button up.
                    Pe = 4  -> middle button down.
                    Pe = 5  -> middle button up.
                    Pe = 6  -> right button down.
                    Pe = 7  -> right button up.
                    Pe = 8  -> M4 button down.
                    Pe = 9  -> M4 button up.
                    Pe = 1 0  -> locator outside filter rectangle.
                  The "button" parameter is a bitmask indicating which buttons
                  are pressed:
                    Pb = 0  <- no buttons down.
                    Pb & 1  <- right button down.
                    Pb & 2  <- middle button down.
                    Pb & 4  <- left button down.
                    Pb & 8  <- M4 button down.
                  The "row" and "column" parameters are the coordinates of the
                  locator position in the xterm window, encoded as ASCII deci-
                  mal.
                  The "page" parameter is not used by xterm.
                  */
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '\'' + '}', -1), //Insert Ps Column(s) (default = 1) (DECIC), VT420 and up.
      new Sym(CTRL_7BIT[CTRL_CSI] + "*^0" + '\'' + '~', -1), //Delete Ps Column(s) (default = 1) (DECDC), VT420 and up.
  };
  
  Lexer lexer;
  XtermHandler term;
  
  public XtermStream(XtermHandler x) {
    if (model == null) {
      model = new Lexer(null, 256);
      for (Sym sym : CTRL_XTERM) {
        model.addSymbol(sym.sym, sym.id);
      }
      model.compile();
      model.defineUserSet("0123456789;", 0);
      model.printTree();
    }
    term = x;
    lexer = new Lexer(this, 256);
    model.transferModel(lexer);
  }
  
  public void feed(byte b) {
    //System.out.print(String.format("[%02x:%c]", b, b));
    lexer.feed(b);
  }

  public void feed(byte[] b, int len) {
    //for (int i = 0; i < len; i++)
    //  System.out.print(String.format("[%02x:%c]", b[i], b[i]));
    lexer.feed(b, 0, len);
  }
  
  @Override
  public void symbol(byte[] symdata, int len, int sym) {
    lastSymData = symdata;
    lastSymLen = len;
    symDataIx = 0;
    int a;
    switch (sym) {
    case X_BS: {
      System.out.println("xterm.backspace");
      term.cursorCol(-1);
      break; 
    } 
    case X_DECSC: {
      System.out.println("xterm.saveCursor");
      term.saveCursor();
      break; 
    } 
    case X_DECRC: {
      System.out.println("xterm.restoreCursor");
      term.restoreCursor();
      break; 
    } 
    case X_SGR: {
      while ((a = getNextArg()) != -1) {
        handleCharacterAttribute(a);
      }
      break; 
    } 
    case X_CUP: {
      int row = getNextArg();
      int col = getNextArg();
      if (row == -1) row = 1;
      if (col == -1) col = 1;
      System.out.println("xterm.setCursorPos " + col + "," + row);
      term.setCursorPosition(col, row);
      break; 
    } 
    case X_VPA: {
      int row = getNextArg();
      if (row == -1) row = 1;
      System.out.println("xterm.setRow " + row);
      term.setCursorRow(row);
      break; 
    } 
    case X_CUU: {
      int x = getNextArg();
      if (x == -1) x = 1;
      System.out.println("xterm.cursorUp " + x);
      term.cursorRow(-x);
      break; 
    } 
    case X_CUD: {
      int x = getNextArg();
      if (x == -1) x = 1;
      System.out.println("xterm.cursorDown " + x);
      term.cursorRow(x);
      break; 
    } 
    case X_CUF: {
      int x = getNextArg();
      if (x == -1) x = 1;
      System.out.println("xterm.cursorForw " + x);
      term.cursorCol(x);
      break; 
    } 
    case X_CUB: {
      int x = getNextArg();
      if (x == -1) x = 1;
      System.out.println("xterm.cursorback " + x);
      term.cursorCol(-x);
      break; 
    } 
    case X_CNL: {
      int x = getNextArg();
      if (x == -1) x = 1;
      System.out.println("xterm.cursornewline " + x);
      term.cursorNewline(x);
      break; 
    } 
    case X_CPL: {
      int x = getNextArg();
      if (x == -1) x = 1;
      System.out.println("xterm.cursorprevline " + x);
      term.cursorPrevline(x);
      break; 
    } 
    case X_CHA: {
      int x = getNextArg();
      if (x == -1) x = 1;
      System.out.println("xterm.setcol " + x);
      term.setCursorCol(x);
      break; 
    } 
    case X_EL: {
      int x = getNextArg();
      if (x == -1) x = 0;
      System.out.println("xterm.eraseline " + x);
      switch (x) {
      case 0: term.eraseLineRight(); break;
      case 1: term.eraseLineLeft(); break;
      case 2: term.eraseLineAll(); break;
      }
      break; 
    } 
    case X_DCH: {
      int x = getNextArg();
      if (x == -1) x = 1;
      System.out.println("xterm.delchars " + x);
      term.delete(x);
      break; 
    } 
    case X_RI: {
      System.out.println("xterm.reverseIndex");
      term.cursorPrevline(1);;
      break; 
    } 
    case X_ED: {
      int x = getNextArg();
      if (x == -1) x = 0;
      System.out.println("xterm.eraseDisplay " + x);
      switch (x) {
      case 0: term.eraseDisplayBelow(); break;
      case 1: term.eraseDisplayAbove(); break;
      case 2: term.eraseDisplayAll(); break;
      case 3: term.eraseDisplaySavedLines(); break;
      }
      break; 
    } 
    case X_DECSTBM: {
      int mi = getNextArg();
      int ma = getNextArg();
      System.out.println("xterm.defineScrollRegion " + mi + "," + ma);
      term.setScrollRegion(mi, ma);
      break; 
    } 
    case X_SU: {
      int x = getNextArg();
      if (x == -1) x = 1;
      System.out.println("xterm.scrollUp " + x);
      term.scroll(x);
      break; 
    } 
    case X_SD: {
      int x = getNextArg();
      if (x == -1) x = 1;
      System.out.println("xterm.scrollDown " + x);
      term.scroll(-x);
      break; 
    } 
    case X_DECSET: {
      while ((a = getNextArg()) != -1) {
        handleDecSetRstAttribute(a, true);
      }
      break; 
    } 
    case X_DECRST: {
      while ((a = getNextArg()) != -1) {
        handleDecSetRstAttribute(a, false);
      }
      break; 
    } 
    case X_CHARSET0: {
      handleCharacterAttribute(0); // TODO
      break;
    }
    default:
      System.out.println("XTERMSYM:" + new String(symdata, 0, len) + " " + String.format("%08x", sym));
      break;
    }
  }

  @Override
  public abstract void data(byte[] data, int len);
  
  void handleDecSetRstAttribute(int a, boolean enable) {
    switch (a) {
    case 1  : /* TODO */ System.out.println("XTERM DECSETRST:Application Cursor Keys (DECCKM)."); break;
    case 2  : /* TODO */ System.out.println("XTERM DECSETRST:Designate USASCII for character sets G0-G3 (DECANM), and set VT100 mode."); break;
    case 3  : /* TODO */ System.out.println("XTERM DECSETRST:132 Column Mode (DECCOLM)."); break;
    case 4  : /* TODO */ System.out.println("XTERM DECSETRST:Smooth (Slow) Scroll (DECSCLM)."); break;
    case 5  : /* TODO */ System.out.println("XTERM DECSETRST:Reverse Video (DECSCNM)."); break;
    case 6  : /* TODO */ System.out.println("XTERM DECSETRST:Origin Mode (DECOM)."); break;
    case 7  : /* TODO */ System.out.println("XTERM DECSETRST:Wraparound Mode (DECAWM)."); break;
    case 8  : /* TODO */ System.out.println("XTERM DECSETRST:Auto-repeat Keys (DECARM)."); break;
    case 9  : /* TODO */ System.out.println("XTERM DECSETRST:Send Mouse X & Y on button press.  See the section Mouse Tracking.  This is the X10 xterm mouse protocol."); break;
    case 10 : /* TODO */ System.out.println("XTERM DECSETRST:Show toolbar (rxvt)."); break;
    case 12 : /* TODO */ System.out.println("XTERM DECSETRST:Start Blinking Cursor (att610)."); break;
    case 18 : /* TODO */ System.out.println("XTERM DECSETRST:Print form feed (DECPFF)."); break;
    case 19 : /* TODO */ System.out.println("XTERM DECSETRST:Set print extent to full screen (DECPEX)."); break;
    case 25 : /* TODO */ System.out.println("XTERM DECSETRST:Show Cursor (DECTCEM)."); break;
    case 30 : /* TODO */ System.out.println("XTERM DECSETRST:Show scrollbar (rxvt)."); break;
    case 35 : /* TODO */ System.out.println("XTERM DECSETRST:Enable font-shifting functions (rxvt)."); break;
    case 38 : /* TODO */ System.out.println("XTERM DECSETRST:Enter Tektronix Mode (DECTEK)."); break;
    case 40 : /* TODO */ System.out.println("XTERM DECSETRST:Allow 80 -> 132 Mode."); break;
    case 41 : /* TODO */ System.out.println("XTERM DECSETRST:more(1) fix (see curses resource)."); break;
    case 42 : /* TODO */ System.out.println("XTERM DECSETRST:Enable National Replacement Character sets (DECNRCM)."); break;
    case 44 : /* TODO */ System.out.println("XTERM DECSETRST:Turn On Margin Bell."); break;
    case 45 : /* TODO */ System.out.println("XTERM DECSETRST:Reverse-wraparound Mode."); break;
    case 46 : /* TODO */ System.out.println("XTERM DECSETRST:Start Logging.  This is normally disabled by a compile-time option."); break;
    case 47 : System.out.println("xterm.setAlternateScreen " + enable); 
    //Ps = 4 7  -> Use Alternate Screen Buffer.  (This may be disabled by the titeInhibit resource).
    //Ps = 4 7  -> Use Normal Screen Buffer, clearing screen first if in the Alternate Screen.  (This may be disabled by the titeInhibit resource).
    term.setAlternateScreenBuffer(enable);
    break;
    case 66 : /* TODO */ System.out.println("XTERM DECSETRST:Application keypad (DECNKM)."); break;
    case 67 : /* TODO */ System.out.println("XTERM DECSETRST:Backarrow key sends backspace (DECBKM)."); break;
    case 69 : /* TODO */ System.out.println("XTERM DECSETRST:Enable left and right margin mode (DECLRMM), VT420 and up."); break;
    case 95 : /* TODO */ System.out.println("XTERM DECSETRST:Do not clear screen when DECCOLM is set/reset (DECNCSM), VT510 and up."); break;
    case 1000 : /* TODO */ System.out.println("XTERM DECSETRST:Send Mouse X & Y on button press and release.  See the section Mouse Tracking.  This is the X11 xterm mouse protocol."); break;
    case 1001 : /* TODO */ System.out.println("XTERM DECSETRST:Use Hilite Mouse Tracking."); break;
    case 1002 : /* TODO */ System.out.println("XTERM DECSETRST:Use Cell Motion Mouse Tracking."); break;
    case 1003 : /* TODO */ System.out.println("XTERM DECSETRST:Use All Motion Mouse Tracking."); break;
    case 1004 : /* TODO */ System.out.println("XTERM DECSETRST:Send FocusIn/FocusOut events."); break;
    case 1005 : /* TODO */ System.out.println("XTERM DECSETRST:Enable UTF-8 Mouse Mode."); break;
    case 1006 : /* TODO */ System.out.println("XTERM DECSETRST:Enable SGR Mouse Mode."); break;
    case 1007 : /* TODO */ System.out.println("XTERM DECSETRST:Enable Alternate Scroll Mode."); break;
    case 1010 : /* TODO */ System.out.println("XTERM DECSETRST:Scroll to bottom on tty output (rxvt)."); break;
    case 1011 : /* TODO */ System.out.println("XTERM DECSETRST:Scroll to bottom on key press (rxvt)."); break;
    case 1015 : /* TODO */ System.out.println("XTERM DECSETRST:Enable urxvt Mouse Mode."); break;
    case 1034 : /* TODO */ System.out.println("XTERM DECSETRST:Interpret 'meta' key, sets eighth bit. (enables the eightBitInput resource)."); break;
    case 1035 : /* TODO */ System.out.println("XTERM DECSETRST:Enable special modifiers for Alt and Num-Lock keys.  (This enables the numLock resource)."); break;
    case 1036 : /* TODO */ System.out.println("XTERM DECSETRST:Send ESC   when Meta modifies a key.  (This enables the metaSendsEscape resource)."); break;
    case 1037 : /* TODO */ System.out.println("XTERM DECSETRST:Send DEL from the editing-keypad Delete key."); break;
    case 1039 : /* TODO */ System.out.println("XTERM DECSETRST:Send ESC  when Alt modifies a key.  (This enables the altSendsEscape resource)."); break;
    case 1040 : /* TODO */ System.out.println("XTERM DECSETRST:Keep selection even if not highlighted. (This enables the keepSelection resource)."); break;
    case 1041 : /* TODO */ System.out.println("XTERM DECSETRST:Use the CLIPBOARD selection.  (This enables the selectToClipboard resource)."); break;
    case 1042 : /* TODO */ System.out.println("XTERM DECSETRST:Enable Urgency window manager hint when Control-G is received.  (This enables the bellIsUrgent resource)."); break;
    case 1043 : /* TODO */ System.out.println("XTERM DECSETRST:Enable raising of the window when Control-G is received.  (enables the popOnBell resource)."); break;
    case 1044 : /* TODO */ System.out.println("XTERM DECSETRST:Reuse the most recent data copied to CLIPBOARD.  (This enables the keepClipboard resource)."); break;
    case 1047 : 
    //Ps = 1 0 4 7  -> Use Alternate Screen Buffer.  (This may be disabled by the titeInhibit resource).
    //Ps = 1 0 4 7  -> Use Normal Screen Buffer, clearing screen first if in the Alternate Screen.  (This may be disabled by the titeInhibit resource).
    term.setAlternateScreenBuffer(enable);
    break;
    case 1048 :
    //Ps = 1 0 4 8  -> Save cursor as in DECSC.  (This may be disabled by the titeInhibit resource).
    //Ps = 1 0 4 8  -> Restore cursor as in DECRC.  (This may be disabled by the titeInhibit resource).
    if (enable) {
      System.out.println("xterm.saveCursor");
      term.saveCursor();
    } else {
      System.out.println("xterm.restoreCursor");
      term.restoreCursor();
    }
    break;
    case 1049 :
    //Ps = 1 0 4 9  -> Save cursor as in DECSC and use Alternate Screen Buffer, clearing it first.
    // (This may be disabled by the titeInhibit resource).  This combines the effects of the 1 0 4 7 
    // and 1 0 4 8  modes.  Use this with terminfo-based applications rather than the 4 7  mode.
    //Ps = 1 0 4 9  -> Use Normal Screen Buffer and restore cursor as in DECRC.  
    // (This may be disabled by the titeInhibit resource).  This combines the effects of the 1 0 4 7
    // and 1 0 4 8  modes.  Use this with terminfo-based applications rather than the 4 7  mode.
    if (enable) {
      System.out.println("xterm.saveCursor alternateScreenBuffer:true eraseAll");
      term.saveCursor();
      term.setAlternateScreenBuffer(true);
      term.eraseDisplayAll();
    } else {
      System.out.println("xterm.eraseAll alternateScreeBuffer:false restoreCursor");
      term.eraseDisplayAll();
      term.setAlternateScreenBuffer(false);
      term.restoreCursor();
    }
    break;
    case 1050 : /* TODO */ System.out.println("XTERM DECSETRST:Set terminfo/termcap function-key mode."); break;
    case 1051 : /* TODO */ System.out.println("XTERM DECSETRST:Set Sun function-key mode."); break;
    case 1052 : /* TODO */ System.out.println("XTERM DECSETRST:Set HP function-key mode."); break;
    case 1053 : /* TODO */ System.out.println("XTERM DECSETRST:Set SCO function-key mode."); break;
    case 1060 : /* TODO */ System.out.println("XTERM DECSETRST:Set legacy keyboard emulation (X11R6)."); break;
    case 1061 : /* TODO */ System.out.println("XTERM DECSETRST:Set VT220 keyboard emulation."); break;
    case 2004 : /* TODO */ System.out.println("XTERM DECSETRST:Set bracketed paste mode."); break;
    }
  }
  void handleCharacterAttribute(int a) {
    //System.out.println("xterm.handleCharacterAttrib:" + a);
    switch (a) {
      case 0: term.setTextDefault(); break; //Normal (default).
      case 1: term.setTextBold(true); break; //Bold.
      case 2: break; //Faint, decreased intensity (ISO 6429).
      case 3: break; //Italicized (ISO 6429).
      case 4: break; //Underlined.
      case 5: break; //Blink (appears as Bold).
      case 7: term.setTextInverse(true); break; //Inverse.
      case 8: break; //Invisible, i.e., hidden (VT300).
      case 9: break; //Crossed-out characters (ISO 6429).
      case 21: break; //Doubly-underlined (ISO 6429).
      case 22: term.setTextBold(false); break; //Normal (neither bold nor faint).
      case 23: break; //Not italicized (ISO 6429).
      case 24: break; //Not underlined.
      case 25: break; //Steady (not blinking).
      case 27: term.setTextInverse(false); break; //Positive (not inverse).
      case 28: break; //Visible, i.e., not hidden (VT300).
      case 29: break; //Not crossed-out (ISO 6429).
      case 30: term.setTextFgColor(TEXT_BLACK); break; //Set foreground color to Black.
      case 31: term.setTextFgColor(TEXT_RED); break; //Set foreground color to Red.
      case 32: term.setTextFgColor(TEXT_GREEN); break; //Set foreground color to Green.
      case 33: term.setTextFgColor(TEXT_YELLOW); break; //Set foreground color to Yellow.
      case 34: term.setTextFgColor(TEXT_BLUE); break; //Set foreground color to Blue.
      case 35: term.setTextFgColor(TEXT_MAGENTA); break; //Set foreground color to Magenta.
      case 36: term.setTextFgColor(TEXT_CYAN); break; //Set foreground color to Cyan.
      case 37: term.setTextFgColor(TEXT_WHITE); break; //Set foreground color to White.
      case 39: term.setTextFgColor(TEXT_DEFAULT); break; //Set foreground color to default (original).
      case 40: term.setTextBgColor(TEXT_BLACK); break; //Set background color to Black.
      case 41: term.setTextBgColor(TEXT_RED); break; //Set background color to Red.
      case 42: term.setTextBgColor(TEXT_GREEN); break; //Set background color to Green.
      case 43: term.setTextBgColor(TEXT_YELLOW); break; //Set background color to Yellow.
      case 44: term.setTextBgColor(TEXT_BLUE); break; //Set background color to Blue.
      case 45: term.setTextBgColor(TEXT_MAGENTA); break; //Set background color to Magenta.
      case 46: term.setTextBgColor(TEXT_CYAN); break; //Set background color to Cyan.
      case 47: term.setTextBgColor(TEXT_WHITE); break; //Set background color to White.
      case 49: term.setTextBgColor(TEXT_DEFAULT); break; //Set background color to default (original).
      case 90: term.setTextFgColor(TEXT_BLACK); break; //Set foreground color to Black.
      case 91: term.setTextFgColor(TEXT_BRED); break; //Set foreground color to Red.
      case 92: term.setTextFgColor(TEXT_BGREEN); break; //Set foreground color to Green.
      case 93: term.setTextFgColor(TEXT_BYELLOW); break; //Set foreground color to Yellow.
      case 94: term.setTextFgColor(TEXT_BBLUE); break; //Set foreground color to Blue.
      case 95: term.setTextFgColor(TEXT_BMAGENTA); break; //Set foreground color to Magenta.
      case 96: term.setTextFgColor(TEXT_BCYAN); break; //Set foreground color to Cyan.
      case 97: term.setTextFgColor(TEXT_BWHITE); break; //Set foreground color to White.
      case 100: term.setTextBgColor(TEXT_BLACK); break; //Set background color to Black.
      case 101: term.setTextBgColor(TEXT_BRED); break; //Set background color to Red.
      case 102: term.setTextBgColor(TEXT_BGREEN); break; //Set background color to Green.
      case 103: term.setTextBgColor(TEXT_BYELLOW); break; //Set background color to Yellow.
      case 104: term.setTextBgColor(TEXT_BBLUE); break; //Set background color to Blue.
      case 105: term.setTextBgColor(TEXT_BMAGENTA); break; //Set background color to Magenta.
      case 106: term.setTextBgColor(TEXT_BCYAN); break; //Set background color to Cyan.
      case 107: term.setTextBgColor(TEXT_BWHITE); break; //Set background color to White.
    }
  }
  
  
  byte lastSymData[];
  int symDataIx;
  int lastSymLen;
  public int getNextArg() {
    if (symDataIx >= lastSymLen) return -1;
    int ret = -1;
    for (;symDataIx < lastSymLen; symDataIx++) {
      byte b = lastSymData[symDataIx];
      if (b >= '0' && b <= '9') {
        if (ret == -1) {
          ret = b - '0';
        } else {
          ret = ret * 10 + b - '0';
        }
      } else {
        if (ret == -1) {
          continue;
        } else {
          break;
        }
      }
    }
    return ret;
  }

  public void flush() {
    lexer.flush();
  }
}
