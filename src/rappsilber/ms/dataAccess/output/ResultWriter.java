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
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
/**
 * interface that should be implemented by any kind of result output - to enable
 * an easy exchange of different out-put formats
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public interface ResultWriter {
    /**
     * Should be called before any other output to produce a header.
     *
     */
    void writeHeader();
    /**
     * outputs the actual data/results for one peptide spectra match
     * @param match
     */
    void writeResult(MatchedXlinkedPeptide match) throws IOException;

    /**
     * returns, how many matched results where written
     */
    int getResultCount();

    /**
     * returns, how many matched results where written
     */
    int getTopResultCount();
    
    /**
     * should the match freed up after writing out
     * @param doFree true: call match.free() after writing 
     */
    void setFreeMatch(boolean doFree);

    /**
     * Announces, that no more data should be written. <br/>
     * normally closes the stream/file/connection
     */
    void finished();

    /**
     * This function should wait for the last elements to be written out.
     * The idea being, that if the writer is asynchron we should be able to
     * wait for it to finish it's work before closing the application
     * @return if finished() wasn't called yet it will return immediately - else it will wait until everything was written and then return with true;
     */
    boolean waitForFinished();
    
    /**
     * if a output has a buffer flush that buffer
     */
    void flush();


    /**
     * implements a ping that can be regularly called e.g. to enable watchdog like behaviour
     */
    void ping();
    
}
