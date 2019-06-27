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

import java.util.Collection;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public interface PeakList extends Iterable<SpectraPeak> {
    /**
     * the intensity of the peak, that if all peaks where sorted by intensity
     * would be in the middle
     * @return
     */
    double getMedianIntensity();
    /**
     * returns the average intensity of all peaks
     * @return
     */
    double getMeanIntensity();
    /**
     * returns the highest observed intensity within this list
     * @return
     */
    double getMaxIntensity();
    /**
     * returns the index of the highest observed intensity within this list
     * @return
     */
    double getMaxIntensityIndex();
    /**
     * returns the peak that is at the given position
     * @param mz
     * @return the peak or null if none was found
     */
    SpectraPeak getPeakAt(double mz);

    /**
     * Checks, whether the PeakList has a peak exactly at the given m/z
     * @param mz the m/z to check for a peak
     * @return true: the list contains a peak exactly at the given m/z; false: otherwise
     */
    boolean hasPeakAt(double mz);
    /**
     * returns a list of all peaks that are more intents then the given intensity
     * @param minIntensity
     * @return
     */
    Collection<SpectraPeak> getPeaks(double minIntensity);
    /**
     * returns the n most intents peaks
     * @param number
     * @return
     */
    Collection<SpectraPeak> getTopPeaks(int number);
}
