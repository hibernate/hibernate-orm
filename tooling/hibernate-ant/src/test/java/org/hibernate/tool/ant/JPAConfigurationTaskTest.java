/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.apache.tools.ant.BuildException;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JPAConfigurationTaskTest {

	@Test
	public void testDefaultDescription() {
		JPAConfigurationTask task = new JPAConfigurationTask();
		assertEquals("JPA Configuration", task.getDescription());
	}

	@Test
	public void testPersistenceUnit() {
		JPAConfigurationTask task = new JPAConfigurationTask();
		assertNull(task.getPersistenceUnit());
		task.setPersistenceUnit("myPU");
		assertEquals("myPU", task.getPersistenceUnit());
	}

	@Test
	public void testSetConfigurationFileThrows() {
		JPAConfigurationTask task = new JPAConfigurationTask();
		assertThrows(BuildException.class, () -> task.setConfigurationFile(new File("/tmp/cfg.xml")));
	}

	@Test
	public void testClone() throws CloneNotSupportedException {
		JPAConfigurationTask task = new JPAConfigurationTask();
		task.setPersistenceUnit("testPU");
		JPAConfigurationTask clone = (JPAConfigurationTask) task.clone();
		assertNotNull(clone);
		assertEquals("testPU", clone.getPersistenceUnit());
	}
}
