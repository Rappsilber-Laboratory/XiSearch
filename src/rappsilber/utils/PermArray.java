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

import java.util.Arrays;
import java.util.Iterator;

/**
 * Generates all permutations of a list of objects with repeated elements.
 * E.g. <code> [1,2,2]</code> will return <br/> <pre><code>
 * [1,2,2]
 * [2,1,2]
 * [2,2,1]</code></pre>
 * Translated from 
 * <pre>http://blog.bjrn.se/2008/04/lexicographic-permutations-using.html</pre>
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class PermArray<T extends Comparable> implements Iterable<T[]> {

    T[] seq;
    int first;
    int last;
    int next;
    boolean firstcall = true;

    public PermArray(T[] seq) {
        this.seq = seq.clone();
        Arrays.sort(this.seq);
        first = 0;
        this.last = seq.length;
        next = last-1;
    }
    
    
    
    
  public <T extends Comparable> void reverse(T[] seq, int start, int end) {
        // seq = seq[:start] + reversed(seq[start:end]) + \
        //       seq[end:]
        end -= 1;
        if (end <= start)
            return;
        while (true) {
            T d = seq[end];
            seq[end] = seq[start];
            seq[start] = d;
            if (start == end || start+1 == end)
                return;
            start += 1;
            end -= 1;
        }
    }
  
  
    public  boolean next_permutation()  {

//      not sure about that yet
//        if seq:
//            raise StopIteration

        if (firstcall) {
            firstcall = false;
            return true;
        }

        if (last == 1)
            return false;

        while (true) {
            next = last - 1;

            while (true) {
                int next1 = next;
                next -= 1;

                if (seq[next].compareTo(seq[next1]) < 0) {
                    // Step 2.
                    int mid = last - 1;
                    while (!(seq[next].compareTo(seq[mid]) < 0))
                        mid -= 1;
                    T d = seq[next];
                    seq[next] = seq[mid];
                    seq[mid] = d;

                    // Step 3.
                    reverse(seq, next1, last);
                            
                    return true;
                }
                if (next == first)
                    return false;
            }
        }
        //return false;
    }
    
    
    public static void main(String[] args) {
        String[] Seq = new String[]{"1","1","2","2","3"};
        PermArray<String> test = new PermArray<String>(Seq);
        
        while (test.next_permutation()) {
            System.out.println(MyArrayUtils.toString(test.seq,","));
        }
        
        for (String[] s : new PermArray<String>(test.seq)) {
            System.out.println(MyArrayUtils.toString(s, ";"));
        }
        
    }

    @Override
    public Iterator<T[]> iterator() {
        return new Iterator<T[]>() {
            boolean has_next = next_permutation();

            @Override
            public boolean hasNext() {
                return has_next;
            }

            @Override
            public T[] next() {
                T[] ret =  seq.clone();
                has_next = next_permutation();
                return ret;
            }
        };
    }
}
