/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.exporter.hbm;

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
		HibernateMappingSettings s = new HibernateMappingSettings("field", "none", true, true);
		assertTrue(s.hasNonDefaultAccess());
	}

	@Test
	public void testHasNonDefaultAccessNull() {
		HibernateMappingSettings s = new HibernateMappingSettings(null, "none", true, true);
		assertFalse(s.hasNonDefaultAccess());
	}

	@Test
	public void testHasNonDefaultAccessEmpty() {
		HibernateMappingSettings s = new HibernateMappingSettings("", "none", true, true);
		assertFalse(s.hasNonDefaultAccess());
	}

	@Test
	public void testHasNonDefaultCascadeAll() {
		HibernateMappingSettings s = new HibernateMappingSettings("property", "all", true, true);
		assertTrue(s.hasNonDefaultCascade());
	}

	@Test
	public void testHasNonDefaultCascadeNull() {
		HibernateMappingSettings s = new HibernateMappingSettings("property", null, true, true);
		assertFalse(s.hasNonDefaultCascade());
	}

	@Test
	public void testHasNonDefaultCascadeEmpty() {
		HibernateMappingSettings s = new HibernateMappingSettings("property", "", true, true);
		assertFalse(s.hasNonDefaultCascade());
	}
}
