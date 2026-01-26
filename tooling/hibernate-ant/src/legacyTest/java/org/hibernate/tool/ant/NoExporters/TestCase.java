/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hibernate.tool.ant.NoExporters;

import org.apache.tools.ant.BuildException;
import org.hibernate.tools.test.util.AntUtil;
import org.hibernate.tools.test.util.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TestCase {
	
	@TempDir
	public File outputFolder = new File("output");
	
	private File resourcesDir = null;
	
	@BeforeEach
	public void setUp() {
		resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
	}
	
	@Test
	public void testNoConnInfoExport() {

		String[] resources = new String[] {"build.xml", "hibernate.properties"};
		ResourceUtil.createResources(this, resources, resourcesDir);
		File buildFile = new File(resourcesDir, "build.xml");	
		
		AntUtil.Project project = AntUtil.createProject(buildFile);
		project.setProperty("resourcesDir", resourcesDir.getAbsolutePath());
		
		try {
		
			project.executeTarget("testNoExporters");
			fail("should have failed with no exporters!");
		
		} catch (BuildException e) {
			
			// should happen!
			assertTrue(e.getMessage().contains("No exporters specified"));
			
		}

	}
	
}
