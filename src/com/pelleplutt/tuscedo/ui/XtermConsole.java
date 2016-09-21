package com.pelleplutt.tuscedo.ui;

import java.awt.Color;
import java.io.UnsupportedEncodingException;

import com.pelleplutt.tuscedo.Console;
import com.pelleplutt.tuscedo.ProcessHandler;
import com.pelleplutt.tuscedo.XtermHandler;
import com.pelleplutt.tuscedo.XtermStream;
import com.pelleplutt.util.FastTextPane;

public class XtermConsole implements Console {
  WorkArea.View view;
  ProcessHandler ph;
  XtermStreamHandler xstd, xerr;
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
    xstd = new XtermStreamHandler(WorkArea.STYLE_BASH_OUT, textEncoding) {
      @Override
      public void data(byte[] data, int len) {
        try {
          view.ftp.addText(new String(data, 0, len, textEnc), defStyle.id, 
              colInverse ? colBG : colFG, colInverse ? colFG : colBG, bold);
        } catch (UnsupportedEncodingException e) {}
      }
    };
    xerr = new XtermStreamHandler(WorkArea.STYLE_BASH_ERR, textEncoding) {
      @Override
      public void data(byte[] data, int len) {
        try {
          view.ftp.addText(new String(data, 0, len, textEnc), defStyle.id, 
              colInverse ? colBG : colFG, colInverse ? colFG : colBG, bold);
        } catch (UnsupportedEncodingException e) {}
      }
    };
  }
  
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

  abstract class XtermStreamHandler implements XtermHandler {
    final XtermStream xterm;
    final FastTextPane.Style defStyle;
    Color colFG;
    Color colBG;
    boolean bold;
    boolean colInverse;
    byte buf[] = new byte[256];
    volatile int bufIx = 0;
    String textEnc;
    
    public XtermStreamHandler(FastTextPane.Style def, String textEncoding) {
      textEnc = textEncoding;
      defStyle = new FastTextPane.Style(def);
      colFG = defStyle.getFg();
      colBG = defStyle.getBg();
      
      xterm = new XtermStream(this) {
        @Override
        public void symbol(byte[] symdata, int len, int sym) {
          flushBuf();
          super.symbol(symdata, len, sym);
        }
        @Override
        public void data(byte[] data, int len) {
          putBuf(data, len);
        }
      };
    }
    
    void putBuf(final byte[] data, final int len) {
      for (int i = 0; i < len; i++) {
        byte b  = data[i];
        buf[bufIx++] = b;
        if (b == '\n' || b == '\r'|| bufIx >= buf.length) {
          flushBuf();
        }
      }
    }
    
    void flushBuf() {
      XtermStreamHandler.this.data(buf, bufIx);
      bufIx = 0;
    }
    
    public void feed(byte b) {
      xterm.feed(b);
    }
    
    public void feed(byte[] b, int len) {
      xterm.feed(b, len);
    }

    abstract public void data(byte[] data, int len);
  
    // Xterm impl
    
    @Override
    public void setTextFgColor(int palette) {
      colFG = palette > 0 ? colXtermPalette[palette] : defStyle.getFg();
      if (colFG == null) colFG = view.ftp.getForeground();
    }
    @Override
    public void setTextBgColor(int palette) {
      colBG = palette > 0 ? colXtermPalette[palette] : defStyle.getBg();
      if (colBG == null) colBG = view.ftp.getBackground();
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
      if (colFG == null) colFG = view.ftp.getForeground();
      if (colBG == null) colBG = view.ftp.getBackground();
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
    public void cursorRow(int x) {
      int n = view.ftp.getCursorRow() + x;
      view.ftp.setCursorRow(Math.min(view.ftp.getHeightChars(), Math.max(0, n)));
    }
    @Override
    public void cursorCol(int x) {
      int n = view.ftp.getCursorColumn() + x;
      view.ftp.setCursorColumn(Math.min(view.ftp.getHeightChars(), Math.max(0, n)));
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
  }
}
