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
package rappsilber.gui.pinpoint;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import rappsilber.config.AbstractRunConfig;
import rappsilber.config.DBRunConfig;
import rappsilber.db.ConnectionPool;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.dataAccess.msm.MSMListIterator;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoLabel;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.spectra.Spectra;
import rappsilber.utils.MyArrayUtils;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DB2PinPoint extends javax.swing.JFrame {

    protected String generateID(String id, String prot1acc, String prot2acc, int pepLink1, int pepLink2, int pepSite1, int pepSite2, String seq1, String seq2, String prot1name, String prot2name, String prot1desc, String prot2desc, String run) {
        id=id.replace("%A1", prot1acc)
                .replace("%A2", prot2acc)
                .replace("%S1", ""+(pepLink1+1))
                .replace("%S2", ""+(pepLink2+1))
                .replace("%L1", ""+(pepLink1+pepSite1+1))
                .replace("%L2", ""+(pepLink2+pepSite2+1))
                .replace("%P1", seq1)
                .replace("%P2", seq2)
                .replace("%N1", prot1name)
                .replace("%N2", prot2name)
                .replace("%D1", prot1desc)
                .replace("%D2", prot2desc)
                .replace("%R", run)
                ;
        return id;
    }
    
    
    public class RetentionTime extends HashMap<String, HashMap<Integer, Double>> {
        public void add(Spectra s) {
            String runname = s.getRun().toLowerCase();
            HashMap<Integer, Double> run = get(runname);
            if (run == null) {
                run = new HashMap<Integer, Double>();
                super.put(runname, run);
                if (runname.endsWith(".raw")) {
                    super.put(runname.substring(0,runname.length()-4), run);
                } else {
                    super.put(runname + ".raw", run);
                }
            }
            double ets =s.getElutionTimeStart();
            double ete = s.getElutionTimeEnd();
            if (ets >0 && ete >0) {
                ets=ets + ete /2;
            } else if (ete > 0)
                ets = ete;
            run.put(s.getScanNumber(), ets);
        }
        
        public Double get(String runname, int scan, double defaultRT) {
            HashMap<Integer, Double> run = get(runname.toLowerCase());
            if (run == null)
                return defaultRT;
            Double ret = run.get(scan);
            if (ret == null)
                return defaultRT;
            return ret;
        }
        
    }

    
    public void setStatus(final String text) {
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                txtStatus.setText(text);
            }
        });
    }
    
    
    
    /**
     * Creates new form DB2PinPoint
     */
    public DB2PinPoint() {
        initComponents();
        getSearch.addStatusListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setStatus(e.getActionCommand());
            }
        });
        cbLabelActionPerformed(null);
        //getSearch.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
    
    
    
    
    protected void readInRetentionTimes(RetentionTime rt) throws IOException, FileNotFoundException, ParseException {
        MSMListIterator iter;
        ToleranceUnit t = new ToleranceUnit("100ppm");
        iter = new MSMListIterator(t, 0, new AbstractRunConfig() {
        });
        File[] list = flPeakList.getFiles();
        if (list.length > 0) {
            for (File f : list) {
                iter.addFile(f.getAbsolutePath(), "", t);
            }
            iter.init();

            while (iter.hasNext()) {
                rt.add(iter.next());
            }
        }

    }
    

//    public void writeSkyLineOld() {
//        
//        setStatus("Start writing Skyline library");
//
//        HashMap<AminoAcid,Integer> modificationIds = new HashMap<AminoAcid, Integer>() {
//            public Integer get(Object aa) {
//                Integer i = super.get(aa);
//                if (i==null) 
//                    return 0;
//                else
//                    return i;
//            }
//        };
//        
//        File out = fbPinPoint.getFile();
//        
//        try {
//            
//            PrintWriter pw = new PrintWriter(out);
//            
//            // get the search id
//            int[] searchids = getSearch.getSelectedSearchIds();
//            Connection connection = getSearch.getConnection();
//            DBRunConfig conf = new DBRunConfig(connection);
//            
//            if (searchids == null || searchids.length == 0) {
//                return;
//            }
//            
//            //int searchid = searchids[0];
//            
//            conf.readConfig(searchids);
//            
//            DecimalFormat modFormat = new DecimalFormat("[+#,##0.000];[#,##0.000]");
//            
//            
//            // fake-lysin-crosslinker
//            AminoAcid K = conf.getAminoAcid("K");
//            HashMap<String,String> crosslinkerKMod = new HashMap<String, String>();
//            for (CrossLinker xl : conf.getCrossLinker()) {
//                crosslinkerKMod.put(xl.getName(),"K"+modFormat.format(xl.getCrossLinkedMass() - K.mass + Util.WATER_MASS) );
//            }
//            
//            
//            // get the identifications
//            Statement st = connection.createStatement();
//            ResultSet rs = null;
//            
//            String whereInner = "Search_id in (" + MyArrayUtils.toString(searchids, ",") + ") AND dynamic_rank = true ";
//            if (ckAutoValidated.isSelected() && ckManual.isSelected())
//                whereInner+= " AND ( autovalidated = true OR validated  in ('" + MyArrayUtils.toString(txtValidation.getText().split(","), "','") + "'))";
//            else if (ckAutoValidated.isSelected())
//                whereInner+= " AND ( autovalidated = true )";
//            else if (ckManual.isSelected())
//                whereInner+= " AND (validated in ('" + MyArrayUtils.toString(txtValidation.getText().split(","), "','") + "'))";
//            if (ckXLOnly.isSelected())
//                whereInner += " AND peptide2 is not null ";
//            
//            
//            String querry = "SELECT ve.peptide1, ve.peptide2, s.elution_time_start, s.elution_time_end, ve.precursor_charge, p1.accession_number, p2.accession_number, ve.crosslinker, ve.run_name, ve.scan_number , ve.match_score"
//                    + " FROM (SELECT * FROM v_export_materialized WHERE " + whereInner +") ve INNER JOIN "
//                    + " protein p1 ON ve.display_protein1_id = p1.id LEFT OUTER JOIN "
//                    + " protein p2 ON ve.display_protein2_id = p2.id INNER JOIN "
//                    + " spectrum s ON ve.spectrum_id = s.id" ;
//
//            pw.println("file\tscan\tcharge\tsequence\tmodifications");
//            
//            rs = st.executeQuery(querry);
//            while (rs.next()) {
//                // get the match informations
//                String peptide1 = rs.getString(1);
//                String peptide2 = rs.getString(2);
//                Double ets = rs.getDouble(3);
//                Double ete = rs.getDouble(4);
//                int charge = rs.getInt(5);
//                String prot1 = rs.getString(6);
//                String prot2 = rs.getString(7);
//                String xl = rs.getString(8);
//                String run = rs.getString(9);
//                int scan = rs.getInt(10);
//                double score = rs.getInt(11);
//
//                if (!run.contains("."))
//                    run = run + ".raw";
//                
//                // turn them into peptides
//                Sequence p1s = new Sequence(peptide1, conf);
//                Peptide p1 = new Peptide(p1s,0,p1s.length());
//                Sequence p2s = new Sequence(peptide2, conf);
//                Peptide p2 = new Peptide(p2s,0,p2s.length());
//                StringBuffer base  = new StringBuffer();
//                StringBuffer baseAA  = new StringBuffer();
//                
//                for (AminoAcid aa : p1.toArray()) {
//                    double diff = 0;
//                    AminoAcid baseaa = aa;
//                    if (aa instanceof AminoModification) {
//                        AminoModification am = (AminoModification) aa;
//                        while (am.BaseAminoAcid instanceof  AminoModification) {
//                            am= (AminoModification) am.BaseAminoAcid;
//                        }
//                        baseaa = am.BaseAminoAcid;
//                        base.append(baseaa);
//                        baseAA.append(baseaa);
//                        base.append(modFormat.format(aa.mass-baseaa.mass));
//                    } else {
//                        base.append(aa);
//                        baseAA.append(aa);
//                    }
//                }
//
//                if ( peptide2 != null) {
//
//                    base.append(crosslinkerKMod.get(xl));
//                    baseAA.append(K.SequenceID);
//
//                    for (AminoAcid aa : p2.toArray()) {
//                        double diff = 0;
//                        AminoAcid baseaa = aa;
//                        if (aa instanceof AminoModification) {
//                            AminoModification am = (AminoModification) aa;
//                            while (am.BaseAminoAcid instanceof  AminoModification) {
//                                am= (AminoModification) am.BaseAminoAcid;
//                            }
//                            baseaa = am.BaseAminoAcid;
//                            base.append(baseaa);
//                            baseAA.append(baseaa);
//                            base.append(modFormat.format(aa.mass-baseaa.mass));
//                        } else {
//                            base.append(aa);
//                            baseAA.append(aa);
//                        }
//                    }
//                }
//                //"file\tscan\tcharge\tsequence\tmodifications"
//                pw.println(run + "\t" + scan + "\t"  + charge + "\t" + baseAA + "\t" + base);
//                
//            }
//            pw.flush();;
//            pw.close();
//
//            setStatus("finished");
//            
//            
//        } catch (SQLException ex) {
//            Logger.getLogger(DB2PinPoint.class.getName()).log(Level.SEVERE, null, ex);
//            setStatus("Error : " + ex);
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(DB2PinPoint.class.getName()).log(Level.SEVERE, null, ex);
//            setStatus("Error : " + ex);
//        }
//        
//    }    
    

    
    public void writeSkyLine() {
        
        setStatus("Start writing Skyline library");

        HashMap<AminoAcid,Integer> modificationIds = new HashMap<AminoAcid, Integer>() {
            public Integer get(Object aa) {
                Integer i = super.get(aa);
                if (i==null) 
                    return 0;
                else
                    return i;
            }
        };
        
        File out = fbPinPoint.getFile();
        
        try {
            DecimalFormat modFormat = new DecimalFormat("[+#,##0.000];[-#,##0.000]");
            DecimalFormat modFormatDispl = new DecimalFormat("[+#,##0.00000];[-#,##0.00000]");
            Double dGToWater = -39.010899035;
            String sGToWater = "G"+modFormat.format(dGToWater);
            Double zeroMod = 0.0;
            
            
            PrintWriter pw = new PrintWriter(out);
            
            // get the search id
            int[] searchids = getSearch.getSelectedSearchIds();
            Connection connection = getSearch.getConnection();
            DBRunConfig conf = new DBRunConfig(connection);
            
            if (searchids == null || searchids.length == 0) {
                return;
            }
            
            //int searchid = searchids[0];
            
            conf.readConfig(searchids);
            
//            DecimalFormat modFormat = new DecimalFormat("[+#,##0.000];[-#,##0.000]");
            
            
            // fake-lysin-crosslinker
            AminoAcid K = conf.getAminoAcid("K");
            HashMap<String,String> crosslinkerKModNoHydro = new HashMap<String, String>();
            HashMap<String,String> crosslinkerKModSingleHydro = new HashMap<String, String>();
            HashMap<String,String> crosslinkerKModDualHydro = new HashMap<String, String>();
            for (CrossLinker xl : conf.getCrossLinker()) {
                crosslinkerKModNoHydro.put(xl.getName(),"K"+modFormat.format(xl.getCrossLinkedMass() - K.mass + Util.WATER_MASS) );
                crosslinkerKModSingleHydro.put(xl.getName(),"K"+modFormat.format(xl.getCrossLinkedMass() - K.mass + Util.WATER_MASS - Util.HYDROGEN_MASS) );
                crosslinkerKModDualHydro.put(xl.getName(),"K"+modFormat.format(xl.getCrossLinkedMass() - K.mass + Util.WATER_MASS - 2*Util.HYDROGEN_MASS) );
            }
            
            boolean xi3db = false;
            try {
                ResultSet t = connection.createStatement().executeQuery("SELECT * FROM spectrum_source limit 1");
                t.close();
                xi3db = true;
            }catch(SQLException sex) {
                
            }

            
            // get the identifications
            Statement st = connection.createStatement();
            ResultSet rs = null;
            
            String whereInner = "Search_id in (" + MyArrayUtils.toString(searchids, ",") + ") AND dynamic_rank = true  ";
            String whereOuter = "";
            if (ckXLOnly.isSelected())
                if (xi3db)
                    whereOuter += " pep2.sequence is not null ";
                else
                    whereInner += " AND peptide2 is not null ";
            
            if (ckAutoValidated.isSelected() && ckManual.isSelected())
                whereInner+= " AND ( autovalidated = true OR validated  in ('" + MyArrayUtils.toString(txtValidation.getText().split(","), "','") + "'))";
            else if (ckAutoValidated.isSelected())
                whereInner+= " AND ( autovalidated = true )";
            else if (ckManual.isSelected())
                whereInner+= " AND (validated in ('" + MyArrayUtils.toString(txtValidation.getText().split(","), "','") + "'))";
            if (!ckDecoys.isSelected())
                whereInner+= " AND (not is_decoy)";
            
            
            String querry = "SELECT ve.peptide1, ve.peptide2, s.elution_time_start, s.elution_time_end, ve.precursor_charge, p1.accession_number, p2.accession_number, ve.crosslinker, ve.run_name, ve.scan_number , ve.match_score, ve.pep1_link_pos, ve.pep2_link_pos"
                    + " FROM (SELECT * FROM v_export_materialized WHERE " + whereInner +") ve INNER JOIN "
                    + " protein p1 ON ve.display_protein1_id = p1.id LEFT OUTER JOIN "
                    + " protein p2 ON ve.display_protein2_id = p2.id INNER JOIN "
                    + " spectrum s ON ve.spectrum_id = s.id"
                    +  (whereOuter.isEmpty() ? "" : "WHERE " + whereOuter);

            
            if (xi3db) {
                querry = "SELECT pep1.sequence, pep2.sequence, s.elution_time_start, "
                        + " s.elution_time_end, sm.precursor_charge, prot1.accession_number, prot2.accession_number, "
                        + " xl.name, ss.name, s.scan_number, sm.score, mp1.link_position, mp2.link_position "
                        + " FROM "
                        + "  (SELECT * FROM spectrum_match WHERE " + whereInner +") sm INNER JOIN "
                        + "  (SELECT * FROM matched_peptide WHERE Search_id in (" + MyArrayUtils.toString(searchids, ",") + ") and match_type = 1) mp1 "
                        + "     ON sm.id = mp1.match_id "                        
                        + "     INNER JOIN "
                        + "  peptide pep1 on mp1.peptide_id = pep1.id "
                        + "     INNER JOIN "
                        + "  has_protein hp1 on mp1.peptide_id = hp1.peptide_id and hp1.display_site "
                        + "     INNER JOIN "
                        + "  protein prot1 on hp1.protein_id =prot1.id "
                        + "     LEFT OUTER JOIN"
                        + "  (SELECT * FROM matched_peptide WHERE Search_id in (" + MyArrayUtils.toString(searchids, ",") + ") and match_type = 2) mp2 "
                        + "     ON sm.id = mp2.match_id "                        
                        + "     LEFT OUTER JOIN"
                        + "  peptide pep2 on mp2.peptide_id = pep2.id "
                        + "     LEFT OUTER JOIN"
                        + "  has_protein hp2 on mp2.peptide_id = hp2.peptide_id and hp2.display_site "
                        + "     LEFT OUTER JOIN"
                        + "  protein prot2 on hp2.protein_id =prot2.id "
                        + "     LEFT OUTER JOIN"
                        + "  (SELECT id, regexp_replace(regexp_replace(description,E'.*Name:',''),E';.*','') as name from crosslinker) xl on mp1.crosslinker_id =xl.id "
                        + " INNER JOIN "
                        + " spectrum s ON sm.spectrum_id = s.id "
                        + " INNER JOIN "
                        + " spectrum_source ss ON s.source_id = ss.id"
                        +  (whereOuter.isEmpty() ? "" : " WHERE " + whereOuter);
            }
            
            pw.println("file\tscan\tcharge\tsequence\tscore-type\tscore");
            HashSet<String> mods = new HashSet<String>();
            HashSet<String> xlSiteMods = new HashSet<String>();
            HashSet<String> xlMods = new HashSet<String>();
            rs = st.executeQuery(querry);
            while (rs.next()) {
                // get the match informations
                String peptide1 = rs.getString(1);
                String peptide2 = rs.getString(2);
                Double ets = rs.getDouble(3);
                Double ete = rs.getDouble(4);
                int charge = rs.getInt(5);
                String prot1 = rs.getString(6);
                String prot2 = rs.getString(7);
                String xl = rs.getString(8);
                String run = rs.getString(9);
                int scan = rs.getInt(10);
                double score = rs.getDouble(11);
                int link1 = rs.getInt(12);
                int link2 = rs.getInt(13);
                
                if (!run.contains("."))
                    run = run + ".mzML";

                if (!run.endsWith(".raw"))
                    run = run.substring(0,run.length()-4)+"mzML";
                
                // turn them into peptides
                Sequence p1s = new Sequence(peptide1, conf);
                Peptide p1 = new Peptide(p1s,0,p1s.length());
                StringBuffer base  = new StringBuffer();
                AminoAcid[] p1Seq = p1.toArray();

                
                for (int aap =0; aap<p1Seq.length;aap++) {
                    
                    AminoAcid aa = p1Seq[aap] ;
                    
                    AminoAcid baseaa = aa;
                    if (aa instanceof AminoModification) {
                        AminoModification am = (AminoModification) aa;
                        while (am.BaseAminoAcid instanceof  AminoModification) {
                            am= (AminoModification) am.BaseAminoAcid;
                        }
                        baseaa = am.BaseAminoAcid;
                        base.append(baseaa);
//                        if (aap == )
                        base.append(modFormat.format(aa.mass-baseaa.mass));
                        mods.add(baseaa+modFormatDispl.format(aa.mass-baseaa.mass));
                        
                    } else {
                        base.append(aa);
                        if (aap == link1 && peptide2 != null) {
                            base.append(modFormat.format(Util.HYDROGEN_MASS));
                            xlSiteMods.add(aa+modFormatDispl.format(Util.HYDROGEN_MASS));
                        }
                    }
                }
                
                if ( peptide2 != null) {
                    Sequence p2s = new Sequence(peptide2, conf);
                    Peptide p2 = new Peptide(p2s,0,p2s.length());
//                    base.append(sGToWater);
                    // do we have modifications on the linkage site?
                    int modCount =2;
                    if (!(p1.aminoAcidAt(link1) instanceof AminoModification || 
                            p1.aminoAcidAt(link1) instanceof AminoLabel) )
                        modCount--;

                    if (!(p2.aminoAcidAt(link2) instanceof AminoModification || 
                            p2.aminoAcidAt(link2) instanceof AminoLabel)) 
                        modCount--;
                    
                    String xlmod = crosslinkerKModDualHydro.get(xl);
                    if (modCount == 1) {
                        xlmod = crosslinkerKModSingleHydro.get(xl);
                    } else if (modCount == 2) {
                        xlmod = crosslinkerKModNoHydro.get(xl);
                    }
                    base.append(xlmod);
                    
                    xlMods.add(xlmod);
                        
                    
                    AminoAcid[] p2Seq = p2.toArray();
                    
                    for (int aap =0; aap<p2Seq.length;aap++) {

                        AminoAcid aa = p2Seq[aap] ;
                        double diff = 0;
                        AminoAcid baseaa = aa;
                        if (aa instanceof AminoModification) {
                            AminoModification am = (AminoModification) aa;
                            while (am.BaseAminoAcid instanceof  AminoModification) {
                                am= (AminoModification) am.BaseAminoAcid;
                            }
                            baseaa = am.BaseAminoAcid;
                            base.append(baseaa);
                            base.append(modFormat.format(aa.mass-baseaa.mass));
                            mods.add(baseaa+modFormatDispl.format(aa.mass-baseaa.mass));
                            
                        } else {
                            base.append(aa);
                            if (aap == link2) {
                                base.append(modFormat.format(Util.HYDROGEN_MASS));
                                xlSiteMods.add(aa+modFormatDispl.format(Util.HYDROGEN_MASS));
                            }
                        }
                    }
                }
                
                
                pw.println(run + "\t" + scan + "\t"  + charge + "\t" + base + "\tUNKNOWN\t" + score);
                
            }
            pw.flush();;
            pw.close();

            setStatus("finished");
            
            final JFrame w = new JFrame("Found Modifications");
            w.getContentPane().setLayout(new javax.swing.BoxLayout(w.getContentPane(), javax.swing.BoxLayout.Y_AXIS));
            
            JTextArea txtMods = new JTextArea("found Modifications: \n\t" + MyArrayUtils.toString(mods, "\n\t") +"\n"
                    + "fake crosslinker found : \n\t" + MyArrayUtils.toString(xlMods, "\n\t") +"\n"
                    + "cross-linker sites: \n\t" + MyArrayUtils.toString(xlSiteMods, "\n\t") +"\n"
                    + "These need to be defined in skyline");
            JScrollPane sp = new JScrollPane(txtMods);
            sp.setPreferredSize(new Dimension(200,200));
            w.getContentPane().add(sp);
            JButton btnOK = new JButton("OK");
            w.getContentPane().add(btnOK);
            w.setPreferredSize(new Dimension(400, 400));
            w.pack();
            w.setVisible(xi3db);
            btnOK.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    w.setVisible(false);
                    w.dispose();
                }
            });
            
            
            
        } catch (SQLException ex) {
            Logger.getLogger(DB2PinPoint.class.getName()).log(Level.SEVERE, null, ex);
            setStatus("Error : " + ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DB2PinPoint.class.getName()).log(Level.SEVERE, null, ex);
            setStatus("Error : " + ex);
        }
        
    }    
    
    
    public void write() {
        double factor = 1;
        String sfactor = cbRetentionTimeFactor.getSelectedItem().toString();
        if (sfactor.contentEquals("Minutes (1)")) {
            factor = 1;
        } else if (sfactor.contentEquals("Seconds (60)")) {
            factor = 60;
        } else {
            try {
                factor = Double.parseDouble(sfactor);
            } catch (NumberFormatException ne) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, sfactor + " is not recognised as number", ne);
                setStatus(sfactor + " is not recognised as number");
                return;
            }
        }
        
        setStatus("read retention times");
        RetentionTime rt = new RetentionTime();
        try {
            readInRetentionTimes(rt);
            setStatus("read from database");
        } catch (Exception ex) {
            Logger.getLogger(DB2PinPoint.class.getName()).log(Level.SEVERE, null, ex);
            setStatus("error reading retention times - read data from database: " + ex);
        }
        

        HashMap<AminoAcid,Integer> modificationIds = new HashMap<AminoAcid, Integer>() {
            public Integer get(Object aa) {
                Integer i = super.get(aa);
                if (i==null) 
                    return 0;
                else
                    return i;
            }
        };
        
        File out = fbPinPoint.getFile();
        
        try {
            
            PrintWriter pw = new PrintWriter(out);
            
            // get the search id
            int[] searchids = getSearch.getSelectedSearchIds();
            Connection connection = getSearch.getConnection();
            DBRunConfig conf = new DBRunConfig(connection);
            
            if (searchids == null || searchids.length == 0) {
                return;
            }
            
            //int searchid = searchids[0];
            
            conf.readConfig(searchids);
            
            // get the fixed modifications
            StringBuffer sbFixedModHeader = new StringBuffer();
            for (AminoModification am : conf.getFixedModifications()) {
                pw.append("static,");
                pw.append(am.BaseAminoAcid.SequenceID);
                pw.append(",");
                pw.append(Util.sixDigits.format(am.mass - am.BaseAminoAcid.mass));
                pw.println("");
            }
            
            int varModCount=0;
            HashMap<Double,Integer> massToID = new HashMap<Double, Integer>();
            HashMap<Integer, ArrayList<String>>  idToModAA= new HashMap<Integer, ArrayList<String>>();
            HashMap<Integer, Double>  idToModMass= new HashMap<Integer, Double>();
            
            // variable modifcations
            for (AminoModification am : conf.getVariableModifications()) {
                Double modmass = Math.round((am.mass - am.BaseAminoAcid.mass)*10000.0)/10000.0;
                Integer id = massToID.get(modmass);
                if (id == null) {
                    id = ++varModCount;
                    massToID.put(modmass, id);
                    ArrayList<String> modAA = new ArrayList<String>();
                    modAA.add(am.BaseAminoAcid.SequenceID);
                    idToModAA.put(id, modAA);
                    idToModMass.put(id,am.mass - am.BaseAminoAcid.mass);
                } else {
                    ArrayList<String> modAA = idToModAA.get(id);
                    modAA.add(am.BaseAminoAcid.SequenceID);
                }
                
                modificationIds.put(am, id);
                
            }
            // label
            for (AminoLabel al : conf.getLabel()) {
                Double modmass = Math.round((al.mass - al.BaseAminoAcid.mass)*10000.0)/10000.0;
                Integer id = massToID.get(modmass);
                if (id == null) {
                    id = ++varModCount;
                    massToID.put(modmass, id);
                    ArrayList<String> modAA = new ArrayList<String>();
                    modAA.add(al.BaseAminoAcid.SequenceID);
                    idToModAA.put(id, modAA);
                    idToModMass.put(id,al.mass - al.BaseAminoAcid.mass);
                } else {
                    ArrayList<String> modAA = idToModAA.get(id);
                    modAA.add(al.BaseAminoAcid.SequenceID);
                }
                
                modificationIds.put(al, id);
            }
            
            for (Integer id=1; id<= varModCount; id++) {
                pw.append("dynamic,");
                pw.append(id.toString());
                pw.append(",");
                pw.append(MyArrayUtils.toString(idToModAA.get(id), ""));
                pw.append(",");
                pw.append(Util.sixDigits.format(idToModMass.get(id)));
                pw.println("");
            }
            
            // fake-lysin-crosslinker
            AminoAcid K = conf.getAminoAcid("K");
            HashMap<String, CrossLinker> crosslinkers = new HashMap<String, CrossLinker>();
            HashMap<String, Integer> crosslinkersID = new HashMap<String, Integer>();
            for (CrossLinker xl : conf.getCrossLinker()) {
                int id = varModCount + 1 + crosslinkersID.size();
                crosslinkersID.put(xl.getName(),id);
                pw.append("dynamic,");
                pw.append(Integer.toString(id));
                pw.append(",K,");
                pw.append(Util.sixDigits.format(xl.getCrossLinkedMass() - K.mass + Util.WATER_MASS));
                pw.println("");
            }
            
            
            // get the identifications
            Statement st = connection.createStatement();
            ResultSet rs = null;
            
            String where = "Search_id in (" + MyArrayUtils.toString(searchids, ",") + ") AND dynamic_rank = true ";
            if (ckAutoValidated.isSelected() && ckManual.isSelected())
                where+= " AND ( autovalidated = true OR validated  in ('" + MyArrayUtils.toString(txtValidation.getText().split(","), "','") + "'))";
            else if (ckAutoValidated.isSelected())
                where+= " AND ( autovalidated = true )";
            else if (ckManual.isSelected())
                where+= " AND (validated in ('" + MyArrayUtils.toString(txtValidation.getText().split(","), "','") + "'))";
            if (!ckDecoys.isSelected())
                where+= " AND (!isDecoy)";
            
            
            String querry = "SELECT ve.peptide1, ve.peptide2, s.elution_time_start, "
                    + " s.elution_time_end, ve.precursor_charge, p1.accession_number, p2.accession_number, "
                    + " ve.crosslinker, ve.run_name, ve.scan_number,p1.name, p2.name,p1.Description, p2.Description,  "
                    + " pep1_link_pos, pep2_link_pos, peptide_position1, peptide_position2"
                    + " FROM (SELECT * FROM v_export_materialized WHERE " + where +") ve INNER JOIN "
                    + " protein p1 ON ve.display_protein1_id = p1.id LEFT OUTER JOIN "
                    + " protein p2 ON ve.display_protein2_id = p2.id INNER JOIN "
                    + " spectrum s ON ve.spectrum_id = s.id" ;
            
            boolean xi3db = false;
            try {
                ResultSet t = connection.createStatement().executeQuery("SELECT * FROM spectrum_source limit 1");
                t.close();
                xi3db = true;
            }catch(SQLException sex) {
                
            }
            if (xi3db) {
                querry = "SELECT pep1.sequence, pep2.sequence, s.elution_time_start, "
                        + " s.elution_time_end, sm.precursor_charge, prot1.accession_number, prot2.accession_number, "
                        + " xl.name, ss.name, s.scan_number, prot1.name, prot2.name,prot1.Description, prot2.Description,  "
                        + " mp1.link_position, mp2.link_position, hp1.peptide_position, hp2.peptide_position"
                        + " FROM "
                        + "  (SELECT * FROM spectrum_match WHERE " + where +") sm INNER JOIN "
                        + "  (SELECT * FROM matched_peptide WHERE Search_id in (" + MyArrayUtils.toString(searchids, ",") + ") and match_type = 1) mp1 "
                        + "     ON sm.id = mp1.match_id "                        
                        + "     INNER JOIN "
                        + "  peptide pep1 on mp1.peptide_id = pep1.id "
                        + "     INNER JOIN "
                        + "  has_protein hp1 on mp1.peptide_id = hp1.peptide_id and hp1.display_site "
                        + "     INNER JOIN "
                        + "  protein prot1 on hp1.protein_id =prot1.id "
                        + "     LEFT OUTER JOIN"
                        + "  (SELECT * FROM matched_peptide WHERE Search_id in (" + MyArrayUtils.toString(searchids, ",") + ") and match_type = 1) mp2 "
                        + "     ON sm.id = mp2.match_id "                        
                        + "     LEFT OUTER JOIN"
                        + "  peptide pep2 on mp2.peptide_id = pep2.id "
                        + "     LEFT OUTER JOIN"
                        + "  has_protein hp2 on mp2.peptide_id = hp2.peptide_id and hp2.display_site "
                        + "     LEFT OUTER JOIN"
                        + "  protein prot2 on hp2.protein_id =prot2.id "
                        + "     LEFT OUTER JOIN"
                        + "  crosslinker xl on mp1.crosslinker_id =xl.id "
                        + " INNER JOIN "
                        + " spectrum s ON sm.spectrum_id = s.id"
                        + " INNER JOIN "
                        + " spectrum_source ss ON s.source_id = ss.id" ;
            }
            
            
            rs = st.executeQuery(querry);
            while (rs.next()) {
                // get the match informations
                String peptide1 = rs.getString(1);
                String peptide2 = rs.getString(2);
                Double ets = rs.getDouble(3);
                Double ete = rs.getDouble(4);
                int charge = rs.getInt(5);
                String prot1acc = rs.getString(6);
                String prot2acc = rs.getString(7);
                String xl = rs.getString(8);
                String run = rs.getString(9);
                int scan = rs.getInt(10);
                String prot1name = rs.getString(11);
                String prot2name = rs.getString(12);
                String prot1desc = rs.getString(13);
                String prot2desc = rs.getString(14);
                int link1 = rs.getInt(15);
                int link2 = rs.getInt(16);
                int pepSite1 = rs.getInt(17);
                int pepSite2 = rs.getInt(18);
                
                // turn them into peptides
                Sequence p1s = new Sequence(peptide1, conf);
                Peptide p1 = new Peptide(p1s,0,p1s.length());
                Sequence p2s = new Sequence(peptide2, conf);
                Peptide p2 = new Peptide(p2s,0,p2s.length());

                // the concatinated sequence
                String base = p1.toStringBaseSequence() + "K" + p2.toStringBaseSequence();
                
                // get the modifications
                String mod = "00";
                for (int aaid = 0; aaid<p1.length(); aaid++) {
                    AminoAcid aa = p1.aminoAcidAt(aaid);
                    mod += modificationIds.get(aa);
                }
                // add the "faked" crosslinker
                mod += crosslinkersID.get(xl);
                // and modifications for the second peptide
                for (int aaid = 0; aaid<p2.length(); aaid++) {
                    AminoAcid aa = p2.aminoAcidAt(aaid);
                    mod += modificationIds.get(aa);
                }
                ets = rt.get(run, scan, ets)/factor;
                
                String id = cbLabel.getSelectedItem().toString();
                
                id=generateID( id, prot1acc, prot2acc, link1, link2, pepSite1, pepSite2, peptide1, peptide2, prot1name, prot2name, prot1desc, prot2desc, run);
                
                pw.println(base + "," + charge + "," + Util.sixDigits.format(ets) + "," + mod + ",," + id + "," + run + "," + scan);
                pw.println("0,0");
                
            }
            pw.flush();
            pw.close();

            setStatus("finished");
            
            
        } catch (SQLException ex) {
            Logger.getLogger(DB2PinPoint.class.getName()).log(Level.SEVERE, null, ex);
            setStatus("Error : " + ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DB2PinPoint.class.getName()).log(Level.SEVERE, null, ex);
            setStatus("Error : " + ex);
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

        txtStatus = new java.awt.TextField();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        getSearch = new rappsilber.gui.components.db.GetSearch();
        flPeakList = new rappsilber.gui.components.FileList();
        btnWrite = new javax.swing.JButton();
        fbPinPoint = new rappsilber.gui.components.FileBrowser();
        jLabel1 = new javax.swing.JLabel();
        ckAutoValidated = new javax.swing.JCheckBox();
        ckManual = new javax.swing.JCheckBox();
        txtValidation = new javax.swing.JTextField();
        btnWriteSkyLine = new javax.swing.JButton();
        ckXLOnly = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        cbRetentionTimeFactor = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        cbLabel = new javax.swing.JComboBox();
        lblIDExample = new javax.swing.JLabel();
        ckDecoys = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        txtStatus.setEditable(false);

        getSearch.setBorder(javax.swing.BorderFactory.createTitledBorder("Searches"));
        jTabbedPane1.addTab("Search", getSearch);

        flPeakList.setBorder(javax.swing.BorderFactory.createTitledBorder("read Retention times from:"));
        flPeakList.setDescription("peak-list (mgf,msm,apl)");
        flPeakList.setExtensions(new String[] {"mgf", "msm", "apl"});
        jTabbedPane1.addTab("Retention Times", flPeakList);

        btnWrite.setText("PinPoint");
        btnWrite.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnWriteActionPerformed(evt);
            }
        });

        fbPinPoint.setDescription("Generic Library (txt,ssl)");
        fbPinPoint.setExtensions(new String[] {"txt", "ssl"});
        fbPinPoint.setLoad(false);

        jLabel1.setText("Library-File");

        ckAutoValidated.setText("Auto-validated");

        ckManual.setText("manual validation");
        ckManual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckManualActionPerformed(evt);
            }
        });

        txtValidation.setText("A");
        txtValidation.setEnabled(false);

        btnWriteSkyLine.setText("SkyLine");
        btnWriteSkyLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnWriteSkyLineActionPerformed(evt);
            }
        });

        ckXLOnly.setText("Cross-link only");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("PinPoint Options"));

        jLabel2.setText("Retention factor(input retention time)");

        cbRetentionTimeFactor.setEditable(true);
        cbRetentionTimeFactor.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Minutes (1)", "Seconds (60)", ".01667" }));

        jLabel3.setText("Label");

        cbLabel.setEditable(true);
        cbLabel.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "%A1_%L1;%A2_%L2", "%A1 %L1 xl %A2 %L2", "%A1;%A2", "%P1-%S1 x %P2-%S2", "%R: %L1;%L2", "%R: %A1;%A2", "%A1 xl %A2", "%N1 xl %N2", "%D1 xl %D2", "%D1 %L1 xl %D2 %L2", "%R: %A1 %N1 %D1 - %L1 -XL- %A2 %N2 %D2 - %L2", "%A1_%L1_%L2", " " }));
        cbLabel.setToolTipText("Available replacements:%A1, %A2; %N1, %N2; %D1, %D2; %P1, %P2; %S1, %S2; %L1, %L2; %R");
        cbLabel.setAutoscrolls(true);
        cbLabel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbLabelActionPerformed(evt);
            }
        });

        lblIDExample.setText("(EXAMPLE)");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblIDExample))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbRetentionTimeFactor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbRetentionTimeFactor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 25, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(lblIDExample)))
        );

        ckDecoys.setText("export decoys");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(txtStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fbPinPoint, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnWriteSkyLine)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnWrite))
                    .addComponent(jTabbedPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ckAutoValidated)
                            .addComponent(ckXLOnly)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(ckManual)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtValidation, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(ckDecoys))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 24, Short.MAX_VALUE)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(ckAutoValidated)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(ckManual)
                            .addComponent(txtValidation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ckXLOnly)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ckDecoys)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnWrite)
                        .addComponent(btnWriteSkyLine))
                    .addComponent(jLabel1)
                    .addComponent(fbPinPoint, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnWriteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnWriteActionPerformed
    
        btnWrite.setEnabled(false);
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    write();
                } catch (Exception e) {
                    setStatus(e.toString());
                }
                EventQueue.invokeLater(new Runnable() {

                    public void run() {
                        btnWrite.setEnabled(true);
                    }
                });
                
            }
        }).start();
        
        
    }//GEN-LAST:event_btnWriteActionPerformed

    private void ckManualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckManualActionPerformed
        txtValidation.setEnabled(ckManual.isSelected());
    }//GEN-LAST:event_ckManualActionPerformed

    private void btnWriteSkyLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnWriteSkyLineActionPerformed
        btnWriteSkyLine.setEnabled(false);
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    writeSkyLine();
                } catch (Exception e) {
                    Logger.getLogger(DB2PinPoint.class.getName()).log(Level.SEVERE, "Error:",e);
                    setStatus(e.toString());
                }
                EventQueue.invokeLater(new Runnable() {

                    public void run() {
                        btnWriteSkyLine.setEnabled(true);
                    }
                });
                
            }
        }).start();
    }//GEN-LAST:event_btnWriteSkyLineActionPerformed

    private void cbLabelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbLabelActionPerformed
        
        String id = cbLabel.getSelectedItem().toString();
        id=generateID( id, "P000001", "P000002", 7, 5, 48, 84, "KSNLGR", "ILYKOR", "SAMP1", "SAMP2", "First sample protein", "Second sample protein","MyRawFile");
        lblIDExample.setText(id);
    }//GEN-LAST:event_cbLabelActionPerformed

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
            java.util.logging.Logger.getLogger(DB2PinPoint.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DB2PinPoint.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DB2PinPoint.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DB2PinPoint.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new DB2PinPoint().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnWrite;
    private javax.swing.JButton btnWriteSkyLine;
    private javax.swing.JComboBox cbLabel;
    private javax.swing.JComboBox cbRetentionTimeFactor;
    private javax.swing.JCheckBox ckAutoValidated;
    private javax.swing.JCheckBox ckDecoys;
    private javax.swing.JCheckBox ckManual;
    private javax.swing.JCheckBox ckXLOnly;
    private rappsilber.gui.components.FileBrowser fbPinPoint;
    private rappsilber.gui.components.FileList flPeakList;
    private rappsilber.gui.components.db.GetSearch getSearch;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblIDExample;
    private java.awt.TextField txtStatus;
    private javax.swing.JTextField txtValidation;
    // End of variables declaration//GEN-END:variables
}
