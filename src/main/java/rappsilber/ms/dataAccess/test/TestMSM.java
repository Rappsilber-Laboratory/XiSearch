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
package rappsilber.ms.dataAccess.test;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JTextField;
import rappsilber.config.AbstractRunConfig;
import rappsilber.gui.components.ShowText;
import rappsilber.gui.components.getFileDialog;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class TestMSM {
    public static class TestMSMStatistic {
        public long countSpectra = 0;
        public long countPeaks = 0;
        public boolean wasError = false;
        public Exception errorException = null;
        private final  Object syncObject = new Object();
        
        public synchronized long increment(Spectra s) {
            countSpectra++;
            countPeaks+=s.getPeaks().size();
            return countSpectra;
        }
        
        public synchronized long getSpectra() {
            return countSpectra;
        }

        public synchronized long getPeaks() {
            return countPeaks;
        }
        
        public synchronized String toString() {
            String r  = "Spectra: " + countSpectra + "\nPeaks: " + countPeaks + (wasError? "\nError occured" : "\nSuccess");
            if (wasError) {
                errorException.printStackTrace(System.out);
                r += "\n" + errorException.getMessage();
                StackTraceElement[] st = errorException.getStackTrace();
                for (int i=0; i< st.length;i++) {
                    r+= "\n" + st[i].toString();
                }
            }            
            return r;
        }
    }    
    
    /**
     * Tests whether a file can be used as peak-list.
     * If there is a problem with the file an error message will be reported (the message returned from the error message)
     * @param msmFile
     * @return null if everything is ok; the error message otherwise
     */
    protected static TestMSMStatistic testMSM(String msmFile, TestMSMStatistic stats, ActionListener progress, ActionListener complete) {
        AbstractMSMAccess msm= null;
        boolean continueOnError=true;
        try {
            msm = AbstractMSMAccess.getMSMIterator(msmFile, new ToleranceUnit(0,"da"), 0,AbstractRunConfig.DUMMYCONFIG);
            while (msm.hasNext()) {
                try {
                    for (Spectra s : msm) {
                        long count = stats.increment(s);
                        if (progress != null && count % 1000 == 0) {
                            progress.actionPerformed(new ActionEvent(msm, (int)stats.countSpectra, "" + stats.countSpectra));
                        }
                    }
                    if (complete != null) {
                        complete.actionPerformed(new ActionEvent(stats, (int)stats.countSpectra, "" + stats.countSpectra));
                    }
                } catch (Exception e) {
                    if (continueOnError) {
                        Logger.getLogger(TestMSM.class.getName()).log(Level.WARNING, "error while reading file:", e);
                    } else {
                        throw e;
                    }
                    
                }
            }
            return stats;
        } catch (Exception e) {
            Logger.getLogger(TestMSM.class.getName()).log(Level.WARNING, "error while reading file:", e);
            stats.errorException = e;
            stats.wasError = true;
            if (complete != null) {
                complete.actionPerformed(new ActionEvent(stats, (int)stats.countSpectra, "" + stats.countSpectra));
            }
        }
        // every thing is fine
        return stats;
    }

    /**
     * Tests whether a file can be used as peak-list.
     * If there is a problem with the file an error message will be reported (the message returned from the error message)
     * @param msmFile
     * @return null if everything is ok; the error message otherwise
     */
    public static TestMSMStatistic testMSM(String msmFile) {
        TestMSMStatistic stats = new TestMSMStatistic();
        testMSM(msmFile, stats, null, null);
        // every thing is fine
        return stats;
    }    

    /**
     * Tests whether a file can be used as peak-list.
     * If there is a problem with the file an error message will be reported (the message returned from the error message)
     * @param msmFile
     * @param progress
     * @param complete
     * @return null if everything is ok; the error message otherwise
     */
    public static TestMSMStatistic testMSMAsync(final String msmFile, final ActionListener progress, final ActionListener complete) {
        final TestMSMStatistic stats = new TestMSMStatistic();
        Runnable runner = new Runnable() {

            public void run() {
                testMSM(msmFile, stats, progress, complete);
            }
        };
        Thread async = new Thread(runner);
        async.setName("TestMSM_" + async.getName());
        async.start();
        // every thing is fine
        return stats;
    }    
    
    public static void main(String[] args) throws InterruptedException {
        TestMSMStatistic result = null;
        final AtomicBoolean complete = new AtomicBoolean(false);
        JFrame progresreport = new JFrame("Progress");
        JTextField txtprogress = new JTextField();
        txtprogress.setPreferredSize(new Dimension(200, 60));
        progresreport.add(txtprogress);
        progresreport.setPreferredSize(new Dimension(200, 60));
        progresreport.setSize(new Dimension(200, 60));
        
        final Object syncProgress = new Object();
        
        
        if (args.length == 0) {
            File f = getFileDialog.getFile(new String[]{".apl",".mgf",".msm", ".zip"},"PeakLists");
            
            result = testMSMAsync(f.getAbsolutePath(),new ActionListener() {

                public void actionPerformed(ActionEvent e) { // called every 1000 spectra
                    synchronized(syncProgress) {
                        syncProgress.notify();
                    }
                    System.out.println("Progress: read pectra: " + e.getActionCommand());
                }
            },new ActionListener() { // called when the full msm list is read 

                public void actionPerformed(ActionEvent e) {
                    complete.set(true);
                    synchronized(syncProgress) {
                        syncProgress.notify();
                    }
                    System.out.println("Completed: read pectra: " + e.getActionCommand());
                }
            });
            
            progresreport.setVisible(true);
            while (!complete.get()) {
                synchronized(syncProgress) {
                    syncProgress.wait();
                }
                txtprogress.setText(result.toString());
            }
            progresreport.setVisible(false);
            progresreport.dispose();
            ShowText.showText(result.toString());
            
        }   else {
            result = testMSM(args[0]);
        }
        
            
        System.out.println(result.toString());
        if (result.wasError) {
            result.errorException.printStackTrace(System.out);
        }
        
    }    
}
