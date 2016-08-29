package com.pelleplutt.tuscedo.ui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import com.pelleplutt.tuscedo.ProcessHandler;
import com.pelleplutt.tuscedo.Tuscedo;
import com.pelleplutt.util.AppSystem;

public class ProcessACTextField extends ACTextField implements ProcessHandler {
  Process process;
  InputStream in, err;
  OutputStream out;
  static final KeyStroke killKey = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);
  static final KeyStroke EOFKey = KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK);
  Object stdKillKeyAction;
  Object stdEOFKeyAction;
  
  public ProcessACTextField() {
  }
  
  public void linkToProcess(Process p) {
    setBackground(Tuscedo.colInputBashBg);
    process = p;
    in = p.getInputStream();
    err = p.getErrorStream();
    out = p.getOutputStream();
    stdKillKeyAction = getInputMap().get(killKey);
    getInputMap().put(killKey, "kill");
    getActionMap().put("kill", actionKill);
    stdEOFKeyAction = getInputMap().get(EOFKey);
    getInputMap().put(EOFKey, "eof");
    getActionMap().put("eof", actionEOF);
  }
  
  public boolean isLinkedToProcess() {
    return process != null;
  }
  
  int killProcess(Process p) {
    int returnCode = -1;
    AppSystem.closeSilently(in);
    AppSystem.closeSilently(err);
    AppSystem.closeSilently(out);
    p.destroy();
    try {
      returnCode = p.waitFor();
    } catch (InterruptedException ie) {}
    return returnCode;
  }
  
  public void closeStdin() {
    try {
      out.write(4);
      out.flush();
    } catch (Throwable t) {}
    AppSystem.closeSilently(out);
  }
  
  public int unlinkFromProcess() {
    setBackground(Tuscedo.colInputBg);
    int returnCode = -1;
    if (process != null) {
      killProcess(process);
      getInputMap().put(killKey, stdKillKeyAction);
      getInputMap().put(EOFKey, stdEOFKeyAction);
      process = null;
    }
    return returnCode;
  }
  
  public InputStream stdout() {
    return in;
  }
  
  public InputStream stderr() {
    return err;
  }
  
  public OutputStream stdin() {
    return out;
  }
  
  AbstractAction actionKill = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      unlinkFromProcess();
    }
  };

  AbstractAction actionEOF = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      closeStdin();
    }
  };
}
