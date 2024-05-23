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
import rappsilber.utils.CountOccurence;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CountAAPair extends HashMap<AminoAcid,CountOccurence<AminoAcid>>{
    private static final long serialVersionUID = -6475556837505776086L;

    public CountOccurence<AminoAcid> get(AminoAcid aa) {
        CountOccurence<AminoAcid> ret = super.get(aa);
        if (ret == null) {
            ret = new CountOccurence<AminoAcid>();
            put(aa,ret);
        }
        return ret;
    }

    public int add(AminoAcid n, AminoAcid c) {
        CountOccurence<AminoAcid> nCol = super.get(n);
        if (nCol == null) {
            nCol = new CountOccurence<AminoAcid>();
            put(n,nCol);
        }
        return nCol.add(c);
    }

    public int get(AminoAcid n, AminoAcid c) {
        CountOccurence<AminoAcid> nCol = super.get(n);
        if (nCol == null) {
            return 0;
        } else {
            return nCol.count(c);
        }
    }

}