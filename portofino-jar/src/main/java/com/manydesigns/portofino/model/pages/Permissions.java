/*
 * Copyright (C) 2005-2011 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * There are special exceptions to the terms and conditions of the GPL
 * as it is applied to this software. View the full text of the
 * exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
 * software distribution.
 *
 * This program is distributed WITHOUT ANY WARRANTY; and without the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
 * or write to:
 * Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307  USA
 *
 */

package com.manydesigns.portofino.model.pages;

import com.manydesigns.portofino.model.Model;
import com.manydesigns.portofino.model.ModelObject;
import com.manydesigns.portofino.model.ModelVisitor;
import org.apache.commons.collections.CollectionUtils;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.*;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla       - alessio.stalla@manydesigns.com
*/
@XmlAccessorType(XmlAccessType.NONE)
public class Permissions implements ModelObject {
    public static final String copyright =
            "Copyright (c) 2005-2011, ManyDesigns srl";

    public static final String NONE = "none";
    public static final String VIEW = "view";
    public static final String EDIT = "edit";

    //**************************************************************************
    // Fields
    //**************************************************************************

    protected final Set<String> none;
    protected final Set<String> view;
    protected final Set<String> edit;
    protected final Set<String> deny;

    protected final List<Permission> customPermissions;

    protected WithPermissions parent;

    protected final Set<String> actualDeny;
    //<action, set<group>>
    protected final Map<String, Set<String>> actualPermissions;

    //**************************************************************************
    // Constructors
    //**************************************************************************

    public Permissions() {
        none = new HashSet<String>();
        view = new HashSet<String>();
        edit = new HashSet<String>();
        deny = new HashSet<String>();

        customPermissions = new ArrayList<Permission>();
        
        actualPermissions = new HashMap<String, Set<String>>();
        actualDeny = new HashSet<String>();
    }

    //**************************************************************************
    // ModelObject implementation
    //**************************************************************************

    public void afterUnmarshal(Unmarshaller u, Object parent) {
        this.parent = (WithPermissions) parent;
    }

    public void reset() {
        actualPermissions.clear();
        actualDeny.clear();
    }

    public void init(Model model) {
        actualDeny.addAll(deny);

        //<group, set<permission>>
        Map<String, Set<String>> calculatedPermissions =
                new HashMap<String, Set<String>>();

        //Inherited permissions
        WithPermissions ancestor = (parent != null) ? parent.getParent() : null;
        if(ancestor != null) {
            actualDeny.addAll(ancestor.getPermissions().getActualDeny());

            //<action, set<group>>
            Map<String, Set<String>> parentPermissions =
                        ancestor.getPermissions().getActualPermissions();
            for(Map.Entry<String, Set<String>> entry : parentPermissions.entrySet()) {
                for(String group : entry.getValue()) {
                    Set<String> perms = calculatedPermissions.get(group);
                    if(perms == null) {
                        perms = new HashSet<String>();
                        calculatedPermissions.put(group, perms);
                    }
                    perms.add(entry.getKey());
                }
            }
        }

        //Local overrides
        for(String group : none) {
            HashSet<String> set = new HashSet<String>();
            set.add(NONE);
            calculatedPermissions.put(group, set);
        }
        for(String group : view) {
            HashSet<String> set = new HashSet<String>();
            set.add(VIEW);
            calculatedPermissions.put(group, set);
        }
        for(String group : edit) {
            HashSet<String> set = new HashSet<String>();
            set.add(VIEW);
            set.add(EDIT);
            calculatedPermissions.put(group, set);
        }

        //Reverse map: <group, set<permission>> --> <permission, set<group>>
        //(easier to check)
        actualPermissions.put(NONE, new HashSet<String>());
        actualPermissions.put(VIEW, new HashSet<String>());
        actualPermissions.put(EDIT, new HashSet<String>());

        for (Map.Entry<String, Set<String>> entry : calculatedPermissions.entrySet()) {
            for(String permission : entry.getValue()) {
                Set<String> groups = actualPermissions.get(permission);
                if(groups != null) {
                    groups.add(entry.getKey());
                } //else skip (custom permissions)
            }
        }

        //Custom permissions
        for(Permission perm : customPermissions) {
            actualPermissions.put(perm.getName(), perm.getGroups());
        }
    }

    public void link(Model model) {}

    public void visitChildren(ModelVisitor visitor) {}

    public String getQualifiedName() {
        return null;
    }

    //**************************************************************************
    // Permission verification
    //**************************************************************************

    public boolean isAllowed(String operation, List<String> groups) {
        //Administrators are always allowed?
        /*if(groups.contains(SecurityLogic.ADMINISTRATORS_GROUP_ID)) {
            return true;
        }*/

        //Deny wins over any other permission
        if (CollectionUtils.containsAny(actualDeny, groups)) {
            return false;
        }

        Set<String> perm = actualPermissions.get(operation);

        if(perm == null || perm.isEmpty()) {
            return false;
        }

        return CollectionUtils.containsAny(perm, groups);
    }

    //**************************************************************************
    // Getters/setters
    //**************************************************************************

    @XmlElementWrapper(name="none")
    @XmlElement(name = "group", type = java.lang.String.class)
    public Set<String> getNone() {
        return none;
    }

    @XmlElementWrapper(name="view")
    @XmlElement(name = "group", type = java.lang.String.class)
    public Set<String> getView() {
        return view;
    }

    @XmlElementWrapper(name="edit")
    @XmlElement(name = "group", type = java.lang.String.class)
    public Set<String> getEdit() {
        return edit;
    }


    @XmlElementWrapper(name="deny")
    @XmlElement(name = "group", type = java.lang.String.class)
    public Set<String> getDeny() {
        return deny;
    }

    @XmlElement(name = "permission", type = Permission.class)
    public List<Permission> getCustomPermissions() {
        return customPermissions;
    }

    public WithPermissions getParent() {
        return parent;
    }

    public void setParent(WithPermissions parent) {
        this.parent = parent;
    }

    public Map<String, Set<String>> getActualPermissions() {
        return actualPermissions;
    }

    public Set<String> getActualDeny() {
        return actualDeny;
    }
}