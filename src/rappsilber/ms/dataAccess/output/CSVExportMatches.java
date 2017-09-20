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
package rappsilber.ms.dataAccess.output;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.GZIPOutputStream;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.score.ScoreSpectraMatch;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.NonProteinPeptide;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.fasta.FastaHeader;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptideWeighted;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CSVExportMatches extends AbstractResultWriter implements ResultWriter{
    public RunConfig m_config;
    /** the stream used to write something */
    PrintStream m_out;
    private int m_resultCount=0;
    private int m_topResultCount=0;
    private boolean m_isOpenModification = false;
    private boolean gziped = false;

    /**
     * create a new class and connect it to the given output stream
     * @param out where to output the data
     */
    public CSVExportMatches(OutputStream out, RunConfig config) throws IOException {
        this(out, config, false);
    }

    /**
     * create a new class and connect it to the given output stream
     * @param out where to output the data
     */
    public CSVExportMatches(OutputStream out, RunConfig config, boolean gziped) throws IOException {
        this.gziped=gziped;
        if (gziped) {
            m_out = new PrintStream(new GZIPOutputStream(out));
        } else
            m_out = new PrintStream(out);
        m_config = config;
    }
    
    private String scanHeader() {
        return "Run,Scan,Source,ElutionStart,ElutionEnd,PrecursorMass,PrecoursorCharge,PrecurserMZ,CalcMass,CalcMZ,validated,decoy,MatchRank";
    }

    private String peptideHeader(int PeptideNumber) {
        PeptideNumber++;
        String ret = ",Protein"+ PeptideNumber +
                ",Fasta"+ PeptideNumber +
                ",Protein"+ PeptideNumber + "decoy" +
                ",Peptide" + PeptideNumber +
                ",BasePeptide" + PeptideNumber +
                ",PeptideLinkMap" + PeptideNumber +
                ",PeptideMass" + PeptideNumber +
                ",Start" + PeptideNumber +
                ",LengthPeptide" + PeptideNumber +
                ",Link" + PeptideNumber +
                ",Linked AminoAcid " + PeptideNumber +
                ",LinkWindow" + PeptideNumber +
                ",ProteinLink" + PeptideNumber +
                ",ProteinCount" + PeptideNumber +
                ",PositionCount" + PeptideNumber +
                ",Modifications" + PeptideNumber +
                ",ModificationPositions" + PeptideNumber +
                ",ModificationMasses" + PeptideNumber +
                ",OpenModPosition" + PeptideNumber +
                ",OpenMass" + PeptideNumber +
                ",OpenModWindow" + PeptideNumber;
                
        return ret;
    }

    private String crosslinkerHeader() {
        String header = ",Crosslinker,CrosslinkerMass, decoyCrosslinker";
        return header;
    }

    private String scoreHeader() {
        String header = "";
        Collection<ScoreSpectraMatch> scores = m_config.getScores();
        for (ScoreSpectraMatch score : scores) {
            for (String name : score.scoreNames() )
                header += "," + name;
        }
        return header;
    }

    private String scanValues(MatchedXlinkedPeptide match) {
        Spectra s = match.getSpectrum();
        try {
            double calcMass = match.getPeptides()[0].getMass();
            if (match.getPeptide(1) != null)
                calcMass += match.getPeptides()[1].getMass() + match.getCrosslinker().getCrossLinkedMass();
            double calcMZ = calcMass / s.getPrecurserCharge() + Util.PROTON_MASS;

            return "\"" + s.getRun() + "\"," + s.getScanNumber() + "," + s.getSource() + ","  +
                    s.getElutionTimeStart() + "," + s.getElutionTimeEnd() + "," +
                    s.getPrecurserMass() + "," + s.getPrecurserCharge() + "," +
                    s.getPrecurserMZ() + "," + calcMass + "," + calcMZ + "," + match.isValidated() + "," + (match.isDecoy()?"1":"0") + "," + match.getMatchrank();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private String CrosslinkerValues(MatchedXlinkedPeptide match) {
        CrossLinker cl = match.getCrosslinker();
        if (cl == null)
            return ",,,";
        else
            return ",\"" + cl.getName() + "\"," +
                    cl.getCrossLinkedMass() + "," + (match.getCrosslinker().isDecoy()?"1":"0");
    }

    private String peptideValues(MatchedXlinkedPeptide match, int PeptideNumber) {
        Peptide[]  peps = match.getPeptides();

        if (peps.length > PeptideNumber) {
            double[] weights = null;
            if (match instanceof MatchedXlinkedPeptideWeighted)
                weights = ((MatchedXlinkedPeptideWeighted) match).getLinkageWeights(PeptideNumber);
            else {
                weights = new double[peps[PeptideNumber].length()];
                java.util.Arrays.fill(weights, 0);
            }            
            Peptide p = match.getPeptides()[PeptideNumber];
            FastaHeader fh = p.getSequence().getSplitFastaHeader();
            String accession = fh.getAccession().replace("\"", "'");
            String description = (fh.isSplit() ? fh.getDescription().replace("\"", "'") : "");
            String decoy = (p.getSequence().isDecoy() ? "1" : "0");
            String pepsequence = p.toString();
            String pepBaseSequence = p.toStringBaseSequence();
            String pepWeightedSequence = p.toString(weights);
            String pepMass = Double.toString(p.getMass()).replace(",", ",") ;
            String pepStart = Integer.toString(p.getStart()+1).replace(",", "");
            String pepLength = Integer.toString(p.length());
            int ipepLinkSite = (match.getLinkingSite(PeptideNumber) + 1);
            String pepLinkSite = ipepLinkSite < 1 ? "" : Integer.toString(ipepLinkSite).replaceAll(",", "");
            String linkedAAA = ipepLinkSite < 1 ? "" : p.aminoAcidAt(match.getLinkingSite(PeptideNumber)).toString();
            String linkWindow = ipepLinkSite < 1 ? "" : sequenceWindow(p, match.getLinkingSite(PeptideNumber), 20);
            String protLinkSite = ipepLinkSite < 1 ? "" : Integer.toString((p.getStart() + match.getLinkingSite(PeptideNumber) + 1)).replace(",", "");
            String protCount =  Integer.toString(p.getProteinCount());
            String siteCounts = Integer.toString(p.getPositions().length);
            if (p.isNTerminal() || p instanceof NonProteinPeptide)
                pepsequence = "-." + pepsequence;
            else {
                StringBuilder sb = new StringBuilder();
                Peptide.PeptidePositions[] pps = p.getPositions();
                HashSet<String> found = new HashSet<String>(pps.length);
                for (Peptide.PeptidePositions pp : pps) {
                    String aa = "-";
                    if (pp.start >0)
                        aa = pp.base.aminoAcidAt(pp.start-1).toString();
                    if (!found.contains(aa)) {
                        sb.append(aa);
                        found.add(aa);
                    }
                }
                if (found.size() >1) {
                    pepsequence = "[" + sb + "]." + pepsequence;
                } else {
                    pepsequence = sb + "." + pepsequence;
                }
                
            }

            if (p.isCTerminal() || p instanceof NonProteinPeptide)
                pepsequence += ".-";
            else {
                StringBuilder sb = new StringBuilder();
                Peptide.PeptidePositions[] pps = p.getPositions();
                HashSet<String> found = new HashSet<String>(pps.length);
                for (Peptide.PeptidePositions pp : pps) {
                    String aa = "-";
                    if (pp.base.length() > pp.start + p.length())
                        aa = pp.base.aminoAcidAt(pp.start+p.length()).toString();
                    if (!found.contains(aa)) {
                        sb.append(aa);
                        found.add(aa);
                    }
                }
                if (found.size() >1) {
                    pepsequence += ".[" + sb +"]";
                } else {
                    pepsequence += "." + sb +"";
                }
            }
            
            
            StringBuilder s = new StringBuilder(",\"" + accession + "\"" +
                    ",\"" + description + "\"" +
                    "," + decoy +
                    ",\"" + pepsequence + "\"" +
                    ",\"" + pepBaseSequence + "\"" +
                    ",\"" + pepWeightedSequence + "\"" +
                    "," + pepMass +
                    "," + pepStart +
                    "," + pepLength +
                    "," + pepLinkSite +
                    ",\"" + linkedAAA + "\"" +
                    ",\"" + linkWindow + "\"" +
                    "," + protLinkSite +
                    "," + protCount +
                    "," + siteCounts + ",");
            HashMap<Integer,AminoAcid> mods = p.getModification();
            if (mods.size() == 0) {
                s.append(",,,,,");
            } else {
                StringBuilder m = new StringBuilder();
                StringBuilder mp = new StringBuilder();
                StringBuilder mm = new StringBuilder();
                double om_mass = 0;
                int om_pos = 0;
                m.append("\"");
                for (Integer i : mods.keySet() ) {
                    if (mods.get(i) instanceof AminoModification) {
                        AminoModification mod = (AminoModification) mods.get(i);
                        m.append(mod.SequenceID + ";");
                        mp.append((i+1) + ";");
                        mm.append(mod.weightDiff + ";");
                        if (mod.SequenceID.contains("_om")) {
                            om_mass = mod.weightDiff;
                            om_pos = i;
                        }
                    }
                }
                
                s.append(m.substring(0, m.length() - 1));
                s.append("\",");
                s.append(mp.substring(0, mp.length() - 1));
                s.append(",");
                s.append(mm.substring(0, mm.length() - 1));
                if (om_mass == 0)
                    s.append(",,,");
                else {
                    s.append("," + (om_pos+1));
                    s.append("" + Double.toString(om_mass).replace(",", ""));
                    s.append("," + sequenceWindow(p, om_pos, 20));
                }

            }


//        return ",Protein"+ PeptideNumber +
//                ",Peptide" + PeptideNumber +
//                ",Start" + PeptideNumber +
//                ",LengthPeptide" + PeptideNumber +
//                ",Link" + PeptideNumber +
//                ",ProteinLink" + PeptideNumber;
            return s.toString();
        } else
            return "," +
                    ",," +
                    "," +
                    "," +
                    "," +
                    "," +
                    "," +
                    ",,,,,,,,,,,,,"
                    ;
    }

    private String scoreValues(MatchedXlinkedPeptide match) {
        String line = "";
        Collection<ScoreSpectraMatch> scores = m_config.getScores();
        for (ScoreSpectraMatch score : scores) {
            for (String name : score.scoreNames() )
                line += "," + match.getScore(name);
        }
        return line;
    }



    public void writeHeader() {
        StringBuffer Header = new StringBuffer();
        Header.append(scanHeader());
        Header.append(crosslinkerHeader());
        for (int i = 0; i < m_config.getMaxCrosslinkedPeptides(); i++)
            Header.append(peptideHeader(i));

        Header.append(scoreHeader());

        m_out.println(Header);
    }

    public void writeResult(MatchedXlinkedPeptide match) {
//        Spectra s= match.getSpectra();
//        if (s == null)
//            System.out.println("found it");
        StringBuffer line = new StringBuffer();
        line.append(scanValues(match));
        line.append(CrosslinkerValues(match));
        for (int i = 0; i < m_config.getMaxCrosslinkedPeptides(); i++)
            line.append(peptideValues(match, i));
        line.append(scoreValues(match));

        m_out.println(line);
        m_resultCount++;
        if (match.getMatchrank() == 1)
            m_topResultCount++;
        if (m_doFreeMatch)
            match.free();

    }

    @Override
    public void finished() {
        m_out.close();
        super.finished();
    }

    @Override
    public int getResultCount() {
        return m_resultCount;
    }

    @Override
    public int getTopResultCount() {
        return m_topResultCount;
    }

    private String sequenceWindow(Peptide p, int pos, int window) {
        StringBuilder sb = new StringBuilder();
        StringBuilder end = new StringBuilder();
        Sequence s = p.getSequence();
        int sp = p.getStart() + pos;
        int from = sp - window;
        int to = sp + window;
        if (from <0) {
            for (int l = from; l < 0; l++)
                sb.append('.');
            from = 0;
        }
        
        if (to >= s.length()) {
            for (int l = s.length(); l<=to;l++ )
                end.append('.');
            to = s.length() - 1;
        }
        
        for (int l = from ; l<= to ; l++) {
            AminoAcid aa = s.aminoAcidAt(l);
            if (aa instanceof AminoModification)
                sb.append(((AminoModification)aa).BaseAminoAcid.SequenceID);
            else
                sb.append(aa.SequenceID);
        }
        sb.append(end);
        return sb.toString();
    }

    public void flush() {
        m_out.flush();
    }

}
