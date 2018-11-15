package com.pelleplutt.tuscedo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pelleplutt.operandi.Compiler;
import com.pelleplutt.operandi.proc.ExtCall;
import com.pelleplutt.operandi.proc.MListMap;
import com.pelleplutt.operandi.proc.MSet;
import com.pelleplutt.operandi.proc.Processor;
import com.pelleplutt.operandi.proc.Processor.M;
import com.pelleplutt.operandi.proc.ProcessorError;
import com.pelleplutt.tuscedo.ui.UIWorkArea;
import com.pelleplutt.util.AppSystem;

public class MDisk extends MObj {
  
  public static final String KEY_STREAM_ID = ".stream";
  public MDisk(UIWorkArea wa, Compiler comp) {
    super(wa, comp, "disk");
  }

  public void init(UIWorkArea wa, Compiler comp) {
    this.workarea = wa;
    addFunc("read", OperandiScript.FN_DISK_READ, comp);
    addFunc("readb", OperandiScript.FN_DISK_READB, comp);
    addFunc("write", OperandiScript.FN_DISK_WRITE, comp);
    addFunc("writeb", OperandiScript.FN_DISK_WRITEB, comp);
    addFunc("mkdir", OperandiScript.FN_DISK_MKDIR, comp);
    addFunc("ls", OperandiScript.FN_DISK_LS, comp);
    addFunc("find_file", OperandiScript.FN_DISK_FIND_FILE, comp);
    addFunc("mv", OperandiScript.FN_DISK_MOVE, comp);
    addFunc("stat", OperandiScript.FN_DISK_STAT, comp);
    addFunc("cp", OperandiScript.FN_DISK_COPY, comp);
    addFunc("rm", OperandiScript.FN_DISK_RM, comp);
    addFunc("touch", OperandiScript.FN_DISK_TOUCH, comp);
    addFunc("open", OperandiScript.FN_DISK_OPEN, comp);
  }
  
  public static void createDiskFunctions(OperandiScript os) {
    os.setExtDef(OperandiScript.FN_DISK_READ, "(<filename>) - returns a file as a string", 
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 1) return null;
        String path = args[0].asString();
        return new Processor.M(AppSystem.readFile(new File(path)));
      }
    });
    os.setExtDef(OperandiScript.FN_DISK_READB, "(<filename>) - returns a file as a byte array",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 1) return null;
        String path = args[0].asString();
        byte d[];
        try {
          d = Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
          return null;
        }
        MListMap arr = new MListMap();
        for (byte b : d) {
          arr.add(new Processor.M((int)b & 0xff));
        }
        return new Processor.M(arr);
      }
    });
    os.setExtDef(OperandiScript.FN_DISK_WRITE, "(<filename>, <string>) - writes file as a string", 
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 2) return null;
        String path = args[0].asString();
        String data = args[1].asString();
        return new Processor.M(AppSystem.writeFile(new File(path), data) ? 1 : 0);
      }
    });
    os.setExtDef(OperandiScript.FN_DISK_WRITEB, "(<filename>, <array>) - writes binary file as a byte array",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 2) return null;
        String path = args[0].asString();
        MSet data = args[1].ref;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.size());
        for (int i = 0; i < data.size(); i++) {
          baos.write(data.get(i).asInt() & 0xff);
        }
        return new Processor.M(AppSystem.writeFile(new File(path), baos.toByteArray()) ? 1 : 0);
      }
    });
    os.setExtDef(OperandiScript.FN_DISK_FIND_FILE, "(<path>,<filter>,<recurse>) - returns array of files matching the filter", 
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 1) return null;
        String path = args[0].asString();
        String filter = args.length > 1 ? args[1].asString() : "*";
        boolean recurse = args.length > 2 ? args[2].asInt() != 0 : false;
        List<File> files = AppSystem.findFiles(path, filter, recurse);
        MListMap mfiles = new MListMap();
        for (File f : files) {
          mfiles.add(new Processor.M(f.getAbsolutePath()));
        }
        return new Processor.M(mfiles);
      }
    });
    os.setExtDef(OperandiScript.FN_DISK_LS, "(<path>(,<filter>)) - returns array of files and directories in given path, optionally filtered",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        String path = ".";
        FileFilter filter = null;
        if (args.length > 0) path = args[0].asString();
        if (args.length > 1) {
          String ffilt = args[1].asString();
          String patStr = "\\Q" + ffilt.replaceAll("\\*", "\\\\E(.*)\\\\Q") + "\\E";
          final Pattern pattern = Pattern.compile(patStr);
          filter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
              Matcher m = pattern.matcher(pathname.getAbsolutePath());
              return m.matches();
            }
          };
        }
        File[] files = new File(path).listFiles(filter);
        MListMap mfiles = new MListMap();
        if (files != null) {
          for (File f : files) {
            mfiles.add(new Processor.M(f.getAbsolutePath()));
          }
        }
        return new Processor.M(mfiles);
      }
    });
    os.setExtDef(OperandiScript.FN_DISK_STAT, "(<filename>) - returns file information as a struct",
        new ExtCall() {
      @SuppressWarnings("unchecked")
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 1) return null;
        String path = args[0].asString();
        Map<String, Object> m;
        try {
          m = Files.readAttributes(Paths.get(path), "posix:*", LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
          return null;
        }
        if (m == null) return null;
        MListMap mstat = new MListMap();
        mstat.put("last_modified_time", new Processor.M(m.get("lastModifiedTime").toString()));
        mstat.put("last_access_time", new Processor.M(m.get("lastAccessTime").toString()));
        mstat.put("creation_time", new Processor.M(m.get("creationTime").toString()));
        mstat.put("file_key", new Processor.M(m.get("fileKey").toString()));
        mstat.put("owner", new Processor.M(m.get("owner").toString()));
        mstat.put("group", new Processor.M(m.get("group").toString()));
        mstat.put("size", new Processor.M(((Long)m.get("size")).intValue()));
        mstat.put("is_dir", new Processor.M(m.get("isDirectory")));
        mstat.put("is_symlink", new Processor.M(m.get("isSymbolicLink")));
        mstat.put("is_other", new Processor.M(m.get("isOther")));
        mstat.put("is_file", new Processor.M(m.get("isRegularFile")));
        MListMap perms = new MListMap();
        for (PosixFilePermission perm : (Set<PosixFilePermission>)m.get("permissions")) {
          perms.add(new Processor.M(perm.toString()));
        }
        mstat.put("permissions", new Processor.M(perms));
        return new Processor.M(mstat);
      }
    });
    os.setExtDef(OperandiScript.FN_DISK_MOVE, "(<source>, <destination>) - moves a file",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 2) return null;
        String src = args[0].asString();
        String dst = args[1].asString();
        try {
          Files.move(Paths.get(src), Paths.get(dst), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
          throw new ProcessorError(e.getMessage());
        }
        return null;
      }
    });
    os.setExtDef(OperandiScript.FN_DISK_COPY, "(<source>, <destination>) - copies a file",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 2) return null;
        String src = args[0].asString();
        String dst = args[1].asString();
        try {
          Files.copy(Paths.get(src), Paths.get(dst), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
          throw new ProcessorError(e.getMessage());
        }
        return null;
      }
    });
    os.setExtDef(OperandiScript.FN_DISK_RM, "(<path>) - removes a file or directory",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 1) return null;
        String src = args[0].asString();
        try {
          Files.delete(Paths.get(src));
        } catch (IOException e) {
          throw new ProcessorError(e.getMessage());
        }
        return null;
      }
    });
    os.setExtDef(OperandiScript.FN_DISK_MKDIR, "(<path>) - creates a directory",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 1) return null;
        String src = args[0].asString();
        new File(src).mkdirs();
        return null;
      }
    });
    os.setExtDef(OperandiScript.FN_DISK_TOUCH, "(<path>) - touches a file",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 1) return null;
        String src = args[0].asString();
        new File(src).setLastModified(System.currentTimeMillis());
        return null;
      }
    });
    os.setExtDef(OperandiScript.FN_DISK_OPEN, "(<path>) - opens a file",
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 1) return null;
        String src = args[0].asString();
        InputStream is = null;
        if (src.equals("///stdin") || src.equals("///stdio")) {
          is = System.in;
        } else if (src.equals(Settings.inst().string(Settings.BASH_CONNECTION_STREAM_STRING))) {
          is = os.currentWA.getSerial().getSerialInputStream();
        } else {
          try {
            is = new FileInputStream(src);
          } catch (FileNotFoundException e) {}
        }
        if (is == null) return null;
        
        MObj mobj = new MObj(os.currentWA, os.comp, "file") {
          @Override
          public void init(UIWorkArea wa, Compiler comp) {
            addFunc("read", OperandiScript.FN_FILE_READ, comp);
            addFunc("readline", OperandiScript.FN_FILE_READLINE, comp);
            addFunc("close", OperandiScript.FN_FILE_CLOSE, comp);
          }
        };
        int filestreamIx = os.filestreams.size();
        os.filestreams.add(is);
        mobj.putIntern(KEY_STREAM_ID, new M(filestreamIx));
        M mfile = new M(mobj);
        return mfile;
      }
    });
  }
  public static void createFileFunctions(OperandiScript os) {
    os.setExtDef(OperandiScript.FN_FILE_READ, "(file) - returns a byte from a file", 
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 1) return null;
        int hdl = args[0].asInt();
        if (hdl >= os.filestreams.size()) return null;
        if (os.filestreams.get(hdl) == null) return null;
        int res = -1;
        try {
          res = os.filestreams.get(hdl).read();
        } catch (Throwable t) {}
        return new M(res);
      }
    });
    os.setExtDef(OperandiScript.FN_FILE_READLINE, "(file) - return a line from a file", 
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 1) return null;
        int hdl = args[0].asInt();
        if (hdl >= os.filestreams.size()) return null;
        if (os.filestreams.get(hdl) == null) return null;
        int res = -1;
        try {
          res = os.filestreams.get(hdl).read();
        } catch (Throwable t) {}
        return new M(res);
      }
    });
    os.setExtDef(OperandiScript.FN_FILE_CLOSE, "(file) - closes a file", 
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 1) return null;
        int hdl = args[0].asInt();
        if (hdl >= os.filestreams.size()) return null;
        if (os.filestreams.get(hdl) == null) return null;
        try {
          os.filestreams.get(hdl).close();
        } catch (Throwable t) {}
        os.filestreams.set(hdl, null);
        return null;
      }
    });
  }
}

  
