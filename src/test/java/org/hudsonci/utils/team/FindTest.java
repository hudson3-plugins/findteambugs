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

import org.hudsonci.utils.team.Find.FileFunction;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static junit.framework.Assert.assertTrue;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * Unit test for simple App.
 */
public class FindTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public FindTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( FindTest.class );
    }
    
    int numberOfTeamsXmls = 0;
    
    public void testFind() throws IOException
    {
        assertEquals(Find.doIt(new String[] {"-qf", "target/test/"}), 0);
        
        // Verify that the file system corresponds to teams.xml
        FileFunction fun = new FileFunction() {
            public void call(File f) {
                verifyFileSystem(f);
            }
        };
        Find.scan(new File("target/test"), 3, fun);
        assertEquals(numberOfTeamsXmls, 1);
    }
    
    private void verifyFileSystem(File teamsXmlFile) {
        assertTrue("teams.xml".equals(teamsXmlFile.getName()));
        numberOfTeamsXmls++;
        File teamsDir = teamsXmlFile.getParentFile();
        File homeDir = teamsDir.getParentFile();
        try {
            SAXReader reader = new SAXReader();
            Document inDoc = reader.read(teamsXmlFile);
            Element root = inDoc.getRootElement();
            assertTrue("teamManager".equals(root.getName()));
            List<Element> elements = (List<Element>) root.elements();
            for (Element element : elements) {
                if ("team".equals(element.getName())) {
                    List<Element> children = (List<Element>) element.elements();
                    File teamDir = null;
                    for (Element child : children) {
                        String childName = child.getName();
                        if ("name".equals(childName)) {
                            // team must exist
                            if ("public".equals(child.getText())) {
                                teamDir = homeDir;
                            } else {
                                teamDir = new File(teamsDir, child.getText());
                            }
                            assertTrue("Not exists "+teamDir.getAbsolutePath(), teamDir.exists());
                            assertTrue("Not a dir  "+teamDir.getAbsolutePath(), teamDir.isDirectory());
                        } else if ("job".equals(childName)) {
                            // we expect team name to be written before jobs
                            assertNotNull(teamDir);
                            Element idElement = child.element("id");
                            assertNotNull(idElement);
                            String id = idElement.getText();
                            File jobDir = new File(new File(teamDir, "jobs"), id);
                            assertTrue("Not exists "+jobDir.getAbsolutePath(), jobDir.exists());
                            assertTrue("Not exists "+jobDir.getAbsolutePath(), jobDir.isDirectory());
                        }
                    }
                }
            }
        } catch (DocumentException ex) {
            Logger.getLogger(FindTest.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
