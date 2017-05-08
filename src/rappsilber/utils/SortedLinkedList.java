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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;


/**
 *
 * @param <T> 
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SortedLinkedList<T> extends java.util.LinkedList<T> {
  private static final long serialVersionUID = 5211106976202643129L;
  private Comparator<T> m_Comparator;
  
  private class BaseComparator<T> implements Comparator<T> {

    public int compare(T arg0, T arg1) {
      return ((Comparable<T>)arg0).compareTo(arg1);
    }
    
  }
  
  /**
   * initilises a list with the default comperator
   */
  public SortedLinkedList() {
    m_Comparator = new BaseComparator<T>();
  }

  /**
   * initilises a list with the given comperator
   * @param comp
   */
  public SortedLinkedList(Comparator<T> comp) {
    m_Comparator = comp;
  }
  
  /**
   * creates a new list and adds all element of c
   * @param c
   */
  public SortedLinkedList(Collection<? extends T> c) {
    this();
    Iterator i = c.iterator();
    while (i.hasNext()) {
      this.add((T)i.next());
    }
  }

  /**
   * creates a new list and adds all element of c and uses comp for sorting
   * @param comp 
   * @param c
   */
  public SortedLinkedList(Comparator<T> comp, Collection<? extends T> c) {
    this(comp);
    Iterator i = c.iterator();
    while (i.hasNext()) {
      this.add((T)i.next());
    }
  }
  
  /**
   * Overrides the default LinkedList.add so the elements get added 
   * in a ordered list
   * @param e
   * @return
   */
  @Override
  public boolean add(T e) {
    int pos = Collections.binarySearch(this, e, getComparator());
    if (pos<0) pos = -(pos + 1);
    super.add(pos, e);
    return true;
  }


    @Override
    public boolean addAll(Collection<? extends T> c) {
        if (c.size()  == 0)
            return false;
        for (T e : c) {
            add(e);
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return addAll(c);
    }

   /**
   * add e only if it would be inserted maximal at pos maxpos
   * @param e
   * @param maxPos
   * @return
   */
  public boolean addMax(T e, int maxPos) {
    int pos = Collections.binarySearch(this, e, getComparator()) + 1;
    if (pos<0) pos =0;
    if (pos < maxPos) {
      super.add(pos, e);
      return true;
    } else 
      return false;
  }
  
  /**
   * returns the used comperator
   * @return
   */
  public Comparator<T> getComparator() {
    return m_Comparator;
  }
  

}
