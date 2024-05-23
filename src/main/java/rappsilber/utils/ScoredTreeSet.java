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

import java.util.Iterator;
import java.util.TreeSet;


/**
 * a list of elements, that get sorted by the attached score
 * @param <Store> type of the elements to store
 * @param <Score> typer of the score used for sorting the elmenets
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ScoredTreeSet<Store,Score extends Comparable> extends TreeSet<ScoredObject<Store,Score>> {
    private static final long serialVersionUID = -6608986000737131211L;

    //SortedLinkedList<ScoredObject<Store,Score>> m_innerList = new SortedLinkedList<ScoredObject<Store, Score>>();
    //TreeSet<ScoredObject<Store,Score>> m_innerList = new TreeSet<ScoredObject<Store, Score>>();
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
  public ScoredTreeSet() {
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
    return this.add(so);
  }
  

//  public int indexOf(Object e) {
//      return m_innerList.indexOf(new ScoredObject(e, 0));
//  }

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
    throw new UnsupportedOperationException("does not work yet");
  }

//  public Store get(int Index) {
//      return ((ScoredObject<Store,Score>)m_innerList.get(Index)).m_Store;
//  }

//  public ScoredObject<Store,Score> getScoredObject(int Index) {
//      return m_innerList.get(Index);
//  }

  public Store getFirst() {
      return ((ScoredObject<Store,Score>)first()).m_Store;
  }

  public Store getLast() {
      return ((ScoredObject<Store,Score>)last()).m_Store;
  }

  public Iterator<Store> getStoreIterator() {
      return new Iterator<Store>() {


            Iterator<ScoredObject<Store,Score>> innerIterator = iterator();


            public boolean hasNext() {
                return innerIterator.hasNext();
            }

            public Store next() {
                return innerIterator.next().getStore();
            }

            public void remove() {
                innerIterator.remove();
            }
        };
  }



//    /**
//    * replaces the object at position index with the given object.<br/>
//    * the nex object has then the score of the previous object
//    * @param index
//    * @param element
//    * @return the previously stored object
//    * @throws IndexOutOfBoundsException {@inheritDoc}
//    */
//    public Store set(int index, Object element) {
//        ScoredObject<Store,Score> o = (ScoredObject<Store, Score>) m_innerList.get(index);
//        Store oldVal = o.m_Store;
//        o.m_Store = (Store) element;
//        return oldVal;
//    }
//
    
//    /**
//    *
//    * @param e
//    * @param s
//    * @param maxPos
//    * @return
//    */
//    public boolean addMax(Store e, Score s, int maxPos) {
//        ScoredObject<Store,Score> o = new ScoredObject<Store, Score>(e, s);
//        return m_innerList.add(o);
//    }

//    public int size() {
//        return m_innerList.size();
//    }

//  /**
//   * retuyrns the comparator
//   * @return
//   */
//  public Comparator<Store> getComparator() {
//    return m_Comparator;
//  }
  

}
