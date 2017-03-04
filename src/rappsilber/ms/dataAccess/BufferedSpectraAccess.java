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
package rappsilber.ms.dataAccess;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author stahir
 */
public class BufferedSpectraAccess extends AbstractSpectraAccess implements Runnable, StackedSpectraAccess {//, MultiReadSpectraAccess {

    private SpectraAccess m_innerAccess;
    private ArrayBlockingQueue<Spectra> m_buffer = new ArrayBlockingQueue<Spectra>(10);
    private int m_numberSpectra = 0;
    private Thread m_fillBuffer;
    private final Object m_accessSynchronisation = new Object();
    private boolean m_finishedReading = false;
    private int m_buffersize = 10;
    private final ReentrantLock lock = new ReentrantLock();

    public BufferedSpectraAccess(int BufferSize) {
        m_buffer = new ArrayBlockingQueue<Spectra>(BufferSize);
        m_fillBuffer = new Thread(this);
        m_fillBuffer.setName("BufferedSpectraAccess_fill" + m_fillBuffer.getId());
        m_buffersize = BufferSize;
    }

    public BufferedSpectraAccess(SpectraAccess source, int BufferSize) {
        this(BufferSize);
        setReader(source);
    }

    @Override
    public Spectra current() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * returns whether another spectra can be read.
     * in a multi-threaded case the result of hasNext can be only be taken as a 
     * hint. Unless hasNext and next are consistently synchronised one thread 
     * can receive the next element that was here indicated to be available.
     * As this is a general case no synchronisation is performed here.
     * @return true if another spectra should be retrievable; false otherwise
     */
    @Override
    public boolean hasNext() {
        return innerHasNext() || ! m_buffer.isEmpty();
    }

    private boolean innerHasNext() {
        if (m_innerAccess.hasNext() && m_finishedReading)
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Reader finished before all data where read in.", new Exception());
        return (m_innerAccess.hasNext() || !m_buffer.isEmpty());
    }
    long lastEmptyReported = Calendar.getInstance().getTimeInMillis() - 30000;
    int bec = 0;
    boolean doReportEmpty = true;
    int emptyReported = 0;

    /**
     * returns the next available spectra or null if already all spectra where 
     * read.
     * This is synchronised
     * @return 
     */
    @Override
    public Spectra next() {
        
        // mark up if we have nothing left in the buffer
        if (m_buffer.isEmpty()) {
            if (doReportEmpty) {
                bec++;
                long n = Calendar.getInstance().getTimeInMillis();
                // but don't do it all the time
                if (n - lastEmptyReported > 30000) {
                    lastEmptyReported = n;
                    Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Buffer is empty on next (" + bec + ") " + Thread.currentThread().getName() );
                    //bec = 0;
                    if (emptyReported++ > 10) {
                        doReportEmpty = false;
                    }
                }
            }
        }

        lock.lock();
        try{
            if (innerHasNext() || !m_buffer.isEmpty()) {
                try {
                    Spectra s = null;
                    while ((innerHasNext() || !m_buffer.isEmpty()) && s == null) {
                        s = m_buffer.poll(100, TimeUnit.MILLISECONDS);
                    }
                    if (s != null)
                        m_numberSpectra++;
                    return s;
                } catch (InterruptedException ex) {
                    return null;
                }
            } else {
                return null;
            }
        } finally{
            lock.unlock();
        }
    }


//    @Override
    public int next(int maxcount, Collection<Spectra> c) {

        if (m_buffer.isEmpty()) {
            if (doReportEmpty) {
                bec++;
                long n = Calendar.getInstance().getTimeInMillis();
                // but don't do it all the time
                if (n - lastEmptyReported > 30000) {
                    lastEmptyReported = n;
                    Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Buffer is empty on batch next (" + bec + ") " + Thread.currentThread().getName() );
                    //bec = 0;
                    if (emptyReported++ > 10) {
                        doReportEmpty = false;
                    }
                }
            }
        }
        
        lock.lock();
        try {
            int ret = 0;
            if (innerHasNext()) {
                try {
                    Spectra s = null;
                    while (innerHasNext() && s == null) {
                        s = m_buffer.poll(100, TimeUnit.MILLISECONDS);
                    }
                    c.add(s);
                    ret = 1;
                } catch (InterruptedException ex) {
                }
                ret += m_buffer.drainTo(c,maxcount);
                m_numberSpectra+=ret;                
                return ret;
            }
        } finally {
            lock.unlock();
        }
        return 0;
    }

    
    @Override
    public void run() {
        if (m_innerAccess instanceof MultiReadSpectraAccess) {
            multiReadInner();
        } else {
            standardReadInner();
        }
//        synchronized (m_accessSynchronisation) {
            m_finishedReading = true;
//        }
    }

    @Override
    public int countReadSpectra() {
        return m_numberSpectra;
    }

//    @Override
//    public int getEntriesCount() {
//        if (m_innerAccess instanceof AbstractSpectraAccess)
//            return ((AbstractSpectraAccess)m_innerAccess).getEntriesCount();
//        return 0;
//
//    }
//
//    @Override
//    public int getSpectraCount() {
//        if (m_innerAccess instanceof AbstractSpectraAccess)
//            return ((AbstractSpectraAccess)m_innerAccess).getEntriesCount();
//        return 0;
//    }
    
    StackTraceElement[] startingFrom = null;
    @Override
    public void setReader(SpectraAccess innerAccess) {
        m_innerAccess = innerAccess;
        m_finishedReading = false;
        startingFrom = Thread.currentThread().getStackTrace();
        if (!m_fillBuffer.isAlive()) {
            m_fillBuffer.start();
            
        }
    }

    @Override
    public boolean canRestart() {
        return m_innerAccess.canRestart();
    }

    @Override
    public void restart() {
    }

    public void close() {
        m_innerAccess.close();
    }

    @Override
    public void gatherData() throws FileNotFoundException {
        if (m_innerAccess instanceof AbstractSpectraAccess) {
            ((AbstractSpectraAccess) m_innerAccess).gatherData();
        }
    }

    public int getSpectraCount() {
        if (m_innerAccess instanceof AbstractSpectraAccess) {
            return ((AbstractSpectraAccess) m_innerAccess).getSpectraCount();
        }
        return -1;
    }

    /**
     * the standard way to access an arbitrary inner SpectraAccess.
     */
    protected void standardReadInner() {
        try {
            while (m_innerAccess.hasNext()) {
                try {

                    Spectra s = m_innerAccess.next();
                    if (s != null) {
                        m_buffer.put(s);
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(BufferedSpectraAccess.class.getName()).log(Level.SEVERE, "Interrupted on put", ex);
                }
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            throw new Error(e);
        }
    }

    /**
     * if the inner spectra access is a {@link MultiReadSpectraAccess} then 
     * requesting several spectra at a time should be faster
     */ 
    protected void multiReadInner() {
        MultiReadSpectraAccess bsa = (MultiReadSpectraAccess) m_innerAccess;
        try {
            ArrayList<Spectra> prebuff = new ArrayList<Spectra>(m_buffersize);
            while (m_innerAccess.hasNext()) {
                
                bsa.next(m_buffersize, prebuff);
                //Spectra s = m_innerAccess.next();
                lock.lock(); 
                try {
                    for (Spectra s : prebuff) {
                       if (s != null) {
                            try {

                                    m_buffer.put(s);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Might have lost a spectra", ex);
                            }
                       } else {
                           Exception e = new NullPointerException("Got a null as spectra from the inner reader!");
                           Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Odd thing happened", e);
                       }
                    }
                } finally {
                    lock.unlock();
                }
                prebuff.clear();
            }
        } catch (Exception e) {
            Logger.getLogger(BufferedSpectraAccess.class.getName()).log(Level.SEVERE, null, e);
            throw new Error(e);
        }
    }


}
