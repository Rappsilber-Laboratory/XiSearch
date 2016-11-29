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

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class StringUtils {
    
    
    /** formats to 6 decimal places*/
    public static DecimalFormat sixDigits = new DecimalFormat("#,##0.000000");
    /** formats to 5 decimal places*/
    public static DecimalFormat fiveDigits = new DecimalFormat("#,##0.00000");
    /** formats to 4 decimal places*/
    public static DecimalFormat fourDigits = new DecimalFormat("#,##0.0000");
    /** formats to 3 decimal places*/
    public static DecimalFormat threeDigits = new DecimalFormat("#,##0.000");
    /** formats to 2 decimal places*/
    public static DecimalFormat twoDigits = new DecimalFormat("#,##0.00");
    /** formats to 1 decimal place*/
    public static DecimalFormat oneDigit = new DecimalFormat("#,##0.0");    
    
    public static int LevenshteinDistance(String s, String t) {
        int len_s = s.length();
        int len_t = t.length();
        int last_s = s.length()-1;
        int last_t = t.length()-1;
        int cost = 0;

        /* test for degenerate cases of empty strings */
        if (len_s == 0) return len_t;
        if (len_t == 0) return len_s;

        /* test if last characters of the strings match */
        if (s.charAt(len_s - 1) == t.charAt(len_t - 1))
            cost = 0;
        else                          
            cost = 1;
        
        

        /* return minimum of delete char from s, delete char from t, and delete char from both */
        return Math.min(LevenshteinDistance(s.substring(0, last_s), t) + 1,Math.min(
                       LevenshteinDistance(s, t.substring(0, last_t)) + 1,
                       LevenshteinDistance(s.substring(0,last_s), t.substring(0,last_t)) + cost));
    }    
    

    public static int editCost(String s, String t, int gapCost, int replaceCost) {
        int len_s = s.length();
        int len_t = t.length();
        int last_s = s.length()-1;
        int last_t = t.length()-1;
        char[] sc = s.toCharArray();
        char[] tc = t.toCharArray();
        
        int[][] ca = new int[sc.length][tc.length];
        ca[0][0] = sc[0] == tc[0] ? replaceCost: 0;
        for (int i = 1; i < sc.length; i++) {
            ca[i][0] = ca[i-1][0] + gapCost;
        }
        for (int i = 1; i < tc.length; i++) {
            ca[0][i] = ca[0][i-1] + gapCost;
        }
        
        for (int si = 1; si< sc.length; si ++) {
            for (int ti =1; ti<tc.length; ti++) {
                int cost = sc[si] == tc[ti] ? 0 : replaceCost;
                ca[si][ti] = Math.min(ca[si-1][ti-1] + cost, Math.min(ca[si][ti-1]+gapCost, ca[si-1][ti] + gapCost));
            }
        }
        
        return ca[sc.length-1][tc.length-1];

    }    
    
    public static double editCost(String s, String t, double gapCost, double replaceCost, HashMap<Character,HashMap<Character,Double>> specialCosts, HashSet<Character> space) {
        int len_s = s.length();
        int len_t = t.length();
        int last_s = s.length()-1;
        int last_t = t.length()-1;
        char[] sc = s.toCharArray();
        char[] tc = t.toCharArray();
        
        double[][] ca = new double[sc.length][tc.length];
        ca[0][0] = sc[0] == tc[0] ? replaceCost: 0;
        for (int i = 1; i < sc.length; i++) {
            ca[i][0] = ca[i-1][0] + gapCost;
        }
        for (int i = 1; i < tc.length; i++) {
            ca[0][i] = ca[0][i-1] + gapCost;
        }
        
        for (int si = 1; si< sc.length; si ++) {
            for (int ti =1; ti<tc.length; ti++) {
                double cost;
                if (sc[si] == tc[ti]) {
                    cost= 0;
                } else if (space.contains(sc[si]) && space.contains(tc[ti])) {
                    cost = 0;  
                } else {
                    
                    HashMap<Character,Double> thisReplaceCost = specialCosts.get(sc[si]);
                    if (thisReplaceCost == null) {
                        cost = replaceCost;
                    } else {
                        Double d = thisReplaceCost.get(tc[ti]);
                        if (d == null) {
                            cost = replaceCost;
                        } else {
                            cost = d;
                        }
                            
                    }
                }
                ca[si][ti] = Math.min(ca[si-1][ti-1] + cost, Math.min(ca[si][ti-1]+gapCost, ca[si-1][ti] + gapCost));
            }
        }
        
        return ca[sc.length-1][tc.length-1];

    }    
    
    public static double editCost(String s, String t, double gapCost, double spaceGapCost,  double replaceCost, HashSet<Character> space) {
        int len_s = s.length();
        int len_t = t.length();
        int last_s = s.length()-1;
        int last_t = t.length()-1;
        char[] sc = s.toCharArray();
        char[] tc = t.toCharArray();
        
        double[][] ca = new double[sc.length][tc.length];
        ca[0][0] = sc[0] == tc[0] ? 0: replaceCost;
        for (int i = 1; i < sc.length; i++) {
            ca[i][0] = ca[i-1][0] + gapCost;
        }
        for (int i = 1; i < tc.length; i++) {
            ca[0][i] = ca[0][i-1] + gapCost;
        }
        
        
        for (int si = 1; si< sc.length; si ++) {
            for (int ti =1; ti<tc.length; ti++) {
                double cost = 0;
                double sgap = gapCost;
                double tgap = gapCost;
                if (sc[si] != tc[ti]) {
                    if (space.contains(sc[si]) || space.contains(tc[ti])) {
                        if (space.contains(sc[si]) && space.contains(tc[ti])) {
                            cost = 0;  
                            sgap = spaceGapCost;
                            tgap = spaceGapCost;
                        } else {
                            if (space.contains(sc[si]))
                                tgap = spaceGapCost;
                            else
                                sgap = spaceGapCost;
                        }
                    } else {
                        cost = replaceCost;
                    }
                }
                ca[si][ti] = Math.min(ca[si-1][ti-1] + cost, Math.min(ca[si][ti-1]+sgap, ca[si-1][ti] + tgap));
            }
        }
        
        return ca[sc.length-1][tc.length-1];

    }    
    
}
