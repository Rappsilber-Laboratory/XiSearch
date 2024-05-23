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
package rappsilber.ms;

import rappsilber.utils.Util;

/**
 * Describes how m/z values should be matched.<br/>
 * The given tolerance can be either absolute (n da) or relative (ppm)
 *
 * @author Salman Tahir
 */
public class ToleranceUnit {

    public static ToleranceUnit ZEROTOLERANCE = new ToleranceUnit(0, "da");
    /**
     * the actual tolerance
     */
    private double value;
    /**
     * the assigned unit
     */
    private String unit;

    /**
     * is the tolerance relative(ppm) or absolute (da)
     */
    private boolean m_relativeUnit = false;

    /**
     * creates anew tolerance unit with the given tolerance
     *
     * @param value
     * @param unit
     */
    public ToleranceUnit(String value, String unit) {
        this(Double.parseDouble(value.trim()), unit);
    }

    /**
     * creates anew tolerance unit with the given tolerance
     *
     * @param value
     */
    public ToleranceUnit(String value) {
        this(value.replaceAll("[^0-9., ]", ""), value.replaceAll("[0-9., ]", ""));
    }

    /**
     * creates anew tolerance unit with the given tolerance
     *
     * @param value
     * @param unit
     */
    public ToleranceUnit(double value, String unit) {
        this.value = value;
        this.unit = unit;

        if (unit.equalsIgnoreCase("ppm")) {
            m_relativeUnit = true;
        } else if (!(unit.equalsIgnoreCase("da") || unit.equalsIgnoreCase("m/z") || unit.equalsIgnoreCase("mz"))) {
            throw new NumberFormatException("unknow unit " + unit);
        }
    }// end constructor

    // Accessor methods
    /**
     * returns the specified unit
     *
     * @return
     */
    public String getUnit() {
        return this.unit;
    }

    /**
     * returns the specified tolerance
     *
     * @return
     */
    public double getValue() {
        return this.value;
    }


    /**
     * sets the tolerance to a new value and sets
     *
     * @param v
     */
    public void setValue(double v) {
        this.value = v;
    }

    /**
     * compares two values under consideration of the tolerance
     *
     * it's made final - since at least during proviling that reduced the time
     * spent here by 1/5th but might be different on real-time conditions
     *
     * @param value1
     * @param value2
     * @return &lt;0: value1 + tolerance &lt; value2 <br/> =0:value1 - tolerance
     * &lt;= value2 &lt;= value1 + tolerance <br/> &gt;0: value1 - tolerance
     * &gt;= value2
     *
     */
    public final int compare(double value1, double value2) {

        if (m_relativeUnit) {
            //consider the higher tolerance
//            tolerance = Math.max(value1, value2) * Util.ONE_PPM * value;
            if (value1 > value2) {
                double t = value1 * Util.ONE_PPM * value;
                if (value1 - t > value2) {
                    return 1;
                } else {
                    return 0;
                }
            } else {
                double t = value2 * Util.ONE_PPM * value;
                if (value2 - t > value1) {
                    return -1;
                } else {
                    return 0;
                }
//                tolerance = value2 * Util.ONE_PPM * value;
            }
        }

        //double diff = value1 - value2;
        if (value1 + value < value2) {
            return -1;
        } else if (value1 - value > value2) {
            return 1;
        } else {
            return 0;
        }

    }

    /**
     * compares two values under consideration of the tolerance
     *
     * it's made final - since at least during profiling that reduced the time
     * spent here by 1/5th but might be different on real-time conditions
     *
     * @param value1
     * @param value2
     * @return &lt;0: value1 + tolerance &lt; value2 <br/> =0:value1 - tolerance
     * &lt;= value2 &lt;= value1 + tolerance <br/> &gt;0: value1 - tolerance
     * &gt;= value2
     *
     */
    public final int compareDoubleError(double value1, double value2) {

        Range r1 = getRange(value1);
        Range r2 = getRange(value2);

        if (r1.max < r2.min) {
            return -1;
        }
        if (r2.max < r1.min) {
            return 1;
        }

        return 0;

    }

    /**
     * returns the minimal difference between two peaks
     *
     * @param value1
     * @param value2
     * @return
     */
    public double minDiff(double value1, double value2) {
        double diff;
        double uncleanedDiff = Math.abs(value2 - value1);

        if (m_relativeUnit) {
            //consider the higher tolerance
            if (value1 > value2) {
                diff = (value1 - value1 * Util.ONE_PPM * value) - (value2 + value1 * Util.ONE_PPM * value);
            } else {
                diff = (value2 - value2 * Util.ONE_PPM * value) - (value1 + value2 * Util.ONE_PPM * value);
            }
        } else {
            diff = uncleanedDiff - value;
            if (diff < 0) {
                diff = 0;
            }
        }
        return (diff < uncleanedDiff ? diff : uncleanedDiff);
    }

    /**
     * what is the smallest value the given value could represent (v -
     * tolerance)
     *
     * @param v
     * @return smallest value that is within the tolerance-window
     */
    public double getMinRange(double v) {
        if (m_relativeUnit) {
            return v - v * Util.ONE_PPM * this.value;
        } else {
            return v - this.value;
        }
    }

    /**
     * what is the biggest value the given value could represent (v + tolerance)
     *
     * @param v
     * @return BIGGEST value that is within the tolerance-window
     */
    public double getMaxRange(double v) {
        if (m_relativeUnit) {
            return v + v * Util.ONE_PPM * this.value;
        } else {
            return v + this.value;
        }
    }

    /**
     * what is the smallest and largest  value the given value could represent 
     *
     * @param v
     * @return BIGGEST value that is within the tolerance-window
     */
    public Range getRange(double v) {
        Range ret = new Range();
        if (m_relativeUnit) {
            double abs = v * Util.ONE_PPM * this.value;
            ret.min = v - abs;
            ret.max = v + abs;
        } else {
            ret.min = v - this.value;
            ret.max = v + this.value;
        }
        return ret;
    }

    /**
     * what is the smallest and largest  value the given value could represent (v -
     * tolerance) under the condition, that the tolerance is based on a
     * different value.<br/>
     * For an absolute tolerance this is the same as getMinRange(mass).
     *
     * @param mass the center of the tolerance-window
     * @param referenceMass defines the size of the tolerance window
     * @return the of the tolerance-window
     */
    public Range getRange(double mass, double referenceMass) {
        Range r = new Range();
        
        if (m_relativeUnit) {
            double e = referenceMass * Util.ONE_PPM * this.value;
            r.min = mass - e;
            r.max = mass + e;
        } else {
            r.min = mass - this.value;
            r.max = mass + this.value;
        }
        return r;
    }
    
    
    /**
     * what is the smallest value the given value could represent (v -
     * tolerance) under the condition, that the tolerance is based on a
     * different value.<br/>
     * For an absolute tolerance this is the same as getMinRange(mass).
     *
     * @param mass the center of the tolerance-window
     * @param referenceMass defines the size of the tolerance window
     * @return smallest value that is within the tolerance-window
     */
    public double getMinRange(double mass, double referenceMass) {
        if (m_relativeUnit) {
            return mass - referenceMass * Util.ONE_PPM * this.value;
        } else {
            return mass - this.value;
        }
    }

    /**
     * what is the biggest value the given value could represent (v + tolerance)
     * under the condition, that the tolerance is based on a different
     * value.<br/>
     * For an absolute tolerance this is the same as getMaxRange(mass).
     *
     * @param mass the center of the tolerance-window
     * @param referenceMass defines the size of the tolerance window
     * @return biggest value that is within the tolerance-window
     */
    public double getMaxRange(double mass, double referenceMass) {
        if (m_relativeUnit) {
            return mass + referenceMass * Util.ONE_PPM * this.value;
        } else {
            return mass + this.value;
        }
    }

    /**
     * what is the biggest value the given value could represent (v + tolerance)
     *
     * @param v
     * @return BIGGEST value that is within the tolerance-window
     */
    public double getRangeSize(double v) {
        if (m_relativeUnit) {
            return 2 * v * Util.ONE_PPM * this.value;
        } else {
            return 2 * this.value;
        }
    }

    /**
     * what is the biggest value the given value could represent (v + tolerance)
     *
     * @param v
     * @return BIGGEST value that is within the tolerance-window
     */
    public double getAbsoluteError(double v) {
        if (m_relativeUnit) {
            return v * Util.ONE_PPM * this.value;
        } else {
            return this.value;
        }
    }

    public String toString() {
        return String.valueOf(this.value) + " " + this.unit;
    }

    public String toString(double exp, double calc) {
        double error = exp - calc;
        if (isRelative() && unit.contentEquals("ppm")) {
            error = error / calc * 1000000;
            return Util.twoDigits.format(error) + "ppm";
        }
        return Util.threeDigits.format(error) + "Da";
    }

    public static ToleranceUnit parseArgs(String args) {
        return new ToleranceUnit(args);
    }

    public boolean isRelative() {
        return m_relativeUnit;
    }

    /**
     * calculates the error between the two values. If the error unit is Da then
     * it just returns the difference. For ppm it will return the ppm value.
     *
     * @param expMass the measured mass
     * @param referenceMass the theoretical mass
     * @return
     */
    public double getError(double expMass, double referenceMass) {
        if (isRelative()) {
            return (referenceMass - expMass) / referenceMass * 1000000;
        }
        return (referenceMass - expMass);
    }

}// end class
