/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package org.hibernate.shards.util;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTask;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Extension to the Junit ant task that, instead of actually executing tests,
 * generates code that adds all the tests that would have been executed to
 * a list of classes.  Used in conjunction with
 * org.hibernate.shards.util.NonPermutedTests.java.template
 *
 * @author maxr@google.com (Max Ross)
 */
public class NonPermutedTestsScraper extends JUnitTask {

  private String propertyToSet;

  public NonPermutedTestsScraper() throws Exception {
  }

  public void setPropertyToSet(String propertyToSet) {
    this.propertyToSet = propertyToSet;
  }

  public void execute() throws BuildException {
    List<String> testClasses = scrapeNonPermutedTestClasses();
    String testClassCode = testClassesToCode(testClasses);
    getProject().setProperty(propertyToSet, testClassCode);
  }

  private String testClassesToCode(List<String> testClasses) {
    StringBuilder sb = new StringBuilder();
    for(String testClass : testClasses) {
      sb.append("    classes.add(").append(testClass).append(".class);\n");
    }
    return sb.toString();
  }

  private List<String> scrapeNonPermutedTestClasses() {
    List<String> testClasses = new ArrayList<String>();

    for(Enumeration iTests = getIndividualTests(); iTests.hasMoreElements(); ) {
      Object obj = iTests.nextElement();
      if(obj instanceof JUnitTest) {
        JUnitTest test = (JUnitTest) obj;
        if(test.getName().endsWith("Test") &&
            !test.getName().endsWith("PermutedIntegrationTest")) {
          testClasses.add(test.getName());
        }
      }
    }
    return testClasses;
  }
}
