package com.pelleplutt.tuscedo;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.Map.*;

import javax.swing.*;

import com.pelleplutt.*;
import com.pelleplutt.tuscedo.ui.*;
import com.pelleplutt.tuscedo.ui.UISimpleTabPane.*;
import com.pelleplutt.util.*;
import com.pelleplutt.util.AppSystem.*;
import com.pelleplutt.util.io.*;

public class Tuscedo implements Runnable, UIInfo.UIListener {
  public static boolean noterm = false;
  public volatile static boolean settingsDirty = false;
  Container mainContainer;
  static Tuscedo inst;
  static List<Window> windows = new ArrayList<Window>();
  static Map<String, Window> windowForSplitPane = new HashMap<String, Window>();
  static volatile int __tabId;
  static volatile int __elementId;
  volatile boolean running = true;
  List <Tickable> tickables = new ArrayList<Tickable>();
  Map <String, UIInfo> uiobjects = new HashMap<String, UIInfo>();
  Timer timer;

  private Tuscedo() {
    Thread t = new Thread(this, "commonticker");
    t.setDaemon(true);
    t.start();
    UIInfo.addGlobalListener(this);
    timer = new Timer();
    AppSystem.addDisposable(timer);
    AppSystem.addDisposable(new Disposable() {
      @Override
      public void dispose() {
        PortConnector.getPortConnector().dispose();
      }
    });
    registerTickable(new Tickable() {
      @Override
      public void tick() {
        if (settingsDirty) {
          settingsDirty = false;
          Settings.inst().saveSettings();
        }
      }});
  }

  public static Tuscedo inst() {
    if (inst == null) {
      inst = new Tuscedo();
      try {
        checkInitScripts();
      } catch (Throwable t) {t.printStackTrace();}
    }
    return inst;
  }

  public Timer getTimer() {
    return timer;
  }

  public void registerTickable(Tickable t) {
    synchronized (tickables) {
      if (!tickables.contains(t)) tickables.add(t);
    }
  }

  public void deregisterTickable(Tickable t) {
    synchronized (tickables) {
      tickables.remove(t);
    }
  }

  public void registerWindow(Window w, UISplitPane sp) {
    synchronized (windows) {
      windows.add(w);
      windowForSplitPane.put(sp.getUIInfo().getId(), w);
      Log.println("window registered " + windows.size() + " on splitpane " + sp.getUIInfo().getId());
      w.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          synchronized (windows) {
            Window w = (Window)e.getSource();
            if (w == null) return;
            w.setVisible(false);
            windows.remove(w);
            Log.println("window deregistered " + windows.size());
            if (windows.isEmpty()) {
              Tuscedo.onExit();
            }
          }
        }
      });
    }
  }

  static int __winposx = Settings.inst().integer(Settings.WINDOW_POSX_INT);
  static int __winposy = Settings.inst().integer(Settings.WINDOW_POSY_INT);

  public void create(UIWorkArea uiw) {
    JFrame f = new JFrame();
    f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    f.getContentPane().setLayout(new BorderLayout());
    f.getContentPane().setBackground(UICommon.colGenericBg);
    f.setSize(Settings.inst().integer(Settings.WINDOW_DIMW_INT), Settings.inst().integer(Settings.WINDOW_DIMH_INT));
    f.setLocation(__winposx, __winposy);
    __winposx += 50;
    __winposy += 50;
    try {
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      if (__winposx + Settings.inst().integer(Settings.WINDOW_DIMW_INT) >= screenSize.width) {
        __winposx -= screenSize.width - Settings.inst().integer(Settings.WINDOW_DIMW_INT);
      }
      if (__winposy + Settings.inst().integer(Settings.WINDOW_DIMH_INT) >= screenSize.height) {
        __winposy -= screenSize.height - Settings.inst().integer(Settings.WINDOW_DIMH_INT);
      }
    } catch (Throwable t) {
      __winposx = 0;
      __winposy = 0;
    }


    mainContainer = f.getContentPane();

    UISplitPane jsp = new UISplitPane(JSplitPane.HORIZONTAL_SPLIT);
    UICommon.decorateSplitPane(jsp, true);
    TuscedoTabPane tabs = new TuscedoTabPane();
    tabs.setFont(UICommon.font);
    addWorkAreaTab(tabs, uiw);
    tabs.addWindowListener(tabs, f);
    jsp.setTopUI(tabs);
    jsp.setBottomUI(null);
    mainContainer.add(jsp);
    registerWindow(f, jsp);

    try {
      f.setIconImage(AppSystem.loadImage("tuscedo.png"));
    } catch (Throwable t) {}
    f.setTitle(Essential.name + " " + Essential.vMaj + "." + Essential.vMin + "." + Essential.vMic);

    f.setVisible(true);

    f.addComponentListener(new ComponentListener() {
      @Override
      public void componentShown(ComponentEvent e) {
      }
      @Override
      public void componentResized(ComponentEvent e) {
        Settings.inst().setInt(Settings.WINDOW_DIMW_INT, e.getComponent().getWidth());
        Settings.inst().setInt(Settings.WINDOW_DIMH_INT, e.getComponent().getHeight());
        settingsDirty = true;
      }
      @Override
      public void componentMoved(ComponentEvent e) {
        Settings.inst().setInt(Settings.WINDOW_POSX_INT, e.getComponent().getX());
        Settings.inst().setInt(Settings.WINDOW_POSY_INT, e.getComponent().getY());
        settingsDirty = true;
      }
      @Override
      public void componentHidden(ComponentEvent e) {
      }
    });;
  }

  public String addWorkAreaTab(UISimpleTabPane stp, UIWorkArea w) {
    if (w == null) w = new UIWorkArea();
    w.build();
    Tab t = stp.createTab("", w);
    stp.selectTab(t);
    w.updateTitle();
    w.setStandardFocus();
    return w.getUIInfo().getId();
  }

  public String addGraphTab(UISimpleTabPane stp) {
    return addGraphTab(stp);
  }
  public String addGraphTab(UISimpleTabPane stp, List<List<Float>> vals, String name) {
    UIGraphPanel gp = new UIGraphPanel("");
    Tab t = stp.createTab(null, gp);
    stp.selectTab(t);
    if (vals != null) {
      for (int i = 0; i < vals.size(); i++) {
        if (i == 0) {
          gp.addSamplesFloat(vals.get(i));
        } else {
          UIGraphPanel.SampleSet ss = gp.newSampleSet();
          gp.addSamplesFloat(vals.get(i), i);
          ss.getUIInfo().setName(name + ("" + (i+1)));
        }
      }
    }
    gp.zoomAll(true, true, new Point());
    return gp.getSampleSet(0).getUIInfo().getId();
  }
  public String addGraph3dTab(UISimpleTabPane stp, int w, int h, float[][] model) {
    UI3DPanel gp = new UI3DPanel(w, h, model);
    Tab t = stp.createTab(null, gp);
    stp.selectTab(t);
    return gp.getRenderSpec().getUIInfo().getId();
  } // TODO PETER args
  public String addCanvasTab(UISimpleTabPane stp, int w, int h) {
    UICanvasPanel cp = new UICanvasPanel(w, h);
    Tab t = stp.createTab(null, cp);
    stp.selectTab(t);
    return cp.getUIInfo().getId();
  }
  public String addFDTab(UISimpleTabPane stp, UIWorkArea wa) {
    UIFDPanel cp = new UIFDPanel(wa);
    Tab t = stp.createTab(null, cp);
    stp.selectTab(t);
    return cp.getUIInfo().getId();
  }
  
  public UIInfo getUIObject(String id) {
    UIInfo uii = uiobjects.get(id);
    if (uii == null) {
      Log.println("WARN: tried to get uiinfo for id '" + id + "' but none found");
      Log.println("existing ids:");
      for (Entry<String, UIInfo> e: uiobjects.entrySet()) {
        Log.println(e.getKey() + ":" + e.getValue());
      }
      new Error("null uiinfo").printStackTrace();
    }
    return uii;
  }


  public static Scene3D scene3d = new Scene3D();

  static void checkInitScripts() {
    File initPath = new File(System.getProperty("user.home") + File.separator +
        Essential.userSettingPath + File.separator);
    try {
      String initDeclaration = new String(AppSystem.getAppResource("init-op/init-declaration"));
      String[] initResources = initDeclaration.split("\\r?\\n");
      for (String initResource : initResources) {
        boolean copy = false;
        File initFile = new File(initPath, new File(initResource).getName());
        if (initFile.exists()) {
          URL url = Thread.currentThread().getContextClassLoader().getResource(initResource);
          long resourceDate = url.openConnection().getLastModified();
          long fileDate = initFile.lastModified();
          if (resourceDate > fileDate) {
            copy = true;
          }
        } else {
          copy = true;
        }
        if (copy) {
          AppSystem.copyAppResource(initResource, initFile);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void cli() {
    UIWorkArea wa = new UIWorkArea();
    wa.build();
    wa.getScript().currentWA = wa;
    BufferedReader buffer=new BufferedReader(new InputStreamReader(System.in));
    String input = "";
    while (true) {
      String line = null;
      try { line = buffer.readLine(); } catch (IOException e) { }
      if (line == null) break;
      if (line.trim().endsWith("\\")) {
        int sep = line.lastIndexOf('\\');
        input += line.substring(0,sep)+"\n";
        continue;
      } else {
        input += line + "\n";
      }
      wa.getScript().runScript(wa, input);
      input = "";
    }
  }

  static boolean no3d = false;

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Throwable ignore) {
    }

    if (System.getProperty("os.name").indexOf("indow") > 0) {
      UICommon.COMMON_FONT = "lucida console";
    } else {
      UICommon.COMMON_FONT = Font.MONOSPACED;
    }

    // purejavacomm

//    @SuppressWarnings("rawtypes")
//    Enumeration e = purejavacomm.CommPortIdentifier.getPortIdentifiers();
//    while (e.hasMoreElements()) {
//      CommPortIdentifier portId = (CommPortIdentifier) e.nextElement();
//      if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
//        System.out.println(portId.getName());
//      }
//    }

/*    SerialPort port;
    try {
      port = (SerialPort) CommPortIdentifier.getPortIdentifier("ttyUSB3").open("tusc", 400);
      port.setSerialPortParams(921600/2, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
      InputStream is = (InputStream) port.getInputStream();
      int d;
      while ((d = is.read()) != -1) {
        System.out.print((char)d);
      }
      System.out.println("closed");
      port.close();
    } catch (PortInUseException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (NoSuchPortException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (UnsupportedCommOperationException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
  */
    boolean doCli = false;
    boolean doGui = true;
    UIWorkArea tmpwa = null;
    File scrFile = null;
    String scrArgs[] = null;
    for (int i = 0; i < args.length; i++) {
      if (args[i].endsWith(".op")) {
        scrArgs = new String[args.length - i - 1];
        System.arraycopy(args, i+1, scrArgs, 0, scrArgs.length);
        scrFile = new File(args[i]);
        if (doCli) {
          tmpwa = new UIWorkArea();
          tmpwa.build();
          tmpwa.getScript().currentWA = tmpwa;
          tmpwa.getScript().runScript(tmpwa, scrFile, args[i], scrArgs);
        }
      } else if (args[i].equals("--nogui")) {
        doCli = true;
      } else if (args[i].equals("--noterm")) {
        noterm = true;
      } else if (args[i].equals("--no3d")) {
        no3d = true;
      }
    }
    if (doCli) {
      cli();
    } else if (doGui) {
      final File fscrFile = scrFile;
      final String[] fscrArgs = scrArgs;
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          UIWorkArea uiw = new UIWorkArea();
          Tuscedo.inst().create(uiw);
          if (fscrFile != null) {
            uiw.getScript().runScript(uiw, fscrFile, fscrFile.getName(), fscrArgs);
          }
        }
      });
      if (!no3d) {
        scene3d.init();
        scene3d.render();
        render3dloop();
      }
    }
  } // main

  static boolean rendering = false;
  static RenderSpec renderSpec;

  static void render3dloop() {
    // start render loop, must be in the main thread
    synchronized (scene3d) {
      while (inst.running) {
        try {
          scene3d.wait();
        } catch (InterruptedException ignore) {}
        if (inst.running) {
          scene3d.render(renderSpec);
          renderSpec = null;
        }
        rendering = false;
        scene3d.notifyAll();
      }
    }
  }

  // called from another thread, commence 3d rendering and wait until finished
  public BufferedImage render3d(RenderSpec rs) {
    if (no3d) {
      throw new RuntimeException("3D disabled");
    }
    synchronized (scene3d) {
      // if a rendering is already ongoing, wait till finished
      while (running && rendering) {
        try {
          scene3d.wait(100);
        } catch (InterruptedException ignore) {}
      }
      renderSpec = rs;
      rendering = true;
      scene3d.notifyAll();
      while (running && rendering) {
        try {
          scene3d.wait(100);
        } catch (InterruptedException ignore) {}
      }
      return scene3d.getImage();
    }
  }

  public static void onExit() {
    AppSystem.disposeAll();
    inst.running = false;
    if (scene3d != null) {
      scene3d.destroy();
      synchronized (scene3d) {
        scene3d.notifyAll();
      }
    }
    try {
      Thread.sleep(60); // why? well - why not?
    } catch (Throwable t) {}
    System.exit(0);
  }

  @Override
  public void run() {
    while (running) {
      int i = 0;
      for (i = 0; i < tickables.size(); i++) {
        try {
          tickables.get(i).tick();
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }
      AppSystem.sleep(200);
    }
  }

  @Override
  public void onRemoved(UIO parent, UIO child) {
    Log.println("onRemoved " + child.getUIInfo().asString() + " parent:" + parent.getUIInfo().asString());
    uiobjects.remove(child.getUIInfo().getId());
    Log.println(uiobjects.size() + " uios left");
    if (uiobjects.isEmpty()) {
      onExit();
    } else {
      if (parent != null && parent instanceof UISplitPane) {
        UISplitPane sp = ((UISplitPane)parent);
        if (sp.getTopUI() == null && sp.getBottomUI() == null) {
          Log.println("splitter " + sp.getUIInfo().getId() + " is empty, closing window");
          Window w = windowForSplitPane.get(sp.getUIInfo().getId());
          if (w != null) {
            w.setVisible(false);
            windows.remove(w);
            w.dispose();
          }
        }
      }
    }
  }

  @Override
  public void onAdded(UIO parent, UIO child) {
    //System.out.println("onAdded    " + stringify(child.getUIInfo(), 0) + "  to  " + stringify(parent.getUIInfo(), 0));
  }

  @Override
  public void onClosed(UIO parent, UIO child) {
    Log.println("onClosed " + child.getUIInfo().asString());
    uiobjects.remove(child.getUIInfo().getId());
    child.onClose();
  }

  @Override
  public void onCreated(UIInfo i) {
    uiobjects.put(i.getId(), i);
    Log.println("onCreated " + i.asString());
  }

  @Override
  public void onEvent(UIO obj, Object event) {
  }

  private String stringify(UIInfo i, int level) {
    String s ="";
    while(level-- > 0) s+="  ";
    return s + i.asString();
  }
  private void recurse(UIInfo i, StringBuilder sb, int level) {
    sb.append(stringify(i, level) + "\n");
    for (UIInfo c : i.children) recurse(c,sb,level+1);
  }
  public String dumpUITree() {
    StringBuilder sb = new StringBuilder();
    for (Entry<String, UIInfo> e : uiobjects.entrySet()) {
      if (e.getValue().getParent() == null) {
        recurse(e.getValue(), sb, 0);
      }
    }
    return sb.toString();
  }

  private void recurseFind(UIInfo i, List<UIO> res, Class<?> clz) {
    if (clz.isInstance(i.getUI())) res.add(i.getUI());
    for (UIInfo c : i.children) recurseFind(c, res, clz);
  }
  public void getUiComponents(List<UIO> res, Class<?> clz) {
    for (Entry<String, UIInfo> e : uiobjects.entrySet()) {
      recurseFind(e.getValue(), res, clz);
    }
  }

  private void recurseRepaint(UIInfo i) {
    for (UIInfo c : i.children) {
      c.getUI().repaint();
      recurseRepaint(c);
    }
  }
  public void repaintAll() {
    for (Entry<String, UIInfo> e : uiobjects.entrySet()) {
      if (e.getValue().getParent() == null) {
        recurseRepaint(e.getValue());
      }
    }
  }

  private void recurseRedecorate(UIInfo i) {
    for (UIInfo c : i.children) {
      c.getUI().decorateUI();
      recurseRedecorate(c);
    }
  }
  public void redecorateAll() {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          for (Entry<String, UIInfo> e : uiobjects.entrySet()) {
            if (e.getValue().getParent() == null) {
              recurseRedecorate(e.getValue());
            }
          }
        }
      });
  }
}
