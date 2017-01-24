package com.pelleplutt.tuscedo;

import java.awt.Point;
import java.io.ByteArrayOutputStream;
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

public class OperandiScript {

  Executable exe, pexe;
  Compiler comp;
  Processor proc;
  Map<String, ExtCall> extDefs = new HashMap<String, ExtCall>();
  volatile WorkArea currentWA;
  WorkArea.View runningView;

  public OperandiScript() {
    proc = new Processor(0x10000);
    reset();
  }

  public void reset() {
    exe = pexe = null;
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
    currentWA = wa;
    WorkArea.View view = wa.getCurrentView();
    proc.reset();
    comp.injectGlobalVariable(null, "fiskorv");
    try {
      Source src = new Source.SourceString("cli", s);
      exe = comp.compileIncrementally(src, pexe);
      pexe = exe;
    } catch (CompilerError ce) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos);
      comp.printCompilerError(ps, Compiler.getSource(), ce);
      String err = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      AppSystem.closeSilently(ps);
      wa.appendViewText(view, err, WorkArea.STYLE_BASH_ERR);
      return;
    }
    int i = 0;
    proc.setExe(exe);
    M fiskorv = new Processor.M(new MListMap());
    M fiskorvEntry = new Processor.M(comp.getLinker().lookupFunctionAddress("println"));
    fiskorvEntry.type = Processor.TFUNC;
    fiskorv.ref.put("fu", fiskorvEntry);
    int fiskorvAddr = comp.getLinker().lookupVariableAddress(null, "fiskorv");
    proc.setMemory(fiskorvAddr, fiskorv);
    try {
      for (; i < 10000000; i++) {
        proc.step();
      }
    } catch (ProcessorFinishedError pfe) {
      M m = pfe.getRet();
      if (m != null && m.type != Processor.TNIL) {
        wa.appendViewText(view, "script returned " + m.asString() + "\n", WorkArea.STYLE_BASH_OUT);
      }
    } catch (ProcessorError pe) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos);
      proc.dumpError(pe, ps);
      String err = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      AppSystem.closeSilently(ps);
      wa.appendViewText(view, err, WorkArea.STYLE_BASH_ERR);
    }
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


}
