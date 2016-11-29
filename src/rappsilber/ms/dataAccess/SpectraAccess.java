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

import java.io.IOException;
import java.util.Iterator;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public interface SpectraAccess extends Iterable<Spectra>, Iterator<Spectra>  {
    Iterator<Spectra> iterator();


    /**
     * returns the Spectra that was read (and returned) on the last call of next()
     * @return the currently active spectra
     */
    Spectra current();


    /**
     * if not otherwise defined in derived classess, removing spectra from the
     * input is not possible.
     */
    void remove();


    /** how many spectra where read in */
    int countReadSpectra();

    
    /**
     * The sequencelist used for lookup-purposes
     * @return the m_sequences
     */
    SequenceList getSequences();

    /**
     * The SequnceList is used for loockup purposes. At the moment that only
     * includes the lookup of Peptides via peptide-id.
     * @param sequences
     */
    void setSequences(SequenceList sequences);

    boolean canRestart();

    void restart() throws IOException;

    void close();
}
