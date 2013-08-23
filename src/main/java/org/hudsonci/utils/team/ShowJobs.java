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

/**
 *
 * @author Bob Foster
 */
public class ShowJobs {

    public static void main(String[] args) {
        if (args.length != 1) {
            usage();
        }
        File dir = new File(args[0]);
        if (!dir.exists() || !dir.isDirectory()) {
            usage();
        }
        Find.scan(dir, 3, new Find.FileFunction() {
            public void call(File file) {
                File teamsDir = file.getParentFile();
                File homeDir = teamsDir.getParentFile();
                System.out.println(homeDir.getName());
                printJobs(homeDir, "  ");
                printTeams(teamsDir, "  ");
            }
        });
    }
    
    private static void printJobs(File dir, String indent) {
        File jobsDir = new File(dir, "jobs");
        if (jobsDir.exists() && jobsDir.isDirectory()) {
            for (File f : jobsDir.listFiles()) {
                if (f.isDirectory()) {
                    System.out.println(indent+f.getName());
                }
            }
        }
    }
    
    private static void printTeams(File teamsDir, String indent) {
        for (File team : teamsDir.listFiles()) {
            if (team.isDirectory()) {
                System.out.println(indent+team.getName());
                printJobs(team, indent+"  ");
            }
        }
    }
    
    private static void usage() {
        System.out.println("Usage: java -cp findteambugs.jar org.hudsonci.utils.team.ShowJobs DIR");
        System.out.println("       Where DIR is directory to search");
        System.exit(1);
    }
}
