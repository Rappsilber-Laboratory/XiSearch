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
package rappsilber.ms.dataAccess;

import java.util.ArrayList;
import java.util.HashMap;
import rappsilber.ms.spectra.Spectra;

/**
 * Base class for acces to spectras that can handle sequential and
 * indexed access to thwe stored elements
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class IndexedSpectraAccess extends AbstractSpectraAccess{

    /**
     * class that handles the index for the datastorage(presumatly file)
     */
    public class FileIndexList extends HashMap<String,HashMap<Integer,Long>> {
        private static final long serialVersionUID = 6896729690614827448L;

        /** return a prevoisly stored location for a spectra
         * @param run run of the scan
         * @param scan scan number of the scan
         * @return
         */
        public long getFilePosition(String run, int scan) {
            return this.get(run).get(scan);
        }

        /** add an index for a spectra
         * @param run run of the scan
         * @param scan scan number of the scan
         * @param filePos where to find it in the file
         */
        public void addPosition(String run, int scan, long filePos) {
            HashMap<Integer,Long> scan2pos = this.get(run);
            if (scan2pos == null) {
                scan2pos = new HashMap<Integer, Long>();
                this.put(run,scan2pos);
            }
            scan2pos.put(scan, filePos);
        }
    }

    /**
     * the index for accessing spectra based on run and scan
     */
    protected FileIndexList index = new FileIndexList();

    /**
     * Read a spectra for a scan number
     * can return more then one spectra, if there is more then one result possible
     *
     * @return
     */
    protected abstract ArrayList<Spectra> readScan();

    /**
     * go to a given postion within the file
     *
     * @param pos the next position to be read
     * @return true if position set; flase otherwise
     */
    protected abstract boolean seek(long pos);

    /**
     * create the index for the file
     * @return number of scans in the file
     */
    public abstract int IndexFile();

    
    /**
     * reads a scan and returns the Spectra from it (or more in case of
     * uncertain charge states)
     * @param run
     * @param scan
     * @return one spectra per charge state
     */
    public ArrayList<Spectra> getSpectra(String run, int scan) {
        if (seek(index.getFilePosition(run, scan))) {
            return readScan();
        } else {
            return null;
        }

    }

    /**
     * returns the number of scans within the spectra
     * @return number of scans
     */
    public abstract int getScanCount();

}
