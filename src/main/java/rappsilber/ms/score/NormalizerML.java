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
import rappsilber.config.RunConfig;
import rappsilber.data.ScoreInfosML;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 * Normalises scores and sums them up.
 * The idea is, to first normalise each score, that they all are centred around 0.
 * In the next step I divide each score by a pre-determined "good" cutoff score.
 * The result of that should be, that each score has a good cut-off at 1 and is 
 * normalised.
 * Finally all scores are getting weighted and summed up.
 * 
 * Both the cut-off and weight are derived from the trees that make up the 
 * {@link AutoValidation}. 
 * Basically the earlier in the tree building a score is used the higher the 
 * weight and the the cut-off point is a (hight) weighted average of all cutting
 * values of this attribute.
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class NormalizerML extends AbstractScoreSpectraMatch {


    /** should the original score be replaced by the normalised one? */
    boolean m_InsertNormalizedScores = false;
    Double missingScoreValue = null;
    /** contains information about the score like the expected average and standard-deviation */
    HashMap<String,ScoreInfosML.ScoreInfoStruct> m_scoreInfos = ScoreInfosML.getScoreInfos();

    static double m_order = 100001;
    public static final String NAME = "match score";
    RunConfig m_config;

    public NormalizerML(RunConfig conf) {
        super();
        m_config = conf;
        Object dv = m_config.retrieveObject("normalizerml_defaultsubscorevalue");
        if (dv != null) {
            missingScoreValue=new Double(dv.toString().trim());
        }
    }


    public double score(MatchedXlinkedPeptide match) {
        HashMap<String, Double> scores = match.getScores();
        double CombScore = 0;
        int scorecount = 0;
        double totalWeight= 0;

        for (String name : scores.keySet()) {
            ScoreInfosML.ScoreInfoStruct si = m_scoreInfos.get(name);
            double score = scores.get(name);
            if (si != null && si.weigth !=0 && score < 999999) {
                double normScore = (score - si.average)/si.stdev;
                if (!Double.isNaN(normScore) && !Double.isInfinite(normScore) && si.factor != 0 && si.weigth != Double.NaN ) {
                    scorecount ++;
                    
                    if (si.normalizedSplit == 0) {
                        CombScore += normScore/si.factor * si.weigth;
                    } else {
                        CombScore += normScore/si.normalizedSplit * si.factor * si.weigth;
                    }
                    
                    totalWeight += Math.abs(si.weigth);
                    
                    if (m_InsertNormalizedScores) {
                        scores.put(name, normScore);
                    }

                } else if (missingScoreValue != null)  {
                    scorecount ++;
                    totalWeight += Math.abs(si.weigth);
                    CombScore += missingScoreValue;
                }
//                else {
//                    System.err.println("Scpre " + name + " is " + normScore);
//                }
            }
        }
        
        double finalScore = 0;
        if (scorecount > 0) {
            finalScore = CombScore/totalWeight*7;
        }
        super.addScore(match, NAME, finalScore);
            
        return finalScore;
    }

    public double getOrder() {
        return m_order;
    }


    public String[] scoreNames() {
        return new String[]{NAME};
    }


}
