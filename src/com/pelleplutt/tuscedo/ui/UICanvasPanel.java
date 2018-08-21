package com.pelleplutt.tuscedo.ui;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.*;

import com.pelleplutt.tuscedo.*;

public class UICanvasPanel extends JPanel implements UIO {
  volatile BufferedImage pri, sec;
  JScrollPane scrl;
  Renderer renderer;
  Graphics2D g;
  Color color = Color.black;
  final UIInfo uiinfo;
  static int __id = 0;
  
  public UIInfo getUIInfo() {
    return uiinfo;
  }
  
  public UICanvasPanel(int w, int h) {
    uiinfo = new UIInfo(this, "canvas" + __id, "");
    UIInfo.fireEventOnCreated(uiinfo);

    __id++;
    pri = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
    sec = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
    Renderer renderer = new Renderer();
    Dimension d = new Dimension(w,h);
    renderer.setMinimumSize(d);
    renderer.setPreferredSize(d);
    renderer.setMaximumSize(d);
    scrl = new JScrollPane(renderer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrl.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE); // fix artefacts 
    UICommon.decorateScrollPane(scrl);
    setLayout(new BorderLayout());
    add(scrl, BorderLayout.CENTER);
    UICommon.defineCommonActions(renderer, JComponent.WHEN_IN_FOCUSED_WINDOW);

  }

  public void decorateUI() {
    // TODO decor
  }

  Graphics2D _g;
  private Graphics2D getSecGraphics() {
    if (_g == null) {
      _g = (Graphics2D)sec.getGraphics();
      _g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
      _g.setColor(color);
      _g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
    }
    return _g;
  }
  
  public void setColor(int color) {
    Graphics2D g = getSecGraphics();
    this.color = new Color(color);
    g.setColor(this.color);
  }
  
  public int getWidth() {
    return sec.getWidth();
  }
  
  public int getHeight() {
    return sec.getHeight();
  }
  
  public void drawLine(int x1, int y1, int x2, int y2) {
    Graphics2D g = getSecGraphics();
    g.drawLine(x1, y1, x2, y2);
  }
  
  public void drawRect(int x, int y, int w, int h) {
    Graphics2D g = getSecGraphics();
    g.drawRect(x,y,w,h);
  }
  
  public void fillRect() {
    Graphics2D g = getSecGraphics();
    g.fillRect(0,0,sec.getWidth(), sec.getHeight());
  }
  
  public void fillRect(int x, int y, int w, int h) {
    Graphics2D g = getSecGraphics();
    g.fillRect(x,y,w,h);
  }
  
  public void drawOval(int x, int y, int w, int h) {
    Graphics2D g = getSecGraphics();
    g.drawOval(x,y,w,h);
  }
  
  public void fillOval(int x, int y, int w, int h) {
    Graphics2D g = getSecGraphics();
    g.fillOval(x,y,w,h);
  }

  public void drawText(int x, int y, String s) {
    Graphics2D g = getSecGraphics();
    g.drawString(s, x, y);
  }
  
  public void blit() {
    pri.setData(sec.getData());
    repaint();
  }
  
  class Renderer extends JPanel {
    public void paint(Graphics og) {
      Graphics2D g = (Graphics2D)og;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
      g.drawImage(pri,0,0,this);
    }
  }
}
