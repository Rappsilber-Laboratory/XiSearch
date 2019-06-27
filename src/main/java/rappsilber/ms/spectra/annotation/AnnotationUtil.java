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

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.ms.sequence.ions.DoubleFragmentation;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class AnnotationUtil {


    public static ArrayList<SpectraPeakMatchedFragment> getReducedAnnotation(SpectraPeak peak, MatchedFragmentCollection mfc) {
        ArrayList<SpectraPeakMatchedFragment> spmf = peak.getMatchedAnnotation();

    //            if ((int) peak.getMZ() == 330)
    //                System.out.println("found");

        ArrayList<SpectraPeakMatchedFragment> spmfReduced = new ArrayList<SpectraPeakMatchedFragment>();

        if (spmf.size() > 0) {
            // all basic non-lossy explanations get through
            for (SpectraPeakMatchedFragment mf: spmf) {
                Fragment f = mf.getFragment();
                if (!mf.matchedMissing() && !f.isClass(DoubleFragmentation.class) && !f.isClass(Loss.class)) {
                    spmfReduced.add(mf);
                    // is it backed up by losses
                    if (mfc.getMatchedFragmentGroup(f, mf.getCharge()).getLosses().size()>0) {
                        // use it to estimate the error-curve
                    }
                }
            }

            // only if non found look for losses , that have nonlossy matches
            if (spmfReduced.size() == 0)
                for (SpectraPeakMatchedFragment mf: spmf) {
                    Fragment f = mf.getFragment();
                    int charge = mf.getCharge();
                    try {
                    if (!mf.matchedMissing() && !f.isClass(DoubleFragmentation.class) && f.isClass(Loss.class)) {
                        if (mfc.getMatchedFragmentGroup(f, charge).isBaseFragmentFound())
                            spmfReduced.add(mf);
                    }
                    } catch(NullPointerException npe) {
                        Logger.getLogger(rappsilber.ms.spectra.annotation.AnnotationUtil.class.getName()).log(Level.SEVERE,"NPE",npe);
                     //   System.exit(1);
                    }
                }


            // next nonlossy double fragmentations
            if (spmfReduced.size() == 0)
                for (SpectraPeakMatchedFragment mf: spmf) {
                    Fragment f = mf.getFragment();
                    int charge = mf.getCharge();
                    if (!mf.matchedMissing() && f.isClass(DoubleFragmentation.class) && !f.isClass(Loss.class)) {
                        spmfReduced.add(mf);
                    }
                }



            // all basic non-lossy explanations get through
            for (SpectraPeakMatchedFragment mf: spmf) {
                Fragment f = mf.getFragment();
                if (!f.isClass(DoubleFragmentation.class) && !f.isClass(Loss.class)) {
                    spmfReduced.add(mf);
                }
            }

            // only if non found look for losses , that have nonlossy matches
            if (spmfReduced.size() == 0)
                for (SpectraPeakMatchedFragment mf: spmf) {
                    Fragment f = mf.getFragment();
                    int charge = mf.getCharge();
                    if (!f.isClass(DoubleFragmentation.class) && f.isClass(Loss.class)) {
                        if (mfc.getMatchedFragmentGroup(f, charge).isBaseFragmentFound())
                            spmfReduced.add(mf);
                    }
                }


            // next nonlossy double fragmentations
            if (spmfReduced.size() == 0)
                for (SpectraPeakMatchedFragment mf: spmf) {
                    Fragment f = mf.getFragment();
                    int charge = mf.getCharge();
                    if (f.isClass(DoubleFragmentation.class) && !f.isClass(Loss.class)) {
                        spmfReduced.add(mf);
                    }
                }

            // next lossy double fragmentation
            if (spmfReduced.size() == 0)
                for (SpectraPeakMatchedFragment mf: spmf) {
                    Fragment f = mf.getFragment();
                    int charge = mf.getCharge();
                    if (f.isClass(DoubleFragmentation.class) && f.isClass(Loss.class)) {
                        if (mfc.getMatchedFragmentGroup(f, charge).isBaseFragmentFound())
                            spmfReduced.add(mf);
                    }
                }

            // losses
            if (spmfReduced.size() == 0)
                for (SpectraPeakMatchedFragment mf: spmf) {
                    Fragment f = mf.getFragment();
                    int charge = mf.getCharge();
                    if (!f.isClass(DoubleFragmentation.class) && f.isClass(Loss.class)) {
                        spmfReduced.add(mf);
                    }
                }
        }
        return spmfReduced;
    }

}
