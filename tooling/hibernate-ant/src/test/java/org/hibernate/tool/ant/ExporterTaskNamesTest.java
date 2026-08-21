/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExporterTaskNamesTest {

	private final HibernateToolTask parent = new HibernateToolTask();

	@Test
	public void testHbm2CfgXmlExporterTaskName() {
		assertEquals("hbm2cfgxml (Generates hibernate.cfg.xml)",
				new Hbm2CfgXmlExporterTask(parent).getName());
	}

	@Test
	public void testHbm2JavaExporterTaskName() {
		assertEquals("hbm2java (Generates a set of .java files)",
				new Hbm2JavaExporterTask(parent).getName());
	}

	@Test
	public void testHbm2DAOExporterTaskName() {
		assertEquals("hbm2dao (Generates a set of DAOs)",
				new Hbm2DAOExporterTask(parent).getName());
	}

	@Test
	public void testHbm2DocExporterTaskName() {
		assertEquals("hbm2doc (Generates html schema documentation)",
				new Hbm2DocExporterTask(parent).getName());
	}

	@Test
	public void testHbmLintExporterTaskName() {
		assertEquals("hbmlint (scans mapping for errors)",
				new HbmLintExporterTask(parent).getName());
	}

	@Test
	public void testHbm2HbmXmlExporterTaskName() {
		assertEquals("hbm2hbmxml (Generates a set of hbm.xml files)",
				new Hbm2HbmXmlExporterTask(parent).getName());
	}

	@Test
	public void testHbm2JavaSetters() {
		Hbm2JavaExporterTask task = new Hbm2JavaExporterTask(parent);
		task.setEjb3(false);
		task.setJdk5(false);
		// verify defaults were true
		Hbm2JavaExporterTask fresh = new Hbm2JavaExporterTask(parent);
		assertEquals(true, fresh.ejb3);
		assertEquals(true, fresh.jdk5);
	}

	@Test
	public void testHbm2CfgXmlSetEjb3() {
		Hbm2CfgXmlExporterTask task = new Hbm2CfgXmlExporterTask(parent);
		task.setEjb3(true);
	}
}
