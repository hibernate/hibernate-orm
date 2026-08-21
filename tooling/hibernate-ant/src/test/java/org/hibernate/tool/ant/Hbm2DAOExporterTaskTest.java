/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Hbm2DAOExporterTaskTest {

	@Test
	public void testGetName() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DAOExporterTask task = new Hbm2DAOExporterTask(parent);
		assertEquals("hbm2dao (Generates a set of DAOs)", task.getName());
	}
}
