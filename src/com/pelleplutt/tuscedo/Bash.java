package com.pelleplutt.tuscedo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
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
  int lastReturnCode = 0;

  public Bash(ProcessHandler ph, Console c, SerialStreamProvider ssp) {
    handler = ph;
    console = c;
    pwd = new File(".");
    serialProvider = ssp;
  }
  
  String[] breakArgs(String input) {
    input = input.trim();
    List<String> list = new ArrayList<String>();
    char quoteChar = 0;
    StringBuilder sb = null;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (quoteChar != 0) {
        sb.append(c);
        if (c == quoteChar) {
          list.add(sb.toString());
          sb = null;
          quoteChar = 0;
        }
        continue;
      } else if (c == ' ' || c == '\t') {
        if (sb != null) {
          list.add(sb.toString());
          sb = null;
        }
        continue;
      } else if (c == '=') {
        if (sb != null) {
          list.add(sb.toString());
        }
        list.add(Character.toString(c));
        sb = null;
        continue;
      } else if (c == '\'' || c == '"') {
        quoteChar = c;
      }
      if (sb == null) {
        sb = new StringBuilder();
      }
      sb.append(c);
    }
    if (sb != null) {
      list.add(sb.toString());
    }
    Log.println(list.toString());
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
    final String strChain = settings.string(Settings.BASH_CHAIN_STRING);
    final String strOutfile = settings.string(Settings.BASH_OUTPUT_STRING);
    final String strOutappend = settings.string(Settings.BASH_APPEND_STRING);
    final String strErrfile = settings.string(Settings.BASH_ERR_OUTPUT_STRING);
    final String strErrappend = settings.string(Settings.BASH_ERR_APPEND_STRING);
    final String strInfile = settings.string(Settings.BASH_INPUT_STRING);
    final String strPipe = settings.string(Settings.BASH_PIPE_STRING);
    final String strLastRet = settings.string(Settings.BASH_LAST_RET_STRING);
    
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
        try {new File(sc.stdoutPath).delete();} catch (Throwable t) {}
      }
      else if (arg.equals(strOutappend)) {
        sc.stdout = FILE_APPEND;
        sc.stdoutPath = args[++i].trim();
      }
      else if (arg.equals(strErrfile)) {
        sc.stderr = FILE;
        sc.stderrPath = args[++i].trim();
        try {new File(sc.stderrPath).delete();} catch (Throwable t) {}
      }
      else if (arg.equals(strErrappend)) {
        sc.stderr = FILE_APPEND;
        sc.stderrPath = args[++i].trim();
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
        if (arg.equals(strLastRet)) {
          arg = Integer.toString(lastReturnCode);
        }
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
    final String strConn = settings.string(Settings.BASH_CONNECTION_STREAM_STRING);
    ProcessGroup pg = new ProcessGroup();
    InputStream serialInputStream = null;
    OutputStream serialOutputStream = null;
    for (SubCommand c : cmds) {
      boolean serialStdin = false;
      boolean serialStdout = false;
      
      if (c.stdinPath != null && c.stdinPath.equals(strConn)) {
        c.stdinPath = null;
        if (serialInputStream == null) {
          serialInputStream = serialProvider.getSerialInputStream();
        }
        serialStdin = serialInputStream != null;
        Log.println("subcommand " + c.args[0] + " stdin from conn");
      }
      if (c.stdoutPath != null && c.stdoutPath.equals(strConn)) {
        c.stdoutPath = null;
        if (serialOutputStream == null) {
          serialOutputStream = serialProvider.getSerialOutputStream();
        }
        serialStdout = serialOutputStream != null;
        Log.println("subcommand " + c.args[0] + " stdout to conn");
      }
      pg.addCmd(c.args, pwd, 
          c.stdin == PIPE, 
          c.stdinPath, 
          c.stdoutPath,
          c.stderrPath,
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
        
      } else if (args.length > 1 && args[1].equals("=")) {
        
        // TODO, perhaps, if ever...
        
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
    String stderrPath;
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
    lastReturnCode = ret;
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
