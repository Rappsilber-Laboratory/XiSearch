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
package rappsilber.ms.statistics.utils;

import java.util.HashMap;
import rappsilber.ms.sequence.AminoAcid;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class AAPair2Double extends HashMap<AminoAcid, HashMap<AminoAcid, Double>>{
    private static final long serialVersionUID = 7063495137830561311L;

    public HashMap<AminoAcid, Double> get(AminoAcid aa) {
        HashMap<AminoAcid, Double> ret = super.get(aa);
        if (ret == null) {
            ret = new HashMap<AminoAcid, Double>();
            put(aa,ret);
        }
        return ret;
    }

    public void set(AminoAcid n, AminoAcid c, double value) {
        HashMap<AminoAcid, Double> nCol = super.get(n);
        if (nCol == null) {
            nCol = new HashMap<AminoAcid, Double>();
            put(n,nCol);
        }
        nCol.put(c,value);
    }

    public double get(AminoAcid n, AminoAcid c, double defaultValue) {
        HashMap<AminoAcid, Double> nCol = super.get(n);
        if (nCol == null || ! nCol.containsKey(c)) {
            return defaultValue;
        } else {
            return nCol.get(c);
        }
    }

}