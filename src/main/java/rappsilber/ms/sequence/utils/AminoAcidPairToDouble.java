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
package rappsilber.ms.sequence.utils;

import java.util.HashMap;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.utils.ArithmeticScoredOccurence;
import rappsilber.utils.CountOccurence;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class AminoAcidPairToDouble extends HashMap<AminoAcid,HashMap<AminoAcid,Double>> {

    private static final long serialVersionUID = -6717512921527632230L;

    ArithmeticScoredOccurence<AminoAcid> NSum = new ArithmeticScoredOccurence<AminoAcid>();
    ArithmeticScoredOccurence<AminoAcid> CSum = new ArithmeticScoredOccurence<AminoAcid>();
    CountOccurence NCount = new CountOccurence();
    CountOccurence CCount = new CountOccurence();

    double AllSum = 0;
    double AllCount = 0;

    public void put(AminoAcid N, AminoAcid C, double probability) {
        NSum.add(N, probability);
        AllSum+=probability;
        NCount.add(N);
        CSum.add(C, probability);
        CCount.add(C);

        HashMap<AminoAcid, Double> Cmap = this.get(N);

        if (Cmap == null) {
            Cmap = new HashMap<AminoAcid, Double>();
            this.put(N, Cmap);
        }
        AllCount++;
        Cmap.put(C, probability);

    }

    public void AverageMissing() {

        for (AminoAcid C : CSum.getScoredObjects()) {
            for (AminoAcid N : NSum.getScoredObjects()){
                if (getNullable(N, C) == null) {
                    put(N,C,(NSum.Score(N, AllSum/AllCount) + CSum.Score(C, AllSum/AllCount))/(NCount.count(N) + NCount.count(N)));
                }
            }
        }

    }

    public Double getNullable(AminoAcid N, AminoAcid C) {
        HashMap<AminoAcid,Double> cHash = this.get(N);
        if (cHash == null) { // not found as NTerminal -> take the average
            return null;
        } else {
            Double probability = cHash.get(C);
            if (probability == null) { // but not the C-site amino acid
                return null;
            } else {
                return probability;
            }
        }

    }


    public double get(AminoAcid N, AminoAcid C) {
        HashMap<AminoAcid,Double> cHash = this.get(N);
        Double probability;

        if (cHash == null) { // not found as NTerminal -> take the average

            probability = AllSum/AllCount;

            if (CSum.seen(C)) {
                // we have found this one as Cside of a pair - so we take the average for that
                return  (probability + CSum.Score(C,0)) / 2;
            } else {
                // never found on a c-side of a fragmentation -> take the average of all c-sides
                return  (probability + AllSum) / 2;
            }
        } else { // N terminal site was found
            probability = cHash.get(C);
            if (probability == null) { // but not the C-site amino acid
                return (NSum.Score(N,0) + AllSum) / 2;
            } else {
                // everything was found
                return probability;
            }
        }

    }
}
