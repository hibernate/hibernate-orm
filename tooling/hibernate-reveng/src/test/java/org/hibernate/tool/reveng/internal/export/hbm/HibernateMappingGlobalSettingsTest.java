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
		assertNull(settings.getSchemaName());
		assertNull(settings.getCatalogName());
		assertNull(settings.getDefaultCascade());
		assertNull(settings.getDefaultPackage());
		assertNull(settings.getDefaultAccess());
		assertTrue(settings.isAutoImport());
		assertTrue(settings.isDefaultLazy());
	}

	@Test
	public void testHasNonDefaultSettingsAllDefaults() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		assertFalse(settings.hasNonDefaultSettings());
	}

	@Test
	public void testHasDefaultPackage() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		assertFalse(settings.hasDefaultPackage());
		settings.setDefaultPackage("com.example");
		assertTrue(settings.hasDefaultPackage());
		assertEquals("com.example", settings.getDefaultPackage());
	}

	@Test
	public void testHasSchemaName() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		assertFalse(settings.hasSchemaName());
		settings.setSchemaName("MY_SCHEMA");
		assertTrue(settings.hasSchemaName());
		assertEquals("MY_SCHEMA", settings.getSchemaName());
	}

	@Test
	public void testHasCatalogName() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		assertFalse(settings.hasCatalogName());
		settings.setCatalogName("MY_CATALOG");
		assertTrue(settings.hasCatalogName());
		assertEquals("MY_CATALOG", settings.getCatalogName());
	}

	@Test
	public void testHasNonDefaultCascade() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		assertFalse(settings.hasNonDefaultCascade());

		settings.setDefaultCascade("none");
		assertFalse(settings.hasNonDefaultCascade());

		settings.setDefaultCascade("all");
		assertTrue(settings.hasNonDefaultCascade());
		assertEquals("all", settings.getDefaultCascade());
	}

	@Test
	public void testHasNonDefaultAccess() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		assertFalse(settings.hasNonDefaultAccess());

		settings.setDefaultAccess("property");
		assertFalse(settings.hasNonDefaultAccess());

		settings.setDefaultAccess("field");
		assertTrue(settings.hasNonDefaultAccess());
		assertEquals("field", settings.getDefaultAccess());
	}

	@Test
	public void testAutoImport() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		assertTrue(settings.isAutoImport());
		settings.setAutoImport(false);
		assertFalse(settings.isAutoImport());
	}

	@Test
	public void testDefaultLazy() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		assertTrue(settings.isDefaultLazy());
		settings.setDefaultLazy(false);
		assertFalse(settings.isDefaultLazy());
	}

	@Test
	public void testHasNonDefaultSettingsWithPackage() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setDefaultPackage("com.example");
		assertTrue(settings.hasNonDefaultSettings());
	}

	@Test
	public void testHasNonDefaultSettingsWithSchema() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setSchemaName("SCH");
		assertTrue(settings.hasNonDefaultSettings());
	}

	@Test
	public void testHasNonDefaultSettingsNotLazy() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setDefaultLazy(false);
		assertTrue(settings.hasNonDefaultSettings());
	}

	@Test
	public void testHasNonDefaultSettingsNotAutoImport() {
		HibernateMappingGlobalSettings settings = new HibernateMappingGlobalSettings();
		settings.setAutoImport(false);
		assertTrue(settings.hasNonDefaultSettings());
	}
}
