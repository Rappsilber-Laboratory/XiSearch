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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * a memory saving implementation of ArrayList, that is not using an array of objects but an array of values (no extra referencing)
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class IntArrayList implements Collection<Integer>, List<Integer>, RandomAccess {
    int[] list;
    int count=0;

    public IntArrayList() {
        this(1);
    }
    
    public IntArrayList(int capacity) {
        list = new int[capacity];
    }

    
    public IntArrayList(int[] values) {
        this(values.length);
        addAll(values);
    }
    

    public IntArrayList(Collection<Integer> values) {
        this(values.size());
        addAll(values);
    }
    
    
    public int add(int value) {
        if (list.length == count) {
            int step = (list.length / 10) + 1;
            
            list = java.util.Arrays.copyOf(list, count + step);
        }
        list[count++] = value;
        return count;
    }
    
    public void add(int pos, int value) {
        if (pos>count) {
            set(pos, value);
            return;
        }
            
        if (list.length == count) {
            int step = (list.length / 10) + 1;
            
            list = java.util.Arrays.copyOf(list, count + step);
        }
        System.arraycopy(list, pos, list, pos+1, count-pos);
        count++;
        list[pos] = value;
    }

    public int set(int pos, int value) {
        if (list.length < pos+1) {
            java.util.Arrays.fill(list, count, list.length, 0);
            list = java.util.Arrays.copyOf(list, pos+1);
            count = pos + 1;
            list[pos] = value;
            return 0;
        } else if (pos > count) {
            java.util.Arrays.fill(list, count, pos-1, 0);
            count = pos + 1;
            list[pos] = value;
            return 0;
        } else if (pos == count) {
            count++;
            list[pos] = value;
            return 0;
        } else {
            int old = list[pos];
            list[pos] = value;
            return old;
        }
    }
    
    public Integer get(int pos) {
        if (pos> count || pos <0) {
            throw new IndexOutOfBoundsException("Index out of range:"+ pos);
        }
        return list[pos];
    }
    
    public Integer remove(int pos) {
        if (pos>= count || pos <0) {
            return 0;
        }
        int ret = list[pos];
        System.arraycopy(list, pos+1, list, pos, count-pos - 1);
        count--;
        return ret;
    }
    
    

    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count==0;
    }

    public boolean contains(Object o) {
        if (o instanceof Integer) {
            int c = (Integer)o;
            for (int i = 0; i<count;i++) {
                if (list[i] == c) {
                    return true;
                }
            }
        }
        return false;
    }

    public Iterator<Integer> iterator() {
        final IntArrayList ial = this;
        return new Iterator<Integer>() {
            int[] ilist = list;
            int icount = count;
            int inext = 0;

            public boolean hasNext() {
                return inext<icount;
            }

            public Integer next() {
                return ilist[inext++];
            }

            public void remove() {
                ial.remove(inext-1);
            }
        };
    }

    
    public Object[] toArray() {
        Integer[] a = new Integer[count];
        for (int i = 0; i < count; i++) {
            a[i]=list[i];
        }
        return a;
    }

    public <T> T[] toArray(T[] a) {
        if (a instanceof Integer[]) {
            if (a.length < count) {
                return toArray(a);
            } else {
                Integer[] ta = new Integer[count];
                for (int i = 0; i < count; i++) {
                    ta[i]=list[i];
                }
                return (T[])ta;
            }
        }
        throw new UnsupportedOperationException("Cannot convert int[] to the given type ("+ a.getClass().getSimpleName() + ")");
    }

    public int[] toIntArray() {
        int[] ret = new int[count];
        for (int i=0; i<count;i++) {
            ret[i]=list[i];
        }
        return ret;
    }
    
    public boolean add(Integer e) {
        return add((int)e) >=0;
    }

    public boolean remove(Object o) {
        if (o instanceof Integer) {
            Integer c = (Integer) o;
            for (int i=0; i< count; i++) {
                if (list[i] == c) {
                    remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        Collection<Integer> ci = (Collection<Integer>) c;
        for (Integer i : ci) {
            if (!contains(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean addAll(Collection<? extends Integer> c) {
        Collection<Integer> ci = (Collection<Integer>) c;
        if (list.length < count + c.size()) {
            int[] temp = new int[count+ c.size()];
            System.arraycopy(list, 0, temp, 0, count);
            list = temp;
        }
        for (Integer i : c) {
            list[count++] = i;
        }
        return true;
    }

    public boolean addAll(IntArrayList c) {
        if (list.length<count+c.size()) {
            list = java.util.Arrays.copyOf(list, count + c.size());
        }
        System.arraycopy(c.list, 0, list, count, c.size());
        
        return true;
    }

    
    public boolean addAll(int[] values) {
        if (list.length<count+values.length) {
            list = java.util.Arrays.copyOf(list, count + values.length);
        }
        for (int i = 0; i<values.length; i++) {
            list[count++] = values[i];
        }
        
        return true;
    }

    public boolean removeAll(Collection<?> c) {
        Collection<Integer> ci = (Collection<Integer>) c;
        for (Integer i : ci) {
            if (!remove(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean retainAll(Collection<?> c) {
        Collection<Integer> ci = (Collection<Integer>) c;
        for (int i=count -1; i>=0;i--) {
            if (c.contains((Integer)list[i])) {
                if (remove(i)<0) {
                    return false;
                }
            }
        }
        return true;
        
    }

    public void clear() {
        count = 0;
    }

    /**
     * returns the actual storage array.
     * <br/>Be careful there are likely more entries in the array as the list supposed to have.
     * These are stand-ins for later points in time.
     * Also the array that is actually stores the data might change (e.g. by adding or removing elements from the list) and the changes to the array would not persist.
     * 
     * <br/></br><b>So be warned...!</b>
     * 
     * <br/></br><b>This is utterly not thread-safe</b>
     */
    public int[] getRawArray() {
        return list;
    }
    
    @Override
    public IntArrayList clone() {
        IntArrayList ret = new IntArrayList(0);
        ret.list = (int[])list.clone();
        ret.count = count;
        return ret;
    }
    
    /**
     * efficient way to add a value to all entries in the list
     * @param a 
     */
    public void addToAll(int a) {
        for (int i=count-1; i>=0; i--) {
            list[i]+=a;
        }
    }

    /**
     * efficient way to multiply each value in the list with the given value
     * @param a 
     */
    public void multiplyToAll(int a) {
        for (int i=count-1; i>=0; i--) {
            list[i]*=a;
        }
    }
    
    @Override
    public Integer set(int pos, Integer value) {
        if (list.length < pos+1) {
            java.util.Arrays.fill(list, count, list.length, 0);
            list = java.util.Arrays.copyOf(list, pos+1);
            count = pos + 1;
            list[pos] = value;
            return 0;
        } else if (pos > count) {
            java.util.Arrays.fill(list, count, pos-1, 0);
            count = pos + 1;
            list[pos] = value;
            return 0;
        } else {
            int old = list[pos];
            list[pos] = value;
            return old;
        }
    }

    @Override
    public void add(int pos, Integer value) {
        if (pos>count) {
            set(pos, value);
            return;
        }
            
        if (list.length == count) {
            int step = (list.length / 10) + 1;
            
            list = java.util.Arrays.copyOf(list, count + step);
        }
        System.arraycopy(list, pos, list, pos+1, count-pos);
        count++;
        list[pos] = value;
    }


    @Override
    public int indexOf(Object o) {
        int value = ((Number) o).intValue();
        for (int i = 0; i < count; i++) {
            if (value == list[i]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int value = ((Number) o).intValue();
        for (int i = count -1; i >=0; i--) {
            if (value == list[i]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public ListIterator<Integer> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<Integer> listIterator(final int index) {
        final IntArrayList ial = this;
        return new ListIterator<Integer>() {
            int pos = index;
            @Override
            public boolean hasNext() {
                return pos<count;
            }

            @Override
            public Integer next() {
                return list[pos++];
            }

            @Override
            public boolean hasPrevious() {
                return pos > 0;
            }

            @Override
            public Integer previous() {
                return list[--pos];
            }

            @Override
            public int nextIndex() {
                return pos;
            }

            @Override
            public int previousIndex() {
                return pos-1;
            }

            @Override
            public void remove() {
                ial.remove(pos-1);
            }

            @Override
            public void set(Integer e) {
                ial.set(pos-1,e);
            }

            @Override
            public void add(Integer e) {
                ial.add(pos-1,e);
            }
        };
    }

    @Override
    public List<Integer> subList(int fromIndex, int toIndex) {
        int newSize = toIndex- fromIndex;
        IntArrayList ret = new IntArrayList(newSize);
        
        for (int i =0; i< count;i++) {
            ret.list[i] = list[fromIndex++];
        }
        
        ret.count = newSize;
        
        return ret;
        
    }


    @Override
    public boolean addAll(int index, Collection<? extends Integer> c) {

        int fromindex = index;

        if (index < count) {
            // we need to copy at leats parts of the list
            int newSize = count + c.size();
            
            int toindex= index+c.size();
            
            // we have enough space
            if (newSize <= list.length) {
                
                // shift entries to a later space
                int copycount = count - index + 1;
                System.arraycopy(list, fromindex, list, toindex, copycount);
                // insert the entries
                for (Integer i : c) {
                    list[fromindex++] = i;
                }
                
                count = newSize;
                
            } else {
                
                // we need to make space :(
                int[] values = new int[newSize];
                // copy everything upo to index
                System.arraycopy(list, 0, values, 0, index);
                // copy everything from index to the new place
                System.arraycopy(list, fromindex, values, toindex, count-index);
                
                // insert the entries
                for (Integer i : c) {
                    values[fromindex++] = i;
                }
                list = values;
                count = newSize;
                
            }
        } else {
            int newSize = index + c.size();
            // we "just" need to add them at the end
            if (newSize < this.list.length) {
                java.util.Arrays.fill(list, count, index, 0);
                // insert the entries
                for (Integer i : c) {
                    list[fromindex++] = i;
                }
            } else {

                java.util.Arrays.fill(list, count, index, 0);
                list = java.util.Arrays.copyOf(list, newSize);
                // insert the entries
                for (Integer i : c) {
                    list[fromindex++] = i;
                }
            }
            count = newSize;
        }
        return true;
    }
    
    public static void main(String[] args) {
        IntArrayList ial = new IntArrayList();
        ial.add(0);
        ial.add(1);
        ial.add(2);
        ial.add(3);
        ial.add(5);
        ial.add(6);
        ial.add(7);
        ial.add(7);
        ial.add(8);
        ial.add(9);
        ial.add(9);
        ial.add(4, 4);
        ial.remove(7);
        ial.set(10,10);
        ial.set(11,11);
        ial.set(12,12);

        System.out.println("initial list :");
        for (int i =0; i < ial.size(); i++) {
            System.out.println(i + " : "  + ial.get(i));
        }
        
        ial.remove(12);
        System.out.println("removed index 12 :");
        for (int i =0; i < ial.size(); i++) {
            System.out.println(i + " : "  + ial.get(i));
        }

        ial.remove(11);

        System.out.println("removed index 11 :");
        for (int i =0; i < ial.size(); i++) {
            System.out.println(i + " : "  + ial.get(i));
        }
        
        
        ial.set(16,15);

        System.out.println("index index 16 to 15:");
        for (int i =0; i < ial.size(); i++) {
            System.out.println(i + " : "  + ial.get(i));
        }
        
        ial.remove(11);
        System.out.println("removed index 11 :");
        for (int i =0; i < ial.size(); i++) {
            System.out.println(i + " : "  + ial.get(i));
        }
        
        IntArrayList ial2 = new IntArrayList(new int[]{100,200,300});
        System.out.println("initialised second list :");
        for (int i =0; i < ial2.size(); i++) {
            System.out.println(i + " : "  + ial2.get(i));
        }
        
        ial.addAll(2, ial2);

        System.out.println("added second list to first at index 2 :");
        for (int i =0; i < ial.size(); i++) {
            System.out.println(i + " : "  + ial.get(i));
        }
        
    }



}
