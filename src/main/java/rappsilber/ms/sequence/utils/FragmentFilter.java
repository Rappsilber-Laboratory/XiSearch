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
package rappsilber.ms.sequence.utils;

import rappsilber.ms.sequence.ions.Fragment;

/**
 * provides an interface for "plugable" filter to select fragments
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public interface FragmentFilter {

    /**
     * Returns whether the fragment should be considered or ignored
     * @param f true: take into account ; false: ignore the fragment
     * @return
     */
    boolean isValid(Fragment f);
}
