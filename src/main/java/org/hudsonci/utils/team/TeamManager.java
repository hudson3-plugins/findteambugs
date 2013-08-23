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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * Pseudo TeamManager class as data container
 * @author Bob Foster
 */
public class TeamManager {

    Set<String> sysAdmins = new TreeSet<String>();
    Map<String,Team> teamMap = new TreeMap<String,Team>();
    
    public Set<String> getSysAdmins() {
        return sysAdmins;
    }
    
    public Team getTeam(String name) {
        return teamMap.get(name);
    }
    
    public Collection<Team> getTeams() {
        return teamMap.values();
    }
    
    public boolean containsTeam(String name) {
        return teamMap.containsKey(name);
    }

    public boolean read(File file, Find find) {
        
        SAXReader reader = new SAXReader();
        try {
            Document inDoc = reader.read(file);
            Element root = inDoc.getRootElement();
            if (!"teamManager".equals(root.getName())) {
                find.error(file.getAbsolutePath()+" skipped; format error");
                find.error("Root element must be teamManager");
                return false;
            }
            find.verbose("Scanning "+file.getAbsolutePath());
            
            for (Element child :  (List<Element>) root.elements()) {
                String name = child.getName();
                if ("sysAdmin".equals(name)) {
                    String admin = child.getTextTrim();
                    if (!sysAdmins.add(admin)) {
                        find.warn("Duplicate sysAdmin "+admin);
                    }
                } else if ("team".equals(name)) {
                    Team team = new Team();
                    team.read(child, find, teamMap);
                }
            }
            return true;
        } catch (DocumentException e) {
            find.exception(file, e, "Error reading teams.xml");
        }
        return false;
    }
    
    public void writeTeamsXml(File file) throws IOException {
        Document outDoc = DocumentHelper.createDocument();
        Element root = outDoc.addElement("teamManager");
        for (String admin : sysAdmins) {
            Team.addTextElement(root, "sysAdmin", admin);
        }
        for (Team team : teamMap.values()) {
            Element teamElement = root.addElement("team");
            team.write(teamElement);
        }
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(new FileWriter(file), format);
        try {
            writer.write(outDoc);
        } finally {
            writer.close();
        }
    }
}
