/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.misc;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransformHbmXmlTaskTest {

	private Project project;
	private TransformHbmXmlTask task;
	private Method determineCopyNameMethod;

	@BeforeEach
	void setUp() throws Exception {
		project = ProjectBuilder.builder().build();
		task = project.getTasks().create("transformHbm", TransformHbmXmlTask.class);
		determineCopyNameMethod = TransformHbmXmlTask.class.getDeclaredMethod("determineCopyName", File.class);
		determineCopyNameMethod.setAccessible(true);
	}

	@Test
	public void testDetermineCopyNameNoRenaming() throws Exception {
		// When no renaming is defined, returns the original file name
		File hbmFile = new File("/tmp/Person.hbm.xml");
		String result = (String) determineCopyNameMethod.invoke(task, hbmFile);
		assertEquals("Person.hbm.xml", result);
	}

	@Test
	public void testDetermineCopyNameWithPrefix() throws Exception {
		task.getRenaming().getPrefix().set("transformed-");
		File hbmFile = new File("/tmp/Person.hbm.xml");
		String result = (String) determineCopyNameMethod.invoke(task, hbmFile);
		assertEquals("transformed-Person.hbm.xml", result);
	}

	@Test
	public void testDetermineCopyNameWithSuffix() throws Exception {
		task.getRenaming().getSuffix().set("-new");
		File hbmFile = new File("/tmp/Person.hbm.xml");
		String result = (String) determineCopyNameMethod.invoke(task, hbmFile);
		assertEquals("Person-new.hbm.xml", result);
	}

	@Test
	public void testDetermineCopyNameWithExtension() throws Exception {
		task.getRenaming().getExtension().set("orm.xml");
		File hbmFile = new File("/tmp/Person.hbm.xml");
		String result = (String) determineCopyNameMethod.invoke(task, hbmFile);
		assertEquals("Person.orm.xml", result);
	}

	@Test
	public void testDetermineCopyNameWithPrefixAndSuffix() throws Exception {
		task.getRenaming().getPrefix().set("pre-");
		task.getRenaming().getSuffix().set("-post");
		File hbmFile = new File("/tmp/Person.hbm.xml");
		String result = (String) determineCopyNameMethod.invoke(task, hbmFile);
		assertEquals("pre-Person-post.hbm.xml", result);
	}

	@Test
	public void testDetermineCopyNameWithAll() throws Exception {
		task.getRenaming().getPrefix().set("pre-");
		task.getRenaming().getSuffix().set("-post");
		task.getRenaming().getExtension().set("xml");
		File hbmFile = new File("/tmp/Person.hbm.xml");
		String result = (String) determineCopyNameMethod.invoke(task, hbmFile);
		assertEquals("pre-Person-post.xml", result);
	}

	@Test
	public void testDefaultConventions() {
		assertEquals("H2", task.getTargetDatabaseName().get());
		assertNotNull(task.getUnsupportedFeatures().get());
		assertEquals(false, task.getDeleteHbmFiles().get());
	}

	@Test
	public void testAreNoneDefinedDefault() {
		assertTrue(task.getRenaming().areNoneDefined());
	}

	@Test
	public void testDetermineCopyFileNoOutputDir() throws Exception {
		Method determineCopyFileMethod = TransformHbmXmlTask.class.getDeclaredMethod(
				"determineCopyFile", String.class, File.class);
		determineCopyFileMethod.setAccessible(true);

		File hbmFile = new File("/tmp/mappings/Person.hbm.xml");
		File result = (File) determineCopyFileMethod.invoke(task, "Person.orm.xml", hbmFile);
		assertEquals(new File("/tmp/mappings", "Person.orm.xml"), result);
	}
}
