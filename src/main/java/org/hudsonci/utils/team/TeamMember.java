/*******************************************************************************
 *
 * Copyright (c) 2013 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Bob Foster
 *
 *******************************************************************************/ 

package org.hudsonci.utils.team;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.dom4j.Element;
import org.hudsonci.utils.team.Find;

/**
 * @author Bob Foster
 */
public class TeamMember {
    
    String memberName;
    Set<String> permissions = new TreeSet<String>();

    public String getName() {
        return memberName;
    }

    public Set<String> getPermissions() {
        return permissions;
    }
    
    public void read(Element parent, Find find, Map<String,TeamMember> memberMap, String suffix) {
        List<Element> elements = (List<Element>) parent.elements();
        if (elements.isEmpty()) {
            find.warn("Team member with no name or permissions");
            return;
        }
        String nameString = "";
        for (Element child : elements) {
            String name = child.getName();
            if ("name".equals(name)) {
                memberName = child.getTextTrim();
                if (memberName.isEmpty()) {
                    find.warn("Team member with empty name"+suffix);
                    return;
                } else if (memberMap.containsKey(memberName)) {
                    find.warn("Team member "+memberName+" is duplicate"+suffix);
                } else {
                    memberMap.put(memberName, this);
                    nameString = memberName + " ";
                }
            } else if ("permissions".equals(name)) {
                    String permissions = child.getTextTrim();
                    if (permissions.isEmpty()) {
                        find.warn("Team member "+nameString+"has empty permissions"+suffix);
                        continue;
                    }
                    for (String permission : permissions.split(",")) {
                        permission = permission.trim();
                        if (this.permissions.contains(permission)) {
                            find.warn("Team member "+nameString+"has duplicate permission "+permission+suffix);
                        }
                        this.permissions.add(permission);
                    }
            }
        }
        if (permissions.isEmpty()) {
            find.warn("Team member "+nameString+"has no permissions"+suffix);
        }
    }
        
    public void write(Element parent) {
        Team.addTextElement(parent, "name", memberName);
        String permCsv = Team.setToCsv(permissions);
        Team.addTextElement(parent, "permissions", permCsv);
    }
}
