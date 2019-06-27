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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.*;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.annotation.SpectraPeakAnnotation;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.utils.CountOccurence;
import rappsilber.utils.Util;

/**
 * A ouput module for writing the results as tabular text to a stream (like system.out)
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class PeakListWriter extends AbstractResultWriter{
    /** the stream used to write something */
    PrintStream m_out;
    private int m_resultCount = 0;
    private int m_topResultCount = 0;
    private boolean m_doFreeMatch = false;
    private NumberFormat numberFormat;
    private Locale locale=Locale.ENGLISH;

    /** 
     * create a new class and connect it to the given output stream
     * @param out where to output the data
     */
    public PeakListWriter(OutputStream out) {
        m_out = new PrintStream(out);
        // we need annotations
        BufferedResultWriter.m_ForceNoClearAnnotationsOnBuffer=true;
        Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        finished();
                    }catch (Exception e) {}
                }
            });
    }

    public void writeHeader() {
                m_out.println("Run\tScanNumber\tcrosslinker\tFastaHeader1\tPeptide1\tFastaHeader2\t" +
                        "Peptide2\tMatchedPeptide\talpha/beta\tLinkerPosition1\tLinkerPostion2\t" +
                        "FragmentName\tFragment\tNeutralMass\tCharge\tCalcMZ\tExpMZ\tMS2Error\tIsotopPeak\tDescription\tvirtual\tBasePeak\tAbsoluteIntesity\tRelativeIntensity\tIsPrimaryMatch");
    }

    public String peakToAscii(double relative) {

        return Util.repeatString("-",(int) Math.round(relative*49) + 1);

    }

    public boolean setLocale(String locale) {
        Locale l = Util.getLocale(locale);
        if (l == null) 
            return false;
        setLocale(l);
        return true;
    }
    
    public void setLocale(Locale locale) {
        this.locale = locale;
        numberFormat = NumberFormat.getNumberInstance(locale);
        DecimalFormat fformat  = (DecimalFormat) numberFormat;
        DecimalFormatSymbols symbols=fformat.getDecimalFormatSymbols();
        fformat.setMaximumFractionDigits(6);
    }
    
    private String d2s(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d))
            return Double.toString(d);
        return numberFormat.format(d);
    }
    
    private String i2s(int i) {
        return numberFormat.format(i);
    }
    
    public void writeResult(MatchedXlinkedPeptide match) {
        Spectra s = match.getSpectrum();
        Peptide[] peps = match.getPeptides();
        Peptide pep1 = peps[0];
        Peptide pep2 = (peps.length>1? peps[1] : null);
        CountOccurence<Fragment> count = new CountOccurence<Fragment>();
        MatchedFragmentCollection mfc = match.getMatchedFragments();
        double maxIntens = s.getMaxIntensity();


        for (SpectraPeak peak :  match.getSpectrum().getPeaks()) {
//            ArrayList<SpectraPeakMatchedFragment> spmf = peak.getMatchedAnnotation();
            double relative = peak.getIntensity()/s.getMaxIntensity();

//            if ((int) peak.getMZ() == 330)
//                System.out.println("found");
//            ArrayList<SpectraPeakMatchedFragment> spmfReduced;
            // How to check if a peak has matches - write this 'header' for each match, for no match, write header only
            if (peak.getMatchedAnnotation().isEmpty()) {
                try {
                m_out.print(
                        s.getRun() + "\t" + i2s(s.getScanNumber()) + 
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
                        "\t"  + d2s(peak.getMZ()) +
                        "\t" + 
                        "\t" + (peak.hasAnnotation(SpectraPeakAnnotation.monoisotop)? "monoisotopic" :(peak.hasAnnotation(SpectraPeakAnnotation.isotop)? "isotope": "")) +
                        "\t" +
                        "\t\t\t" + d2s(peak.getIntensity()) +"\t" + d2s(relative) + "\t");
                m_out.println("\t" + peakToAscii(relative));
                } catch (Exception e) {
                    throw new Error(e);
                }
            } else {
//               spmfReduced = AnnotationUtil.getReducedAnnotation(peak, mfc) ;

                boolean first=true;
                for (SpectraPeakMatchedFragment mf: peak.getMatchedAnnotation()) {
                    Fragment f = mf.getFragment();
                    count.add(f);
                    Peptide p = f.getPeptide();
                    if (Math.abs(mf.getMZ() - peak.getMZ()) > 1)
                        p = f.getPeptide();
                    m_out.print(
                            s.getRun() + "\t" + i2s(s.getScanNumber()) +
                            "\t" + (pep2!=null ? match.getCrosslinker().getName() : "") +
                            "\t" + pep1.getSequence().getFastaHeader() +
                            "\t" + pep1.toString() +
                            "\t" + (pep2!=null ? pep2.getSequence().getFastaHeader() :"") +
                            "\t" + (pep2!=null ? pep2.toString():"") +
                            "\t" + p.toString() + "\t" + (p == pep1 ? "alpha" : "beta")  +
                            "\t" + (pep2!=null ? i2s(match.getLinkingSite(0)+1) :"") +
                            "\t" + (pep2!=null ? i2s(match.getLinkingSite(1)+1) : "") +
                            "\t" + f.name() +
                            "\t" + f + "\t" + d2s(f.getNeutralMass()) +
                            "\t" + mf.getCharge() + "\t" + d2s(mf.getMZ()) +
                            "\t" + d2s(peak.getMZ())  +
                            "\t" + d2s(peak.getMZ() - mf.getMZ()) +
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
                    m_out.print("\t" + d2s(peak.getIntensity()) +"\t" + d2s(peak.getIntensity()/maxIntens));
                    m_out.print("\t" + (mf.isPrimary()?1:0) );
                    if (first) {
                        m_out.println("\t" + peakToAscii(relative));
                        first = false;
                    } else {
                        m_out.println("\t ^");
                    }

                }
            }
        }
        m_out.println();
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


    public int getResultCount() {
        return m_resultCount;
    }

    public int getTopResultCount() {
        return m_topResultCount;
    }
    
    public void flush() {
        m_out.flush();
    }

    
    @Override
    public void finished() {
        flush();
        m_out.close();
    }    
}
