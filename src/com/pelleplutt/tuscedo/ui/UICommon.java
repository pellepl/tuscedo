package com.pelleplutt.tuscedo.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicScrollBarUI;

import com.pelleplutt.tuscedo.Settings;
import com.pelleplutt.tuscedo.Settings.ModCallback;
import com.pelleplutt.tuscedo.Tuscedo;
import com.pelleplutt.util.FastTextPane;

public class UICommon {
  public static final Font COMMON_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 11);


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
  
  public static FastTextPane.Style STYLE_CONN_IN;
  public static FastTextPane.Style STYLE_BASH_INPUT;
  public static FastTextPane.Style STYLE_BASH_DBG;
  public static FastTextPane.Style STYLE_BASH_OUT;
  public static FastTextPane.Style STYLE_BASH_ERR;
  public static FastTextPane.Style STYLE_FIND;
  public static FastTextPane.Style STYLE_FIND_ALL;
  public static FastTextPane.Style STYLE_SERIAL_INFO;
  public static FastTextPane.Style STYLE_SERIAL_ERR;
  
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
        if (STYLE_FIND_ALL == null) STYLE_FIND_ALL = new FastTextPane.Style(STYLE_ID_FIND_ALL, colFindMarkFg, colFindMarkBg, true);
        else STYLE_FIND_ALL.setFgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_gen_info_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colGenericInfoFg = new Color(val);
        if (STYLE_SERIAL_INFO == null) STYLE_SERIAL_INFO = new FastTextPane.Style(STYLE_ID_SERIAL_INFO, colGenericInfoFg, null, true);
        else STYLE_SERIAL_INFO.setFgColor(val); 
        Tuscedo.inst().redecorateAll();
      }
    });
    s.listenTrig("col_gen_err_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colGenericErrFg = new Color(val);
        if (STYLE_SERIAL_ERR == null) STYLE_SERIAL_ERR = new FastTextPane.Style(STYLE_ID_SERIAL_ERR, colGenericErrFg, null, true);
        else STYLE_SERIAL_ERR.setFgColor(val); 
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

  }
  
  public static final int STYLE_ID_CONN_IN = 1;
  public static final int STYLE_ID_HELP = 5;
  public static final int STYLE_ID_BASH_OUT = 10;
  public static final int STYLE_ID_BASH_ERR = 11;
  public static final int STYLE_ID_BASH_INPUT = 12;
  public static final int STYLE_ID_BASH_DBG = 13;
  public static final int STYLE_ID_SERIAL_INFO = 20;
  public static final int STYLE_ID_SERIAL_ERR = 21;
  public static final int STYLE_ID_FIND = 30;
  public static final int STYLE_ID_FIND_ALL = 31;
  
  public static void decorateFTP(FastTextPane ftp) {
    if (ftp == null) return;
    ftp.setForeground(colTextFg);
    ftp.setBackground(colGenericBg);
    ftp.setFont(COMMON_FONT);
  }
  
  public static void decorateComponent(JComponent c) {
    if (c == null) return;
    c.setForeground(colTextFg);
    c.setBackground(colGenericBg);
    c.setFont(COMMON_FONT);
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
    tp.setFont(COMMON_FONT);
    tp.setBackground(colInputBg);
    tp.setCaretColor(new Color(192, 192, 192));
    tp.setForeground(colInputFg);
    tp.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
  }
  
  public static void defineAction(JComponent c, String name, String keys, 
      AbstractAction action) {
    KeyMap key = KeyMap.getKeyDef(name);
    if (key == null) {
      key = KeyMap.fromString(keys);
      KeyMap.set(name, keys);
    }
    c.getInputMap(JComponent.WHEN_FOCUSED).put(
        KeyStroke.getKeyStroke(key.keyCode, key.modifiers),
        name);
    c.getActionMap().put(name, action);
  }

  public static void defineAnonAction(JComponent c, String name, String keys, int when, 
      AbstractAction action) {
    KeyMap key = KeyMap.fromString(keys);
    c.getInputMap(when).put(
        KeyStroke.getKeyStroke(key.keyCode, key.modifiers),
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
}
