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

package org.hudsonci;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Bob Foster
 */
public class Cmd {

  private static final int INIT = 0;
  private static final int INARG = 1;
  private static final int INSTRING = 2;
  private static final int INESCAPE = 3;
  
  static String[] split(String cmd) {
    int state = INARG;
    StringBuilder sb = new StringBuilder();
    List<String> list = new ArrayList<String>();
    char endString = 0;
    
    for (int i = 0; i < cmd.length(); i++) {
      char c = cmd.charAt(i);
      switch (state) {
        case INIT:
          if (c != ' ') {
            if (c == '"' || c == '\'') {
              state = INSTRING;
              endString = c;
            } else {
              state = INARG;
              sb.append(c);
            }
          }
          break;
        case INARG:
          if (c == ' ') {
            state = INIT;
            if (sb.length() > 0) {
              list.add(sb.toString());
              sb.setLength(0);
            }
          } else
            sb.append(c);
          break;
        case INSTRING:
          if (c == endString) {
            list.add(sb.toString());
            sb.setLength(0);
            state = INIT;
          } else if (c == '\\')
            state = INESCAPE;
          else
            sb.append(c);
          break;
        case INESCAPE:
          if (c != '"' && c != '\'')
            throw new IllegalArgumentException("Only escaped quotes allowed");
          sb.append(c);
          state = INSTRING;
        break;
      }
    }
    if (sb.length() > 0) {
      if (state == INSTRING)
        throw new IllegalArgumentException("Unterminated string");
      list.add(sb.toString());
    }
    return list.toArray(new String[list.size()]);
  }
  
  /**
   * Simple method to exec a command line and return the result.
   * Note that no shell is invoked, so shell operations like
   * filename expansion are not available.
   * 
   * @param command to execute
   * @return return code from process
   * @throws IOException 
   */
  public static int exec(String command) throws IOException {
    String[] cmd = split(command);
    Process p = Runtime.getRuntime().exec(cmd);
    try {
      return p.waitFor();
    } catch (InterruptedException ex) {
      return p.exitValue();
    }
  }
  
  public static Returns run(String command) throws IOException {
    return run(null, command);
  }
  
  public static Returns run(File dir, String command) throws IOException {
    String[] cmd = split(command);
    return run(dir, cmd);
  }
  
  public static Returns run(File dir, String[] cmd) throws IOException {
    ProcessBuilder pb = new ProcessBuilder(cmd);
    return run(dir, pb, null, null);
  }
  
  public static Returns run(File dir, String[] cmd, String until, Map env) throws IOException {
    ProcessBuilder pb = new ProcessBuilder(cmd);
    return run(dir, pb, until, env);
  }
  
  public static Returns run(File dir, ProcessBuilder pb, String until, Map env) throws IOException {
    pb.redirectErrorStream(true);
    pb.directory(dir);
    
    if (env != null) {
      Map pbenv = pb.environment();
      pbenv.putAll(env);
    }
    
    Process p = pb.start();
    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    List<String> list = new ArrayList<String>();
    boolean foundUntil = false;
    try {
      while (true) {
        String line = reader.readLine();
        if (line == null)
          break;
        if (until != null && line.startsWith(until)) {
          foundUntil = true;
          break;
        }
        list.add(line);
      }
    } finally {
      reader.close();
    }
    try {
      String[] output = list.toArray(new String[list.size()]);
      if (foundUntil) {
        p.destroy();
        return new Returns(0, output);
      }
      return new Returns(p.waitFor(), output);
    } catch (InterruptedException ex) {
      Thread.interrupted();
      return new Returns(-99999, new String[0]);
    }
  }

  public static Returns pipe(File dir, String... strings) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < strings.length; i++) {
      if (i > 0)
        sb.append(" | ");
      sb.append(strings[i]);
    }
    String cmds = sb.toString();
    ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", cmds);
    return run(dir, pb, null, null);
  }
}
