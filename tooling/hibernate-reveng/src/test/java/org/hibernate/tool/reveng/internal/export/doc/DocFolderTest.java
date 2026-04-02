/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.doc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DocFolderTest {

	@TempDir
	File tempDir;

	@Test
	public void testRootConstructor() {
		DocFolder root = new DocFolder(tempDir);
		assertNull(root.getName());
		assertNull(root.getParent());
		assertEquals(tempDir, root.getFile());
	}

	@Test
	public void testRootConstructorNull() {
		assertThrows(IllegalArgumentException.class, () -> new DocFolder((File) null));
	}

	@Test
	public void testChildConstructor() {
		DocFolder root = new DocFolder(tempDir);
		DocFolder child = new DocFolder("entities", root);
		assertEquals("entities", child.getName());
		assertEquals(root, child.getParent());
		assertTrue(child.getFile().exists());
		assertTrue(child.getFile().isDirectory());
	}

	@Test
	public void testChildConstructorNullName() {
		DocFolder root = new DocFolder(tempDir);
		assertThrows(IllegalArgumentException.class, () -> new DocFolder(null, root));
	}

	@Test
	public void testChildConstructorNullParent() {
		assertThrows(IllegalArgumentException.class, () -> new DocFolder("entities", null));
	}

	@Test
	public void testGetPathFoldersRoot() {
		DocFolder root = new DocFolder(tempDir);
		List<DocFolder> path = root.getPathFolders();
		assertEquals(1, path.size());
		assertEquals(root, path.get(0));
	}

	@Test
	public void testGetPathFoldersNested() {
		DocFolder root = new DocFolder(tempDir);
		DocFolder level1 = new DocFolder("l1", root);
		DocFolder level2 = new DocFolder("l2", level1);

		List<DocFolder> path = level2.getPathFolders();
		assertEquals(3, path.size());
		assertEquals(root, path.get(0));
		assertEquals(level1, path.get(1));
		assertEquals(level2, path.get(2));
	}

	@Test
	public void testToString() {
		DocFolder root = new DocFolder(tempDir);
		DocFolder child = new DocFolder("entities", root);
		assertEquals("entities", child.toString());
	}

	@Test
	public void testExistingDirectory() {
		// Pre-create the directory
		File existing = new File(tempDir, "preexisting");
		assertTrue(existing.mkdir());
		DocFolder root = new DocFolder(tempDir);
		DocFolder child = new DocFolder("preexisting", root);
		assertNotNull(child);
		assertTrue(child.getFile().isDirectory());
	}
}
