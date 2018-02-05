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
package rappsilber.ms.dataAccess.filter.spectrafilter;

import java.io.IOException;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.AbstractStackedSpectraAccess;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class AbstractSpectraFilter extends AbstractStackedSpectraAccess{

    public abstract boolean passScan(Spectra s);
    
    int m_readSpectra = 0;
    int m_discardedSpectra = 0;

//    SpectraAccess m_reader;

    Spectra m_current = null;
    Spectra m_next = null;

    @Override
    public void setReader(SpectraAccess innerAccess){
        super.setReader(innerAccess);
        //next();
    }


    @Override
    public Spectra current() {
        return m_current;
    }

    @Override
    public int countReadSpectra() {
        return m_readSpectra;
    }

    public boolean hasNext() {
        if (m_current == null && m_next == null)
            next();
        
        synchronized(m_sync) {
            return m_next != null;
        }
    }

    public Spectra next() {
        synchronized(m_sync) {
            m_current = m_next;
            m_next = null;
            Spectra n = null;

            while (m_InnerAcces.hasNext()) {
                n = m_InnerAcces.next();

                double precMass = n.getPrecurserMass();

                if (passScan(n)) {
                    m_next = n;
                    m_readSpectra++;
                    break;
                } else {
                    m_discardedSpectra++;
                }
            }
            return m_current;
        }

    }
    
    @Override
    public void restart() throws IOException {
        m_next = null;
        m_current = null;
        super.restart();
    }    

    public long getDiscardedSpectra() {
        return m_discardedSpectra;
    }
    
}
