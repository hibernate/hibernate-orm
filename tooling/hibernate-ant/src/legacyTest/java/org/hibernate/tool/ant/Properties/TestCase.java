/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.Properties;

import org.hibernate.tool.ant.test.utils.AntUtil;
import org.hibernate.tool.ant.test.utils.FileUtil;
import org.hibernate.tool.ant.test.utils.JdbcUtil;
import org.hibernate.tool.ant.test.utils.ResourceUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

		assertTrue( FileUtil
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
