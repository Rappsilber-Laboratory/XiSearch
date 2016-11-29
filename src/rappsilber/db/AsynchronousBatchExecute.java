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
package rappsilber.db;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Provides a way to run some batchupdates in a second thread
 * especially usefull - for running large batch-update, that can be split
 * into several smaller ones.
 * E.g. preparing the next 1000 batch-elementsl while the first 1000 gets
 * send to the database
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class AsynchronousBatchExecute {



    /**
     * how many threads are permited
     * Should be max AvailableProcessors - 1
     */
    private static final int MAX_CONCURENT_EXECUTE = 2;
    /**
     * How many updates are active
     */
    private int m_CountConcurrentThread = 0;
//    private Object m_threadNotification = new Object();


//    private Statement m_toRun;



    /**
     * the actual thread, that sends data to the database
     */
    private class runner implements Runnable{
        private Statement m_stm;

        /**
         * constructor
         * @param stm - the Statement containing the batch-commands to be executed
         */
        public runner(Statement stm) {
            m_stm = stm;
        }

        /**
         * gets called by Thread and sends the actual data to the database
         */
        public void run() {
            try {
                m_stm.executeBatch();
                m_stm.clearBatch();
            } catch (SQLException ex) {
                throw new Error(ex);
            }
            decrementThreads();
        }

    }

    /**
     * syncronised function for keeping track of the used threads
     * Also ensures that only the maximum number of threads are used.
     * If all threads are used it waits until one finishes.
     */
    private synchronized void incrementThreads() {
        if (m_CountConcurrentThread >= MAX_CONCURENT_EXECUTE) {
            try {
                wait();
            } catch (InterruptedException ex) {
            }
        }
        m_CountConcurrentThread ++;
    }

    /**
     * syncronised function for keeping track of the used threads
     */
    private synchronized void decrementThreads() {
        m_CountConcurrentThread --;
        notify();
    }

    /**
     * adds a new statement that can be executed asynchronysly
     * @param toRun
     */
    public void runBatchUpdate(Statement toRun) {
        incrementThreads();
        new Thread(new runner(toRun)).start();
    }

    /**
     * before anything can be done with the transfered data - we have to ensure
     * that all data got there in the first place.
     * That is done here. It basicaly waits until all outstanding updatethreads
     * have finished their tascs and then returns
     */
    public void waitFinisheAllUpdates() {
        while (m_CountConcurrentThread > 0) {
            try {
                wait(2000);
            } catch (InterruptedException ex) {
            }
        }
    }


}
