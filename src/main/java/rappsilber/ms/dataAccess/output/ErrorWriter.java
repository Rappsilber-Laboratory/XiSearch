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

import rappsilber.ms.sequence.ions.loss.Loss;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.*;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.utils.CountOccurence;

/**
 * A ouput module for writing the results as tabular text to a stream (like system.out)
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ErrorWriter extends AbstractResultWriter{
    /** the stream used to write something */
    PrintStream m_out;
    private int m_resultCount = 0;
    private int m_topResultCount = 0;

    /** 
     * create a new class and connect it to the given output stream
     * @param out where to output the data
     */
    public ErrorWriter(OutputStream out) {
        m_out = new PrintStream(out);
    }

    public void writeHeader() {
                m_out.println("Run\tScanNumber\tcrosslinker\tFastaHeader1\tPeptide1\tFastaHeader2\t" +
                        "Peptide2\tMatchedPeptide\talpha/beta\tLinkerPosition1\tLinkerPostion2\t" +
                        "FragmentName\tFragment\tNeutralMass\tCharge\tCalcMZ\tExpMZ\tIsotopPeak\tDescription\tvirtual\tBasePeak");
    }

    public void writeResult(MatchedXlinkedPeptide match) {
        Spectra s = match.getSpectrum();
        Peptide pep1 = match.getPeptides()[0];
        Peptide pep2 = match.getPeptides()[1];
        CountOccurence<Fragment> count = new CountOccurence<Fragment>();
        MatchedFragmentCollection mfc = match.getMatchedFragments();

        for (SpectraPeak peak :  match.getSpectrum().getPeaks()) {
            ArrayList<SpectraPeakMatchedFragment> spmf = peak.getMatchedAnnotation();

//            if ((int) peak.getMZ() == 330)
//                System.out.println("found");

            ArrayList<SpectraPeakMatchedFragment> spmfReduced = new ArrayList<SpectraPeakMatchedFragment>();

            if (spmf.size() > 0) {
                // all basic non-lossy explanations get through
                for (SpectraPeakMatchedFragment mf: spmf) {
                    Fragment f = mf.getFragment();
                    if (!mf.matchedMissing() && !f.isClass(DoubleFragmentation.class) && !f.isClass(Loss.class)) {
                        spmfReduced.add(mf);
                    }
                }

//                // only if non found look for losses , that have nonlossy matches
//                if (spmfReduced.size() == 0)
//                    for (SpectraPeakMatchedFragment mf: spmf) {
//                        Fragment f = mf.getFragment();
//                        int charge = mf.getCharge();
//                        if (!mf.matchedMissing() && !f.isClass(DoubleFragmentation.class) && f.isClass(Loss.class)) {
//                            if (mfc.getMatchedFragmentGroup(f, charge).isBaseFragmentFound())
//                                spmfReduced.add(mf);
//                        }
//                    }
//
//
//                // next nonlossy double fragmentations
//                if (spmfReduced.size() == 0)
//                    for (SpectraPeakMatchedFragment mf: spmf) {
//                        Fragment f = mf.getFragment();
//                        int charge = mf.getCharge();
//                        if (!mf.matchedMissing() && f.isClass(DoubleFragmentation.class) && !f.isClass(Loss.class)) {
//                            spmfReduced.add(mf);
//                        }
//                    }
//
//
//
//                // all basic non-lossy explanations get through
//                for (SpectraPeakMatchedFragment mf: spmf) {
//                    Fragment f = mf.getFragment();
//                    if (!f.isClass(DoubleFragmentation.class) && !f.isClass(Loss.class)) {
//                        spmfReduced.add(mf);
//                    }
//                }
//
//                // only if non found look for losses , that have nonlossy matches
//                if (spmfReduced.size() == 0)
//                    for (SpectraPeakMatchedFragment mf: spmf) {
//                        Fragment f = mf.getFragment();
//                        int charge = mf.getCharge();
//                        if (!f.isClass(DoubleFragmentation.class) && f.isClass(Loss.class)) {
//                            if (mfc.getMatchedFragmentGroup(f, charge).isBaseFragmentFound())
//                                spmfReduced.add(mf);
//                        }
//                    }
//
//
//                // next nonlossy double fragmentations
//                if (spmfReduced.size() == 0)
//                    for (SpectraPeakMatchedFragment mf: spmf) {
//                        Fragment f = mf.getFragment();
//                        int charge = mf.getCharge();
//                        if (f.isClass(DoubleFragmentation.class) && !f.isClass(Loss.class)) {
//                            spmfReduced.add(mf);
//                        }
//                    }
//
//                // next lossy double fragmentation
//                if (spmfReduced.size() == 0)
//                    for (SpectraPeakMatchedFragment mf: spmf) {
//                        Fragment f = mf.getFragment();
//                        int charge = mf.getCharge();
//                        if (f.isClass(DoubleFragmentation.class) && f.isClass(Loss.class)) {
//                            if (mfc.getMatchedFragmentGroup(f, charge).isBaseFragmentFound())
//                                spmfReduced.add(mf);
//                        }
//                    }
//
//                // losses
//                if (spmfReduced.size() == 0)
//                    for (SpectraPeakMatchedFragment mf: spmf) {
//                        Fragment f = mf.getFragment();
//                        int charge = mf.getCharge();
//                        if (!f.isClass(DoubleFragmentation.class) && f.isClass(Loss.class)) {
//                            spmfReduced.add(mf);
//                        }
//                    }
//            }
//
//            if (spmfReduced.size() == 0) {
//                m_out.print(
//                        s.getRun() + "\t" + s.getScanNumber() + "\t" + match.getCrosslinker().getName() +
//                        "\t" + pep1.getSequence().getFastaHeader() +
//                        "\t" + pep1.toString() +
//                        "\t" + pep2.getSequence().getFastaHeader() +
//                        "\t" + pep2.toString() +
//                        "\t" +  "\t" +
//                        "\t" +
//                        "\t" +
//                        "\t" +
//                        "\t" + "\t" +
//                        "\t" + "\t" +
//                        "\t"  + peak.getMZ() +
//                        "\t" +
//                        "\t" +
//                        "\t\n");
//            }
//

            for (SpectraPeakMatchedFragment mf: spmfReduced) {
                Fragment f = mf.getFragment();
                count.add(f);
                Peptide p = f.getPeptide();
                if (Math.abs(mf.getMZ() - peak.getMZ()) > 1)
                    p = f.getPeptide();
                m_out.print(
                        s.getRun() + "\t" + s.getScanNumber() + "\t" + (match.getCrosslinker() == null? "" :  match.getCrosslinker().getName()) +
                        "\t" + pep1.getSequence().getFastaHeader() +
                        "\t" + pep1.toString() +
                        "\t" + (pep2 != null ? pep2.getSequence().getFastaHeader():"") +
                        "\t" + (pep2 != null ? pep2.toString():"") +
                        "\t" + p.toString() + "\t" + (p == pep1 ? "alpha" : "beta")  +
                        "\t" + (match.getLinkingSite(0)+1) +
                        "\t" + (match.getLinkingSite(1)+1) +
                        "\t" + f.name() +
                        "\t" + f + "\t" + f.getNeutralMass() +
                        "\t" + mf.getCharge() + "\t" + mf.getMZ() +
                        "\t" + mf.getFragment().getMZ(mf.getCharge()) +
                        "\t" + (mf.getFragment().getMZ(mf.getCharge()) - peak.getMZ()));
                if (f instanceof Loss) {
                    Loss l = (Loss)f;
                    MatchedBaseFragment mbf = mfc.getMatchedFragmentGroup(l, mf.getCharge());
                    //SpectraPeak base = l.getBasePeak(match.getSpectra(), mf.getCharge(), mf.getModification(), match.getFragmentTolerance());
                    //m_out.print("\t" + (base == null ? "\"No Base Peak\"" : "" + base.getMZ()));
                    m_out.print("\t" + (mbf.isBaseFragmentFound() ? mbf.getBasePeak().getMZ() : "\"No Base Peak\""));
                } else {
                    m_out.print("\t");
                }
                m_out.println();

            }
        }
        m_resultCount++;
        if (match.getMatchrank() == 1)
            m_topResultCount++;

        if (m_doFreeMatch)
            match.free();

//        for (Fragment f :  match.getPeptide(0)Fragments()) {
//           m_out.print(pep1.getSequence().getFastaHeader() + "\t");
//           m_out.print(pep1.toString() + "\t");
//           m_out.print(pep1.getMass() + "\t");
//           m_out.print(pep2.getSequence().getFastaHeader() + "\t");
//           m_out.print(pep2.toString() + "\t");
//           m_out.print(pep2.getMass() + "\t");
//           m_out.print("alpha\t");
//           if (f instanceof BIon) {
//               m_out.print("B-Ion\t");
//           } else if (f instanceof YIon) {
//               m_out.print("Y-Ion\t");
//           } else if (f instanceof AIon) {
//               m_out.print("A-Ion\t");
//           } else {
//               m_out.print(f.getClass().getSimpleName() + "\t");
//           }
//           m_out.print(f.name() + "\t");
//
//           m_out.print(f.toString() + "\t");
//           m_out.print(f.getMass() + "\t");
//           m_out.print(count.count(f) + "\t");
//           m_out.println();
//        }
//
//        for (Fragment f :  match.getPeptide2Fragments()) {
//           m_out.print(pep1.getSequence().getFastaHeader() + "\t");
//           m_out.print(pep1.toString() + "\t");
//           m_out.print(pep1.getMass() + "\t");
//           m_out.print(pep2.getSequence().getFastaHeader() + "\t");
//           m_out.print(pep2.toString() + "\t");
//           m_out.print(pep2.getMass() + "\t");
//           m_out.print("beta\t");
//           if (f instanceof BIon) {
//               m_out.print("B-Ion\t");
//           } else if (f instanceof YIon) {
//               m_out.print("Y-Ion\t");
//           } else if (f instanceof AIon) {
//               m_out.print("A-Ion\t");
//           } else {
//               m_out.print(f.getClass().getSimpleName() + "\t");
//           }
//           m_out.print(f.name() + "\t");
//           m_out.print(f.toString() + "\t");
//           m_out.print(f.getMass() + "\t");
//           m_out.print(count.count(f) + "\t");
//           m_out.println();
//        }
        }

    }


    public int getResultCount() {
        return m_resultCount;
    }

    public int getTopResultCount() {
        return m_topResultCount;
    }

    public void setFreeMatch(boolean doFree) {
        m_doFreeMatch = doFree;
    }

    public void flush() {
        m_out.flush();
    }


}
