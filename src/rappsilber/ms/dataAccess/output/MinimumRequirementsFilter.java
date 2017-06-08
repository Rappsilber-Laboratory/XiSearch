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
import rappsilber.ms.score.FragmentCoverage;
import rappsilber.ms.score.Normalizer;
import rappsilber.ms.score.SpectraCoverage;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MinimumRequirementsFilter extends AbstractStackedResultWriter {

    private int m_max_rank = -1;
    public MinimumRequirementsFilter() {
    }

    public MinimumRequirementsFilter(ResultWriter innerWriter) {
        setInnerWriter(innerWriter);
    }


    @Override
    public void writeResult(MatchedXlinkedPeptide match) throws IOException {
        if (match.getScore("fragment " + FragmentCoverage.mNL) <= 1 ) {
            if (m_doFreeMatch)
                match.free();
            return;
        }

        // top matches get always writen
        if (match.getMatchrank() <= 2)
            innerWriteResult(match);
        else {
            // if it explaines more then 5% of the spectra - peak or intensity wise - and has 3 or more fragmentation sites it will be writen
            if (((match.getScore(SpectraCoverage.mp)>0.025 ||
                    match.getScore(SpectraCoverage.pmp)>0.025) && (
                    match.getMatchedFragments().size() >2)))
                // also exlude any match, that has a larger negative delta score, then its own match score - should be meaningless match
                if (-2*match.getScore("delta")<match.getScore("match score"))
                    if (getMaxRank() == -1 || match.getMatchrank() <= getMaxRank()) {
                        innerWriteResult(match);
                        return;
                    }
            if (m_doFreeMatch)
                match.free();
        }

    }

    /**
     * @return the m_max_rank
     */
    public int getMaxRank() {
        return m_max_rank;
    }

    /**
     * @param m_max_rank the m_max_rank to set
     */
    public void setMaxRank(int maxRank) {
        this.m_max_rank = maxRank;
    }

    @Override
    public void selfFinished() {
    }

    @Override
    public boolean selfWaitForFinished() {
        return true;
    }

}
