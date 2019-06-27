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
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DuplicateShifted extends AbstractStackedSpectraAccess {
    /** the next level - provides the actual spectra */
//    private SpectraAccess m_innerAcces;

    private Spectra m_current;

    private Spectra m_next;
    private Spectra m_nextShifted;

    private double  m_ShiftMass;
    private int     m_countReadSpectra = 0;
//    private final Object m_sync = new Object();

    public DuplicateShifted(double ShiftMass) {
        this.m_ShiftMass = ShiftMass;
    }

    public DuplicateShifted(SpectraAccess innerAcces, double ShiftMass) {
        this.m_ShiftMass = ShiftMass;
        setReader(innerAcces);
    }




    @Override
    public Spectra current() {
        return m_current;
    }

    @Override
    public int countReadSpectra() {
        return m_countReadSpectra;
    }

    @Override
    public boolean hasNext() {
        synchronized(m_sync) {
            return m_nextShifted != null;
        }
    }

    @Override
    public Spectra next() {
        synchronized(m_sync) {
            if (m_next != null) {
                m_current = m_next;
                m_next = null;
            } else {
                m_current = m_nextShifted;
                if (m_InnerAcces.hasNext()) {
                    m_next = m_InnerAcces.next();
                    m_nextShifted = m_next.cloneShifted(m_ShiftMass);
                    m_nextShifted.setRun(m_nextShifted.getRun() + "_SHIFTED");
                }
            }
            return m_current;
        }


    }

    @Override
    public void setReader(SpectraAccess innerAccess) {
        super.setReader(innerAccess);
        next();
    }


}
