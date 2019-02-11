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

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public interface ConfigEntity {
    String getConfigType();

    String getConfigClass();

    Class getConfigClass(Class defaultClass);

    boolean hasConfigTag(String tag);

    String[] getConfigValues(String name);

    String[] getConfigValues(String name,String defaultValues[]);

    String getConfigValue(String name,String defaultValues);

    String getConfigValue(String name);
}
