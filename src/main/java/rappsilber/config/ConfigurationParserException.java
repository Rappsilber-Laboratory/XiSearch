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
public class ConfigurationParserException extends Exception{
    private static final long serialVersionUID = -485860201910687548L;

    int m_line=-1;

    public ConfigurationParserException(String Message) {
        super(Message);
    }

    public ConfigurationParserException(Throwable cause) {
        super(cause);
    }

    public ConfigurationParserException(String Message, Throwable cause) {
        super(Message, cause);
    }

    public ConfigurationParserException(String Message,  Throwable cause, int line) {
        super(Message, cause);
        m_line = line;
    }

    public ConfigurationParserException(String Message, int line) {
        super(Message);
        m_line = line;
    }

    @Override
    public String toString() {
        String ret = super.toString();
        if (m_line >= 0) {
            return ret + ": line " + m_line;
        } else {
            return ret;
        }
    }

}
