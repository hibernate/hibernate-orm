/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileExporterTest {

	@Test
	public void testExportAndRelease(@TempDir File tempDir) throws Exception {
		File outputFile = new File(tempDir, "output.sql");
		FileExporter exporter = new FileExporter(outputFile.getAbsolutePath());
		assertFalse(exporter.acceptsImportScripts());
		exporter.export("CREATE TABLE test (id INT)");
		exporter.export("CREATE TABLE test2 (id INT)");
		exporter.release();
		String content = new String(Files.readAllBytes(outputFile.toPath()));
		assertTrue(content.contains("CREATE TABLE test (id INT)"));
		assertTrue(content.contains("CREATE TABLE test2 (id INT)"));
	}
}
