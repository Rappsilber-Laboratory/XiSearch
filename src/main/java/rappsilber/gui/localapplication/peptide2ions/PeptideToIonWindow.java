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
package rappsilber.gui.localapplication.peptide2ions;

import java.beans.Visibility;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.crosslinker.SymetricSingleAminoAcidRestrictedCrossLinker;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.BIon;
import rappsilber.ms.sequence.ions.BLikeDoubleFragmentation;
import rappsilber.ms.sequence.ions.CrossLinkedFragmentProducer;
import rappsilber.ms.sequence.ions.CrosslinkedFragment;
import rappsilber.ms.sequence.ions.CrosslinkerContaining;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.PeptideIon;
import rappsilber.ms.sequence.ions.YIon;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.utils.AvergineUtil;
import rappsilber.utils.Util;

/**
 * small GUI to calculate the possible fragments (including cross-linked ones)
 * and their masses.<br/>
 * Instead of the neutral mass, it can also calculate the m/z-value for a given charge
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class PeptideToIonWindow extends javax.swing.JFrame {
    private static final long serialVersionUID = -8233783078499041646L;
    CrossLinker cl;

    /**
     * creates a list of possible fragments
     * @param peptide1String    sequence of peptide one
     * @param peptide2String    sequence of peptide two
     * @param cl                cross linker
     * @param strLink1          linkage-site one
     * @param strLink2          linkage-site two
     * @param strCharge         charge state
     * @return
     */
    protected static String getFragments(String peptide1String, String peptide2String, CrossLinker cl, String strLink1, String strLink2, String strCharge, RunConfig conf, boolean isotopes) {
        try {
            
            Sequence s1 = new Sequence(peptide1String, conf);
            Sequence s2 = new Sequence(peptide2String, conf);
            Peptide p1 = new Peptide(s1, 0, s1.length());
            Peptide p2 = new Peptide(s2, 0, s2.length());
            int link1 = Integer.parseInt(strLink1) - 1;
            int link2 = Integer.parseInt(strLink2) - 1;
            int charge = Integer.parseInt(strCharge);
            System.out.println(p1.toString() + " x " + p2.toString() + "\n");
            ArrayList<Fragment> frags1 = p1.getPrimaryFragments(conf);
            ArrayList<Fragment> frags2 = p2.getPrimaryFragments(conf);
            
            for (CrossLinkedFragmentProducer clp : conf.getCrossLinkedFragmentProducers()) {
                frags1.addAll(clp.createCrosslinkedFragments(frags1, frags2, conf.getCrossLinker().get(0), link1, link2));
            }
//            frags1.addAll(conf..createCrosslinkedFragments(frags1, frags2, conf.getCrossLinker().get(0), link1, link2));

            Loss.includeLosses(frags1, conf.getCrossLinker().get(0), true, conf);
            Loss.includeLosses(frags2, conf.getCrossLinker().get(0), true, conf);

            //frags1.addAll(CrosslinkedFragment.createCrosslinkedFragments(frags1, p2, cl, link1));
            if (charge >0) {
                
            }
            StringBuilder sb = new StringBuilder();
            if (charge == 0) {
                sb.append("ion, sequence, peptide, mass\n");
            } else if (isotopes) {
                sb.append("ion ,charge, sequence, peptide, m/z, intensity, isotope\n");
            } else {
                sb.append("ion ,charge, sequence, peptide, m/z\n");
            }
            if (peptide1String.length() > 0)
                fragsToString(frags1, link1, sb, charge,isotopes);
            if (peptide2String.length() > 0)
                fragsToString(frags2, link2, sb, charge,isotopes);
            return sb.toString();

        } catch(Exception e) {
            JOptionPane.showConfirmDialog(null, e);
            e.printStackTrace();
            return e.toString() + e.getMessage();
        }
    }

    protected static void fragsToString(ArrayList<Fragment> frags1, int link1, StringBuilder sb, int charge, boolean isotopes) {
        for (Fragment f : frags1) {
            
            if (f.isClass(CrosslinkerContaining.class) || (f.getStart() < link1 && f.getEnd() < link1) || (f.getStart() > link1)) {
                if (charge == 0)
                    sb.append(f.name() + ", " + f.toString()+ ", " + f.getPeptide() + ", " + f.getNeutralMass() + "\n");
                for (int c = 1; c<= charge; c++) {
                    sb.append(f.name() + ", z" +c + " , " + f.toString()+ ", " + f.getPeptide() + ",  " + f.getMZ(c));
                    if (isotopes) {
                        double intensity = getIntensity(f,0);
                        sb.append("," + intensity + ", 0 \n");
                        for (int i = 1;i<6; i++) {
                            intensity = getIntensity(f,i);
                            if (intensity>0.1)
                                sb.append(f.name() + ", z" +c  + ", " + f.toString()+ ", " + f.getPeptide() + ", " + (f.getMZ(c) + Util.C13_MASS_DIFFERENCE*i/c) + "," + intensity + "," +i + "\n" );
                        }
                    } else {
                        sb.append("\n");
                    }
                }
            }
            
        }
    }

    protected static double getIntensity(Fragment f, int isotope) {
        double maxintensity = AvergineUtil.relativeHight(f.getMass(),0);
        for (int i=1;i<6;i++) {
            maxintensity  =Math.max(maxintensity, AvergineUtil.relativeHight(f.getMass(),i));
        }
        double intensity = AvergineUtil.relativeHight(f.getMass(),isotope)/maxintensity*100;
        if (f.isClass(BIon.class))
            intensity/=1.5;
        if (f.isClass(Loss.class))
            intensity/=2.0;
        return intensity;
    }

    /** Creates new form PeptideToIonWindow */
    public PeptideToIonWindow() {
        initComponents();
//        BIon.register();
//        YIon.register();
//        PeptideIon.register();
//
//        BLikeDoubleFragmentation.register();
//        AminoModification.Mox.registerVariable();
//        HashSet<AminoAcid> linkedResidues = new HashSet<AminoAcid>();
//        linkedResidues.add(AminoAcid.K);
//        cl = new SymetricSingleAminoAcidRestrictedCrossLinker("BS3", 138.06807, 138.06807, linkedResidues);
        setVisible(true);
        GenericTextPopUpMenu copyPaste = new GenericTextPopUpMenu();
        copyPaste.installContextMenu(this);
        
    }

    /** This method is called from within the constructor to
     * initialise the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtPep1 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        txtPep2 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        txtLinker1 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        txtLinker2 = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txtCharge = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        ckIsotopes = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtConfig = new javax.swing.JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtOut = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Peptide1:");

        txtPep1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPep1ActionPerformed(evt);
            }
        });

        jLabel2.setText("Peptide2 :");

        txtPep2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPep2ActionPerformed(evt);
            }
        });

        jLabel3.setText("Linker1:");

        txtLinker1.setText("0");
        txtLinker1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtLinker1ActionPerformed(evt);
            }
        });

        jLabel4.setText("Linker2:");

        txtLinker2.setText("0");
        txtLinker2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtLinker2ActionPerformed(evt);
            }
        });

        jLabel5.setText("charge:");

        txtCharge.setText("0");
        txtCharge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtChargeActionPerformed(evt);
            }
        });

        jButton1.setText("Calc");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        ckIsotopes.setText("Isotopes");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtPep2)
                    .addComponent(txtPep1, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtLinker1, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtLinker2, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ckIsotopes)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtCharge, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel1)
                                    .addComponent(txtPep1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel2)
                                    .addComponent(txtPep2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel3)
                                    .addComponent(txtLinker1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(ckIsotopes))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel5)
                                        .addComponent(txtCharge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel4)
                                        .addComponent(txtLinker2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addComponent(jButton1)))
                .addContainerGap())
        );

        jSplitPane1.setDividerLocation(400);

        txtConfig.setColumns(20);
        txtConfig.setRows(5);
        txtConfig.setText("#################\n## Cross Linker\n##   Modifications are generated as Variable modifications, with the naming convention:\n##   LinkedAminoAcidSymbol followed by name of crosslinker in lower case and name of modification\n##BS3\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS3;MASS:138.06807;LINKEDAMINOACIDS:K,S,T,Y,nterm;MODIFICATIONS:NH2,16.01872407,OH,17.002739665\ncrosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS3;MASS:138.06807;LINKEDAMINOACIDS:K,nterm;MODIFICATIONS:NH2,16.01872407,OH,17.002739665\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS3;MASS:138.06807;LINKEDAMINOACIDS:K,nterm\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS3_decoy;MASS:100.06807;LINKEDAMINOACIDS:K,nterm;decoy\n##BS2G\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS2G;MASS:96.02112055;LINKEDAMINOACIDS:K,S,T,Y,nterm\n#crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS2G_decoy;MASS:66.02112055;LINKEDAMINOACIDS:K,S,T,Y,nterm;decoy\n\n###################\n## Digest\n##Tryptic digest\n#digestion:PostAAConstrainedDigestion:DIGESTED:K,R;ConstrainingAminoAcids:P;NAME=Trypsin\ndigestion:PostAAConstrainedDigestion:DIGESTED:K,R;ConstrainingAminoAcids:;NAME=Trypsin\n##No Digestion e.g. for Synthetic Peptide\n#digestion:NoDigestion:\n\n###################\n##Modifications\n##--Variable Modifications\n##Mox = 131.040485 + 15.99491\nmodification:variable::SYMBOL:Mox;MODIFIED:M;MASS:147.035395\n\n##--Fixed Modifications\nmodification:fixed::SYMBOL:Ccm;MODIFIED:C;MASS:160.03065\n\n\n###################\n##Losses\n## Water\nloss:AminoAcidRestrictedLoss:NAME:H20;aminoacids:S,T,D,E;MASS:18.01056027;cterm;ID:1\n## Amonia\nloss:AminoAcidRestrictedLoss:NAME:NH3;aminoacids:R,K,N,Q;MASS:17.02654493;nterm;ID:2\n## SO2 from Mox\nloss:AminoAcidRestrictedLoss:NAME:SO2;aminoacids:Mox;MASS:63.96189;ID:3\n## CH3SOH from Mox\n## can be ignored, since it will in most cases match to the same peak as SO2 loss\n#loss:AminoAcidRestrictedLoss:NAME:CH3SOH;aminoacids:Mox;MASS:63.99828547;ID:4\n##AIons as loss from BIons\nloss:AIonLoss;ID:5\n#crosslinker modified fragment\n#loss:CrosslinkerModified;ID:6\n\n\n\n####################\n##Tolerances\ntolerance:precursor:6ppm\ntolerance:fragment:20ppm\n\n####################\n## Non-Lossy Fragments to consider\nfragment:BIon;ID:1\nfragment:YIon;ID:2\nfragment:PeptideIon;ID:3\n#fragment:BLikeDoubleFragmentation;ID:4\n\n####################\n## isotop annotation\nIsotopPattern:Averagin\n\n#####################\n## include linear fragments\nEVALUATELINEARS:true\n\n\n####################\n## how many peaks to consider for mgc-search\nmgcpeaks:200\ntopmgchits:150\ntopmgxhits:10\n\nmissedcleavages:4\n\n\n\n\n#####################\n## IO-settings\nBufferInput:100\nBufferOutput:100\n\n#####################\n## how many cpus to use\nUseCPUs:-1\n\n\n#####################\n## -- statistics\nstatistic:/home/lfischer/Projects/Xlink/test/statistic/IntensityStatistic.csv\n\n\n#####################\n##\nenableIndicatorPeaks:false\n\nAUTOMATICEVALUATIONSCORE:J48ModeledManual001;1\n\nConservativeLosses:4\n\nTOPMATCHESONLY:true\n\n");
        jScrollPane2.setViewportView(txtConfig);

        jSplitPane1.setRightComponent(jScrollPane2);

        txtOut.setColumns(20);
        txtOut.setRows(5);
        jScrollPane1.setViewportView(txtOut);

        jSplitPane1.setLeftComponent(jScrollPane1);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 806, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 336, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtPep1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPep1ActionPerformed

    }//GEN-LAST:event_txtPep1ActionPerformed

    private void txtPep2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPep2ActionPerformed

    }//GEN-LAST:event_txtPep2ActionPerformed

    private void txtLinker1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtLinker1ActionPerformed

    }//GEN-LAST:event_txtLinker1ActionPerformed

    private void txtLinker2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtLinker2ActionPerformed

    }//GEN-LAST:event_txtLinker2ActionPerformed

    private void txtChargeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtChargeActionPerformed
        int c = 0;
        try{
            c = Integer.parseInt(txtCharge.getText().trim());
            txtCharge.setText(""+c);
        } catch(Exception e){
            txtCharge.setText("0");
        }
        
        if (c == 0) {
            ckIsotopes.setEnabled(false);
        } else {
            ckIsotopes.setEnabled(false);
        }
    }//GEN-LAST:event_txtChargeActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        RunConfig conf = null;
        try {
            conf = new RunConfigFile(new StringReader(txtConfig.getText()));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PeptideToIonWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PeptideToIonWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(PeptideToIonWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        txtOut.setText(getFragments(txtPep1.getText(), txtPep2.getText(), cl, txtLinker1.getText(), txtLinker2.getText(), txtCharge.getText(), conf,ckIsotopes.isSelected()));
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PeptideToIonWindow().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox ckIsotopes;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTextField txtCharge;
    private javax.swing.JTextArea txtConfig;
    private javax.swing.JTextField txtLinker1;
    private javax.swing.JTextField txtLinker2;
    private javax.swing.JTextArea txtOut;
    private javax.swing.JTextField txtPep1;
    private javax.swing.JTextField txtPep2;
    // End of variables declaration//GEN-END:variables

}
