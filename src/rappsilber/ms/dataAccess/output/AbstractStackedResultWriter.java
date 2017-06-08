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
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class AbstractStackedResultWriter extends AbstractResultWriter{

    private ResultWriter m_innerWriter;
    private int topResultCount=0;


    @Override
    public void writeHeader() {
        getInnerWriter().writeHeader();
    }


    protected void innerWriteResult(MatchedXlinkedPeptide match) throws IOException{
        getInnerWriter().writeResult(match);
        if (match.getMatchrank() == 1)
            topResultCount++;
    }


    @Override
    public int getResultCount() {
        return getInnerWriter().getResultCount();
    }

    @Override
    public int getTopResultCount() {
        return getInnerWriter().getTopResultCount();
    }
    
    @Override
    public void setFreeMatch(boolean doFree) {
        getInnerWriter().setFreeMatch(doFree);
        m_doFreeMatch = doFree;
    }

    public abstract void selfFinished();

    public abstract boolean selfWaitForFinished();
    
//    public void finishedPropagete() {
//        selfFinished();
//        getInnerWriter().finished();
//    }

//    @Override
//    public boolean waitForFinished() {
//        return getInnerWriter().waitForFinished();
//    }
//    
    @Override
    public void finished() {
        super.finished();
        selfFinished();
        getInnerWriter().finished();
    }

    public boolean waitForFinished() {
        boolean ret = selfWaitForFinished();
        if (ret) 
            return getInnerWriter().waitForFinished();
        return ret;
        
    }

    /**
     * @return the m_innerWriter
     */
    public ResultWriter getInnerWriter() {
        return m_innerWriter;
    }

    /**
     * @param m_innerWriter the m_innerWriter to set
     */
    public void setInnerWriter(ResultWriter innerWriter) {
        this.m_innerWriter = innerWriter;
    }

    private final Object flushSync = new Object();
    public void flush() {
        synchronized(flushSync) {
            m_innerWriter.flush();
        }
    }

}
