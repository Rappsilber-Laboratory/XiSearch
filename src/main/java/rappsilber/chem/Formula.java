/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.chem;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import rappsilber.ms.statistics.utils.UpdateableDouble;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class Formula {

    static final Pattern elemDef
            = Pattern.compile(
                    "\\s*(-)?([0-9]*[A-Z][a-z]?)((?:-[0-9])?[0-9]*(?:\\.[0-9]+)?)");

    final String[] elements;
    final double[] counts;
    private final int hashcode;

    @Override
    public int hashCode() {
        return this.hashcode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Formula other = (Formula) obj;
        if (!Arrays.deepEquals(this.elements, other.elements)) {
            return false;
        }
        return Arrays.equals(this.counts, other.counts);
    }

    public Formula(String[] elements, double[] counts) {
        this.elements = elements;
        this.counts = counts;
        this.hashcode = Arrays.hashCode(counts) * 31 + Arrays.hashCode(elements);
    }

    /**
     * Convert a String representation of a chemical formula into a Formula
     * object.
     *
     * Accepts formulas like CH3 = 1xC 3xH C5H5-C3 = 2xC 5xH C5H5C-3 = 2xC 5xH
     * C5H5 13C = 5xC 5xH 1x13C
     *
     * @param f
     * @return
     */
    public static Formula parseformula(String f) {
        TreeMap<String, UpdateableDouble> counts = new TreeMap<>();
        Matcher m = IsotopeCalculation.elemDef.matcher(f);
        while (m.find()) {
            String name = m.group(2);
            int neg = m.group(1) != null ? -1 : 1;
            double count = m.group(3) == null ? 1 : Double.parseDouble(m.group(3));
            UpdateableDouble c = counts.get(name);
            if (c == null) {
                c = new UpdateableDouble(count);
                counts.put(name, c);
            } else {
                c.value += neg * count;
            }
        }
        String[] elemName = new String[counts.size()];
        double[] elemCount = new double[counts.size()];
        int i = 0;
        for (Map.Entry<String, UpdateableDouble> e : counts.entrySet()) {
            elemName[i] = e.getKey();
            elemCount[i++] = e.getValue().value;
        }

        return new Formula(elemName, elemCount);
    }
}
