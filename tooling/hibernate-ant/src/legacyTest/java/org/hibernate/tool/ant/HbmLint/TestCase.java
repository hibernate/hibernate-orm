/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.HbmLint;

import org.hibernate.tool.ant.test.utils.AntUtil;
import org.hibernate.tool.ant.test.utils.FileUtil;
import org.hibernate.tool.ant.test.utils.JdbcUtil;
import org.hibernate.tool.ant.test.utils.ResourceUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
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
	public void tearDown() throws Exception {
		JdbcUtil.dropDatabase(this);
	}

	// TODO HBX-3313: Verify why this does not work on Windows
	@Test
	@DisabledOnOs(OS.WINDOWS)
	public void testHbmLint() {

		String[] resources = new String[] {"build.xml", "SchemaIssues.hbm.xml", "hibernate.cfg.xml"};
		ResourceUtil.createResources(this, resources, resourcesDir);
		File buildFile = new File(resourcesDir, "build.xml");
		ResourceUtil.createResources(this, new String[] { "/hibernate.properties" }, resourcesDir);

		AntUtil.Project project = AntUtil.createProject(buildFile);
		project.setProperty("destinationDir", destinationDir.getAbsolutePath());
		project.setProperty("resourcesDir", resourcesDir.getAbsolutePath());

		File hbmLintResult = new File(destinationDir, "hbmlint-result.txt");
		assertFalse(hbmLintResult.exists());

		project.executeTarget("testHbmLint");

		assertTrue(hbmLintResult.exists());

		assertTrue(FileUtil
				.findFirstString("BadType", hbmLintResult)
				.contains("SCHEMA_TABLE_MISSING"));

		assertTrue(FileUtil
				.findFirstString("Category", hbmLintResult)
				.contains("SCHEMA_TABLE_MISSING"));

		assertTrue(FileUtil
				.findFirstString("Column", hbmLintResult)
				.contains("SCHEMA_TABLE_MISSING"));

		assertTrue(FileUtil
				.findFirstString("does_not_exist", hbmLintResult)
				.contains("SCHEMA_TABLE_MISSING"));

		assertTrue(FileUtil
				.findFirstString("hilo_table", hbmLintResult)
				.contains("SCHEMA_TABLE_MISSING"));

		assertTrue(FileUtil
				.findFirstString("MissingTable", hbmLintResult)
				.contains("SCHEMA_TABLE_MISSING"));

		assertTrue(FileUtil
				.findFirstString("MISSING_ID_GENERATOR", hbmLintResult)
				.contains("does_not_exist"));

	}


}
