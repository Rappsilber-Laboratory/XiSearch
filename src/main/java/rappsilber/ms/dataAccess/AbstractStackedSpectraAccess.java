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
public abstract class AbstractStackedSpectraAccess extends AbstractSpectraAccess implements StackedSpectraAccess{
    protected SpectraAccess m_InnerAcces;
    protected final Object m_sync = new Object();

    public void setReader(SpectraAccess innerAccess) {
        m_InnerAcces = innerAccess;
    }


    @Override
    public boolean hasNext() {
        synchronized (m_sync) {
            return m_InnerAcces.hasNext();
        }
    }

    @Override
    public Spectra current() {
        return m_InnerAcces.current();
    }
    
    @Override
    public void restart() throws IOException {
        m_InnerAcces.restart();
    }

    public boolean canRestart() {
        return m_InnerAcces.canRestart();
    }

    public void close() {
        m_InnerAcces.close();
    }

    @Override
    public int countReadSpectra() {
        return m_InnerAcces.countReadSpectra();
    }


    @Override
    public void gatherData() throws FileNotFoundException {
        if (m_InnerAcces instanceof AbstractSpectraAccess) {
            ((AbstractSpectraAccess) m_InnerAcces).gatherData();
        }
    }

    public int getSpectraCount() {
        if (m_InnerAcces instanceof AbstractSpectraAccess) {
            return ((AbstractSpectraAccess) m_InnerAcces).getSpectraCount();
        }
        return -1;
    }

    @Override
    public long getDiscardedSpectra() {
        if (m_InnerAcces instanceof AbstractSpectraAccess) {
            return ((AbstractSpectraAccess) m_InnerAcces).getDiscardedSpectra();
        }
        return 0;
    }

    
}
