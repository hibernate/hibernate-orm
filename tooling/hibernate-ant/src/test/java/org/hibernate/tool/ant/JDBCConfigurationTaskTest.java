/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JDBCConfigurationTaskTest {

	@Test
	public void testDefaultDescription() {
		JDBCConfigurationTask task = new JDBCConfigurationTask();
		assertEquals("JDBC Configuration (for reverse engineering)", task.getDescription());
	}

	@Test
	public void testDefaultValues() {
		JDBCConfigurationTask task = new JDBCConfigurationTask();
		assertTrue(task.preferBasicCompositeIds);
		assertTrue(task.detectOneToOne);
		assertTrue(task.detectManyToMany);
		assertTrue(task.detectOptimisticLock);
		assertNull(task.reverseEngineeringStrategyClass);
		assertNull(task.packageName);
		assertNull(task.revengFiles);
	}

	@Test
	public void testSetPackageName() {
		JDBCConfigurationTask task = new JDBCConfigurationTask();
		task.setPackageName("com.example.model");
		assertEquals("com.example.model", task.packageName);
	}

	@Test
	public void testSetReverseStrategy() {
		JDBCConfigurationTask task = new JDBCConfigurationTask();
		task.setReverseStrategy("com.example.MyStrategy");
		assertEquals("com.example.MyStrategy", task.reverseEngineeringStrategyClass);
	}

	@Test
	public void testSetPreferBasicCompositeIds() {
		JDBCConfigurationTask task = new JDBCConfigurationTask();
		task.setPreferBasicCompositeIds(false);
		assertFalse(task.preferBasicCompositeIds);
	}

	@Test
	public void testSetDetectOneToOne() {
		JDBCConfigurationTask task = new JDBCConfigurationTask();
		task.setDetectOneToOne(false);
		assertFalse(task.detectOneToOne);
	}

	@Test
	public void testSetDetectManyToMany() {
		JDBCConfigurationTask task = new JDBCConfigurationTask();
		task.setDetectManyToMany(false);
		assertFalse(task.detectManyToMany);
	}

	@Test
	public void testSetDetectOptimisticLock() {
		JDBCConfigurationTask task = new JDBCConfigurationTask();
		task.setDetectOptimisticLock(false);
		assertFalse(task.detectOptimisticLock);
	}

	@Test
	public void testClone() throws CloneNotSupportedException {
		JDBCConfigurationTask task = new JDBCConfigurationTask();
		task.setPackageName("com.example");
		task.setReverseStrategy("com.example.Strategy");
		task.setPreferBasicCompositeIds(false);
		task.setDetectOneToOne(false);
		task.setDetectManyToMany(false);
		task.setDetectOptimisticLock(false);

		JDBCConfigurationTask clone = (JDBCConfigurationTask) task.clone();
		assertNotNull(clone);
		assertEquals("com.example", clone.packageName);
		assertEquals("com.example.Strategy", clone.reverseEngineeringStrategyClass);
		assertFalse(clone.preferBasicCompositeIds);
		assertFalse(clone.detectOneToOne);
		assertFalse(clone.detectManyToMany);
		assertFalse(clone.detectOptimisticLock);
	}
}
