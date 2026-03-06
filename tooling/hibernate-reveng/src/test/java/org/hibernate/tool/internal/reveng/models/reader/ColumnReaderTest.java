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

import jakarta.persistence.TemporalType;

import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.hibernate.tool.internal.reveng.models.reader.ModelsDatabaseSchemaReaderTest.TestRevengDialect;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ColumnReader}.
 *
 * @author Koen Aers
 */
public class ColumnReaderTest {

	private DefaultStrategy strategy;
	private TestRevengDialect dialect;
	private TableIdentifier tableId;
	private TableMetadata tableMetadata;

	@BeforeEach
	public void setUp() {
		strategy = new DefaultStrategy();
		RevengSettings settings = new RevengSettings(strategy);
		settings.setDefaultPackageName("com.example");
		strategy.setSettings(settings);
		dialect = new TestRevengDialect();
		tableId = TableIdentifier.create(null, null, "TEST_TABLE");
		tableMetadata = new TableMetadata("TEST_TABLE", "TestTable", "com.example");
	}

	@Test
	public void testBasicColumns() {
		dialect.addColumn("TEST_TABLE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("TEST_TABLE", "NAME", java.sql.Types.VARCHAR, 255, 0, true);
		dialect.addPrimaryKey("TEST_TABLE", "ID", 1);

		ColumnReader reader = ColumnReader.create(dialect, strategy);
		reader.readColumns(tableMetadata, tableId, null, null);

		List<ColumnMetadata> columns = tableMetadata.getColumns();
		assertEquals(2, columns.size());

		ColumnMetadata idCol = findColumn(columns, "ID");
		assertEquals("id", idCol.getFieldName());
		assertTrue(idCol.isPrimaryKey());
		assertFalse(idCol.isNullable());

		ColumnMetadata nameCol = findColumn(columns, "NAME");
		assertEquals("name", nameCol.getFieldName());
		assertFalse(nameCol.isPrimaryKey());
		assertTrue(nameCol.isNullable());
		assertEquals(String.class, nameCol.getJavaType());
	}

	@Test
	public void testColumnSizeAndScale() {
		dialect.addColumn("TEST_TABLE", "PRICE", java.sql.Types.VARCHAR, 100, 0, true);
		dialect.addColumn("TEST_TABLE", "AMOUNT", java.sql.Types.VARCHAR, 50, 3, true);

		ColumnReader reader = ColumnReader.create(dialect, strategy);
		reader.readColumns(tableMetadata, tableId, null, null);

		List<ColumnMetadata> columns = tableMetadata.getColumns();
		ColumnMetadata priceCol = findColumn(columns, "PRICE");
		assertEquals(100, priceCol.getLength());
		assertEquals(100, priceCol.getPrecision());
		assertEquals(0, priceCol.getScale());

		ColumnMetadata amountCol = findColumn(columns, "AMOUNT");
		assertEquals(50, amountCol.getLength());
		assertEquals(3, amountCol.getScale());
	}

	@Test
	public void testPrimaryKeyMarking() {
		dialect.addColumn("TEST_TABLE", "COL_A", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("TEST_TABLE", "COL_B", java.sql.Types.VARCHAR, 255, 0, true);
		dialect.addColumn("TEST_TABLE", "COL_C", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("TEST_TABLE", "COL_A", 1);
		dialect.addPrimaryKey("TEST_TABLE", "COL_C", 2);

		ColumnReader reader = ColumnReader.create(dialect, strategy);
		reader.readColumns(tableMetadata, tableId, null, null);

		List<ColumnMetadata> columns = tableMetadata.getColumns();
		assertTrue(findColumn(columns, "COL_A").isPrimaryKey());
		assertFalse(findColumn(columns, "COL_B").isPrimaryKey());
		assertTrue(findColumn(columns, "COL_C").isPrimaryKey());
	}

	@Test
	public void testTemporalTypes() {
		dialect.addColumn("TEST_TABLE", "DATE_COL", java.sql.Types.DATE, 0, 0, true);
		dialect.addColumn("TEST_TABLE", "TIME_COL", java.sql.Types.TIME, 0, 0, true);
		dialect.addColumn("TEST_TABLE", "TS_COL", java.sql.Types.TIMESTAMP, 0, 0, true);
		dialect.addColumn("TEST_TABLE", "TEXT_COL", java.sql.Types.VARCHAR, 255, 0, true);

		ColumnReader reader = ColumnReader.create(dialect, strategy);
		reader.readColumns(tableMetadata, tableId, null, null);

		List<ColumnMetadata> columns = tableMetadata.getColumns();
		assertEquals(TemporalType.DATE, findColumn(columns, "DATE_COL").getTemporalType());
		assertEquals(TemporalType.TIME, findColumn(columns, "TIME_COL").getTemporalType());
		assertEquals(TemporalType.TIMESTAMP, findColumn(columns, "TS_COL").getTemporalType());
		assertNull(findColumn(columns, "TEXT_COL").getTemporalType());
	}

	@Test
	public void testLobTypes() {
		dialect.addColumn("TEST_TABLE", "CLOB_COL", java.sql.Types.CLOB, 0, 0, true);
		dialect.addColumn("TEST_TABLE", "BLOB_COL", java.sql.Types.BLOB, 0, 0, true);
		dialect.addColumn("TEST_TABLE", "TEXT_COL", java.sql.Types.VARCHAR, 255, 0, true);

		ColumnReader reader = ColumnReader.create(dialect, strategy);
		reader.readColumns(tableMetadata, tableId, null, null);

		List<ColumnMetadata> columns = tableMetadata.getColumns();
		assertTrue(findColumn(columns, "CLOB_COL").isLob());
		assertEquals(String.class, findColumn(columns, "CLOB_COL").getJavaType());
		assertTrue(findColumn(columns, "BLOB_COL").isLob());
		assertEquals(byte[].class, findColumn(columns, "BLOB_COL").getJavaType());
		assertFalse(findColumn(columns, "TEXT_COL").isLob());
	}

	@Test
	public void testExcludedColumn() {
		dialect.addColumn("TEST_TABLE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("TEST_TABLE", "SECRET", java.sql.Types.VARCHAR, 255, 0, true);
		dialect.addPrimaryKey("TEST_TABLE", "ID", 1);

		DefaultStrategy excludingStrategy = new DefaultStrategy() {
			@Override
			public boolean excludeColumn(TableIdentifier table, String column) {
				return "SECRET".equals(column);
			}
		};
		RevengSettings settings = new RevengSettings(excludingStrategy);
		settings.setDefaultPackageName("com.example");
		excludingStrategy.setSettings(settings);

		ColumnReader reader = ColumnReader.create(dialect, excludingStrategy);
		reader.readColumns(tableMetadata, tableId, null, null);

		List<ColumnMetadata> columns = tableMetadata.getColumns();
		assertEquals(1, columns.size());
		assertEquals("ID", columns.get(0).getColumnName());
	}

	@Test
	public void testNoColumns() {
		ColumnReader reader = ColumnReader.create(dialect, strategy);
		reader.readColumns(tableMetadata, tableId, null, null);

		assertTrue(tableMetadata.getColumns().isEmpty());
	}

	@Test
	public void testVersionColumn() {
		dialect.addColumn("TEST_TABLE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("TEST_TABLE", "VERSION", java.sql.Types.INTEGER, 10, 0, true);
		dialect.addPrimaryKey("TEST_TABLE", "ID", 1);

		DefaultStrategy versionStrategy = new DefaultStrategy() {
			@Override
			public String getOptimisticLockColumnName(TableIdentifier identifier) {
				return "VERSION";
			}
		};
		RevengSettings settings = new RevengSettings(versionStrategy);
		settings.setDefaultPackageName("com.example");
		versionStrategy.setSettings(settings);

		ColumnReader reader = ColumnReader.create(dialect, versionStrategy);
		reader.readColumns(tableMetadata, tableId, null, null);

		List<ColumnMetadata> columns = tableMetadata.getColumns();
		assertFalse(findColumn(columns, "ID").isVersion());
		assertTrue(findColumn(columns, "VERSION").isVersion());
	}

	@Test
	public void testNullableDefaults() {
		dialect.addColumn("TEST_TABLE", "NULLABLE_COL", java.sql.Types.VARCHAR, 255, 0, true);
		dialect.addColumn("TEST_TABLE", "NOT_NULL_COL", java.sql.Types.VARCHAR, 255, 0, false);

		ColumnReader reader = ColumnReader.create(dialect, strategy);
		reader.readColumns(tableMetadata, tableId, null, null);

		List<ColumnMetadata> columns = tableMetadata.getColumns();
		assertTrue(findColumn(columns, "NULLABLE_COL").isNullable());
		assertFalse(findColumn(columns, "NOT_NULL_COL").isNullable());
	}

	private ColumnMetadata findColumn(List<ColumnMetadata> columns, String columnName) {
		for (ColumnMetadata c : columns) {
			if (c.getColumnName().equals(columnName)) {
				return c;
			}
		}
		return null;
	}
}
