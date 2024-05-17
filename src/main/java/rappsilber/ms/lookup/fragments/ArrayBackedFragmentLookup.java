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
package rappsilber.ms.lookup.fragments;


import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rappsilber.utils.DoubleArrayList;
import org.rappsilber.utils.DoubleArraySlice;
import org.rappsilber.utils.IntArrayList;
import rappsilber.config.AbstractRunConfig;
import rappsilber.config.RunConfig;
import rappsilber.ms.Range;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.crosslinker.SymetricSingleAminoAcidRestrictedCrossLinker;
import rappsilber.ms.lookup.peptides.PeptideLookup;
import rappsilber.ms.lookup.peptides.PeptideTree;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.sequence.digest.NoDigestion;
import rappsilber.ms.sequence.ions.CrossLinkedFragmentProducer;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.utils.ArithmeticScoredOccurence;
import rappsilber.utils.Util;

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


public class ArrayBackedFragmentLookup implements FragmentLookup {
    DoubleArrayList fragMasses = new DoubleArrayList(10000);
    IntArrayList pepIDs = new IntArrayList(10000);
    double m_MaximumPeptideMass = Double.MAX_VALUE;
    double m_MinimumMass=0;
    RunConfig m_config;
    ToleranceUnit tolerance;
    private SequenceList  m_list;
    int m_fragmentCount = 0;
    
    Comparator<DoubleArraySlice> massCompare =  new Comparator<DoubleArraySlice>(){
                @Override
                public int compare(DoubleArraySlice o1, DoubleArraySlice o2) {
                    return Double.compare(o1.get(0), o1.get(0));
                }
            };
    private int m_maxPeakCandidates;
    
    public ArrayBackedFragmentLookup(PeptideLookup peptideList, SequenceList list, int threads, RunConfig config){
        this.m_list = list;
        this.m_config = config;
        this.tolerance = config.getFragmentTolerance();
        insertFragementsFromPeptides(peptideList);
        
    }
    

    public void insertFragementsFromPeptides(Iterable<Peptide> peptides) {
        try {
            fragMasses.setMaxIncrement(1000000);
            pepIDs.setMaxIncrement(1000000);
            int count =0;
            int maxPeps = m_list.getAllPeptideIDs().length;
            int percent = 0;
            //Peptide pep;
            for (Peptide pep : peptides ) {
                try {
                    if (pep.getMass() < m_MaximumPeptideMass && pep.getMass() > m_MinimumMass) {
                        ArrayList<Fragment> frags;
                        if (m_config == null) {
                            frags = pep.getPrimaryFragments();

                        } else {
                            
                            frags = pep.getPrimaryFragments(m_config);
                            for (CrossLinker cl : m_config.getCrossLinker())
                                for (CrossLinkedFragmentProducer cfp : m_config.getPrimaryCrossLinkedFragmentProducers()) {
                                    frags.addAll(cfp.createCrosslinkedFragments(frags, new ArrayList<Fragment>(), cl, false));
                                }
                            
                        }
                        for (int i = 0; i < frags.size(); i++) {
                            
                            Fragment f = frags.get(i);
                            fragMasses.add(f.getMass());
                            pepIDs.add(pep.getPeptideIndex());
                            
                        }
                    }
                    
                } catch (Exception e) {
                    throw new Error(e);
                }
                m_fragmentCount = fragMasses.size();
                if (++count % 1000 == 0) {
                    int newPercent = count*100/maxPeps;
                    if (newPercent>percent) {
                        m_config.getStatusInterface().setStatus(count + " peptides fragmented (" + newPercent +"% = " + m_fragmentCount + " fragments)"  );
                        percent = newPercent;
                    }
                }
            }
            m_config.getStatusInterface().setStatus(count + " peptides fragmented of "+  maxPeps +" peptides (" + m_fragmentCount + " fragments)"  );

            Util.verboseGC();
            m_config.getStatusInterface().setStatus(count + " peptides fragmented - sorting"  );
            
            fragMasses.quicksort(pepIDs);
            m_config.getStatusInterface().setStatus(" Fragmenttree build"  );
            Util.verboseGC();
            
        } catch (Exception error) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "error while building fragment tree",error);
            System.err.println(error);
            error.printStackTrace(System.err);
            System.exit(0);

        }
    }

    @Override
    public ArrayList<Peptide> getForMass(double mass) {
        return getForMass(mass, mass);
    }

    @Override
    public ArrayList<Peptide> getForMass(double mass, double referenceMass) {
        Range r = tolerance.getRange(mass,referenceMass);
        HashSet<Integer> pepids  =getPeptideIds(r);
        ArrayList<Peptide> ret  =new ArrayList<>(pepids.size());
        for (int id : pepids) {
            Peptide p  = m_list.getPeptide(id);
            ret.add(m_list.getPeptide(id));
        }
        return ret;
    }

    protected HashSet<Integer> getPeptideIds(Range r) {
        HashSet<Integer> pepids  =new HashSet<>();
        // find the smallest entry larger then min
        int smallest=fragMasses.size()/2;
        int step = fragMasses.size()/2;
        double m0;
        double m1;
        m0 = (smallest>0? fragMasses.get(smallest-1) : 0);
        m1 = (smallest<fragMasses.size()? fragMasses.get(smallest) : 0);

        while ((m0>=r.min || m1<r.min) && step>0) {
            step=step/2;
            if (m0 < r.min) {
                smallest = smallest + step;
            } else {
                smallest = smallest - step;
            }
            m0 = (smallest>0? fragMasses.get(smallest-1) : 0);
            m1 = (smallest<fragMasses.size()? fragMasses.get(smallest) : 0);
        }
        int next = smallest;
        while (next<fragMasses.size() && fragMasses.get(next)<=r.max) {
            pepids.add((int)pepIDs.get(next++));
        }
        return pepids;
    }
    
    @Override
    public ArrayList<Peptide> getForMass(double mass, double referenceMass, double maxMass) {
        Range r = tolerance.getRange(mass,referenceMass);
        Collection<Integer> pepids = getPeptideIds(r);

        ArrayList<Peptide> ret  =new ArrayList<>(pepids.size());
        for (int id : pepids) {
            Peptide p  = m_list.getPeptide(id);
            if (p.getMass()<maxMass)
                ret.add(m_list.getPeptide(id));
        }
        return ret;
    }

    @Override
    public ArrayList<Peptide> getForMass(double mass, double referenceMass, double maxMass, int maxPeptides) {
        Range r = tolerance.getRange(mass,referenceMass);
        Collection<Integer> pepids = getPeptideIds(r);
        
        if (pepids.size() <= maxPeptides) {
            ArrayList<Peptide> ret  =new ArrayList<>(pepids.size());
            for (int id : pepids) {
                Peptide p  = m_list.getPeptide(id);
                if (p.getMass()<maxMass)
                    ret.add(m_list.getPeptide(id));
            }
            return ret;
        }
        return new ArrayList<>(0);
    }
    
    

    @Override
    public Map<Peptide, Double> getPeptidesForMasses(double mass) {
        HashMap<Peptide, Double> ret = new HashMap<Peptide, Double>();
        for (Peptide p : getForMass(mass)) {
            ret.put(p, mass);
        }
        return ret;    
    }

    @Override
    public int getFragmentCount() {
        return m_fragmentCount;
    }

    @Override
    public int countPeptides(double mass) {
        return getPeptideIds(tolerance.getRange(mass)).size();
    }

    @Override
    public int countPeptides(double mass, double targetMass) {
        return getPeptideIds(tolerance.getRange(mass,targetMass)).size();
    }

    @Override
    public Peptide lastFragmentedPeptide() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArithmeticScoredOccurence<Peptide> getAlphaCandidates(Spectra s, ToleranceUnit precursorTolerance) {
        double maxPeptideMass=precursorTolerance.getMaxRange(s.getPrecurserMass());
        int maxcandidates = m_config.getMaximumPeptideCandidatesPerPeak();
        return this.getAlphaCandidates(s, maxPeptideMass);
    }    
    
    @Override
    public ArithmeticScoredOccurence<Peptide> getAlphaCandidates(Spectra s, double maxPeptideMass) {
        ArithmeticScoredOccurence<Peptide> peakMatchScores = new ArithmeticScoredOccurence<Peptide>();

        if (m_maxPeakCandidates == -1) {
            //   go through mgc spectra
            for (SpectraPeak sp : s) {
                //      for each peak
                //           count found peptides
                ArrayList<Peptide> matchedPeptides = this.getForMass(sp.getMZ(),sp.getMZ(),maxPeptideMass); // - Util.PROTON_MASS);
                
                // add fragments for that match to any delta mass as well
                for (double d : m_config.getAlphaCandidateDeltaMasses()) {
                    matchedPeptides.addAll(this.getForMass(sp.getMZ()-d,sp.getMZ(),maxPeptideMass));
                }
                
                double peakScore = (double) matchedPeptides.size() / getFragmentCount();
                for (Peptide p : matchedPeptides) {
                    peakMatchScores.multiply(p, peakScore);
                }
            }
        } else {
            //   go through mgc spectra
            for (SpectraPeak sp : s) {
                //      for each peak
                //           count found peptides
                ArrayList<Peptide> matchedPeptides = getForMass(sp.getMZ(),sp.getMZ(),maxPeptideMass,m_maxPeakCandidates);
                
                // add fragments for that match to any delta mass as well
                for (double d : m_config.getAlphaCandidateDeltaMasses()) {
                    matchedPeptides.addAll(this.getForMass(sp.getMZ()-d,sp.getMZ(),maxPeptideMass));
                }
                
                double peakScore = (double) matchedPeptides.size() / getFragmentCount();
                for (Peptide p : matchedPeptides) {
                    peakMatchScores.multiply(p, peakScore);
                }
            }
        }
        return peakMatchScores;
    }

    @Override
    public void writeOutTree(File out) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setTolerance(ToleranceUnit tolerance) {
        this.tolerance = tolerance;
    }

    @Override
    public void clear() {
        this.fragMasses.clear();
        this.pepIDs.clear();
        this.pepIDs = new IntArrayList();
        this.pepIDs.setMaxIncrement(1000000);
        this.fragMasses = new DoubleArrayList();
        this.fragMasses.setMaxIncrement(1000000);
    }
    
    public static void main(String[] args) {
        RunConfig  conf =new AbstractRunConfig() {
            {
                setFragmentTolerance(new ToleranceUnit("20ppm"));
                setPrecoursorTolerance(new ToleranceUnit("10ppm"));
                setDigestion(new NoDigestion());
                
                setCrosslinker(
                        new CrossLinker[]{
                            new SymetricSingleAminoAcidRestrictedCrossLinker(
                                    "dummy", 
                                    1, 1, 
                                    new AminoAcid[]{AminoAcid.K})
                        }
                );
                try {
                    Fragment.parseArgs("BIon", this);
                    Fragment.parseArgs("YIon", this);
                    Fragment.parseArgs("PeptideIon", this);
                } catch (ParseException ex) {
                    Logger.getLogger(ArrayBackedFragmentLookup.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
        };
        SequenceList t = new SequenceList(conf);
        Sequence s1 = new Sequence("KKKKKKKK", conf);
        Sequence s2 = new Sequence("KKKYYKKK", conf);
        t.add(s1);
        t.add(s2);
        PeptideLookup pepLookup = new PeptideTree(conf.getPrecousorTolerance());
        conf.getDigestion_method().setPeptideLookup(pepLookup, pepLookup);
        t.digest(conf.getDigestion_method(), conf.getCrossLinker());
        ArrayBackedFragmentLookup test = new ArrayBackedFragmentLookup(pepLookup, t, 1, conf);
        ArrayList<Peptide> peps1 = test.getForMass(403.30327443699997);
        ArrayList<Peptide> peps2 = test.getForMass(566.366603012);
    }
}