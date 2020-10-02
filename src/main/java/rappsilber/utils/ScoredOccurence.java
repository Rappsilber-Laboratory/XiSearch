/*
 * Copyright 2017 Lutz Fischer <lfischer@staffmail.ed.ac.uk>.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public interface ScoredOccurence<T> extends Iterable<T>{

    /**
     * returns the score that was calculated for the given object
     * @param o the object in question
     * @param defaultScore if no score was calculated return this score.
     * @return
     */
    double Score(T o, double defaultScore);

    /**
     * Add the given number to the previously calculated value for this object.
     * if the object wasn't seen jet store the value itself. (e.g.
     * assume the previous value was 0)
     *
     * @param o the object in question
     * @param score the value that should be added to the previuos value
     * @return the new value (score) for the object
     */
    double add(T o, double score);
    void addAllNew(ScoredOccurence<T> list );

    void addAllHighest(ScoredOccurence<T> list );
    void addAllLowest(ScoredOccurence<T> list );
    
    /**
     * returns an ArrayList of elements the [ranks] highest associated values
     * @param ranks how many unique scores to return
     * @param maxTotal return at most this number of results
     * @return
     */
    ArrayList<T> getHighestNEntries(int ranks, int maxTotal);

    /**
     * returns an ArrayList of elements the [ranks] highest associated values
     * @param ranks how many unique scores to return
     * @param maxTotal return at most this number of results
     * @return
     */
    ScoredOccurence<T> getHighestNMappings(int ranks, int maxTotal);

    /**
     * returns an ArrayList of elements the [ranks] highest associated values
     * @param ranks how many unique scores to return
     * @param maxTotal return at most this number of results
     * @return
     */
    ArrayList<T> getLowestNEntries(int ranks, int maxTotal);

    ArrayList<T> getLowestNEntries(int ranks, int maxTotal, final Comparator<T> firstCompare);

    /**
     * returns an ArrayList of elements the [ranks] highest associated values
     * @param ranks how many unique scores to return
     * @param maxTotal return at most this number of results
     * @return
     */
    ScoredOccurence<T> getLowestNMappings(int ranks, int maxTotal);

    /**
     * returns a list of all seen objects
     * @return
     */
    Collection<T> getScoredObjects();

    T[] getScoredSortedArray(T[] a);

    ArrayList<T> getSortedEntries();

    Iterator<T> iterator();

    /**
     * multiply the previously calculated value for this object, with the given
     * number. if the object wasn't seen jet store the value itself. (e.g.
     * assume the previous value was 1)
     * @param o the object in question
     * @param score the value that should be multiplied with the previous value
     * @return the new value (score) for the object
     */
    double multiply(T o, double score);

    /**
     * returns whether a given object was already seen. (via add or multiply)
     * @param o the object in question
     * @return true: was already seen; false otherwise
     */
    boolean seen(T o);

    int size();
    
}
