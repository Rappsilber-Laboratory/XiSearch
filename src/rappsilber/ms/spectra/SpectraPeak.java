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
package rappsilber.ms.spectra;

import rappsilber.ms.spectra.annotation.SpectraPeakAnnotation;
import java.util.ArrayList;
import java.util.Comparator;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.statistics.utils.UpdateableInteger;
import rappsilber.ms.statistics.utils.UpdateableLong;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SpectraPeak implements Comparable {
    /** the mass over charge ratio for the peak */
    private double m_mz;
    /** if the peak has a known charge, then it will be stored here */
    // TODO: must remove the usage of charge for individual peaks
    private int m_charge = -1;
    /** the intensity of the peak */
    private double m_Intensity;
    /** list of annotation for that peak */
    private ArrayList<SpectraPeakAnnotation> m_Annotaions = new ArrayList<SpectraPeakAnnotation>();

    private double m_Specificity=1;

    private UpdateableLong m_id = new UpdateableLong(-1);

    /** list of Annotation refering to matched fragments */
    private ArrayList<SpectraPeakMatchedFragment> m_MatchedFragments = new ArrayList<SpectraPeakMatchedFragment>();

    /** a comparator, that compares peaks based on their intensity */
    public static Comparator<SpectraPeak> INTENSITY_COMPARATOR = new Comparator<SpectraPeak>() {
        public int compare(SpectraPeak o1, SpectraPeak o2) {
            return Double.compare(o1.m_Intensity, o2.m_Intensity);
        }
    };




    /** The tolerance used for comparing two peaks. This is set at the start of the search session
        so we don't deal with it in terms of individual peaks at the moment
     */
    private ToleranceUnit m_tolerance = null;

    //<editor-fold desc="constructors">
    //----------------------------------
    //constructors

    /** 
     * creates a new peak with the given mz and intensity
     * @param mz
     * @param intensity
     */
    public SpectraPeak(double mz, double intensity) {
        m_mz = mz;
        m_Intensity = intensity;
        m_tolerance = new ToleranceUnit("0", "da");
    }

    /**
     * creates a new peak with the given mz, charge, intensity and tolerance
     * @param mz
     * @param charge
     * @param intensity
     * @param tolerance
     */
    public SpectraPeak(double mz, int charge, double intensity, ToleranceUnit tolerance) {
        m_mz = mz;
        m_charge = charge;
        m_Intensity = intensity;
        m_tolerance = tolerance;
    }

    /**
     * creates a new peak with the given mz, intensity and tolerance
     * @param mz
     * @param intensity
     * @param tolerance
     */
    public SpectraPeak(double mz, double intensity, ToleranceUnit tolerance)  {
        m_mz = mz;
        m_Intensity = intensity;
        m_tolerance = tolerance;
    }

    //</editor-fold>

    /**
     * @return the mass over charge ratio for the peak
     */
    public double getMZ() {
        return m_mz;
    }

    /**
     * Set the mass over charge ratio for the peak
     * @param mz the mass over charge ratio
     */
    public void setMZ(double mz) {
        this.m_mz = mz;
    }

    /**
     * @return the peak intensity
     */
    public double getIntensity() {
        return m_Intensity;
    }

    /**
     * @param Intensity the Intensity to set
     */
    public void setIntensity(double Intensity) {
        this.m_Intensity = Intensity;
    }

    /**
     * compares two peaks under the tolerance constrain.
     * @param o another peak
     * @return -1 if o has a higher m/z value then this + tolerance <br/>
     *          0 if o and this have the same mz within the tolerance <br\>
     *          1 if o has a smaller m/z value then this - tolerance
     */
    @Override
    public int compareTo(Object o) {
        return m_tolerance.compare(m_mz, ((SpectraPeak)o).m_mz);
    }


    /**
     * Add an annotation to the peak
     * @param a
     */
    public void annotate(SpectraPeakAnnotation a) {
        if (!m_Annotaions.contains(a)) {
            this.m_Annotaions.add(a);
            if (a instanceof SpectraPeakMatchedFragment) {
                this.m_MatchedFragments.add((SpectraPeakMatchedFragment)a);
//                ((SpectraPeakMatchedFragment)a).AssignTo(this);
            }
        }
    }

    /**
     * returns a list of fragments, that could have produced this peak
     * @return list of matched fragments
     */
    public ArrayList<Fragment> getMatchedFragments() {
        ArrayList<Fragment> frags = new ArrayList<Fragment>(m_MatchedFragments.size());
        for (SpectraPeakMatchedFragment mf : m_MatchedFragments ) {
            frags.add(mf.getFragment());
        }
        return frags;
    }

    private Fragment topFrags = null;

    public Fragment getTopMatchedFragment(MatchedFragmentCollection mfc) {
        if (topFrags == null) {
            ArrayList<Fragment> frags = new ArrayList<Fragment>(m_MatchedFragments.size());
            for (SpectraPeakMatchedFragment mf : m_MatchedFragments ) {
                frags.add(mf.getFragment());
            }
        }
        return topFrags;
    }

    /**
     * returns a list of annotations, that represent a matched fragment
     * @return
     */
    public ArrayList<SpectraPeakMatchedFragment> getMatchedAnnotation() {
        ArrayList<SpectraPeakMatchedFragment> ret = new ArrayList<SpectraPeakMatchedFragment>();
        for (SpectraPeakMatchedFragment mf : m_MatchedFragments ) {
            ret.add(mf);
        }
        return ret;
    }

    /**
     * Add an annotation to the peak
     * @param a
     */
    public void deleteAnnotation(SpectraPeakAnnotation a) {
        if (m_Annotaions.contains(a))
            this.m_Annotaions.remove(a);
        if (m_MatchedFragments.contains(a)) {
            m_MatchedFragments.remove(a);
        }
    }

    public void deleteAnnotation(Fragment f) {

        for (SpectraPeakMatchedFragment mf : m_MatchedFragments) {
            if (mf.getFragment() == f) {
                m_MatchedFragments.remove(mf);
                m_Annotaions.remove(mf);
                break;
            }
        }
    }


    /**
     * returns whether the peak has been anotated at least ones
     * @return
     */
    public boolean isAnnotated() {
        return m_Annotaions.size() > 0;
    }

    /**
     * returns whether the peak has been annotated at least ones
     * @param a 
     * @return
     */
    public boolean hasAnnotation(SpectraPeakAnnotation a) {
        return m_Annotaions.contains(a);
    }

    /**
     * @return -1 if the charge was not defined yet; else the charge of the ion
     */
    public int getCharge() {
        return m_charge;
    }

    /**
     * @param charge the charge of the ion represented by this peak
     */
    // TODO: must remove the ability to set/get charge for individual peaks
    public void setCharge(int charge) {
        this.m_charge = charge;
    }

    /**
     * the tolerance, that is used to compare m/z values
     * @return
     */
    public ToleranceUnit getTolerance() {
        return m_tolerance;
    }

    /**
     * sets the tolerance, that is used to compare m/z values
     * @param tolerance 
     */
    public void setTolerance(ToleranceUnit tolerance) {
        m_tolerance = tolerance;
    }

    /**
     * returns all annotations, that where assigned to this peak
     * @return
     */
    ArrayList<SpectraPeakAnnotation> getAllAnnotations() {
        return m_Annotaions;
    }

    /**
     * returns a new peak that has the same intensity and m/z value as this one
     */
    @Override
    public SpectraPeak clone() {
        // create a new peak
        SpectraPeak p = new SpectraPeak(m_mz, m_charge, m_Intensity, m_tolerance);
//        // copy annotations
//        for (SpectraPeakAnnotation a : getAllAnnotations()) {
//            p.annotate(a.clone());
//        }
        p.m_id = m_id;

        return p;

    }

    public SpectraPeak cloneComplete() {
        // create a new peak
        SpectraPeak p = new SpectraPeak(m_mz, m_charge, m_Intensity, m_tolerance);
        // copy annotations
        for (SpectraPeakAnnotation a : getAllAnnotations()) {
            p.annotate(a.clone());
        }
        p.m_id = m_id;

        return p;

    }

    /**
     * just a small function, in case the gc has trouble freeing up the resources
     */
    public void free() {
        for (SpectraPeakAnnotation spa : m_Annotaions)
            spa.free();

        m_Annotaions.clear();
        m_Annotaions  = null;
        m_MatchedFragments.clear();
        m_MatchedFragments = null;
        m_tolerance = null;
    }


    public long getID() {
        return m_id.value;
    }

    public void setID(long id) {
        m_id.value = id;
    }

    @Override
    public String toString(){
        String sr = "";
        sr += Util.fourDigits.format(this.m_mz) + "\t\t"+ Util.fourDigits.format(this.m_Intensity);
        return sr;
    }

    public String toString(java.text.NumberFormat format){
        String sr = "";
        sr += format.format(this.m_mz) + "\t\t"+ format.format(this.m_Intensity);
        return sr;
    }

}
