package com.pelleplutt.tuscedo;

import static com.pelleplutt.tuscedo.OperandiIRQHandler.IRQ_BLOCK_SYSTEM;
import static com.pelleplutt.tuscedo.OperandiIRQHandler.IRQ_SYSTEM_TIMER;

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
import com.pelleplutt.operandi.proc.MMapRef;
import com.pelleplutt.operandi.proc.MSerializer;
import com.pelleplutt.operandi.proc.MSet;
import com.pelleplutt.operandi.proc.Processor;
import com.pelleplutt.operandi.proc.Processor.M;
import com.pelleplutt.operandi.proc.ProcessorError;
import com.pelleplutt.operandi.proc.ProcessorError.ProcessorFinishedError;
import com.pelleplutt.tuscedo.Tuscedo.TuscedoTabPane;
import com.pelleplutt.tuscedo.ui.Scene3D;
import com.pelleplutt.tuscedo.ui.UI3DPanel;
import com.pelleplutt.tuscedo.ui.UICanvasPanel;
import com.pelleplutt.tuscedo.ui.UICommon;
import com.pelleplutt.tuscedo.ui.UIGraphPanel;
import com.pelleplutt.tuscedo.ui.UIGraphPanel.SampleSet;
import com.pelleplutt.tuscedo.ui.UIInfo;
import com.pelleplutt.tuscedo.ui.UIInfo.UIListener;
import com.pelleplutt.tuscedo.ui.UIO;
import com.pelleplutt.tuscedo.ui.UISimpleTabPane;
import com.pelleplutt.tuscedo.ui.UISimpleTabPane.Tab;
import com.pelleplutt.tuscedo.ui.UIWorkArea;
import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.AppSystem.Disposable;
import com.pelleplutt.util.Log;
import com.pelleplutt.util.io.Port;

public class OperandiScript implements Runnable, Disposable {
  public static final String VAR_SERIAL = "ser";
  public static final String VAR_DISK = "disk";
  public static final String VAR_NET = "net";
  public static final String VAR_SYSTEM = "sys";
  public static final String VAR_CONF = "conf";
  public static final String VAR_INFO = "__info";
  
  public static final String FN_SERIAL_INFO = VAR_SERIAL + ":info";
  public static final String FN_SERIAL_DISCONNECT = VAR_SERIAL + ":disconnect";
  public static final String FN_SERIAL_CONNECT = VAR_SERIAL + ":connect";
  public static final String FN_SERIAL_TX = VAR_SERIAL + ":tx";
  public static final String FN_SERIAL_ON_RX = VAR_SERIAL + ":on_rx";
  public static final String FN_SERIAL_ON_RX_CLEAR = VAR_SERIAL + ":on_rx_clear";
  public static final String FN_SERIAL_ON_RX_LIST = VAR_SERIAL + ":on_rx_list";
  public static final String FN_SERIAL_LOG_START = VAR_SERIAL + ":log_start";
  public static final String FN_SERIAL_LOG_AWAIT = VAR_SERIAL + ":log_await";
  public static final String FN_SERIAL_LOG_STOP = VAR_SERIAL + ":log_stop";
  public static final String FN_SERIAL_LOG_GET = VAR_SERIAL + ":log_get";
  public static final String FN_SERIAL_LOG_SIZE = VAR_SERIAL + ":log_size";
  public static final String FN_SERIAL_LOG_CLEAR = VAR_SERIAL + ":log_clear";
  
  public static final String FN_NET_IFC = VAR_NET + ":ifc";
  public static final String FN_NET_GET = VAR_NET + ":get";
  public static final String FN_NET_LOCALHOST = VAR_NET + ":localhost";

  public static final String FN_SYS_EXEC = VAR_SYSTEM + ":exec";
  public static final String FN_SYS_GET_HOME = VAR_SYSTEM + ":get_home";

  public static final String FN_DISK_LS =  VAR_DISK + ":ls";
  public static final String FN_DISK_FIND_FILE =  VAR_DISK + ":find_file";
  public static final String FN_DISK_READ =  VAR_DISK + ":read";
  public static final String FN_DISK_READB =  VAR_DISK + ":readb";
  public static final String FN_DISK_WRITE =  VAR_DISK + ":write";
  public static final String FN_DISK_WRITEB =  VAR_DISK + ":writeb";
  public static final String FN_DISK_STAT =  VAR_DISK + ":stat";
  public static final String FN_DISK_MOVE =  VAR_DISK + ":mv";
  public static final String FN_DISK_COPY =  VAR_DISK + ":cp";
  public static final String FN_DISK_RM =  VAR_DISK + ":rm";
  public static final String FN_DISK_MKDIR =  VAR_DISK + ":mkdir";
  public static final String FN_DISK_TOUCH =  VAR_DISK + ":touch";

  public static final String FN_UI_CLOSE = "ui:close";
  public static final String FN_UI_GET_NAME = "ui:get_name";
  public static final String FN_UI_SET_NAME = "ui:set_name";
  public static final String FN_UI_GET_ID = "ui:get_id";
  public static final String FN_UI_GET_ANCESTOR = "ui:get_ancestor";
  public static final String FN_UI_GET_PARENT = "ui:get_parent";
  public static final String FN_UI_GET_CHILDREN = "ui:get_children";
  public static final String FN_UI_ON_MOUSE_PRESS = "ui:on_mouse_press";
  public static final String FN_UI_ON_MOUSE_RELEASE = "ui:on_mouse_release";
  public static final String FN_UI_ON_KEY_PRESS = "ui:on_key_press";
  public static final String FN_UI_ON_KEY_RELEASE = "ui:on_key_release";
  public static final String FN_UI_GET_MOUSE_PRESS = "ui:get_mouse_press";
  public static final String FN_UI_GET_MOUSE_RELEASE = "ui:get_mouse_release";
  public static final String FN_UI_GET_KEY_PRESS = "ui:get_key_press";
  public static final String FN_UI_GET_KEY_RELEASE = "ui:get_key_release";

  public static final String KEY_UI_ID = ".uio_id";

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

  public static final int PROC_HALT = 1;
  
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
  
  Map<Object, String> defhelp = new HashMap<Object, String>();
  
  public void setExtDef(String name, String help, ExtCall call) {
    extDefs.put(name, call);
    setExtHelp(name, help);
  }
  
  public void setExtHelp(String name, String help) {
    defhelp.put(name, help);
  }
  
  void procReset() {
    exe = pexe = null;
    running = false;
    halted = false;
    lastSrcDbg = null;
    Processor.addCommonExtdefs(extDefs);
    setExtDef("println", "(...) - prints arguments with newline", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          currentWA.appendViewText(currentWA.getCurrentView(), "\n", UICommon.STYLE_OP_OUT);
        } else {
          for (int i = 0; i < args.length; i++) {
            currentWA.appendViewText(currentWA.getCurrentView(), args[i].asString() + (i < args.length - 1 ? " " : ""),
                UICommon.STYLE_OP_OUT);
          }
        }
        currentWA.appendViewText(currentWA.getCurrentView(), "\n", UICommon.STYLE_OP_OUT);
        return null;
      }
    });
    setExtDef("print", "(...) - prints arguments", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
        } else {
          for (int i = 0; i < args.length; i++) {
            currentWA.appendViewText(currentWA.getCurrentView(), args[i].asString() + (i < args.length - 1 ? " " : ""),
                UICommon.STYLE_OP_OUT);
          }
        }
        return null;
      }
    });
    setExtDef("sleep", "(<time>) - sleeps given milliseconds", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        AppSystem.sleep(args[0].asInt());
        return null;
      }
    });
    setExtDef("sqrt", "(<x>) - returns square root of x", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        return new M((float) Math.sqrt(args[0].asFloat()));
      }
    });
    setExtDef("log", "(<x>) - returns natural logarithm of x", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        return new M((float) Math.log(args[0].asFloat()));
      }
    });
    setExtDef("log10", "(<x>) - returns 10 base logarithm of x", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        return new M((float) Math.log10(args[0].asFloat()));
      }
    });
    setExtDef("pow", "(<x>,<y>) - returns x powered by y.", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 2) {
          return null;
        }
        return new M((float) Math.pow(args[0].asFloat(), args[1].asFloat()));
      }
    });
    setExtDef("frac", "(<x>) - returns fractional part of x", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        float f = args[0].asFloat();
        return new M(f - (int) f);
      }
    });
    setExtDef("abs", "(<x>) - returns absolute of x", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        return new M((float) Math.abs(args[0].asFloat()));
      }
    });
    setExtDef("max", "(...) - returns maximum value", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        float max = Float.MIN_VALUE;
        for (M m : args) {
          float v;
          if (m.type == Processor.TSET) {
            v = __maxrec(m);
          } else {
            v = m.asFloat();
          }
          if (v > max)
            max = v;
        }
        return new M(max);
      }
    });
    setExtDef("min", "(...) - returns minimum value", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        float min = Float.MAX_VALUE;
        for (M m : args) {
          float v;
          if (m.type == Processor.TSET) {
            v = __minrec(m);
          } else {
            v = m.asFloat();
          }
          if (v < min)
            min = v;
        }
        return new M(min);
      }
    });
    setExtDef("sin", "(<x>) - returns sinus", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        return new M((float) Math.sin(args[0].asFloat()));
      }
    });
    setExtDef("cos", "(<x>) - returns cosinus", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        return new M((float) Math.cos(args[0].asFloat()));
      }
    });
    setExtDef("tan", "(<x>) - returns tangent", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        return new M((float) Math.tan(args[0].asFloat()));
      }
    });
    setExtDef("sinh", "(<x>) - returns hyperbolical sinus", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        return new M((float) Math.sinh(args[0].asFloat()));
      }
    });
    setExtDef("cosh", "(<x>) - returns hyperbolical cosinus", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        return new M((float) Math.cosh(args[0].asFloat()));
      }
    });
    setExtDef("tanh", "(<x>) - returns hyperbolical tangent", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        return new M((float) Math.tanh(args[0].asFloat()));
      }
    });
    setExtDef("asin", "(<x>) - returns arcsinus", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        return new M((float) Math.asin(args[0].asFloat()));
      }
    });
    setExtDef("acos", "(<x>) - returns arccosinus", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        return new M((float) Math.acos(args[0].asFloat()));
      }
    });
    setExtDef("atan", "(<x>) - returns arctangent", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        return new M((float) Math.atan(args[0].asFloat()));
      }
    });
    setExtDef("atan2", "(<y>, <x>) - returns 2-argument arctangent", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length <= 1) {
          return null;
        }
        return new M((float) Math.atan2(args[0].asFloat(), args[1].asFloat()));
      }
    });
    setExtDef("time", "() - returns current time in milliseconds", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        return new M((int) System.currentTimeMillis());
      }
    });
    setExtDef("base", "(<x>, <base>) - returns <x> in base <base>", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length <= 1) {
          return null;
        }
        return new M(Integer.toString(args[0].asInt(), args[1].asInt()));
      }
    });
    setExtDef("srlz", "(<x>) - stringifies x (see dsrlz)", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 1) {
          return null;
        }
        return new M(MSerializer.serialize(args[0]));
      }
    });
    setExtDef("dsrlz", "(<string>) - destringifies string (see srlz)", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 1) {
          return null;
        }
        return MSerializer.deserialize(args[0].asString());
      }
    });
    setExtDef("uitree", "() - dumps the ui tree", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        currentWA.appendViewText(currentWA.getCurrentView(), Tuscedo.inst().dumpUITree(), UICommon.STYLE_OP_DBG);
        return null;
      }
    });
    setExtDef("timer_start", "(<future_ms>, <recurrence_ms>, <func>) - starts a timer", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length != 3) {
          return null;
        }
        final int timerAddr = args[2].i;
        Log.println(
            "operandi add timer: in " + args[0].asInt() + "ms, rec " + args[1].asInt() + ", calling " + args[2]);
        Tuscedo.inst.getTimer().addTask(new Runnable() {
          @Override
          public void run() {
            if (running) {
              irqHandler.queue(IRQ_BLOCK_SYSTEM, IRQ_SYSTEM_TIMER).trigger(timerAddr);
            }
          }
        }, args[0].asInt(), args[1].asInt());
        return null;
      }
    });
    setExtDef("trap", "() - interrupts wfi", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        OperandiScript.this.getIRQHandler()
            .queue(OperandiIRQHandler.IRQ_BLOCK_SYSTEM, OperandiIRQHandler.IRQ_SYSTEM_USER).trigger(0);
        return null;
      }
    });
    setExtDef("__help", "() - dumps help struct", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length > 0) {
          String help = defhelp.get(args[0].asString());
          if (help != null) {
            currentWA.appendViewText(currentWA.getCurrentView(), args[0].asString() + help + "\n",
                UICommon.STYLE_OP_DBG);
          }
        } else {
          List<String> h = new ArrayList<>();
          for (String k : extDefs.keySet()) {
            if (k.contains(":"))
              continue;
            String help = defhelp.get(k);
            if (help == null)
              help = "";
            h.add(k + help);

          }
          h.sort(null);
          for (String s : h)
            currentWA.appendViewText(currentWA.getCurrentView(), s + "\n", UICommon.STYLE_OP_DBG);
          h.clear();

          for (String k : extDefs.keySet()) {
            if (!k.contains(":"))
              continue;
            String help = defhelp.get(k);
            if (help == null)
              help = "";
            h.add(k + help);
          }
          h.sort(null);
          for (String s : h)
            currentWA.appendViewText(currentWA.getCurrentView(), s + "\n", UICommon.STYLE_OP_DBG);
          h.clear();
        }
        return null;
      }
    });
    setExtDef("exit", "(<x>) - kills app", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        Tuscedo.onExit();
        System.exit((args == null || args.length == 0) ? 0 : (args[0].asInt()));
        return null;
      }
    });
    setExtDef("wfi", "(<x>) - waits for interrupt given milliseconds", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        return new M(awaitIRQ(args.length > 0 ? args[0].asInt() : 0) ? 1 : 0);
      }
    });

    createGraphFunctions();
    createCanvasFunctions();
    create3DFunctions();
    createUIFunctions();
    createTuscedoTabPaneFunctions();
    createSerialFunctions();
    MNet.createNetFunctions(this);
    MSys.createSysFunctions(this);
    MDisk.createDiskFunctions(this);
    getIRQHandler().createIRQFunctions(this);
    
    setExtHelp("rand", "() - return random 32-bit number");
    setExtHelp("randseed", "(<x>) - sets random seed");
    setExtHelp("cpy", "(<x>) - returns a copy of x");
    setExtHelp("byte", "(<x>) - returns x as a byte");
    setExtHelp("strstr", "(<string>, <pattern>(, <fromindex>)) - returns first index of pattern in string, or -1");
    setExtHelp("strstrr", "(<string>, <pattern>(, <fromindex>)) - returns last index of pattern in string, or -1");
    setExtHelp("lines", "(<string>) - returns an array of lines");
    setExtHelp("atoi", "(<x>) - returns x as a number");
    
    comp = new Compiler(extDefs, 0x4000, 0x0000);
    proc.reset();
    proc.user = 0;
    irqHandler.reset();
    irqHandler.installIRQHandlers(this);
  }
  
  float __maxrec(Processor.M m) {
    float max = Float.MIN_VALUE;
    if (m.type == Processor.TSET) {
      for (int f = 0; f < m.ref.size(); f++) {
        float v = __maxrec(m.ref.get(f));
        if (v > max) max = v;
      }
    } else {
      float v = m.asFloat();
      if (v > max) max = v;
    }
    return max;
  }

  float __minrec(Processor.M m) {
    float min = Float.MAX_VALUE;
    if (m.type == Processor.TSET) {
      for (int f = 0; f < m.ref.size(); f++) {
        float v = __minrec(m.ref.get(f));
        if (v < min) min = v;
      }
    } else {
      float v = m.asFloat();
      if (v < min) min = v;
    }
    return min;
  }

  UIO getUIOByScriptId(M me) {
    if (me == null || me.type != Processor.TSET) return null;
    M uioId = me.ref.get(new M(KEY_UI_ID));
    if (uioId == null || uioId.type != Processor.TSTR) return null;
    UIInfo inf = Tuscedo.inst().getUIObject(uioId.str); 
    UIO uio = inf == null ? null : inf.getUI();
    return uio;
  }
  
  public void runOperandiInitScripts(UIWorkArea wa) {
    String path = System.getProperty("user.home") + File.separator + 
        Essential.userSettingPath + File.separator;
    List<File> files = AppSystem.findFiles(path, "init-*.op", false);
    System.out.println("running operandi init-*.op files in " + path + ":" + files);
    for (File f : files) {
      String s = AppSystem.readFile(f);
      if (s != null) {
        runScript(wa, f, s);
      }
    }
  }

  public void runScript(UIWorkArea wa, String s) {
    if (s.startsWith("#reset") && s.length() < 8) {
      resetForce();
      runOperandiInitScripts(wa);
    } else if (s.startsWith("#load ")) {
      String fullpath = s.substring("#load ".length()).trim();
      int pathDelim = fullpath.lastIndexOf(File.separator);
      String path = pathDelim >= 0 ? fullpath.substring(0, pathDelim) : ".";
      String file = pathDelim >= 0 ? fullpath.substring(pathDelim+1) : fullpath;
      List<File> files = AppSystem.findFiles(path, file, false);
      System.out.println(path + " " + file + " " + files);
      for (File f : files) {
        String scr = AppSystem.readFile(f);
        if (scr != null) {
          runScript(wa, f, scr);
        }
      }
    } else {
      synchronized (q) {
        q.add(new RunRequest(wa, new Source.SourceString("cli", s)));
        q.notifyAll();
      }
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
        wa.appendViewText(currentView, err, UICommon.STYLE_OP_ERR);
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
                UICommon.STYLE_OP_DBG);
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
        currentWA.appendViewText(currentView, m.asString() + "\n", 
            UICommon.STYLE_OP_OUT);
      }
    } catch (ProcessorError pe) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos);
      proc.dumpError(pe, ps);
      String err = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      AppSystem.closeSilently(ps);
      currentWA.appendViewText(currentView, err, UICommon.STYLE_OP_ERR);
    }
    finally {
      running = false;
    }
  }
  
  public void halt(boolean on) {
    lastSrcDbg = null;
    if (!running) return;
    halted = on;
    if (halted) proc.user |= PROC_HALT;
    else        proc.user &= ~PROC_HALT;
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
        UICommon.STYLE_OP_DBG);
    proc.raiseInterrupt(addr);
  }
  
  public void reset() {
    if (running) {
      resetForce();
    }
  }
  
  public void resetForce() {
    boolean wasRunning = running;
    running = false;
    halted = false;
    synchronized (q) {
      q.notifyAll();
    }
    currentWA.appendViewText(currentView, "processor reset\n", UICommon.STYLE_OP_DBG);
    if (wasRunning) backtrace();
    procReset();
  }
  
  public void dumpPC() {
    currentWA.appendViewText(currentView, String.format("PC:0x%08x\n", proc.getPC()), 
        UICommon.STYLE_OP_DBG);
  }
  
  public void dumpFP() {
    currentWA.appendViewText(currentView, String.format("FP:0x%08x\n", proc.getFP()), 
        UICommon.STYLE_OP_DBG);
  }
  
  public void dumpSP() {
    currentWA.appendViewText(currentView, String.format("SP:0x%08x\n", proc.getSP()), 
        UICommon.STYLE_OP_DBG);
  }
  
  public void dumpSR() {
    currentWA.appendViewText(currentView, String.format("SR:0x%08x\n", proc.getSR()), 
        UICommon.STYLE_OP_DBG);
  }
  
  public void dumpMe() {
    M me = proc.getMe();
    currentWA.appendViewText(currentView, String.format("me:%s\n", me == null ? "nil" : me.asString()), 
        UICommon.STYLE_OP_DBG);
  }
  
  public void backtrace() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    proc.unwindStackTrace(ps);
    String bt = new String(baos.toByteArray(), StandardCharsets.UTF_8);
    AppSystem.closeSilently(ps);
    currentWA.appendViewText(currentView, bt, UICommon.STYLE_OP_DBG);
  }
  
  public void dumpStack() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    proc.printStack(ps, "  ", 50);
    String bt = new String(baos.toByteArray(), StandardCharsets.UTF_8);
    AppSystem.closeSilently(ps);
    currentWA.appendViewText(currentView, bt, UICommon.STYLE_OP_DBG);
  }
  
  public static final String DBG_HALT = "halt";
  public static final String DBG_HALT_SHORT = "h";
  public static final String DBG_CONT = "cont";
  public static final String DBG_CONT_SHORT = "c";
  public static final String DBG_NEXT = "next";
  public static final String DBG_NEXT_SHORT = "n";
  public static final String DBG_STEP = "step";
  public static final String DBG_STEP_SHORT = "s";
  public static final String DBG_BACK = "backtrace";
  public static final String DBG_BACK_SHORT = "bt";
  public static final String DBG_STACK = "stack";
  public static final String DBG_STACK_SHORT = "st";
  public static final String DBG_INT = "interrupt";
  public static final String DBG_INT_SHORT = "int";
  public static final String DBG_RES = "reset";
  public static final String DBG_RES_SHORT = "res";
  public static final String DBG_V_PC = "pc";
  public static final String DBG_V_FP = "fp";
  public static final String DBG_V_SP = "sp";
  public static final String DBG_V_ME = "me";
  public static final String DBG_V_SR = "sr";
  
  public void dumpDbgHelp() {
    currentWA.appendViewText(currentView,  ""
        + DBG_HALT + " (" + DBG_HALT_SHORT + ") - halt processor execution\n"
        + DBG_CONT + " (" + DBG_CONT_SHORT + ") - resume processor execution\n"
        + DBG_NEXT + " (" + DBG_NEXT_SHORT + ") - step source\n"
        + DBG_STEP + " (" + DBG_STEP_SHORT + ") - step instruction\n"
        + DBG_BACK + " (" + DBG_BACK_SHORT + ") - dumps backtrace\n"
        + DBG_STACK + " (" + DBG_STACK_SHORT + ") - dumps stack\n"
        + DBG_INT + " (" + DBG_INT_SHORT + ") - calls interrupt <funcname>\n"
        + DBG_RES + " (" + DBG_RES_SHORT + ") - resets processor\n"
        + DBG_V_PC + ", "+ DBG_V_FP + ", "+ DBG_V_SP + ", "+ DBG_V_ME + ", "+ DBG_V_SR + " - shows register\n"
        ,
        UICommon.STYLE_OP_DBG);
  }
  
  //
  // specific functions
  //
  
  private void createSerialFunctions() {
    setExtDef(FN_SERIAL_INFO, "() - returns current serial connection parameters",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        return new M(currentWA.getConnectionInfo());
      }
    });
    setExtDef(FN_SERIAL_DISCONNECT, "() - disconnects current serial connection",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        String res = currentWA.getConnectionInfo();
        currentWA.closeSerial();
        return new M(res);
      }
    });
    setExtDef(FN_SERIAL_CONNECT, "(<serialparams>) - connects to serial, returns non-zero on success",
        new ExtCall() {
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
    setExtDef(FN_SERIAL_TX, "(<string|data>) - transmits string, or raw byte if int, or raw bytes if array of ints",
        new ExtCall() {
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
    setExtDef(FN_SERIAL_ON_RX, "(<filter>, <func>) - executes function when filter is matched on a serial line of data, the function arguments will be (line, filter), if func is nil the filter is cleared",
        new ExtCall() {
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
    setExtDef(FN_SERIAL_ON_RX_CLEAR, "() - clears all filters",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        currentWA.clearSerialFilters();
        return null;
      }
    });
    setExtDef(FN_SERIAL_ON_RX_LIST, "() - returns current filters and corresponding functions",
        new ExtCall() {
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
    setExtDef(FN_SERIAL_LOG_START, "() - starts logging the serial input",
        new ExtCall() {
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
    setExtDef(FN_SERIAL_LOG_STOP, "() - stops logging the serial input, returns the captured log",
        new ExtCall() {
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
    setExtDef(FN_SERIAL_LOG_AWAIT, "(<filter>, <timeout>) - awaits until filter is found in log, returns index of filter, or -1 timeout",
        new ExtCall() {
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
    setExtDef(FN_SERIAL_LOG_SIZE, "() - returns the size of current captured log",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        synchronized (logLock) {
          if (logThread == null) return new M(0);
          return new M(log.size());
        } // synchro
      }
    });
    setExtDef(FN_SERIAL_LOG_GET, "() - returns current captured log",
        new ExtCall() {
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
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_ON_MOUSE_PRESS));
    f.type = Processor.TFUNC;
    mobj.putIntern("on_mouse_press", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_ON_MOUSE_RELEASE));
    f.type = Processor.TFUNC;
    mobj.putIntern("on_mouse_release", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_ON_KEY_PRESS));
    f.type = Processor.TFUNC;
    mobj.putIntern("on_key_press", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_ON_KEY_RELEASE));
    f.type = Processor.TFUNC;
    mobj.putIntern("on_key_release", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_GET_MOUSE_PRESS));
    f.type = Processor.TFUNC;
    mobj.putIntern("get_mouse_press", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_GET_MOUSE_RELEASE));
    f.type = Processor.TFUNC;
    mobj.putIntern("get_mouse_release", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_GET_KEY_PRESS));
    f.type = Processor.TFUNC;
    mobj.putIntern("get_key_press", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_GET_KEY_RELEASE));
    f.type = Processor.TFUNC;
    mobj.putIntern("get_key_release", f);
  }

  private M createGenericMUIObj(UIO uio) {
    if (uio == null) return null;
    MObj mobj;
    if (uio instanceof SampleSet) {
      mobj = createGraphMUIO();
    } else if (uio instanceof UICanvasPanel) {
      mobj = createCanvasMUIO();
    } else if (uio instanceof UI3DPanel) {
      mobj = create3DMUIO();
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
        addFunc("add", "graph:add", comp);
        addFunc("data", "graph:data", comp);
        addFunc("zoom_all", "graph:zoom_all", comp);
        addFunc("zoom", "graph:zoom", comp);
        addFunc("zoom_x", "graph:zoom_x", comp);
        addFunc("zoom_y", "graph:zoom_y", comp);
        addFunc("set_type", "graph:type", comp);
        addFunc("count", "graph:count", comp);
        addFunc("scroll_x", "graph:scroll_x", comp);
        addFunc("scroll_y", "graph:scroll_y", comp);
        addFunc("scroll_sample", "graph:scroll_sample", comp);
        addFunc("set_mul", "graph:set_mul", comp);
        addFunc("set_offs", "graph:set_offs", comp);
        addFunc("min", "graph:min", comp);
        addFunc("max", "graph:max", comp);
        addFunc("join", "graph:join", comp);
      }
    };
  }
  
  private void createGraphFunctions() {
    setExtDef("graph", "((<name>,),<data>,...,(<line|plot|bar>)) - opens a graph view", 
        new ExtCall() {
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
    setExtDef("graph:add", "(<data>) - adds data to graph, either as value or as list",
        new ExtCall() {
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
    setExtDef("graph:data", "() - returns all data of graph as a list",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        MSet msetc = new MListMap();
        for (double d : ss.getSamples()) {
          msetc.add(new M((float)d));
        }
        return new M(msetc);
      }
    });
    setExtDef("graph:zoom_all", "() - makes all data visible",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        if (!((UIGraphPanel)ss.getUIInfo().getParent().getUI()).isUserZoomed()) {
          ((UIGraphPanel)ss.getUIInfo().getParent().getUI()).zoomAll(true, true, new Point(0,0));
        }
        return null;
      }
    });
    setExtDef("graph:zoom", "(<horizontal>, <vertical>) - set zooming factors",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 2)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ((UIGraphPanel)ss.getUIInfo().getParent().getUI()).zoom(args[0].asFloat(), args[1].asFloat());
        return null;
      }
    });
    setExtDef("graph:zoom_x", "(<horizontal>) - set horizontal zooming factor",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ((UIGraphPanel)ss.getUIInfo().getParent().getUI()).zoom(args[0].asFloat(), 0);
        return null;
      }
    });
    setExtDef("graph:zoom_y", "(<vertical>) - set vertical zooming factor",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ((UIGraphPanel)ss.getUIInfo().getParent().getUI()).zoom(0, args[0].asFloat());
        return null;
      }
    });
    setExtDef("graph:type", "(<line|plot|bar>) - set graph type",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ss.setGraphType(parseGraphType(args[0]));
        return null;
      }
    });
    setExtDef("graph:count", "() - returns number of graph samples",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        return new M(ss.getSampleCount());
      }
    });
    setExtDef("graph:get","(<index>) - returns sample at index", 
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        return new M((float)(ss.getSample(args[0].asInt())));
      }
    });
    setExtDef("graph:scroll_x", "(<sampleindex>) - scrolls horizontally to sample index",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ((UIGraphPanel)ss.getUIInfo().getParent().getUI()).scrollToSampleX(args[0].asInt());
        return null;
      }
    });
    setExtDef("graph:scroll_y", "(<samplevalue>) - scrolls vertically to sample value",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ((UIGraphPanel)ss.getUIInfo().getParent().getUI()).scrollToValY(args[0].asFloat());
        return null;
      }
    });
    setExtDef("graph:scroll_sample", "(<sampleindex>) - scrolls to sample",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ss.scrollToSample(args[0].asInt());
        return null;
      }
    });
    setExtDef("graph:set_mul", "(<factor>) - set graph multiplication factor",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ss.setMultiplier(args[0].asFloat());
        ss.repaint();
        return null;
      }
    });
    setExtDef("graph:set_offs", "(<offset>) - set graph offset",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ss.setOffset(args[0].asFloat());
        ss.repaint();
        return null;
      }
    });
    setExtDef("graph:min", "() - returns minimum value",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        return new M((float)ss.getMin());
      }
    });
    setExtDef("graph:max", "() - returns maximum value",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        return new M((float)ss.getMax());
      }
    });
    setExtDef("graph:join", "(<graph>, ...) - join this graph with another, or others",
        new ExtCall() {
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
        addFunc("set_color", "canvas:set_color", comp);
        addFunc("draw_line", "canvas:draw_line", comp);
        addFunc("draw_rect", "canvas:draw_rect", comp);
        addFunc("fill_rect", "canvas:fill_rect", comp);
        addFunc("draw_oval", "canvas:draw_oval", comp);
        addFunc("fill_oval", "canvas:fill_oval", comp);
        addFunc("draw_text", "canvas:draw_text", comp);
        addFunc("get_width", "canvas:width", comp);
        addFunc("get_height", "canvas:height", comp);
        addFunc("blit", "canvas:blit", comp);
      }
    };
  }
  
  private void addOperandiUIListener(UIO ui) {
    ui.getUIInfo().addListener(new UIListener() {
      @Override
      public void onRemoved(UIO parent, UIO child) {
      }
      @Override
      public void onEvent(UIO uio, Object event) {
        if (event == UIInfo.EVENT_MOUSE_PRESS) {
          int addr = uio.getUIInfo().irqMousePressAddr;
          if (addr > 0) {
            OperandiScript.this.getIRQHandler()
            .queue(OperandiIRQHandler.IRQ_BLOCK_UI, OperandiIRQHandler.IRQ_UI_MOUSE_PRESS)
            .trigger(addr);
          }
        }
        else if (event == UIInfo.EVENT_MOUSE_RELEASE) {
          int addr = uio.getUIInfo().irqMouseReleaseAddr;
          if (addr > 0) {
            OperandiScript.this.getIRQHandler()
            .queue(OperandiIRQHandler.IRQ_BLOCK_UI, OperandiIRQHandler.IRQ_UI_MOUSE_RELEASE)
            .trigger(addr);
          }
        }
        else if (event == UIInfo.EVENT_KEY_PRESS) {
          int addr = uio.getUIInfo().irqKeyPressAddr;
          if (addr > 0) {
            OperandiScript.this.getIRQHandler()
            .queue(OperandiIRQHandler.IRQ_BLOCK_UI, OperandiIRQHandler.IRQ_UI_KEY_PRESS)
            .trigger(addr);
          }
        }
        else if (event == UIInfo.EVENT_KEY_RELEASE) {
          int addr = uio.getUIInfo().irqKeyReleaseAddr;
          if (addr > 0) {
            OperandiScript.this.getIRQHandler()
            .queue(OperandiIRQHandler.IRQ_BLOCK_UI, OperandiIRQHandler.IRQ_UI_KEY_RELEASE)
            .trigger(addr);
          }
        }
      }
      @Override
      public void onCreated(UIInfo obj) {
      }
      @Override
      public void onClosed(UIO parent, UIO child) {
      }
      @Override
      public void onAdded(UIO parent, UIO child) {
      }
    });
  }

  private void createCanvasFunctions() {
    setExtDef("canvas", "((<title>),(<w>),(<h>)) - creates a canvas",
        new ExtCall() {
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
        
        Tab tab = UISimpleTabPane.getTabByComponent(currentWA);
        if (tab == null) {
          Tuscedo.inst().create(currentWA);
          tab = UISimpleTabPane.getTabByComponent(currentWA);
        }
        String id = Tuscedo.inst().addCanvasTab(tab.getPane(), w, h);
        UICanvasPanel ui = ((UICanvasPanel)Tuscedo.inst().getUIObject(id).getUI()); 
        ui.getUIInfo().setName(name);
        
        MObj mobj = createCanvasMUIO();
        M mui = new M(mobj);
        addUIMembers(mobj, ui);
        addOperandiUIListener(ui);
        return mui;
      }
    });
    setExtDef("canvas:set_color", "(<color>) - sets current color",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.setColor(args[0].asInt());
        return null;
      }
    });
    setExtDef("canvas:draw_line", "(<x1>,<y1>,<x2>,<y2>) - draws a line",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 4)  return null;
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.drawLine(args[0].asInt(), args[1].asInt(), args[2].asInt(),args[3].asInt());
        return null;
      }
    });
    setExtDef("canvas:draw_rect", "(<x>,<y>,<w>,<h>) - draws a rectangle",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 4)  return null;
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.drawRect(args[0].asInt(), args[1].asInt(), args[2].asInt(),args[3].asInt());
        return null;
      }
    });
    setExtDef("canvas:fill_rect", "(<x>,<y>,<w>,<h>) - draws a filled rectangle",
        new ExtCall() {
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
    setExtDef("canvas:draw_oval", "(<x>,<y>,<w>,<h>) - draws an oval",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 4)  return null;
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.drawOval(args[0].asInt(), args[1].asInt(), args[2].asInt(),args[3].asInt());
        return null;
      }
    });
    setExtDef("canvas:fill_oval", "(<x>,<y>,<w>,<h>) - draws a filled oval",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 4)  return null;
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.fillOval(args[0].asInt(), args[1].asInt(), args[2].asInt(),args[3].asInt());
        return null;
      }
    });
    setExtDef("canvas:draw_text", "(<x>,<y>,<text>) - draws text",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 3)  return null;
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.drawText(args[0].asInt(), args[1].asInt(), args[2].asString());
        return null;
      }
    });
    setExtDef("canvas:width", "() - returns width",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        return new Processor.M(cp.getWidth());
      }
    });
    setExtDef("canvas:height", "() - returns height",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        return new Processor.M(cp.getHeight());
      }
    });
    setExtDef("canvas:blit", "() - blits changes to canvas",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.blit();
        return null;
      }
    });
  }
  
  private MObj create3DMUIO() {
    return new MObj(currentWA, comp, "graph3d") {
      @Override
      public void init(UIWorkArea wa, Compiler comp) {
        addFunc("get_width", "graph3d:width", comp);
        addFunc("get_height", "graph3d:height", comp);
        addFunc("set_pos", "graph3d:set_pos", comp);
        addFunc("set_view", "graph3d:set_view", comp);
        addFunc("set_size", "graph3d:set_size", comp);
        addFunc("set_model_heightmap", "graph3d:set_model_heightmap", comp);
        addFunc("set_model_heightmap_color", "graph3d:set_model_heightmap_color", comp);
        addFunc("set_model_cloud", "graph3d:set_model_cloud", comp);
        addFunc("set_model_cloud_color", "graph3d:set_model_cloud_color", comp);
        addFunc("blit", "graph3d:blit", comp);
      }
    };
  }

  private float[][] convHeightMap(M m) {
    // TODO check sizes etc, toss proper error desc
    MSet set = m.ref;
    int h = set.size();
    int w = set.get(0).ref.size();
    float[][] f = new float[w][h];
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        f[x][y] = m.ref.get(y).ref.get(x).asFloat();
      }
    }
    return f;
  }
  private float[][][] convHeightMapColor(M m) {
    // TODO check sizes etc, toss proper error desc
    MSet set = m.ref;
    int h = set.size();
    int w = set.get(0).ref.size();
    float[][][] f = new float[w][h][2];
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        MSet mset = m.ref.get(y).ref.get(x).ref;
        f[x][y] = new float[] {mset.get(0).asFloat(), 
            Scene3D.colToFloat(mset.get(1).asFloat(), mset.get(2).asFloat(), mset.get(3).asFloat())};
      }
    }
    return f;
  }
  private float[][][] convPointCloud(M m) {
    // TODO check sizes etc, toss proper error desc
    MSet set = m.ref;
    int d = set.size();
    int h = set.get(0).ref.size();
    int w = set.get(0).ref.get(0).ref.size();
    float[][][] f = new float[w][h][d];
    for (int z = 0; z < d; z++) {
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          f[x][y][z] = m.ref.get(z).ref.get(y).ref.get(x).asFloat();
        }
      }
    }
    return f;
  }
  private float[][][][] convPointCloudColor(M m) {
    // TODO check sizes etc, toss proper error desc
    MSet set = m.ref;
    int d = set.size();
    int h = set.get(0).ref.size();
    int w = set.get(0).ref.get(0).ref.size();
    float[][][][] f = new float[w][h][d][2];
    for (int z = 0; z < d; z++) {
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          MSet mset = m.ref.get(z).ref.get(y).ref.get(x).ref;
          f[x][y][z] = new float[] {mset.get(0).asFloat(), 
              Scene3D.colToFloat(mset.get(1).asFloat(), mset.get(2).asFloat(), mset.get(3).asFloat())};
        }
      }
    }
    return f;
  }
  private void create3DFunctions() {
    setExtDef("graph3d", "((<title>),(<w>),(<h>)) - creates a 3d graph",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        int w, h;
        String name = "GRAPH3D";
        Tab tab = UISimpleTabPane.getTabByComponent(currentWA);
        if (tab == null) {
          Tuscedo.inst().create(currentWA);
          tab = UISimpleTabPane.getTabByComponent(currentWA);
        }
        UISimpleTabPane tpane = null;
        tpane = tab.getPane();
        w = tpane.getWidth();
        h = tpane.getHeight() -tab.getHeight();
        float[][] model = new float[8][8];
        if (args != null && args.length > 0) {
          if (args[0].type == Processor.TSTR) {
            name = args[0].asString();
            if (args.length > 1) {
              M mmodel = args[1];
              model = convHeightMap(mmodel);
            }
            if (args.length > 2) {
              w = args[2].asInt();
            }
            if (args.length > 3) {
              h = args[3].asInt();
            }
          } else {
            M mmodel = args[0];
            model = convHeightMap(mmodel);
            if (args.length > 1) {
              w = args[1].asInt();
            }
            if (args.length > 2) {
              h = args[2].asInt();
            }
          }
        }
        
        w = ((w+99)/100)*100;
        
        String id = Tuscedo.inst().addGraph3dTab(tpane, w, h, 
            model);
        UI3DPanel ui = ((UI3DPanel)Tuscedo.inst().getUIObject(id).getUI()); 
        ui.getUIInfo().setName(name);
        
        MObj mobj = create3DMUIO();
        M mui = new M(mobj);
        addUIMembers(mobj, ui);
        return mui;
      }
    });
    setExtDef("graph3d:width", "() - returns width",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        return new Processor.M(cp.getWidth());
      }
    });
    setExtDef("graph3d:height", "() - returns height",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        return new Processor.M(cp.getHeight());
      }
    });
    setExtDef("graph3d:set_pos", "(<x>, <y>, <z>) - set beholders position",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.setPlayerPosition(args[0].asFloat(), args[1].asFloat(), args[2].asFloat());
        return null;
      }
    });
    setExtDef("graph3d:set_view", "(<yaw>, <pitch>, <roll>) - sets beholders viewing orientation",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.setPlayerView(args[0].asFloat(), args[1].asFloat(), args[2].asFloat());
        return null;
      }
    });
    setExtDef("graph3d:set_size", "(<width>, <height>) - sets viewport dimensions",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.setSize(args[0].asInt(), args[1].asInt());
        return null;
      }
    });
    setExtDef("graph3d:set_model_heightmap", "(<heightmap>) - sets heightmap data model (array of arrays of floats)",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.setHeightMap(convHeightMap(args[0]));
        return null;
      }
    });
    setExtDef("graph3d:set_model_heightmap_color", "(<heightmap-colored>) - sets colored heightmap data model (array of arrays of 4 float vector [height, red, green, blue])",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.setHeightMapColor(convHeightMapColor(args[0]));
        return null;
      }
    });
    setExtDef("graph3d:set_model_cloud", "(<cloud>, <isolevel>) - sets point cloud data model (array of arrays of arrays of floats)",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        float isolevel = 0.5f;
        boolean faceted = false;
        if (args.length > 1) isolevel = args[1].asFloat();
        if (args.length > 2) faceted = args[2].asInt() != 0;
        cp.setPointCloud(convPointCloud(args[0]), isolevel, faceted);
        return null;
      }
    });
    setExtDef("graph3d:set_model_cloud_color", "(<cloud>, <isolevel>) - sets colored point cloud data model (array of arrays of arrays of 4 float vector [height, red, green, blue]))",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        float isolevel = 0.5f;
        boolean faceted = false;
        if (args.length > 1) isolevel = args[1].asFloat();
        if (args.length > 2) faceted = args[2].asInt() != 0;
        cp.setPointCloudColor(convPointCloudColor(args[0]), isolevel, faceted);
        return null;
      }
    });
    setExtDef("graph3d:blit", "() - blits changes to graph",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.blit();
        return null;
      }
    });
  }

  private void createUIFunctions() {
    setExtDef(FN_UI_CLOSE,  "() - closes this ui instance", 
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO uio = getUIOByScriptId(p.getMe());
        if (uio != null) uio.getUIInfo().close();
        return null;
      }
    });
    setExtDef(FN_UI_GET_ANCESTOR,  "() - returns ancestor of this ui instance", 
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO uio = getUIOByScriptId(p.getMe());
        if (uio == null) return null;
        UIInfo parenInf = uio.getUIInfo().getAncestor();
        if (parenInf == null) return null;
        return createGenericMUIObj(parenInf.getUI());
      }
    });
    setExtDef(FN_UI_GET_PARENT,  "() - returns parent of this ui instance", 
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO uio = getUIOByScriptId(p.getMe());
        if (uio == null) return null;
        UIInfo parenInf = uio.getUIInfo().getParent();
        if (parenInf == null) return null;
        return createGenericMUIObj(parenInf.getUI());
      }
    });
    setExtDef(FN_UI_GET_CHILDREN,  "() - returns children of this ui instance", 
        new ExtCall() {
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
    setExtDef(FN_UI_SET_NAME,  "(<name>) - sets name of this ui instance", 
        new ExtCall() {
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
    setExtDef(FN_UI_GET_NAME, "() - returns name of this ui instance", 
         new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO uio = getUIOByScriptId(p.getMe());
        if (uio != null) {
          return new M(uio.getUIInfo().getName());
        }
        return null;
      }
    });
    setExtDef(FN_UI_GET_ID, "() - returns id of this ui instance", 
         new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO uio = getUIOByScriptId(p.getMe());
        if (uio != null) {
          return new M(uio.getUIInfo().id);
        }
        return null;
      }
    });
    setExtDef(FN_UI_ON_MOUSE_PRESS, "(<func>) - calls func on mouse press",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO cp = getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.getUIInfo().irqMousePressAddr = args[0].i;
        return null;
      }
    });
    setExtDef(FN_UI_ON_MOUSE_RELEASE, "(<func>) - calls func on mouse release",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO cp = getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.getUIInfo().irqMouseReleaseAddr = args[0].i;
        return null;
      }
    });
    setExtDef(FN_UI_ON_KEY_PRESS, "(<func>) - calls func on key press",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO cp = getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.getUIInfo().irqKeyPressAddr = args[0].i;
        return null;
      }
    });
    setExtDef(FN_UI_ON_KEY_RELEASE, "(<func>) - calls func on key release",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO cp = getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.getUIInfo().irqKeyReleaseAddr = args[0].i;
        return null;
      }
    });
    setExtDef(FN_UI_GET_MOUSE_PRESS, "() - returns last mouse press",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO cp = getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        MSet mres = new MListMap();
        UIInfo ui = cp.getUIInfo();
        mres.add(new M(ui.mousepressx));
        mres.add(new M(ui.mousepressy));
        mres.add(new M(ui.mousepressb));
        return new M(mres);
      }
    });
    setExtDef(FN_UI_GET_MOUSE_RELEASE, "() - returns last mouse release",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO cp = getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        MSet mres = new MListMap();
        UIInfo ui = cp.getUIInfo();
        mres.add(new M(ui.mouserelx));
        mres.add(new M(ui.mouserely));
        mres.add(new M(ui.mouserelb));
        return new M(mres);
      }
    });
    setExtDef(FN_UI_GET_KEY_PRESS, "() - returns last key press",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO cp = getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        UIInfo ui = cp.getUIInfo();
        return new M(ui.keypress);
      }
    });
    setExtDef(FN_UI_GET_KEY_RELEASE, "() - returns last key release",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO cp = getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        UIInfo ui = cp.getUIInfo();
        return new M(ui.keyrel);
      }
    });

  }


  private MObj createTuscedoTabPaneMUIO() {
    return new MObj(currentWA, comp, "pane") {
      @Override
      public void init(UIWorkArea wa, Compiler comp) {
        addFunc("set_pos", "pane:set_pos", comp);
        addFunc("get_pos", "pane:get_pos", comp);
        addFunc("set_size", "pane:set_size", comp);
        addFunc("get_size", "pane:get_size", comp);
      }
    };
  }
  
  private void createTuscedoTabPaneFunctions() {
    setExtDef("pane:set_pos", "(<x>, <y>) - sets position of this ui component", 
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 2)  return null;
        TuscedoTabPane tp= (TuscedoTabPane)getUIOByScriptId(p.getMe());
        if (tp == null) return null;
        Window w = SwingUtilities.getWindowAncestor(tp);
        w.setLocation(args[0].asInt(), args[1].asInt());
        return null;
      }
    });
    setExtDef("pane:get_pos",  "() - returns position of this ui component", 
        new ExtCall() {
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
    setExtDef("pane:set_size",  "(<w>, <h>) - sets size of this ui component", 
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 2)  return null;
        TuscedoTabPane tp= (TuscedoTabPane)getUIOByScriptId(p.getMe());
        if (tp == null) return null;
        Window w = SwingUtilities.getWindowAncestor(tp);
        w.setSize(args[0].asInt(), args[1].asInt());
        return null;
      }
    });
    setExtDef("pane:get_size",  "() - returns size of this ui component", 
        new ExtCall() {
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
    appVariables.put(VAR_SERIAL, m);
    appVariables.put(VAR_NET, m);
    appVariables.put(VAR_SYSTEM, m);
    appVariables.put(VAR_CONF, m);
    appVariables.put(VAR_DISK, m);
    appVariables.put(VAR_INFO, m);
  }
  void injectAppVariables(Compiler comp) {
    for (String var : appVariables.keySet()) {
      comp.injectGlobalVariable(null, var);
    }
  }
  void injectAppVariablesValues(UIWorkArea wa, Compiler comp, Processor proc) {
    for (String var : appVariables.keySet()) {
      int varAddr = comp.getLinker().lookupVariableAddress(null, var);
      M val;
      if (var.equals(VAR_SERIAL)) {
        val = new M(new MSerial(wa, comp));
      } else if (var.equals(VAR_NET)) {
        val = new M(new MNet(wa, comp));
      } else if (var.equals(VAR_SYSTEM)) {
        val = new M(new MSys(wa, comp));
      } else if (var.equals(VAR_CONF)) {
        val = new M(new MConf(wa, comp));
      } else if (var.equals(VAR_DISK)) {
        val = new M(new MDisk(wa, comp));
      } else if (var.equals(VAR_INFO)) {
        val = new M(new MMapRef(defhelp));
      } else {
        val = appVariables.get(var);
      }
      proc.setMemory(varAddr, val);
    }
  }

  private Object wfi = new Object();
  public void irqTriggered() {
    synchronized (wfi) {
      wfi.notify();
    }
  }
  public boolean awaitIRQ(long millisecs) {
    synchronized (wfi) {
      if (!irqHandler.pendingIRQ()) {
        AppSystem.waitSilently(wfi, millisecs);
      }
      return irqHandler.pendingIRQ();
    }
  }
}
