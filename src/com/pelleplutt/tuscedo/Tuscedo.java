package com.pelleplutt.tuscedo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import com.pelleplutt.tuscedo.ui.GraphPanel;
import com.pelleplutt.tuscedo.ui.SimpleTabPane;
import com.pelleplutt.tuscedo.ui.WorkArea;
import com.pelleplutt.util.AppSystem;

import purejavacomm.CommPortIdentifier;

public class Tuscedo {
  Container mainContainer;
  static Tuscedo inst;
  static List<Window> windows = new ArrayList<Window>();
  static volatile int __tabId;
  
  private Tuscedo() {
  }
  
  public static Tuscedo inst() {
    if (inst == null) {
      inst = new Tuscedo();
    }
    return inst;
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
  
  public void addWorkAreaTab(SimpleTabPane stp) {
    WorkArea w = new WorkArea();
    w.build();
    stp.selectTab(stp.createTab(Tuscedo.getTabID(), w));
    w.updateTitle();
    w.setStandardFocus();
  }
  
  public void addGraphTab(SimpleTabPane stp, String input) {
    GraphPanel gp = new GraphPanel();
    stp.selectTab(stp.createTab(Tuscedo.getTabID(), gp));
    BufferedReader reader = new BufferedReader(new StringReader(input));
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        try {
          double d = Double.parseDouble(line);
          gp.addSample(d);
        } catch (NumberFormatException nfe) {} 
      }
      gp.zoomAll(true, true, new Point());
    } catch (IOException e) {
      e.printStackTrace();
    }
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
    public void tabRemoved(SimpleTabPane tp, Tab t, Component content) {
      if (content instanceof WorkArea) {
        //Log.println("dispose workarea ..");
        ((WorkArea)content).dispose();
        //Log.println("disposed workarea");
      }
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
}
