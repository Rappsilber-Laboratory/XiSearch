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
import java.util.TreeMap;
import rappsilber.ms.Range;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.spectra.annotation.SpectraPeakAnnotation;
import rappsilber.utils.SortedLinkedList;

/**
 * A list of SpectraPeackCluster, that also provides access to the Clusters based
 * on there m/z values.
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SpectraPeakClusterList extends SortedLinkedList<SpectraPeakCluster> {
    private static final long serialVersionUID = -9146736574728848016L;
    TreeMap<Double,ArrayList<SpectraPeakCluster>> m_mzAccess = new TreeMap<Double, ArrayList<SpectraPeakCluster>>();
    private ToleranceUnit m_tolerance;

    /**
     * creates a new list of SpectraPeakCluster
     * @param tolerance
     */
    public SpectraPeakClusterList(ToleranceUnit tolerance) {
        m_tolerance = tolerance;
    }

    @Override
    public boolean add(SpectraPeakCluster spc) {
        Double mz = spc.getMZ();
        ArrayList<SpectraPeakCluster> list;

        // add it to the list
        boolean ret = super.add(spc);

        // and register it for acces via m/z
        if ((list = m_mzAccess.get(mz)) == null) {
            list = new ArrayList<SpectraPeakCluster>();
            m_mzAccess.put(mz, list);
        }
        list.add(spc);
        return ret;
    }

    @Override
    public SpectraPeakCluster remove(int pos) {
        SpectraPeakCluster spc = super.remove(pos);
        if (spc != null) {
            ArrayList<SpectraPeakCluster> mz = m_mzAccess.get(spc.getMZ());
            mz.remove(spc);
            if (mz.size() == 0)
                m_mzAccess.remove(spc.getMZ());
        }
        return spc;
    }

    /**
     * removes a cluster from the list
     * @param spc
     * @return
     */
    public boolean remove(SpectraPeakCluster spc) {
        boolean ret = super.remove(spc);
        if (ret)  {
            ArrayList<SpectraPeakCluster> mz = m_mzAccess.get(spc.getMZ());
            mz.remove(spc);
            if (mz.size() == 0)
                m_mzAccess.remove(spc.getMZ());
        }
        return ret;

    }

    /**
     * removes a cluster from the list
     * @param o
     * @return
     */
    @Override
    public boolean remove(Object o){
        if (o instanceof SpectraPeakCluster)
            return remove((SpectraPeakCluster)o);
        else
            return false;
    }

    /**
     * get the list of all clusters, that start at the given m/z value. <br/>
     * The m/z check is done via the ToleranceUnit
     * @param mz
     * @return
     * @see setTolerance(ToleranceUnit tolerance)
     */
    public ArrayList<SpectraPeakCluster> get(double mz) {
        ArrayList<SpectraPeakCluster> ret = new ArrayList<SpectraPeakCluster>();
        Range r = m_tolerance.getRange(mz);
        for (Collection<SpectraPeakCluster> c : m_mzAccess.subMap(r.min,r.max).values()){
            ret.addAll(c);
        }
        return ret;
    }

    /**
     * returns wether there is any cluster at the given m/z-value
     * @param mz
     * @return
     * @see setTolerance(ToleranceUnit tolerance)
     */
    public boolean hasCluster(double mz) {
        Range r = m_tolerance.getRange(mz);
        return m_mzAccess.subMap(r.min, r.max).size() > 0;
    }

    /**
     * returns wether there is any cluster at the given m/z-value, that has the
     * given charge
     * @param mz 
     * @param charge requested chargestate of the cluster
     * @return
     * @see setTolerance(ToleranceUnit tolerance)
     */
    public boolean hasCluster(double mz,int charge) {
        ArrayList<SpectraPeakCluster> ret = new ArrayList<SpectraPeakCluster>();
        Range r = m_tolerance.getRange(mz);
        for (Collection<SpectraPeakCluster> c : m_mzAccess.subMap(r.min,r.max).values()){
            for (SpectraPeakCluster spc : c)
                if (spc.getCharge() == charge) {
                    return true;
                }
        }
        return false;
    }

    /**
     * returns wether there is any cluster at the given m/z-value, that has the
     * given charge and at least the given intensity
     * @param mz
     * @param charge requested chargestate of the cluster
     * @param minIntesity  minimal intesity of the cluster
     * @return
     * @see setTolerance(ToleranceUnit tolerance)
     */
    public boolean hasCluster(double mz,int charge, double minIntesity) {
        ArrayList<SpectraPeakCluster> ret = new ArrayList<SpectraPeakCluster>();
        Range r = m_tolerance.getRange(mz);
        for (Collection<SpectraPeakCluster> c : m_mzAccess.subMap(r.min,r.max).values()){
            for (SpectraPeakCluster spc : c)
                if (spc.getCharge() == charge && spc.getSummedIntensity() > minIntesity) {
                    return true;
                }
        }
        return false;
    }

    /**
     * returns a list of all clusters, that contain the given peak
     * @param mz
     * @return
     */
    public ArrayList<SpectraPeakCluster> clusterHavingPeak(double mz) {
        ArrayList<SpectraPeakCluster> ret = new ArrayList<SpectraPeakCluster>(this.size());
        for (SpectraPeakCluster spc : this)
            if (spc.hasPeakAt(mz))
                ret.add(spc);
        return ret;
    }

    /**
     * returns a the number of cluster, that contain the given peak
     * @param mz
     * @return
     */
    public int countClusterHavingPeak(double mz) {
        int ret = 0;
        for (SpectraPeakCluster spc : this)
            if (spc.hasPeakAt(mz))
                ret ++;
        return ret;
    }

    /**
     * @return the tolerance that is used for comparing m/z values
     */
    public ToleranceUnit getTolerance() {
        return m_tolerance;
    }

    /**
     * @param tolerance the Tolerance that is used for comparing m/z values
     */
    public void setTolerance(ToleranceUnit tolerance) {
        this.m_tolerance = tolerance;
    }

    @Override
    public void clear() {
        for (SpectraPeakCluster spc : this) {
            for (SpectraPeak sp : spc) {
                sp.deleteAnnotation(SpectraPeakAnnotation.isotop);
                sp.deleteAnnotation(SpectraPeakAnnotation.monoisotop);
            }
        }
        m_mzAccess.clear();
        super.clear();
        //m_tolerance = null;
    }

}
