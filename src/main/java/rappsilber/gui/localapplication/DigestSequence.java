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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
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
import rappsilber.ui.TextBoxStatusInterface;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DigestSequence extends javax.swing.JFrame {

    /** Creates new form DigestSequence */
    public DigestSequence() {
        initComponents();
        try {
            BufferedReader br = Util.readFromClassPath(".rappsilber.data.DefaultConfig.conf");
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line + "\n");
            br.close();
            txtConfig.setText(sb.toString());

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        GenericTextPopUpMenu copyPaste = new GenericTextPopUpMenu();
        copyPaste.installContextMenu(this);

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        spConfig = new javax.swing.JScrollPane();
        txtConfig = new javax.swing.JTextArea();
        jScrollPane4 = new javax.swing.JScrollPane();
        txtResult = new javax.swing.JTextArea();
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

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jSplitPane1.setDividerLocation(200);

        txtConfig.setColumns(20);
        txtConfig.setRows(5);
        txtConfig.setText("#################\n## Cross Linker\n##   Modifications are generated as Variable modifications, with the naming convention:\n##   LinkedAminoAcidSymbol followed by name of crosslinker in lower case and name of modification\n##BS3\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS3;MASS:138.06807;LINKEDAMINOACIDS:K,S,T,Y,nterm;MODIFICATIONS:NH2,17.026549105,OH,18.0105647,LOOP,0\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS3;MASS:138.06807;LINKEDAMINOACIDS:K,nterm;MODIFICATIONS:NH2,17.026549105,OH,18.0105647,LOOP,0\ncrosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS3;MASS:138.06807;LINKEDAMINOACIDS:K,nterm\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS3_decoy;MASS:100.06807;LINKEDAMINOACIDS:K,nterm;decoy\n##BS2G\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS2G;MASS:96.02112055;LINKEDAMINOACIDS:K,S,T,Y,nterm;MODIFICATIONS:NH2,17.026549105,OH,18.0105647,LOOP,0\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS2G;MASS:96.02112055;LINKEDAMINOACIDS:K,S,T,Y,nterm\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS2G_decoy;MASS:66.02112055;LINKEDAMINOACIDS:K,S,T,Y,nterm;decoy\n\n#crosslinker:DummyCrosslinker:\n\n###################\n## Digest\n##Tryptic digest\ndigestion:PostAAConstrainedDigestion:DIGESTED:K,R;ConstrainingAminoAcids:P;NAME=Trypsin\n#digestion:PostAAConstrainedDigestion:DIGESTED:K,R;ConstrainingAminoAcids:;NAME=Trypsin\\P\n#digestion:PostAAConstrainedDigestion:DIGESTED:K;ConstrainingAminoAcids:P;NAME=LysC\n#digestion:PostAAConstrainedDigestion:DIGESTED:K;ConstrainingAminoAcids:;NAME=LysC\\P\n##No Digestion e.g. for Synthetic Peptide\n#digestion:NoDigestion:\n\n\n\n####################\n##Tolerances\ntolerance:precursor:6ppm\ntolerance:fragment:20ppm\n\n\nmissedcleavages:3\n\n\n\n");
        spConfig.setViewportView(txtConfig);

        jSplitPane1.setRightComponent(spConfig);

        txtResult.setColumns(20);
        txtResult.setRows(5);
        jScrollPane4.setViewportView(txtResult);

        jSplitPane1.setLeftComponent(jScrollPane4);

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

        fileBrowser1.setExtensions(new String[] {"fasta", "txt"});

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fileBrowser1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                        .addGroup(pSequenceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pSequenceLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 77, Short.MAX_VALUE)
                                .addComponent(btnDigest))
                            .addGroup(pSequenceLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addGroup(pSequenceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(ckK2R)
                                    .addComponent(ckCounts)
                                    .addComponent(ckCrossLinkeable)))))
                    .addGroup(pSequenceLayout.createSequentialGroup()
                        .addComponent(lblSequence)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pSequenceLayout.setVerticalGroup(
            pSequenceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pSequenceLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblSequence, javax.swing.GroupLayout.DEFAULT_SIZE, 16, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pSequenceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pSequenceLayout.createSequentialGroup()
                        .addComponent(ckCrossLinkeable)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ckK2R)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ckCounts)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnDigest))
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9))
        );

        txtStatus.setEditable(false);

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
            .addComponent(jSplitPane1)
            .addComponent(pSequence, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(txtStatus)
            .addGroup(layout.createSequentialGroup()
                .addComponent(fbSaveList, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSave))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pSequence, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 271, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(fbSaveList, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSave))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnDigestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDigestActionPerformed
        RunConfig fconf = null;
        final PeptideLookup plCros = new PeptideTree(new ToleranceUnit(0,"Da"));
        final PeptideLookup plLinear = new PeptideTree(new ToleranceUnit(0,"Da"));

        

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

        final Digestion d = fconf.getDigestion_method();
                
        d.setPeptideLookup(new PeptideTree(fconf.getPrecousorTolerance()) , new PeptideTree(conf.getPrecousorTolerance()));
        d.setMaxMissCleavages(fconf.getMaxMissCleavages());
        d.setPeptideLookup(plCros, plLinear);
        
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
                txtResult.setText("Peptides : " + countPeptide + "\n"
                        + "crosslinkable :" + countCrosslinkable
                        + "\n===========================\n"
                        + "Crosslinkable peptides\n-----------------------------\n");
                
                for (Peptide p : plCros) {
                    txtResult.append(p.toString() + ", " + p.getMass() + "\n");
                }
                if (!ckCrossLinkeable.isSelected()) {
                    txtResult.append("\n===========================\n"
                            + "Non-Crosslinkable peptides\n-----------------------------\n");
                    for (Peptide p : plLinear) {
                        txtResult.append(p.toString() + ", " + p.getMass() + "\n");
                    }
                }
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
            }   pw = new PrintWriter(out);
            pw.append(txtResult.getText());
            pw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DigestSequence.class.getName()).log(Level.SEVERE, "File not found", ex);
            txtStatus.setText("File not found error");
        } finally {
            pw.close();
        }
    }//GEN-LAST:event_btnSaveActionPerformed

    
    String getKRVersion(Peptide p, Peptide Orig) {
        String ret  = "";
        ArrayList<Peptide> peps = new ArrayList<Peptide>();
        HashSet<AminoAcid> K = new HashSet<AminoAcid>(1);
        K.add(AminoAcid.K);
        int tk = p.countAminoAcid(K);
        if (tk >1) {
            boolean found = false;
            for (int i = 0; i < p.length();i++) {
                if(p.aminoAcidAt(i) == AminoAcid.K)  {
                    Peptide p2 = p.clone();
                    p2.replace(AminoAcid.K,AminoAcid.R);
                    ret += getKRVersion(p2,Orig);
                }
            }
        }
        return ret + "\n> " + Orig.toString() + "\n" + p;
    }
    
    
    
    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new DigestSequence().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDigest;
    private javax.swing.JButton btnSave;
    private javax.swing.JCheckBox ckCounts;
    private javax.swing.JCheckBox ckCrossLinkeable;
    private javax.swing.JCheckBox ckK2R;
    private rappsilber.gui.components.FileBrowser fbSaveList;
    private rappsilber.gui.components.FileBrowser fileBrowser1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblSequence;
    private javax.swing.JPanel pSequence;
    private javax.swing.JScrollPane spConfig;
    private javax.swing.JScrollPane spSequence;
    private javax.swing.JTextArea txtConfig;
    private javax.swing.JTextArea txtResult;
    private javax.swing.JTextArea txtSequence;
    private javax.swing.JTextField txtStatus;
    // End of variables declaration//GEN-END:variables

}