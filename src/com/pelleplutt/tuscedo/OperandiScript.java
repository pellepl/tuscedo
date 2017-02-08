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

import com.pelleplutt.Essential;
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
import com.pelleplutt.tuscedo.ui.DrawPanel;
import com.pelleplutt.tuscedo.ui.GraphPanel;
import com.pelleplutt.tuscedo.ui.SimpleTabPane;
import com.pelleplutt.tuscedo.ui.SimpleTabPane.Tab;
import com.pelleplutt.tuscedo.ui.WorkArea;
import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.AppSystem.Disposable;
import com.pelleplutt.util.io.Port;

public class OperandiScript implements Runnable, Disposable {
  public static final String FN_SERIAL_INFO = "__serial_info";
  public static final String FN_SERIAL_DISCONNECT = "__serial_disconnect";
  public static final String FN_SERIAL_CONNECT = "__serial_connect";
  public static final String FN_SERIAL_TX = "__serial_tx";
  public static final String FN_SERIAL_ON_RX = "__serial_on_rx";
  public static final String FN_SERIAL_ON_RX_CLEAR = "__serial_on_rx_clear";
  public static final String FN_SERIAL_ON_RX_LIST = "__serial_on_rx_list";
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
  
//  // after compile and setExe
//  M fiskorv = new Processor.M(new MListMap());
//  M fiskorvEntry = new Processor.M(comp.getLinker().lookupFunctionAddress("println"));
//  fiskorvEntry.type = Processor.TFUNC;
//  fiskorv.ref.put("fu", fiskorvEntry);
//  int fiskorvAddr = comp.getLinker().lookupVariableAddress(null, "fiskorv");
//  proc.setMemory(fiskorvAddr, fiskorv);

  static Map<String, M> appVariables = new HashMap<String, M>();
  
  static {
    populateAppVariables();
  }
  
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
    extDefs.put("time", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        return new M((int)System.currentTimeMillis());
      }
    });
    extDefs.put("__tab_close", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        tab.getPane().removeTab(tab);
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
    createGraphFunctions(extDefs);
    createCanvasFunctions(extDefs);
    createSerialFunctions(extDefs);
    
    comp = new Compiler(extDefs, 0x4000, 0x0000);
    proc.reset();
  }
  
  Tab getTabByScriptId(M me) {
    if (me == null || me.type != Processor.TSET) return null;
    M mtabId = me.ref.get(new M(".tid"));
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
    injectAppVariables(comp);
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
    injectAppVariablesValues(wa, comp, proc);
    runProcessor();
  }
  
  void doCallAddress(WorkArea wa, int addr, List<M> args) {
    currentWA = wa;
    currentView = wa.getCurrentView();
    proc.resetAndCallAddress(addr, args, null);
    injectAppVariables(comp);
    injectAppVariablesValues(wa, comp, proc);
    runProcessor();
  }

  boolean stepInstr = false;
  void runProcessor() {
    try {
      running = true;
      String dbg = null;
      while (running) {
        if (halted) {
          currentWA.onScriptStart(proc);
          while (dbg == null || dbg.equals(lastSrcDbg)) {
            dbg = stepInstr ? proc.stepInstr() : proc.stepSrc();
            if (dbg == null) continue;
          }
          stepInstr = false;
          lastSrcDbg = dbg;
          currentWA.onScriptStop(proc);
          if (dbg != null) {
            int nestedIRQ = proc.getNestedIRQ();
            String irqInfo = nestedIRQ > 0 ? "[IRQ"+nestedIRQ+"] " : "";
            currentWA.appendViewText(currentView, irqInfo + dbg + "\n", WorkArea.STYLE_BASH_DBG);
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
  
  public void stepInstr() {
    synchronized (q) {
      stepInstr = true;
      q.notifyAll();
    }
  }
  
  public void interrupt(int addr) {
    currentWA.appendViewText(currentView, String.format("interrupt -> 0x%08x\n", addr), WorkArea.STYLE_BASH_INPUT);
    proc.raiseInterrupt(addr);
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
  
  public void dumpPC() {
    currentWA.appendViewText(currentView, String.format("PC:0x%08x\n", proc.getPC()), WorkArea.STYLE_BASH_INPUT);
  }
  
  public void dumpFP() {
    currentWA.appendViewText(currentView, String.format("FP:0x%08x\n", proc.getFP()), WorkArea.STYLE_BASH_INPUT);
  }
  
  public void dumpSP() {
    currentWA.appendViewText(currentView, String.format("SP:0x%08x\n", proc.getSP()), WorkArea.STYLE_BASH_INPUT);
  }
  
  public void dumpSR() {
    currentWA.appendViewText(currentView, String.format("SR:0x%08x\n", proc.getSR()), WorkArea.STYLE_BASH_INPUT);
  }
  
  public void dumpMe() {
    currentWA.appendViewText(currentView, String.format("me:%s\n", proc.getMe().asString()), WorkArea.STYLE_BASH_INPUT);
  }
  
  public void backtrace() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    proc.unwindStackTrace(ps);
    String bt = new String(baos.toByteArray(), StandardCharsets.UTF_8);
    AppSystem.closeSilently(ps);
    currentWA.appendViewText(currentView, bt, WorkArea.STYLE_BASH_INPUT);
  }
  
  private void createSerialFunctions(Map<String, ExtCall> extDefs) {
    extDefs.put(FN_SERIAL_INFO, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        return new M(currentWA.getConnectionInfo());
      }
    });
    extDefs.put(FN_SERIAL_DISCONNECT, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        String res = currentWA.getConnectionInfo();
        currentWA.closeSerial();
        return new M(res);
      }
    });
    extDefs.put(FN_SERIAL_CONNECT, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return new M(0);
        String c = args[0].asString();
        int atIx = c.indexOf('@');
        int divIx = c.indexOf('/');
        if (atIx > 0 && divIx > 0) {
          String c2 = c.substring(0, atIx) + " ";
          c2 += WorkArea.PORT_ARG_BAUD + c.substring(atIx+1, divIx) + " ";
          c2 += WorkArea.PORT_ARG_DATABITS + c.charAt(divIx+1) + " "; 
          c2 += WorkArea.PORT_ARG_STOPBITS + c.charAt(divIx+3) + " ";
          if (c.charAt(divIx+2) == 'E') c2 += WorkArea.PORT_ARG_PARITY + Port.PARITY_EVEN_S.toLowerCase();
          else if (c.charAt(divIx+2) == 'O') c2 += WorkArea.PORT_ARG_PARITY + Port.PARITY_ODD_S.toLowerCase();
          else c2 += WorkArea.PORT_ARG_PARITY + Port.PARITY_NONE_S.toLowerCase();
          c=c2;
        }
        boolean res = currentWA.handleOpenSerial(c);
        return new M(res ? 1 : 0);
      }
    });
    extDefs.put(FN_SERIAL_TX, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0 || !currentWA.getSerial().isConnected()) return new M(0);
        if (args[0].type == Processor.TSET) {
          MSet set = args[0].ref;
          boolean bytify = true;
          ByteArrayOutputStream baos = new ByteArrayOutputStream(set.size());
          for (int i = 0; i < set.size(); i++) {
            M e = set.get(i);
            if (e.type != Processor.TINT || e.type == Processor.TINT && (e.i < 0 || e.i > 255)) {
              bytify = false;
              break;
            } else {
              baos.write((byte)(e.i & 0xff));
            }
          }
          if (bytify) {
            currentWA.transmit(baos.toByteArray());
          } else {
            for (int i = 0; i < set.size(); i++) {
              currentWA.transmit(set.get(i).asString());
            }
          }
          AppSystem.closeSilently(baos);
        } else if (args[0].type == Processor.TINT && args[0].i >= 0 && args[0].i < 255) {
          currentWA.transmit(new byte[]{(byte)args[0].i});
        } else {
          currentWA.transmit(args[0].asString());
        }
        return new M(1);
      }
    });
    extDefs.put(FN_SERIAL_ON_RX, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 2) return null;
        if (args[1].type != Processor.TFUNC && args[1].type != Processor.TANON && args[1].type != Processor.TNIL) {
          throw new ProcessorError("second argument must be function or nil");
        }
        if (args[1].type == Processor.TNIL) {
          currentWA.removeSerialFilter(args[0].asString());
        } else {
          currentWA.registerSerialFilter(args[0].asString(), args[1].i);
        }
        return null;
      }
    });
    extDefs.put(FN_SERIAL_ON_RX_CLEAR, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        currentWA.clearSerialFilters();
        return null;
      }
    });
    extDefs.put(FN_SERIAL_ON_RX_LIST, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        List<WorkArea.RxFilter> filters = currentWA.getSerialFilters();
        MListMap listMap = new MListMap();
        for (WorkArea.RxFilter f :filters) {
          String func = comp.getLinker().lookupAddressFunction(f.addr);
          if (func == null) {
            func = String.format("0x%08x", f.addr);
          }
          listMap.put(f.filter, new M(func));
        }
        return new M(listMap);
      }
    });
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
        graph.ref.put(".tid", new M(tabID));
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
        graphFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__tab_close"));
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
  
  private void createCanvasFunctions(Map<String, ExtCall> extDefs) {
    extDefs.put("canvas", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        String name = "CANVAS";
        int w = 300;
        int h = 200;
        if (args != null && args.length > 0) {
          if (args[0].type == Processor.TSTR) {
            name = args[0].asString();
          } else {
            w = args[0].asInt();
            if (args.length > 1) {
              h = args[1].asInt();
            }
          }
        }
        
        String tabID = Tuscedo.inst().addCanvasTab(SimpleTabPane.getTabByComponent(currentWA).getPane(), w, h);
        Tuscedo.inst().getTab(tabID).setText(name);
        M canvas = new Processor.M(new MListMap());
        canvas.ref.put(".tid", new M(tabID));
        M canvasFunc;
        canvasFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__canvas_set_color"));
        canvasFunc.type = Processor.TFUNC;
        canvas.ref.put("set_color", canvasFunc);
        canvasFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__canvas_draw_line"));
        canvasFunc.type = Processor.TFUNC;
        canvas.ref.put("draw_line", canvasFunc);
        canvasFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__canvas_draw_rect"));
        canvasFunc.type = Processor.TFUNC;
        canvas.ref.put("draw_rect", canvasFunc);
        canvasFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__canvas_fill_rect"));
        canvasFunc.type = Processor.TFUNC;
        canvas.ref.put("fill_rect", canvasFunc);
        canvasFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__canvas_draw_oval"));
        canvasFunc.type = Processor.TFUNC;
        canvas.ref.put("draw_oval", canvasFunc);
        canvasFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__canvas_fill_oval"));
        canvasFunc.type = Processor.TFUNC;
        canvas.ref.put("fill_oval", canvasFunc);
        canvasFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__canvas_draw_text"));
        canvasFunc.type = Processor.TFUNC;
        canvas.ref.put("draw_text", canvasFunc);
        canvasFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__canvas_width"));
        canvasFunc.type = Processor.TFUNC;
        canvas.ref.put("get_width", canvasFunc);
        canvasFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__canvas_height"));
        canvasFunc.type = Processor.TFUNC;
        canvas.ref.put("get_height", canvasFunc);
        canvasFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__canvas_blit"));
        canvasFunc.type = Processor.TFUNC;
        canvas.ref.put("blit", canvasFunc);

        canvasFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__tab_title"));
        canvasFunc.type = Processor.TFUNC;
        canvas.ref.put("set_title", canvasFunc);
        canvasFunc = new Processor.M(comp.getLinker().lookupFunctionAddress("__tab_close"));
        canvasFunc.type = Processor.TFUNC;
        canvas.ref.put("close", canvasFunc);
        return canvas;
      }
    });
    extDefs.put("__canvas_set_color", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        ((DrawPanel)tab.getContent()).setColor(args[0].asInt());
        return null;
      }
    });
    extDefs.put("__canvas_draw_line", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 4)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        ((DrawPanel)tab.getContent()).drawLine(args[0].asInt(), args[1].asInt(), args[2].asInt(),args[3].asInt());
        return null;
      }
    });
    extDefs.put("__canvas_draw_rect", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 4)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        ((DrawPanel)tab.getContent()).drawRect(args[0].asInt(), args[1].asInt(), args[2].asInt(),args[3].asInt());
        return null;
      }
    });
    extDefs.put("__canvas_fill_rect", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || (args.length != 0 && args.length != 4))  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        if (args.length == 4) {
          ((DrawPanel)tab.getContent()).fillRect(args[0].asInt(), args[1].asInt(), args[2].asInt(),args[3].asInt());
        } else {
          ((DrawPanel)tab.getContent()).fillRect();
        }
        return null;
      }
    });
    extDefs.put("__canvas_draw_oval", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 4)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        ((DrawPanel)tab.getContent()).drawOval(args[0].asInt(), args[1].asInt(), args[2].asInt(),args[3].asInt());
        return null;
      }
    });
    extDefs.put("__canvas_fill_oval", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 4)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        ((DrawPanel)tab.getContent()).fillOval(args[0].asInt(), args[1].asInt(), args[2].asInt(),args[3].asInt());
        return null;
      }
    });
    extDefs.put("__canvas_draw_text", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 3)  return null;
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        ((DrawPanel)tab.getContent()).drawText(args[0].asInt(), args[1].asInt(), args[2].asString());
        return null;
      }
    });
    extDefs.put("__canvas_width", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        return new Processor.M(((DrawPanel)tab.getContent()).getWidth());
      }
    });
    extDefs.put("__canvas_height", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        return new Processor.M(((DrawPanel)tab.getContent()).getHeight());
      }
    });
    extDefs.put("__canvas_blit", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        Tab tab = getTabByScriptId(p.getMe());
        if (tab == null) return null;
        ((DrawPanel)tab.getContent()).blit();
        return null;
      }
    });
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
  
  static void populateAppVariables() {
    appVariables.put("__version", new M(Essential.vMaj + "." + Essential.vMin + "." + Essential.vMic));
    appVariables.put("ser", new M(null));
  }
  static void injectAppVariables(Compiler comp) {
    for (String var : appVariables.keySet()) {
      comp.injectGlobalVariable(null, var);
    }
  }
  static void injectAppVariablesValues(WorkArea wa, Compiler comp, Processor proc) {
    for (String var : appVariables.keySet()) {
      int varAddr = comp.getLinker().lookupVariableAddress(null, var);
      M val;
      if (var.equals("ser")) {
        val = new M(new MSerial(wa, comp));
      } else {
        val = appVariables.get(var);
      }
      proc.setMemory(varAddr, val);
    }
  }

}
