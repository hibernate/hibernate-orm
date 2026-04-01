/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.hbm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HbmExporterTest {

	@Test
	public void testGetName() {
		HbmExporter exporter = new HbmExporter();
		assertEquals("hbm2hbmxml", exporter.getName());
	}

	@Test
	public void testSetGlobalSettings() {
		HbmExporter exporter = new HbmExporter();
		HibernateMappingGlobalSettings hgs = new HibernateMappingGlobalSettings();
		exporter.setGlobalSettings(hgs);
		// No exception means success; field is package-private
		assertNotNull(exporter.globalSettings);
	}

	@Test
	public void testDefaultGlobalSettings() {
		HbmExporter exporter = new HbmExporter();
		assertNotNull(exporter.globalSettings);
	}
}
