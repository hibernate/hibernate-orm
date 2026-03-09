/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.IndexMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.hibernate.tool.internal.reveng.models.reader.ModelsDatabaseSchemaReaderTest.TestRevengDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IndexReader}.
 *
 * @author Koen Aers
 */
public class IndexReaderTest {

	private TestRevengDialect dialect;
	private TableMetadata tableMetadata;

	@BeforeEach
	public void setUp() {
		dialect = new TestRevengDialect();
		tableMetadata = new TableMetadata("EMPLOYEE", "Employee", "com.example")
			.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true))
			.addColumn(new ColumnMetadata("EMAIL", "email", String.class))
			.addColumn(new ColumnMetadata("NAME", "name", String.class))
			.addColumn(new ColumnMetadata("DEPARTMENT_ID", "departmentId", Long.class));
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

		List<IndexMetadata> indexes = tableMetadata.getIndexes();
		assertEquals(1, indexes.size());
		assertEquals("IDX_EMAIL", indexes.get(0).getIndexName());
		assertTrue(indexes.get(0).isUnique());
		assertEquals(1, indexes.get(0).getColumnNames().size());
		assertEquals("EMAIL", indexes.get(0).getColumnNames().get(0));

		// Single-column unique index should mark the column as unique
		ColumnMetadata emailCol = findColumn("EMAIL");
		assertTrue(emailCol.isUnique());
	}

	@Test
	public void testSingleColumnNonUniqueIndex() {
		dialect.addIndexInfo("EMPLOYEE", "IDX_DEPT", "DEPARTMENT_ID", true);

		IndexReader reader = IndexReader.create(dialect);
		reader.readIndexes(tableMetadata, null, null);

		List<IndexMetadata> indexes = tableMetadata.getIndexes();
		assertEquals(1, indexes.size());
		assertFalse(indexes.get(0).isUnique());

		// Non-unique index should NOT mark column as unique
		ColumnMetadata deptCol = findColumn("DEPARTMENT_ID");
		assertFalse(deptCol.isUnique());
	}

	@Test
	public void testCompositeUniqueIndex() {
		dialect.addIndexInfo("EMPLOYEE", "IDX_NAME_DEPT", "NAME", false);
		dialect.addIndexInfo("EMPLOYEE", "IDX_NAME_DEPT", "DEPARTMENT_ID", false);

		IndexReader reader = IndexReader.create(dialect);
		reader.readIndexes(tableMetadata, null, null);

		List<IndexMetadata> indexes = tableMetadata.getIndexes();
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

		List<IndexMetadata> indexes = tableMetadata.getIndexes();
		assertEquals(2, indexes.size());
	}

	private ColumnMetadata findColumn(String columnName) {
		for (ColumnMetadata c : tableMetadata.getColumns()) {
			if (c.getColumnName().equals(columnName)) {
				return c;
			}
		}
		return null;
	}
}
