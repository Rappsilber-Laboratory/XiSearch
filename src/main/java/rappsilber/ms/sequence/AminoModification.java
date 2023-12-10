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
import rappsilber.config.AbstractRunConfig;
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
    public boolean postDigest = false;
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

    public AminoModification(String SequenceID,
                                AminoAcid BaseAminoAcid,
                                Double mass, boolean postDigest) {
        this(SequenceID, BaseAminoAcid, mass);
        this.postDigest = postDigest;
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

//    public static AminoModification getModifictaion(String className, String Options) {
//        Class d= null;
//        try {
//
//            d = Class.forName("rappsilber.ms.sequence." + className);
//            Method m = d.getMethod("parseArgs", String.class);
//            return (AminoModification) m.invoke(null, Options);
//
//
//        } catch (ClassNotFoundException ex) {
//            Logger.getLogger(AminoModification.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            Logger.getLogger(AminoModification.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IllegalArgumentException ex) {
//            Logger.getLogger(AminoModification.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (InvocationTargetException ex) {
//            Logger.getLogger(AminoModification.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (NoSuchMethodException ex) {
//            Logger.getLogger(AminoModification.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (SecurityException ex) {
//            Logger.getLogger(AminoModification.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return null;
//
//    }
//
//    public static AminoModification parseArgs(String args) throws ParseException {
//
//        // Complete this and return a AminoModification object
//        AminoModification mod = null;
//        // parses something like: "Symbol:Mox;ModifiedAminoAcid:M;MassChange:15.99491"
//        String symbol = "";
//        AminoAcid to_update = null;
//        double mass_change = 0d;
//
//       String[] options = args.split(";");
//        for (String a : options) {
//            // Strip the string of whitespace and make it uppercase for comparison
//            String x = (a.trim());
//            // the amino acid substring
//            String value = x.substring(x.indexOf(":") + 1);
//            x=x.toUpperCase();
//
//            if( x.startsWith("SYMBOL") ){
//                symbol = value;
//            }else if ( x.startsWith("MODIFIED") ){
//                to_update = AminoAcid.getAminoAcid(value);
//            }else if ( x.startsWith("MASS") ){
//                mass_change = Double.parseDouble(value);
//            }else{
//                throw new ParseException("Could not read type of modifications from config file, " +
//                        " read: '" + args +"'", 0);
//            }
//             
//        }
//
//        return new AminoModification(symbol, to_update, to_update.mass + mass_change);
//    }

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
        Double mass_change = null;
        Double deltaMass = null;
        boolean postdigest = false;
        boolean anyterm = false;
        
        Integer pep_position = POSITIONAL_UNRESTRICTED;
        Integer prot_position = POSITIONAL_UNRESTRICTED;

       String[] options = args.split(";");
        for (String a : options) {
            // Strip the string of whitespace and make it uppercase for comparison
            String x = (a.trim());
            // the amino acid substring
            String value = x.substring(x.indexOf(":") + 1).trim();
            x=x.toUpperCase();

            if( x.startsWith("SYMBOL:") ){
                if (rappsilber.utils.Util.AutoCaseSymbols) {
                    if (value.length()>1)
                        symbol = value.substring(0, 1).toUpperCase()+value.substring(1).toLowerCase();
                    else
                        symbol = value.toUpperCase();
                } else
                    symbol = value;
                
            } else if( x.startsWith("SYMBOLEXT:") ){
                if (rappsilber.utils.Util.AutoCaseSymbols) {
                    symbolext = value.toLowerCase();
                } else
                    symbolext = value;
            }else if ( x.startsWith("MODIFIED:") ){
                String[] v = value.split(",");
                if (value.contentEquals("X") ||value.contentEquals("ANY") || value.contentEquals("*") )
                    for (AminoAcid aa : config.getAllAminoAcids()) {
                        if (!(aa instanceof AminoModification))
                            to_update.add(aa);
                    }
                else
                    for (String saa : v) {
                        saa = saa.trim();
                        String saa_term = saa.replaceAll("-", "").replaceAll(" ", "");
                        if (saa_term.contentEquals("nterm")||
                                saa_term.contentEquals("protnterm")||
                                saa_term.contentEquals("proteinnterm")) {
                            anyterm = true;
                            if (prot_position != POSITIONAL_UNRESTRICTED)
                                throw new ParseException("Protein-position defined more then ones '" + args +"'", 0);
                            prot_position = POSITIONAL_NTERMINAL;
                            continue;
                        }
                        if (saa_term.contentEquals("cterm")||
                                saa_term.contentEquals("protcterm")||
                                saa_term.contentEquals("proteincterm")) {
                            anyterm = true;
                            if (prot_position != POSITIONAL_UNRESTRICTED)
                                throw new ParseException("Protein-position defined more then ones '" + args +"'", 0);
                            prot_position = POSITIONAL_CTERMINAL;
                            continue;
                        }
                        if (saa_term.contentEquals("pepnterm")||saa_term.contentEquals("peptidenterm")) {
                            anyterm = true;
                            if (pep_position != POSITIONAL_UNRESTRICTED)
                                throw new ParseException("Peptide-position defined more then ones '" + args +"'", 0);
                            pep_position = POSITIONAL_NTERMINAL;
                            postdigest = true;
                            continue;
                        }
                        if (saa_term.contentEquals("pepcterm")||saa_term.contentEquals("peptidecterm")) {
                            anyterm = true;
                            if (pep_position != POSITIONAL_UNRESTRICTED)
                                throw new ParseException("Peptide-position defined more then ones '" + args +"'", 0);
                            pep_position = POSITIONAL_CTERMINAL;
                            postdigest = true;
                            continue;
                        }
                        AminoAcid aa  = config.getAminoAcid(saa);
                        if (aa == null) {
                            String message = "Can't parse specificity \"" + 
                                            saa+ "\" of modification: " + args;
                            Logger.getLogger(
                                    AminoModification.class.getCanonicalName()).
                                    log(Level.SEVERE, message);
                            throw new ParseException(message, 0);
                        }
                        to_update.add(aa);
                    }
            }else if ( x.startsWith("MASS:") ){
                mass_change = Double.parseDouble(value);
            }else if ( x.startsWith("DELTAMASS:") ){
                deltaMass = Double.parseDouble(value);
            }else if (x.startsWith("POSTDIGEST:")) {
                postdigest = AbstractRunConfig.getBoolean(value, postdigest);
            }else if (x.startsWith("PEPTIDEPOSITION:")) {
                if (pep_position != POSITIONAL_UNRESTRICTED)
                    throw new ParseException("Peptide Position defined more then ones '" + args +"'", 0);

                if (value.toLowerCase().contentEquals("nterm") || value.toLowerCase().contentEquals("nterminal")) {
                    pep_position = POSITIONAL_NTERMINAL;
                    postdigest = true;
                } else if (value.toLowerCase().contentEquals("cterm") || value.toLowerCase().contentEquals("cterminal")) {
                    pep_position = POSITIONAL_CTERMINAL;
                    postdigest = true;
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
            if (deltaMass != null && mass_change == null) {
                mass_change = to_update.get(0).mass + deltaMass;
            }
            if (symbol.length() == 0) {
                symbol = to_update.get(0).SequenceID + symbolext;
            }
            AminoModification am = new AminoModification(symbol, to_update.get(0), mass_change);
            am.pep_position=pep_position;
            am.prot_position=prot_position;
            am.postDigest = postdigest;
            ret.add(am);
        } else {
            if (symbolext.isEmpty()) {
                throw new ParseException("Multiple aminoacids are to be modified but no symbolextension is defined: '"+args +"'" ,0);
            }
            if (deltaMass == null) {
                throw new ParseException("Multiple aminoacids are to be modified but no deltamass is defined: '"+args +"'" ,0);
            }
            for (AminoAcid aa : to_update) {
                AminoModification am = new AminoModification(aa.SequenceID + symbolext, aa, aa.mass + deltaMass);
                am.pep_position=pep_position;
                am.prot_position=prot_position;
                am.postDigest = postdigest;
                ret.add(am);
            }
        }
        if (anyterm) {
            if (symbolext.isEmpty()) {
                throw new ParseException("Terminal modification is defined but no symbolextension: '"+args +"'" ,0);
            }
            if (deltaMass == null) {
                throw new ParseException("Terminal modification is defined but no deltamass: '"+args +"'" ,0);
            }
            for (AminoAcid aa : config.getAllAminoAcids()) {
                AminoModification am = new AminoModification(aa.SequenceID + symbolext, aa, aa.mass + deltaMass);
                am.pep_position=pep_position;
                am.prot_position=prot_position;
                am.postDigest = postdigest;
                ret.add(am);
            }
        }
                
        return ret;
    }

    public AminoAcid getBaseAminoAcid() {
        return this.BaseAminoAcid;
    }

}
