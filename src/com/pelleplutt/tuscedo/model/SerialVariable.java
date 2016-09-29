package com.pelleplutt.tuscedo.model;

import com.pelleplutt.tuscedo.Serial;

public class SerialVariable extends Variable {
  Serial serial;

  public SerialVariable(String name, Serial value) {
    super(name, value);
    this.serial = value;
    this.set(Variable.INPUTSTREAM, new InVariable());
    this.set(Variable.OUTPUTSTREAM, new OutVariable());
  }
  
  class InVariable extends Variable {
    public InVariable() {
      super(Variable.INPUTSTREAM, SerialVariable.this);
    }
    
    public Object getValue() {
      return serial.getSerialInputStream();
    }
  }

  class OutVariable extends Variable {
    public OutVariable() {
      super(Variable.OUTPUTSTREAM, SerialVariable.this);
    }
    
    public Object getValue() {
      return serial.getSerialOutputStream();
    }
  }
}
