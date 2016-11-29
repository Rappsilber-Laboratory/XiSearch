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
package rappsilber.ms.dataAccess.tempstorage;

import java.util.HashMap;
import java.util.Iterator;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.spectra.Spectra;

/**
 * Stores all mgc-scores for all spectra.
 * This specific class just keeps a list off all scores in memory.
 * 
 * Writing scores should be thread-save.
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MemoryStoredScoreResults {
    private SequenceList sequences;
    
    /**
     *  sub class that translates the stored information back into peptide and scores
     */
    public class PepIDDouble extends HashMap<Integer, Double>  implements Iterable<Peptide> {
        public double getScore(Peptide pep) {
            return get(pep.getPeptideIndex());
        }
        
        public Iterator<Peptide> iterator() {
            return new Iterator<Peptide>() {
                
                Iterator<Integer> pepids = keySet().iterator();
                public boolean hasNext() {
                    return pepids.hasNext();
                }

                public Peptide next() {
                    return sequences.getPeptide(pepids.next());
                }

                public void remove() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            };
        }
        
    }
    
    /**
     * a small wrapper class, as Java does not permit arrays of generic data-types
     */
    private static class SpectraToPepIDDouble extends HashMap<Integer,HashMap<Double,PepIDDouble>> {
        
    }
    
    /**
     * the actual storage.
     * Its basically an array with one entry preallocated for each spectra
     */
    SpectraToPepIDDouble[] m_storage;

    public MemoryStoredScoreResults(SequenceList sequences, int countSpectra) {
        this.sequences = sequences;
        this.m_storage = new SpectraToPepIDDouble[countSpectra];
        for (int i = 0; i<countSpectra;i++) {
            m_storage[i] = new SpectraToPepIDDouble();
        }
                
    }
    
    
    
    
    /**
     * stores a spectra/peptide match with the according score
     * @param s
     * @param p
     * @param mgcScore 
     */
    public void storage(Spectra s,Peptide p, double mgcScore) {
        int sid = s.getReadID();
        int pid = p.getPeptideIndex();
        synchronized(m_storage[sid]) {
            HashMap<Double,PepIDDouble> chargeList = m_storage[sid].get(s.getPrecurserCharge());
            if (chargeList == null) {
                chargeList = new HashMap<Double, PepIDDouble>();
                PepIDDouble ps = new PepIDDouble();
                ps.put(pid, mgcScore);
                chargeList.put(s.getPrecurserMZ(), ps);
                m_storage[sid].put(sid, chargeList);
            } else {
                PepIDDouble ps = chargeList.get(s.getPrecurserMZ());
                if (ps == null) {
                    ps = new PepIDDouble();
                    ps.put(pid, mgcScore);
                    chargeList.put(s.getPrecurserMZ(),ps);
                } else
                    ps.put(pid, mgcScore);
            }
        }
    }
    
    public PepIDDouble retriveScores(Spectra s) {
        int sid = s.getReadID();
        SpectraToPepIDDouble sp= m_storage[sid];
        return sp.get(s.getPrecurserCharge()).get(s.getPrecurserMZ());
    }
    
}
