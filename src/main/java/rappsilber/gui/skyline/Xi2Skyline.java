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
package rappsilber.gui.skyline;

import java.awt.Dimension;
import java.awt.EventQueue;
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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.rappsilber.data.csv.CSVResultSet;
import org.rappsilber.data.csv.CsvMultiParser;
import rappsilber.config.AbstractRunConfig;
import rappsilber.config.DBRunConfig;
import rappsilber.config.RunConfig;
import rappsilber.config.RunConfigFile;
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
public class Xi2Skyline extends javax.swing.JFrame {

    public class RetentionTime extends HashMap<String, HashMap<Integer, Double>> {

        public void add(Spectra s) {
            String runname = s.getRun().toLowerCase();
            HashMap<Integer, Double> run = get(runname);
            if (run == null) {
                run = new HashMap<Integer, Double>();
                super.put(runname, run);
                if (runname.endsWith(".raw")) {
                    super.put(runname.substring(0, runname.length() - 4), run);
                } else {
                    super.put(runname + ".raw", run);
                }
            }
            double ets = s.getElutionTimeStart();
            double ete = s.getElutionTimeEnd();
            if (ets > 0 && ete > 0) {
                ets += ete / 2;
            } else if (ete > 0) {
                ets = ete;
            }
            run.put(s.getScanNumber(), ets);
        }

        public Double get(String runname, int scan, double defaultRT) {
            HashMap<Integer, Double> run = get(runname.toLowerCase());
            if (run == null) {
                return defaultRT;
            }
            Double ret = run.get(scan);
            if (ret == null) {
                return defaultRT;
            }
            return ret;
        }

    }

    /**
     * class used to hold all infos for a peptide. Mainly used to ease the
     * switching of peptides to ensure the same peptides are always in the same
     * order.
     */
    private class PepInfo {

        String peptideSequence;
        String proteinAccesion;
        int peptideLinkPos;
        int peptidePosition;

        public PepInfo(String peptideSequence, String proteinAccesion, int peptideLinkPos, int peptidePosition) {
            this.peptideSequence = peptideSequence;
            this.proteinAccesion = proteinAccesion;
            this.peptideLinkPos = peptideLinkPos;
            this.peptidePosition = peptidePosition;
        }

    }

    /**
     * Creates new form DB2PinPoint
     */
    public Xi2Skyline() {
        initComponents();
        getSearch.addStatusListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setStatus(e.getActionCommand());
            }
        });
        cbLabelActionPerformed(null);
        //getSearch.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        try {
            if (this.getSearch.getConnection() == null) {
                this.tpTabs.remove(0);
            }
        } catch (Exception e) {
                this.tpTabs.remove(0);
        }
        
    }

    public void setStatus(final String text) {
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                txtStatus.setText(text);
            }
        });
    }

    protected String generateID(String id, String prot1acc, String prot2acc, int pepLink1, int pepLink2, int pepSite1, int pepSite2, String seq1, String seq2, String prot1name, String prot2name, String prot1desc, String prot2desc, String run) {
        id = id.replace("%A1", prot1acc)
                .replace("%A2", prot2acc)
                .replace("%S1", "" + (pepLink1 + 1))
                .replace("%S2", "" + (pepLink2 + 1))
                .replace("%L1", "" + (pepLink1 + pepSite1 + 1))
                .replace("%L2", "" + (pepLink2 + pepSite2 + 1))
                .replace("%P1", seq1)
                .replace("%P2", seq2)
                .replace("%N1", prot1name)
                .replace("%N2", prot2name)
                .replace("%D1", prot1desc)
                .replace("%D2", prot2desc)
                .replace("%R", run);
        return id;
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

    private String massToSkylineMod(DecimalFormat f, double mass) {
        return "[" + (mass >=0?"+"+f.format(mass):f.format(mass)) + "]";
    }

    private String massToCrosslinkMod(DecimalFormat f, double mass) {
        return (mass >=0?"+"+f.format(mass):f.format(mass));
    }
    
    public void writeSkyLine() {

        setStatus("Start writing Skyline library");

        HashMap<AminoAcid, Integer> modificationIds = new HashMap<AminoAcid, Integer>() {
            public Integer get(Object aa) {
                Integer i = super.get(aa);
                if (i == null) {
                    return 0;
                } else {
                    return i;
                }
            }
        };

        File out = fbPinPoint.getFile();
        if (out == null) {
            setStatus("no output file defined");
            return;
        }

        try {
            final Locale numberlocale = lpNumberLocale.getSelectLocale();
            DecimalFormat numberFormat = (DecimalFormat) NumberFormat.getNumberInstance(numberlocale);
            Double zeroMod = 0.0;
            boolean irtLinears = false;
            boolean xlOnly = false;
            String irtLinearsProtein = null;

            PrintWriter pw = new PrintWriter(out);
            //  RunConfig conf;
            Connection connection = null;

            // get the search id
            int[] searchids = getSearch.getSelectedSearchIds();
            RunConfig conf = null;
            if (flResults.getFiles().length == 0) {
                connection = getSearch.getConnection();
                conf = new DBRunConfig(connection);
                if (searchids == null || searchids.length == 0) {
                    setStatus("No search/csv-file selected as input");
                    return;
                }
                //int searchid = searchids[0];
                ((DBRunConfig) conf).readConfig(searchids);
            } else {
                if (fbConfigFile.getFile() != null) {
                    try {
                        conf = new RunConfigFile(fbConfigFile.getFile());
                    } catch (ParseException ex) {
                        Logger.getLogger(Xi2Skyline.class.getName()).log(Level.SEVERE, null, ex);
                        setStatus("Error reading input file: " + ex);
                        return;
                    }
                } else if (gsConfigSearch.getSelectedSearchIds().length != 0) {
                    connection = gsConfigSearch.getConnection();
                    conf = new DBRunConfig(connection);
                    //int searchid = searchids[0];
                    ((DBRunConfig) conf).readConfig(gsConfigSearch.getSelectedSearchIds());
                } else {
                    Logger.getLogger(Xi2Skyline.class.getName()).log(Level.SEVERE, "No config defined");
                    setStatus("Error : No config defined");
                    return;
                }

            }

//            DecimalFormat modFormat = new DecimalFormat("[+#,##0.000];[-#,##0.000]");
            // fake-lysin-crosslinker
            AminoAcid K = conf.getAminoAcid("K");
            HashMap<String, String> crosslinkerKModNoHydro = new HashMap<String, String>();
            HashMap<String, String> crosslinkerKModSingleHydro = new HashMap<String, String>();
            HashMap<String, String> crosslinkerKModDualHydro = new HashMap<String, String>();
            HashMap<String, String> crosslinkerMass = new HashMap<String, String>();
            HashSet<String> crosslinkerNames = new HashSet<String>();
            for (CrossLinker xl : conf.getCrossLinker()) {
                double mass = xl.getCrossLinkedMass() - K.mass + Util.WATER_MASS;
                String massName = Integer.toString((int) (xl.getCrossLinkedMass() * 1000));
                crosslinkerNames.add(xl.getName());
                crosslinkerNames.add(massName);
                crosslinkerMass.put(xl.getName(),massToCrosslinkMod(numberFormat, xl.getCrossLinkedMass()));
                crosslinkerKModNoHydro.put(xl.getName(), "K" + massToSkylineMod(numberFormat, mass));
                crosslinkerKModNoHydro.put(massName, "K" + massToSkylineMod(numberFormat, mass));
                
                mass = xl.getCrossLinkedMass() - K.mass + Util.WATER_MASS - Util.HYDROGEN_MASS;
                crosslinkerKModSingleHydro.put(xl.getName(), "K" + massToSkylineMod(numberFormat, mass));
                crosslinkerKModSingleHydro.put(massName, "K" + massToSkylineMod(numberFormat, mass));
                
                mass = xl.getCrossLinkedMass() - K.mass + Util.WATER_MASS - 2 * Util.HYDROGEN_MASS;
                crosslinkerKModDualHydro.put(xl.getName(), "K" + massToSkylineMod(numberFormat, mass));
                crosslinkerKModDualHydro.put(massName, "K" + massToSkylineMod(numberFormat, mass));
            }

            boolean xi3db = false;
            ResultSet rs = null;
            Statement st = null;
            String querry = null;
            if (connection != null) {
                try {
                    ResultSet t = connection.createStatement().executeQuery("SELECT * FROM spectrum_source limit 1");
                    t.close();
                    xi3db = true;
                } catch (Exception ex) {

                }

                // get the identifications
                st = connection.createStatement();

                String whereInner = "Search_id in (" + MyArrayUtils.toString(searchids, ",") + ") AND dynamic_rank = true  ";
                String whereOuter = "";
                if (ckXLOnly.isSelected()) {
                    if (!ckAllLinearsOf.isSelected()) {
                        if (xi3db) {
                            whereOuter += " pep2.sequence is not null ";
                        } else {
                            whereInner += " AND peptide2 is not null ";
                        }
                    } else {
                        if (ckAllLinearsOf.isSelected()) {
                            irtLinears = true;
                            irtLinearsProtein = txtAllLinearsOf.getText();
                        }
                    }
                    xlOnly = true;
                }

                if (ckAutoValidated.isSelected() && ckManual.isSelected()) {
                    whereInner += " AND ( autovalidated = true OR validated  in ('" + MyArrayUtils.toString(txtValidation.getText().split(","), "','") + "'))";
                } else if (ckAutoValidated.isSelected()) {
                    whereInner += " AND ( autovalidated = true )";
                } else if (ckManual.isSelected()) {
                    whereInner += " AND (validated in ('" + MyArrayUtils.toString(txtValidation.getText().split(","), "','") + "'))";
                }
                if (!ckDecoys.isSelected()) {
                    whereInner += " AND (not is_decoy)";
                }

                querry = "SELECT ve.peptide1, ve.peptide2, s.elution_time_start, s.elution_time_end, ve.precursor_charge, p1.accession_number AS accession_number1, p2.accession_number as accession_number2, "
                        + "ve.crosslinker, null as crosslinkermass, ve.run_name, ve.scan_number , ve.match_score as score, "
                        + " ve.pep1_link_pos+1 AS link_position1, ve.pep2_link_pos+1  AS link_position2"
                        + " FROM (SELECT * FROM v_export_materialized WHERE " + whereInner + ") ve INNER JOIN "
                        + " protein p1 ON ve.display_protein1_id = p1.id LEFT OUTER JOIN "
                        + " protein p2 ON ve.display_protein2_id = p2.id INNER JOIN "
                        + " spectrum s ON ve.spectrum_id = s.id"
                        + (whereOuter.isEmpty() ? "" : "WHERE " + whereOuter)
                        + " ORDER BY ";

                if (xi3db) {
                    querry = "SELECT pep1.sequence as peptide1, pep2.sequence as peptide2, s.elution_time_start, "
                            + " s.elution_time_end, sm.precursor_charge, prot1.accession_number  as accession_number1, prot2.accession_number  as accession_number2, "
                            + " xl.name AS crosslinker, xl.mass AS crosslinkermass, ss.name as run_name, s.scan_number, sm.score, "
                            + " mp1.link_position +1 AS link_position1, mp2.link_position +1 AS link_position2, hp1.peptide_position + 1 AS peptide_position1, hp2.peptide_position + 1 AS peptide_position2"
                            + " FROM "
                            + "  (SELECT * FROM spectrum_match WHERE " + whereInner + ") sm INNER JOIN "
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
                            + "  (SELECT id, mass, regexp_replace(regexp_replace(description,E'.*Name:',''),E';.*','') as name from crosslinker) xl on mp1.crosslinker_id =xl.id "
                            + " INNER JOIN "
                            + " spectrum s ON sm.spectrum_id = s.id "
                            + " INNER JOIN "
                            + " spectrum_source ss ON s.source_id = ss.id"
                            + (whereOuter.isEmpty() ? "" : " WHERE " + whereOuter + " ORDER BY sm.id");
                }
            }

            pw.println("file\tscan\tcharge\tsequence\tscore-type\tscore");
            HashSet<String> mods = new HashSet<String>();
            HashSet<String> xlSiteMods = new HashSet<String>();
            HashSet<String> xlMods = new HashSet<String>();

            if (flResults.getFiles().length > 0) {
                CsvMultiParser csvp = new CsvMultiParser(flResults.getFiles());
                csvp.open(true);
                csvp.setAlternative("peptide1", "sequence1");
                csvp.setAlternative("peptidcuurentlye1", "pep1");
                csvp.setAlternative("peptide1", "pepseq1");
                csvp.setAlternative("peptide2", "sequence2");
                csvp.setAlternative("peptide2", "pep2");
                csvp.setAlternative("peptide2", "pepseq2");
                csvp.setAlternative("elution_time_start", "rt start");
                csvp.setAlternative("elution_time_end", "rt end");
                csvp.setAlternative("precursor_charge", "charge");
                csvp.setAlternative("accession_number1", "accession1");
                csvp.setAlternative("accession_number1", "protein1");
                csvp.setAlternative("accession_number2", "accession2");
                csvp.setAlternative("accession_number2", "protein2");
                csvp.setAlternative("crosslinker", "xl");
                csvp.setAlternative("crosslinkermass", "xlmass");
                csvp.setAlternative("crosslinkermass", "crosslinker_mass");
                csvp.setAlternative("crosslinkermass", "crosslinker.mass");
                csvp.setAlternative("crosslinkermass", "CrosslinkerModMass");
                csvp.setAlternative("crosslinkermass", "crosslinkermodmass");
                csvp.setAlternative("run_name", "run");
                csvp.setAlternative("run_name", "raw");
                csvp.setAlternative("run_name", "raw file");
                csvp.setAlternative("scan_number", "scan");
                csvp.setAlternative("score", "match_score");
                csvp.setAlternative("score", "pvalue");
                csvp.setAlternative("link_position1", "link1");
                csvp.setAlternative("link_position1", "LinkPos1");
                csvp.setAlternative("link_position1", "from");
                csvp.setAlternative("link_position2", "link2");
                csvp.setAlternative("link_position2", "to");
                csvp.setAlternative("link_position2", "LinkPos2");
                csvp.setAlternative("peptide_position1", "pos1");
                csvp.setAlternative("peptide_position1", "peppos1");
                csvp.setAlternative("peptide_position1", "pep_pos1");
                csvp.setAlternative("peptide_position2", "pos2");
                csvp.setAlternative("peptide_position2", "peppos2");
                csvp.setAlternative("peptide_position2", "pep_pos2");
                rs = new CSVResultSet(csvp);
            } else {
                rs = st.executeQuery(querry);
            }

            while (rs.next()) {

                // get the match informations
                String peptide1 = rs.getString("peptide1");
                String peptide2 = rs.getString("peptide2");
                int charge = rs.getInt("precursor_charge");
//                String prot1 = rs.getString(6);
//                String prot2 = rs.getString(7);
                String xl = rs.getString("crosslinker");
                Double xlMass = rs.getDouble("crosslinkermass");
                
                // if we don't have a known name - check for a known mass
                if ((xl == null || xl.isEmpty() || !crosslinkerNames.contains(xl)) && xlMass != null) {
                    xl = Integer.toString((int)(xlMass*1000));
                }
                
                String run = rs.getString("run_name");
                int scan = rs.getInt("scan_number");
                double score = rs.getDouble("score");
//                int link1 = rs.getInt(12);
//                int link2 = rs.getInt(13);
                PepInfo pep1 = new PepInfo(peptide1, rs.getString("accession_number1"), rs.getInt("link_position1"), rs.getInt("peptide_position1"));
                PepInfo pep2 = null;
                if (rs.getString(2) != null) {
                    pep2 = new PepInfo(peptide2, rs.getString("accession_number2"), rs.getInt("link_position2"), rs.getInt("peptide_position2"));
                }

                if (pep2 != null || (!xlOnly) || (irtLinearsProtein != null && pep1.proteinAccesion.contains(irtLinearsProtein))) {

                    // potentially invert peptides
                    if (pep2 != null) {
                        int protComp = pep1.proteinAccesion.compareTo(pep2.proteinAccesion);
                        // same protein?
                        if (protComp == 0) {
                            int LinkPosComp = (pep1.peptideLinkPos + pep1.peptidePosition) - (pep2.peptideLinkPos + pep2.peptidePosition);
                            // also same linkage site        ?
                            if (LinkPosComp == 0) {
                                if (pep1.peptideSequence.compareTo(pep2.peptideSequence) > 0) {
                                    PepInfo dummy = pep1;
                                    pep1 = pep2;
                                    pep2 = dummy;
                                }
                            } else if (LinkPosComp > 0) {
                                PepInfo dummy = pep1;
                                pep1 = pep2;
                                pep2 = dummy;

                            }
                        } else if (protComp > 0) {
                            PepInfo dummy = pep1;
                            pep1 = pep2;
                            pep2 = dummy;
                        }
                    }

                    if (!run.contains(".")) {
                        run += ".mzML";
                    }

                    if (!run.endsWith(".raw")) {
                        run = run.substring(0, run.length() - 4) + "mzML";
                    }

                    // turn them into peptides
                    Sequence p1s = new Sequence(pep1.peptideSequence, conf);
                    Peptide p1 = new Peptide(p1s, 0, p1s.length());
                    StringBuffer base = new StringBuffer();
                    AminoAcid[] p1Seq = p1.toArray();

                    for (int aap = 0; aap < p1Seq.length; aap++) {

                        AminoAcid aa = p1Seq[aap];

                        AminoAcid baseaa = aa;
                        if (aa instanceof AminoModification) {
                            AminoModification am = (AminoModification) aa;
                            while (am.BaseAminoAcid instanceof AminoModification) {
                                am = (AminoModification) am.BaseAminoAcid;
                            }
                            baseaa = am.BaseAminoAcid;
                            base.append(baseaa);
                            //                        if (aap == )
                            base.append(massToSkylineMod(numberFormat, aa.mass - baseaa.mass));
                            mods.add(baseaa + massToSkylineMod(numberFormat, aa.mass - baseaa.mass));

                        } else {
                            base.append(aa);
                        }
                    }

                    if (pep2 != null) {
                        Sequence p2s = new Sequence(pep2.peptideSequence, conf);
                        Peptide p2 = new Peptide(p2s, 0, p2s.length());
                        base.append("-");

                        AminoAcid[] p2Seq = p2.toArray();

                        for (int aap = 0; aap < p2Seq.length; aap++) {

                            AminoAcid aa = p2Seq[aap];
                            double diff = 0;
                            AminoAcid baseaa = aa;
                            if (aa instanceof AminoModification) {
                                AminoModification am = (AminoModification) aa;
                                while (am.BaseAminoAcid instanceof AminoModification) {
                                    am = (AminoModification) am.BaseAminoAcid;
                                }
                                baseaa = am.BaseAminoAcid;
                                base.append(baseaa);
                                base.append(massToSkylineMod(numberFormat, aa.mass - baseaa.mass));
                                mods.add(baseaa + massToSkylineMod(numberFormat, aa.mass - baseaa.mass));

                            } else {
                                base.append(aa);
                            }
                        }
                        xlMods.add(crosslinkerMass.get(xl));
                        base.append("-[").append(crosslinkerMass.get(xl)).append("@");
                        base.append(pep1.peptideLinkPos);
                        base.append(",");
                        base.append(pep2.peptideLinkPos);
                        base.append("]");
                    }

                    pw.println(run + "\t" + scan + "\t" + charge + "\t" + base + "\tUNKNOWN\t" + numberFormat.format(score));
                    
                }

            }
            pw.flush();;
            pw.close();

            setStatus("finished");

            final JFrame w = new JFrame("Found Modifications");
            w.getContentPane().setLayout(new javax.swing.BoxLayout(w.getContentPane(), javax.swing.BoxLayout.Y_AXIS));

            JTextArea txtMods = new JTextArea("found Modifications: \n\t" + MyArrayUtils.toString(mods, "\n\t") + "\n"
                    + "crosslinker found : \n\t" + MyArrayUtils.toString(xlMods, "\n\t") + "\n"
                    + "These need to be defined in skyline");
            JScrollPane sp = new JScrollPane(txtMods);
            sp.setPreferredSize(new Dimension(200, 200));
            w.getContentPane().add(sp);
            JButton btnOK = new JButton("OK");
            w.getContentPane().add(btnOK);
            w.setPreferredSize(new Dimension(400, 400));
            w.pack();
            w.setVisible(true);
            btnOK.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    w.setVisible(false);
                    w.dispose();
                }
            });

        } catch (SQLException ex) {
            Logger.getLogger(Xi2Skyline.class.getName()).log(Level.SEVERE, null, ex);
            setStatus("Error : " + ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Xi2Skyline.class.getName()).log(Level.SEVERE, null, ex);
            setStatus("Error : " + ex);
        } catch (IOException ex) {
            Logger.getLogger(Xi2Skyline.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(Xi2Skyline.class.getName()).log(Level.SEVERE, null, ex);
            setStatus("error reading retention times - read data from database: " + ex);
        }

        HashMap<AminoAcid, Integer> modificationIds = new HashMap<AminoAcid, Integer>() {
            public Integer get(Object aa) {
                Integer i = super.get(aa);
                if (i == null) {
                    return 0;
                } else {
                    return i;
                }
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

            int varModCount = 0;
            HashMap<Double, Integer> massToID = new HashMap<Double, Integer>();
            HashMap<Integer, ArrayList<String>> idToModAA = new HashMap<Integer, ArrayList<String>>();
            HashMap<Integer, Double> idToModMass = new HashMap<Integer, Double>();

            // variable modifcations
            for (AminoModification am : conf.getVariableModifications()) {
                Double modmass = Math.round((am.mass - am.BaseAminoAcid.mass) * 10000.0) / 10000.0;
                Integer id = massToID.get(modmass);
                if (id == null) {
                    id = ++varModCount;
                    massToID.put(modmass, id);
                    ArrayList<String> modAA = new ArrayList<String>();
                    modAA.add(am.BaseAminoAcid.SequenceID);
                    idToModAA.put(id, modAA);
                    idToModMass.put(id, am.mass - am.BaseAminoAcid.mass);
                } else {
                    ArrayList<String> modAA = idToModAA.get(id);
                    modAA.add(am.BaseAminoAcid.SequenceID);
                }

                modificationIds.put(am, id);

            }
            // label
            for (AminoLabel al : conf.getLabel()) {
                Double modmass = Math.round((al.mass - al.BaseAminoAcid.mass) * 10000.0) / 10000.0;
                Integer id = massToID.get(modmass);
                if (id == null) {
                    id = ++varModCount;
                    massToID.put(modmass, id);
                    ArrayList<String> modAA = new ArrayList<String>();
                    modAA.add(al.BaseAminoAcid.SequenceID);
                    idToModAA.put(id, modAA);
                    idToModMass.put(id, al.mass - al.BaseAminoAcid.mass);
                } else {
                    ArrayList<String> modAA = idToModAA.get(id);
                    modAA.add(al.BaseAminoAcid.SequenceID);
                }

                modificationIds.put(al, id);
            }

            for (Integer id = 1; id <= varModCount; id++) {
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
                crosslinkersID.put(xl.getName(), id);
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
            if (ckAutoValidated.isSelected() && ckManual.isSelected()) {
                where += " AND ( autovalidated = true OR validated  in ('" + MyArrayUtils.toString(txtValidation.getText().split(","), "','") + "'))";
            } else if (ckAutoValidated.isSelected()) {
                where += " AND ( autovalidated = true )";
            } else if (ckManual.isSelected()) {
                where += " AND (validated in ('" + MyArrayUtils.toString(txtValidation.getText().split(","), "','") + "'))";
            }
            if (!ckDecoys.isSelected()) {
                where += " AND (!isDecoy)";
            }

            String querry = "SELECT ve.peptide1, ve.peptide2, s.elution_time_start, "
                    + " s.elution_time_end, ve.precursor_charge, p1.accession_number, p2.accession_number, "
                    + " ve.crosslinker, ve.run_name, ve.scan_number,p1.name, p2.name,p1.Description, p2.Description,  "
                    + " pep1_link_pos, pep2_link_pos, peptide_position1, peptide_position2"
                    + " FROM (SELECT * FROM v_export_materialized WHERE " + where + ") ve INNER JOIN "
                    + " protein p1 ON ve.display_protein1_id = p1.id LEFT OUTER JOIN "
                    + " protein p2 ON ve.display_protein2_id = p2.id INNER JOIN "
                    + " spectrum s ON ve.spectrum_id = s.id";

            boolean xi3db = false;
            try {
                ResultSet t = connection.createStatement().executeQuery("SELECT * FROM spectrum_source limit 1");
                t.close();
                xi3db = true;
            } catch (SQLException sex) {

            }
            if (xi3db) {
                querry = "SELECT pep1.sequence, pep2.sequence, s.elution_time_start, "
                        + " s.elution_time_end, sm.precursor_charge, prot1.accession_number, prot2.accession_number, "
                        + " xl.name, ss.name, s.scan_number, prot1.name, prot2.name,prot1.Description, prot2.Description,  "
                        + " mp1.link_position, mp2.link_position, hp1.peptide_position, hp2.peptide_position"
                        + " FROM "
                        + "  (SELECT * FROM spectrum_match WHERE " + where + ") sm INNER JOIN "
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
                        + " spectrum_source ss ON s.source_id = ss.id";
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
                Peptide p1 = new Peptide(p1s, 0, p1s.length());
                Sequence p2s = new Sequence(peptide2, conf);
                Peptide p2 = new Peptide(p2s, 0, p2s.length());

                // the concatinated sequence
                String base = p1.toStringBaseSequence() + "K" + p2.toStringBaseSequence();

                // get the modifications
                String mod = "00";
                for (int aaid = 0; aaid < p1.length(); aaid++) {
                    AminoAcid aa = p1.aminoAcidAt(aaid);
                    mod += modificationIds.get(aa);
                }
                // add the "faked" crosslinker
                mod += crosslinkersID.get(xl);
                // and modifications for the second peptide
                for (int aaid = 0; aaid < p2.length(); aaid++) {
                    AminoAcid aa = p2.aminoAcidAt(aaid);
                    mod += modificationIds.get(aa);
                }
                ets = rt.get(run, scan, ets) / factor;

                String id = cbLabel.getSelectedItem().toString();

                id = generateID(id, prot1acc, prot2acc, link1, link2, pepSite1, pepSite2, peptide1, peptide2, prot1name, prot2name, prot1desc, prot2desc, run);

                pw.println(base + "," + charge + "," + Util.sixDigits.format(ets) + "," + mod + ",," + id + "," + run + "," + scan);
                pw.println("0,0");

            }
            pw.flush();
            pw.close();

            setStatus("finished");

        } catch (SQLException ex) {
            Logger.getLogger(Xi2Skyline.class.getName()).log(Level.SEVERE, null, ex);
            setStatus("Error : " + ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Xi2Skyline.class.getName()).log(Level.SEVERE, null, ex);
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

        pPinpoint = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        cbRetentionTimeFactor = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        cbLabel = new javax.swing.JComboBox();
        lblIDExample = new javax.swing.JLabel();
        btnWrite = new javax.swing.JButton();
        txtStatus = new java.awt.TextField();
        tpTabs = new javax.swing.JTabbedPane();
        pDBInput = new javax.swing.JPanel();
        ckAutoValidated = new javax.swing.JCheckBox();
        ckManual = new javax.swing.JCheckBox();
        txtValidation = new javax.swing.JTextField();
        ckXLOnly = new javax.swing.JCheckBox();
        ckDecoys = new javax.swing.JCheckBox();
        ckAllLinearsOf = new javax.swing.JCheckBox();
        txtAllLinearsOf = new javax.swing.JTextField();
        getSearch = new rappsilber.gui.components.db.GetSearch();
        jPanel2 = new javax.swing.JPanel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        flResults = new rappsilber.gui.components.FileList();
        jPanel3 = new javax.swing.JPanel();
        fbConfigFile = new rappsilber.gui.components.FileBrowser();
        gsConfigSearch = new rappsilber.gui.components.db.GetSearch();
        lpNumberLocale = new rappsilber.gui.components.LocalPicker();
        jLabel4 = new javax.swing.JLabel();
        flPeakList = new rappsilber.gui.components.FileList();
        fbPinPoint = new rappsilber.gui.components.FileBrowser();
        jLabel1 = new javax.swing.JLabel();
        btnWriteSkyLine = new javax.swing.JButton();

        pPinpoint.setBorder(javax.swing.BorderFactory.createTitledBorder("PinPoint Options"));

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

        javax.swing.GroupLayout pPinpointLayout = new javax.swing.GroupLayout(pPinpoint);
        pPinpoint.setLayout(pPinpointLayout);
        pPinpointLayout.setHorizontalGroup(
            pPinpointLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pPinpointLayout.createSequentialGroup()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblIDExample))
            .addGroup(pPinpointLayout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbRetentionTimeFactor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pPinpointLayout.setVerticalGroup(
            pPinpointLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pPinpointLayout.createSequentialGroup()
                .addGroup(pPinpointLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbRetentionTimeFactor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(pPinpointLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(lblIDExample)))
        );

        btnWrite.setText("PinPoint");
        btnWrite.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnWriteActionPerformed(evt);
            }
        });

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        txtStatus.setEditable(false);

        ckAutoValidated.setText("Auto-validated");

        ckManual.setText("manual validation");
        ckManual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckManualActionPerformed(evt);
            }
        });

        txtValidation.setText("A");
        txtValidation.setEnabled(false);

        ckXLOnly.setText("Cross-link only");
        ckXLOnly.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckXLOnlyActionPerformed(evt);
            }
        });

        ckDecoys.setText("export decoys");

        ckAllLinearsOf.setText("All Linears of ");
        ckAllLinearsOf.setEnabled(false);
        ckAllLinearsOf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckAllLinearsOfActionPerformed(evt);
            }
        });

        txtAllLinearsOf.setText("IRT");
        txtAllLinearsOf.setEnabled(false);

        getSearch.setBorder(javax.swing.BorderFactory.createTitledBorder("Searches"));

        javax.swing.GroupLayout pDBInputLayout = new javax.swing.GroupLayout(pDBInput);
        pDBInput.setLayout(pDBInputLayout);
        pDBInputLayout.setHorizontalGroup(
            pDBInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pDBInputLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pDBInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(getSearch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pDBInputLayout.createSequentialGroup()
                        .addComponent(ckAutoValidated)
                        .addGap(18, 18, 18)
                        .addComponent(ckManual)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtValidation, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(ckXLOnly)
                        .addGap(18, 18, 18)
                        .addComponent(ckDecoys)
                        .addGap(18, 18, 18)
                        .addComponent(ckAllLinearsOf)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtAllLinearsOf, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 58, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pDBInputLayout.setVerticalGroup(
            pDBInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pDBInputLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(getSearch, javax.swing.GroupLayout.DEFAULT_SIZE, 291, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pDBInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(ckAutoValidated)
                    .addComponent(ckManual)
                    .addComponent(txtValidation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ckXLOnly)
                    .addComponent(ckDecoys)
                    .addComponent(ckAllLinearsOf)
                    .addComponent(txtAllLinearsOf, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        tpTabs.addTab("Read From DB", pDBInput);

        flResults.setDescription("CSV-Files");
        flResults.setExtensions(new String[] {".csv"});
        jTabbedPane2.addTab("CSV-Files", flResults);

        fbConfigFile.setDescription("Config Files");
        fbConfigFile.setExtensions(new String[] {"*.conf", "*.txt", "*.config"});

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fbConfigFile, javax.swing.GroupLayout.DEFAULT_SIZE, 852, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fbConfigFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Config from file", jPanel3);
        jTabbedPane2.addTab("Config from DB", gsConfigSearch);

        lpNumberLocale.setToolTipText("Defines how numbers are writen out to the result file");
        lpNumberLocale.setDefaultLocal(java.util.Locale.ENGLISH);

        jLabel4.setText("Number Format:");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane2)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lpNumberLocale, javax.swing.GroupLayout.PREFERRED_SIZE, 268, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lpNumberLocale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addContainerGap())
        );

        tpTabs.addTab("Read Results From File", jPanel2);

        flPeakList.setBorder(javax.swing.BorderFactory.createTitledBorder("read Retention times from:"));
        flPeakList.setDescription("peak-list (mgf,msm,apl)");
        flPeakList.setExtensions(new String[] {"mgf", "msm", "apl"});
        tpTabs.addTab("Retention Times", flPeakList);

        fbPinPoint.setDescription("Generic Library (txt,ssl)");
        fbPinPoint.setExtensions(new String[] {"txt", "ssl"});
        fbPinPoint.setLoad(false);

        jLabel1.setText("Library-File");

        btnWriteSkyLine.setText("SkyLine");
        btnWriteSkyLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnWriteSkyLineActionPerformed(evt);
            }
        });

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
                        .addComponent(btnWriteSkyLine))
                    .addComponent(tpTabs))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tpTabs)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnWriteSkyLine)
                    .addComponent(jLabel1)
                    .addComponent(fbPinPoint, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
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
                    Logger.getLogger(Xi2Skyline.class.getName()).log(Level.SEVERE, "Error:", e);
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
        id = generateID(id, "P000001", "P000002", 7, 5, 48, 84, "KSNLGR", "ILYKOR", "SAMP1", "SAMP2", "First sample protein", "Second sample protein", "MyRawFile");
        lblIDExample.setText(id);
    }//GEN-LAST:event_cbLabelActionPerformed

    private void ckAllLinearsOfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckAllLinearsOfActionPerformed
        txtAllLinearsOf.setEnabled(ckAllLinearsOf.isSelected());
    }//GEN-LAST:event_ckAllLinearsOfActionPerformed

    private void ckXLOnlyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckXLOnlyActionPerformed
        ckAllLinearsOf.setEnabled(ckXLOnly.isSelected());
        txtAllLinearsOf.setEnabled(ckAllLinearsOf.isSelected() && ckXLOnly.isSelected());
    }//GEN-LAST:event_ckXLOnlyActionPerformed

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
            java.util.logging.Logger.getLogger(Xi2Skyline.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Xi2Skyline.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Xi2Skyline.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Xi2Skyline.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Xi2Skyline().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnWrite;
    private javax.swing.JButton btnWriteSkyLine;
    private javax.swing.JComboBox cbLabel;
    private javax.swing.JComboBox cbRetentionTimeFactor;
    private javax.swing.JCheckBox ckAllLinearsOf;
    private javax.swing.JCheckBox ckAutoValidated;
    private javax.swing.JCheckBox ckDecoys;
    private javax.swing.JCheckBox ckManual;
    private javax.swing.JCheckBox ckXLOnly;
    private rappsilber.gui.components.FileBrowser fbConfigFile;
    private rappsilber.gui.components.FileBrowser fbPinPoint;
    private rappsilber.gui.components.FileList flPeakList;
    private rappsilber.gui.components.FileList flResults;
    private rappsilber.gui.components.db.GetSearch getSearch;
    private rappsilber.gui.components.db.GetSearch gsConfigSearch;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JLabel lblIDExample;
    private rappsilber.gui.components.LocalPicker lpNumberLocale;
    private javax.swing.JPanel pDBInput;
    private javax.swing.JPanel pPinpoint;
    private javax.swing.JTabbedPane tpTabs;
    private javax.swing.JTextField txtAllLinearsOf;
    private java.awt.TextField txtStatus;
    private javax.swing.JTextField txtValidation;
    // End of variables declaration//GEN-END:variables
}
