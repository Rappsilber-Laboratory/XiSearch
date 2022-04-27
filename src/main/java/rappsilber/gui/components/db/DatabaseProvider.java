/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.gui.components.db;

import java.sql.Connection;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public interface DatabaseProvider {

    Connection getConnection();

    String getConnectionString();
    
}
