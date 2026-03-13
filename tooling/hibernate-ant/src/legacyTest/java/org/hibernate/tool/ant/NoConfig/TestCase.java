/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.NoConfig;

import org.apache.tools.ant.BuildException;
import org.hibernate.tool.ant.test.utils.AntUtil;
import org.hibernate.tool.ant.test.utils.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
	}

	@Test
	public void testNoConfig() {

		String[] resources = new String[] {"build.xml"};
		ResourceUtil.createResources(this, resources, resourcesDir);
		File buildFile = new File(resourcesDir, "build.xml");

		AntUtil.Project project = AntUtil.createProject(buildFile);
		project.setProperty("destinationDir", destinationDir.getAbsolutePath());
		project.setProperty("resourcesDir", resourcesDir.getAbsolutePath());

		File generatedSchema = new File(destinationDir, "generated-schema.sql");
		assertFalse(generatedSchema.exists());

		try {

			project.executeTarget("testNoConfig");
			fail("Exception should happen");

		} catch (BuildException e) {

			assertTrue(e.getMessage().contains("No configuration specified"));

		}

		assertFalse(generatedSchema.exists());
	}

}
