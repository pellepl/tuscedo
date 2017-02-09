package com.pelleplutt.tuscedo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import purejavacomm.CommPortIdentifier;

import com.pelleplutt.tuscedo.ui.DrawPanel;
import com.pelleplutt.tuscedo.ui.GraphPanel;
import com.pelleplutt.tuscedo.ui.SimpleTabPane;
import com.pelleplutt.tuscedo.ui.SimpleTabPane.Tab;
import com.pelleplutt.tuscedo.ui.WorkArea;
import com.pelleplutt.util.AppSystem;

public class Tuscedo implements Runnable {
  Container mainContainer;
  static Tuscedo inst;
  static List<Window> windows = new ArrayList<Window>();
  static volatile int __tabId;
  static volatile int __ownableId;
  volatile boolean running = true;
  List <Tickable> tickables = new ArrayList<Tickable>();
  Map <String, Tab> tabs = new HashMap<String, Tab>();
  Map <String, Ownable> ownables = new HashMap<String, Ownable>();
  
  private Tuscedo() {
    Thread t = new Thread(this, "commonticker");
    t.setDaemon(true);
    t.start();
  }
  
  public static Tuscedo inst() {
    if (inst == null) {
      inst = new Tuscedo();
    }
    return inst;
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
              AppSystem.disposeAll();
            }
          }
        }
      });
    }
  }

  public void create() {
    JFrame f = new JFrame();
    f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    f.getContentPane().setLayout(new BorderLayout());
    f.getContentPane().setBackground(WorkArea.colGenericBg);
    f.setSize(600, 400);
    //f.setLocationByPlatform(true); // cannot use this - windows x&y will report 0 until moved
    f.setLocation(100, 100);
    registerWindow(f);
    
    mainContainer = f.getContentPane();
    TuscedoTabPane tabs = new TuscedoTabPane();
    tabs.setFont(WorkArea.COMMON_FONT);

    addWorkAreaTab(tabs);
    mainContainer.add(tabs);
    tabs.addWindowListener(tabs, f);
    
    f.setVisible(true);
  }
  
  public String addWorkAreaTab(SimpleTabPane stp) {
    WorkArea w = new WorkArea();
    w.build();
    String tabID = Tuscedo.getTabID();
    String oID = Tuscedo.getOwnableID();
    Tab t = stp.createTab(tabID, w);
    w.setOwner(t);
    stp.selectTab(t);
    w.updateTitle();
    w.setStandardFocus();
    tabs.put(tabID, t);
    ownables.put(oID, w);
    return tabID;
  }
  
  public String addGraphTab(SimpleTabPane stp, List<Float> vals) {
    GraphPanel gp = new GraphPanel() {
      @Override
      public GraphPanel getGraphPanelFromOverlayObject(Object o) {
        Tab t = (Tab)o;
        return (GraphPanel)t.content;
      }
    };
    String tabID = Tuscedo.getTabID();
    String oID = Tuscedo.getOwnableID();
    Tab t = stp.createTab(tabID, gp);
    gp.setOwner(t);
    stp.selectTab(t);
    if (vals != null) {
      for (float s : vals) gp.addSample(s);
    }
    gp.zoomAll(true, true, new Point());
    tabs.put(tabID, t);
    ownables.put(oID, gp);
    return tabID;
  }
  
  public String addCanvasTab(SimpleTabPane stp, int w, int h) {
    DrawPanel gp = new DrawPanel(w, h);
    String tabID = Tuscedo.getTabID();
    String oID = Tuscedo.getOwnableID();
    Tab t = stp.createTab(tabID, gp);
    gp.setOwner(t);
    stp.selectTab(t);
    tabs.put(tabID, t);
    ownables.put(oID, gp);
    return tabID;
  }
  
  public Tab getTab(String id) {
    return tabs.get(id);
  }
  
  public class TuscedoTabPane extends SimpleTabPane implements SimpleTabPane.TabListener {
    public TuscedoTabPane() {
      this.addTabListener(this);
    }
    @Override
    public SimpleTabPane onEvacuationCreateTabPane() {
      TuscedoTabPane ttp = new TuscedoTabPane();
      return ttp;
    }
    
    @Override
    public void onEvacuationNewWindow(Window w) {
      Tuscedo.inst().registerWindow(w);
    }
    
    @Override
    public void onEvacuationNewTab(Tab oldTab, Tab newTab) {
      tabs.put(oldTab.id, newTab);
    }

    @Override
    public void tabRemoved(SimpleTabPane tp, Tab t, Component content) {
      if (content instanceof WorkArea) {
        AppSystem.dispose((WorkArea)content);
      }
      tabs.remove(t.getID());
    }
    @Override
    public void tabPaneEmpty(SimpleTabPane pane) {
      pane.removeTabListener(this);
      Window w = SwingUtilities.getWindowAncestor(pane);
      if (w != null && w.isVisible()) {
        w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
      }
    }
    @Override
    public void tabSelected(SimpleTabPane pane, Tab t) {
      Component c = t.getContent();
      if (c instanceof WorkArea) {
        ((WorkArea)c).onTabSelected(t);
      }
    }
  }
  
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Throwable ignore) {
    }
    
    @SuppressWarnings("rawtypes")
    Enumeration e = purejavacomm.CommPortIdentifier.getPortIdentifiers();
    while (e.hasMoreElements()) {
      CommPortIdentifier portId = (CommPortIdentifier) e.nextElement();
      if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
        System.out.println(portId.getName());
      }
    }
    
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
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        Tuscedo.inst().create();
      }
    });
  }

  public static String getTabID() {
    __tabId++;
    return "TAB" + __tabId;
  }

  public static String getOwnableID() {
    __ownableId++;
    return "o" + __ownableId;
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
}
