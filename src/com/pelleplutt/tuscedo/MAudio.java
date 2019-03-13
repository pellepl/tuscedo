package com.pelleplutt.tuscedo;

import java.io.*;

import javax.sound.sampled.*;

import com.pelleplutt.operandi.Compiler;
import com.pelleplutt.operandi.proc.*;
import com.pelleplutt.operandi.proc.Processor.*;
import com.pelleplutt.tuscedo.ui.*;
import com.pelleplutt.util.*;

public class MAudio extends MObj {

  public MAudio(UIWorkArea wa, Compiler comp) {
    super(wa, comp, "audio");
  }

  static Processor.M Mfrequency= new Processor.M("frequency");
  static Processor.M Msample_bits = new Processor.M("sample_bits");
  static Processor.M Mdata = new Processor.M("data");

  @Override
  public void init(UIWorkArea wa, Compiler comp) {
    this.workarea = wa;
    addFunc("play", "audio:play", comp);
  }
  
  public static void createAudioFunctions(OperandiScript os) {
    os.setExtDef("audio:play", "(<data>(, freq(, splSizeInBits(, channels)))) - plays data as audio", 
        new ExtCall() {
      public Processor.M exe(Processor p, Processor.M[] args) {
        if (args.length < 1) return null;
        try {
          if (args.length < 2) play(args[0]);
          else if (args.length < 3) play(args[0], args[1].asFloat(), 16, 1);
          else if (args.length < 4) play(args[0], args[1].asFloat(), args[2].asInt(), 1);
          else                      play(args[0], args[1].asFloat(), args[2].asInt(), args[3].asInt());
        } catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }
    });
  }

  static void play(M m) throws Exception {
    play(m, 8000.f, 16, 1);
  }

  static void play(M m, float freq, int splSizeInBits, int channels) throws Exception {
    byte data[] = null;
    if (m != null) {
      if (m.type != Processor.TSET) return;
      M mdata = m.ref.get(Mdata); 
      if (mdata == null || mdata.type != Processor.TSET) {
        int range = (1 << (splSizeInBits-1))-1;
        int ix = 0;
        int length = m.ref.size();
        data = new byte[length*splSizeInBits/8*channels];
        for (int i = 0; i < length; i++) {
          for (int c = 0; c < channels; c++) {
            float fval = m.ref.getElement(i*channels + c).asFloat();
            int val = (int)(fval * range); 
            for (int v = 0; v < splSizeInBits; v += 8) {
              data[ix++] = (byte)(val & 0xff);
              val >>= 8;
            }
          }
        }
      } else {
        M mfreq = m.ref.get(Mfrequency);
        if (mfreq != null) freq = mfreq.asFloat();
        M msample_bits = m.ref.get(Msample_bits);
        if (msample_bits != null) splSizeInBits = msample_bits.asInt();
        int range = (1 << (splSizeInBits-1))-1;
        channels = mdata.ref.size();
        int length = mdata.ref.get(0).ref.size();
        data = new byte[length*splSizeInBits/8*channels];
        int ix = 0;
        for (int i = 0; i < length; i++) {
          for (int c = 0; c < channels; c++) {
            float fval = mdata.ref.get(c).ref.getElement(i).asFloat();
            int val = (int)(fval * range);
            for (int v = 0; v < splSizeInBits; v += 8) {
              data[ix++] = (byte)(val & 0xff);
              val >>= 8;
            }
          }
        }
      }
    }
     
    System.out.println("playing " + data.length + " bytes, " + freq + "Hz, " + splSizeInBits + " bits, " + channels + " channels");
    AudioFormat format = new AudioFormat(
        freq, splSizeInBits, channels,
        true, /* signed */
        false /* big-endian */);
    DataLine.Info info = new DataLine.Info(Clip.class, format);
    Clip clip = (Clip)AudioSystem.getLine(info);
    clip.open(format, data, 0, data.length);
    clip.start();
  }
}
