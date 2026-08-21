/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Hbm2HbmXmlExporterTaskTest {

	@Test
	public void testGetName() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2HbmXmlExporterTask task = new Hbm2HbmXmlExporterTask(parent);
		assertEquals("hbm2hbmxml (Generates a set of hbm.xml files)", task.getName());
	}
}
