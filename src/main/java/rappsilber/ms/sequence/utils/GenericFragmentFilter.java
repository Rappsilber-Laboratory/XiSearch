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
package rappsilber.ms.sequence.utils;

import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class GenericFragmentFilter implements FragmentFilter{


    private Class m_IncludeFragmentClass = Fragment.class;
    private Class m_ExcludeFragmentClass = Loss.class;

    private int m_maxLoss = 0;
    private int m_minLoss = 0;

    public boolean isValid(Fragment f) {

        if ((!f.isClass(m_IncludeFragmentClass)) || f.isClass(getExcludeFragmentClass())) {
            return false;
        }

        // if we are looking at the actual ocurence then we also check about loses
        if (f instanceof Loss) {
            int count = ((Loss)f).getLossCount();
            if (count >= getMinLoss()  && count <= getMaxLoss()) { // not to few or to many losses
                return true;
            }

        } else if (getMinLoss() <= 0) { // does it need to be a lossy peak
            return true;
        }

        return false;

    }

    /**
     * returns a Filter, that uses as much as possible of this object, but works
     * on
     * @return
     */
    public GenericFragmentFilter cloneNonLossy() {

        GenericFragmentFilter f = new GenericFragmentFilter();
        if (f.getIncludeFragmentClass().isAssignableFrom(Loss.class)) {
            f.setIncludeFragmentClass(Fragment.class);
        }

        f.setExcludeFragmentClass(getExcludeFragmentClass());
        f.setMaxLoss(0);
        f.setMinLoss(0);

        return f;
    }

    /**
     * @return the m_IncludeFragmentClass
     */
    public Class getIncludeFragmentClass() {
        return m_IncludeFragmentClass;
    }

    /**
     * @param m_IncludeFragmentClass the m_IncludeFragmentClass to set
     */
    public void setIncludeFragmentClass(Class m_IncludeFragmentClass) {
        this.m_IncludeFragmentClass = m_IncludeFragmentClass;
    }

    /**
     * @return the m_ExcludeFragmentClass
     */
    public Class getExcludeFragmentClass() {
        return m_ExcludeFragmentClass;
    }

    /**
     * @param m_ExcludeFragmentClass the m_ExcludeFragmentClass to set
     */
    public void setExcludeFragmentClass(Class m_ExcludeFragmentClass) {
        this.m_ExcludeFragmentClass = m_ExcludeFragmentClass;
    }

    /**
     * @return the m_maxLoss
     */
    public int getMaxLoss() {
        return m_maxLoss;
    }

    /**
     * @param m_maxLoss the m_maxLoss to set
     */
    public void setMaxLoss(int m_maxLoss) {
        this.m_maxLoss = m_maxLoss;
    }

    /**
     * @return the m_minLoss
     */
    public int getMinLoss() {
        return m_minLoss;
    }

    /**
     * @param m_minLoss the m_minLoss to set
     */
    public void setMinLoss(int m_minLoss) {
        this.m_minLoss = m_minLoss;
    }


}
