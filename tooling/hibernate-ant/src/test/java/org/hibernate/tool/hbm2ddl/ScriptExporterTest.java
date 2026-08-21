/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScriptExporterTest {

	@Test
	public void testExport() throws Exception {
		ScriptExporter exporter = new ScriptExporter();
		assertFalse(exporter.acceptsImportScripts());

		PrintStream original = System.out;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		System.setOut(new PrintStream(baos));
		try {
			exporter.export("CREATE TABLE test (id INT)");
		}
		finally {
			System.setOut(original);
		}
		assertTrue(baos.toString().contains("CREATE TABLE test (id INT)"));
	}

	@Test
	public void testRelease() throws Exception {
		ScriptExporter exporter = new ScriptExporter();
		exporter.release(); // should be a no-op
	}
}
