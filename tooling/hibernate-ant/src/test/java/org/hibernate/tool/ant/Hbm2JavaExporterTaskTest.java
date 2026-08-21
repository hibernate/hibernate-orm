/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Hbm2JavaExporterTaskTest {

	@Test
	public void testGetName() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2JavaExporterTask task = new Hbm2JavaExporterTask(parent);
		assertEquals("hbm2java (Generates a set of .java files)", task.getName());
	}

	@Test
	public void testDefaultValues() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2JavaExporterTask task = new Hbm2JavaExporterTask(parent);
		assertTrue(task.ejb3);
		assertTrue(task.jdk5);
	}

	@Test
	public void testSetEjb3() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2JavaExporterTask task = new Hbm2JavaExporterTask(parent);
		task.setEjb3(false);
		assertFalse(task.ejb3);
	}

	@Test
	public void testSetJdk5() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2JavaExporterTask task = new Hbm2JavaExporterTask(parent);
		task.setJdk5(false);
		assertFalse(task.jdk5);
	}
}
