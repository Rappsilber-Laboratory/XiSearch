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
package rappsilber.ms.sequence;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import rappsilber.config.RunConfig;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class NonAminoAcidModification {
    
    public static NonAminoAcidModification NO_MODIFICATION  = 
            new NonAminoAcidModification("", 0) {
                public String toString() {
                    return "";
                };
            };

    public String Id;
    public double Mass;
    public boolean isNterminal;
    public boolean isProteinNterminal = false;
    public boolean isCterminal;
    public boolean isProteinCterminal = false;


    public NonAminoAcidModification(String Id, double Mass) {
        this.Id = Id;
        this.Mass = Mass;
    }

    public NonAminoAcidModification(String Id, double Mass, boolean isNterminal) {
        this.Id = Id;
        this.Mass = Mass;
        this.isNterminal = isNterminal;
        this.isCterminal = !isNterminal;
    }


    public boolean canModify(Peptide p) {
        if (isProteinCterminal)
            return p.isCTerminal();
        else if (isProteinNterminal)
            return p.isNTerminal();
        else return true;

    }


    public static List<NonAminoAcidModification> parseArgs(String args, RunConfig config) throws ParseException {
        //modification:variable:NonAminoAcidModification:SIDE:N;SYMBOL:ca;MASS:43.005814
        //modification:variable:NonAminoAcidModification:SIDE:N;SYMBOL:ac;MASS:42.010565
        //nterminalmodification:variable:NonAminoAcidModification:SIDE:N;SYMBOL:ac;MASS:42.010565
        // Complete this and return a AminoModification object
        AminoModification mod = null;
        // parses something like: "Symbol:Mox;ModifiedAminoAcid:M;MassChange:15.99491"
        String side = "";
        String symbol = "";
        AminoAcid to_update = null;
        double mass_change = 0d;

       String[] options = args.split(";");
        for (String a : options) {
            // Strip the string of whitespace and make it uppercase for comparison
            String x = (a.trim());
            // the amino acid substring
            String value = x.substring(x.indexOf(":") + 1);
            x=x.toUpperCase();

            if( x.startsWith("SIDE") ){
                side = value;
            }else if ( x.startsWith("SYMBOL") ){
                symbol = value;
            }else if ( x.startsWith("MASS") ){
                mass_change = Double.parseDouble(value);
            }else{
                throw new ParseException("Could not read type of modifications from config file, " +
                        " read: '" + args +"'", 0);
            }

        }
        NonAminoAcidModification ret = new NonAminoAcidModification(symbol, mass_change, side.toUpperCase().contains("N"));
        List<NonAminoAcidModification> retL = new ArrayList<NonAminoAcidModification>(1);
        retL.add(ret);
        return retL;
    }

    public String toString() {
        return "." + Id + ".";
    }

}
