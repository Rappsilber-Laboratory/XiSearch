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
package rappsilber.ms.sequence.digest;

import java.text.ParseException;
import java.util.ArrayList;
import rappsilber.config.RunConfig;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class UnspecificDigestion extends Digestion{

    public UnspecificDigestion( int misscleavages) {
        super(new AminoAcid[0], misscleavages);
    }

    public UnspecificDigestion() {
        super(new AminoAcid[0], new AminoAcid[0]);
    }
    
    /**
     * tests whether a sequence can be cleaved after the given amin-oacid
     * @param seq
     * @param AAPos
     * @return
     */
    @Override
    protected boolean isCleavageSite(Sequence seq, int AAPos) {
        return true;
    }    

    @Override
    public boolean isDigestedPeptide(Peptide p) {
        return true;
    }

    
    
    
    public static Digestion parseArgs(String args, RunConfig conf) throws ParseException {
        
        // Complete this and return a PostAAConstrainedDigestion
        String name = "enzyme";

        // parses something like: DigestedAminoAcids:R,K;ConstrainingAminoAcids:P
        String[] options = args.split(";");
        for (String a : options) {
            // Strip the string of whitespace and make it uppercase for comparison
            String x = (a.trim()).toUpperCase();
            // the amino acid substring
            String aa_substring = x.substring(x.indexOf(":") + 1);

            if (x.startsWith("NAME")){
                name = aa_substring;
            } else {
                throw new ParseException("Could not read type of Digested AA's from config file, " +
                        " read: '" + args +"'", 0);
            }
        }
        AminoAcid aas[]= new AminoAcid[0];
        
        UnspecificDigestion d =  new UnspecificDigestion();


        d.setName(name);

        return d;
    }
    
}
