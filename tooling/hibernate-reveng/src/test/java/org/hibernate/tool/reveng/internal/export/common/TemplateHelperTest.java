/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemplateHelperTest {

	@TempDir
	private File tempDir;

	private TemplateHelper helper;

	@BeforeEach
	public void setUp() {
		helper = new TemplateHelper();
		helper.init(tempDir, new String[]{tempDir.getAbsolutePath()});
	}

	@Test
	public void testGetOutputDirectory() {
		assertEquals(tempDir, helper.getOutputDirectory());
	}

	@Test
	public void testPutAndRemoveFromContext() {
		helper.putInContext("testKey", "testValue");
		// Should not throw on remove
		helper.removeFromContext("testKey");
	}

	@Test
	public void testPutInContextNullValueThrows() {
		assertThrows(IllegalStateException.class, () -> helper.putInContext("key", null));
	}

	@Test
	public void testRemoveFromContextNonExistentThrows() {
		assertThrows(IllegalStateException.class, () -> helper.removeFromContext("nonExistent"));
	}

	@Test
	public void testSetupContext() {
		helper.setupContext();
		assertNotNull(helper.getContext());
	}

	@Test
	public void testProcessString() {
		helper.setupContext();
		helper.putInContext("name", "World");
		StringWriter output = new StringWriter();
		helper.processString("Hello ${name}!", output);
		assertEquals("Hello World!", output.toString());
	}

	@Test
	public void testEnsureExistenceCreatesDir() {
		File dest = new File(tempDir, "sub/dir/file.txt");
		helper.ensureExistence(dest);
		assertTrue(dest.getParentFile().exists());
		assertTrue(dest.getParentFile().isDirectory());
	}

	@Test
	public void testEnsureExistenceExistingDir() {
		File dest = new File(tempDir, "existing.txt");
		// tempDir already exists as directory - should not throw
		helper.ensureExistence(dest);
	}

	@Test
	public void testInitWithNonExistentPath() {
		TemplateHelper h = new TemplateHelper();
		// Non-existent path should just log a warning, not throw
		h.init(tempDir, new String[]{"/nonexistent/path"});
		assertNotNull(h.getOutputDirectory());
	}

	@Test
	public void testInitWithFilePath() throws IOException {
		TemplateHelper h = new TemplateHelper();
		File regularFile = new File(tempDir, "notadir.txt");
		try (FileOutputStream fos = new FileOutputStream(regularFile)) {
			fos.write("test".getBytes());
		}
		// Regular file (not dir, not jar/zip) should just log a warning
		h.init(tempDir, new String[]{regularFile.getAbsolutePath()});
		assertNotNull(h.getOutputDirectory());
	}

	@Test
	public void testTemplateExists() throws IOException {
		// Create a template file
		File templateFile = new File(tempDir, "test.ftl");
		try (FileOutputStream fos = new FileOutputStream(templateFile)) {
			fos.write("Hello ${name}".getBytes());
		}
		// Re-init to pick up the template
		helper.init(tempDir, new String[]{tempDir.getAbsolutePath()});
		assertTrue(helper.templateExists("test.ftl"));
		assertFalse(helper.templateExists("nonexistent.ftl"));
	}

	@Test
	public void testProcessTemplate() throws IOException {
		File templateFile = new File(tempDir, "greeting.ftl");
		try (FileOutputStream fos = new FileOutputStream(templateFile)) {
			fos.write("Hi ${who}!".getBytes());
		}
		helper.init(tempDir, new String[]{tempDir.getAbsolutePath()});
		helper.setupContext();
		helper.putInContext("who", "Alice");
		StringWriter output = new StringWriter();
		helper.processTemplate("greeting.ftl", output, "test");
		assertEquals("Hi Alice!", output.toString());
	}

	@Test
	public void testTemplatesCreateFile() throws IOException {
		helper.setupContext();
		TemplateHelper.Templates templates = helper.new Templates();
		templates.createFile("file content here", "output.txt");
		File created = new File(tempDir, "output.txt");
		assertTrue(created.exists());
	}
}
