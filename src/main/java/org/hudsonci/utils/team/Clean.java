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
 * Cleans a hudson home directory so the only thing left in it are job folders,
 * their parents and config.xml files one level beneath. Used for test data.
 * @author Bob Foster
 */
public class Clean {

    private static class CleanerException extends Exception {
        public CleanerException(String msg) {
            super(msg);
        }
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -cp team2team.jar com.oracle.dcs.Cleaner HUDSON_HOME_PATH");
            System.exit(1);
        }
        File home = new File(args[0]);
        try {
            new Clean().clean(home);
        } catch (CleanerException ex) {
            System.err.println(ex.getMessage());
            System.exit(2);
        }
    }
    
    enum State {
        ABOVE,
        JOBS,
        JOB,
        BELOW
    }

    private void clean(File home) throws CleanerException {
        clean(home, State.ABOVE);
    }
    
    /**
     * @param dir
     * @param state
     * @return true if delete directory
     */
    private boolean clean(File dir, State state) throws CleanerException {
        File[] files = dir.listFiles();
        switch (state) {
            case ABOVE:
                // Just recurse - deleting any files and any dirs for which clean returns true
                // If any directory clean returns false, return false; otherwise true
                boolean delete = true;
                for (File file : files) {
                    if (file.isDirectory()) {
                        if ("jobs".equals(file.getName())) {
                            clean(file, State.JOBS);
                            delete = false;
                        } else {
                            // Must propagage return upward
                            if (clean(file, state)) {
                                delete(file);
                            } else {
                                delete = false;
                            }
                            
                        }
                    } else if (!"teams.xml".equals(file.getName())) {
                        delete(file);
                    }
                }
                return delete;
            case JOBS:
                // In a jobs folder, recurse into job folders
                for (File file : files) {
                    if (file.isDirectory()) {
                        clean(file, State.JOB);
                    } else {
                        delete(file);
                    }
                }
                // never delete this dir
                return false;
            case JOB:
                // In a job folder, keep the config.xml and delete everything else
                for (File file : files) {
                    if (file.isFile() && !"config.xml".equals(file.getName())) {
                        delete(file);
                    } else if (file.isDirectory()) {
                        if (clean(file, State.BELOW)) {
                            delete(file);
                        }
                    }
                }
                // never delete this dir
                return false;
            case BELOW:
                // delete everything and return true
                for (File file : files) {
                    if (file.isDirectory()) {
                        clean(file, State.BELOW);
                    }
                    delete(file);
                }
                return true;
            default:
                return false;
        }
    }
    
    private void delete(File file) throws CleanerException {
        if (!file.delete()) {
            throw new CleanerException("Can't delete "+file.getAbsolutePath()+" - aborting clean");
        }
    }
    
}
