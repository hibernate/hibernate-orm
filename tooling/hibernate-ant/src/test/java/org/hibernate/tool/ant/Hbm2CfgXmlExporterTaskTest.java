/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Hbm2CfgXmlExporterTaskTest {

	@Test
	public void testGetName() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2CfgXmlExporterTask task = new Hbm2CfgXmlExporterTask(parent);
		assertEquals("hbm2cfgxml (Generates hibernate.cfg.xml)", task.getName());
	}

	@Test
	public void testSetEjb3() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2CfgXmlExporterTask task = new Hbm2CfgXmlExporterTask(parent);
		task.setEjb3(true);
		// Just verify no exception
	}
}
