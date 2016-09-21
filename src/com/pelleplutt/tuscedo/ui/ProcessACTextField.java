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
  
  static final KeyStroke upKey = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
  static final KeyStroke downKey = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
  static final KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
  static final KeyStroke tabKey = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
  static final KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
  Object stdKillKeyAction;
  Object stdEOFKeyAction;
  Object stdBGKeyAction;
  Object stdUpKeyAction;
  Object stdDownKeyAction;
  Object stdEnterKeyAction;
  Object stdTabKeyAction;
  Object stdEscKeyAction;
  WorkArea workarea;
  
  public ProcessACTextField(WorkArea workarea) {
    this.workarea = workarea;
    
    stdUpKeyAction = getInputMap().get(upKey);
    getInputMap().put(upKey, "iup");
    getActionMap().put("iup", actionUp);
    stdDownKeyAction = getInputMap().get(downKey);
    getInputMap().put(downKey, "idown");
    getActionMap().put("idown", actionDown);
    stdEnterKeyAction = getInputMap().get(enterKey);
    getInputMap().put(enterKey, "ienter");
    getActionMap().put("ienter", actionEnter);
    stdTabKeyAction = getInputMap().get(tabKey);
    getInputMap().put(tabKey, "itab");
    getActionMap().put("itab", actionTab);
    stdEscKeyAction = getInputMap().get(escKey);
    getInputMap().put(escKey, "iesc");
    getActionMap().put("iesc", actionEsc);

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
    getActionMap().put("bg", actionBg);
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
  public void sendToStdIn(byte b[]) {
    if (process != null) {
      try {
        process.write(b, 0, b.length);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
  
  @Override
  public void sendToStdIn(byte b[], int offs, int len) {
    if (process != null) {
      try {
        process.write(b, offs, len);
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
  
  AbstractAction actionBg = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      sendToBack();
    }
  };

  
  

  AbstractAction actionUp = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (workarea.onKeyUp(e)) {
        ProcessACTextField.this.getActionMap().get(stdUpKeyAction).actionPerformed(e);
      }
    }
  };
  AbstractAction actionDown = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (workarea.onKeyDown(e)) {
        ProcessACTextField.this.getActionMap().get(stdDownKeyAction).actionPerformed(e);
      }
    }
  };
  AbstractAction actionEnter = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (workarea.onKeyEnter(e)) {
        ProcessACTextField.this.getActionMap().get(stdEnterKeyAction).actionPerformed(e);
      }
    }
  };
  AbstractAction actionTab = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (workarea.onKeyTab(e)) {
        ProcessACTextField.this.getActionMap().get(stdTabKeyAction).actionPerformed(e);
      }
    }
  };
  AbstractAction actionEsc = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (workarea.onKeyEsc(e)) {
        ProcessACTextField.this.getActionMap().get(stdEscKeyAction).actionPerformed(e);
      }
    }
  };
}
