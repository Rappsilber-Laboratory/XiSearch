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
package rappsilber.ms.spectra.match;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public interface ScoredMatch {
    
    /**
     * returns the score for this match
     * @return 
     */
    double getScore();
    
    boolean isInternal();
    
    boolean isAmbigious();
    
    boolean isDecoy();
    
    boolean isDecoy(int site);
    
    int sites();
    
    int length(int site);
    
    public String key();    

    /**
     * provides a list of the matched fragments. <br/>
     * @return
     */
    MatchedFragmentCollection getMatchedFragments();

    /**
     * @return the m_matchrank
     */
    int getMatchrank();

     /**
     * @return the m_matchrank
     */
    void setMatchrank(int rank);   
    /**
     * Returns, whether peptide one and two could be consecutive Peptides in a sequence.
     * Therefore indicates, whether a cross-linked match could actually be a linear modified one
     * @return the m_mightBeLinear
     */
    Boolean getMightBeLinear();
}
