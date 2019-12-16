/* 
 * Copyright 2016 Lutz Fischer <l.fischer@ed.ac.uk>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rappsilber.ms.dataAccess.output;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import rappsilber.ms.dataAccess.output.ResultWriter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 * provides asynchronous writer by buffering the actual request to write in a
 * queue and use a separate thread to access the inner writer.<br/>
 * Since the WriteResult method is synchronised, the class can also be used to
 * combine results produced by several threads, into one out-put stream.<br/>
 * The class uses an ArrayBlockingQueue to to hand over the data to the actual
 * ResultWriter. Therefor the writeRsult-method just fills up the queue and only
 * blocks if the queue is full and has to wait for the ResultWriter to process
 * the next element.
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class BufferedResultWriter extends AbstractStackedResultWriter implements ResultWriter, BatchResultWriter {

    // private ResultWriter m_innerWriter;
    private ArrayBlockingQueue<MatchedXlinkedPeptide> m_buffer;
//    protected Object m_running_Synch = new Object();
    private AtomicBoolean m_running = new AtomicBoolean(true);
    private Thread m_runner;
    private AtomicInteger m_countMatches = new AtomicInteger(0);
    private AtomicInteger m_countTopMatches = new AtomicInteger(0);
    private AtomicInteger m_countForwardedMatches = new AtomicInteger(0);
//    private final Object m_countMatchesSync = new Object();
//    private int m_runningCount = 0;
//    private boolean m_doFreeMatch = false;
    private AtomicBoolean m_active = new AtomicBoolean();
//    private final Object m_activeSync = new Object();
//    private final Object m_finished_sync = new Object();
    private AtomicBoolean m_finished = new AtomicBoolean(false);
//    /**
//     * how many buffered writers are still active - meaning having still data to
//     * be written
//     */
//    private static int m_countActiveWriters = 0;
//    private static final Object m_countActiveWritersSync = new Object();
    private Object m_doFlushSync = new Object();
    private final Object m_writeSync = new Object();

    private AtomicBoolean m_doFlush = new AtomicBoolean(true);

    private static int m_defaultbuffersize = 10;
    
    private int m_buffersize = m_defaultbuffersize;

    
    public static LinkedList<BufferedResultWriter> allActiveWriters = new LinkedList<BufferedResultWriter>();

    public static LinkedList<BufferedResultWriter> allWriters = new LinkedList<BufferedResultWriter>();
    
    
    private boolean m_exceptionOccured = false;
    
    public static boolean m_clearAnnotationsOnBuffer = true;
    
    public static boolean m_ForceNoClearAnnotationsOnBuffer = false;
   
//    private static void incActiveCounter() {
//        synchronized (m_countActiveWritersSync) {
//            m_countActiveWriters ++;
//        }
//    }
//
//    private static void decActiveCounter() {
//        synchronized (m_countActiveWritersSync) {
//            m_countActiveWriters --;
//        }
//    }
//    public static boolean hasActiveBuffer() {
//        int count = 0;
//        synchronized (m_countActiveWritersSync) {
//            count = m_countActiveWriters;
//        }
//        return count > 0;
//    }
    /**
     * creates a new BufferedResultWriter, that forwards the results to the
     * given ResultWriter.<br/>
     *
     * @param InnerResultWriter
     */
    public BufferedResultWriter(ResultWriter InnerResultWriter) {
        this(InnerResultWriter, m_defaultbuffersize);

    }

    /**
     * creates a new BufferedResultWriter, that forwards the results to the
     * given ResultWriter.<br/>
     * Up to buffersize elements can be stored in the buffer - when writing the
     * buffersize+1 element the method blocks, until the inner writer has
     * finished to write the first element in the queue.
     *
     * @param InnerResultWriter The actual method for physically writing the
     * result
     * @param buffersize the size of the buffer - indicating how many results
     * can be queued up before the writeResult-method blocks
     */
    public BufferedResultWriter(ResultWriter InnerResultWriter, int buffersize) {
        m_active.set(false);
        setInnerWriter(InnerResultWriter);
        m_buffersize = buffersize;
        m_buffer = new ArrayBlockingQueue<MatchedXlinkedPeptide>(buffersize);
        startProcessing();
        allWriters.add(this);
    }

    /**
     * Starts a new thread, that calls the processQueue-method
     * Should normally not needed to be called from anywhere outside this class
     * But currently have some bug with thread synchronisation that leads to a 
     * premature death of the processing thread - and then restarting it can be 
     * helpful.
     */
    public void startProcessing() {
        
        if (getInnerWriter() instanceof BatchResultWriter) {
            m_runner = new Thread(new Runnable() {
                public void run() {
                    try {
                        processQueueBatch();
                    } catch (IOException ex) {
                        m_exceptionOccured = true;
                        Logger.getLogger(BufferedResultWriter.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
            m_runner.setName("BufferedResultWriter_batchforward" + m_runner.getId());
        } else {
            m_runner = new Thread(new Runnable() {
                public void run() {
                    try {
                        processQueue();
                    } catch (IOException ex) {
                        m_exceptionOccured = true;
                        Logger.getLogger(BufferedResultWriter.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
            m_runner.setName("BufferedResultWriter_forward" + m_runner.getId());
        }

        m_runner.start();
        allActiveWriters.add(this);

    }

    @Override
    public void writeResult(MatchedXlinkedPeptide match) throws IOException {
        // for reason of memory consumptions we can strip some infos from the spectrum here.
        if (m_clearAnnotationsOnBuffer && !m_ForceNoClearAnnotationsOnBuffer) {
            match.reduceToMinimum();
        }
        
        if (!isAlive() && ! m_exceptionOccured) {
            startProcessing();
        }
        boolean notWriten = true;
        while (notWriten) {
            try {
                synchronized(m_writeSync) {
                    if (m_buffer.offer(match,60,TimeUnit.SECONDS)) {
                        notWriten = false;
                    } else if (!m_runner.isAlive()) {
                        String message= "The writer part of the of the buffer has stoped";
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, message);
                        // prevent an automatic restart

                        throw new IOException(message);
                    }
                }

                    
            } catch (InterruptedException ex) {
                Logger.getLogger(BufferedResultWriter.class.getName()).log(Level.WARNING, "interrupted while writing - retrying", ex);
            }
        }
        m_countMatches.incrementAndGet();
        if (match.getMatchrank() == 1)
            m_countTopMatches.incrementAndGet();
//        m_runningCount--;
    }

    @Override
    public void batchWriteResult(Collection<MatchedXlinkedPeptide> matches) throws IOException{
        
//        if (match.getSpectra() == null)
//            System.out.println("found it");
//        if ((++m_runningCount) > 1) {
//            m_runningCount += 0;
//        }

        int diff = 0;
        int countBefore = 0 ;
        int countAfter = 0;
        synchronized(m_writeSync) {
            countBefore = m_countMatches.get();
            for (MatchedXlinkedPeptide match : matches) {
                boolean written = false;
                while (!written) {
                    try {
                        written = m_buffer.offer(match,60,TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(BufferedResultWriter.class.getName()).log(Level.WARNING, "interrupted while writing - retrying", ex);
                    }
                }
                if (!m_runner.isAlive()) {
                    String message= "The writer part of the of the buffer has stoped";
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "{0}:{1}", new Object[]{Thread.currentThread().getName(), message});
                    throw new IOException(message);
                }
                m_countMatches.incrementAndGet();
                if (match.getMatchrank() == 1)
                    m_countTopMatches.incrementAndGet();
            }
            countAfter=m_countMatches.get();
            diff = countAfter - countBefore;
                
        }
        if (diff != matches.size()) 
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "did not take up all results from the input");
//        m_runningCount--;
    }
    
    
    
    
    long c = Calendar.getInstance().getTimeInMillis() - 30000;
    int bfc = 0;
    boolean doReportFull = true;
    int fullReported = 0;

    /**
     * waits for elements to become available and forwards these
     */
    private void processQueue() throws IOException {
        while (m_running.get() || !m_buffer.isEmpty()) {
            try {
                // mark up if we have nothing left in the buffer
                if (m_buffer.remainingCapacity() == 0) {
                    if (doReportFull) {
                        bfc++;
                        long n = Calendar.getInstance().getTimeInMillis();
                        // but don't do it all the time
                        if (n - c > 30000) {
                            c = n;
                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Buffer is full (" + bfc + ") in Thread " + Thread.currentThread().getName() );
                            //bec = 0;
                            if (fullReported++ > 10) {
                                doReportFull = false;
                            }
                        }
                    }
                }
                
                ArrayList<MatchedXlinkedPeptide> list =  new ArrayList<MatchedXlinkedPeptide>(m_buffersize);
                // wait for something to be in the queue
                MatchedXlinkedPeptide m = m_buffer.poll(1000, TimeUnit.MILLISECONDS);
                if (m!= null) {
                    innerWriteResult(m);
                    m_countForwardedMatches.incrementAndGet();
                    if (m_doFreeMatch) {
                        m.free();
                    }
                    list.clear();
                    // get everything else that is in the list
                    m_buffer.drainTo(list);

                    if (!list.isEmpty()) {
                        // write all the results we got
                        for (MatchedXlinkedPeptide next : list) {
                            innerWriteResult(next);
                            m_countForwardedMatches.incrementAndGet();
                            if (m_doFreeMatch) {
                                next.free();
                            }
                        }
                    } else {
    //                    synchronized (m_activeSync) {
    //                        if (m_active) {
    ////                            decActiveCounter();
    //                            m_active = false;
    //                        }
    //                    }
                    }
                    list.clear();
                }
                
                if (m_doFlush.get()) {

                    
                    m_buffer.drainTo(list);
                    for (MatchedXlinkedPeptide next : list) {
                        innerWriteResult(next);
                        m_countForwardedMatches.incrementAndGet();
                        if (m_doFreeMatch) {
                            next.free();
                        }
                    }
                    super.flush();
                    m_doFlush.getAndSet(false);
                }


            } catch (InterruptedException ex) {
                Logger.getLogger(BufferedResultWriter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
//        getInnerWriter().finished();
//        getInnerWriter().waitForFinished();
//        synchronized(m_finished_sync) {
        m_finished.set(true);
        allActiveWriters.remove(this);
//        }

//        synchronized (m_activeSync) {
//            if (m_active) {
////                decActiveCounter();
//                m_active = false;
//            }
//        }
    }

    /**
     * waits for elements to become available and forwards these
     */
    private void processQueueBatch() throws IOException {
        BatchResultWriter batchwrite = (BatchResultWriter) getInnerWriter();
        
        while (m_running.get() || !m_buffer.isEmpty()) {
            try {
                // mark up if we have nothing left in the buffer
                if (m_buffer.remainingCapacity() == 0) {
                    if (doReportFull) {
                        bfc++;
                        long n = Calendar.getInstance().getTimeInMillis();
                        // but don't do it all the time
                        if (n - c > 30000) {
                            c = n;
                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Buffer is full (" + bfc + ") in Thread " + Thread.currentThread().getName() );
                            //bec = 0;
                            if (fullReported++ > 10) {
                                doReportFull = false;
                            }
                        }
                    }
                }
                
                ArrayList<MatchedXlinkedPeptide> list =  new ArrayList<MatchedXlinkedPeptide>(m_buffersize);
                // wait for something to be in the queue
                MatchedXlinkedPeptide m = m_buffer.poll(1000, TimeUnit.MILLISECONDS);
                if (m!= null) {
                    
                    list.add(m);
                    
                    // get everything else that is in the list
                    m_buffer.drainTo(list);
                    m_countForwardedMatches.addAndGet(list.size());
                    
                    batchwrite.batchWriteResult((Collection<MatchedXlinkedPeptide>) list.clone());
                    if (m_doFreeMatch) {
                        for (MatchedXlinkedPeptide match : list)
                            match.free();
                    }
                    list.clear();
                }
                
                if (m_doFlush.get()) {

                    
                    m_buffer.drainTo(list);
                    m_countForwardedMatches.addAndGet(list.size());
                    if (!list.isEmpty()) {
                        batchwrite.batchWriteResult(list);
                        if (m_doFreeMatch) {
                            for (MatchedXlinkedPeptide match : list)
                                match.free();
                        }
                        list.clear();
                    }
                    
                    super.flush();
                    m_doFlush.getAndSet(false);
                }


            } catch (InterruptedException ex) {
                Logger.getLogger(BufferedResultWriter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
//        getInnerWriter().finished();
//        getInnerWriter().waitForFinished();
//        synchronized(m_finished_sync) {
        m_finished.set(true);
        allActiveWriters.remove(this);
//        }

//        synchronized (m_activeSync) {
//            if (m_active) {
////                decActiveCounter();
//                m_active = false;
//            }
//        }
    }
    
    public void selfFinished() {
        setRunning(false);
        //m_innerWriter.finished();
    }

    public int getResultCount() {
        return m_countMatches.get();
    }

    public int getTopResultCount() {
        return m_countTopMatches.get();
    }
    
    public void setFreeMatch(boolean doFree) {
        m_doFreeMatch = doFree;
    }

    @Override
    public boolean selfWaitForFinished() {
        if (m_running.get()) {
            selfFinished();
        }
        boolean waiting = true;
        int countDown = 100;
        while (waiting || (countDown<0 && m_buffer.isEmpty())) {
            try {
                waiting = !(m_finished.get());
                if (waiting) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(BufferedResultWriter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return true;

    }

    public boolean isFinished() {
        if (m_buffer.isEmpty())
            return !m_runner.isAlive();
        else return false;
    }

    public boolean isBufferEmpty() {
        return m_buffer.isEmpty();
    }

    public int bufferedMatches() {
        return m_buffer.size();
    }    
    public boolean isAlive() {
        return m_runner.isAlive();
    }
    
    /**
     * flush but don't forward the flush to the inner writer
     */
    public void selfFlush() {
//        boolean doFlush;
        m_doFlush.set(true);
        if (m_runner.isAlive() && !m_buffer.isEmpty()) {
            while ((m_doFlush.get() || (m_finished.get() && !m_buffer.isEmpty())) && m_runner.isAlive() ) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(BufferedResultWriter.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }    
    
    
    /**
     * flush
     */
    public void flush() {
//        boolean doFlush;
        selfFlush();
        getInnerWriter().flush();
    }

    /**
     * @return the m_running
     */
    public boolean isRunning() {
        return m_running.get();
    }

    /**
     * @param m_running the m_running to set
     */
    protected void setRunning(boolean running) {
        this.m_running.getAndSet(running);
    }

    /**
     * @return the m_runner
     */
    public Thread getBufferThread() {
        return m_runner;
    }

    /**
     * @return the m_countForwardedMatches
     */
    public int getForwardedMatchesCount() {
        return m_countForwardedMatches.get();
    }
}
