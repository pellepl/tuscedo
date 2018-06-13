package com.pelleplutt.tuscedo.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

import com.pelleplutt.tuscedo.ui.UIInfo.UIListener;
import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.UIUtil;

public class UIGraphPanel extends JPanel implements UIO, UIListener {
  public static final int GRAPH_LINE = 0;
  public static final int GRAPH_BAR = 1;
  public static final int GRAPH_PLOT = 2;
  double magHor = 2.0;
  double magVer = 100.0;
  Renderer renderer;
  JScrollPane scrl;
  static final double MAG_HOR_MIN = 0.000001;
  static final double MAG_HOR_MAX = 100;
  static final double MAG_VER_MIN = 0.000001;
  static final double MAG_VER_MAX = 100000;
  static DecimalFormat decFormat = new DecimalFormat("#.##");
  int oldW, oldH;
  boolean draggingTriggered;
  boolean dragging, dragVeri, dragHori;
  int selAnchorX, selAnchorY;
  int selEndX, selEndY;
  List<SampleSet> sets = new ArrayList<SampleSet>();
  static int __id = 0;
  final UIInfo uiinfo;

  public UIInfo getUIInfo() {
    return uiinfo;
  }

  boolean userZoomed = false;

  boolean selActive, selAllX, selAllY;
  double selMinYSample, selMaxYSample;
  int selMinXSample, selMaxXSample;
  volatile boolean forcePaintSel;

  static final Color colGraph[] = { new Color(255, 255, 64, 192),
      new Color(255, 64, 255, 192), new Color(64, 255, 255, 192),
      new Color(64, 64, 255, 192), new Color(64, 255, 64, 192),
      new Color(255, 64, 64, 192), new Color(255, 192, 64, 192),
      new Color(192, 255, 64, 192), new Color(255, 64, 192, 192),
      new Color(192, 64, 255, 192), new Color(64, 255, 192, 192),
      new Color(64, 192, 255, 192), };
  static final Color colGraphFade[];
  static {
    colGraphFade = new Color[colGraph.length];
    for (int i = 0; i < colGraph.length; i++) {
      colGraphFade[i] = new Color(colGraph[i].getRed(), colGraph[i].getGreen(),
          colGraph[i].getBlue(), 98);
    }
  }
  static final Color colGraphMark[];
  static {
    colGraphMark = new Color[colGraph.length];
    for (int i = 0; i < colGraph.length; i++) {
      colGraphMark[i] = new Color(Math.min(255, 64 + colGraph[i].getRed()),
          Math.min(255, 64 + colGraph[i].getGreen()),
          Math.min(255, 64 + colGraph[i].getBlue()), 240);
    }
  }
  Color colSelEdge = new Color(0, 255, 255, 128);
  Color colSelArea = new Color(0, 255, 255, 64);

  public static class SampleSet implements UIO {
    List<Double> samples = new ArrayList<Double>();
    double minSample = 0;
    double maxSample = 0;
    double sum = 0;
    int graphType = GRAPH_LINE;
    static int __id = 0;
    final UIInfo uiinfo;

    public UIInfo getUIInfo() {
      return uiinfo;
    }

    double mul = 1.0;
    boolean detail;
    boolean selActive, selAllX, selAllY;
    double selMinYSample, selMaxYSample;
    int selMinXSample, selMaxXSample;
    double selMin, selMax, selSum;
    int selCount;
    boolean hidden;
    int colIndex;

    public SampleSet(String name) {
      uiinfo = new UIInfo(this, "samples" + __id, name);
      UIInfo.fireEventOnCreated(uiinfo);
      colIndex = __id;
      __id++;
    }

    public void setGraphType(int type) {
      graphType = type;
    }

    public void addSample(double sample) {
      addSampleInternal(sample);
      UIInfo par = getUIInfo().getParent();
      if (par != null && par.getUI() instanceof UIGraphPanel) {
        ((UIGraphPanel) par.getUI()).sampleUpdate();
      }
    }

    public void addSamples(List<Double> samples) {
      for (double d : samples) {
        addSampleInternal(d);
      }
      UIInfo par = getUIInfo().getParent();
      if (par != null && par.getUI() instanceof UIGraphPanel) {
        ((UIGraphPanel) par.getUI()).sampleUpdate();
      }
    }

    protected void addSampleInternal(double sample) {
      if (samples.isEmpty()) {
        minSample = sample;
        maxSample = sample;
      } else {
        minSample = sample < minSample ? sample : minSample;
        maxSample = sample > maxSample ? sample : maxSample;
      }
      samples.add(sample);
      sum += sample;
    }

    public int getSampleCount() {
      return samples.size();
    }

    public double getSample(int ix) {
      return samples.get(ix);
    }

    public double getMin() {
      return minSample;
    }

    public double getMax() {
      return maxSample;
    }

    public double getSum() {
      return sum;
    }

    public double getAvg() {
      return sum / samples.size();
    }

    public int getSampleCountSel() {
      return selActive ? selCount : getSampleCount();
    }

    public double getMinSel() {
      return selActive ? selMin : getMin();
    }

    public double getMaxSel() {
      return selActive ? selMax : getMax();
    }

    public double getSumSel() {
      return selActive ? selSum : getSum();
    }

    public double getAvgSel() {
      return selActive ? (selSum / selCount) : getAvg();
    }

    public void scrollToSample(int splIx) {
      UIInfo par = getUIInfo().getParent();
      if (par != null && par.getUI() instanceof UIGraphPanel) {
        ((UIGraphPanel) par.getUI()).scrollToSampleX(splIx);
        scrollToSampleY(splIx);
      }
    }

    public void scrollToSampleY(int splIx) {
      UIInfo par = getUIInfo().getParent();
      if (par != null && par.getUI() instanceof UIGraphPanel) {
        double splVal = 0;
        if (splIx >= 0 && splIx < samples.size())
          splVal = samples.get(splIx);
        ((UIGraphPanel) par.getUI()).scrollToValY(splVal);
      }
    }

    @Override
    public void repaint() {
      UIInfo par = getUIInfo().getParent();
      if (par != null) {
        par.getUI().repaint();
      }
    }

    public void setMultiplier(double mul) {
      this.mul = mul;
      UIInfo par = getUIInfo().getParent();
      if (par != null && par.getUI() instanceof UIGraphPanel) {
        ((UIGraphPanel) par.getUI()).sampleUpdate();
      }
    }

    public boolean isDetailed() {
      return detail;
    }

    public void setDetailed(boolean d) {
      detail = d;
    }

    public void select(boolean allY, boolean allX, double minYSample,
        double maxYSample, int minXSample, int maxXSample) {
      selActive = true;
      int startSample = allX ? 0 : Math.max(0, minXSample);
      int endSample = allX ? (samples.size() - 1)
          : Math.min(samples.size() - 1, maxXSample);
      selAllY = allY;
      selAllX = allX;
      selMinYSample = minYSample;
      selMaxYSample = maxYSample;
      selMinXSample = minXSample;
      selMaxXSample = maxXSample;
      selSum = 0;
      selCount = 0;
      selMin = Double.MAX_VALUE;
      selMax = Double.MIN_VALUE;
      for (int i = startSample; i <= endSample; i++) {
        double s = samples.get(i);
        if ((selAllX || (i > selMinXSample && i < selMaxXSample))
            && (selAllY || (s > selMinYSample && s < selMaxYSample))) {
          selCount++;
          selSum += s;
          selMin = selMin < s ? selMin : s;
          selMax = selMax > s ? selMax : s;
        }
      }
      if (selCount == 0)
        selActive = false;
    }

    public void unselect() {
      selActive = false;
    }

    public void export(File f) {
      StringBuilder sb = new StringBuilder();
      String nl = System.lineSeparator();
      for (double d : samples)
        sb.append(d + nl);
      AppSystem.writeFile(f, sb.toString());
    }

    public List<Double> getSamples() {
      return samples;
    }
  } // class SampleSet

  public UIGraphPanel(String name) {
    uiinfo = new UIInfo(this, "graph" + __id, null);
    UIInfo.fireEventOnCreated(uiinfo);

    __id++;
    addSampleSet(new SampleSet(name));

    renderer = new Renderer();
    scrl = new JScrollPane(renderer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrl.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE); // fix
                                                                    // artefacts
    UIWorkArea.decorateScrollPane(scrl);
    setLayout(new BorderLayout());
    add(scrl, BorderLayout.CENTER);
    renderer.addMouseWheelListener(mouseHandler);
    renderer.addMouseListener(mouseHandler);
    renderer.addMouseMotionListener(mouseHandler);

    int when = JComponent.WHEN_IN_FOCUSED_WINDOW;
    UIWorkArea.defineAnonAction(renderer, "scrl.up", "up", when, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        scrl.getVerticalScrollBar()
            .setValue((int) (scrl.getVerticalScrollBar().getValue() - 1));
      }
    });
    UIWorkArea.defineAnonAction(renderer, "scrl.down", "down",when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            scrl.getVerticalScrollBar()
                .setValue((int) (scrl.getVerticalScrollBar().getValue() + 1));
          }
        });
    UIWorkArea.defineAnonAction(renderer, "scrl.left", "left",when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            scrl.getHorizontalScrollBar()
                .setValue((int) (scrl.getHorizontalScrollBar().getValue() - 1));
          }
        });
    UIWorkArea.defineAnonAction(renderer, "scrl.right", "right",when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            scrl.getHorizontalScrollBar()
                .setValue((int) (scrl.getHorizontalScrollBar().getValue() + 1));
          }
        });
    UIWorkArea.defineAnonAction(renderer, "scrl.up.more", "ctrl+up",when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            scrl.getVerticalScrollBar()
                .setValue((int) (scrl.getVerticalScrollBar().getValue() - 10));
          }
        });
    UIWorkArea.defineAnonAction(renderer, "scrl.down.more", "ctrl+down",when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            scrl.getVerticalScrollBar()
                .setValue((int) (scrl.getVerticalScrollBar().getValue() + 10));
          }
        });
    UIWorkArea.defineAnonAction(renderer, "scrl.left.more", "ctrl+left",when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            scrl.getHorizontalScrollBar().setValue(
                (int) (scrl.getHorizontalScrollBar().getValue() - 10));
          }
        });
    UIWorkArea.defineAnonAction(renderer, "scrl.right.more", "ctrl+right",when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            scrl.getHorizontalScrollBar().setValue(
                (int) (scrl.getHorizontalScrollBar().getValue() + 10));
          }
        });
    UIWorkArea.defineAnonAction(renderer, "sel.exp.up", "shift+up",when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            expandSelection(0, -1);
          }
        });
    UIWorkArea.defineAnonAction(renderer, "sel.exp.down", "shift+down",when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            expandSelection(0, 1);
          }
        });
    UIWorkArea.defineAnonAction(renderer, "sel.exp.left", "shift+left",when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            expandSelection(-1, 0);
          }
        });
    UIWorkArea.defineAnonAction(renderer, "sel.exp.right", "shift+right",when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            expandSelection(1, 0);
          }
        });
    UIWorkArea.defineAnonAction(renderer, "sel.exp.up.more", "ctrl+shift+up",when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            expandSelection(0, -10);
          }
        });
    UIWorkArea.defineAnonAction(renderer, "sel.exp.down.more", "ctrl+shift+down",when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            expandSelection(0, 10);
          }
        });
    UIWorkArea.defineAnonAction(renderer, "sel.exp.left.more", "ctrl+shift+left",when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            expandSelection(-10, 0);
          }
        });
    UIWorkArea.defineAnonAction(renderer, "sel.exp.right.more", "ctrl+shift+right",when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            expandSelection(10, 0);
          }
        });
    UIWorkArea.defineAnonAction(renderer, "sel.clear", "escape", when,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            unselect();
          }
        });

    renderer.setFocusable(true);
    renderer.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        if (oldW > 0 && oldH > 0) {
          double magHFact = (double) getWidth() / (double) oldW;
          double magVFact = (double) getHeight() / (double) oldH;
          magResize(magHor * magHFact, magVer * magVFact,
              new Point(getWidth() / 2, getHeight() / 2));
          repaint();
        }
        oldW = getWidth();
        oldH = getHeight();
      }
    });
  }

  public SampleSet getSampleSet(int i) {
    return sets.get(i);
  }

  public void addSampleSet(SampleSet set) {
    if (!sets.contains(set)) {
      sets.add(set);
      set.getUIInfo().addListener(UIGraphPanel.this);
      getUIInfo().addChild(set);
    }
    repaint();
  }

  public void removeSampleSet(SampleSet set) {
    if (sets.contains(set)) {
      sets.remove(set);
      getUIInfo().removeChild(set);
    }
    repaint();
  }

  protected void sampleUpdate() {
    renderer.recalcSize(getMaxSample(), getMinSample(), magHor, magVer,
        getSampleCount());
    repaint();
  }

  public void addSample(double sample) {
    sets.get(0).addSampleInternal(sample);
    sampleUpdate();
  }

  public void addSamples(List<Double> samples) {
    for (double d : samples) {
      sets.get(0).addSampleInternal(d);
    }
    sampleUpdate();
  }

  public int getSampleCount() {
    int max = 0;
    for (SampleSet set : sets) {
      max = Math.max(max, set.samples.size());
    }
    return max;
  }

  public double getMaxSample() {
    double max = 0;
    for (SampleSet set : sets) {
      max = Math.max(max, set.maxSample * set.mul);
    }
    return max;
  }

  public double getMinSample() {
    double min = 0;
    for (SampleSet set : sets) {
      min = Math.min(min, set.minSample * set.mul);
    }
    return min;
  }

  public void scrollToSampleX(int splIx) {
    int vpw = scrl.getViewport().getWidth();
    double scrollVal = ((double) (splIx) * magHor - vpw / 2);
    scrl.getHorizontalScrollBar().setValue((int) (scrollVal));
  }

  public void scrollToValY(double val) {
    double minGSample = Math.min(0, getMinSample());

    int origoY = (int) (magVer * -minGSample);
    double scrollVal = magVer * (getMaxSample() - minGSample)
        - (origoY + val * magVer);
    scrl.getVerticalScrollBar().setValue((int) (scrollVal - getHeight() / 2));
  }

  public boolean isUserZoomed() {
    return userZoomed;
  }

  public void select(boolean allY, boolean allX, int sx, int sy, int ex,
      int ey) {
    double minGSample = getMinSample(); // Math.min(0, minSample);
    int hh = renderer.getHeight();
    if (sy < ey) {
      int tmp = sy;
      sy = ey;
      ey = tmp;
    }
    if (sx > ex) {
      int tmp = sx;
      sx = ex;
      ex = tmp;
    }
    selAllY = allY;
    selAllX = allX;
    selMinYSample = (hh - sy) / magVer + minGSample;
    selMaxYSample = (hh - ey) / magVer + minGSample;
    selMinXSample = (int) (sx / magHor);
    selMaxXSample = (int) (ex / magHor + 1);
    for (SampleSet set : sets) {
      set.select(selAllY, selAllX, selMinYSample, selMaxYSample, selMinXSample,
          selMaxXSample);
    }

    selActive = true;
  }

  public void unselect() {
    for (SampleSet set : sets) {
      set.unselect();
    }
    selActive = false;
    repaint();
  }

  class GraphAction implements ActionListener {
    public SampleSet set;

    public GraphAction(SampleSet set) {
      this.set = set;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getActionCommand().equals("Delete")) {
        removeSampleSet(set);
      } else if (e.getActionCommand().equals("Hide")) {
        set.hidden = true;
        repaint();
      } else if (e.getActionCommand().equals("Show")) {
        set.hidden = false;
        repaint();
      } else if (e.getActionCommand().equals("Export")) {
        File f = UIUtil.selectFile(renderer,
            "Export graph \"" + set.getUIInfo().getName() + "\"", "Export");
        set.export(f);
      }
    }
  };

  void openMenu(MouseEvent e, SampleSet s) {
    JPopupMenu m = new JPopupMenu(s.getUIInfo().getName());
    JMenuItem i;
    ActionListener graphAction = new GraphAction(s);
    m.add(i = new JMenuItem(s.hidden ? "Show" : "Hide"));
    i.addActionListener(graphAction);
    m.add(i = new JMenuItem("Export"));
    i.addActionListener(graphAction);
    m.add(i = new JMenuItem("Delete"));
    i.addActionListener(graphAction);
    m.show(renderer, e.getX(), e.getY());
  }

  void expandSelection(int dx, int dy) {
    if (dx != 0)
      selEndX += dx;
    if (dy != 0)
      selEndY += dy;
    select(selAllY, selAllX, selAnchorX, selAnchorY, selEndX, selEndY);
    forcePaintSel = true;
    repaint();
  }

  class Renderer extends JPanel {
    Dimension __d = new Dimension();

    public void recalcSize(double maxSample, double minSample, double magHor,
        double magVer, int samples) {
      int h = (int) Math.round((maxSample - Math.min(0, minSample)) * magVer);
      int w = (int) Math.round(samples * magHor);
      __d.width = w;
      __d.height = h;
      setMinimumSize(__d);
      setPreferredSize(__d);
      setSize(__d);
    }

    public void paint(Graphics2D g, List<Double> samples, double mul,
        int graphType, boolean paintGrid, int ww, int hh, int vpw, int vph,
        int vpx, int vpy, double minSample, double maxSample, double magHor,
        double magVer, int colIx) {
      double minGSample = minSample; // Math.min(0, minSample);
      double minViewSample = Math.max(minGSample,
          maxSample - (vpy + vph) / magVer);
      double maxViewSample = Math.max(vph / magVer + minGSample,
          maxSample - vpy / magVer);

      int origoY = (int) (magVer * -minGSample);

      if (paintGrid) {
        double visValuesInView = vph / magVer;
        int log10 = (int) Math.log10(visValuesInView);
        double unitStep = Math.pow(10, log10 - 1);
        if (unitStep * magVer < 10)
          unitStep *= 10;
        if (unitStep * magVer >= 40)
          unitStep /= 2;

        double dimin = 1;
        int expDimin = 0;
        if (log10 > 3) {
          expDimin = (log10 / 3) * 3;
          dimin = Math.pow(10, expDimin);
        } else if (log10 < 0) {
          expDimin = (int) (log10);
          dimin = Math.pow(10, -expDimin);
        }

        g.setColor(Color.darkGray);
        double y = (int) (minViewSample / unitStep) * unitStep;
        int guard = 0;
        while (y <= maxViewSample) {
          int gy = hh - (int) (magVer * (y - minGSample));
          g.setColor(Color.darkGray);
          g.drawLine(0, gy, ww, gy);
          g.setColor(Color.gray);
          if (expDimin > 0) {
            g.drawString(decFormat.format(y / dimin) + "E" + expDimin, vpx, gy);
          } else if (expDimin < 0) {
            g.drawString(decFormat.format(y * dimin) + "E" + expDimin, vpx, gy);
          } else {
            g.drawString(decFormat.format(y), vpx, gy);
          }
          y += unitStep;
          if (guard++ > vph)
            return;
        }
        g.setColor(Color.gray);
        g.drawLine(0, hh - origoY, ww, hh - origoY);
        return;
      }

      if (samples.isEmpty())
        return;

      int startSample = (int) Math.max(0, vpx / magHor - 1);
      int endSample = (int) Math.min(samples.size() - 1,
          ((vpx + vpw) / magHor) + 1);
      g.setColor(colGraph[colIx]);
      switch (graphType) {
      case GRAPH_BAR:
        paintGraphBar(g, samples, mul, minGSample, origoY, startSample,
            endSample, vpx, vpw, hh, colIx);
        break;
      case GRAPH_LINE:
        paintGraphLine(g, samples, mul, minGSample, origoY, startSample,
            endSample, vpx, vpw, hh, colIx);
        break;
      case GRAPH_PLOT:
        paintGraphPlot(g, samples, mul, minGSample, origoY, startSample,
            endSample, vpx, vpw, hh, colIx);
        break;
      }
    }

    @Override
    public void paint(Graphics og) {
      Graphics2D g = (Graphics2D) og;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
      int ww = getWidth();
      int hh = getHeight();
      int vpw = scrl.getViewport().getWidth();
      int vph = scrl.getViewport().getHeight();
      int vpx = scrl.getHorizontalScrollBar().getValue();
      int vpy = scrl.getVerticalScrollBar().getValue();

      double maxSample = getMaxSample();
      double minSample = getMinSample();

      g.setColor(Color.black);
      g.fillRect(0, 0, ww, hh);

      // sample sets
      paint(g, null, 0, 0, true, ww, hh, vpw, vph, vpx, vpy, minSample,
          maxSample, magHor, magVer, 0);
      for (SampleSet set : sets) {
        if (!set.hidden) {
          paint(g, set.samples, set.mul, set.graphType, false, ww, hh, vpw, vph,
              vpx, vpy, minSample, maxSample, magHor, magVer,
              set.colIndex % colGraph.length);
        }
      }

      // selection
      if (dragging || forcePaintSel) {
        g.setColor(colSelArea);
        int sx = Math.min(selAnchorX, selEndX);
        int sy = Math.min(selAnchorY, selEndY);
        int ex = Math.max(selAnchorX, selEndX);
        int ey = Math.max(selAnchorY, selEndY);
        if (dragHori) {
          sx = -1;
          ex = ww + 1;
        }
        if (dragVeri) {
          sy = -1;
          ey = hh + 1;
        }
        g.fillRect(sx + 1, sy + 1, ex - sx, ey - sy);
        g.setColor(colSelEdge);
        g.drawRect(sx, sy, ex - sx, ey - sy);
        forcePaintSel = false;
      }

      // legend
      int wwv = Math.max(vpw / 8, 40);
      int hhv = g.getFont().getSize() + 4;
      int xv = vpx + vpw - wwv - 2;
      int yv = vpy + 2;
      int hhv_inc = hhv;
      for (SampleSet set : sets) {
        if (set.isDetailed()) {
          hhv_inc = hhv * 5;
        }
        paintLegend(g, set, xv, yv, wwv, hhv, set.colIndex % colGraph.length);
        xv -= wwv + 2;
        if (xv < vpx + 100) {
          xv = vpx + vpw - wwv - 2;
          yv += hhv_inc + 2;
          hhv_inc = hhv;
        }
      }
    }

    Rectangle _clipR = new Rectangle();

    void paintLegend(Graphics2D g, SampleSet set, int x, int y, int w, int h,
        int c) {
      int bh = set.isDetailed() ? h * 5 : h;
      g.setColor(colGraphFade[c]);
      g.fillRect(x, y, w, bh);
      if (!set.hidden) {
        g.setColor(colGraph[c]);
      }
      g.drawRect(x, y, w, bh);
      g.getClipBounds(_clipR);
      g.setClip(x, y, w, bh);
      g.setColor(set.isDetailed() ? Color.white : Color.black);
      g.drawString(set.getUIInfo().getName(), x + 2, y + h - 2);
      if (set.isDetailed()) {
        g.drawString(" <:" + set.getMinSel(), x + 2, y + h * 2 - 2);
        g.drawString(" >:" + set.getMaxSel(), x + 2, y + h * 3 - 2);
        g.drawString(" ~:" + set.getAvgSel(), x + 2, y + h * 4 - 2);
        g.drawString(" #:" + set.getSampleCountSel(), x + 2, y + h * 5 - 2);
      }
      g.setClip(_clipR);
    }

    void paintGraphBar(Graphics2D g, List<Double> samples, double mul,
        double minGSample, int origoY, int startSample, int endSample, int vpx,
        int vpw, int hh, int colIx) {
      int gw = (int) (magHor);
      for (int i = startSample; i <= endSample; i++) {
        int gx = (int) (i * magHor);
        double s = samples.get(i) * mul;
        int gy = hh - (int) (magVer * (s - minGSample));
        int ymin = hh - origoY;
        int ymax = gy;
        if (ymax < ymin) {
          int t = ymin;
          ymin = ymax;
          ymax = t;
        }
        boolean sel = selActive
            && (selAllX || (i > selMinXSample && i < selMaxXSample))
            && (selAllY || (s > selMinYSample && s < selMaxYSample));
        if (sel) {
          g.setColor(colGraphMark[colIx]);
          if (gw < 3) {
            g.drawLine(gx - 1, ymin, gx - 1, ymax);
            g.drawLine(gx, ymin, gx, ymax);
            g.drawLine(gx + 1, ymin, gx + 1, ymax);
          } else {
            g.drawRect(gx, ymin, gw, ymax - ymin);
            g.setColor(colGraph[colIx]);
            g.fillRect(gx + 1, ymin + 1, gw - 1, ymax - ymin - 1);
          }
        } else {
          g.setColor(colGraph[colIx]);
          if (gw < 3) {
            g.drawLine(gx, ymin, gx, ymax);
          } else {
            g.drawRect(gx, ymin, gw, ymax - ymin);
            g.setColor(colGraphFade[colIx]);
            g.fillRect(gx + 1, ymin + 1, gw - 1, ymax - ymin - 1);
          }
        }
      }
    }

    int fillPX[] = new int[4];
    int fillPY[] = new int[4];

    void paintGraphLine(Graphics2D g, List<Double> samples, double mul,
        double minGSample, int origoY, int startSample, int endSample, int vpx,
        int vpw, int hh, int colIx) {
      if (startSample >= samples.size())
        return;
      int prevGAvgY = hh - (int) (magVer
          * (samples.get(Math.max(0, startSample - 1)) * mul - minGSample));
      int prevGMinY = prevGAvgY;
      int prevGMaxY = prevGAvgY;
      int prevGX = (int) (startSample * magHor);
      int gx = vpx - 1;
      double vmin = Double.POSITIVE_INFINITY;
      double vmax = Double.NEGATIVE_INFINITY;
      double vsum = 0;
      int vcount = 0;

      for (int i = startSample; i <= endSample; i++) {
        gx = (int) (i * magHor);
        double s = samples.get(i) * mul;
        vmin = s < vmin ? s : vmin;
        vmax = s > vmax ? s : vmax;
        vsum += s;
        vcount++;
        if (gx != prevGX) {
          int gAvgY = hh
              - (int) (magVer * ((vsum / (double) vcount) - minGSample));
          int gMinY = hh - (int) (magVer * (vmin - minGSample));
          int gMaxY = hh - (int) (magVer * (vmax - minGSample));
          fillPX[0] = prevGX;
          fillPY[0] = prevGMinY;
          fillPX[1] = gx;
          fillPY[1] = gMinY;
          fillPX[2] = gx;
          fillPY[2] = gMaxY;
          fillPX[3] = prevGX;
          fillPY[3] = prevGMaxY;
          g.setColor(colGraphFade[colIx]);
          g.fillPolygon(fillPX, fillPY, 4);
          if (selActive && (selAllX || (i > selMinXSample && i < selMaxXSample))
              && (selAllY || (s > selMinYSample && s < selMaxYSample))) {
            g.setColor(colGraphMark[colIx]);
            g.drawLine(prevGX, prevGAvgY - 1, gx, gAvgY - 1);
            g.drawLine(prevGX, prevGAvgY + 1, gx, gAvgY + 1);
          } else {
            g.setColor(colGraph[colIx]);
          }
          g.drawLine(prevGX, prevGAvgY, gx, gAvgY);
          prevGX = gx;
          prevGAvgY = gAvgY;
          prevGMinY = gMinY;
          prevGMaxY = gMaxY;
          vmin = Double.POSITIVE_INFINITY;
          vmax = Double.NEGATIVE_INFINITY;
          vsum = 0;
          vcount = 0;
        }
      }
    }

    void paintGraphPlot(Graphics2D g, List<Double> samples, double mul,
        double minGSample, int origoY, int startSample, int endSample, int vpx,
        int vpw, int hh, int colIx) {
      for (int i = startSample; i <= endSample; i++) {
        double s = samples.get(i) * mul;
        int gx = (int) (i * magHor);
        int gy = hh - (int) (magVer * (s - minGSample));
        if (selActive && (selAllX || (i > selMinXSample && i < selMaxXSample))
            && (selAllY || (s > selMinYSample && s < selMaxYSample))) {
          g.setColor(colGraphMark[colIx]);
          g.fillRect(gx - 2, gy - 2, 5, 5);
        } else {
          g.setColor(colGraph[colIx]);
          g.fillRect(gx - 1, gy - 1, 3, 3);
        }
      }
    }
  } // Renderer

  /**
   * x = mouse offset
   * before magnification:
   *         o          x
   *         |----------+-----|
   * 012345678901234567890123456789
   *         <--------r------->
   * o = scrollbar offset
   * r = viewport range
   * 
   * after magnification:
   *            o'      x
   *            |-------+---|
   * 012345678901234567890123456789
   *            <-----r'----> 
   * o' = new scrollbar offset 
   * r' = new viewport range
   * 
   * we want
   * (x-o)/r = (x-o')/r'
   * so new scrollbar offset is 
   * o' = x - r'*((x-o)/r)
   */
  void magResize(double newMagHor, double newMagVer, Point pivot) {
    boolean updHor = newMagHor != magHor;
    boolean updVer = newMagVer != magVer;

    double rangeH = (double) getWidth() / magHor;
    double pivotH = (double) pivot.getX() / magHor;
    double offsH = (double) scrl.getHorizontalScrollBar().getValue() / magHor;
    double portionH = (pivotH - offsH) / rangeH;

    magHor = newMagHor;
    magHor = Math.min(magHor, MAG_HOR_MAX);
    magHor = Math.max(magHor, MAG_HOR_MIN);

    double nrangeH = (double) getWidth() / magHor;
    double noffsH = pivotH - portionH * nrangeH;
    double rangeV = (double) getHeight() / magVer;
    double pivotV = (double) pivot.getY() / magVer;
    double offsV = (double) scrl.getVerticalScrollBar().getValue() / magVer;
    double portionV = (pivotV - offsV) / rangeV;

    magVer = newMagVer;
    magVer = Math.min(magVer, MAG_VER_MAX);
    magVer = Math.max(magVer, MAG_VER_MIN);

    double nrangeV = (double) getHeight() / magVer;
    double noffsV = pivotV - portionV * nrangeV;

    if (updHor || updVer) {
      renderer.recalcSize(getMaxSample(), getMinSample(), magHor, magVer,
          getSampleCount());
      if (updHor) {
        scrl.getHorizontalScrollBar().setValue((int) (noffsH * magHor));
      }
      if (updVer) {
        scrl.getVerticalScrollBar().setValue((int) (noffsV * magVer));
      }
    }
  }

  MouseAdapter mouseHandler = new MouseAdapter() {
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
      userZoomed = true;
      double magFact = 1.0;
      if (e.getWheelRotation() < 0) {
        magFact = 1.1;
      } else if (e.getWheelRotation() > 0) {
        magFact = 0.9;
      }

      boolean veri = (e.getModifiers() & Event.CTRL_MASK) == 0;
      boolean hori = (e.getModifiers() & Event.SHIFT_MASK) == 0;

      if (hori) {
        magResize(magHor * magFact, magVer, e.getPoint());
      }
      if (veri) {
        magResize(magHor, magVer * magFact, e.getPoint());
      }
      repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
      renderer.requestFocusInWindow();
      if (e.getButton() == MouseEvent.BUTTON1) {
        draggingTriggered = true;
        dragVeri = (e.getModifiers() & Event.CTRL_MASK) != 0;
        dragHori = (e.getModifiers() & Event.SHIFT_MASK) != 0;
        selAnchorX = e.getX();
        selAnchorY = e.getY();
      } else if (e.getButton() == MouseEvent.BUTTON3) {
        SampleSet s = clickedLegend(e.getX(), e.getY());
        if (s != null) {
          openMenu(e, s);
        }
      }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
      if (draggingTriggered || dragging) {
        draggingTriggered = false;
        dragging = true;
        dragVeri = (e.getModifiers() & Event.CTRL_MASK) != 0;
        dragHori = (e.getModifiers() & Event.SHIFT_MASK) != 0;
        selEndX = e.getX();
        selEndY = e.getY();
        repaint();
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
        if (dragging) {
          select(dragVeri, dragHori, selAnchorX, selAnchorY, selEndX, selEndY);
        }
        draggingTriggered = false;
        dragging = false;
        repaint();
      }
    }

    SampleSet clickedLegend(int x, int y) {
      int vpw = scrl.getViewport().getWidth();
      int vpx = scrl.getHorizontalScrollBar().getValue();
      int vpy = scrl.getVerticalScrollBar().getValue();

      int wwv = Math.max(vpw / 8, 40);
      int hhv = getFont().getSize() + 4;
      int xv = vpx + vpw - wwv - 2;
      int yv = vpy + 2;
      int hhv_inc = hhv;
      for (SampleSet set : sets) {
        if (set.isDetailed()) {
          hhv_inc = hhv * 5;
        }
        if (x >= xv && x <= xv + wwv && y >= yv
            && y <= yv + (set.isDetailed() ? hhv * 5 : hhv)) {
          return set;
        }
        xv -= wwv + 2;
        if (xv < vpx + 100) {
          xv = vpx + vpw - wwv - 2;
          yv += hhv_inc + 2;
          hhv_inc = hhv;
        }
      }
      return null;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1 && !e.isConsumed()
          && e.getClickCount() == 2) {
        zoomAll((e.getModifiers() & Event.SHIFT_MASK) == 0,
            (e.getModifiers() & Event.CTRL_MASK) == 0, e.getPoint());
      } else if (e.getButton() == MouseEvent.BUTTON1 && !e.isConsumed()
          && e.getClickCount() == 1) {
        SampleSet s = clickedLegend(e.getX(), e.getY());
        if (s != null) {
          s.setDetailed(!s.isDetailed());
          repaint();
          return;
        }
        if (!dragging) {
          unselect();
        }
      }
    }
  };

  public void zoomAll(boolean hori, boolean veri, Point pivot) {
    double newMagVer = magVer;
    double newMagHor = magHor;
    userZoomed = false;
    if (veri) {
      newMagVer = Math.max(MAG_VER_MIN, (double) scrl.getViewport().getHeight()
          / (getMaxSample() - Math.min(0, getMinSample())));
    }
    if (hori) {
      newMagHor = Math.max(MAG_HOR_MIN,
          (double) scrl.getViewport().getWidth() / (double) getSampleCount());
    }
    magResize(newMagHor, newMagVer, pivot);
    repaint();
  }

  public void zoom(double x, double y) {
    userZoomed = false;
    Point pivot = new Point(
        scrl.getHorizontalScrollBar().getValue()
            + scrl.getViewport().getWidth() / 2,
        scrl.getVerticalScrollBar().getValue()
            + scrl.getViewport().getHeight() / 2);
    double newMagVer = magVer;
    double newMagHor = magHor;
    if (x > 0) {
      newMagHor = x;
    }
    if (y > 0) {
      newMagVer = y;
    }
    magResize(newMagHor, newMagVer, pivot);
    repaint();
  }

  @Override
  public void onRemoved(UIO parent, UIO child) {
    if (child instanceof SampleSet) {
      removeSampleSet((SampleSet) child);
    }
    if (sets.isEmpty()) {
      child.getUIInfo().removeListener(UIGraphPanel.this);
      getUIInfo().close();
    }
  }

  @Override
  public void onAdded(UIO parent, UIO child) {
  }

  @Override
  public void onClosed(UIO parent, UIO child) {
  }

  @Override
  public void onCreated(UIInfo obj) {
  }

  @Override
  public void onEvent(UIO obj, Object event) {
  }
}
