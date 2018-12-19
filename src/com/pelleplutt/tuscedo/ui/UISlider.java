package com.pelleplutt.tuscedo.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

public class UISlider extends JPanel implements UIO {
  static int __sliderid = 0;
  final UIInfo uiinfo;
  public int min, max, val;
  MouseAdapter mouseAdapter;
  
  @Override
  public UIInfo getUIInfo() { return uiinfo; }
  public void onClose() {}

  public UISlider() {
    min = 0;
    max = 10;
    val = 5;
    uiinfo = new UIInfo(this, "slider" + __sliderid, "");
    UIInfo.fireEventOnCreated(uiinfo);
    __sliderid++;
    setLayout(new BorderLayout());
    setPreferredSize(new Dimension(30,UICommon.font.getSize()));
    setMinimumSize(new Dimension(30,UICommon.font.getSize()));
    setMaximumSize(new Dimension(30,UICommon.font.getSize()));
    decorateUI();
    mouseAdapter = new MouseAdapter() {
      @Override
      public void mouseDragged(MouseEvent e) {
      }

      @Override
      public void mousePressed(MouseEvent e) {
      }

      @Override
      public void mouseReleased(MouseEvent e) {
      }

    };
    addMouseListener(mouseAdapter);
    addMouseMotionListener(mouseAdapter);
  }
  
  public void decorateUI() {
    setBackground(UICommon.colScrollBarDBg);
    setForeground(UICommon.colScrollBarLFg);
  }
  
  final int handleW = 9;
  public void paint(Graphics g1) 
  {
    Graphics2D g = (Graphics2D)g1;
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int w = getWidth();
    int h = getHeight();
    g.setColor(getBackground());
    g.fillRect(0, 0, w, h);
    if (max - min <= 0) return;
    int x = (w * val) / (max - min);
    g.setColor(getForeground());
    g.fillRoundRect(x-handleW/2, 0, handleW, h, 6, 6);
    
  }
}
