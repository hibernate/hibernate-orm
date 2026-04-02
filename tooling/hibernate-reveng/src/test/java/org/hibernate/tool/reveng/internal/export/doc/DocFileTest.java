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
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DocFileTest {

	@TempDir
	File tempDir;

	@Test
	public void testConstructor() {
		DocFolder root = new DocFolder(tempDir);
		DocFile file = new DocFile("index.html", root);
		assertEquals("index.html", file.getName());
		assertEquals(root, file.getFolder());
		assertNotNull(file.getFile());
		assertEquals(new File(tempDir, "index.html"), file.getFile());
	}

	@Test
	public void testConstructorNullName() {
		DocFolder root = new DocFolder(tempDir);
		assertThrows(IllegalArgumentException.class, () -> new DocFile(null, root));
	}

	@Test
	public void testConstructorNullFolder() {
		assertThrows(IllegalArgumentException.class, () -> new DocFile("index.html", null));
	}

	@Test
	public void testGetPathFolders() {
		DocFolder root = new DocFolder(tempDir);
		DocFolder sub = new DocFolder("sub", root);
		DocFile file = new DocFile("page.html", sub);

		List<DocFolder> path = file.getPathFolders();
		assertEquals(2, path.size());
		assertEquals(root, path.get(0));
		assertEquals(sub, path.get(1));
	}

	@Test
	public void testBuildRefToSameFolder() {
		DocFolder root = new DocFolder(tempDir);
		DocFile source = new DocFile("a.html", root);
		DocFile target = new DocFile("b.html", root);

		String ref = source.buildRefTo(target);
		assertEquals("b.html", ref);
	}

	@Test
	public void testBuildRefToSubFolder() {
		DocFolder root = new DocFolder(tempDir);
		DocFolder sub = new DocFolder("entities", root);
		DocFile source = new DocFile("index.html", root);
		DocFile target = new DocFile("Person.html", sub);

		String ref = source.buildRefTo(target);
		assertEquals("entities/Person.html", ref);
	}

	@Test
	public void testBuildRefToParentFolder() {
		DocFolder root = new DocFolder(tempDir);
		DocFolder sub = new DocFolder("entities", root);
		DocFile source = new DocFile("Person.html", sub);
		DocFile target = new DocFile("index.html", root);

		String ref = source.buildRefTo(target);
		assertEquals("../index.html", ref);
	}

	@Test
	public void testBuildRefToSiblingFolder() {
		DocFolder root = new DocFolder(tempDir);
		DocFolder entities = new DocFolder("entities", root);
		DocFolder tables = new DocFolder("tables", root);
		DocFile source = new DocFile("Person.html", entities);
		DocFile target = new DocFile("PERSON.html", tables);

		String ref = source.buildRefTo(target);
		assertEquals("../tables/PERSON.html", ref);
	}

	@Test
	public void testToString() {
		DocFolder root = new DocFolder(tempDir);
		DocFile file = new DocFile("index.html", root);
		assertEquals("index.html", file.toString());
	}
}
