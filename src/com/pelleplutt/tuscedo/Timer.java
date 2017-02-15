package com.pelleplutt.tuscedo;

import java.util.ArrayList;
import java.util.List;

import com.pelleplutt.util.AppSystem;

public class Timer implements DisposableRunnable {
  List<Entry> entries = new ArrayList<Entry>();
  volatile boolean running = true;
  final Object lock = new Object();
  
  public Timer() {
    Thread t = new Thread(this, "timer");
    t.setDaemon(true);
    t.start();
    
  }
  
  @Override
  public void dispose() {
    synchronized (lock) {
      running = false;
      lock.notify();
    }
  }

  @Override
  public void run() {
    while (running) {
      Entry runE = null;
      synchronized (lock) {
        while (running && entries.isEmpty()) {
          AppSystem.waitSilently(lock, 0);
        }
        long t;
        if (!running) break;
        if (entries.isEmpty()) continue;
        while (running && !entries.isEmpty() && (t = System.currentTimeMillis()) < entries.get(0).trigTime) {
          AppSystem.waitSilently(lock, entries.get(0).trigTime - t);
        }
        if (running && !entries.isEmpty() && System.currentTimeMillis() >= entries.get(0).trigTime) {
          runE = entries.remove(0);
        }
      }
      if (runE != null) {
        long t = System.currentTimeMillis();
        runE.task.run();
        if (runE.recurrence > 0 && runE.active) {
          runE.trigTime = runE.trigTime + runE.recurrence;
          if (runE.assureTimes && runE.trigTime < t) {
            runE.trigTime = t;
          }
          insertSorted(runE);
        } else {
          runE.active = false;
        }
      }
    }
  }
  
  void insertSorted(Entry e) {
    synchronized (lock) {
      Entry higherTrig = null;
      int ix;
      for (ix = 0; ix < entries.size(); ix++) {
        Entry ecmp = entries.get(ix);
        if (ecmp.trigTime > e.trigTime) {
          higherTrig = ecmp;
          break;
        }
      }
      if (higherTrig == null) {
        entries.add(e);
      } else {
        entries.add(ix, e);
      }
    }
  }

  public Entry addTask(Runnable task, long futureMs) {
    return addTask(task, futureMs, 0);
  }
  
  public Entry addTask(Runnable task, long futureMs, long recurrenceMs) {
    Entry e = new Entry();
    e.task = task;
    e.trigTime = System.currentTimeMillis() + futureMs;
    e.recurrence = recurrenceMs;
    e.active = true;
    insertSorted(e);
    synchronized (lock) {
      lock.notify();
    }
    return e;
  }
  
  public class Entry {
    private Runnable task;
    volatile boolean active;
    boolean assureTimes = false;
    private long trigTime;
    private long recurrence;
    public void stop() {
      active = false;
    }
    public void restart() {
      if (!active) {
        insertSorted(this);
      }
    }
  }
}
