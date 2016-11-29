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

import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.AbstractStackedSpectraAccess;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.score.BS3ReporterIonScore;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class BS3ReporterIonFilter extends AbstractSpectraFilter{
//    SpectraAccess m_InnerAcces;
    Spectra m_next = null;
    Spectra m_current = null;

    //private final Object nextSychronization= new Object();

    BS3ReporterIonScore scoreing = new BS3ReporterIonScore();

    double cutOff = 0.9;

    

//    @Override
//    public Spectra current() {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    @Override
//    public int countReadSpectra() {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

//    @Override
//    public boolean hasNext() {
//        synchronized (m_sync) {
//            return m_next != null;
//        }
//    }

//    @Override
//    public synchronized Spectra next() {
//        synchronized (m_sync) {
//            m_current = m_next;
//
//            if (m_InnerAcces.hasNext()) {
//                Spectra n = m_InnerAcces.next();
//                double score;
//                while ((score = scoreing.getScore(n)[0]) < cutOff && m_InnerAcces.hasNext())
//                    n = m_InnerAcces.next();
//                if (score > cutOff)
//                    m_next = n;
//            } else
//                m_next = null;
//            return m_current;
//        }
//    }

//    @Override
//    public void setReader(SpectraAccess innerAccess) {
//        m_InnerAcces = innerAccess;
//        if (m_next == null && m_InnerAcces.hasNext())
//            m_next = m_InnerAcces.next();
//    }

    @Override
    public boolean passScan(Spectra s) {
        return scoreing.getScore(s)[0] >= cutOff;
    }
    
}
