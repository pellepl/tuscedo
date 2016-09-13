package com.pelleplutt.tuscedo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import com.pelleplutt.tuscedo.ui.SimpleTabPane;
import com.pelleplutt.tuscedo.ui.WorkArea;
import com.pelleplutt.util.AppSystem;

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

    WorkArea w1 = new WorkArea();
    w1.build();
    WorkArea w2 = new WorkArea();
    w2.build();

    tabs.createTab("MAIN", w1);
    w1.updateTitle();
    mainContainer.add(tabs);
    tabs.addWindowListener(tabs, f);
    
    f.setVisible(true);

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
