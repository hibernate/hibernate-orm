/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.apache.tools.ant.types.FileSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JavaFormatterTaskTest {

	@TempDir
	private File tempDir;

	@Test
	public void testAddConfiguredFileSet() {
		JavaFormatterTask task = new JavaFormatterTask();
		assertEquals(0, task.fileSets.size());
		task.addConfiguredFileSet(new FileSet());
		assertEquals(1, task.fileSets.size());
		task.addConfiguredFileSet(new FileSet());
		assertEquals(2, task.fileSets.size());
	}

	@Test
	public void testClone() throws CloneNotSupportedException {
		JavaFormatterTask task = new JavaFormatterTask();
		task.failOnError = true;
		task.addConfiguredFileSet(new FileSet());
		JavaFormatterTask clone = (JavaFormatterTask) task.clone();
		assertNotNull(clone);
		assertEquals(true, clone.failOnError);
		// Clone should include filesets from original
		assertFalse(clone.fileSets.isEmpty());
	}

	@Test
	public void testReadConfig() throws Exception {
		JavaFormatterTask task = new JavaFormatterTask();
		File cfgFile = new File(tempDir, "formatter.properties");
		Properties props = new Properties();
		props.setProperty("tabWidth", "4");
		props.setProperty("indentStyle", "tab");
		try (FileOutputStream fos = new FileOutputStream(cfgFile)) {
			props.store(fos, null);
		}
		// readConfig is private, use reflection
		Method readConfig = JavaFormatterTask.class.getDeclaredMethod("readConfig", File.class);
		readConfig.setAccessible(true);
		Properties result = (Properties) readConfig.invoke(task, cfgFile);
		assertNotNull(result);
		assertEquals("4", result.getProperty("tabWidth"));
		assertEquals("tab", result.getProperty("indentStyle"));
	}

	@Test
	public void testGetFilesEmpty() throws Exception {
		JavaFormatterTask task = new JavaFormatterTask();
		// getFiles is private, use reflection
		Method getFiles = JavaFormatterTask.class.getDeclaredMethod("getFiles");
		getFiles.setAccessible(true);
		File[] files = (File[]) getFiles.invoke(task);
		assertNotNull(files);
		assertEquals(0, files.length);
	}
}
