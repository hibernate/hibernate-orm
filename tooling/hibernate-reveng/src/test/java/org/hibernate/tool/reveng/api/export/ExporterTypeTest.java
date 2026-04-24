/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.export;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ExporterTypeTest {

	@Test
	public void testExporterType() {
		assertEquals(
				"org.hibernate.tool.reveng.internal.exporter.entity.EntityExporter",
				ExporterType.JAVA.className());
		assertEquals(
				"org.hibernate.tool.reveng.internal.exporter.cfg.CfgXmlExporter",
				ExporterType.CFG.className());
	}

}
