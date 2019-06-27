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
package rappsilber.ms.statistics;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SearchStatistic {
    long spectra_read = 0;
    long matches_writen = 0;
    long spectra_matched = 0;
    long matches_autovalidated = 0;
    long target_proteins = 0;
    long decoy_proteins = 0;
    long target_peptides = 0;
    long decoy_peptides = 0;
    
    public void add(SearchStatistic ss) {
        this.spectra_read+=ss.spectra_read;
        this.spectra_matched+=ss.spectra_matched;
        this.matches_writen+=ss.matches_writen;
        this.target_proteins+=ss.target_proteins;
        this.target_peptides+=ss.target_peptides;
        this.decoy_proteins+=ss.decoy_proteins;
        this.decoy_peptides+=ss.decoy_peptides;
        this.matches_autovalidated+=ss.matches_autovalidated;
    }
}
