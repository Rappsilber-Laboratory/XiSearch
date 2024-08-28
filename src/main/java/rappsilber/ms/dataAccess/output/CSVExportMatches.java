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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
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
import rappsilber.utils.MyArrayUtils;
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
    private String delimChar = ",";
    /** this gets set to true if the delimChar is explicitly set */
    private boolean delimCharSet = false;
    private String quoteChar = "\"";
    private boolean quoteDoubles=false;
    private String localNumberGroupingSeperator;
    private String localNumberDecimalSeparator;
    private NumberFormat numberFormat;
    private Locale locale=Locale.ENGLISH;

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
        } else {
            m_out = new PrintStream(out);
        }
        m_config = config;
        setLocale(Locale.getDefault());
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    finished();
                }catch (Exception e) {}
            }
        });
    }

    
    public boolean setLocale(String locale) {
        Locale l = Util.getLocale(locale);
        if (l == null) {
            return false;
        }
        setLocale(l);
        return true;
    }
    
    public void setLocale(Locale locale) {
        this.locale = locale;
        numberFormat = NumberFormat.getNumberInstance(locale);
        DecimalFormat fformat  = (DecimalFormat) numberFormat;
        fformat.setGroupingUsed(false);
        DecimalFormatSymbols symbols=fformat.getDecimalFormatSymbols();
        fformat.setMaximumFractionDigits(6);
        localNumberGroupingSeperator= ""+symbols.getGroupingSeparator();
        localNumberDecimalSeparator= ""+symbols.getDecimalSeparator();
        quoteDoubles = localNumberDecimalSeparator.contentEquals(delimChar);
    }

    


    /**
     * @return the delimChar
     */
    public String getDelimChar() {
        return delimChar;
    }

    /**
     * @param delimChar the delimChar to set
     */
    public void setDelimChar(String delimChar) {
        this.delimChar = delimChar;
        
            quoteDoubles = localNumberDecimalSeparator.contentEquals(delimChar);
        delimCharSet=true;
    }

    /**
     * @return the quoteChar
     */
    public String getQuoteChar() {
        return quoteChar;
    }

    /**
     * @param quoteChar the quoteChar to set
     */
    public void setQuoteChar(String quoteChar) {
        this.quoteChar = quoteChar;
    }
    
    private String scanHeader() {
        return "Run"+ delimChar + "Scan" + delimChar + "ScanTitle" + delimChar + 
                "peakListFileName"+ delimChar + "ScanId"+ delimChar + 
                "Source" + delimChar + "ElutionStart"+ delimChar + 
                "ElutionEnd"+ delimChar + "PrecursorMass"+ delimChar + 
                "PrecoursorCharge"+ delimChar + "PrecurserMZ"+ delimChar + 
                "PrecurserIntensity"+ delimChar + "CalcMass"+ delimChar + 
                "CalcMZ"+ delimChar + "validated"+ delimChar + 
                "decoy"+ delimChar + "MatchRank";
    }

    private String peptideHeader(int PeptideNumber) {
        PeptideNumber++;
        String ret = delimChar +"Protein"+ PeptideNumber +
                delimChar +"Name"+ PeptideNumber +
                delimChar +"Description"+ PeptideNumber +
                delimChar +"Fasta"+ PeptideNumber +
                delimChar +"Protein"+ PeptideNumber + "decoy" +
                delimChar +"Peptide" + PeptideNumber +
                delimChar +"BasePeptide" + PeptideNumber +
                delimChar +"PeptideLinkMap" + PeptideNumber +
                delimChar +"PeptideMass" + PeptideNumber +
                delimChar +"PeptideWeight" + PeptideNumber +
                delimChar +"Start" + PeptideNumber +
                delimChar +"LengthPeptide" + PeptideNumber +
                delimChar +"Link" + PeptideNumber +
                delimChar +"Linked AminoAcid " + PeptideNumber +
                delimChar +"LinkWindow" + PeptideNumber +
                delimChar +"ProteinLink" + PeptideNumber +
                delimChar +"ProteinCount" + PeptideNumber +
                delimChar +"PositionCount" + PeptideNumber +
                delimChar +"Modifications" + PeptideNumber +
                delimChar +"ModificationPositions" + PeptideNumber +
                delimChar +"ModificationMasses" + PeptideNumber +
                delimChar +"OpenModPosition" + PeptideNumber +
                delimChar +"OpenMass" + PeptideNumber +
                delimChar +"OpenModWindow" + PeptideNumber;
                
        return ret;
    }

    private String crosslinkerHeader() {
        String header = delimChar +"Crosslinker"+ delimChar + "CrosslinkerMass"+ delimChar + " decoyCrosslinker";
        return header;
    }

    private String scoreHeader() {
        String header = "";
        Collection<ScoreSpectraMatch> scores = m_config.getScores();
        for (ScoreSpectraMatch score : scores) {
            for (String name : score.scoreNames() ) {
                header += delimChar + name;
            }
        }
        return header;
    }

    private String d2s(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return Double.toString(d);
        }
        if (quoteDoubles) {
            return quoteChar + numberFormat.format(d) + quoteChar;
        } else {
            return numberFormat.format(d);
        }
    }
    
    private String i2s(int i) {
        return numberFormat.format(i);
    }
    
    private String scanValues(MatchedXlinkedPeptide match) {
        Spectra s = match.getSpectrum();
        try {
            double calcMass = match.getPeptides()[0].getMass();
            if (match.getPeptide(1) != null) {
                calcMass += match.getPeptides()[1].getMass() + match.getCrosslinker().getCrossLinkedMass();
            }
            double calcMZ = calcMass / s.getPrecurserCharge() + Util.PROTON_MASS;

            return quoteChar + s.getRun().replace(quoteChar, " ") + quoteChar + delimChar + 
                    i2s(s.getScanNumber()) + delimChar  + 
                    quoteChar + s.getScanTitle().replace(quoteChar, " ") + quoteChar + delimChar + 
                    quoteChar + s.getPeakFileName().replace(quoteChar, quoteChar+quoteChar) + quoteChar + delimChar  + 
                    i2s(s.getReadID()) + delimChar + quoteChar + s.getSource() + quoteChar + delimChar  +
                    d2s(s.getElutionTimeStart()) + delimChar + 
                    d2s(s.getElutionTimeEnd()) + delimChar +
                    d2s(s.getPrecurserMass()) + delimChar + 
                    s.getPrecurserCharge() + delimChar +
                    d2s(s.getPrecurserMZ()) + delimChar + 
                    d2s(s.getPrecurserIntensity()) + delimChar + 
                    d2s(calcMass) + delimChar + d2s(calcMZ) + delimChar + 
                    match.isValidated() + delimChar + 
                    (match.isDecoy()?"1":"0") + delimChar + match.getMatchrank();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private String CrosslinkerValues(MatchedXlinkedPeptide match) {
        CrossLinker cl = match.getCrosslinker();
        if (cl == null) {
            return MyArrayUtils.toString(Collections.nCopies(3, delimChar), "");
        } else {
            return delimChar+quoteChar + cl.getName().replace(quoteChar, " ") + quoteChar + delimChar +
                    d2s(cl.getCrossLinkedMass()) + delimChar + (match.getCrosslinker().isDecoy()?"1":"0");
        }
    }

    private String peptideValues(MatchedXlinkedPeptide match, int PeptideNumber) {
        Peptide[]  peps = match.getPeptides();

        if (peps.length > PeptideNumber) {
            double[] weights = null;
            if (match instanceof MatchedXlinkedPeptideWeighted) {
                weights = ((MatchedXlinkedPeptideWeighted) match).getLinkageWeights(PeptideNumber);
            } else {
                weights = new double[peps[PeptideNumber].length()];
                java.util.Arrays.fill(weights, 0);
            }
            Peptide p = match.getPeptides()[PeptideNumber];
            int ipepLinkSite = (match.getLinkingSite(PeptideNumber));
            Peptide.PeptidePositions[] pps = p.getPositions();
            StringBuilder sbAccessions= new  StringBuilder();
            StringBuilder sbNames = new  StringBuilder();
            StringBuilder sbDescription = new  StringBuilder();
            StringBuilder sbFasta = new  StringBuilder();
            StringBuilder sbPepStart = new  StringBuilder();
            StringBuilder sbProtLink = new  StringBuilder();
            for (Peptide.PeptidePositions pp : pps) {
                FastaHeader fh=pp.base.getSplitFastaHeader();
                String name = fh.isSplit() ? pp.base.getSplitFastaHeader().getName(): "";
                if (name != null) {
                    name = name.replace(quoteChar, " ").replace(";", " ");
                } else {
                    name = "";
                }
                sbAccessions.append(pp.base.getSplitFastaHeader().getAccession().replace(quoteChar, " ").replace(";", " ")).append(";");
                sbNames.append(name).append(";");
                sbDescription.append((fh.isSplit() ? fh.getDescription().replace(quoteChar, " ").replace(";", " ") : "")).append(";");
                sbFasta.append(fh.getHeader().replace(quoteChar, " ").replace(";", " ")).append(";");
                sbPepStart.append(i2s(pp.start+1)).append(";");
                sbProtLink.append(i2s(pp.start+ipepLinkSite+1)).append(";");
            }
            //FastaHeader fh = p.getSequence().getSplitFastaHeader();
            String accession = sbAccessions.substring(0,sbAccessions.length()-1);
            String name = sbNames.substring(0,sbNames.length()-1);
            String description = sbDescription.substring(0,sbDescription.length()-1);
            String fasta = sbFasta.substring(0,sbFasta.length()-1);
            String decoy = (p.getSequence().isDecoy() ? "1" : "0");
            String pepsequence = p.toString();
            String pepBaseSequence = p.toStringBaseSequence();
            String pepWeightedSequence = p.toString(weights);
            String pepMass = d2s(p.getMass()) ;
            String pepStart = sbPepStart.substring(0, sbPepStart.length()-1);
            String pepLength = Integer.toString(p.length());
            String pepLinkSite = ipepLinkSite < 0 ? "" : i2s(ipepLinkSite + 1);
            String linkedAAA = ipepLinkSite < 0 ? "" : p.aminoAcidAt(ipepLinkSite).toString();
            String linkWindow = ipepLinkSite < 0 ? "" : sequenceWindow(p, ipepLinkSite, 20);
            String protLinkSite = ipepLinkSite < 0 ? "" : sbProtLink.substring(0, sbProtLink.length()-1).replace(localNumberGroupingSeperator, "");
            if (pps.length >1) {
                pepStart="\""+pepStart+"\"";
                protLinkSite="\""+protLinkSite+"\"";
            }
            String protCount =  i2s(p.getProteinCount());
            String siteCounts = i2s(p.getPositions().length);
            String pepWeight = "";
            if (PeptideNumber == 0) {
                pepWeight = ""+d2s(match.getPeptide1Weight());
            }
            if (PeptideNumber == 1) {
                pepWeight = ""+d2s(match.getPeptide2Weight());
            }
            if (p.isNTerminal() || p instanceof NonProteinPeptide) {
                pepsequence = "-." + pepsequence;
            } else {
                StringBuilder sb = new StringBuilder();
                HashSet<String> found = new HashSet<String>(pps.length);
                for (Peptide.PeptidePositions pp : pps) {
                    String aa = "-";
                    if (pp.start >0) {
                        aa = pp.base.aminoAcidAt(pp.start-1).toString();
                    }
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

            if (p.isCTerminal() || p instanceof NonProteinPeptide) {
                pepsequence += ".-";
            } else {
                StringBuilder sb = new StringBuilder();
                HashSet<String> found = new HashSet<String>(pps.length);
                for (Peptide.PeptidePositions pp : pps) {
                    String aa = "-";
                    if (pp.base.length() > pp.start + p.length()) {
                        aa = pp.base.aminoAcidAt(pp.start+p.length()).toString();
                    }
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
            
            
            StringBuilder s = new StringBuilder(delimChar+quoteChar + accession.replace(quoteChar, " ") + quoteChar +
                    delimChar+quoteChar + name.replace(quoteChar, " ") + quoteChar +
                    delimChar+quoteChar + description.replace(quoteChar, " ") + quoteChar +
                    delimChar+quoteChar + fasta.replace(quoteChar, " ") + quoteChar +
                    delimChar + decoy +
                    delimChar+quoteChar + pepsequence + quoteChar +
                    delimChar+quoteChar + pepBaseSequence + quoteChar +
                    delimChar+quoteChar + pepWeightedSequence + quoteChar +
                    delimChar + pepMass +
                    delimChar + pepWeight +
                    delimChar + pepStart +
                    delimChar + pepLength +
                    delimChar + pepLinkSite +
                    delimChar+quoteChar + linkedAAA + quoteChar +
                    delimChar+quoteChar + linkWindow + quoteChar +
                    delimChar + protLinkSite +
                    delimChar + protCount +
                    delimChar + siteCounts + delimChar);
            HashMap<Integer,AminoAcid> mods = p.getModification();
            if (mods.size() == 0) {
                s.append(MyArrayUtils.toString(Collections.nCopies(5, delimChar), ""));
            } else {
                StringBuilder m = new StringBuilder();
                StringBuilder mp = new StringBuilder();
                StringBuilder mm = new StringBuilder();
                double om_mass = 0;
                int om_pos = 0;
                m.append(quoteChar);
                int modCount = 0;
                for (Integer i : mods.keySet() ) {
                    if (mods.get(i) instanceof AminoModification) {
                        modCount++;
                        AminoModification mod = (AminoModification) mods.get(i);
                        m.append(mod.SequenceID + ";");
                        mp.append((i+1) + ";");
                        mm.append(mod.weightDiff + ";");
                        if (mod.SequenceID.contains("_om")) {
                            om_mass = mod.weightDiff;
                            om_pos = i;
                        }
                    } else {
                        // replacement of some kind
                        AminoAcid aaReplacement  = mods.get(i);
                        AminoAcid aaOrig = p.getSourceSequence().aminoAcidAt(p.getStart()+i);
                        m.append(aaOrig + "->"+aaReplacement+" ;");
                        mp.append((i+1) + ";");
                        mm.append((aaReplacement.mass - aaOrig.mass) + ";");
                    }
                }
                
                s.append(m.substring(0, m.length() - 1));
                s.append(quoteChar + delimChar);
                String smp =mp.substring(0, mp.length() - 1);
                if (smp.contains(delimChar)) {
                    s.append(quoteChar);
                }
                s.append(smp);
                if (smp.contains(delimChar)) {
                    s.append(quoteChar);
                }
                s.append(delimChar);
                if (smp.contains(delimChar)) {
                    s.append(quoteChar);
                }
                s.append(mm.substring(0, mm.length() - 1));
                if (smp.contains(delimChar)) {
                    s.append(quoteChar);
                }
                if (om_mass == 0) {
                    s.append(MyArrayUtils.toString(Collections.nCopies(3, delimChar), ""));
                } else {
                    s.append(delimChar + i2s(om_pos+1));
                    s.append("" + d2s(om_mass));
                    s.append(delimChar + sequenceWindow(p, om_pos, 20));
                }

            }


//        return ",Protein"+ PeptideNumber +
//                ",Peptide" + PeptideNumber +
//                ",Start" + PeptideNumber +
//                ",LengthPeptide" + PeptideNumber +
//                ",Link" + PeptideNumber +
//                ",ProteinLink" + PeptideNumber;
            return s.toString();
        } else {
            return delimChar +
                    MyArrayUtils.toString(Collections.nCopies(2, delimChar), "") +
                    delimChar +
                    delimChar +
                    delimChar +
                    delimChar +
                    delimChar +
                    delimChar +
                    ",,,,,,,,,,,,,,,".replace(",", ""+delimChar)
                    ;
        }
    }

    private String scoreValues(MatchedXlinkedPeptide match) {
        String line = "";
        Collection<ScoreSpectraMatch> scores = m_config.getScores();
        for (ScoreSpectraMatch score : scores) {
            for (String name : score.scoreNames() ) {
                line += delimChar + d2s(match.getScore(name));
            }
        }
        return line;
    }



    public void writeHeader() {
        if ((!delimCharSet) && delimChar.contentEquals(",") && localNumberDecimalSeparator.contentEquals(delimChar) ) {
            setDelimChar(";");
        }
        StringBuffer Header = new StringBuffer();
        Header.append(scanHeader());
        Header.append(crosslinkerHeader());
        for (int i = 0; i < m_config.getMaxCrosslinkedPeptides(); i++) {
            Header.append(peptideHeader(i));
        }

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
        for (int i = 0; i < m_config.getMaxCrosslinkedPeptides(); i++) {
            line.append(peptideValues(match, i));
        }
        line.append(scoreValues(match));

        m_out.println(line);
        m_resultCount++;
        if (match.getMatchrank() == 1) {
            m_topResultCount++;
        }
        if (m_doFreeMatch) {
            match.free();
        }

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
            for (int l = from; l < 0; l++) {
                sb.append('.');
            }
            from = 0;
        }
        
        if (to >= s.length()) {
            for (int l = s.length(); l<=to;l++ ) {
                end.append('.');
            }
            to = s.length() - 1;
        }
        
        for (int l = from ; l<= to ; l++) {
            AminoAcid aa = s.aminoAcidAt(l);
            if (aa instanceof AminoModification) {
                sb.append(((AminoModification)aa).BaseAminoAcid.SequenceID);
            } else {
                sb.append(aa.SequenceID);
            }
        }
        sb.append(end);
        return sb.toString();
    }

    public void flush() {
        m_out.flush();
    }

}
