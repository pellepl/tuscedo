package com.pelleplutt.tuscedo.ui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import com.pelleplutt.util.AppSystem;

public class ProcessACTextField extends ACTextField {
  Process process;
  InputStream in, err;
  OutputStream out;
  static final KeyStroke killKey = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);
  Object stdKillKeyAction;
  
  public ProcessACTextField() {
  }
  
  public void linkToProcess(Process p) {
    process = p;
    in = p.getInputStream();
    err = p.getErrorStream();
    out = p.getOutputStream();
    stdKillKeyAction = getInputMap().get(killKey);
    getInputMap().put(killKey, "kill");
    getActionMap().put("kill", actionKill);
  }
  
  public boolean isLinkedToProcess() {
    return process != null;
  }
  
  public int unlinkFromProcess() {
    int returnCode = -1;
    if (process != null) {
      Process p = process;
      p.destroy();
      try {
        returnCode = p.waitFor();
      } catch (InterruptedException ie) {}
      AppSystem.closeSilently(in);
      AppSystem.closeSilently(err);
      AppSystem.closeSilently(out);
      getInputMap().put(killKey, stdKillKeyAction);
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
}
