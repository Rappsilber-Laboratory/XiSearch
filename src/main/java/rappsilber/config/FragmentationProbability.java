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
package rappsilber.config;

import java.util.HashMap;
import rappsilber.ms.sequence.AminoAcid;

/**
 * reads in a file, that defines the 
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FragmentationProbability {
    private class FragmentationSite {
        /** how often did a fragmention appear */
        int occurence;
        /** how often could it have been observed */
        int possible;
        /** what was the average intensity */
        double intensity;
        /** Standart diviation of the intensity */
        double intenistyStdDev;
    }

    private class FragmentationList extends HashMap<AminoAcid,HashMap<AminoAcid, FragmentationSite>> {
        private static final long serialVersionUID = 2512705763765453144L;
    }

    //HashMap<AminoAcid,HashMap<AminoAcid, FragmentationSite>>
    private HashMap<Class, FragmentationList> m_IonFragmentation;


    // read in the satstic
    // score occurence

}
