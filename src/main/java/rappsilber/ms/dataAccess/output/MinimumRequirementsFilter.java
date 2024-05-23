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
import rappsilber.ms.score.SpectraCoverage;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MinimumRequirementsFilter extends AbstractStackedResultWriter {
    private double ms2limit =Double.NaN;
    private int m_max_rank = -1;
    public MinimumRequirementsFilter() {
        
    }
    public MinimumRequirementsFilter(double ms2errorlimit) {
        ms2limit=ms2errorlimit;
    }

    public MinimumRequirementsFilter(ResultWriter innerWriter) {
        setInnerWriter(innerWriter);
    }
    public MinimumRequirementsFilter(ResultWriter innerWriter,double ms2errorlimit) {
        setInnerWriter(innerWriter);
        ms2limit=ms2errorlimit;
    }


    @Override
    public void writeResult(MatchedXlinkedPeptide match) throws IOException {
        if (match.getScore("fragment " + FragmentCoverage.mNL) <= 1 || 
                // do we have a defined ms2 limit?
                ((!Double.isNaN(ms2limit)) && (
                    // overall should not exceed it
                    (match.getScore(rappsilber.ms.score.Error.mAverageAbsoluteMS2) > ms2limit) ||
                    // pep1 should not exceed it
                    (match.getScore(rappsilber.ms.score.Error.mAverageAbsolutePep1MS2) > ms2limit ) ||
                    // cross-linked fragments should not exceed it
                    ((!Double.isNaN(match.getScore(rappsilber.ms.score.Error.mAverageAbsoluteXLMS2))) 
                            && match.getScore(rappsilber.ms.score.Error.mAverageAbsoluteXLMS2) > ms2limit) ||
                    // pep2 should not exceed it
                    ((!Double.isNaN(match.getScore(rappsilber.ms.score.Error.mAverageAbsolutePep2MS2))) 
                            && match.getScore(rappsilber.ms.score.Error.mAverageAbsolutePep2MS2) > ms2limit)
                    )
                )
                ) {
            if (m_doFreeMatch) {
                match.free();
            }
            return;
        }

        // top matches get always writen
        if (match.getMatchrank() <= 2) {
            innerWriteResult(match);
        } else {
            // if it explaines more then 5% of the spectra - peak or intensity wise - and has 3 or more fragmentation sites it will be writen
            if (((match.getScore(SpectraCoverage.mp)>0.025 ||
                    match.getScore(SpectraCoverage.pmp)>0.025) && (
                    match.getMatchedFragments().size() >2))) {
                if (-2*match.getScore("delta")<match.getScore("match score")) {
                    if (getMaxRank() == -1 || match.getMatchrank() <= getMaxRank()) {
                        innerWriteResult(match);
                        return;
                    }
                }
            }
            if (m_doFreeMatch) {
                match.free();
            }
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
