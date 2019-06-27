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

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class LinearRegression2D {

    private double m_Xsum = 0d;
    private double m_Xvar = 0d;
    private double m_Ysum = 0d;
    private double m_XYsum = 0d;
    private long m_countPair = 0;
    private double m_Xavr = 0;
    private double m_Yavr = 0;

    private double m_slope;
    private double m_yIntercept;


    public void addPair(double x, double y) {
        if (m_countPair == 0) {
            // initialise with first values
            m_Xavr = x;
            m_Yavr = y;
        } else {
            double dx = x - m_Xavr;
            double dy = y - m_Yavr;
            m_Xvar += dx * dx * (double) m_countPair / (double) (m_countPair + 1.0);
            m_XYsum += dx * dy * (double) m_countPair / (double) (m_countPair + 1.0);
            m_Xavr += dx / (m_countPair + 1.0);
            m_Yavr += dy / (m_countPair + 1.0);
        }
        m_Xsum += x;
        m_Ysum += y;
        m_countPair++;
    }



    public boolean calc(){
        // if there is only one value
        if (m_countPair < 2) {
            return false;
        }
        m_slope = m_XYsum / m_Xvar;
        m_yIntercept = (m_Ysum - m_slope * m_Xsum) / ((double) m_countPair);
        return true;
    }

    public double predictY(double x) {
        return m_yIntercept + m_slope * x;
    }

    public double getYInterxcept() {
        return m_yIntercept;
    }

    public double getSlope() {
        return m_slope;
    }

    public void clear() {
        m_Xsum = 0d;
        m_Xvar = 0d;
        m_Ysum = 0d;
        m_XYsum = 0d;
        m_countPair = 0;
        m_Xavr = 0;
        m_Yavr = 0;
    }

}
