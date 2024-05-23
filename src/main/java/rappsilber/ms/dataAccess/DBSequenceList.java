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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import rappsilber.config.RunConfig;
import rappsilber.db.ConnectionPool;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.sequence.fasta.FastaFile;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DBSequenceList extends SequenceList {
    private static final long serialVersionUID = 7167660110415075170L;

    public DBSequenceList(ConnectionPool dbCon, int SearchID, String basedir, RunConfig config) throws SQLException, IOException {
        super(config);
        Connection con = dbCon.getConnection();

        ResultSet rs = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).executeQuery(
                "SELECT file_path || '/' || file_name, decoy_file, id FROM sequence_file sdb " +
                "INNER JOIN search_sequencedb ssdb ON ssdb.seqdb_id = sdb.id  " +
                "WHERE ssdb.search_id = " + SearchID);

        while (rs.next()) {
            FastaFile source = new FastaFile(basedir + rs.getString(1));
            source.setId(rs.getLong(3));
            
            if (rs.getBoolean(2)) {
                addFasta(new File(source.getPath()), DECOY_GENERATION.ISDECOY);
            } else {
                addFasta(new File(basedir + rs.getString(1)));
            }

            for (Sequence s : this) {
                if (s.getSource() == null || s.getSource().getId() == null) {
                    s.setSource(source);
                }
            }
            
        }
    }
}
