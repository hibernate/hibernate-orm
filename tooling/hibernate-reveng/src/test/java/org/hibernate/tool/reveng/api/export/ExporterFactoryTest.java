/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.export;

import org.hibernate.tool.reveng.internal.export.common.AbstractExporter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

	public static class TestExporter extends AbstractExporter {
		@Override protected void doStart() {}
	}

}
