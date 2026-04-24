/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.hibernate.tool.reveng.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.IndexDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;
import org.hibernate.tool.reveng.internal.reader.ModelsDatabaseSchemaReaderTest.TestRevengDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IndexReader}.
 *
 * @author Koen Aers
 */
public class IndexReaderTest {

	private TestRevengDialect dialect;
	private TableDescriptor tableMetadata;

	@BeforeEach
	public void setUp() {
		dialect = new TestRevengDialect();
		tableMetadata = new TableDescriptor("EMPLOYEE", "Employee", "com.example")
			.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true))
			.addColumn(new ColumnDescriptor("EMAIL", "email", String.class))
			.addColumn(new ColumnDescriptor("NAME", "name", String.class))
			.addColumn(new ColumnDescriptor("DEPARTMENT_ID", "departmentId", Long.class));
	}

	@Test
	public void testNoIndexes() {
		IndexReader reader = IndexReader.create(dialect);
		reader.readIndexes(tableMetadata, null, null);

		assertTrue(tableMetadata.getIndexes().isEmpty());
	}

	@Test
	public void testSingleColumnUniqueIndex() {
		dialect.addIndexInfo("EMPLOYEE", "IDX_EMAIL", "EMAIL", false);

		IndexReader reader = IndexReader.create(dialect);
		reader.readIndexes(tableMetadata, null, null);

		List<IndexDescriptor> indexes = tableMetadata.getIndexes();
		assertEquals(1, indexes.size());
		assertEquals("IDX_EMAIL", indexes.get(0).getIndexName());
		assertTrue(indexes.get(0).isUnique());
		assertEquals(1, indexes.get(0).getColumnNames().size());
		assertEquals("EMAIL", indexes.get(0).getColumnNames().get(0));

		// Single-column unique index should mark the column as unique
		ColumnDescriptor emailCol = findColumn("EMAIL");
		assertTrue(emailCol.isUnique());
	}

	@Test
	public void testSingleColumnNonUniqueIndex() {
		dialect.addIndexInfo("EMPLOYEE", "IDX_DEPT", "DEPARTMENT_ID", true);

		IndexReader reader = IndexReader.create(dialect);
		reader.readIndexes(tableMetadata, null, null);

		List<IndexDescriptor> indexes = tableMetadata.getIndexes();
		assertEquals(1, indexes.size());
		assertFalse(indexes.get(0).isUnique());

		// Non-unique index should NOT mark column as unique
		ColumnDescriptor deptCol = findColumn("DEPARTMENT_ID");
		assertFalse(deptCol.isUnique());
	}

	@Test
	public void testCompositeUniqueIndex() {
		dialect.addIndexInfo("EMPLOYEE", "IDX_NAME_DEPT", "NAME", false);
		dialect.addIndexInfo("EMPLOYEE", "IDX_NAME_DEPT", "DEPARTMENT_ID", false);

		IndexReader reader = IndexReader.create(dialect);
		reader.readIndexes(tableMetadata, null, null);

		List<IndexDescriptor> indexes = tableMetadata.getIndexes();
		assertEquals(1, indexes.size());
		assertTrue(indexes.get(0).isUnique());
		assertEquals(2, indexes.get(0).getColumnNames().size());

		// Multi-column unique index should NOT mark individual columns as unique
		assertFalse(findColumn("NAME").isUnique());
		assertFalse(findColumn("DEPARTMENT_ID").isUnique());
	}

	@Test
	public void testMultipleIndexes() {
		dialect.addIndexInfo("EMPLOYEE", "IDX_EMAIL", "EMAIL", false);
		dialect.addIndexInfo("EMPLOYEE", "IDX_DEPT", "DEPARTMENT_ID", true);

		IndexReader reader = IndexReader.create(dialect);
		reader.readIndexes(tableMetadata, null, null);

		List<IndexDescriptor> indexes = tableMetadata.getIndexes();
		assertEquals(2, indexes.size());
	}

	private ColumnDescriptor findColumn(String columnName) {
		for (ColumnDescriptor c : tableMetadata.getColumns()) {
			if (c.getColumnName().equals(columnName)) {
				return c;
			}
		}
		return null;
	}
}
