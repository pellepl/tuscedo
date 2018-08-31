package com.pelleplutt.tuscedo.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.pelleplutt.Essential;
import com.pelleplutt.tuscedo.Settings;
import com.pelleplutt.tuscedo.ui.UIInfo.UIListener;
import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.UIUtil;

public class UISimpleTabPane extends JPanel implements UIO {
  JPanel tabPanel;
  JPanel contentPanel;
  Tab selectedTab;
  GridBagLayout grid;
  GridBagConstraints gridc = new GridBagConstraints();
  Tab dragSource;
  Tab dropMarkTab;
  Window dragGhost;
  Window cursorOwner;

  static int __paneid = 0;
  List<Tab> tabs = new ArrayList<Tab>();
  
  List<TabListener> listeners = new ArrayList<TabListener>();
  
  final static List<UISimpleTabPane> panes = new ArrayList<UISimpleTabPane>();
  final UIInfo uiinfo;
  @Override
  public UIInfo getUIInfo() { return uiinfo; }
  public void onClose() {}

  public UISimpleTabPane() {
    uiinfo = new UIInfo(this, "tabpane" + __paneid, "");
    UIInfo.fireEventOnCreated(uiinfo);

    __paneid++;
    setLayout(new BorderLayout());
    grid = new GridBagLayout();
    tabPanel = new JPanel(grid);
    gridc.fill = GridBagConstraints.BOTH;
    gridc.gridheight = 1;
    gridc.gridy = 0;
    gridc.gridwidth = 1;

    contentPanel = new JPanel(new CardLayout(0, 0));
    tabPanel.setBackground(Color.black);
    add(tabPanel, BorderLayout.NORTH);
    add(contentPanel, BorderLayout.CENTER);
    
    if (!panes.contains(UISimpleTabPane.this)) {
      //System.out.println("register tabpane " + id);
      panes.add(UISimpleTabPane.this);
    }
  }
  
  public void decorateUI() {
    for (Tab t : tabs) {
      decorateTabLabel(t);
    }
  }

  
  public void addTabListener(TabListener l) {
    synchronized (listeners) {
      if (!listeners.contains(l)) listeners.add(l);
    }
  }
  
  public void removeTabListener(TabListener l) {
    synchronized (listeners) {
      listeners.remove(l);
    }
  }
  
  void fireTabRemoved(UISimpleTabPane tp, Tab t, Component content) {
    List<TabListener> localListeners;
    synchronized (listeners) {
      localListeners = new ArrayList<TabListener>(this.listeners);
    }
    for (TabListener l : localListeners) {
      l.tabRemoved(tp, t, content);
    }
  }
  
  void fireTabSelected(UISimpleTabPane tp, Tab t) {
    List<TabListener> localListeners;
    synchronized (listeners) {
      localListeners = new ArrayList<TabListener>(this.listeners);
    }
    for (TabListener l : localListeners) {
      l.tabSelected(tp, t);
    }
  }
  
  void fireTabPaneEmpty(UISimpleTabPane tp) {
    List<TabListener> localListeners;
    synchronized (listeners) {
      localListeners = new ArrayList<TabListener>(this.listeners);
    }
    for (TabListener l : localListeners) {
      l.tabPaneEmpty(tp);
    }
  }
  
  public static Tab getTabByComponent(Component c) {
    synchronized (panes) {
      for (UISimpleTabPane stp : panes) {
        for (Tab tab : stp.tabs) {
          //System.out.println("check " + tab.id + " in pane " + stp.id);
          if (tab.content == c) {
            return tab;
          }
        }
        
        while (c != null) {
          if (c instanceof UIO) {
            UIO uio = ((UIO)c).getUIInfo().getUI(); 
            if (uio instanceof Component) {
              for (Tab tab : stp.tabs) {
                if (tab.content == uio) {
                  return tab;
                }
              }
            }
          }
          c = c.getParent();
        }
      }
    }
    // no tab found, running gui-less?
    return null;
  }

  void decorateTabLabel(Tab tab) {
    tab.setPreferredSize(
        new Dimension(UICommon.font.getSize() + 4, UICommon.font.getSize() + 4));
    tab.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
    tab.setOpaque(true);
    tab.setForeground(UICommon.colTabFg);
    tab.setBackground(tab == selectedTab ? UICommon.colTabSelBg : UICommon.colTabNonSelBg);
    tab.setFont(UICommon.font);
  }

  public Tab createTab(String name, Component c) {
    Tab tab = new Tab(name);
    contentPanel.add(c, tab.getID());
    decorateTabLabel(tab);
    tabPanel.add(tab);

    tab.content = c;
    tabs.add(tab);
    getUIInfo().addChild(tab);
    tab.owner = this;
    if (c instanceof UIO) {
      tab.getUIInfo().addChild(((UIO)c).getUIInfo());
      (((UIO)c).getUIInfo()).addListener(tab);
    }

    tab.addMouseListener(tabMouseListener);
    tab.addMouseMotionListener(tabMouseListener);

    if (selectedTab == null) {
      selectTab(tab.getID());
    } else {
      computeLayout();
    }
    
    if (selectedTab != null) {
      selectedTab.closeButton.setVisible(tabs.size() > 1);
    }

    return tab;
  }
  
  Tab getTab(String id) {
    for (Tab t : tabs) {
      if (t.getID().equals(id)) {
        return t;
      }
    }
    return null;
  }

  public Tab getTab(int i) {
    return tabs.get(0);
  }

  int getTabIndex(String id) {
    int i = 0;
    for (Tab t : tabs) {
      if (t.getID().equals(id)) {
        return i;
      }
      i++;
    }
    return -1;
  }

  public void removeTab(String id) {
    Tab t = getTab(id);
    if (t == null)
      return;
    removeTab(t);
  }

  public void removeTab(Tab t) {
    removeTab(t, true, true);
  }
  
  void removeTab(Tab t, boolean fireTab, boolean firePane) {
    int ix = getTabIndex(t.uiinfo.id);
    if (ix > 0) {
      selectTab(ix - 1);
    } else if (ix < tabs.size()-2) {
      selectTab(ix + 1);
    } else {
      selectTab(ix == 0 ? 1 : 0);
    }
    tabPanel.remove(t);
    tabs.remove(t);
    t.getUIInfo().close();
    t.owner = null;

    if (selectedTab != null) {
      selectedTab.closeButton.setVisible(tabs.size() > 1);
    }

    computeLayout();
    
    if (fireTab) fireTabRemoved(this, t, t.getContent());
    if (firePane && tabs.isEmpty()) {
      fireTabPaneEmpty(this);
    }
  }

  public void removeAllTabs() {
    while (!tabs.isEmpty()) {
      removeTab(tabs.get(0), true, false);
    }
  }

  public void moveTabBefore(String id, int index) {
    Tab t = getTab(id);
    if (t == null)
      return;
    int curIx = tabs.indexOf(t);
    if (curIx <= index) index--;
    tabs.remove(t);
    if (tabs.isEmpty()) {
      tabs.add(t);
    } else {
      tabs.add(Math.min(tabs.size()-1, Math.max(0, index)), t);
    }
    computeLayout();
  }

  public void moveTabAfter(String id, int index) {
    Tab t = getTab(id);
    if (t == null)
      return;
    int curIx = tabs.indexOf(t);
    if (curIx <= index) index--;
    tabs.remove(t);
    if (index >= tabs.size() - 1) {
      tabs.add(t);
    } else {
      tabs.add(Math.min(tabs.size()-1, Math.max(0, index+1)), t);
    }
    computeLayout();
  }
  
  public void selectTab(int ix) {
    if (ix >= 0 && ix < tabs.size()) {
      selectTab(tabs.get(ix));
    }
  }

  public void selectNextTab() {
    int ix = tabs.indexOf(selectedTab);
    ix++;
    if (ix >= tabs.size()) {
      ix = 0;
    }
    selectTab(ix);
  }

  public void selectPrevTab() {
    int ix = tabs.indexOf(selectedTab);
    ix--;
    if (ix < 0) {
      ix =  tabs.size()-1;
    }
    selectTab(ix);
  }

  public void selectTab(String id) {
    Tab t = getTab(id);
    if (t == null)
      return;
    selectTab(t);
  }

  public void selectTab(Tab t) {
    ((CardLayout) contentPanel.getLayout()).show(contentPanel, t.getID());
    if (selectedTab != null) {
      selectedTab.setBackground(UICommon.colTabNonSelBg);
      selectedTab.closeButton.setVisible(false);
    }
    selectedTab = t;
    t.closeButton.setVisible(tabs.size() > 1);
    t.setBackground(UICommon.colTabSelBg);
    computeLayout();
    t.markNotified(0);
    fireTabSelected(this, t);
  }

  public void setTabTitle(String id, String title) {
    Tab t = getTab(id);
    if (t == null)
      return;
    setTabTitle(t, title);
  }

  public void setTabTitle(Tab t, String title) {
    t.setText(title);
  }

  public void markTabNotified(String id, int level) {
    Tab t = getTab(id);
    if (t == null)
      return;
    markTabNotified(t, level);
  }

  public void markTabNotified(Tab t, int level) {
    t.markNotified(level);
  }

  final Insets __i = new Insets(0, 0, 0, 1);

  void computeLayout() {
    int gridx = 1;
    gridc.anchor = GridBagConstraints.CENTER;
    gridc.insets = __i;
    gridc.gridwidth = 1;
    gridc.gridheight = 1;
    gridc.fill = GridBagConstraints.BOTH;
    gridc.weightx = 1.0;
    //System.out.println("layout");
    for (Tab t : tabs) {
      boolean sel = t == selectedTab;
      gridc.gridx = gridx;
      if (sel) {
        String text = t.getText();
        gridc.ipadx = text == null ? 8 : getFontMetrics(getFont()).stringWidth(text);
      } else {
        gridc.ipadx = 0;
      }
      grid.setConstraints(t, gridc);
      //System.out.println("    " + t.id + " at ix " + gridx);
      gridx += 1;
    }
    tabPanel.revalidate();
  }
  
  public UISimpleTabPane onEvacuationCreateTabPane() {
    return new UISimpleTabPane();
  }
  
  public void onEvacuationNewWindow(Window w) {
  }

  public void onEvacuationNewTab(Tab oldTab, Tab newTab) {
  }

  public void evacuateTab(Tab tab, Point location) {
    if (tab.owner.tabs.size() < 2) {
      Window w = SwingUtilities.windowForComponent(tab);
      w.setLocation(location);
      return;
    }
    
    List<UIInfo> children = new ArrayList<UIInfo>(tab.getUIInfo().children);
    for (UIInfo cuio : children) {
      tab.getUIInfo().removeChild(cuio);
      cuio.removeListener(tab);
    }

    UISimpleTabPane.this.removeTab(tab, false, true);

    UISimpleTabPane newPane = onEvacuationCreateTabPane();
    newPane.setFont(getFont());
    
    Tab newTab = newPane.createTab(null, tab.content);
    for (UIInfo cuio : children) {
      newTab.getUIInfo().addChild(cuio);
      cuio.addListener(newTab);
    }
    newTab.setText(tab.getUIInfo().getName());
    onEvacuationNewTab(tab, newTab);
    createNewWindow(newPane, SwingUtilities.getWindowAncestor(this).getSize(), 
        location);
  }
  
  protected void createNewWindow(final UISimpleTabPane newPane, Dimension size, Point location) {
    JFrame w = new JFrame();//SwingUtilities.getWindowAncestor(this));
    onEvacuationNewWindow(w);
    w.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    w.setSize(size == null ? new Dimension(400,300) : size);
    w.getContentPane().setLayout(new BorderLayout());
    w.getContentPane().add(newPane);
    if (location == null) {
      w.setLocationByPlatform(true);
    } else {
      w.setLocation(location);
    }
    addWindowListener(newPane, w);
    try {
      w.setIconImage(AppSystem.loadImage("tuscedo.png"));
      w.setTitle(Essential.name + " " + Essential.vMaj + "." + Essential.vMin + "." + Essential.vMic);
    } catch (Throwable t) {}

    w.setVisible(true);
  }
  
  public void addWindowListener(final UISimpleTabPane stp, Window w) {
    w.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        synchronized (panes) {
          stp.removeAllTabs();
          panes.remove(stp);
          //System.out.println("deregister tabpane " + stp.id);
        }
      }
    });
  }
  
  protected boolean moveBeforeElseAfter(MouseEvent me, Tab t) {
    Point p = SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), t);
    return (p.x < t.getWidth() / 2); 
  }
  
  MouseAdapter tabMouseListener = new MouseAdapter() {
    @Override
    public void mousePressed(MouseEvent me) {
      if (SwingUtilities.isLeftMouseButton(me)) {
        Tab t = (Tab) me.getSource();
        t.owner.selectTab(t);
      }
    }

    @Override
    public void mouseDragged(MouseEvent me) {
      if (SwingUtilities.isLeftMouseButton(me)) {
        if (dragSource == null) {
          if (me.getSource() instanceof Tab) {
            Tab t = (Tab)me.getSource();
            cursorOwner = SwingUtilities.getWindowAncestor(t); 
            cursorOwner.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            dragSource = t;
            if (Settings.inst().integer("tab_drag_ghost.int") > 0) {
              dragGhost = UIUtil.createGhost(t);
            }
          }
        } else {
          if (Settings.inst().integer("tab_drag_ghost.int") > 0) {
            Point p = me.getLocationOnScreen();
            dragGhost.setLocation(p.x + 1, p.y + 1);
          }
          if (dropMarkTab != null) {
            dropMarkTab.unmarkDrop();
          }
          
          synchronized (panes) {
            for (UISimpleTabPane pane : panes) {
              Window w = SwingUtilities.getWindowAncestor(pane);
              if (w == null) continue;
              Point q = SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), w);
              Component c = SwingUtilities.getDeepestComponentAt(w, q.x, q.y);
              if (c == null) continue;
              //System.out.println(pane.id + ": " + w.getX() + "," + w.getY() + "  " + c.getClass().getSimpleName());
              if (c instanceof Tab) {
                Tab t = (Tab)c;
                if (t != dragSource) {
                  dropMarkTab = t;
                  t.markDrop(moveBeforeElseAfter(me, t));
                }
                break;
              }
            }
          }
        }
      }
    }
    
    @Override
    public void mouseReleased(MouseEvent me) {
      if (cursorOwner != null) {
        cursorOwner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        cursorOwner = null;
      }
      if (dragGhost != null) {
        dragGhost.setVisible(false);
        dragGhost.dispose();
        dragGhost = null;
      }
      if (dropMarkTab != null) {
        dropMarkTab.unmarkDrop();
        dropMarkTab = null;
      }
      if (SwingUtilities.isLeftMouseButton(me)) {
        if (dragSource != null) {
          synchronized (panes) {
            boolean relocated = false;
            //System.out.println("relocating tab " + dragSource.id);
            for (UISimpleTabPane pane : panes) {
              //System.out.println("try pane " + pane.id);
              Window w = SwingUtilities.getWindowAncestor(pane);
              if (w == null) continue;
              Point p = 
                  SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), w);
              Component c = SwingUtilities.getDeepestComponentAt(w, p.x, p.y);
              if (c == null) continue;
              if (c instanceof Tab) {
                Tab t = (Tab)c;
                if (t.owner == dragSource.owner) {
                  // move within same window
                  if (moveBeforeElseAfter(me, t)) {
                    //System.out.println("move "+dragSource.id+" bef " + t.id);
                    moveTabBefore(dragSource.uiinfo.id, getTabIndex(t.uiinfo.id));
                  } else {
                    //System.out.println("move "+dragSource.id+" aft " + t.id);
                    moveTabAfter(dragSource.uiinfo.id, getTabIndex(t.uiinfo.id));
                  }
                } else {
                  // move to other window
                  //System.out.println("move from pane " + dragSource.owner.id + " to " + t.owner.id);
                  List<UIInfo> children = new ArrayList<UIInfo>(dragSource.getUIInfo().children);

                  UISimpleTabPane srcowner = dragSource.owner;
                  
                  for (UIInfo cuio : children) {
                    dragSource.getUIInfo().removeChild(cuio);
                    cuio.removeListener(dragSource);
                  }
                  
                  srcowner.removeTab(dragSource, false, true);
                  
                  Tab evaTab = t.owner.createTab(null, dragSource.content);
                  for (UIInfo cuio : children) {
                    evaTab.getUIInfo().addChild(cuio);
                    cuio.addListener(evaTab);
                  }
                  evaTab.setText(dragSource.getUIInfo().getName());
                  if (moveBeforeElseAfter(me, t)) {
                    t.owner.moveTabBefore(evaTab.uiinfo.id, t.owner.getTabIndex(t.uiinfo.id));
                  } else {
                    t.owner.moveTabAfter(evaTab.uiinfo.id, t.owner.getTabIndex(t.uiinfo.id));
                  }
                  t.owner.selectTab(evaTab);
                }
                relocated = true;
                break;
              }
            }
            if (!relocated) {
              //System.out.println("no hit, evacuating");
              evacuateTab(dragSource, me.getLocationOnScreen());
            }
          }
          dragSource = null;
        }
      }
    }
  };
  
  public static class Tab extends JPanel implements UIO, UIListener {
    public Component content;
    public UISimpleTabPane owner;
    JButton closeButton;
    private int markDrop;
    final static Color colMarkDrop = new Color(255,255,255,64);
    static int __tabid = 0;
    final UIInfo uiinfo;
    volatile int isNotified = 0;
    @Override
    public UIInfo getUIInfo() { return uiinfo; }
    public void onClose() {}

    public void decorateUI() {
      if (owner != null) owner.decorateTabLabel(this);
      repaint();
    }

    public Tab(final String n) {
      uiinfo = new UIInfo(this, "tab" + __paneid + "_" + __tabid, n);
      __tabid++;
      UIInfo.fireEventOnCreated(uiinfo);

      setLayout(new BorderLayout());
      JButton b = new JButton(new AbstractAction("x") {
        @Override
        public void actionPerformed(ActionEvent e) {
          Tab.this.owner.removeTab(uiinfo.id);
        }
      });
      b.setContentAreaFilled(false);
      b.setFocusPainted(false);
      b.setFocusable(false);
      b.setBorder(null);
      b.setBorderPainted(false);
      b.setVisible(false);
      closeButton = b;
      Font f = getFont();
      int x = f.getSize(); // SwingUtilities.computeStringWidth(null, "x");
      b.setBounds(new Rectangle(x,x));
      add(b, BorderLayout.EAST);
    }
    
    public void markDrop(boolean moveBeforeElseAfter) {
      markDrop = moveBeforeElseAfter ? 1 : 2;
      repaint();
    }

    public void unmarkDrop() {
      markDrop = 0;
      repaint();
    }

    public String getText() {
      return uiinfo.getFirstName();
    }
    
    public void setText(String t) {
      uiinfo.name = t;
      if (owner != null) {
        owner.computeLayout();
      }
      repaint();
    }
    
    public String getID() {
      return uiinfo.id;
    }
    
    public Component getContent() {
      return content;
    }
    
    public UISimpleTabPane getPane() {
      return owner;
    }
    
    public void markNotified(int level) {
      if (level > 1) {
        if (isNotified == 0)
          return; // no need setting old notification if notification is already cleared
        if (isNotified == 1 && owner != null && owner.selectedTab == this)
          level = 0; // no need setting unread notification on selected tab
      }
      isNotified = level;
      repaint();
    }
    
    
    @Override
    public void paint(Graphics og) {
      og.setColor(getBackground());
      og.fillRect(0, 0, getWidth(), getHeight());
      Graphics2D g = (Graphics2D) og;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(getForeground());
      g.setFont(getFont());
      String t = getText();
      int w;
      if (t == null) {
        w = 8;
      } else {
        w = g.getFontMetrics().stringWidth(t);
        g.drawString(t, Math.max(0, (getWidth() - w) / 2),
            getFont().getSize());
      }
      paintComponents(g);
      if (isNotified > 0) {
        g.setColor(isNotified == 1 ? UICommon.colTabNotifyNewFg : UICommon.colTabNotifyOldFg);
        g.fillOval(2, 2, 6, 6);
      }
      g.setColor(colMarkDrop);
      if (markDrop == 1) {
        g.fillRect(0, 0, getWidth()/2, getHeight());
      } else if (markDrop == 2) {
        g.fillRect(getWidth()/2, 0, getWidth()/2, getHeight());
      }
    }

    @Override
    public String toString() {
      return getText();
    }

    @Override
    public void onRemoved(UIO parent, UIO child) {
      this.getPane().removeTab(parent.getUIInfo().getId());
      child.getUIInfo().removeListener(Tab.this);
    }

    @Override
    public void onAdded(UIO parent, UIO child) {
    }

    @Override
    public void onClosed(UIO parent, UIO child) {
      this.getPane().removeTab(parent.getUIInfo().getId());
      child.getUIInfo().removeListener(Tab.this);
    }

    @Override
    public void onCreated(UIInfo obj) {
    }

    @Override
    public void onEvent(UIO obj, Object event) {
    }

    public int isNotified() {
      return isNotified;
    }
  } // class Tab
  
  public interface TabListener {
    public void tabRemoved(UISimpleTabPane tp, Tab t, Component content);
    public void tabPaneEmpty(UISimpleTabPane pane);
    public void tabSelected(UISimpleTabPane pane, Tab t);
  }
}
