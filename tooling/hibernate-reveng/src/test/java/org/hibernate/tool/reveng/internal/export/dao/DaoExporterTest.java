/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.dao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DaoExporterTest {

	@Test
	public void testGetName() {
		DaoExporter exporter = new DaoExporter();
		assertEquals("hbm2dao", exporter.getName());
	}

	@Test
	public void testSessionFactoryName() {
		DaoExporter exporter = new DaoExporter();
		assertEquals("SessionFactory", exporter.getSessionFactoryName());
		exporter.setSessionFactoryName("CustomFactory");
		assertEquals("CustomFactory", exporter.getSessionFactoryName());
	}
}
