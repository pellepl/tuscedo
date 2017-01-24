package com.pelleplutt.tuscedo;

import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pelleplutt.operandi.Compiler;
import com.pelleplutt.operandi.CompilerError;
import com.pelleplutt.operandi.Executable;
import com.pelleplutt.operandi.Source;
import com.pelleplutt.operandi.proc.ExtCall;
import com.pelleplutt.operandi.proc.MListMap;
import com.pelleplutt.operandi.proc.MSet;
import com.pelleplutt.operandi.proc.Processor;
import com.pelleplutt.operandi.proc.Processor.M;
import com.pelleplutt.operandi.proc.ProcessorError;
import com.pelleplutt.operandi.proc.ProcessorError.ProcessorFinishedError;
import com.pelleplutt.tuscedo.ui.GraphPanel;
import com.pelleplutt.tuscedo.ui.SimpleTabPane;
import com.pelleplutt.tuscedo.ui.SimpleTabPane.Tab;
import com.pelleplutt.tuscedo.ui.WorkArea;
import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.AppSystem.Disposable;

public class OperandiScript implements Runnable, Disposable {

  Executable exe, pexe;
  Compiler comp;
  Processor proc;
  Map<String, ExtCall> extDefs = new HashMap<String, ExtCall>();
  volatile WorkArea currentWA;
  WorkArea.View currentView;
  volatile boolean running; 
  volatile boolean killed;
  volatile boolean halted;
  List<RunRequest> q = new ArrayList<RunRequest>();
  String lastSrcDbg;
  
//  // after reset, before compile
//  comp.injectGlobalVariable(null, "fiskorv");
//  // after compile and setExe
//  M fiskorv = new Processor.M(new MListMap());
//  M fiskorvEntry = new Processor.M(comp.getLinker().lookupFunctionAddress("println"));
//  fiskorvEntry.type = Processor.TFUNC;
//  fiskorv.ref.put("fu", fiskorvEntry);
//  int fiskorvAddr = comp.getLinker().lookupVariableAddress(null, "fiskorv");
//  proc.setMemory(fiskorvAddr, fiskorv);

  
  public OperandiScript() {
    proc = new Processor(0x10000);
    procReset();
    Thread t = new Thread(this, "operandi");
    t.setDaemon(true);
    t.start();
  }
  
  @Override
  public void run() {
    while (!killed) {
      synchronized (q) {
        if (q.isEmpty()) {
          AppSystem.waitSilently(q, 0);
        }
        if (q.isEmpty() || running) continue;
        
        RunRequest rr = q.remove(0);
        try {
          rr.wa.onScriptStart(proc);
          if (rr.src != null) {
            doRunScript(rr.wa, rr.src);
          } else {
            doCallAddress(rr.wa, rr.callAddr, rr.args);
          }
        } catch (Throwable t) {
          t.printStackTrace();
        } finally {
          rr.wa.onScriptStop(proc);
        }
      }
    }
  }


  void procReset() {
    exe = pexe = null;
    running = false;
    halted = false;
    lastSrcDbg = null;
    Processor.addCommonExtdefs(extDefs);
    extDefs.put("println", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          currentWA.appendViewText(currentWA.getCurrentView(), "\n", WorkArea.STYLE_BASH_OUT);
        } else {
          for (int i = 0; i < args.length; i++) {
            currentWA.appendViewText(currentWA.getCurrentView(), args[i].asString() + (i < args.length-1 ? " " : ""), 
                WorkArea.STYLE_BASH_OUT);
          }
        }
        currentWA.appendViewText(currentWA.getCurrentView(), "\n", WorkArea.STYLE_BASH_OUT);
        return null;
      }
    });
    extDefs.put("print", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
        } else {
          for (int i = 0; i < args.length; i++) {
            currentWA.appendViewText(currentWA.getCurrentView(), args[i].asString() + (i < args.length-1 ? " " : ""), 
                WorkArea.STYLE_BASH_OUT);
          }
        }
        return null;
      }
    });
    extDefs.put("sleep", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
        } else {
          AppSystem.sleep(args[0].asInt());
        }
        return null;
      }
    });
    createGraphFunctions(extDefs);
    
    comp = new Compiler(extDefs, 0x4000, 0x0000);
    proc.reset();
  }
  
  Tab getTabByScriptId(M me) {
    if (me == null || me.type != Processor.TSET) return null;
    M mtabId = me.ref.get(new M("__id"));
    if (mtabId == null || mtabId.type != Processor.TSTR) return null;
    Tab tab = Tuscedo.inst().getTab(mtabId.str);
    if (tab == null) return null;
    return tab;
  }

  public void runScript(WorkArea wa, String s) {
    synchronized (q) {
      q.add(new RunRequest(wa, new Source.SourceString("cli", s)));
      q.notifyAll();
    }
  }
  
  public void runScript(WorkArea wa, File f, String s) {
    synchronized (q) {
      q.add(new RunRequest(wa, new Source.SourceFile(f, s)));
      q.notifyAll();
    }
  }
  
  public void runFunc(WorkArea wa, int addr, List<M> args) {
    synchronized (q) {
      q.add(new RunRequest(wa, addr, args));
      q.notifyAll();
    }
  }
  
  void doRunScript(WorkArea wa, Source src) {
    currentWA = wa;
    currentView = wa.getCurrentView();
    proc.reset();
    try {
      exe = comp.compileIncrementally(src, pexe);
      pexe = exe;
    } catch (CompilerError ce) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos);
      comp.printCompilerError(ps, Compiler.getSource(), ce);
      String err = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      AppSystem.closeSilently(ps);
      wa.appendViewText(currentView, err, WorkArea.STYLE_BASH_ERR);
      return;
    }
    proc.setExe(exe);
    runProcessor();
  }
  
  void doCallAddress(WorkArea wa, int addr, List<M> args) {
    proc.resetAndCallAddress(addr, args, null);
    runProcessor();
  }

  void runProcessor() {
    try {
      running = true;
      String dbg = null;
      while (running) {
        if (halted) {
          currentWA.onScriptStart(proc);
          while (dbg == null || dbg.equals(lastSrcDbg)) {
            dbg = proc.stepSrc();
            if (dbg == null) continue;
          }
          lastSrcDbg = dbg;
          currentWA.onScriptStop(proc);
          if (dbg != null) {
            currentWA.appendViewText(currentView, dbg + "\n", WorkArea.STYLE_BASH_DBG);
          }
          synchronized (q) {
            AppSystem.waitSilently(q, 0);
          }
        } else {
          proc.step();
        }
      }
    } catch (ProcessorFinishedError pfe) {
      M m = pfe.getRet();
      if (m != null && m.type != Processor.TNIL) {
        currentWA.appendViewText(currentView, "script returned " + m.asString() + "\n", WorkArea.STYLE_BASH_OUT);
      }
    } catch (ProcessorError pe) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos);
      proc.dumpError(pe, ps);
      String err = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      AppSystem.closeSilently(ps);
      currentWA.appendViewText(currentView, err, WorkArea.STYLE_BASH_OUT);
    }
    finally {
      running = false;
    }
  }
  
  public void halt(boolean on) {
    lastSrcDbg = null;
    if (!running) return;
    halted = on;
    if (!halted) {
      synchronized (q) {
        q.notifyAll();
      }
    }
  }
  
  public void step() {
    synchronized (q) {
      q.notifyAll();
    }
  }
  
  public void reset() {
    if (running) {
      running = false;
      halted = false;
      synchronized (q) {
        q.notifyAll();
      }
      currentWA.appendViewText(currentView, "processor reset\n", WorkArea.STYLE_BASH_INPUT);
      backtrace();
      procReset();
    }
  }
  
  public void backtrace() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    proc.unwindStackTrace(ps);
    String bt = new String(baos.toByteArray(), StandardCharsets.UTF_8);
    AppSystem.closeSilently(ps);
    currentWA.appendViewText(currentView, bt, WorkArea.STYLE_BASH_INPUT);
  }
  
  
  private void createGraphFunctions(Map<String, ExtCall> extDefs) {
    extDefs.put("graph", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        String name = "GRAPH";
        int type = GraphPanel.GRAPH_LINE;
        List<Float> vals = null;
        if (args != null) {
          if (args.length > 0) {
            name = args[0].asString();
          }
          for (int i = 1; i < args.length; i++) {
            if (args[i].type == Processor.TSET) {
              vals = new ArrayList<Float>();
              MSet set = args[i].ref;
              for (int x = 0; x < set.size(); x++) {
                M val = set.get(x);
                vals.add(val.asFloat());
              }
            }
            else if (args[i].type == Processor.TSTR) {
              type = parseGraphType(args[i]);
            }
          }
        }
        
        String tabID = Tuscedo.inst().addGraphTab(SimpleTabPane.getTabByComponent(currentWA).getPane(), vals);
        ((GraphPanel)Tuscedo.inst().getTab(tabID).getContent()).setGraphType(type);;
        Tuscedo.inst().getTab(tabID).setText(name);
        M graph = new Processor.M(new MListMap());
        graph.ref.put("__id", new M(tabID));
        M graphFunc;
        graphFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__graph_add"));
        graphFunc.type = Processor.TFUNC;
        graph.ref.put("add", graphFunc);
        graphFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__graph_zoom_all"));
        graphFunc.type = Processor.TFUNC;
        graph.ref.put("zoom_all", graphFunc);
        graphFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__graph_zoom"));
        graphFunc.type = Processor.TFUNC;
        graph.ref.put("zoom", graphFunc);
        graphFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__graph_zoom_x"));
        graphFunc.type = Processor.TFUNC;
        graph.ref.put("zoom_x", graphFunc);
        graphFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__graph_zoom_y"));
        graphFunc.type = Processor.TFUNC;
        graph.ref.put("zoom_y", graphFunc);
        graphFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__graph_close"));
        graphFunc.type = Processor.TFUNC;
        graph.ref.put("close", graphFunc);
        graphFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__graph_type"));
        graphFunc.type = Processor.TFUNC;
        graph.ref.put("set_type", graphFunc);
        graphFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__tab_title"));
        graphFunc.type = Processor.TFUNC;
        graph.ref.put("set_title", graphFunc);
        graphFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__graph_size"));
        graphFunc.type = Processor.TFUNC;
        graph.ref.put("size", graphFunc);
        graphFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__graph_get"));
        graphFunc.type = Processor.TFUNC;
        graph.ref.put("get", graphFunc);
        graphFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__graph_scroll_x"));
        graphFunc.type = Processor.TFUNC;
        graph.ref.put("scroll_x", graphFunc);
        graphFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__graph_scroll_y"));
        graphFunc.type = Processor.TFUNC;
        graph.ref.put("scroll_y", graphFunc);
        graphFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__graph_scroll_sample"));
        graphFunc.type = Processor.TFUNC;
        graph.ref.put("scroll_sample", graphFunc);
        return graph;
      }
    });
    extDefs.put("__graph_add", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        if (args[0].type != Processor.TSET) {
          ((GraphPanel)tab.getContent()).addSample(args[0].asFloat());
        } else {
          for (int i = 0; i < args[0].ref.size(); i++) {
            ((GraphPanel)tab.getContent()).addSample(args[0].ref.get(i).asFloat());
          }
          
        }
        return null;
      }
    });
    extDefs.put("__graph_zoom_all", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        ((GraphPanel)tab.getContent()).zoomAll(true, true, new Point(0,0));
        return null;
      }
    });
    extDefs.put("__graph_zoom", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 2)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        ((GraphPanel)tab.getContent()).zoom(args[0].asFloat(), args[1].asFloat());
        return null;
      }
    });
    extDefs.put("__graph_zoom_x", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        ((GraphPanel)tab.getContent()).zoom(args[0].asFloat(), 0);
        return null;
      }
    });
    extDefs.put("__graph_zoom_y", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        ((GraphPanel)tab.getContent()).zoom(0, args[0].asFloat());
        return null;
      }
    });
    extDefs.put("__graph_close", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        tab.getPane().removeTab(tab);
        return null;
      }
    });
    extDefs.put("__graph_type", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        ((GraphPanel)tab.getContent()).setGraphType(parseGraphType(args[0]));
        return null;
      }
    });
    extDefs.put("__graph_size", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        return new M(((GraphPanel)tab.getContent()).getSampleCount());
      }
    });
    extDefs.put("__graph_get", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        return new M((float)((GraphPanel)tab.getContent()).getSample(args[0].asInt()));
      }
    });
    extDefs.put("__graph_scroll_x", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        ((GraphPanel)tab.getContent()).scrollToSampleX(args[0].asInt());
        return null;
      }
    });
    extDefs.put("__graph_scroll_y", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        ((GraphPanel)tab.getContent()).scrollToValY(args[0].asFloat());
        return null;
      }
    });
    extDefs.put("__graph_scroll_sample", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        ((GraphPanel)tab.getContent()).scrollToSample(args[0].asInt());
        return null;
      }
    });
    extDefs.put("__tab_title", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        tab.getPane().setTabTitle(tab, args[0].asString());
        return null;
      }
    });
  }
  
  int parseGraphType(M a) {
    int type = GraphPanel.GRAPH_LINE;
    if (a.asString().equalsIgnoreCase("plot")) {
      type = GraphPanel.GRAPH_PLOT;
    }
    else if (a.asString().equalsIgnoreCase("bar")) {
      type = GraphPanel.GRAPH_BAR;
    }
    return type;
  }
  
  @Override
  public void dispose() {
    running = false;
    synchronized (q) {
      killed = true;
      q.notifyAll();
    }
  }
  class RunRequest {
    public final WorkArea wa; 
    public final Source src;
    public final int callAddr;
    public List<M> args;
    public RunRequest(WorkArea wa, Source src) {
      this.wa = wa; this.src = src; this.callAddr = 0; this.args = null;
    }
    public RunRequest(WorkArea wa, int addr, List<M> args) {
      this.wa = wa; this.src = null; this.callAddr = addr; this.args = args;
    }
  }
  public boolean isRunning() {
    return running;
  }

  public int lookupFunc(String f) {
    return comp.getLinker().lookupFunctionAddress(f);
  }
}
