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
package rappsilber.ms.dataAccess.filter.fastafilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import rappsilber.ms.sequence.Sequence;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FilterByID implements FastaFilter {

    HashSet<String> m_accessions = new HashSet<String>();

    public static enum filtermode {INCLUDE, EXCLUDE};
    
    public static filtermode INCLUDE = filtermode.INCLUDE;
    public static filtermode EXCLUDE = filtermode.EXCLUDE;
    private filtermode m_mode = filtermode.INCLUDE;


    public FilterByID() {
    }
    
    public FilterByID(filtermode mode) {
        m_mode = mode;
    }
    
    public void setFilterMode(filtermode mode) {
        m_mode = mode;
    }
    
    
    
    
    
    public void setAccessions(Collection<String> accessions) {
        m_accessions = new HashSet<String>(accessions);
    }
    
    public void addAccession(String accession) {
        m_accessions.add(accession);
    }

    public void addAllAccessions(Collection<String> accessions) {
        m_accessions.addAll(accessions);
    }
    
    public Sequence[] getSequences(Sequence s) {
        String accession = s.getSplitFastaHeader().getAccession();
        if (m_mode == INCLUDE) {
            if (m_accessions.contains(accession)) {
                return new Sequence[]{s};
            }

            return new Sequence[0];
        } else {
            if (m_accessions.contains(accession)) {
                return new Sequence[0];
            }

            return new Sequence[]{s};
        }
    }

    public Collection<Sequence> getSequences(Collection<Sequence> s) {
        ArrayList<Sequence> ret = new ArrayList<Sequence>(s.size());
        if (m_mode == INCLUDE) {
            for (Sequence rs : s) {
                if (m_accessions.contains(rs.getSplitFastaHeader().getAccession())) {
                    ret.add(rs);
                }
            }
        } else {
            for (Sequence rs : s) {
                if (!m_accessions.contains(rs.getSplitFastaHeader().getAccession())) {
                    ret.add(rs);
                }
            }
        }
        
        return ret;
    }
    
    
}
