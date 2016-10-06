package com.pelleplutt.tuscedo;

import com.pelleplutt.tuscedo.ui.Bash;
import com.pelleplutt.util.FastTextPane;

public class ProcessGroupInfo {
  public final Bash bash;
  public final FastTextPane.Doc alternateScreenBuffer;
  public volatile boolean displayAlternateScreenBuffer; 
  
  public ProcessGroupInfo(Bash b, FastTextPane.Doc s) {
    bash = b;
    alternateScreenBuffer = s;
  }
}
