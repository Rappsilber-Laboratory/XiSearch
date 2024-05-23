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
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import rappsilber.config.AbstractRunConfig;
import rappsilber.gui.components.GenericTextPopUpMenu;
import rappsilber.ms.dataAccess.filter.fastafilter.FastaFilter;
import rappsilber.ms.dataAccess.filter.fastafilter.FilterByID;
import rappsilber.ms.dataAccess.filter.fastafilter.MultiFilterAnd;
import rappsilber.ms.dataAccess.filter.fastafilter.NoFilter;
import rappsilber.ms.dataAccess.filter.fastafilter.RandomFilter;
import rappsilber.ms.dataAccess.filter.fastafilter.SizeRangeFilter;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.SequenceList;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FastaTools extends javax.swing.JFrame {

    /** Creates new form FastaTools */
    public FastaTools() {
        initComponents();
        fbFastaLoad.setExtensions(new String[] {".fasta", ".txt", ".fastalist"});
        fbFastaSave.setExtensions(new String[] {".fasta"});
        
        
        
        GenericTextPopUpMenu copyPaste = new GenericTextPopUpMenu();
        copyPaste.installContextMenu(this);
        
        fbFastaLoad.setEnabled(false);
        fbFastaSave.setEnabled(false);
        
        csvIdentifications.setEditable(true);

        csvIdentifications.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String[] columns = new String[csvIdentifications.getCSV().getMaxColumns()];
                for (int i = 0;i<columns.length; i++) {
                    columns[i] = csvIdentifications.getCSV().getHeader(i);
                }
                cbColumns.setModel(new DefaultComboBoxModel(columns));
            }
        });

        csvIdentifications.addHeaderChangedListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String[] columns = new String[csvIdentifications.getCSV().getMaxColumns()];
                for (int i = 0;i<columns.length; i++) {
                    columns[i] = csvIdentifications.getCSV().getHeader(i);
                }
                cbColumns.setModel(new DefaultComboBoxModel(columns));
            }
        });        
        
       
    }
    
    
    
    public SequenceList getSequences() {
        SequenceList sl = null;
        MultiFilterAnd filter = new MultiFilterAnd();
        filter.addFilter(getAccessionsFilter());
        if (ckRandomSubset.isSelected()) {
            if (ckPreferedSize.isSelected()) {
                filter.addFilter(new RandomFilter((Integer)spCountProteins.getValue(), (Integer)spPreferedSize.getValue()));
            } else {
                filter.addFilter(new RandomFilter((Integer)spCountProteins.getValue()));
            }
        }
        if (ckSize.isSelected()) {
            filter.addFilter(new SizeRangeFilter((Integer)spSizeMin.getValue(), (Integer)spSizeMax.getValue()));
        }
        
        // read from a file
        if (rbInputFile.isSelected()) {
            
            try {
                sl = new SequenceList(SequenceList.DECOY_GENERATION.ISTARGET, fbFastaLoad.getFile(), AbstractRunConfig.DUMMYCONFIG);
                
            } catch (IOException ex) {
                Logger.getLogger(FastaTools.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (rbInputMultiFasta.isSelected()) {
            sl=new SequenceList(SequenceList.DECOY_GENERATION.ISTARGET, AbstractRunConfig.DUMMYCONFIG);
            for (File f : flFastaFiles.getFiles()){
                try {
                    sl.addFasta(f);
                } catch (IOException ex) {
                    Logger.getLogger(FastaTools.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
                
        } else {
            // read from textfield
            String f = txtFastaIn.getText();
            if (f.replaceAll("[\\n\\r\\s\\t]", "").length() == 0) {
                return null;
            }
            String[] s = f.split("[\\n\\r]+");
            sl = new SequenceList(AbstractRunConfig.DUMMYCONFIG);
            StringBuffer aaseq = new StringBuffer();
            int sc = 0;
            String fh = ">Sequnence" + sc;
            for (int r = 0; r< s.length; r++) {
                String line = s[r].trim();
                if (line.startsWith(">")) {
                    if (aaseq.length() > 0) {
                        sl.add(new Sequence(aaseq.toString(), fh,AbstractRunConfig.DUMMYCONFIG));
                    }
                    fh= line;
                    aaseq = new StringBuffer();
                    sc++;
                } else {
                    line = line.replaceAll("[\\s\\t]", "");
                    aaseq.append(line);
                }
            }
            if (aaseq.length() > 0) {
                sl.add(new Sequence(aaseq.toString(), fh.substring(1),AbstractRunConfig.DUMMYCONFIG));
            }            
        }
        
        
       // filterByIdentification(sl);
        sl.applyFilter(filter);
        return sl;
    }

    protected FastaFilter getAccessionsFilter() {
        if (csvIdentifications.getCSV() != null && !rbNoIdentification.isSelected()) {
            String accessionColumn = cbColumns.getModel().getSelectedItem().toString();
            HashSet<String> accessions = new HashSet<String>();
            int column = csvIdentifications.getCSV().getColumn(accessionColumn);
            for (int row = csvIdentifications.getCSV().getRowCount() -1; row>=0;row--) {
                String acc = csvIdentifications.getCSV().getValue(column, row);
                if (acc.contains(",")) {
                    String[] accs =acc.split(",");
                    for (int a=0;a<accs.length;a++) {
                        accessions.add(accs[a].trim());
                    }
                } else if (acc.contains(";")) {
                    String[] accs =acc.split(";");
                    for (int a=0;a<accs.length;a++) {
                        accessions.add(accs[a].trim());
                    }
                } else {
                    accessions.add(csvIdentifications.getCSV().getValue(column, row));
                }
            }
            FilterByID filter = new FilterByID(rbIncludeIdentifications.isSelected() ? FilterByID.filtermode.INCLUDE : FilterByID.filtermode.EXCLUDE );
            filter.addAllAccessions(accessions);
            return filter;
        } else {
            return new NoFilter();
        }
        
    }
    

    public void outputSequenceList(SequenceList sl) {
        StringBuffer out = new StringBuffer();
        for (Sequence s : sl) {
            out.append(s.toFasta(80));
            out.append("\n\n");
        }
        if (rbOutputSequence.isSelected()) {
            txtFastaOut.setText(out.toString());
        } else {
            try {
                PrintWriter pw = new PrintWriter(fbFastaSave.getFile());
                pw.append(out);
                pw.flush();
                pw.close();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgInputSelect = new javax.swing.ButtonGroup();
        bgOutputSelect = new javax.swing.ButtonGroup();
        bgIdentFilter = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        pIO = new javax.swing.JPanel();
        fbFastaLoad = new rappsilber.gui.components.FileBrowser();
        spFastaIn = new javax.swing.JScrollPane();
        txtFastaIn = new javax.swing.JTextArea();
        rbInputFile = new javax.swing.JRadioButton();
        rbInputSequence = new javax.swing.JRadioButton();
        spFastaOut = new javax.swing.JScrollPane();
        txtFastaOut = new javax.swing.JTextArea();
        fbFastaSave = new rappsilber.gui.components.FileBrowser();
        rbOutputFile = new javax.swing.JRadioButton();
        rbOutputSequence = new javax.swing.JRadioButton();
        btnRandomize = new javax.swing.JButton();
        btnReverseKR = new javax.swing.JButton();
        btnReverse = new javax.swing.JButton();
        btnRun = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        flFastaFiles = new rappsilber.gui.components.FileList();
        rbInputMultiFasta = new javax.swing.JRadioButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        cbColumns = new javax.swing.JComboBox();
        rbIncludeIdentifications = new javax.swing.JRadioButton();
        rbExcludeIdentifications = new javax.swing.JRadioButton();
        rbNoIdentification = new javax.swing.JRadioButton();
        csvIdentifications = new org.rappsilber.data.csv.gui.CSVFilteredPanel();
        jPanel3 = new javax.swing.JPanel();
        spCountProteins = new javax.swing.JSpinner();
        lblProteinCount = new javax.swing.JLabel();
        ckSize = new javax.swing.JCheckBox();
        spSizeMin = new javax.swing.JSpinner();
        lblMinSize = new javax.swing.JLabel();
        spSizeMax = new javax.swing.JSpinner();
        lblMaxSize = new javax.swing.JLabel();
        ckPreferedSize = new javax.swing.JCheckBox();
        spPreferedSize = new javax.swing.JSpinner();
        ckRandomSubset = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        fbFastaLoad.setDescription("Fasta-Files");
        fbFastaLoad.setExtensions(new String[] {"fasta", "txt", "fasta.gz", "txt.gz"});

        txtFastaIn.setColumns(20);
        txtFastaIn.setRows(5);
        spFastaIn.setViewportView(txtFastaIn);

        bgInputSelect.add(rbInputFile);
        rbInputFile.setText("Input");
        rbInputFile.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rbInputFileStateChanged(evt);
            }
        });

        bgInputSelect.add(rbInputSequence);
        rbInputSequence.setSelected(true);
        rbInputSequence.setText("Sequence");
        rbInputSequence.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rbInputSequenceStateChanged(evt);
            }
        });

        txtFastaOut.setColumns(20);
        txtFastaOut.setRows(5);
        spFastaOut.setViewportView(txtFastaOut);

        fbFastaSave.setDescription("Fasta");
        fbFastaSave.setExtensions(new String[] {".fasta"});

        bgOutputSelect.add(rbOutputFile);
        rbOutputFile.setText("Output File");
        rbOutputFile.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rbOutputFileStateChanged(evt);
            }
        });

        bgOutputSelect.add(rbOutputSequence);
        rbOutputSequence.setSelected(true);
        rbOutputSequence.setText("Output Sequence");
        rbOutputSequence.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rbOutputSequenceStateChanged(evt);
            }
        });

        btnRandomize.setText("Randomize");
        btnRandomize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRandomizeActionPerformed(evt);
            }
        });

        btnReverseKR.setText("Reverse (KR avare)");
        btnReverseKR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReverseKRActionPerformed(evt);
            }
        });

        btnReverse.setText("Reverse");
        btnReverse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReverseActionPerformed(evt);
            }
        });

        btnRun.setText("Run");
        btnRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRunActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pIOLayout = new javax.swing.GroupLayout(pIO);
        pIO.setLayout(pIOLayout);
        pIOLayout.setHorizontalGroup(
            pIOLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pIOLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pIOLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(spFastaOut, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 604, Short.MAX_VALUE)
                    .addComponent(spFastaIn, javax.swing.GroupLayout.DEFAULT_SIZE, 604, Short.MAX_VALUE)
                    .addComponent(rbInputSequence, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pIOLayout.createSequentialGroup()
                        .addComponent(rbInputFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fbFastaLoad, javax.swing.GroupLayout.DEFAULT_SIZE, 540, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pIOLayout.createSequentialGroup()
                        .addComponent(rbOutputFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(fbFastaSave, javax.swing.GroupLayout.DEFAULT_SIZE, 492, Short.MAX_VALUE))
                    .addGroup(pIOLayout.createSequentialGroup()
                        .addComponent(btnRun)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnReverse)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnReverseKR)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRandomize))
                    .addComponent(rbOutputSequence, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap())
        );
        pIOLayout.setVerticalGroup(
            pIOLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pIOLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pIOLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rbInputFile)
                    .addComponent(fbFastaLoad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addComponent(rbInputSequence)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(spFastaIn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pIOLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fbFastaSave, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rbOutputFile))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbOutputSequence)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spFastaOut, javax.swing.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pIOLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnRandomize)
                    .addComponent(btnReverseKR)
                    .addComponent(btnReverse)
                    .addComponent(btnRun))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Input/Output", pIO);

        flFastaFiles.setDescription("Fasta Files");
        flFastaFiles.setExtensions(new String[] {"txt", "fasta"});

        bgInputSelect.add(rbInputMultiFasta);
        rbInputMultiFasta.setText("Multiple Fasta Files");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rbInputMultiFasta)
                    .addComponent(flFastaFiles, javax.swing.GroupLayout.PREFERRED_SIZE, 516, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(rbInputMultiFasta)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(flFastaFiles, javax.swing.GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Multiple Fasta", jPanel1);

        jLabel1.setText("Accession Numbers");

        bgIdentFilter.add(rbIncludeIdentifications);
        rbIncludeIdentifications.setText("Include");

        bgIdentFilter.add(rbExcludeIdentifications);
        rbExcludeIdentifications.setText("Exclude");

        bgIdentFilter.add(rbNoIdentification);
        rbNoIdentification.setSelected(true);
        rbNoIdentification.setText("don't filter");

        csvIdentifications.setAutoCreateRowSorter(true);
        csvIdentifications.setAutoLoadFile(true);
        csvIdentifications.setEditable(true);
        csvIdentifications.setHasHeader(true);
        csvIdentifications.setShowSavePanel(false);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(rbNoIdentification)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(rbExcludeIdentifications)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(rbIncludeIdentifications))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbColumns, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(csvIdentifications, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rbExcludeIdentifications)
                    .addComponent(rbIncludeIdentifications)
                    .addComponent(rbNoIdentification))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(csvIdentifications, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(cbColumns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Filter by Identifications", jPanel2);

        spCountProteins.setEnabled(false);

        lblProteinCount.setText("Count");
        lblProteinCount.setEnabled(false);

        ckSize.setText("within size range");
        ckSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckSizeActionPerformed(evt);
            }
        });

        spSizeMin.setEnabled(false);

        lblMinSize.setText("min");
        lblMinSize.setEnabled(false);

        spSizeMax.setEnabled(false);

        lblMaxSize.setText("Max");
        lblMaxSize.setEnabled(false);

        ckPreferedSize.setText("prefered Size");
        ckPreferedSize.setEnabled(false);
        ckPreferedSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckPreferedSizeActionPerformed(evt);
            }
        });

        spPreferedSize.setEnabled(false);

        ckRandomSubset.setText("Random Subset");
        ckRandomSubset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckRandomSubsetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(ckSize)
                        .addGap(18, 18, 18)
                        .addComponent(lblMinSize)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spSizeMin, javax.swing.GroupLayout.DEFAULT_SIZE, 81, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(ckPreferedSize)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(ckRandomSubset)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblProteinCount)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(spCountProteins, javax.swing.GroupLayout.DEFAULT_SIZE, 85, Short.MAX_VALUE)
                            .addComponent(spPreferedSize))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblMaxSize)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spSizeMax, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(201, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ckSize)
                    .addComponent(lblMinSize)
                    .addComponent(spSizeMin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblMaxSize)
                    .addComponent(spSizeMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblProteinCount)
                    .addComponent(spCountProteins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ckRandomSubset))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ckPreferedSize)
                    .addComponent(spPreferedSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(348, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Subset", jPanel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnRandomizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRandomizeActionPerformed
        SequenceList decoy = new SequenceList(null);
        decoy.addAll(getSequences().includeShuffled());
        outputSequenceList(decoy);
        JOptionPane.showMessageDialog(rootPane, "Done");

        
    }//GEN-LAST:event_btnRandomizeActionPerformed

    private void btnReverseKRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReverseKRActionPerformed
        SequenceList decoy = new SequenceList(null);
        decoy.addAll(getSequences().includeReverseKRAvera());
        outputSequenceList(decoy);
        JOptionPane.showMessageDialog(rootPane, "Done");
        
        
    }//GEN-LAST:event_btnReverseKRActionPerformed

    private void btnReverseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReverseActionPerformed
        SequenceList decoy = new SequenceList(new AbstractRunConfig() {{setDecoyTreatment(SequenceList.DECOY_GENERATION.ISTARGET);}});
        decoy.addAll(getSequences().includeReverse());
        outputSequenceList(decoy);
        JOptionPane.showMessageDialog(rootPane, "Done");
    }//GEN-LAST:event_btnReverseActionPerformed

    private void btnRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRunActionPerformed
        SequenceList sequences = new SequenceList(AbstractRunConfig.DUMMYCONFIG);
        sequences.addAll(getSequences());
        outputSequenceList(sequences);
        JOptionPane.showMessageDialog(rootPane, "Done");
        
        
    }//GEN-LAST:event_btnRunActionPerformed

    private void rbInputSequenceStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rbInputSequenceStateChanged
        txtFastaIn.setEnabled(rbInputSequence.isSelected());
    }//GEN-LAST:event_rbInputSequenceStateChanged

    private void rbInputFileStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rbInputFileStateChanged
        fbFastaLoad.setEnabled(rbInputFile.isSelected());
    }//GEN-LAST:event_rbInputFileStateChanged

    private void rbOutputFileStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rbOutputFileStateChanged
        fbFastaSave.setEnabled(rbOutputFile.isSelected());
    }//GEN-LAST:event_rbOutputFileStateChanged

    private void rbOutputSequenceStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rbOutputSequenceStateChanged
        txtFastaOut.setEnabled(rbOutputSequence.isSelected());        
    }//GEN-LAST:event_rbOutputSequenceStateChanged

    private void ckSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckSizeActionPerformed
        lblMinSize.setEnabled(ckSize.isSelected());
        spSizeMin.setEnabled(ckSize.isSelected());
        lblMaxSize.setEnabled(ckSize.isSelected());
        spSizeMax.setEnabled(ckSize.isSelected());
        
        
    }//GEN-LAST:event_ckSizeActionPerformed

    private void ckPreferedSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckPreferedSizeActionPerformed
        spPreferedSize.setEnabled(ckPreferedSize.isSelected());
        
    }//GEN-LAST:event_ckPreferedSizeActionPerformed

    private void ckRandomSubsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckRandomSubsetActionPerformed
        spCountProteins.setEnabled(ckRandomSubset.isSelected());
        ckPreferedSize.setEnabled(ckRandomSubset.isSelected());
        ckPreferedSizeActionPerformed(evt);
    }//GEN-LAST:event_ckRandomSubsetActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(FastaTools.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(FastaTools.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(FastaTools.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(FastaTools.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new FastaTools().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgIdentFilter;
    private javax.swing.ButtonGroup bgInputSelect;
    private javax.swing.ButtonGroup bgOutputSelect;
    private javax.swing.JButton btnRandomize;
    private javax.swing.JButton btnReverse;
    private javax.swing.JButton btnReverseKR;
    private javax.swing.JButton btnRun;
    private javax.swing.JComboBox cbColumns;
    private javax.swing.JCheckBox ckPreferedSize;
    private javax.swing.JCheckBox ckRandomSubset;
    private javax.swing.JCheckBox ckSize;
    private org.rappsilber.data.csv.gui.CSVFilteredPanel csvIdentifications;
    private rappsilber.gui.components.FileBrowser fbFastaLoad;
    private rappsilber.gui.components.FileBrowser fbFastaSave;
    private rappsilber.gui.components.FileList flFastaFiles;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblMaxSize;
    private javax.swing.JLabel lblMinSize;
    private javax.swing.JLabel lblProteinCount;
    private javax.swing.JPanel pIO;
    private javax.swing.JRadioButton rbExcludeIdentifications;
    private javax.swing.JRadioButton rbIncludeIdentifications;
    private javax.swing.JRadioButton rbInputFile;
    private javax.swing.JRadioButton rbInputMultiFasta;
    private javax.swing.JRadioButton rbInputSequence;
    private javax.swing.JRadioButton rbNoIdentification;
    private javax.swing.JRadioButton rbOutputFile;
    private javax.swing.JRadioButton rbOutputSequence;
    private javax.swing.JSpinner spCountProteins;
    private javax.swing.JScrollPane spFastaIn;
    private javax.swing.JScrollPane spFastaOut;
    private javax.swing.JSpinner spPreferedSize;
    private javax.swing.JSpinner spSizeMax;
    private javax.swing.JSpinner spSizeMin;
    private javax.swing.JTextArea txtFastaIn;
    private javax.swing.JTextArea txtFastaOut;
    // End of variables declaration//GEN-END:variables
}
