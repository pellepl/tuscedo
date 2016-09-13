package com.pelleplutt.tuscedo.ui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import com.pelleplutt.tuscedo.ProcessGroup;
import com.pelleplutt.tuscedo.ProcessHandler;

public class ProcessACTextField extends ACTextField implements ProcessHandler {
  ProcessGroup process;
  static final KeyStroke killKey = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);
  static final KeyStroke EOFKey = KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK);
  static final KeyStroke backgroundKey = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK);
  Object stdKillKeyAction;
  Object stdEOFKeyAction;
  Object stdBGKeyAction;
  WorkArea workarea;
  
  public ProcessACTextField(WorkArea workarea) {
    this.workarea = workarea;
  }
  
  @Override
  public void linkToProcess(ProcessGroup p) {
    workarea.onLinkedProcess(this, p);
    process = p;
    stdKillKeyAction = getInputMap().get(killKey);
    getInputMap().put(killKey, "kill");
    getActionMap().put("kill", actionKill);
    stdEOFKeyAction = getInputMap().get(EOFKey);
    getInputMap().put(EOFKey, "eof");
    getActionMap().put("eof", actionEOF);
    stdBGKeyAction = getInputMap().get(backgroundKey);
    getInputMap().put(backgroundKey, "bg");
    getActionMap().put("bg", actionBG);
  }
  
  @Override
  public boolean isLinkedToProcess() {
    return process != null;
  }
  
  @Override
  public void closeStdin() {
    try {
      process.close();
    } catch (Throwable t) {}
  }
  
  @Override
  public void sendToStdIn(String line) {
    if (process != null) {
      try {
        process.writeLine(line);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
  
  @Override
  public void sendToBack() {
    if (isLinkedToProcess()) {
      process.setBackground(true);
      unlinkFromProcess();
    }
  }
  
  @Override
  public void unlinkFromProcess() {
    workarea.onUnlinkedProcess(this, process);
    if (process != null) {
      getInputMap().put(killKey, stdKillKeyAction);
      getInputMap().put(EOFKey, stdEOFKeyAction);
      process = null;
    }
  }
  
  @Override
  public ProcessGroup getLinkedProcess() {
    return process;
  }
  
  @Override
  public void kill()  {
      if (process != null) {
      process.kill(true);
      unlinkFromProcess();
    }
  }
  
  AbstractAction actionKill = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      kill();
    }
  };

  AbstractAction actionEOF = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      closeStdin();
    }
  };

  AbstractAction actionBG = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      sendToBack();
    }
  };
}
