/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExporterTypeTest {

	@Test
	public void testExporterType() {
		assertEquals(
				"org.hibernate.tool.reveng.internal.export.java.JavaExporter",
				ExporterType.JAVA.className());
		assertEquals(
				"org.hibernate.tool.reveng.internal.export.cfg.CfgExporter",
				ExporterType.CFG.className());
	}

}
