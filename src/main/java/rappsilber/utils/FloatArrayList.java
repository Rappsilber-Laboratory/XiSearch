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
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FloatArrayList implements Collection<Float>, List<Float>, RandomAccess {
    float[] list;
    int count=0;

    public FloatArrayList() {
        this(1);
    }
    
    public FloatArrayList(int capacity) {
        list = new float[capacity];
    }

    
    public FloatArrayList(float[] values) {
        this(values.length);
        addAll(values);
    }
    

    public FloatArrayList(Collection<Float> values) {
        this(values.size());
        addAll(values);
    }
    
    
    public int add(float value) {
        if (list.length == count) {
            int step = (list.length / 10) + 1;
            
            list = java.util.Arrays.copyOf(list, count + step);
        }
        list[count++] = value;
        return count;
    }
    
    public void add(int pos, float value) {
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

    public float set(int pos, float value) {
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
            float old = list[pos];
            list[pos] = value;
            return old;
        }
    }
    
    public Float get(int pos) {
        if (pos> count || pos <0) {
            throw new IndexOutOfBoundsException("Index out of range:"+ pos);
        }
        return list[pos];
    }
    
    public Float get(int pos, float defaultReturn) {
        if (pos> count || pos <0) {
            return defaultReturn;
        }
        return list[pos];
    }    
    
    public Float remove(int pos) {
        if (pos>= count || pos <0) {
            return null;
        }
        float ret = list[pos];
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
        if (o instanceof Float) {
            float c = (Float)o;
            for (int i = 0; i<count;i++) {
                if (list[i] == c) {
                    return true;
                }
            }
        }
        return false;
    }

    public Iterator<Float> iterator() {
        final FloatArrayList ial = this;
        return new Iterator<Float>() {
            float[] ilist = list;
            int icount = count;
            int inext = 0;

            public boolean hasNext() {
                return inext<icount;
            }

            public Float next() {
                return ilist[inext++];
            }

            public void remove() {
                ial.remove(inext-1);
            }
        };
    }

    
    public Object[] toArray() {
        Float[] a = new Float[count];
        for (int i = 0; i < count; i++) {
            a[i]=list[i];
        }
        return a;
    }

    public <T> T[] toArray(T[] a) {
        if (a instanceof Float[]) {
            if (a.length < count) {
                return toArray(a);
            } else {
                Float[] ta = new Float[count];
                for (int i = 0; i < count; i++) {
                    ta[i]=list[i];
                }
                return (T[])ta;
            }
        }
        throw new UnsupportedOperationException("Cannot convert float[] to the given type ("+ a.getClass().getSimpleName() + ")");
    }

    public float[] toFloatArray() {
        float[] ret = new float[count];
        for (int i=0; i<count;i++) {
            ret[i]=list[i];
        }
        return ret;
    }
    
    public boolean add(Float e) {
        return add((float)e) >=0;
    }

    public boolean remove(Object o) {
        if (o instanceof Float) {
            Float c = (Float) o;
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
        Collection<Float> ci = (Collection<Float>) c;
        for (Float i : ci) {
            if (!contains(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean addAll(Collection<? extends Float> c) {
        Collection<Float> ci = (Collection<Float>) c;
        for (Float i : ci) {
            if (!add(i)) {
                return false;
            }
        }
        return true;
    }


    
    public boolean addAll(FloatArrayList c) {
        return true;
    }

    
    public boolean addAll(float[] values) {
        if (list.length<count+values.length) {
            list = java.util.Arrays.copyOf(list, count + values.length);
        }
        for (int i = 0; i<values.length; i++) {
            list[count++] = values[i];
        }
        
        return true;
    }

    public boolean addAll(double[] values) {
        if (list.length<count+values.length) {
            list = java.util.Arrays.copyOf(list, count + values.length);
        }
        for (int i = 0; i<values.length; i++) {
            list[count++] = (float) values[i];
        }
        
        return true;
    }
    
    public boolean removeAll(Collection<?> c) {
        Collection<Float> ci = (Collection<Float>) c;
        for (Float i : ci) {
            if (!remove(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean retainAll(Collection<?> c) {
        Collection<Float> ci = (Collection<Float>) c;
        for (int i=count -1; i>=0;i--) {
            if (c.contains((Float)list[i])) {
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
    public float[] getRawArray() {
        return list;
    }
    
    @Override
    public FloatArrayList clone() {
        FloatArrayList ret = new FloatArrayList(0);
        ret.list = (float[])list.clone();
        ret.count = count;
        return ret;
    }
    
    /**
     * efficient way to add a value to all entries in the list
     * @param a 
     */
    public void addToAll(float a) {
        for (int i=count-1; i>=0; i--) {
            list[i]+=a;
        }
    }

    /**
     * efficient way to multiply each value in the list with the given value
     * @param a 
     */
    public void multiplyToAll(float a) {
        for (int i=count-1; i>=0; i--) {
            list[i]*=a;
        }
    }
    
    
    public static void main(String[] args) {
        FloatArrayList ial = new FloatArrayList();
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
        
        FloatArrayList ial2 = new FloatArrayList(new float[]{100,200,300});
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

    @Override
    public boolean addAll(int index, Collection<? extends Float> c) {

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
                for (Float i : c) {
                    list[fromindex++] = i;
                }
                
                count = newSize;
                
            } else {
                
                // we need to make space :(
                float[] values = new float[newSize];
                // copy everything upo to index
                System.arraycopy(list, 0, values, 0, index);
                // copy everything from index to the new place
                System.arraycopy(list, fromindex, values, toindex, count-index);
                
                // insert the entries
                for (Float i : c) {
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
                for (Float i : c) {
                    list[fromindex++] = i;
                }
            } else {

                java.util.Arrays.fill(list, count, index, 0);
                list = java.util.Arrays.copyOf(list, newSize);
                // insert the entries
                for (Float i : c) {
                    list[fromindex++] = i;
                }
            }
            count = newSize;
        }
        return true;
    }


    @Override
    public Float set(int pos, Float value) {
        if (list.length < pos+1) {
            java.util.Arrays.fill(list, count, list.length, 0);
            list = java.util.Arrays.copyOf(list, pos+1);
            count = pos + 1;
            list[pos] = value;
            return null;
        } else if (pos > count) {
            java.util.Arrays.fill(list, count, pos-1, 0);
            count = pos + 1;
            list[pos] = value;
            return null;
        } else {
            float old = list[pos];
            list[pos] = value;
            return old;
        }
    }

    @Override
    public void add(int pos, Float value) {
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
        float value = ((Number) o).floatValue();
        for (int i = 0; i < count; i++) {
            if (value == list[i]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        float value = ((Number) o).floatValue();
        for (int i = count -1; i >=0; i--) {
            if (value == list[i]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public ListIterator<Float> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<Float> listIterator(final int index) {
        final FloatArrayList ial = this;
        return new ListIterator<Float>() {
            int pos = index;
            @Override
            public boolean hasNext() {
                return pos<count;
            }

            @Override
            public Float next() {
                return list[pos++];
            }

            @Override
            public boolean hasPrevious() {
                return pos > 0;
            }

            @Override
            public Float previous() {
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
            public void set(Float e) {
                ial.set(pos-1,e);
            }

            @Override
            public void add(Float e) {
                ial.add(pos-1,e);
            }
        };
    }

    @Override
    public List<Float> subList(int fromIndex, int toIndex) {
        int newSize = toIndex- fromIndex;
        FloatArrayList ret = new FloatArrayList(newSize);
        
        for (int i =0; i< count;i++) {
            ret.list[i] = list[fromIndex++];
        }
        
        ret.count = newSize;
        
        return ret;
        
    }

}
