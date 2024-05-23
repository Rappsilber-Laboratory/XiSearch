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
package rappsilber.ms.spectra.match.filter;

import java.util.ArrayList;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 * meta filter, that can apply a set of filters to a given spectra
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class BatchFilter implements MatchFilter{
    
    private ArrayList<MatchFilter> m_filters = new ArrayList<MatchFilter>();

    public BatchFilter() {

    }
    
    public BatchFilter(MatchFilter[] filters) {
        for (MatchFilter mf: filters) {
            addFilter(mf);
        }
    }




    public void addFilter(MatchFilter filter) {
        m_filters.add(filter);
    }

    @Override
    public void filter(MatchedXlinkedPeptide match) {
        for (MatchFilter mf : m_filters) {
            mf.filter(match);
        }
    }


}
