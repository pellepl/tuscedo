package com.pelleplutt.tuscedo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.pelleplutt.util.AppSystem;
import com.pelleplutt.util.AppSystem.Disposable;
import com.pelleplutt.util.Log;

public class ProcessGroup implements Disposable {

  static final int READER_TICK_RATE_MIN_MS = 2;
  static final int READER_TICK_RATE_MAX_MS = 200;
  
  static final int CONSOLE = 0;
  static final int FILE = 1;
  static final int PIPE = 2;
  static final int OTHER = 3;
  
  OutputStream stdin;
  InputStream stdout;
  InputStream stderr;
  private boolean background;
  ProcessConsole cons;
  public final int id;
  
  List<Command> commands = new ArrayList<Command>();
  
  public ProcessGroup(int id) {
    this.id = id;
  }
  
  public synchronized void setBackground(boolean b) {
    background = b;
  }
  
  public synchronized boolean isBackground() {
    return background;
  }
  
  void startProcess(final Command cmd, 
      final InputStream toStdIn, final OutputStream toStdOut, final OutputStream toStdErr) throws IOException {
    Log.println(cmd.args[0] + " starting, in:" + cmd.stdin + " out:" + cmd.stdout + " err:" + cmd.stderr);
    //String[] envp = null;
    String[] envp = { "TERM=xterm" };
    final Process process = Runtime.getRuntime().exec(cmd.args, envp, cmd.pwd);
    cmd.process = process;
    cmd.in = process.getOutputStream();
    cmd.out = process.getInputStream();
    cmd.err = process.getErrorStream();
    if (stdin == null) stdin = cmd.in;
    stdout = cmd.out;
    stderr = cmd.err;
    if (cmd.stdin != CONSOLE) {
      Thread processIn = new Thread(new Runnable() {
        public void run() {
          try {
            int b;
            switch (cmd.stdin) {
            case FILE:
            case PIPE:
            case OTHER:
              if (toStdIn != null) {
                while ((b = toStdIn.read()) != -1) {
                  cmd.in.write(b);
                  cmd.in.flush();
                }
              }
              break;
            }
          } catch (Throwable t) { /*t.printStackTrace();*/ }
          finally {
            AppSystem.closeSilently(toStdIn);
            AppSystem.closeSilently(cmd.in);
          }
        }
      }, "procin:" + cmd.args[0]);
      processIn.setDaemon(true);
      processIn.start();
    }
    
    Thread processOut = new Thread(new Runnable() {
      public void run() {
        try {
          if (cmd.stdout == CONSOLE) {
            int d;
            byte buf[] = new byte[256];
////            String line;
//            BufferedReader out = new BufferedReader(
//                new InputStreamReader(cmd.out));
////            while ((line = out.readLine()) != null) {
////              if (cons != null) {
////                cons.outln(ProcessGroup.this, line);
////              }
////            }
//            while ((d = out.read()) != -1) {
//              cons.out(ProcessGroup.this, (byte)d);
//            }

            TickableReader out = new TickableReader(cmd.out, 1024,
                READER_TICK_RATE_MIN_MS, READER_TICK_RATE_MAX_MS);
            while ((d = out.read(buf)) != -1) {
              cons.out(ProcessGroup.this, buf, d);
            }
          } else {
            int b;
            switch (cmd.stdout) {
            case FILE:
            case PIPE:
            case OTHER:
              if (toStdOut != null) {
                while ((b = cmd.out.read()) != -1) {
                  toStdOut.write(b);
                }
              }
              break;
            }
          }
        } catch (Throwable t) { t.printStackTrace(); }
        finally {
          if (cmd.stdout != OTHER) {
            AppSystem.closeSilently(toStdOut);
          }
          AppSystem.closeSilently(cmd.out);
        }
      }
    }, "procout:" + cmd.args[0]);
    processOut.setDaemon(true);
    processOut.start();
    cmd.thrOut = processOut;
    
    Thread processErr = new Thread(new Runnable() {
      public void run() {
        try {
          if (cmd.stderr == CONSOLE) {
            int d;
            byte buf[] = new byte[256];

////            String line;
//            BufferedReader out = new BufferedReader(
//                new InputStreamReader(cmd.err));
////            while ((line = out.readLine()) != null) {
////              if (cons != null) {
////                cons.errln(ProcessGroup.this, line);
////              }
////            }
//            while ((d = out.read()) != -1) {
//              cons.err(ProcessGroup.this, (byte)d);
//            }
            TickableReader err = new TickableReader(cmd.err, 1024, 
                READER_TICK_RATE_MIN_MS, READER_TICK_RATE_MAX_MS);
            while ((d = err.read(buf)) != -1) {
              cons.err(ProcessGroup.this, buf, d);
            }

          } else {
            int b;
            switch (cmd.stderr) {
            case FILE:
            case OTHER:
              if (toStdErr != null) {
                while ((b = cmd.err.read()) != -1) {
                  toStdErr.write(b);
                  toStdErr.flush();
                }
              }
              break;
            }
          }
        } catch (Throwable t) { }
        finally {
          if (cmd.stderr != OTHER) {
            AppSystem.closeSilently(toStdErr);
          }
          AppSystem.closeSilently(cmd.err);
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
        finally {
          AppSystem.dispose(ProcessGroup.this);
          if (cons != null) {
            cons.exit(ProcessGroup.this, ret);
          }
        }
        Log.println(cmd.args[0] + " exited " + ret);
      }
    }, "proc:" + cmd.args[0]);
    processWatch.setDaemon(true);
    processWatch.start();
  }
  
  public void write(int b) throws IOException {
    stdin.write(b);
  }

  public void write(byte[] b) throws IOException {
    stdin.write(b);
    stdin.flush();
  }

  public void write(byte[] b, int off, int len) throws IOException {
    stdin.write(b, off, len);
    stdin.flush();
  }
  
  public void writeLine(String s) throws IOException {
    stdin.write((s + "\n").getBytes());
    stdin.flush();
  }
  
  public void addCmd(final String[] args, final File pwd) {
    addCmd(args, pwd, false, null, null, null, false, false, false);
  }
  
  public void addCmd(final String[] args, final File pwd, boolean pipeWithPrevious) {
    addCmd(args, pwd, pipeWithPrevious, null, null, null, false, false, false);
  }

  public void addCmd(final String[] args, final File pwd, boolean pipeWithPrevious, 
      String stdinFile, String stdoutFile, String stderrFile) {
    addCmd(args, pwd, pipeWithPrevious, stdinFile, stdoutFile, stderrFile, false, false, false);
  }
  public void addCmd(final String[] args, final File pwd, boolean pipeWithPrevious, 
      boolean otherStdin, boolean otherStdout, boolean otherStderr) {
    addCmd(args, pwd, pipeWithPrevious, null, null, null, otherStdin, otherStdout, otherStderr);
  }

  public void addCmd(final String[] args, final File pwd,  
      String stdinFile, String stdoutFile, String stderrFile) {
    addCmd(args, pwd, false, stdinFile, stdoutFile, stderrFile, false, false, false);
  }
  public void addCmd(final String[] args, final File pwd,  
      boolean otherStdin, boolean otherStdout, boolean otherStderr) {
    addCmd(args, pwd, false, null, null, null, otherStdin, otherStdout, otherStderr);
  }

  public void addCmd(final String[] args, final File pwd, boolean pipeWithPrevious, 
      String stdinFile, String stdoutFile, String stderrFile,
      boolean otherStdin, boolean otherStdout, boolean otherStderr) {
    Command cmd = new Command();
    cmd.stdin = pipeWithPrevious ? PIPE : (stdinFile != null ? FILE : (otherStdin ? OTHER : CONSOLE));
    cmd.stdout = (stdoutFile != null ? FILE : (otherStdout ? OTHER : CONSOLE));
    cmd.stderr = (stderrFile != null ? FILE : (otherStderr ? OTHER : CONSOLE));
    cmd.stdinPath = stdinFile;
    cmd.stdoutPath = stdoutFile;
    cmd.stderrPath = stderrFile;
    cmd.args = args;
    cmd.pwd = pwd;
    if (pipeWithPrevious) {
      commands.get(commands.size()-1).stdout = PIPE;
    }
    commands.add(cmd);
  }
  
  public void start(InputStream otherin, OutputStream otherout, OutputStream othererr, 
      ProcessConsole cons) throws IOException {
    try {
      stdin = null;
      this.cons = cons;
      PipedInputStream pipeIn = null;
      PipedOutputStream pipeOut = null;
      for (int cix = 0; cix < commands.size(); cix++) {
        Command c = commands.get(cix);
        InputStream curIn = null;
        OutputStream curOut = null;
        OutputStream curErr = null;
        if (c.stdin == PIPE) {
          curIn = pipeIn;
        } else if (c.stdin == FILE) {
          @SuppressWarnings("resource")
          FileInputStream fis = new FileInputStream(new File(c.stdinPath));
          curIn = fis;
        } else if (c.stdin == OTHER) {
          curIn = otherin;
        }
        if (c.stdout == PIPE) {
          pipeOut = new PipedOutputStream();
          pipeIn = new PipedInputStream(pipeOut); // this is saved till next iteration
          curOut = pipeOut;
        } else if (c.stdout == FILE) {
          @SuppressWarnings("resource")
          FileOutputStream fos = new FileOutputStream(new File(c.stdoutPath), true);
          curOut = fos;
        } else if (c.stdout == OTHER) {
          curOut = otherout;
        }
  
        if (c.stderr == FILE) {
          @SuppressWarnings("resource")
          FileOutputStream fos = new FileOutputStream(new File(c.stderrPath), true);
          curErr = fos;
        } else if (c.stdout == OTHER) {
          curErr = othererr;
        }
        
        startProcess(c, curIn, curOut, curErr);
      }
    } catch (Throwable t) {
      AppSystem.dispose(ProcessGroup.this);
      if (cons != null) {
        cons.exit(ProcessGroup.this, -1);
      }
      throw t;
    }
  }

  public void close() {
    try {
      stdin.write(4);
      stdin.flush();
    } catch (Throwable t) {}
    AppSystem.closeSilently(stdin);
  }
  
  public int kill(boolean synchronous) {
    int returnCode = -1;
    for (Command c : commands) {
      boolean wasAlive = false;
      try {
        AppSystem.closeSilently(c.in);
        AppSystem.closeSilently(c.err);
        AppSystem.closeSilently(c.out);
        wasAlive = true; // 1.8 c.process.isAlive();
        if (wasAlive) c.process.destroy();
      } catch (Throwable t) {}
      if (synchronous && wasAlive) {
        try {
          if (c != null && c.process != null) {
            returnCode = c.process.waitFor();
          }
        } catch (InterruptedException ie) {}
      }
    }
    return returnCode;
  }
  
  public class Command {
    Thread thrOut;
    File pwd;
    
    String args[];
    Process process;

    int stdin = CONSOLE;
    String stdinPath;
    OutputStream in;

    int stdout = CONSOLE;
    String stdoutPath;
    InputStream out;
    
    int stderr = CONSOLE;
    String stderrPath;
    InputStream err;
  }
  
  public interface ProcessConsole {
    public void outln(ProcessGroup pg, String s);
    public void out(ProcessGroup pg, byte b);
    public void out(ProcessGroup pg, byte b[], int len);
    public void errln(ProcessGroup pg, String s);
    public void err(ProcessGroup pg, byte b);
    public void err(ProcessGroup pg, byte b[], int len);
    public void exit(ProcessGroup pg, int ret);
  }

  @Override
  public void dispose() {
    kill(false);
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Command c : commands) {
      for (String a : c.args) {
        sb.append(a + " ");
      }
    }
    return sb.toString();
  }
}
