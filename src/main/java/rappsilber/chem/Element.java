/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.chem;

import java.util.Arrays;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class Element {
    String symbol;
    double avMass;
    double[] masses;
    double[] prob; 
//    public static Element[] all_elements = new Element[]{
//        
//    }

    public Element(String symbol, double avMass, double[] masses, double[] prob) {
        this.symbol = symbol;
        this.avMass = avMass;
        this.masses = masses;
        this.prob = prob;
    }
    
    public Element(Element e, int maxIso) {
        this.symbol = e.symbol;
        this.avMass = e.avMass;
        this.masses = Arrays.copyOf(e.masses, maxIso);
        this.prob = Arrays.copyOf(e.prob, maxIso);
    }
    
}
