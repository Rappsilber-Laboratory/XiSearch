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
package rappsilber.ms.sequence;

import java.util.HashSet;


/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SequenceUtils {

      public static boolean containsAminoAcid(AminoAcidSequence s, AminoAcid aa) {
          for (int i = s.length(); --i>=0;) {
              if (s.aminoAcidAt(i).equals(aa)) {
                  return true;
              }
          }
          return false;
      }

      public static boolean containsAminoAcid(AminoAcidSequence s, HashSet<AminoAcid> aas) {
          for (int i = s.length(); --i>=0;) {
              if (aas.contains(s.aminoAcidAt(i))) {
                  return true;
              }
          }
          return false;
      }

      public static int countAminoAcid(AminoAcidSequence s, AminoAcid aa) {
          int count = 0;
          for (int i = s.length(); --i>=0;) {
              if (s.aminoAcidAt(i).equals(aa)) {
                  count++;
              }
          }
          return count;
      }

      public static int countAminoAcid(AminoAcidSequence s, HashSet<AminoAcid> aas) {
          int count = 0;
          for (int i = s.length(); --i>=0;) {
              if (aas.contains(s.aminoAcidAt(i))) {
                  count++;
              }
          }
          return count;
      }




}
