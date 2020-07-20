/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.ms.score;

import rappsilber.ms.crosslinker.NonCovalentBound;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 * Boost scores for linear matches and non-covalent matches
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class BoostLNAPS extends AbstractScoreSpectraMatch{

    public static String DEFAULT_BASESCORE = NormalizerML.NAME;
    public static boolean DEFAULT_OVERWRITE = false;
    public static double DEFAULT_FACTOR = 1.15d;
    
    private String basescore = DEFAULT_BASESCORE;
    private boolean overwrite = DEFAULT_OVERWRITE;
    private double factor = DEFAULT_FACTOR;

    public BoostLNAPS(String basescore, Double factor, Boolean overwrite) {
        if (basescore != null)
            this.basescore = basescore;
        if (factor != null)
            this.factor = factor;
        if (overwrite != null)
            this.overwrite = overwrite;
    }
    
    
    @Override
    public double score(MatchedXlinkedPeptide match) {
        Double boost = match.getScore("BoostLNAPS");
        Double base = match.getScore(basescore);
        if (boost == null || Double.isNaN(boost))
            boost = 1d;
        if (match.getCrosslinker() instanceof NonCovalentBound)
            boost *= factor;
        if (match.getPeptide2() == null)
            boost *= factor;
        
        match.setScore("BoostLNAPS", boost);
        if (overwrite && boost != 1.0) {
            base *= boost;
            match.setScore(basescore, base);
        }
        return boost;
    }

    @Override
    public double getOrder() {
        return NormalizerML.m_order + 1;
    }
    
}
