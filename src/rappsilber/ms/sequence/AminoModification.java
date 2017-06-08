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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.sequence.digest.Digestion;
import rappsilber.utils.Util;

/**
 * Class that represents a PTM  of an aminoaccid
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class AminoModification extends AminoAcid {
    public AminoAcid BaseAminoAcid;
    public double weightDiff;
    public Integer pep_position = POSITIONAL_UNRESTRICTED;
    public Integer prot_position = POSITIONAL_UNRESTRICTED;
    public final static Integer POSITIONAL_UNRESTRICTED = null;
    public final static Integer POSITIONAL_NTERMINAL = 0;
    public final static Integer POSITIONAL_CTERMINAL = -1;

    /** some default modifications needed for handling special aminoacids (B and Z) */
    public static AminoModification Zq = new AminoModification("Zq", Z, Q.mass).registerVariable();
    public static AminoModification Bd = new AminoModification("Bd", B, D.mass).registerVariable();

    public static AminoModification Mox = new AminoModification("Mox", M, M.mass + Util.OXYGEN_MASS);

    /** mapping of base-aminoacids to registered modifications */
    private static HashMap<AminoAcid, ArrayList<AminoModification>> m_base2var_modifications;
    private static HashMap<AminoAcid, AminoModification> m_base2fixed_modifications;

    public AminoModification(String SequenceID,
                                AminoAcid BaseAminoAcid,
                                Double mass) {
        super(SequenceID,mass);
        this.BaseAminoAcid = BaseAminoAcid;
        this.weightDiff = mass - BaseAminoAcid.mass;
    }

    public AminoModification registerVariable() {
        super.register();

        if (m_base2var_modifications == null)
            m_base2var_modifications = new HashMap<AminoAcid, ArrayList<AminoModification>>();

        ArrayList<AminoModification> ml;
        if ((ml = m_base2var_modifications.get(BaseAminoAcid)) == null) {
            ml = new ArrayList<AminoModification>();
            m_base2var_modifications.put(BaseAminoAcid, ml);
        }
        ml.add(this);
        
        return this;
    }

    public AminoModification registerFixed() {
        super.register();

        if (m_base2fixed_modifications == null)
            m_base2fixed_modifications = new  HashMap<AminoAcid, AminoModification>();

        AminoModification prev =  m_base2fixed_modifications.put(BaseAminoAcid, this);

        if (prev != null) {
                Logger.getLogger(AminoModification.class.getName()).log(Level.WARNING,
                    "Replaced previously defined fixed modification "
                    + prev.SequenceID + " with " + SequenceID);
        }

        return this;
    }

    public static ArrayList<AminoModification> getVariableModifications(AminoAcid aminoAcid) {
        return m_base2var_modifications.get(aminoAcid);
    }

    public static AminoModification getFixedModification(AminoAcid aminoAcid) {
        return m_base2fixed_modifications.get(aminoAcid);
    }

    public static Collection<AminoModification> getAllFixedModification() {
        return m_base2fixed_modifications.values();
    }

    public static AminoModification getModifictaion(String className, String Options) {
        Class d= null;
        try {

            d = Class.forName("rappsilber.ms.sequence." + className);
            Method m = d.getMethod("parseArgs", String.class);
            return (AminoModification) m.invoke(null, Options);


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

    public static AminoModification parseArgs(String args) throws ParseException {

        // Complete this and return a AminoModification object
        AminoModification mod = null;
        // parses something like: "Symbol:Mox;ModifiedAminoAcid:M;MassChange:15.99491"
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

            if( x.startsWith("SYMBOL") ){
                symbol = value;
            }else if ( x.startsWith("MODIFIED") ){
                to_update = AminoAcid.getAminoAcid(value);
            }else if ( x.startsWith("MASS") ){
                mass_change = Double.parseDouble(value);
            }else{
                throw new ParseException("Could not read type of modifications from config file, " +
                        " read: '" + args +"'", 0);
            }
             
        }

        return new AminoModification(symbol, to_update, to_update.mass + mass_change);
    }

    public static List<AminoModification> getModifictaion(String className, String Options, RunConfig config) {
        Class d= null;
        try {

            d = Class.forName("rappsilber.ms.sequence." + className);
            Method m = d.getMethod("parseArgs", String.class, RunConfig.class);
            return (List<AminoModification>) m.invoke(null, Options, config);


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


    public static List<AminoModification> parseArgs(String args, RunConfig config) throws ParseException {

        // Complete this and return a AminoModification object
        AminoModification mod = null;
        // parses something like: "Symbol:Mox;ModifiedAminoAcid:M;MassChange:15.99491"
        String symbol = "";
        String symbolext = "";
        ArrayList<AminoAcid> to_update = new ArrayList<AminoAcid>();
//        AminoAcid to_update = null;
        double mass_change = 0d;
        double deltaMass = 0d;
        
        Integer pep_position = POSITIONAL_UNRESTRICTED;
        Integer prot_position = POSITIONAL_UNRESTRICTED;

       String[] options = args.split(";");
        for (String a : options) {
            // Strip the string of whitespace and make it uppercase for comparison
            String x = (a.trim());
            // the amino acid substring
            String value = x.substring(x.indexOf(":") + 1);
            x=x.toUpperCase();

            if( x.startsWith("SYMBOL:") ){
                symbol = value;
            } else if( x.startsWith("SYMBOLEXT:") ){
                symbolext = value;
            }else if ( x.startsWith("MODIFIED:") ){
                String[] v = value.split(",");
                if (value.contentEquals("X") || value.contentEquals("*") )
                    for (AminoAcid aa : config.getAllAminoAcids()) {
                        if (!(aa instanceof AminoModification))
                            to_update.add(aa);
                    }
                else
                    for (String aa : v) {
                        to_update.add(config.getAminoAcid(aa));
                    }
            }else if ( x.startsWith("MASS:") ){
                mass_change = Double.parseDouble(value);
            }else if ( x.startsWith("DELTAMASS:") ){
                deltaMass = Double.parseDouble(value);
            }else if (x.startsWith("PEPTIDEPOSITION:")) {
                if (value.toLowerCase().contentEquals("nterm") || value.toLowerCase().contentEquals("nterminal")) {
                    pep_position = POSITIONAL_NTERMINAL;
                } else if (value.toLowerCase().contentEquals("cterm") || value.toLowerCase().contentEquals("cterminal")) {
                    pep_position = POSITIONAL_CTERMINAL;
                } else if (value.toLowerCase().contentEquals("any")) {
                    pep_position = POSITIONAL_UNRESTRICTED;
                } else {
                    throw  new ParseException("PEPTIDEPOSITION is only permited to be nterm,nterminal,ctrem,cterminal or any",0);
                }  
            }else if (x.startsWith("PROTEINPOSITION:")) {
                if (value.toLowerCase().contentEquals("nterm") || value.toLowerCase().contentEquals("nterminal")) {
                    prot_position = POSITIONAL_NTERMINAL;
                } else if (value.toLowerCase().contentEquals("cterm") || value.toLowerCase().contentEquals("cterminal")) {
                    prot_position = POSITIONAL_CTERMINAL;
                } else if (value.toLowerCase().contentEquals("any")) {
                    prot_position = POSITIONAL_UNRESTRICTED;
                } else {
                    throw  new ParseException("PROTEINPOSITION is only permited to be nterm,nterminal,ctrem,cterminal or any",0);
                }  
            }else{
                throw new ParseException("Could not read type of modifications from config file, " +
                        " read: '" + args +"' ("+x+")", 0);
            } 

        }
        
        
        ArrayList<AminoModification> ret = new ArrayList<AminoModification>(1);

        if ( to_update.size() == 1 ) {
            if (deltaMass != 0 && mass_change == 0) {
                mass_change = to_update.get(0).mass + deltaMass;
            }
            if (symbol.length() == 0) {
                symbol = to_update.get(0).SequenceID + symbolext;
            }
            AminoModification am = new AminoModification(symbol, to_update.get(0), mass_change);
            am.pep_position=pep_position;
            am.prot_position=prot_position;
            ret.add(am);
        } else {
            if (symbolext.isEmpty()) {
                throw new ParseException("Multiple aminoacids are to be modified but no symbolextension is defined: '"+args +"'" ,0);
            }
            if (deltaMass == 0) {
                throw new ParseException("Multiple aminoacids are to be modified but no deltamass is defined: '"+args +"'" ,0);
            }
            for (AminoAcid aa : to_update) {
                AminoModification am = new AminoModification(aa.SequenceID + symbolext, aa, aa.mass + deltaMass);
                am.pep_position=pep_position;
                am.prot_position=prot_position;
                ret.add(am);
            }
        }
                
        return ret;
    }

}
