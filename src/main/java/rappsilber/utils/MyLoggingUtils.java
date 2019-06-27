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
package rappsilber.utils;

import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MyLoggingUtils {
    
    
    /**
     * Looks for system-properties that follow the schema of:
     * java.util.logging.loglevel:logger=level
     * e.g. with the opetion
     * -D java.util.logging.loglevel:org.rappsilber=FINE
     * one can then adjust the logging level.
     * It will also addjust the level for the handlers that are assigned to 
     * closest parent-logger that has any handler attached.
     * If no extra handler where attached then the console-handler will be set 
     * to the given level
     */
    public static void SystemPropertyToLoggerLevel() {
        Logger.getGlobal().setLevel(Level.WARNING);
        HashMap<String,Level> logLevels=new HashMap<String,Level>();
        for(String name:System.getProperties().stringPropertyNames()){
            String logger="java.util.logging.loglevel:";
            if(name.startsWith(logger)){
                String loggerName=name.substring(logger.length());
                String loggerValue=System.getProperty(name);
                try {
                    Level level = Level.parse(loggerValue);
                    Logger l =  Logger.getLogger(loggerName);
                    l.setLevel(level);
                    Logger.getLogger(MyLoggingUtils.class.getName()).log(Level.INFO, "Set logging for {0} to {1}({2})", new Object[]{loggerName, level.intValue(), level.getName()});
                    
                    Logger lg = Logger.getGlobal().getParent();
                    while (l.getHandlers().length == 0 && l !=lg ) {
                        l=l.getParent();
                    }
                    Logger.getLogger(MyLoggingUtils.class.getName()).log(Level.INFO, "Set logging for handlers of {0} to {1}({2})", new Object[]{l.getName(), level.intValue(), level.getName()});
                    for(Handler handler : l.getHandlers()) {
                        if (handler.getLevel().intValue() > level.intValue()) {
                            handler.setLevel(level);
                        }
                    }                    
                
                } catch (IllegalArgumentException e) {
                    Logger.getLogger(MyLoggingUtils.class.getName()).log(Level.WARNING, "Error setting logging level for {0} to {1}", new Object[]{loggerName, loggerValue});
                }
            }
        }
    }
}
