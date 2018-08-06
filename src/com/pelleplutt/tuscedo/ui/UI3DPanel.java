package com.pelleplutt.tuscedo.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.*;

import com.pelleplutt.tuscedo.*;
import com.pelleplutt.tuscedo.Timer;

public class UI3DPanel extends JPanel implements UIO {
  static final int MOVE_UP = (1<<0);
  static final int MOVE_DOWN = (1<<1);
  static final int MOVE_LEFT = (1<<2);
  static final int MOVE_RIGHT = (1<<3);
  static final int MOVE_FORWARD = (1<<4);
  static final int MOVE_BACK = (1<<5);
  static final int MODEL_ROLL_LEFT = (1<<6);
  static final int MODEL_ROLL_RIGHT = (1<<7);
  static final int MODEL_PITCH_UP = (1<<8);
  static final int MODEL_PITCH_DOWN = (1<<9);
  
  volatile int keys = 0;
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
  
  Timer.Entry timerEntry = null;
  Runnable keyTask = new Runnable() {
    public void run() {
      if ((keys & MOVE_FORWARD) != 0) {
        renderSpec.cameraWalk(.5f);
      }
      else if ((keys & MOVE_BACK) != 0) {
        renderSpec.cameraWalk(-.5f);
      }
      if ((keys & MOVE_LEFT) != 0) {
        renderSpec.cameraStrafe(-.5f);
      }
      else if ((keys & MOVE_RIGHT) != 0) {
        renderSpec.cameraStrafe(.5f);
      }
      if ((keys & MOVE_UP) != 0) {
        renderSpec.cameraDescend(.5f);
      }
      else if ((keys & MOVE_DOWN) != 0) {
        renderSpec.cameraDescend(-.5f);
      }
      if ((keys & MODEL_ROLL_LEFT) != 0) {
        renderSpec.modelMatrix.rotate(-0.02f, 0f, 1f, 0f);
      }
      else if ((keys & MODEL_ROLL_RIGHT) != 0) {
        renderSpec.modelMatrix.rotate(0.02f, 0f, 1f, 0f);
      }
      if ((keys & MODEL_PITCH_UP) != 0) {
        renderSpec.modelMatrix.rotate(-0.02f, 1f, 0f, 0f);
      }
      else if ((keys & MODEL_PITCH_DOWN) != 0) {
        renderSpec.modelMatrix.rotate(0.02f, 1f, 0f, 0f);
      }
      blit();
    }
  };
  
  void triggerKeys(int oldmask, int newmask) {
    keys = newmask;
    if (oldmask == 0) {
      if (timerEntry != null) {
        timerEntry.stop();
        timerEntry = null;
      }
      timerEntry = Tuscedo.inst().getTimer().addTask(keyTask, 0, 20);
    }
    if (newmask == 0) {
      if (timerEntry != null) {
        timerEntry.stop();
        timerEntry = null;
      }
    }
  }
  
  void registerMotionKeys(String key, String actionName, final int keymask) {
    UICommon.defineAnonAction(renderer, actionName + ".press", key, 
        JComponent.WHEN_IN_FOCUSED_WINDOW, true, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        triggerKeys(keys, keys |= keymask);
      }
    });
    UICommon.defineAnonAction(renderer, actionName + ".release", key, 
        JComponent.WHEN_IN_FOCUSED_WINDOW, false, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        triggerKeys(keys, keys &= ~keymask);
      }
    });

  }
  
  public UI3DPanel(int w, int h, float[][] model) {
    uiinfo = new UIInfo(this, "3d" + __id, "");
    UIInfo.fireEventOnCreated(uiinfo);

    __id++;
    pri = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
    renderer = new Renderer();
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
    UICommon.defineAnonAction(renderer, "3d.mode", "f1", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.primitive++;
        if (renderSpec.primitive > RenderSpec.PRIMITIVE_DOTS) renderSpec.primitive = 0;
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.grid.size", "f2", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        float m = renderSpec.gridMul;
        int zeroes = (int)Math.round(Math.log10(m));
        if ((int)(m / Math.pow(10, zeroes)) != 1) {
          m *= 2;
        } else {
          m *= 5;
        }
        if (m > 1000f) m = 1f;
        renderSpec.gridMul = m;
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.grid.contrast", "f3", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.gridContrast += 0.25f;
        if (renderSpec.gridContrast > 1.0f) renderSpec.gridContrast = 0.0f;
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.light.pos", "f4", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.lightPos.set(renderSpec.playerPos);
        blit();
      }
    });
    registerMotionKeys("w", "3d.mov.forw", MOVE_FORWARD);
    registerMotionKeys("s", "3d.mov.back", MOVE_BACK);
    registerMotionKeys("a", "3d.mov.left", MOVE_LEFT);
    registerMotionKeys("d", "3d.mov.right", MOVE_RIGHT);
    registerMotionKeys("q", "3d.mov.up", MOVE_UP);
    registerMotionKeys("e", "3d.mov.down", MOVE_DOWN);
    registerMotionKeys("up", "3d.mod.up", MODEL_PITCH_UP);
    registerMotionKeys("down", "3d.mod.down", MODEL_PITCH_DOWN);
    registerMotionKeys("left", "3d.mod.left", MODEL_ROLL_LEFT);
    registerMotionKeys("right", "3d.mod.right", MODEL_ROLL_RIGHT);
    
    renderSpec = new RenderSpec();
    renderSpec.playerPos.set(0, model.length, model.length);
    renderSpec.qdir.set(0,0,0,1);
    renderSpec.cameraUpdate(0, -0.7f*1000f, 0);

    renderSpec.cullFaces = false;
    renderSpec.depthTest = true;
    renderSpec.model = model;
    renderSpec.modelDataDirty = true;
    renderSpec.modelDirty = true;
    renderSpec.width = w;
    renderSpec.height = h;
    renderSpec.dimensionDirty = true;
    renderSpec.lightPos.set(40000f, 40000f, 15000f);
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
  
  class Renderer extends JPanel {
    public void paint(Graphics og) {
      Graphics2D g = (Graphics2D)og;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
      g.drawImage(pri,0,0,this);
    }
  }

  public void blit() {
    render();
    repaint();
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

  public void setPlayerPosition(float x, float y, float z) {
    renderSpec.playerPos.set(x,y,z);
    blit();
  }
  public void setPlayerView(float yaw, float pitch, float roll) {
    renderSpec.qdir.set(0,0,0,1);
    renderSpec.cameraUpdate(yaw*1000f, pitch*1000f, roll*1000f);
    blit();
  }
  public void setSize(int w, int h) {
    super.setSize(w, h);
    Dimension d = new Dimension(w,h);
    renderer.setMinimumSize(d);
    renderer.setPreferredSize(d);
    renderer.setMaximumSize(d);
    _g = null;
    pri = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
    renderSpec.width = w;
    renderSpec.height = h;
    renderSpec.dimensionDirty = true;
    blit();
    revalidate();
  }
}
