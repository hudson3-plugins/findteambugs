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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hudsonci.Cmd;
import org.hudsonci.Returns;

/**
 * Find team and team job errors, report and optionally fix.
 * @author Bob Foster
 */
public class Find {
    public static void main( String[] args )
    {
        System.exit(doIt(args));
    }
    
    static Set<Integer> argSet = new TreeSet<Integer>();
    
    private static boolean arg(String[] args, char letter, String word) {
        int len = word.length();
        int n = 0;
        for (String arg : args) {
            if (arg.startsWith("--")) {
                if (len == arg.length()-2 && word.equals(arg.substring(2))) {
                    argSet.add(n);
                    return true;
                }
            } else if (arg.startsWith("-")) {
                String chars = arg.substring(1);
                for (int i = 0; i < chars.length(); i++) {
                    if (chars.charAt(i) == letter) {
                        argSet.add(n);
                        return true;
                    }
                }
            }
            n++;
        }
        return false;
    }
    
    private static String arg(String[] args, int i) {
        int n = 0;
        for (String arg : args) {
            if (!arg.startsWith("-") && n++ == i) {
                argSet.add(n);
                return arg;
            }
        }
        return null;
    }
    
    // for testability
    static int doIt(String[] args) {
        boolean verbose = !arg(args, 'q', "quiet");
        boolean fix = arg(args, 'f', "fix");
        String homePath = arg(args, 0);
        if (homePath == null || args.length > argSet.size()) {
            return usage();
        }
        File file = new File(homePath);
        if (!file.exists() || !file.isDirectory()) {
            return usage();
        }
        Find app = new Find();
        app.scan(file, verbose);
        int returnCode = app.errors();
        if (returnCode == 0 && fix) {
            returnCode = app.doActions();
        }
        if (returnCode != 0) {
            System.err.println(""+returnCode+" errors detected"+(verbose ? "" : "; use -v or --verbose for details"));
            System.err.println("The teams.xml file is unchanged");
        };
        return -returnCode;
    }

    private static int usage() {
        System.out.println("Usage: java -jar findteambugs.jar [ SWITCHES ] HUDSON_HOME");
        System.out.println("       where HUDSON_HOME is a path to valid Hudson home");
        System.out.println("                          to scan for team-related bugs");
        System.out.println("       where SWITCHES are:");
        System.out.println("         -q | --quiet     Don't write to standard output (optional)");
        System.out.println("         -f | --fix       Fix team-related bugs (optional)");
        System.out.println("                          Removes orphan jobs and badly named jobs");
        System.out.println("                          from disk, and missing jobs from teams.xml");
        System.out.println("                          Back up Hudson home before fixing.");
        System.out.println("       Run with no switches to see bugs detected.");
        return 1;
    }
    
    public Find() {
        actionsStringWriter = new StringWriter();
        actionsWriter = new PrintWriter(actionsStringWriter);
    }
    
    boolean verbose;
    StringWriter actionsStringWriter;
    PrintWriter actionsWriter;
    int errors = 0;
    File hudsonHomeDir;
    File hudsonJobsDir;
    File hudsonTeamsDir;
    private TeamManager tm;
        
    private int errors() {
        return errors;
    }
    
    /**
     * Function used with scan.
     */
    public static interface FileFunction {
        void call(File file);
    }
    
    private void scan(File file, final boolean verbose) {
        FileFunction fun = new FileFunction() {
            public void call(File f) {
                convert(f, verbose);
            }
        };
        if (!scan(file, 3, fun)) {
            error("Failed to find teams.xml within three directory levels");
        }
    }
    
    /**
     * Scan a directory up to n levels deep and apply FileFunction to
     * any teams.xml file encountered. Used in Find and ShowJobs.
     * @param file directory; either a Hudson home or outer dir
     * @param level number of directory levels to scan
     * @param fun function to call for teams.xml files
     * @return 
     */
    public static boolean scan(File file, int level, FileFunction fun) {
        File[] files = file.listFiles();
        boolean found = false;
        for (File f : files) {
            if (f.isDirectory() && level > 0) {
                if (scan(f, level - 1, fun)) {
                    found = true;
                }
            } else if (f.isFile() && "teams.xml".equals(f.getName())) {
                fun.call(f);
                found = true;
            }
        }
        return found;
    }

    private interface Action {
        void write();
        void run();
    }
    
    private abstract class WriteAction implements Action {
        public void run() {}
    }
    
    private abstract class RunAction implements Action {
        public void write() {}
    }
    
    // RunActions stick around for multiple projects
    private List<Action> runActions = new ArrayList<Action>();
    
    // Other actions only apply to a single project
    private List<Action> actions = null;
    
    private int doActions() {
        // Read and apply the actions
        info("Beginning actions...");
        actionsWriter.close();
        StringBuffer sb = actionsStringWriter.getBuffer();
        BufferedReader reader = new BufferedReader(new StringReader(sb.toString()));
        try {
        String line;
            while ((line = reader.readLine()) != null) {
                Returns returns = Cmd.run(line);
                if (returns.getResult() != 0) {
                    error("Action failed: "+line);
                    for (String log : returns.getOutput()) {
                        verbose(log);
                    }
                    return returns.getResult();
                } else {
                    info(line);
                }
            }
        } catch (IOException e) {
            // Can't happen
        }
        for (Action action : runActions) {
            action.run();
        }
        return 0;
    }
        
    private class Remove extends WriteAction {
        File dir;
        
        public Remove(File dir) {
            this.dir = dir;
        }
        
        public void write() {
            // folder has already been verified to be on disk
            if (File.separatorChar == '/') {
                actionsWriter.println("rm -rf \""+dir.getAbsolutePath()+"\"");
            } else {
                actionsWriter.println("rmdir \""+dir.getAbsolutePath()+"\" /s /q");
            }
        }
    }
    
    private class RemoveFile extends WriteAction {
        File file;
        
        public RemoveFile(File file) {
            this.file = file;
        }
        
        public void write() {
            // folder has already been verified to be on disk
            if (File.separatorChar == '/') {
                actionsWriter.println("rm \""+file.getAbsolutePath()+"\"");
            } else {
                actionsWriter.println("del \""+file.getAbsolutePath()+"\"");
            }
        }
    }
    
    private class Backup extends WriteAction {
        File file;
        
        public Backup(File file) {
            this.file = file;
        }
        
        public void write() {
            File bakFile = getUniqueBakFile(file);
            String oldPath = file.getAbsolutePath();
            String newPath = bakFile.getAbsolutePath();
            if (File.separatorChar == '/') {
                actionsWriter.println("mv \""+oldPath+"\" \""+newPath+"\"");
            } else {
                actionsWriter.println("copy \""+oldPath+"\" \""+newPath+"\" /A /Y");
                actionsWriter.println("del \""+oldPath+"\"");
            }
        }
    }
    
    private class WriteTeamsXml extends RunAction {
        File file;
        TeamManager tm;
        
        public WriteTeamsXml(File file, TeamManager tm) {
            this.file = file;
            this.tm = tm;
        }
        
        public void run() {
            try {
                tm.writeTeamsXml(file);
            } catch (IOException ex) {
                exception(file, ex, "Writing fixed teams.xml");
            }
        }
    }
    
    private void init(File file, boolean verbose) {
        this.verbose = verbose;
        hudsonTeamsDir = file.getParentFile();
        hudsonHomeDir = hudsonTeamsDir.getParentFile();
        hudsonJobsDir = new File(hudsonHomeDir, "jobs");
        
        tm = new TeamManager();
        actions = new ArrayList<Action>();
    }

    private void convert(File file, boolean verbose) {
        init(file, verbose);
        convert(file);
    }
    
    private void convert(File file) {
        if (!tm.read(file, this)) {
            error("Could not read teams.xml file "+file.getAbsolutePath());
            error("This Hudson home skipped ");
            return;
        }
        
        verifyJobsOnDisk();

        verifyOnlyJobsOnDisk();
        
        verifyJobVisibility();

        actions.add(new Backup(file));
        runActions.add(new WriteTeamsXml(file, tm));

        writeActions();
    }
    
    private File getUniqueBakFile(File file) {
        StringBuilder sb = new StringBuilder(file.getName());
        sb.append(".bak");
        int base = sb.length();
        File parent = file.getParentFile();
        for (int i = 1; ; i++) {
            if (i > 1) {
                sb.setLength(base);
                sb.append(i);
            }
            File test = new File(parent, sb.toString());
            if (!test.exists()) {
                return test;
            }
        }
    }
    
    private void writeActions() {
        if (!actions.isEmpty()) {
            for (Action action : actions) {
                action.write();
            }
        } else {
            warn("There are no actions for this teams.xml");
        }
    }
    
    private File getJobDir(Team team, String jobName) {
        if (team.isPublic) {
            return new File(hudsonJobsDir, jobName);
        }
        return new File(getTeamJobsDir(team), jobName);
    }
    
    private File getTeamJobsDir(Team team) {
        File teamDir = team.isPublic() ? hudsonHomeDir : new File(hudsonTeamsDir, team.teamName);
        return new File(teamDir, "jobs");
    }
    
    private void verifyJobsOnDisk() {
        for (Team team : tm.getTeams()) {
            for (Iterator<String> it = team.getJobNames().iterator(); it.hasNext(); ) {
                String jobName = it.next();
                File jobDir = getJobDir(team, jobName);
                if (!jobDir.exists() || !jobDir.isDirectory()) {
                    warn("Team "+team.getName()+" job "+jobName+" not found at "+jobDir.getAbsolutePath());
                    it.remove();
                }
            }
        }
    }
    
    private void verifyOnlyJobsOnDisk() {
        // If there are any orphan teams on disk that aren't in teams.xml, remove them
        for (File f : hudsonTeamsDir.listFiles()) {
            if (f.isDirectory()) {
                Team team = tm.getTeam(f.getName());
                if (team == null) {
                    info("Orphan team at "+f.getAbsolutePath());
                    actions.add(new Remove(f));
                }
            }
        }
        // if there are any dirs in jobs folders that aren't in teams.xml, remove them
        // Don't check public team jobs - they aren't in teams.xml
        for (Team team : tm.getTeams()) {
            if (!team.isPublic()) {
                File teamJobsDir = getTeamJobsDir(team);
                if (teamJobsDir.exists() && teamJobsDir.isDirectory()) {
                    for (File jobFile : teamJobsDir.listFiles()) {
                        if (jobFile.isDirectory()) {
                            String jobName = jobFile.getName();
                            if (!team.containsJob(jobName)) {
                                info("Orphan job "+jobName+" at "+jobFile.getAbsolutePath());
                                actions.add(new Remove(jobFile));
                            }
                        } else {
                            info("Job folder contains non-directory: "+jobFile.getAbsolutePath());
                            actions.add(new RemoveFile(jobFile));
                        }
                    }
                }
            }
        }
    }
    
    private void verifyJobVisibility() {
        // If there are any visibilites that aren't teams, remove them
        for (Team team : tm.getTeams()) {
            for (TeamJob job : team.getJobs()) {
                for (Iterator<String> it = job.getVisibilities().iterator(); it.hasNext(); ) {
                    String visibility = it.next();
                    if (!tm.containsTeam(visibility)) {
                        info("Job "+job.getId()+" visibility "+visibility+" is not a team");
                        it.remove();
                    }
                    
                }
            }
        }
    }
    
    public void info(String msg) {
        verbose("INFO: "+msg);
    }
    
    public void warn(String msg) {
        verbose("WARNING: "+msg);
    }
    
    public void verbose(String msg) {
        if (verbose) {
            System.out.println(msg);
        }
    }

    public void error(String msg) {
        errors++;
        verbose("ERROR: "+msg);
    }

    public void exception(File file, Exception e, String what) {
        errors++;
        System.out.println("Exception while "+what+" "+file.getAbsolutePath());
        e.printStackTrace();
    }
}