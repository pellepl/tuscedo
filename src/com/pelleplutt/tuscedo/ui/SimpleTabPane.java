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

public class SimpleTabPane extends JPanel {
  JPanel tabPanel;
  JPanel contentPanel;
  Tab selectedTab;
  GridBagLayout grid;
  GridBagConstraints gridc = new GridBagConstraints();
  Tab dragSource;
  Tab dropMarkTab;
  Window dragGhost;
  Window cursorOwner;

  static int __id = 0;
  final int id;
  List<Tab> tabs = new ArrayList<Tab>();
  
  List<TabListener> listeners = new ArrayList<TabListener>();
  
  final static List<SimpleTabPane> panes = new ArrayList<SimpleTabPane>();

  public SimpleTabPane() {
    id = __id++;
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
    
    if (!panes.contains(SimpleTabPane.this)) {
      //System.out.println("register tabpane " + id);
      panes.add(SimpleTabPane.this);
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
  
  void fireTabRemoved(SimpleTabPane tp, Tab t, Component content) {
    List<TabListener> localListeners;
    synchronized (listeners) {
      localListeners = new ArrayList<TabListener>(this.listeners);
    }
    for (TabListener l : localListeners) {
      l.tabRemoved(tp, t, content);
    }
  }
  
  void fireTabSelected(SimpleTabPane tp, Tab t) {
    List<TabListener> localListeners;
    synchronized (listeners) {
      localListeners = new ArrayList<TabListener>(this.listeners);
    }
    for (TabListener l : localListeners) {
      l.tabSelected(tp, t);
    }
  }
  
  void fireTabPaneEmpty(SimpleTabPane tp) {
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
      for (SimpleTabPane stp : panes) {
        for (Tab tab : stp.tabs) {
          //System.out.println("check " + tab.id + " in pane " + stp.id);
          if (tab.content == c) {
            return tab;
          }
        }
      }
    }
    return null;
  }

  void decorateTabLabel(Tab tab) {
    tab.setPreferredSize(
        new Dimension(getFont().getSize() + 4, getFont().getSize() + 4));
    tab.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
    tab.setOpaque(true);
    tab.setForeground(Color.lightGray);
    tab.setBackground(Color.darkGray);
    tab.setFont(getFont());
  }

  public Tab createTab(String idAndName, Component c) {
    contentPanel.add(c, idAndName);
    Tab tab = new Tab(idAndName);
    decorateTabLabel(tab);
    tabPanel.add(tab);

    tab.content = c;
    tabs.add(tab);
    tab.owner = this;

    tab.addMouseListener(tabMouseListener);
    tab.addMouseMotionListener(tabMouseListener);

    if (selectedTab == null) {
      selectTab(idAndName);
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
      if (t.id.equals(id)) {
        return t;
      }
    }
    return null;
  }

  int getTabIndex(String id) {
    int i = 0;
    for (Tab t : tabs) {
      if (t.id.equals(id)) {
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
    int ix = getTabIndex(t.id);
    if (ix > 0) {
      selectTab(ix - 1);
    } else if (ix < tabs.size()-2) {
      selectTab(ix + 1);
    } else {
      selectTab(ix == 0 ? 1 : 0);
    }
    tabPanel.remove(t);
    tabs.remove(t);
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

  public void selectTab(String id) {
    Tab t = getTab(id);
    if (t == null)
      return;
    selectTab(t);
  }

  public void selectTab(Tab t) {
    ((CardLayout) contentPanel.getLayout()).show(contentPanel, t.id);
    if (selectedTab != null) {
      selectedTab.setBackground(Color.darkGray);
      selectedTab.closeButton.setVisible(false);
    }
    selectedTab = t;
    t.closeButton.setVisible(tabs.size() > 1);
    t.setBackground(Color.gray);
    computeLayout();
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

  static final Insets __i = new Insets(0, 0, 0, 1);

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
        gridc.ipadx = getFontMetrics(getFont()).stringWidth(t.text);
      } else {
        gridc.ipadx = 0;
      }
      grid.setConstraints(t, gridc);
      //System.out.println("    " + t.id + " at ix " + gridx);
      gridx += 1;
    }
    tabPanel.revalidate();
  }
  
  public SimpleTabPane onEvacuationCreateTabPane() {
    return new SimpleTabPane();
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
    SimpleTabPane newPane = onEvacuationCreateTabPane();
    newPane.setFont(getFont());
    SimpleTabPane.this.removeTab(tab, false, true);
    Tab newTab = newPane.createTab(tab.id, tab.content);
    newTab.setText(tab.getText());
    onEvacuationNewTab(tab, newTab);
    createNewWindow(newPane, SwingUtilities.getWindowAncestor(this).getSize(), 
        location);
  }
  
  protected void createNewWindow(final SimpleTabPane newPane, Dimension size, Point location) {
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
    w.setVisible(true);
  }
  
  public void addWindowListener(final SimpleTabPane stp, Window w) {
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
            //dragGhost = UIUtil.createGhost(t);
          }
        } else {
          //Point p = me.getLocationOnScreen();
          //dragGhost.setLocation(p.x + 1, p.y + 1);
          if (dropMarkTab != null) {
            dropMarkTab.unmarkDrop();
          }
          
          synchronized (panes) {
            for (SimpleTabPane pane : panes) {
              Window w = SwingUtilities.getWindowAncestor(pane);
              if (w == null) continue;
              Point p = SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), w);
              Component c = SwingUtilities.getDeepestComponentAt(w, p.x, p.y);
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
            for (SimpleTabPane pane : panes) {
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
                    moveTabBefore(dragSource.id, getTabIndex(t.id));
                  } else {
                    //System.out.println("move "+dragSource.id+" aft " + t.id);
                    moveTabAfter(dragSource.id, getTabIndex(t.id));
                  }
                } else {
                  // move to other window
                  //System.out.println("move from pane " + dragSource.owner.id + " to " + t.owner.id);
                  dragSource.owner.removeTab(dragSource, false, true);
                  Tab evaTab = t.owner.createTab(dragSource.id, dragSource.content);
                  evaTab.setText(dragSource.getText());
                  if (moveBeforeElseAfter(me, t)) {
                    t.owner.moveTabBefore(evaTab.id, t.owner.getTabIndex(t.id));
                  } else {
                    t.owner.moveTabAfter(evaTab.id, t.owner.getTabIndex(t.id));
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

  public static class Tab extends JPanel {
    public Component content;
    public SimpleTabPane owner;
    String text;
    final public String id;
    JButton closeButton;
    private int markDrop;
    final static Color colMarkDrop = new Color(255,255,255,64);

    public Tab(final String n) {
      text = n;
      id = n;
      setLayout(new BorderLayout());
      JButton b = new JButton(new AbstractAction("x") {
        @Override
        public void actionPerformed(ActionEvent e) {
          Tab.this.owner.removeTab(id);
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
      return text;
    }
    
    public void setText(String t) {
      text = t;
      if (owner != null) {
        owner.computeLayout();
      }
      repaint();
    }
    
    public String getID() {
      return id;
    }
    
    public Component getContent() {
      return content;
    }
    
    public SimpleTabPane getPane() {
      return owner;
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
      int w = g.getFontMetrics().stringWidth(getText());
      g.drawString(getText(), Math.max(0, (getWidth() - w) / 2),
          getFont().getSize());
      paintComponents(g);
      g.setColor(colMarkDrop);
      if (markDrop == 1) {
        g.fillRect(0, 0, getWidth()/2, getHeight());
      } else if (markDrop == 2) {
        g.fillRect(getWidth()/2, 0, getWidth()/2, getHeight());
      }
    }

  } // class Tab
  
  public interface TabListener {
    public void tabRemoved(SimpleTabPane tp, Tab t, Component content);
    public void tabPaneEmpty(SimpleTabPane pane);
    public void tabSelected(SimpleTabPane pane, Tab t);
  }
}
