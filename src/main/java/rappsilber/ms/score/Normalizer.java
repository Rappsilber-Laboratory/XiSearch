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
package rappsilber.ms.score;

import java.util.HashMap;
import rappsilber.data.ScoreInfos;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 * normalises scores and sums them up
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class Normalizer extends AbstractScoreSpectraMatch {


    /** should the original score be replaced by the normalized one? */
    boolean m_InsertNormalizedScores = false;
    /** contains information about the score like the expected average and standard-deviation */
    HashMap<String,ScoreInfos.ScoreInfoStruct> m_scoreInfos = ScoreInfos.getScoreInfos();

    double m_order = 100000;
    public static final String NAME = "match score";

    public Normalizer() {
        super();
    }


    public double score(MatchedXlinkedPeptide match) {
        HashMap<String, Double> scores = match.getScores();
        double CombScore = 0;
        int scorecount = 0;

        for (String name : scores.keySet()) {
            ScoreInfos.ScoreInfoStruct si = m_scoreInfos.get(name);
            if (si != null) {
                double normScore = (scores.get(name) - si.average)/si.stdev;
                if (!Double.isNaN(normScore) && !Double.isInfinite(normScore)) {
                    scorecount ++;
                    CombScore += normScore * si.weigth;
                    
                    if (m_InsertNormalizedScores) {
                        scores.put(name, normScore);
                    }

                }
//                else {
//                    System.err.println("Scpre " + name + " is " + normScore);
//                }
            }
        }
        if (scorecount == 0)
            super.addScore(match, NAME, 0);
        else
            super.addScore(match, NAME, CombScore/scorecount);
        return 0;
    }

    public double getOrder() {
        return m_order;
    }


    public String[] scoreNames() {
        return new String[]{NAME};
    }


}
