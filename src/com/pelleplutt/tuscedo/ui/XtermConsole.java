package com.pelleplutt.tuscedo.ui;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.UnsupportedEncodingException;

import com.pelleplutt.tuscedo.Console;
import com.pelleplutt.tuscedo.ProcessGroup;
import com.pelleplutt.tuscedo.ProcessGroupInfo;
import com.pelleplutt.tuscedo.ProcessHandler;
import com.pelleplutt.tuscedo.XtermHandler;
import com.pelleplutt.tuscedo.XtermStream;
import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.AppSystem.Disposable;
import com.pelleplutt.util.FastTextPane;
import com.pelleplutt.util.Log;

/**
 * Contains two XtermStreamHandlers, one for stdout and one for stderr.
 * Takes care of handling output from the XtermStreams for out and err. 
 * Also takes care of key input events and translates them to proper
 * VT_* sequences to the process.
 * @author petera
 */
public class XtermConsole implements Console, KeyListener, Disposable, Runnable {
  WorkArea.View view;
  ProcessHandler ph;
  XtermStreamHandler xstd, xerr;
  volatile boolean running = true;
  long tick = 50;
  FastTextPane.Doc originalDoc;
  FastTextPane.Doc alternateDoc;
  boolean alternateScreenBuffer;
  
  Color colXtermPalette[] = {
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

  public XtermConsole(WorkArea.View v, ProcessHandler ph, String textEncoding) {
    this.view = v;
    this.ph = ph;
    originalDoc = view.ftp.getDocument();
    alternateDoc = new FastTextPane.Doc();
    view.ftp.setKeyListener(this);
    xstd = new XtermStreamHandler(WorkArea.STYLE_BASH_OUT, textEncoding);
    xerr = new XtermStreamHandler(WorkArea.STYLE_BASH_ERR, textEncoding);
    AppSystem.addDisposable(this);
    Thread t = new Thread(this, "XtermConsoleTickler");
    t.setDaemon(true);
    t.start();
  }
  
  // implements Runnable
  @Override
  public void run() {
    while (running) {
      boolean flushedStd = xstd.flushBuf() > 0;
      boolean flushedErr = xerr.flushBuf() > 0;
      if (flushedStd || flushedErr) {
        tick = Math.max(tick/2, 10);
      } else {
        tick = Math.min(tick*2, 250);
      }
      AppSystem.sleep(tick);
    }
  }
  
  // implements Disposable
  @Override
  public void dispose() {
    running = false;
  }
  
  // implements Console
  @Override
  public void close() {
    AppSystem.dispose(this);
  }
  
  @Override
  public void reset() {
    xstd.xterm.flush();
    xstd.setTextDefault();
    xerr.xterm.flush();
    xerr.setTextDefault();
  }

  @Override
  public void stdout(String s) {
    view.ftp.addText(s, WorkArea.STYLE_BASH_OUT);
  }

  @Override
  public void stdout(byte b) {
    xstd.feed(b);
  }
  
  @Override
  public void stdout(byte[] b, int len) {
    xstd.feed(b, len);
  }

  @Override
  public void stderr(String s) {
    view.ftp.addText(s, WorkArea.STYLE_BASH_ERR);
  }

  @Override
  public void stderr(byte b) {
    xerr.feed(b);
  }

  @Override
  public void stderr(byte[] b, int len) {
    xerr.feed(b, len);
  }
  
  protected void setAlternateScreenBuffer(boolean b) {
    FastTextPane.Doc alt = alternateDoc;
    ProcessGroup pg = ph.getLinkedProcess();
    Log.println("set alt screen for " + (pg == null ? "null" : pg.toString()) + " : " + b);
    if (pg != null) {
      ProcessGroupInfo pgi = (ProcessGroupInfo)pg.getUserData();
      alt = pgi.alternateScreenBuffer;
      pgi.displayAlternateScreenBuffer = b;
    }
    if (b) {
      view.ftp.setDocument(alt);
      view.ftp.setTerminalMode(true);
    } else {
      view.ftp.setDocument(originalDoc);
      view.ftp.setTerminalMode(false);
    }
  }

  public void reviveScreenBuffer(ProcessGroup pg) {
    Log.println("revive screen for " + (pg == null ? "null" : pg.toString()));
    if (pg != null) {
      ProcessGroupInfo pgi = (ProcessGroupInfo)pg.getUserData();
      if (pgi.displayAlternateScreenBuffer) {
        view.ftp.setDocument(pgi.alternateScreenBuffer);
        view.ftp.setTerminalMode(true);
      }
    }
  }

  public void forceOriginalScreenBuffer() {
    view.ftp.setDocument(originalDoc);
    view.ftp.setTerminalMode(false);
  }


  /**
   * class XtermStreamHandler
   * handles xterm commands and data and bridges it to the FastTermPane
   * @author petera
   */
  class XtermStreamHandler implements XtermHandler {
    final XtermStream xterm;
    final FastTextPane.Style defStyle;
    Color colFG;
    Color colBG;
    boolean bold;
    boolean colInverse;
    byte buf[] = new byte[256];
    volatile int bufIx = 0;
    String textEnc;
    int savedCursorRow, savedCursorCol;
    
    public XtermStreamHandler(FastTextPane.Style def, String textEncoding) {
      textEnc = textEncoding;
      defStyle = new FastTextPane.Style(def);
      colFG = defStyle.getFg();
      colBG = defStyle.getBg();
      
      xterm = new XtermStream(this) {
        @Override
        public void data(byte[] data, int len, int offset) {
          putBuf(data, len);
        }
        @Override
        public void symbol(byte[] symdata, int len, int sym, int offset) {
          flushBuf();
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
      XtermStreamHandler.this.data(buf, bufIx);
      bufIx = 0;
      return oldBufIx;
    }
    
    public void feed(byte b) {
      xterm.feed(b);
    }
    
    public void feed(byte[] b, int len) {
      xterm.feed(b, len);
    }

    public void data(byte[] data, int len) {
      if (len <= 0) return;
      try {
        Color fg = colInverse ? colBG : colFG;
        Color bg = colInverse ? colFG : colBG;
        if (fg == null) {
          fg = colInverse ? view.ftp.getBackground() : view.ftp.getForeground();
        }
        view.ftp.addText(new String(data, 0, len, textEnc), defStyle.id, 
            fg, bg, bold);
      } catch (UnsupportedEncodingException e) {}
    }
  
    // implements XtermHandler
    
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
      view.ftp.setCursor(col-1, row-1);
    }
    @Override
    public void setCursorRow(int x) {
      view.ftp.setCursorRow(x-1);
    }
    @Override
    public void setCursorCol(int x) {
      view.ftp.setCursorColumn(x-1);
    }
    @Override
    public void cursorRow(int x) {
      int n = view.ftp.getCursorRow() + x;
      view.ftp.setCursorRow(n);
    }
    @Override
    public void cursorCol(int x) {
      int n = view.ftp.getCursorColumn() + x;
      view.ftp.setCursorColumn(n);
    }
    @Override
    public void saveCursor() {
      savedCursorRow = view.ftp.getCursorRow();
      savedCursorCol = view.ftp.getCursorColumn();
    }
    @Override
    public void cursorNewline(int x) {
      view.ftp.nextRow();
    }
    @Override
    public void cursorPrevline(int x) {
      view.ftp.prevRow();
    }
    @Override
    public void restoreCursor() {
      System.out.println("xtermcons.restore @ " + (savedCursorCol + 1) + "," + (savedCursorRow + 1) +
          " from " + (view.ftp.getCursorColumn() + 1) + "," + (view.ftp.getCursorRow() + 1));
      setCursorPosition(savedCursorCol, savedCursorRow);
    }
    @Override
    public void eraseLineRight() {
      view.ftp.eraseLineAfter();
    }
    @Override
    public void eraseLineLeft() {
      view.ftp.eraseLineBefore();
    }
    @Override
    public void eraseLineAll() {
      view.ftp.eraseLineFull();
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
      view.ftp.clear(); 
    }
    @Override
    public void eraseDisplaySavedLines() {
      //TODO
    }
    @Override
    public void delete(int chars) {
      view.ftp.deleteChars(chars);
    }
    @Override
    public void setScrollRegion(int minRow, int maxRow) {
      view.ftp.setScrollArea(minRow-1, maxRow-1);
    }
    @Override
    public void scroll(int lines) {
      view.ftp.scroll(lines);
    }
    @Override
    public void setAlternateScreenBuffer(boolean b) {
      XtermConsole.this.setAlternateScreenBuffer(b);
    }
  }

  // implements KeyListener
  
  static final byte VT_UP[] = new byte[] {(byte)0x1b, (byte)'[', (byte)'A'};
  static final byte VT_DOWN[] = new byte[] {(byte)0x1b, (byte)'[', (byte)'B'};
  static final byte VT_BACK[] = new byte[] {(byte)0x1b, (byte)'[', (byte)'D'};
  static final byte VT_FORW[] = new byte[] {(byte)0x1b, (byte)'[', (byte)'C'};
  static final byte VT_HOME[] = new byte[] {(byte)0x1b, (byte)'[', (byte)'H'};
  static final byte VT_END[] = new byte[] {(byte)0x1b, (byte)'[', (byte)'F'};
  static final byte VT_DELETE[] = new byte[] {(byte)0x1b, (byte)'[', (byte)'3', (byte)'~'};
  
  @Override
  public void keyTyped(KeyEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void keyPressed(KeyEvent e) {
    char c = e.getKeyChar();
    int d = e.getKeyCode();
    if (c > 0 && c <= 255 && d != KeyEvent.VK_ENTER && d != KeyEvent.VK_DELETE) {
      ph.sendToStdIn((byte)e.getKeyChar());
    } else {
      switch (d) {
      case KeyEvent.VK_UP: ph.sendToStdIn(VT_UP); break;
      case KeyEvent.VK_DOWN: ph.sendToStdIn(VT_DOWN); break;
      case KeyEvent.VK_LEFT: ph.sendToStdIn(VT_BACK); break;
      case KeyEvent.VK_RIGHT: ph.sendToStdIn(VT_FORW); break;
      case KeyEvent.VK_ENTER: ph.sendToStdIn((byte)13); break;
      case KeyEvent.VK_HOME: ph.sendToStdIn(VT_HOME); break;
      case KeyEvent.VK_END: ph.sendToStdIn(VT_END); break;
      case KeyEvent.VK_DELETE: ph.sendToStdIn(VT_DELETE); break;
      default: 
        System.out.println("unhandled " + e);
        break;
      }
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    // TODO Auto-generated method stub
    
  }
}
