package com.pelleplutt.tuscedo.model;

import com.pelleplutt.tuscedo.ui.UIWorkArea;

public class WorkAreaVariable extends Variable {
  UIWorkArea area;

  public WorkAreaVariable(String name, UIWorkArea value) {
    super(name, value);
    this.area = value;
  }

}
