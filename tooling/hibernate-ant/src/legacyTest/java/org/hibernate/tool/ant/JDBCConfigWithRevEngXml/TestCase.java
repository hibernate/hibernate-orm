/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.JDBCConfigWithRevEngXml;

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
	public void testJDBCConfigWithRevEngXml() {

		String[] resources = new String[] {"build.xml", "hibernate.reveng.xml"};
		ResourceUtil.createResources(this, resources, resourcesDir);
		File buildFile = new File(resourcesDir, "build.xml");
		ResourceUtil.createResources(this, new String[] { "/hibernate.properties" }, resourcesDir);

		AntUtil.Project project = AntUtil.createProject(buildFile);
		project.setProperty("destinationDir", destinationDir.getAbsolutePath());
		project.setProperty("resourcesDir", resourcesDir.getAbsolutePath());

		File bottomUp = new File(destinationDir, "foo/BottomUp.java");
		assertFalse(bottomUp.exists());

		project.executeTarget("testJDBCConfigWithRevEngXml");

		String log = AntUtil.getLog(project);
		assertFalse(log.contains("Exception"), log);

		assertTrue(bottomUp.exists());
		assertTrue( FileUtil
				.findFirstString("good", bottomUp)
				.contains("Boolean"));
		assertTrue(FileUtil
				.findFirstString("something", bottomUp)
				.contains("SomeUserType"));
	}

}
