/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.chem;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class IsotopeCalculation {
    static final Pattern elemDef = Pattern.compile("\\s*-?[0-9]*[A-Z][a-z]?(?:-[0-9])?[0-9]*");
    HashMap<String, Element> elements;
    double neutron = 1.00866491600; // need this for approx. isotope mass calculations
    double proton = 1.007276466812; // need this for approx. isotope mass calculations
    int max = 10; // maximum number of isotopes to bother checking
    double[] nulliso = new double[max]; // a null isotope pattern (starting point for calculations)
    {
        nulliso[0]=1;
    }
    HashMap<Formula, double[]> formula_isos  = new HashMap();
    HashMap<Formula, Double> formula_mono  = new HashMap();
    
    
    public IsotopeCalculation(Element[] els){ // pass in your elements table (as parsed by parseTable)
        nulliso[0]=1;

	for(int i=1; i<this.max; i++){
		this.nulliso[i]= 0;
	}
        elements = new HashMap<>(els.length);
	// each row in this table is: Element, Average, iso0mass, iso0%, iso1mass, iso1%, etc.
	for(int i=0; i<els.length; i++){
            this.elements.put(els[i].symbol, new Element(els[i], max));
	}
}


    public int isomax(double[] iso){ // very basic
	// used in formulachainO
	int maxi = 0;
	double maxp = 0;
	for(int i=0; i<iso.length; i++){
		if(iso[i] > maxp){
			maxp = iso[i];
			maxi = i;
		}
	}
	return maxi;
    }

// methods for dealing with single formulae...

// here we combine the isos for elements in a formula, and also the mass
    public Object formula(String f){
	// called by formulachain, formulamass
	Formula fn = Formula.parseformula(f); // check it's the right format!
	return formula(fn);
    }

    protected Object formula(Formula fn) {
        // we might've already done this formula
        double[] previso = this.formula_isos.get(fn);
        
        if(previso != null) {
            return previso;
        }
        
        double[] iso = this.nulliso;
        double monomass = 0;
        for(int i=0; i<fn.elements.length; i++){
            String ename = fn.elements[i]; // element name
            double n = fn.counts[i]; // number of this type of element
            Element e = this.elements.get(ename);
            if(e == null){
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "No defintion for element " + ename + " found !");
                System.exit(-1);
            }
            monomass += e.masses[0] * n;
            for(int j=0; j<n; j++){
                iso = this.combine(e.prob, iso); // combine iso
            }
        }
        this.formula_isos.put(fn, iso);
        this.formula_mono.put(fn, monomass);
        return iso;
    }


    // this is the key calculation!!
    public double[] combine(double[] isoA, double[] isoB){
	// called by formulachain and formula
	// each iso is a list of probabilities
	// we want to combine probabilities from isoA, isoB and isoC, 
	// remembering that the corresponding index in isoC is the sum of indices being considered in the other two
	double[] isoC = new double[max];
	for(int i = 0; i < this.max; i++){
		for(int j=0; j<=i; j++){
			double d = isoA[j] * isoB[i-j];
			if(!Double.isNaN(d)){
				isoC[i] += d;
			}
		}
	}
	return isoC;
}

// calls formula() unless mass is already there
    public Object formulamass(String f){
	// called by formulachainmass
	Formula fn = Formula.parseformula(f);
        Double mass = this.formula_mono.get(fn);
	if(mass!=null) {
            return mass;
        }
        
        this.formula(fn);
	return this.formula_mono.get(fn);
    }

//    public static void main(String[] args) {
//        IsotopeCalculation ic = new IsotopeCalculation(args);
//    }


    
}
