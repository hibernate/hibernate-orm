/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.JavaFormatter;

import org.hibernate.tool.ant.test.utils.AntUtil;
import org.hibernate.tool.ant.test.utils.FileUtil;
import org.hibernate.tool.ant.test.utils.ResourceUtil;
import org.hibernate.tool.reveng.api.java.DefaultJavaPrettyPrinterStrategy;
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
	}

	@Test
	public void testJavaFormatFile() {

		String[] resources = new String[] {"build.xml", "formatting/SimpleOne.java"};
		ResourceUtil.createResources(this, resources, resourcesDir);
		File buildFile = new File(resourcesDir, "build.xml");

		AntUtil.Project project = AntUtil.createProject(buildFile);
		project.setProperty("destinationDir", destinationDir.getAbsolutePath());
		project.setProperty("resourcesDir", resourcesDir.getAbsolutePath());

		File simpleOne = new File(destinationDir, "formatting/SimpleOne.java");
		assertFalse(simpleOne.exists());

		project.executeTarget("copyfiles");

		String log = AntUtil.getLog(project);
		assertFalse(log.contains("Exception"));

		assertTrue(simpleOne.exists());
		assertFalse( FileUtil
				.findFirstString("public", simpleOne)
				.contains("SimpleOne"));

		DefaultJavaPrettyPrinterStrategy formatter = new DefaultJavaPrettyPrinterStrategy();
		formatter.formatFile( simpleOne );

		assertTrue(FileUtil
				.findFirstString("public", simpleOne)
				.contains("SimpleOne"));

	}

	@Test
	public void testJavaJdk5FormatFile() {

		String[] resources = new String[] {"build.xml", "formatting/Simple5One.java"};
		ResourceUtil.createResources(this, resources, resourcesDir);
		File buildFile = new File(resourcesDir, "build.xml");

		AntUtil.Project project = AntUtil.createProject(buildFile);
		project.setProperty("destinationDir", destinationDir.getAbsolutePath());
		project.setProperty("resourcesDir", resourcesDir.getAbsolutePath());

		File simple5One = new File(destinationDir, "formatting/Simple5One.java");
		assertFalse(simple5One.exists());

		project.executeTarget("copyfiles");

		String log = AntUtil.getLog(project);
		assertFalse(log.contains("Exception"));

		assertTrue(simple5One.exists());
		assertFalse(FileUtil
				.findFirstString("public", simple5One)
				.contains("Simple5One"));

		project.executeTarget("copyfiles");

		assertFalse(log.contains("Exception"));

		DefaultJavaPrettyPrinterStrategy formatter = new DefaultJavaPrettyPrinterStrategy();
		assertTrue(
				formatter.formatFile(simple5One),
				"formatting should pass when using default settings");

		assertTrue(FileUtil
				.findFirstString("public", simple5One)
				.contains("Simple5One"));
	}

	@Test
	public void testAntFormatTask() {

		String[] resources = new String[] {"build.xml", "formatting/SimpleOne.java"};
		ResourceUtil.createResources(this, resources, resourcesDir);
		File buildFile = new File(resourcesDir, "build.xml");

		AntUtil.Project project = AntUtil.createProject(buildFile);
		project.setProperty("destinationDir", destinationDir.getAbsolutePath());
		project.setProperty("resourcesDir", resourcesDir.getAbsolutePath());

		File simpleOne = new File(destinationDir, "formatting/SimpleOne.java");
		assertFalse(simpleOne.exists());

		project.executeTarget("copyfiles");

		String log = AntUtil.getLog(project);
		assertFalse(log.contains("Exception"));

		assertTrue(simpleOne.exists());
		assertFalse(FileUtil
				.findFirstString("public", simpleOne)
				.contains("SimpleOne"));

		project.executeTarget("formatfiles");

		assertTrue(FileUtil
				.findFirstString("public", simpleOne)
				.contains("SimpleOne"));

	}

	@Test
	public void testConfig() {

		String[] resources = new String[] {"build.xml", "formatting/SimpleOne.java", "formatting/Simple5One.java", "emptyconfig.properties"};
		ResourceUtil.createResources(this, resources, resourcesDir);
		File buildFile = new File(resourcesDir, "build.xml");

		AntUtil.Project project = AntUtil.createProject(buildFile);
		project.setProperty("destinationDir", destinationDir.getAbsolutePath());
		project.setProperty("resourcesDir", resourcesDir.getAbsolutePath());

		File simpleOne = new File(destinationDir, "formatting/SimpleOne.java");
		assertFalse(simpleOne.exists());
		File simple5One = new File(destinationDir, "formatting/Simple5One.java");
		assertFalse(simple5One.exists());

		project.executeTarget("copyfiles");

		String log = AntUtil.getLog(project);
		assertFalse(log.contains("Exception"));

		assertTrue(simpleOne.exists());
		assertTrue(simple5One.exists());

		assertFalse(FileUtil
				.findFirstString("public", simpleOne)
				.contains("SimpleOne"));
		assertFalse(FileUtil
				.findFirstString("public", simple5One)
				.contains("Simple5One"));

		project.executeTarget("formatfiles");
		log = AntUtil.getLog(project);
		assertFalse(log.contains("Exception"));

		assertTrue(FileUtil
				.findFirstString("public", simpleOne)
				.contains("SimpleOne"));
		assertTrue(FileUtil
				.findFirstString("public", simple5One)
				.contains("Simple5One"));

		assertTrue(simpleOne.delete());
		assertTrue(simple5One.delete());

	}

}
