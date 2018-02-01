/* 
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

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionPool implements Runnable {

    private final static int DEFAULT_POOL_MINSIZE = 2;
    private final static int DEFAULT_POOL_MAXSIZE = 16;
    private final static boolean DEFAULT_POOL_WAIT_IF_BUSY = true;

    private String driver, url, username, password;
    private int maxConnections;
    private boolean waitIfBusy;
    private Vector availableConnections, busyConnections;
    private boolean connectionPending = false;
    private int minimumConnections = DEFAULT_POOL_MINSIZE;

    public ConnectionPool(String driver, String url,
            String username, String password) throws SQLException {
        this(driver, url, username, password, DEFAULT_POOL_MINSIZE, DEFAULT_POOL_MAXSIZE, DEFAULT_POOL_WAIT_IF_BUSY);
    }

    public ConnectionPool(String driver, String url,
            String username, String password,
            int initialConnections,
            int maxConnections,
            boolean waitIfBusy)
            throws SQLException {
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
        this.maxConnections = maxConnections;
        this.waitIfBusy = waitIfBusy;
        if (initialConnections > maxConnections) {
            initialConnections = maxConnections;
        }
        minimumConnections = initialConnections;
        availableConnections = new Vector(initialConnections);
        busyConnections = new Vector();
        for (int i = 0; i < initialConnections; i++) {
            availableConnections.addElement(makeNewConnection());
        }
    }

    public synchronized Connection getConnection()
            throws SQLException {
        if (!availableConnections.isEmpty()) {
            Connection existingConnection
                    = (Connection) availableConnections.lastElement();
            int lastIndex = availableConnections.size() - 1;
            availableConnections.removeElementAt(lastIndex);
            boolean connectionUsable = false;
            if (!existingConnection.isClosed()) {
                try {
                    Statement st = existingConnection.createStatement();
                    st.execute("SELECT 1+1;");
                    st.close();
                    connectionUsable = true;
                } catch (Exception e) {
                }
            }
            // If connection on available list is closed (e.g.,
            // it timed out), then remove it from available list
            // and repeat the process of obtaining a connection.
            // Also wake up threads that were waiting for a
            // connection because maxConnection limit was reached.
            if (existingConnection.isClosed() || !connectionUsable) {
                notifyAll(); // Freed up a spot for anybody waiting
                return (getConnection());
            } else {
                busyConnections.addElement(existingConnection);
                return (existingConnection);
            }
        } else {

            // Three possible cases:
            // 1) You haven't reached maxConnections limit. So
            //    establish one in the background if there isn't
            //    already one pending, then wait for
            //    the next available connection (whether or not
            //    it was the newly established one).
            // 2) You reached maxConnections limit and waitIfBusy
            //    flag is false. Throw SQLException in such a case.
            // 3) You reached maxConnections limit and waitIfBusy
            //    flag is true. Then do the same thing as in second
            //    part of step 1: wait for next available connection.
            if ((totalConnections() < maxConnections)
                    && !connectionPending) {
                makeBackgroundConnection();
            } else if (!waitIfBusy) {
                throw new SQLException("Connection limit reached");
            }
            // Wait for either a new connection to be established
            // (if you called makeBackgroundConnection) or for
            // an existing connection to be freed up.
            try {
                wait();
            } catch (InterruptedException ie) {
            }
            // Someone freed up a connection, so try again.
            return (getConnection());
        }
    }

    // You can't just make a new connection in the foreground
    // when none are available, since this can take several
    // seconds with a slow network connection. Instead,
    // start a thread that establishes a new connection,
    // then wait. You get woken up either when the new connection
    // is established or if someone finishes with an existing
    // connection.
    private void makeBackgroundConnection() {
        connectionPending = true;
        try {
            Thread connectThread = new Thread(this);
            connectThread.setName(connectThread.getName() + " - ConnectionPool:makeBackgroundConnection");
            connectThread.start();
        } catch (OutOfMemoryError oome) {
            // Give up on new connection
        }
    }

    public void run() {
        int tries = 12;
        while (tries >0) {
            try {
                while (totalConnections() < this.minimumConnections) {
                    Connection connection = makeNewConnection();
                    synchronized (this) {
                        availableConnections.addElement(connection);
                        tries=0;
                        connectionPending = false;
                        notifyAll();

                    }
                }
            } catch (Exception e) { 
                // SQLException or OutOfMemory
                // Give up on new connection and wait for existing one
                // to free up.
                // we try several times and if it still fails fail silently.
                tries--;
                try {
                    Thread.currentThread().sleep(5000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ConnectionPool.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
        }
    }

    // This explicitly makes a new connection. Called in
    // the foreground when initializing the ConnectionPool,
    // and called in the background when running.
    private Connection makeNewConnection()
            throws SQLException {
        int timeOut = 10;
        while (true) {
            try {
                try {
                    // Load database driver if not already loaded
                    Class.forName(driver);
                    // Establish network connection to database
                    Connection connection
                            = DriverManager.getConnection(url, username, password);
                    return (connection);
                } catch (ClassNotFoundException cnfe) {
                    // Simplify try/catch blocks of people using this by
                    // throwing only one exception type.
                    throw new SQLException("Can't find class for driver: "
                            + driver);
                }
            } catch (SQLException e) {
                if (timeOut-- == 0) {
                    throw e;
                }
                try {
                    Thread.currentThread().sleep((10-timeOut)*10000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ConnectionPool.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

  

    public synchronized void free(Connection connection) {
        busyConnections.removeElement(connection);
        availableConnections.addElement(connection);
        // Wake up threads that are waiting for a connection
        notifyAll();
    }

    public synchronized int totalConnections() {
        return (availableConnections.size()
                + busyConnections.size());
    }

    /**
     * Close all the connections. Use with caution: be sure no connections are
     * in use before calling. Note that you are not <I>required</I> to call this
     * when done with a ConnectionPool, since connections are guaranteed to be
     * closed when garbage collected. But this method gives more control
     * regarding when the connections are closed.
     */
    public synchronized void closeAllConnections() {
        closeConnections(availableConnections);
        availableConnections = new Vector();
        closeConnections(busyConnections);
        busyConnections = new Vector();
    }

    private void closeConnections(Vector connections) {
        try {
            for (int i = 0; i < connections.size(); i++) {
                Connection connection
                        = (Connection) connections.elementAt(i);
                if (!connection.isClosed()) {
                    connection.close();
                }
            }
        } catch (SQLException sqle) {
            // Ignore errors; garbage collect anyhow
        }
    }

    public synchronized String toString() {
        String info
                = "ConnectionPool(" + url + "," + username + ")"
                + ", available=" + availableConnections.size()
                + ", busy=" + busyConnections.size()
                + ", min=" + minimumConnections
                + ", max=" + maxConnections;
        return (info);
    }
}
