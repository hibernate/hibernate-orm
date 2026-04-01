/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RevengSettingsTest {

	@Test
	public void testDefaults() {
		RevengSettings settings = new RevengSettings(null);
		assertEquals("", settings.getDefaultPackageName());
		assertTrue(settings.getDetectOptimsticLock());
		assertTrue(settings.createCollectionForForeignKey());
		assertTrue(settings.createManyToOneForForeignKey());
		assertTrue(settings.getDetectManyToMany());
		assertTrue(settings.getDetectOneToOne());
		assertNull(settings.getRootStrategy());
	}

	@Test
	public void testSetDefaultPackageName() {
		RevengSettings settings = new RevengSettings(null);
		RevengSettings result = settings.setDefaultPackageName("com.example");
		assertEquals("com.example", settings.getDefaultPackageName());
		assertEquals(settings, result);
	}

	@Test
	public void testSetDefaultPackageNameNull() {
		RevengSettings settings = new RevengSettings(null);
		settings.setDefaultPackageName("com.example");
		settings.setDefaultPackageName(null);
		assertEquals("", settings.getDefaultPackageName());
	}

	@Test
	public void testSetDefaultPackageNameTrimmed() {
		RevengSettings settings = new RevengSettings(null);
		settings.setDefaultPackageName("  com.example  ");
		assertEquals("com.example", settings.getDefaultPackageName());
	}

	@Test
	public void testSetDetectOptimisticLock() {
		RevengSettings settings = new RevengSettings(null);
		settings.setDetectOptimisticLock(false);
		assertFalse(settings.getDetectOptimsticLock());
	}

	@Test
	public void testSetCreateCollectionForForeignKey() {
		RevengSettings settings = new RevengSettings(null);
		settings.setCreateCollectionForForeignKey(false);
		assertFalse(settings.createCollectionForForeignKey());
	}

	@Test
	public void testSetCreateManyToOneForForeignKey() {
		RevengSettings settings = new RevengSettings(null);
		settings.setCreateManyToOneForForeignKey(false);
		assertFalse(settings.createManyToOneForForeignKey());
	}

	@Test
	public void testSetDetectManyToMany() {
		RevengSettings settings = new RevengSettings(null);
		settings.setDetectManyToMany(false);
		assertFalse(settings.getDetectManyToMany());
	}

	@Test
	public void testSetDetectOneToOne() {
		RevengSettings settings = new RevengSettings(null);
		settings.setDetectOneToOne(false);
		assertFalse(settings.getDetectOneToOne());
	}

	@Test
	public void testFluentApi() {
		RevengSettings settings = new RevengSettings(null);
		RevengSettings result = settings
				.setDefaultPackageName("com.example")
				.setDetectOptimisticLock(false)
				.setCreateCollectionForForeignKey(false)
				.setCreateManyToOneForForeignKey(false)
				.setDetectManyToMany(false)
				.setDetectOneToOne(false);
		assertEquals(settings, result);
	}
}
