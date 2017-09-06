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

import java.util.HashSet;
import rappsilber.ms.sequence.ions.Fragment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import rappsilber.config.RunConfig;
import rappsilber.utils.Util;


// <editor-fold defaultstate="collapsed" desc=" UML Marker "> 
// #[regen=yes,id=DCE.3BFAEC1E-6771-9AF9-184F-46CAA052FBD4]
// </editor-fold> 
/**
 * Represents a Peptide after digestion
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class Peptide implements AminoAcidSequence{



    public static class PeptidePositions {
        public Sequence base;
        public int      start;
        //public short    length;

        public PeptidePositions(Sequence base, int start, int length) {
            this.base = base;
            this.start = start;
//            this.length = (short) length;
        }
        
        public int hashCode() {
            return base.hashCode() + start ;
        }
        
        public boolean equals(Object o) {
            PeptidePositions pp = (PeptidePositions) o;
            return o == this || (pp.base == this.base && pp.start == start);// && pp.length == length);
        }

    }

    private long m_id = -1;
    
//    private NonAminoAcidModification m_nterminal_modification = NonAminoAcidModification.NO_MODIFICATION;
//    private NonAminoAcidModification m_cterminal_modification = NonAminoAcidModification.NO_MODIFICATION;



//    /** list of possible fragments for this peptide */
//    private ArrayList<Fragment> m_Fragments = null;

    /** the mass of the peptide */
    private double  m_mass;

//    /** where in the sequence does the peptide start */
//    protected int     m_start;

    /** length of the peptide */
    protected short     m_length;

//    /** the sequence/protein, of that produced this peptide */
//    protected Sequence m_sequence;

    /**
     * defines, whether a peptide is N-terminal
     * So it contains the first amino-acid in a protein
     */
    private boolean   m_isNTerminal = false;
    /**
     * defines, whether a peptide is C-terminal
     * So it contains the last amino-acid in a protein
     */
    private boolean   m_isCTerminal = false;



    private PeptidePositions[] m_sources = null;
//    private HashMap<Sequence,HashSet<Integer>> m_startPositions = null;
//    private HashSet<Sequence> m_proteins = new HashSet<Sequence>();

    /**
     * for reduced memory consumption - the peptides can be reference by an index
     * and this is that index
     */
    private int     m_peptideIndex;


    /** 
     * if this is a modified peptied (a modification not part of the sequence)
     * then all modifications are listed here
     */
    private HashMap<Integer, AminoAcid> m_modificationSides =
            new HashMap<Integer, AminoAcid>();


    // <editor-fold desc=" Constructors ">
    /**
     * basic constructor
     * @param start start relative to the sequence
     * @param length length of the peptide
     */
    // <editor-fold defaultstate="collapsed" desc=" UML Marker "> 
    // #[regen=yes,id=DCE.4868D8DA-E3B5-E3F0-912A-FADA4EE15514]
    // </editor-fold> 
    public Peptide (Sequence sequence, int start, int length) {
        m_mass = 0;
//        m_start  = start;
        m_length = (short)length;
//        m_sequence = sequence;

//        m_isCTerminal = m_start + m_length == m_sequence.length();
//        m_isNTerminal = m_start == 0 || (m_start == 1 && m_sequence.aminoAcidAt(0) == AminoAcid.M);

        //addSource(this);
        addSource(sequence, start, length);        

        recalcMass();
    }

    /**
     * constructor that creates another peptide based on an existing peptide
     *
     * @param p     base peptide
     * @param start start relative to the peptide
     * @param length length of the new peptide
     */
    public Peptide (Peptide p, int start, int length) {
        this(p.getSequence(), p.getStart() + start, length);
        PeptidePositions[] pos = p.getPositions();
        m_sources = new PeptidePositions[pos.length];
//        m_sources = new PeptidePositions[pos.length];
        for (int pp = 0; pp<pos.length; pp++) {
            Sequence s = pos[pp].base;
            int ps = pos[pp].start + start;
            PeptidePositions npp = new PeptidePositions(s, ps, length);
            m_sources[pp] = npp;
            
//            HashSet<Integer> protPos = m_startPositions.get(s);
//            if (protPos == null) {
//                protPos = new HashSet<Integer>(1);
//                m_startPositions.put(s, protPos);
//            }
//            protPos.add(ps);
            
        }
        
//        for (int i = 0; i< pos.length; i++) {
//            m_sources[i] = new PeptidePositions(pos[i].base, pos[i].start+start, length);
//        }
        for (Integer i : p.m_modificationSides.keySet()) {
            if (i >= start && i < length) {
                AminoAcid mod = p.m_modificationSides.get(i);
                m_modificationSides.put(i, mod);
//                this.m_mass -= (p.aminoAcidAt(i).mass - mod.mass);
            }
        }
        
        recalcMass();

    }

    protected Peptide() {
        
    }

    /**
     * produces a copy of the Peptide
     * @param p base-peptide
     */
    public Peptide(Peptide p) {
        this(p.getSequence(), p.getStart(), p.m_length);
        this.m_sources = p.m_sources.clone();
//        this.m_startPositions = (HashMap<Sequence, HashSet<Integer>>) p.m_startPositions.clone();
//        this.m_startPositions = (HashMap<Sequence, HashSet<Integer>>) p.m_startPositions.clone();

        this.m_modificationSides =
                (HashMap<Integer, AminoAcid>) p.m_modificationSides.clone();
//        this.m_cterminal_modification = p.getCTerminalModification();
//        this.m_nterminal_modification = p.getNTerminalModification();
        this.m_mass = p.getMass();
    }

    /**
     * produces a copy of the Peptide that has the given modification at the given
     * aminoacid
     * @param p         base peptide
     * @param position  position of the modified aminoacid
     * @param am        the nmodification
     */
    public Peptide(Peptide p, int position, AminoAcid am) {
        this(p);
//        this.m_mass -= (p.aminoAcidAt(position + m_start).mass
//                           - am.mass);
        this.m_modificationSides.put(position, am);
        recalcMass();
    }


    // </editor-fold>Constructors


    /**
     * @return the m_length
     */
    public short getLength() {
        return m_length;
    }

    /**
     * @param m_length the m_length to set
     */
    public void setLength(short m_length) {
        this.m_length = m_length;
    }


    public AminoAcid[] toArray() {
        AminoAcid[] aas = new AminoAcid[getLength()];
        for (int i = getLength() ; --i >= 0;)
            if (m_modificationSides.containsKey(i))
                aas[i] = m_modificationSides.get(i);
            else
                aas[i] = getSequence().aminoAcidAt(getStart() + i);

        return aas;
    }


    /**
     * returns the sequence that this peptide was derivet from
     * @return sequence
     */
    // <editor-fold defaultstate="collapsed" desc=" UML Marker "> 
    // #[regen=yes,regenBody=yes,id=DCE.A3F6B512-096B-CD50-3754-0DD7B900EE3C]
    // </editor-fold> 
    public Sequence getSequence() {
        return m_sources[0].base;
        //return m_sequence;
    }

    public boolean isDecoy() {
        return getSequence().isDecoy();
    }


//    /**
//     * sets the sequence that this peptide was derivet from
//     */
//    // <editor-fold defaultstate="collapsed" desc=" UML Marker "> 
//    // #[regen=yes,regenBody=yes,id=DCE.7C471099-FC9B-0FD1-F4D9-F71146A78C28]
//    // </editor-fold> 
//    public void setSequence (Sequence val) {
//        m_sequence = val;
//    }

    /**
     * get the mass of the peptide
     * @return
     */
    // <editor-fold defaultstate="collapsed" desc=" UML Marker "> 
    // #[regen=yes,regenBody=yes,id=DCE.6EA13D73-970A-2A24-5AF6-46D2FC91D59A]
    // </editor-fold> 
    public double getMass () {
        return m_mass;
    }

    /**
     * get the mass of the peptide
     * @return
     */
    // <editor-fold defaultstate="collapsed" desc=" UML Marker "> 
    // #[regen=yes,regenBody=yes,id=DCE.94BA9698-8A79-43D5-9F46-1183196422FB]
    // </editor-fold> 
    public void setMass (double val) {
        m_mass = val;
    }

    /**
     * returns a list of fragments <br/>
     * If the function fragmentPrimary was called beforehand - then the precalculated
     * fragments are returned, else the peptide will be fragmented but the
     * result not stored.
     * @see fragmentPrimary()
     * @return
     */
    // <editor-fold defaultstate="collapsed" desc=" UML Marker "> 
    // #[regen=yes,regenBody=yes,id=DCE.5BDBC219-87C3-654F-4A74-36D13C0C5F72]
    // </editor-fold> 
    public ArrayList<Fragment> getPrimaryFragments () {

//        if (m_Fragments == null) {

            ArrayList<Fragment> f = new ArrayList<Fragment>(getLength() * 2);
            f = Fragment.fragment(this, false);
//            for (int i = 1; i < m_length; i++) {
//                f.add(new YIon(this, i));
//                f.add(new BIon(this, i));
//            }
//
//            f.add(new PeptideIon(this));

            return f;
//        } else
//            return m_Fragments;
    }

    /**
     * returns a list of fragments <br/>
     * If the function fragmentPrimary was called beforehand - then the precalculated
     * fragments are returned, else the peptide will be fragmented but the
     * result not stored.
     * @see fragmentPrimary()
     * @return
     */
    public ArrayList<Fragment> getPrimaryFragments (RunConfig config) {
//        if (toString().contentEquals("MoxAK")) {
//            System.err.println("found it");
//        }
//        if (m_Fragments == null) {

            ArrayList<Fragment> f = new ArrayList<Fragment>(getLength() * 2);
            f = Fragment.fragment(this, config, false);
//            for (int i = 1; i < m_length; i++) {
//                f.add(new YIon(this, i));
//                f.add(new BIon(this, i));
//            }
//
//            f.add(new PeptideIon(this));

            return f;
//        } else
//            return m_Fragments;
    }


    /**
     * returns a list of fragments <br/>
     * If the function fragmentPrimary was called beforehand - then the precalculated
     * fragments are returned, else the peptide will be fragmented but the
     * result not stored.
     * @see fragmentPrimary()
     * @return
     */
    // <editor-fold defaultstate="collapsed" desc=" UML Marker ">
    // #[regen=yes,regenBody=yes,id=DCE.5BDBC219-87C3-654F-4A74-36D13C0C5F72]
    // </editor-fold>
    public ArrayList<Fragment> getFragments () {

//        if (m_Fragments == null) {

            ArrayList<Fragment> f = new ArrayList<Fragment>(getLength() * 2);
            f = Fragment.fragment(this, true);
//            for (int i = 1; i < m_length; i++) {
//                f.add(new YIon(this, i));
//                f.add(new BIon(this, i));
//            }
//
//            f.add(new PeptideIon(this));

            return f;
//        } else
//            return m_Fragments;
    }

    /**
     * returns a list of fragments <br/>
     * If the function fragmentPrimary was called beforehand - then the precalculated
     * fragments are returned, else the peptide will be fragmented but the
     * result not stored.
     * @see fragmentPrimary()
     * @return
     */
    public ArrayList<Fragment> getFragments (RunConfig config) {

//        if (m_Fragments == null) {

            ArrayList<Fragment> f = new ArrayList<Fragment>(getLength() * 2);
            f = Fragment.fragment(this, config, true);
//            for (int i = 1; i < m_length; i++) {
//                f.add(new YIon(this, i));
//                f.add(new BIon(this, i));
//            }
//
//            f.add(new PeptideIon(this));

            return f;
//        } else
//            return m_Fragments;
    }


    /**
     * @return the start of the peptide within the sequence
     */
    public int getStart() {
        return m_sources[0].start;
    }

    /**
     * @return the length of the peptide
     */
    public int length() {
        return getLength();
    }


    /**
     * @return whether the peptide is NTerminal in respect to the originating
     * sequence
     */
    public boolean isNTerminal() {
        return m_isNTerminal;
    }

    /**
     * @return whether the peptide is NTerminal in respect to the originating
     * sequence
     */
    public boolean isProteinNTerminal() {
        return m_isNTerminal;
    }
    
    /**
     * @return whether the peptide is CTerminal in respect to the originating
     * sequence
     */
    public boolean isCTerminal() {
        return m_isCTerminal;
    }

    /**
     * @return whether the peptide is CTerminal in respect to the originating
     * sequence
     */
    public boolean isProteinCTerminal() {
        return m_isCTerminal;
    }
    
    /**
     * @return the list modification on this peptide
     */
    public HashMap<Integer,AminoAcid> getModification() {
        return m_modificationSides;
    }

//    /**
//     * fragments the peptide into b and y ions
//     * @return the number of fragments
//     */
//    public int fragmentPrimary() {
//        this.m_Fragments = null;
//        this.m_Fragments = getPrimaryFragments();
//        return m_Fragments.size();
//    }
//
//    /**
//     * fragments the peptide into b and y ions
//     * @return the number of fragments
//     */
//    public int fragmentPrimary(RunConfig conf) {
//        this.m_Fragments = null;
//        this.m_Fragments = getPrimaryFragments(conf);
//        return m_Fragments.size();
//    }


    /**
     * creates copies of this peptide, that are modified with the
     * registered modifications
     * @param conf configuration, describing what label to use
     * @see AminoModification.registerModification
     * @return
     */
    public ArrayList<Peptide> label(RunConfig conf) {
        ArrayList<Peptide> returnList = new ArrayList<Peptide>();
        Peptide pepLabeled = new Peptide(this);
        boolean labeled = false;
        for (AminoLabel al : conf.getLabel()) {
            if (containsAminoAcid(al.BaseAminoAcid)) {
                pepLabeled.replace(al.BaseAminoAcid, al);
                labeled = true;
            }
        }
        if (labeled)
            returnList.add(pepLabeled);
        return returnList;
    }


    /**
     * creates copies of this peptide, that are modified with the
     * registered ({@see AminoModification.registerModification}) modifications
     * @return 
     */
    public ArrayList<Peptide> modify(RunConfig conf) {
//        if (this.toString().contentEquals("NANKLLMQDGGIK"))
//            System.err.println("foudn it");

        ArrayList<Peptide> returnList = new ArrayList<Peptide>();
        ArrayList<NonAminoAcidModification> ctmods = conf.getVariableCterminalPeptideModifications();
        ArrayList<NonAminoAcidModification> ntmods = conf.getVariableNterminalPeptideModifications();
        modify(conf, returnList,0,0);
        if (ctmods.size() > 0 || ntmods.size() >0) {
            ArrayList<Peptide> returnList2 = new ArrayList<Peptide>();

            for (NonAminoAcidModification m : ctmods) {
                Peptide np = this.clone();
//                np.setCTerminalModification(m);
                np.recalcMass();
                returnList2.add(np);

                for (NonAminoAcidModification mn : ntmods) {
                    Peptide np2 = np.clone();
//                    np2.setNterminalModification(mn);
                    returnList2.add(np2);
                }
            }

            for (NonAminoAcidModification m : ntmods) {
                Peptide np = this.clone();
//                np.setNterminalModification(m);
                returnList2.add(np);
            }



            for (Peptide p : returnList)  {
                int mods = p.m_modificationSides.size();
                if (mods < rappsilber.utils.Util.MaxModificationPerPeptide) {
                    for (NonAminoAcidModification m : ctmods) {
                        Peptide np = p.clone();
//                        np.setCTerminalModification(m);
                        returnList2.add(np);

                        if (mods < rappsilber.utils.Util.MaxModificationPerPeptide -1)
                            for (NonAminoAcidModification mn : ntmods) {
                                Peptide np2 = np.clone();
//                                np2.setNterminalModification(mn);
                                returnList2.add(np2);
                            }
                    }

                    for (NonAminoAcidModification m : ntmods) {
                        Peptide np = p.clone();
//                        np.setNterminalModification(m);
                        returnList2.add(np);
                    }
                }

            }
            returnList.addAll(returnList2);
        }
        return returnList;
    }

    /**
     * creates copies of this peptide, that are modified with the
     * registered modifications
     * @see AminoModification.registerModification
     * @return
     */
    public ArrayList<Peptide> modify() {
        ArrayList<Peptide> returnList = new ArrayList<Peptide>();
        modify(returnList,0,0);
        return returnList;
    }


    /**
     * changes one amino acid with another one -&gt; applies a modification
     * @param position where to modify
     * @param aa        what should be there
     * @return the previous amino-acid or modified amino-acid at the given place
     */
    public AminoAcid modify(int position, AminoAcid aa) {
        AminoAcid prev = m_modificationSides.put(position,aa);
        if (prev==null) {
            prev = aminoAcidAt(position);
//            m_mass -= (m_sequence.aminoAcidAt(m_start + position).mass - aa.mass);
        }
        recalcMass();
        return prev;
    }

    /**
     * recursively generates all possible peptides with all possible
     * modifications
     * @param returnList
     * @param startPosition
     */
    private int modify(ArrayList<Peptide> returnList, int startPosition, int modifiedPeptides) {
        ArrayList<Peptide> modified = new ArrayList<Peptide>();

        if (modifiedPeptides > rappsilber.utils.Util.MaxModifiedPeptidesPerPeptide)
            return modifiedPeptides;

        if (this.m_modificationSides.size() > rappsilber.utils.Util.MaxModificationPerPeptide)
            return modifiedPeptides;

//        int countMods = previousModifications;

        // get the possible modifications for the current aminoacid
        AminoAcid aa = aminoAcidAt(startPosition);


        // get the possible modifications for the current aminoacid
        ArrayList<AminoModification> mods =
                AminoModification.getVariableModifications(aa);
        if (mods != null) {
            int count = mods.size();
            for (int i = 0; i < count; i++) {
                AminoModification am = mods.get(i);
                if ((am.pep_position == AminoModification.POSITIONAL_UNRESTRICTED || 
                        am.pep_position>=0 && startPosition == am.pep_position ||
                        am.pep_position<0 && startPosition == this.length()-am.pep_position+1) && 
                        (am.prot_position == AminoModification.POSITIONAL_UNRESTRICTED || 
                        am.prot_position>=0 && startPosition+this.getStart() == am.prot_position ||
                        am.prot_position<0 && startPosition+this.getStart() == this.getSequence().length()-am.prot_position+1)) {
                    Peptide modPep = new Peptide(this,startPosition, mods.get(i));
                    modified.add(modPep);
                    returnList.add(modPep);
                    modifiedPeptides++;
                }
            }
        }
        if (startPosition < this.getLength() - 1) {
            modifiedPeptides = modify(returnList, startPosition + 1, modifiedPeptides);
            for (Peptide toMod : modified) {
                if (modifiedPeptides > rappsilber.utils.Util.MaxModificationPerPeptide)
                    break;
                modifiedPeptides = toMod.modify(returnList, startPosition + 1, modifiedPeptides);
            }
        }


        return modifiedPeptides;

    }


    /**
     * recursively generates all possible peptides with all possible
     * modifications
     * @param returnList
     * @param startPosition
     */
    private int modify(RunConfig conf, ArrayList<Peptide> returnList, int startPosition, int modifiedPeptides) {
        ArrayList<Peptide> modified = new ArrayList<Peptide>();

        if (modifiedPeptides > rappsilber.utils.Util.MaxModifiedPeptidesPerPeptide)
            return modifiedPeptides;

        if (this.m_modificationSides.size() >= rappsilber.utils.Util.MaxModificationPerPeptide)
            return modifiedPeptides;


//        int countMods = previousModifications;
        
        // get the possible modifications for the current aminoacid
        AminoAcid aa = aminoAcidAt(startPosition);


        ArrayList<AminoModification> mods =
                conf.getVariableModifications(aa);
        
        if (mods != null) {
            int count = mods.size();
            for (int i = 0; i < count; i++) {
                AminoModification am = mods.get(i);
                if ((am.pep_position == AminoModification.POSITIONAL_UNRESTRICTED || 
                        am.pep_position == AminoModification.POSITIONAL_NTERMINAL && startPosition == 0 ||
                        am.pep_position== AminoModification.POSITIONAL_CTERMINAL && startPosition == this.length()-1) && 
                        (am.prot_position == AminoModification.POSITIONAL_UNRESTRICTED || 
                        am.prot_position == AminoModification.POSITIONAL_NTERMINAL && startPosition == 0 && this.isNTerminal() ||
                        am.prot_position == AminoModification.POSITIONAL_CTERMINAL && startPosition == this.length() -1 && this.isCTerminal())) {
                    Peptide modPep = new Peptide(this,startPosition, mods.get(i));
                    modified.add(modPep);
                    returnList.add(modPep);
                    modifiedPeptides++;
                }
            }
        }
        
        if (startPosition == 0) {
            List<NonAminoAcidModification> ntermmods = conf.getVariableNterminalPeptideModifications();
            for (NonAminoAcidModification ntm : ntermmods) {
                if (ntm.canModify(this)) {
                    Peptide modPep = new Peptide(this);
//                    modPep.setNterminalModification(ntm);
                    modified.add(modPep);
                    returnList.add(modPep);
                    modifiedPeptides++;
                }
            }
        }

        if (startPosition == this.getLength() -1) {
            List<NonAminoAcidModification> ctermmods = conf.getVariableCterminalPeptideModifications();
            for (NonAminoAcidModification ctm : ctermmods) {
                if (ctm.canModify(this)) {
                    Peptide modPep = new Peptide(this);
//                    modPep.setCTerminalModification(ctm);
                    modified.add(modPep);
                    returnList.add(modPep);
                    modifiedPeptides++;
                }
            }
        }
        
        if (startPosition < this.getLength() - 1) {
            modifiedPeptides = modify(conf,returnList, startPosition + 1, modifiedPeptides);
            for (Peptide toMod : modified) {
                if (modifiedPeptides > rappsilber.utils.Util.MaxModifiedPeptidesPerPeptide)
                    break;
                modifiedPeptides = toMod.modify(conf,returnList, startPosition + 1, modifiedPeptides);
            }
        }

        
        return modifiedPeptides;

    }

    /**
     * Converts the underlying sequence into a string by concatenating the amino acid ids
     * @return a string representation of the sequence
     */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder(getLength());
//        out.append(m_nterminal_modification.toString());

        if (m_modificationSides.size() > 0) {
            // TODO more than one modification
            for (int i = 0 ; i< getLength(); i++) {
                if (m_modificationSides.containsKey(i))
                    out.append(m_modificationSides.get(i).SequenceID);
                else
                    out.append(getSequence().aminoAcidAt(getStart() + i).SequenceID);
            }
        } else
            for (int i = 0 ; i< getLength(); i++)
                out.append(getSequence().aminoAcidAt(getStart() + i));

//        out.append(m_cterminal_modification.toString());

        return out.toString();
    }

    /**
     * Converts the underlying sequence into a string by concatenating the amino acid ids
     * @return a string representation of the sequence
     */
    public String toString(double[] weights) {
        StringBuilder out = new StringBuilder(getLength());
//        out.append(m_nterminal_modification.toString());

        if (m_modificationSides.size() > 0) {
            // TODO more than one modification
            for (int i = 0 ; i< getLength(); i++) {
                if (m_modificationSides.containsKey(i))
                    out.append(m_modificationSides.get(i).SequenceID);
                else
                    out.append(getSequence().aminoAcidAt(getStart() + i).SequenceID);
                if (weights[i] != 0)
                     out.append("(" + Util.twoDigits.format(weights[i]) + ")");
            }
        } else
            for (int i = 0 ; i< getLength(); i++) {
                out.append(getSequence().aminoAcidAt(getStart() + i));
                if (weights[i] != 0)
                     out.append("(" + Util.twoDigits.format(weights[i]) + ")");
            }

//        out.append(m_cterminal_modification.toString());

        return out.toString();
    }


    /**
     * Converts the underlying sequence into a string by concatenating the amino acid ids
     * @return a string representation of the sequence
     */
    public String toStringBaseSequence() {
        StringBuilder out = new StringBuilder(getLength());

        for (int i = 0 ; i< getLength(); i++) {
            AminoAcid aa = getSequence().aminoAcidAt(getStart() + i);
            if (aa instanceof AminoModification)
                aa = ((AminoModification)aa).BaseAminoAcid;
            out.append(aa);
        }

        return out.toString();
    }

    public AminoAcid aminoAcidAt(int pos) {
        AminoAcid aa = m_modificationSides.get(pos);
        if (aa != null)
            return aa;
        else
            return getSequence().aminoAcidAt(getStart() + pos);
    }

    public AminoAcid nonLabeledAminoAcidAt(int pos) {
        AminoAcid aa = m_modificationSides.get(pos);
        if (aa != null)
            return aa;
        else
            return getSequence().nonLabeledAminoAcidAt(getStart() + pos);
    }

    public boolean containsAminoAcid(AminoAcid aa) {
        return SequenceUtils.containsAminoAcid(this, aa);
    }

    public Peptide subSequence(short from, short length) {
        return new Peptide(this, from, length);
    }

    public int countAminoAcid(HashSet<AminoAcid> aas) {
        return SequenceUtils.countAminoAcid(this, aas);
    }

    public boolean containsAminoAcids(HashSet<AminoAcid> aas) {
        return SequenceUtils.containsAminoAcid(this, aas);
    }

    public void free() {
//        if (m_Fragments != null) {
//            for (Fragment f : m_Fragments)
//                f.free();
//            m_Fragments.clear();
//            m_Fragments = null;
//        }
        m_modificationSides.clear();
        m_modificationSides = null;
//        m_sequence = null;
    }

    /**
     * recalculates the mass of an peptide <br/>
     * E.g. when the mass of an amino-acid was modified globally)
     */
    public void recalcMass() {
        int start =getStart();
        m_mass = 0;
        for (int i = length(); --i>=0 ; ) {
            m_mass += aminoAcidAt(i).mass;
        }

        
//        m_mass += m_nterminal_modification.Mass;
//        m_mass += m_cterminal_modification.Mass;



//        for (int i = start + length() - 1; i>=start; i-- )
//            m_mass += m_sequence.aminoAcidAt(i).mass;
        m_mass += Util.WATER_MASS;

    }

    public AminoAcid setAminoAcidAt(int pos, AminoAcid aa) {
        AminoAcid aaOld = aminoAcidAt(pos);
        modify(pos, aa);
        //m_mass += aa.mass - aaOld.mass;
        return aaOld;
    }

    public int replace(AminoAcid oldAA, AminoAcid newAA) {
        int found = 0;
        int length = length();
        for (int i=0; i < length; i++ ) {
            if (aminoAcidAt(i) == oldAA) {
                setAminoAcidAt(i, newAA);
                found++;
            }
        }
        m_mass += (newAA.mass - oldAA.mass) * found;
        return found;
    }
    
    public int replace(AminoModification AAM) {
        return replace(AAM.BaseAminoAcid, AAM);
    }


    public Peptide clone() {
        Peptide p = new Peptide(this);
        p.setPeptideIndex(m_peptideIndex);
        return p;
    }

    public AminoAcidSequence getSourceSequence() {
        return getSequence();
    }


    public boolean equalSequence(AminoAcidSequence aas) {
        if (aas.length() == length()) {
            for (int i = length() - 1; i>= 0; i--) {
                if (aas.aminoAcidAt(i) != aminoAcidAt(i))
                    return false;
            }
            return true;
        }
        return false;
    }

    public boolean equalSequenceAAMass(AminoAcidSequence aas) {
        if (aas.length() == length()) {
            for (int i = length() - 1; i>= 0; i--) {
                if (aas.aminoAcidAt(i).mass != aminoAcidAt(i).mass)
                    return false;
            }
            return true;
        }
        return false;
    }


    public void addSource(Sequence seq, int start, int length) {
        if (m_sources == null) {
            m_sources = new PeptidePositions[1];
            m_sources[0] = new PeptidePositions(seq, start, length);
        } else {
            // don't add an already existing position
            for (PeptidePositions pp : getPositions()) {
                if (pp.base == seq && pp.start == start) 
                    return;
            }
            
            PeptidePositions[] ns = new PeptidePositions[m_sources.length + 1];
            System.arraycopy(m_sources, 0, ns, 0, m_sources.length);
            ns[m_sources.length] = new PeptidePositions(seq, start, length);

            m_sources = ns;
            
        }

        m_isCTerminal = m_isCTerminal || start + length == seq.length();
        m_isNTerminal = m_isNTerminal || start == 0 || (start == 1 && seq.aminoAcidAt(0) == AminoAcid.M);

    }

    public void addSource(Peptide pep) {
        addSource(pep.getSequence(), pep.getStart(), pep.length());
    }

    public PeptidePositions[] getPositions() {
        if (m_sources.length > 100)
            return new PeptidePositions[]{new PeptidePositions(Sequence.UNSPECIFIC_SEQUENCE, 0, this.length())};
        return m_sources;
    }

    protected void setPositions(PeptidePositions[] pos) {
        m_sources = pos;
    }

    public int getProteinCount() {
        HashSet s = new HashSet();
        for (PeptidePositions pp : m_sources) {
            s.add(pp.base);
        }
        return s.size();
    }

    public int getOccurenceCount() {
        return m_sources.length;
    }


    public long getID() {
        return m_id;
    }

    public void setID(long id) {
        m_id = id;
    }


    /**
     * @return the m_peptideIndex
     */
    public int getPeptideIndex() {
        return m_peptideIndex;
    }

    /**
     * @param m_peptideIndex the m_peptideIndex to set
     */
    public void setPeptideIndex(int peptideIndex) {
        this.m_peptideIndex = peptideIndex;
    }


//    /**
//     * @return the NTerminalModification
//     */
//    public NonAminoAcidModification getNTerminalModification() {
//        return m_nterminal_modification;
//    }
//
//    /**
//     * @param NTerminalModification the NTerminalModification to set
//     */
//    public void setNterminalModification(NonAminoAcidModification NTerminalModification) {
//        double  md = 0;
//        if (NTerminalModification != null)
//            md = NTerminalModification.Mass;
//        if (this.m_nterminal_modification != null)
//            md -= this.m_nterminal_modification.Mass;
//        this.m_nterminal_modification = NTerminalModification;
//        this.m_mass += md;
//    }
//
//    /**
//     * @return the m_cterminal_modification
//     */
//    public NonAminoAcidModification getCTerminalModification() {
//        return m_cterminal_modification;
//    }
//
//    /**
//     * @param m_cterminal_modification the m_cterminal_modification to set
//     */
//    public void setCTerminalModification(NonAminoAcidModification CTerminalModification) {
//        double md = CTerminalModification.Mass - this.m_cterminal_modification.Mass;
//        this.m_cterminal_modification = CTerminalModification;
//        this.m_mass += md;
//    }
    
    
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
                if (current < length())
                    return aminoAcidAt(current++);
                return null;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

}

