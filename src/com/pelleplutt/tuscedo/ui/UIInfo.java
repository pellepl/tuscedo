package com.pelleplutt.tuscedo.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

public class UIInfo {
  public static final String EVENT_MOUSE_PRESS = "evmousepress";
  public static final String EVENT_MOUSE_RELEASE = "evmouserelease";
  public static final String EVENT_KEY_PRESS = "evkeypress";
  public static final String EVENT_KEY_RELEASE = "evkeyrelease";

  private UIO ui;
  final public String id;
  public String name;
  public UIInfo parent;
  public List<UIInfo> children;
  boolean closed;
  public int mousepressx, mousepressy, mousepressb;
  public int mouserelx, mouserely, mouserelb;
  public int keypress, keyrel;

  public UIInfo(UIO ui, String id, String name) {
    this.ui = ui;
    this.id = id;
    this.name = name;
    this.children = new ArrayList<UIInfo>();
    this.closed = false;
  }

  public UIO getUI() {
    return ui;
  }
  public String getName() {
    return name;
  }
  public String getFirstName() {
    List<UIInfo> q = new ArrayList<UIInfo>();
    q.add(this);
    while (!q.isEmpty()) {
      UIInfo i = q.remove(0);
      if (i.name != null) return i.name;
      q.addAll(i.children);
    }
    return null;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getId() {
    return id;
  }
  public UIInfo getParent() {
    return parent;
  }
  public UIO getParentUI() {
    return parent == null ? null : parent.ui;
  }
  public void close() {
    int ix = 0;
    while (!children.isEmpty() && ix < children.size()) {
      if (!children.get(ix).closed) {
        children.get(ix).close();
      } else {
        ix++;
      }
    }
    if (!closed) {
      closed = true;
      fireEventOnClosed(parent != null ? parent.ui : null, ui);
      if (parent != null) parent.removeChild(this);
      this.listeners.clear();
    }
  }
  public void addChild(UIInfo c) {
    if (c.parent != null && !c.parent.getId().equals(getId())) c.parent.removeChild(c);
    if (!children.contains(c)) {
      c.closed = false;
      c.parent = this;
      children.add(c);
      c.fireEventOnAdded(ui, c.ui);
    }
  }
  public void removeChild(UIInfo c) {
    if (children.contains(c)) {
      children.remove(c);
      c.fireEventOnRemoved(ui, c.ui);
      c.parent = null;
    }
  }
  public void addChild(UIO o) {
    addChild(o.getUIInfo());
  }
  public void removeChild(UIO o) {
    removeChild(o.getUIInfo());
  }
  public void event(Object ev) {
    fireEventGeneric(ui, ev);
  }
  public UIInfo getAncestor() {
    UIInfo op = null;
    UIInfo p = parent;
    while (p != null) {
      op = p;
      p = p.parent;
    }
    return op;
  }
  public UIO getAncestorUI() {
    UIInfo p = getAncestor();
    return p == null ? null : p.ui;
  }
  public void setUI(UIO o) {
    ui = o;
  }
  public String toString() {
    return getName();
  }

  List<UIListener> listeners = new ArrayList<UIListener>();

  public int irqMousePressAddr = 0;
  public int irqMouseReleaseAddr = 0;
  public int irqKeyPressAddr = 0;
  public int irqKeyReleaseAddr = 0;

  static List<UIListener> glisteners = new ArrayList<UIListener>();
  public void addListener(UIListener l) {
    if (!listeners.contains(l)) listeners.add(l);
  }
  public void removeListener(UIListener l) {
    if (listeners.contains(l)) listeners.remove(l);
  }
  public static void addGlobalListener(UIListener l) {
    if (!glisteners.contains(l)) glisteners.add(l);
  }
  public static void removeGlobalListener(UIListener l) {
    if (glisteners.contains(l)) glisteners.remove(l);
  }

  void fireEventOnRemoved(UIO parent, UIO child) {
    List<UIListener> lstnr = new ArrayList<UIListener>(listeners);
    for (UIListener l : lstnr) l.onRemoved(parent, child);
    for (UIListener l : glisteners) l.onRemoved(parent, child);
  }
  void fireEventOnAdded(UIO parent, UIO child) {
    List<UIListener> lstnr = new ArrayList<UIListener>(listeners);
    for (UIListener l : lstnr) l.onAdded(parent, child);
    for (UIListener l : glisteners) l.onAdded(parent, child);
  }
  void fireEventOnClosed(UIO parent, UIO child) {
    List<UIListener> lstnr = new ArrayList<UIListener>(listeners);
    for (UIListener l : lstnr) l.onClosed(parent, child);
    for (UIListener l : glisteners) l.onClosed(parent, child);
  }
  public static void fireEventOnCreated(UIInfo o) {
    for (UIListener l : glisteners) l.onCreated(o);
  }
  void fireEventGeneric(UIO o, Object event) {
    List<UIListener> lstnr = new ArrayList<UIListener>(listeners);
    for (UIListener l : lstnr) l.onEvent(o, event);
    for (UIListener l : glisteners) l.onEvent(o, event);
  }

  public interface UIListener {
    void onRemoved(UIO parent, UIO child);
    void onAdded(UIO parent, UIO child);
    void onClosed(UIO parent, UIO child);
    void onCreated(UIInfo obj);
    void onEvent(UIO obj, Object event);
  }

  public static class UIAdapter implements UIListener {
    @Override
    public void onRemoved(UIO parent, UIO child) {}
    @Override
    public void onAdded(UIO parent, UIO child) {}
    @Override
    public void onClosed(UIO parent, UIO child) {}
    @Override
    public void onCreated(UIInfo obj) {}
    @Override
    public void onEvent(UIO obj, Object event) {}
  }

  public String asString() {
    return "<" + getId() + ":[" + getName() + "]:" + getUI().getClass().getSimpleName() + ">";
  }

  public void registerInteractionCallbacks(UIO uio, JComponent c) {
    c.setFocusable(true);
    c.addMouseListener(mouseHandler);
    c.addKeyListener(new KeyListener() {
      @Override
      public void keyTyped(KeyEvent e) {
      }
      @Override
      public void keyReleased(KeyEvent e) {
        keyrel = e.getKeyCode();
        fireEventGeneric(ui, UIInfo.EVENT_KEY_RELEASE);
      }
      @Override
      public void keyPressed(KeyEvent e) {
        keypress = e.getKeyCode();
        fireEventGeneric(ui, UIInfo.EVENT_KEY_PRESS);
      }
    });
  }
  MouseAdapter mouseHandler = new MouseAdapter() {
    @Override
    public void mousePressed(MouseEvent e) {
      mousepressx = e.getX();
      mousepressy = e.getY();
      mousepressb = e.getButton();
      fireEventGeneric(ui, UIInfo.EVENT_MOUSE_PRESS);
    }
    @Override
    public void mouseReleased(MouseEvent e) {
      mouserelx = e.getX();
      mouserely = e.getY();
      mouserelb = e.getButton();
      fireEventGeneric(ui, UIInfo.EVENT_MOUSE_RELEASE);
    }
  };
}
