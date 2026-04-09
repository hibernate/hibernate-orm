/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.orm.jbt.api.wrp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hibernate.tool.reveng.api.reveng.RevengSettings;
import org.hibernate.tool.orm.jbt.internal.factory.RevengSettingsWrapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RevengSettingsWrapperTest {

	private RevengSettings wrappedRevengSettings = null;
	private RevengSettingsWrapper revengSettingsWrapper = null;
	
	@BeforeEach
	public void beforeEach() {
		revengSettingsWrapper = RevengSettingsWrapperFactory.createRevengSettingsWrapper(null);
		wrappedRevengSettings = (RevengSettings)revengSettingsWrapper.getWrappedObject();
	}
	
	@Test
	public void testConstruction() {
		assertNotNull(wrappedRevengSettings);
		assertNotNull(revengSettingsWrapper);
	}
	
	
	@Test
	public void testSetDefaultPackageName() {
		assertEquals("", wrappedRevengSettings.getDefaultPackageName());
		assertSame(revengSettingsWrapper, revengSettingsWrapper.setDefaultPackageName("foo"));
		assertEquals("foo", wrappedRevengSettings.getDefaultPackageName());
	}
	
	@Test
	public void testSetDetectManyToMany() {
		assertTrue(wrappedRevengSettings.getDetectManyToMany());
		assertSame(revengSettingsWrapper, revengSettingsWrapper.setDetectManyToMany(false));
		assertFalse(wrappedRevengSettings.getDetectManyToMany());
	}
	
	@Test
	public void testSetDetectOneToOne() {
		assertTrue(wrappedRevengSettings.getDetectOneToOne());
		assertSame(revengSettingsWrapper, revengSettingsWrapper.setDetectOneToOne(false));
		assertFalse(wrappedRevengSettings.getDetectOneToOne());
	}
	
	@Test
	public void testSetDetectOptimisticLock() {
		assertTrue(wrappedRevengSettings.getDetectOptimsticLock());
		assertSame(revengSettingsWrapper, revengSettingsWrapper.setDetectOptimisticLock(false));
		assertFalse(wrappedRevengSettings.getDetectOptimsticLock());
	}
	
}
