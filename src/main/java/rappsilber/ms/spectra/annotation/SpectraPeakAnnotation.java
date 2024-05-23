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

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SpectraPeakAnnotation {

    public static final SpectraPeakAnnotation matched_ion = new SpectraPeakAnnotation("matched_ion");
    /**
     * annotates a peak as belonging to an isotop-cluster
     */
    public static final SpectraPeakAnnotation isotop = new SpectraPeakAnnotation("isotop");
    /**
     * annotates a peak as as the monoisotopic peak of an isotop-cluster
     */
    public static final SpectraPeakAnnotation monoisotop = new SpectraPeakAnnotation("monoisotop");
    /**
     * should be used after matching .e.g just before writing out a match, to annotate unmatched peaks.<br/>
     * Just so we can easily highlight them as such in the viewer
     */
    public static final SpectraPeakAnnotation unmatched = new SpectraPeakAnnotation("unmatched");

    public static final SpectraPeakAnnotation virtual = new SpectraPeakAnnotation("virtual");


    protected String m_Annotation;
    protected Object m_value = null;


    public SpectraPeakAnnotation(String Annotation) {
        m_Annotation = Annotation;
    }

    public SpectraPeakAnnotation(String Annotation, Object value) {
        m_Annotation = Annotation;
        m_value      = value;
    }

    public void free() {
        m_value = null;
        m_Annotation = null;
    }
    
    public SpectraPeakAnnotation clone() {
        if (m_value != null) {
            return new SpectraPeakAnnotation(m_Annotation,m_value);
        } else {
            return this;
        }

    }


}
