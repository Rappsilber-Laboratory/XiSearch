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
package rappsilber.ms.sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import rappsilber.config.AbstractRunConfig;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.lookup.peptides.PeptideLookup;
import rappsilber.ms.sequence.digest.Digestion;
import rappsilber.ms.sequence.fasta.FastaFile;
import rappsilber.ms.sequence.fasta.FastaHeader;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class Sequence implements AminoAcidSequence{
    public static final Sequence EMPTY_SEQUENCE = new Sequence(new AminoAcid[0]);
    public static final Peptide EMPTY_PEPTIDE = new Peptide(EMPTY_SEQUENCE, 0, 0);
    
    public static Pattern m_sequenceSplitXmod = Pattern.compile("[A-Z][^A-Z]*");
    public static Pattern m_sequenceSplitModX = Pattern.compile("[^A-Z]*[A-Z]");
    private static final Pattern m_expected_mod_pattern = Pattern.compile("[A-Z]\\((([^A-Z]+\\|)*[^A-Z]+\\|?)\\)");
    private static final Pattern m_expected_mod_patternModX = Pattern.compile("\\((([^A-Z]+\\|)*[^A-Z]+\\|?)\\)[A-Z]");
    private static final Pattern m_custom_mod_pattern = Pattern.compile("([^=]*=)?\\[([^\\]]+)\\]");

    //public char[] m_sequence;
    
    private TreeMap<Integer,ArrayList<AminoAcid>> m_expected_Modifications = new TreeMap<Integer, ArrayList<AminoAcid>>();
    

    private ArrayList<Peptide> m_peptides = null;
    private String m_FastaHeader = null;
    private FastaHeader m_SplittFastaHeader = null;
    private double m_weight = Util.WATER_MASS;
    private boolean m_isDecoy = false;

    /** a dummy sequence, that all peptides get assigned to, that appear in to many places (ambiguous peptides)  */
    public static final Sequence UNSPECIFIC_SEQUENCE = new Sequence("", "___AMBIGUOUS___",AbstractRunConfig.DUMMYCONFIG);

    /**
     * what file defined the sequence
     */
    private FastaFile m_source;

    private long m_id = -1;
    
    /**
     * counts all ever created objects of Sequence
     */
    static private int m_countSequences;
    
    /**
     * a unique id for this instance of Sequence
     */
    private int m_uniqueID = m_countSequences++;
    
    
    /**
     * for decoy or other derived sequences this should point to the originating sequence
     */
    private Sequence m_base = this;

    public AminoAcid[] m_sequence;
    
    /** the target complement for a decoy or the current object itself for a target */
    public Sequence target = this;

//    /**
//     * Creates a new Object representing the sequence
//     * @param sequence
//     */
//    public Sequence(String sequence) {
//        String modSeq;
//
//        if (sequence.endsWith(".")) {
//            modSeq = sequence.substring(0, sequence.length()-1);
//        } else 
//            modSeq = sequence;
//        
//        modSeq = modSeq.replaceAll("\\n", "").replaceAll("\\r", "").replaceAll("\\t", "").replaceAll("\\s", "");
//
//        Matcher m = m_sequenceSplit.matcher(modSeq);
//        ArrayList<AminoAcid> temp = new ArrayList<AminoAcid>(modSeq.length());
//        while (m.find()) {
//            // get the next AminoAcid
//            String aaStr = modSeq.substring(m.start(), m.end());
//            AminoAcid aa = AminoAcid.getAminoAcid(aaStr);
//            if (aa == null) {
//                System.err.println("==================================================================" );
//                System.err.println("==================================================================" );
//                System.err.println("==================================================================" );
//                System.err.println("==================================================================" );
//                System.err.println("Don't know how to handle \"" + aaStr + "\" at " + m.start() + " in\n\t" + sequence);
//                System.err.println("Will be replaced by \"X\"");
//                System.err.println("This will exclude any peptide containing it!!!!!!!!!!!!!");
//                System.err.println("==================================================================" );
//                System.err.println("==================================================================" );
//                System.err.println("==================================================================" );
//                System.err.println("==================================================================" );
//                aa=AminoAcid.X;
//            }
//            if (aa.mass != Double.POSITIVE_INFINITY) 
//                m_weight += aa.mass;
//            temp.add(aa);
//        }
//
//        m_sequence = new AminoAcid[temp.size()];
//        m_sequence = temp.toArray(m_sequence);
//    }

    /**
     * Creates a new Object representing the sequence
     * @param sequence
     */
    public Sequence(String sequence, RunConfig config) {
        initXmod(sequence, config);
    }


    /**
     * Creates a new Object representing the sequence
     * @param sequence
     */
    public Sequence(String sequence, RunConfig config, boolean modX) {
        if (modX) {
            initModX(sequence, config);
        } else {
            initXmod(sequence, config);
        }
    }


    private void initXmod(String sequence, RunConfig config) {
        String modSeq;
        if (sequence.endsWith(".")) {
            modSeq = sequence.substring(0, sequence.length()-1);
        } else {
            modSeq = sequence;
        }

        modSeq = modSeq.replaceAll("\\n", "").replaceAll("\\r", "").replaceAll("\\t", "").replaceAll("\\s", "");
        
        
        Matcher m = m_sequenceSplitXmod.matcher(modSeq);

        ArrayList<AminoAcid> temp = new ArrayList<AminoAcid>(modSeq.length());
        while (m.find()) {
            // get the next AminoAcid
            String aaStr = modSeq.substring(m.start(), m.end());
            
            // does it define any expected modifications
            Matcher mExpMod = m_expected_mod_pattern.matcher(aaStr);
            if (mExpMod.matches()) {
                ArrayList<AminoAcid> positionalExpMods = new ArrayList<AminoAcid>();
                String sExpMods = mExpMod.group(1).trim();
                String[] expMods = sExpMods.split("\\|");
                aaStr = aaStr.substring(aaStr.length() -1);
                for (String mod : expMods) {
                    AminoAcid replacement = config.getAminoAcid(aaStr+mod.trim());
                    
                    if (replacement == null) {
                        try {
                            Matcher mexp = m_custom_mod_pattern.matcher(mod);
                            // we might have a freely defined modification
                            if (mexp.matches()) {
                                double massDifference = Double.parseDouble(mexp.group(2));
                                AminoAcid base = config.getAminoAcid(aaStr);
                                AminoModification am = new AminoModification(aaStr + mod, base, base.mass + massDifference);
                                replacement = am;
                                config.addKnownModification(am);
                            } else {
                                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Unknown expected modification " + aaStr+mod);
                            }
                        } catch (Exception e) {
                            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Unknown expected modification " + aaStr+mod, e);
                        }
                    }
                    if (replacement != null) {
                        positionalExpMods.add(replacement);
                    }
                }
                if (sExpMods.endsWith("|")) {
                    positionalExpMods.add(config.getAminoAcid(aaStr));
                }
                    
                if (!positionalExpMods.isEmpty()) {
                    m_expected_Modifications.put(temp.size(), positionalExpMods);
                }
            }
            
            AminoAcid aa = config.getAminoAcid(aaStr);
            if (aa == null) {
                System.err.println("==================================================================" );
                System.err.println("==================================================================" );
                System.err.println("==================================================================" );
                System.err.println("==================================================================" );
                System.err.println("Don't know how to handle \"" + aaStr + "\" at " + m.start() + " in\n\t" + sequence);
                System.err.println("Will be replaced by \"X\"");
                System.err.println("This will exclude any peptide containing it!!!!!!!!!!!!!");
                System.err.println("expected_mod_pattern: "+ m_expected_mod_pattern.toString() );
                System.err.println("==================================================================" );
                System.err.println("==================================================================" );
                System.err.println("==================================================================" );
                System.err.println("==================================================================" );
                aa=AminoAcid.X;
            }
            if (aa.mass != Double.POSITIVE_INFINITY) {
                m_weight += aa.mass;
            }
            temp.add(aa);
        }

        m_sequence = new AminoAcid[temp.size()];
        m_sequence = temp.toArray(m_sequence);
    }

    private void initModX(String sequence, RunConfig config) {
        String modSeq;
        if (sequence.endsWith(".")) {
            modSeq = sequence.substring(0, sequence.length()-1);
        } else {
            modSeq = sequence;
        }

        modSeq = modSeq.replaceAll("\\n", "").replaceAll("\\r", "").replaceAll("\\t", "").replaceAll("\\s", "");
        
        
        Matcher m = m_sequenceSplitModX.matcher(modSeq);

        ArrayList<AminoAcid> temp = new ArrayList<AminoAcid>(modSeq.length());
        while (m.find()) {
            // get the next AminoAcid
            String aaStr = modSeq.substring(m.start(), m.end());
            int aalen = aaStr.length();
            if (aalen > 1) {
                // convert from modX to Xmod
                String baseAA = aaStr.substring(aalen -1);
                String mod = aaStr.substring(0, aalen-1);
                aaStr = baseAA + mod;
            }
            
            
            // does it define any expected modifications
            Matcher mExpMod = m_expected_mod_patternModX.matcher(aaStr);
            if (mExpMod.matches()) {
                ArrayList<AminoAcid> positionalExpMods = new ArrayList<AminoAcid>();
                String sExpMods = mExpMod.group(1).trim();
                String[] expMods = sExpMods.split("\\|");
                aaStr = aaStr.substring(0,1);
                for (String mod : expMods) {
                    AminoAcid replacement = config.getAminoAcid(aaStr+mod.trim());
                    
                    if (replacement == null) {
                        try {
                            Matcher mexp = m_custom_mod_pattern.matcher(mod);
                            // we might have a freely defined modification
                            if (mexp.matches()) {
                                double massDifference = Double.parseDouble(mexp.group(2));
                                AminoAcid base = config.getAminoAcid(aaStr);
                                AminoModification am = new AminoModification(aaStr + mod, base, base.mass + massDifference);
                                replacement = am;
                                config.addKnownModification(am);
                            } else {
                                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Unknown expected modification " + aaStr+mod);
                            }
                        } catch (Exception e) {
                            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Unknown expected modification " + aaStr+mod, e);
                        }
                    }
                    if (replacement != null) {
                        positionalExpMods.add(replacement);
                    }
                }
                if (sExpMods.endsWith("|")) {
                    positionalExpMods.add(config.getAminoAcid(aaStr));
                }
                    
                if (!positionalExpMods.isEmpty()) {
                    m_expected_Modifications.put(temp.size(), positionalExpMods);
                }
            }
            
            AminoAcid aa = config.getAminoAcid(aaStr);
            if (aa == null) {
                System.err.println("==================================================================" );
                System.err.println("==================================================================" );
                System.err.println("==================================================================" );
                System.err.println("==================================================================" );
                System.err.println("Don't know how to handle \"" + aaStr + "\" at " + m.start() + " in\n\t" + sequence);
                System.err.println("Will be replaced by \"X\"");
                System.err.println("This will exclude any peptide containing it!!!!!!!!!!!!!");
                System.err.println("expected_mod_pattern: "+ m_expected_mod_pattern.toString() );
                System.err.println("==================================================================" );
                System.err.println("==================================================================" );
                System.err.println("==================================================================" );
                System.err.println("==================================================================" );
                aa=AminoAcid.X;
            }
            if (aa.mass != Double.POSITIVE_INFINITY) {
                m_weight += aa.mass;
            }
            temp.add(aa);
        }

        m_sequence = new AminoAcid[temp.size()];
        m_sequence = temp.toArray(m_sequence);
    }
    
    public Sequence(AminoAcid[] sequence) {
        m_sequence = sequence.clone();
    }

    
    public TreeMap<Integer,ArrayList<AminoAcid>> getExpectedModifications() {
        return m_expected_Modifications;
    }

    public Map<Integer,ArrayList<AminoAcid>> getExpectedModifications(Peptide p) {
        
        return m_expected_Modifications.subMap(p.getStart(), p.getStart() + p.length());
    }


    /**
     * Creates a new Object representing the sequence
     * @param sequence
     */
    public Sequence(Sequence sequence, int start, int length) {
        m_sequence = new AminoAcid[length];
        System.arraycopy(sequence.m_sequence, start, m_sequence, 0, length);
        m_FastaHeader = sequence.getFastaHeader();
        m_SplittFastaHeader = sequence.getSplitFastaHeader().cloneHeader("");
    }


    public Sequence(String sequence, String fastaHeader, RunConfig conf) {
        this(sequence, conf);
        m_FastaHeader = fastaHeader;
        m_SplittFastaHeader = new FastaHeader(fastaHeader);
    }

    /**
     * returns the (unmodified) amino-acid at the given position
     * @param position
     * @return
     */
    public AminoAcid aminoAcidAt(int position) {
        return m_sequence[position];
    }

    /**
     * returns the (unmodified) amino-acid at the given position
     * @param position
     * @return
     */
    public AminoAcid baseAminoAcidAt(int position) {
        
        AminoAcid ret =  m_sequence[position];
        if (ret instanceof AminoModification || ret instanceof AminoLabel) {
            boolean notbase = true;
            
            while (notbase) {
                
                if (ret instanceof AminoModification) {
                    ret = ((AminoModification) ret).BaseAminoAcid;
                } else if (ret instanceof AminoLabel) {
                    ret = ((AminoLabel) ret).BaseAminoAcid;
                } else {
                    notbase = false;
                }
            }
            return ret;
        } 
        return ret;
    }
    
    /**
     * returns the amino-acid at the given position
     * in case of a label - the base-amino-acid is returned
     * @param position
     * @return
     */
    public AminoAcid nonLabeledAminoAcidAt(int position) {
        AminoAcid aa = m_sequence[position];
        if (aa instanceof AminoLabel) {
            return ((AminoLabel) aa).BaseAminoAcid;
        } else {
            return aa;
        }
    }


    /**
     *
     * @return length of the sequence
     */
    public int length () {
        return m_sequence.length;
    }

    /**
     * Returns the list of theoretical peptides
     * @return List of peptides
     */
    public ArrayList<Peptide> getPeptides() {
        if (m_peptides == null) {
            m_peptides = new ArrayList<Peptide>();
        }
        return m_peptides;
    }

    /**
     * Digests the sequence into peptides
     * @param method
     * @return
     */
    public int digest(Digestion method, double MaxMath, ArrayList<CrossLinker> cl) {
        this.m_peptides = method.digest(this, MaxMath, cl);
        return m_peptides.size();
    }



    /**
     * Applies all possible modifications to all peptides and creates new modified peptieds
     * @return
     */
    public int modify(RunConfig conf, PeptideLookup lookup, PeptideLookup lookuplinear, ArrayList<CrossLinker> linkers, Digestion enzym) {
//        if (m_FastaHeader.contentEquals("Cut9"))
//            System.err.println("found it");
        Iterator<Peptide> peps = m_peptides.iterator();
        ArrayList<Peptide> newPeps = new ArrayList<Peptide>();
        ArrayList<Peptide> newPepsLinear = new ArrayList<Peptide>();
        while (peps.hasNext()) {
            Peptide pep = peps.next();
            peptideloop: for (Peptide p : pep.modify(conf,ModificationType.variable)) {
                if (enzym.isDigestedPeptide(p)) {
                    clloop: for (CrossLinker cl: linkers) {
                        if (cl.canCrossLink(p)) {
                            lookup.addPeptide(p);
                            newPeps.add(p);
                            continue peptideloop;
                        }
                    }
                    lookuplinear.addPeptide(p);
                    newPeps.add(p);
                }
            }
        }
        m_peptides.addAll(newPeps);

        return newPeps.size();
    }

    /**
     * Applies all possible modifications to all peptides and creates new modified peptieds
     * @return
     */
    public int modify(RunConfig conf, PeptideLookup lookup, ArrayList<CrossLinker> linkers, Digestion enzym) {
//        if (m_FastaHeader.contentEquals("Cut9"))
//            System.err.println("found it");
        Iterator<Peptide> peps = m_peptides.iterator();
        ArrayList<Peptide> newPeps = new ArrayList<Peptide>();
        while (peps.hasNext()) {
            Peptide pep = peps.next();
            peptideloop: for (Peptide p : pep.modify(conf, ModificationType.variable)) {
                if (enzym.isDigestedPeptide(p)) {
                    clloop: for (CrossLinker cl: linkers) {
                        if (cl.canCrossLink(p)) {
                            lookup.addPeptide(p);
                            newPeps.add(p);
                            continue peptideloop;
                        }
                    }
                }
            }
        }
        m_peptides.addAll(newPeps);
        return newPeps.size();
    }


    /**
     * Applies all possible modifications to all peptides and creates new modified peptieds
     * @return
     */
    public int label(RunConfig conf, PeptideLookup lookup) {
        Iterator<Peptide> peps = m_peptides.iterator();
        ArrayList<Peptide> newPeps = new ArrayList<Peptide>();
        while (peps.hasNext()) {
            Peptide pep = peps.next();
//            newPeps.addAll(pep.label(conf));
            for (Peptide p : pep.label(conf)) {
                lookup.addPeptide(p);
                newPeps.add(p);
            }
        }
        m_peptides.addAll(newPeps);
        return newPeps.size();
    }

    /**
     * @return the m_FastaHeader
     */
    public String getFastaHeader() {
        return m_FastaHeader;
    }



    /**
     * @return the split up fasta-header
     */
    public FastaHeader getSplitFastaHeader() {
        return m_SplittFastaHeader;
    }


    /**
     * @param m_FastaHeader the m_FastaHeader to set
     */
    public void setFastaHeader(String FastaHeader) {
        this.m_FastaHeader = FastaHeader;
        m_SplittFastaHeader = new FastaHeader(FastaHeader);
    }
    
    public void setFastaHeader(FastaHeader fh) {
        m_SplittFastaHeader = fh;
        m_FastaHeader = fh.getHeader();
    }

    @Override
    public String toString() {
        StringBuffer sequence = new StringBuffer(m_sequence.length);
        for (int i = 0; i< m_sequence.length; i++) {
            sequence.append(m_sequence[i].toString());
        }
        return sequence.toString();
    }

    public String toFasta(int chars) {
        StringBuffer sequence = new StringBuffer(m_sequence.length);
        sequence.append(">");
        sequence.append(m_FastaHeader);
        for (int i = 0; i< m_sequence.length; i++) {
            if (i %chars == 0) {
                sequence.append('\n');
            }
            sequence.append(m_sequence[i].toString());
        }
        return sequence.toString();
    }
    
    /**
     * @return the weight of the sequence
     */
    public double getWeight() {
        return m_weight;
    }

    @Override
    public boolean containsAminoAcid(AminoAcid aa) {
        return SequenceUtils.containsAminoAcid(this, aa);
    }

    @Override
    public Sequence subSequence(final short from, final short length) {
        final int l = this.length();
        return new Sequence(this,from, length) {
            boolean nt = from == 0;
            boolean ct = from + length == l;
            @Override
            public boolean isCTerminal() {
                return ct;
            }
            @Override
            public boolean isNTerminal() {
                return nt;
            }
        };
    }

    public int countAminoAcid(HashSet<AminoAcid> aas) {
        return SequenceUtils.countAminoAcid(this, aas);
    }

    public AminoAcid[] toArray() {
        return m_sequence;
    }

    public AminoAcid setAminoAcidAt(int pos, AminoAcid aa) {
        AminoAcid aaOld = m_sequence[pos];
        m_sequence[pos] = aa;
        m_weight += aa.mass - aaOld.mass;
        return aaOld;
    }

    public int replace(AminoAcid oldAA, AminoAcid newAA) {
        int found = 0;
        for (int i=0; i < m_sequence.length; i++ ) {
            if (m_sequence[i] == oldAA) {
                m_sequence[i] = newAA;
                found++;
            }
        }
        m_weight += (newAA.mass - oldAA.mass) * found;
        return found;
    }



    public int replace(AminoModification[] modifications) {
        HashMap<AminoAcid,AminoAcid> mapOldNew = new HashMap<AminoAcid, AminoAcid>(modifications.length);
        for (AminoModification am : modifications) {
            mapOldNew.put(am.BaseAminoAcid,am);
        }
        return replace(mapOldNew);
    }

    public int replace(Collection<AminoModification> modifications) {
        HashMap<AminoAcid,AminoAcid> mapOldNew = new HashMap<AminoAcid, AminoAcid>(modifications.size());
        for (AminoModification am : modifications) {
            mapOldNew.put(am.BaseAminoAcid,am);
        }
        return replace(mapOldNew);
    }

    public int replace(HashMap<AminoAcid,AminoAcid> modifications) {

        int found = 0;
        for (int i=0; i < m_sequence.length; i++ ) {
            if (modifications.containsKey(m_sequence[i])) {
                AminoAcid newAA = modifications.get(m_sequence[i]);
                m_weight += (newAA.mass - m_sequence[i].mass);
                m_sequence[i] = newAA;
                found++;
            }
        }
        return found;
    }

    public int replace(AminoModification AAM) {
        return replace(AAM.BaseAminoAcid, AAM);
    }

    public int applyFixedModifications() {
        return replace(AminoModification.getAllFixedModification());
    }

    public int applyFixedModifications(RunConfig conf) {
        return replace(conf.getFixedModificationsPreDigest());
    }

    public int getStart() {
        return 0;
    }




    /**
     * @return the m_isDecoy
     */
    public boolean isDecoy() {
        return m_isDecoy;
    }

    /**
     * @param m_isDecoy the isDecoy to set
     */
    public void setDecoy(boolean isDecoy) {
        this.m_isDecoy = isDecoy;
    }

    public Sequence getSourceSequence() {
        return m_base;
    }


    public long getID() {
        return m_id;
    }

    public void setID(long id) {
        m_id = id;
    }

    @Override
    public boolean isNTerminal() {
        return true;
    }

    @Override
    public boolean isProteinNTerminal() {
        return true;
    }

    @Override
    public boolean isCTerminal() {
        return false;
    }

    @Override
    public boolean isProteinCTerminal() {
        return false;
    }

    public Sequence reverse() {
        Sequence rev = new Sequence(m_sequence);
        rev.m_SplittFastaHeader = m_SplittFastaHeader.cloneHeader("REV_");
        rev.m_FastaHeader = rev.m_SplittFastaHeader.getHeader();
        for (int i = 0; i < m_sequence.length; i++) {
            int source = m_sequence.length - 1 - i;
            rev.m_sequence[i] = m_sequence[m_sequence.length - 1 - i];
            if (m_expected_Modifications.get(source) != null) {
                rev.m_expected_Modifications.put(i, m_expected_Modifications.get(source));
            }
        }

        if (rev.m_sequence[length()-1] == AminoAcid.M && 
            rev.m_sequence[0] != AminoAcid.M) {
            rev.m_sequence[length()-1] = rev.m_sequence[0];
            rev.m_sequence[0] = AminoAcid.M;
        }
        
        rev.target = this;
        rev.setSource(m_source);
        return rev;
    }

    public Sequence shuffle() {
        AminoAcid[] newSequence = new AminoAcid[length()];
        ArrayList<AminoAcid> ranSeq = new ArrayList<AminoAcid>(m_sequence.length);

        for (int i=0; i<newSequence.length;i++) {
            ranSeq.add(m_sequence[i]);
        }

        newSequence[0] = aminoAcidAt(0);
        for (int i=1; i<newSequence.length;i++) {
            int r = (int)(ranSeq.size() * Math.random());
            newSequence[i] = ranSeq.get(r);
            ranSeq.remove(r);
        }
        
        Sequence rev =  new Sequence(newSequence);
        TreeMap<Integer,ArrayList<AminoAcid>> newExpMod = new TreeMap<Integer,ArrayList<AminoAcid>>();
        for (Map.Entry<Integer,ArrayList<AminoAcid>> e : m_expected_Modifications.entrySet()) {
            int p = e.getKey();
            AminoAcid a = aminoAcidAt(p);
            ArrayList<Integer> possible = new ArrayList<>(this.length() / 2);
            for (Integer i =0; i<length(); i++) {
                if (a == newSequence[i] && newExpMod.get(i) == null) {
                    possible.add(i);
                }
            }
            if (possible.size()>0) {
                newExpMod.put(possible.get((int)(Math.random()*possible.size())), e.getValue());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,"Could not add all FASTA-variable modifications to the decoy-protein");
                System.exit(-1);
            }
        }
        rev.m_expected_Modifications = newExpMod;
        rev.m_SplittFastaHeader = m_SplittFastaHeader.cloneHeader("RAN_");
        rev.m_FastaHeader = rev.m_SplittFastaHeader.getHeader();
        rev.target = this;
        rev.setSource(m_source);
        return rev;
    }    

    public Sequence reverseAvare(AminoAcid[] specials) {
        ArrayList<AminoAcid> aas = new ArrayList<AminoAcid>(specials.length);
        for (int aa = 0; aa< specials.length; aa++) {
            aas.add(specials[aa]);
        }
        return reverseAvare(aas);
    }
    
    public Sequence reverseAvare(ArrayList<AminoAcid> specials) {
        int total = specials.size();
        if (total < 2) {
            return reverse();
        }
        
        HashMap<AminoAcid,Integer> ids = new HashMap<AminoAcid, Integer>(specials.size());
        
        for (int i=0; i < specials.size(); i++) {
            ids.put(specials.get(i), i);
        }
        
        AminoAcid[] newSequence = new AminoAcid[length()];
        int last = -1;
        int nextins = newSequence.length - 1;
        for (int i=0; i<newSequence.length;i++) {
            AminoAcid aa = m_sequence[i];
            AminoAcid ins = aa;
            Integer aaid = ids.get(aa);
            if (aaid != null) {
                if (aaid == last) {
                    last = (last + 1) % total;
                    ins = specials.get(last);
                } else {
                    last = aaid;
                }
            } 
            newSequence[nextins --] = ins;
        }
        // restor an n-terminal methionine
        if (newSequence[newSequence.length-1] == AminoAcid.M && 
            newSequence[0] != AminoAcid.M) {
            newSequence[newSequence.length-1] = newSequence[0];
            newSequence[0] = AminoAcid.M;
        }
        Sequence rev =  new Sequence(newSequence);
        TreeMap<Integer,ArrayList<AminoAcid>> newExpMod = new TreeMap<Integer,ArrayList<AminoAcid>>();
        for (Map.Entry<Integer,ArrayList<AminoAcid>> e : m_expected_Modifications.entrySet()) {
            int p = e.getKey();
            AminoAcid a = aminoAcidAt(p);
            ArrayList<Integer> possible = new ArrayList<>(this.length() / 2);
            for (Integer i =0; i<length(); i++) {
                if (a == newSequence[i] && newExpMod.get(i) == null) {
                    possible.add(i);
                }
            }
            if (possible.size()>0) {
                newExpMod.put(possible.get((int)(Math.random()*possible.size())), e.getValue());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,"Could not add all FASTA-variable modifications to the decoy-protein");
                System.exit(-1);
            }
        }
        rev.m_expected_Modifications = newExpMod;
        rev.m_SplittFastaHeader = m_SplittFastaHeader.cloneHeader("REV_");
        rev.m_FastaHeader = rev.m_SplittFastaHeader.getHeader();
        rev.target = this;
        rev.setSource(m_source);
        return rev;
    }    
    
    public Sequence shuffle(Collection<AminoAcid> fixedAminoAcids) {
        HashSet<AminoAcid> fixed = new HashSet<>(fixedAminoAcids);
        AminoAcid[] newSequence = new AminoAcid[length()];
        ArrayList<AminoAcid> ranSeq = new ArrayList<AminoAcid>(m_sequence.length);

        for (int i=0; i<newSequence.length;i++) {
            if (!fixed.contains(m_sequence[i])) {
                ranSeq.add(m_sequence[i]);
            }
        }
        
        newSequence[0] = aminoAcidAt(0);
        for (int i=1; i<newSequence.length;i++) {
            if (!fixed.contains(m_sequence[i])) {
                int r = (int)(ranSeq.size() * Math.random());
                newSequence[i] = ranSeq.get(r);
                ranSeq.remove(r);
            } else {
                newSequence[i] = m_sequence[i];
            }
        }
        
        Sequence rev =  new Sequence(newSequence);
        TreeMap<Integer,ArrayList<AminoAcid>> newExpMod = new TreeMap<Integer,ArrayList<AminoAcid>>();
        for (Map.Entry<Integer,ArrayList<AminoAcid>> e : m_expected_Modifications.entrySet()) {
            int p = e.getKey();
            AminoAcid a = aminoAcidAt(p);
            ArrayList<Integer> possible = new ArrayList<>(this.length() / 2);
            for (Integer i =0; i<length(); i++) {
                if (a == newSequence[i] && newExpMod.get(i) == null) {
                    possible.add(i);
                }
            }
            if (possible.size()>0) {
                newExpMod.put(possible.get((int)(Math.random()*possible.size())), e.getValue());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,"Could not add all FASTA-variable modifications to the decoy-protein");
                System.exit(-1);
            }
        }
        rev.m_expected_Modifications = newExpMod;
        rev.m_SplittFastaHeader = m_SplittFastaHeader.cloneHeader("RAN_");
        rev.m_FastaHeader = rev.m_SplittFastaHeader.getHeader();
        rev.target = this;
        rev.setSource(m_source);
        return rev;
    }      


    /**
     * generate a randomized sequences with the same length as the current 
     * sequence. Optionally some amino acids can be kept in place e.g. to have 
     * the same length distribution for peptides
     * @param fixedAminoAcids amino-acids not to be randomized
     * @param conf config that provides the list of amino acids
     * @param rand  the random number generator to use
     * @return 
     */
    public Sequence randomize(Collection<AminoAcid> fixedAminoAcids, RunConfig conf, Random rand) {
        HashSet<AminoAcid> fixed = new HashSet<>(fixedAminoAcids);
        AminoAcid[] newSequence = new AminoAcid[length()];
        
        HashSet<AminoAcid> selection = new HashSet<>();
        HashSet<AminoAcid> nonSelection = new HashSet<>(fixed);
        nonSelection.add(AminoAcid.B);
        nonSelection.add(AminoAcid.Z);
        nonSelection.add(AminoAcid.X);
        for (AminoAcid aa : this) {
            selection.add(aa);
        }
        for (AminoModification aam : conf.getVariableModifications()) {
            nonSelection.add(aam);
        }
        
        for (AminoModification aam : conf.getKnownModifications()) {
            nonSelection.add(aam);
        }

        for (AminoLabel aam : conf.getLabel()) {
            nonSelection.add(aam);
        }

        for (AminoModification aam : conf.getFixedModifications()) {
            nonSelection.add(aam);
        }

        for (AminoAcid aa : conf.getAllAminoAcids()) {
            if (!nonSelection.contains(aa)) {
                selection.add(aa);
            }
        }
        ArrayList<AminoAcid> choices = new ArrayList<>(selection);
        
        int randCount = choices.size();
        newSequence[0] = aminoAcidAt(0);
        for (int i=1; i<newSequence.length;i++) {
            if (!fixed.contains(m_sequence[i])) {
                int r = (int)(randCount * rand.nextDouble());
                newSequence[i] = choices.get(r);
            } else {
                newSequence[i] = m_sequence[i];
            }
        }
        
        // transfer the expected modifications to the decoy protein
        Sequence rev =  new Sequence(newSequence);
        TreeMap<Integer,ArrayList<AminoAcid>> newExpMod = new TreeMap<Integer,ArrayList<AminoAcid>>();
        for (Map.Entry<Integer,ArrayList<AminoAcid>> e : m_expected_Modifications.entrySet()) {
            int p = e.getKey();
            AminoAcid a = aminoAcidAt(p);
            ArrayList<Integer> possible = new ArrayList<>(this.length() / 2);
            for (Integer i =0; i<length(); i++) {
                if (a == newSequence[i] && newExpMod.get(i) == null) {
                    possible.add(i);
                }
            }
            if (possible.size()>0) {
                newExpMod.put(possible.get((int)(Math.random()*possible.size())), e.getValue());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Could not add all FASTA-variable modifications to the decoy-protein");
            }
        }
        rev.m_expected_Modifications = newExpMod;

        rev.m_SplittFastaHeader = m_SplittFastaHeader.cloneHeader("RAN_");
        rev.m_FastaHeader = rev.m_SplittFastaHeader.getHeader();
        rev.target = this;
        rev.setSource(m_source);
        return rev;
    }      

    /**
     * generate a randomized sequences with the same length as the current 
     * sequence. The possible aminoacids are taken from the same protein and 
     * selected randomly but following the likelyhood of two aminoacids following each other
     * @param fixedAminoAcids amino-acids not to be randomized
     * @param conf config that provides the list of amino acids
     * @param rand  the random number generator to use
     * @return 
     */
    public Sequence randomizeDirected(Collection<AminoAcid> fixedAminoAcids, RunConfig conf, Random rand) {
        HashSet<AminoAcid> fixed = new HashSet<>(fixedAminoAcids);
        AminoAcid[] newSequence = new AminoAcid[length()];
        
        HashSet<AminoAcid> nonSelection = new HashSet<>(fixed);
        HashMap<AminoAcid,ArrayList<AminoAcid>> choices = new HashMap<>();
        ArrayList<AminoAcid> all = new ArrayList<AminoAcid>(m_sequence.length);
        for (int aa = 1; aa<m_sequence.length-1;aa++) {
            AminoAcid prevAA = m_sequence[aa-1];
            AminoAcid followAA = m_sequence[aa];
            if (!fixed.contains(followAA)) {
                ArrayList<AminoAcid> followers = choices.get(prevAA);
                if (followers == null) {
                    followers = new ArrayList<>(2);
                    followers.add(followAA);
                    choices.put(prevAA, followers);
                } else {
                    followers.add(followAA);
                }
                all.add(followAA);
            }
        }

        newSequence[0] = aminoAcidAt(0);
        newSequence[newSequence.length-1] = aminoAcidAt(newSequence.length-1);
        for (int i=1; i<newSequence.length-1;i++) {
            if (!fixed.contains(m_sequence[i])) {
                ArrayList<AminoAcid> followers = choices.get(newSequence[i-1]);
                if (followers == null) {
                    followers = all;
                }
                int index = (int)(Math.random()* followers.size());
                newSequence[i] = followers.get(index);
            } else {
                newSequence[i] = m_sequence[i];
            }
        }
        
        Sequence rev =  new Sequence(newSequence);
        TreeMap<Integer,ArrayList<AminoAcid>> newExpMod = new TreeMap<Integer,ArrayList<AminoAcid>>();
        for (Map.Entry<Integer,ArrayList<AminoAcid>> e : m_expected_Modifications.entrySet()) {
            int p = e.getKey();
            AminoAcid a = aminoAcidAt(p);
            ArrayList<Integer> possible = new ArrayList<>(this.length() / 2);
            for (Integer i =0; i<length(); i++) {
                if (a == newSequence[i] && newExpMod.get(i) == null) {
                    possible.add(i);
                }
            }
            if (possible.size()>0) {
                newExpMod.put(possible.get((int)(Math.random()*possible.size())), e.getValue());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Could not add all FASTA-variable modifications to the decoy-protein");
            }
        }
        rev.m_expected_Modifications = newExpMod;
        rev.m_SplittFastaHeader = m_SplittFastaHeader.cloneHeader("RAN_");
        rev.m_FastaHeader = rev.m_SplittFastaHeader.getHeader();
        rev.target = this;
        rev.setSource(m_source);
        return rev;
    }      

    
    /**
     * generate N randomized sequences bassed on the current one and return the 
     * closest in weight to the original.
     * @param fixedAminoAcids amino-acids not to be randomized
     * @param conf config that provides the list of amino acids
     * @param N how many sequences to randomize
     * @param rand  the random number generator to use
     * @return 
     */
    public Sequence randomizeNDirected(Collection<AminoAcid> fixedAminoAcids, RunConfig conf,int N, Random rand) {
        Sequence[] s = new Sequence[N];
        double diffmass = Double.MAX_VALUE;
        Sequence ret = null;
        for (int i =0 ; i<N;i++) {
            s[i]=randomizeDirected(fixedAminoAcids, conf, rand);
            double d = Math.abs(s[i].m_weight - m_weight);
            if (d<diffmass) {
                ret = s[i];
                diffmass = d;
            }
        }
        
        return ret;
    }      
    
    /**
     * generate N randomized sequences bassed on the current one and return the 
     * closest in weight to the original.
     * @param fixedAminoAcids amino-acids not to be randomized
     * @param conf config that provides the list of amino acids
     * @param N how many sequences to randomize
     * @param rand  the random number generator to use
     * @return 
     */
    public Sequence randomizeN(Collection<AminoAcid> fixedAminoAcids, RunConfig conf,int N, Random rand) {
        Sequence[] s = new Sequence[N];
        double diffmass = Double.MAX_VALUE;
        Sequence ret = null;
        for (int i =0 ; i<N;i++) {
            s[i]=randomize(fixedAminoAcids, conf, rand);
            double d = Math.abs(s[i].m_weight - m_weight);
            if (d<diffmass) {
                ret = s[i];
                diffmass = d;
            }
        }
        
        return ret;
    }      
    
    
    public void swapWithPredecesor(HashSet<AminoAcid> aas) {
        int first =1;
        if (m_sequence[0]  == AminoAcid.M) {
            first = 2;
        }
        for (int i = first ; i< m_sequence.length; i ++) {
            AminoAcid aa = m_sequence[i];
            if (aas.contains(aa)) {
                m_sequence[i] = m_sequence[i-1];
                m_sequence[i-1] = aa;
            }
        }
        
        
    }      
    
    public int getUniqueID(){
        return m_uniqueID;
    }
    
    /**
     * should not normally be used - its only use is for randomising decoy 
     * peptides that are within the target database.
     * @param seq 
     */
    public Sequence addSequence(AminoAcid[] seq) {
        AminoAcid[] temp = new AminoAcid[m_sequence.length+seq.length];
        System.arraycopy(m_sequence,0 , temp, 0, m_sequence.length);
        System.arraycopy(seq,0 , temp, m_sequence.length,seq.length);
        m_sequence = temp;
        return this;
    }

    @Override
    public Iterator<AminoAcid> iterator() {
        return new Iterator<AminoAcid>() {
            int current = 0;
            @Override
            public boolean hasNext() {
                return current < length();
            }

            @Override
            public AminoAcid next() {
                if (current < length()) {
                    return aminoAcidAt(current++);
                }
                return null;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    /**
     * @return the source
     */
    public FastaFile getSource() {
        return m_source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(FastaFile source) {
        this.m_source = source;
    }

}
