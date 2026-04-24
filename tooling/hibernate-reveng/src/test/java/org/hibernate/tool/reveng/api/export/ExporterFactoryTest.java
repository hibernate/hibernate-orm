/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Properties;

import org.junit.jupiter.api.Test;

public class ExporterFactoryTest {

	@Test
	public void testCreateExporter() {
		try {
			ExporterFactory.createExporter("foobar");
			fail();
		} catch(Throwable t) {
			assertTrue(t.getMessage().contains("foobar"));
		}
		Exporter exporter = ExporterFactory.createExporter(
				"org.hibernate.tool.reveng.api.export.ExporterFactoryTest$TestExporter");
		assertNotNull(exporter);
		assertTrue(exporter instanceof TestExporter);
		exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		assertNotNull(exporter);
		assertEquals(
				ExporterType.JAVA.className(),
				exporter.getClass().getName());
	}

	public static class TestExporter implements Exporter {
		private final Properties properties = new Properties();
		@Override public Properties getProperties() { return properties; }
		@Override public void start() {}
	}

}
