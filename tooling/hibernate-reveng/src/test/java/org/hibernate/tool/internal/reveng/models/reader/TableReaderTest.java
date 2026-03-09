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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.GenerationType;
import jakarta.persistence.TemporalType;

import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.api.reveng.RevengStrategy.SchemaSelection;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.CompositeIdMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.IndexMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.hibernate.tool.internal.reveng.models.reader.ModelsDatabaseSchemaReaderTest.TestRevengDialect;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TableReader}.
 *
 * @author Koen Aers
 */
public class TableReaderTest {

	private DefaultStrategy strategy;
	private TestRevengDialect dialect;

	@BeforeEach
	public void setUp() {
		strategy = new DefaultStrategy();
		RevengSettings settings = new RevengSettings(strategy);
		settings.setDefaultPackageName("com.example");
		strategy.setSettings(settings);
		dialect = new TestRevengDialect();
	}

	@Test
	public void testEmptySchema() {
		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		assertTrue(result.isEmpty());
	}

	@Test
	public void testSingleTable() {
		dialect.addTable("EMPLOYEE", null, null);
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("EMPLOYEE", "NAME", java.sql.Types.VARCHAR, 255, 0, true);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		assertEquals(1, result.size());
		assertTrue(result.containsKey("EMPLOYEE"));

		TableMetadata employee = result.get("EMPLOYEE");
		assertEquals("EMPLOYEE", employee.getTableName());
		assertEquals("Employee", employee.getEntityClassName());
		assertEquals("com.example", employee.getEntityPackage());

		List<ColumnMetadata> columns = employee.getColumns();
		assertEquals(2, columns.size());

		ColumnMetadata idCol = columns.get(0);
		assertEquals("ID", idCol.getColumnName());
		assertEquals("id", idCol.getFieldName());
		assertTrue(idCol.isPrimaryKey());
		assertFalse(idCol.isNullable());

		ColumnMetadata nameCol = columns.get(1);
		assertEquals("NAME", nameCol.getColumnName());
		assertEquals("name", nameCol.getFieldName());
		assertFalse(nameCol.isPrimaryKey());
		assertTrue(nameCol.isNullable());
		assertEquals(String.class, nameCol.getJavaType());
	}

	@Test
	public void testMultipleTables() {
		dialect.addTable("DEPARTMENT", null, null);
		dialect.addColumn("DEPARTMENT", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("DEPARTMENT", "ID", 1);

		dialect.addTable("EMPLOYEE", null, null);
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		assertEquals(2, result.size());
		assertTrue(result.containsKey("DEPARTMENT"));
		assertTrue(result.containsKey("EMPLOYEE"));
	}

	@Test
	public void testExcludedTable() {
		dialect.addTable("EMPLOYEE", null, null);
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);

		dialect.addTable("AUDIT_LOG", null, null);
		dialect.addColumn("AUDIT_LOG", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("AUDIT_LOG", "ID", 1);

		DefaultStrategy excludingStrategy = new DefaultStrategy() {
			@Override
			public boolean excludeTable(org.hibernate.tool.api.reveng.TableIdentifier ti) {
				return "AUDIT_LOG".equals(ti.getName());
			}
		};
		RevengSettings settings = new RevengSettings(excludingStrategy);
		settings.setDefaultPackageName("com.example");
		excludingStrategy.setSettings(settings);

		TableReader reader = TableReader.create(dialect, excludingStrategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		assertEquals(1, result.size());
		assertTrue(result.containsKey("EMPLOYEE"));
		assertFalse(result.containsKey("AUDIT_LOG"));
	}

	@Test
	public void testSchemaAndCatalogSetWhenDifferentFromDefaults() {
		dialect.addTable("EMPLOYEE", "MY_CATALOG", "MY_SCHEMA");
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);

		TableReader reader = TableReader.create(dialect, strategy, "DEFAULT_CAT", "DEFAULT_SCHEMA");
		Map<String, TableMetadata> result = reader.readTables();

		TableMetadata employee = result.get("EMPLOYEE");
		assertEquals("MY_SCHEMA", employee.getSchema());
		assertEquals("MY_CATALOG", employee.getCatalog());
	}

	@Test
	public void testSchemaAndCatalogNotSetWhenMatchingDefaults() {
		dialect.addTable("EMPLOYEE", "DEFAULT_CAT", "DEFAULT_SCHEMA");
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);

		TableReader reader = TableReader.create(dialect, strategy, "DEFAULT_CAT", "DEFAULT_SCHEMA");
		Map<String, TableMetadata> result = reader.readTables();

		TableMetadata employee = result.get("EMPLOYEE");
		assertNull(employee.getSchema());
		assertNull(employee.getCatalog());
	}

	@Test
	public void testCompositeId() {
		dialect.addTable("ORDER_ITEM", null, null);
		dialect.addColumn("ORDER_ITEM", "ORDER_ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("ORDER_ITEM", "PRODUCT_ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("ORDER_ITEM", "QUANTITY", java.sql.Types.INTEGER, 10, 0, true);
		dialect.addPrimaryKey("ORDER_ITEM", "ORDER_ID", 1);
		dialect.addPrimaryKey("ORDER_ITEM", "PRODUCT_ID", 2);

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		TableMetadata orderItem = result.get("ORDER_ITEM");
		CompositeIdMetadata compositeId = orderItem.getCompositeId();
		assertNotNull(compositeId);
		assertEquals("id", compositeId.getFieldName());
		assertEquals("OrderItemId", compositeId.getIdClassName());
		assertEquals("com.example", compositeId.getIdClassPackage());
		assertEquals(2, compositeId.getAttributeOverrides().size());
	}

	@Test
	public void testSinglePrimaryKeyNoCompositeId() {
		dialect.addTable("EMPLOYEE", null, null);
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("EMPLOYEE", "NAME", java.sql.Types.VARCHAR, 255, 0, true);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		assertNull(result.get("EMPLOYEE").getCompositeId());
	}

	@Test
	public void testTemporalColumns() {
		dialect.addTable("EVENT", null, null);
		dialect.addColumn("EVENT", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("EVENT", "EVENT_DATE", java.sql.Types.DATE, 0, 0, true);
		dialect.addColumn("EVENT", "START_TIME", java.sql.Types.TIME, 0, 0, true);
		dialect.addColumn("EVENT", "CREATED_AT", java.sql.Types.TIMESTAMP, 0, 0, true);
		dialect.addPrimaryKey("EVENT", "ID", 1);

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		List<ColumnMetadata> columns = result.get("EVENT").getColumns();
		assertEquals(TemporalType.DATE, findColumn(columns, "EVENT_DATE").getTemporalType());
		assertEquals(TemporalType.TIME, findColumn(columns, "START_TIME").getTemporalType());
		assertEquals(TemporalType.TIMESTAMP, findColumn(columns, "CREATED_AT").getTemporalType());
		assertNull(findColumn(columns, "ID").getTemporalType());
	}

	@Test
	public void testLobColumns() {
		dialect.addTable("DOCUMENT", null, null);
		dialect.addColumn("DOCUMENT", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("DOCUMENT", "CONTENT", java.sql.Types.CLOB, 0, 0, true);
		dialect.addColumn("DOCUMENT", "DATA", java.sql.Types.BLOB, 0, 0, true);
		dialect.addPrimaryKey("DOCUMENT", "ID", 1);

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		List<ColumnMetadata> columns = result.get("DOCUMENT").getColumns();
		assertTrue(findColumn(columns, "CONTENT").isLob());
		assertEquals(String.class, findColumn(columns, "CONTENT").getJavaType());
		assertTrue(findColumn(columns, "DATA").isLob());
		assertEquals(byte[].class, findColumn(columns, "DATA").getJavaType());
		assertFalse(findColumn(columns, "ID").isLob());
	}

	@Test
	public void testColumnTypes() {
		dialect.addTable("TYPES_TABLE", null, null);
		dialect.addColumn("TYPES_TABLE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("TYPES_TABLE", "NAME", java.sql.Types.VARCHAR, 255, 0, true);
		dialect.addColumn("TYPES_TABLE", "ACTIVE", java.sql.Types.BIT, 1, 0, true);
		dialect.addPrimaryKey("TYPES_TABLE", "ID", 1);

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		List<ColumnMetadata> columns = result.get("TYPES_TABLE").getColumns();
		assertNotNull(findColumn(columns, "ID").getJavaType());
		assertEquals(String.class, findColumn(columns, "NAME").getJavaType());
	}

	@Test
	public void testIdentityStrategy() {
		dialect.addTable("EMPLOYEE", null, null);
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("EMPLOYEE", "NAME", java.sql.Types.VARCHAR, 255, 0, true);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);
		dialect.addSuggestedPrimaryKeyStrategy("EMPLOYEE", "identity");

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		ColumnMetadata idCol = findColumn(result.get("EMPLOYEE").getColumns(), "ID");
		assertEquals(GenerationType.IDENTITY, idCol.getGenerationType());
		assertTrue(idCol.isAutoIncrement());
	}

	@Test
	public void testSequenceStrategy() {
		dialect.addTable("EMPLOYEE", null, null);
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);
		dialect.addSuggestedPrimaryKeyStrategy("EMPLOYEE", "sequence");

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		ColumnMetadata idCol = findColumn(result.get("EMPLOYEE").getColumns(), "ID");
		assertEquals(GenerationType.SEQUENCE, idCol.getGenerationType());
		assertFalse(idCol.isAutoIncrement());
	}

	@Test
	public void testAssignedStrategyNoGenerationType() {
		dialect.addTable("EMPLOYEE", null, null);
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);
		dialect.addSuggestedPrimaryKeyStrategy("EMPLOYEE", "assigned");

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		ColumnMetadata idCol = findColumn(result.get("EMPLOYEE").getColumns(), "ID");
		assertNull(idCol.getGenerationType());
	}

	@Test
	public void testNoStrategyNoGenerationType() {
		dialect.addTable("EMPLOYEE", null, null);
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		ColumnMetadata idCol = findColumn(result.get("EMPLOYEE").getColumns(), "ID");
		assertNull(idCol.getGenerationType());
	}

	@Test
	public void testCompositeIdNoStrategy() {
		dialect.addTable("ORDER_ITEM", null, null);
		dialect.addColumn("ORDER_ITEM", "ORDER_ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("ORDER_ITEM", "PRODUCT_ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("ORDER_ITEM", "ORDER_ID", 1);
		dialect.addPrimaryKey("ORDER_ITEM", "PRODUCT_ID", 2);
		dialect.addSuggestedPrimaryKeyStrategy("ORDER_ITEM", "identity");

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		// Composite ID should not get a generation strategy
		ColumnMetadata orderIdCol = findColumn(result.get("ORDER_ITEM").getColumns(), "ORDER_ID");
		assertNull(orderIdCol.getGenerationType());
	}

	@Test
	public void testTableComment() {
		dialect.addTable("EMPLOYEE", null, null, "Employee records");
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		assertEquals("Employee records", result.get("EMPLOYEE").getComment());
	}

	@Test
	public void testTableCommentNull() {
		dialect.addTable("EMPLOYEE", null, null);
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		assertNull(result.get("EMPLOYEE").getComment());
	}

	@Test
	public void testSchemaSelectionWildcardReplacement() {
		dialect.addTable("EMPLOYEE", null, null);
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);

		DefaultStrategy wildcardStrategy = new DefaultStrategy() {
			@Override
			public List<SchemaSelection> getSchemaSelections() {
				List<SchemaSelection> selections = new ArrayList<>();
				selections.add(new SchemaSelection() {
					@Override
					public String getMatchCatalog() { return "MY_CAT.*"; }
					@Override
					public String getMatchSchema() { return "MY_SCHEMA.*"; }
					@Override
					public String getMatchTable() { return "EMP.*"; }
				});
				return selections;
			}
		};
		RevengSettings settings = new RevengSettings(wildcardStrategy);
		settings.setDefaultPackageName("com.example");
		wildcardStrategy.setSettings(settings);

		TableReader reader = TableReader.create(dialect, wildcardStrategy, null, null);
		reader.readTables();

		assertEquals("MY_CAT%", dialect.getLastGetTablesCatalog());
		assertEquals("MY_SCHEMA%", dialect.getLastGetTablesSchema());
		assertEquals("EMP%", dialect.getLastGetTablesTable());
	}

	@Test
	public void testViewIncludedWithoutIndexes() {
		dialect.addTable("EMPLOYEE_VIEW", null, null, "VIEW", null);
		dialect.addColumn("EMPLOYEE_VIEW", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("EMPLOYEE_VIEW", "NAME", java.sql.Types.VARCHAR, 255, 0, true);
		dialect.addPrimaryKey("EMPLOYEE_VIEW", "ID", 1);
		dialect.addIndexInfo("EMPLOYEE_VIEW", "IDX_NAME", "NAME", true);

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		assertEquals(1, result.size());
		assertTrue(result.containsKey("EMPLOYEE_VIEW"));
		assertTrue(result.get("EMPLOYEE_VIEW").getIndexes().isEmpty(),
			"VIEWs should not have indexes processed");
	}

	@Test
	public void testSynonymIncluded() {
		dialect.addTable("EMPLOYEE_SYN", null, null, "SYNONYM", null);
		dialect.addColumn("EMPLOYEE_SYN", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("EMPLOYEE_SYN", "ID", 1);

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		assertEquals(1, result.size());
		assertTrue(result.containsKey("EMPLOYEE_SYN"));
	}

	@Test
	public void testSystemTableExcluded() {
		dialect.addTable("SYS_CONFIG", null, null, "SYSTEM TABLE", null);
		dialect.addColumn("SYS_CONFIG", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("SYS_CONFIG", "ID", 1);

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		assertTrue(result.isEmpty(), "SYSTEM TABLE type should be excluded");
	}

	@Test
	public void testTableTypeHasIndexes() {
		dialect.addTable("EMPLOYEE", null, null, "TABLE", null);
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("EMPLOYEE", "NAME", java.sql.Types.VARCHAR, 255, 0, true);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);
		dialect.addIndexInfo("EMPLOYEE", "IDX_NAME", "NAME", true);

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		assertFalse(result.get("EMPLOYEE").getIndexes().isEmpty(),
			"TABLE type should have indexes processed");
	}

	@Test
	public void testQuotedTableName() {
		dialect.setQuoteAllNames(true);
		dialect.addTable("EMPLOYEE", null, null);
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);

		TableReader reader = TableReader.create(dialect, strategy, null, null);
		Map<String, TableMetadata> result = reader.readTables();

		assertTrue(result.containsKey("`EMPLOYEE`"));
		TableMetadata employee = result.get("`EMPLOYEE`");
		assertEquals("`EMPLOYEE`", employee.getTableName());
	}

	@Test
	public void testQuotedCatalogAndSchema() {
		dialect.setQuoteAllNames(true);
		dialect.addTable("EMPLOYEE", "MY-CATALOG", "MY-SCHEMA");
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);

		TableReader reader = TableReader.create(dialect, strategy, "DEFAULT_CAT", "DEFAULT_SCHEMA");
		Map<String, TableMetadata> result = reader.readTables();

		TableMetadata employee = result.get("`EMPLOYEE`");
		assertNotNull(employee);
		assertEquals("`MY-CATALOG`", employee.getCatalog());
		assertEquals("`MY-SCHEMA`", employee.getSchema());
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
