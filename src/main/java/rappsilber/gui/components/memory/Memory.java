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
package rappsilber.gui.components.memory;

import java.awt.Component;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import rappsilber.ui.MemMapStatusControl;
import rappsilber.utils.MyArrayUtils;
import rappsilber.utils.ObjectWrapper;
import rappsilber.utils.StringUtils;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class Memory extends javax.swing.JPanel {

    
    java.util.Timer m_scanTimer = new java.util.Timer(true);
    private int m_timeout = 600;
    Runtime runtime = Runtime.getRuntime();
    private MemMapStatusControl remoteMem;
    
    
    protected class ScanTask extends TimerTask {
        AtomicBoolean running = new AtomicBoolean(false);
        double recentMinFreeMem = 0;
        double recentMaxFreeMem = 0;
        LinkedList<Double> recent = new LinkedList<>();
        int maxRecent=10;
        int logMemory = 0;
        int didgc = 0;
        
        @Override
        public void run() {
            if (running.compareAndSet(false, true)) {
                try {
                    double fm = runtime.freeMemory();
                    String fmu = "B";
                    double mm = runtime.maxMemory();
                    double tm = runtime.totalMemory();
                    if (getRemoteMem() != null) {
                        fm = getRemoteMem().getFreeMem();
                        mm = getRemoteMem().getMaxMem();
                        tm = getRemoteMem().geTotalMem();
                    }
                    
                    
                    double um = tm-fm;
                    
                    
                    recent.add(um);
                    if (recent.size()>maxRecent) {
                        recent.removeFirst();
                    }
                    ObjectWrapper<Double> min= new ObjectWrapper<>();
                    ObjectWrapper<Double> max = new ObjectWrapper<>();
                    MyArrayUtils.minmax(recent,min,max);
                    String message = "Used: " + StringUtils.toHuman(um) + " of " + StringUtils.toHuman(mm) + "  (Free:" + StringUtils.toHuman(fm) + " Total:" + StringUtils.toHuman(tm) + " Max:"+ StringUtils.toHuman(mm) +") (recent used:[" + (min.value == null ? "Min is NULL" : StringUtils.toHuman(min.value)) +".." + (max.value ==null ? "Max is NULL" :  StringUtils.toHuman(max.value)) +"])";
                    if (tglLog.isSelected()) {
                        if (logMemory++ % 60 == 0 ) {
                            Logger.getLogger(Memory.class.getName()).log(Level.INFO,message);
                        }
                    } else 
                        logMemory = 0;
                    if (txtMemory!=null) {
                        txtMemory.setText(message);
                    }
                    if (tglAGC.isSelected() && mm-um < 10*1024*1024 && didgc== 0) {
                        Logger.getLogger(Memory.class.getName()).log(Level.INFO,"AutoGC triggered");
                        if (getRemoteMem()!=null) {
                            getRemoteMem().initiateGC();
                        }
                        message = "Used: " + StringUtils.toHuman(um) + " of " + StringUtils.toHuman(mm) + "  (Free:" + StringUtils.toHuman(fm) + " Total:" + StringUtils.toHuman(tm) + " Max:"+ StringUtils.toHuman(mm) +")";
                        Logger.getLogger(Memory.class.getName()).log(Level.INFO,"Memory before GC:" + message);
                        
                        System.gc();
                        System.gc();
                        
                        fm = runtime.freeMemory();
                        mm = runtime.maxMemory();
                        tm = runtime.totalMemory();
                        um = tm-fm;
                        message = "Used: " + StringUtils.toHuman(um) + " of " + StringUtils.toHuman(mm) + "  (Free:" + StringUtils.toHuman(fm) + " Total:" + StringUtils.toHuman(tm) + " Max:"+ StringUtils.toHuman(mm) +")";
                        Logger.getLogger(Memory.class.getName()).log(Level.INFO,"Memory after GC:" + message);
                        didgc=100;
                    } else if (didgc>0) {
                        didgc--;
                    }
                } catch (Exception e) {
                    Logger.getLogger(Memory.class.getName()).log(Level.INFO,"Error during memory display:",e);
                }
                running.set(false);
            }
        }
        
    }
    
    /**
     * Creates new form Memory
     */
    public Memory() {
        initComponents();
        m_scanTimer.scheduleAtFixedRate(new ScanTask(), 1000, 1000);
        this.addAncestorListener(new AncestorListener() {

            public void ancestorAdded(AncestorEvent event) {
                Component c =  Memory.this;
                while (c.getParent() != null) {
                    c = c.getParent();
                }
                if (c instanceof java.awt.Window) {
                    ((java.awt.Window) c).addWindowListener(new WindowListener() {

                        public void windowOpened(WindowEvent e) {
                            m_scanTimer.scheduleAtFixedRate(new ScanTask(), 1000, 1000);
                        }

                        public void windowClosing(WindowEvent e) {
                        }

                        public void windowClosed(WindowEvent e) {
                            m_scanTimer.cancel();
                        }

                        public void windowIconified(WindowEvent e) {
                        }

                        public void windowDeiconified(WindowEvent e) {
                        }

                        public void windowActivated(WindowEvent e) {
                        }

                        public void windowDeactivated(WindowEvent e) {
                        }
                    });
                }
            }

            public void ancestorRemoved(AncestorEvent event) {
            }

            public void ancestorMoved(AncestorEvent event) {
                
            }
        });
    }
    
    @Override
    protected void finalize() {
        m_scanTimer.cancel();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        txtMemory = new javax.swing.JTextField();
        gc = new javax.swing.JButton();
        tglLog = new javax.swing.JToggleButton();
        tglAGC = new javax.swing.JToggleButton();

        gc.setText("gc");
        gc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gcActionPerformed(evt);
            }
        });

        tglLog.setText("log");

        tglAGC.setText("aGC");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(txtMemory, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tglLog)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tglAGC)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(gc)
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtMemory)
                    .addComponent(tglAGC, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(gc, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tglLog, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void gcActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gcActionPerformed

        double fm = runtime.freeMemory();
        double mm = runtime.maxMemory();
        double tm = runtime.totalMemory();
        double um = tm-fm;
        Logger.getLogger(this.getClass().getName()).log(Level.INFO,"GC triggered");
        String message = "Used: " + StringUtils.toHuman(um) + " of " + StringUtils.toHuman(mm) + "  (Free:" + StringUtils.toHuman(fm) + " Total:" + StringUtils.toHuman(tm) + " Max:"+ StringUtils.toHuman(mm) +")";
        Logger.getLogger(Memory.class.getName()).log(Level.INFO,"Memory before GC:" + message);
        System.gc();
        System.gc();
        
        fm = runtime.freeMemory();
        mm = runtime.maxMemory();
        tm = runtime.totalMemory();
        um = tm-fm;
        message = "Used: " + StringUtils.toHuman(um) + " of " + StringUtils.toHuman(mm) + "  (Free:" + StringUtils.toHuman(fm) + " Total:" + StringUtils.toHuman(tm) + " Max:"+ StringUtils.toHuman(mm) +")";
        Logger.getLogger(Memory.class.getName()).log(Level.INFO,"Memory after GC:" + message);        
        if (remoteMem != null) {
            remoteMem.initiateGC();
        }
                      
    }//GEN-LAST:event_gcActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton gc;
    private javax.swing.JToggleButton tglAGC;
    private javax.swing.JToggleButton tglLog;
    private javax.swing.JTextField txtMemory;
    // End of variables declaration//GEN-END:variables

    /**
     * @return the remoteMem
     */
    public MemMapStatusControl getRemoteMem() {
        return remoteMem;
    }

    /**
     * @param remoteMem the remoteMem to set
     */
    public void setRemoteMem(MemMapStatusControl remoteMem) {
        this.remoteMem = remoteMem;
    }
}
