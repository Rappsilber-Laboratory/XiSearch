/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.ui;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rappsilber.utils.MemoryInfoProvider;
import rappsilber.applications.XiProcess;
import rappsilber.utils.StringUtils;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class MemMapStatusControl implements StatusInterface, MemoryInfoProvider{
    MappedByteBuffer buffer;
    IntBuffer statusIDBuffer;
    CharBuffer statusStringBuffer;
    LongBuffer freeMemBuffer;
    LongBuffer maxMemBuffer;
    LongBuffer totalMemBuffer;
    IntBuffer activeThreadsBuffer;
    IntBuffer setThreadsBuffer;
    ByteBuffer doGCBuffer;
    ByteBuffer incThreadBuffer;
    ByteBuffer decThreadBuffer;
    XiProcess xiProcess;
    
    AtomicInteger mid = new AtomicInteger(Integer.MIN_VALUE);
    String currentStatus;
    int offset = 0;
    boolean keepForwarding = true;
    

    public MemMapStatusControl(File path) throws IOException {
        FileChannel channel = FileChannel.open( path.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE );

        buffer = channel.map( FileChannel.MapMode.READ_WRITE, 0, 16384);
        statusIDBuffer = buffer.alignedSlice(8).asIntBuffer();
        buffer.getLong();
        activeThreadsBuffer = buffer.alignedSlice(8).asIntBuffer();
        buffer.getLong();
        setThreadsBuffer = buffer.alignedSlice(8).asIntBuffer();
        buffer.getLong();
        freeMemBuffer = buffer.alignedSlice(8).asLongBuffer();
        buffer.getLong();
        maxMemBuffer = buffer.alignedSlice(8).asLongBuffer();
        buffer.getLong();
        totalMemBuffer = buffer.alignedSlice(8).asLongBuffer();
        buffer.getLong();
        doGCBuffer = buffer.alignedSlice(1);
        buffer.get();
        incThreadBuffer = buffer.alignedSlice(1);
        buffer.get();
        decThreadBuffer = buffer.alignedSlice(1);
        buffer.get();
        statusStringBuffer = buffer.alignedSlice(4096).asCharBuffer();
    }
    
    

    @Override
    public void setStatus(String status) {
        currentStatus = status;
        String forwardStatus = status.substring(0,Math.min(500, status.length()));
        if (forwardStatus.length() < 500) {
            StringBuffer sb = new StringBuffer(forwardStatus);
            forwardStatus += " ".repeat(500 - forwardStatus.length());
        }
        Integer id = mid.incrementAndGet();
       
        if (id>100000) {
            mid.set(Integer.MIN_VALUE);
        }
        
        statusIDBuffer.put(0, id);
        
        statusStringBuffer.position(0);
        statusStringBuffer.put(forwardStatus);
    }

    @Override
    public String getStatus() {
        return currentStatus;
    }

    public void autoWriteMemInfo() {
        keepForwarding = true;
        Runnable runnable = new Runnable() {
            Runtime runtime = Runtime.getRuntime();
            byte doGC = 0;
            byte incThread = 0;
            byte decThread = 0;
            public void run() {
                while (keepForwarding) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MemMapStatusControl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    // forward current memory infos
                    freeMemBuffer.put(0, runtime.freeMemory());
                    maxMemBuffer.put(0, runtime.maxMemory());
                    totalMemBuffer.put(0, runtime.totalMemory());
                    // check if gc is requested
                    byte currentDoGC = doGCBuffer.get(0);
                    // but only if the gc flag was up before (poor mans "thread safe")
                    if (currentDoGC == doGC && currentDoGC != 0) {
                        long fm = runtime.freeMemory();
                        long mm = runtime.maxMemory();
                        long tm = runtime.totalMemory();
                        long um = tm-fm;
                        Logger.getLogger(MemMapStatusControl.class.getName()).log(Level.INFO,"GC triggered remotely");
                        String message = "Used: " + StringUtils.toHuman(um) + " of " + StringUtils.toHuman(mm) + "  (Free:" + StringUtils.toHuman(fm) + " Total:" + StringUtils.toHuman(tm) + " Max:"+ StringUtils.toHuman(mm) +")";
                        Logger.getLogger(MemMapStatusControl.class.getName()).log(Level.INFO,"Memory before GC:" + message);
                        
                        System.gc();
                        System.gc();
                        
                        fm = runtime.freeMemory();
                        mm = runtime.maxMemory();
                        tm = runtime.totalMemory();
                        freeMemBuffer.put(0, fm);
                        maxMemBuffer.put(0, mm);
                        totalMemBuffer.put(0, tm);
                        um = tm-fm;
                        message = "Used: " + StringUtils.toHuman(um) + " of " + StringUtils.toHuman(mm) + "  (Free:" + StringUtils.toHuman(fm) + " Total:" + StringUtils.toHuman(tm) + " Max:"+ StringUtils.toHuman(mm) +")";
                        Logger.getLogger(MemMapStatusControl.class.getName()).log(Level.INFO,"Memory after GC:" + message);
                        doGCBuffer.put(0, (byte)0);
                        currentDoGC = 0;
                        doGC  = 0;
                    } else {
                        doGC = currentDoGC;
                    }
                    
                    // should we increment the number of search threads?
                    byte currentInc = incThreadBuffer.get(0);
                    if (currentInc == incThread && incThread != 0) {
                        
                        
                    }
                }
            }
        };
        Thread t = new Thread(runnable,"memInfoForward");
        t.setDaemon(true);
        t.start();
    }

    public void autoThreadForward(XiProcess xip) {
        keepForwarding = true;
        Runnable runnable = new Runnable() {
            byte doGC = 0;
            byte incThread = 0;
            byte decThread = 0;
            public void run() {
                while (keepForwarding) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MemMapStatusControl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    // check if thread increment is requested
                    byte currentInc = incThreadBuffer.get(0);
                    // but only if the increment flag was up before (poor mans "thread safe")
                    if (currentInc == incThread && incThread != 0) {
                        xip.increaseSearchThread();
                        incThreadBuffer.put(0, (byte)0);
                        incThread = 0;
                        currentInc = 0;
                    } else {
                        incThread = currentInc;
                    }
                    // check if thread decrement is requested
                    byte currentDec = decThreadBuffer.get(0);
                    // but only if the flag was up before (poor mans "thread safe")
                    if (currentDec == decThread && decThread != 0) {
                        xip.decreaseSearchThread();
                        decThreadBuffer.put(0, (byte)0);
                        decThread = 0;
                        currentDec = 0;
                    } else {
                        decThread = currentDec;
                    }
                }
            }
        };
        Thread t = new Thread(runnable,"memInfoForward");
        t.setDaemon(true);
        t.start();
    }
    
    
    public void autoforwardStatus(final StatusInterface to) {
        keepForwarding = true;
        Runnable runnable = new Runnable() {
            public void run() {
                String prevStatus = "";
                while (keepForwarding) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MemMapStatusControl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    statusStringBuffer.position(0);
                    String remotestatus = statusStringBuffer.toString().trim();
                    if (!remotestatus.contentEquals(prevStatus)) {
                        prevStatus = remotestatus;
                        to.setStatus(prevStatus);
                    }
                }
            }
        };
        Thread t = new Thread(runnable,"statusForward");
        t.setDaemon(true);
        t.start();
    }

    public void stopAutoforwardStatus() {
        keepForwarding = false;
    }
    
    
    public static void main(String[] args) throws IOException {
        File f = new File(args[1]);
        if (args[0].contentEquals("-r")) {
            MemMapStatusControl readstatus = new MemMapStatusControl(f);
            readstatus.autoforwardStatus(new StatusInterface() {
                @Override
                public void setStatus(String status) {
                    System.out.println("got : " + status);
                }

                @Override
                public String getStatus() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });
            while (true) {
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MemMapStatusControl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
        }else if (args[0].contentEquals("-w")) {
            MemMapStatusControl writestatus = new MemMapStatusControl(f);
            Random r  =new Random();
            int i = 0;
            while (true) {
                writestatus.setStatus("status " + i);
                i++;
                try {
                    Thread.currentThread().sleep((long)(r.nextDouble()*1000));
                } catch (InterruptedException ex) {
                    Logger.getLogger(MemMapStatusControl.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (i> 100) {
                    writestatus.setStatus("wrapping status");
                    i=0;
                    try {
                        Thread.currentThread().sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MemMapStatusControl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } 
    }
    
    public long getFreeMem() {
        return freeMemBuffer.get(0);
    }

    public long getMaxMem() {
        return maxMemBuffer.get(0);
    }
    
    public long geTotalMem() {
        return totalMemBuffer.get(0);
    }

    public void initiateGC() {
        this.doGCBuffer.put(0, (byte)1);
    }
    
    public void incrementThread() {
        this.incThreadBuffer.put(0, (byte)1);
    }
    
    public void decrementThread() {
        this.decThreadBuffer.put(0, (byte)1);
    }
}
