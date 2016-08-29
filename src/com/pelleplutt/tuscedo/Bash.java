package com.pelleplutt.tuscedo;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.Log;

public class Bash {
  ProcessHandler handler;
  File pwd;
  Console console;
  static Pattern argBreak = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");
  List<SubCommand> chain;
  volatile int chainIx;

  public Bash(ProcessHandler ph, Console c) {
    handler = ph;
    console = c;
    pwd = new File(".");
  }
  
  String[] breakArgs(String input) {
    List<String> list = new ArrayList<String>();
    Matcher m = argBreak.matcher(input);
    while (m.find()) {
      list.add(m.group(1).replace("\"", ""));
    }
    return list.toArray(new String[list.size()]);
  }
  
  void cd(String args[]) throws IOException {
    File newPwd;
    if (args.length < 2 || args[1].trim().length() == 0) {
      console.stdout(pwd.getCanonicalPath() + "\n");
    } else {
      String path = args[1].trim();
      if (path.startsWith(File.separator)) {
        newPwd = new File(path);
      } else if (path.startsWith("~")) {
        path = path.replace("~", System.getProperty("user.home"));
        newPwd = new File(path);
      } else {
        newPwd = new File(pwd, path);
      }
      if (!newPwd.exists()) {
        console.stderr(newPwd.getCanonicalPath() + ": No such directory\n");
      } else if (!newPwd.isDirectory()) {
        console.stderr(newPwd.getCanonicalPath() + ": Not a directory\n");
      } else {
        pwd = newPwd;
        console.stdout(pwd.getCanonicalPath() + "\n");
      }
    }
  }
  
  void startProcess(final SubCommand cmd, final SubCommand preCmd) throws IOException {
    Log.println(cmd.args[0] + " starting, in:" + cmd.stdin + " out:" + cmd.stdout);
    final ProcessHandler pHandler = handler;
    final Process process = Runtime.getRuntime().exec(cmd.args, null, pwd);
    pHandler.linkToProcess(process);
    
    if (cmd.stdin == STDIN_PROCESS) {
      Thread processIn = new Thread(new Runnable() {
        public void run() {
          BufferedReader in = null;
          try {
            in = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(preCmd.buf.toByteArray())));
            String stdin;
            while ((stdin = in.readLine()) != null) {
              console.stdin(stdin + "\n");
            }
          } catch (Throwable t) {
          }
          finally {
            handler.closeStdin();
            AppSystem.closeSilently(in);
          }
        }
      }, "procin:" + cmd.args[0]);
      processIn.setDaemon(true);
      processIn.start();
    }
    
    Thread processOut = new Thread(new Runnable() {
      public void run() {
        try {
          BufferedReader out = new BufferedReader(
              new InputStreamReader(pHandler.stdout()));
          String stdout;
          if (cmd.stdout == STDOUT_PROCESS) {
            cmd.buf = new ByteArrayOutputStream();
          }
          while ((stdout = out.readLine()) != null) {
            switch (cmd.stdout) {
            case STDOUT_FILE:
            case STDOUT_FILE_APPEND:
              break;
            case STDOUT_PROCESS:
              cmd.buf.write((stdout + "\n").getBytes());
              cmd.buf.flush();
              break;
            case STDOUT_SERIAL:
              break;
            default:
              console.stdout(stdout + "\n");
              break;
            }
          }
        } catch (Throwable t) {
        }
      }
    }, "procout:" + cmd.args[0]);
    cmd.thrOut = processOut;
    processOut.setDaemon(true);
    processOut.start();
    
    Thread processErr = new Thread(new Runnable() {
      public void run() {
        try {
          BufferedReader err = new BufferedReader(
              new InputStreamReader(pHandler.stderr()));
          String stderr;
          while ((stderr = err.readLine()) != null) {
            console.stderr(stderr + "\n");
          }
        } catch (Throwable t) {
        }
      }
    }, "procerr:" + cmd.args[0]);
    processErr.setDaemon(true);
    processErr.start();
    
    
    Thread processWatch = new Thread(new Runnable() {
      public void run() {
        int ret = -1;
        try {
          ret = process.waitFor();
          cmd.thrOut.join();
        } catch (InterruptedException e) {
        }
        Log.println(cmd.args[0] + " exited " + ret);
        pHandler.unlinkFromProcess();
        if (ret != 0) {
          chain = null;
          chainIx = 0;
        } else if (!chain.isEmpty() && chainIx <= chain.size()-1) {
          SubCommand sc = chain.get(chainIx++);
          try {
            startProcess(sc, cmd);
          } catch (IOException e) {
            pHandler.unlinkFromProcess();
            console.stderr(e.getMessage() + "\n");
            e.printStackTrace();
          }
        } else {
          chain = null;
          chainIx = 0;
        }
      }
    }, "proc:" + cmd.args[0]);
    processWatch.setDaemon(true);
    processWatch.start();
  }
  
  void parseCommand(String args[]) {
    String strChain = Settings.inst().string(Settings.BASH_CHAIN_STRING);
    String strOutfile = Settings.inst().string(Settings.BASH_OUTPUT_STRING);
    String strOutappend = Settings.inst().string(Settings.BASH_APPEND_STRING);
    String strInfile = Settings.inst().string(Settings.BASH_INPUT_STRING);
    String strPipe = Settings.inst().string(Settings.BASH_PIPE_STRING);
    
    List<SubCommand> cmds = new ArrayList<SubCommand>();
    SubCommand sc = null;
    List<String> curArgs = null;
    
    for (int i = 0; i < args.length; i++) {
      String arg = args[i].trim();
      if (arg.length() == 0) continue;
      
      if (arg.equals(strChain)) {
        sc.args = curArgs == null ? sc.args : curArgs.toArray(new String[curArgs.size()]);
        cmds.add(sc);
        sc = new SubCommand();
        curArgs = null;
      }
      else if (arg.equals(strOutfile)) {
        sc.stdout = STDOUT_FILE;
        sc.stdoutPath = args[++i].trim();
      }
      else if (arg.equals(strOutappend)) {
        sc.stdout = STDOUT_FILE_APPEND;
        sc.stdoutPath = args[++i].trim();
      }
      else if (arg.equals(strInfile)) {
        sc.stdin = STDIN_FILE;
        sc.stdinPath = args[++i].trim();
      }
      else if (arg.equals(strPipe)) {
        sc.args = curArgs == null ? sc.args : curArgs.toArray(new String[curArgs.size()]);
        sc.stdout = STDOUT_PROCESS;
        cmds.add(sc);
        sc = new SubCommand();
        sc.stdin = STDIN_PROCESS;
        curArgs = null;
      } else {
        if (sc == null) {
          sc = new SubCommand();
        }
        if (curArgs == null) curArgs = new ArrayList<String>();
        curArgs.add(arg);
      }
    }
    if (sc != null) {
      sc.args = curArgs == null ? sc.args : curArgs.toArray(new String[curArgs.size()]);
      cmds.add(sc);
    }
    
    chain = cmds;
  }
  
  public void start() throws IOException {
    SubCommand sc = chain.get(chainIx++);
    startProcess(sc, null);
  }
  
  public void input(String input) {
    String[] args = breakArgs(input);
    try {
      if (handler.isLinkedToProcess()) {
        
        // process linked, send input to process
        handler.stdin().write((input + "\n").getBytes());
        handler.stdin().flush();
        
      } else if (args[0].toLowerCase().equals("cd")) {
        
        cd(args);
        
      } else {
        
        chain = null;
        chainIx = 0;
        parseCommand(args);
        start();
        
      }
    } catch (Exception e) {
      console.stderr(e.getMessage() + "\n");
      e.printStackTrace();
    }
  }

  interface Console {
    void stdout(String s);

    void stderr(String s);
    
    void stdin(String s);
  }

  static final int STDIN_CONSOLE = 0;
  static final int STDIN_FILE = 1;
  static final int STDIN_PROCESS = 3;
  static final int STDIN_SERIAL = 4;
  
  static final int STDOUT_CONSOLE = 0; 
  static final int STDOUT_FILE = 1; 
  static final int STDOUT_FILE_APPEND = 2; 
  static final int STDOUT_PROCESS = 3; 
  static final int STDOUT_SERIAL = 4; 
  
  class SubCommand {
    String args[];

    int stdin = STDIN_CONSOLE;
    String stdinPath;

    int stdout = STDOUT_CONSOLE;
    String stdoutPath;
    
    Thread thrOut;
    
    ByteArrayOutputStream buf;
  }
}
