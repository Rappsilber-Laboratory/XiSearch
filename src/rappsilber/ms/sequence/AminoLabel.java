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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;

/**
 * Class that represents labeled  aminoaccids - since these do not
 * change chemical properties, the crosslinker has to be aware of them
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class AminoLabel extends AminoAcid {

    public AminoAcid BaseAminoAcid;
    public double weightDiff;
    public String labelID;

    public AminoLabel(AminoAcid BaseAminoAcid,
                                Double massDiff) {
        super(BaseAminoAcid.SequenceID + "*" + massDiff + "*", BaseAminoAcid.mass + massDiff);
        this.BaseAminoAcid = BaseAminoAcid;
        this.weightDiff = massDiff;
        this.labelID = "*" + massDiff + "*";
    }

    public AminoLabel(String symbol, AminoAcid BaseAminoAcid,
                                Double massDiff) {
        super(symbol, BaseAminoAcid.mass + massDiff);
        this.BaseAminoAcid = BaseAminoAcid;
        this.weightDiff = massDiff;
        String lid = null;
        for (int i = 0; i< symbol.length();i++) {
            if (BaseAminoAcid.SequenceID.length()<=i || !symbol.substring(0,i+1).contentEquals(BaseAminoAcid.SequenceID.substring(0,i+1))) {
                lid = symbol.substring(i);
            }

        }
        if (lid == null) {
            this.labelID = "*" + massDiff + "*";
        } else
            this.labelID = lid;
    }



    public static AminoLabel parseArgs(String args, RunConfig config) throws ParseException {

        // Complete this and return a AminoModification object
        AminoModification mod = null;
        // parses something like: "Symbol:Mox;ModifiedAminoAcid:M;MassChange:15.99491"
        AminoAcid to_update = null;
        double mass_change = Double.NaN;
        double mass = Double.NaN;
        String symbol = null;

       String[] options = args.split(";");
        for (String a : options) {
            // Strip the string of whitespace and make it uppercase for comparison
            String x = (a.trim()).toUpperCase();
            // the amino acid substring
            String value = x.substring(x.indexOf(":") + 1).trim();

            if ( x.startsWith("MODIFIED") ){
                to_update = config.getAminoAcid(value);
            } else if (x.startsWith("SYMBOL"))
                {
                symbol = value;
            }else if ( x.startsWith("MASSDIFF") ){
                mass_change = Double.parseDouble(value.trim());

            }else if ( x.startsWith("MASS") ){
                mass = Double.parseDouble(value.trim());
            }else{
                throw new ParseException("Could not read type of modifications from config file, " +
                        " read: '" + args +"'", 0);
            }

        }
        if (!Double.isNaN(mass))
            mass_change = mass - to_update.mass;

//        for (CrossLinker cl: config.getCrossLinker()) {
//            if (cl instanceof )
//        }
        if (symbol == null)
            return new AminoLabel(to_update, mass_change);
        else
            return new AminoLabel(symbol, to_update, mass_change);
        
    }

    public static AminoLabel getLabel(String className, String Options, RunConfig config) {
        Class d= null;
        try {

            d = Class.forName("rappsilber.ms.sequence." + className);
            Method m = d.getMethod("parseArgs", String.class, RunConfig.class);
            return (AminoLabel) m.invoke(null, Options, config);


        } catch (ClassNotFoundException ex) {
            Logger.getLogger(AminoModification.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(AminoModification.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(AminoModification.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(AminoModification.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(AminoModification.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(AminoModification.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }


}
