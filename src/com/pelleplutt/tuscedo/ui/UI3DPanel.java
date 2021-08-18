package com.pelleplutt.tuscedo.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.pelleplutt.tuscedo.*;
import com.pelleplutt.tuscedo.Timer;
import com.pelleplutt.tuscedo.ui.UIInfo.*;

public class UI3DPanel extends JPanel implements UIO, UIListener {
  static final int MOVE_UP = (1<<0);
  static final int MOVE_DOWN = (1<<1);
  static final int MOVE_LEFT = (1<<2);
  static final int MOVE_RIGHT = (1<<3);
  static final int MOVE_FORWARD = (1<<4);
  static final int MOVE_BACK = (1<<5);
  static final int MOVE_ROLL_LEFT = (1<<6);
  static final int MOVE_ROLL_RIGHT = (1<<7);
  static final int MOVE_PITCH_LEFT = (1<<8);
  static final int MOVE_PITCH_RIGHT = (1<<9);
  static final int MOVE_YAW_LEFT = (1<<10);
  static final int MOVE_YAW_RIGHT = (1<<11);
  static final int MODEL_ROLL_LEFT = (1<<12);
  static final int MODEL_ROLL_RIGHT = (1<<13);
  static final int MODEL_PITCH_UP = (1<<14);
  static final int MODEL_PITCH_DOWN = (1<<15);

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
  public void onClose() {
    if (timerEntry != null) {
      timerEntry.stop();
      timerEntry = null;
    }
    for (RenderSpec rs : specs) {
      UIInfo parent = rs.getUIInfo().getParent();
      if (parent == null) {
        rs.glfinalize();
      }
    }
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
      if ((keys & MOVE_PITCH_LEFT) != 0) {
        renderSpec.cameraUpdate(0, -10f, 0);
      }
      else if ((keys & MOVE_PITCH_RIGHT) != 0) {
        renderSpec.cameraUpdate(0, 10f, 0);
      }
      if ((keys & MOVE_YAW_LEFT) != 0) {
        renderSpec.cameraUpdate(-10f, 0, 0);
      }
      else if ((keys & MOVE_YAW_RIGHT) != 0) {
        renderSpec.cameraUpdate(10f, 0, 0);
      }
      if ((keys & MOVE_ROLL_LEFT) != 0) {
        renderSpec.cameraUpdate(0, 0, -10f);
      }
      else if ((keys & MOVE_ROLL_RIGHT) != 0) {
        renderSpec.cameraUpdate(0, 0, 10f);
      }
      if ((keys & MODEL_ROLL_LEFT) != 0) {
        renderSpec.modelRotate(-0.02f, 0f, 1f, 0f);
      }
      else if ((keys & MODEL_ROLL_RIGHT) != 0) {
        renderSpec.modelRotate(0.02f, 0f, 1f, 0f);
      }
      if ((keys & MODEL_PITCH_UP) != 0) {
        renderSpec.modelRotate(-0.02f, 1f, 0f, 0f);
      }
      else if ((keys & MODEL_PITCH_DOWN) != 0) {
        renderSpec.modelRotate(0.02f, 1f, 0f, 0f);
      }
      if ((keys & (MOVE_ROLL_RIGHT | MOVE_ROLL_LEFT)) == 0) {
        renderSpec.cameraUpdate(0,0,0);
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
        if (!UI3DPanel.this.isFocusOwner()) return;
        triggerKeys(keys, keys |= keymask);
      }
    });
    UICommon.defineAnonAction(renderer, actionName + ".release", key,
        JComponent.WHEN_IN_FOCUSED_WINDOW, false, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!UI3DPanel.this.isFocusOwner()) return;
        triggerKeys(keys, keys &= ~keymask);
      }
    });

  }

  public UI3DPanel(int w, int h, float[][] model) {
    uiinfo = new UIInfo(this, "3d" + __id, "3d"+__id);
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
    setLayout(new BorderLayout());
    add(scrl, BorderLayout.CENTER);
    add(new UISlider(), BorderLayout.SOUTH);
    decorateUI();

    renderer.addMouseListener(mouseCtrl);
    renderer.addMouseMotionListener(mouseCtrl);
    renderer.addMouseWheelListener(mouseCtrl);

    try {
      robot = new Robot();
    } catch (AWTException e) {
      e.printStackTrace();
    }

    int when = JComponent.WHEN_IN_FOCUSED_WINDOW;
    UICommon.defineCommonActions(renderer, when);

    UICommon.defineAnonAction(renderer, "3d.mode", "f1", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.primitive++;
        if (renderSpec.primitive > RenderSpec.PRIMITIVE_DOTS) renderSpec.primitive = 0;
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.grid.size", "f3", when, new AbstractAction() {
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
    UICommon.defineAnonAction(renderer, "3d.grid.contrast", "f2", when, new AbstractAction() {
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
    UICommon.defineAnonAction(renderer, "3d.light.toggleshadows", "f5", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.disableShadows = !renderSpec.disableShadows;
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.color.smoothflat", "f6", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.smoothOrFlat = renderSpec.smoothOrFlat == 0 ? 1 : 0;
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.color.togglecheckered", "f7", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.checkered = !renderSpec.checkered;
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mov.reset", "numpad5", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        renderSpec.qdir.identity();
        renderSpec.cameraUpdate(0, 0, 0);
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mov.origo", "0", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!UI3DPanel.this.isFocusOwner()) return;
        renderSpec.cameraPosition(0, 0, 0);
        renderSpec.qdir.identity();
        renderSpec.cameraUpdate(0, 0, 0);
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mov.distlookforw", "1", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!UI3DPanel.this.isFocusOwner()) return;
        renderSpec.cameraPosition(0,5,200);
        renderSpec.qdir.identity();
        renderSpec.cameraUpdate(0, 0, 0);
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mov.distlookback", "2", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!UI3DPanel.this.isFocusOwner()) return;
        renderSpec.cameraPosition(0,5,-200);
        renderSpec.qdir.identity().rotationY((float)Math.PI);
        renderSpec.cameraUpdate(0, 0, 0);
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mov.distlookleft", "3", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!UI3DPanel.this.isFocusOwner()) return;
        renderSpec.cameraPosition(-200,5,0);
        renderSpec.qdir.identity().rotationY((float)-Math.PI*0.5f);
        renderSpec.cameraUpdate(0, 0, 0);
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mov.distlookright", "4", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!UI3DPanel.this.isFocusOwner()) return;
        renderSpec.cameraPosition(200,5,0);
        renderSpec.qdir.identity().rotationY((float)Math.PI*0.5f);
        renderSpec.cameraUpdate(0, 0, 0);
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mov.distlookup", "5", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!UI3DPanel.this.isFocusOwner()) return;
        renderSpec.cameraPosition(0,200,0);
        renderSpec.qdir.identity().rotationX((float)-Math.PI*0.5f);
        renderSpec.cameraUpdate(0, 0, 0);
        blit();
      }
    });
    UICommon.defineAnonAction(renderer, "3d.mov.distlookdown", "6", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!UI3DPanel.this.isFocusOwner()) return;
        renderSpec.cameraPosition(0,-200,0);
        renderSpec.qdir.identity().rotationX((float)Math.PI*0.5f);
        renderSpec.cameraUpdate(0, 0, 0);
        blit();
      }
    });
    registerMotionKeys("w", "3d.mov.forw", MOVE_FORWARD);
    registerMotionKeys("s", "3d.mov.back", MOVE_BACK);
    registerMotionKeys("a", "3d.mov.left", MOVE_LEFT);
    registerMotionKeys("d", "3d.mov.right", MOVE_RIGHT);
    registerMotionKeys("q", "3d.mov.up", MOVE_UP);
    registerMotionKeys("e", "3d.mov.down", MOVE_DOWN);
    registerMotionKeys("numpad8", "3d.mov.pitl", MOVE_PITCH_LEFT);
    registerMotionKeys("numpad2", "3d.mov.pitr", MOVE_PITCH_RIGHT);
    registerMotionKeys("numpad4", "3d.mov.yawl", MOVE_YAW_LEFT);
    registerMotionKeys("numpad6", "3d.mov.yawr", MOVE_YAW_RIGHT);
    registerMotionKeys("z", "3d.mov.roll", MOVE_ROLL_LEFT);
    registerMotionKeys("c", "3d.mov.rolr", MOVE_ROLL_RIGHT);
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
    renderSpec.lightPos.set(-60, 200, 300);

    addRenderSpec(renderSpec);
  }
  public void decorateUI() {
    UICommon.decorateScrollPane(scrl);
  }

  Robot robot;
  MouseAdapter mouseCtrl = new MouseAdapter() {
    Point clickPointScreen;
    @Override
    public void mouseClicked(MouseEvent arg0) {
      UI3DPanel.this.requestFocus();
    }
    @Override
    public void mouseDragged(MouseEvent e) {
      UI3DPanel.this.setCursor(blankCursor);
      int dx = e.getXOnScreen() - clickPointScreen.x;
      int dy = e.getYOnScreen() - clickPointScreen.y;
      if (dx != 0 || dy != 0) {
        renderSpec.cameraUpdate(-dx*2f, -dy*2f, 0);
        robot.mouseMove(clickPointScreen.x, clickPointScreen.y);
        if (timerEntry == null) blit();
      }
    }
    @Override
    public void mouseEntered(MouseEvent arg0) {
    }
    @Override
    public void mouseExited(MouseEvent arg0) {
      UI3DPanel.this.setCursor(null);
      triggerKeys(keys, 0);
    }
    @Override
    public void mouseMoved(MouseEvent arg0) {
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
      if (timerEntry == null) blit();
    }
  };

  Graphics2D _g;
  private Graphics2D getPriGraphics() {
    if (_g == null) {
      _g = (Graphics2D)pri.getGraphics();
      _g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
      _g.setColor(color);
      _g.setFont(new Font(UICommon.COMMON_FONT, Font.PLAIN, 10));
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

  volatile boolean rendering = false;
  public void blit() {
    if (rendering) return;
    rendering = true;
    SwingUtilities.invokeLater(() -> {
      render();
      repaint();
      rendering = false;
    });
  }

  static final AffineTransform flip = AffineTransform.getScaleInstance(1d, -1d);
  synchronized void render() {
    Graphics2D g = getPriGraphics();
    BufferedImage bi = Tuscedo.inst().render3d(renderSpec);
    AffineTransform tran = AffineTransform.getTranslateInstance(0,
        bi.getHeight());
    tran.concatenate(flip);
    g.setFont(UICommon.font);
    g.setTransform(tran);
    g.drawImage(bi, 0, 0, null);
    g.setTransform(AffineTransform.getScaleInstance(1.0, 1.0));
    g.setColor(Color.white);
    g.drawString(String.format("POS  x:%-8.1f y:%-8.1f z:%-8.1f"
        , renderSpec.playerPos.x
        , renderSpec.playerPos.y
        , renderSpec.playerPos.z), 0, 12);
    g.drawString(String.format("LOOK x:%-8.1f y:%-8.1f z:%-8.1f"
        , -renderSpec.vdirz.x
        , -renderSpec.vdirz.y
        , -renderSpec.vdirz.z), 0, 24);
//    float s, t;
//    s = (float)Math.atan2(renderSpec.vdirx.y, renderSpec.vdirx.x);
//    t = (float)Math.acos(renderSpec.vdirx.z / 1f);
//    g.drawString(String.format("ANGX [s:%-3.1f] t:%-3.1f"
//        , Math.toDegrees(s)
//        , Math.toDegrees(t)
//        ), 100, 36);
//    s = (float)Math.atan2(renderSpec.vdiry.y, renderSpec.vdiry.x);
//    t = (float)Math.acos(renderSpec.vdiry.z / 1f);
//    g.drawString(String.format("ANGY  s:%-3.1f  t:%-3.1f"
//        , Math.toDegrees(s)
//        , Math.toDegrees(t)
//        ), 100, 48);
//    s = (float)Math.atan2(renderSpec.vdirz.y, renderSpec.vdirz.x);
//    t = (float)Math.acos(renderSpec.vdirz.z / 1f);
//    g.drawString(String.format("ANGZ  s:%-3.1f  t:%-3.1f"
//        , Math.toDegrees(s)
//        , Math.toDegrees(t)
//        ), 100, 60);
    g.setColor(Color.green);
    g.drawLine(34, 34+24,
        (int)(34 + -renderSpec.vdirx.x * 32),
        (int)(34 + 24 + -renderSpec.vdirx.y * 32));
    g.setColor(Color.red);
    g.drawLine(34, 34+24,
        (int)(34 + -renderSpec.vdiry.x * 32),
        (int)(34 + 24 + -renderSpec.vdiry.y * 32));
    g.setColor(Color.yellow);
    g.drawLine(34, 34+24,
        (int)(34 + -renderSpec.vdirz.x * 32),
        (int)(34 + 24 + -renderSpec.vdirz.y * 32));
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
  public void setHeightMap(float[][] map) {
    renderSpec.modeltype = RenderSpec.MODEL_HEIGHTMAP;
    renderSpec.model = map;
    renderSpec.modelDirty = true;
    renderSpec.modelDataDirty = true;
    blit();
  }
  public void setHeightMapColor(float[][][] map) {
    renderSpec.modeltype = RenderSpec.MODEL_HEIGHTMAP_COLOR;
    renderSpec.model = map;
    renderSpec.modelDirty = true;
    renderSpec.modelDataDirty = true;
    blit();
  }
  public void setPointCloud(float[][][] map, float isolevel, boolean faceted) {
    renderSpec.modeltype = RenderSpec.MODEL_POINTCLOUD;
    renderSpec.model = map;
    renderSpec.modelDirty = true;
    renderSpec.modelDataDirty = true;
    renderSpec.isolevel = isolevel;
    renderSpec.faceted = faceted;
    blit();
  }
  public void setPointCloudColor(float[][][][] map, float isolevel, boolean faceted) {
    renderSpec.modeltype = RenderSpec.MODEL_POINTCLOUD_COLOR;
    renderSpec.model = map;
    renderSpec.modelDirty = true;
    renderSpec.modelDataDirty = true;
    renderSpec.isolevel = isolevel;
    renderSpec.faceted = faceted;
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

  List<RenderSpec> specs = new ArrayList<RenderSpec>();

  public void addRenderSpec(RenderSpec spec) {
    if (!specs.contains(spec)) {
      specs.add(spec);
      spec.getUIInfo().addListener(this);
      getUIInfo().addChild(spec);
      if (spec != renderSpec) renderSpec.children.add(spec);
    }
    blit();
  }

  public void removeRenderSpec(RenderSpec spec) {
    if (specs.contains(spec) && spec.getUIInfo().getParentUI() == this) {
      getUIInfo().removeChild(spec);
      renderSpec.children.remove(spec);
    }

  }

  public RenderSpec getRenderSpec() {
    return renderSpec;
  }

  @Override
  public void onRemoved(UIO parent, UIO child) {
    child.getUIInfo().removeListener(UI3DPanel.this);
    if (child instanceof RenderSpec) {
      removeRenderSpec((RenderSpec) child);
      if (child == renderSpec) {
        SwingUtilities.invokeLater(new Runnable() { public void run() { getUIInfo().close(); } } );
      }
    }
  }
  @Override
  public void onAdded(UIO parent, UIO child) {
  }
  @Override
  public void onClosed(UIO parent, UIO child) {
  }
  @Override
  public void onCreated(UIInfo obj) {
  }
  @Override
  public void onEvent(UIO obj, Object event) {
  }

}
