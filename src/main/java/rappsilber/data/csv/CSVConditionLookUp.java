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
package rappsilber.data.csv;

import java.util.ArrayList;
import rappsilber.utils.HashMapList;

/**
 * provides a hashed access for a joined condition
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CSVConditionLookUp {
    private int[] m_columns;
    HashMapList<String, Object> m_toplevel;
    CSVRandomAccess m_csv;

//    public CSVConditionLookUp(CSVRandomAccess m_csv, int[] columns) {
//        m_csv=m_csv;
//        m_columns = columns;
//        build();
//    }

    public CSVConditionLookUp(CSVRandomAccess csv, int ... columns) {
        m_csv=csv;
        m_columns = columns;
        build();
    }    
    
    
    private final static ArrayList<Integer> NORESULT = new ArrayList<Integer>(){
                @Override
                public boolean add(Integer e) {
                    throw new UnsupportedOperationException("This is an always empty ArrayList");
                }
                
                @Override
                public boolean isEmpty() {
                    return true;
                }
                
            };

    
    protected HashMapList<String, Object> getAddSubMap(HashMapList<String, Object> parent, String value) {
        HashMapList<String, Object> m = (HashMapList<String, Object>) parent.get(value);
        if (m==null) {
            m = new HashMapList<String, Object>();
            parent.put(value, m);
        }
        return m;
    }

    /**
     * sets up a nested hash-map for the defined columns
     */
    public void build() {
        m_toplevel = new HashMapList<String, Object>();
        for (int r = m_csv.getRowCount()-1; r>= 0 ; r--) {
            HashMapList<String, Object> level = m_toplevel;
            int c = 0;
            for (; c< m_columns.length-1; c ++) {
                level = getAddSubMap(level, m_csv.getValue(m_columns[c], r));
            }
            ArrayList<Integer> lines = (ArrayList<Integer>) level.get(m_csv.getValue(m_columns[c], r));
            if ( lines == null) {
                lines = new ArrayList<Integer>();
                level.put(m_csv.getValue(m_columns[c], r),lines);
            }
            lines.add(r);
        }
    }
    
//    public ArrayList<Integer> getRows(String[] values) {
    public ArrayList<Integer> getRows(String ... values) {
        HashMapList<String, Object> level = m_toplevel;
        int c = 0;
        for (; c< values.length-1; c ++) {
            level = getAddSubMap(level, values[c]);
        }
        ArrayList<Integer> lines = (ArrayList<Integer>) level.get(values[c]);
        if (lines == null)
            return NORESULT;
        return lines;
    }
}
