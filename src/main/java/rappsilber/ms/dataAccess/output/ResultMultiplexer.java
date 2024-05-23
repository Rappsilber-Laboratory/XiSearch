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
package rappsilber.ms.dataAccess.output;

import java.io.IOException;
import java.util.ArrayList;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ResultMultiplexer extends AbstractResultWriter{
    private ArrayList<ResultWriter> m_out = new ArrayList<ResultWriter>();
    private int                     m_resultCount = 0;
    private int                     m_topResultCount = 0;

    public void writeHeader() {
        for (ResultWriter writer : m_out) {
            writer.writeHeader();
        }
    }

    public void writeResult(MatchedXlinkedPeptide match) throws IOException {
        for (ResultWriter writer : m_out) {
            writer.writeResult(match);
        }
        m_resultCount ++;
        if (match.getMatchrank() == 1) {
            m_topResultCount++;
        }
        if (m_doFreeMatch) {
            match.free();
        }
    }

    public int getResultCount() {
        return m_resultCount;
    }

    public int getTopResultCount() {
        return m_topResultCount;
    }
    
    public void finished() {
        for (ResultWriter writer : m_out) {
            writer.finished();
        }
        super.finished();
    }

    public void addResultWriter(ResultWriter out) {
        m_out.add(out);
    }
    
    public ArrayList<ResultWriter> getWriters() {
        return m_out;
    }


    @Override
    public boolean waitForFinished() {
        for (ResultWriter writer : m_out) {
            if (!writer.waitForFinished()) {
                return false;
            }
        }
        return true;
    }

    public void flush() {
        for (ResultWriter rw : m_out) {
            rw.flush();
        }
    }
    
    public void ping() {
        for (ResultWriter writer : m_out) {
            writer.ping();
        }
        
    }
}
