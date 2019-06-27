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

import java.util.ArrayList;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public interface ScoreSpectraMatch extends Comparable<ScoreSpectraMatch> {

    /**
     * calculates a number of scores and returns one (hopefully most meaning full)
     *  score.
     * The scores are saved under the list of names with in the match
     * @param match
     * @return one of the scores
     */
    double score(MatchedXlinkedPeptide match);

    /**
     * returns a name for the score collection
     * @return name for the score-group
     */
    String name();

    /**
     * returns a list of names for all scores that are provided by this instance
     * @return Array of Strings under which
     */
    String[] scoreNames();


    /**
     * should provide the average of all scored scans for the given score
     * @param name name of the score
     * @return average of all up to now seen scores
     */
    double getAverage(String name);

    /**
     * should provide the median (or as of now an estimate) of all scored scans for the given score
     * @param name name of the score
     * @return median of all up to now seen scores
     */
    double getMedian(String name);

    /**
     * should provide the median absolute deviation (or as of now an estimate) of all scored scans for the given score
     * @param name name of the score
     * @return median absolute deviation
     */
    double getMAD(String name);
    

    /**
     * should provide the standard deviation of all scored scans for the given score
     * @param name name of the score
     * @return average of all up to now seen scores
     */
    double getStdDev(String name);

    /**
     * the smallest observed value for a given score
     * @param name name of the score
     * @return smallest score value
     */
    double getMin(String name);
    /**
     * the largest observed value for a given score
     * @param name name of the score
     * @return largest score value
     */
    double getMax(String name);

    
    
    double getOrder();

    
    
}
