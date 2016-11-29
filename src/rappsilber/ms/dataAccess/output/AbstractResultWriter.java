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

import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class AbstractResultWriter implements ResultWriter{
    boolean m_finished = false;
    protected boolean m_doFreeMatch = false;



    @Override
    public void setFreeMatch(boolean doFree) {
        m_doFreeMatch=doFree;
    }

    @Override
    public void finished() {
        m_finished = true;
    }

    @Override
    public boolean waitForFinished() {
        flush();
        return m_finished;
    }

//    public void flush() {
//        
//    }
}
