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

import rappsilber.ms.dataAccess.*;
import rappsilber.ms.spectra.Spectra;


/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class RandomSpectraSubset extends AbstractStackedSpectraAccess {
    int m_readSpectra = 0;

//    SpectraAccess m_reader;

    int m_current = -1;

    int m_numSpectra = 0;

    Spectra[] m_subset;

    int m_SubSetSize =0;


    public RandomSpectraSubset(int NumSpectra) {
        m_numSpectra = NumSpectra;
        m_subset = new Spectra[NumSpectra];
    }

    public RandomSpectraSubset(SpectraAccess innerAccess, int NumSpectra) {
        this(NumSpectra);
        setReader(innerAccess);
    }

    public void setReader(SpectraAccess innerAccess){
        m_InnerAcces = innerAccess;
        getScans();
    }


    public void getScans() {
        // fill up the buffer
        while (m_SubSetSize < m_numSpectra && m_InnerAcces.hasNext()) {
            m_subset[m_SubSetSize++] = m_InnerAcces.next();
        }

        // now make it a random selection with the rest
        while (m_InnerAcces.hasNext()) {
            int id =(int) (Math.random() * m_numSpectra);
            m_subset[id] = m_InnerAcces.next();
        }

    }


    @Override
    public Spectra current() {
        return m_InnerAcces.current();
    }

    @Override
    public int countReadSpectra() {
        return m_readSpectra;
    }

    public boolean hasNext() {
        return m_current < m_SubSetSize - 1;
    }

    public Spectra next() {
        m_current++;
        return m_subset[m_current];
    }

}
