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
    addFunc("save", OperandiScript.FN_SERIAL_SAVE, comp);
    addFunc("on_rx", OperandiScript.FN_SERIAL_ON_RX, comp);
    addFunc("on_rx_clear", OperandiScript.FN_SERIAL_ON_RX_CLEAR, comp);
    addFunc("on_rx_list", OperandiScript.FN_SERIAL_ON_RX_LIST, comp);
    addFunc("on_rx_replay", OperandiScript.FN_SERIAL_ON_RX_REPLAY, comp);
    addFunc("log_start", OperandiScript.FN_SERIAL_LOG_START, comp);
    addFunc("log_stop", OperandiScript.FN_SERIAL_LOG_STOP, comp);
    addFunc("log_await", OperandiScript.FN_SERIAL_LOG_AWAIT, comp);
    addFunc("log_get", OperandiScript.FN_SERIAL_LOG_GET, comp);
    addFunc("log_size", OperandiScript.FN_SERIAL_LOG_SIZE, comp);
    addFunc("set_rts_dtr", OperandiScript.FN_SERIAL_SET_RTS_DTR, comp);
    addFunc("set_hw_flow", OperandiScript.FN_SERIAL_SET_HW_FLOW_CONTROL, comp);
  }
}
