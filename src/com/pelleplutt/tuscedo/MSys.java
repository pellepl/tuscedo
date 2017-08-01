package com.pelleplutt.tuscedo;

import java.io.IOException;
import java.util.Map;

import com.pelleplutt.operandi.Compiler;
import com.pelleplutt.operandi.proc.ExtCall;
import com.pelleplutt.operandi.proc.MListMap;
import com.pelleplutt.operandi.proc.Processor;
import com.pelleplutt.operandi.proc.ProcessorError;
import com.pelleplutt.operandi.proc.Processor.M;
import com.pelleplutt.tuscedo.ui.UIWorkArea;
import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.AppSystem.ProcessResult;

public class MSys extends MObj {
  public MSys(UIWorkArea wa, Compiler comp) {
    super(wa, comp, "sys");
  }

  public void init(UIWorkArea wa, com.pelleplutt.operandi.Compiler comp) {
    this.workarea = wa;
    addFunc("exec", OperandiScript.FN_SYS_EXEC, comp);
    addFunc("get_home", OperandiScript.FN_SYS_GET_HOME, comp);
  }
  
  static public void createSysFunctions(Map<String, ExtCall> extDefs) {
    extDefs.put(OperandiScript.FN_SYS_GET_HOME, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        return new M(System.getProperty("user.home"));
      }
    });
    extDefs.put(OperandiScript.FN_SYS_EXEC, new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length == 0) {
          return null;
        }
        try {
          ProcessResult pr = AppSystem.run(args[0].asString(), null, null, true, true);
          MListMap mpr = new MListMap();
          mpr.put("ret", new M(pr.code));
          mpr.put("stdout", new M(pr.output));
          mpr.put("stderr", new M(pr.err));
          return new M(mpr);
        } catch (IOException | InterruptedException e) {
          throw new ProcessorError(e.getMessage());
        }
      }
    });
  }


}
