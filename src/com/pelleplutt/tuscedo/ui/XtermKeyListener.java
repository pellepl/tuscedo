package com.pelleplutt.tuscedo.ui;

import java.awt.event.*;

import javax.swing.*;

public abstract class XtermKeyListener implements KeyListener {
  
  public static final byte XT_VT_UP[] = new byte[] {(byte)0x1b, (byte)'[', (byte)'A'};
  public static final byte XT_VT_DOWN[] = new byte[] {(byte)0x1b, (byte)'[', (byte)'B'};
  public static final byte XT_VT_BACK[] = new byte[] {(byte)0x1b, (byte)'[', (byte)'D'};
  public static final byte XT_VT_FORW[] = new byte[] {(byte)0x1b, (byte)'[', (byte)'C'};
  public static final byte XT_VT_HOME[] = new byte[] {(byte)0x1b, (byte)'[', (byte)'H'};
  public static final byte XT_VT_END[] = new byte[] {(byte)0x1b, (byte)'[', (byte)'F'};
  public static final byte XT_VT_DELETE[] = new byte[] {(byte)0x1b, (byte)'[', (byte)'3', (byte)'~'};

  public abstract void sendVT(byte b);
  public abstract void sendVT(byte b[]);
  
  @Override
  public void keyTyped(KeyEvent e) {
  }

  @Override
  public void keyPressed(KeyEvent e) {
    char c = e.getKeyChar();
    int d = e.getKeyCode();
    if (c > 0 && c <= 255 && d != KeyEvent.VK_ENTER && d != KeyEvent.VK_DELETE) {
      sendVT((byte)e.getKeyChar());
    } else {
      switch (d) {
      case KeyEvent.VK_UP: sendVT(XT_VT_UP); break;
      case KeyEvent.VK_DOWN: sendVT(XT_VT_DOWN); break;
      case KeyEvent.VK_LEFT: sendVT(XT_VT_BACK); break;
      case KeyEvent.VK_RIGHT: sendVT(XT_VT_FORW); break;
      case KeyEvent.VK_ENTER: sendVT((byte)13); break;
      case KeyEvent.VK_HOME: sendVT(XT_VT_HOME); break;
      case KeyEvent.VK_END: sendVT(XT_VT_END); break;
      case KeyEvent.VK_DELETE: sendVT(XT_VT_DELETE); break;
      default: 
        System.out.println("xterm keypressed unhandled " + e);
        break;
      }
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
  }
}
