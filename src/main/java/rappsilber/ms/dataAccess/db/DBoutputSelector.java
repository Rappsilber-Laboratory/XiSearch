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
package rappsilber.ms.dataAccess.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.db.ConnectionPool;
import rappsilber.ms.dataAccess.output.AbstractResultWriter;
import rappsilber.ms.dataAccess.output.AbstractStackedResultWriter;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 * This is just a generic 
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DBoutputSelector extends AbstractStackedResultWriter {

    public DBoutputSelector(RunConfig config, ConnectionPool cp, int searchID) {
        AbstractResultWriter inner = null;
        try {
            Connection c = cp.getConnection();
            try {
                // a 
                c.createStatement().execute("Select * from raw_file limit 1");
                cp.free(c);
                inner = new XiDBWriterBiogrid(config, cp, searchID);
                Logger.getLogger(DBoutputSelector.class.getName()).log(Level.INFO, "Seems to be the xi2.5 style ");
            } catch (SQLException ex) {
            }
            
            if (inner == null) {
                try {
                    // a 
                    c.createStatement().execute("Select * from spectrum_source limit 1");
                    cp.free(c);
                    inner = new XiDBWriterBiogridXi3(config, cp, searchID);
                    Logger.getLogger(DBoutputSelector.class.getName()).log(Level.INFO, "Seems to be the xi3 style ");
                } catch (SQLException ex) {
                    Logger.getLogger(DBoutputSelector.class.getName()).log(Level.INFO, "Seems to be the old db style ");
                }
            }
            
            if (inner == null) {
                inner = new XiDBWriterCopySqlIndividualBatchIDs(config, cp, searchID);
            }
            
        } catch (SQLException ex) {
            
            Logger.getLogger(DBoutputSelector.class.getName()).log(Level.SEVERE, "could not open a connection to the database", ex);
            inner = new XiDBWriterCopySqlIndividualBatchIDs(config, cp, searchID);
        }

        setInnerWriter(inner);
        
    }
    
    
    @Override
    public void selfFinished() {
        return;
    }

    @Override
    public boolean selfWaitForFinished() {
        return true;
    }

    public void writeResult(MatchedXlinkedPeptide match) throws IOException {
        innerWriteResult(match);
    }
    
}
