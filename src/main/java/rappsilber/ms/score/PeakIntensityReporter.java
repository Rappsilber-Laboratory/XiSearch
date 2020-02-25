/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.ms.score;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import rappsilber.config.RunConfig;
import static rappsilber.ms.score.FragmentCoverage.m;
import static rappsilber.ms.score.FragmentCoverage.mAll;
import static rappsilber.ms.score.FragmentCoverage.mAllLossy;
import static rappsilber.ms.score.FragmentCoverage.whole;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 * 
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class PeakIntensityReporter extends AbstractScoreSpectraMatch{
    
    private HashSet<Double> m_peaks = new HashSet<>();
    private HashMap<Double,String> m_peaksToNames = new HashMap<>();
    String[] names;
    String m_prefix = "peak_";
    NumberFormat m_format = new DecimalFormat("#.0000");
    
    

    public PeakIntensityReporter(Collection<Double> peaks) {
        this.m_peaks.addAll(peaks);
        initCalculations(this.scoreNames());
    }
    
    public PeakIntensityReporter(Collection<Double> peaks, String prefix) {
        this(peaks);
        this.m_prefix = prefix;
    }

    @Override
    public double score(MatchedXlinkedPeptide match) {
        for (Double d : m_peaks) {
            Spectra s = match.getSpectrum();
            SpectraPeak sp = s.getPeakAt(d);
            if (sp == null)
                match.setScore(m_peaksToNames.get(d), 0);
            else {
                match.setScore(m_peaksToNames.get(d), sp.getIntensity());
            }
        }
        return 0;
    }

    @Override
    public double getOrder() {
        return 0;
    }
    
    
    @Override
    public String[] scoreNames() {
        if (m_peaks == null)
            return new String[0];
        if (names != null && names.length == m_peaks.size())
            return names;
        String[] names = new String[m_peaks.size()];
        int i=0;
        m_peaksToNames = new HashMap<>();
        for (Double d : m_peaks) {
            String n =  m_prefix + m_format.format(d);
            names[i++] = n;
            m_peaksToNames.put(d, n);
        }
        this.names=names;
        return names;
    }

    /**
     * @return the m_peaks
     */
    public HashSet<Double> getPeaks() {
        return m_peaks;
    }

    /**
     * @param m_peaks the m_peaks to set
     */
    public void setPeaks(HashSet<Double> m_peaks) {
        this.m_peaks = m_peaks;
    }
    
    
}
