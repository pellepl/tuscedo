package com.pelleplutt.tuscedo.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

public class GraphPanel extends JPanel {
  public static final int GRAPH_LINE = 0;
  public static final int GRAPH_BAR = 1;
  public static final int GRAPH_PLOT = 2;
  List<Double> samples;
  double minSample = 0;
  double maxSample = 0;
  double sum = 0;
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
  boolean dragging;
  int selAnchorX, selAnchorY;
  int selEndX, selEndY;
  Color colGraph = new Color(255,255,64,192);
  Color colGraphFade = new Color(255,255,0,96);
  Color colSelEdge = new Color(0,255,255,128);
  Color colSelArea = new Color(0,255,255,64);

  int graphType = GRAPH_LINE;
  
  public GraphPanel() {
    samples = new ArrayList<Double>();
    renderer = new Renderer();
    scrl = new JScrollPane(renderer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrl.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE); // fix artefacts 
    WorkArea.decorateScrollPane(scrl);
    setLayout(new BorderLayout());
    add(scrl, BorderLayout.CENTER);
    renderer.addMouseWheelListener(mouseHandler);
    renderer.addMouseListener(mouseHandler);
    renderer.addMouseMotionListener(mouseHandler);
    renderer.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 8));
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        if (oldW > 0 && oldH > 0) {
          double magHFact = (double)getWidth() / (double)oldW;
          double magVFact = (double)getHeight() / (double)oldH;
          magResize(magHor * magHFact, magVer * magVFact, new Point(getWidth()/2, getHeight()/2));
          repaint();
        }
        oldW = getWidth();
        oldH = getHeight();
      }
    });
  }
  
  public void setGraphType(int type) {
    graphType = type;
    repaint();
  }
  
  protected void addSampleInternal(double sample) {
    minSample = sample < minSample ? sample : minSample; 
    maxSample = sample > maxSample ? sample : maxSample; 
    samples.add(sample);
  }
  
  protected void sampleUpdate() {
    renderer.recalcSize();
    repaint();
  }
  
  public void addSample(double sample)  {
    addSampleInternal(sample);
    sampleUpdate();
  }
  
  public void addSamples(List<Double> samples) {
    for (double d : samples) {
      addSampleInternal(d);
    }
    sampleUpdate();
  }
  
  public int getSampleCount() {
    return samples.size();
  }
  
  public double getSample(int ix) {
    return samples.get(ix);
  }
  
  public void scrollToSample(int splIx) {
    scrollToSampleX(splIx);
    scrollToSampleY(splIx);
  }
  public void scrollToSampleX(int splIx) {
    int vpw = scrl.getViewport().getWidth();
    double scrollVal = ((double)(splIx) * magHor - vpw/2);
    scrl.getHorizontalScrollBar().setValue((int)(scrollVal));
  }
  
  public void scrollToSampleY(int splIx) {
    double splVal = 0;
    if (splIx >= 0 && splIx < samples.size()) splVal = samples.get(splIx);
    scrollToValY(splVal);
  }
  
  public void scrollToValY(double val) {
    double minGSample = Math.min(0, minSample);

    int origoY = (int)(magVer * - minGSample);
    double scrollVal = magVer * ( maxSample - minGSample ) - (origoY + val * magVer);
    scrl.getVerticalScrollBar().setValue((int)(scrollVal - getHeight()/2));
  }
  
  class Renderer extends JPanel {
    Dimension __d = new Dimension();
    public void recalcSize() {
      int h = (int)Math.round((maxSample - Math.min(0, minSample)) * magVer); 
      int w = (int)Math.round(samples.size() * magHor);
      __d.width = w;
      __d.height = h;
      setMinimumSize(__d);
      setPreferredSize(__d);
      setSize(__d);
    }
    
    public void paint(Graphics og) {
      Graphics2D g = (Graphics2D)og;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);

      double minGSample = Math.min(0, minSample);
      int ww = getWidth();
      int hh = getHeight();
      int vpw = scrl.getViewport().getWidth();
      int vph = scrl.getViewport().getHeight();
      int vpx = scrl.getHorizontalScrollBar().getValue();
      int vpy = scrl.getVerticalScrollBar().getValue();
      double minViewSample = Math.max(minGSample, maxSample - (vpy + vph) / magVer);
      double maxViewSample = Math.max(vph / magVer + minGSample, maxSample - vpy / magVer);

      g.setColor(Color.black);
      g.fillRect(0,0,ww,hh);
      if (samples.isEmpty()) return;
      
      int origoY = (int)(magVer * - minGSample);
      
      double visValuesInView = vph / magVer;
      int log10 = (int) Math.log10(visValuesInView);
      double unitStep = Math.pow(10, log10 - 1);
      if (unitStep * magVer < 10) unitStep *= 10;
      if (unitStep * magVer >= 40) unitStep /= 2;
      
      double dimin = 1;
      int expDimin = 0;
      if (log10 > 3) {
        expDimin = (log10 / 3) * 3;
        dimin = Math.pow(10, expDimin);
      } else if (log10 < 0) {
        expDimin = (int)(log10);
        dimin = Math.pow(10, -expDimin);
      }
      
      g.setColor(Color.darkGray);
      double y = (int)(minViewSample / unitStep) * unitStep;
      int guard = 0;
      while (y <= maxViewSample) {
        int gy = hh - (int)(magVer * (y - minGSample)); 
        g.setColor(Color.darkGray);
        g.drawLine( 0, gy, ww, gy);
        g.setColor(Color.gray);
        if (expDimin > 0) {
          g.drawString(decFormat.format(y / dimin) + "E" + expDimin,
              vpx, gy);
        } else if (expDimin < 0) {
          g.drawString(decFormat.format(y * dimin) + "E" + expDimin,
              vpx, gy);
        } else {
          g.drawString(decFormat.format(y), vpx, gy);
        }
        y += unitStep;
        if (guard++ > vph) return;
      }
      g.setColor(Color.gray);
      g.drawLine( 0, hh - origoY, ww, hh - origoY);

      int startSample = (int)Math.max(0, vpx / magHor - 1);
      int endSample = (int)Math.min(samples.size()-1, ((vpx + vpw) / magHor) + 1);
      g.setColor(colGraph);
      switch (graphType) {
      case GRAPH_BAR:
        paintGraphBar(g, minGSample, origoY, startSample, endSample, vpx, vpw, hh);
        break;
      case GRAPH_LINE:
        paintGraphLine(g, minGSample, origoY, startSample, endSample, vpx, vpw, hh);
        break;
      case GRAPH_PLOT:
        paintGraphPlot(g, minGSample, origoY, startSample, endSample, vpx, vpw, hh);
        break;
      }
      
      if (dragging) {
        g.setColor(colSelArea);
        int sx = Math.min(selAnchorX, selEndX);
        int sy = Math.min(selAnchorY, selEndY);
        int ex = Math.max(selAnchorX, selEndX);
        int ey = Math.max(selAnchorY, selEndY);
        g.fillRect(sx+1, sy+1, ex-sx, ey-sy);
        g.setColor(colSelEdge);
        g.drawRect(sx, sy, ex-sx, ey-sy);
      }
    }
  }
  
  void paintGraphBar(Graphics2D g, double minGSample, int origoY, int startSample, int endSample, 
      int vpx, int vpw, int hh) {
    int gw = (int)(magHor);
    for (int i = startSample; i <= endSample; i++) {
      int gx = (int)(i * magHor);
      int gy = hh - (int)(magVer * (samples.get(i) - minGSample));
      int ymin = hh - origoY;
      int ymax = gy;
      if (ymax < ymin) {
        int t = ymin;
        ymin = ymax;
        ymax = t;
      }
      if (gw < 3) {
        g.drawLine(gx, ymin, gx, ymax);
      } else {
        g.setColor(colGraph);
        g.drawRect(gx, ymin, gw, ymax-ymin);
        g.setColor(colGraphFade);
        g.fillRect(gx+1, ymin+1, gw-1, ymax-ymin-1);
      }
    }
  }
  
  int fillPX[] = new int[4];
  int fillPY[] = new int[4];
  void paintGraphLine(Graphics2D g, double minGSample, int origoY, int startSample, int endSample, 
      int vpx, int vpw, int hh) {
    int prevGAvgY = hh - (int)(magVer * (samples.get(Math.max(0, startSample-1)) - minGSample));
    int prevGMinY = prevGAvgY;
    int prevGMaxY = prevGAvgY;
    int prevGX = (int)(startSample * magHor);
    int gx = vpx-1;
    double vmin = Double.POSITIVE_INFINITY;
    double vmax = Double.NEGATIVE_INFINITY;
    double vsum = 0;
    int vcount = 0;
    
    for (int i = startSample; i <= endSample; i++) {
      gx = (int)(i * magHor);
      double s = samples.get(i);
      vmin = s < vmin ? s : vmin;
      vmax = s > vmax ? s : vmax;
      vsum += s;
      vcount++;
      if (gx != prevGX) {
        int gAvgY = hh - (int)(magVer * ((vsum/(double)vcount) - minGSample));
        int gMinY = hh - (int)(magVer * (vmin - minGSample));
        int gMaxY = hh - (int)(magVer * (vmax - minGSample));
        fillPX[0] = prevGX; fillPY[0] = prevGMinY;
        fillPX[1] = gx;     fillPY[1] = gMinY;
        fillPX[2] = gx;     fillPY[2] = gMaxY;
        fillPX[3] = prevGX; fillPY[3] = prevGMaxY;
        g.setColor(colGraphFade);
        g.fillPolygon(fillPX, fillPY, 4);
        g.setColor(colGraph);
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
  
  void paintGraphPlot(Graphics2D g, double minGSample, int origoY, int startSample, int endSample, 
      int vpx, int vpw, int hh) {
    for (int i = startSample; i <= endSample; i++) {
      int gx = (int)(i * magHor);
      int gy = hh - (int)(magVer * (samples.get(i) - minGSample));
      g.fillRect(gx-1, gy-1, 3,3);
    }
  }
  
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
    if (newMagHor != magHor) {
      double rangeH = (double)getWidth() / magHor;
      double pivotH = (double)pivot.getX() / magHor;
      double offsH = (double)scrl.getHorizontalScrollBar().getValue() / magHor;
      double portionV = (pivotH - offsH) / rangeH;
      
      magHor = newMagHor;
      magHor = Math.min(magHor, MAG_HOR_MAX);
      magHor = Math.max(magHor, MAG_HOR_MIN);
      renderer.recalcSize();

      double nrangeH = (double)getWidth() / magHor;
      double noffsH = pivotH - portionV * nrangeH;
      scrl.getHorizontalScrollBar().setValue((int)(noffsH * magHor));
    }
    if (newMagVer != magVer) {
      double rangeV = (double)getHeight() / magVer;
      double pivotV = (double)pivot.getY() / magVer;
      double offsV = (double)scrl.getVerticalScrollBar().getValue() / magVer;
      double portionV = (pivotV - offsV) / rangeV;

      magVer = newMagVer;
      magVer = Math.min(magVer, MAG_VER_MAX);
      magVer = Math.max(magVer, MAG_VER_MIN);
      renderer.recalcSize();

      double nrangeV = (double)getHeight() / magVer;
      double noffsV = pivotV - portionV * nrangeV;
      scrl.getVerticalScrollBar().setValue((int)(noffsV * magVer));
    }
  }
  
  MouseAdapter mouseHandler = new MouseAdapter() {
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
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
      if (e.getButton() == MouseEvent.BUTTON1) {
        draggingTriggered = true;
        selAnchorX = e.getX();
        selAnchorY = e.getY();
      }
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
        draggingTriggered = false;
        dragging = false;
        repaint();
      }
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
      if (draggingTriggered || dragging) {
        draggingTriggered = false;
        dragging = true;
        selEndX = e.getX();
        selEndY = e.getY();
        repaint();
      }
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1 && !e.isConsumed() && e.getClickCount()==2) {
      	zoomAll((e.getModifiers() & Event.SHIFT_MASK) == 0,
      			(e.getModifiers() & Event.CTRL_MASK) == 0,
      			e.getPoint());
      }
    }
  };
  
  public void zoomAll(boolean hori, boolean veri, Point pivot) {
    double newMagVer = magVer;
    double newMagHor = magHor;
    if (veri) {
      newMagVer = 
        Math.max(MAG_VER_MIN, (double)scrl.getViewport().getHeight() / (maxSample - Math.min(0, minSample)));
    }
    if (hori) {
      newMagHor = 
        Math.max(MAG_HOR_MIN, (double)scrl.getViewport().getWidth() / (double)samples.size());
    }
    magResize(newMagHor, newMagVer, pivot);
    repaint();
  }
  public void zoom(double x, double y) {
    Point pivot = new Point(
        scrl.getHorizontalScrollBar().getValue() + scrl.getViewport().getWidth()/2, 
        scrl.getVerticalScrollBar().getValue() + scrl.getViewport().getHeight()/2);
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
}
