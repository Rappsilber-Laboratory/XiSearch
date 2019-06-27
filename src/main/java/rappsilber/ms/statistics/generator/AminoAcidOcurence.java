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
package rappsilber.ms.statistics.generator;

import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.utils.CountOccurence;

/**
 * a small statistics-module, that counts the frequency of occurrence for each
 * amino acid
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class AminoAcidOcurence extends AbstractStatistic{
    //StatisticsFragementationSiteSingle

    CountOccurence<AminoAcid> AminoOccurence = new CountOccurence<AminoAcid>();


    /** default constructor */
    public AminoAcidOcurence() {
    }

    public void countSpectraMatch(MatchedXlinkedPeptide match) {
        Peptide p1 = match.getPeptide(0);
        Peptide p2 = match.getPeptide(1);
        for (int i = 0; i<p1.length(); i++)
            AminoOccurence.add(p1.aminoAcidAt(i));
        for (int i = 0; i<p2.length(); i++)
            AminoOccurence.add(p2.aminoAcidAt(i));
    }

    public String getTable() {
        StringBuffer sb = new StringBuffer();
        for (AminoAcid aa : AminoAcid.getRegisteredAminoAcids()) {
            sb.append(aa.toString() + "\t" + AminoOccurence.count(aa) + "\n");
        }

        return sb.toString();

    }


}
