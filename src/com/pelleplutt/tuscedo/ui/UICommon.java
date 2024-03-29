package com.pelleplutt.tuscedo.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import com.pelleplutt.tuscedo.Settings;
import com.pelleplutt.tuscedo.Settings.ModCallback;
import com.pelleplutt.tuscedo.Tuscedo;
import com.pelleplutt.tuscedo.ui.UISimpleTabPane.Tab;
import com.pelleplutt.util.FastTextPane;

public class UICommon {
  public static String COMMON_FONT = Font.MONOSPACED;
  public static Font font = new Font(COMMON_FONT, Font.PLAIN, 11);

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
  public static Color colTimestampFg;
  public static Color colScrollBarLFg;
  public static Color colScrollBarDFg;
  public static Color colScrollBarLBg;
  public static Color colScrollBarDBg;
  public static Color colDivMain = new Color(0xf0f000);
  public static Color colDivSec = new Color(0xa0a000);
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
  public static FastTextPane.Style STYLE_TIMESTAMP;
  public static FastTextPane.Style STYLE_OP_IN;
  public static FastTextPane.Style STYLE_OP_DBG;
  public static FastTextPane.Style STYLE_OP_OUT;
  public static FastTextPane.Style STYLE_OP_ERR;

  public static final int STYLE_ID_CONN_IN = 1;
  public static final int STYLE_ID_TIMESTAMP = 3;
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

  static void uiUpdate() {
    Tuscedo.inst().redecorateAll();
    Tuscedo.settingsDirty = true;
  }

  static {
    Settings s = Settings.inst();
    s.listenTrigInt("col_gen_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colGenericBg = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_text_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colTextFg = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_timestamp_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colTimestampFg = new Color(val);
        if (STYLE_TIMESTAMP == null) STYLE_TIMESTAMP = new FastTextPane.Style(STYLE_ID_TIMESTAMP, colTimestampFg, null, false);
        else STYLE_TIMESTAMP.setFgColor(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_input_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colInputFg = new Color(val);
        if (STYLE_CONN_IN == null) STYLE_CONN_IN = new FastTextPane.Style(STYLE_ID_CONN_IN, colInputFg, null, false);
        else STYLE_CONN_IN.setFgColor(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_input_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colInputBg = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_input_bash_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colInputBashBg = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_bash_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colBashFg = new Color(val);
        if (STYLE_BASH_INPUT == null) STYLE_BASH_INPUT = new FastTextPane.Style(STYLE_ID_BASH_INPUT, colBashFg, null, false);
        else STYLE_BASH_INPUT.setFgColor(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_bash_dbg_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colBashDbgFg = new Color(val);
        if (STYLE_BASH_DBG == null) STYLE_BASH_DBG = new FastTextPane.Style(STYLE_ID_BASH_DBG, colBashDbgFg, null, false);
        else STYLE_BASH_DBG.setFgColor(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_op_out_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colOpOut = new Color(val);
        if (STYLE_OP_OUT == null) STYLE_OP_OUT = new FastTextPane.Style(STYLE_ID_OP_OUT, colOpOut, null, false);
        else STYLE_OP_OUT.setFgColor(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_op_in_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colOpIn = new Color(val);
        if (STYLE_OP_IN == null) STYLE_OP_IN = new FastTextPane.Style(STYLE_ID_OP_IN, colOpIn, null, false);
        else STYLE_OP_IN.setFgColor(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_op_err_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colOpErr = new Color(val);
        if (STYLE_OP_ERR == null) STYLE_OP_ERR = new FastTextPane.Style(STYLE_ID_OP_ERR, colOpErr, null, false);
        else STYLE_OP_ERR.setFgColor(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_op_dbg_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colOpDbg = new Color(val);
        if (STYLE_OP_DBG == null) STYLE_OP_DBG = new FastTextPane.Style(STYLE_ID_OP_DBG, colOpDbg, null, false);
        else STYLE_OP_DBG.setFgColor(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_process_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colProcessFg = new Color(val);
        if (STYLE_BASH_OUT == null) STYLE_BASH_OUT = new FastTextPane.Style(STYLE_ID_BASH_OUT, colProcessFg, null, false);
        else STYLE_BASH_OUT.setFgColor(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_process_err_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colProcessErrFg = new Color(val);
        if (STYLE_BASH_ERR == null) STYLE_BASH_ERR = new FastTextPane.Style(STYLE_ID_BASH_ERR, colProcessErrFg, null, false);
        else STYLE_BASH_ERR.setFgColor(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_find_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colFindFg = new Color(val);
        if (STYLE_FIND == null) STYLE_FIND = new FastTextPane.Style(STYLE_ID_FIND, colFindFg, colFindBg, false);
        else STYLE_FIND.setFgColor(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_find_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colFindBg = new Color(val);
        if (STYLE_FIND == null) STYLE_FIND = new FastTextPane.Style(STYLE_ID_FIND, colFindFg, colFindBg, false);
        else STYLE_FIND.setBgColor(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_find_mark_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colFindMarkFg = new Color(val);
        if (STYLE_FIND_MARK == null) STYLE_FIND_MARK = new FastTextPane.Style(STYLE_ID_FIND_MARK, colFindMarkFg, colFindMarkBg, true);
        else STYLE_FIND_MARK.setFgColor(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_gen_info_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colGenericInfoFg = new Color(val);
        if (STYLE_GENERIC_INFO == null) STYLE_GENERIC_INFO = new FastTextPane.Style(STYLE_ID_GENERIC_INFO, colGenericInfoFg, null, true);
        else STYLE_GENERIC_INFO.setFgColor(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_gen_err_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colGenericErrFg = new Color(val);
        if (STYLE_GENERIC_ERR == null) STYLE_GENERIC_ERR = new FastTextPane.Style(STYLE_ID_GENERIC_ERR, colGenericErrFg, null, true);
        else STYLE_GENERIC_ERR.setFgColor(val);
        uiUpdate();
      }
    });

    s.listenTrigInt("scrollbar_w.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        uiUpdate();
      }
    });
    s.listenTrigInt("scrollbar_h.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        uiUpdate();
      }
    });

    s.listenTrigInt("col_tab_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colTabFg = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_tab_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colTabBg = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_tab_sel_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colTabSelBg = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_tab_nonsel_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colTabNonSelBg = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_tab_notifynew_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colTabNotifyNewFg = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_tab_notifyold_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colTabNotifyOldFg = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_scrollbar_l_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colScrollBarLFg = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_scrollbar_d_fg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colScrollBarDFg = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_scrollbar_l_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colScrollBarLBg = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_scrollbar_d_bg.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colScrollBarDBg = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_div_main.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colDivMain = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("col_div_sec.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        colDivSec = new Color(val);
        uiUpdate();
      }
    });
    s.listenTrigInt("div_size.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        uiUpdate();
      }
    });
    s.listenTrigInt("font_size.int", new ModCallback<Integer>() {
      public void modified(String key, Integer val) {
        font = new Font(font.getFontName(), Font.PLAIN, val);
        uiUpdate();
      }
    });
    s.listenTrigString("font_name.string", new ModCallback<String>() {
      public void modified(String key, String val) {
        font = new Font(val, Font.PLAIN, font.getSize());
        uiUpdate();
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

  public static void decorateJButton(JButton b) {
    if (b == null) return;
    b.setFont(font);
    b.setBackground(colScrollBarDBg);
    b.setForeground(colInputFg);
    b.setBorder(new CompoundBorder(BorderFactory.createLineBorder(colScrollBarLFg, 1), BorderFactory.createEmptyBorder(4,16,4,16)));
    b.setRolloverEnabled(false);

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

  public static void decorateSplitPane(JSplitPane sp, final boolean main) {
    if (sp == null) return;
    sp.setUI(new BasicSplitPaneUI() {
      @Override
      public BasicSplitPaneDivider createDefaultDivider() {
        return new BasicSplitPaneDivider(this) {
          public void setBorder(Border b) {
          }

          @Override
          public void paint(Graphics g) {
            g.setColor(main ? colDivMain : colDivSec);
            g.fillRect(0, 0, getSize().width, getSize().height);
            super.paint(g);
          }
        };
      }
    });
    sp.setBorder(null);
    sp.setDividerSize(Settings.inst().integer("div_size.int"));
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
    UICommon.defineAction(c, "common.tab.add", "ctrl+shift+t", when, actionAddTab);
    UICommon.defineAction(c, "common.tab.addfd", "ctrl+shift+q", when, actionAddFDTab);
    UICommon.defineAction(c, "common.tab.close", "ctrl+d", when, actionCloseTab);
    UICommon.defineAction(c, "common.app.close", "ctrl+alt+d", when, actionCloseApp);
    UICommon.defineAction(c, "common.tab.1", "ctrl+shift+1",when, actionSelTab[0]);
    UICommon.defineAction(c, "common.tab.2", "ctrl+shift+2",when, actionSelTab[1]);
    UICommon.defineAction(c, "common.tab.3", "ctrl+shift+3",when, actionSelTab[2]);
    UICommon.defineAction(c, "common.tab.4", "ctrl+shift+4",when, actionSelTab[3]);
    UICommon.defineAction(c, "common.tab.5", "ctrl+shift+5",when, actionSelTab[4]);
    UICommon.defineAction(c, "common.tab.6", "ctrl+shift+6",when, actionSelTab[5]);
    UICommon.defineAction(c, "common.tab.7", "ctrl+shift+7",when, actionSelTab[6]);
    UICommon.defineAction(c, "common.tab.8", "ctrl+shift+8",when, actionSelTab[7]);
    UICommon.defineAction(c, "common.tab.9", "ctrl+shift+9",when, actionSelTab[8]);
    UICommon.defineAction(c, "common.tab.10", "ctrl+shift+0",when, actionSelTab[9]);
    UICommon.defineAction(c, "common.tab.prev", "ctrl+page_up",when, actionPrevTab);
    UICommon.defineAction(c, "common.tab.next", "ctrl+page_down",when, actionNextTab);
  }

  static AbstractAction actionAddTab = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      Tuscedo.inst().addWorkAreaTab(
          UISimpleTabPane.getTabByComponent((Component)e.getSource()).getPane(),
          null);
    }
  };

  static AbstractAction actionAddFDTab = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      Tab tab = UISimpleTabPane.getTabByComponent((Component)e.getSource());
      if (tab.getContent() instanceof UIWorkArea) {
        Tuscedo.inst().addFDTab(tab.getPane(), (UIWorkArea)tab.getContent());
      }
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

  static AbstractAction actionCloseApp = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      Tuscedo.onExit();
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

  static AbstractAction actionPrevTab = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      UISimpleTabPane.Tab t = UISimpleTabPane.getTabByComponent((Component)e.getSource());
      UISimpleTabPane stp = t.getPane();
      stp.selectPrevTab();
    }
  };

  static AbstractAction actionNextTab = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      UISimpleTabPane.Tab t = UISimpleTabPane.getTabByComponent((Component)e.getSource());
      UISimpleTabPane stp = t.getPane();
      stp.selectNextTab();
    }
  };
}
