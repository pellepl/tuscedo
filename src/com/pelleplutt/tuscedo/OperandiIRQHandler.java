package com.pelleplutt.tuscedo;

import java.util.ArrayList;
import java.util.List;

import com.pelleplutt.operandi.Source;
import com.pelleplutt.operandi.proc.ExtCall;
import com.pelleplutt.operandi.proc.IRQHandler;
import com.pelleplutt.operandi.proc.Processor;
import com.pelleplutt.operandi.proc.Processor.M;
import com.pelleplutt.util.Log;

public class OperandiIRQHandler implements IRQHandler {
  static final int NUM_IRQ_BLOCKS = 2;
  static final int IRQS_PER_BLOCK = 8;
  
  public static final int IRQ_BLOCK_SYSTEM = 0;
  public static final int IRQ_SYSTEM_HALT = 0;
  public static final int IRQ_SYSTEM_TIMER = 1;
  public static final int IRQ_SYSTEM_USER = 7;

  public static final int IRQ_BLOCK_UI = 1;
  public static final int IRQ_UI_KEY_PRESS = 0;
  public static final int IRQ_UI_KEY_RELEASE = 1;
  public static final int IRQ_UI_MOUSE_PRESS = 2;
  public static final int IRQ_UI_MOUSE_RELEASE = 3;
  public static final int IRQ_UI_MOUSE_MOVE = 4;
  
  int[] irqPending = new int[NUM_IRQ_BLOCKS];
  int[] irqHandling = new int[NUM_IRQ_BLOCKS];
  int[] irqFunctions = new int[NUM_IRQ_BLOCKS*IRQS_PER_BLOCK];
  IRQQueue[] queues = new IRQQueue[NUM_IRQ_BLOCKS*IRQS_PER_BLOCK];
  
  Processor proc;
  OperandiScript script;
  
  public OperandiIRQHandler(OperandiScript op, Processor proc) {
    this.proc = proc;
    this.script = op;
    for (int i = 0; i < queues.length; i++) {
      queues[i] = new IRQQueue(i / IRQS_PER_BLOCK, i % IRQS_PER_BLOCK);
    }
    reset();
  }
  
  public void reset() {
    for (int i = 0; i < irqPending.length; i++) {
      irqPending[i] = 0;
      irqHandling[i] = 0;
    }
    for (int i = 0; i < queues.length; i++) {
      queues[i].reset();
    }
  }
  
  public void coldstart() {
    for (int i = 0; i < irqFunctions.length; i++) {
      irqFunctions[i] = 0x8f8f8f8f;
    }
  }
  
  public boolean pendingIRQ() {
    for (int block = 0; block < NUM_IRQ_BLOCKS; block++) {
      if ((irqPending[block] | irqHandling[block]) != 0) return true;
    }
    return false;
    
  }

  public void registerIRQ(int block, int irq, int addr) {
    irqFunctions[block * IRQS_PER_BLOCK + irq] = addr;
  }
  
  public void triggerIRQ(int block, int irq) {
    irqPending[block] |= (1<<irq);
    script.irqTriggered();
  }
  
  @Override
  public void leftIRQ(int pc) {
    loop:
    for (int block = 0; block < NUM_IRQ_BLOCKS; block++) {
      if ((irqHandling[block]) != 0) {
        for (int i = 0; i < IRQS_PER_BLOCK; i++) {
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

  @Override
  public void step(int pc) {
    loop:
    for (int block = 0; block < NUM_IRQ_BLOCKS; block++) {
      if ((irqPending[block] & ~irqHandling[block]) != 0) {
        for (int irq = 0; irq < IRQS_PER_BLOCK; irq++) {
          int mask = 1<<irq;
          if ((irqHandling[block] & mask)!=0) {
            //System.out.println("handling irq blk" + block + " level" + irq);
            break loop; // higher interrupts ongoing
          }
          if ((irqPending[block] & mask)!=0) {
            irqPending[block] &= ~mask;
            if (irqFunctions[block * IRQS_PER_BLOCK + irq] != 0x8f8f8f8f) {
              irqHandling[block] |= mask;
              proc.raiseInterrupt(irqFunctions[block * IRQS_PER_BLOCK + irq]);
              //System.out.println("!raising irq blk" + block + " level" + irq + ", handler " + irqFunctions[block * IRQS_PER_BLOCK + irq]);
              break loop;
            }
          }
        }
        break;
      }
    }
  }

  public void installIRQHandlers(OperandiScript op) {
    for (int block = 0; block < NUM_IRQ_BLOCKS; block++) {
      for (int irq = 0; irq< IRQS_PER_BLOCK; irq++) {
        String sirq = "" + (block * IRQS_PER_BLOCK + irq);
        op.doRunScript(null, new Source.SourceString("irqhandlers",  
            "module irq;\n" +
            "func __IRQ" + sirq + "_handler() {\n" +
            "  while __IRQ" + sirq + "_has_req() {\n" +
            "    f = __IRQ" + sirq + "_consume();\n"+
            "    f();\n" +
            "  }\n" +
            "}\n"), null);
        registerIRQ(block, irq, op.lookupFunc("irq.func.__IRQ" + sirq + "_handler"));
      }
    }
  }
  
  public IRQQueue queue(int block, int irq) {
    return queues[block * IRQS_PER_BLOCK + irq];
  }
  
  public void createIRQFunctions(OperandiScript os) {
    for (int block = 0; block < NUM_IRQ_BLOCKS; block++) {
      final int fblock = block;
      for (int irq = 0; irq< IRQS_PER_BLOCK; irq++) {
        final int firq = irq;
        String sirq = "" + (block * IRQS_PER_BLOCK + irq);
        os.setExtDef("__IRQ" + sirq + "_consume", "() - TODO", 
            new ExtCall() {
         public Processor.M exe(Processor p, Processor.M[] args) {
           Integer addr = os.getIRQHandler().queue(fblock, firq).consume(); 
           return addr == null ? null : new M(addr.intValue());
         }
       });
       os.setExtDef("__IRQ" + sirq + "_has_req", "() - TODO", 
            new ExtCall() {
         public Processor.M exe(Processor p, Processor.M[] args) {
           return new M(os.getIRQHandler().queue(fblock, firq).hasIRQ());
         }
       });
      }
    }

  }
  
  class IRQQueue {
    List<Integer> irqreqs = new ArrayList<Integer>();
    final int block, irq;
    public IRQQueue(int block, int irq) {
      this.block = block;
      this.irq = irq;
    }
    public void reset() {
      irqreqs = new ArrayList<Integer>();
    }
    public void trigger(int functionAddress) {
      synchronized (irqreqs) {
        irqreqs.add(functionAddress);
      }
      triggerIRQ(block, irq);
    }
    
    public int hasIRQ() {
      synchronized (irqreqs) {
        return irqreqs.isEmpty() ? 0 : 1;
      }
    }
    
    public Integer consume() {
      synchronized (irqreqs) {
        if (irqreqs.isEmpty()) {
          Log.println("ERROR: queue empty");
          return null;
        }
        //System.out.println("IRQ" + (block * IRQS_PER_BLOCK + irq) + " consuming " + irqreqs.get(0) + " of " + irqreqs.size());
        return irqreqs.remove(0);
      }
    }
  }
}
