package com.pelleplutt.tuscedo;

import java.awt.Point;
import java.awt.Window;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import com.pelleplutt.Essential;
import com.pelleplutt.operandi.Compiler;
import com.pelleplutt.operandi.CompilerError;
import com.pelleplutt.operandi.Executable;
import com.pelleplutt.operandi.Source;
import com.pelleplutt.operandi.proc.ByteCode;
import com.pelleplutt.operandi.proc.ExtCall;
import com.pelleplutt.operandi.proc.MListMap;
import com.pelleplutt.operandi.proc.MSet;
import com.pelleplutt.operandi.proc.Processor;
import com.pelleplutt.operandi.proc.Processor.M;
import com.pelleplutt.operandi.proc.ProcessorError;
import com.pelleplutt.operandi.proc.ProcessorError.ProcessorFinishedError;
import com.pelleplutt.tuscedo.Tuscedo.TuscedoTabPane;
import com.pelleplutt.tuscedo.ui.UICanvasPanel;
import com.pelleplutt.tuscedo.ui.UIGraphPanel;
import com.pelleplutt.tuscedo.ui.UIGraphPanel.SampleSet;
import com.pelleplutt.tuscedo.ui.UIInfo;
import com.pelleplutt.tuscedo.ui.UIO;
import com.pelleplutt.tuscedo.ui.UISimpleTabPane;
import com.pelleplutt.tuscedo.ui.UISimpleTabPane.Tab;
import com.pelleplutt.tuscedo.ui.UIWorkArea;
import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.AppSystem.Disposable;
import com.pelleplutt.util.Log;
import com.pelleplutt.util.io.Port;

public class OperandiScript implements Runnable, Disposable {
  public static final String FN_SERIAL_INFO = "__serial_info";
  public static final String FN_SERIAL_DISCONNECT = "__serial_disconnect";
  public static final String FN_SERIAL_CONNECT = "__serial_connect";
  public static final String FN_SERIAL_TX = "__serial_tx";
  public static final String FN_SERIAL_ON_RX = "__serial_on_rx";
  public static final String FN_SERIAL_ON_RX_CLEAR = "__serial_on_rx_clear";
  public static final String FN_SERIAL_ON_RX_LIST = "__serial_on_rx_list";
  public static final String FN_SERIAL_LOG_START = "__serial_log_start";
  public static final String FN_SERIAL_LOG_AWAIT = "__serial_log_await";
  public static final String FN_SERIAL_LOG_STOP = "__serial_log_stop";
  public static final String FN_SERIAL_LOG_GET = "__serial_log_get";
  public static final String FN_SERIAL_LOG_SIZE = "__serial_log_size";
  public static final String FN_SERIAL_LOG_CLEAR = "__serial_log_clear";
  
  public static final String FN_NET_IFC = "__net_ifc";
  public static final String FN_NET_GET = "__net_get";
  public static final String FN_NET_LOCALHOST = "__net_localhost";

  public static final String FN_SYS_EXEC = "__sys_exec";
  public static final String FN_SYS_GET_HOME = "__sys_get_home";

  public static final String FN_DISK_LS = "__disk_ls";
  public static final String FN_DISK_FIND_FILE = "__disk_find";
  public static final String FN_DISK_READ = "__disk_read";
  public static final String FN_DISK_READB = "__disk_readb";
  public static final String FN_DISK_STAT = "__disk_stat";
  public static final String FN_DISK_MOVE = "__disk_move";
  public static final String FN_DISK_COPY = "__disk_copy";

  public static final String KEY_UI_ID = ".uio_id";
  public static final String FN_UI_CLOSE = "__ui_close";
  public static final String FN_UI_GET_NAME = "__ui_get_name";
  public static final String FN_UI_SET_NAME = "__ui_set_name";
  public static final String FN_UI_GET_ID = "__ui_get_id";
  public static final String FN_UI_GET_ANCESTOR = "__ui_ancestor";
  public static final String FN_UI_GET_PARENT = "__ui_parent";
  public static final String FN_UI_GET_CHILDREN = "__ui_children";

  Executable exe, pexe;
  Compiler comp;
  Processor proc;
  Map<String, ExtCall> extDefs = new HashMap<String, ExtCall>();
  volatile UIWorkArea currentWA;
  UIWorkArea.View currentView;
  volatile boolean running; 
  volatile boolean killed;
  volatile boolean halted;
  List<RunRequest> q = new ArrayList<RunRequest>();
  String lastSrcDbg;
  OperandiIRQHandler irqHandler;
  final Object logLock = new Object();
  volatile Thread logThread;
  InputStream login;
  ByteArrayOutputStream log;
  volatile byte[] logMatch;
  volatile int logMatchIx;
  volatile int logParseIx;

  static Map<String, M> appVariables = new HashMap<String, M>();
  
  static {
    populateAppVariables();
  }

  public OperandiScript() {
    proc = new Processor(0x10000);
    irqHandler = new OperandiIRQHandler(this, proc);
    proc.setIRQHandler(irqHandler);
    procReset();
    Thread t = new Thread(this, "operandi");
    t.setDaemon(true);
    t.start();
    irqHandler.installIRQHandlers(this);;
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

  public OperandiIRQHandler getIRQHandler() {
    return irqHandler;
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
          currentWA.appendViewText(currentWA.getCurrentView(), "\n", UIWorkArea.STYLE_BASH_OUT);
        } else {
          for (int i = 0; i < args.length; i++) {
            currentWA.appendViewText(currentWA.getCurrentView(), args[i].asString() + (i < args.length-1 ? " " : ""), 
                UIWorkArea.STYLE_BASH_OUT);
          }
        }
        currentWA.appendViewText(currentWA.getCurrentView(), "\n", UIWorkArea.STYLE_BASH_OUT);
        return null;
      }
    });
    extDefs.put("print", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
        } else {
          for (int i = 0; i < args.length; i++) {
            currentWA.appendViewText(currentWA.getCurrentView(), args[i].asString() + (i < args.length-1 ? " " : ""), 
                UIWorkArea.STYLE_BASH_OUT);
          }
        }
        return null;
      }
    });
    extDefs.put("__IRQ_consume_timer", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        Integer addr = irqHandler.consumeTimerIRQ(); 
        return addr == null ? null : new M(addr.intValue());
      }
    });
    extDefs.put("__IRQ_timer", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        return new M(irqHandler.hasTimerIRQ());
      }
    });
    extDefs.put("sleep", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        } 
        AppSystem.sleep(args[0].asInt());
        return null;
      }
    });
    extDefs.put("sqrt", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        } 
        return new M((float)Math.sqrt(args[0].asFloat()));
      }
    });
    extDefs.put("sin", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        } 
        return new M((float)Math.sin(args[0].asFloat()));
      }
    });
    extDefs.put("cos", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        } 
        return new M((float)Math.cos(args[0].asFloat()));
      }
    });
    extDefs.put("tan", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        } 
        return new M((float)Math.tan(args[0].asFloat()));
      }
    });
    extDefs.put("asin", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        } 
        return new M((float)Math.asin(args[0].asFloat()));
      }
    });
    extDefs.put("acos", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        } 
        return new M((float)Math.acos(args[0].asFloat()));
      }
    });
    extDefs.put("atan", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        } 
        return new M((float)Math.atan(args[0].asFloat()));
      }
    });
    extDefs.put("atan2", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length <= 1) {
          return null;
        } 
        return new M((float)Math.atan2(args[0].asFloat(),args[1].asFloat()));
      }
    });
    extDefs.put("time", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        return new M((int)System.currentTimeMillis());
      }
    });
    extDefs.put("base", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length <= 1) {
          return null;
        } 
        return new M(Integer.toString(args[0].asInt(),args[1].asInt()));
      }
    });
    extDefs.put("uitree", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        currentWA.appendViewText(currentWA.getCurrentView(), Tuscedo.inst().dumpUITree(), 
            UIWorkArea.STYLE_BASH_OUT);
        return null;
      }
    });
    extDefs.put("timer_start", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length != 3) {
          return null;
        } 
        final int timerAddr = args[2].i;
        Log.println("operandi add timer: in " +args[0].asInt() + "ms, rec " + args[1].asInt() + ", calling " + args[2]);
        Tuscedo.inst.getTimer().addTask(new Runnable() {
          @Override
          public void run() {
            if (running) getIRQHandler().callTimerIRQ(timerAddr);
          }
        }, args[0].asInt(), args[1].asInt());
        return null;
      }
    });
    extDefs.put(FN_UI_CLOSE, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO uio = getUIOByScriptId(p.getMe());
        if (uio != null) uio.getUIInfo().close();
        return null;
      }
    });
    extDefs.put(FN_UI_GET_ANCESTOR, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO uio = getUIOByScriptId(p.getMe());
        if (uio == null) return null;
        UIInfo parenInf = uio.getUIInfo().getAncestor();
        if (parenInf == null) return null;
        return createGenericMUIObj(parenInf.getUI());
      }
    });
    extDefs.put(FN_UI_GET_PARENT, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO uio = getUIOByScriptId(p.getMe());
        if (uio == null) return null;
        UIInfo parenInf = uio.getUIInfo().getParent();
        if (parenInf == null) return null;
        return createGenericMUIObj(parenInf.getUI());
      }
    });
    extDefs.put(FN_UI_GET_CHILDREN, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO uio = getUIOByScriptId(p.getMe());
        if (uio == null) return null;
        UIInfo uii = uio.getUIInfo();
        MSet msetc = new MListMap();
        for (UIInfo cuii : uii.children) {
          msetc.add(createGenericMUIObj(cuii.getUI()));
        }
        return new M(msetc);
      }
    });
    extDefs.put(FN_UI_SET_NAME, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        UIO uio = getUIOByScriptId(p.getMe());
        if (uio != null) {
          uio.getUIInfo().setName(args[0].asString());
          UIInfo anc = uio.getUIInfo().getAncestor();
          if (anc != null) {
            anc.getUI().repaint();
          }
        }
        return null;
      }
    });
    extDefs.put(FN_UI_GET_NAME, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO uio = getUIOByScriptId(p.getMe());
        if (uio != null) {
          return new M(uio.getUIInfo().getName());
        }
        return null;
      }
    });
    extDefs.put(FN_UI_GET_ID, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO uio = getUIOByScriptId(p.getMe());
        if (uio != null) {
          return new M(uio.getUIInfo().id);
        }
        return null;
      }
    });
    extDefs.put("__help", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        for (String k : extDefs.keySet()) {
          if (k.startsWith("__")) continue;
          currentWA.appendViewText(currentWA.getCurrentView(), k + "\n", UIWorkArea.STYLE_SERIAL_INFO);
        }
        for (String k : extDefs.keySet()) {
          if (!k.startsWith("__")) continue;
          currentWA.appendViewText(currentWA.getCurrentView(), k + "\n", UIWorkArea.STYLE_SERIAL_INFO);
        }
        return null;
      }
    });
    createGraphFunctions(extDefs);
    createCanvasFunctions(extDefs);
    createTuscedoTabPaneFunctions(extDefs);
    createSerialFunctions(extDefs);
    MNet.createNetFunctions(extDefs);
    MSys.createSysFunctions(extDefs);
    MDisk.createDiskFunctions(extDefs);
    
    comp = new Compiler(extDefs, 0x4000, 0x0000);
    proc.reset();
    irqHandler.reset();
  }
  
  UIO getUIOByScriptId(M me) {
    if (me == null || me.type != Processor.TSET) return null;
    M uioId = me.ref.get(new M(KEY_UI_ID));
    if (uioId == null || uioId.type != Processor.TSTR) return null;
    UIInfo inf = Tuscedo.inst().getUIObject(uioId.str); 
    UIO uio = inf == null ? null : inf.getUI();
    return uio;
  }
  
  public void runScript(UIWorkArea wa, String s) {
    synchronized (q) {
      q.add(new RunRequest(wa, new Source.SourceString("cli", s)));
      q.notifyAll();
    }
  }
  
  public void runScript(UIWorkArea wa, File f, String s) {
    synchronized (q) {
      q.add(new RunRequest(wa, new Source.SourceFile(f, s)));
      q.notifyAll();
    }
  }
  
  public void runFunc(UIWorkArea wa, int addr, List<M> args) {
    synchronized (q) {
      q.add(new RunRequest(wa, addr, args));
      q.notifyAll();
    }
  }
  
  void doRunScript(UIWorkArea wa, Source src) {
    currentWA = wa;
    currentView = wa == null ? null : wa.getCurrentView();
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
      if (wa != null) {
        wa.appendViewText(currentView, err, UIWorkArea.STYLE_BASH_ERR);
      } else {
        System.out.println(err);
      }
      return;
    }
    proc.setExe(exe);
    injectAppVariablesValues(wa, comp, proc);
    runProcessor();
  }
  
  void doCallAddress(UIWorkArea wa, int addr, List<M> args) {
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
            currentWA.appendViewText(currentView, irqInfo + dbg + "\n", 
                UIWorkArea.STYLE_BASH_DBG);
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
        currentWA.appendViewText(currentView, "script returned " + m.asString() + "\n", 
            UIWorkArea.STYLE_BASH_OUT);
      }
    } catch (ProcessorError pe) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos);
      proc.dumpError(pe, ps);
      String err = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      AppSystem.closeSilently(ps);
      currentWA.appendViewText(currentView, err, UIWorkArea.STYLE_BASH_OUT);
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
    currentWA.appendViewText(currentView, String.format("interrupt -> 0x%08x\n", addr), 
        UIWorkArea.STYLE_BASH_INPUT);
    proc.raiseInterrupt(addr);
  }
  
  public void reset() {
    if (running) {
      running = false;
      halted = false;
      synchronized (q) {
        q.notifyAll();
      }
      currentWA.appendViewText(currentView, "processor reset\n", UIWorkArea.STYLE_BASH_INPUT);
      backtrace();
      procReset();
    }
  }
  
  public void dumpPC() {
    currentWA.appendViewText(currentView, String.format("PC:0x%08x\n", proc.getPC()), 
        UIWorkArea.STYLE_BASH_INPUT);
  }
  
  public void dumpFP() {
    currentWA.appendViewText(currentView, String.format("FP:0x%08x\n", proc.getFP()), 
        UIWorkArea.STYLE_BASH_INPUT);
  }
  
  public void dumpSP() {
    currentWA.appendViewText(currentView, String.format("SP:0x%08x\n", proc.getSP()), 
        UIWorkArea.STYLE_BASH_INPUT);
  }
  
  public void dumpSR() {
    currentWA.appendViewText(currentView, String.format("SR:0x%08x\n", proc.getSR()), 
        UIWorkArea.STYLE_BASH_INPUT);
  }
  
  public void dumpMe() {
    M me = proc.getMe();
    currentWA.appendViewText(currentView, String.format("me:%s\n", me == null ? "nil" : me.asString()), 
        UIWorkArea.STYLE_BASH_INPUT);
  }
  
  public void backtrace() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    proc.unwindStackTrace(ps);
    String bt = new String(baos.toByteArray(), StandardCharsets.UTF_8);
    AppSystem.closeSilently(ps);
    currentWA.appendViewText(currentView, bt, UIWorkArea.STYLE_BASH_INPUT);
  }
  
  public void dumpStack() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    proc.printStack(ps, "  ", 50);
    String bt = new String(baos.toByteArray(), StandardCharsets.UTF_8);
    AppSystem.closeSilently(ps);
    currentWA.appendViewText(currentView, bt, UIWorkArea.STYLE_BASH_INPUT);
  }
  
  //
  // specific functions
  //
  
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
          c2 += UIWorkArea.PORT_ARG_BAUD + c.substring(atIx+1, divIx) + " ";
          c2 += UIWorkArea.PORT_ARG_DATABITS + c.charAt(divIx+1) + " "; 
          c2 += UIWorkArea.PORT_ARG_STOPBITS + c.charAt(divIx+3) + " ";
          if (c.charAt(divIx+2) == 'E') c2 += UIWorkArea.PORT_ARG_PARITY + Port.PARITY_EVEN_S.toLowerCase();
          else if (c.charAt(divIx+2) == 'O') c2 += UIWorkArea.PORT_ARG_PARITY + Port.PARITY_ODD_S.toLowerCase();
          else c2 += UIWorkArea.PORT_ARG_PARITY + Port.PARITY_NONE_S.toLowerCase();
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
        List<UIWorkArea.RxFilter> filters = currentWA.getSerialFilters();
        MListMap listMap = new MListMap();
        for (UIWorkArea.RxFilter f :filters) {
          String func = comp.getLinker().lookupAddressFunction(f.addr);
          if (func == null) {
            func = String.format("0x%08x", f.addr);
          }
          listMap.put(f.filter, new M(func));
        }
        return new M(listMap);
      }
    });
    extDefs.put(FN_SERIAL_LOG_START, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        synchronized (logLock) {
          if (logThread != null) return null;

          logMatchIx = 0;
          logParseIx = 0;
          logMatch = null;
          log = new ByteArrayOutputStream();
          login = currentWA.getSerial().attachSerialIO();
          DisposableRunnable task = new DisposableRunnable() {
            @Override
            public void run() {
              int c;
              try {
                while (logThread != null && (c = login.read()) != -1) {
                  log.write(c);
                  if (logMatch != null) {
                    if (logMatch[logMatchIx] == (byte)c) {
                      logMatchIx++;
                      if (logMatchIx >= logMatch.length) {
                        synchronized (logLock) {
                          logMatchIx = 0;
                          logMatch = null;
                          logParseIx = log.size();
                          logLock.notifyAll();
                        }
                      }                          
                    } else {
                      logMatchIx = 0;
                    }
                  }
                }
              } catch (IOException e) {}
              AppSystem.dispose(this);
            }
            
            @Override
            public void dispose() {
              synchronized (logLock) {
                logThread = null;
                AppSystem.closeSilently(login);
                logLock.notifyAll();
              }
            }
          };
          AppSystem.addDisposable(task);
          logThread = new Thread(task, "operandi-log");
          logThread.setDaemon(true);
          logThread.start();
        }
        return null;
      }
    });
    extDefs.put(FN_SERIAL_LOG_STOP, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        String s = null;
        synchronized (logLock) {
          s = log == null ? "" : log.toString();
          logThread = null;
          AppSystem.closeSilently(log);
          log = null;
          AppSystem.closeSilently(login);
          logLock.notifyAll();
        }
        return new M(s);
      }
    });
    extDefs.put(FN_SERIAL_LOG_AWAIT, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 1) return null;
        String await = args[0].asString();
        int timeout = args.length > 1 ? args[1].asInt() : 0;
        synchronized (logLock) {
          if (logThread == null) return new M(-1);
          
          logMatch = await.getBytes();
          logMatchIx = 0;

          // try things we have already
          byte[] data = log.toByteArray();
          for (int ix = logParseIx; ix < data.length; ix++) {
            if (logMatch[logMatchIx] == data[ix]) {
              logMatchIx++;
              if (logMatchIx >= logMatch.length) {
                logMatchIx = 0;
                logMatch = null;
                logParseIx = ix;
                return new M(logParseIx);
              }
            } else {
              logMatchIx = 0;
            }
          }
          
          // no match, await trigger
          logParseIx = data.length;
          AppSystem.waitSilently(logLock, timeout);
          if (logMatch == null) {
            // hit
            return new M(logParseIx);
          } else {
            // timeout
            logMatchIx = 0;
            logMatch = null;
            return new M(-1);
          }
        } // synchro
      }
    });
    extDefs.put(FN_SERIAL_LOG_SIZE, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        synchronized (logLock) {
          if (logThread == null) return new M(0);
          return new M(log.size());
        } // synchro
      }
    });
    extDefs.put(FN_SERIAL_LOG_GET, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        synchronized (logLock) {
          if (args.length == 0) return new M(log.toString());
          byte[] data = log.toByteArray();
          int offs = args[0].asInt();
          if (args.length == 1) {
            byte[] dst = new byte[data.length - offs];
            System.arraycopy(data, offs, dst, 0, data.length - offs);
            return new M(new String(dst));
          }
          if (args.length >= 2) {
            int len = args[1].asInt();
            int reallen = Math.min(data.length - offs, len);
            byte[] dst = new byte[reallen];
            System.arraycopy(data, offs, dst, 0, reallen);
            return new M(new String(dst));
          }
        } // synchro
        return null;
      }
    });
  }

  private void addUIMembers(MObj mobj, UIO uio) {
    mobj.putIntern(KEY_UI_ID, new M(uio.getUIInfo().getId()));
    M f;
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_GET_ID));
    f.type = Processor.TFUNC;
    mobj.putIntern("get_id", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_SET_NAME));
    f.type = Processor.TFUNC;
    mobj.putIntern("set_name", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_GET_NAME));
    f.type = Processor.TFUNC;
    mobj.putIntern("get_name", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_GET_PARENT));
    f.type = Processor.TFUNC;
    mobj.putIntern("get_parent", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_GET_CHILDREN));
    f.type = Processor.TFUNC;
    mobj.putIntern("get_children", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_GET_ANCESTOR));
    f.type = Processor.TFUNC;
    mobj.putIntern("get_ancestor", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_CLOSE));
    f.type = Processor.TFUNC;
    mobj.putIntern("close", f);
  }

  private M createGenericMUIObj(UIO uio) {
    if (uio == null) return null;
    MObj mobj;
    if (uio instanceof SampleSet) {
      mobj = createGraphMUIO();
    } else if (uio instanceof UICanvasPanel) {
      mobj = createCanvasMUIO();
    } else if (uio instanceof TuscedoTabPane) {
      mobj = createTuscedoTabPaneMUIO();
    } else {
      mobj = new MObj(currentWA, comp, "uiobject") {
        @Override
        public void init(UIWorkArea wa, Compiler comp) {
        }
      };
    }
    M mui = new M(mobj);
    addUIMembers(mobj, uio);
    return mui;
  }
    
  private MObj createGraphMUIO() {
    return new MObj(currentWA, comp, "graph") {
      @Override
      public void init(UIWorkArea wa, Compiler comp) {
        addFunc("add", "__graph_add", comp);
        addFunc("zoom_all", "__graph_zoom_all", comp);
        addFunc("zoom", "__graph_zoom", comp);
        addFunc("zoom_x", "__graph_zoom_x", comp);
        addFunc("zoom_y", "__graph_zoom_y", comp);
        addFunc("set_type", "__graph_type", comp);
        addFunc("count", "__graph_count", comp);
        addFunc("scroll_x", "__graph_scroll_x", comp);
        addFunc("scroll_y", "__graph_scroll_y", comp);
        addFunc("scroll_sample", "__graph_scroll_sample", comp);
        addFunc("set_mul", "__graph_set_mul", comp);
        addFunc("min", "__graph_min", comp);
        addFunc("max", "__graph_max", comp);
        addFunc("merge", "__graph_merge", comp);
      }
    };
  }
  
  private void createGraphFunctions(Map<String, ExtCall> extDefs) {
    extDefs.put("graph", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        String name = "GRAPH";
        int type = UIGraphPanel.GRAPH_LINE;
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
        
        Tab t = UISimpleTabPane.getTabByComponent(currentWA);
        if (t == null) {
          Tuscedo.inst().create(currentWA);
          t = UISimpleTabPane.getTabByComponent(currentWA);
        }
        String id = Tuscedo.inst().addGraphTab(t.getPane(), vals);
        SampleSet ui = ((SampleSet)Tuscedo.inst().getUIObject(id).getUI()); 
        ui.setGraphType(type);
        ui.getUIInfo().setName(name);
        MObj mobj = createGraphMUIO();
        M mui = new M(mobj);
        addUIMembers(mobj, ui);
        return mui;
      }
    });
    extDefs.put("__graph_add", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        if (args[0].type != Processor.TSET) {
          ss.addSample(args[0].asFloat());
        } else {
          for (int i = 0; i < args[0].ref.size(); i++) {
            ss.addSample(args[0].ref.get(i).asFloat());
          }
        }
        return null;
      }
    });
    extDefs.put("__graph_zoom_all", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        if (!((UIGraphPanel)ss.getUIInfo().getParent().getUI()).isUserZoomed()) {
          ((UIGraphPanel)ss.getUIInfo().getParent().getUI()).zoomAll(true, true, new Point(0,0));
        }
        return null;
      }
    });
    extDefs.put("__graph_zoom", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 2)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ((UIGraphPanel)ss.getUIInfo().getParent().getUI()).zoom(args[0].asFloat(), args[1].asFloat());
        return null;
      }
    });
    extDefs.put("__graph_zoom_x", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ((UIGraphPanel)ss.getUIInfo().getParent().getUI()).zoom(args[0].asFloat(), 0);
        return null;
      }
    });
    extDefs.put("__graph_zoom_y", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ((UIGraphPanel)ss.getUIInfo().getParent().getUI()).zoom(0, args[0].asFloat());
        return null;
      }
    });
    extDefs.put("__graph_type", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ss.setGraphType(parseGraphType(args[0]));
        return null;
      }
    });
    extDefs.put("__graph_count", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        return new M(ss.getSampleCount());
      }
    });
    extDefs.put("__graph_get", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        return new M((float)(ss.getSample(args[0].asInt())));
      }
    });
    extDefs.put("__graph_scroll_x", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ((UIGraphPanel)ss.getUIInfo().getParent().getUI()).scrollToSampleX(args[0].asInt());
        return null;
      }
    });
    extDefs.put("__graph_scroll_y", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ((UIGraphPanel)ss.getUIInfo().getParent().getUI()).scrollToValY(args[0].asFloat());
        return null;
      }
    });
    extDefs.put("__graph_scroll_sample", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ss.scrollToSample(args[0].asInt());
        return null;
      }
    });
    extDefs.put("__graph_set_mul", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ss.setMultiplier(args[0].asFloat());
        ss.repaint();
        return null;
      }
    });
    extDefs.put("__graph_min", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        return new M((float)ss.getMin());
      }
    });
    extDefs.put("__graph_max", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        return new M((float)ss.getMax());
      }
    });
    extDefs.put("__graph_merge", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;

        SampleSet sssrc = (SampleSet)getUIOByScriptId(p.getMe());
        if (sssrc == null) return null;

        UIGraphPanel src = ((UIGraphPanel)sssrc.getUIInfo().getParent().getUI());
        
        for (int i = 0; i < args.length; i++) {
          SampleSet ssover = (SampleSet)getUIOByScriptId(args[i]);
          if (ssover == null) continue;
          UIGraphPanel over = ((UIGraphPanel)ssover.getUIInfo().getParent().getUI());
          over.removeSampleSet(ssover);
          src.addSampleSet(ssover);
        }
        return null;
      }
    });
  }
  
  int parseGraphType(M a) {
    int type = UIGraphPanel.GRAPH_LINE;
    if (a.asString().equalsIgnoreCase("plot")) {
      type = UIGraphPanel.GRAPH_PLOT;
    }
    else if (a.asString().equalsIgnoreCase("bar")) {
      type = UIGraphPanel.GRAPH_BAR;
    }
    return type;
  }
  
  private MObj createCanvasMUIO() {
    return new MObj(currentWA, comp, "canvas") {
      @Override
      public void init(UIWorkArea wa, Compiler comp) {
        addFunc("set_color", "__canvas_set_color", comp);
        addFunc("draw_line", "__canvas_draw_line", comp);
        addFunc("draw_rect", "__canvas_draw_rect", comp);
        addFunc("fill_rect", "__canvas_fill_rect", comp);
        addFunc("draw_oval", "__canvas_draw_oval", comp);
        addFunc("fill_oval", "__canvas_fill_oval", comp);
        addFunc("draw_text", "__canvas_draw_text", comp);
        addFunc("get_width", "__canvas_width", comp);
        addFunc("get_height", "__canvas_height", comp);
        addFunc("__test", "__canvas_test", comp);
        addFunc("blit", "__canvas_blit", comp);
      }
    };
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
            if (args.length > 1) {
              w = args[1].asInt();
            }
            if (args.length > 2) {
              h = args[2].asInt();
            }
          } else {
            w = args[0].asInt();
            if (args.length > 1) {
              h = args[1].asInt();
            }
          }
        }
        
        String id = Tuscedo.inst().addCanvasTab(UISimpleTabPane.getTabByComponent(currentWA).getPane(), w, h);
        UICanvasPanel ui = ((UICanvasPanel)Tuscedo.inst().getUIObject(id).getUI()); 
        ui.getUIInfo().setName(name);
        
        MObj mobj = createCanvasMUIO();
        M mui = new M(mobj);
        addUIMembers(mobj, ui);
        return mui;
      }
    });
    extDefs.put("__canvas_set_color", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.setColor(args[0].asInt());
        return null;
      }
    });
    extDefs.put("__canvas_draw_line", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 4)  return null;
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.drawLine(args[0].asInt(), args[1].asInt(), args[2].asInt(),args[3].asInt());
        return null;
      }
    });
    extDefs.put("__canvas_draw_rect", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 4)  return null;
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.drawRect(args[0].asInt(), args[1].asInt(), args[2].asInt(),args[3].asInt());
        return null;
      }
    });
    extDefs.put("__canvas_fill_rect", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null)  return null;
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        if (args.length == 4) {
          cp.fillRect(args[0].asInt(), args[1].asInt(), args[2].asInt(),args[3].asInt());
        } else {
          cp.fillRect();
        }
        return null;
      }
    });
    extDefs.put("__canvas_draw_oval", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 4)  return null;
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.drawOval(args[0].asInt(), args[1].asInt(), args[2].asInt(),args[3].asInt());
        return null;
      }
    });
    extDefs.put("__canvas_fill_oval", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 4)  return null;
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.fillOval(args[0].asInt(), args[1].asInt(), args[2].asInt(),args[3].asInt());
        return null;
      }
    });
    extDefs.put("__canvas_draw_text", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 3)  return null;
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.drawText(args[0].asInt(), args[1].asInt(), args[2].asString());
        return null;
      }
    });
    extDefs.put("__canvas_width", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        return new Processor.M(cp.getWidth());
      }
    });
    extDefs.put("__canvas_height", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        return new Processor.M(cp.getHeight());
      }
    });
    extDefs.put("__canvas_blit", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.blit();
        return null;
      }
    });
    extDefs.put("__canvas_test", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.__test();
        return null;
      }
    });
  }

  private MObj createTuscedoTabPaneMUIO() {
    return new MObj(currentWA, comp, "pane") {
      @Override
      public void init(UIWorkArea wa, Compiler comp) {
        addFunc("set_pos", "__pane_set_pos", comp);
        addFunc("get_pos", "__pane_get_pos", comp);
        addFunc("set_size", "__pane_set_size", comp);
        addFunc("get_size", "__pane_get_size", comp);
      }
    };
  }
  
  private void createTuscedoTabPaneFunctions(Map<String, ExtCall> extDefs) {
    extDefs.put("__pane_set_pos", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 2)  return null;
        TuscedoTabPane tp= (TuscedoTabPane)getUIOByScriptId(p.getMe());
        if (tp == null) return null;
        Window w = SwingUtilities.getWindowAncestor(tp);
        w.setLocation(args[0].asInt(), args[1].asInt());
        return null;
      }
    });
    extDefs.put("__pane_get_pos", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        TuscedoTabPane tp= (TuscedoTabPane)getUIOByScriptId(p.getMe());
        if (tp == null) return null;
        Window w = SwingUtilities.getWindowAncestor(tp);
        MListMap l = new MListMap();
        l.add(new M(w.getLocation().x));
        l.add(new M(w.getLocation().y));
        return new M(l);
      }
    });
    extDefs.put("__pane_set_size", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 2)  return null;
        TuscedoTabPane tp= (TuscedoTabPane)getUIOByScriptId(p.getMe());
        if (tp == null) return null;
        Window w = SwingUtilities.getWindowAncestor(tp);
        w.setSize(args[0].asInt(), args[1].asInt());
        return null;
      }
    });
    extDefs.put("__pane_get_size", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        TuscedoTabPane tp= (TuscedoTabPane)getUIOByScriptId(p.getMe());
        if (tp == null) return null;
        Window w = SwingUtilities.getWindowAncestor(tp);
        MListMap l = new MListMap();
        l.add(new M(w.getSize().width));
        l.add(new M(w.getSize().height));
        return new M(l);
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
    public final UIWorkArea wa; 
    public final Source src;
    public final int callAddr;
    public List<M> args;
    public RunRequest(UIWorkArea wa, Source src) {
      this.wa = wa; this.src = src; this.callAddr = 0; this.args = null;
    }
    public RunRequest(UIWorkArea wa, int addr, List<M> args) {
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
    appVariables.put("__appversion", new M(Essential.vMaj + "." + Essential.vMin + "." + Essential.vMic));
    appVariables.put("__appname", new M(Essential.name));
    appVariables.put("__compilerversion", new M(Compiler.VERSION));
    appVariables.put("__bcversion", new M(ByteCode.VERSION));
    appVariables.put("__processorversion", new M(Processor.VERSION));
    M m = new M();
    m.type = Processor.TSET;
    appVariables.put("ser", m);
    appVariables.put("net", m);
    appVariables.put("sys", m);
    appVariables.put("conf", m);
    appVariables.put("disk", m);
  }
  static void injectAppVariables(Compiler comp) {
    for (String var : appVariables.keySet()) {
      comp.injectGlobalVariable(null, var);
    }
  }
  static void injectAppVariablesValues(UIWorkArea wa, Compiler comp, Processor proc) {
    for (String var : appVariables.keySet()) {
      int varAddr = comp.getLinker().lookupVariableAddress(null, var);
      M val;
      if (var.equals("ser")) {
        val = new M(new MSerial(wa, comp));
      } else if (var.equals("net")) {
        val = new M(new MNet(wa, comp));
      } else if (var.equals("sys")) {
        val = new M(new MSys(wa, comp));
      } else if (var.equals("conf")) {
        val = new M(new MConf(wa, comp));
      } else if (var.equals("disk")) {
        val = new M(new MDisk(wa, comp));
      } else {
        val = appVariables.get(var);
      }
      proc.setMemory(varAddr, val);
    }
  }
}
