package com.pelleplutt.tuscedo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pelleplutt.util.Log;

public class Bash implements ProcessGroup.Console {
  ProcessHandler handler;
  File pwd;
  Console console;
  static Pattern argBreak = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");
  List<List<SubCommand>> chain;
  volatile int chainIx;
  Settings settings = Settings.inst();
  SerialStreamProvider serialProvider;

  public Bash(ProcessHandler ph, Console c, SerialStreamProvider ssp) {
    handler = ph;
    console = c;
    pwd = new File(".");
    serialProvider = ssp;
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
  
  
  void parseCommand(String args[]) {
    String strChain = settings.string(Settings.BASH_CHAIN_STRING);
    String strOutfile = settings.string(Settings.BASH_OUTPUT_STRING);
    String strOutappend = settings.string(Settings.BASH_APPEND_STRING);
    String strInfile = settings.string(Settings.BASH_INPUT_STRING);
    String strPipe = settings.string(Settings.BASH_PIPE_STRING);
    
    List<List<SubCommand>> cmds = new ArrayList<List<SubCommand>>();
    List<SubCommand> subCmds = new ArrayList<SubCommand>();

    SubCommand sc = null;
    List<String> curArgs = null;
    
    for (int i = 0; i < args.length; i++) {
      String arg = args[i].trim();
      if (arg.length() == 0) continue;
      
      if (arg.equals(strChain)) {
        sc.args = curArgs == null ? sc.args : curArgs.toArray(new String[curArgs.size()]);
        subCmds.add(sc);
        cmds.add(subCmds);
        subCmds = new ArrayList<SubCommand>();
        sc = new SubCommand();
        curArgs = null;
      }
      else if (arg.equals(strOutfile)) {
        sc.stdout = FILE;
        sc.stdoutPath = args[++i].trim();
      }
      else if (arg.equals(strOutappend)) {
        sc.stdout = FILE_APPEND;
        sc.stdoutPath = args[++i].trim();
      }
      else if (arg.equals(strInfile)) {
        sc.stdin = FILE;
        sc.stdinPath = args[++i].trim();
      }
      else if (arg.equals(strPipe)) {
        sc.args = curArgs == null ? sc.args : curArgs.toArray(new String[curArgs.size()]);
        sc.stdout = PIPE;
        subCmds.add(sc);
        sc = new SubCommand();
        sc.stdin = PIPE;
        curArgs = null;
      } else {
        if (sc == null) sc = new SubCommand();
        if (curArgs == null) curArgs = new ArrayList<String>();
        curArgs.add(arg);
      }
    }
    if (sc != null) {
      sc.args = curArgs == null ? sc.args : curArgs.toArray(new String[curArgs.size()]);
      subCmds.add(sc);
      cmds.add(subCmds);
    }
    
    chain = cmds;
  }
  
  public void start() throws IOException {
    Log.println("start chain ix " + chainIx + " of " + chain.size());
    if (chainIx < chain.size()) {
      List<SubCommand> commands = chain.get(chainIx++);
      startProcessGroup(commands);
    }
  }
  
  void startProcessGroup(List<SubCommand> cmds) throws IOException {
    final String strSerial = settings.string(Settings.BASH_SERIAL_STREAM_STRING);
    ProcessGroup pg = new ProcessGroup();
    InputStream serialInputStream = null;
    OutputStream serialOutputStream = null;
    for (SubCommand c : cmds) {
      boolean serialStdin = false;
      boolean serialStdout = false;
      
      if (c.stdinPath != null && c.stdinPath.equals(strSerial)) {
        c.stdinPath = null;
        if (serialInputStream == null) {
          serialInputStream = serialProvider.getSerialInputStream();
        }
        serialStdin = serialInputStream != null;
        Log.println("subcommand " + c.args[0] + " stdin from serial");
      }
      if (c.stdoutPath != null && c.stdoutPath.equals(strSerial)) {
        c.stdoutPath = null;
        if (serialOutputStream == null) {
          serialOutputStream = serialProvider.getSerialOutputStream();
        }
        serialStdout = serialOutputStream != null;
        Log.println("subcommand " + c.args[0] + " stdout to serial");
      }
      pg.addCmd(c.args, pwd, 
          c.stdin == PIPE, 
          c.stdinPath, 
          c.stdoutPath,
          null,
          serialStdin,
          serialStdout,
          false);
    }
    pg.start(serialInputStream, serialOutputStream, null, this);
    handler.linkToProcess(pg);
  }
  
  public void input(String input) {
    String[] args = breakArgs(input);
    try {
      if (handler.isLinkedToProcess()) {
        
        // process linked, send input to process
        handler.sendToStdIn(input);
        
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

  static final int CONSOLE = 0;
  static final int FILE = 1;
  static final int FILE_APPEND = 2;
  static final int PIPE = 3;
  static final int SERIAL = 4;
  
  class SubCommand {
    String args[];
    int stdin = CONSOLE;
    String stdinPath;
    int stdout = CONSOLE;
    String stdoutPath;
    int stderr = CONSOLE;
    String stdoutErr;
  }

  List<String> bashCompletions = new ArrayList<String>(); 
  public List<String> suggestFileSystemCompletions(String prefix, String s, String cmd, boolean includeFiles, boolean includeDirs) {
    if (s.startsWith(cmd + " ")) {
      final int cmdIx = cmd.length() + 1;
      bashCompletions.clear();
      int lastFS = s.lastIndexOf(File.separator);
      String path = lastFS < 0 ? "" : s.substring(cmdIx, lastFS+1);
      String filter = lastFS < 0 ? s.substring(cmdIx) : s.substring(lastFS+1);
      
      File srcDir;
      if (path.startsWith(File.separator)) {
        srcDir = new File(path);
      } else if (path.startsWith("~" + File.separator)) {
        srcDir = new File(System.getProperty("user.home"), path.substring(2));
      } else {
        srcDir = new File(pwd, path);
      }

      File[] files = srcDir.listFiles();
      if (files == null) {
        return null;
      }
      for (File f : files) {
        try {
          if (f.getName().startsWith(filter) && 
              (
                  (includeDirs && f.isDirectory()) || 
                  (includeFiles && f.isFile())
              )) {
            String c = prefix + s + f.getName().substring(filter.length());
            bashCompletions.add(c);
          }
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
      return bashCompletions;
    }
    return null;
  }

  @Override
  public void outln(String s) {
    console.stdout(s + "\n");
  }

  @Override
  public void errln(String s) {
    console.stderr(s + "\n");
  }

  @Override
  public void exit(int ret) {
    if (ret == 0) {
      try {
        if (chainIx < chain.size()) {
          start();
        } else {
          handler.unlinkFromProcess();
        }
      } catch (Throwable t) {
        handler.unlinkFromProcess();
        t.printStackTrace();
      }
    } else {
      handler.unlinkFromProcess();
    }
    
  }
}
