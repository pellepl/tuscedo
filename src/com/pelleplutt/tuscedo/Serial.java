package com.pelleplutt.tuscedo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import com.pelleplutt.tuscedo.ui.WorkArea;
import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.io.Port;
import com.pelleplutt.util.io.PortConnector;

public class Serial implements SerialStreamProvider {
  PortConnector serial;
  InputStream serialIn;
  OutputStream serialOut;
  Thread serialPump;
  volatile boolean serialRun;
  volatile boolean serialRunning;
  List<OutputStream> attachedSerialIOs = new ArrayList<OutputStream>();
  Port setting;
  
  final Object LOCK_SERIAL = new Object();

  byte serialBuf[] = new byte[256*64];
  volatile int serialBufIx = 0;
  
  WorkArea area;
  
  public Serial(WorkArea area) {
    this.area = area;
    serial = PortConnector.getPortConnector();
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
        serialOut.write(s.getBytes());
        serialOut.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
  
  public String[] getDevices() {
    return serial.getDevices();
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
      String t;
      synchronized (serialBuf) {
        t = new String(serialBuf, 0, serialBufIx);
        serialBufIx = 0;
      }
      area.onSerialData(t);
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
}
