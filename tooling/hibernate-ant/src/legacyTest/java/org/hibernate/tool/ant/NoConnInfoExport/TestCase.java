/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.NoConnInfoExport;

import org.hibernate.tool.ant.test.utils.AntUtil;
import org.hibernate.tool.ant.test.utils.FileUtil;
import org.hibernate.tool.ant.test.utils.ResourceUtil;
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
		destinationDir.mkdir();
		resourcesDir = new File(outputFolder, "resources");
		resourcesDir.mkdir();
	}

	@Test
	public void testNoConnInfoExport() {

		String[] resources = new String[] {"build.xml", "hibernate.cfg.xml", "TopDown.hbm.xml", "hibernate.properties"};
		ResourceUtil.createResources(this, resources, resourcesDir);
		File buildFile = new File(resourcesDir, "build.xml");

		AntUtil.Project project = AntUtil.createProject(buildFile);
		project.setProperty("destinationDir", destinationDir.getAbsolutePath());
		project.setProperty("resourcesDir", resourcesDir.getAbsolutePath());

		File noConnInfo = new File(destinationDir, "noConnInfo.sql");
		assertFalse(noConnInfo.exists());

		project.executeTarget("testNoConnInfoExport");

		assertTrue(noConnInfo.exists());
		assertTrue( FileUtil
				.findFirstString("create", noConnInfo)
				.contains("create table TopDown (id bigint not null, name varchar(255), primary key (id));"));
	}

}
