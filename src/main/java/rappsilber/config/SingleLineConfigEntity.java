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
package rappsilber.config;

import java.util.HashSet;
import rappsilber.utils.HashMapList;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SingleLineConfigEntity implements ConfigEntity{

    HashMapList<String,String[]> m_ConfigValues = new HashMapList<String, String[]>();
    HashSet<String>              m_ConfigTags = new HashSet<String>();
    String                       m_ConfigType;
    String                       m_ConfigClass;


    public void ReadConfig(String config) {
        // Assumed structure
        // Type:Class:variable:value;variable:value1,value2,value3;tag;tag;
        //
        //
        String[] args = config.split(":",3);
        // parse out the common fields
        m_ConfigType = args[0];
        m_ConfigClass = args[1];

        // now splitr up the remainder
        for (String arg : args[2].split(";")) {
            String[] argParts = arg.split(":");
            String argName = argParts[0].toUpperCase();
            if (argParts.length == 1) {
                m_ConfigValues.put(argName, null);
                m_ConfigTags.add(argName);
            } else {
                String[] argValues = argParts[1].split(",");
                m_ConfigValues.put(argName, argValues);
            }
        }
    }


    public String getConfigType() {
        return m_ConfigType;
    }

    public String getConfigClass() {
        return m_ConfigClass;
    }

    public Class getConfigClass(Class defaultClass) {
        Class c = null;
        try {
            c = Class.forName(m_ConfigClass);
        } catch(Exception e) {
            String dcn = defaultClass.getCanonicalName();
            String cpath = dcn.substring(0, dcn.lastIndexOf("."));
            try {
                c = Class.forName(cpath + "." + m_ConfigClass);
            } catch (ClassNotFoundException ex) {
                c = defaultClass;
            }
        }
        return c;
    }


    public boolean hasConfigTag(String tag) {
        return m_ConfigTags.contains(tag);
    }

    public String[] getConfigValues(String name) {
        return m_ConfigValues.get(name);
    }

    public String[] getConfigValues(String name,String defaultValues[]) {
        String[] ret =  m_ConfigValues.get(name);
        return ret == null ? defaultValues : ret;
    }

    public String getConfigValue(String name,String defaultValues) {
        String[] ret =  m_ConfigValues.get(name);
        return ret == null ? defaultValues : ret[0];
    }

    public String getConfigValue(String name) {
        String[] ret =  m_ConfigValues.get(name);
        return ret == null ? null : ret[0];
    }

}
