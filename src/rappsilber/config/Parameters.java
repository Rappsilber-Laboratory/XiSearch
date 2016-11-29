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
package rappsilber.config;

import java.io.File;
import java.util.ArrayList;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.crosslinker.SymetricSingleAminoAcidRestrictedCrossLinker;
import rappsilber.ms.score.ScoreSpectraMatch;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.digest.Digestion;
import rappsilber.ms.sequence.digest.PostAAConstrainedDigestion;

/**
 * Static class that will hold all the SELECTED parameters from the user.
 * These can then be referenced throughout the program but should be done so
 * only post processing the user input otherwise the default setting for
 * each will be applied
 * @author stahir
 */
public class Parameters {


    // OPTIONS originating from the config file
    /**
     * Missed clevages to consider - DEFAULT SETTING: 2
     */
    public static int MISSED_CLEAVAGES = 2;

    /**
     * Top number of MGC matches to consider for searching - DEFAULT SETTING: 10
     * i.e. we take the the first ten and if the 11th, 12th etc. have the same score
     * as the 10th, we take them also
     */
    public static int TOP_MGC = 10;

    /**
     * This option determines whether the peptides we consider are synthetic or not.
     * Synthetic peptides are NOT digested - DEFAULT SETTING; false
     */
    public static boolean SYNTHETIC_PEPTIDES = false;
    
    /**
     * Digestion enzyme to cleave peptides with - DEFAULT SETTING: TRYPSIN
     */
    public static Digestion ENZYME = new PostAAConstrainedDigestion(
            new AminoAcid[]{AminoAcid.R, AminoAcid.K},
            new AminoAcid[]{AminoAcid.P});


    /**
     * Stores a list of all selected fixed modifications - DEFAULT SETTING: none
     */
    public static ArrayList<AminoModification> FIXED_MODIFICATIONS = new ArrayList<AminoModification>();
        /**
     * Stores a list of all selected variable modifications - DEFAULT SETTING: none
     */
    public static ArrayList<AminoModification> VARIABLE_MODIFICATIONS = new ArrayList<AminoModification>();



    // Options originating from the UI
    /**
     * MSM file with spectra - DEFAULT SETTING: null so must check before proceeding
     */
    public static File MSM_FILE = null;

    public static CrossLinker CROSSLINKER = new SymetricSingleAminoAcidRestrictedCrossLinker("BS2G-d0", 96.02112055, 96.02112055, new AminoAcid[] {AminoAcid.K});

    /**
     * Sequence file holding proteins to process - DEFAULT SETTING: null so must check before proceeding
     */
    public static File SEQUENCE_FILE  = null;

    /**
     * Experiment Name of search session - DEFAULT SETTING: "TEST"
     */
    public static String EXPERIMENT_NAME = "TEST";

    /**
     * mass tolerance for the precursor ion
     */
    public static ToleranceUnit PRECURSOR_TOLERANCE = new ToleranceUnit(6, "ppm");

    /**
     * mass tolerance for the fragment ions
     */
    public static ToleranceUnit FRAGMENT_TOLERANCE = new ToleranceUnit(25, "ppm");

    public static ArrayList<ScoreSpectraMatch> SCORING = new ArrayList<ScoreSpectraMatch>();


    /**
     * User that initiated the search - DEFAULT SETTING: "DEVELOPER"
     */
    public static String USER = "DEVELOPER";
    

    
}// end class Parameters
