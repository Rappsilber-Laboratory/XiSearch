/*
 * Copyright 2018 Lutz Fischer <lfischer@staffmail.ed.ac.uk>.
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
package rappsilber.utils;

import rappsilber.ms.dataAccess.db.XiDBWriterBiogridXi3;

/**
 * Have observed cases where postgresql was simply waiting for a lock indefinitely and not returning.<br/>
 * This is used to break up these waits:
 * <pre>
 * java.lang.Object.wait(Native Method)
 * java.lang.Object.wait(Object.java:503)
 * org.postgresql.core.v3.QueryExecutorImpl.waitOnLock(QueryExecutorImpl.java:91)
 * org.postgresql.core.v3.QueryExecutorImpl.execute(QueryExecutorImpl.java:228)
 * org.postgresql.jdbc2.AbstractJdbc2Connection.executeTransactionCommand(AbstractJdbc2Connection.java:793)
 * org.postgresql.jdbc2.AbstractJdbc2Connection.rollback(AbstractJdbc2Connection.java:846)
 * </pre>
 * <p>to stop this InterruptSender just call {@link #interrupt()} and it should die gracefully</p>
 * <p>usage:</p>
 * <pre>
 *   InterruptSender is = new InterruptSender({@link Thread#currentThread()}, 10000,"some name");
 *   is.start();
 *   try {
 *     [do something thatz could get stuck on a wait]
 *   } finally {
 *      is.interrupt();
 *   }
 * </pre>
 * 
 */
public class InterruptSender extends Thread {
    
    /** the target thread to interrupt after the wait*/
    Thread target;
    /** how long to wait before sending the interrupt */
    int miliseconds;
    

    /**
     * this will setup
     * @param target
     * @param miliseconds
     * @param name 
     */
    public InterruptSender(Thread target, int miliseconds) {
        this(target, miliseconds, "");
    }
    
    /**
     * this will setup
     * @param target
     * @param miliseconds
     * @param name 
     */
    public InterruptSender(Thread target, int miliseconds, String name) {
        this.target = target;
        this.miliseconds = miliseconds;
        this.setName("InterruptSender " + name);
    }

    @Override
    public void run() {
        try {
            this.sleep(miliseconds);
            target.interrupt();
        } catch (InterruptedException ex) {
        }
    }
    
}
