/*
 * Copyright (c) 2013 bobfoster.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    bobfoster - initial API and implementation and/or initial documentation
 */

package org.hudsonci.utils.team;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.dom4j.Element;

/**
 *
 * @author Bob Foster
 */
public class TeamJob {
    String id;
    Set<String> visibilitySet = new TreeSet<String>();

    public String getId() {
        return id;
    }
    
    public Set<String> getVisibilities() {
        return visibilitySet;
    }

    public void read(Element parent, Find find, Map<String,TeamJob> jobMap, String suffix) {
        List<Element> elements = (List<Element>) parent.elements();
        if (elements.isEmpty()) {
            find.warn("Job with no id or visibility"+suffix);
            return;
        }
        boolean checkId = true;
        String nameString = "";
        for (Element child : elements) {
            String name = child.getName();
            if ("id".equals(name)) {
                String jobId = child.getTextTrim();
                if (jobMap.containsKey(jobId)) {
                    find.warn("Duplicate job "+jobId+suffix);
                    checkId = false;
                } else {
                    id = jobId;
                    nameString = "in job " + id + " ";
                    jobMap.put(id, this);
                }
            } else if ("visibility".equals(name)) {
                String rawVisibility = child.getTextTrim();
                String[] teams = rawVisibility.split(",");
                for (String team : teams) {
                    if (!visibilitySet.add(team.trim())) {
                        find.warn("Duplicate visibility "+team+" "+nameString+suffix);
                    }
                }
            }
        }
        if (checkId && id == null) {
            find.warn("Job with no id"+suffix);
        }
    }
    
    public void write(Element parent) {
        Team.addTextElement(parent, "id", id);
        if (!visibilitySet.isEmpty()) {
            Team.addTextElement(parent, "visibility", Team.setToCsv(visibilitySet));
        }
    }

}
