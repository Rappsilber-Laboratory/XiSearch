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

import java.util.Collections;
import java.util.Comparator;


/**
 * a list of elements, that get sorted by the attached score
 * @param <Store> type of the elements to store
 * @param <Score> typer of the score used for sorting the elmenets
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ScoredLinkedList<Store,Score extends Comparable> extends java.util.LinkedList {
  private static final long serialVersionUID = 5211106976202643129L;
  //private Comparator<Store> m_Comparator;


  
//  private class BaseComperator<T> implements Comparator<T> {
//
//    public int compare(T arg0, T arg1) {
//      return ((Comparable<T>)arg0).compareTo(arg1);
//    }
//
//  }
//

  /**
   * default constructor to create an empty ScoredLinkedList, that uses the default compareator
   */
  public ScoredLinkedList() {
    //m_Comparator = new BaseComperator<Store>();
  }

//  /**
//   * defines a scoredlinkedlist that uses the given compareator
//   * @param comp
//   */
//  public ScoredLinkedList(Comparator<Store> comp) {
//    m_Comparator = comp;
//  }
  
  
  /**
   * Overrides the default LinkedList.add so the elements get added 
   * in a ordered list
   * @param e
   * @param value the score of the element
   * @return
   */
  public boolean add(Store e, Score value) {
    ScoredObject<Store, Score> so = new ScoredObject<Store, Score>(e, value);
    int pos = Collections.binarySearch(this, so, so);
    if (pos<0) pos = -(pos + 1);
    super.add(pos, so);
    return true;
  }
  

  @Override
  public int indexOf(Object e) {
      return super.indexOf(new ScoredObject(e, 0));
  }

  /**
   * Sets the score of the given element.<br/>
   * if the element was already know, then it will be rescored - and reinserted
   * at the right place.<br/>
   * If it does not exist, then it will be added at a position determined based
   * on the score.
   * @param e the element to store
   * @param value the score of the element
   */
  public void setScore(Store e, Score value) {
    int currentPos = this.indexOf(e);
    if (currentPos < 0) {
        add(e, value);
    } else {
        ScoredObject<Store, Score> so = new ScoredObject<Store, Score>(e, value);
        int newPos = Collections.binarySearch(this, e,so);
        if (newPos != currentPos && newPos != currentPos + 1) {
            super.add(newPos, so);
            super.remove((newPos < currentPos ? currentPos + 1: currentPos));
        } else
            ((ScoredObject)super.get(currentPos)).m_Score = value;
    }
  }

  @Override
  public Store get(int Index) {
      return ((ScoredObject<Store,Score>)super.get(Index)).m_Store;
  }

  @Override
  public Store getFirst() {
      return ((ScoredObject<Store,Score>)super.getFirst()).m_Store;
  }

  @Override
  public Store getLast() {
      return ((ScoredObject<Store,Score>)super.getLast()).m_Store;
  }

  /**
   * replaces the object at position index with the given object.<br/>
   * the nex object has then the score of the previous object
   * @param index
   * @param element
   * @return the previously stored object
   * @throws IndexOutOfBoundsException {@inheritDoc}
   */
  @Override
  public Store set(int index, Object element) {
    ScoredObject<Store,Score> o = (ScoredObject<Store, Score>) super.get(index);
    Store oldVal = o.m_Store;
    o.m_Store = (Store) element;
    return oldVal;
  }

  @Override
  public void add(int index, Object element) {
        throw new Error("ScoredLinkedList.add(int index, Object element): Not Implemented");
    }
    
  /**
   * 
   * @param e
   * @param s
   * @param maxPos 
   * @return
   */
  public boolean addMax(Store e, Score s, int maxPos) {
    ScoredObject<Store,Score> o = new ScoredObject<Store, Score>(e, s);
    int pos = Collections.binarySearch(this, o, o) ;
    if (pos<0) pos =0;
    if (pos < maxPos) {
      super.add(pos, o);
      return true;
    } else 
      return false;
  }

//  /**
//   * retuyrns the comparator
//   * @return
//   */
//  public Comparator<Store> getComparator() {
//    return m_Comparator;
//  }
  

}
