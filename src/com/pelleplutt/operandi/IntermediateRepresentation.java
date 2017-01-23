package com.pelleplutt.operandi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pelleplutt.operandi.ASTNode.ASTNodeSymbol;

public class IntermediateRepresentation {
  private List<Module> modules = new ArrayList<Module>();
  // module / symbol pairs from previous sources
  private Map<String, List<ASTNodeSymbol>> accGlobalVars = new HashMap<String, List<ASTNodeSymbol>>();
  // module / symbol pairs from current sources
  private Map<String, List<ASTNodeSymbol>> curGlobalVars = new HashMap<String, List<ASTNodeSymbol>>();
  
  public void setModules(List<Module> modules) {
    this.modules = modules;
  }
  
  public void accumulateGlobals() {
    for (String mod : curGlobalVars.keySet()) {
      List<ASTNodeSymbol> syms = curGlobalVars.get(mod);
      List<ASTNodeSymbol> accSyms = accGlobalVars.get(mod);
      if (accSyms == null) {
        accSyms = new ArrayList<ASTNodeSymbol>();
        accGlobalVars.put(mod, accSyms);
      }
      accSyms.addAll(syms);
    }
    curGlobalVars.clear();
  }

  public List<ASTNodeSymbol> getGlobalVariables(String module) {
    return accGlobalVars.get(module);
  }

  public void addGlobalVariables(String module, Set<ASTNodeSymbol> globals) {
    List<ASTNodeSymbol> modGlobs = curGlobalVars.get(module);
    if (modGlobs == null) {
      modGlobs = new ArrayList<ASTNodeSymbol>(); 
      curGlobalVars.put(module, modGlobs);
    }
    for (ASTNodeSymbol esym : globals) {
      modGlobs.add(esym);
    }
  }

  public void clearModules() {
    modules.clear();
  }
  
  public List<Module> getModules() {
    return modules;
  }

  public void injectGlobalVariable(String module, String varName) {
    ASTNodeSymbol esym = new ASTNodeSymbol(varName);
    List<ASTNodeSymbol> modGlobs = curGlobalVars.get(module);
    if (modGlobs == null) {
      modGlobs = new ArrayList<ASTNodeSymbol>(); 
      curGlobalVars.put(module, modGlobs);
    }
    if (!modGlobs.contains(esym)) modGlobs.add(esym);
  }
}
