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
package rappsilber.ms.spectra.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.utils.SortedLinkedList;

/**
 * an algorithm to deconvolute overlapping isotope clusters
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class Averagin extends IsotopPattern{

    /**
    * Constants for averagine isotope calculation
    */
    public static final double[] AVERAGINE_A = { -0.02576, 0.049889, 0.029321,
                                                0.020406, 0.012126, 0.013333,
                                                0.006667, 0.003333, 0.001667};
    /** the a-constant used for all following peaks */
    public static final double AVERAGINE_A_LAST = AVERAGINE_A[AVERAGINE_A.length - 1];


    /**
    * Constants for averagine isotope calculation
    */
    public static final double[] AVERAGINE_B = { 136, 0, -2.93,
                                                -2.04, -12.0, -26.0,
                                                -39, -58,-87};
    /** the b-constant used for all following peaks */
    public static final double AVERAGINE_B_LAST = AVERAGINE_B[AVERAGINE_A.length - 1];

    /** by what mass does the mono-isotopic peak officially "disappear" */
    public static final double BORDER_MASS = -AVERAGINE_B[0]/AVERAGINE_A[0];
    
    /**
     * factor by which the calculated intensity to the measured intensity can differ
     * without starting a new cluster
     */
    private double m_AveraginBreakUp = 4;

//    private double m_maxIsotopDistance = 5.1;
//
//    /** after how many peaks to check for the start of a second cluster */
//    private int    m_checkClusterPeaks = 5;

//    private static Anotation m_this = new Averagin();
//
//
//    {
//        Anotation.RegisterAnotaion(this);
//    }

    /**
     * what would be the expected relative hight of an peak for a fragment of
     * the given mass if it is the n-th peak in an isotope cluster
     * @param mass
     * @param averagineOffset
     * @return
     */
    protected double relativeHight(double mass,int averagineOffset) {
        double ret;
        if (averagineOffset<AVERAGINE_A.length) {
            ret = mass*AVERAGINE_A[averagineOffset] + AVERAGINE_B[averagineOffset];
        } else {
            ret = mass*AVERAGINE_A_LAST + AVERAGINE_B_LAST;
        }
        return (ret < 0 ? 0 : ret);
    }

    /**
     * Returns the sum of absolute errors for all the peaks in the pattern
     * @param peaks
     * @param firstPeak
     * @param averagineOffset
     * @return
     */
    private double error(ArrayList<SpectraPeak> peaks, int firstPeakId, int averagineOffset) {
        // take the first peak as reference
        SpectraPeak firstPeak = peaks.get(firstPeakId);
        double mass = firstPeak.getMZ() * firstPeak.getCharge();
        double factor = firstPeak.getIntensity() /
                        relativeHight(mass, averagineOffset);

        double retError = 0;

        int lastPeak = Math.min(peaks.size(), firstPeakId + AVERAGINE_A.length - averagineOffset - 1);

        //calculate the missing peaks
        for (int p = 0; p < averagineOffset; p++){
            retError += relativeHight(mass,
                    averagineOffset) * factor;
        }
        double prevIntens = firstPeak.getIntensity();
        for (int p = firstPeakId + 1; p < lastPeak;p ++) {
            averagineOffset++;
            SpectraPeak peak = peaks.get(p);

            // stop when we encounter an non isotop peak or a monoisotop peak
            if ((!peak.hasAnnotation(SpectraPeakAnnotation.isotop)) &&
                    peak.hasAnnotation(SpectraPeakAnnotation.monoisotop))
                break;

            // peak is part of the cluster
            // calculate theoratical intensity
            double calcIntens = relativeHight(mass,
                    averagineOffset) * factor;

            // calculate the delta
            double expIntens = peak.getIntensity();
            double calcDelta = prevIntens - calcIntens;
            double expDelta = prevIntens - peak.getIntensity();

            retError +=  Math.abs(expDelta - calcDelta) + Math.abs(expIntens - calcIntens);
            prevIntens = peak.getIntensity();
        }

        return retError;

    }

    /**
     * Step through the pattern and look for a second isotope pattern hidden within that one
     * @param peaks
     * @param firstPeak
     * @param firstAveragineOffset
     * @return
     */
    public int AnnotateOverlayingPattern(SpectraPeak[] peaks, int firstPeak, int firstAveragineOffset) {
        //TODO this
        // check whether we can reduce the error for the first isotop pattern by assuming,
        // that another isotop pattern is overlayed on it
        // walk through the pattern
        
        throw new UnsupportedOperationException("Overlaying patterns get recognised later");

        
        // take the difference between meassured and calculated intensity as peak for the next pattern

    }

    @Override
    public void AnnotateIsotops (Spectra spectra, int MaxCharge) {
        // first annotate the isotop-clusters
        findIsostopClusters(spectra, MaxCharge);
        deConvoluteIsotops(spectra, MaxCharge);

    }

    /**
     * finds the peaks, that can belong to an isotope cluster, and annotates
     * these as such
     * @param spectra
     * @param MaxCharge
     */
    public void findIsostopClusters(Spectra spectra, int MaxCharge) {
        super.AnnotateIsotops(spectra, MaxCharge);
    }

    /**
     * tries to detect, where one isotope cluster ends and a new starts and
     * splits the cluster at that place into two cluster.
     * @param spectra
     * @param MaxCharge
     */
    public void deConvoluteIsotops(Spectra spectra, int MaxCharge) {
        // take a look at the annotated isotop peaks
        Collection<SpectraPeakCluster> IsoptopClusters = spectra.getIsotopeClusters();
        SortedLinkedList<SpectraPeakCluster> newIsoptopClusters = new SortedLinkedList<SpectraPeakCluster>();


        for (SpectraPeakCluster cluster : IsoptopClusters) {
            // small clusters are not checked
            // and also if we previously extended this cluster to lower masses 
            // this check will not make sense
            if (cluster.size() < 5 || cluster.isExtended())
                continue;
            

            Iterator<SpectraPeak> peaks = cluster.iterator();

            // check for overlaying clusters of same charge state
            SpectraPeak firstPeak = peaks.next();

            double mass = firstPeak.getMZ() * firstPeak.getCharge();

            int peakID = 0;

            // get the factor for isotop-clusters
            double factor = firstPeak.getIntensity() /
                            relativeHight(mass, peakID++);
            SpectraPeakCluster newCluster = null;
            // go through all peaks and check whether it might start a new cluster
            while (peaks.hasNext()) {
                SpectraPeak p = peaks.next();
                double calcIntes = relativeHight(mass, peakID++) * factor;
                double expIntens = p.getIntensity();

                if (calcIntes/getAveraginBreakUp() > expIntens || expIntens > calcIntes * getAveraginBreakUp()) {
                    // ok we assume here starts a new Isotop-Cluster
                    newCluster = new SpectraPeakCluster(spectra.getTolearance());
                    newCluster.setMonoIsotopic(p);
                    p.annotate(SpectraPeakAnnotation.monoisotop);
                    newCluster.setCharge(cluster.getCharge());
                    newCluster.setMZ(p.getMZ());
                    peakID = 0;
                    factor = p.getIntensity() /
                            relativeHight(mass, peakID++);
                    newIsoptopClusters.add(newCluster);
                }

                if (newCluster != null) { // seemingly we found a new cluster
                    newCluster.add(p); // and this peak belongs to it
                    while (peaks.hasNext()) {
                        p = peaks.next();
                        newCluster.add(p);
                    }
                    //peaks.remove();    // this one does no longer belong to the original cluster
                    //cluster.remove(p);
                }
            }
        }

        // add all newly found clusters
        IsoptopClusters.addAll(newIsoptopClusters);
    }

    /**
     * @return the factor that determines whether a peak is considered part of a cluster or a start of a new cluster
     */
    public double getAveraginBreakUp() {
        return m_AveraginBreakUp;
    }

    /**
     * @param AveraginBreakUp the factor that determines whether a peak is considered part of a cluster or a start of a new cluster
     */
    public void setAveraginBreakUp(double AveraginBreakUp) {
        this.m_AveraginBreakUp = AveraginBreakUp;
    }

}
