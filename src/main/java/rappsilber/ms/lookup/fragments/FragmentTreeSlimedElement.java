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
package rappsilber.ms.lookup.fragments;

public class FragmentTreeSlimedElement {
    public static int INITIAL = 1;
    public static int INCREMENT = 1;
    public int[] m_peptideIds = new int[INITIAL];
    public int m_countPeptides;
    
    public FragmentTreeSlimedElement(int pepID) {
        m_peptideIds[0] = pepID;
        m_countPeptides = 1;
    }

    public  void add(int pepId) {
        synchronized(this) {
            if (m_peptideIds.length == m_countPeptides) {
//                int[] newIds = new int[m_countPeptides + (m_countPeptides/2) + INCREMENT];
                int[] newIds = new int[m_countPeptides + INCREMENT];
                System.arraycopy(m_peptideIds, 0, newIds, 0, m_peptideIds.length);
                m_peptideIds = newIds;
            }
            m_peptideIds[m_countPeptides++] = pepId;
        }
    }

    public void shrink() {
        synchronized(this) {
            if (m_peptideIds.length > m_countPeptides) {
                int[] newIds = new int[m_countPeptides];
                System.arraycopy(m_peptideIds, 0, newIds, 0, m_countPeptides);
                m_peptideIds = newIds;
            }
        }
    }
}
