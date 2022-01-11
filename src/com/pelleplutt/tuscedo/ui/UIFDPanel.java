package com.pelleplutt.tuscedo.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

public class UIFDPanel extends JPanel implements UIO, UIWorkArea.SerialLineListener {
  static final int LOCAL = 0;
  static final int REMOTE = 1;

  final UIInfo uiinfo;
  static int __id = 0;
  final UIWorkArea workarea;

  static final int HISTORY_LEN = 50;

  int adcHistIx = 0;

  History histAdcUp = new History(HISTORY_LEN);
  History histAdcDown = new History(HISTORY_LEN);

  // ultrasonix

  UIGraphPanel graphUSADC;
  UIGraphPanel.SampleSet graphADCDataUp;
  UIGraphPanel.SampleSet graphADCDataDown;
  volatile boolean paused = false;
  JButton butAdcHistPrev, butAdcHistNext;
  JLabel adcHist;


  class USReceiver {
    UIGraphPanel graph;
    UIGraphPanel.SampleSet up;
    UIGraphPanel.SampleSet down;
    public USReceiver(String name) {
      graph = new UIGraphPanel(name);
      up = graph.newSampleSet();
      down = graph.getSampleSet(0);
      up.getUIInfo().setName(name + " up");
      down.getUIInfo().setName(name + " down");
    }
  }
  class USStick {
    USReceiver[] quality; // local/remote quality readings
    USReceiver[] distance; // local/remote distance readings (in samples)
    public USStick(int stick) {
      quality = new USReceiver[2];
      distance = new USReceiver[2];
      quality[LOCAL] = new USReceiver("usQ" + stick + " LOC");
      quality[REMOTE] = new USReceiver("usQ" + stick + " REM");
      distance[LOCAL] = new USReceiver("usD" + stick + " LOC");
      distance[REMOTE] = new USReceiver("usD" + stick + " REM");
    }
  };

  USStick[] usStick = new USStick[2];

  // positioning
  class PosStick {
    UIGraphPanel graph;
    UIGraphPanel.SampleSet x,y,z;
  }
  PosStick[] posStick = new PosStick[2];



  public UIInfo getUIInfo() {
    return uiinfo;
  }
  public void onClose() {
    workarea.removeSerialLineListener(this);
  }

  void transmit(String s) {
    workarea.transmit(s);
  }

  void buildUIButtons() {
    JButton b;
    JPanel buttons = new JPanel();
    UICommon.decorateComponent(buttons);

    b = new JButton(new AbstractAction("RESET") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        transmit("reset\n");
      }
    });
    UICommon.decorateJButton(b);
    buttons.add(b);

    b = new JButton(new AbstractAction("ADC") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        transmit("\nproto_adc_dump 1\n");
      }
    });
    UICommon.decorateJButton(b);
    buttons.add(b);

    b = new JButton(new AbstractAction("PAUSE") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        if (!paused) {
          paused = true;
          ((JButton)arg0.getSource()).setText("START");
        } else {
          paused = false;
          ((JButton)arg0.getSource()).setText("PAUSE");
        }
      }
    });
    UICommon.decorateJButton(b);
    buttons.add(b);

    b = new JButton(new AbstractAction("CLEAR") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        graphUSADC.clear();
        for (int i = 0; i < 2; i ++) {
          posStick[i].graph.clear();
          usStick[i].quality[LOCAL].graph.clear();
          usStick[i].quality[REMOTE].graph.clear();
          usStick[i].distance[LOCAL].graph.clear();
          usStick[i].distance[REMOTE].graph.clear();
        }
        histAdcUp.clear();
        histAdcDown.clear();
        adcHistIx = 0;
        adcUpdateGraphHistory();
        graphUSADC.repaint();
      }
    });
    UICommon.decorateJButton(b);
    buttons.add(b);

    butAdcHistPrev = new JButton(new AbstractAction("< ADC") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        if (adcHistIx < HISTORY_LEN-1) {
          adcHistIx++;
        }
        adcUpdateGraphHistory();
      }
    });
    UICommon.decorateJButton(butAdcHistPrev);
    buttons.add(butAdcHistPrev);

    adcHist = new JLabel();
    UICommon.decorateComponent(adcHist);
    buttons.add(adcHist);

    butAdcHistNext = new JButton(new AbstractAction("ADC >") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        if (adcHistIx > 0) {
          adcHistIx--;
        }
        adcUpdateGraphHistory();
      }
    });
    butAdcHistNext.setEnabled(false);
    UICommon.decorateJButton(butAdcHistNext);
    buttons.add(butAdcHistNext);

    add(buttons, BorderLayout.SOUTH);
  }

  void buildUIGraphs() {
    Border borderA = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4), BorderFactory.createLineBorder(UICommon.colDivMain));
    Border borderB = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2), BorderFactory.createLineBorder(UICommon.colDivSec));

    for (int stick = 0; stick < 2; stick++) {
      posStick[stick] = new PosStick();
      posStick[stick].graph = new UIGraphPanel("Pos");
      posStick[stick].graph.setBorder(borderA);
      posStick[stick].y = posStick[stick].graph.newSampleSet();
      posStick[stick].x = posStick[stick].graph.newSampleSet();
      posStick[stick].z = posStick[stick].graph.getSampleSet(0);
      posStick[stick].x.getUIInfo().setName("X");
      posStick[stick].x.setColor(0x88ffff);
      posStick[stick].y.getUIInfo().setName("Y");
      posStick[stick].y.setColor(0x444444);
      posStick[stick].z.getUIInfo().setName("Z");
      posStick[stick].z.setColor(0xff8888);
      posStick[stick].graph.zoom(3, 0.01);
      posStick[stick].graph.setResizeStep(200);

    }

    graphUSADC = new UIGraphPanel("ADC");
    graphUSADC.setBorder(borderA);
    graphADCDataUp = graphUSADC.newSampleSet();
    graphADCDataDown = graphUSADC.getSampleSet(0);
    graphADCDataUp.getUIInfo().setName("ADC up");
    graphADCDataDown.getUIInfo().setName("ADC down");
    graphADCDataUp.setColor(0xffff88);
    graphADCDataDown.setColor(0xff88ff);

    JPanel[] usRawPanels = new JPanel[4];
    for (int stick = 0; stick < 2; stick++) {
      usRawPanels[stick] = new JPanel(new GridLayout(4,1));
      UICommon.decorateComponent(usRawPanels[stick]);
      usRawPanels[stick].setBorder(borderA);
      usStick[stick] = new USStick(stick+1);
      int sensors[] = {REMOTE, LOCAL};
      for (int sensor : sensors) {
        usStick[stick].quality[sensor].graph.setBorder(borderB);
        usStick[stick].quality[sensor].graph.setResizeStep(200);
        usStick[stick].quality[sensor].up.setColor(sensor == LOCAL ? 0xffff00 : 0x00ff00);
        usStick[stick].quality[sensor].down.setColor(sensor == LOCAL ? 0xff00ff : 0x00ffff);
        usStick[stick].quality[sensor].graph.zoom(2, 100);
        usRawPanels[stick].add(usStick[stick].quality[sensor].graph);
        usStick[stick].distance[sensor].graph.setResizeStep(200);
        usStick[stick].distance[sensor].graph.setBorder(borderB);
        usStick[stick].distance[sensor].up.setColor(sensor == LOCAL ? 0xffff88 : 0x88ff88);
        usStick[stick].distance[sensor].down.setColor(sensor == LOCAL ? 0xff88ff : 0x88ffff);
        usStick[stick].distance[sensor].graph.zoom(2, 0.1);
        usRawPanels[stick].add(usStick[stick].distance[sensor].graph);
      }
      UIGraphPanel graphsPerStick[] = {
        usStick[stick].quality[REMOTE].graph, usStick[stick].distance[REMOTE].graph, usStick[stick].quality[LOCAL].graph, usStick[stick].distance[LOCAL].graph};
      for (int i = 0; i < graphsPerStick.length-1; i++) {
        for (int j = 1; j < graphsPerStick.length; j++) {
          graphsPerStick[i].linkOtherGraphPanel(graphsPerStick[j]);
        }
      }
    }

    JPanel graphPanel = new JPanel(new GridLayout(1,2));
    UICommon.decorateComponent(graphPanel);
    JPanel posGraphPanel = new JPanel(new GridLayout(2,1));
    UICommon.decorateComponent(posGraphPanel);
    JPanel usGraphPanel = new JPanel(new GridLayout(1,2));
    UICommon.decorateComponent(usGraphPanel);
    JPanel usRawGraphPanel = new JPanel(new GridLayout(2,1));
    UICommon.decorateComponent(usRawGraphPanel);

    posGraphPanel.add(posStick[0].graph);
    posGraphPanel.add(posStick[1].graph);
    usRawGraphPanel.add(usRawPanels[0]);
    usRawGraphPanel.add(usRawPanels[1]);
    usGraphPanel.add(usRawGraphPanel);
    usGraphPanel.add(graphUSADC);
    graphPanel.add(posGraphPanel);
    graphPanel.add(usGraphPanel);

    add(graphPanel, BorderLayout.CENTER);

  }

  void buildUI() {
    buildUIGraphs();
    buildUIButtons();
  }

  public UIFDPanel(UIWorkArea wa) {
    uiinfo = new UIInfo(this, "fd" + __id, "");
    __id++;
    this.workarea = wa;
    uiinfo.name = "FD" + __id;
    workarea.addSerialLineListener(this);

    setLayout(new BorderLayout());

    UISimpleTabPane.Tab t = UISimpleTabPane.getTabByComponent(this);
    if (t != null) {
      t.setText("FD:" + workarea.getConnectionInfo());
    }

    UICommon.defineCommonActions(this, JComponent.WHEN_IN_FOCUSED_WINDOW);
    uiinfo.registerInteractionCallbacks(this, this);

    buildUI();
  }

  public void decorateUI() {
  }

  List<Double> getValuesFromStringArray(String[] array, int arrayOffset) {
    if (arrayOffset >= array.length) return new ArrayList<Double>();
    List<Double> v = new ArrayList<Double>(array.length - arrayOffset);
    for (int i = arrayOffset; i < array.length; i++) {
      try {
        v.add(Double.valueOf(array[i]));
      } catch(Throwable ignore) {}
    }
    return v;
  }


  void onInputADCData(int stick, boolean upElseDown, String[] words) {
    List<Double> vals = getValuesFromStringArray(words, 1);
    // TODO PETER stick
    graphUSADC.zoomForceVertical(-400, 6000);
    if (upElseDown) {
      graphADCDataUp.setSamples(vals);
      histAdcUp.save(vals);
    } else {
      graphADCDataDown.setSamples(vals);
      histAdcDown.save(vals);
    }
  }

  void adcUpdateGraphHistory() {
    butAdcHistPrev.setEnabled(adcHistIx < HISTORY_LEN - 1);
    butAdcHistNext.setEnabled(adcHistIx > 0);
    adcHist.setText("" + adcHistIx);
    List<Double> valsU = (List<Double>)histAdcUp.get(adcHistIx);
    List<Double> valsD = (List<Double>)histAdcDown.get(adcHistIx);
    if (valsU != null && valsD != null) {
      graphADCDataUp.setSamples(valsU);
      graphADCDataDown.setSamples(valsD);
      graphUSADC.repaint();
    }
  }

  void onInputRawUltrasoundData(int stick, String[] words) {
    List<Double> vals = getValuesFromStringArray(words, 3);
    if (vals.size() < 8 || stick < 1 || stick > 2 ) return;
    int sensors[] = {LOCAL, REMOTE};
    stick--;
    for (int sensor : sensors) {
      usStick[stick].quality[sensor].up.addSample(vals.get(sensor == LOCAL ? 5 : 1));
      usStick[stick].quality[sensor].down.addSample(vals.get(sensor == LOCAL ? 7 : 3));
      usStick[stick].quality[sensor].graph.zoom(2,1);
      usStick[stick].quality[sensor].graph.zoomForceVertical(-0.1, 1.1);
      usStick[stick].quality[sensor].graph.scrollToSampleX(0x7fffffff);
      usStick[stick].distance[sensor].up.addSample(vals.get(sensor == LOCAL ? 4 : 0));
      usStick[stick].distance[sensor].down.addSample(vals.get(sensor == LOCAL ? 6 : 2));
      usStick[stick].distance[sensor].graph.zoom(2,100);
      usStick[stick].distance[sensor].graph.zoomForceVertical(-1, 500);
      usStick[stick].distance[sensor].graph.scrollToSampleX(0x7fffffff);
    }
  }

  void onInputPos(int stick, String[] words) {
    List<Double> vals = getValuesFromStringArray(words, 2);
    if (vals.size() < 3 || stick < 1 || stick > 2 ) return;
    stick--;
    posStick[stick].x.addSample(vals.get(0));
    posStick[stick].y.addSample(vals.get(1));
    posStick[stick].z.addSample(vals.get(2));
    posStick[stick].graph.zoom(2, 0.01);
    posStick[stick].graph.zoomForceVertical(-1700, 1700);
    posStick[stick].graph.scrollToSampleX(0x7fffffff);
  }

  // SerialLineListener
  @Override
  public void onLine(String line) {
    if (paused) return;
    String[] words = line.split("\\s+");
    if (words.length == 0) return;
    if (words[0].startsWith("ADC") && words[0].length() >= 5) {
      // butAdcHistPrevU: <x> <x> <x> ...
      // butAdcHistPrevD: <x> <x> <x> ...
      // butAdcHistNextU: <x> <x> <x> ...
      // butAdcHistNextD: <x> <x> <x> ...
      onInputADCData(words[0].charAt(3) - '0', words[0].charAt(4) == 'U', words);
    } else if (words[0].equals("raw") && words.length == 11) {
      //       left up / left down / right up / right down
      // raw 1 LU/LD/RU/RD:      <dist in samples, LU> <qual, LU>       <dist in samples, LD> <qual, LD>       <dist in samples, RU> <qual, RU>       <dist in samples, RD> <qual, RD>
      onInputRawUltrasoundData(words[1].charAt(0) - '0', words);
    } else if (words[0].equals("pos") && words.length == 5) {
      // pos 1: <x> <y> <z>
      onInputPos(words[1].charAt(0) - '0', words);
    }

  }
  @Override
  public void onDisconnect() {
    UISimpleTabPane.Tab t = UISimpleTabPane.getTabByComponent(this);
    if (t != null) {
      t.setText("FD disconnected");
    }
  }
  @Override
  public void onConnect() {
    UISimpleTabPane.Tab t = UISimpleTabPane.getTabByComponent(this);
    if (t != null) {
      t.setText("FD:" + workarea.getConnectionInfo());
    }
  }

  class History {
    Object[] buffer;
    int histIx;
    int histLen;
    public History(int sz) {
      buffer = new Object[sz];
      histIx = histLen = 0;
    }
    public void save(Object o) {
      buffer[histIx] = o;
      if (histIx >= buffer.length-1) {
        histIx = 0;
      } else {
        histIx++;
      }
      if (histLen < buffer.length) {
        histLen++;
      }
    }
    public Object get(int stepsBack) {
      stepsBack++;
      if (stepsBack >= histLen) return null;
      int ix;
      if (histIx < stepsBack) {
        ix = histIx + buffer.length - stepsBack;
      } else {
        ix = histIx - stepsBack;
      }
      return buffer[ix];
    }
    public void clear() {
      histIx = 0;
      histLen = 0;
    }
  }


}
