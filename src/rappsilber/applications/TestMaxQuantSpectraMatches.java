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
package rappsilber.applications;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import rappsilber.config.AbstractRunConfig;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.ions.AIon;
import rappsilber.ms.sequence.ions.BIon;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.YIon;


/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class TestMaxQuantSpectraMatches {

    HashMap<Double,ArrayList<Double>> errors = new HashMap<Double, ArrayList<Double>>(){
      @Override
      public ArrayList<Double> get(Object v) {
          ArrayList<Double> values = super.get(v);

          if (values == null) {
              values = new ArrayList<Double>();
              super.put((Double) v,values);
          }
          return values;
      }
    };

    public void addLine(String Peptide, String ionNames, String mzValues) {
        String[] ions = ionNames.split(";");
        String[] mz = mzValues.split(";");
        Sequence s = new Sequence(Peptide, AbstractRunConfig.DUMMYCONFIG);
        Peptide p = new Peptide(s, 0, s.length());
        for (int i = 0; i<ions.length; i++) {

            double meassured = 0;
            double mass = 0;
            try {
                String ion = ions[i];
                String[] ionParts = ion.split("\\(");
                String ionName = ionParts[0];
                Fragment f = null;
                if (ionName.startsWith("y")) {
                    f = new YIon(p, p.length() - Integer.parseInt(ionName.substring(1)));
                } else if (ionName.startsWith("b")) {
                    f = new BIon(p, Integer.parseInt(ionName.substring(1)));
                } else if (ionName.startsWith("a")) {
                    f = new AIon(p, Integer.parseInt(ionName.substring(1)));
                } else
                    continue;
                mass = 0;
                if (ionParts.length > 1) {
                    int charge = Integer.parseInt(ionParts[1].substring(0, 1));
                    mass = f.getMZ(charge);
                } else {
                    mass = f.getMZ(1);

                }
                meassured = Double.parseDouble(mz[i]);
            } catch (NumberFormatException numberFormatException) {

            }
            if (meassured > 0)
                errors.get((double)((int)(mass/2))*2).add(mass-meassured);
        }
    }

    public void write(PrintStream out) {
        TreeSet<Double> keys = new TreeSet<Double>();
        for (Double mass : errors.keySet()) {
            if (errors.get(mass).size()>10)
                keys.add(mass);
        }
        for (Double e : keys) {
            out.print(e + ",");
        }

        out.println();
        boolean found = true;
        for (int i = 0; found; i++) {
            found = false;
            for (Double mass : keys) {
                ArrayList<Double> meassurements = errors.get(mass);
                if (meassurements.size() > i) {
                    out.print(meassurements.get(i) + ",");
                    found = true;
                } else
                    out.print(",");
            }
            out.println();
        }

    }


    public static void main(String[] argv) throws FileNotFoundException, IOException {
        TestMaxQuantSpectraMatches search = new TestMaxQuantSpectraMatches();
        new AminoModification("Mox", AminoAcid.M, 147.035395).registerVariable();
        new AminoModification("Ccis", AminoAcid.C, 160.03065).registerFixed();
        //File csvIn = new File(argv[0]);
        File csvIn = new File("/home/lfischer/YeastAccuracy.csv");
        BufferedReader br = new BufferedReader(new FileReader(csvIn));
        String line = null;
        while ((line = br.readLine()) != null) {
            if (line.matches("\\s*\"?\\s*[a-zA-Z\\s]+\\s*\"?\\s*,\\s*\"?\\s*[a-zA-Z\\s]+\\s*\"?\\s*,\\s*\"?\\s*[a-zA-Z\\s]+\\s*\"?\\s*(,.*)?"))
                continue;
            String[] row = line.split(",",4);
            String peptide  = row[0];
            String ions  = row[1];
            String mz  = row[2];
            if (!peptide.startsWith("(a"))
                search.addLine(peptide, ions, mz);
        }

        search.write(new PrintStream("/home/lfischer/YeastAccuracy.list_2da.csv"));

    }

}
