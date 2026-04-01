/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TemplateHelperTest {

	@TempDir
	File tempDir;

	private TemplateHelper helper;

	@BeforeEach
	public void setUp() {
		helper = new TemplateHelper();
		helper.init(tempDir, new String[]{tempDir.getAbsolutePath()});
	}

	@Test
	public void testInitWithDirectoryPath() {
		TemplateHelper h = new TemplateHelper();
		assertDoesNotThrow(() -> h.init(tempDir, new String[]{tempDir.getAbsolutePath()}));
	}

	@Test
	public void testInitWithNonExistentPath() {
		TemplateHelper h = new TemplateHelper();
		assertDoesNotThrow(() -> h.init(tempDir, new String[]{"/nonexistent/path"}));
	}

	@Test
	public void testPutInContext() {
		assertDoesNotThrow(() -> helper.putInContext("key1", "value1"));
	}

	@Test
	public void testPutInContextNullValue() {
		assertThrows(IllegalStateException.class, () -> helper.putInContext("key1", null));
	}

	@Test
	public void testRemoveFromContext() {
		helper.putInContext("key1", "value1");
		assertDoesNotThrow(() -> helper.removeFromContext("key1"));
	}

	@Test
	public void testRemoveFromContextNonExistent() {
		assertThrows(IllegalStateException.class, () -> helper.removeFromContext("nonexistent"));
	}

	@Test
	public void testEnsureExistenceCreatesDirectory() {
		File newFile = new File(new File(tempDir, "newdir"), "test.txt");
		assertDoesNotThrow(() -> helper.ensureExistence(newFile));
	}

	@Test
	public void testEnsureExistenceExistingDirectory() {
		File file = new File(tempDir, "test.txt");
		assertDoesNotThrow(() -> helper.ensureExistence(file));
	}

	@Test
	public void testEnsureExistenceNotADirectory() throws Exception {
		File regularFile = new File(tempDir, "notadir");
		regularFile.createNewFile();
		File child = new File(regularFile, "test.txt");
		assertThrows(RuntimeException.class, () -> helper.ensureExistence(child));
	}
}
