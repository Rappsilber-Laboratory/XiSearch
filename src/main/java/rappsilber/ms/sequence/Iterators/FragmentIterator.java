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
package rappsilber.ms.sequence.Iterators;

import java.util.Iterator;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.ions.Fragment;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public interface FragmentIterator  extends Iterator<Fragment>{

    /**
     * @return the Sequence, to wich the current fragment belongs
     */
    public Sequence getCurrentSequence();

    /**
     * @return the Peptide, to wich the current fragment belongs
     */
    public Peptide getCurrentPeptide();
}
