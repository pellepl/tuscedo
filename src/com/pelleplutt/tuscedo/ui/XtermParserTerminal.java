package com.pelleplutt.tuscedo.ui;

import java.awt.Color;
import java.io.UnsupportedEncodingException;

import com.pelleplutt.tuscedo.XtermParserAbstract;
import com.pelleplutt.tuscedo.XtermTerminalIfc;
import com.pelleplutt.util.FastTermPane;
import com.pelleplutt.util.FastTextPane;

/**
 * class XtermParserTerminal
 * handles xterm commands and data and bridges it to the FastTermPane
 * @author petera
 */
public class XtermParserTerminal implements XtermTerminalIfc {

  XConsole console;
  final XtermParserAbstract xterm;
  final FastTextPane.Style defStyle;
  Color colFG;
  Color colBG;
  boolean bold;
  boolean colInverse;
  byte buf[] = new byte[256];
  volatile int bufIx = 0;
  String textEnc;
  int savedCursorRow, savedCursorCol;
  FastTermPane ftp;
  static Color colXtermPalette[] = {
      null,
      new Color(0x000000),
      new Color(0xff4444),
      new Color(0x44ff44),
      new Color(0xffff44),
      new Color(0x4444ff),
      new Color(0xff44ff),
      new Color(0x44ffff),
      new Color(0xdfdfdf),
      new Color(0xff8888),
      new Color(0x88ff88),
      new Color(0xffff88),
      new Color(0x8888ff),
      new Color(0xff88ff),
      new Color(0x88ffff),
      new Color(0xffffff),
  };


  
  public XtermParserTerminal(FastTermPane ftp, XConsole console, FastTextPane.Style def, String textEncoding) {
    this.ftp = ftp;
    this.console = console;
    textEnc = textEncoding;
    defStyle = new FastTextPane.Style(def);
    colFG = defStyle.getFg();
    colBG = defStyle.getBg();
    
    xterm = new XtermParserAbstract(this) {
      @Override
      public void data(byte[] data, int len, int offset) {
        // got output data from lexer, buffer it
        putBuf(data, len);
      }
      // got xterm control command from lexer
      @Override
      public void symbol(byte[] symdata, int len, int sym, int offset) {
        // first, flush buffered output data
        flushBuf();
        // then pass on to xtermparser so it can call this properly
        super.symbol(symdata, len, sym, offset);
      }
    };
  }
  
  synchronized void putBuf(final byte[] data, final int len) {
    for (int i = 0; i < len; i++) {
      byte b = data[i];
      buf[bufIx++] = b;
      if (b == '\n' || b == '\r'|| bufIx >= buf.length) {
        flushBuf();
      }
    }
  }
  
  public synchronized int flushBuf() {
    int oldBufIx = bufIx;
    XtermParserTerminal.this.outputText(buf, bufIx);
    bufIx = 0;
    return oldBufIx;
  }
  
  // give stuff to xtermparser
  public void feed(byte b) {
    xterm.feed(b);
  }
  
  // give stuff to xtermparser
  public void feed(byte[] b, int len) {
    xterm.feed(b, len);
  }

  // got buffered raw output from xtermparser
  void outputText(byte[] data, int len) {
    if (len <= 0) return;
    try {
      Color fg = colInverse ? colBG : colFG;
      Color bg = colInverse ? colFG : colBG;
      if (fg == null) {
        fg = colInverse ? ftp.getBackground() : ftp.getForeground();
      }
      ftp.addText(new String(data, 0, len, textEnc), defStyle.id, 
          fg, bg, bold);
    } catch (UnsupportedEncodingException e) {}
  }

  // implements XtermTerminalIfc, called by XtermParserAbstract
  
  @Override
  public void setTextFgColor(int palette) {
    colFG = palette > 0 ? colXtermPalette[palette] : defStyle.getFg();
  }
  @Override
  public void setTextBgColor(int palette) {
    colBG = palette > 0 ? colXtermPalette[palette] : defStyle.getBg();
  }
  @Override
  public void setTextBold(boolean b) {
    bold = b;
  }
  @Override
  public void setTextDefault() {
    colFG = defStyle.getFg();
    colBG = defStyle.getBg();
    bold = defStyle.getBold();
    colInverse = false;
  }
  @Override
  public void setTextInverse(boolean b) {
    colInverse = b;
  }
  @Override
  public void setCursorPosition(int col, int row) {
    ftp.setCursor(col-1, row-1);
  }
  @Override
  public void setCursorRow(int x) {
    ftp.setCursorRow(x-1);
  }
  @Override
  public void setCursorCol(int x) {
    ftp.setCursorColumn(x-1);
  }
  @Override
  public void cursorRow(int x) {
    int n = ftp.getCursorRow() + x;
    ftp.setCursorRow(n);
  }
  @Override
  public void cursorCol(int x) {
    int n = ftp.getCursorColumn() + x;
    ftp.setCursorColumn(n);
  }
  @Override
  public void saveCursor() {
    savedCursorRow = ftp.getCursorRow();
    savedCursorCol = ftp.getCursorColumn();
  }
  @Override
  public void cursorNewline(int x) {
    ftp.nextRow();
  }
  @Override
  public void cursorPrevline(int x) {
    ftp.prevRow();
  }
  @Override
  public void restoreCursor() {
    System.out.println("xtermcons.restore @ " + (savedCursorCol + 1) + "," + (savedCursorRow + 1) +
        " from " + (ftp.getCursorColumn() + 1) + "," + (ftp.getCursorRow() + 1));
    setCursorPosition(savedCursorCol, savedCursorRow);
  }
  @Override
  public void eraseLineRight() {
    ftp.eraseLineAfter();
  }
  @Override
  public void eraseLineLeft() {
    ftp.eraseLineBefore();
  }
  @Override
  public void eraseLineAll() {
    ftp.eraseLineFull();
  }
  @Override
  public void eraseDisplayBelow() {
    //TODO
  }
  @Override
  public void eraseDisplayAbove() {
    //TODO
  }
  @Override
  public void eraseDisplayAll() {
    ftp.clear(); 
  }
  @Override
  public void eraseDisplaySavedLines() {
    //TODO
  }
  @Override
  public void delete(int chars) {
    ftp.deleteChars(chars);
  }
  @Override
  public void setScrollRegion(int minRow, int maxRow) {
    ftp.setScrollArea(minRow-1, maxRow-1);
  }
  @Override
  public void scroll(int lines) {
    ftp.scroll(lines);
  }
  @Override
  public void setAlternateScreenBuffer(boolean b) {
    console.setAlternateScreenBuffer(ftp, b);
  }
}
