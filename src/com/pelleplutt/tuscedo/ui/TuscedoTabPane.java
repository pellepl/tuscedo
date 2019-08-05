package com.pelleplutt.tuscedo.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import com.pelleplutt.Essential;
import com.pelleplutt.tuscedo.Tuscedo;
import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.Log;

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
  public JFrame onEvacuationCreateFrame() {
    JFrame w = new JFrame();
    Tuscedo.inst().registerWindow((Window)w);
    try {
      w.setIconImage(AppSystem.loadImage("tuscedo.png"));
      w.setTitle(Essential.name + " " + Essential.vMaj + "." + Essential.vMin + "." + Essential.vMic);
    } catch (Throwable t) {}

    return w;
  }
  
  @Override
  protected void buildNewWindow(final JFrame f, final UISimpleTabPane tabs) {
    Container mainContainer = f.getContentPane();
    
    UISplitPane jsp = new UISplitPane(JSplitPane.HORIZONTAL_SPLIT);
    UICommon.decorateSplitPane(jsp, true);
    tabs.setFont(UICommon.font);
    tabs.addWindowListener(tabs, f);
    jsp.setTopUI(tabs);
    jsp.setBottomUI(null);
    mainContainer.add(jsp);
  }

  @Override
  public void tabRemoved(UISimpleTabPane tp, Tab t, Component content) {
    // on evacuation , will shutdown operandi script = bad
    // if (content instanceof UIWorkArea) {
    // AppSystem.dispose((UIWorkArea)content);
    // }
  }

  @Override
  public void tabPaneEmpty(UISimpleTabPane pane) {
    Log.println(pane.getUIInfo().asString() + " is empty");
    getUIInfo().fireEventOnRemoved(getUIInfo().getParentUI(), this);
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
      ((UIWorkArea) c).onTabSelected(t);
    }
  }
}
