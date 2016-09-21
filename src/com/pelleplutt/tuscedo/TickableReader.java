package com.pelleplutt.tuscedo;

import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;

import com.pelleplutt.util.AppSystem;

/**
 * TickableReader wrapping an input stream. At instantiation the class will 
 * create a daemon thread reading the input stream into a buffer.
 * Readers of this thread will get data when either a new line character is
 * encountered, a timeout occurs, flush or close is called externally, or the
 * buffer is full.
 * Basically, this input stream chunks together data over time intervals. When
 * the underlying stream returns data, the time is set to minimum tick.
 * Each time the tick times out without getting any data, the delay is doubled
 * up till maximum tick
 * .
 * @author petera
 */
public class TickableReader extends InputStream implements Runnable, Flushable {
  InputStream in;
  byte buffer[];
  volatile int wix; // write index
  volatile int rix; // read index
  volatile int pix; // publish index
  long tickRate, mintick, maxtick;
  final Object LOCK = new Object();
  volatile boolean open = true;
  long start;
  
  public TickableReader(InputStream i, int bufferSize, long mintick, long maxtick) {
    in = i;
    buffer = new byte[bufferSize];
    this.mintick = mintick;
    this.maxtick = maxtick;
    tickRate = mintick;
    Thread t = new Thread(this, "tickread");
    t.setDaemon(true);
    t.start();
    start = System.currentTimeMillis();
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  private void awaitWrite() {
    synchronized(LOCK) {
      while (rix == pix && open) {
        // no more published data to read, notify writer and wait
        LOCK.notify();
        AppSystem.waitSilently(LOCK, tickRate);
        if (rix == pix) {
          // if still no more data published, force publish index to current write index
          if (wix != pix) {
            pix = wix;
            tickRate = mintick;
          } else {
            tickRate = Math.min(maxtick, tickRate*2);
          }
        }
      }
    }
  }
  
  @Override
  public int read(byte[] b, int offs, int len) throws IOException {
    awaitWrite();
    int r = 0;
    if (!open && rix == pix) {
      // not more published and closed, return EOF
      r = -1;
    } else {
      while (rix != pix && r < len) {
        b[offs++] = buffer[rix++];
        r++;
        rix %= buffer.length;
      }
      tickRate = mintick;
      synchronized(LOCK) {
        // tell writer we've read
        LOCK.notify();
      }
    }
    return r;
  }

  @Override
  public int read() throws IOException {
    awaitWrite();
    int d;
    if (!open && rix == pix) {
      // not more published and closed, return EOF
      d = -1;
    } else {
      d = buffer[rix++] & 0xff;
      rix %= buffer.length;
      tickRate = mintick;
      synchronized(LOCK) {
        // tell writer we've read
        LOCK.notify();
      }
    }
    return d;
  }

  @Override
  public void run() {
    int d;
    try {
      while ((d = in.read()) != -1) {
        buffer[wix++] = (byte)d;
        wix %= buffer.length;
        if (d == '\n' || d == '\r') {
          synchronized(LOCK) {
            // got us a newline, update publish index and notify reader
            pix = wix;
            LOCK.notify();
          }
        }
        synchronized(LOCK) {
          while (wix == (buffer.length + rix - 1) % buffer.length) {
            // buffer full, publish what we have and notify reader
            pix = wix;
            LOCK.notify();
            AppSystem.waitSilently(LOCK, maxtick);
          }
        }
      }
    } catch (Throwable t) {
    } finally {
      synchronized(LOCK) {
        // we've been closed, publish what we have and notify reader
        open = false;
        pix = wix;
        LOCK.notify();
      }
      AppSystem.closeSilently(in);
    }
  }
  
  @Override
  public void flush() {
    synchronized(LOCK) {
      // external trigger, publish what we have and notify reader and writer
      pix = wix;
      LOCK.notifyAll();
    }
  }
  
  @Override
  public void close() {
    synchronized(LOCK) {
      // external close, publish what we have and notify reader and writer
      open = false;
      pix = wix;
      LOCK.notifyAll();
    }
    AppSystem.closeSilently(in);
  }
}
