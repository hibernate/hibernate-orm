/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.JDBCConfiguration;

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
	public void testJDBCConfiguration() {

		String[] resources = new String[] {"JDBCConfiguration.xml"};
		ResourceUtil.createResources(this, resources, resourcesDir);
		File buildFile = new File(resourcesDir, "JDBCConfiguration.xml");
		ResourceUtil.createResources(this, new String[] { "/hibernate.properties" }, resourcesDir);
		File templatesDir = new File(resourcesDir, "templates");
		assertTrue(templatesDir.mkdir());
		File pojoTemplateDir = new File(templatesDir, "pojo");
		assertTrue(pojoTemplateDir.mkdir());
		ResourceUtil.createResources(this, new String[] { "Pojo.ftl" }, pojoTemplateDir);

		AntUtil.Project project = AntUtil.createProject(buildFile);
		project.setProperty("destinationDir", destinationDir.getAbsolutePath());
		project.setProperty("resourcesDir", resourcesDir.getAbsolutePath());

		File noTemplate = new File(destinationDir, "no-template/BottomUp.java");
		File withTemplate = new File(destinationDir, "with-template/BottomUp.java");
		File cfgxml = new File(destinationDir, "cfgxml/hibernate.cfg.xml");

		assertFalse(noTemplate.exists());
		assertFalse(withTemplate.exists());
		assertFalse(cfgxml.exists());

		project.executeTarget("testJDBCConfiguration");

		String log = AntUtil.getLog(project);
		assertFalse(log.contains("Exception"), log);

		assertTrue(noTemplate.exists());
		assertTrue( FileUtil
				.findFirstString("public", noTemplate)
				.contains("BottomUp"));

		assertTrue(withTemplate.exists());
		assertTrue(FileUtil
				.findFirstString("template", withTemplate)
				.contains("/** Made by a template in your neighborhood */"));

		assertTrue(cfgxml.exists());
		assertTrue(FileUtil
				.findFirstString("mapping", cfgxml)
				.contains("BottomUp.hbm.xml"));

	}

}
