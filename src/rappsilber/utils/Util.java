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
package rappsilber.utils;

import java.sql.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.regex.*;

import rappsilber.ms.ToleranceUnit;

/**
 * Provides some general static functions needed/ helpful for the rest of the
 * library/program
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class Util {

    /**
     * the maximal ratio between two consecutive peeks to be considered part of
     * the same isotope cluster
     */
    public static double IsotopClusterMaxYRation = 5.5;

    /**
     * maximal how many peaks are considered to belong to one isotope cluster
     */
    public static int IsotopClusterMaxPeaks = 7;

    /**
     * maximal number of variable modifications per peptide
     */
    public static int MaxModificationPerPeptide = 3;
    /**
     * maximal number of peptides that can be generated via modifications from a
     * single peptide
     */
    public static int MaxModifiedPeptidesPerPeptide = 20;

    /**
     * formats to 6 decimal places
     */
    public static DecimalFormat sixDigits = new DecimalFormat("#,##0.000000");
    /**
     * formats to 5 decimal places
     */
    public static DecimalFormat fiveDigits = new DecimalFormat("#,##0.00000");
    /**
     * formats to 4 decimal places
     */
    public static DecimalFormat fourDigits = new DecimalFormat("#,##0.0000");
    /**
     * formats to 3 decimal places
     */
    public static DecimalFormat threeDigits = new DecimalFormat("#,##0.000");
    /**
     * formats to 2 decimal places
     */
    public static DecimalFormat twoDigits = new DecimalFormat("#,##0.00");
    /**
     * formats to 1 decimal place
     */
    public static DecimalFormat oneDigit = new DecimalFormat("#,##0.0");

    // Used for setting the scale of our calculations  involving bigdecimals
    //public static final MathContext mc = new MathContext(6);
    // Masses of various molecules and such like used by our program
    /**
     * mass of a single proton (H+)
     */
    //public static final double PROTON_MASS = 1.007825017; // Proton mass = 1.007825017 Da
    public static final double PROTON_MASS = 1.00727646677; // according to wikipedia
    /**
     * what is a difference between C12 and C13
     */
    public static final double C13_MASS_DIFFERENCE = 1.00335;

    /**
     * mass of H2O
     */
    public static final double WATER_MASS = 18.01056027; // Mass of water = 18.01056027 Da
    /**
     * mass of NH3
     */
    public static final double AMMONIA_MASS = 17.02654493; // Mass of ammonia = 17.02654493 Da
    /**
     * mass of O
     */
    public static final double OXYGEN_MASS = 15.99491; // mass of fixed mod
    /**
     * mass of C
     */
    public static final double CARBON_MASS = 12; // mass of fixed mod

    public static final double HYDROXO_BS2GD0 = 114.0316808; // Mass of var mod
    public static final double HYDROXO_BS2GD4 = 118.0563805; // Mass of var mod
    public static final double OXIDATION_M = 15.99491024; // Mass of Var Mod
    public static final double NITROGEN_MASS = 14.003074; // Mass of Var Mod
    public static final double HYDROGEN_MASS = 1.007825035; // Mass of Var Mod


    // Cross-linker masses for pairs
    public static final double ONE_PPM = 0.000001;


    public static String[] mannScore(int matched_ions, int total_ions, ToleranceUnit msms_tol,
            int totalPeaks, double[] min_max) {
        // need for cumulative score
        String score_and_p[] = new String[2];
        /* Parameter list:
		 * 1. 'matched_ions' = the number of ions that match within the MS2 spectrum
		 * 2. 'total_ions' = the total number of ions that fall within the range of
		 *    the masses in the MS2 spectrum
		 * 3. 'msms_tol' = the tolerance unit, used to caluculate the 'p' - the
		 *    probability of a random match.
         */
        int n = total_ions;
        //double double_n = total_ions;
        int k = matched_ions;
        //double double_k = matched_ions;
        // System.out.println("Total peaks = " + totalPeaks);

        // double p = 0.04");
// 		System.out.println("-------");
// 		System.out.println("hits = " + k);
// 		System.out.println("in range = " + n);
// 		System.out.println("total peaks = " + totalPeaks);
// 		System.out.println("min = " + min_max[0]);
// 		System.out.println("max = " + min_max[1]);
// 		System.out.println("-------");
        // Using tolerance window
        double p;
        if (msms_tol.getUnit() == "ppm") {
            // parts per million
            p = (double) (Util.ONE_PPM * msms_tol.getValue());
        } else {
            // we have a dalton unit
            p = (double) msms_tol.getValue();
        }
        p = p * 2;
        // old
        // p = p.multiply( new double(4) );
        p = p * totalPeaks;
        // p = p.divide( new double(100), Util.mc);
        p = p / (min_max[1] - min_max[0]);

        // need for Cumulative score
        score_and_p[1] = String.valueOf(p);
// 		System.out.println("small p value = " + score_and_p[1]);
        // end use tolerance window

        double score = 0; // initialize the score
        //score = score.setScale(6, double.ROUND_HALF_UP);
        if (k == 0) {
            // if there are no matching ions the score is 0
            // return "";
            // need for cumulative score
            score_and_p[0] = "";
        } // end if

        // .setScale(4, )
        /* Probability
		 * -----------
         */
        // Part 1: (n k) permutations
        try {

            // recursive component for probs
            double permutations = factorial(n);
            double divisor = factorial(k);
            divisor = divisor * factorial(n - k);
            permutations = permutations / divisor;

            // Part 2: p^k
            double part2 = (double) Math.pow(p, k);

            // Part 3: (1-p)^(n-k)
            double part3 = 1;
            part3 = part3 - p;
            part3 = (double) Math.pow(part3, n - k);

            double probability = (permutations * part2 * part3);
            //  end recursive component

            if (probability <= 0) {
                // we do not allow 0 or neagtive probability
                // return "0";
                score_and_p[0] = "0";
            } else {
                // log for score
                double log_probability = probability;
                score = (double) (-10d * Math.log10(log_probability));
                //score = Math.round(score*100.0d) / 100.0d;
                // System.out.println( "SCORE = "  + score.toString() );
                // return score.toString();
                score_and_p[0] = String.valueOf(score);
            }
        } catch (IllegalArgumentException ie) {
            String error = "n = " + n + " k = " + k;
            System.err.println("Problem with factorial calculation: " + error);
            // return error;
            score_and_p[0] = error;
        }

        //return "";
        return score_and_p;
    }// end method mannScore()

    private static final double[] factorials = {
        1.0 /* 0 */,
        1.0 /* 1 */,
        2.0 /* 2 */,
        6.0 /* 3 */,
        24.0 /* 4 */,
        120.0 /* 5 */,
        720.0 /* 6 */,
        5040.0 /* 7 */,
        40320.0 /* 8 */,
        362880.0 /* 9 */,
        3628800.0 /* 10 */,
        39916800.0 /* 11 */,
        479001600.0 /* 12 */,
        6227020800.0 /* 13 */,
        87178291200.0 /* 14 */,
        1307674368000.0 /* 15 */,
        20922789888000.0 /* 16 */,
        355687428096000.0 /* 17 */,
        6402373705728000.0 /* 18 */,
        121645100408832000.0 /* 19 */,
        2432902008176640000.0 /* 20 */,
        51090942171709440000.0 /* 21 */,
        1124000727777607680000.0 /* 22 */,
        25852016738884976640000.0 /* 23 */,
        620448401733239439360000.0 /* 24 */,
        15511210043330985984000000.0 /* 25 */,
        403291461126605635584000000.0 /* 26 */,
        10888869450418352160768000000.0 /* 27 */,
        304888344611713860501504000000.0 /* 28 */,
        8841761993739701954543616000000.0 /* 29 */,
        265252859812191058636308480000000.0 /* 30 */,
        8222838654177922817725562880000000.0 /* 31 */,
        263130836933693530167218012160000000.0 /* 32 */,
        8683317618811886495518194401280000000.0 /* 33 */,
        295232799039604140847618609643520000000.0 /* 34 */,
        10333147966386144929666651337523200000000.0 /* 35 */,
        371993326789901217467999448150835200000000.0 /* 36 */,
        13763753091226345046315979581580902400000000.0 /* 37 */,
        523022617466601111760007224100074291200000000.0 /* 38 */,
        20397882081197443358640281739902897356800000000.0 /* 39 */,
        815915283247897734345611269596115894272000000000.0 /* 40 */}; // end array declaration

    private static final int m_lastFactorial_id = factorials.length - 1;

    public static double factorial(int n) {
        // For fast factorial calculation we assume
        // most factorial calculations we carry out are in the range 0 <= n <= 20
        // so we just do a very fast lookup in memory.
        // If the case arises where n > 20 we do the calculation
        if (n < 0) {
            throw new IllegalArgumentException("factorial can only handle n >= 0");
        }

        if (n > m_lastFactorial_id) {
            double result = factorials[m_lastFactorial_id]; // start with the last precalculated one
            for (int i = factorials.length; i <= n; i++) {
                result = result * i; // and calculate the factorial
            }
            return result;
        } else {
            // do a simple lookup
            return factorials[n]; // take the shortcut :)
        }
    }// end method factorial

    /**
     * @param filePath name of file to open. The file can reside anywhere in the
     * classpath
     */
    public static BufferedReader readFromClassPath(String filePath) throws java.io.IOException {
        String path = filePath;
        System.err.println("!!!reading config!!!");
        System.err.println("from : " + filePath);
        StringBuffer fileData = new StringBuffer(1000);
        URL res = Object.class.getResource(path);

        while (res == null && path.contains(".")) {

            path = path.replaceFirst("\\.", Matcher.quoteReplacement(File.separator));
            res = Object.class.getResource(path);
        }

        if (res == null) {
            path = filePath;
            while (res == null && path.contains(".")) {

                path = path.replaceFirst("\\.", "/");
                res = Object.class.getResource(path);
            }
        }

        InputStream is = Object.class.getResourceAsStream(path);
        return new BufferedReader(new InputStreamReader(is));
    }

    public static String repeatString(String s, int repeat) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < repeat; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    public static void forceGC() {
        for (int i = 1; i > 0; i--) {
            System.gc();
        }
    }

    public static String doubleToString(double value) {
//        DecimalFormat oneDigit = new DecimalFormat("0.0");
        String fUnit = "B";
        if (value > 1024 * 1024 * 900) {
            value /= 1024 * 1024 * 1024;
            fUnit = "GB";
        } else if (value > 1024 * 900) {
            value /= 1024 * 1024;
            fUnit = "MB";
        } else if (value > 900) {
            value /= 1024;
            fUnit = "KB";
        }
        return "" + oneDigit.format(value) + fUnit;

    }

    public static void joinAllThread(Thread[] gatherthread) {
        boolean running = true;
        while (running) {
            running = false;
            for (int i = 0; i < gatherthread.length; i++) {
                try {
                    gatherthread[i].join(1000);
                } catch (InterruptedException ex1) {
                }
                running |= gatherthread[i].isAlive();
            }
        }
    }

    /**
     * creates a string that contains some info about the current memory situation
     * @return String representation of used and free memory
     */
    public static String memoryToString() {
        Runtime runtime = Runtime.getRuntime();

        double fm = runtime.freeMemory();
        String fmu = "B";
        double mm = runtime.maxMemory();
        double tm = runtime.totalMemory();
        double um = tm-fm;
        return "Used: " + StringUtils.toHuman(um) + " of " + StringUtils.toHuman(mm) + "  (Free:" + StringUtils.toHuman(fm) + " Total:" + StringUtils.toHuman(tm) + " Max:"+ StringUtils.toHuman(mm) +")";
    }
    
}// end class Util
