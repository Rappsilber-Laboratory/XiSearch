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

import java.util.concurrent.atomic.AtomicBoolean;

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
    private Thread target;
    /** how long to wait before sending the interrupt */
    private int miliseconds;
    private AtomicBoolean canceled = new AtomicBoolean(false);
    private StackTraceElement caller ;
    private String callerID;
    private boolean checkMethod=false;
    

    /**
     * this will setup
     * @param target
     * @param miliseconds
     * @param name 
     */
    public InterruptSender(Thread target, int miliseconds) {
        this(target, miliseconds, "");
    }
    public InterruptSender(int miliseconds) {
        this(currentThread(), miliseconds);
    }   
    
    /**
     * this will setup the interruptsender
     * @param target
     * @param miliseconds
     * @param name 
     */
    public InterruptSender(Thread target, int miliseconds, String name) {
        this.target = target;
        this.miliseconds = miliseconds;
        this.setName("InterruptSender " + name);
        caller=target.getStackTrace()[1];
        callerID=caller.getClassName()+"->"+caller.getMethodName();
        this.setDaemon(true);
    }

    @Override
    public void run() {
        try {
            this.sleep(miliseconds);
            boolean doInterrupt = false;
            // are we still in the same method that called us
            if (checkMethod ) {
                for (StackTraceElement ste : target.getStackTrace()) {
                    if ((ste.getClassName()+"->"+ste.getMethodName()).contentEquals(callerID)){
                        doInterrupt = true;
                        break;
                    }
                }
            } else {
                doInterrupt = true;
            }
            if (!canceled.get() && doInterrupt) {
                target.interrupt();
            }
        } catch (InterruptedException ex) {
        }
    }
    
    public void cancel() {
        canceled.set(true);
        this.interrupt();
    }

    /**
     * @return the checkMethod
     */
    public boolean checkMethod() {
        return checkMethod;
    }

    /**
     * if this is set to true the interrupt will only be send if the target 
     * thread is still in the method that called the interruptsender in the 
     * first place (actually if the method is still part of the stack trace)
     * @param checkMethod the checkMethod to set
     */
    public InterruptSender setCheckMethod(boolean checkMethod) {
        this.checkMethod = checkMethod;
        return this;
    }
    
}
