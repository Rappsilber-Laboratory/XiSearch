/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.utils;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class AvergineUtil {
    
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
    public static double relativeHight(double mass,int averagineOffset) {
        double ret;
        if (averagineOffset<AVERAGINE_A.length) {
            ret = mass*AVERAGINE_A[averagineOffset] + AVERAGINE_B[averagineOffset];
        } else {
            ret = mass*AVERAGINE_A_LAST + AVERAGINE_B_LAST;
        }
        return (ret < 0 ? 0 : ret);
    }    
}
