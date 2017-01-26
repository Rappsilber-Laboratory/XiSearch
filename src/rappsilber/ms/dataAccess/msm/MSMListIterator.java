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
package rappsilber.ms.dataAccess.msm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.statistics.utils.ObjectContainer;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MSMListIterator extends AbstractMSMAccess {

    private ArrayList<AbstractMSMAccess> m_MSMiterators = new ArrayList<AbstractMSMAccess>();
    private Iterator<AbstractMSMAccess>  m_iterator = null;
    private AbstractMSMAccess m_current;
    private AbstractMSMAccess m_next;
    private int         m_countReadSpectra = 0;
    private int         m_minCharge = 2;
    private int         m_file_id = 0;
    private RunConfig   m_config;
    private int        m_nextID = 0;
    
    protected Object gatherDataSync = new Object();


    public MSMListIterator(ToleranceUnit t , int minCharge, RunConfig config) throws FileNotFoundException, IOException  {
        m_config = config;
        setToleranceUnit(t);
        m_minCharge = minCharge;

    }


    public MSMListIterator(File MSMListFile, ToleranceUnit t , int minCharge, RunConfig config) throws FileNotFoundException, IOException, ParseException  {
        this(t,minCharge,config);
        m_inputPath = MSMListFile.getAbsolutePath();
        BufferedReader list  = new BufferedReader(new FileReader(MSMListFile));
        String line;
        while ((line = list.readLine()) != null) {
            line = line.trim();
            if (line.length() > 0 &&  ! line.startsWith("#")) {
                addFile(line, MSMListFile.getParent(), t);
            }
        }
        setNext();
    }

    public MSMListIterator(String[] MSMListFile, String basePath, ToleranceUnit t , int minCharge, RunConfig config) throws FileNotFoundException, IOException, ParseException  {
        this(t,minCharge,config);
        for (String f: MSMListFile)
                addFile(f, basePath, t);
        setNext();

    }

    public MSMListIterator(ArrayList<String> MSMListFile, String basePath, ToleranceUnit t , int minCharge, RunConfig config) throws FileNotFoundException, IOException, ParseException  {
        this(MSMListFile.toArray(new String[MSMListFile.size()]),basePath, t,minCharge,config);
    }
    
    public String addFile(String line, String ParentPath, ToleranceUnit t) throws FileNotFoundException, IOException, ParseException {
        File msm = new File(line);
        if (!msm.exists()) {
            if (ParentPath == null) {
                throw new FileNotFoundException("could not find referenced MSM-file: " + line);
            }
            msm = new File(ParentPath + File.separator + line);
            // System.out.println("read msm file " + msm.getAbsoluteFile());
        }
        m_MSMiterators.add(AbstractMSMAccess.getMSMIterator(msm, t, m_minCharge, m_config));
        return msm.getAbsolutePath();
    }

    public void addAccess(AbstractMSMAccess inner) {
        m_MSMiterators.add(inner);
    }
    
    public String addFile(String name, InputStream msmfile, ToleranceUnit t) throws FileNotFoundException, IOException, ParseException {
        AbstractMSMAccess inner = AbstractMSMAccess.getMSMIterator(name, msmfile, t, m_minCharge, m_config);
        m_MSMiterators.add(inner);
        return name;
    }
    
    protected void setNext() {
        if (m_iterator == null) {
            m_iterator = m_MSMiterators.iterator();
            m_next = m_iterator.next();
        }
        m_current = m_next;
        if (m_iterator.hasNext())
            m_next = m_iterator.next();
        
        while (m_iterator.hasNext() && !m_next.hasNext()) {
            m_next = m_iterator.next();
        }
        Logger.getLogger(MSMListIterator.class.getName()).log(Level.INFO, "now read data from " + m_current.getInputPath());

    }
    
    @Override
    public void gatherData() throws FileNotFoundException {
        m_iterator = m_MSMiterators.iterator();
        m_next = m_iterator.next();
        setNext();

        m_MaxPrecursorMass = 0;
        m_scanCount = 0;
//        m_countSpectra = 0;
        for (AbstractMSMAccess i : m_MSMiterators) {
            Logger.getLogger(MSMListIterator.class.getName()).log(Level.INFO, "Collect some data from " + m_current.getInputPath());
            i.gatherData();
            m_MaxPrecursorMass = Math.max(m_MaxPrecursorMass, i.getMaxPrecursorMass());
//            m_scanCount += i.getEntriesCount();
            m_scanCount += i.getSpectraCount();
        }
    }
    
    

    protected void gatherData(AbstractMSMAccess inner) throws FileNotFoundException, IOException {
        double MaxPrecursorMass = 0;
        int scanCount = 0;
        
        inner.gatherData();
        inner.restart();
        
        synchronized(gatherDataSync) {
            m_MaxPrecursorMass = Math.max(m_MaxPrecursorMass, inner.m_MaxPrecursorMass);
            m_scanCount += inner.m_scanCount;
        }
        
    }    
    
    protected void gatherData(MSMListIterator inner, int cpus) throws FileNotFoundException, IOException {
        double MaxPrecursorMass = 0;
        int scanCount = 0;
        
        inner.gatherData(cpus);
        inner.restart();
        
        synchronized(gatherDataSync) {
            m_MaxPrecursorMass = Math.max(m_MaxPrecursorMass, inner.m_MaxPrecursorMass);
            m_scanCount += inner.m_scanCount;
        }
        
    }        
    
    public void gatherData(final int cpus) throws FileNotFoundException {
        if (cpus <= 1)  {
            gatherData();
            return;
        }
        Thread[] gatherthread;
        m_iterator = m_MSMiterators.iterator();
        m_next = m_iterator.next();
        setNext();
        final CountDownLatch threadwait = new  CountDownLatch(cpus);

        m_MaxPrecursorMass = 0;
        m_scanCount = 0;
        gatherthread = new Thread[Math.min(cpus,m_MSMiterators.size())];
        HashMap<AbstractMSMAccess,ObjectContainer<FileNotFoundException>> ex = new HashMap<AbstractMSMAccess, ObjectContainer<FileNotFoundException>>();
//        m_countSpectra = 0;
        
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "gather data multithreaded");
        
        int nextThread=0;
        for (final AbstractMSMAccess i : m_MSMiterators) {

            String message = "Collect some data from " + i.getInputPath();
            Logger.getLogger(MSMListIterator.class.getName()).log(Level.INFO, message);
            System.err.println(message);
            
            boolean noThread = true;
            while (noThread) {

                
                if (gatherthread[nextThread] == null || !(gatherthread[nextThread].isAlive())) {
                    final ObjectContainer<FileNotFoundException> exCont = new ObjectContainer<FileNotFoundException>();
                    ex.put(i, exCont);
                    gatherthread[nextThread] = new Thread() {
                        public void run (){
                            try {

                                if (i instanceof MSMListIterator) {
                                    gatherData(((MSMListIterator) i),Math.max(1,(int)(cpus/2.0+0.5)));
                                } else {
                                    gatherData(i);
                                }
                                String message = "finished reading " + i.getInputPath();
                                Logger.getLogger(MSMListIterator.class.getName()).log(Level.INFO, message);
                                System.err.println(message);
                            } catch (FileNotFoundException ex1) {
                                Logger.getLogger(MSMListIterator.class.getName()).log(Level.SEVERE, null, ex1);
                                exCont.obj = ex1;
                            } catch (IOException ex1) {
                                Logger.getLogger(MSMListIterator.class.getName()).log(Level.SEVERE, null, ex1);
                            }
                        }
                    };
                    gatherthread[nextThread].setName(gatherthread[nextThread].getName()+ " - gather peaklist info" );
                    gatherthread[nextThread].start();
                    noThread = false;
                } else {
                    nextThread = (nextThread + 1) % cpus;
                    if (nextThread == 0) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex1) {
                        }
                    }
                }
                
            }
            for (ObjectContainer<FileNotFoundException> exCont : ex.values()) {
                if (exCont.obj != null)
                    throw exCont.obj;
            }
//            i.gatherData();
//            m_MaxPrecursorMass = Math.max(m_MaxPrecursorMass, i.getMaxPrecursorMass());
////            m_scanCount += i.getEntriesCount();
//            m_scanCount += i.getSpectraCount();
        }
        Logger.getLogger(MSMListIterator.class.getName()).log(Level.INFO, "Waiting for the data collection to to finish");
        Util.joinAllThread(gatherthread);
        
        
        try {
            System.err.println("Reopening input msm files");
            restart();
        } catch (IOException ioex) {
            Logger.getLogger(ZipMSMListIterator.class.getName()).log(Level.SEVERE, "Error reopening files", ioex);
        }        
        
    }
    
    
    @Override
    public Spectra current() {
        return m_current.current();
    }

    public boolean hasNext() {
        return m_current.hasNext() || m_next.hasNext();
    }
    
    protected void publishNextSpectra(Spectra s){
        
    }

    public AbstractMSMAccess getCurrentAccessor() {
        return m_current;
    }

    public synchronized Spectra next() {
        if (!m_current.hasNext()) {
            setNext();
        }
        if (m_current.hasNext()) {
            m_countReadSpectra++;
        }
        Spectra n = m_current.next();
        publishNextSpectra(n);
        if (n!= null) 
            n.setReadID(m_nextID++);
        return n;
    }


    @Override
    public int countReadSpectra() {
        return m_countReadSpectra;
    }

    protected void setReadSpectra(int i) {
        m_countReadSpectra = i;
    }    
    
    public AbstractMSMAccess getCurrentMSMFile() {
        return m_current;
    }

    /**
     * @return the m_inputPath
     */
    public String getInputPath() {
        if (m_current == null)
            return m_inputPath ;
        else
            return m_inputPath + " -> " + m_current.getInputPath();
    }

    @Override
    public boolean canRestart() {
        return true;
    }

    @Override
    public void restart()  throws IOException {
        m_nextID = 0;
        for (AbstractMSMAccess msm : m_MSMiterators) {
            msm.restart();
        }

        m_iterator = null;
        setNext();

    }
    
    public void init() {
        if (m_current == null) 
            setNext();
    }
    
    

    @Override
    public void close() {
        for (AbstractMSMAccess msm : m_MSMiterators) {
            msm.close();
        }
        m_iterator = null;
        m_current = null;
        m_next = null;
    }

    protected void closeAndForget() {
        close();
        m_MSMiterators.clear();
    }
    /**
     * if this listiterator is part of a outer level listiterator, can it just forward the inner reader?
     * @return 
     */
    public boolean canTransfer() {
        return true;
    }


}
