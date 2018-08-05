package com.pelleplutt.tuscedo.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.*;

import com.pelleplutt.tuscedo.*;
import com.pelleplutt.tuscedo.ui.Scene3D.*;

public class UI3DPanel extends JPanel implements UIO {
  volatile BufferedImage pri;
  JScrollPane scrl;
  Renderer renderer;
  Graphics2D g;
  Color color = Color.black;
  final UIInfo uiinfo;
  static int __id = 0;
  
  static Cursor blankCursor;
  static {
    BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
  }
  
  RenderSpec renderSpec;
  
  public UIInfo getUIInfo() {
    return uiinfo;
  }
  
  public UI3DPanel(int w, int h, float[][] model) {
    uiinfo = new UIInfo(this, "3d" + __id, "");
    UIInfo.fireEventOnCreated(uiinfo);

    __id++;
    pri = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
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
    
    renderer.addMouseListener(mouseCtrl);
    renderer.addMouseMotionListener(mouseCtrl);
    renderer.addMouseWheelListener(mouseCtrl);
    
    try {
      robot = new Robot();
    } catch (AWTException e) {
      e.printStackTrace();
    }
    
    int when = JComponent.WHEN_IN_FOCUSED_WINDOW;
    UICommon.defineAnonAction(renderer, "3d.vis.mode", "f1", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.primitive++;
        if (renderSpec.primitive > RenderSpec.PRIMITIVE_DOTS) renderSpec.primitive = 0;
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.vis.cull", "f2", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.depthTest = !renderSpec.depthTest;
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.vis.depth", "f3", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.cullFaces = !renderSpec.cullFaces;
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mov.forw", "w", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.cameraWalk(1f);
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mov.back", "s", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.cameraWalk(-1f);
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mov.left", "a", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.cameraStrafe(-1f);
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mov.right", "d", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.cameraStrafe(1f);
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mov.up", "q", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.cameraDescend(1f);
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mov.down", "e", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.cameraDescend(-1f);
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mod.left", "left", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.modelMatrix.rotate(-0.1f, 0f, 1f, 0f);
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mod.right", "right", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.modelMatrix.rotate(0.1f, 0f, 1f, 0f);
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mod.up", "up", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.modelMatrix.rotate(-0.1f, 1f, 0f, 0f);
        blit();
      }
      }); 
    UICommon.defineAnonAction(renderer, "3d.mod.down", "down", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.modelMatrix.rotate(0.1f, 1f, 0f, 0f);
        blit();
      }
    });
    
    renderSpec = new RenderSpec();
    renderSpec.playerPos.set(0, 7, -10);
    renderSpec.cameraUpdate(0, (float)(-3f*Math.PI/4f*1000f), 0);
    renderSpec.cullFaces = false;
    renderSpec.depthTest = true;
    renderSpec.model = model;
    renderSpec.modelDataDirty = true;
    renderSpec.modelDirty = true;

  }
  public void decorateUI() {
    // TODO decor
  }
  
  Robot robot;
  MouseAdapter mouseCtrl = new MouseAdapter() {
    Point clickPointScreen;
    @Override
    public void mouseClicked(MouseEvent arg0) {
      // TODO Auto-generated method stub
    }
    @Override
    public void mouseDragged(MouseEvent e) {
      UI3DPanel.this.setCursor(blankCursor);
      int dx = e.getXOnScreen() - clickPointScreen.x; 
      int dy = e.getYOnScreen() - clickPointScreen.y;
      if (dx != 0 || dy != 0) {
        renderSpec.cameraUpdate(-dx, -dy, 0); // TODO test only
        robot.mouseMove(clickPointScreen.x, clickPointScreen.y);
        blit();
      }
    }
    @Override
    public void mouseEntered(MouseEvent arg0) {
      // TODO Auto-generated method stub
    }
    @Override
    public void mouseExited(MouseEvent arg0) {
      UI3DPanel.this.setCursor(null);
    }
    @Override
    public void mouseMoved(MouseEvent arg0) {
      // TODO Auto-generated method stub
    }
    @Override
    public void mousePressed(MouseEvent e) {
      clickPointScreen = new Point(e.getXOnScreen(), e.getYOnScreen());
    }
    @Override
    public void mouseReleased(MouseEvent arg0) {
      UI3DPanel.this.setCursor(null);
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
      renderSpec.cameraWalk(-2f * e.getWheelRotation());
      blit();
    }
  };

  Graphics2D _g;
  private Graphics2D getPriGraphics() {
    if (_g == null) {
      _g = (Graphics2D)pri.getGraphics();
      _g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
      _g.setColor(color);
      _g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
    }
    return _g;
  }
  
  public void setColor(int color) {
    Graphics2D g = getPriGraphics();
    this.color = new Color(color);
    g.setColor(this.color);
  }
  
  public int getWidth() {
    return pri.getWidth();
  }
  
  public int getHeight() {
    return pri.getHeight();
  }
  
  public void blit() {
    render();
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

  static final AffineTransform flip = AffineTransform.getScaleInstance(1d, -1d);
  void render() {
    Graphics2D g = getPriGraphics();
    BufferedImage bi = Tuscedo.inst().render3d(renderSpec);
    AffineTransform tran = AffineTransform.getTranslateInstance(0,
        bi.getHeight());
    tran.concatenate(flip);
    g.setTransform(tran);
    g.drawImage(bi, 0, 0, null);
  }
}
