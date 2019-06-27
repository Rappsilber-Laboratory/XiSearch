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
import rappsilber.ms.sequence.ions.BIon;
import rappsilber.ms.sequence.ions.loss.BIonWaterGain;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakAnnotation;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.utils.CountOccurence;

/**
 * looks for BIons that have a gain of water - suppostly that can happen at the B(n-1) ion
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class WaterGain extends AbstractStatistic{
    //StatisticsFragementationSiteSingle

    CountOccurence<Integer> BIonOccurence = new CountOccurence<Integer>();
    CountOccurence<Integer> BIonOccurenceReverse = new CountOccurence<Integer>();
    CountOccurence<Integer> BIonOccurenceOnly = new CountOccurence<Integer>();
    CountOccurence<Integer> BIonOccurenceReverseOnly = new CountOccurence<Integer>();
    int count = 0;



    public WaterGain() {
    }

    public void countSpectraMatch(MatchedXlinkedPeptide match) {
        MatchedFragmentCollection MatchedFragments = new MatchedFragmentCollection(match.getSpectrum().getPrecurserCharge());
        for (SpectraPeak sp : match.getSpectrum().getPeaks()) {
            for (SpectraPeakMatchedFragment mf : sp.getMatchedAnnotation()) {
                MatchedFragments.add(mf.getFragment(), mf.getCharge(), sp);
                if (mf.getFragment().isClass(BIonWaterGain.class)) {
                    count++;
                }
            }
        }

        for (MatchedBaseFragment mbf : MatchedFragments) {
            for (Loss l : mbf.getLosses().keySet()) {

                SpectraPeak sp = mbf.getLosses().get(l);
                // check only, if it is the only "loss" at the BIon
                if (l instanceof BIonWaterGain && l.getParentFragment() instanceof BIon
                        //&& mbf.isBaseFragmentFound() // and a B-Ion without loss was found
                        &&  sp.hasAnnotation(SpectraPeakAnnotation.monoisotop)) { // and is the monoisotopic peak of a cluster
                        BIonOccurence.add((int)l.length());
                        BIonOccurenceReverse.add(l.getPeptide().length() - l.length());
                        if (sp.getMatchedFragments().size() == 1) {
                            BIonOccurenceOnly.add((int)l.length());
                            BIonOccurenceReverseOnly.add(l.getPeptide().length() - l.length());
                        }

                }

            }
        }
    }



    public String getTable() {
        StringBuffer sb = new StringBuffer();
        for (Integer i : BIonOccurence.getCountedObjects()) {
            sb.append("B" + i + "\t" + BIonOccurence.count(i) + "\n");
        }

        for (Integer i : BIonOccurenceReverse.getCountedObjects()) {
            sb.append("B(n - " + i + ")\t" + BIonOccurenceReverse.count(i) + "\n");
        }

        for (Integer i : BIonOccurenceOnly.getCountedObjects()) {
            sb.append("Only B" + i + "\t" + BIonOccurenceOnly.count(i) + "\n");
        }

        for (Integer i : BIonOccurenceReverseOnly.getCountedObjects()) {
            sb.append("Only B(n - " + i + ")\t" + BIonOccurenceReverseOnly.count(i) + "\n");
        }

        return sb.toString();

    }


}
