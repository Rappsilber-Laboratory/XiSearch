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

import rappsilber.ms.sequence.ions.CrosslinkedFragment;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.spectra.SpectraPeakCluster;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SpectraPeakMatchedFragment extends SpectraPeakAnnotation {
    /** the charge of the matched fragment */
    private int m_charge;
    private double m_Modification;
    /**
     * if this is matched for a missing mono-isotopic peak, then expected m/z
     * value of that peak is stored here.
     */
    private double m_missingPeakMZ = 0;
    /**
     * is this the primary explanation - meaning hopefully most likely explanation for the given peak
     */
    private boolean m_isPrimary = false;
    private double m_SupportLevel = 0;
    private SpectraPeakCluster m_cluster;
//    /**
//     * matched to which peak
//     */
//    private SpectraPeak m_assignedTo;
    /** a score for the match */
    private double m_MatchScore = 1;

    public SpectraPeakMatchedFragment(Fragment f,  int charge) {
        super("Fragment", f);
        m_charge = charge;
    }

    public SpectraPeakMatchedFragment(Fragment f,  int charge, SpectraPeakCluster cluster) {
        this(f, charge);
        m_cluster = cluster;
    }

    public SpectraPeakMatchedFragment(Fragment f,  int charge, double missingPeakMZ) {
        super("Fragment", f);
        m_charge = charge;
        m_missingPeakMZ = missingPeakMZ;
    }

    public SpectraPeakMatchedFragment(Fragment f,  int charge, double missingPeakMZ, SpectraPeakCluster cluster) {
        this(f, charge,missingPeakMZ);
        m_cluster = cluster;
    }

    public Fragment getFragment() {
        return (Fragment) m_value;
    }


    /**
     * @return the m_charge
     */
    public int getCharge() {
        return m_charge;
    }

    public double getMZ() {
        int charge = getCharge();
        return ((Fragment) m_value).getMZ(charge) + getModification()/charge;
    }

    public double getMass() {
        return ((Fragment) m_value).getNeutralMass()  + getModification();
    }

    public String getDescription() {
        if (getMissingPeakMZ()>0) {
            return "linear missing mono";
        } else  {
            return "linear" ;
        }
    }

    public boolean isLinear() {
        return !getFragment().isClass(CrosslinkedFragment.class);
    }

    public boolean matchedMissing() {
        return getMissingPeakMZ()>0;
    }


    public void setModification(double mass)  {
        m_Modification = mass;
    }

    public double getModification()  {
        return m_Modification;
    }

    /**
     * @return the missingPeakMZ
     */
    public double getMissingPeakMZ() {
        return m_missingPeakMZ;
    }

    /**
     * @param MissingPeakMZ the m_missingPeakMZ to set
     */
    public void setMissingPeakMZ(double MissingPeakMZ) {
        this.m_missingPeakMZ = MissingPeakMZ;
    }

//    /**
//     * @return the peak the fragment is assigned to
//     */
//    public SpectraPeak AssignedTo() {
//        return m_assignedTo;
//    }
//
//    /**
//     * @param AssignedPeak the peak the fragment is assigned to
//     */
//    public void AssignTo(SpectraPeak AssignedPeak) {
//        this.m_assignedTo = AssignedPeak;
//    }

//    public void deleteAssignment() {
//        if (m_assignedTo != null)
//            m_assignedTo.deleteAnnotation(this);
//    }

    @Override
    public SpectraPeakMatchedFragment clone() {
        SpectraPeakMatchedFragment f = new SpectraPeakMatchedFragment((Fragment)m_value, m_charge, m_missingPeakMZ);
        f.m_Modification = m_Modification;
//        f.m_assignedTo = m_assignedTo;
        f.m_MatchScore = m_MatchScore;
        return f;
    }

    @Override
    public void free() {
        super.free();
//        m_assignedTo = null;
    }

    /**
     * @return the whether this match is the primary match for this peak
     */
    public boolean isPrimary() {
        return m_isPrimary;
    }

    /**
     * sets or unsets this match to be the primary match for the given peak
     * @param m_isPrimary the m_isPrimary to set
     */
    public void setPrimary(boolean primary) {
        this.m_isPrimary = primary;
    }

    /**
     * sets this match to be the primary match for the given peak
     */
    public void setPrimary() {
        this.m_isPrimary = true;
    }

    /**
     * @return the m_cluster
     */
    public SpectraPeakCluster getCluster() {
        return m_cluster;
    }

    /**
     * @param m_cluster the m_cluster to set
     */
    public void setCluster(SpectraPeakCluster m_cluster) {
        this.m_cluster = m_cluster;
    }

    /**
     * @return the m_SupportLevel
     */
    public double getSupportLevel() {
        return m_SupportLevel;
    }

    /**
     * @param m_SupportLevel the m_SupportLevel to set
     */
    public void setSupportLevel(double m_SupportLevel) {
        this.m_SupportLevel = m_SupportLevel;
    }

}
