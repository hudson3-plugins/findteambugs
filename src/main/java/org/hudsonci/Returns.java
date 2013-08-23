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

/**
 * Return from OS commands and tools.
 * 
 * @author Bob Foster
 */
public class Returns {
    private int result;
    private String[] output; // Stdout and Stderr merged
    
    public Returns(int result, String[] output) {
      this.result = result;
      this.output = output;
    }

    public int getResult() {
      return result;
    }

    public String[] getOutput() {
      return output;
    }
}
