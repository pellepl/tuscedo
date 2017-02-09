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

import com.pelleplutt.tuscedo.Ownable;

public abstract class GraphPanel extends JPanel implements Ownable {
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
  List<Object> overlayedGraphs = new ArrayList<Object>();
  public Object owner;
  public Object getOwner() {return owner;}
  public void setOwner(Object o) {owner = o;}
  
  static final Color colGraph[] = {
      new Color(255,255, 64, 192),
      new Color(255, 64,255, 192),
      new Color( 64,255,255, 192),
      new Color( 64, 64,255, 192),
      new Color( 64,255, 64, 192),
      new Color(255, 64, 64, 192),
      new Color(255,192, 64, 192),
      new Color(192,255, 64, 192),
      new Color(255, 64,192, 192),
      new Color(192, 64,255, 192),
      new Color( 64,255,192, 192),
      new Color( 64,192,255, 192),
  };
  static final Color colGraphFade[];
  static {
    colGraphFade = new Color[colGraph.length];
    for (int i = 0; i < colGraph.length; i++) {
      colGraphFade[i] = new Color(colGraph[i].getRed(), colGraph[i].getGreen(), colGraph[i].getBlue(), 98);
    }
  }
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
  
  public void addOverlayGraphObject(Object o) {
    if (!overlayedGraphs.contains(o)) overlayedGraphs.add(o);
  }
  
  public void removeOverlayGraphObject(Object o) {
    if (!overlayedGraphs.contains(o)) overlayedGraphs.add(o);
  }
  
  public abstract GraphPanel getGraphPanelFromOverlayObject(Object o);
  
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
    renderer.recalcSize(maxSample, minSample, magHor, magVer, samples.size());
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
    if (overlayedGraphs.isEmpty())
      return samples.size();
    else {
      int max = samples.size();
      for (Object o : overlayedGraphs) {
        max = Math.max(max, getGraphPanelFromOverlayObject(o).getSampleCount());
      }
      return max;
    }
  }
  
  public double getMaxSample() {
    if (overlayedGraphs.isEmpty())
      return maxSample;
    else {
      double max = maxSample;
      for (Object o : overlayedGraphs) {
        max = Math.max(max, getGraphPanelFromOverlayObject(o).getMaxSample());
      }
      return max;
    }
  }
  
  public double getMinSample() {
    if (overlayedGraphs.isEmpty())
      return minSample;
    else {
      double min = minSample;
      for (Object o : overlayedGraphs) {
        min = Math.min(min, getGraphPanelFromOverlayObject(o).getMinSample());
      }
      return min;
    }
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
    public void recalcSize(double maxSample, double minSample, double magHor, double magVer, int samples) {
      int h = (int)Math.round((maxSample - Math.min(0, minSample)) * magVer); 
      int w = (int)Math.round(samples * magHor);
      __d.width = w;
      __d.height = h;
      setMinimumSize(__d);
      setPreferredSize(__d);
      setSize(__d);
    }
    
    public void paint(Graphics2D g,
        List<Double> samples,
        boolean paintClear, boolean paintGrid,
        int ww, int hh, int vpw, int vph, int vpx, int vpy,
        double minSample, double maxSample, double magHor, double magVer,
        int colIx) {
      double minGSample = minSample; //Math.min(0, minSample);
      double minViewSample = Math.max(minGSample, maxSample - (vpy + vph) / magVer);
      double maxViewSample = Math.max(vph / magVer + minGSample, maxSample - vpy / magVer);

      if (paintClear) {
        g.setColor(Color.black);
        g.fillRect(0,0,ww,hh);
      }
      if (samples.isEmpty()) return;
      
      int origoY = (int)(magVer * - minGSample);
      
      if (paintGrid) {
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
      }

      int startSample = (int)Math.max(0, vpx / magHor - 1);
      int endSample = (int)Math.min(samples.size()-1, ((vpx + vpw) / magHor) + 1);
      g.setColor(colGraph[colIx]);
      switch (graphType) {
      case GRAPH_BAR:
        paintGraphBar(g, samples, minGSample, origoY, startSample, endSample, vpx, vpw, hh, colIx);
        break;
      case GRAPH_LINE:
        paintGraphLine(g, samples, minGSample, origoY, startSample, endSample, vpx, vpw, hh, colIx);
        break;
      case GRAPH_PLOT:
        paintGraphPlot(g, samples, minGSample, origoY, startSample, endSample, vpx, vpw, hh, colIx);
        break;
      }
    }
    
    @ Override
    public void paint(Graphics og) {
      Graphics2D g = (Graphics2D)og;
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

      paint(g, samples, 
          true, true,
          ww, hh, vpw, vph, vpx, vpy,
          minSample, maxSample, magHor, magVer,
          0);
      if (!overlayedGraphs.isEmpty()) {
        int c = 1;
        for (Object o : overlayedGraphs) {
          GraphPanel gp = getGraphPanelFromOverlayObject(o);
          paint(g, gp.samples, 
              false, false,  
              ww, hh, vpw, vph, vpx, vpy,
              minSample, maxSample, magHor, magVer,
              c % colGraph.length);
          c++;
        }
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
      
      if (!overlayedGraphs.isEmpty()) {
        int wwv = Math.max(vpw / 8, 40);
        int hhv = g.getFont().getSize() + 4;
        int xv = vpx + vpw - wwv - 2;
        int yv = vpy + hhv + 2;
        if (getOwner() != null) {
          paintLegend(g, getOwner().toString(), xv, yv, wwv, hhv, 0);
        }
        int c = 1;
        for (Object o : overlayedGraphs) {
          xv -= wwv + 2;
          if (xv < vpx + 100) {
            xv = vpx + vpw - wwv - 2;
            yv += hhv + 2;
          }
          GraphPanel gp = getGraphPanelFromOverlayObject(o);
          if (gp.getOwner() != null) {
            paintLegend(g, gp.getOwner().toString(), 
                xv, yv, wwv, hhv, 
                c % colGraph.length);
          }
          c++;
        }
      }
    }
    
    Rectangle _clipR = new Rectangle();
    void paintLegend(Graphics2D g, String txt, int x, int y, int w, int h, int c) {
      g.setColor(colGraphFade[c]);
      g.fillRect(x, y, w, h);
      g.setColor(colGraph[c]);
      g.drawRect(x, y, w, h);
      g.getClipBounds(_clipR);
      g.setClip(x, y, w, h);
      g.setColor(Color.black);
      g.drawString(txt, x+2, y + h - 2);
      g.setClip(_clipR);
    }
    
    void paintGraphBar(Graphics2D g, List<Double> samples, double minGSample, int origoY, int startSample, int endSample, 
        int vpx, int vpw, int hh, int colIx) {
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
          g.setColor(colGraph[colIx]);
          g.drawRect(gx, ymin, gw, ymax-ymin);
          g.setColor(colGraphFade[colIx]);
          g.fillRect(gx+1, ymin+1, gw-1, ymax-ymin-1);
        }
      }
    }
    
    int fillPX[] = new int[4];
    int fillPY[] = new int[4];
    void paintGraphLine(Graphics2D g, List<Double> samples, double minGSample, int origoY, int startSample, int endSample, 
        int vpx, int vpw, int hh, int colIx) {
      if (startSample >= samples.size()) return;
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
          g.setColor(colGraphFade[colIx]);
          g.fillPolygon(fillPX, fillPY, 4);
          g.setColor(colGraph[colIx]);
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
    
    void paintGraphPlot(Graphics2D g, List<Double> samples, double minGSample, int origoY, int startSample, int endSample, 
        int vpx, int vpw, int hh, int colIx) {
      for (int i = startSample; i <= endSample; i++) {
        int gx = (int)(i * magHor);
        int gy = hh - (int)(magVer * (samples.get(i) - minGSample));
        g.fillRect(gx-1, gy-1, 3,3);
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

    double rangeH = (double)getWidth() / magHor;
    double pivotH = (double)pivot.getX() / magHor;
    double offsH = (double)scrl.getHorizontalScrollBar().getValue() / magHor;
    double portionH = (pivotH - offsH) / rangeH;
    
    magHor = newMagHor;
    magHor = Math.min(magHor, MAG_HOR_MAX);
    magHor = Math.max(magHor, MAG_HOR_MIN);

    double nrangeH = (double)getWidth() / magHor;
    double noffsH = pivotH - portionH * nrangeH;
    double rangeV = (double)getHeight() / magVer;
    double pivotV = (double)pivot.getY() / magVer;
    double offsV = (double)scrl.getVerticalScrollBar().getValue() / magVer;
    double portionV = (pivotV - offsV) / rangeV;

    magVer = newMagVer;
    magVer = Math.min(magVer, MAG_VER_MAX);
    magVer = Math.max(magVer, MAG_VER_MIN);

    double nrangeV = (double)getHeight() / magVer;
    double noffsV = pivotV - portionV * nrangeV;
    
    if (updHor || updVer) {
      renderer.recalcSize(getMaxSample(), getMinSample(), magHor, magVer, getSampleCount());
      if (updHor) {
        scrl.getHorizontalScrollBar().setValue((int)(noffsH * magHor));
      }
      if (updVer) {
        scrl.getVerticalScrollBar().setValue((int)(noffsV * magVer));
      }
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
        Math.max(MAG_VER_MIN, (double)scrl.getViewport().getHeight() / (getMaxSample() - Math.min(0, getMinSample())));
    }
    if (hori) {
      newMagHor = 
        Math.max(MAG_HOR_MIN, (double)scrl.getViewport().getWidth() / (double)getSampleCount());
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
