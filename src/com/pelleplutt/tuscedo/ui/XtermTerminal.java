package com.pelleplutt.tuscedo.ui;

import com.pelleplutt.tuscedo.Console;
import com.pelleplutt.tuscedo.ProcessGroup;
import com.pelleplutt.tuscedo.ProcessGroupInfo;
import com.pelleplutt.tuscedo.ProcessHandler;
import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.AppSystem.Disposable;
import com.pelleplutt.util.FastTermPane;
import com.pelleplutt.util.FastTextPane;
import com.pelleplutt.util.Log;

/**
 * Bridges a process' xterm output to a FastTermPane.
 * Being a Console, it registers as a ProcessHanlder and this gets all 
 * output from the process. This is bridged to XtermStreams via 
 * XtermParserTerminals, which in turn* parses the xterm data from the 
 * process and formats it in given FastTermPane.
 * Contains two XtermParserTerminals, one for stdout and one for stderr.
 * Takes care of handling output from the XtermStreams for out and err. 
 * Also takes care of key input events and translates them to proper
 * VT_* sequences to the process.
 * @author petera
 */
public class XtermTerminal implements Console, XConsole, Disposable, Runnable {
  FastTermPane ftp;
  ProcessHandler ph;
  XtermParserTerminal xstd, xerr;
  volatile boolean running = true;
  long tick = 50;
  FastTextPane.Doc originalDoc;
  FastTextPane.Doc alternateDoc;
  boolean alternateScreenBuffer;
  
  public XtermTerminal(FastTermPane ftp, ProcessHandler ph, String textEncoding) {
    this.ftp = ftp;
    this.ph = ph;
    originalDoc = ftp.getDocument();
    alternateDoc = new FastTextPane.Doc();
    ftp.setKeyListener(new XtermKeyListener() {
      @Override
      public void sendVT(byte[] b) {
        XtermTerminal.this.ph.sendToStdIn(b);
      }
      @Override
      public void sendVT(byte b) {
        XtermTerminal.this.ph.sendToStdIn(b);
      }
    });
    xstd = new XtermParserTerminal(ftp, this, WorkArea.STYLE_BASH_OUT, textEncoding);
    xerr = new XtermParserTerminal(ftp, this, WorkArea.STYLE_BASH_ERR, textEncoding);
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
    ftp.addText(s, WorkArea.STYLE_BASH_OUT);
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
    ftp.addText(s, WorkArea.STYLE_BASH_ERR);
  }

  @Override
  public void stderr(byte b) {
    xerr.feed(b);
  }

  @Override
  public void stderr(byte[] b, int len) {
    xerr.feed(b, len);
  }

  // implements XConsole
  @Override
  public void setAlternateScreenBuffer(FastTermPane ftp, boolean b) {
    FastTextPane.Doc alt = alternateDoc;
    ProcessGroup pg = ph.getLinkedProcess();
    Log.println("set alt screen for " + (pg == null ? "null" : pg.toString()) + " : " + b);
    if (pg != null) {
      ProcessGroupInfo pgi = (ProcessGroupInfo)pg.getUserData();
      alt = pgi.alternateScreenBuffer;
      pgi.displayAlternateScreenBuffer = b;
    }
    if (b) {
      ftp.setDocument(alt);
      ftp.setTerminalMode(true);
    } else {
      ftp.setDocument(originalDoc);
      ftp.setTerminalMode(false);
    }
  }

  public void reviveScreenBuffer(ProcessGroup pg) {
    Log.println("revive screen for " + (pg == null ? "null" : pg.toString()));
    if (pg != null) {
      ProcessGroupInfo pgi = (ProcessGroupInfo)pg.getUserData();
      if (pgi.displayAlternateScreenBuffer) {
        ftp.setDocument(pgi.alternateScreenBuffer);
        ftp.setTerminalMode(true);
      }
    }
  }

  public void forceOriginalScreenBuffer() {
    ftp.setDocument(originalDoc);
    ftp.setTerminalMode(false);
  }
 
}
