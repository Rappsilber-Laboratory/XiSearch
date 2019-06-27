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
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.*;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.annotation.AnnotationUtil;
import rappsilber.ms.spectra.annotation.SpectraPeakAnnotation;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.utils.CountOccurence;
import rappsilber.utils.SortedLinkedList2;
import rappsilber.utils.Util;

/**
 * A ouput module for writing the results as tabular text to a stream (like system.out)
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DeIsotopedPeakListWriter extends AbstractResultWriter{
    /** the stream used to write something */
    PrintStream m_out;
    private int m_resultCount = 0;
    private int m_topResultCount = 0;

    public void flush() {
        m_out.flush();
    }




    private class mzToDescription implements Comparable<mzToDescription> {
        String Run;
        String ScanNumber;
        String crosslinker;
        Peptide Peptide1;
        Peptide Peptide2;
        Fragment matchedFragment;
        int     Charge;
        SpectraPeak peak;
        boolean virtual;
        SpectraPeak BasePeak;
        SpectraPeak RealtiveIntensity;

        public mzToDescription(String Run, String ScanNumber, CrossLinker crosslinker, Peptide Peptide1, Peptide Peptide2, Fragment matchedFragment, int Charge, SpectraPeak peak, boolean virtual, SpectraPeak BasePeak, SpectraPeak RealtiveIntensity) {
            this.Run = Run;
            this.ScanNumber = ScanNumber;
            if (crosslinker == null)
                this.crosslinker = "";
            else
                this.crosslinker = crosslinker.getName();

            this.Peptide1 = Peptide1;
            this.Peptide2 = Peptide2;
            this.matchedFragment = matchedFragment;
            this.Charge = Charge;
            this.peak = peak;
            this.virtual = virtual;
            this.BasePeak = BasePeak;
            this.RealtiveIntensity = RealtiveIntensity;
        }


        public String toString() {
            StringBuffer ret = new StringBuffer();

            return "";
        }

        @Override
        public int compareTo(mzToDescription o) {
            return Double.compare(this.peak.getMZ(), o.peak.getMZ());
        }



    }

    /** 
     * create a new class and connect it to the given output stream
     * @param out where to output the data
     */
    public DeIsotopedPeakListWriter(OutputStream out) {
        m_out = new PrintStream(out);

    }

    public void writeHeader() {
                m_out.println("Run\tScanNumber\tcrosslinker\tFastaHeader1\tPeptide1\tFastaHeader2\t" +
                        "Peptide2\tMatchedPeptide\talpha/beta\tLinkerPosition1\tLinkerPostion2\t" +
                        "FragmentName\tFragment\tNeutralMass\tCharge\tCalcMZ\tExpMZ\tMS2Error\tIsotopPeak\tDescription\tvirtual\tBasePeak\tAbsoluteIntesity\tRelativeIntensity");
    }

    public String peakToAscii(double relative) {

        return Util.repeatString("-",(int) Math.round(relative*49) + 1);

    }

    public void writeResult(MatchedXlinkedPeptide match) {
        Spectra s = match.getSpectrum();
        Peptide pep1 = match.getPeptides()[0];
        Peptide pep2 = match.getPeptides()[1];
        CountOccurence<Fragment> count = new CountOccurence<Fragment>();
        MatchedFragmentCollection mfc = match.getMatchedFragments();
        double maxIntens = s.getMaxIntensity();
        SortedLinkedList2<mzToDescription> matchedFragments;

        for (SpectraPeak peak :  match.getSpectrum().getPeaks()) {
            ArrayList<SpectraPeakMatchedFragment> spmf = peak.getMatchedAnnotation();
            double relative = peak.getIntensity()/s.getMaxIntensity();

//            if ((int) peak.getMZ() == 330)
//                System.out.println("found");

            ArrayList<SpectraPeakMatchedFragment> spmfReduced = AnnotationUtil.getReducedAnnotation(peak, mfc);

            if (spmfReduced.size() == 0) {
                try {
                m_out.print(
                        s.getRun() + "\t" + s.getScanNumber() + 
                        "\t" + (pep2!=null ? match.getCrosslinker().getName() : "") +
                        "\t" + pep1.getSequence().getFastaHeader() +
                        "\t" + pep1.toString() +
                        "\t" + (pep2!=null ? pep2.getSequence().getFastaHeader() : "") +
                        "\t" + (pep2!=null ? pep2.toString() :"")+
                        "\t" +  "\t" + 
                        "\t" + 
                        "\t" + 
                        "\t" + 
                        "\t" + "\t" + 
                        "\t" + "\t" +
                        "\t"  + peak.getMZ() +
                        "\t" + 
                        "\t" + (peak.hasAnnotation(SpectraPeakAnnotation.monoisotop)? "monoisotpoic" :(peak.hasAnnotation(SpectraPeakAnnotation.isotop)? "isotope": "")) +
                        "\t" +
                        "\t\t\t" + peak.getIntensity() +"\t" + relative );
                m_out.println("\t" + peakToAscii(relative));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }

            boolean first=true;
            for (SpectraPeakMatchedFragment mf: spmfReduced) {
                Fragment f = mf.getFragment();
                count.add(f);
                Peptide p = f.getPeptide();
                if (Math.abs(mf.getMZ() - peak.getMZ()) > 1)
                    p = f.getPeptide();
                m_out.print(
                        s.getRun() + "\t" + s.getScanNumber() +
                        "\t" + (pep2!=null ? match.getCrosslinker().getName() : "") +
                        "\t" + pep1.getSequence().getFastaHeader() +
                        "\t" + pep1.toString() +
                        "\t" + (pep2!=null ? pep2.getSequence().getFastaHeader() :"") +
                        "\t" + (pep2!=null ? pep2.toString():"") +
                        "\t" + p.toString() + "\t" + (p == pep1 ? "alpha" : "beta")  +
                        "\t" + (pep2!=null ? match.getLinkingSite(0)+1 :"") +
                        "\t" + (pep2!=null ? match.getLinkingSite(1)+1 : "") +
                        "\t" + f.name() +
                        "\t" + f + "\t" + f.getNeutralMass() +
                        "\t" + mf.getCharge() + "\t" + mf.getMZ() +
                        "\t" + peak.getMZ()  +
                        "\t" + (peak.getMZ() - mf.getMZ()) +
                        "\t" + (peak.hasAnnotation(SpectraPeakAnnotation.monoisotop)? "monoisotpoic" :"")  +
                        "\t\"" + (mf.isLinear() ? "linear" : "crosslinked") + (mf.matchedMissing() ? " missing mono": "") + "\"" +
                        "\t" + (peak.hasAnnotation(SpectraPeakAnnotation.virtual) ? "virtual": "no" ));
                if (f instanceof Loss) {
                    Loss l = (Loss)f;
                    MatchedBaseFragment mbf = mfc.getMatchedFragmentGroup(l, mf.getCharge());
                    //SpectraPeak base = l.getBasePeak(match.getSpectra(), mf.getCharge(), mf.getModification(), match.getFragmentTolerance());
                    //m_out.print("\t" + (base == null ? "\"No Base Peak\"" : "" + base.getMZ()));
                    m_out.print("\t" + (mbf.isBaseFragmentFound() ? mbf.getBasePeak().getMZ() : "\"No Base Peak\""));
                } else {
                    m_out.print("\t");
                }
                m_out.print("\t" + peak.getIntensity() +"\t" + peak.getIntensity()/maxIntens);
                if (first) {
                    m_out.println("\t" + peakToAscii(relative));
                    first = false;
                } else {
                    m_out.println("\t ^");
                }

            }
        }
        m_resultCount++;
        if (match.getMatchrank() == 1)
            m_topResultCount++;

        if (m_doFreeMatch)
            match.free();



    }



    public int getResultCount() {
        return m_resultCount;
    }

    public int getTopResultCount() {
        return m_topResultCount;
    }



}
