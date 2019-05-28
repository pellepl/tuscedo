package com.pelleplutt.tuscedo.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.text.*;

import com.pelleplutt.*;
import com.pelleplutt.operandi.proc.*;
import com.pelleplutt.operandi.proc.Processor.*;
import com.pelleplutt.tuscedo.*;
import com.pelleplutt.util.*;
import com.pelleplutt.util.AppSystem.*;
import com.pelleplutt.util.io.*;

/**
 * Major workarea.
 * This is a JPanel containing the viewPanel in center and the inputPanel at bottom.
 * viewPanel and inputPanel are card layouts.
 * inputPanels card layout holds ProcessACTextfields.
 * viewPanels card layout holds Views.
 * A View contains a FastTermPane and a FastTextPane. These are wrapped in 
 * scroll panes which are wrapped in two diffent split panes.
 *  
 * @author petera
 */
public class UIWorkArea extends JPanel implements Disposable, UIO {
  static final int ISTATE_INPUT = 0;
  static final int ISTATE_FIND = 1;
  static final int ISTATE_FIND_REGEX = 2;
  static final int ISTATE_OPEN_SERIAL = 3;
  static final int ISTATE_BASH = 4;
  static final int ISTATE_HEX = 5;
  static final int ISTATE_SCRIPT = 6;
  static final int _ISTATE_NUM = 7;
  
  public static final String PORT_ARG_BAUD = "baud:";
  public static final String PORT_ARG_DATABITS = "databits:";
  public static final String PORT_ARG_PARITY = "parity:";
  public static final String PORT_ARG_STOPBITS = "stopbits:";
  
  int istate = ISTATE_INPUT;
  JPanel viewPanel = new JPanel();
  JPanel inputPanel = new JPanel();
  View curView;
  
  View views[] = new View[_ISTATE_NUM];
  JLabel inputLabel[] = new JLabel[_ISTATE_NUM];
  JLabel infoLabel[] = new JLabel[_ISTATE_NUM];
  Bash bash[] = new Bash[_ISTATE_NUM];
  ProcessACTextField input[] = new ProcessACTextField[_ISTATE_NUM];
  JScrollPane winSugListSp;

  int lastFindIndex = -1;
  String lastFindString = null;
  List<OffsetSpan> findResult;
  Settings settings;
  Serial serial;
  OperandiScript script;
  StringBuilder lineBuffer = new StringBuilder();
  List<RxFilter> serialFilters = new ArrayList<RxFilter>();
  Map<String, SerialNotifierInfo> serialNotifiers = new HashMap<String, SerialNotifierInfo>();

  static String[] prevSerialDevices;
  
  static final int[] baudRates = {
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
      4000000,
      3500000,
      3000000,
      2500000,
      2000000,
      1500000,
      1000000,
      Port.BAUD_4800,
      Port.BAUD_2400,
      Port.BAUD_1200,
      Port.BAUD_600,
      Port.BAUD_300,
      Port.BAUD_110,
  };
  static final int databits[] = {
      Port.BYTESIZE_8,
      Port.BYTESIZE_7,
      Port.BYTESIZE_6,
      Port.BYTESIZE_5,
  };
  static final String parities[] = {
      Port.PARITY_NONE_S.toLowerCase(),
      Port.PARITY_EVEN_S.toLowerCase(),
      Port.PARITY_ODD_S.toLowerCase(),
  };
  static final int stopbits[] = {
      Port.STOPBIT_ONE,
      Port.STOPBIT_TWO,
  };
  
  static final String sugBaudRate[];
  static final String sugDataBits[];
  static final String sugParities[];
  static final String sugStopBits[];
  
  static {
    sugBaudRate = new String[baudRates.length];
    for (int i = 0; i < baudRates.length; i++) sugBaudRate[i] = PORT_ARG_BAUD + baudRates[baudRates.length - 1 - i];
    sugDataBits = new String[databits.length];
    for (int i = 0; i < databits.length; i++) sugDataBits[i] = PORT_ARG_DATABITS + databits[databits.length - 1 - i];
    sugParities = new String[parities.length];
    for (int i = 0; i < parities.length; i++) sugParities[i] = PORT_ARG_PARITY + parities[parities.length - 1 - i];
    sugStopBits = new String[stopbits.length];
    for (int i = 0; i < stopbits.length; i++) sugStopBits[i] = PORT_ARG_STOPBITS + stopbits[stopbits.length - 1 - i];
  }
  
  JWindow winSug;
  JList<String> winSugList;
  final UIInfo uiinfo;
  static int __id = 0;
  
  public UIInfo getUIInfo() {
    return uiinfo;
  }
  public void onClose() {}
  
  public UIWorkArea() {
    uiinfo = new UIInfo(this, "workarea" + __id, "");
    UIInfo.fireEventOnCreated(uiinfo);

    __id++;
    settings = Settings.inst();
    serial = new Serial(this);
    script = new OperandiScript();
    AppSystem.addDisposable(script);
    Tuscedo.inst().registerTickable(serial);
    script.runOperandiInitScripts(this);
  }
  
  public void decorateUI() {
    for (int i = 0; i < _ISTATE_NUM; i++) {
      UICommon.decorateTextEditor(input[i]);
      if (views[i] != null) {
        UICommon.decorateFTP(views[i].ftp);
        UICommon.decorateFTP(views[i].ftpSec);
        UICommon.decorateScrollPane(views[i].mainScrollPane);
        UICommon.decorateScrollPane(views[i].secScrollPane);
        UICommon.decorateSplitPane(views[i].splitHor);
        UICommon.decorateSplitPane(views[i].splitVer);
      }
      if (input[i] != null) {
        UICommon.decorateHiliteLabel(inputLabel[i]);
        UICommon.decorateHiliteLabel(infoLabel[i]);
      }
    }
    UICommon.decorateScrollPane(winSugListSp);
  }

  public void build() {
    setLayout(new BorderLayout());
    
    JPanel ip[] = new JPanel[_ISTATE_NUM];
    viewPanel.setLayout(new CardLayout(0,0));
    inputPanel.setLayout(new CardLayout(0,0));
    
    for (int i = 0; i < _ISTATE_NUM; i++) {
      final int istateNum = i;
      View view = null;
      if (i == ISTATE_INPUT || i == ISTATE_BASH) {
        view = new View();
      }
      input[i] = new ProcessACTextField(this);
      UICommon.decorateTextEditor(input[i]);
      input[i].setSuggestionListener(new ACTextField.SuggestionListener() {
        @Override
        public void gotSuggestions(List<String> suggestions) {
          onGotSuggestions(istateNum, suggestions);
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
          List<String> sugs = null;
          if (istateNum == ISTATE_OPEN_SERIAL) {
            sugs = giveOpenSerialSuggestions(userInput);
          } else {
            sugs = giveInputBashSuggestions(userInput, istateNum);
          }
          return sugs;
        }
      });
      final int curi = i;
      input[i].addMouseListener(new MouseAdapter() {
        String __str = null;
        int __ix;
        @Override
        public void mousePressed(MouseEvent e) {
          if (e.getButton() == MouseEvent.BUTTON2) {
            View view = getCurrentView();
            String s = null;
            if (view != null && view.ftp != null) {
              s = view.ftp.getSelectedText();
            }
            if (s != null) {
              int start_ix = input[curi].getCaretPosition();
              int end_ix = start_ix;
              String curs = input[curi].getText();
              if (input[curi].getSelectionStart() != end_ix) {
                start_ix = input[curi].getSelectionStart();
                end_ix = input[curi].getSelectionEnd();
              }
              try {
                __str = curs.substring(0, start_ix) + s + curs.substring(end_ix);
                __ix = start_ix + s.length();
                input[curi].setText(__str);
                input[curi].setCaretPosition(__ix);
                
              } catch (Exception ignore) {}
            } else {
              __str = null;
            }
          }
        }
        public void mouseClicked(MouseEvent e) {
          // super duper ugly work around for linux's middle button text paster
          // which is not accessible from java
          if (e.getButton() == MouseEvent.BUTTON2) {
            if (__str != null) {
              try {
                input[curi].setText(__str);
                input[curi].setCaretPosition(__ix);
              } catch (Exception ignore) {}
              __str = null;
            }
          }
        }
      });
      
      String histPath = System.getProperty("user.home") + File.separator + 
          Essential.userSettingPath + File.separator + Essential.historyFile + i;
      input[i].setupHistory(histPath, 4096, 1024); // TODO configurable
      UICommon.defineCommonActions(input[i], JComponent.WHEN_FOCUSED);
      UICommon.defineAction(input[i], "input.find", "ctrl+f", actionOpenFind);
      UICommon.defineAction(input[i], "input.findback", "ctrl+shift+f", actionOpenFindBack);
      UICommon.defineAction(input[i], "input.findregx", "alt+f", actionOpenFindRegex);
      UICommon.defineAction(input[i], "input.findregxback", "alt+shift+f", actionOpenFindRegexBack);
      UICommon.defineAction(input[i], "input.hex", "ctrl+h", actionOpenHex);
      UICommon.defineAction(input[i], "input.script", "ctrl+p", actionOpenScript);
      UICommon.defineAction(input[i], "input.inputclose", "escape", actionInputClose);
      UICommon.defineAction(input[i], "input.inputenter", "enter", actionInputEnter);
      UICommon.defineAction(input[i], "input.inputenterback", "shift+enter", actionInputEnterBack);
      UICommon.defineAction(input[i], "input.openserial", "ctrl+o", actionOpenSerialConfig);
      UICommon.defineAction(input[i], "input.closeserial", "ctrl+shift+o", actionCloseSerial);
      UICommon.defineAction(input[i], "input.help", "f1", actionShowHelp);
      UICommon.defineAction(input[i], "input.closetab", "ctrl+d", actionCloseTab);
      UICommon.defineAction(input[i], "input.bash", "ctrl+b", actionOpenBash);
      UICommon.defineAction(input[i], "input.complete", "ctrl+space", actionOpenCompletion);
      UICommon.defineAction(input[i], "log.input.split", "ctrl+w", actionSplit);
      UICommon.defineAction(input[i], "log.input.clear", "ctrl+l", actionClear);
      UICommon.defineAction(input[i], "log.input.pageup", "alt+page_up", actionLogPageUp);
      UICommon.defineAction(input[i], "log.input.pagedown", "alt+page_down", actionLogPageDown);
      UICommon.defineAction(input[i], "log.input.scrollup", "alt+up", actionLogUp);
      UICommon.defineAction(input[i], "log.input.scrolldown", "alt+down", actionLogDown);
      UICommon.defineAction(input[i], "log.input.scrollleft", "alt+left", actionLogLeft);
      UICommon.defineAction(input[i], "log.input.scrollright", "alt+right", actionLogRight);
      UICommon.defineAction(input[i], "log.input.home", "ctrl+home", actionLogHome);
      UICommon.defineAction(input[i], "log.input.end", "ctrl+end", actionLogEnd);
      UICommon.defineAction(input[i], "log.input.xtermtoggle", "ctrl+shift+x", actionLogXtermToggle);

      
      inputLabel[i] = new JLabel();
      UICommon.decorateHiliteLabel(inputLabel[i]);
      inputLabel[i].setVisible(false);

      infoLabel[i] = new JLabel();
      UICommon.decorateHiliteLabel(infoLabel[i]);
      infoLabel[i].setVisible(false);
      
      ip[i] = new JPanel(new BorderLayout());
      ip[i].add(inputLabel[i], BorderLayout.WEST);
      ip[i].add(input[i], BorderLayout.CENTER);
      ip[i].add(infoLabel[i], BorderLayout.EAST);
      
      if (view != null) {
        viewPanel.add(view, Integer.toString(i));
      }
      inputPanel.add(ip[i], Integer.toString(i));
      
      if (i == ISTATE_INPUT || i == ISTATE_BASH) {
        XtermTerminal xc = new XtermTerminal(view.ftp, input[i], "UTF-8");
        view.xterm = xc;
        bash[i] = new Bash(input[i], xc, serial, this);
      }
      
      views[i] = view;
    }
    
    input[ISTATE_OPEN_SERIAL].setForcedModel(true);
    
    add(viewPanel, BorderLayout.CENTER);
    add(inputPanel, BorderLayout.SOUTH);

    ((CardLayout)inputPanel.getLayout()).show(inputPanel, Integer.toString(ISTATE_INPUT));
    ((CardLayout)viewPanel.getLayout()).show(viewPanel, Integer.toString(ISTATE_INPUT));
    setStandardFocus();
    
    curView = views[ISTATE_INPUT];
    
    winSug = new JWindow(SwingUtilities.getWindowAncestor(input[ISTATE_INPUT]));
    winSug.setLayout(new BorderLayout());
    winSugList = new JList<String>();
    winSugList.setForeground(UICommon.colInputFg);
    winSugList.setBackground(UICommon.colInputBg);
    winSugList.setFont(UICommon.font);
    winSugList.setSelectionBackground(UICommon.colInputFg);
    winSugList.setSelectionForeground(UICommon.colInputBg);
    winSugListSp = new JScrollPane(winSugList, 
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    UICommon.decorateScrollPane(winSugListSp);
    //sp.setBorder(BorderFactory.createLineBorder(colTextFg, 1));
    winSug.add(winSugListSp, BorderLayout.CENTER);
  }

  public void setInfo(String s) {
    if (s != null && s.length() > 0) {
      infoLabel[istate].setText(s);
      infoLabel[istate].setVisible(true);
    } else {
      infoLabel[istate].setVisible(false);
    }
  }
  
  public void transmit(String in) {
    views[ISTATE_INPUT].ftp.addText(in, UICommon.STYLE_CONN_IN);
    serial.transmit(in);
  }
  
  public void transmit(byte[] b) {
    if (b != null && b.length > 0) {
      String s = AppSystem.formatBytes(b);
      views[ISTATE_INPUT].ftp.addText("[" + s + "]\n", UICommon.STYLE_CONN_IN);
      serial.transmit(b);
    }
  }
  
  public String getConnectionInfo() {
    Port portSetting = serial.getSerialConfig();
    if (portSetting != null) {
      return portSetting.portName + "@" + portSetting.baud + "/" + 
        portSetting.databits + Port.parityToString(portSetting.parity).charAt(0) + 
        portSetting.stopbits;
    } else {
      return "";
    }
  }
  
  String titleConn = "";
  String titlePwd = "";
  String titleCmd = "";
  public void updateTitle() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        titleConn = getConnectionInfo();
        File pwd = bash[istate] != null ? bash[istate].pwd : null;
        if (pwd != null) {
          titlePwd = bash[istate].pwd.getAbsolutePath();
          try {
            titlePwd = bash[istate].pwd.getCanonicalPath();
          } catch (IOException e) {
          }
          String userPath = System.getProperty("user.home");
          if (titlePwd.startsWith(userPath)) {
            titlePwd = "~" + titlePwd.substring(userPath.length());
          }
        }
        
        ProcessGroup pg = input[istate].getLinkedProcess();
        if (pg == null) {
          titleCmd = "";
        } else {
          titleCmd = pg.toString();
        }
        
        String title;
        title = titleConn.length() > 0 ? titleConn + "|" : ""; 
        title += titlePwd;
        title += titleCmd.length() > 0 ? ":" + titleCmd : "";
        UISimpleTabPane.Tab t = UISimpleTabPane.getTabByComponent(UIWorkArea.this);
        if (t != null) {
          if (istate == ISTATE_BASH) {
            title = "[BASH] " + title;
          }
          t.setText(title);
        }
      }
    });
  }

  protected void findAllOccurences(String regex, boolean isRegex) {
    String text = curView.ftp.getText();
    Pattern pat = Pattern.compile(isRegex ? regex : Pattern.quote(regex));
    Matcher mat = pat.matcher(text);
    findResult = new ArrayList<OffsetSpan>();
    while (mat.find()) {
      curView.ftp.addStyleByOffset(UICommon.STYLE_FIND_MARK, mat.start(), mat.end());
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
      curView.ftp.removeStyle(UICommon.STYLE_FIND);
      OffsetSpan offs = null;
      if (!str.equals(lastFindString)) {
        curView.ftp.removeStyle(UICommon.STYLE_FIND_MARK);
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
        curView.ftp.addStyleByOffset(UICommon.STYLE_FIND, offs.start, offs.end);
        curView.ftp.scrollToOffset(offs.start);
        setInfo((lastFindIndex + 1) + " OF " + findResult.size());
      }
    } catch (PatternSyntaxException pse) {
      setInfo("BAD REGEX");
    }
  }
  
  public boolean handleOpenSerial(String s) {
    boolean res = false;
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
      views[ISTATE_INPUT].ftp.addText("Opening " + portSetting.portName + " " + 
          portSetting.baud + " " + portSetting.databits + 
          Port.parityToString(portSetting.parity).charAt(0) + portSetting.stopbits + "...\n", 
          UICommon.STYLE_GENERIC_INFO);

      if (portSetting.portName.equals("stdio")) {
        serial.openStdin();
      } else {
        serial.open(portSetting);
      }
      
      views[ISTATE_INPUT].ftp.addText("Connected\n", UICommon.STYLE_GENERIC_INFO);
      if (istate == ISTATE_OPEN_SERIAL) enterInputState(ISTATE_INPUT);
      res=true;
    } catch (Exception e) {
      views[ISTATE_INPUT].ftp.addText("Failed [" + e.getMessage() + "]\n", UICommon.STYLE_GENERIC_ERR);
    } finally {
      input[ISTATE_OPEN_SERIAL].setEnabled(true);
      updateTitle();
    }
    return res;
  }

  public void enterInputState(int istate) {
    winSug.setVisible(false);
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
    case ISTATE_HEX:
      inputLabel[istate].setText("HEX");
      break;
    case ISTATE_OPEN_SERIAL:
      inputLabel[istate].setText("OPEN SERIAL");
      break;
    case ISTATE_BASH:
      inputLabel[istate].setText("BASH");
      break;
    case ISTATE_SCRIPT:
      inputLabel[istate].setText("SCRIPT");
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
    if (views[istate] != null) {
      ((CardLayout)viewPanel.getLayout()).show(viewPanel, Integer.toString(istate));
      curView = views[istate];
    }
    updateTitle();
    setStandardFocus();
  }
  
  void leaveInputState(int istate) {
    switch (istate) {
    case ISTATE_FIND:
    case ISTATE_FIND_REGEX:
      lastFindIndex = -1;
      lastFindString = null;
      curView.ftp.removeStyle(UICommon.STYLE_FIND);
      curView.ftp.removeStyle(UICommon.STYLE_FIND_MARK);
      break;
    }
  }
  
  public OperandiScript getScript() {
    return script;
  }
  
  void actionInputEnter(boolean shift) {
    if (winSug.isVisible()) {
      input[istate].setFilterNewLine(false);
      input[istate].setText(winSugList.getSelectedValue());
      input[istate].setFilterNewLine(true);
      winSug.setVisible(false);
      return;
    }
    String in = input[istate].getText(); 
    if (!input[istate].isForcedModel() && in.length() > 0 && !shift) {
      input[istate].addHistory(in);
    }
    input[istate].resetLastSuggestionIndex();
    switch (istate) {
    case ISTATE_INPUT:
      setInfo("");
      if (!shift) {
        input[istate].setText("");
        if (in.startsWith(settings.string(Settings.BASH_PREFIX_STRING))) {
          String s = in.substring(settings.string(Settings.BASH_PREFIX_STRING).length()); 
          views[ISTATE_INPUT].ftp.addText(s + "\n", UICommon.STYLE_BASH_INPUT);
          bash[istate].input(s);
        } else if (in.startsWith(settings.string(Settings.SCRIPT_PREFIX_STRING))) {
          String s = in.substring(settings.string(Settings.SCRIPT_PREFIX_STRING).length()); 
          getCurrentView().ftp.addText(s + "\n", UICommon.STYLE_BASH_INPUT);
          script.runScript(this, in + "\n");
        } else {
          transmit(in + Settings.inst().string(Settings.SERIAL_NEWLINE_STRING));
        }
      } else {
        input[istate].setFilterNewLine(false);
        try {
          input[istate].getDocument().insertString(input[istate].getCaretPosition(), "\n", null);
        } catch (BadLocationException e) {}
        input[istate].setFilterNewLine(true);
      }
      break;
    case ISTATE_FIND:
    case ISTATE_FIND_REGEX:
      handleFind(in, shift, istate == ISTATE_FIND_REGEX);
      break;
    case ISTATE_HEX:
    {
      byte[] b = AppSystem.parseBytes(in);
      transmit(b);
      break;
    }
    case ISTATE_OPEN_SERIAL:
      handleOpenSerial(in);
      break;
    case ISTATE_BASH:
      if (input[istate].isLinkedToProcess() && curView.ftp.isTerminalMode()) {
        // do not echo to running bashes
      } else {
        views[ISTATE_BASH].ftp.addText(in + "\n", UICommon.STYLE_BASH_INPUT);
      }
      bash[istate].input(in);
      input[istate].setText("");
      break;
    case ISTATE_SCRIPT:
      if (!shift) {
        if (script.isRunning()) {
          if (in.trim().length() > 0) {
            lastDbgCmd = in;
          } else {
            in = lastDbgCmd;
          }
          if (in.equalsIgnoreCase(OperandiScript.DBG_HALT) || in.equalsIgnoreCase(OperandiScript.DBG_HALT_SHORT)) {
            script.halt(true);
          } else if (in.equalsIgnoreCase(OperandiScript.DBG_CONT) || in.equalsIgnoreCase(OperandiScript.DBG_CONT_SHORT)) {
            script.halt(false);
          } else if (in.equalsIgnoreCase(OperandiScript.DBG_NEXT) || in.equalsIgnoreCase(OperandiScript.DBG_NEXT_SHORT)) {
            script.step();
          } else if (in.equalsIgnoreCase(OperandiScript.DBG_STEP) || in.equalsIgnoreCase(OperandiScript.DBG_STEP_SHORT)) {
            script.stepInstr();
          } else if (in.equalsIgnoreCase(OperandiScript.DBG_BACK) || in.equalsIgnoreCase(OperandiScript.DBG_BACK_SHORT)) {
            script.backtrace();
          } else if (in.equalsIgnoreCase(OperandiScript.DBG_STACK) || in.equalsIgnoreCase(OperandiScript.DBG_STACK_SHORT)) {
            script.dumpStack();
          } else if (in.startsWith(OperandiScript.DBG_INT + " ") || in.startsWith(OperandiScript.DBG_INT_SHORT + " ")) {
            script.interrupt(script.lookupFunc(in.split("\\s")[1]));
          } else if (in.equalsIgnoreCase(OperandiScript.DBG_V_PC)) {
            script.dumpPC();
          } else if (in.equalsIgnoreCase(OperandiScript.DBG_V_FP)) {
            script.dumpFP();
          } else if (in.equalsIgnoreCase(OperandiScript.DBG_V_SP)) {
            script.dumpSP();
          } else if (in.equalsIgnoreCase(OperandiScript.DBG_V_ME)) {
            script.dumpMe();
          } else if (in.equalsIgnoreCase(OperandiScript.DBG_V_SR)) {
            script.dumpSR();
          } else if (in.equalsIgnoreCase(OperandiScript.DBG_RES) || in.equalsIgnoreCase(OperandiScript.DBG_RES_SHORT)) {
            script.reset();
          } else {
            script.dumpDbgHelp();
          }
        } else {
          if (in.equals("#init")) {
            script.runOperandiInitScripts(this);
          } else {
            getCurrentView().ftp.addText(in + "\n", UICommon.STYLE_BASH_INPUT);
            script.runScript(this, in + "\n");
          }
        }
        input[istate].setText("");
      } else {
        // shift + enter
        input[istate].setFilterNewLine(false);
        try {
          input[istate].getDocument().insertString(input[istate].getCaretPosition(), "\n", null);
        } catch (BadLocationException e) {}
        input[istate].setFilterNewLine(true);
      }
      break;
    default:
      break;
    }
  }
  String lastDbgCmd = "";
  
  public View getCurrentView() {
    return curView;
  }
  
  public View getSerialView() {
    return views[ISTATE_INPUT];
  }
  
  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_BLACK = "\u001B[30m";
  public static final String ANSI_RED = "\u001B[31;1m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String ANSI_BLUE = "\u001B[34m";
  public static final String ANSI_PURPLE = "\u001B[35m";
  public static final String ANSI_CYAN = "\u001B[36m";
  public static final String ANSI_WHITE = "\u001B[37;1m";
  public void appendViewText(View view, String text, FastTextPane.Style style) {
    if (getParent() == null) {
      if (!Tuscedo.noterm) {
        if (style == UICommon.STYLE_OP_OUT) {
          System.out.print(ANSI_WHITE);
        }
        else if (style == UICommon.STYLE_OP_ERR) {
          System.out.print(ANSI_RED);
        }
        else if (style == UICommon.STYLE_OP_DBG) {
          System.out.print(ANSI_CYAN);
        }
        else {
          System.out.print(ANSI_RESET);
        }
      }
      System.out.print(text);
      if (!Tuscedo.noterm) System.out.print(ANSI_RESET);
    } else {
      view.ftp.addText(text, style);
    }
  }
  
  String serialMatch(final String prefix, final String in, final String[] defs, 
      final List<String> suggestions) {
    // get defined part of the input, incomplete or not
    int nextSpace = in.indexOf(' ');
    String defInput;
    if (nextSpace < 0) {
      defInput = in;
    } else {
      defInput = in.substring(0, nextSpace);
    }
    
    // find exact match or submatches among defs
    boolean match = false;
    for (String dev : defs) {
      if (defInput.equals(dev)) {
        match = true;
      } else if (dev.startsWith(defInput)) {
        suggestions.add(prefix + dev);
      }
    }
    return match ? defInput : null;
  }
  
  List<String> giveOpenSerialSuggestions(final String input) {
    List<String> suggestions = new ArrayList<String>();
    String restInput = input;
    String match;
    String pre = "";
    
    // get device part of the input
    match = serialMatch(pre, restInput, prevSerialDevices, suggestions);
    if (match == null) {
      return suggestions.isEmpty() ? null : suggestions;
    }
    restInput = restInput.substring(match.length()).trim();
    pre = pre + match + " ";

    // get baudrate part of the input
    match = serialMatch(pre, restInput, sugBaudRate, suggestions);
    if (match == null) {
      return suggestions.isEmpty() ? null : suggestions;
    }
    restInput = restInput.substring(match.length()).trim();
    pre = pre + match + " ";
    
    // get databit part of the input
    match = serialMatch(pre, restInput, sugDataBits, suggestions);
    if (match == null) {
      return suggestions.isEmpty() ? null : suggestions;
    }
    restInput = restInput.substring(match.length()).trim();
    pre = pre + match + " ";
    
    // get parity part of the input
    match = serialMatch(pre, restInput, sugParities, suggestions);
    if (match == null) {
      return suggestions.isEmpty() ? null : suggestions;
    }
    restInput = restInput.substring(match.length()).trim();
    pre = pre + match + " ";
    
    // get stopbit part of the input
    match = serialMatch(pre, "", sugStopBits, suggestions);
    return suggestions.isEmpty() ? null : suggestions;
  }
  
  List<String> giveInputBashSuggestions(final String userInput, final int istateNum) {
    if (istate != ISTATE_BASH && istate != ISTATE_INPUT) return null;
    final String bashPrefix = istate != ISTATE_BASH ? 
        settings.string(Settings.BASH_PREFIX_STRING) :
          "";
    if (userInput.startsWith(settings.string(bashPrefix)) && 
        userInput.length() >= bashPrefix.length()) {
      if (userInput.startsWith(bashPrefix + "cd ")) {
        return bash[istate].suggestFileSystemCompletions(bashPrefix, 
            userInput.substring(bashPrefix.length()), 
            "cd", false, true);
      }
      else if (userInput.startsWith(bashPrefix)) {
        int spaceIx = userInput.lastIndexOf(' ');
        if (spaceIx > 0) {
          return bash[istate].suggestFileSystemCompletions(bashPrefix, 
              userInput.substring(bashPrefix.length()), 
              userInput.substring(bashPrefix.length(), spaceIx).trim(), true, true);
        }
      }
    }
    return null;
  }

  void actionFind(boolean shift, boolean regex) {
    if (istate != ISTATE_FIND && !regex) {
      UIWorkArea.this.enterInputState(ISTATE_FIND);
    } else if (istate != ISTATE_FIND_REGEX && regex) {
      UIWorkArea.this.enterInputState(ISTATE_FIND_REGEX);
    } else {
      handleFind(input[istate].getText(), shift, regex);
    }
  }
  
  void actionHex(boolean shift) {
    if (istate != ISTATE_HEX) {
      UIWorkArea.this.enterInputState(ISTATE_HEX);
    }
  }
  
  void actionScript(boolean shift) {
    if (istate != ISTATE_HEX) {
      UIWorkArea.this.enterInputState(ISTATE_SCRIPT);
    }
  }
  
  void actionBash() {
    UIWorkArea.this.enterInputState(ISTATE_BASH);
  }
  
  
  public void onGotSuggestions(final int istateNum, List<String> suggestions) {
    if (winSug.isVisible()) {
      updateSuggestionWindow(suggestions);
    }
  }

  void actionOpenCompletion() {
    List<String> s = input[istate].getCurrentSuggestions();
    if (s == null || s.isEmpty()) {
      return;
    }
    if (s.size() == 1) {
      input[istate].setFilterNewLine(false);
      input[istate].setText(s.get(0));
      input[istate].setFilterNewLine(true);
      return;
    }
    String[] arr = s.toArray(new String[s.size()]);
    Window w = SwingUtilities.getWindowAncestor(input[istate]);
    Point p = w.getLocation();
    Point p2 = SwingUtilities.convertPoint(input[istate], 0,0, w);
    int fh = getFontMetrics(UICommon.font).getHeight()+2;
    int h = fh * Math.min(6, arr.length);
    p.x += p2.x;
    p.y += p2.y;
    p.y -= h;
    showSuggestionWindow(p.x, p.y, input[istate].getWidth(), h, arr);
  }
  
  void showSuggestionWindow(int x, int y, int w, int h, String items[]) {
    DefaultListModel<String> lm = new DefaultListModel<String>();
    for (String item : items) {
      lm.addElement(item);
    }
    winSugList.setModel(lm);
    winSugList.setSelectedIndex(items.length-1);
    winSugList.ensureIndexIsVisible(items.length-1);
    winSug.setSize(w, h);
    winSug.setLocation(x, y);
    winSug.setVisible(true);
  }
  
  void updateSuggestionWindow(List<String> items) {
    if (items == null) return;
    if (items.size() == 1) {
      winSug.setVisible(false);
      return;
    }
    DefaultListModel<String> lm = new DefaultListModel<String>();
    for (String item : items) {
      lm.addElement(item);
    }
    winSugList.setModel(lm);
    winSugList.setSelectedIndex(items.size()-1);
    winSugList.ensureIndexIsVisible(items.size()-1);
    Window w = SwingUtilities.getWindowAncestor(input[istate]);
    Point p = w.getLocation();
    Point p2 = SwingUtilities.convertPoint(input[istate], 0,0, w);
    int fh = getFontMetrics(UICommon.font).getHeight()+2;
    int h = fh * Math.min(6, items.size());
    p.x += p2.x;
    p.y += p2.y;
    p.y -= h;
    winSug.setLocation(p);
    winSug.setSize(input[istate].getWidth(), h);
  }
  
  void showHelp() {
    // TODO this should be its own view
    curView.ftp.clear();
    String name = Essential.name + " v" + Essential.vMaj + "." +
        Essential.vMin + "." + Essential.vMic; 
    int midLen = Essential.longname.length() / 2;
    curView.ftp.addText( String.format("%" + (midLen + name.length()/2) + "s\n", name), 
        UICommon.STYLE_ID_HELP, Color.white, null, true);
    curView.ftp.addText(Essential.longname + "\n\n", UICommon.STYLE_ID_HELP, 
        Color.lightGray, null, false);
    Set<String> unsorted = KeyMap.getActions();
    List<String> sorted = new ArrayList<String>(unsorted);
    java.util.Collections.sort(sorted);
    for (String d : sorted) {
      KeyMap km = KeyMap.getKeyDef(d);
      curView.ftp.addText(String.format("%" + (midLen-1) + "s  ", d), UICommon.STYLE_ID_HELP, 
          Color.cyan, null, false);
      curView.ftp.addText(km.toString() + "\n", UICommon.STYLE_ID_HELP, Color.yellow, null, false);
    }
  }
  
  void closeTab() {
    if (input[istate].getText().equals("")) {
      UISimpleTabPane.Tab t = UISimpleTabPane.getTabByComponent(this); 
      UISimpleTabPane stp = t.getPane();
      stp.removeTab(t);
    }
  }
  
  void selectTab(int ix) {
    UISimpleTabPane.Tab t = UISimpleTabPane.getTabByComponent(this); 
    UISimpleTabPane stp = t.getPane();
    stp.selectTab(ix);
  }
  
  void openSerialConfig() {
    String[] prevDev = prevSerialDevices;
    String[] devices = serial.getDevices();
    prevSerialDevices = devices;

    List<String> model = new ArrayList<String>();
    for (int d = 0; d < devices.length; d++) {
      String device = devices[devices.length - d - 1];
      model.add(device); 
    }
    input[ISTATE_OPEN_SERIAL].setSuggestions(model);
    enterInputState(ISTATE_OPEN_SERIAL);
    
    if (prevDev == null) {
      if (devices != null) {
        setInfo((devices.length == 0 ? "NO" : devices.length) + 
            " SERIAL" + (devices.length != 1 ? "S" : ""));
      }
    } else if (devices != null) {
      List<String> added = new ArrayList<String>(Arrays.asList(devices));
      List<String> removed = new ArrayList<String>(Arrays.asList(prevDev));
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
        setInfo((devices.length == 0 ? "NO" : devices.length) + 
            " SERIAL" + (devices.length != 1 ? "S" : ""));
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
  }
  
  public void closeSerial() {
    serial.closeSerial();
  }
  
  AbstractAction actionOpenFind = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionFind(false, false);
    }
  };

  AbstractAction actionOpenBash = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionBash();
    }
  };

  AbstractAction actionOpenHex = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionHex(false);
    }
  };

  AbstractAction actionOpenScript = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionScript(false);
    }
  };

  AbstractAction actionOpenCompletion = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionOpenCompletion();
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
        UIWorkArea.this.enterInputState(ISTATE_INPUT);
      }
      onKeyEsc(e);
    }
  };

  AbstractAction actionInputEnter = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      actionInputEnter(false);
      onKeyEnter(e);
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

  AbstractAction actionCloseSerial= new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      closeSerial();
    }
  };

  AbstractAction actionSplit = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      curView.cycleSplit();
    }
  };

  AbstractAction actionClear = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      curView.ftp.clear();
    }
  };

  AbstractAction actionShowHelp = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      showHelp();
    }
  };

  AbstractAction actionCloseTab = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      closeTab();
    }
  };

  AbstractAction actionLogPageUp = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      curView.ftp.scrollPagesRelative(-1);
    }
  };

  AbstractAction actionLogPageDown = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      curView.ftp.scrollPagesRelative(1);
    }
  };

  AbstractAction actionLogUp = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      curView.ftp.scrollLinesRelative(-1);
    }
  };

  AbstractAction actionLogDown = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      curView.ftp.scrollLinesRelative(1);
    }
  };

  AbstractAction actionLogLeft = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      curView.ftp.scrollHorizontalRelative(-1);
    }
  };

  AbstractAction actionLogRight = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      curView.ftp.scrollHorizontalRelative(1);
    }
  };

  AbstractAction actionLogHome = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      curView.ftp.scrollToOffset(0);
    }
  };

  AbstractAction actionLogEnd = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      curView.ftp.scrollToEnd();
    }
  };

  AbstractAction actionLogXtermToggle = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      views[ISTATE_INPUT].rawMode = !views[ISTATE_INPUT].rawMode;
      setInfo("XTERM " + (views[ISTATE_INPUT].rawMode ? "OFF":"ON"));
    }
  };

  @Override
  public void dispose() {
    //Log.println("workarea close serial");
    serial.closeSerial();
    Tuscedo.inst().deregisterTickable(serial);
    AppSystem.dispose(script);
    winSug.dispose();
    for (Bash b : bash) {
      //Log.println("workarea close bash " + b);
      if (b != null) {
        b.close();
      }
    }
  }

  public void onSerialData(byte[] b) {
    String s = new String(b);
    if (views[ISTATE_INPUT].rawMode) {
      views[ISTATE_INPUT].ftp.addText(s);
    } else {
      views[ISTATE_INPUT].xterm.stdout(b, b.length);
    }
    final UISimpleTabPane.Tab t = UISimpleTabPane.getTabByComponent(UIWorkArea.this);
    if (t != null && t.isNotified() != 1) {
      t.markNotified(1);
      Tuscedo.inst().getTimer().addTask(new Runnable() {
        @Override
        public void run() {
          t.markNotified(2);
        }
      }, 1000);
    }
    int nlix;
    processSerialNotifiers(b);
    do {
      nlix = s.indexOf('\n');
      if (nlix >= 0) {
        lineBuffer.append(s.substring(0, nlix));
        s = s.substring(nlix+1);
        handleSerialLine(lineBuffer.toString());
        lineBuffer.delete(0, lineBuffer.length());
      } else {
        lineBuffer.append(s);
      }
    } while (nlix >= 0);
  }
  
  public void handleSerialLine(String s) {
    for (RxFilter f : serialFilters) {
      Matcher m = f.pattern.matcher(s);
      if (m.find()) {
        List<M> umargs = new ArrayList<M>();
        umargs.add(new M(s));
        umargs.add(new M(f.filter));
        do {
          int groups = m.groupCount();
          for (int g = 1; g <= groups; g++) {
            umargs.add(new M(m.group(g)));
          }
          
        } while (m.find());
        script.runFunc(this, f.addr, umargs);
      }
    }
  }
  
  void processSerialNotifiers(byte[] b) {
    if (serialNotifiers.isEmpty()) return;
    for (byte d : b) {
      for (SerialNotifierInfo sni : serialNotifiers.values()) {
        if (sni.index < sni.filter.length) {
          if (sni.filter[sni.index] == d) {
            sni.index++;
            if (sni.index >= sni.filter.length) {
              sni.index = 0;
              sni.sn.call();
            }
          } else {
            sni.index = 0;
          }
        } else {
          sni.index = 0;
        }
      } // per serial notifier
    } // per byte
  }
  
  public void setSerialNotifier(String filter, SerialNotifier sn) {
    if (sn == null) {
      removeSerialNotifier(filter);
    } else {
      SerialNotifierInfo sni = new SerialNotifierInfo();
      sni.filter = filter.getBytes();
      sni.sn = sn;
      serialNotifiers.put(filter, sni);
    }
  }
  
  public void removeSerialNotifier(String filter) {
    serialNotifiers.remove(filter);
  }
  
  public void addSerialFilter(String filter, int address) {
    try {
      Pattern.compile(filter);
    } catch (PatternSyntaxException exception) {
      throw new RuntimeException(exception);
    }
    RxFilter f = new RxFilter();
    f.filter = filter;
    f.addr = address;
    f.pattern = Pattern.compile(filter);
    removeSerialFilter(filter);
    serialFilters.add(f);
  }
  
  public void removeSerialFilter(String filter) {
    for (RxFilter ef : serialFilters) {
      if (ef.filter.equals(filter)) {
        serialFilters.remove(ef);
        break;
      }
    }
  }
  
  public List<RxFilter> getSerialFilters() {
    return serialFilters;
  }
  
  public void onSerialDisconnect() {
    lineBuffer = new StringBuilder();
    views[ISTATE_INPUT].ftp.addText("Disconnected\n", UICommon.STYLE_GENERIC_INFO);
    updateTitle();
  }
  

  public void onLinkedProcess(ProcessACTextField tf, ProcessGroup process) {
    Log.println("link process " + process.toString());
    tf.setBackground(UICommon.colInputBashBg);
    Bash bash = ((ProcessGroupInfo)process.getUserData()).bash;
    ((XtermTerminal)bash.console).reviveScreenBuffer(process);
    updateTitle();
  }
  
  public void onUnlinkedProcess(ProcessACTextField tf, ProcessGroup process) {
    if (process != null) {
      Log.println("unlink process " + process.toString());
      Bash bash = ((ProcessGroupInfo)process.getUserData()).bash;
      ((XtermTerminal)bash.console).forceOriginalScreenBuffer();
    }
    tf.setBackground(UICommon.colInputBg);
    updateTitle();
  }
  
  public void onScriptStart(Processor proc) {
    if (input != null && input[ISTATE_SCRIPT] != null) 
      input[ISTATE_SCRIPT].setBackground(UICommon.colInputBashBg);
  }

  public void onScriptStop(Processor proc) {
    if (input != null && input[ISTATE_SCRIPT] != null) 
      input[ISTATE_SCRIPT].setBackground(UICommon.colInputBg);
  }

  
  public void onTabSelected(UISimpleTabPane.Tab t) {
    setStandardFocus();
  }
  
  // these functions intercept the key events from autocompletetextfield
  // return true to pass the event back to the textfield
  
  public boolean onKeyUp(ActionEvent e) {
    if (winSug.isVisible()) {
      winSugList.setSelectedIndex(winSugList.getSelectedIndex()-1);
      winSugList.ensureIndexIsVisible(winSugList.getSelectedIndex());
      return false;
    } else if (istate == ISTATE_SCRIPT && input[istate].getText().contains("\n")) {
      input[istate].generateOriginalUpKeyPress(e);
      return false;
    }
    return true;
  }
  
  public boolean onKeyDown(ActionEvent e) {
    if (winSug.isVisible()) {
      winSugList.setSelectedIndex(winSugList.getSelectedIndex()+1);
      winSugList.ensureIndexIsVisible(winSugList.getSelectedIndex());
      return false;
    } else if (istate == ISTATE_SCRIPT && input[istate].getText().contains("\n")) {
      input[istate].generateOriginalDownKeyPress(e);
      return false;
    }
    return true;
  }
  
  public boolean onKeyEnter(ActionEvent e) {
    if (winSug.isVisible()) {
      ((ProcessACTextField)e.getSource()).setFilterNewLine(false);
      ((ProcessACTextField)e.getSource()).setText(winSugList.getSelectedValue());
      ((ProcessACTextField)e.getSource()).setFilterNewLine(true);
      winSug.setVisible(false);
      return false;
    }
    return true;
  }
  
  public boolean onKeyTab(ActionEvent e) {
//    if (winSug.isVisible()) {
//      winSug.setVisible(false);
//      return false;
//    }
    return true;
  }
  
  public boolean onKeyEsc(ActionEvent e) {
    if (winSug.isVisible()) {
      winSug.setVisible(false);
      return false;
    }
    return true;
  }
  
  
  public class OffsetSpan {
    public int start, end;
    public OffsetSpan(int start, int end) {
      this.start = start;
      this.end = end;
    }
  } // class OffsetSpan

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
  } // class AutoAdjustmentListener

  public class View extends JPanel implements MouseListener {
    public XtermTerminal xterm;
    FastTermPane ftp;
    FastTextPane ftpSec;
    JSplitPane splitVer, splitHor;
    JScrollPane mainScrollPane;
    JScrollPane secScrollPane;
    JComponent curSplit;
    boolean rawMode = true;
    
    public View() {
      ftp = new FastTermPane();
      UICommon.decorateFTP(ftp);
      
      ftpSec = new FastTextPane();
      ftpSec.setDocument(ftp.getDocument());
      
      ftp.addMouseListener(this);
      ftpSec.addMouseListener(this);
      
      UICommon.decorateFTP(ftpSec);
      mainScrollPane = new JScrollPane(ftp,
          JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      UICommon.decorateScrollPane(mainScrollPane);
      secScrollPane = new JScrollPane(ftpSec,
          JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      UICommon.decorateScrollPane(secScrollPane);

      mainScrollPane.getVerticalScrollBar().addAdjustmentListener(
          new AutoAdjustmentListener(mainScrollPane));
      secScrollPane.getVerticalScrollBar().addAdjustmentListener(
          new AutoAdjustmentListener(secScrollPane));
      
      splitVer = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
      UICommon.decorateSplitPane(splitVer);
      splitVer.setDividerLocation(0);
      splitHor = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      UICommon.decorateSplitPane(splitHor);
      splitHor.setDividerLocation(100);
      
      curSplit = mainScrollPane;
      
      setLayout(new BorderLayout());
      add(mainScrollPane, BorderLayout.CENTER);
    }
    
    public String getText() {
      return ftp.getText();
    }
    
    /** Returns whole log if none selected */
    public String getSelectedText() {
      String s = ftp.getSelectedText();
      if (s == null) s = ftp.getText();
      return s;
    }
    
    public void cycleSplit() {
      if (curSplit == splitVer) {
        remove(curSplit);
        add(mainScrollPane, BorderLayout.CENTER);
        revalidate();
        curSplit = mainScrollPane;
      } else if (curSplit == splitHor) {
        remove(curSplit);
        splitVer.setTopComponent(secScrollPane);
        splitVer.setBottomComponent(mainScrollPane);
        add(splitVer, BorderLayout.CENTER);
        revalidate();
        splitVer.setDividerLocation(mainScrollPane.getHeight()/2);
        curSplit = splitVer;
      } else {
        remove(curSplit);
        splitHor.setLeftComponent(mainScrollPane);
        splitHor.setRightComponent(secScrollPane);
        add(splitHor, BorderLayout.CENTER);
        revalidate();
        splitHor.setDividerLocation(mainScrollPane.getWidth()/2);
        curSplit = splitHor;
      }
    }

    
    // impl MouseListener for ftps
    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if (SwingUtilities.isRightMouseButton(e)) {
        // TODO menuize
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

  } // class View
  
  public static class RxFilter {
    public Pattern pattern;
    public String filter;
    public int addr;
  }

  public void setStandardFocus() {
    input[istate].requestFocus();
  }

  public Serial getSerial() {
    return serial;
  }

  public void registerSerialFilter(String filter, int operandiAddress) {
    addSerialFilter(filter, operandiAddress);
  }
  public void clearSerialFilters() {
    serialFilters.clear();
  }
  
  class SerialNotifierInfo {
    SerialNotifier sn;
    byte[] filter;
    int index;
  }
}
