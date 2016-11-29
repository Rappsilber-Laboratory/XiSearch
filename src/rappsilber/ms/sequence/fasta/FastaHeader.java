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
package rappsilber.ms.sequence.fasta;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FastaHeader {

    private String m_header;
    private String m_accession;
    private String m_name;
    private String m_genename;
    private String m_description;
    private boolean m_isSplit = false;
    
    private static Pattern m_re_nothing = Pattern.compile("^$");

    // accession numbers
    private static java.util.regex.Pattern[] m_PatternsAccesseion = new Pattern[]{
        Pattern.compile("^([\\w-]+)\\s+(?:\\w+)\\sprotein"), // SGD Header
        Pattern.compile("^(?:gi\\|(\\w+)\\|.*)"), // GI header
        Pattern.compile("^\\w+\\:(\\w+)\\W+(?:.*)"), // IPI
        Pattern.compile("^(?:REV_|rev_)?sp\\|([\\w-]+)\\|(?:.*)"), // sprot version 1
        Pattern.compile("^(?:REV_|rev_)?tr\\|([\\w-]+)\\|(?:.*)"), // sprot version 1
        Pattern.compile("^(\\w+)\\|(?:.*)"), //sprot version 2
        Pattern.compile("^([^\\s]*)\\s+[^\"]*\"(?:.*)\""), //sprot version 2
        Pattern.compile("^([\\w\\.]+)\\|(([^\\|]*)\\|([^\\|]*)\\|([^\\|]*))(\\|.*)?"), //sanger
        Pattern.compile("^\\s*([^\\s]+)\\s+(?:[^\\s]*)\\s+(?:[^\\.]+)"), // Jimi's
        Pattern.compile("^\\s*(?:[^\\:]\\:)([^\\.]+)"), // Jimi's
        Pattern.compile("^\\s*([^\\s]+)(?:\\s+.*)"), // simple accession number with space seperated description
    };
    // describtions
    private static java.util.regex.Pattern[] m_PatternsDescription = new Pattern[]{
        Pattern.compile("^([\\w-]+\\s+\\w+)\\sprotein"), // SGD Header
        Pattern.compile("^(?:gi\\|(?:\\w+)\\|(.*))"), // GI header
        Pattern.compile("^\\w+\\:(?:\\w+)\\W+(?:(?:.*)Gene_Symbol=(?:[^\\s]*))*(.*)"), // IPI
        Pattern.compile("^(?:REV_|rev_)?sp\\|(?:[\\w-]+)\\|(.*)"), // sprot version 1
        Pattern.compile("^(?:REV_|rev_)?tr\\|(?:[\\w-]+)\\|(.*)"), // sprot version 1
        Pattern.compile("^(?:\\w+)\\|(.*)"), //sprot version 2
        Pattern.compile("^(?:[^\\s]*)\\s+[^\"]*\"(.*)\""), //sprot version 2
        Pattern.compile("^(?:[\\w\\.]+)\\|(([^\\|]*)\\|([^\\|]*)\\|([^\\|]*))(\\|.*)?"), //sanger
        Pattern.compile("^\\s*(?:[^\\s]+)\\s+(?:[^\\s]*)\\s+([^\\.]+)"), // Jimi's
        Pattern.compile("^\\s*([^\\:]\\:)(?:[^\\.]+)"), // Jimi's
        Pattern.compile("^\\s*(.*[^\\s])\\s*"), // simple accession number with space seperated description
    };
    // name
    private static java.util.regex.Pattern[] m_PatternName = new Pattern[]{
        m_re_nothing, // SGD Header
        m_re_nothing, // GI header
        Pattern.compile("^\\w+\\:(?:\\w+)\\W+(?:.*)Gene_Symbol=([^\\s]*)(?:.*)"), // IPI
        Pattern.compile("^(?:REV_|rev_)?sp\\|(?:[\\w-]+)\\|([^\\s]*)(?:.*)"), // sprot version 1
        Pattern.compile("^(?:REV_|rev_)?tr\\|(?:[\\w-]+)\\|([^\\s]*)(?:.*)"), // sprot version 1
        m_re_nothing, //sprot version 2
        m_re_nothing, //sprot version 2
        m_re_nothing, //sanger
        m_re_nothing, // Jimi's
        m_re_nothing, // Jimi's
        m_re_nothing, // simple accession number with space seperated description
    };
    // gene name
    private static java.util.regex.Pattern[] m_PatternGeneName = new Pattern[]{
        m_re_nothing, // SGD Header
        m_re_nothing, // GI header
        Pattern.compile("^\\w+\\:(?:\\w+)\\W+(?:.*)Gene_Symbol=([^\\s]*)(?:.*)"), // IPI
        Pattern.compile("^(?:REV_|rev_)?sp\\|(?:[\\w-]+)\\|(?:.*)GN=([^\\s]*)(?:.*)"), // sprot version 1
        Pattern.compile("^(?:REV_|rev_)?tr\\|(?:[\\w-]+)\\|([?:.*]*)GN=([^\\s]*)(?:.*)"), // sprot version 1
        m_re_nothing, //sprot version 2
        m_re_nothing, //sprot version 2
        m_re_nothing, //sanger
        m_re_nothing, // Jimi's
        m_re_nothing, // Jimi's
        m_re_nothing, // simple accession number with space seperated description
    };


//    public static String
//
//    private static parseHeader() {
/*        SGD=$header =~ /^>([\w-]+)+\s+(\w+)/ or die "Couldn't match SGD accession";`0a$accession = $1;`0a$description = $2;`0a$header =~ /"([^"]+)"/;`0a$description .= " ".$1;`0a`0a`0a`0a
GI=$header =~ /^>(gi\|(\w+)\|.*)/ or die "couldn't match sprot accession";`0a$accession = 'GI:'.$2;`0a$description = $1;`0a`0a`0a`0a`0a`0a
IPI=`0a$header =~ /^>\w+\:(\w+)\W+(.*)/ or die "couldn't match ipi accession";`0a$accession = $1;`0a$description = $2;`0a`0a`0a`0a
MyFilter=$header =~ />\s*(?:[^\:]\:|)([^\.]+)/ or croak "Could not find accession in head";`0a$accession = $1;`0amy @parts = split(/\|/,$header);`0amy $end = pop @parts;`0a$end =~ s/[^\w\s]/ /g;`0a$end =~ s/\s+/ /g;`0a$description = substr($end,0,40);`0a`0a`0a`0a
Sprot2=$header =~ /^>sp\|(\w+)\|(.*)/ or die "couldn't match sprot accession";`0a$accession = $1;`0a$description = $2;`0a
Sprot=$header =~ /^>(\w+)\|(.*)/ or die "couldn't match sprot accession";`0a$accession = $1;`0a$description = $2;`0a
PA=eval($filter_conf{AUniversalFilter});`0a$description =~ s/\s+/./g;`0a($accession,$description) = ($description,$accession);`0a`0a
a_d_42=eval($filter_conf{AUniversalFilter});`0a`0a$description =~ s/\s+/-/g;`0a`0a$accession = "IPI:$accession";`0a`0amy $a_len = length($accession);`0amy $d_len = length($description);`0amy $o_len = 7;`0amy $max_len = 42;`0a`0aif($a_len + $o_len > $max_len){`0a  die "accession without description is too long";`0a}`0aif($a_len + $d_len + $o_len > $max_len){`0a      $description = substr(`0a               $description,`0a                0,`0a           $max_len - $a_len - $o_len`0a   );      `0a}`0a`0a`0a`0a
S*/


    
    public FastaHeader(String header, String accession, String name, String description) {
        this(header);
        this.m_accession = accession;
        this.m_name = name;
        this.m_description = description;
    }

    public FastaHeader(String header, String accession, String name, String genename, String description) {
        this(header);
        this.m_accession = accession;
        this.m_genename = genename;
        this.m_name = name;
        this.m_description = description;
    }

    public FastaHeader(String header) {
        this.m_header = header;
        splitHeader(header);
    }

    protected void splitHeader(String header) {
        
        boolean notFound = true;
        for (int i = 0; i < m_PatternsAccesseion.length; i++) {
            Pattern p = m_PatternsAccesseion[i];
            Matcher m = p.matcher(header);
            if (m.matches()) {
                m_accession = m.group(1);
                m = m_PatternsDescription[i].matcher(header);
                if (m.matches() && m.group(1).length()>0) {
                    m_description = m.group(1);
                } else {
                    m_description = header;
                }
                m = m_PatternName[i].matcher(header);
                if (m.matches() && m.group(1).length()>0) {
                    m_name = m.group(1);
                } else {
                    m_name = "";
                }
                m = m_PatternGeneName[i].matcher(header);
                if (m.matches() && m.group(1).length()>0) {
                    m_genename = m.group(1);
                } else {
                    m_genename = "";
                }
                notFound = false;
                m_isSplit = !m_accession.contentEquals(header);
                break;
            }
        }
        if (notFound) {
            this.m_accession = header;
            this.m_name = "";
            this.m_genename = "";
            this.m_description = header;
            m_isSplit = false;
        }
    }



    /**
     * @return the m_header
     */
    public String getHeader() {
        return m_header;
    }

    /**
     * @param m_header the m_header to set
     */
    public void setHeader(String header) {
        this.m_header = header;
        this.splitHeader(header);
    }

    /**
     * @return the m_accession
     */
    public String getAccession() {
        return m_accession;
    }

    /**
     * @param m_accession the m_accession to set
     */
    public void setAccession(String accession) {
        this.m_accession = accession;
    }

    /**
     * @return the m_name
     */
    public String getName() {
        return m_name;
    }

    /**
     * @param m_name the m_name to set
     */
    public void setName(String name) {
        this.m_name = name;
    }

    /**
     * @return the m_description
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * @param m_description the m_description to set
     */
    public void setDescription(String description) {
        this.m_description = description;
    }

    public boolean isSplit() {
        return m_isSplit;
    }

    /**
     * creates a 
     * @return 
     */
    public FastaHeader cloneHeader(String prefix){
        String decoyAccession = prefix + m_accession;
        String decoyHeader = m_header.replace(m_accession, decoyAccession);
        String decoyName = (m_name==null || m_name.isEmpty() ? null : prefix + m_name);
        String decoyGeneName = (m_genename == null || m_name.isEmpty() ? null :prefix + m_genename);
        String decoyDescribtion = m_description;
        
        if (m_name != null && (!m_name.isEmpty()) && !m_name.contentEquals(m_accession)) {
            decoyHeader = decoyHeader.replace(m_name, decoyName);
        }
        
        if (m_genename != null && (!m_genename.isEmpty())  && !m_genename.contentEquals(m_name))
            decoyHeader = decoyHeader.replace(m_genename, decoyGeneName);
        
        FastaHeader decoy = new FastaHeader(decoyHeader, decoyAccession, decoyName, decoyGeneName, decoyDescribtion);
        return decoy;
    }

}
