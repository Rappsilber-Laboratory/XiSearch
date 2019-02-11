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
package rappsilber.ms.dataAccess.filter.spectrafilter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import rappsilber.ms.dataAccess.SpectraAccess;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DBScanFilteredSpectrumAccess extends ScanFilteredSpectrumAccess{

    public DBScanFilteredSpectrumAccess() {
        super();
    }

//    public DBScanFilteredSpectrumAccess(SpectraAccess innerAccess) {
//        super(innerAccess);
//    }
//
    public DBScanFilteredSpectrumAccess(boolean whitelist) {
        super(whitelist);
    }
    
    
    public void readFromSearch(Connection con, int search_id) throws SQLException {
        Statement s = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        ResultSet rs = null;

        try { // new layout
            rs = s.executeQuery("SELECT ss.name, s.scan_number "
                +                       " FROM "
                + "                     (SELECT spectrum_id as id FROM spectrum_match WHERE dynamic_rank = 'true' AND search_id = " + search_id +") sm INNER JOIN Spectrum s on sm.id = s.id INNER JOIN spectrum_source ss on s.source_id = ss.id;");
        } catch (SQLException sex) { // failed - assume old layout
            rs = s.executeQuery("SELECT run_name, scan_number "
                           +                       "FROM "
                           + "                     v_export_materialized WHERE dynamic_rank = 'true' AND search_id = " + search_id +";");            
        }
        
        while (rs.next()) {
            super.SelectScan(rs.getString(1), rs.getInt(2));
        }
        
        rs.close();
        s.close();
    }
    
}
