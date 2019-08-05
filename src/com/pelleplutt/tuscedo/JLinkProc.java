package com.pelleplutt.tuscedo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.pelleplutt.util.AppSystem.Disposable;
import com.pelleplutt.util.Log;

public class JLinkProc implements Disposable {
  static int g_port = 32100 + (int)(Math.random() * 100);
  static int g_ports = 0;
  int port;
  String exe, args;
  Process process;
  Socket socket;
  InputStream rttIn;
  OutputStream rttOut;
  
  public JLinkProc(String exe, String args) {
    this.exe = exe;
    this.args = args;
    
  }
  
  public void start(final Callback callback) throws IOException {
    if (process != null) return;
    //JLink.exe -device NRF52840_XXAA -If SWD -RTTTelnetPort 11277 -SelectEmuBySN 483065663 -Speed 40000
    List<String> conspl = new ArrayList<String>();
    conspl.add(exe);

    if (!args.toLowerCase().contains("-device")) {
      args = "-device NRF52840_XXAA " + args;
    }
    if (!args.toLowerCase().contains("-if")) {
      args = "-If SWD " + args;
    }
    if (!args.toLowerCase().contains("-speed")) {
      args = "-Speed 4000 " + args;
    }

    port = g_port + g_ports++;
    if (g_ports > 2311) g_ports = 0;
    args += " -RTTTelnetPort " + port;
    
    for (String arg : args.split("\\s")) conspl.add(arg);
    
    ProcessBuilder pb = new ProcessBuilder(conspl);
    pb.redirectErrorStream(true);
    process = pb.start();
    Log.println(conspl.toString());
    if (process != null) {
      process.getOutputStream().write("r\r\n".getBytes()); // issue reset via JLink commander
      process.getOutputStream().flush();
    }
    Thread watcher = new Thread(new Runnable() {
      public void run() {
        Log.println("RTT JLINK watcher started port " + port);
        Throwable err = null;
        try {
          //Connecting to J-Link via USB...O.K.
          //Connecting to J-Link via USB...FAILED: Cannot connect to J-Link via USB.
          // For some reason we do not get the output from the process
//          boolean connected = false;
//          InputStream pis = process.getInputStream();
//          BufferedReader in = new BufferedReader(new InputStreamReader(pis));
//          String l;
//          while ((l = in.readLine()) != null) {
//            Log.println("> " + l);
//            if (l.contains("Connecting to J-Link")) {
//              if (l.contains("O.K.")) {
//                connected = true;
//              } else {
//                connected = false;
//                err = new IOException("Could not connect:" + l);
//              }
//              break;
//            }
//          }
//          Log.println("RTT JLINK connected:" + connected);
//          if (!connected && process != null && !process.isAlive()) {
//            err = new IOException("return code " + process.exitValue());
//          } else if (connected) 
          {
            // instead of parsing outputs and stuff, simply try to open the socket 
            socket = new Socket("localhost", port);
            rttIn = socket.getInputStream();
            rttOut = socket.getOutputStream();
            if (callback != null) callback.onConnected(JLinkProc.this);
            process.waitFor();
          }
        }
        catch (Throwable t) {err = t;}
        if (callback != null) callback.onClosed(JLinkProc.this, err);
        Log.println("RTT JLINK watcher stopped " + port);
      }
    }, "jlink-watcher");
    watcher.setDaemon(true);
    watcher.start();
  }
  
  public void stop() {
    try {if (socket != null) socket.close();} catch (Throwable t) {}
    try {if (process != null) process.destroyForcibly();} catch (Throwable t) {}
    socket = null;
    process = null;
  }
  
  public InputStream getInputStream() {
    return rttIn;
  }

  public OutputStream getOutputStream() {
    return rttOut;
  }

  public interface Callback {
    public void onConnected(JLinkProc p);
    public void onClosed(JLinkProc p, Throwable t);
  }

  @Override
  public void dispose() {
    stop();
  }
}
