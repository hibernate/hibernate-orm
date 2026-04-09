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
package org.hibernate.tool.ant.Properties;

import org.hibernate.tools.test.util.AntUtil;
import org.hibernate.tools.test.util.FileUtil;
import org.hibernate.tools.test.util.JdbcUtil;
import org.hibernate.tools.test.util.ResourceUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class TestCase {
	
	@TempDir
	public File outputFolder = new File("output");
	
	private File destinationDir = null;
	private File resourcesDir = null;
	
	@BeforeEach
	public void setUp() {
		destinationDir = new File(outputFolder, "destination");
		assertTrue(destinationDir.mkdir());
		resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		JdbcUtil.createDatabase(this);
	}
	
	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}
	
	@Test
	public void testProperties() {

		String[] resources = new String[] {"build.xml", "SomeClass.hbm.xml"};
		ResourceUtil.createResources(this, resources, resourcesDir);
		File buildFile = new File(resourcesDir, "build.xml");	
		ResourceUtil.createResources(this, new String[] { "/hibernate.properties" }, resourcesDir);
		File templatesDir = new File(resourcesDir, "templates");
		assertTrue(templatesDir.mkdir());
		File pojoTemplateDir = new File(templatesDir, "pojo");
		assertTrue(pojoTemplateDir.mkdir());
		ResourceUtil.createResources(this, new String[] { "Pojo.ftl" }, pojoTemplateDir);
		
		AntUtil.Project project = AntUtil.createProject(buildFile);
		project.setProperty("destinationDir", destinationDir.getAbsolutePath());
		project.setProperty("resourcesDir", resourcesDir.getAbsolutePath());
		
		File file = new File(destinationDir, "SomeClass.java");
		assertFalse(file.exists());
		
		project.executeTarget("testProperties");

		String log = AntUtil.getLog(project);
        assertFalse(log.contains("Exception"), log);
		
		assertTrue(file.exists());
		
		assertTrue(FileUtil
				.findFirstString("hbm2java.weirdAl", file)
				.contains("foo3"));
		assertTrue(FileUtil
				.findFirstString("ant.project.name", file)
				.contains("PropertiesTest"));
		assertTrue(FileUtil
				.findFirstString("foo.weirdAl", file)
				.contains("does not exist"));
		assertTrue(FileUtil
				.findFirstString("bar", file)
				.contains("foo2"));
		assertTrue(FileUtil
				.findFirstString("file", file)
				.contains("some.file"));
		assertTrue(FileUtil
				.findFirstString("value", file)
				.contains("some value"));

	}
	
}
