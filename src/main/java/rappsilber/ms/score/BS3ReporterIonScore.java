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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.dataAccess.msm.MSMIterator;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class BS3ReporterIonScore extends AbstractScoreSpectraMatch{
    Double[] classContainingIonsMZ = new Double[]{138.09134, 139.07536, 156.10191, 192.13829, 194.15394, 221.16484, 222.14886, 239.1754,240.15942, 267.17032, 285.18088};
    Double[] classContainingIonsScore = new Double[]{0.16797257590597, 0.00048971596474, 0.06709108716944, 4.89715964740451E-05, 4.89715964740451E-05, 0.00734573947111, 0.00048971596474, 0.0847208619001,0.00195886385896, 0.00342801175318, 0.0024485798237};

    Double[] classCrosslinkedIonsMZ = new Double[]{305.22235, 350.24382};
    Double[] classCrosslinkedIonsScore = new Double[]{0.01641414141414, 0.00252525252525};

    Double[] classModifiedIonsMZ = new Double[]{157.08592, 257.18597};
    Double[] classModifiedIonsScore = new Double[]{0.01641414141414, 0.00252525252525};

    private static final String[] m_scoreNames = {"BS3ReporterIonScore","Crosslinked", "Modified", "Containing"};

    @Override
    public double score(MatchedXlinkedPeptide match) {
        double[] score = getScore(match.getSpectrum());
        addScore(match, m_scoreNames[0], score[0]);
        addScore(match, m_scoreNames[1], score[1]);
        addScore(match, m_scoreNames[2], score[2]);
        addScore(match, m_scoreNames[3], score[3]);

        return score[0];
    }


    public double[] getScore(Spectra s) {
        double cross = 1;
        double contain = 1;
        double modified = 1;


        for (int i = 0; i< classContainingIonsMZ.length;i++) {
            if (s.getPeakAt(classContainingIonsMZ[i]) != null)  {
                contain *= classContainingIonsScore[i];
            }
        }

        for (int i = 0; i< classModifiedIonsMZ.length;i++) {
            if (s.getPeakAt(classModifiedIonsMZ[i]) != null)  {
                modified *= classModifiedIonsScore[i];
            }
        }

        for (int i = 0; i< classCrosslinkedIonsMZ.length;i++) {
            if (s.getPeakAt(classCrosslinkedIonsMZ[i]) != null)  {
                cross *= classCrosslinkedIonsScore[i];
            }
        }


        return new double[] {-Math.log(cross * contain + (1-modified)), -Math.log(cross), -Math.log(contain), -Math.log(modified)};

    }

    @Override
    public double getOrder() {
        return 10000;
    }

    @Override
    public String[] scoreNames() {
        return m_scoreNames;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ParseException {
        BS3ReporterIonScore score = new BS3ReporterIonScore();
        AbstractMSMAccess in = AbstractMSMAccess.getMSMIterator(args[0], new ToleranceUnit(args[1]), 0, null);

        while (in.hasNext()) {
            Spectra s = in.next();
            System.out.println(s.getRun() + "," +s.getScanNumber() +"," + score.getScore(s)[0]);
        }

    }



}
