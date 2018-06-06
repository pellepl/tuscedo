package com.pelleplutt.tuscedo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import com.pelleplutt.Essential;
import com.pelleplutt.tuscedo.ui.Scene3D;
import com.pelleplutt.tuscedo.ui.UICanvasPanel;
import com.pelleplutt.tuscedo.ui.UIGraphPanel;
import com.pelleplutt.tuscedo.ui.UIInfo;
import com.pelleplutt.tuscedo.ui.UIO;
import com.pelleplutt.tuscedo.ui.UISimpleTabPane;
import com.pelleplutt.tuscedo.ui.UISimpleTabPane.Tab;
import com.pelleplutt.tuscedo.ui.UIWorkArea;
import com.pelleplutt.util.AppSystem;

public class Tuscedo implements Runnable, UIInfo.UIListener {
  Container mainContainer;
  static Tuscedo inst;
  static List<Window> windows = new ArrayList<Window>();
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
  }
  
  public static Tuscedo inst() {
    if (inst == null) {
      inst = new Tuscedo();
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
  
  public void registerWindow(Window w) {
    synchronized (windows) {
      windows.add(w);
      //Log.println("window registered " + windows.size());
      w.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          synchronized (windows) {
            Window w = (Window)e.getSource();
            if (w == null) return;
            w.setVisible(false);
            windows.remove(w);
            //Log.println("window deregistered " + windows.size());
            if (windows.isEmpty()) {
              Tuscedo.onExit();
              AppSystem.disposeAll();
            }
          }
        }
      });
    }
  }

  public void create(UIWorkArea uiw) {
    JFrame f = new JFrame();
    f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    f.getContentPane().setLayout(new BorderLayout());
    f.getContentPane().setBackground(UIWorkArea.colGenericBg);
    f.setSize(600, 400);
    //f.setLocationByPlatform(true); // cannot use this - windows x&y will report 0 until moved
    f.setLocation(100, 100);
    registerWindow(f);
    
    mainContainer = f.getContentPane();
    TuscedoTabPane tabs = new TuscedoTabPane();
    tabs.setFont(UIWorkArea.COMMON_FONT);

    addWorkAreaTab(tabs, uiw);

    mainContainer.add(tabs);
    tabs.addWindowListener(tabs, f);
    
    try {
      f.setIconImage(AppSystem.loadImage("tuscedo.png"));
      f.setTitle(Essential.name + " " + Essential.vMaj + "." + Essential.vMin + "." + Essential.vMic);
    } catch (Throwable t) {}
    
    f.setVisible(true);
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
    return addGraphTab(stp, null);
  }
  public String addGraphTab(UISimpleTabPane stp, List<Float> vals) {
    UIGraphPanel gp = new UIGraphPanel("");
    Tab t = stp.createTab(null, gp);
    stp.selectTab(t);
    if (vals != null) {
      for (float s : vals) gp.addSample(s);
    }
    gp.zoomAll(true, true, new Point());
    return gp.getSampleSet(0).getUIInfo().getId();
  }
  
  public String addCanvasTab(UISimpleTabPane stp, int w, int h) {
    UICanvasPanel cp = new UICanvasPanel(w, h);
    Tab t = stp.createTab(null, cp);
    stp.selectTab(t);
    return cp.getUIInfo().getId();
  }
  
  public UIInfo getUIObject(String id) {
    return uiobjects.get(id);
  }
  
  public class TuscedoTabPane extends UISimpleTabPane implements UISimpleTabPane.TabListener {
    public TuscedoTabPane() {
      super();
      this.addTabListener(this);
    }
    @Override
    public UISimpleTabPane onEvacuationCreateTabPane() {
      TuscedoTabPane ttp = new TuscedoTabPane();
      return ttp;
    }
    
    @Override
    public void onEvacuationNewWindow(Window w) {
      Tuscedo.inst().registerWindow(w);
    }
    
    @Override
    public void tabRemoved(UISimpleTabPane tp, Tab t, Component content) {
      if (content instanceof UIWorkArea) {
        AppSystem.dispose((UIWorkArea)content);
      }
    }
    @Override
    public void tabPaneEmpty(UISimpleTabPane pane) {
      pane.removeTabListener(this);
      Window w = SwingUtilities.getWindowAncestor(pane);
      if (w != null && w.isVisible()) {
        w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
      }
    }
    @Override
    public void tabSelected(UISimpleTabPane pane, Tab t) {
      Component c = t.getContent();
      if (c instanceof UIWorkArea) {
        ((UIWorkArea)c).onTabSelected(t);
      }
    }
  }
  
  public static Scene3D test3d = new Scene3D();
  
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Throwable ignore) {
    }
    
    // lwjgl3 scene to bufferedimage
    
//    ByteBuffer nativeBuffer = BufferUtils.createByteBuffer(w*h*3);
//    BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
//    GL11.glReadPixels(0, 0, w, h, GL12.GL_BGR, GL11.GL_UNSIGNED_BYTE, nativeBuffer);
//    byte[] imgData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
//    nativeBuffer.get(imgData);


    
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
    if (args.length > 0 && args[0].endsWith(".op")) {
      UIWorkArea wa = new UIWorkArea();
      wa.build();
      wa.getScript().runScript(wa, new File(args[0]), args[0]);
    } else {
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          Tuscedo.inst().create(null);
        }
      });
      test3d.init();
      test3d.render();
      render3dloop();
    }
  } // main
  
  static final Object renderLock = new Object();
  static boolean rendering = false;
  
  static void render3dloop() {
    // start render loop, must be in the main thread
    synchronized (test3d) {
      while (inst.running) {
        try {
          test3d.wait();
        } catch (InterruptedException ignore) {}
        synchronized(renderLock) {
          if (inst.running) {
            test3d.render();
          }
          rendering = false;
          renderLock.notifyAll();
        }
      }
    }
  }
  
  // called from another thread, commence 3d rendering and wait until finished
  public void render3d() {
    synchronized (test3d) {
      test3d.notifyAll();
    }
    synchronized(renderLock) {
      rendering = true;
      while (running && rendering) {
        try {
          renderLock.wait(100);
        } catch (InterruptedException ignore) {}
      }
    }
  }
  
  static void onExit() {
    inst.running = false;
    test3d.destroy();
    synchronized (test3d) {
      test3d.notifyAll();
    }
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
    //System.out.println("onRemoved  " + stringify(child.getUIInfo(), 0) + " from " + stringify(parent.getUIInfo(), 0));
  }

  @Override
  public void onAdded(UIO parent, UIO child) {
    //System.out.println("onAdded    " + stringify(child.getUIInfo(), 0) + "  to  " + stringify(parent.getUIInfo(), 0));
  }

  @Override
  public void onClosed(UIO parent, UIO child) {
//    System.out.println("onClosed   " + 
//      stringify(child.getUIInfo(), 0) + 
//      "  in  " + 
//      (parent == null ? "null":stringify(parent.getUIInfo(), 0)));
    uiobjects.remove(child.getUIInfo().getId());
  }

  @Override
  public void onCreated(UIInfo i) {
    uiobjects.put(i.getId(), i);
//    System.out.println("onCreated  " + stringify(i, 0));
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
}
