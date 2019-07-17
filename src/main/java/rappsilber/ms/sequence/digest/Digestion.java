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
package rappsilber.ms.sequence.digest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.lookup.peptides.PeptideLookup;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoAcidSequence;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.Peptide;
import rappsilber.utils.PermArray;

/**
 *  @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
// <editor-fold defaultstate="collapsed" desc=" UML Marker "> 
// #[regen=yes,id=DCE.256F8C21-35FB-28DB-0395-73795A3C1341]
// </editor-fold> 
public class Digestion {

    protected HashSet<AminoAcid> m_CTermAminoAcids;
    protected HashSet<AminoAcid> m_NTermAminoAcids = new HashSet<AminoAcid>();
    protected int m_AminoAcidsPerPeptide = 9;
    protected int m_seqLength=0;
    protected int m_peptides =0;
    private   int m_maxMissCleavages=0;
    private   int m_minPeptideLength=0;
    private   String m_name;

    private PeptideLookup m_peptidetree;
    private PeptideLookup m_peptideTreeLinear;
    private RunConfig     m_config;


//	public static final Pattern internal_K = Pattern.compile(".*K[A-Z].*"); // 1.
//	public static final Pattern k_n_term = Pattern.compile("K");// 2.
//	public static final Pattern r_n_term = Pattern.compile("R"); // 3.




    // <editor-fold defaultstate="collapsed" desc=" UML Marker "> 
    // #[regen=yes,id=DCE.1B50A03B-847D-A06E-5D1B-B4BEA9FB4DCE]
    // </editor-fold> 
    public Digestion (AminoAcid[] CTermAminoAcids, int misscleavages,RunConfig config) {
        this(CTermAminoAcids, new AminoAcid[0],config);
        m_maxMissCleavages = misscleavages;

    }

    // <editor-fold defaultstate="collapsed" desc=" UML Marker "> 
    // #[regen=yes,id=DCE.1B50A03B-847D-A06E-5D1B-B4BEA9FB4DCE]
    // </editor-fold> 
    public Digestion (AminoAcid[] CTermAminoAcids, AminoAcid[] NTermAminoAcids, int misscleavages, RunConfig config) {
        this(CTermAminoAcids, NTermAminoAcids,config);
        m_maxMissCleavages = misscleavages;

    }
    
    
    public Digestion (AminoAcid[] CTermAminoAcids, AminoAcid[] NTermAminoAcids, RunConfig config) {
        m_CTermAminoAcids = new HashSet<AminoAcid>(CTermAminoAcids.length);
        m_CTermAminoAcids.addAll(Arrays.asList(CTermAminoAcids));
        m_NTermAminoAcids = new HashSet<AminoAcid>(NTermAminoAcids.length);
        m_NTermAminoAcids.addAll(Arrays.asList(NTermAminoAcids));
        m_config = config;
        setMinPeptideLength(m_config.retrieveObject("minpeptidelength", 0));
        
    }


    /**
     * do the actual digestion of a sequence
     * @param seq
     * @param cl
     * @return
     */
    public ArrayList<Peptide> digest(Sequence seq,ArrayList<CrossLinker> cl) {
        return digest(seq, Double.MAX_VALUE, cl);
    }
    
    /**
     * do the actual digestion of a sequence
     * @param seq
     * @param cl
     * @return
     */
    public ArrayList<Peptide> digest(AminoAcidSequence seq,ArrayList<CrossLinker> cl) {
        if (seq instanceof Sequence)
            return digest((Sequence)seq, Double.MAX_VALUE, cl);
        if (seq instanceof Peptide)
            return digest((Peptide)seq, Double.MAX_VALUE, cl);
        return digest((Sequence)seq, Double.MAX_VALUE, cl);
    }

    /**
     * tests whether a sequence can be cleaved after the given amino-acid
     * @param seq
     * @param AAPos
     * @return
     */
    protected boolean isCleavageSite(Sequence seq, int AAPos) {
        return isCleavageSite((AminoAcidSequence) seq, AAPos);
    }

    /**
     * tests whether a sequence can be cleaved after the given amin-oacid
     * @param seq
     * @param AAPos
     * @return
     */
    protected boolean isCleavageSite(AminoAcidSequence seq, int AAPos) {
        return (m_CTermAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos)));
    }

    
    /**
     * tests whether a sequence can be cleaved after the given amin-oacid
     * @param seq
     * @param AAPos
     * @return
     */
    protected boolean isCleavagePair(Sequence seq, int AAPos1, int AAPos2) {
        return (m_CTermAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos1)) && m_CTermAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos2))) ||
               (m_NTermAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos1+1)) && m_CTermAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos2+1)));
    }
    
//    private boolean linkable(Peptide p) {
//        Matcher m = internal_K.matcher(p.toString());
//        return (m.matches() || (p.getStart() == 0));
//    }

    /**
     * test whether a peptide could have been be the result of this digestion
     * @param p
     * @return
     */
    public boolean isDigestedPeptide(Peptide p) {
        Sequence s = p.getSequence();
        int start = p.getStart();
        int end = p.getStart() + p.length() - 1;

        return (start == 0 ||               // n-terminal
                m_CTermAminoAcids.contains(      // or specified aminoacid
                s.aminoAcidAt(start - 1))   // before the peptide
                )
                &&
                (end ==  s.length() - 1 ||  // c-terminal
                m_CTermAminoAcids.contains(      // or ends
                s.aminoAcidAt(end)));       // with specified aminoacid
    }

    
    protected void addPeptide(Peptide p, Sequence s, ArrayList<Peptide> sequencePeptides) {
        if (p.getMass() == Double.POSITIVE_INFINITY || p.length() <=1)
            return;


//        ArrayList<Peptide> peps = m_peptidetree.getForMass(p.getMass());
//        if (peps != null && peps.size() > 0) {
//            // there is already at least one peptide with the same mass
//            for (int pID = 0; pID<peps.size(); pID++) {
//                Peptide storedPep = peps.get(pID);
//
//                if (storedPep.equalSequence(p)) {
//                    // it's the same
//
//                    if (!s.isDecoy()) {
//
//                        // not a decoy -> it has to be added
//
//                        if (storedPep.getSequence().isDecoy()) {
//                            // the stored peptide was a decoy one - so we are replacinf this
//                            peps.set(pID, p);
//                        } else {
//                            // target peptide -> add another source
//                            storedPep.addSource(p);
//                        }
//                    } else if (storedPep.getSequence().isDecoy()) {
//                        // add another decoy source
//                        storedPep.addSource(p);
//                    }
//                    return;
//                }
//            }
//        }
        m_peptidetree.addPeptide(p);
        sequencePeptides.add(p);
    }

    protected void addPeptide(Peptide p, Sequence s, ArrayList<Peptide> sequencePeptides,ArrayList<CrossLinker> cl) {
        addPeptide(p, s, sequencePeptides, cl, true);
    }
    
    /**
     * returns, whether the unmodified version should be accepted as well
     * @param p
     * @param s
     * @param pos
     * @param mods
     * @param mpeps
     * @param modcount
     * @return 
     */
    protected boolean getModPeptides(Peptide p, Sequence s, int pos, Map<Integer, ArrayList<AminoAcid>> mods, HashMap<Integer,ArrayList<Peptide>> mpeps, int modcount) {
        // are we permited to leave the following amino acids unmodified
        if (getModPeptides(p, s, pos+1, mods, mpeps, modcount)) {
            // yes so we can look for modifications on only this site
            ArrayList<AminoAcid> posmods = mods.get(pos);
            if (posmods != null) {
                AminoAcid origAA = p.aminoAcidAt(pos);
                ArrayList<Peptide> pepstore = mpeps.get(modcount + 1);
                if (pepstore == null) {
                    pepstore = new ArrayList<Peptide>();
                    mpeps.put(modcount+1, pepstore);
                }
                // new peptide with the modification
                boolean unmod = false;
                for (AminoAcid aa : posmods) {
                    if (aa != origAA) {
                        Peptide mp = new Peptide(p, pos, aa);
                        pepstore.add(mp);
                        if (modcount < m_config.getMaximumModificationPerPeptide()) {
                            getModPeptides(mp, s, pos, mods, mpeps, modcount+1);
                        }
                    } else {
                        unmod = true;
                    }
                }
                
                return unmod;
            } else {
                // nothing to say you can have this unmodified
                return true;
            }
            
        }
        
        return false;
        
    }
    
    protected void addPeptide(Peptide p, Sequence s, ArrayList<Peptide> sequencePeptides,ArrayList<CrossLinker> cl, boolean extraCheck) {

        if (p.getMass() == Double.POSITIVE_INFINITY)
            return;

        if (p.getLength() < m_minPeptideLength)
            return;

        if (p.getStart() == 0 && p.aminoAcidAt(0) == AminoAcid.M && p.length() > 1 && ! isCleavageSite(s, 0) && extraCheck) {
            Peptide p2 = new Peptide(p, 1, p.length() - 1) {
                @Override
                public boolean isNTerminal() {
                    return true;
                }
            };
            addPeptide(p2, s, sequencePeptides, cl);
        }

        Map<Integer, ArrayList<AminoAcid>> mods = new TreeMap<Integer, ArrayList<AminoAcid>>();
        
        // are there any expected modification?
        if (extraCheck) {
            mods = s.getExpectedModifications(p); 
            if (!mods.isEmpty()){
                
                int maxModPep = m_config.getMaximumModifiedPeptidesPerFASTAPeptide();
                int maxMods = Math.min(m_config.getMaximumModificationPerFASTAPeptide(),mods.size());
                int totalPeps=0;
                Object[] modEntries = new Object[mods.size()];
                {
                    int i =0;
                    for (Map.Entry<Integer, ArrayList<AminoAcid>> e : mods.entrySet()) {
                        modEntries[i++]=e;
                    }
                }

                
                // yes there are some expected modifications
                ArrayList<Peptide> modPeps = new ArrayList<Peptide>();
                // so we need to add these - with in the limits of the number of permited modifications
                // for each possible number of modifications on this peptide
                for (int mm = 1; mm<=maxMods; mm++) {
                    Boolean[] modHere = new Boolean[mods.size()];
                    for (int bmm=0; bmm<mm;bmm++) {
                        modHere[bmm]=true;
                    }
                    for (int bmm=mm; bmm<modHere.length;bmm++) {
                        modHere[bmm]=false;
                    }
                    // go through all permutations of where the modifications could be applied
                    PermArray<Boolean> perm = new  PermArray<Boolean>(modHere);
                    for (Boolean[] modSet : perm) {
                        // create a new peptide for this modification
                        ArrayList<Peptide> np = new ArrayList<Peptide>(1);
                        np.add(p.clone());
                        // and apply the modification at the correct places for this permutation
                        for (int m = 0 ; m< modSet.length; m++) {
                            if (modSet[m]) {
                                // modification should be here so get the modifications that should go to this place
                                Map.Entry<Integer, ArrayList<AminoAcid>> e = 
                                        (Map.Entry<Integer, ArrayList<AminoAcid>>) modEntries[m];
                                // where in the peptide the modification gets applied
                                int pos = e.getKey();
                                // what are the modifications to be applied
                                ArrayList<AminoAcid> aam = e.getValue();
                                // modify all previous modified peptides of this round with the first modification here
                                for (Peptide mp : np) {
                                    mp.modify(pos-mp.getStart(), aam.get(0));
                                }
                                // if there are more then a single modification
                                if (aam.size()>1) {
                                    // then we need to clone the previous peptide(s) and aply the additional modification on the cloned one
                                    ArrayList<Peptide> cloned = new ArrayList<Peptide>();
                                    // clone all previous modified peptides for each aditional modification
                                    for (int aa = 1; aa<aam.size();aa++) {
                                        for (Peptide mp : np) {
                                            Peptide sm = mp.clone();
                                            sm.modify(pos-sm.getStart(), aam.get(aa));
                                            cloned.add(sm);
                                        }
                                    }
                                    // register all the new peptides in the list of peptides for this modification permutation
                                    np.addAll(cloned);
                                }
                            }
                        }
                        // remember the modified peptides
                        modPeps.addAll(np);
                    }
                    // if we reached/exceded the permited number of modified peptides stop here
                    if (modPeps.size() >= maxModPep)
                        break;
                }

                // all expected extra peptides get added as well
                for (Peptide mp : modPeps) {
                    addPeptide(mp, s, sequencePeptides, cl, false);
                }
            }
        }

        
//        if (CrossLinker.canCrossLink(cl, p.subSequence((short)0, (short)(p.length() - 1))))
        if (mods.isEmpty()) {
            if (CrossLinker.canCrossLink(cl, p))
                m_peptidetree.addPeptide(p);
            else if (p.length() > 3)
                m_peptideTreeLinear.addPeptide(p);
            sequencePeptides.add(p);
        }
        
    }


    /**
     * do the actual digestion of a sequence, but only return peptides with a
     * given maximum mass
     * @param seq
     * @param MaxMass
     * @param cl
     * @return
     */
    public ArrayList<Peptide> digest(Peptide seq, double MaxMass, ArrayList<CrossLinker> cl) {
        int countPeptides = 0;
        int seqLength = seq.length();
        int pepSeqLen = 0;
        short prevMC=seq.getMissedCleavages();
        if (MaxMass == 0)
            return new ArrayList<Peptide>();
        
//        if ((int)((seqLength / m_AminoAcidsPerPeptide)) < 0)
//            System.err.println("seqLength / m_AminoAcidsPerPeptide = " + seqLength + " / " +
//                    m_AminoAcidsPerPeptide + " = " + (seqLength / m_AminoAcidsPerPeptide) );

        ArrayList<Peptide> peptides = new ArrayList<Peptide>(
                ((seqLength / (m_AminoAcidsPerPeptide + 1))));
        int pepStart = 0;
        LinkedList<Integer> missCleavStart = new LinkedList<Integer>();

        //System.out.println("\n" + seq.getFastaHeader());

        for (int i = 0 ; i < seqLength; i++ ) {
            //System.out.print(seq.aminoAcidAt(i).toString());

            if (isCleavageSite(seq, i)) {
                //System.out.print("|");


                Peptide pep=new Peptide(seq,pepStart, i - pepStart + 1);
                seq.setMissedCleavages((short)0);


                if ((pep.getMass() <= MaxMass)) {
                    addPeptide(pep, seq.getSequence(), peptides, cl);
                    countPeptides++;
                    pepSeqLen+=pep.length();

                    ListIterator<Integer> it = missCleavStart.listIterator();
                    short mc = (short) missCleavStart.size();
                    while (it.hasNext()) {
                        int mstart = it.next();
                        pep = new Peptide(seq,mstart, i - mstart + 1);
                        if (mc > prevMC)
                            pep.setMissedCleavages(mc);
                        else
                            pep.setMissedCleavages(prevMC);
                        mc --;

                        if (pep.getMass() <= MaxMass) { 
                            //peptides.add(pep);
                            addPeptide(pep, seq.getSequence(), peptides, cl);

                            countPeptides++;
                            pepSeqLen+=pep.length();
                        }
                    }


                    missCleavStart.add(pepStart);

                    if (missCleavStart.size()>m_maxMissCleavages) {
                        missCleavStart.removeFirst();
                    }

                } else { // if this one is to heavy then all misscleavages containing it would be to haevy as well
                    missCleavStart.clear();
                }
                pepStart = i + 1;
            }
        }
        //missCleavStart.removeLast();
        if ( pepStart < seqLength) {
            Peptide lastPep = new Peptide(seq,pepStart, seqLength - pepStart);

            if (lastPep.getMass() <= MaxMass) {

                addPeptide(lastPep, seq.getSequence(), peptides, cl);
                pepSeqLen+=lastPep.length();
                countPeptides++;
                    

                ListIterator<Integer> it = missCleavStart.listIterator();
                while (it.hasNext()) {
                    int mstart = it.next();
                    lastPep=new Peptide(seq,mstart, seqLength - mstart);
                    if (lastPep.getMass() <= MaxMass) { // && CrossLinker.canCrossLink(cl, lastPep)) {
                        //peptides.add(lastPep);
                        addPeptide(lastPep, seq.getSequence(), peptides, cl);
                        pepSeqLen+=lastPep.length();
                        countPeptides++;
                    }
                }
            }
        }


        // just take the average of all digested sequences for estimation of expected peptides
        if (!peptides.isEmpty()) {
            m_AminoAcidsPerPeptide = (m_AminoAcidsPerPeptide*m_peptides+ pepSeqLen)/(m_peptides+peptides.size());
            m_peptides += peptides.size();
            m_seqLength += seqLength;
        }



        return peptides;
    }
    
    

    /**
     * do the actual digestion of a sequence, but only return peptides with a
     * given maximum mass
     * @param seq
     * @param MaxMass
     * @param cl
     * @return
     */
    public ArrayList<Peptide> digest(Sequence seq, double MaxMass, ArrayList<CrossLinker> cl) {
        int countPeptides = 0;
        int seqLength = seq.length();
        int pepSeqLen = 0;
        if (MaxMass == 0)
            return new ArrayList<Peptide>();
        short prevMC =0;
        
//        if ((int)((seqLength / m_AminoAcidsPerPeptide)) < 0)
//            System.err.println("seqLength / m_AminoAcidsPerPeptide = " + seqLength + " / " +
//                    m_AminoAcidsPerPeptide + " = " + (seqLength / m_AminoAcidsPerPeptide) );

        ArrayList<Peptide> peptides = new ArrayList<Peptide>(
                ((seqLength / (m_AminoAcidsPerPeptide + 1))));
        int pepStart = 0;
        LinkedList<Integer> missCleavStart = new LinkedList<Integer>();

        //System.out.println("\n" + seq.getFastaHeader());

        for (int i = 0 ; i < seqLength; i++ ) {
            //System.out.print(seq.aminoAcidAt(i).toString());

            if (isCleavageSite(seq, i)) {
                //System.out.print("|");


                Peptide pep=new Peptide(seq,pepStart, i - pepStart + 1);
                pep.setMissedCleavages((short)0);


                if ((pep.getMass() <= MaxMass)) {
                    addPeptide(pep, seq, peptides, cl);
                    countPeptides++;
                    pepSeqLen+=pep.length();

                    ListIterator<Integer> it = missCleavStart.listIterator();
                    
                    short mc = (short)(missCleavStart.size());
                    while (it.hasNext()) {
                        int mstart = it.next();
                        pep = new Peptide(seq,mstart, i - mstart + 1);
                        pep.setMissedCleavages(mc);
//                        if (pep.toString().contentEquals("ARRK")) {
//                            System.err.println("here");
//                        }
                        if (pep.getMass() <= MaxMass) { // && CrossLinker.canCrossLink(cl, pep)) {
                            //peptides.add(pep);
                            addPeptide(pep, seq, peptides, cl);

                            countPeptides++;
                            pepSeqLen+=pep.length();
                        }
                    }


                    missCleavStart.add(pepStart);

                    if (missCleavStart.size()>m_maxMissCleavages) {
                        missCleavStart.removeFirst();
                    }

                } else { // if this one is to heavy then all misscleavages containing it would be to haevy as well
                    missCleavStart.clear();
                }
                pepStart = i + 1;
            }
        }
        //missCleavStart.removeLast();
        if ( pepStart < seqLength) {
            Peptide lastPep = new Peptide(seq,pepStart, seqLength - pepStart);

            if (lastPep.getMass() <= MaxMass) {

//                if (CrossLinker.canCrossLink(cl,lastPep))
//                    //peptides.add(lastPep);
                addPeptide(lastPep, seq, peptides, cl);
                pepSeqLen+=lastPep.length();
                countPeptides++;
                    

                ListIterator<Integer> it = missCleavStart.listIterator();
                while (it.hasNext()) {
                    int mstart = it.next();
                    lastPep=new Peptide(seq,mstart, seqLength - mstart);
                    if (lastPep.getMass() <= MaxMass) { // && CrossLinker.canCrossLink(cl, lastPep)) {
                        //peptides.add(lastPep);
                        addPeptide(lastPep, seq, peptides, cl);
                        pepSeqLen+=lastPep.length();
                        countPeptides++;
                    }
                }
            }
        }


        // just take the average of all digested sequences for estimation of expected peptides
        if (!peptides.isEmpty()) {
            m_AminoAcidsPerPeptide = (m_AminoAcidsPerPeptide*m_peptides+ pepSeqLen)/(m_peptides+peptides.size());
            m_peptides += peptides.size();
            m_seqLength += seqLength;
        }


//        includeMisscleaved(seq, peptides);

        return peptides;
    }

//    public void setPeptideLookup(PeptideLookup peptree) {
//        m_peptidetree = peptree;
//    }

    public void setPeptideLookup(PeptideLookup crosslinked, PeptideLookup linear) {
        m_peptidetree = crosslinked;
        m_peptideTreeLinear = linear;
    }

    public PeptideLookup getPeptideLookup() {
        return m_peptidetree;
    }

    /**
     * @return the m_AminoAcidsPerPeptide
     */
    public int getAminoAcidsPerPeptide() {
        return m_AminoAcidsPerPeptide;
    }

    /**
     * @param AminoAcidsPerPeptide the m_AminoAcidsPerPeptide to set
     */
    public void setAminoAcidsPerPeptide(int AminoAcidsPerPeptide) {
        this.m_AminoAcidsPerPeptide = AminoAcidsPerPeptide;
    }

    /**
     * @return the m_chars
     */
    public HashSet<AminoAcid> getDigestionAA() {
        return m_CTermAminoAcids;
    }

    /**
     * @param aminoAcids the amino acids that can be cleaved
     */
    public void setChars(HashSet<AminoAcid> aminoAcids) {
        this.m_CTermAminoAcids = (HashSet<AminoAcid>) aminoAcids.clone();
    }



//    protected int includeMisscleaved(Sequence s, ArrayList<Peptide> peptides) {
//        int countPeptides = peptides.size();
//        int countMisscleavedPeptides = countPeptides * m_maxMissCleavages -
//                (m_maxMissCleavages+1)*m_maxMissCleavages/2;
//
//        if (countMisscleavedPeptides <= 0)
//            return 0;
//
//        ArrayList<Peptide> miscleaved =
//                new ArrayList<Peptide>(countMisscleavedPeptides);
//
//        for (int m = 1; m<=m_maxMissCleavages; m++) {
//            for (int p = 0; p < countPeptides - m; p++) {
//                int pLength=0;
//                for (int cp = p; cp <= p+m;cp ++)
//                    pLength += peptides.get(cp).length();
//                Peptide mp = new Peptide(s, peptides.get(p).getStart(),pLength);
//                miscleaved.add(mp);
//            }
//        }
//        peptides.addAll(miscleaved);
//        return miscleaved.size();
//    }

    /**
     * @return the m_maxMissCleavages
     */
    public int getMaxMissCleavages() {
        return m_maxMissCleavages;
    }

    /**
     * @param maxMissCleavages the m_maxMissCleavages to set
     */
    public void setMaxMissCleavages(int max) {
        m_maxMissCleavages = max;
    }

    public static Digestion getDigestion(String className, String Options) {
        Class d= null;
        try {

            d = Class.forName("rappsilber.ms.sequence.digest." + className);
            Method m = d.getMethod("parseArgs", String.class);
            return (Digestion) m.invoke(null, Options);


        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Digestion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Digestion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Digestion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(Digestion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(Digestion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(Digestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }

    public static Digestion getDigestion(String className, String Options, RunConfig config ) {
        Class d= null;
        try {

            d = Class.forName("rappsilber.ms.sequence.digest." + className);
            Method m = d.getMethod("parseArgs", String.class, RunConfig.class);
            return (Digestion) m.invoke(null, Options, config);


        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Digestion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Digestion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Digestion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(Digestion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(Digestion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(Digestion.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }




    // TODO: You cannot instantiate an Abstract class! So must change the
    // the type of class Digestion or give a concrete implementation

//    public static Digestion parseArgs(String args) throws ParseException {
//
//        // Complete this and return a Digestion object
//        ArrayList<AminoAcid> DigestedAminoAcids = new ArrayList<AminoAcid>();
//        String name = null;
//
//        // parses something like: name:enzyme;DigestedAminoAcids:R,K
//        String[] options = args.split(";");
//        for (String a : options) {
//
//            // Strip the string of whitespace and make it uppercase for comparison
//            String[] as = a.split(":");
//            String aName = as[0].toUpperCase();
//            // the amino acid substring
//
//            if( aName.startsWith("DIGESTED") ){
//                String[] amino_acids = as[1].split(",");
//                for(String b : amino_acids)
//                    DigestedAminoAcids.add(AminoAcid.getAminoAcid(b));
//            } else if ( aName.contentEquals("NAME")) {
//               name = as[1];
//            }else{
//                throw new ParseException("Could not read type of Digested AA's from config file, " +
//                        " read: '" + args +"'", 0);
//            }
//        }
//        Digestion d = new Digestion(DigestedAminoAcids.toArray(new AminoAcid[0]), new AminoAcid[0]);
//
//        d.setName(name);
//
//        return d;
//    }


    public static Digestion parseArgs(String args, RunConfig config) throws ParseException {

        // Complete this and return a Digestion object
        ArrayList<AminoAcid> DigestedAminoAcids = new ArrayList<AminoAcid>();

        // parses something like: DigestedAminoAcids:R,K

            // Strip the string of whitespace and make it uppercase for comparison
            String x = (args.trim()).toUpperCase();
            // the amino acid substring
            String aa_substring = x.substring(x.indexOf(":") + 1);

            String[] amino_acids = aa_substring.split(",");
            if( x.startsWith("DIGESTED") ){
                for(String b : amino_acids)
                    DigestedAminoAcids.add(config.getAminoAcid(b.trim()));
            }else{
                throw new ParseException("Could not read type of Digested AA's from config file, " +
                        " read: '" + args +"'", 0);
            }

        return new Digestion(DigestedAminoAcids.toArray(new AminoAcid[0]),new AminoAcid[0], config);
    }

    /**
     * @return the name of the digestion method (enzyme)
     */
    public String Name() {
        return m_name;
    }

    /**
     *
     * @param name the name of the digestion method (enzyme)
     */
    public void setName(String name) {
        this.m_name = name;
    }

    /**
     * @return the m_minPeptideLength
     */
    protected int getMinPeptideLength() {
        return m_minPeptideLength;
    }

    /**
     * @param m_minPeptideLength the m_minPeptideLength to set
     */
    protected void setMinPeptideLength(int m_minPeptideLength) {
        this.m_minPeptideLength = m_minPeptideLength;
    }




}

