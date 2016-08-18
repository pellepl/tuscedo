package com.pelleplutt.tuscedo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicScrollBarUI;

import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.FastTextPane;
import com.pelleplutt.util.Log;
import com.pelleplutt.util.io.PortConnector;

public class Tuscedo {
  static final int ISTATE_INPUT = 0;
  static final int ISTATE_FIND = 1;
  static final int ISTATE_FIND_REGEX = 2;
  static final int _ISTATE_NUM = 3;
  
  static final int STYLE_ID_FIND = 10;
  static final int STYLE_ID_FIND_ALL = 11;

  JTextField input;
  JFrame f;
  FastTextPane ftp, ftp2;
  Font font = new Font(Font.MONOSPACED, Font.PLAIN, 11);
  JLabel inputLabel;
  JLabel infoLabel;
  int istate = ISTATE_INPUT;
  String savedInput[] = new String[_ISTATE_NUM];
  String infoStrings[] = new String[_ISTATE_NUM];
  Color colTextFg = new Color(255, 255, 192);
  Color colInputFg = new Color(192, 255, 255);
  Color colInputBg = new Color(48, 48, 64);
  Color colFindFg = colInputBg;
  Color colFindBg = colInputFg;
  Color colFindMarkFg = new Color(255,192,255);
  Color colFindMarkBg = null;
  int lastFindIndex = -1;
  String lastFindString = null;
  List<OffsetSpan> findResult;
  JSplitPane splitVer, splitHor;
  JScrollPane mainScrollPane;
  JScrollPane secScrollPane;
  JComponent curView;

  public Tuscedo() {
  }

  public void create() {
    build();
  }
  
  void decorateFTP(FastTextPane ftp) {
    ftp.setForeground(colTextFg);
    ftp.setBackground(Color.black);
    ftp.setFont(font);
  }
  
  void decorateScrollPane(JScrollPane sp) {
    sp.getVerticalScrollBar().setUI(new SpecScrollBarUI());
    sp.getHorizontalScrollBar().setUI(new SpecScrollBarUI());
    sp.getVerticalScrollBar().setBackground(Color.black);
    sp.getHorizontalScrollBar().setBackground(Color.black);
    sp.setBackground(Color.black);
    sp.setBorder(null);
    sp.getHorizontalScrollBar().setFocusable(false);
    sp.getVerticalScrollBar().setFocusable(false);
    sp.getHorizontalScrollBar().setRequestFocusEnabled(false);
    sp.getVerticalScrollBar().setRequestFocusEnabled(false);
    sp.setFocusable(false);
  }
  
  void decorateSplitPane(JSplitPane sp) {
    sp.setBorder(null);
    sp.setDividerSize(4);
  }

  private void build() {
    UIManager.put("ScrollBar.width", 6);
    UIManager.put("ScrollBar.height", 6);

    f = new JFrame();
    f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    f.getContentPane().setLayout(new BorderLayout());
    f.setSize(600, 400);
    f.setLocationByPlatform(true);

    ftp = new FastTextPane();
    decorateFTP(ftp);
    
    ftp2 = new FastTextPane();
    ftp2.setDocument(ftp.getDocument());
    decorateFTP(ftp2);

    mainScrollPane = new JScrollPane(ftp,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    decorateScrollPane(mainScrollPane);
    secScrollPane = new JScrollPane(ftp2,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    decorateScrollPane(secScrollPane);

    input = new JTextField();
    input.setFont(font);
    input.setBackground(colInputBg);
    input.setCaretColor(new Color(192, 192, 192));
    input.setForeground(colInputFg);
    input.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

    input.getInputMap().put(
        KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK),
        "find");
    input.getInputMap().put(
        KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
        "findshift");
    input.getInputMap().put(
        KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK),
        "findregx");
    input.getInputMap().put(
        KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
        "findregxshift");
    input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        "inputback");
    input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
        "inputenter");
    input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK),
        "inputentershift");
    input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK),
        "openserial");
    input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK),
        "splitnone");
    input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
        "splitver");
    input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK),
        "splithor");
    input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK),
        "clear");
    input.getActionMap().put("find", actionOpenFind);
    input.getActionMap().put("findshift", actionOpenFindShift);
    input.getActionMap().put("findregx", actionOpenFindRegex);
    input.getActionMap().put("findregxshift", actionOpenFindRegexShift);
    input.getActionMap().put("inputback", actionInputBack);
    input.getActionMap().put("inputenter", actionInputEnter);
    input.getActionMap().put("inputentershift", actionInputEnterShift);
    input.getActionMap().put("openserial", actionOpenSerial);
    input.getActionMap().put("splitnone", actionSplitNone);
    input.getActionMap().put("splitver", actionSplitVer);
    input.getActionMap().put("splithor", actionSplitHor);
    input.getActionMap().put("clear", actionClear);

    inputLabel = new JLabel();
    inputLabel.setFont(font);
    inputLabel.setBackground(colInputFg);
    inputLabel.setForeground(colInputBg);
    inputLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
    inputLabel.setVisible(false);

    infoLabel = new JLabel();
    infoLabel.setFont(font);
    infoLabel.setBackground(colInputFg);
    infoLabel.setForeground(colInputBg);
    infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
    infoLabel.setVisible(false);

    JPanel ip = new JPanel(new BorderLayout());
    ip.add(inputLabel, BorderLayout.WEST);
    ip.add(input, BorderLayout.CENTER);
    ip.add(infoLabel, BorderLayout.EAST);

    splitVer = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    decorateSplitPane(splitVer);
    splitVer.setDividerLocation(0);
    splitHor = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    decorateSplitPane(splitHor);
    splitHor.setDividerLocation(100);
    
    curView = mainScrollPane;
    f.getContentPane().add(mainScrollPane, BorderLayout.CENTER);
    f.getContentPane().add(ip, BorderLayout.SOUTH);

    String text = AppSystem
        .readFile(new File("/home/petera/proj/generic/spiffs/src/spiffs_nucleus.c"));
    ftp.setText(text);
    
    f.setVisible(true);
  }

  public void transmit(String s) {
  }
  
  public void setInfo(String s) {
    infoStrings[istate] = s;
    if (s != null && s.length() > 0) {
      infoLabel.setText(s);
      infoLabel.setVisible(true);
    } else {
      infoLabel.setVisible(false);
    }
  }

  protected void findAllOccurences(String regex, boolean isRegex) {
    String text = ftp.getText();
    Pattern pat = Pattern.compile(isRegex ? regex : Pattern.quote(regex));
    Matcher mat = pat.matcher(text);
    findResult = new ArrayList<OffsetSpan>();
    while (mat.find()) {
      ftp.addStyleByOffset(STYLE_ID_FIND_ALL, colFindMarkFg, colFindMarkBg, true, 
          mat.start(), mat.end());
      if (mat.end() > mat.start()) {
        findResult.add(new OffsetSpan(mat.start(), mat.end()));
      }
    }
    
    setInfo(findResult.isEmpty() ? ("NONE FOUND") : "FOUND " + findResult.size());
  }
  
  public void handleFind(String str, boolean shift, boolean regex) {
    try {
      if (str == null || str.length() == 0) {
        setInfo("");
        lastFindString = null;
        lastFindIndex = 0;
        return;
      }
      ftp.removeStyle(STYLE_ID_FIND);
      OffsetSpan offs = null;
      if (!str.equals(lastFindString)) {
        ftp.removeStyle(STYLE_ID_FIND_ALL);
        findAllOccurences(str, regex);
        if (!findResult.isEmpty()) {
          lastFindIndex = shift ? findResult.size() - 1 : 0;
          offs = findResult.get(lastFindIndex);
        }
      } else {
        if (shift) {
          lastFindIndex--;
          if (lastFindIndex < 0) {
            lastFindIndex = findResult.size() - 1;
          }
        } else {
          lastFindIndex++;
          if (lastFindIndex >= findResult.size()) {
            lastFindIndex = 0;
          }
        }
        if (!findResult.isEmpty()) {
          offs = findResult.get(lastFindIndex);
        }
      }
      if (offs != null) {
        lastFindString = str;
        ftp.addStyleByOffset(STYLE_ID_FIND, colFindFg, colFindBg, true, offs.start, offs.end);
        ftp.scrollToOffset(offs.start);
        setInfo((lastFindIndex + 1) + " OF " + findResult.size());
      }
    } catch (PatternSyntaxException pse) {
      setInfo("BAD REGEX");
    }
  }

  public void enterInputState(int istate) {
    if (istate == this.istate) return;
    leaveInputState(this.istate);
    this.istate = istate;
    Log.println("enter state " + istate);
    switch (istate) {
    case ISTATE_FIND:
      inputLabel.setText("FIND");
      break;
    case ISTATE_FIND_REGEX:
      inputLabel.setText("REGEX");
      break;
    default:
      this.istate = ISTATE_INPUT;
      inputLabel.setVisible(false);
      break;
    }
    input.setText(savedInput[istate]);
    setInfo(infoStrings[istate]);
    if (istate != ISTATE_INPUT) {
      inputLabel.setVisible(true);
    }
  }
  
  public void leaveInputState(int istate) {
    Log.println("leaving state " + istate);
    savedInput[istate] = input.getText();
    switch (istate) {
    case ISTATE_FIND:
    case ISTATE_FIND_REGEX:
      infoStrings[istate] = null;
      lastFindIndex = -1;
      lastFindString = null;
      ftp.removeStyle(STYLE_ID_FIND);
      ftp.removeStyle(STYLE_ID_FIND_ALL);
      break;
    }
  }

  void actionFind(boolean shift, boolean regex) {
    if (istate != ISTATE_FIND && !regex) {
      Tuscedo.this.enterInputState(ISTATE_FIND);
    } else if (istate != ISTATE_FIND_REGEX && regex) {
      Tuscedo.this.enterInputState(ISTATE_FIND_REGEX);
    } else {
      handleFind(input.getText(), shift, regex);
    }
  }
  
  AbstractAction actionOpenFind = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionFind(false, false);
    }
  };

  AbstractAction actionOpenFindShift = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionFind(true, false);
    }
  };
  
  AbstractAction actionOpenFindRegex = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionFind(false, true);
    }
  };

  AbstractAction actionOpenFindRegexShift = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionFind(true, true);
    }
  };

  AbstractAction actionInputBack = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (input.getText().length() > 0) {
        input.setText("");
      } else {
        Tuscedo.this.enterInputState(ISTATE_INPUT);
      }
    }
  };

  void actionInputEnter(boolean shift) {
    switch (istate) {
    case ISTATE_FIND:
      handleFind(input.getText(), shift, false);
      break;
    case ISTATE_FIND_REGEX:
      handleFind(input.getText(), shift, true);
      break;
    default:
      String s = input.getText() + "\n";
      transmit(s);
      ftp.addText(s, 1, colInputFg, null, false);
      ftp.scrollToLineNumber(ftp.countLines() + 1);
      ftp2.scrollToLineNumber(ftp.countLines() + 1);
      input.setText("");
      break;
    }
  }

  AbstractAction actionInputEnter = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionInputEnter(false);
    }
  };

  AbstractAction actionInputEnterShift = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionInputEnter(true);
    }
  };

  AbstractAction actionOpenSerial = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      PortConnector pc = PortConnector.getPortConnector();
      String[] devices = pc.getDevices();
      for (String s : devices) {
        Log.println(s);
      }
    }
  };

  AbstractAction actionSplitNone = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (curView == mainScrollPane) return;
      f.getContentPane().remove(curView);
      f.getContentPane().add(mainScrollPane);
      f.revalidate();
      curView = mainScrollPane;
    }
  };

  AbstractAction actionSplitVer = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (curView == splitVer) return;
      f.getContentPane().remove(curView);
      splitVer.setTopComponent(secScrollPane);
      splitVer.setBottomComponent(mainScrollPane);
      f.getContentPane().add(splitVer);
      f.revalidate();
      splitVer.setDividerLocation(0.5);
      curView = splitVer;
    }
  };

  AbstractAction actionSplitHor = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (curView == splitHor) return;
      f.getContentPane().remove(curView);
      splitHor.setLeftComponent(mainScrollPane);
      splitHor.setRightComponent(secScrollPane);
      f.getContentPane().add(splitHor);
      f.revalidate();
      splitHor.setDividerLocation(0.5);
      curView = splitHor;
    }
  };

  AbstractAction actionClear = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      ftp.clear();
    }
  };

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
          ptrack = new GradientPaint(0, 0, new Color(64, 64, 64),
              UIManager.getInt("ScrollBar.width"), 0, new Color(96, 96, 96));
        } else {
          ptrack = new GradientPaint(0, 0, new Color(64, 64, 64), 0,
              UIManager.getInt("ScrollBar.height"), new Color(96, 96, 96));
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
          ttrack = new GradientPaint(0, 0, new Color(196, 196, 196),
              UIManager.getInt("ScrollBar.width"), 0, new Color(128, 128, 128));
        } else {
          ttrack = new GradientPaint(0, 0, new Color(196, 196, 196), 0,
              UIManager.getInt("ScrollBar.height"), new Color(128, 128, 128));
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

    private JButton createZeroButton() {
      JButton jbutton = new JButton();
      jbutton.setPreferredSize(new Dimension(0, 0));
      jbutton.setMinimumSize(new Dimension(0, 0));
      jbutton.setMaximumSize(new Dimension(0, 0));
      jbutton.setFocusable(false);
      return jbutton;
    }

  }
  
  public class OffsetSpan {
    public int start, end;
    public OffsetSpan(int start, int end) {
      this.start = start;
      this.end = end;
    }
    
  }

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Throwable ignore) {
    }
    Tuscedo t = new Tuscedo();
    t.create();
  }
}
