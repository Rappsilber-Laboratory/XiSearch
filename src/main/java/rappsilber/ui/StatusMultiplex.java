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
package rappsilber.ui;

import java.util.ArrayList;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class StatusMultiplex implements StatusInterface{
    private ArrayList<StatusInterface> m_interfaces = new ArrayList<StatusInterface>();
    private String m_status;
    
    public void addInterface(StatusInterface i) {
        m_interfaces.add(i);
    }

    public void setStatus(String status) {
        m_status = status;
        for (StatusInterface i: m_interfaces ) {
            i.setStatus(status);
        }
    }

    public String getStatus() {
        return m_status;
    }
    
}
