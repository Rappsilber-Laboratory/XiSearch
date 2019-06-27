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

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class Version {
    /** build - svn-revision*/
    public int build;
    /** extension */
    public String extension;
    /** the major number of the version*/
    public int major;
    /** the minor number of the version*/
    public int minor;

    public Version(int major, int minor, int build) {
        this.major = major;
        this.minor = minor;
        this.build = build;
        this.extension="";
    }
    public Version(int major, int minor, int build, int extension) {
        this.major = major;
        this.minor = minor;
        this.build = build;
        this.extension=""+extension;
    }

    public Version(int major, int minor, String svn_refbuild) {
        this.major = major;
        this.minor = minor;
        this.build = Integer.parseInt(svn_refbuild.replaceAll("\\$Rev:\\s*", "").replaceAll("\\s*\\$", ""));
    }
    
    public Version(int major, int minor, String buidl_svn_refbuild, String extension) {
        this.major = major;
        this.minor = minor;
        this.build = Integer.parseInt(buidl_svn_refbuild.replaceAll("\\$Rev:\\s*", "").replaceAll("\\s*\\$", ""));
        this.extension = extension;
    }
    
    
    
    public Version(int major, int minor, int build, String ext_svn_refbuild) {
        this.major = major;
        this.minor = minor;
        this.build = build;
        this.extension = ""+Integer.parseInt(ext_svn_refbuild.replaceAll("\\$Rev:\\s*", "").replaceAll("\\s*\\$", ""));
    }
    
    public String setExtension(String svn_refbuild) {
        if (svn_refbuild.matches("\\$Rev:\\s*[0-9]+\\s*\\$"))
            this.extension = ""+Integer.parseInt(svn_refbuild.replaceAll("\\$Rev:\\s*", "").replaceAll("\\s*\\$", ""));
        else {
            this.extension = svn_refbuild;
        }
        return this.extension;
    }

    public String toLongString() {
        if ( extension == "0")
            return String.format("%02d.%02d.%07d", major ,minor ,build);
        return String.format("%02d.%02d.%02d.%07d", major ,minor ,build, extension);
    }

    @Override
    public String toString() {
        
        return major + "." + minor + "." + build + (extension == null || extension.isEmpty() ? "":  "."+extension );
    }
}
