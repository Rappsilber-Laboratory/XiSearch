/*
 * Copyright 2018 Lutz Fischer <lfischer@staffmail.ed.ac.uk>.
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
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class SingleSpectrumAccess extends AbstractSpectraAccess {
    Spectra current = null;
    Spectra spectrum;

    public SingleSpectrumAccess(Spectra spectrum) {
        this.spectrum = spectrum;
    }
    
    
    @Override
    public Spectra current() {
        return current;
    }

    @Override
    public int getSpectraCount() {
        return 1;
    }

    @Override
    public void gatherData() throws FileNotFoundException {
    }

    @Override
    public int countReadSpectra() {
        return (current == null?0:1);
    }

    @Override
    public boolean canRestart() {
        return true;
    }

    @Override
    public void restart() throws IOException {
        current=null;
    }

    @Override
    public void close() {
        
    }

    @Override
    public boolean hasNext() {
        return current == null;
    }

    @Override
    public Spectra next() {
        if (current == null) {
            current= spectrum;
        }
        return current;
    }
    
    
    
}
