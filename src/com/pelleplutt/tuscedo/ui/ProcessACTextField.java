package com.pelleplutt.tuscedo.ui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import com.pelleplutt.tuscedo.ProcessGroup;
import com.pelleplutt.tuscedo.ProcessHandler;
import com.pelleplutt.tuscedo.Tuscedo;

public class ProcessACTextField extends ACTextField implements ProcessHandler {
  ProcessGroup process;
  static final KeyStroke killKey = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);
  static final KeyStroke EOFKey = KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK);
  Object stdKillKeyAction;
  Object stdEOFKeyAction;
  
  public ProcessACTextField() {
  }
  
  public void linkToProcess(ProcessGroup p) {
    setBackground(Tuscedo.colInputBashBg);
    process = p;
    getInputMap().put(killKey, "kill");
    getActionMap().put("kill", actionKill);
    stdEOFKeyAction = getInputMap().get(EOFKey);
    getInputMap().put(EOFKey, "eof");
    getActionMap().put("eof", actionEOF);
  }
  
  public boolean isLinkedToProcess() {
    return process != null;
  }
  
  public void closeStdin() {
    try {
      process.close();
    } catch (Throwable t) {}
  }
  
  public void sendToStdIn(String line) {
    try {
      process.writeLine(line);
    } catch (IOException e) {
      System.out.println("WRITELINE " + line);
      e.printStackTrace();
    }
  }
  
  public int unlinkFromProcess() {
    setBackground(Tuscedo.colInputBg);
    int returnCode = -1;
    if (process != null) {
      returnCode = process.kill(true);
      getInputMap().put(killKey, stdKillKeyAction);
      getInputMap().put(EOFKey, stdEOFKeyAction);
      process = null;
    }
    return returnCode;
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
