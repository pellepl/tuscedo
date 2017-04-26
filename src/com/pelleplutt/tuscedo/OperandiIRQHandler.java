package com.pelleplutt.tuscedo;

import java.util.ArrayList;
import java.util.List;

import com.pelleplutt.operandi.Source;
import com.pelleplutt.operandi.proc.IRQHandler;
import com.pelleplutt.operandi.proc.Processor;
import com.pelleplutt.util.Log;

public class OperandiIRQHandler implements IRQHandler {
  static final int NUM_IRQ_BLOCKS = 2;
  
  public static final int IRQ_BLOCK_SYSTEM = 0;
  public static final int IRQ_SYSTEM_HALT = 0;
  public static final int IRQ_SYSTEM_TIMER = 1;

  public static final int IRQ_BLOCK_UI = 1;
  public static final int IRQ_UI_KEY = 0;
  public static final int IRQ_UI_MOUSE_PRESS = 1;
  public static final int IRQ_UI_MOUSE_RELEASE = 2;
  public static final int IRQ_UI_MOUSE_MOVE = 3;
  
  int[] irqPending = new int[NUM_IRQ_BLOCKS];
  int[] irqHandling = new int[NUM_IRQ_BLOCKS];
  
  int[] irqFunctions = new int[NUM_IRQ_BLOCKS*32];
  
  Processor proc;
  
  List<Integer> timerIRQs = new ArrayList<Integer>();
  
  public OperandiIRQHandler(OperandiScript op, Processor proc) {
    this.proc = proc;
    reset();
  }
  
  public void reset() {
    timerIRQs = new ArrayList<Integer>();
    for (int i = 0; i < irqPending.length; i++) {
      irqPending[i] = 0;
      irqHandling[i] = 0;
    }
  }
  
  public void coldstart() {
    for (int i = 0; i < irqFunctions.length; i++) {
      irqFunctions[i] = 0x8f8f8f8f;
    }
  }

  public void registerIRQ(int block, int irq, int addr) {
    irqFunctions[block * 32 + irq] = addr;
  }
  
  public void triggerIRQ(int block, int irq) {
    irqPending[block] |= (1<<irq);
  }
  
  @Override
  public void leftIRQ(int pc) {
    loop:
    for (int block = 0; block < NUM_IRQ_BLOCKS; block++) {
      if ((irqHandling[block]) != 0) {
        for (int i = 0; i < 32; i++) {
          int mask = 1<<i;
          if ((irqHandling[block] & mask) !=0) {
            //System.out.println("    left irq blk" + block + " level" + i);
            irqHandling[block] &= ~mask;
            break loop;
          }
        }
      }
    }
  }
  int test = 0;
  @Override
  public void step(int pc) {
    loop:
    for (int block = 0; block < NUM_IRQ_BLOCKS; block++) {
      if ((irqPending[block] & ~irqHandling[block]) != 0) {
        for (int i = 0; i < 32; i++) {
          int mask = 1<<i;
          if ((irqHandling[block] & mask)!=0) {
            //System.out.println("handling irq blk" + block + " level" + i);
            break loop; // higher interrupts ongoing
          }
          if ((irqPending[block] & mask)!=0 && irqFunctions[block * 32 + i] != 0x8f8f8f8f) {
            irqHandling[block] |= mask;
            irqPending[block] &= ~mask;
            proc.raiseInterrupt(irqFunctions[block * 32 + i]);
            //System.out.println("!raising irq blk" + block + " level" + i + ", handler " + irqFunctions[block * 32 + i]);
            break loop;
          }
        }
        break;
      }
    }
  }

  public void callTimerIRQ(int timerFunctionAddress) {
    synchronized (timerIRQs) {
      //Log.println("trig timerirq: addr " + timerFunctionAddress + ", got " + timerIRQs.size() + " reqs");
      timerIRQs.add(timerFunctionAddress);
    }
    triggerIRQ(IRQ_BLOCK_SYSTEM, IRQ_SYSTEM_TIMER);
  }
  
  public int hasTimerIRQ() {
    synchronized (timerIRQs) {
      //Log.println("has " + timerIRQs.size());
      return timerIRQs.isEmpty() ? 0 : 1;
    }
  }
  
  public Integer consumeTimerIRQ() {
    synchronized (timerIRQs) {
      if (timerIRQs.isEmpty()) {
        Log.println("ERROR: queue empty");
        return null;
      }
      //Log.println("consuming " + timerIRQs.get(0) + " of " + timerIRQs.size());
      return timerIRQs.remove(0);
    }
  }
  
  public void installIRQHandlers(OperandiScript op) {
    op.doRunScript(null, new Source.SourceString("irqhandlers",  
    "module irq;\n" +
    "func __IRQ_timer_handler() {\n" +
    "  while __IRQ_timer() {\n" +
    "    timer_f = __IRQ_consume_timer();\n"+
    "    timer_f();\n" +
    "  }\n" +
    "}\n"));
    registerIRQ(IRQ_BLOCK_SYSTEM, IRQ_SYSTEM_TIMER, op.lookupFunc("irq.func.__IRQ_timer_handler"));
  }
}
