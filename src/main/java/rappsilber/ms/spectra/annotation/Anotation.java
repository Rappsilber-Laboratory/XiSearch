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
package rappsilber.ms.spectra.annotation;

import java.util.LinkedList;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class Anotation {

    private static LinkedList<Anotation> m_registered = null;

    /**
     * registers a annotation class for automatic annotation of a spectra
     * @param a
     */
    public static void RegisterAnotaion(Anotation a) {
        if (m_registered == null) {
            m_registered = new LinkedList<Anotation>();
        }
        m_registered.add(a);
    }


    /**
     * use all registered annotation classes to annotate a spectrum
     * @param s the spectrum to annotate
     */
    public static void anotateAll(Spectra s)  {
        for (Anotation a : m_registered) {
            a.anotate(s);
        }
    }

    /**
     * annotate the spectrum
     * @param s
     */
    public abstract void anotate(Spectra s);

    /**
     * annotate the spectrum under the condition, that it was matched to the given peptide
     * @param s
     * @param Peptide
     */
    public abstract void anotate(Spectra s, Peptide Peptide);

    /**
     * annotate the spectrum under the condition, that it was matched to the given peptides
     * @param s
     * @param Peptide1
     * @param Peptide2
     */
    public abstract void anotate(Spectra s, Peptide Peptide1, Peptide Peptide2);

}
