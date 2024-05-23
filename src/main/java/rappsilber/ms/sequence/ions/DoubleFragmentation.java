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
package rappsilber.ms.sequence.ions;

import rappsilber.ms.sequence.Peptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class DoubleFragmentation extends Fragment implements SecondaryFragment{

    private static boolean m_enabled = false;

    protected DoubleFragmentation (Peptide peptide, short Start, short length, double weightDiff) {
        super(peptide,
            Start,
            length,
            weightDiff);
    }

    protected DoubleFragmentation () {}


    public static void setEnable(boolean enabled) {
        m_enabled = enabled;
    }

    public static boolean isEnabled() {
        return m_enabled;
    }

    public static boolean isDisabled() {
        return !m_enabled;
    }

   // public abstract getBaseFragments();

    public double getBaseSupportLevel() {
        return SUPPORT_DOUBLE_FRAGMENATION;
    }

}
