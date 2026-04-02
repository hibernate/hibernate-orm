/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.doc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DocFileManagerTest {

	@Test
	public void testCopyFile(@TempDir File tempDir) throws IOException {
		File target = new File(tempDir, "doc-style.css");
		DocFileManager.copy(
				getClass().getClassLoader(),
				"doc/doc-style.css",
				target
		);
		assertTrue(target.exists());
		assertTrue(target.length() > 0);
	}

	@Test
	public void testCopyFileNotFound(@TempDir File tempDir) {
		File target = new File(tempDir, "nonexistent.txt");
		assertThrows(IllegalArgumentException.class,
				() -> DocFileManager.copy(getClass().getClassLoader(), "nonexistent/file.txt", target));
	}

	@Test
	public void testGetRefDelegation() {
		DocFolder root = new DocFolder(new File("/tmp/doc"));
		DocFile from = new DocFile("index.html", root);
		DocFolder sub = new DocFolder("tables", root);
		DocFile to = new DocFile("summary.html", sub);

		// getRef returns from.buildRefTo(to)
		assertEquals(from.buildRefTo(to), "tables/summary.html");
	}
}
