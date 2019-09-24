package com.pelleplutt.tuscedo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import com.pelleplutt.tuscedo.ui.UIWorkArea;
import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.Log;
import com.pelleplutt.util.io.Port;
import com.pelleplutt.util.io.PortConnector;

public class Serial implements SerialStreamProvider, Tickable {
  public static final int USER_HW_FLOWOFF = 0;
  public static final int USER_HW_FLOWCONST_HI = 1;
  public static final int USER_HW_FLOWCONST_LO = 2;
  public static final int USER_HW_FLOWFLANK_HI = 3;
  public static final int USER_HW_FLOWFLANK_LO = 4;
  public static final int FLOW_CTRL_NONE = 0;
  public static final int FLOW_XON_XOFF = (1<<0);
  public static final int FLOW_RTS_CTS = (1<<1);
  public static final int FLOW_DSR_DTR = (1<<2);
  PortConnector serial;
  InputStream serialIn;
  OutputStream serialOut;
  Thread serialPump;
  volatile Runnable onClose = null;
  volatile boolean serialRun;
  volatile boolean serialRunning;
  List<OutputStream> attachedSerialIOs = new ArrayList<OutputStream>();
  Port setting;
  int userHwFlowRTS = USER_HW_FLOWOFF;
  int userHwFlowDTR = USER_HW_FLOWOFF;
  int flowctrl;
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
  
  public void setFlowControl(int flowctrl) {
    this.flowctrl = flowctrl;
    setting.xonxoff = (flowctrl & FLOW_XON_XOFF) == FLOW_XON_XOFF;
    setting.rtscts = (flowctrl & FLOW_RTS_CTS)  == FLOW_RTS_CTS;
    setting.dsrdtr = (flowctrl & FLOW_DSR_DTR) == FLOW_DSR_DTR;
    try {
      serial.configure(setting);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void setUserHwFlowControl(int rts, int dtr) {
    this.userHwFlowRTS = rts;
    this.userHwFlowDTR = dtr;
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
    if (userHwFlowDTR == USER_HW_FLOWOFF && userHwFlowRTS == USER_HW_FLOWOFF) return;
    boolean rtshigh = this.rts;
    boolean dtrhigh = this.dtr;
    if (userHwFlowDTR == USER_HW_FLOWCONST_HI)      dtrhigh = true;
    else if (userHwFlowDTR == USER_HW_FLOWCONST_LO) dtrhigh = false;
    else if (userHwFlowDTR == USER_HW_FLOWFLANK_HI) dtrhigh = pre;
    else if (userHwFlowDTR == USER_HW_FLOWFLANK_LO) dtrhigh = !pre;
    if (userHwFlowRTS == USER_HW_FLOWCONST_HI)      rtshigh = true;
    else if (userHwFlowRTS == USER_HW_FLOWCONST_LO) rtshigh = false;
    else if (userHwFlowRTS == USER_HW_FLOWFLANK_HI) rtshigh = pre;
    else if (userHwFlowRTS == USER_HW_FLOWFLANK_LO) rtshigh = !pre;
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

  boolean isSerial = false;
  
  public void openSerial(Port portSetting) throws Exception {
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
      isSerial = true;
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
      isSerial = false;
    }
  }
  
  public void openSocket(String server, int port) throws Exception  {
    synchronized (LOCK_SERIAL) {
      final Socket s = new Socket(server, port);
      onClose = new Runnable() { public void run() { try {
        Log.println("close socket"); s.close();
      } catch (IOException e) {} } };
      serialIn = s.getInputStream();
      serialOut = s.getOutputStream();
      serialRun = true;
      serialPump = new Thread(serialEaterRunnable, "socket:" + server + ":" + port);
      serialPump.setDaemon(true);
      serialPump.start();
      isSerial = false;
    }
  }
  
  public void openServerSocket(int port) throws Exception {
    synchronized (LOCK_SERIAL) {
      final ServerSocket ss = new ServerSocket(port);
      onClose = new Runnable() { public void run() { try {
        Log.println("close server socket"); ss.close();
      } catch (IOException e) {e.printStackTrace();} } };
      try {
        ss.setReuseAddress(true);
        ss.setSoTimeout(3000);
        final Socket s = ss.accept();
        onClose = new Runnable() { public void run() { try {
          Log.println("close server socket"); s.close(); ss.close();
        } catch (IOException e) {e.printStackTrace();} } };
        serialIn = s.getInputStream();
        serialOut = s.getOutputStream();
        serialRun = true;
        serialPump = new Thread(serialEaterRunnable, "serversocket:" + port);
        serialPump.setDaemon(true);
        serialPump.start();
        isSerial = false;
      } finally {
        ss.close();
      }
    }
  }
  
  public void openFile(String file) throws Exception  {
    synchronized (LOCK_SERIAL) {
      File f = new File(file);
      serialIn = new FileInputStream(f);
      serialOut = null;
      serialRun = true;
      serialPump = new Thread(serialEaterRunnable, "file:" + file);
      serialPump.setDaemon(true);
      serialPump.start();
      isSerial = false;
    }
  }
  
  public void openJlinkRTT(String idOrArgs) throws Exception {
    synchronized (LOCK_SERIAL) {
      String exe = Settings.inst().string("exe_jlink.string");
      String args = Settings.inst().string("exe_jlink_args.string");
      if (args == null) args = "";
      if (exe == null || exe.trim().length() == 0) {
        throw new FileNotFoundException("No path to jlink set. Please issue conf.exe_jlink=\"<path>\" in script mode.");
      }
      if (idOrArgs != null && idOrArgs.trim().length() > 0) {
        if (!idOrArgs.contains("-")) {
          args += "-SelectEmuBySN " + idOrArgs;
        } else {
          args += idOrArgs;
        }
      }
      final JLinkProc proc = new JLinkProc(exe, args);
      AppSystem.addDisposable(proc);


      proc.start(new JLinkProc.Callback() {
        @Override
        public void onConnected(JLinkProc p) {
          serialIn = p.getInputStream();
          serialOut = p.getOutputStream();
          serialRun = true;
          serialPump = new Thread(serialEaterRunnable, "rtt:" + idOrArgs);
          serialPump.setDaemon(true);
          serialPump.start();
          isSerial = false;
        }

        @Override
        public void onClosed(JLinkProc p, Throwable t) {
          if (t != null) t.printStackTrace();
          AppSystem.dispose(p);
          Serial.this.close();
        }
      });
      
      onClose = new Runnable() { public void run() { Log.println("close rtt proc"); AppSystem.dispose(proc); } };
    }
  }
  
  public void close() {
    //Log.println("closing attached streams");
    synchronized(attachedSerialIOs) {
      for (OutputStream o : attachedSerialIOs) {
        AppSystem.closeSilently(o);
      }
      attachedSerialIOs.clear();
    }
    //Log.println("closing serial streams, running " + serialRunning);
    
    if (onClose != null) {
      try {
        onClose.run();
      } catch (Throwable t) {
        t.printStackTrace();
      }
      onClose = null;
    }
    
    synchronized (LOCK_SERIAL) {
      if (serialIn != null) {
        if (isSerial) serial.disconnectSilently();
        serialIn = null;
        serialOut = null;
        serialRun = false;
        int spoonGuard = 8;
        while (serialRunning && --spoonGuard > 0) {
          Log.println("awaiting close serial streams..");
          AppSystem.waitSilently(LOCK_SERIAL, 1000);
        }
        if (spoonGuard == 0) {
          Log.println("WARNING: close time out - lingering connection");
          serialRunning = false;
        }
        isSerial = false; 
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
