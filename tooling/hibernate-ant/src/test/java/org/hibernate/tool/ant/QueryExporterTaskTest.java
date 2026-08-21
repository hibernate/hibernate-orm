/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QueryExporterTaskTest {

	@Test
	public void testGetName() {
		HibernateToolTask parent = new HibernateToolTask();
		QueryExporterTask task = new QueryExporterTask(parent);
		assertEquals("query (Executes queries)", task.getName());
	}

	@Test
	public void testAddText() {
		HibernateToolTask parent = new HibernateToolTask();
		QueryExporterTask task = new QueryExporterTask(parent);
		task.addText("  SELECT * FROM foo  ");
		// addText appends trimmed text; just verify no exception
	}

	@Test
	public void testAddTextNull() {
		HibernateToolTask parent = new HibernateToolTask();
		QueryExporterTask task = new QueryExporterTask(parent);
		task.addText(null);
		// null should be a no-op
	}

	@Test
	public void testAddTextEmpty() {
		HibernateToolTask parent = new HibernateToolTask();
		QueryExporterTask task = new QueryExporterTask(parent);
		task.addText("");
		// empty should be a no-op
	}

	@Test
	public void testCreateHql() {
		HibernateToolTask parent = new HibernateToolTask();
		QueryExporterTask task = new QueryExporterTask(parent);
		QueryExporterTask.HQL hql = task.createHql();
		assertNotNull(hql);
		assertEquals(1, task.queries.size());
	}

	@Test
	public void testHqlAddText() {
		QueryExporterTask.HQL hql = new QueryExporterTask.HQL();
		hql.addText("  FROM Bar  ");
	}

	@Test
	public void testHqlAddTextNull() {
		QueryExporterTask.HQL hql = new QueryExporterTask.HQL();
		hql.addText(null);
	}

	@Test
	public void testIsNotEmpty() {
		HibernateToolTask parent = new HibernateToolTask();
		QueryExporterTask task = new QueryExporterTask(parent);
		assertTrue(task.isNotEmpty("hello"));
		assertFalse(task.isNotEmpty(""));
		assertFalse(task.isNotEmpty(null));
	}

	@Test
	public void testSetDestFile() {
		HibernateToolTask parent = new HibernateToolTask();
		QueryExporterTask task = new QueryExporterTask(parent);
		task.setDestFile("output.txt");
		// verify no exception
	}
}
