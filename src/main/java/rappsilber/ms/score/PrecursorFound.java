/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.ms.score;

import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 * Boost scores for linear matches and non-covalent matches
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class PrecursorFound extends AbstractScoreSpectraMatch{
    
    private static final String epi = "Exp_Precursor_Intensity";
    private static final String epir ="Exp_Precursor_Intensity_Relative"; 
    private static final String mpi = "Matched_Precursor_Intensity";
    private static final String mpir = "Matched_Precursor_Intensity_Relative";

    public PrecursorFound() {
    }
    
    
    @Override
    public double score(MatchedXlinkedPeptide match) {
        Spectra s = match.getSpectrum();
        double expMZ =  s.getPrecurserMZExp();
        double matchMZ = s.getPrecurserMZ();
        double expCharge =  s.getPrecurserChargeExp();
        double matchCharge = s.getPrecurserMZ();
        SpectraPeak expPeak = s.getPeakAt(expMZ);
        SpectraPeak matchPeak = s.getPeakAt(matchMZ);
        double base = s.getMaxIntensity();
        double score;
        if (expPeak != null) {
            score = expPeak.getIntensity();
            addScore(match, epi, score);
            addScore(match, epir, score/base);
        } else {
            addScore(match, epi, 0);
            addScore(match, epir, 0);
        }
        
        if (matchPeak != null) {
            score = matchPeak.getIntensity();
            addScore(match, mpi, score);
            addScore(match, mpir, score/base);
        } else {
            score = 0;
            addScore(match, mpi, 0);
            addScore(match, mpir, 0);
        }
        
        return score;
        
    }

    @Override
    public double getOrder() {
        return 1;
    }
    
    
    @Override
    public String[] scoreNames() {

        return new String[]{epi, epir, mpi, mpir};
    }
    
}
