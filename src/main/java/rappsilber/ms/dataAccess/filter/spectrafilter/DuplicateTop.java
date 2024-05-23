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

import rappsilber.ms.dataAccess.AbstractStackedSpectraAccess;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DuplicateTop extends AbstractStackedSpectraAccess {
    /** the next level - provides the actual spectra */
//    private SpectraAccess m_innerAcces;

    private Spectra m_current;

    private Spectra m_next;
    private Spectra m_nextTop;

    private int     m_TopPeaks;
    private double     m_TopWindow;
    private int     m_countReadSpectra = 0;
//    private final Object m_sync = new Object();

    public DuplicateTop(int toppeaks,double window) {
        this.m_TopPeaks = toppeaks;
        m_TopWindow = window;
    }

    public DuplicateTop(SpectraAccess innerAccess, int toppeaks, double window) {
        this(toppeaks, window);
        setReader(innerAccess);
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
    public Spectra next() {
        synchronized(m_sync) {
            if (m_next != null) {
                m_current = m_next;
                m_next = null;
            } else {
                m_current = m_nextTop;
                if (m_InnerAcces.hasNext()) {
                    m_next = m_InnerAcces.next();
                    m_nextTop = m_next.cloneTopPeaks(m_TopPeaks,m_TopWindow);
                    m_nextTop.setRun(m_nextTop.getRun() + "_TOP");
                }
            }
            return m_current;
        }


    }

    @Override
    public void setReader(SpectraAccess innerAccess) {
        m_InnerAcces = innerAccess;
        next();
    }


}
