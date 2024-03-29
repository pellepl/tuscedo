package com.pelleplutt.tuscedo;

import static com.pelleplutt.tuscedo.OperandiIRQHandler.*;

import java.awt.*;
import java.io.*;
import java.lang.Math;
import java.nio.charset.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.joml.*;

import com.pelleplutt.*;
import com.pelleplutt.operandi.*;
import com.pelleplutt.operandi.Compiler;
import com.pelleplutt.operandi.proc.*;
import com.pelleplutt.operandi.proc.Processor.*;
import com.pelleplutt.operandi.proc.ProcessorError.*;
import com.pelleplutt.tuscedo.math.Complex;
import com.pelleplutt.tuscedo.math.Emd;
import com.pelleplutt.tuscedo.math.Fft;
import com.pelleplutt.tuscedo.ui.*;
import com.pelleplutt.tuscedo.ui.UIGraphPanel.*;
import com.pelleplutt.tuscedo.ui.UIInfo.*;
import com.pelleplutt.tuscedo.ui.UISimpleTabPane.*;
import com.pelleplutt.util.*;
import com.pelleplutt.util.AppSystem.*;
import com.pelleplutt.util.io.*;

public class OperandiScript implements Runnable, Disposable {
  public static final String VAR_SERIAL = "ser";
  public static final String VAR_DISK = "disk";
  public static final String VAR_NET = "net";
  public static final String VAR_AUDIO = "audio";
  public static final String VAR_SYSTEM = "sys";
  public static final String VAR_CONF = "conf";
  public static final String VAR_INFO = "__info";

  public static final String FN_SERIAL_INFO = VAR_SERIAL + ":info";
  public static final String FN_SERIAL_DISCONNECT = VAR_SERIAL + ":disconnect";
  public static final String FN_SERIAL_CONNECT = VAR_SERIAL + ":connect";
  public static final String FN_SERIAL_TX = VAR_SERIAL + ":tx";
  public static final String FN_SERIAL_SAVE = VAR_SERIAL + ":save";
  public static final String FN_SERIAL_ON_RX = VAR_SERIAL + ":on_rx";
  public static final String FN_SERIAL_ON_RX_CLEAR = VAR_SERIAL + ":on_rx_clear";
  public static final String FN_SERIAL_ON_RX_LIST = VAR_SERIAL + ":on_rx_list";
  public static final String FN_SERIAL_ON_RX_REPLAY = VAR_SERIAL + ":on_rx_replay";
  public static final String FN_SERIAL_LOG_START = VAR_SERIAL + ":log_start";
  public static final String FN_SERIAL_LOG_AWAIT = VAR_SERIAL + ":log_await";
  public static final String FN_SERIAL_LOG_STOP = VAR_SERIAL + ":log_stop";
  public static final String FN_SERIAL_LOG_GET = VAR_SERIAL + ":log_get";
  public static final String FN_SERIAL_LOG_SIZE = VAR_SERIAL + ":log_size";
  public static final String FN_SERIAL_LOG_CLEAR = VAR_SERIAL + ":log_clear";
  public static final String FN_SERIAL_SET_RTS_DTR = VAR_SERIAL + ":set_rts_dtr";
  public static final String FN_SERIAL_SET_FLOW_CONTROL = VAR_SERIAL + ":set_hw_flow";
  public static final String FN_SERIAL_SET_USER_HW_FLOW_CONTROL = VAR_SERIAL + ":set_user_hw_flow";

  public static final String FN_NET_IFC = VAR_NET + ":ifc";
  public static final String FN_NET_GET = VAR_NET + ":get";
  public static final String FN_NET_LOCALHOST = VAR_NET + ":localhost";

  public static final String FN_SYS_EXEC = VAR_SYSTEM + ":exec";
  public static final String FN_SYS_GET_HOME = VAR_SYSTEM + ":get_home";

  public static final String FN_DISK_LS =  VAR_DISK + ":ls";
  public static final String FN_DISK_FIND_FILE =  VAR_DISK + ":find_file";
  public static final String FN_DISK_READ =  VAR_DISK + ":read";
  public static final String FN_DISK_READB =  VAR_DISK + ":readb";
  public static final String FN_DISK_READAUDIO =  VAR_DISK + ":read_audio";
  public static final String FN_DISK_READVIDEO =  VAR_DISK + ":read_video";
  public static final String FN_DISK_WRITE =  VAR_DISK + ":write";
  public static final String FN_DISK_WRITEB =  VAR_DISK + ":writeb";
  public static final String FN_DISK_STAT =  VAR_DISK + ":stat";
  public static final String FN_DISK_MOVE =  VAR_DISK + ":mv";
  public static final String FN_DISK_COPY =  VAR_DISK + ":cp";
  public static final String FN_DISK_RM =  VAR_DISK + ":rm";
  public static final String FN_DISK_MKDIR =  VAR_DISK + ":mkdir";
  public static final String FN_DISK_TOUCH =  VAR_DISK + ":touch";
  public static final String FN_DISK_OPEN =  VAR_DISK + ":open";

  public static final String FN_FILE_READ = "file:read";
  public static final String FN_FILE_READLINE = "file:readline";
  public static final String FN_FILE_CLOSE = "file:close";

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
  public static final String FN_UI_MOUSE_PRESS_FUNC = "ui:mouse_press_func";
  public static final String FN_UI_MOUSE_RELEASE_FUNC = "ui:mouse_release_func";
  public static final String FN_UI_KEY_PRESS_FUNC = "ui:key_press_func";
  public static final String FN_UI_KEY_RELEASE_FUNC = "ui:key_release_func";
  public static final String FN_UI_GET_MOUSE_PRESS = "ui:get_mouse_press";
  public static final String FN_UI_GET_MOUSE_RELEASE = "ui:get_mouse_release";
  public static final String FN_UI_GET_KEY_PRESS = "ui:get_key_press";
  public static final String FN_UI_GET_KEY_RELEASE = "ui:get_key_release";

  public static final String FN_UI_PLACE_TAB_BEFORE = "ui:place_tab_before";
  public static final String FN_UI_PLACE_TAB_AFTER = "ui:place_tab_after";
  public static final String FN_UI_PLACE_TAB_FIRST = "ui:place_tab_first";
  public static final String FN_UI_PLACE_TAB_LAST = "ui:place_tab_last";
  public static final String FN_UI_DETACH_TAB = "ui:detach_tab";
  // TODO PETER
  public static final String FN_UI_DETACH_SPLIT = "ui:detach_split";

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
  volatile ByteArrayOutputStream log;
  volatile byte[] logMatch;
  volatile int logMatchIx;
  volatile int logParseIx;

  List<InputStream> filestreams;

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
            doRunScript(rr.wa, rr.src, rr.strargs);
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

  void initFilestreams() {
    if (filestreams != null) {
      for (InputStream i : filestreams) {
        if (i != null) AppSystem.closeSilently(i);
      }
    }
    filestreams = new ArrayList<InputStream>();
  }

  void procReset() {
    exe = pexe = null;
    running = false;
    halted = false;
    lastSrcDbg = null;
    initFilestreams();
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
        float max = -Float.MAX_VALUE;
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
    setExtDef("sum", "(...) - returns sum of values", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        float sum = 0;
        M count  = new M(0);
        for (M m : args) {
          float v;
          if (m.type == Processor.TSET) {
            v = __sumrec(m, count);
          } else {
            v = m.asFloat();
            count.i++;
          }
          sum += v;
        }
        MListMap r = new MListMap();
        r.makeMap();
        r.put("res", new Processor.M(sum));
        r.put("n", count);
        return new M(r);
      }
    });
    setExtDef("avg", "(...) - returns average of values", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) {
          return null;
        }
        float sum = 0;
        M count  = new M(0);
        for (M m : args) {
          float v;
          if (m.type == Processor.TSET) {
            v = __sumrec(m, count);
          } else {
            v = m.asFloat();
            count.i++;
          }
          sum += v;
        }
        MListMap r = new MListMap();
        r.makeMap();
        r.put("res", new Processor.M((sum/(float)count.i)));
        r.put("n", count);
        return new M(r);
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
    setExtDef("__help_add", "(<module>, <func>, <description>) - adds description to help", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        defhelp.put(args[0].asString() + "." + args[1].asString(), args[2].asString());
        return null;
      }
    });
    setExtDef("__help", "() - dumps help struct", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length > 0 && args[0].asString().equals("func")) {
          List<String> h = new ArrayList<>();
          for (String k : extDefs.keySet()) {
            if (k.contains(":") || k.contains("."))
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
            if (k.contains(":")) {
              String help = defhelp.get(k);
              if (help == null)
                help = "";
              h.add(k + help);
            }
          }
          h.sort(null);
          for (String s : h)
            currentWA.appendViewText(currentWA.getCurrentView(), s + "\n", UICommon.STYLE_OP_DBG);
          h.clear();

          for (Object key : defhelp.keySet()) {
            String k = key.toString();
            if (k.contains(".")) {
              String help = defhelp.get(k);
              if (help == null)
                help = "";
              h.add(k+"\n"+help);
            }
          }
          h.sort(null);
          for (String s : h)
            currentWA.appendViewText(currentWA.getCurrentView(), s + "\n", UICommon.STYLE_OP_DBG);
          h.clear();
        } else if (args.length > 0 && args[0].asString().equals("lang")) {
          try {
            String s = new String(AppSystem.getAppResource("init-op/operandi-ex.txt"));
            currentWA.appendViewText(currentWA.getCurrentView(), "\n" + s + "\n", UICommon.STYLE_OP_DBG);
          } catch (Throwable e) {
            e.printStackTrace();
          }
        } else {
          currentWA.appendViewText(currentWA.getCurrentView(), "Please use __help(\"func\") or __help(\"lang\")\n",
              UICommon.STYLE_OP_DBG);
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
    setExtDef("fft", "(<array>) - performs an fft of array", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length != 1 || args[0].type != Processor.TSET) {
          return null;
        }
        int sz = args[0].ref.size();
        Complex d[] = new Complex[sz];
        for (int f = 0; f < sz; f++) {
          if (args[0].ref.get(f).type == Processor.TSET) {
            d[f] = new Complex(args[0].ref.get(f).ref.get(0).asFloat(), args[0].ref.get(f).ref.get(1).asFloat());
          } else {
            d[f] = new Complex(args[0].ref.get(f).asFloat(), 0);
          }
        }
        Complex r[] = Fft.fft(d);
        MListMap mr = new MListMap();
        mr.makeArr();
        for (int f = 0; f < sz; f++) {
          MListMap mc = new MListMap();
          mc.makeArr();
          mc.add(new M((float)r[f].getReal()));
          mc.add(new M((float)r[f].getImaginary()));
          mr.add(new M(mc));

        }
        return new M(mr);
      }
    });
    setExtDef("ifft", "(<array>) - performs an inverse fft of array", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length != 1 || args[0].type != Processor.TSET) {
          return null;
        }
        int sz = args[0].ref.size();
        Complex d[] = new Complex[sz];
        for (int f = 0; f < sz; f++) {
          if (args[0].ref.get(f).type == Processor.TSET) {
            d[f] = new Complex(args[0].ref.get(f).ref.get(0).asFloat(), args[0].ref.get(f).ref.get(1).asFloat());
          } else {
            d[f] = new Complex(args[0].ref.get(f).asFloat(), 0);
          }
        }
        Complex r[] = Fft.ifft(d);
        MListMap mr = new MListMap();
        mr.makeArr();
        for (int f = 0; f < sz; f++) {
          MListMap mc = new MListMap();
          mc.makeArr();
          mc.add(new M((float)r[f].getReal()));
          mc.add(new M((float)r[f].getImaginary()));
          mr.add(new M(mc));

        }
        return new M(mr);
      }
    });
    setExtDef("emd", "(<array>, <order>, <iterations>, <locality>) - performs an empirical mode decomposition of array", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 1 || args[0].type != Processor.TSET) {
          return null;
        }
        int order = 5, iterations = 20, locality = 0;
        if (args.length > 1) order = args[1].asInt();
        if (args.length > 2) iterations = args[2].asInt();
        if (args.length > 3) locality = args[3].asInt();
        int sz = args[0].ref.size();
        float d[] = new float[sz];
        for (int f = 0; f < sz; f++) {
          d[f] = args[0].ref.get(f).asFloat();
        }
        Emd.EmdData res = Emd.decompose(d,  order, iterations, locality);
        MListMap mres = new MListMap();
        MListMap mresdecompositions[] = new MListMap[order];
        for (int i = 0; i < order; i++) {
          mresdecompositions[i] = new MListMap();
          mres.add(new M(mresdecompositions[i]));
        }
        for (int f = 0; f < sz; f++) {
          for (int i = 0; i < order; i++) {
            mresdecompositions[i].add(new M(res.imfs[i][f]));
          }
        }
        return new M(mres);
      }
    });

    createGraphFunctions();
    createCanvasFunctions();
    create3DFunctions();
    createGenericUIFunctions();
    createTuscedoTabPaneFunctions();
    createSerialFunctions();
    createWorkareaFunctions();
    MNet.createNetFunctions(this);
    MSys.createSysFunctions(this);
    MDisk.createDiskFunctions(this);
    MDisk.createFileFunctions(this);
    MAudio.createAudioFunctions(this);
    getIRQHandler().createIRQFunctions(this);

    setExtHelp("rand", "() - return random 32-bit number");
    setExtHelp("randseed", "(<x>) - sets random seed");
    setExtHelp("cpy", "(<x>) - returns a copy of x");
    setExtHelp("byte", "(<x>) - returns x as a byte");
    setExtHelp("strstr", "(<string>, <pattern>(, <fromindex>)) - returns first index of pattern in string, or -1");
    setExtHelp("strstrr", "(<string>, <pattern>(, <fromindex>)) - returns last index of pattern in string, or -1");
    setExtHelp("strextract", "(<string>, <pattern1>, <pattern2>, ...) - returns a vector of strings");
    setExtHelp("strnum", "(<string>) - returns first found number in string, or nil if none");
    setExtHelp("strnums", "(<string>) - returns a vector of all found numbers in given string");
    setExtHelp("strreplace", "(<string>, <map>) - returns a string where all occurences of map keys are replaced with corresponding value");
    setExtHelp("lines", "(<string>) - returns an array of lines");
    setExtHelp("atoi", "(<x>(, <base>)) - returns x as a number");
    setExtHelp("sort", "(<array>, (<key> | <ix>)) - sorts given array, either by natural order or by given key/index when array contains sets");
    setExtHelp("group", "(<array>, (<key> | <ix>)) - returns given array grouped either by natural order or by given key/index when array contains sets");

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

  float __sumrec(Processor.M m, Processor.M count) {
    float sum = 0;
    if (m.type == Processor.TSET) {
      for (int f = 0; f < m.ref.size(); f++) {
        float s = __sumrec(m.ref.get(f), count);
        sum += s;
      }
    } else {
      sum = m.asFloat();
      count.i++;
    }
    return sum;
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
        runScript(wa, f, s, null);
      }
    }
  }

  public void runScript(UIWorkArea wa, String s) {
    if (s.startsWith("#reset") && s.length() < 8) {
      resetForce();
      runOperandiInitScripts(wa);
    } else if (s.startsWith("#load ")) {
      int pathEnd = s.indexOf('(');
      boolean gotArgs = true;
      if (pathEnd < 0 || s.lastIndexOf(')') < 0 ) {
        pathEnd = s.length()-1;
        gotArgs = false;
      }
      String fullpath = s.substring("#load ".length(),pathEnd).trim();
      int pathDelim = fullpath.lastIndexOf(File.separator);
      String path = pathDelim >= 0 ? fullpath.substring(0, pathDelim) : ".";
      String file = pathDelim >= 0 ? fullpath.substring(pathDelim+1) : fullpath;
      List<File> files = AppSystem.findFiles(path, file, false);
      String argString = gotArgs ? s.substring(pathEnd+1, s.length()-2) : "";
      String[] argsString = argString.split(",");

      System.out.println(path + " " + file + " " + files + " (" + argString + ")");
      for (File f : files) {
        String scr = AppSystem.readFile(f);
        if (scr != null) {
          runScript(wa, f, scr, argsString);
        }
      }
    } else {
      synchronized (q) {
        q.add(new RunRequest(wa, new Source.SourceString("cli", s), null));
        q.notifyAll();
      }
    }
  }

  public void runScript(UIWorkArea wa, File f, String ignored, String[] args) {
    synchronized (q) {
      q.add(new RunRequest(wa, new Source.SourceFile(f, ignored), args));
      q.notifyAll();
    }
  }

  public void runScript(UIWorkArea wa, String src, String origin, String[] args) {
    synchronized (q) {
      q.add(new RunRequest(wa, new Source.SourceString(origin, src), args));
      q.notifyAll();
    }
  }

  public void runFunc(UIWorkArea wa, int addr, List<M> args) {
    synchronized (q) {
      q.add(new RunRequest(wa, addr, args));
      q.notifyAll();
    }
  }

  void doRunScript(UIWorkArea wa, Source src, String[] args) {
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
    proc.setExe(exe, args);
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
        UIWorkArea wa = ((MObj)p.getMe().ref).workarea;
        return new M(wa.getConnectionInfo());
      }
    });
    setExtDef(FN_SERIAL_SET_RTS_DTR, "(<rts>, <dtr>) - sets rts and dtr lines high or low",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIWorkArea wa = ((MObj)p.getMe().ref).workarea;
        boolean rtshigh = args.length > 0 && (args[0].asInt() == 1 || args[0].asString().equals("on") || args[0].asString().equals("high") || args[0].asString().equals("true"));
        boolean dtrhigh = args.length > 1 && (args[1].asInt() == 1 || args[1].asString().equals("on") || args[1].asString().equals("high") || args[1].asString().equals("true"));
        try {
          wa.getSerial().setRTSDTR(rtshigh, dtrhigh);
        } catch (IOException e) {
          e.printStackTrace();
        }
        return null;
      }
    });
    setExtDef(FN_SERIAL_SET_USER_HW_FLOW_CONTROL, "(<rts-setting>, <dtr-setting>) - 0:disable, 1:constant low, 2:constant high, 3:low flank during send, 4:high flank during send",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIWorkArea wa = ((MObj)p.getMe().ref).workarea;
        wa.getSerial().setUserHwFlowControl(args[0].asInt(), args[1].asInt());
        return null;
      }
    });
    setExtDef(FN_SERIAL_SET_FLOW_CONTROL, "(<setting>) - 0:none, 1:xon/xoff, 2:rts/cts, 3:xon/xoff+rts/cts, 4:dsr/dtr, 5:xon/xoff+dsr/dtr, 6:rts/cts+dsr/dtr, 7:xon/xoff+rts/cts+dsr/dtr",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIWorkArea wa = ((MObj)p.getMe().ref).workarea;
        wa.getSerial().setFlowControl(args[0].asInt());
        return null;
      }
    });
    setExtDef(FN_SERIAL_DISCONNECT, "() - disconnects current serial connection",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIWorkArea wa = ((MObj)p.getMe().ref).workarea;
        String res = wa.getConnectionInfo();
        wa.closeSerial();
        return new M(res);
      }
    });
    setExtDef(FN_SERIAL_CONNECT, "(<serialparams>) - connects to serial, returns non-zero on success. Supports \"<serial>@115200/8N1\" format, but also \"stdio\", \"sock://<server>:<port>\", and \"file://<path>\".",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIWorkArea wa = ((MObj)p.getMe().ref).workarea;
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
        boolean res = wa.handleOpenSerial(c, false);
        return new M(res ? 1 : 0);
      }
    });
    setExtDef(FN_SERIAL_TX, "(<string|data>) - transmits string, or raw byte if int, or raw bytes if array of ints",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIWorkArea wa = ((MObj)p.getMe().ref).workarea;
        if (args == null || args.length == 0 || !wa.getSerial().isConnected()) return new M(0);
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
            wa.transmit(baos.toByteArray());
          } else {
            for (int i = 0; i < set.size(); i++) {
              wa.transmit(set.get(i).asString());
            }
          }
          AppSystem.closeSilently(baos);
        } else if (args[0].type == Processor.TINT && args[0].i >= 0 && args[0].i < 255) {
          wa.transmit(new byte[]{(byte)args[0].i});
        } else {
          wa.transmit(args[0].asString());
        }
        return new M(1);
      }
    });
    setExtDef(FN_SERIAL_SAVE, "(<filename>) - stores (selection of) serial log",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIWorkArea wa = ((MObj)p.getMe().ref).workarea;
        if (args == null || args.length == 0) return null;
        String path = args[0].asString();
        return new M(AppSystem.writeFile(new File(path), wa.getSerialView().getSelectedText()) ? 0 : -1);
      }
    });
    setExtDef(FN_SERIAL_ON_RX, "(<filter>, <func>) - executes function when regex filter is matched on a serial line of data, the function arguments will be (line, filter, (groups)*), if func is nil the filter is cleared",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIWorkArea wa = ((MObj)p.getMe().ref).workarea;
        if (args == null || args.length < 2) return null;
        if (args[1].type != Processor.TFUNC && args[1].type != Processor.TANON && args[1].type != Processor.TNIL) {
          throw new ProcessorError("second argument must be function or nil");
        }
        if (args[1].type == Processor.TNIL) {
          wa.removeSerialFilter(args[0].asString());
        } else {
          try {
            wa.registerSerialFilter(args[0].asString(), args[1].i);
          } catch (RuntimeException re) {
            throw new ProcessorError("bad regex: " + re.getCause().getMessage());
          }
        }
        return null;
      }
    });
    setExtDef(FN_SERIAL_ON_RX_CLEAR, "() - clears all filters",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIWorkArea wa = ((MObj)p.getMe().ref).workarea;
        wa.clearSerialFilters();
        return null;
      }
    });
    setExtDef(FN_SERIAL_ON_RX_LIST, "() - returns current filters and corresponding functions",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIWorkArea wa = ((MObj)p.getMe().ref).workarea;
        List<UIWorkArea.RxFilter> filters = wa.getSerialFilters();
        MListMap listMap = new MListMap();
        for (UIWorkArea.RxFilter f :filters) {
          String func = wa.getScript().comp.getLinker().lookupAddressFunction(f.addr);
          if (func == null) {
            func = String.format("0x%08x", f.addr);
          }
          listMap.put(f.filter, new M(func));
        }
        return new M(listMap);
      }
    });
    setExtDef(FN_SERIAL_ON_RX_REPLAY, "(<log>) - runs given text through filters, or whole log if none given",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIWorkArea wa = ((MObj)p.getMe().ref).workarea;
         String text;
         if (args.length > 0) {
           text = args[0].asString();
         } else {
           text = wa.getSerialView().getSelectedText();
         }
         BufferedReader bufReader = new BufferedReader(new StringReader(text));
         String line;
         try {
           while((line = bufReader.readLine()) != null) {
             wa.handleSerialLine(line);
           }
         } catch (IOException ignore) {}

         return null;
      }
    });
    setExtDef(FN_SERIAL_LOG_START, "() - starts logging the serial input",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIWorkArea wa = ((MObj)p.getMe().ref).workarea;
        synchronized (logLock) {
          if (logThread != null) return null;

          logMatchIx = 0;
          logParseIx = 0;
          logMatch = null;
          log = new ByteArrayOutputStream();
          login = wa.getSerial().attachSerialIO();
          DisposableRunnable task = new DisposableRunnable() {
            @Override
            public void run() {
              int c;
              try {
                while (logThread != null && (c = login.read()) != -1) {
                  log.write(c);
                  byte[] localMatch = logMatch;
                  if (localMatch != null) {
                    if (localMatch[logMatchIx] == (byte)c) {
                      logMatchIx++;
                      if (logMatchIx >= localMatch.length) {
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
              finally {
                AppSystem.dispose(this);
              }
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
          if (logThread != null) logThread.interrupt();
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
          if (logThread == null || log == null) return new M(-1);

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
          if (log == null) return null;
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

  private void addGenericUIMembers(MObj mobj, UIO uio) {
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

    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_MOUSE_PRESS_FUNC));
    f.type = Processor.TFUNC;
    mobj.putIntern("mouse_press_func", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_MOUSE_RELEASE_FUNC));
    f.type = Processor.TFUNC;
    mobj.putIntern("mouse_release_func", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_KEY_PRESS_FUNC));
    f.type = Processor.TFUNC;
    mobj.putIntern("key_press_func", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_KEY_RELEASE_FUNC));
    f.type = Processor.TFUNC;
    mobj.putIntern("key_release_func", f);

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
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_PLACE_TAB_AFTER));
    f.type = Processor.TFUNC;
    mobj.putIntern("place_tab_after", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_PLACE_TAB_BEFORE));
    f.type = Processor.TFUNC;
    mobj.putIntern("place_tab_before", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_PLACE_TAB_FIRST));
    f.type = Processor.TFUNC;
    mobj.putIntern("place_tab_first", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_PLACE_TAB_LAST));
    f.type = Processor.TFUNC;
    mobj.putIntern("place_tab_last", f);
    f = new Processor.M(comp.getLinker().lookupFunctionAddress(FN_UI_DETACH_TAB));
    f.type = Processor.TFUNC;
    mobj.putIntern("detach_tab", f);
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
    } else if (uio instanceof UIWorkArea) {
      mobj = createWorkareaMUIO((UIWorkArea)uio);
    } else {
      mobj = new MObj(currentWA, comp, "uiobject") {
        @Override
        public void init(UIWorkArea wa, Compiler comp) {
        }
      };
    }
    M mui = new M(mobj);
    addGenericUIMembers(mobj, uio);
    return mui;
  }

  private MObj createGraphMUIO() {
    return new MObj(currentWA, comp, "graph") {
      @Override
      public void init(UIWorkArea wa, Compiler comp) {
        addFunc("add", "graph:add", comp);
        addFunc("set_data", "graph:set_data", comp);
        addFunc("tag", "graph:tag", comp);
        addFunc("remove_tags", "graph:remove_tags", comp);
        addFunc("data", "graph:data", comp);
        addFunc("fix_vertical", "graph:fix_vertical", comp);
        addFunc("zoom_all", "graph:zoom_all", comp);
        addFunc("zoom", "graph:zoom", comp);
        addFunc("zoom_x", "graph:zoom_x", comp);
        addFunc("zoom_y", "graph:zoom_y", comp);
        addFunc("set_type", "graph:type", comp);
        addFunc("count", "graph:count", comp);
        addFunc("set_color", "graph:set_color", comp);
        addFunc("scroll_x", "graph:scroll_x", comp);
        addFunc("scroll_y", "graph:scroll_y", comp);
        addFunc("scroll_sample", "graph:scroll_sample", comp);
        addFunc("set_mul", "graph:set_mul", comp);
        addFunc("set_offs", "graph:set_offs", comp);
        addFunc("min", "graph:min", comp);
        addFunc("max", "graph:max", comp);
        addFunc("join", "graph:join", comp);
        addFunc("link", "graph:link", comp);
        addFunc("unlink", "graph:unlink", comp);
        addFunc("save", "graph:save", comp);
        addFunc("show", "graph:show", comp);
        addFunc("show_tags", "graph:show_tags", comp);
        addFunc("set_cursor_x", "graph:set_cursor_x", comp);
        addFunc("x_to_index", "graph:x_to_index", comp);
        addFunc("y_to_value", "graph:y_to_value", comp);
      }
    };
  }

  private void createGraphFunctions() {
    setExtDef("graph", "(<name>,(<data>,...,(<line|plot|bar>))) - opens a graph view",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        String name = "GRAPH";
        int type = UIGraphPanel.GRAPH_LINE;
        List<List<Float>> valVals = new ArrayList<List<Float>>();
        if (args != null) {
          if (args.length > 0) {
            name = args[0].asString();
          }
          for (int i = 1; i < args.length; i++) {
            if (args[i].type == Processor.TSET) {
              List<Float> vals = new ArrayList<Float>();
              valVals.add(vals);
              MSet set = args[i].ref;
              for (int x = 0; x < set.size(); x++) {
                vals.add(set.get(x).asFloat());
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
        String id = Tuscedo.inst().addGraphTab(t.getPane(), valVals, name);
        SampleSet ui = ((SampleSet)Tuscedo.inst().getUIObject(id).getUI());
        ui.setGraphType(type);
        ui.getUIInfo().setName(name);
        MObj mobj = createGraphMUIO();
        M mui = new M(mobj);
        addGenericUIMembers(mobj, ui);
        System.out.println("adding operandi ui listener to " + ui);
        addOperandiUIListener(ui);
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
    setExtDef("graph:tag", "(<tag>, (<sampleIndex>)) - tags sampleIndex, or last value",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        int ix = ss.getSampleCount()-1;
        if (args.length > 1) {
          ix = args[1].asInt();
        }
        ss.addTag(ix, args[0].asString());
        return null;
      }
    });
    setExtDef("graph:remove_tags", "() - removes all tags",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        ss.clearTags();
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
    setExtDef("graph:set_data", "(<data>) - replaces data of graph",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        List<Double> set = new ArrayList<Double>();
        if (args[0].type != Processor.TSET) {
          set.add((double)args[0].asFloat());
        } else {
          for (int i = 0; i < args[0].ref.size(); i++) {
            set.add((double)args[0].ref.get(i).asFloat());
          }
        }
        ss.setSamples(set);
        return null;
      }
    });
    setExtDef("graph:fix_vertical", "(min_y, max_y) - makes given vertical data range visible",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 2)  return null;
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null) return null;
        if (!((UIGraphPanel)ss.getUIInfo().getParent().getUI()).isUserZoomed()) {
          ((UIGraphPanel)ss.getUIInfo().getParent().getUI()).zoomForceVertical(args[0].asFloat(), args[1].asFloat());
        }
        return null;
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
    setExtDef("graph:set_color", "(x) - sets graph color",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null || args.length == 0) return null;
        ss.setColor(args[0].asInt());
        ss.repaint();
        return null;
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
        for (int i = 0; i < args.length; i++) {
          recurseJoin(sssrc, args[i]);
        }
        return p.getMe();
      }
      void recurseJoin(SampleSet sssrc, Processor.M joiner) {
        if (joiner.type == Processor.TSET && joiner.ref.getType() == MListMap.TARR) {
          for (int i = 0; i < joiner.ref.size(); i++) {
            recurseJoin(sssrc, joiner.ref.get(i));
          }
        } else {
          UIO uioover = getUIOByScriptId(joiner);
          if (uioover == null) return;
          if (uioover instanceof SampleSet) {
            SampleSet ssover = (SampleSet)uioover;
            if (ssover == sssrc) return;
            UIGraphPanel over = ((UIGraphPanel)ssover.getUIInfo().getParent().getUI());
            over.removeSampleSet(ssover);
            UIGraphPanel src = ((UIGraphPanel)sssrc.getUIInfo().getParent().getUI());
            src.addSampleSet(ssover);
            UIInfo.fireEventOnCreated(ssover.getUIInfo());
          }
        }
      }
    });

    setExtDef("graph:link", "(<graph>, ...) - link this graph with another, or others",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet sssrc = (SampleSet)getUIOByScriptId(p.getMe());
        if (sssrc == null) return null;
        UIGraphPanel src = ((UIGraphPanel)sssrc.getUIInfo().getParent().getUI());
        for (int i = 0; i < args.length; i++) {
          UIO uioover = getUIOByScriptId(args[i]);
          if (uioover == null) continue;
          if (uioover instanceof SampleSet) {
            SampleSet ssover = (SampleSet)uioover;
            if (ssover == sssrc) continue;
            UIGraphPanel over = ((UIGraphPanel)ssover.getUIInfo().getParent().getUI());
            src.linkOtherGraphPanel(over);
          }
        }
        return p.getMe();
      }
    });

    setExtDef("graph:unlink", "(<graph>, ...) - unlink this graph with another, or others",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        SampleSet sssrc = (SampleSet)getUIOByScriptId(p.getMe());
        if (sssrc == null) return null;
        UIGraphPanel src = ((UIGraphPanel)sssrc.getUIInfo().getParent().getUI());
        for (int i = 0; i < args.length; i++) {
          UIO uioover = getUIOByScriptId(args[i]);
          if (uioover == null) continue;
          if (uioover instanceof SampleSet) {
            SampleSet ssover = (SampleSet)uioover;
            if (ssover == sssrc) continue;
            UIGraphPanel over = ((UIGraphPanel)ssover.getUIInfo().getParent().getUI());
            src.unlinkOtherGraphPanel(over);
          }
        }
        return p.getMe();
      }
    });

    setExtDef("graph:save", "(<path>) - saves graph data set to file",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null || args.length == 0) return null;
        return new M(ss.export(new File(args[0].asString())) ? 0 : -1);
      }
    });
    setExtDef("graph:show", "(<0|1>) - shows/hides graph",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null || args.length == 0) return null;
        ss.setHidden(args[0].asInt() == 0);
        return null;
      }
    });
    setExtDef("graph:show_tags", "(<0|1>) - shows/hides tags",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null || args.length == 0) return null;
        ss.setTagsHidden(args[0].asInt() == 0);
        return null;
      }
    });

    setExtDef("graph:set_cursor_x", "(x) - displays a cursor at given index",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null || args.length == 0) return null;
        UIGraphPanel cp = (UIGraphPanel)(getUIOByScriptId(p.getMe()).getUIInfo().getParentUI());
        cp.setUserCursorIndex(args[0].asInt());
        return null;
      }
    });

    setExtDef("graph:x_to_index", "(x) - recalculates a mouse x coordinate to graph index",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null || args.length == 0) return null;
        UIGraphPanel cp = (UIGraphPanel)(getUIOByScriptId(p.getMe()).getUIInfo().getParentUI());
        return new M(cp.mouseXToIndex(args[0].asInt()));
      }
    });
    setExtDef("graph:y_to_value", "(y) - recalculates a mouse y coordinate to graph value",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        SampleSet ss = (SampleSet)getUIOByScriptId(p.getMe());
        if (ss == null || args.length == 0) return null;
        UIGraphPanel cp = (UIGraphPanel)(getUIOByScriptId(p.getMe()).getUIInfo().getParentUI());
        return new M(cp.mouseYToValue(args[0].asInt()));
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

  private MObj createWorkareaMUIO(UIWorkArea wa) {
    return new MObj(wa, comp, "workarea") {
      @Override
      public void init(UIWorkArea wa, Compiler comp) {
        addFunc("get_ser", "workarea:get_ser", comp);
        addFunc("load_script", "workarea:load_script", comp);
        addFunc("run_script", "workarea:run_script", comp);
        addFunc("reset", "workarea:reset", comp);
      }
    };
  }

  private void createWorkareaFunctions() {
    setExtDef("workarea", "(<name>,(<tab|split_v|split_h|window>)) - opens a new workarea",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        Tab t = UISimpleTabPane.getTabByComponent(currentWA);
        if (t == null) {
          Tuscedo.inst().create(currentWA);
          t = UISimpleTabPane.getTabByComponent(currentWA);
        }
        String name = "WORKAREA";
        String mode = "tab";
        if (args != null) {
          if (args.length > 0) {
            name = args[0].asString();
          }
          if (args.length > 1) {
            mode = args[1].asString();
          }
        }
        String id = Tuscedo.inst().addWorkAreaTab(t.getPane(), null);
        UIO ui = Tuscedo.inst().getUIObject(id).getUI();
        ui.getUIInfo().setName(name);
        MObj mobj = createWorkareaMUIO((UIWorkArea)ui);
        M mui = new M(mobj);
        addGenericUIMembers(mobj, ui);

        if (mode.equals("split_v")) {
          ((UIWorkArea)ui).getCurrentView().splitOut(false);
        } else if (mode.equals("split_h")) {
          ((UIWorkArea)ui).getCurrentView().splitOut(true);
        } else if (mode.equals("window")) {
          Tab tw = UISimpleTabPane.getTabByComponent((UIWorkArea)ui);
          tw.getPane().evacuateTabToNewFrame(tw, null, null);
        }
        return mui;
      }
    });
    setExtDef("workarea:get_ser", "() - returns the serial of the workarea",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIWorkArea wa = (UIWorkArea)getUIOByScriptId(p.getMe());
        OperandiScript otherScript = wa.getScript();
        int addr = otherScript.comp.getLinker().lookupVariableAddress(null, VAR_SERIAL);
        return otherScript.proc.getMemory()[addr];
      }
    });
    setExtDef("workarea:load_script", "(<path>, ... <args>) - runs operandi script file in the work area",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) throw new ProcessorError("Must give arguments");
        UIWorkArea wa = (UIWorkArea)getUIOByScriptId(p.getMe());
        OperandiScript otherScript = wa.getScript();
        File f = new File(args[0].asString());
        if (!f.exists() || !f.isFile()) {
          throw new ProcessorError("Script file '" + f.getAbsolutePath() + "' cannot be found.");
        }
        String[] sargs = new String[args.length-1];
        for (int i = 1; i < args.length; i++) sargs[i-1] = args[i].asString();
        otherScript.runScript(wa, f, ""/*ignored*/, sargs);
        return null;
      }
    });
    setExtDef("workarea:run_script", "(<script>, ... <args>) - runs operandi script in the work area",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0) throw new ProcessorError("Must give arguments");
        UIWorkArea wa = (UIWorkArea)getUIOByScriptId(p.getMe());
        OperandiScript otherScript = wa.getScript();
        String[] sargs = new String[args.length-1];
        for (int i = 1; i < args.length; i++) sargs[i-1] = args[i].asString();
        otherScript.runScript(wa, args[0].asString(), currentWA.getUIInfo().getId(), sargs);
        return null;
      }
    });
    setExtDef("workarea:reset", "() - resets this work areas processor",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIWorkArea wa = (UIWorkArea)getUIOByScriptId(p.getMe());
        OperandiScript otherScript = wa.getScript();
        otherScript.resetForce();
        otherScript.runOperandiInitScripts(wa);
        return null;
      }
    });
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
        addFunc("draw_image", "canvas:draw_image", comp);
        addFunc("get_width", "canvas:width", comp);
        addFunc("get_height", "canvas:height", comp);
        addFunc("get_rgb", "canvas:get_rgb", comp);
        addFunc("blit", "canvas:blit", comp);
      }
    };
  }

  private void addOperandiUIListener(UIO ui) {
    UIListener uil = new UIListener() {
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
          addr = uio.getUIInfo().funcMousePressAddr;
          if (addr > 0) {
            List<M> args = new ArrayList<M>(4);
            args.add(new M(uio.getUIInfo().id));
            args.add(new M(uio.getUIInfo().mousepressx));
            args.add(new M(uio.getUIInfo().mousepressy));
            args.add(new M(uio.getUIInfo().mousepressb));
            OperandiScript.this.runFunc(currentWA, addr, args);
          }
        }
        else if (event == UIInfo.EVENT_MOUSE_RELEASE) {
          int addr = uio.getUIInfo().irqMouseReleaseAddr;
          if (addr > 0) {
            OperandiScript.this.getIRQHandler()
            .queue(OperandiIRQHandler.IRQ_BLOCK_UI, OperandiIRQHandler.IRQ_UI_MOUSE_RELEASE)
            .trigger(addr);
          }
          addr = uio.getUIInfo().funcMouseReleaseAddr;
          if (addr > 0) {
            List<M> args = new ArrayList<M>(4);
            args.add(new M(uio.getUIInfo().id));
            args.add(new M(uio.getUIInfo().mousepressx));
            args.add(new M(uio.getUIInfo().mousepressy));
            args.add(new M(uio.getUIInfo().mousepressb));
            OperandiScript.this.runFunc(currentWA, addr, args);
          }
        }
        else if (event == UIInfo.EVENT_KEY_PRESS) {
          int addr = uio.getUIInfo().irqKeyPressAddr;
          if (addr > 0) {
            OperandiScript.this.getIRQHandler()
            .queue(OperandiIRQHandler.IRQ_BLOCK_UI, OperandiIRQHandler.IRQ_UI_KEY_PRESS)
            .trigger(addr);
          }
          addr = uio.getUIInfo().funcKeyPressAddr;
          if (addr > 0) {
            List<M> args = new ArrayList<M>(2);
            args.add(new M(uio.getUIInfo().id));
            args.add(new M(uio.getUIInfo().keypress));
            OperandiScript.this.runFunc(currentWA, addr, args);
          }
        }
        else if (event == UIInfo.EVENT_KEY_RELEASE) {
          int addr = uio.getUIInfo().irqKeyReleaseAddr;
          if (addr > 0) {
            OperandiScript.this.getIRQHandler()
            .queue(OperandiIRQHandler.IRQ_BLOCK_UI, OperandiIRQHandler.IRQ_UI_KEY_RELEASE)
            .trigger(addr);
          }
          addr = uio.getUIInfo().funcKeyReleaseAddr;
          if (addr > 0) {
            List<M> args = new ArrayList<M>(2);
            args.add(new M(uio.getUIInfo().id));
            args.add(new M(uio.getUIInfo().keyrel));
            OperandiScript.this.runFunc(currentWA, addr, args);
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
    };
    ui.getUIInfo().addListener(uil);
  }

  private void createCanvasFunctions() {
    setExtDef("canvas", "((<title>),(<w>),(<h>)) - creates a canvas",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        String name = "CANVAS";
        Tab tab = UISimpleTabPane.getTabByComponent(currentWA);
        if (tab == null) {
          Tuscedo.inst().create(currentWA);
          tab = UISimpleTabPane.getTabByComponent(currentWA);
        }
        int w = tab.getWidth();
        int h = tab.getHeight();
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

        String id = Tuscedo.inst().addCanvasTab(tab.getPane(), w, h);
        UICanvasPanel ui = ((UICanvasPanel)Tuscedo.inst().getUIObject(id).getUI());
        ui.getUIInfo().setName(name);

        MObj mobj = createCanvasMUIO();
        M mui = new M(mobj);
        addGenericUIMembers(mobj, ui);
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
    setExtDef("canvas:draw_image", "(<path to image>,<x>,<y>,(<w>,<h>)) - draws image",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 3)  return null;
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        if (args.length < 5)
          cp.drawImage(args[1].asInt(), args[2].asInt(), args[0].asString());
        else
          cp.drawImage(args[1].asInt(), args[2].asInt(), args[3].asInt(), args[4].asInt(), args[0].asString());
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
    setExtDef("canvas:get_rgb", "(<x>,<y>(,<w>,<h>)) - returns pixel data as single integer for one pixel or as an array of ints [w][h]",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UICanvasPanel cp = (UICanvasPanel)getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        if (args == null || args.length < 2)  return null;
        int x = args[0].asInt();
        int y = args[1].asInt();
        int w = 1;
        int h = 1;
        if (args.length >= 4) {
          w = args[2].asInt();
          h = args[3].asInt();
        }
        if (w == 0 || h == 0) {
          return null;
        }
        int ww = cp.getWidth();
        int hh = cp.getHeight();
        if (w == 1 && h == 1) {
          return new Processor.M(cp.getRGB(x, y));
        }

        MSet msetx = new MListMap();
        for (int xx = x; xx < x+w; xx++) {
          MSet msety = new MListMap();
          msetx.add(new Processor.M(msety));
          for (int yy = y; yy < y+h; yy++) {
            if (xx < 0 || xx >= ww || yy < 0 || yy >= hh) {
              msety.add(new Processor.M(0));
            } else {
              msety.add(new Processor.M(cp.getRGB(xx, yy)));
            }
          }
        }
        return new Processor.M(msetx);
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
        addFunc("join", "graph3d:join", comp);
        addFunc("blit", "graph3d:blit", comp);
        addFunc("model_rotate", "graph3d:model_rotate", comp);
        addFunc("model_rotate_quat", "graph3d:model_rotate_quat", comp);
        addFunc("model_lookat", "graph3d:model_lookat", comp);
        addFunc("model_set", "graph3d:model_set", comp);
        addFunc("model_translate", "graph3d:model_translate", comp);
        addFunc("model_scale", "graph3d:model_scale", comp);
        addFunc("model_reset", "graph3d:model_reset", comp);
        addFunc("mark", "graph3d:mark", comp);
        addFunc("unmark", "graph3d:unmark", comp);
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
        RenderSpec ui = ((RenderSpec)Tuscedo.inst().getUIObject(id).getUI());
        ui.getUIInfo().setName(name);
        MObj mobj = create3DMUIO();
        M mui = new M(mobj);
        addGenericUIMembers(mobj, ui);
        return mui;
      }
    });
    setExtDef("graph3d:width", "() - returns width",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)(getUIOByScriptId(p.getMe()).getUIInfo().getParentUI());
        if (cp == null) return null;
        return new Processor.M(cp.getWidth());
      }
    });
    setExtDef("graph3d:height", "() - returns height",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)(getUIOByScriptId(p.getMe()).getUIInfo().getParentUI());
        if (cp == null) return null;
        return new Processor.M(cp.getHeight());
      }
    });
    setExtDef("graph3d:set_pos", "(<x>, <y>, <z>) - set beholders position",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)(getUIOByScriptId(p.getMe()).getUIInfo().getParentUI());
        if (cp == null) return null;
        cp.setPlayerPosition(args[0].asFloat(), args[1].asFloat(), args[2].asFloat());
        return null;
      }
    });
    setExtDef("graph3d:set_view", "(<yaw>, <pitch>, <roll>) - sets beholders viewing orientation",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)(getUIOByScriptId(p.getMe()).getUIInfo().getParentUI());
        if (cp == null) return null;
        cp.setPlayerView(args[0].asFloat(), args[1].asFloat(), args[2].asFloat());
        return null;
      }
    });
    setExtDef("graph3d:set_size", "(<width>, <height>) - sets viewport dimensions",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)(getUIOByScriptId(p.getMe()).getUIInfo().getParentUI());
        if (cp == null) return null;
        cp.setSize(args[0].asInt(), args[1].asInt());
        return null;
      }
    });
    setExtDef("graph3d:set_model_heightmap", "(<heightmap>) - sets heightmap data model (array of arrays of floats)",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)(getUIOByScriptId(p.getMe()).getUIInfo().getParentUI());
        if (cp == null) return null;
        cp.setHeightMap(convHeightMap(args[0]));
        return null;
      }
    });
    setExtDef("graph3d:set_model_heightmap_color", "(<heightmap-colored>) - sets colored heightmap data model (array of arrays of 4 float vector [height, red, green, blue])",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)(getUIOByScriptId(p.getMe()).getUIInfo().getParentUI());
        if (cp == null) return null;
        cp.setHeightMapColor(convHeightMapColor(args[0]));
        return null;
      }
    });
    setExtDef("graph3d:set_model_cloud", "(<cloud>, <isolevel>) - sets point cloud data model (array of arrays of arrays of floats)",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)(getUIOByScriptId(p.getMe()).getUIInfo().getParentUI());
        if (cp == null) return null;
        float isolevel = 0.5f;
        boolean faceted = false;
        if (args.length > 1) isolevel = args[1].asFloat();
        if (args.length > 2) faceted = args[2].asInt() != 0;
        cp.setPointCloud(convPointCloud(args[0]), isolevel, faceted);
        return null;
      }
    });
    setExtDef("graph3d:set_model_cloud_color", "(<cloud>, <isolevel>) - sets colored point cloud data model (array of arrays of arrays of 4 float vector [weight, red, green, blue]))",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)(getUIOByScriptId(p.getMe()).getUIInfo().getParentUI());
        if (cp == null) return null;
        float isolevel = 0.5f;
        boolean faceted = false;
        if (args.length > 1) isolevel = args[1].asFloat();
        if (args.length > 2) faceted = args[2].asInt() != 0;
        cp.setPointCloudColor(convPointCloudColor(args[0]), isolevel, faceted);
        return null;
      }
    });
    setExtDef("graph3d:join", "(<graph3d>, ...) - join this 3D graph with another, or others",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;

        RenderSpec rs = (RenderSpec)getUIOByScriptId(p.getMe());
        if (rs == null) return null;

        UI3DPanel src = ((UI3DPanel)rs.getUIInfo().getParent().getUI());

        for (int i = 0; i < args.length; i++) {
          RenderSpec rschild = (RenderSpec)getUIOByScriptId(args[i]);
          if (rschild == null) continue;
          UI3DPanel child = ((UI3DPanel)rschild.getUIInfo().getParent().getUI());
          child.removeRenderSpec(rschild);
          src.addRenderSpec(rschild);
        }
        return null;
      }
    });
    setExtDef("graph3d:blit", "() - blits changes to graph",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UI3DPanel cp = (UI3DPanel)(getUIOByScriptId(p.getMe()).getUIInfo().getParentUI());
        if (cp == null) return null;
        cp.blit();
        return null;
      }
    });
    setExtDef("graph3d:model_rotate", "(<angle>, <x>,<y>,<z>) - rotates model <angle> radians around vector <x><y><z>",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 4)  return null;
        RenderSpec rs = (RenderSpec)getUIOByScriptId(p.getMe());
        if (rs == null) return null;
        rs.getModelMatrix().rotate(args[0].asFloat(), args[1].asFloat(), args[2].asFloat(), args[3].asFloat());
        return p.getMe();
      }
    });
    setExtDef("graph3d:model_rotate_quat", "(<x>i,<y>j,<z>k,<w>) - rotates model according to given quaternion",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 4)  return null;
        RenderSpec rs = (RenderSpec)getUIOByScriptId(p.getMe());
        if (rs == null) return null;
        rs.getModelMatrix().rotate(new Quaternionf(args[0].asFloat(), args[1].asFloat(), args[2].asFloat(), args[3].asFloat()));
        return p.getMe();
      }
    });
    setExtDef("graph3d:model_lookat", "(<xdir>,<ydir>,<zdir>,<xup>,<yup>,<zup>) - rotates model according to given look at vector",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 4)  return null;
        RenderSpec rs = (RenderSpec)getUIOByScriptId(p.getMe());
        if (rs == null) return null;
        rs.getModelMatrix().lookAlong(
            args[0].asFloat(), args[1].asFloat(), args[2].asFloat(),
            args[3].asFloat(), args[4].asFloat(), args[5].asFloat());
        return p.getMe();
      }
    });
    setExtDef("graph3d:model_set", "([[m00,m01,m02,m03],..]>) - sets model matrix to given 4x4 matrix",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 1)  return null;
        RenderSpec rs = (RenderSpec)getUIOByScriptId(p.getMe());
        if (rs == null) return null;
        MSet setRows = args[0].ref;
        float m[] = new float[4*4];
        for (int i = 0; i < setRows.size(); i++) {
          MSet setCol = setRows.get(i).ref;
          for (int j = 0; j < setCol.size(); j++) {
            m[j*4+i] = setCol.get(j).asFloat();
          }
        }
        rs.getModelMatrix().set(m);
        return p.getMe();
      }
    });
    setExtDef("graph3d:model_translate", "(<dx>,<dy>,<dz>) - translates model",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 3)  return null;
        RenderSpec rs = (RenderSpec)getUIOByScriptId(p.getMe());
        if (rs == null) return null;
        rs.getModelMatrix().translate(args[0].asFloat(), args[1].asFloat(), args[2].asFloat());
        return p.getMe();
      }
    });
    setExtDef("graph3d:model_scale", "(<sx>,<sy>,<sz>) - scales model",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length < 3)  return null;
        RenderSpec rs = (RenderSpec)getUIOByScriptId(p.getMe());
        if (rs == null) return null;
        rs.getModelMatrix().scale(args[0].asFloat(), args[1].asFloat(), args[2].asFloat());
        return p.getMe();
      }
    });
    setExtDef("graph3d:model_reset", "() - resets modelmatrix",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        RenderSpec rs = (RenderSpec)getUIOByScriptId(p.getMe());
        if (rs == null) return null;
        rs.getModelMatrix().identity();
        return p.getMe();
      }
    });
    setExtDef("graph3d:mark", "(x,y,z(,scale)(,red,green,blue)) - sets and returns a 3d mark",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        RenderSpec rs = (RenderSpec)getUIOByScriptId(p.getMe());
        if (rs == null || args.length < 3) return null;
        float scale = (args.length == 4 || args.length >= 7) ? args[3].asFloat() : 1f;
        float r = args.length == 6 ? args[3].asFloat() : (args.length >= 7 ? args[4].asFloat() : 1f);
        float g = args.length == 6 ? args[4].asFloat() : (args.length >= 7 ? args[5].asFloat() : 1f);
        float b = args.length == 6 ? args[5].asFloat() : (args.length >= 7 ? args[6].asFloat() : 0f);
        RenderSpec.Marker mark = rs.addMarker(args[0].asFloat(), args[1].asFloat(), args[2].asFloat(), scale, r, g, b);
        MObj mobj = create3DMarkerMUIO(mark);
        M mui = new M(mobj);
        return mui;
      }
    });
    setExtDef("graph3d:unmark", "(mark) - removes a 3d mark",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        RenderSpec rs = (RenderSpec)getUIOByScriptId(p.getMe());
        if (rs == null || args.length < 1) return null;
        RenderSpec.Marker marker = null;
        M m = args[0];
        if (m.type == Processor.TSET && m.ref instanceof MObj && ((MObj)m.ref).user instanceof RenderSpec.Marker) {
          marker = (RenderSpec.Marker)((MObj)m.ref).user;
        }
        if (marker == null) return null;
        rs.removeMarker(marker);
        return null;
      }
    });

    setExtDef("graph3dmarker:get_pos", "() - returns marker position as a vector", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        M m = p.getMe();
        RenderSpec.Marker marker = null;
        if (m.type == Processor.TSET && m.ref instanceof MObj && ((MObj)m.ref).user instanceof RenderSpec.Marker) {
          marker = (RenderSpec.Marker)((MObj)m.ref).user;
        }
        if (marker == null) return null;
        MListMap arr = new MListMap();
        arr.add(new Processor.M(marker.x()));
        arr.add(new Processor.M(marker.y()));
        arr.add(new Processor.M(marker.z()));
        return new Processor.M(arr);
      }
    });
    setExtDef("graph3dmarker:set_pos", "(x,y,z) - sets marker position", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        M m = p.getMe();
        RenderSpec.Marker marker = null;
        if (m.type == Processor.TSET && m.ref instanceof MObj && ((MObj)m.ref).user instanceof RenderSpec.Marker) {
          marker = (RenderSpec.Marker)((MObj)m.ref).user;
        }
        if (marker == null || args.length < 3) return null;
        marker.setPos(args[0].asFloat(), args[1].asFloat(), args[2].asFloat());
        return null;
      }
    });
    setExtDef("graph3dmarker:get_color", "() - returns marker color as an rgb vector", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        M m = p.getMe();
        RenderSpec.Marker marker = null;
        if (m.type == Processor.TSET && m.ref instanceof MObj && ((MObj)m.ref).user instanceof RenderSpec.Marker) {
          marker = (RenderSpec.Marker)((MObj)m.ref).user;
        }
        if (marker == null) return null;
        MListMap arr = new MListMap();
        arr.add(new Processor.M(marker.r()));
        arr.add(new Processor.M(marker.g()));
        arr.add(new Processor.M(marker.b()));
        return new Processor.M(arr);
      }
    });
    setExtDef("graph3dmarker:set_color", "(r,g,b) - sets marker color", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        M m = p.getMe();
        RenderSpec.Marker marker = null;
        if (m.type == Processor.TSET && m.ref instanceof MObj && ((MObj)m.ref).user instanceof RenderSpec.Marker) {
          marker = (RenderSpec.Marker)((MObj)m.ref).user;
        }
        if (marker == null || args.length < 3) return null;
        marker.setColor(args[0].asFloat(), args[1].asFloat(), args[2].asFloat());
        return null;
      }
    });
    setExtDef("graph3dmarker:get_scale", "() - returns marker scale", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        M m = p.getMe();
        RenderSpec.Marker marker = null;
        if (m.type == Processor.TSET && m.ref instanceof MObj && ((MObj)m.ref).user instanceof RenderSpec.Marker) {
          marker = (RenderSpec.Marker)((MObj)m.ref).user;
        }
        if (marker == null) return null;
        return new Processor.M(marker.scale());
      }
    });
    setExtDef("graph3dmarker:set_scale", "(scale) - sets marker scale", new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        M m = p.getMe();
        RenderSpec.Marker marker = null;
        if (m.type == Processor.TSET && m.ref instanceof MObj && ((MObj)m.ref).user instanceof RenderSpec.Marker) {
          marker = (RenderSpec.Marker)((MObj)m.ref).user;
        }
        if (marker == null || args.length < 1) return null;
        marker.setScale(args[0].asFloat());
        return null;
      }
    });
  }

  static Component searchForComponentUpwards(UIO o) {
    while (o != null && !(o instanceof Component)) {
      if (o.getUIInfo().getParentUI() != null) {
        o = o.getUIInfo().getParentUI();
      } else {
        o = null;
      }
    }
    return o == null || !(o instanceof Component) ? null : (Component)o;
  }

  private MObj create3DMarkerMUIO(RenderSpec.Marker mark) {
    MObj mo = new MObj(currentWA, comp, "graph3dmarker") {
      @Override
      public void init(UIWorkArea wa, Compiler comp) {
        addFunc("get_pos", "graph3dmarker:get_pos", comp);
        addFunc("set_pos", "graph3dmarker:set_pos", comp);
        addFunc("get_color", "graph3dmarker:get_color", comp);
        addFunc("set_color", "graph3dmarker:set_color", comp);
        addFunc("get_scale", "graph3dmarker:get_scale", comp);
        addFunc("set_scale", "graph3dmarker:set_scale", comp);
      }
    };
    mo.user = mark;
    return mo;
  }

  private void createGenericUIFunctions() {
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
    setExtDef(FN_UI_ON_MOUSE_PRESS, "(<func>) - calls func on mouse press via interrupt",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO cp = getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.getUIInfo().irqMousePressAddr = args[0].i;
        return null;
      }
    });
    setExtDef(FN_UI_MOUSE_PRESS_FUNC, "(<func>) - calls func on mouse press via script",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO cp = getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.getUIInfo().funcMousePressAddr = args[0].i;
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
    setExtDef(FN_UI_MOUSE_RELEASE_FUNC, "(<func>) - calls func on mouse release via script",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO cp = getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.getUIInfo().funcMouseReleaseAddr = args[0].i;
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
    setExtDef(FN_UI_KEY_PRESS_FUNC, "(<func>) - calls func on key press via script",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO cp = getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.getUIInfo().funcKeyPressAddr = args[0].i;
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
    setExtDef(FN_UI_KEY_RELEASE_FUNC, "(<func>) - calls func on key release via script",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO cp = getUIOByScriptId(p.getMe());
        if (cp == null) return null;
        cp.getUIInfo().funcKeyReleaseAddr = args[0].i;
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
    setExtDef(FN_UI_PLACE_TAB_AFTER, "(<ui>) - places this ui after given ui in tab order",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        UIO uio = getUIOByScriptId(p.getMe());
        if (uio == null || !(uio instanceof Component)) return null;
        UIO uioother = getUIOByScriptId(args[0]);
        if (uioother == null || !(uioother instanceof Component)) return null;
        Tab tthis = UISimpleTabPane.getTabByComponent(searchForComponentUpwards(uio));
        Tab tthat = UISimpleTabPane.getTabByComponent(searchForComponentUpwards(uioother));
        tthis.getPane().moveTabAfter(tthis.getID(), tthat.getID());
        return null;
      }
    });
    setExtDef(FN_UI_PLACE_TAB_BEFORE, "(<ui>) - places this ui before given ui in tab order",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args == null || args.length == 0)  return null;
        UIO uio = getUIOByScriptId(p.getMe());
        if (uio == null || !(uio instanceof Component)) return null;
        UIO uioother = getUIOByScriptId(args[0]);
        if (uioother == null || !(uioother instanceof Component)) return null;
        Tab tthis = UISimpleTabPane.getTabByComponent(searchForComponentUpwards(uio));
        Tab tthat = UISimpleTabPane.getTabByComponent(searchForComponentUpwards(uioother));
        tthis.getPane().moveTabBefore(tthis.getID(), tthat.getID());
        return null;
      }
    });
    setExtDef(FN_UI_PLACE_TAB_FIRST, "() - places this ui first in tab order",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO uio = getUIOByScriptId(p.getMe());
        Tab tthis = UISimpleTabPane.getTabByComponent(searchForComponentUpwards(uio));
        tthis.getPane().moveTabBefore(tthis.getID(), 0);
        return null;
      }
    });
    setExtDef(FN_UI_PLACE_TAB_LAST, "() - places this ui last in tab order",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO uio = getUIOByScriptId(p.getMe());
        Tab tthis = UISimpleTabPane.getTabByComponent(searchForComponentUpwards(uio));
        tthis.getPane().moveTabAfter(tthis.getID(), tthis.getPane().getTabCount()-1);
        return null;
      }
    });
    setExtDef(FN_UI_DETACH_TAB, "(x, y, (width, height)) - detaches ui to its own frame",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        UIO uio = getUIOByScriptId(p.getMe());
        Tab tthis = UISimpleTabPane.getTabByComponent(searchForComponentUpwards(uio));
        int x = tthis.getLocationOnScreen().x;
        int y = tthis.getLocationOnScreen().y;
        Dimension sz = null;
        if (args.length >= 1) x = args[0].asInt();
        if (args.length >= 2) y = args[1].asInt();
        if (args.length >= 4) {
          sz = new Dimension(args[2].asInt(), args[3].asInt());
        }
        tthis.getPane().evacuateTabToNewFrame(tthis, new Point(x,y), sz);
        return null;
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
    public String[] strargs;
    public RunRequest(UIWorkArea wa, Source src, String[] strargs) {
      this.wa = wa; this.src = src; this.callAddr = 0; this.strargs = strargs;
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
    appVariables.put(VAR_AUDIO, m);
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
      } else if (var.equals(VAR_AUDIO)) {
        val = new M(new MAudio(wa, comp));
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
