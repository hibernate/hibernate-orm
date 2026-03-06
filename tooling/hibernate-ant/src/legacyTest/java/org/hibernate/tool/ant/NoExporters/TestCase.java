/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.NoExporters;

import org.apache.tools.ant.BuildException;
import org.hibernate.tool.ant.test.utils.AntUtil;
import org.hibernate.tool.ant.test.utils.ResourceUtil;
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
