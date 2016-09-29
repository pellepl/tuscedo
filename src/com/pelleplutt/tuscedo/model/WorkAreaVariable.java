package com.pelleplutt.tuscedo.model;

import com.pelleplutt.tuscedo.ui.WorkArea;

public class WorkAreaVariable extends Variable {
  WorkArea area;

  public WorkAreaVariable(String name, WorkArea value) {
    super(name, value);
    this.area = value;
  }

}
