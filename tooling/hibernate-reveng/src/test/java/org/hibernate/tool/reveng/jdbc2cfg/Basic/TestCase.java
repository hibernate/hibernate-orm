/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.Basic;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private Metadata metadata = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null)
				.createMetadata();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testBasic() {
		JUnitUtil.assertIteratorContainsExactly(
				"There should be three tables!",
				metadata.getEntityBindings().iterator(),
				3);
		Table table = HibernateUtil.getTable(
				metadata,
				JdbcUtil.toIdentifier(this, "BASIC"));
		assertEquals(
				JdbcUtil.toIdentifier(this, "BASIC"),
				JdbcUtil.toIdentifier(this, table.getName()));
		assertEquals(2, table.getColumnSpan());
		Column basicColumn = table.getColumn(0);
		assertEquals(
				JdbcUtil.toIdentifier(this, "A"),
				JdbcUtil.toIdentifier(this, basicColumn.getName()));
		PrimaryKey key = table.getPrimaryKey();
		assertNotNull(key, "There should be a primary key!");
		assertEquals(1, key.getColumnSpan());
		Column column = key.getColumn(0);
		assertTrue(column.isUnique());
		assertSame(basicColumn, column);
	}

	@Test
	public void testScalePrecisionLength() {
		Table table = HibernateUtil.getTable(
				metadata,
				JdbcUtil.toIdentifier(this, "BASIC"));
		Column nameCol = table.getColumn(new Column(JdbcUtil.toIdentifier(this, "NAME")));
		assertEquals(20, nameCol.getLength().intValue());
		assertNull(nameCol.getPrecision());
		assertNull(nameCol.getScale());
	}

	@Test
	public void testCompositeKeys() {
		Table table = HibernateUtil.getTable(
				metadata,
				JdbcUtil.toIdentifier(this, "MULTIKEYED"));
		PrimaryKey primaryKey = table.getPrimaryKey();
		assertEquals(2, primaryKey.getColumnSpan());
	}

}
