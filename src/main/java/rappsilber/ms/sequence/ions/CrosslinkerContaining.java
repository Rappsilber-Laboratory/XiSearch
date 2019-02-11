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
package rappsilber.ms.sequence.ions;

import java.util.HashMap;
import rappsilber.ms.sequence.Peptide;

/**
 * an empty interface, that should be implemented by all fragments classes, that represent a crosslinked fragment
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public interface CrosslinkerContaining {

    boolean canFullfillXlink(HashMap<Peptide,Integer> site);

    public boolean canFullfillXlink(Peptide p, int site);

}
