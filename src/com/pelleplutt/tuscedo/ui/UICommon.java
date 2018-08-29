package com.pelleplutt.tuscedo.ui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.plaf.basic.*;

import com.pelleplutt.tuscedo.*;
import com.pelleplutt.tuscedo.Settings.*;
import com.pelleplutt.util.*;

public class UICommon {
  public static Font font = new Font(Font.MONOSPACED, Font.PLAIN, 11);

  public static Color colBashDbgFg;
  public static Color colBashFg;
  public static Color colFindBg;
  public static Color colFindFg;
  public static Color colFindMarkBg = null;
  public static Color colFindMarkFg;
  public static Color colGenericBg;
  public static Color colGenericErrFg;
  public static Color colGenericInfoFg;
  public static Color colInputBashBg;
  public static Color colInputBg;
  public static Color colInputFg;
  public static Color colProcessErrFg;
  public static Color colProcessFg;
  public static Color colTabBg;
  public static Color colTabFg;
  public static Color colTabNonSelBg;
  public static Color colTabNotifyNewFg;
  public static Color colTabNotifyOldFg;
  public static Color colTabSelBg;
  public static Color colTextFg;
  public static Color colScrollBarLFg;
  public static Color colScrollBarDFg;
  public static Color colScrollBarLBg;
  public static Color colScrollBarDBg;
  public static Color colOpOut;
  public static Color colOpIn;
  public static Color colOpErr;
  public static Color colOpDbg;
  
  public static FastTextPane.Style STYLE_CONN_IN;
  public static FastTextPane.Style STYLE_BASH_INPUT;
  public static FastTextPane.Style STYLE_BASH_DBG;
  public static FastTextPane.Style STYLE_BASH_OUT;
  public static FastTextPane.Style STYLE_BASH_ERR;
  public static FastTextPane.Style STYLE_FIND;
  public static FastTextPane.Style STYLE_FIND_MARK;
  public static FastTextPane.Style STYLE_GENERIC_INFO;
  public static FastTextPane.Style STYLE_GENERIC_ERR;
  public static FastTextPane.Style STYLE_OP_IN;
  public static FastTextPane.Style STYLE_OP_DBG;
  public static FastTextPane.Style STYLE_OP_OUT;
  public static FastTextPane.Style STYLE_OP_ERR;
    
  public static final int STYLE_ID_CONN_IN = 1;
  public static final int STYLE_ID_HELP = 5;
  public static final int STYLE_ID_BASH_OUT = 10;
  public static final int STYLE_ID_BASH_ERR = 11;
  public static final int STYLE_ID_BASH_INPUT = 12;
  public static final int STYLE_ID_BASH_DBG = 13;
  public static final int STYLE_ID_FIND = 30;
  public static final int STYLE_ID_FIND_MARK = 31;
  public static final int STYLE_ID_GENERIC_INFO = 20;
  public static final int STYLE_ID_GENERIC_ERR = 21;
  public static final int STYLE_ID_OP_OUT = 40;
  public static final int STYLE_ID_OP_ERR = 41;
  public static final int STYLE_ID_OP_IN = 42;
  public static final int STYLE_ID_OP_DBG = 43;
  
  static {
    Settings s = Settings.inst();
    s.listenTrig("col_gen_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colGenericBg = new Color(val);
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_text_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colTextFg = new Color(val);
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_input_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colInputFg = new Color(val);
        if (STYLE_CONN_IN == null) STYLE_CONN_IN = new FastTextPane.Style(STYLE_ID_CONN_IN, colInputFg, null, false);
        else STYLE_CONN_IN.setFgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_input_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colInputBg = new Color(val);
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_input_bash_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colInputBashBg = new Color(val);
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_bash_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colBashFg = new Color(val);
        if (STYLE_BASH_INPUT == null) STYLE_BASH_INPUT = new FastTextPane.Style(STYLE_ID_BASH_INPUT, colBashFg, null, false);
        else STYLE_BASH_INPUT.setFgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_bash_dbg_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colBashDbgFg = new Color(val);
        if (STYLE_BASH_DBG == null) STYLE_BASH_DBG = new FastTextPane.Style(STYLE_ID_BASH_DBG, colBashDbgFg, null, false);
        else STYLE_BASH_DBG.setFgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_op_out_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colOpOut = new Color(val);
        if (STYLE_OP_OUT == null) STYLE_OP_OUT = new FastTextPane.Style(STYLE_ID_OP_OUT, colOpOut, null, false);
        else STYLE_OP_OUT.setFgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_op_in_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colOpIn = new Color(val);
        if (STYLE_OP_IN == null) STYLE_OP_IN = new FastTextPane.Style(STYLE_ID_OP_IN, colOpIn, null, false);
        else STYLE_OP_IN.setFgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_op_err_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colOpErr = new Color(val);
        if (STYLE_OP_ERR == null) STYLE_OP_ERR = new FastTextPane.Style(STYLE_ID_OP_ERR, colOpErr, null, false);
        else STYLE_OP_ERR.setFgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_op_dbg_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colOpDbg = new Color(val);
        if (STYLE_OP_DBG == null) STYLE_OP_DBG = new FastTextPane.Style(STYLE_ID_OP_DBG, colOpDbg, null, false);
        else STYLE_OP_DBG.setFgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_process_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colProcessFg = new Color(val);
        if (STYLE_BASH_OUT == null) STYLE_BASH_OUT = new FastTextPane.Style(STYLE_ID_BASH_OUT, colProcessFg, null, false);
        else STYLE_BASH_OUT.setFgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_process_err_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colProcessErrFg = new Color(val);
        if (STYLE_BASH_ERR == null) STYLE_BASH_ERR = new FastTextPane.Style(STYLE_ID_BASH_ERR, colProcessErrFg, null, false);
        else STYLE_BASH_ERR.setFgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_find_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colFindFg = new Color(val);
        if (STYLE_FIND == null) STYLE_FIND = new FastTextPane.Style(STYLE_ID_FIND, colFindFg, colFindBg, false);
        else STYLE_FIND.setFgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_find_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colFindBg = new Color(val);
        if (STYLE_FIND == null) STYLE_FIND = new FastTextPane.Style(STYLE_ID_FIND, colFindFg, colFindBg, false);
        else STYLE_FIND.setBgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_find_mark_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colFindMarkFg = new Color(val);
        if (STYLE_FIND_MARK == null) STYLE_FIND_MARK = new FastTextPane.Style(STYLE_ID_FIND_MARK, colFindMarkFg, colFindMarkBg, true);
        else STYLE_FIND_MARK.setFgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_gen_info_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colGenericInfoFg = new Color(val);
        if (STYLE_GENERIC_INFO == null) STYLE_GENERIC_INFO = new FastTextPane.Style(STYLE_ID_GENERIC_INFO, colGenericInfoFg, null, true);
        else STYLE_GENERIC_INFO.setFgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_gen_err_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colGenericErrFg = new Color(val);
        if (STYLE_GENERIC_ERR == null) STYLE_GENERIC_ERR = new FastTextPane.Style(STYLE_ID_GENERIC_ERR, colGenericErrFg, null, true);
        else STYLE_GENERIC_ERR.setFgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    
    s.listenTrig("scrollbar_w.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("scrollbar_h.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        Tuscedo.inst().redecorateAll();
      }
    });

    s.listenTrig("col_tab_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colTabFg = new Color(val);
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_tab_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colTabBg = new Color(val);
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_tab_sel_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colTabSelBg = new Color(val);
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_tab_nonsel_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colTabNonSelBg = new Color(val);
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_tab_notifynew_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colTabNotifyNewFg = new Color(val);
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_tab_notifyold_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colTabNotifyOldFg = new Color(val);
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_scrollbar_l_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colScrollBarLFg = new Color(val);
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_scrollbar_d_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colScrollBarDFg = new Color(val);
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_scrollbar_l_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colScrollBarLBg = new Color(val);
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_scrollbar_d_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colScrollBarDBg = new Color(val);
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("font_size.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        font = new Font(Font.MONOSPACED, Font.PLAIN, val);
        Tuscedo.inst().redecorateAll();
      }
    });
  }

  public static void decorateFTP(FastTextPane ftp) {
    if (ftp == null) return;
    ftp.setForeground(colTextFg);
    ftp.setBackground(colGenericBg);
    ftp.setFont(font);
  }
  
  public static void decorateComponent(JComponent c) {
    if (c == null) return;
    c.setForeground(colTextFg);
    c.setBackground(colGenericBg);
    c.setFont(font);
  }
  
  public static void decorateHiliteLabel(JLabel l) {
    if (l == null) return;
    l.setFont(font);
    l.setBackground(colInputFg);
    l.setForeground(colInputBg);
    l.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
  }
  
  public static void decorateScrollPane(JScrollPane sp) {
    if (sp == null) return;
    sp.setVisible(false);
    
    UIManager.put("ScrollBar.width", Settings.inst().integer("scrollbar_w.int"));
    UIManager.put("ScrollBar.height", Settings.inst().integer("scrollbar_h.int"));

    sp.getVerticalScrollBar().setUI(new SpecScrollBarUI());
    sp.getHorizontalScrollBar().setUI(new SpecScrollBarUI());
    sp.getVerticalScrollBar().setBackground(Color.black);
    sp.getHorizontalScrollBar().setBackground(Color.black);
    sp.setBackground(colGenericBg);
    sp.setBorder(null);
    sp.getHorizontalScrollBar().setFocusable(false);
    sp.getVerticalScrollBar().setFocusable(false);
    sp.getHorizontalScrollBar().setRequestFocusEnabled(false);
    sp.getVerticalScrollBar().setRequestFocusEnabled(false);
    sp.setFocusable(false);
    sp.setVisible(true);
  }
  
  public static void decorateSplitPane(JSplitPane sp) {
    if (sp == null) return;
    sp.setBorder(null);
    sp.setDividerSize(4);
  }
  
  public static void decorateTextEditor(JTextPane tp) {
    if (tp == null) return;
    tp.setFont(font);
    tp.setBackground(colInputBg);
    tp.setCaretColor(new Color(192, 192, 192));
    tp.setForeground(colInputFg);
    tp.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
  }
  
  public static void defineAction(JComponent c, String name, String keys, 
      int when, AbstractAction action) {
    KeyMap key = KeyMap.getKeyDef(name);
    if (key == null) {
      key = KeyMap.fromString(keys);
      KeyMap.set(name, keys);
    }
    c.getInputMap(when).put(
        KeyStroke.getKeyStroke(key.keyCode, key.modifiers),
        name);
    c.getActionMap().put(name, action);
  }
  public static void defineAction(JComponent c, String name, String keys, 
      AbstractAction action) {
    defineAction(c, name, keys, JComponent.WHEN_FOCUSED, action);
  }

  public static void defineAnonAction(JComponent c, String name, String keys, int when, 
      AbstractAction action) {
    defineAnonAction(c, name, keys, when, true, action);
  }
  
  public static void defineAnonAction(JComponent c, String name, String keys, int when, 
      boolean keyDown, AbstractAction action) {
    KeyMap key = KeyMap.fromString(keys);
    c.getInputMap(when).put(
        KeyStroke.getKeyStroke(key.keyCode, key.modifiers, !keyDown),
        name);
    c.getActionMap().put(name, action);
  }
  
  private static JButton createZeroButton() {
    JButton jbutton = new JButton();
    jbutton.setPreferredSize(new Dimension(0, 0));
    jbutton.setMinimumSize(new Dimension(0, 0));
    jbutton.setMaximumSize(new Dimension(0, 0));
    jbutton.setFocusable(false);
    return jbutton;
  }

  static class SpecScrollBarUI extends BasicScrollBarUI {
    Paint ptrack, ttrack;

    @Override
    protected void paintTrack(Graphics og, JComponent c,
        Rectangle trackBounds) {
      Graphics2D g = (Graphics2D) og;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
      if (ptrack == null) {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
          ptrack = new GradientPaint(0, 0, colScrollBarDBg,
              UIManager.getInt("ScrollBar.width"), 0, colScrollBarLBg);
        } else {
          ptrack = new GradientPaint(0, 0, colScrollBarDBg, 0,
              UIManager.getInt("ScrollBar.height"), colScrollBarLBg);
        }
      }
      g.setPaint(ptrack);
      g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width,
          trackBounds.height);
    }

    @Override
    protected void paintThumb(Graphics og, JComponent c,
        Rectangle thumbBounds) {
      Graphics2D g = (Graphics2D) og;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
      if (ttrack == null) {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
          ttrack = new GradientPaint(0, 0, colScrollBarLFg,
              UIManager.getInt("ScrollBar.width"), 0, colScrollBarDFg);
        } else {
          ttrack = new GradientPaint(0, 0, colScrollBarLFg, 0,
              UIManager.getInt("ScrollBar.height"), colScrollBarDFg);
        }
      }
      g.setPaint(ttrack);
      g.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width,
          thumbBounds.height, UIManager.getInt("ScrollBar.width") / 2,
          UIManager.getInt("ScrollBar.height") / 2);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
      return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
      return createZeroButton();
    }
  } // class SpecScrollBarUI

  public static void defineCommonActions(JComponent c, int when) {
    UICommon.defineAction(c, "common.addtab", "ctrl+shift+t", when, actionAddTab);
    UICommon.defineAction(c, "common.closetab", "ctrl+d", when, actionCloseTab);
    UICommon.defineAction(c, "common.seltab1", "ctrl+shift+1",when, actionSelTab[0]);
    UICommon.defineAction(c, "common.seltab2", "ctrl+shift+2",when, actionSelTab[1]);
    UICommon.defineAction(c, "common.seltab3", "ctrl+shift+3",when, actionSelTab[2]);
    UICommon.defineAction(c, "common.seltab4", "ctrl+shift+4",when, actionSelTab[3]);
    UICommon.defineAction(c, "common.seltab5", "ctrl+shift+5",when, actionSelTab[4]);
    UICommon.defineAction(c, "common.seltab6", "ctrl+shift+6",when, actionSelTab[5]);
    UICommon.defineAction(c, "common.seltab7", "ctrl+shift+7",when, actionSelTab[6]);
    UICommon.defineAction(c, "common.seltab8", "ctrl+shift+8",when, actionSelTab[7]);
    UICommon.defineAction(c, "common.seltab9", "ctrl+shift+9",when, actionSelTab[8]);
    UICommon.defineAction(c, "common.seltab10", "ctrl+shift+0",when, actionSelTab[9]);
  }
  
  static AbstractAction actionAddTab = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      Tuscedo.inst().addWorkAreaTab(
          UISimpleTabPane.getTabByComponent((Component)e.getSource()).getPane(), null);
    }
  };
  
  static AbstractAction actionCloseTab = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      UISimpleTabPane.Tab t = UISimpleTabPane.getTabByComponent((Component)e.getSource()); 
      UISimpleTabPane stp = t.getPane();
      stp.removeTab(t);
    }
  };
  
  static AbstractAction actionSelTab[] = new AbstractAction[10];
  static {
    for (int i = 0; i < 10; i++) {
      final int j = i;
      actionSelTab[i] = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          selectTab(j, (Component)e.getSource());
        }
      };
    }
  }
  
  static void selectTab(int ix, Component c) {
    UISimpleTabPane.Tab t = UISimpleTabPane.getTabByComponent(c); 
    UISimpleTabPane stp = t.getPane();
    stp.selectTab(ix);
  }
}
