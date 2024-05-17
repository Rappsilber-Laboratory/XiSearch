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

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import rappsilber.config.RunConfig;
import rappsilber.config.RunConfigFile;
import rappsilber.gui.components.GenericTextPopUpMenu;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.lookup.peptides.PeptideLookup;
import rappsilber.ms.lookup.peptides.PeptideTree;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.sequence.digest.Digestion;
import rappsilber.ui.StatusInterface;
import rappsilber.ui.TextBoxStatusInterface;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DigestSequence extends javax.swing.JFrame {

    /**
     * Creates new form DigestSequence
     */
    public DigestSequence() {
        initComponents();
        try {
            BufferedReader br = Util.readFromClassPath(".rappsilber.data.DefaultConfig.conf");
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();
            txtConfig.setText(sb.toString());

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        GenericTextPopUpMenu copyPaste = new GenericTextPopUpMenu();
        copyPaste.installContextMenu(this);

    }

    protected void startDigest(SequenceList sl, RunConfig conf, File out, JTextArea txtResult) {
        PeptideLookup plCros = new PeptideTree(new ToleranceUnit(0, "Da"));
        PeptideLookup plLinear = new PeptideTree(new ToleranceUnit(0, "Da"));
        Digestion d = conf.getDigestion_method();

        d.setPeptideLookup(new PeptideTree(conf.getPrecousorTolerance()), new PeptideTree(conf.getPrecousorTolerance()));
        d.setMaxMissCleavages(conf.getMaxMissCleavages());
        d.setPeptideLookup(plCros, plLinear);

        conf.getStatusInterface().setStatus("digest Sequences");

        for (Sequence s : sl) {
            //s.digest(conf.getDigestion_method() , Double.POSITIVE_INFINITY, conf.getCrossLinker());
            d.digest(s, conf.getCrossLinker());
            ArrayList<Peptide> peps = s.getPeptides();
        }
        conf.getStatusInterface().setStatus("apply variable modifications");
        plCros.applyVariableModifications(conf, plLinear);

        int countCrosslinkable = plCros.size();
        int countPeptide = countCrosslinkable + plLinear.size();

//        for (Peptide p : peps) {
//            if ( CrossLinker.canCrossLink(conf.getCrossLinker(), p))
//                countCrosslinkable++;
//        }
        conf.getStatusInterface().setStatus("writing out result");
        final StringBuilder sb = new StringBuilder();
        sb.append("Peptides : " + countPeptide + "\n"
                + "crosslinkable :" + countCrosslinkable
                + "\n===========================\n"
                + "Crosslinkable peptides\n-----------------------------\n"
                + "Peptide, Mass, length, ambiguity, proteins, positions\n");
        int c = 0;
        double total = plCros.size() + (ckCrossLinkeable.isSelected() ? 0 : plLinear.size());
        int report = Math.max((int) total / 1000, 10000);
        PrintWriter pw = null;
        boolean writeOut = out != null;
        if (writeOut)
            try {
            pw = new PrintWriter(out);
            pw.println("Peptide, Mass, length, ambiguity, proteins, positions");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DigestSequence.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (Peptide p : plCros) {
            Peptide.PeptidePositions[] pp = p.getPositions();
            StringBuffer ppsb = new StringBuffer();
            Arrays.stream(pp).forEach(cpp -> ppsb.append(cpp.base.getSplitFastaHeader().getAccession() + ";"));
            StringBuffer ppsb2 = new StringBuffer();
            Arrays.stream(pp).forEach(cpp -> ppsb2.append(cpp.start + ";"));

            String m = p.toString() + ", " + p.getMass() + "," + p.length() + "," + p.getPositions().length + "," + ppsb.substring(0, ppsb.length() - 1) + "," + ppsb2.substring(0, ppsb2.length() - 1) + "\n";
            if (txtResult != null) {
                sb.append(m);
            }
            if (writeOut) {
                pw.append(m);
            }
            if (c++ % report == 0) {
                conf.getStatusInterface().setStatus("writing " + (((int) (c * 1000 / total)) / 10.0) + "%");
            }
            p.free();
        }
        plCros = null;
        if (!ckCrossLinkeable.isSelected()) {
            sb.append("\n===========================\n"
                    + "Non-Crosslinkable peptides\n-----------------------------\n");
            for (Peptide p : plLinear) {
                if (c++ % report == 0) {
                    conf.getStatusInterface().setStatus("writing " + (((int) (c * 1000 / total)) / 10.0) + "%");
                }
                Peptide.PeptidePositions[] pp = p.getPositions();
                StringBuffer ppsb = new StringBuffer();
                Arrays.stream(pp).forEach(cpp -> ppsb.append(cpp.base.getSplitFastaHeader().getAccession() + ";"));
                StringBuffer ppsb2 = new StringBuffer();
                Arrays.stream(pp).forEach(cpp -> ppsb.append(cpp.start + ";"));
                String m = p.toString() + ", " + p.getMass() + "," + p.length() + "," + p.getPositions().length + "," + ppsb.substring(0, ppsb.length() - 1) + "\n";
                //String m = p.toString() + ", " + p.getMass() + "\n";
                if (txtResult != null) {
                    sb.append(m);
                }
                if (writeOut) {
                    pw.append(m);
                }
                p.free();
            }
        }
        plLinear = null;
        sl = null;
        if (writeOut) {
            pw.close();
        }
        final int batch = 10000000;
        if (txtResult != null) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    txtResult.setText("");
                    if (sb.length() > batch) {
                        while (sb.length() > 0) {
                            int sbatch = Math.min(sb.length(), batch);
                            txtResult.append(sb.substring(0, sbatch));
                            sb.delete(0, sbatch);
                        }
                    } else {
                        txtResult.setText(sb.toString());
                    }
                }
            });
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane4 = new javax.swing.JScrollPane();
        txtResult = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        fbConfig = new rappsilber.gui.components.FileBrowser();
        jButton1 = new javax.swing.JButton();
        spConfig = new javax.swing.JScrollPane();
        txtConfig = new javax.swing.JTextArea();
        pSequence = new javax.swing.JPanel();
        lblSequence = new javax.swing.JLabel();
        btnDigest = new javax.swing.JButton();
        ckCrossLinkeable = new javax.swing.JCheckBox();
        ckK2R = new javax.swing.JCheckBox();
        ckCounts = new javax.swing.JCheckBox();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        spSequence = new javax.swing.JScrollPane();
        txtSequence = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        fileBrowser1 = new rappsilber.gui.components.FileBrowser();
        txtStatus = new javax.swing.JTextField();
        fbSaveList = new rappsilber.gui.components.FileBrowser();
        btnSave = new javax.swing.JButton();
        memory2 = new org.rappsilber.gui.components.memory.Memory();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jSplitPane1.setDividerLocation(200);

        txtResult.setColumns(20);
        txtResult.setRows(5);
        jScrollPane4.setViewportView(txtResult);

        jSplitPane1.setLeftComponent(jScrollPane4);

        fbConfig.setDescription("Config");
        fbConfig.setExtensions(new String[] {"txt", "conf", "config"});

        jButton1.setText("Load Config");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        txtConfig.setColumns(20);
        txtConfig.setRows(5);
        txtConfig.setText("#################\n## Cross Linker\n##   Modifications are generated as Variable modifications, with the naming convention:\n##   LinkedAminoAcidSymbol followed by name of crosslinker in lower case and name of modification\n##BS3\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS3;MASS:138.06807;LINKEDAMINOACIDS:K,S,T,Y,nterm;MODIFICATIONS:NH2,17.026549105,OH,18.0105647,LOOP,0\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS3;MASS:138.06807;LINKEDAMINOACIDS:K,nterm;MODIFICATIONS:NH2,17.026549105,OH,18.0105647,LOOP,0\ncrosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS3;MASS:138.06807;LINKEDAMINOACIDS:K,nterm\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS3_decoy;MASS:100.06807;LINKEDAMINOACIDS:K,nterm;decoy\n##BS2G\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS2G;MASS:96.02112055;LINKEDAMINOACIDS:K,S,T,Y,nterm;MODIFICATIONS:NH2,17.026549105,OH,18.0105647,LOOP,0\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS2G;MASS:96.02112055;LINKEDAMINOACIDS:K,S,T,Y,nterm\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS2G_decoy;MASS:66.02112055;LINKEDAMINOACIDS:K,S,T,Y,nterm;decoy\n\n#crosslinker:DummyCrosslinker:\n\n###################\n## Digest\n##Tryptic digest\ndigestion:PostAAConstrainedDigestion:DIGESTED:K,R;ConstrainingAminoAcids:P;NAME=Trypsin\n#digestion:PostAAConstrainedDigestion:DIGESTED:K,R;ConstrainingAminoAcids:;NAME=Trypsin\\P\n#digestion:PostAAConstrainedDigestion:DIGESTED:K;ConstrainingAminoAcids:P;NAME=LysC\n#digestion:PostAAConstrainedDigestion:DIGESTED:K;ConstrainingAminoAcids:;NAME=LysC\\P\n##No Digestion e.g. for Synthetic Peptide\n#digestion:NoDigestion:\n\n\n\n####################\n##Tolerances\ntolerance:precursor:6ppm\ntolerance:fragment:20ppm\n\n\nmissedcleavages:3\n\n\n\n");
        spConfig.setViewportView(txtConfig);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(fbConfig, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1))
            .addComponent(spConfig, javax.swing.GroupLayout.DEFAULT_SIZE, 688, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(spConfig, javax.swing.GroupLayout.DEFAULT_SIZE, 149, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fbConfig, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(jPanel2);

        lblSequence.setText("Sequence");

        btnDigest.setText("Digest");
        btnDigest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDigestActionPerformed(evt);
            }
        });

        ckCrossLinkeable.setText("Cross-linkable");

        ckK2R.setText("K->R");

        ckCounts.setText("CountsOnly");

        txtSequence.setColumns(20);
        txtSequence.setRows(5);
        spSequence.setViewportView(txtSequence);

        jTabbedPane1.addTab("copy&paste", spSequence);

        fileBrowser1.setDescription("FASTA-Files");
        fileBrowser1.setExtensions(new String[] {"fasta", "fasta.gz", "txt"});

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fileBrowser1, javax.swing.GroupLayout.DEFAULT_SIZE, 593, Short.MAX_VALUE)
                .addGap(31, 31, 31))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fileBrowser1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(33, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("fasta-file", jPanel1);

        javax.swing.GroupLayout pSequenceLayout = new javax.swing.GroupLayout(pSequence);
        pSequence.setLayout(pSequenceLayout);
        pSequenceLayout.setHorizontalGroup(
            pSequenceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pSequenceLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pSequenceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pSequenceLayout.createSequentialGroup()
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 641, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(pSequenceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ckK2R)
                            .addComponent(ckCrossLinkeable)
                            .addGroup(pSequenceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(btnDigest, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(ckCounts, javax.swing.GroupLayout.Alignment.TRAILING))))
                    .addComponent(lblSequence))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pSequenceLayout.setVerticalGroup(
            pSequenceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pSequenceLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblSequence, javax.swing.GroupLayout.DEFAULT_SIZE, 16, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pSequenceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(pSequenceLayout.createSequentialGroup()
                        .addComponent(ckCrossLinkeable)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ckK2R)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(ckCounts)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnDigest)))
                .addGap(89, 89, 89))
        );

        txtStatus.setEditable(false);

        fbSaveList.setDescription("csv-files");
        fbSaveList.setExtensions(new String[] {"csv"});

        btnSave.setText("save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pSequence, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(txtStatus)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(memory2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addComponent(fbSaveList, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSave))
            .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pSequence, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(fbSaveList, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSave))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(memory2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnDigestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDigestActionPerformed
        RunConfig fconf = null;

        try {
            fconf = new RunConfigFile(new StringReader(txtConfig.getText()));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DigestSequence.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DigestSequence.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(DigestSequence.class.getName()).log(Level.SEVERE, null, ex);
        }
        fconf.addStatusInterface(new TextBoxStatusInterface(txtStatus));
        final RunConfig conf = fconf;

        final String sSeq = txtSequence.getText();

        Runnable runnable = new Runnable() {
            public void run() {
                conf.getStatusInterface().setStatus("Reading Sequences");
                SequenceList sl = null;
                if (sSeq.trim().length() > 0) {
                    if (sSeq.trim().startsWith(">")) {
                        BufferedReader bf = new BufferedReader(new StringReader(txtSequence.getText()));
                        try {
                            sl = new SequenceList(SequenceList.DECOY_GENERATION.ISTARGET, bf, conf, "");
                        } catch (IOException ex) {
                            Logger.getLogger(DigestSequence.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        Sequence s = new Sequence(txtSequence.getText(), conf);
                        sl = new SequenceList(conf);
                        sl.add(s);
                    }
                }
                if (fileBrowser1.getFile() != null) {
                    try {
                        SequenceList fasta = new SequenceList(new File[]{fileBrowser1.getFile()}, conf);
                        if (sl == null) {
                            sl = fasta;
                        } else {
                            sl.addAll(fasta);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(DigestSequence.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                File out = fbSaveList.getFile();
                startDigest(sl, conf, out, txtResult);
                conf.getStatusInterface().setStatus("Finished");
            }

        };

        Thread dig = new Thread(runnable);
        dig.setName("digest");
        dig.start();;

    }//GEN-LAST:event_btnDigestActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        PrintWriter pw = null;
        try {
            File out = fbSaveList.getFile();
            if (out == null) {
                JOptionPane.showMessageDialog(this, "No file selected", "No file selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            pw = new PrintWriter(out);
            pw.append(txtResult.getText());
            pw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DigestSequence.class.getName()).log(Level.SEVERE, "File not found", ex);
            txtStatus.setText("File not found error");
        } finally {
            pw.close();
        }
    }//GEN-LAST:event_btnSaveActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        try {
            txtConfig.setText(new String(Files.readAllBytes(fbConfig.getFile().toPath()), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            Logger.getLogger(DigestSequence.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    String getKRVersion(Peptide p, Peptide Orig) {
        String ret = "";
        ArrayList<Peptide> peps = new ArrayList<Peptide>();
        HashSet<AminoAcid> K = new HashSet<AminoAcid>(1);
        K.add(AminoAcid.K);
        int tk = p.countAminoAcid(K);
        if (tk > 1) {
            boolean found = false;
            for (int i = 0; i < p.length(); i++) {
                if (p.aminoAcidAt(i) == AminoAcid.K) {
                    Peptide p2 = p.clone();
                    p2.replace(AminoAcid.K, AminoAcid.R);
                    ret += getKRVersion(p2, Orig);
                }
            }
        }
        return ret + "\n> " + Orig.toString() + "\n" + p;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        if (args.length == 0) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    new DigestSequence().setVisible(true);
                }
            });
        } else {
            RunConfigFile fconf = null;
            try {
                fconf = new RunConfigFile(new File(args[0]));
                fconf.addStatusInterface(new StatusInterface() {
                    String status;

                    @Override
                    public void setStatus(String status) {
                        this.status = status;
                        System.err.println(status);
                    }

                    @Override
                    public String getStatus() {
                        return status;
                    }
                });

                SequenceList sl = null;
                SequenceList fasta = new SequenceList(new File[]{new File(args[1])}, fconf);
                if (sl == null) {
                    sl = fasta;
                } else {
                    sl.addAll(fasta);
                }
                File out = new File(args[2]);
                new DigestSequence().startDigest(sl, fconf, out, null);
                fconf.getStatusInterface().setStatus("Finished");
            } catch (IOException|ParseException ex) {
                if (fconf !=null) 
                    fconf.getStatusInterface().setStatus(ex.toString());
                Logger.getLogger(DigestSequence.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }

        }

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDigest;
    private javax.swing.JButton btnSave;
    private javax.swing.JCheckBox ckCounts;
    private javax.swing.JCheckBox ckCrossLinkeable;
    private javax.swing.JCheckBox ckK2R;
    private rappsilber.gui.components.FileBrowser fbConfig;
    private rappsilber.gui.components.FileBrowser fbSaveList;
    private rappsilber.gui.components.FileBrowser fileBrowser1;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblSequence;
    private org.rappsilber.gui.components.memory.Memory memory2;
    private javax.swing.JPanel pSequence;
    private javax.swing.JScrollPane spConfig;
    private javax.swing.JScrollPane spSequence;
    private javax.swing.JTextArea txtConfig;
    private javax.swing.JTextArea txtResult;
    private javax.swing.JTextArea txtSequence;
    private javax.swing.JTextField txtStatus;
    // End of variables declaration//GEN-END:variables

}
