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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.spectra.annotation.SpectraPeakAnnotation;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.utils.SortedLinkedList;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SpectraPeakCluster extends ArrayList<SpectraPeak> implements Comparable,  PeakList{
    private static final long serialVersionUID = 3379426633729200149L;

    /** the monoisotopic peack for that cluster */
    private SpectraPeak m_monoisotopic;
    /** the added intensity of each peak, that belongs to the cluster */
    private double m_SummedIntensity = 0;
    /** the start of the cluster */
    private double m_mz;
    private double mass;
    /** the chargestate of the cluster */
    private int m_charge;
    /** any comparison of m/z-values should be done through this toleranceunit */
    private ToleranceUnit m_tollerance;
    /** this score is used to define how good th cluster fitts to the averagin-cluster */
    private double        m_AveraginScore;
    /**
     * the id by which this cluster will be referred to within the database
     */
    private long m_dbID = -1;

    protected boolean isExtended = false;
    private TreeMap<Double,SpectraPeak> m_tree = new TreeMap<Double, SpectraPeak>();


    /**
     * creates a new empty cluster
     * @param t the tolerance unit used to compare m/z values
     */
    public SpectraPeakCluster(ToleranceUnit t) {
        m_tollerance = t;
    }

    /**
     * define the monoisotopic peak
     * @param p
     */
    public void setMonoIsotopic(SpectraPeak p) {
        m_monoisotopic = p;
        setMZ(p.getMZ());
        setCharge(p.getCharge());
    }

    /** what is the monoisotopic peak
     * @return
     */
    public SpectraPeak getMonoIsotopic() {
        return m_monoisotopic;
    }


    @Override
    public boolean add(SpectraPeak p) {
        double mz = p.getMZ();
        m_tree.put(mz, p);
        boolean ret = super.add(p);

        m_SummedIntensity += p.getIntensity();
        if (size()>1) {
            if (get(size()-1).getMZ()<mz)
                java.util.Collections.sort(this);
        }else if (ret ) {
            setMonoIsotopic(p);
        }
        return ret;
    }

    /**
     * adds a peak at the given index - does not check whether it makes sense
     * @param index
     * @param p
     */
    @Override
    public void add(int index, SpectraPeak p) {
        super.add(index, p);
        m_tree.put(p.getMZ(), p);
        m_SummedIntensity += p.getIntensity();
        if (index == 0) {
            setMonoIsotopic(p);
        }
    }

    @Override
    public SpectraPeak remove(int index) {
        SpectraPeak p = super.remove(index);
        m_tree.remove(p.getMZ());
        m_SummedIntensity -= p.getIntensity();
        if (index == 0) {
            setMonoIsotopic(get(0));
        }
        return p;
    }

    /**
     * removes a SpectraPeak from the list of peaks
     * @param p
     * @return
     */
    public boolean remove(SpectraPeak p) {
        if (super.remove(p)) {
            m_SummedIntensity -= p.getIntensity();

            if (p == getMonoIsotopic())
                if (size() > 0)
                    setMonoIsotopic(get(0));
                else
                    setMonoIsotopic(null);

            m_tree.remove(p.getMZ());

            return true;
        } else
            return false;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof SpectraPeak)
            return remove((SpectraPeak) o);
        else
            return false;
    }


    /**
     * @return the mz-value, where the cluster starts
     */
    public double getMZ() {
        return m_mz;
    }

    /**
     * @param mz the mz-value, where the cluster starts
     */
    public void setMZ(double mz) {
        this.m_mz = mz;
        this.mass = getMZ()*getCharge();
    }

    /**
     * @return the mass of the fragment - that has produced this cluster
     */
    public double getMass() {
        return mass;
    }

    /**
     * the charge-state of the fragment, that produced this cluster
     * @return the charge
     */
    public int getCharge() {
        return m_charge;
    }

    /**
     * returns the collective intensity of all members of the cluster
     * @return
     */
    public double getSummedIntensity() {
        return m_SummedIntensity;
    }

    /**
     * @param charge the charge to set
     */
    public void setCharge(int charge) {
        this.m_charge = charge;
        this.mass = getMZ()*getCharge();
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
        
        if (o instanceof Double)
            return compareTo(((Double)o).doubleValue());

        int ret = m_tollerance.compare(m_mz, ((SpectraPeakCluster)o).m_mz);
        if (ret == 0)
            if (size() < ((SpectraPeakCluster)o).size())
                return 1;
            else if (size() > ((SpectraPeakCluster)o).size())
                return -1;
            else
                return 0;
        else
            return ret;
    }

    /**
     * returns whether this cluster has a higher (return &gt; 0) m/z value, a
     * smaller one (&lt; 0) or the same m/z value as the given one
     * @param mz
     * @return
     */
    public int compareTo(double mz) {
        int ret = m_tollerance.compare(m_mz, mz);
        return ret;
    }


    /**
     * returns the monoisotopic peak, that has the intensities of all peaks
     * added on top of it
     * @return new SpectraPeak
     */
    public SpectraPeak toPeak() {
        // create a single peak for this cluster
        SpectraPeak peak = new SpectraPeak(m_mz, m_charge, m_SummedIntensity, m_tollerance);

        //transfer the anotations
        for (SpectraPeak sp : this) {
            for (SpectraPeakAnnotation a : sp.getAllAnnotations()) {
                if (a instanceof SpectraPeakMatchedFragment)
                    peak.annotate(((SpectraPeakMatchedFragment)a).clone());
                else
                    peak.annotate(a);
            }
        }
        return peak;
    }

    /**
     * returns the monoisotopic peak
     * @param sumIntensities <table><tr><td>true: the peak has the combined intensities of all peaks;</td></tr>
     *                       <tr><td>false: the peak has the highest observed intensity of this cluster</td></tr></table>
     * @return new SpectraPeak
     */
    public SpectraPeak toPeak(boolean sumIntensities) {
        if (sumIntensities)
            return toPeak();
        else
            return toTopPeak();
            //return get(0).clone();
    }


    /**
     * returns the singly charged monoisotopic peak
     * @param sumIntensities <table><tr><td>true: the peak has the combined intensities of all peaks;</td></tr>
     *                       <tr><td>false: the peak has the highest observed intensity of this cluster</td></tr></table>
     * @param deCharge calculate the m/z-value for the singly charge fragment
     * @return new SpectraPeak
     */
    public SpectraPeak toPeak(boolean deCharge, boolean sumIntensities) {
        SpectraPeak p;

        p = toPeak(sumIntensities);
        if (deCharge) {
            p.setMZ((p.getMZ() - Util.PROTON_MASS) * p.getCharge() + Util.PROTON_MASS);
        }
 
        return p;

    }


    /**
     * returns the monoisotopic peak. The intensity of the peak is the one of
     * the most intents peak within the cluster
     * @return new SpectraPeak
     */
    public SpectraPeak toTopPeak() {
        // create a single peak for this cluster

        SpectraPeak peak = new SpectraPeak(m_mz, m_charge, 0, m_tollerance);
        double intensity = 0;

        //transfer the anotations
        for (SpectraPeak sp : this) {
            double spIntens = sp.getIntensity();
            if (spIntens > intensity) {
                peak.setIntensity(spIntens);
                intensity = spIntens;
            }

            for (SpectraPeakAnnotation a : sp.getAllAnnotations()) {
                if (a instanceof SpectraPeakMatchedFragment)
                    peak.annotate(((SpectraPeakMatchedFragment)a).clone());
                else
                    peak.annotate(a);
            }

        }
        return peak;
    }

    /**
     * creates a new Cluster, that contains all of the peaks of this cluster
     * @return
     */
    @Override
    public Object clone() {
        SpectraPeakCluster spc = new SpectraPeakCluster(m_tollerance);
        spc.m_monoisotopic = m_monoisotopic;
        spc.m_SummedIntensity = this.m_SummedIntensity;
        spc.m_mz = m_mz;
        spc.m_charge = m_charge;
        spc.addAll(this);
        return spc;

    }

    /**
     * clones the cluster onto another spectra
     * @param s
     * @return
     */
    public SpectraPeakCluster clone(Spectra s) {
        SpectraPeakCluster cn = new SpectraPeakCluster(m_tollerance);
        
        for (SpectraPeak p : this) {
            SpectraPeak mp = s.getPeakAt(p.getMZ());
            if (mp != null)
                cn.add(mp);
        }

        cn.setCharge(getCharge());
        cn.setMZ(getMZ());
        SpectraPeak mi = s.getPeakAt(getMonoIsotopic().getMZ());
        if (mi != null)
            cn.setMonoIsotopic(mi);
        else
            cn.setMonoIsotopic(getMonoIsotopic().clone());

        return cn;
    }

    public SpectraPeakCluster clone(Spectra s, HashMap<SpectraPeak,SpectraPeak> oldNew) {
        SpectraPeakCluster cn = new SpectraPeakCluster(m_tollerance);

        for (SpectraPeak p : this) {
            SpectraPeak mp = oldNew.get(p);
            if (mp != null)
                cn.add(mp);
        }

        SpectraPeak mi = s.getPeakAt(getMonoIsotopic().getMZ());
        if (mi != null)
            cn.setMonoIsotopic(mi);
        else
            cn.setMonoIsotopic(getMonoIsotopic().clone());

        cn.setCharge(getCharge());
        cn.setMZ(getMZ());

        return cn;
    }

    /** to help the gc this function removes all links to peaks */
    public void free() {
        m_monoisotopic = null;
        m_tollerance = null;
    }

    /**
     * sorts all peaks and returns the intensity of the peak in
     * the middle of the list 
     * @return the median intensity
     */
    public double getMedianIntensity() {
        double dPos = size()/2.0;
        int iPos = (int)dPos;
        if ((double)iPos == dPos)
            return this.get(iPos).getIntensity();
        else
            return (this.get(iPos).getIntensity()
                    + this.get(iPos + 1).getIntensity())/2;
    }

    /**
     * returns a peak at the given m/z value
     * @param mz
     * @return the peak at the given m/z value
     */
    public SpectraPeak getPeakAt(double mz) {
        for (SpectraPeak p : this)
            if (m_tollerance.compare(p.getMZ(),mz) == 0)
                return p;
        return null;
    }

    /**
     * average intensity of peaks within this cluster
     * @return the median intensity
     */
    public double getMeanIntensity() {
        double sumIntens = 0;
        for (SpectraPeak p : this)
            sumIntens += p.getIntensity();
        return sumIntens/size();
    }

    /**
     * returns the highest intensity within this cluster
     * @return
     */
    public double getMaxIntensity() {
        double max = 0;
        for (SpectraPeak p : this)
            if (max < p.getIntensity())
                max = p.getIntensity();
        return max;
    }

    /**
     * returns the index of the most intents peak within this cluster
     * @return
     */
    public double getMaxIntensityIndex() {
        double max = 0;
        int maxPos = -1;
        int pos = 0;
        for (SpectraPeak p : this) {
            if (max < p.getIntensity()) {
                max = p.getIntensity();
                maxPos = pos;
            }
            pos++;
        }
        return maxPos;
    }

    /**
     * returns a list of peaks, that have at least the given intensity
     * @param minIntensity
     * @return
     */
    public Collection<SpectraPeak> getPeaks(double minIntensity) {
        return this;
    }

    /**
     * returns the [number] highest intents peaks within this cluster
     * @param number
     * @return
     */
    public Collection<SpectraPeak> getTopPeaks(int number) {
        SortedLinkedList<SpectraPeak> ret = new SortedLinkedList<SpectraPeak>(new Comparator<SpectraPeak>() {
                            public int compare(SpectraPeak o1, SpectraPeak o2) {
                                return (o1.getIntensity() > o2.getIntensity() ? -1 :
                                    o1.getIntensity() < o2.getIntensity() ? 1 : 0);
                            }
                        }
                );

        // add the non-isotopic peaks
        for (SpectraPeak p : this) {
            ret.add(p);
        }

        return ret.subList(0, Math.min(number,size()));
    }

    /**
     * @return the AveraginScore
     */
    public double getAveraginScore() {
        return m_AveraginScore;
    }

    /**
     * @param AveraginScore the m_AveraginScore to set
     */
    public void setAveraginScore(double AveraginScore) {
        this.m_AveraginScore = AveraginScore;
    }

    @Override
    public boolean hasPeakAt(double mz) {
        return m_tree.containsKey(mz);
    }

    /**
     * @return the m_dbID
     */
    public long getDBid() {
        return m_dbID;
    }

    /**
     * @param m_dbID the m_dbID to set
     */
    public void setDBid(long m_dbID) {
        this.m_dbID = m_dbID;
    }

    @Override
    public String toString() {
        return getMZ()+"*"+getCharge()+"["+size()+"]";
    }

    /**
     * @return the isExtended
     */
    public boolean isExtended() {
        return isExtended;
    }

    /**
     * @param isExtended the isExtended to set
     */
    public void setExtended(boolean isExtended) {
        this.isExtended = isExtended;
    }


}
