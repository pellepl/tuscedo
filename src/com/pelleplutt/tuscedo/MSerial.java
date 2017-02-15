package com.pelleplutt.tuscedo;

import com.pelleplutt.operandi.Compiler;
import com.pelleplutt.tuscedo.ui.UIWorkArea;

public class MSerial extends MObj {
  public MSerial(UIWorkArea wa, Compiler comp) {
    super(wa, comp, "serial");
  }

  public void init(UIWorkArea wa, com.pelleplutt.operandi.Compiler comp) {
    this.workarea = wa;
    addFunc("info", OperandiScript.FN_SERIAL_INFO, comp);
    addFunc("disconnect", OperandiScript.FN_SERIAL_DISCONNECT, comp);
    addFunc("connect", OperandiScript.FN_SERIAL_CONNECT, comp);
    addFunc("tx", OperandiScript.FN_SERIAL_TX, comp);
    addFunc("on_rx", OperandiScript.FN_SERIAL_ON_RX, comp);
    addFunc("clear_on_rx", OperandiScript.FN_SERIAL_ON_RX_CLEAR, comp);
    addFunc("list_on_rx", OperandiScript.FN_SERIAL_ON_RX_LIST, comp);
  }
}
