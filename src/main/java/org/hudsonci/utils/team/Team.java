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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.dom4j.Element;

/**
 *
 * @author Bob Foster
 */
public class Team {
    String teamName;
    String description;
    String customFolderName;
    
    boolean isPublic;

    Map<String,TeamJob> jobMap = new TreeMap<String,TeamJob>();
    Map<String,TeamMember> memberMap = new TreeMap<String,TeamMember>();
   
    public Team() {
    }
    
    public String getName() {
        return teamName;
    }

    public String getDescription() {
        return description;
    }

    public String getCustomFolderName() {
        return customFolderName;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean containsJob(String name) {
        return jobMap.containsKey(name);
    }
    
    public Collection<String> getJobNames() {
        return jobMap.keySet();
    }
    
    public Collection<TeamJob> getJobs() {
        return jobMap.values();
    }
    
    public Collection<TeamMember> getMembers() {
        return memberMap.values();
    }
    
    public void read(Element parent, Find find, Map<String,Team> teamMap) {
        List<Element> elements = (List<Element>) parent.elements();
        if (elements.isEmpty()) {
            find.warn("<team> with no name, members or jobs");
            return;
        }
        String nameString = "";
        for (Element child :  elements) {
            String name = child.getName();
            if ("name".equals(name)) {
                teamName = child.getTextTrim();
                if (teamName.isEmpty()) {
                    find.warn("Team with empty name");
                    return;
                }
                if (teamMap.containsKey(teamName)) {
                    find.warn("Duplicate team "+teamName);
                    return;
                }
                teamMap.put(teamName, this);
                nameString = " in team "+teamName+" ";
                if ("public".equals(teamName)) {
                    isPublic = true;
                }
            } else if ("description".equals(name)) {
                description = child.getTextTrim();
                if (description.isEmpty()) {
                    find.fine("Empty description"+nameString);
                }
            } else if ("customFolderName".equals(name)) {
                customFolderName = child.getTextTrim();
                if (customFolderName.isEmpty()) {
                    find.fine("Empty customFolderName "+nameString);
                }
            } else if ("job".equals(name)) {
                TeamJob job = new TeamJob();
                job.read(child, find, jobMap, nameString);
            } else if ("member".equals(name)) {
                TeamMember member = new TeamMember();
                member.read(child, find, memberMap, nameString);
            }
        }
        if (teamName == null) {
            find.warn("Team with no name");
            return;
        }
        if (description == null) {
            description = teamName;
        }
    }
    
    public void write(Element parent) {
        addTextElement(parent, "name", teamName);
        if (description != null && !description.isEmpty()) {
            addTextElement(parent, "description", description);
        }
        if (customFolderName != null && !customFolderName.isEmpty()) {
            addTextElement(parent, "customFolderName", customFolderName);
        }
        for (TeamMember member : memberMap.values()) {
            Element memberElement = parent.addElement("member");
            member.write(memberElement);
        }
        for (TeamJob job : jobMap.values()) {
            Element jobElement = parent.addElement("job");
            job.write(jobElement);
        }
    }
    
    static Element addTextElement(Element parent, String name, String text) {
        Element element = parent.addElement(name);
        element.setText(text);
        return element;
    }
    
    static String setToCsv(Set<String> set) {
        // Java missing join
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String s : set) {
            if (i++ > 0) {
                sb.append(',');
            }
            sb.append(s);
        }
        return sb.toString();
    }
}
