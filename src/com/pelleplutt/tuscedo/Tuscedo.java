package com.pelleplutt.tuscedo;

import java.awt.BorderLayout;
import java.awt.CardLayout;
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
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicScrollBarUI;

import com.pelleplutt.Essential;
import com.pelleplutt.tuscedo.ui.ACTextField;
import com.pelleplutt.tuscedo.ui.KeyMap;
import com.pelleplutt.tuscedo.ui.ProcessACTextField;
import com.pelleplutt.util.FastTextPane;
import com.pelleplutt.util.io.Port;

public class Tuscedo {
  static final int ISTATE_INPUT = 0;
  static final int ISTATE_FIND = 1;
  static final int ISTATE_FIND_REGEX = 2;
  static final int ISTATE_OPEN_SERIAL = 3;
  static final int _ISTATE_NUM = 4;
  
  static final int STYLE_ID_FIND = 10;
  static final int STYLE_ID_FIND_ALL = 11;
  
  static final String PORT_ARG_BAUD = "baud:";
  static final String PORT_ARG_DATABITS = "databits:";
  static final String PORT_ARG_PARITY = "parity:";
  static final String PORT_ARG_STOPBITS = "stopbits:";

  JFrame f;
  FastTextPane ftp, ftp2;
  static Font font = new Font(Font.MONOSPACED, Font.PLAIN, 11);
  int istate = ISTATE_INPUT;
  JPanel inputPanel = new JPanel();
  JLabel inputLabel[] = new JLabel[_ISTATE_NUM];
  JLabel infoLabel[] = new JLabel[_ISTATE_NUM];
  Bash bash[] = new Bash[_ISTATE_NUM];
  ProcessACTextField input[] = new ProcessACTextField[_ISTATE_NUM];
  public static final Color colTextFg = new Color(255, 255, 192);
  public static final Color colInputFg = new Color(192, 255, 255);
  public static final Color colInputBg = new Color(48, 48, 64);
  public static final Color colInputBashBg = new Color(64, 64, 64);
  public static final Color colBashFg = new Color(255, 255, 255);
  public static final Color colProcessFg = new Color(192, 192, 192);
  public static final Color colProcessErrFg = new Color(192, 128, 128);
  public static final Color colFindFg = colInputBg;
  public static final Color colFindBg = colInputFg;
  public static final Color colFindMarkFg = new Color(255,192,255);
  public static final Color colFindMarkBg = null;
  int lastFindIndex = -1;
  String lastFindString = null;
  List<OffsetSpan> findResult;
  JSplitPane splitVer, splitHor;
  JScrollPane mainScrollPane;
  JScrollPane secScrollPane;
  JComponent curView;
  Settings settings;
  Serial serial;
  
  String[] prevSerialDevices;
  
  public Tuscedo() {
  }

  public void create() {
    build();
    settings = Settings.inst();
  }
  
  public static void decorateFTP(FastTextPane ftp) {
    ftp.setForeground(colTextFg);
    ftp.setBackground(Color.black);
    ftp.setFont(font);
  }
  
  public static void decorateScrollPane(JScrollPane sp) {
    UIManager.put("ScrollBar.width", 6);
    UIManager.put("ScrollBar.height", 6);

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
  
  public static void decorateSplitPane(JSplitPane sp) {
    sp.setBorder(null);
    sp.setDividerSize(4);
  }
  
  public static void decorateTextEditor(JTextPane tp) {
    tp.setFont(font);
    tp.setBackground(colInputBg);
    tp.setCaretColor(new Color(192, 192, 192));
    tp.setForeground(colInputFg);
    tp.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
  }
  
  public static void defineAction(JComponent c, String name, String keys, AbstractAction action) {
  	KeyMap key = KeyMap.getKeyDef(name);
  	if (key == null) {
  		key = KeyMap.fromString(keys);
  		KeyMap.set(name, keys);
  	}
  	c.getInputMap().put(
        KeyStroke.getKeyStroke(key.keyCode, key.modifiers),
        name);
    c.getActionMap().put(name, action);
  }

  private void build() {
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
    
    serial = new Serial(ftp);
    
    mainScrollPane = new JScrollPane(ftp,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    decorateScrollPane(mainScrollPane);
    secScrollPane = new JScrollPane(ftp2,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    decorateScrollPane(secScrollPane);

    mainScrollPane.getVerticalScrollBar().addAdjustmentListener(new AutoAdjustmentListener(mainScrollPane));
    secScrollPane.getVerticalScrollBar().addAdjustmentListener(new AutoAdjustmentListener(secScrollPane));

    JPanel ip[] = new JPanel[_ISTATE_NUM];
    inputPanel.setLayout(new CardLayout(0,0));
    for (int i = 0; i < _ISTATE_NUM; i++) {
      input[i] = new ProcessACTextField();
      decorateTextEditor(input[i]);
      input[i].setSuggestionListener(new ACTextField.SuggestionListener() {
        @Override
        public void gotSuggestions(List<String> suggestions) {
          if (suggestions == null) {
            setInfo(null);
          } else {
            int s = suggestions.size();
            if (s > 1) {
              setInfo(s + " MATCHES");
            } else {
              setInfo(null);
            }
          }
        }
        
        @Override
        public List<String> giveSuggestions(final String userInput) {
          final String bashPrefix = settings.string(Settings.BASH_PREFIX_STRING);
          if (userInput.startsWith(settings.string(bashPrefix)) && userInput.length() >= bashPrefix.length()) {
            if (userInput.startsWith(bashPrefix + "cd ")) {
              return bash[istate].suggestFileSystemCompletions(bashPrefix, userInput.substring(bashPrefix.length()), 
                  "cd", false, true);
            }
          }
          return null;
        }
      });
      defineAction(input[i], "input.find", "ctrl+f", actionOpenFind);
      defineAction(input[i], "input.findback", "ctrl+shift+f", actionOpenFindBack);
      defineAction(input[i], "input.findregx", "alt+f", actionOpenFindRegex);
      defineAction(input[i], "input.findregxback", "alt+shift+f", actionOpenFindRegexBack);
      defineAction(input[i], "input.inputclose", "escape", actionInputClose);
      defineAction(input[i], "input.inputenter", "enter", actionInputEnter);
      defineAction(input[i], "input.inputenterback", "shift+enter", actionInputEnterBack);
      defineAction(input[i], "input.openserial", "ctrl+o", actionOpenSerialConfig);
      defineAction(input[i], "input.help", "f1", actionShowHelp);
      defineAction(input[i], "log.input.splitnone", "ctrl+w", actionSplitNone);
      defineAction(input[i], "log.input.splithori", "shift+ctrl+w", actionSplitHori);
      defineAction(input[i], "log.input.splitveri", "alt+ctrl+w", actionSplitVeri);
      defineAction(input[i], "log.input.clear", "ctrl+delete", actionClear);
      defineAction(input[i], "log.input.pageup", "alt+page_up", actionLogPageUp);
      defineAction(input[i], "log.input.pagedown", "alt+page_down", actionLogPageDown);
      defineAction(input[i], "log.input.scrollup", "alt+up", actionLogUp);
      defineAction(input[i], "log.input.scrolldown", "alt+down", actionLogDown);
      defineAction(input[i], "log.input.scrollleft", "alt+left", actionLogLeft);
      defineAction(input[i], "log.input.scrollright", "alt+right", actionLogRight);
      defineAction(input[i], "log.input.home", "ctrl+home", actionLogHome);
      defineAction(input[i], "log.input.end", "ctrl+end", actionLogEnd);

      inputLabel[i] = new JLabel();
      inputLabel[i].setFont(font);
      inputLabel[i].setBackground(colInputFg);
      inputLabel[i].setForeground(colInputBg);
      inputLabel[i].setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
      inputLabel[i].setVisible(false);

      infoLabel[i] = new JLabel();
      infoLabel[i].setFont(font);
      infoLabel[i].setBackground(colInputFg);
      infoLabel[i].setForeground(colInputBg);
      infoLabel[i].setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
      infoLabel[i].setVisible(false);
      
      ip[i] = new JPanel(new BorderLayout());
      ip[i].add(inputLabel[i], BorderLayout.WEST);
      ip[i].add(input[i], BorderLayout.CENTER);
      ip[i].add(infoLabel[i], BorderLayout.EAST);
      
      inputPanel.add(ip[i], Integer.toString(i));
      
      final ProcessHandler ph = input[i];
      bash[i] = new Bash(input[i], new Bash.Console() {
        @Override
        public void stdout(String s) {
          ftp.addText(s, 1, colProcessFg, null, false);
        }

        @Override
        public void stderr(String s) {
          ftp.addText(s, 1, colProcessErrFg, null, false);
        }
        
        @Override
        public void stdin(String s) {
          ph.sendToStdIn(s);
        }
      }, serial);
    }
    
    input[ISTATE_OPEN_SERIAL].setForcedModel(true);
    
    splitVer = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    decorateSplitPane(splitVer);
    splitVer.setDividerLocation(0);
    splitHor = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    decorateSplitPane(splitHor);
    splitHor.setDividerLocation(100);
    
    curView = mainScrollPane;
    f.getContentPane().add(mainScrollPane, BorderLayout.CENTER);
    f.getContentPane().add(inputPanel, BorderLayout.SOUTH);

    f.setVisible(true);
    
    ((CardLayout)inputPanel.getLayout()).show(inputPanel, Integer.toString(ISTATE_INPUT));
    input[ISTATE_INPUT].requestFocus();

  }

  public void setInfo(String s) {
    if (s != null && s.length() > 0) {
      infoLabel[istate].setText(s);
      infoLabel[istate].setVisible(true);
    } else {
      infoLabel[istate].setVisible(false);
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
  
  public void handleOpenSerial(String s) {
    input[ISTATE_OPEN_SERIAL].setEnabled(false);
    try {
      serial.closeSerial();
      Port portSetting = new Port();
      String defs[] = s.split(" ");
      portSetting.portName = defs[0];
      for (int i = 1; i < defs.length; i++) {
        String d = defs[i];
        if (d != null) {
          if (d.startsWith(PORT_ARG_BAUD)) {
            portSetting.baud = Integer.parseInt(d.substring(PORT_ARG_BAUD.length()));
          } else if (d.startsWith(PORT_ARG_DATABITS)) {
            portSetting.databits = Integer.parseInt(d.substring(PORT_ARG_DATABITS.length()));
          } else if (d.startsWith(PORT_ARG_PARITY)) {
            portSetting.parity = Port.parseParity(d.substring(PORT_ARG_PARITY.length()));
          } else if (d.startsWith(PORT_ARG_STOPBITS)) {
            portSetting.stopbits = Integer.parseInt(d.substring(PORT_ARG_STOPBITS.length()));
          }
        }
      }
      ftp.addText("Opening " + portSetting.portName + " " + portSetting.baud + " " + portSetting.databits +
          Port.parityToString(portSetting.parity).charAt(0) + portSetting.stopbits + "...\n", 
          1, Color.green, null, true);
      
      serial.open(portSetting);
      
      ftp.addText("Connected\n", 1, Color.green, null, true);
      enterInputState(ISTATE_INPUT);
    } catch (Exception e) {
      ftp.addText("Failed [" + e.getMessage() + "]\n", 1, Color.red, null, true);
    } finally {
      input[ISTATE_OPEN_SERIAL].setEnabled(true);
    }
  }

  public void enterInputState(int istate) {
    if (istate == this.istate) return;
    leaveInputState(this.istate);
    this.istate = istate;
    switch (istate) {
    case ISTATE_FIND:
      inputLabel[istate].setText("FIND");
      break;
    case ISTATE_FIND_REGEX:
      inputLabel[istate].setText("REGEX");
      break;
    case ISTATE_OPEN_SERIAL:
      inputLabel[istate].setText("OPEN SERIAL");
      break;
    default:
      this.istate = ISTATE_INPUT;
      inputLabel[istate].setVisible(false);
      break;
    }
    if (istate != ISTATE_INPUT) {
      inputLabel[istate].setVisible(true);
    }
    ((CardLayout)inputPanel.getLayout()).show(inputPanel, Integer.toString(istate));
    input[istate].requestFocus();
  }
  
  void leaveInputState(int istate) {
    switch (istate) {
    case ISTATE_FIND:
    case ISTATE_FIND_REGEX:
      lastFindIndex = -1;
      lastFindString = null;
      ftp.removeStyle(STYLE_ID_FIND);
      ftp.removeStyle(STYLE_ID_FIND_ALL);
      break;
    }
  }
  
  void process(String s) {
    ftp.addText(s + "\n", 1, colBashFg, null, false);
    bash[istate].input(s);
  }
  
  void actionInputEnter(boolean shift) {
    if (!input[istate].isForcedModel()) {
      input[istate].addSuggestion(input[istate].getText());
    }
    input[istate].resetLastSuggestionIndex();
    switch (istate) {
    case ISTATE_INPUT:
      String in = input[istate].getText(); 
      input[istate].setText("");
      if (in.startsWith(settings.string(Settings.BASH_PREFIX_STRING))) {
        process(in.substring(settings.string(Settings.BASH_PREFIX_STRING).length()));
      } else {
        ftp.addText(in + "\n", 1, colInputFg, null, false);
        serial.transmit(in + "\n");
      }
      break;
    case ISTATE_FIND:
    case ISTATE_FIND_REGEX:
      handleFind(input[istate].getText(), shift, istate == ISTATE_FIND_REGEX);
      break;
    case ISTATE_OPEN_SERIAL:
      handleOpenSerial(input[istate].getText());
      break;
    default:
      break;
    }
  }

  void actionFind(boolean shift, boolean regex) {
    if (istate != ISTATE_FIND && !regex) {
      Tuscedo.this.enterInputState(ISTATE_FIND);
    } else if (istate != ISTATE_FIND_REGEX && regex) {
      Tuscedo.this.enterInputState(ISTATE_FIND_REGEX);
    } else {
      handleFind(input[istate].getText(), shift, regex);
    }
  }
  
  void showHelp() {
    ftp.clear();
    String name = Essential.name + " v" + Essential.vMaj + "." + Essential.vMin + "." + Essential.vMic; 
    int midLen = Essential.longname.length() / 2;
    ftp.addText( String.format("%" + (midLen + name.length()/2) + "s\n", name), 
        1, Color.white, null, true);
    ftp.addText(Essential.longname + "\n\n", 1, Color.lightGray, null, false);
    Set<String> unsorted = KeyMap.getActions();
    List<String> sorted = new ArrayList<String>(unsorted);
    java.util.Collections.sort(sorted);
    for (String d : sorted) {
      KeyMap km = KeyMap.getKeyDef(d);
      ftp.addText(String.format("%" + (midLen-1) + "s  ", d), 2, Color.cyan, null, false);
      ftp.addText(km.toString() + "\n", 2, Color.yellow, null, false);
    }
  }
  
  void openSerialConfig() {
    String[] devices = serial.getDevices();
    List<String> model = new ArrayList<String>();
    int[] baudRates = {
        Port.BAUD_921600,
        Port.BAUD_115200,
        Port.BAUD_57600,
        Port.BAUD_38400,
        Port.BAUD_19200,
        Port.BAUD_14400,
        Port.BAUD_9600,
        Port.BAUD_460800,
        Port.BAUD_256000,
        Port.BAUD_230400,
        Port.BAUD_128000,
        Port.BAUD_4800,
        Port.BAUD_2400,
        Port.BAUD_1200,
        Port.BAUD_600,
        Port.BAUD_300,
        Port.BAUD_110,
    };
    int databits[] = {
        Port.BYTESIZE_8,
        Port.BYTESIZE_7,
        Port.BYTESIZE_6,
        Port.BYTESIZE_5,
    };
    String parities[] = {
        Port.PARITY_NONE_S.toLowerCase(),
        Port.PARITY_EVEN_S.toLowerCase(),
        Port.PARITY_ODD_S.toLowerCase(),
    };
    int stopbits[] = {
        Port.STOPBIT_ONE,
        Port.STOPBIT_TWO,
    };
    for (int d = 0; d < devices.length; d++) {
      String device = devices[devices.length - d - 1];
      for (int baud = 0; baud < baudRates.length; baud++) {
        for (int db = 0; db < databits.length; db++) {
          for (int p = 0; p < parities.length; p++) {
            for (int s = 0; s < stopbits.length; s++) {
              model.add(device + 
                  " " + PORT_ARG_BAUD + baudRates[baudRates.length - baud -1] + 
                  " " + PORT_ARG_DATABITS + databits[databits.length - db - 1] +
                  " " + PORT_ARG_PARITY + parities[parities.length - p - 1] + 
                  " " + PORT_ARG_STOPBITS + stopbits[stopbits.length -s -1] 
                      );
            }
          }
        }
      }
    }
    for (int d = 0; d < devices.length; d++) {
      String device = devices[devices.length - d - 1];
      for (int baud = 0; baud < baudRates.length; baud++) {
              model.add(device + 
                  " " + PORT_ARG_BAUD + baudRates[baudRates.length - baud -1]
                      );
      }
    }
    input[ISTATE_OPEN_SERIAL].setSuggestions(model);
    enterInputState(ISTATE_OPEN_SERIAL);
    
    if (prevSerialDevices == null) {
      if (devices != null) {
        setInfo(devices.length + " SERIALS");
      }
    } else if (devices != null) {
      List<String> added = new ArrayList<String>(Arrays.asList(devices));
      List<String> removed = new ArrayList<String>(Arrays.asList(prevSerialDevices));
    match: while (true) {
        for (int i = 0; i < added.size(); i++) {
          String ai = added.get(i);
          for (int j = 0; j  < removed.size(); j++) {
            if (ai.equals(removed.get(j))) {
              added.remove(ai);
              removed.remove(ai);
              continue match;
            }
          }
        }
        break match;
      }
      StringBuilder sb = new StringBuilder();
      if (added.isEmpty() && removed.isEmpty()) {
        setInfo(devices.length + " SERIALS");
      } else {
        for (String d : added) {
          sb.append("+" + d + " ");
        }
        for (String d : removed) {
          sb.append("-" + d + " ");
        }
        setInfo(sb.toString().trim());
      }
    }
    prevSerialDevices = devices;
  }
  
  AbstractAction actionOpenFind = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionFind(false, false);
    }
  };

  AbstractAction actionOpenFindBack = new AbstractAction() {
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

  AbstractAction actionOpenFindRegexBack = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionFind(true, true);
    }
  };

  AbstractAction actionInputClose = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (input[istate].getText().length() > 0 && !input[istate].isForcedModel()) {
        input[istate].setText("");
        input[istate].resetLastSuggestionIndex();
      } else {
        Tuscedo.this.enterInputState(ISTATE_INPUT);
      }
    }
  };

  AbstractAction actionInputEnter = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionInputEnter(false);
    }
  };

  AbstractAction actionInputEnterBack = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionInputEnter(true);
    }
  };

  AbstractAction actionOpenSerialConfig = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      openSerialConfig();
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

  AbstractAction actionSplitVeri = new AbstractAction() {
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

  AbstractAction actionSplitHori = new AbstractAction() {
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

  AbstractAction actionShowHelp = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      showHelp();
    }
  };

  AbstractAction actionLogPageUp = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      ftp.scrollPagesRelative(-1);
    }
  };

  AbstractAction actionLogPageDown = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      ftp.scrollPagesRelative(1);
    }
  };

  AbstractAction actionLogUp = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      ftp.scrollLinesRelative(-1);
    }
  };

  AbstractAction actionLogDown = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      ftp.scrollLinesRelative(1);
    }
  };

  AbstractAction actionLogLeft = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      ftp.scrollHorizontalRelative(-1);
    }
  };

  AbstractAction actionLogRight = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      ftp.scrollHorizontalRelative(1);
    }
  };

  AbstractAction actionLogHome = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      ftp.scrollToOffset(0);
    }
  };

  AbstractAction actionLogEnd = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      ftp.scrollToEnd();
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

  class AutoAdjustmentListener implements AdjustmentListener {
    private volatile int _val = 0;
    private volatile int _ext = 0;
    private volatile int _max = 0;
    private volatile boolean adjusting;
    private final BoundedRangeModel _model;
    
    public AutoAdjustmentListener(JScrollPane p) {
       _model = p.getVerticalScrollBar().getModel();
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
      if (adjusting) return;
      adjusting = true;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          // Get the new max
          int newMax = _model.getMaximum();
          // If the new max has changed and if we were scrolled to bottom
          if (newMax != _max && (_val + _ext == _max)) {
            // Scroll to bottom
            _model.setValue(newMax - _model.getExtent());
          }

          // Save the new values
          _val = _model.getValue();
          _ext = _model.getExtent();
          _max = newMax;
          adjusting = false;
        }
      });
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
