/*
 * Copyright 2017 Lutz Fischer <lfischer@staffmail.ed.ac.uk>.
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
package rappsilber.ms.sequence.ions;

import java.util.ArrayList;
import java.util.Collection;
import rappsilber.ms.crosslinker.CrossLinker;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public interface CrossLinkedFragmentProducer {
    
    ArrayList<Fragment> createCrosslinkedFragments(Collection<Fragment> fragments, Fragment Crosslinked, CrossLinker crosslinker, boolean noPeptideIons);

    ArrayList<Fragment> createCrosslinkedFragments(Collection<Fragment> fragments, Collection<Fragment> Crosslinked, CrossLinker crosslinker, boolean noPeptideIons);

    /**
     *
     * @param fragments
     * @param Crosslinked
     * @param crosslinker
     * @param linkSite1
     * @param linkSite2
     * @return
     */
    ArrayList<Fragment> createCrosslinkedFragments(Collection<Fragment> fragments, Collection<Fragment> Crosslinked, CrossLinker crosslinker, int linkSite1, int linkSite2);
    
}
