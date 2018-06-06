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
package rappsilber.ms.dataAccess;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.spectra.Spectra;

/**
 * Base class for a sequencial access to spectra stored in some way
 *
 * use:
 * SpectraAccess spectrastorage = new ....
 *
 * for (Spectra spectra : spectrastorage) {
 *     doSomething(spectra);
 * }
 *
 * 
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class AbstractSpectraAccess implements SpectraAccess{

    private SequenceList m_sequences;

    /**
     * just returns itself to conform Iterable&lt;Spectra&gt; so it can be used like
     *
     * <pre>
     *
     * SpectraAccess sa = new ...
     * 
     * for (Spectra s: sa) {
     *      doSomethingWithSpectra(sa);
     * }
     * </pre>
     * @return this
     */
    @Override
    public Iterator<Spectra> iterator() {
        return this;
    }


    /**
     * returns the Spectra that was read (and returned) on the last call of next()
     * @return the currently active spectra
     */
    @Override
    public abstract Spectra current();


    /**
     * if not otherwise defined in derived classess, removing spectra from the
     * input is not possible.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }


//    @Override
//    public abstract int countReadSpectra();

    /**
     * The sequencelist used for lookup-purposes
     * @return the m_sequences
     */
    @Override
    public SequenceList getSequences() {
        return m_sequences;
    }

    /**
     * The SequnceList is used for loockup purposes. At the moment that only
     * includes the lookup of Peptides via peptide-id.
     * @param sequences  
     */
    @Override
    public void setSequences(SequenceList sequences) {
        this.m_sequences = sequences;
    }

//
//    public abstract int getEntriesCount();
//
    public abstract int getSpectraCount();
    
    public abstract void gatherData() throws FileNotFoundException;//  throws FileNotFoundException  {
//        
//    }

    public void gatherData(int cpus) throws FileNotFoundException, IOException {
        gatherData();
    }
    public double getMaxPrecursorMass() {
        return Double.MAX_VALUE;
    }
    
    public long getDiscardedSpectra() {
        return 0;
    }
}
