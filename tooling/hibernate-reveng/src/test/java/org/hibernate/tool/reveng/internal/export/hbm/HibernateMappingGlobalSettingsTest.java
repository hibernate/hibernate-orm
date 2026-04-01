/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.hbm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HibernateMappingGlobalSettingsTest {

	@Test
	public void testDefaults() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		assertTrue(settings.isDefaultLazy());
		assertTrue(settings.isAutoImport());
		assertNull(settings.getSchemaName());
		assertNull(settings.getCatalogName());
		assertNull(settings.getDefaultCascade());
		assertNull(settings.getDefaultAccess());
		assertNull(settings.getDefaultPackage());
	}

	@Test
	public void testHasNonDefaultSettingsWithDefaults() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		assertFalse(settings.hasNonDefaultSettings());
	}

	@Test
	public void testSetSchemaName() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setSchemaName("mySchema");
		assertEquals("mySchema", settings.getSchemaName());
		assertTrue(settings.hasSchemaName());
		assertTrue(settings.hasNonDefaultSettings());
	}

	@Test
	public void testSetCatalogName() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setCatalogName("myCatalog");
		assertEquals("myCatalog", settings.getCatalogName());
		assertTrue(settings.hasCatalogName());
		assertTrue(settings.hasNonDefaultSettings());
	}

	@Test
	public void testSetDefaultPackage() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setDefaultPackage("com.example");
		assertEquals("com.example", settings.getDefaultPackage());
		assertTrue(settings.hasDefaultPackage());
		assertTrue(settings.hasNonDefaultSettings());
	}

	@Test
	public void testSetDefaultCascadeNone() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setDefaultCascade("none");
		assertEquals("none", settings.getDefaultCascade());
		assertFalse(settings.hasNonDefaultCascade());
	}

	@Test
	public void testSetDefaultCascadeAll() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setDefaultCascade("all");
		assertTrue(settings.hasNonDefaultCascade());
		assertTrue(settings.hasNonDefaultSettings());
	}

	@Test
	public void testSetDefaultAccessProperty() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setDefaultAccess("property");
		assertEquals("property", settings.getDefaultAccess());
		assertFalse(settings.hasNonDefaultAccess());
	}

	@Test
	public void testSetDefaultAccessField() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setDefaultAccess("field");
		assertTrue(settings.hasNonDefaultAccess());
		assertTrue(settings.hasNonDefaultSettings());
	}

	@Test
	public void testSetAutoImportFalse() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setAutoImport(false);
		assertFalse(settings.isAutoImport());
		assertTrue(settings.hasNonDefaultSettings());
	}

	@Test
	public void testSetDefaultLazyFalse() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setDefaultLazy(false);
		assertFalse(settings.isDefaultLazy());
		assertTrue(settings.hasNonDefaultSettings());
	}

	@Test
	public void testHasSchemaNameEmpty() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setSchemaName("");
		assertFalse(settings.hasSchemaName());
	}

	@Test
	public void testHasCatalogNameEmpty() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setCatalogName("");
		assertFalse(settings.hasCatalogName());
	}

	@Test
	public void testHasDefaultPackageEmpty() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setDefaultPackage("");
		assertFalse(settings.hasDefaultPackage());
	}
}
