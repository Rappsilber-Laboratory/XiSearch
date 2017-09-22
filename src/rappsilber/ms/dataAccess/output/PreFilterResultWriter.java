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
package rappsilber.ms.dataAccess.output;

import java.io.IOException;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.spectra.match.filter.MatchFilter;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class PreFilterResultWriter extends AbstractStackedResultWriter {


    private MatchFilter m_filter;


    public PreFilterResultWriter() {
    }

    public PreFilterResultWriter(ResultWriter innerWriter, MatchFilter filter) {
        setInnerWriter(innerWriter);
        this.m_filter = filter;
    }

    public PreFilterResultWriter(ResultWriter innerWriter) {
        setInnerWriter(innerWriter);
    }


    @Override
    public void writeResult(MatchedXlinkedPeptide match) throws IOException {
        m_filter.filter(match);
        innerWriteResult(match);
    }




    /**
     * @return the m_filter
     */
    public MatchFilter getFilter() {
        return m_filter;
    }

    /**
     * @param m_filter the m_filter to set
     */
    public void setFilter(MatchFilter m_filter) {
        this.m_filter = m_filter;
    }

    @Override
    public void selfFinished() {
    }

    @Override
    public boolean selfWaitForFinished() {
        return true;
    }



}
