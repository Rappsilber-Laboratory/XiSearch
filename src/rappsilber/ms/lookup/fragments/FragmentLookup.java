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
package rappsilber.ms.lookup.fragments;

import java.util.ArrayList;
import java.util.Map;
import rappsilber.ms.lookup.Lookup;
import rappsilber.ms.lookup.Lookup;
import rappsilber.ms.sequence.Iterators.PeptideIterator;
import rappsilber.ms.sequence.Peptide;

public interface FragmentLookup extends Lookup<Peptide> {
    Map<Peptide,Double> getPeptidesForMasses(double mass);
    int getFragmentCount();
    public int countPeptides(double mass);
    public int countPeptides(double mass, double targetMass);

    public Peptide lastFragmentedPeptide();
    public PeptideIterator getPeptideIterator();
//    public boolean nextRound();
}
