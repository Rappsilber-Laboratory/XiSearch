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
package rappsilber.gui.localapplication;

import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import rappsilber.config.AbstractRunConfig;
import rappsilber.data.csv.CSVRandomAccess;
import rappsilber.gui.components.GenericTextPopUpMenu;
import rappsilber.gui.logging.JMessageBoxHandle;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.calibration.StreamingCalibrate;
import rappsilber.ms.dataAccess.filter.spectrafilter.RandomSpectraSubset;
import rappsilber.ms.dataAccess.filter.spectrafilter.ScanFilteredSpectrumAccess;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.dataAccess.msm.MSMListIterator;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ConsistentPeaks extends javax.swing.JFrame {
//    private boolean m_filterChanging = false;

    /** Creates new form ConsistentPeaks */
    public ConsistentPeaks() {
        initComponents();
        JMessageBoxHandle errorLog = new JMessageBoxHandle(false);
        errorLog.setFilter(new Filter() {
            public boolean isLoggable(LogRecord record) {
                return true;
            }
        });

        errorLog.setLevel(Level.WARNING);

        Logger.getLogger("rappsilber").addHandler(errorLog);
        Logger.getLogger("rappsilber").setLevel(Level.ALL);
        Logger.getLogger("rappsilber").addHandler(errorLog);

        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Logger Connected");

//        DefaultTableModel tm;
//        tm = ((DefaultTableModel)tblTargetPeaks.getModel());
//        tm.addTableModelListener(new AutoAddTableModelListener());
        
        
//        fbScanFilterFile.setDescription("CSV-file");
//        fbScanFilterFile.setExtensions(new String[]{".csv"});
//        fbMSMFile.setDescription("MSM-File");
//        fbMSMFile.setExtensions(new String[]{".msm",".msmlist"});
//        fbCSVOut.setDescription("CSV-File");
//        fbCSVOut.setExtensions(new String[]{".csv"});
//        fbCSVOut.setSave();
//
//        // replace the deault (nicly gui-generated) table model with a new one
//        //tblScanFilter.setModel(new AutoAddTableModel(tblScanFilter.getModel()));
//        tblScanFilter.setColumnSelectionAllowed(false);
//        tblScanFilter.setRowSelectionAllowed(false);
//        tblScanFilter.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//
//
//        DefaultTableModel tm = ((DefaultTableModel)tblScanFilter.getModel());
//
//
//        tm.addTableModelListener(new AutoAddTableModelListener());
//        tm = ((DefaultTableModel)tblTargetPeaks.getModel());
//        tm.addTableModelListener(new AutoAddTableModelListener());
//
//        tm.addTableModelListener(new TableModelListener() {
//
//            public void tableChanged(TableModelEvent e) {
//                if (m_filterChanging)
//                    return;
//                DefaultTableModel tm = ((DefaultTableModel)e.getSource());
//                int LastRow = tm.getRowCount() -1;
//                m_filterChanging = true;0.0
//
//                if (tm.getValueAt(LastRow, 0) != null || tm.getValueAt(LastRow, 1) != null)
//                    tm.addRow(new Object[]{null,null});
//                if ((tm.getValueAt(e.getLastRow(), 0) == null || tm.getValueAt(e.getLastRow(), 0).toString().length() == 0)
//                        && tm.getValueAt(e.getLastRow(), 1) == null) {
//                    tm.removeRow(e.getLastRow());
//                }
//                m_filterChanging = false;
//            }
//
//        });
        GenericTextPopUpMenu copyPaste = new GenericTextPopUpMenu();
        copyPaste.installContextMenu(this);
        
        csvTargetPeaks.addHeaderChangedListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setTargetPeakHeaderComboBoxes();
            }
        });
        
    }

    public void setTargetPeakHeaderComboBoxes() {
        CSVRandomAccess csv = csvTargetPeaks.getCSV();
        csv.setAlternative("mz", "peak");
        csv.setAlternative("mz", "peak mz");
        csv.setAlternative("mz", "peak m/z");
        csv.setAlternative("mz", "m/z");
        int cols = csv.getMaxColumns();
        String[] comboBoxHeaders = new String[cols+1];
        comboBoxHeaders[0] = "--Select--";
        for (int i = 1;i<comboBoxHeaders.length;i++) {
            comboBoxHeaders[i] = csv.getHeader(i - 1);
        }
        
        cbMZColumn.setModel(new DefaultComboBoxModel(comboBoxHeaders));
        Integer scanCol = csv.getColumn("mz");
        if ( scanCol != null) {
            cbMZColumn.setSelectedIndex(scanCol+1);
        }
    }
    
    
    protected ArrayList<Double> getTargetPeaks() {
        ArrayList<Double> peaks= new ArrayList<>();
        CSVRandomAccess csv = csvTargetPeaks.getCSV();
        int col = cbMZColumn.getSelectedIndex()-1;
        csv.setRow(-1);
        while (csv.next()) {
            double d = csv.getDouble(col);
            if (d != Double.NaN) {
                peaks.add(d);
            }
        }
        if (peaks.size() >0) {
            return peaks;
        }
        return null;
    }
    
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        fbMSMFile = new rappsilber.gui.components.FileBrowser();
        jLabel3 = new javax.swing.JLabel();
        spToleranceValue = new javax.swing.JSpinner();
        cbToleranceUnit = new javax.swing.JComboBox();
        btnRun = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtResult = new javax.swing.JTextArea();
        fbCSVOut = new rappsilber.gui.components.FileBrowser();
        btnSave = new javax.swing.JButton();
        chkRandom = new javax.swing.JCheckBox();
        spRandom = new javax.swing.JSpinner();
        spMaxMZ = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        spMinMZ = new javax.swing.JSpinner();
        cbSplitByCharge = new javax.swing.JCheckBox();
        cbNeutralLoss = new javax.swing.JCheckBox();
        spMinCharge = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        flMSMFiles = new rappsilber.gui.components.FileList();
        jPanel2 = new javax.swing.JPanel();
        scanFilterComponentCsvCopyPaste1 = new rappsilber.gui.components.filter.ScanFilterComponentCsvCopyPaste();
        jPanel3 = new javax.swing.JPanel();
        cbMZColumn = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        csvTargetPeaks = new rappsilber.gui.components.CSVPanel();
        linearCalibration1 = new rappsilber.gui.components.LinearCalibration();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Xlink Consistent Peaks");

        jLabel2.setText("MSMFile");

        jLabel3.setText("Tolerance");

        spToleranceValue.setModel(new javax.swing.SpinnerNumberModel(Double.valueOf(30.0d), Double.valueOf(0.0d), null, Double.valueOf(1.0d)));

        cbToleranceUnit.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ppm", "da" }));

        btnRun.setText("run");
        btnRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRunActionPerformed(evt);
            }
        });

        txtResult.setColumns(20);
        txtResult.setRows(5);
        jScrollPane1.setViewportView(txtResult);

        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        chkRandom.setText("Random Subset");
        chkRandom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkRandomActionPerformed(evt);
            }
        });

        spRandom.setEnabled(false);

        spMaxMZ.setModel(new javax.swing.SpinnerNumberModel(Double.valueOf(7000.0d), Double.valueOf(0.0d), null, Double.valueOf(1.0d)));

        jLabel1.setText("Max m/z");

        jLabel4.setText("Min m/z");

        spMinMZ.setModel(new javax.swing.SpinnerNumberModel(Double.valueOf(120.0d), Double.valueOf(0.0d), null, Double.valueOf(1.0d)));

        cbSplitByCharge.setText("Split By Charge");
        cbSplitByCharge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbSplitByChargeActionPerformed(evt);
            }
        });

        cbNeutralLoss.setText("Neutral Loss");
        cbNeutralLoss.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbNeutralLossActionPerformed(evt);
            }
        });

        spMinCharge.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));

        jLabel5.setText("Minimum Charge");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(fbCSVOut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSave))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fbMSMFile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spToleranceValue))
                            .addComponent(jLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(spMinCharge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cbToleranceUnit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(45, 45, 45)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(cbSplitByCharge)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(cbNeutralLoss))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(chkRandom)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spRandom, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spMinMZ, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spMaxMZ, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 25, Short.MAX_VALUE)
                                .addComponent(btnRun)))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(fbMSMFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRun))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(spToleranceValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(chkRandom)
                            .addComponent(spRandom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4)
                            .addComponent(spMinMZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1)
                            .addComponent(spMaxMZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cbToleranceUnit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cbSplitByCharge)
                            .addComponent(cbNeutralLoss)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spMinCharge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 178, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnSave)
                    .addComponent(fbCSVOut, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jTabbedPane1.addTab("Peak List", jPanel1);
        jTabbedPane1.addTab("MSM FIles", flMSMFiles);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scanFilterComponentCsvCopyPaste1, javax.swing.GroupLayout.DEFAULT_SIZE, 773, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scanFilterComponentCsvCopyPaste1, javax.swing.GroupLayout.DEFAULT_SIZE, 282, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("SpectraFilter", jPanel2);

        cbMZColumn.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "--Select--" }));

        jLabel6.setText("m/z Column");

        csvTargetPeaks.setEditable(true);
        csvTargetPeaks.setShowSavePanel(false);
        csvTargetPeaks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                csvTargetPeaksActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbMZColumn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(csvTargetPeaks, javax.swing.GroupLayout.DEFAULT_SIZE, 773, Short.MAX_VALUE)
                        .addContainerGap())))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addComponent(csvTargetPeaks, javax.swing.GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(cbMZColumn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Target Peaks", jPanel3);
        jTabbedPane1.addTab("Calibration", linearCalibration1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 802, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRunActionPerformed
        Runnable runnable = new Runnable() {
            boolean m_splitByCharge = cbSplitByCharge.isSelected();

            public void run() {
                try {
                    btnRun.setEnabled(false);

                    ToleranceUnit t = new ToleranceUnit((Double) spToleranceValue.getModel().getValue(), cbToleranceUnit.getModel().getSelectedItem().toString());
                    String msmFile = fbMSMFile.getText();
                    MSMListIterator listInput = new MSMListIterator(t, 1,AbstractRunConfig.DUMMYCONFIG);
                    if (!msmFile.isEmpty()) {
                        listInput.addFile(msmFile, "", t);
                    }

                    //input = new MSMIterator(msm, conf.getFragmentTolerance());
                    File[] list =flMSMFiles.getFiles();
                    if (list.length>0) {
                        for (File f: list) {
                            listInput.addFile(f.getAbsolutePath(), "", t);
                        }
                    }
                    
                    listInput.init();
//                    AbstractMSMAccess msmiter = AbstractMSMAccess.getMSMIterator(fbMSMFile.getText(), t, 1, null);
                    AbstractMSMAccess msmiter = listInput;
                    SpectraAccess sa = msmiter;
                    StreamingCalibrate calibrate = linearCalibration1.getCalibration();

                    ScanFilteredSpectrumAccess fsa = getFilter();
                    if (fsa != null) {
                        fsa.setReader(sa);
                        sa = fsa;
                    }

//                    DeIsotoper di = new DeIsotoper();
//                    di.setReader(sa);
//                    sa = di;


                    if (chkRandom.isSelected()) {
                        RandomSpectraSubset rss = new RandomSpectraSubset(sa, (Integer) spRandom.getModel().getValue());
                        sa = rss;
                    }

                    if (calibrate != null) {
                        calibrate.setReader(sa);
                        sa = calibrate;
                    }

                    ArrayList<Double> TargetPeaks = getTargetPeaks();



                    String result;
                    StringBuffer sb = new StringBuffer();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(bos);
                    //rappsilber.applications.ConsistentPeaksFixedBins.run(fbMSMFile.getFile(), t, fsa, ps);
                    if (cbNeutralLoss.isSelected()) {
                        rappsilber.applications.NeutralLostFixedBins.run(sa, t, ps, (Double) spMinMZ.getModel().getValue(), (Double) spMaxMZ.getModel().getValue(), (Integer) spMinCharge.getModel().getValue(), TargetPeaks);
                    } else {
                        rappsilber.applications.ConsistentPeaksFixedBins.run(sa, t, ps, (Double) spMinMZ.getModel().getValue(), (Double) spMaxMZ.getModel().getValue(), (Integer) spMinCharge.getModel().getValue(), TargetPeaks, m_splitByCharge);
                    }
                    txtResult.setText(bos.toString());
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(ConsistentPeaks.class.getName()).log(Level.WARNING, "File " + fbMSMFile.getText() + " not found", ex);
                } catch (IOException ex) {
                    Logger.getLogger(ConsistentPeaks.class.getName()).log(Level.SEVERE, "Error wile reading file " + fbMSMFile.getText(), ex);
                } catch (Exception ex) {
                    System.err.println(ex);
                    ex.printStackTrace(System.err);
                    Logger.getLogger(ConsistentPeaks.class.getName()).log(Level.SEVERE, "Error :", ex);
                } finally {
                    btnRun.setEnabled(true);
                }
                btnRun.setEnabled(true);
            }
        };

        Thread t = new Thread(runnable);
        t.setName("FindConsistentPeaks");
        t.start();


    }//GEN-LAST:event_btnRunActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        String result = txtResult.getText();
        if (fbCSVOut.getFile() == null) {
            Logger.getLogger(ConsistentPeaks.class.getName()).log(Level.WARNING, "No file selected");
            return;
        }
        if (result.length() == 0) {
            Logger.getLogger(ConsistentPeaks.class.getName()).log(Level.WARNING, "Nothing to be writen as - have you started the run?");
            return;
        }

        try {
            PrintStream ps = new PrintStream(fbCSVOut.getFile());
            ps.println(result);
            ps.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ConsistentPeaks.class.getName()).log(Level.WARNING, "File " + fbCSVOut.getFile().getAbsolutePath() + " not found", ex);
        }
    }//GEN-LAST:event_btnSaveActionPerformed

    private void chkRandomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkRandomActionPerformed
        spRandom.setEnabled(chkRandom.isSelected());
}//GEN-LAST:event_chkRandomActionPerformed

    private void cbNeutralLossActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbNeutralLossActionPerformed
        if (cbNeutralLoss.isSelected()) {
            cbSplitByCharge.setEnabled(false);
//            cbToleranceUnit.getModel().setSelectedItem(cbToleranceUnit.getModel().getElementAt(1));
//            cbToleranceUnit.setEnabled(false);
        } else  {
            cbSplitByCharge.setEnabled(true);
//            cbToleranceUnit.setEnabled(true);
        }
    }//GEN-LAST:event_cbNeutralLossActionPerformed

    private void cbSplitByChargeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbSplitByChargeActionPerformed
        if (cbSplitByCharge.isSelected()) {
            cbNeutralLoss.setEnabled(false);
        } else {
            cbNeutralLoss.setEnabled(true);
        }
    }//GEN-LAST:event_cbSplitByChargeActionPerformed

    private void csvTargetPeaksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_csvTargetPeaksActionPerformed
        setTargetPeakHeaderComboBoxes();
    }//GEN-LAST:event_csvTargetPeaksActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ConsistentPeaks().setVisible(true);
            }
        });
    }


    protected ScanFilteredSpectrumAccess getFilter() {
        
        ScanFilteredSpectrumAccess fsa = scanFilterComponentCsvCopyPaste1.getScanFilter();
//        DefaultTableModel tm = (DefaultTableModel) tblScanFilter.getModel();
//        int count =0;
//        for (int i = 0; i< tm.getRowCount(); i++) {
//            if (tm.getValueAt(i, 0) != null&& tm.getValueAt(i, 1) != null) {
//                fsa.SelectScan(tm.getValueAt(i, 0).toString(), (Integer)tm.getValueAt(i, 1));
//                count ++;
//            }
//        }
        return fsa;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnRun;
    private javax.swing.JButton btnSave;
    private javax.swing.JComboBox cbMZColumn;
    private javax.swing.JCheckBox cbNeutralLoss;
    private javax.swing.JCheckBox cbSplitByCharge;
    private javax.swing.JComboBox cbToleranceUnit;
    private javax.swing.JCheckBox chkRandom;
    private rappsilber.gui.components.CSVPanel csvTargetPeaks;
    private rappsilber.gui.components.FileBrowser fbCSVOut;
    private rappsilber.gui.components.FileBrowser fbMSMFile;
    private rappsilber.gui.components.FileList flMSMFiles;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private rappsilber.gui.components.LinearCalibration linearCalibration1;
    private rappsilber.gui.components.filter.ScanFilterComponentCsvCopyPaste scanFilterComponentCsvCopyPaste1;
    private javax.swing.JSpinner spMaxMZ;
    private javax.swing.JSpinner spMinCharge;
    private javax.swing.JSpinner spMinMZ;
    private javax.swing.JSpinner spRandom;
    private javax.swing.JSpinner spToleranceValue;
    private javax.swing.JTextArea txtResult;
    // End of variables declaration//GEN-END:variables

}
