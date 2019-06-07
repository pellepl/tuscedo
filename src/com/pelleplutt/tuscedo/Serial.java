package com.pelleplutt.tuscedo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import com.pelleplutt.tuscedo.ui.UIWorkArea;
import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.io.Port;
import com.pelleplutt.util.io.PortConnector;

public class Serial implements SerialStreamProvider, Tickable {
  public static final int HW_FLOW_OFF = 0;
  public static final int HW_FLOW_CONST_HI = 1;
  public static final int HW_FLOW_CONST_LO = 2;
  public static final int HW_FLOW_FLANK_HI = 3;
  public static final int HW_FLOW_FLANK_LO = 4;
  PortConnector serial;
  InputStream serialIn;
  OutputStream serialOut;
  Thread serialPump;
  volatile boolean serialRun;
  volatile boolean serialRunning;
  List<OutputStream> attachedSerialIOs = new ArrayList<OutputStream>();
  Port setting;
  int hwFlowRTS = HW_FLOW_OFF;
  int hwFlowDTR = HW_FLOW_OFF;
  boolean rts = false;
  boolean dtr = false;
  Map<String, String[]> deviceExtraInfo = new HashMap<String, String[]>();
  
  final Object LOCK_SERIAL = new Object();

  byte serialBuf[] = new byte[256*64];
  volatile int serialBufIx = 0;
  
  UIWorkArea area;
  
  public Serial(UIWorkArea area) {
    this.area = area;
    serial = PortConnector.getPortConnector();
  }
  
  public void setHwFlowControl(int rts, int dtr) {
    this.hwFlowRTS = rts;
    this.hwFlowDTR = dtr;
  }
  
  public boolean isConnected() {
    return serialRunning;
  }
  
  public Port getSerialConfig() {
    return serialRunning ? setting : null;
  }
  
  public void transmit(String s) {
    if (serialRunning) {
      try {
        beforeTransmit();
        serialOut.write(s.getBytes());
        serialOut.flush();
        afterTransmit();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
  
  public void transmit(byte b[]) {
    if (serialRunning) {
      try {
        beforeTransmit();
        serialOut.write(b);
        serialOut.flush();
        afterTransmit();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
  
  void flowControl(boolean pre) throws IOException {
    if (hwFlowDTR == HW_FLOW_OFF && hwFlowRTS == HW_FLOW_OFF) return;
    boolean rtshigh = this.rts;
    boolean dtrhigh = this.dtr;
    if (hwFlowDTR == HW_FLOW_CONST_HI)      dtrhigh = true;
    else if (hwFlowDTR == HW_FLOW_CONST_LO) dtrhigh = false;
    else if (hwFlowDTR == HW_FLOW_FLANK_HI) dtrhigh = pre;
    else if (hwFlowDTR == HW_FLOW_FLANK_LO) dtrhigh = !pre;
    if (hwFlowRTS == HW_FLOW_CONST_HI)      rtshigh = true;
    else if (hwFlowRTS == HW_FLOW_CONST_LO) rtshigh = false;
    else if (hwFlowRTS == HW_FLOW_FLANK_HI) rtshigh = pre;
    else if (hwFlowRTS == HW_FLOW_FLANK_LO) rtshigh = !pre;
    serial.setRTSDTR(rtshigh, dtrhigh);
  }
  
  void beforeTransmit() throws IOException {
    flowControl(true);
  }
  
  void afterTransmit() throws IOException {
    flowControl(false);
  }
  
  public String[] getDevices() {
    deviceExtraInfo.clear();
    String[] rawDeviceNames = serial.getDevices();
    List<String>deviceNameList = new ArrayList<String>();
    for (String s : rawDeviceNames) {
      String[] i = s.split("\t");
      deviceNameList.add(i[0]);
      deviceExtraInfo.put(i[0], i);
    }
    return (String[])deviceNameList.toArray(new String[deviceNameList.size()]);
  }

  public void open(Port portSetting) throws Exception {
    synchronized (LOCK_SERIAL) {
      setting = portSetting;
      serial.connect(portSetting);
      serial.setTimeout(1000);
      serialIn = serial.getInputStream();
      serialOut = serial.getOutputStream();
      serialRun = true;
      serialPump = new Thread(serialEaterRunnable, "serial:" + portSetting.portName);
      serialPump.setDaemon(true);
      serialPump.start();
    }
  }
  
  public void openStdin() throws Exception {
    synchronized (LOCK_SERIAL) {
      serialIn = System.in;
      serialOut = System.out;
      serialRun = true;
      serialPump = new Thread(serialEaterRunnable, "stdin");
      serialPump.setDaemon(true);
      serialPump.start();
    }
  }
  
  public void closeSerial() {
    //Log.println("closing attached streams");
    synchronized(attachedSerialIOs) {
      for (OutputStream o : attachedSerialIOs) {
        AppSystem.closeSilently(o);
      }
      attachedSerialIOs.clear();
    }
    //Log.println("closing serial streams, running " + serialRunning);
    synchronized (LOCK_SERIAL) {
      if (serialIn != null) {
        serial.disconnectSilently();
        serialIn = null;
        serialOut = null;
        serialRun = false;
        while (serialRunning) {
          //Log.println("await close serial streams..");
          AppSystem.waitSilently(LOCK_SERIAL, 1000);
        }
      }
    }
    //Log.println("serial closed");
  }
  
  public void deattachSerialIO(OutputStream o) {
    synchronized (attachedSerialIOs) {
      //Log.println("deattach " + o);
      attachedSerialIOs.remove(o);
    }
  }
  
  public InputStream attachSerialIO() {
    XPipedInputStream pis = null;
    try {
      pis = new XPipedInputStream(64);
      PipedOutputStream pos;
      pos = new PipedOutputStream(pis);
      synchronized (attachedSerialIOs) {
        //Log.println("attach " + pos);
        attachedSerialIOs.add(pos);
      }
      pis.attached = pos;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return pis;
  }
  
  @Override
  public InputStream getSerialInputStream() {
    return attachSerialIO();
  }
  
  @Override
  public OutputStream getSerialOutputStream() {
    return serialOut;
  }
  
  final Runnable pushSerialToLogRunnable = new Runnable() {
    @Override
    public void run() {
      byte[] report = null;
      synchronized (serialBuf) {
        if (serialBufIx > 0) {
          report = new byte[serialBufIx];
          System.arraycopy(serialBuf, 0, report, 0, serialBufIx);
          serialBufIx = 0;
        }
      }
      if (report != null) area.onSerialData(report);
    }
  };

  final Runnable serialEaterRunnable = new Runnable() {
    @Override
    public void run() {
      int b;
      serialRunning = true;
      //Log.println("serial thread started");
      try {
        while (serialRun) {
          try {
            b = serialIn.read();
            if (b == -1) {
              serialRun = false;
              break;
            }
            synchronized(attachedSerialIOs) {
              for (OutputStream o : attachedSerialIOs) {
                try {
                  o.write(b);
                  o.flush();
                } catch (IOException ioe) {
                }
              }
            }
            synchronized (serialBuf) {
              if (serialBufIx < serialBuf.length) {
                serialBuf[serialBufIx++] = (byte)b;
              }
            }
            if (b == '\n' || serialBufIx >= serialBuf.length/64) {
              SwingUtilities.invokeLater(pushSerialToLogRunnable);
            }
          } catch (SocketTimeoutException ste) {
            // ignore
          }
        } // while
      } catch (IOException e) {
        //e.printStackTrace();
      }
      finally {
        //Log.println("serial thread dead, enter serial lock");
        synchronized (LOCK_SERIAL) {
          serialRunning = false;
          serialRun = false;
          area.onSerialDisconnect();
          //Log.println("serial thread dead, notify");
          LOCK_SERIAL.notifyAll();
        }
      }
    } // run
  };
  
  
  class XPipedInputStream extends PipedInputStream {
    public OutputStream attached;
    public XPipedInputStream(int buf) {
      super(buf);
    }
    @Override
    public void close() throws IOException {
      //Log.println("closing attached stream " + this);
      super.close();
      deattachSerialIO(attached);
    }
  }


  @Override
  public void tick() {
    if (serialRunning) {
      SwingUtilities.invokeLater(pushSerialToLogRunnable);
    }
  }

  public void setRTSDTR(boolean rtshigh, boolean dtrhigh) throws IOException {
    rts = rtshigh;
    dtr = dtrhigh;
    serial.setRTSDTR(rtshigh, dtrhigh);
  }

  public String[] getExtraInfo(String port) {
    return deviceExtraInfo.get(port);
  }
}
