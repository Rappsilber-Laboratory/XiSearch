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
package rappsilber.ms.lookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;
import rappsilber.ms.Range;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.statistics.utils.UpdateableInteger;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ModificationLookup extends TreeMap<Double,HashMap<AminoModification,UpdateableInteger>> implements Lookup<AminoModification>{

    ToleranceUnit m_tolerance = new ToleranceUnit(0, "da");

    @Override
    public void setTolerance(ToleranceUnit tolerance) {
        m_tolerance = tolerance;
    }

    @Override
    public synchronized ArrayList<AminoModification>  getForMass(double mass) {
        ArrayList<AminoModification> allAM = new ArrayList<AminoModification>();
        Range r = m_tolerance.getRange(mass);
        for (HashMap<AminoModification,UpdateableInteger> am : this.subMap(r.min,r.max).values()) {
            allAM.addAll(am.keySet());
        }
        return allAM;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public synchronized ArrayList<AminoModification> getForMass(double mass, double referenceMass) {
        ArrayList<AminoModification> allAM = new ArrayList<AminoModification>();
        for (HashMap<AminoModification,UpdateableInteger> am : this.subMap(m_tolerance.getMinRange(mass,referenceMass), m_tolerance.getMaxRange(mass,referenceMass)).values()) {
            allAM.addAll(am.keySet());
        }
        return allAM;
    }

    public synchronized  void add(AminoModification am) {
        double amMass = am.mass - am.BaseAminoAcid.mass;
       HashMap<AminoModification,UpdateableInteger> ams = get(am.mass - am.BaseAminoAcid.mass);
        if (ams == null) {
            ams = new HashMap<AminoModification,UpdateableInteger>();
            put(amMass,ams);
        }
        ams.put(am,new UpdateableInteger(0));
    }

    public void inc(AminoModification am) {
        double mass = am.weightDiff;
        Range r = m_tolerance.getRange(mass);
        for (HashMap<AminoModification,UpdateableInteger> ams : this.subMap(r.min,r.max).values()) {
            UpdateableInteger i = ams.get(am);
            if (i != null) {
                i.value++;
                break;
            }
        }
        ArrayList<AminoModification> ams = getForMass(am.weightDiff);
    }

    public ArrayList<AminoModification> getAll() {
        ArrayList<AminoModification> allAM = new ArrayList<AminoModification>();
        for (HashMap<AminoModification,UpdateableInteger> am : this.values()) {
            allAM.addAll(am.keySet());
        }
        return allAM;
    }

    public HashMap<AminoModification,UpdateableInteger> getAllCounts() {
        HashMap<AminoModification,UpdateableInteger> allAM = new HashMap<AminoModification,UpdateableInteger>();
        for (HashMap<AminoModification,UpdateableInteger> am : this.values()) {
            allAM.putAll(am);
        }
        return allAM;
    }

}
