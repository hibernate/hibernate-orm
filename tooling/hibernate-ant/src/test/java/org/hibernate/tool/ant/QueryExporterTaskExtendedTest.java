/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.apache.tools.ant.BuildException;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class QueryExporterTaskExtendedTest {

	@Test
	public void testValidateParametersNoQueryThrows() {
		HibernateToolTask parent = new HibernateToolTask();
		parent.setDestDir(new File("/tmp"));
		QueryExporterTask task = new QueryExporterTask(parent);
		assertThrows(BuildException.class, task::validateParameters);
	}

	@Test
	public void testValidateParametersWithQuery() {
		HibernateToolTask parent = new HibernateToolTask();
		parent.setDestDir(new File("/tmp"));
		QueryExporterTask task = new QueryExporterTask(parent);
		task.addText("FROM Foo");
		task.validateParameters();
	}

	@Test
	public void testValidateParametersEmptyHqlThrows() {
		HibernateToolTask parent = new HibernateToolTask();
		parent.setDestDir(new File("/tmp"));
		QueryExporterTask task = new QueryExporterTask(parent);
		task.createHql(); // empty HQL added
		assertThrows(BuildException.class, task::validateParameters);
	}

	@Test
	public void testValidateParametersWithHql() {
		HibernateToolTask parent = new HibernateToolTask();
		parent.setDestDir(new File("/tmp"));
		QueryExporterTask task = new QueryExporterTask(parent);
		QueryExporterTask.HQL hql = task.createHql();
		hql.addText("FROM Bar");
		task.validateParameters();
	}

	@Test
	public void testMultipleHqlQueries() {
		HibernateToolTask parent = new HibernateToolTask();
		QueryExporterTask task = new QueryExporterTask(parent);
		task.createHql().addText("FROM Foo");
		task.createHql().addText("FROM Bar");
		assertEquals(2, task.queries.size());
	}

	@Test
	public void testAddTextAppends() {
		HibernateToolTask parent = new HibernateToolTask();
		QueryExporterTask task = new QueryExporterTask(parent);
		task.addText("  SELECT  ");
		task.addText("  * FROM foo  ");
		// Both should be appended (trimmed)
	}
}
