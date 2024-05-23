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
package rappsilber.ms.lookup.peptides;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import rappsilber.config.RunConfig;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.Iterators.PeptideIterator;
import rappsilber.ms.sequence.ModificationType;
import rappsilber.ms.sequence.NonProteinPeptide;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.sequence.digest.Digestion;
import rappsilber.ms.statistics.utils.StreamingAverageMedianStdDev;
import rappsilber.utils.Util;

public class PeptideTree extends TreeMap<Double, PeptideLookupElement> implements PeptideLookup{
    private static final long serialVersionUID = -4584307853042554730L;

    private ToleranceUnit m_tolerance = new ToleranceUnit(0, "da");
    private long          m_peptideCount = 0;
    private double        m_minimumMass = Double.MAX_VALUE;
    private double        m_MaximumMass = Double.MIN_VALUE;

    private PeptideTree   m_discarded_decoys;

    public PeptideTree(ToleranceUnit t) {
        m_tolerance = t;
        if (t != ToleranceUnit.ZEROTOLERANCE) {
            m_discarded_decoys = new PeptideTree(ToleranceUnit.ZEROTOLERANCE);
        }
    }

    public PeptideTree(SequenceList sequences, ToleranceUnit t) {
        m_discarded_decoys = new PeptideTree(ToleranceUnit.ZEROTOLERANCE);
        PeptideIterator iter = sequences.peptides();
        m_tolerance = t;
        while (iter.hasNext()) {
            this.addPeptide(iter.next());
        }
    }
    
    

    public void addPeptide(Peptide pep) {
        if (pep != null) {
            if (Double.isInfinite(pep.getMass())) {
                return;
            }
            
            Double mass = pep.getMass();
            PeptideLookupElement e;
            if (this.containsKey(mass)) {
                e = this.get(mass);
                boolean decoy = pep.isDecoy();
                

                Peptide same = e.getElementBySequenceAAMass(pep);
                if (same != null) {
                    if (decoy== same.isDecoy())  {
                        same.addSource(pep);
                    } else {
                        if (!decoy) {
                            e.remove(same);
                            e.add(pep);
                            m_discarded_decoys.addPeptide(same);
                        } else {
                            m_discarded_decoys.addPeptide(pep);
                        }
                    }
                } else {
                    e.add(pep);
                    m_peptideCount++;
                }

            } else {
                e = new PeptideLookupElement(mass);
                this.put(mass, e);
                e.add(pep);
                m_peptideCount++;
                if (m_minimumMass > mass) {
                    m_minimumMass = mass;
                }
                if (m_MaximumMass < mass) {
                    m_MaximumMass = mass;
                }
            }

            
        }
    }
    
    
    public ArrayList<Peptide> getForMass(double mass) {
        return getForMass(mass,mass);
    }

    public ArrayList<Peptide> getForMass(double mass, double referenceMass) {
        ArrayList<Peptide> peps = new ArrayList<Peptide>();
        try {
            Collection<PeptideLookupElement> pepElems =
                    this.subMap(m_tolerance.getMinRange(mass, referenceMass), m_tolerance.getMaxRange(mass, referenceMass)).values();
        Iterator<PeptideLookupElement> peIter = pepElems.iterator();

        while (peIter.hasNext()) {
            peps.addAll(peIter.next());
        }
        } catch (Exception ex) {
            throw new Error(ex);
        }
        return peps;
    }


    public ArrayList<Peptide> getForMassRange(double minmass, double maxmass, double referenceMass) {
        ArrayList<Peptide> peps = new ArrayList<Peptide>();
        try {
            Collection<PeptideLookupElement> pepElems =
                    this.subMap(m_tolerance.getMinRange(minmass, referenceMass), m_tolerance.getMaxRange(maxmass, referenceMass)).values();
        Iterator<PeptideLookupElement> peIter = pepElems.iterator();

        while (peIter.hasNext()) {
            peps.addAll(peIter.next());
        }
        } catch (Exception ex) {
            throw new Error(ex);
        }
        return peps;
    }
    
    
    public ArrayList<Peptide> getForExactMassRange(double minmass, double maxmass) {
        ArrayList<Peptide> peps = new ArrayList<Peptide>();
        try {
            Collection<PeptideLookupElement> pepElems =
                    this.subMap(minmass, maxmass).values();
        Iterator<PeptideLookupElement> peIter = pepElems.iterator();

        while (peIter.hasNext()) {
            peps.addAll(peIter.next());
        }
        } catch (Exception ex) {
            throw new Error(ex);
        }
        return peps;
    }
    
    public void setTolerance(ToleranceUnit tolerance) {
        m_tolerance = tolerance;
    }

    public PeptideIterator iterator() {

        return new PeptideIterator() {

            Iterator<PeptideLookupElement> it = values().iterator();
            Iterator<Peptide> current = null;
            Peptide currentPeptide = null;


            public boolean hasNext() {
                return it.hasNext() || (current != null && current.hasNext());
            }

            public Peptide next() {
                if ((current != null && current.hasNext())) {
                    currentPeptide = current.next();
                    return currentPeptide;
                }

                current = it.next().iterator();
                currentPeptide = current.next();
                return currentPeptide;
            }


            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public Sequence getCurrentSequence() {
                return currentPeptide.getSequence();
                
            }

            @Override
            public Peptide current() {
                return currentPeptide;
            }
        };
    }

    public PeptideIterator iteratorAfter(Peptide p) {
        PeptideIterator it = iterator();
        if (p == null) {
            return it;
        }
        Peptide  pep = it.next();
        while (pep != null && !pep.equals(p))  {
            pep = it.next();
        }
        return it;
    }


    @Override
    public ArrayList<Peptide> getPeptidesByExactMass(double mass) {
        PeptideLookupElement pe = this.get(mass);
        return pe;
    }

    public void dump(Writer w) throws IOException {
        BufferedWriter bw = new BufferedWriter(w);
        int targetPeptides = 0;
        int decoyPeptides = 0;

        for (Double mz : super.keySet()) {
            PeptideLookupElement ple = super.get(mz);
            bw.append("" +  mz);
            for (Peptide p : ple) {
                if (p.getSequence().isDecoy()) {
                    decoyPeptides++;
                } else {
                    targetPeptides++;
                }
                bw.append("," + p.getSequence().getFastaHeader().substring(0, Math.min(40,p.getSequence().getFastaHeader().length())) + ":" + p.toString());
            }
            bw.newLine();
        }
        bw.newLine();
        bw.append("Target Peptides : " + targetPeptides + ", Decoy Peptides : " + decoyPeptides);
        bw.close();
    }

    public void dump(String path) throws IOException {
        dump(new FileWriter(path));
    }


    public long getPeptideCount(){
        return m_peptideCount;
    }

    @Override
    public int size() {
        return (int) getPeptideCount();
    }
    
    /**
     * tests whether a peptide of the given sequence already exists in the tree
     * @param pep
     * @return 
     */
    public boolean containsPeptide(Peptide pep) {
        Double mass = pep.getMass();
        PeptideLookupElement e;
        if (this.containsKey(mass)) {
            e = this.get(mass);
            return e.getElementBySequenceAAMass(pep) != null;
        }
        return false;
        
    }

    /**
     * tries to replace decoy-peptides that where also found as target peptides
     * with randomly generated decoy-peptides. And thereby providing more equal 
     * decoy database.
     * @param conf the configuration - needed to get a list of amino-acids
     * @return the list of peptides, that could not be replaced by an unique
     * random peptide of same length
     */
    public ArrayList<Peptide> addDiscared(RunConfig conf) {
        ArrayList<Peptide> nonreplacedPeps = new ArrayList<Peptide>();
        for (Peptide p : m_discarded_decoys) {
            boolean added = false;
            for (int r = 0; r<10; r++) {
                Peptide rand = NonProteinPeptide.randomPeptide(p, conf, true);
                if (!containsPeptide(rand)) {
                    addPeptide(rand);
                    added = true;
                    break;
                }
            }
            if (!added) {
                nonreplacedPeps.add(p);
            }
        }
        return nonreplacedPeps;
    }

    /**
     * tries to replace decoy-peptides that where also found as target peptides
     * with permuted decoy-peptides. And thereby providing more equal 
     * decoy database.<br>
     * The advantage against using random peptide is, that the system stays 
     * reproducible. (The permutation here used does not do a random permutation)
     * @param conf the configuration - needed to get a list of amino-acids
     * @return the list of peptides, that could not be replaced by an unique
     * permuted peptide
     */
    public ArrayList<Peptide> addDiscaredPermut(RunConfig conf) {
        ArrayList<Peptide> nonreplacedPeps = new ArrayList<Peptide>();
        for (Peptide p : m_discarded_decoys) {
            boolean added = false;
            for (Peptide perm : p.permute(conf)) {
                if (!containsPeptide(perm)) {
                    addPeptide(perm);
                    added = true;
                    break;
                }
            }
            if (!added) {
                nonreplacedPeps.add(p);
            }
        }
        return nonreplacedPeps;
    }

    
    public void forceAddDiscarded() {
        for (Peptide p :m_discarded_decoys) {
            PeptideLookupElement e = this.get(p.getMass());
            e.add(p);
            m_peptideCount++;
        }
    }

    public void cleanup() {
        // as we can't delete elements, while iterating we have to delte them in a second step
        ArrayList<PeptideLookupElement> eDelete = new ArrayList<PeptideLookupElement>();
        
        for (PeptideLookupElement ple : this.values()) {
            ArrayList<Peptide> pepDelete = new ArrayList<Peptide>();
            for (Peptide p : ple) {
                if (p.getProteinCount() > 10) {
                    pepDelete.add(p);
                    m_peptideCount--;
                }
            }
            for (Peptide p : pepDelete) {
                ple.remove(p);
            }

            if (ple.size() == 0) {
                eDelete.add(ple);
            }
        }

        for (PeptideLookupElement ple : eDelete) {
            this.remove(ple.getMass());
        }
    }

    public void cleanup(int minLength) {
        ArrayList<PeptideLookupElement> eDelete = new ArrayList<PeptideLookupElement>();
        for (PeptideLookupElement ple : this.values()) {
            ArrayList<Peptide> pepDelete = new ArrayList<Peptide>();
            for (Peptide p : ple) {
                if (p.getProteinCount() > 10 || p.length()<minLength) {
                    pepDelete.add(p);
                    m_peptideCount--;
                }

            }
            for (Peptide p : pepDelete) {
                ple.remove(p);
            }

            if (ple.size() == 0) {
                eDelete.add(ple);
            }
        }

        for (PeptideLookupElement ple : eDelete) {
            this.remove(ple.getMass());
        }
    }

    @Override
    public void cleanup(int minLength, int maxAmbiguity) {
        this.cleanup(minLength, maxAmbiguity, 10);
    }
    
    @Override
    public void cleanup(int minLength, int maxAmbiguity, int maxProtAmbiguity) {
        ArrayList<PeptideLookupElement> eDelete = new ArrayList<PeptideLookupElement>();
        for (PeptideLookupElement ple : this.values()) {
            ArrayList<Peptide> pepDelete = new ArrayList<Peptide>();
            for (Peptide p : ple) {
                if (p.getPositions().length>maxAmbiguity || p.getProteinCount() > maxProtAmbiguity || p.length()<minLength ) {
                    pepDelete.add(p);
                    m_peptideCount--;
                }

            }
            for (Peptide p : pepDelete) {
                ple.remove(p);
            }

            if (ple.size() == 0) {
                eDelete.add(ple);
            }
        }

        for (PeptideLookupElement ple : eDelete) {
            this.remove(ple.getMass());
        }
    }
    
    @Override
    public void applyVariableModifications(RunConfig conf) {
        Digestion enzym = conf.getDigestion_method();
        ArrayList<CrossLinker> cl = conf.getCrossLinker();
        Iterator<Peptide> peps = this.iterator();
        ArrayList<Peptide> newPeps = new ArrayList<Peptide>();
        int cm = this.size();

        int c = 0;
        while (peps.hasNext()) {
            if (c++ % 10000 == 0) {
                    conf.getStatusInterface().setStatus("Applying variable modification " +  Util.twoDigits.format(c*100.0/cm) + "%" );
            }

            Peptide pep = peps.next();
            for (Peptide p : pep.modify(conf, ModificationType.variable)) {
                if (enzym.isDigestedPeptide(p)) {
                    newPeps.add(p);
                    p.getSequence().getPeptides().add(p);
                }
            }
        }
        for (Peptide p : newPeps) {
            addPeptide(p);
        }
    }

    public PeptideLookup applyFixedModificationsPostDigestLinear(RunConfig conf,PeptideLookup Crosslinked) {
        PeptideTree modPeps = new PeptideTree(m_tolerance);
        ArrayList<CrossLinker> cl = conf.getCrossLinker();
        for (Peptide p : this) {
            
            for (AminoModification am : conf.getFixedModificationsPostDigest()) {
                p.replace(am);
            }
            
            if (CrossLinker.canCrossLink(cl, p)) {
                Crosslinked.addPeptide(p);
            } else {
                modPeps.addPeptide(p);
            }
        }
        return modPeps;
    }

    public PeptideLookup applyFixedModificationsPostDigest(RunConfig conf,PeptideLookup linear) {
        PeptideTree modPeps = new PeptideTree(m_tolerance);
        ArrayList<CrossLinker> cl = conf.getCrossLinker();
        for (Peptide p : this) {
            
            for (AminoModification am : conf.getFixedModificationsPostDigest()) {
                p.replace(am);
            }
            
            if (CrossLinker.canCrossLink(cl, p)) {
                modPeps.addPeptide(p);
            } else {
                linear.addPeptide(p);
            }
        }
        return modPeps;
    }
    
    
    @Override
    public void applyVariableModificationsLinear(RunConfig conf,PeptideLookup Crosslinked) {
        Digestion enzym = conf.getDigestion_method();
        ArrayList<CrossLinker> cl = conf.getCrossLinker();
        Iterator<Peptide> peps = this.iterator();
        ArrayList<Peptide> newPeps = new ArrayList<Peptide>();
        int cm = this.size();

        int c = 0;
        while (peps.hasNext()) {
            if (c++ % 10000 == 0) {
                    conf.getStatusInterface().setStatus("Applying variable modification " +  Util.twoDigits.format(c*100.0/cm) + "%" );
            }

            Peptide pep = peps.next();
            for (Peptide p : pep.modify(conf,ModificationType.variable)) {
                if (enzym.isDigestedPeptide(p)) {
                    newPeps.add(p);
                    p.getSequence().getPeptides().add(p);
                }
            }
        }
        for (Peptide p : newPeps) {
            if (CrossLinker.canCrossLink(cl, p)) {
                Crosslinked.addPeptide(p);
            } else {
                addPeptide(p);
            }
        }
    }


    public void applyVariableModifications(RunConfig conf, PeptideLookup linear) {

        Digestion enzym = conf.getDigestion_method();
        ArrayList<CrossLinker> cl = conf.getCrossLinker();
        Iterator<Peptide> peps = this.iterator();
        ArrayList<Peptide> newPeps = new ArrayList<Peptide>();

        int cm = this.size();

        int c = 0;
        while (peps.hasNext()) {
            if (c++ % 10000 == 0) {
                    conf.getStatusInterface().setStatus("Applying variable modification " +  Util.twoDigits.format(c*100.0/cm) + "%" );
            }
            Peptide pep = peps.next();
            ArrayList<Peptide> mps= pep.modify(conf, ModificationType.variable);
            for (Peptide p : mps) {
                if (enzym.isDigestedPeptide(p)) {
                    newPeps.add(p);
                    p.getSequence().getPeptides().add(p);
                }
            }
        }
        for (Peptide p : newPeps) {
            if (CrossLinker.canCrossLink(cl, p)) {
                addPeptide(p);
            } else {
                linear.addPeptide(p);
            }
        }
    }

    public double getMinimumMass() {
        return m_minimumMass;
    }

    public double getMaximumMass() {
        return m_MaximumMass;
    }


    public double countOffset(double offset) {
        int sum = 0;
        int count = 0;
        StreamingAverageMedianStdDev stm = new StreamingAverageMedianStdDev(0.5, 10000000);
        for (Map.Entry<Double, PeptideLookupElement> e : this.entrySet()) {
            count++;
            sum+=this.getForMass(e.getKey()+offset).size();
            stm.addValue(this.getForMass(e.getKey()+offset).size());
        }
        
        return stm.getMedianEstimation();
    }
}
