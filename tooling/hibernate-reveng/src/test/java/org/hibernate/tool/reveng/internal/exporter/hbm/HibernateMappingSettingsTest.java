/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.hbm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HibernateMappingSettings}.
 *
 * @author Koen Aers
 */
public class HibernateMappingSettingsTest {

	@Test
	public void testDefaults() {
		HibernateMappingSettings s = HibernateMappingSettings.defaults();
		assertEquals("property", s.defaultAccess());
		assertEquals("none", s.defaultCascade());
		assertTrue(s.defaultLazy());
		assertTrue(s.autoImport());
		assertFalse(s.hasNonDefaultAccess());
		assertFalse(s.hasNonDefaultCascade());
	}

	@Test
	public void testHasNonDefaultAccessField() {
		HibernateMappingSettings s = new HibernateMappingSettings("field", "none", true, true, null, null);
		assertTrue(s.hasNonDefaultAccess());
	}

	@Test
	public void testHasNonDefaultAccessNull() {
		HibernateMappingSettings s = new HibernateMappingSettings(null, "none", true, true, null, null);
		assertFalse(s.hasNonDefaultAccess());
	}

	@Test
	public void testHasNonDefaultAccessEmpty() {
		HibernateMappingSettings s = new HibernateMappingSettings("", "none", true, true, null, null);
		assertFalse(s.hasNonDefaultAccess());
	}

	@Test
	public void testHasNonDefaultCascadeAll() {
		HibernateMappingSettings s = new HibernateMappingSettings("property", "all", true, true, null, null);
		assertTrue(s.hasNonDefaultCascade());
	}

	@Test
	public void testHasNonDefaultCascadeNull() {
		HibernateMappingSettings s = new HibernateMappingSettings("property", null, true, true, null, null);
		assertFalse(s.hasNonDefaultCascade());
	}

	@Test
	public void testHasNonDefaultCascadeEmpty() {
		HibernateMappingSettings s = new HibernateMappingSettings("property", "", true, true, null, null);
		assertFalse(s.hasNonDefaultCascade());
	}
}
