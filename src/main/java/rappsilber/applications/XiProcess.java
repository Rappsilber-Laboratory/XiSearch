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
package rappsilber.applications;

import rappsilber.config.RunConfig;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.ms.lookup.peptides.PeptideLookup;
import rappsilber.ms.sequence.SequenceList;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public interface XiProcess {

    /**
     * @return the m_outputTopOnly
     */
    boolean OutputTopOnly();

    void addFilter(StackedSpectraAccess f);

    /**
     * @return the m_useCPUs
     */
    int getCPUs();
    
    boolean isRunning();

    /**
     * @return the m_config
     */
    RunConfig getConfig();

    /**
     * @return the m_msmInput
     */
    AbstractSpectraAccess getMSMInput();

    /**
     * @return the m_output
     */
    ResultWriter getOutput();

    long getProcessedSpectra();

    SequenceList getSequenceList();

    String getStatus();

    boolean preparePreFragmentation();
    
    void prepareSearch();

    PeptideLookup getXLPeptideLookup();

    PeptideLookup getLinearPeptideLookup();    

    /**
     * @param outputTopOnly the m_outputTopOnly to set
     */
    void setOutputTopOnly(boolean outputTopOnly);

    void setStatus(String status);

    void startSearch();

    void stop();

    void waitEnd();
    
}
