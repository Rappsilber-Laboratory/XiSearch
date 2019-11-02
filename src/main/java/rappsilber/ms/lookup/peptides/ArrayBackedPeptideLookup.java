/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.ms.lookup.peptides;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.rappsilber.utils.ArrayListCoSwap;
import org.rappsilber.utils.DoubleArrayList;
import org.rappsilber.utils.IntArrayList;
import rappsilber.config.RunConfig;
import rappsilber.ms.Range;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.Iterators.PeptideIterator;
import rappsilber.ms.sequence.ModificationType;
import rappsilber.ms.sequence.NonProteinPeptide;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.sequence.digest.Digestion;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class ArrayBackedPeptideLookup {//implements PeptideLookup{
//
//    DoubleArrayList masses = new DoubleArrayList(1000000);
//    IntArrayList pepIDs = new IntArrayList(10000);
//    SequenceList list;
////    ArrayListCoSwap<Peptide> peptides = new ArrayListCoSwap<>();
//    private ToleranceUnit m_tolerance = new ToleranceUnit(0, "da");
//    private long          m_peptideCount = 0;
//    private double        m_minimumMass = Double.MAX_VALUE;
//    private double        m_MaximumMass = Double.MIN_VALUE;
//    HashMap<Integer,IntArrayList> tempPepIds;
//    HashMap<Integer,IntArrayList> tempMassToPep;
//
//    private PeptideTree   m_discarded_decoys;
//
//    public ArrayBackedPeptideLookup(ToleranceUnit t) {
//        m_tolerance = t;
//        if (t != ToleranceUnit.ZEROTOLERANCE)
//             m_discarded_decoys = new PeptideTree(ToleranceUnit.ZEROTOLERANCE);
//    }
//
//    public ArrayBackedPeptideLookup(SequenceList sequences, ToleranceUnit t) {
//        m_discarded_decoys = new PeptideTree(ToleranceUnit.ZEROTOLERANCE);
//        PeptideIterator iter = sequences.peptides();
//        m_tolerance = t;
//        while (iter.hasNext()) {
//            this.addPeptide(iter.next());
//        }
//        masses.quicksort(pepIDs);
//    }
//    
//    
//
//    public void addPeptide(Peptide pep) {
//        if (pep != null) {
//            if (Double.isInfinite(pep.getMass()))
//                return;
//            
//            pepIDs.add(pep.getPeptideIndex());
//            masses.add(pep.getMass());
//            m_peptideCount++;
//        }
//    }
//    
//    
//
//    protected HashSet<Integer> getPeptideIds(double min, double max) {
//        HashSet<Integer> pepids  =new HashSet<>();
//        // find the smallest entry larger then min
//        int smallest=this.masses.size()/2;
//        int step = smallest;
//        double m0;
//        double m1;
//        m0 = (smallest>0? masses.get(smallest-1) : 0);
//        m1 = (smallest<this.masses.size()? this.masses.get(smallest) : 0);
//
//        while ((m0>=min || m1<min) && step>0) {
//            step=step/2;
//            if (m0 < min) {
//                smallest = smallest + step;
//            } else {
//                smallest = smallest - step;
//            }
//            m0 = (smallest>0? this.masses.get(smallest-1) : 0);
//            m1 = (smallest<this.masses.size()? this.masses.get(smallest) : 0);
//        }
//        int next = smallest;
//        while (next<this.masses.size() && this.masses.get(next)<=max) {
//            pepids.add((int)pepIDs.get(next++));
//        }
//        return pepids;
//    }
//    
//    
//
//    
//    
//
//
//    public ArrayList<Peptide> getForMassRange(double minmass, double maxmass, double referenceMass) {
//        double min = m_tolerance.getMinRange(minmass,referenceMass);
//        double max = m_tolerance.getMaxRange(maxmass,referenceMass);
//        Collection<Integer> pepids = getPeptideIds(min,max);
//
//        ArrayList<Peptide> ret  =new ArrayList<>(pepids.size());
//        for (int id : pepids) {
//            Peptide p  = list.getPeptide(id);
//            ret.add(list.getPeptide(id));
//        }
//        return ret;
//    }
//    
//    
//    public ArrayList<Peptide> getForExactMassRange(double minmass, double maxmass) {
//        Collection<Integer> pepids = getPeptideIds(minmass,maxmass);
//
//        ArrayList<Peptide> ret  =new ArrayList<>(pepids.size());
//        for (int id : pepids) {
//            Peptide p  = list.getPeptide(id);
//            ret.add(list.getPeptide(id));
//        }
//        return ret;
//    }
//    
//    public void setTolerance(ToleranceUnit tolerance) {
//        m_tolerance = tolerance;
//    }
//
//    public PeptideIterator iterator() {
//
//        return new PeptideIterator() {
//            int nextPos = 0;
//            Peptide currentPeptide = null;
//
//
//            public boolean hasNext() {
//                return nextPos<pepIDs.size();
//            }
//
//            public Peptide next() {
//                currentPeptide = list.getPeptide(pepIDs.get(nextPos++));
//                return currentPeptide;
//            }
//
//
//            public void remove() {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            public Sequence getCurrentSequence() {
//                return currentPeptide.getSequence();
//                
//            }
//
//            @Override
//            public Peptide current() {
//                return currentPeptide;
//            }
//        };
//    }
//
//    public PeptideIterator iteratorAfter(Peptide p) {
//        PeptideIterator it = iterator();
//        if (p == null)
//            return it;
//        Peptide  pep = it.next();
//        while (pep != null && !pep.equals(p))  {
//            pep = it.next();
//        }
//        return it;
//    }
//
//
//    @Override
//    public ArrayList<Peptide> getPeptidesByExactMass(double mass) {
//        return getForExactMassRange(mass, mass);
//    }
//
//    public void dump(Writer w) throws IOException {
//        throw new UnsupportedOperationException("not implemented yet");
//    }
//
//    public void dump(String path) throws IOException {
//        dump(new FileWriter(path));
//    }
//
//
//    public long getPeptideCount(){
//        return m_peptideCount;
//    }
//
//    @Override
//    public int size() {
//        return (int) getPeptideCount();
//    }
//    
//    /**
//     * tests whether a peptide of the given sequence already exists in the tree
//     * @param pep
//     * @return 
//     */
//    public boolean containsPeptide(Peptide pep) {
//        Double mass = pep.getMass();
//        ArrayList<Peptide> peps = getForExactMassRange(pep.getMass(), pep.getMass());
//        for (Peptide p : peps) {
//            if (p.equalSequence(pep)) {
//                return true;
//            }
//        }
//        return false;
//        
//    }
//
//    /**
//     * tries to replace decoy-peptides that where also found as target peptides
//     * with randomly generated decoy-peptides. And thereby providing more equal 
//     * decoy database.
//     * @param conf the configuration - needed to get a list of amino-acids
//     * @return the list of peptides, that could not be replaced by an unique
//     * random peptide of same length
//     */
//    public ArrayList<Peptide> addDiscared(RunConfig conf) {
//        ArrayList<Peptide> nonreplacedPeps = new ArrayList<Peptide>();
//        for (Peptide p : m_discarded_decoys) {
//            boolean added = false;
//            for (int r = 0; r<10; r++) {
//                Peptide rand = NonProteinPeptide.randomPeptide(p, conf, true);
//                if (!containsPeptide(rand)) {
//                    addPeptide(rand);
//                    added = true;
//                    break;
//                }
//            }
//            if (!added) {
//                nonreplacedPeps.add(p);
//            }
//        }
//        return nonreplacedPeps;
//    }
//
//    /**
//     * tries to replace decoy-peptides that where also found as target peptides
//     * with permuted decoy-peptides. And thereby providing more equal 
//     * decoy database.<br>
//     * The advantage against using random peptide is, that the system stays 
//     * reproducible. (The permutation here used does not do a random permutation)
//     * @param conf the configuration - needed to get a list of amino-acids
//     * @return the list of peptides, that could not be replaced by an unique
//     * permuted peptide
//     */
//    public ArrayList<Peptide> addDiscaredPermut(RunConfig conf) {
//        ArrayList<Peptide> nonreplacedPeps = new ArrayList<Peptide>();
//        for (Peptide p : m_discarded_decoys) {
//            boolean added = false;
//            for (Peptide perm : p.permute(conf)) {
//                if (!containsPeptide(perm)) {
//                    addPeptide(perm);
//                    added = true;
//                    break;
//                }
//            }
//            if (!added) {
//                nonreplacedPeps.add(p);
//            }
//        }
//        return nonreplacedPeps;
//    }
//
//    
//    public void forceAddDiscarded() {
//        for (Peptide p :m_discarded_decoys) {
//            PeptideLookupElement e = this.get(p.getMass());
//            e.add(p);
//            m_peptideCount++;
//        }
//    }
//
//    public void cleanup() {
//        // as we can't delete elements, while iterating we have to delte them in a second step
//        ArrayList<PeptideLookupElement> eDelete = new ArrayList<PeptideLookupElement>();
//        
//        for (PeptideLookupElement ple : this.values()) {
//            ArrayList<Peptide> pepDelete = new ArrayList<Peptide>();
//            for (Peptide p : ple) {
//                if (p.getProteinCount() > 10) {
//                    pepDelete.add(p);
//                    m_peptideCount--;
//                }
//            }
//            for (Peptide p : pepDelete) {
//                ple.remove(p);
//            }
//
//            if (ple.size() == 0) {
//                eDelete.add(ple);
//            }
//        }
//
//        for (PeptideLookupElement ple : eDelete) {
//            this.remove(ple.getMass());
//        }
//    }
//
//    public void cleanup(int minLength) {
//        ArrayList<PeptideLookupElement> eDelete = new ArrayList<PeptideLookupElement>();
//        for (PeptideLookupElement ple : this.values()) {
//            ArrayList<Peptide> pepDelete = new ArrayList<Peptide>();
//            for (Peptide p : ple) {
//                if (p.getProteinCount() > 10 || p.length()<minLength) {
//                    pepDelete.add(p);
//                    m_peptideCount--;
//                }
//
//            }
//            for (Peptide p : pepDelete) {
//                ple.remove(p);
//            }
//
//            if (ple.size() == 0) {
//                eDelete.add(ple);
//            }
//        }
//
//        for (PeptideLookupElement ple : eDelete) {
//            this.remove(ple.getMass());
//        }
//    }
//
//    @Override
//    public void cleanup(int minLength, int maxAmbiguity) {
//        this.cleanup(minLength, maxAmbiguity, 10);
//    }
//    
//    @Override
//    public void cleanup(int minLength, int maxAmbiguity, int maxProtAmbiguity) {
//        ArrayList<PeptideLookupElement> eDelete = new ArrayList<PeptideLookupElement>();
//        for (PeptideLookupElement ple : this.values()) {
//            ArrayList<Peptide> pepDelete = new ArrayList<Peptide>();
//            for (Peptide p : ple) {
//                if (p.getPositions().length>maxAmbiguity || p.getProteinCount() > maxProtAmbiguity || p.length()<minLength ) {
//                    pepDelete.add(p);
//                    m_peptideCount--;
//                }
//
//            }
//            for (Peptide p : pepDelete) {
//                ple.remove(p);
//            }
//
//            if (ple.size() == 0) {
//                eDelete.add(ple);
//            }
//        }
//
//        for (PeptideLookupElement ple : eDelete) {
//            this.remove(ple.getMass());
//        }
//    }
//    
//    @Override
//    public void applyVariableModifications(RunConfig conf) {
//        Digestion enzym = conf.getDigestion_method();
//        ArrayList<CrossLinker> cl = conf.getCrossLinker();
//        Iterator<Peptide> peps = this.iterator();
//        ArrayList<Peptide> newPeps = new ArrayList<Peptide>();
//        int cm = this.size();
//
//        int c = 0;
//        while (peps.hasNext()) {
//            if (c++ % 10000 == 0) {
//                    conf.getStatusInterface().setStatus("Applying variable modification " +  Util.twoDigits.format(c*100.0/cm) + "%" );
//            }
//
//            Peptide pep = peps.next();
//            for (Peptide p : pep.modify(conf, ModificationType.variable)) {
//                if (enzym.isDigestedPeptide(p)) {
//                    newPeps.add(p);
//                    p.getSequence().getPeptides().add(p);
//                }
//            }
//        }
//        for (Peptide p : newPeps)
//            addPeptide(p);
//    }
//
//    @Override
//    public void applyVariableModificationsLinear(RunConfig conf,PeptideLookup Crosslinked) {
//        Digestion enzym = conf.getDigestion_method();
//        ArrayList<CrossLinker> cl = conf.getCrossLinker();
//        Iterator<Peptide> peps = this.iterator();
//        ArrayList<Peptide> newPeps = new ArrayList<Peptide>();
//        int cm = this.size();
//
//        int c = 0;
//        while (peps.hasNext()) {
//            if (c++ % 10000 == 0) {
//                    conf.getStatusInterface().setStatus("Applying variable modification " +  Util.twoDigits.format(c*100.0/cm) + "%" );
//            }
//
//            Peptide pep = peps.next();
//            for (Peptide p : pep.modify(conf,ModificationType.variable)) {
//                if (enzym.isDigestedPeptide(p)) {
//                    newPeps.add(p);
//                    p.getSequence().getPeptides().add(p);
//                }
//            }
//        }
//        for (Peptide p : newPeps)
//            if (CrossLinker.canCrossLink(cl, p))
//                Crosslinked.addPeptide(p);
//            else
//                addPeptide(p);
//    }
//
//
//    public void applyVariableModifications(RunConfig conf, PeptideLookup linear) {
//
//        Digestion enzym = conf.getDigestion_method();
//        ArrayList<CrossLinker> cl = conf.getCrossLinker();
//        Iterator<Peptide> peps = this.iterator();
//        ArrayList<Peptide> newPeps = new ArrayList<Peptide>();
//
//        int cm = this.size();
//
//        int c = 0;
//        while (peps.hasNext()) {
//            if (c++ % 10000 == 0) {
//                    conf.getStatusInterface().setStatus("Applying variable modification " +  Util.twoDigits.format(c*100.0/cm) + "%" );
//            }
//            Peptide pep = peps.next();
//            ArrayList<Peptide> mps= pep.modify(conf, ModificationType.variable);
//            for (Peptide p : mps) {
//                if (enzym.isDigestedPeptide(p)) {
//                    newPeps.add(p);
//                    p.getSequence().getPeptides().add(p);
//                }
//            }
//        }
//        for (Peptide p : newPeps) {
//            if (CrossLinker.canCrossLink(cl, p))
//                addPeptide(p);
//            else
//                linear.addPeptide(p);
//        }
//    }
//
//    public double getMinimumMass() {
//        return m_minimumMass;
//    }
//
//    public double getMaximumMass() {
//        return m_MaximumMass;
//    }
//
    
}
