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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.tool.api.reveng.RevengDialect;
import org.hibernate.tool.api.reveng.RevengSettings;
import jakarta.persistence.TemporalType;

import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.CompositeIdMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ManyToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModelsDatabaseSchemaReader}, using a test
 * {@link RevengDialect} implementation that returns hardcoded data.
 *
 * @author Koen Aers
 */
public class ModelsDatabaseSchemaReaderTest {

	private DefaultStrategy strategy;
	private TestRevengDialect dialect;

	@BeforeEach
	public void setUp() {
		strategy = new DefaultStrategy();
		RevengSettings settings = new RevengSettings(strategy);
		settings.setDefaultPackageName("com.example");
		settings.setDetectManyToMany(true);
		settings.setDetectOneToOne(true);
		strategy.setSettings(settings);
		dialect = new TestRevengDialect();
	}

	@Test
	public void testSingleTableWithColumns() {
		dialect.addTable("EMPLOYEE", null, null);
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("EMPLOYEE", "NAME", java.sql.Types.VARCHAR, 255, 0, true);
		dialect.addColumn("EMPLOYEE", "SALARY", java.sql.Types.DECIMAL, 10, 2, true);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);

		ModelsDatabaseSchemaReader reader = ModelsDatabaseSchemaReader.create(
			dialect, strategy, null, null);
		List<TableMetadata> result = reader.readSchema();

		assertEquals(1, result.size());
		TableMetadata employee = result.get(0);
		assertEquals("EMPLOYEE", employee.getTableName());
		assertEquals("Employee", employee.getEntityClassName());
		assertEquals("com.example", employee.getEntityPackage());

		List<ColumnMetadata> columns = employee.getColumns();
		assertEquals(3, columns.size());

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

		ColumnMetadata salaryCol = columns.get(2);
		assertEquals("SALARY", salaryCol.getColumnName());
		assertEquals("salary", salaryCol.getFieldName());
	}

	@Test
	public void testTwoTablesWithForeignKey() {
		// DEPARTMENT table
		dialect.addTable("DEPARTMENT", null, null);
		dialect.addColumn("DEPARTMENT", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("DEPARTMENT", "NAME", java.sql.Types.VARCHAR, 255, 0, true);
		dialect.addPrimaryKey("DEPARTMENT", "ID", 1);

		// EMPLOYEE table with FK to DEPARTMENT
		dialect.addTable("EMPLOYEE", null, null);
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("EMPLOYEE", "NAME", java.sql.Types.VARCHAR, 255, 0, true);
		dialect.addColumn("EMPLOYEE", "DEPARTMENT_ID", java.sql.Types.BIGINT, 19, 0, true);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);

		// FK: EMPLOYEE.DEPARTMENT_ID -> DEPARTMENT.ID
		dialect.addExportedKey("DEPARTMENT", "ID", "EMPLOYEE", "DEPARTMENT_ID",
			"FK_EMP_DEPT", 1);

		ModelsDatabaseSchemaReader reader = ModelsDatabaseSchemaReader.create(
			dialect, strategy, null, null);
		List<TableMetadata> result = reader.readSchema();

		assertEquals(2, result.size());

		// Find the tables
		TableMetadata department = findTable(result, "DEPARTMENT");
		TableMetadata employee = findTable(result, "EMPLOYEE");
		assertNotNull(department);
		assertNotNull(employee);

		// Employee should have a ManyToOne FK
		assertEquals(1, employee.getForeignKeys().size());
		ForeignKeyMetadata fk = employee.getForeignKeys().get(0);
		assertEquals("department", fk.getFieldName());
		assertEquals("DEPARTMENT_ID", fk.getForeignKeyColumnName());
		assertEquals("Department", fk.getTargetEntityClassName());

		// Department should have a OneToMany
		assertEquals(1, department.getOneToManys().size());
		OneToManyMetadata o2m = department.getOneToManys().get(0);
		assertEquals("Employee", o2m.getElementEntityClassName());
		assertEquals("department", o2m.getMappedBy());
	}

	@Test
	public void testManyToManyJoinTableFiltered() {
		// USER table
		dialect.addTable("USERS", null, null);
		dialect.addColumn("USERS", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("USERS", "ID", 1);

		// ROLE table
		dialect.addTable("ROLES", null, null);
		dialect.addColumn("ROLES", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("ROLES", "ID", 1);

		// Join table USER_ROLE
		dialect.addTable("USER_ROLE", null, null);
		dialect.addColumn("USER_ROLE", "USER_ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("USER_ROLE", "ROLE_ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("USER_ROLE", "USER_ID", 1);
		dialect.addPrimaryKey("USER_ROLE", "ROLE_ID", 2);

		// FKs from join table
		dialect.addExportedKey("USERS", "ID", "USER_ROLE", "USER_ID", "FK_UR_USER", 1);
		dialect.addExportedKey("ROLES", "ID", "USER_ROLE", "ROLE_ID", "FK_UR_ROLE", 1);

		ModelsDatabaseSchemaReader reader = ModelsDatabaseSchemaReader.create(
			dialect, strategy, null, null);
		List<TableMetadata> result = reader.readSchema();

		// USER_ROLE should be filtered out as M2M join table
		assertEquals(2, result.size());
		assertNotNull(findTable(result, "USERS"));
		assertNotNull(findTable(result, "ROLES"));
		assertNull(findTable(result, "USER_ROLE"));
	}

	@Test
	public void testTemporalAndLobColumns() {
		dialect.addTable("EVENT", null, null);
		dialect.addColumn("EVENT", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("EVENT", "EVENT_DATE", java.sql.Types.DATE, 0, 0, true);
		dialect.addColumn("EVENT", "START_TIME", java.sql.Types.TIME, 0, 0, true);
		dialect.addColumn("EVENT", "CREATED_AT", java.sql.Types.TIMESTAMP, 0, 0, true);
		dialect.addColumn("EVENT", "DESCRIPTION", java.sql.Types.CLOB, 0, 0, true);
		dialect.addColumn("EVENT", "PHOTO", java.sql.Types.BLOB, 0, 0, true);
		dialect.addPrimaryKey("EVENT", "ID", 1);

		ModelsDatabaseSchemaReader reader = ModelsDatabaseSchemaReader.create(
			dialect, strategy, null, null);
		List<TableMetadata> result = reader.readSchema();

		assertEquals(1, result.size());
		TableMetadata event = result.get(0);
		List<ColumnMetadata> columns = event.getColumns();
		assertEquals(6, columns.size());

		ColumnMetadata dateCol = findColumn(columns, "EVENT_DATE");
		assertEquals(TemporalType.DATE, dateCol.getTemporalType());
		assertFalse(dateCol.isLob());

		ColumnMetadata timeCol = findColumn(columns, "START_TIME");
		assertEquals(TemporalType.TIME, timeCol.getTemporalType());

		ColumnMetadata timestampCol = findColumn(columns, "CREATED_AT");
		assertEquals(TemporalType.TIMESTAMP, timestampCol.getTemporalType());

		ColumnMetadata descCol = findColumn(columns, "DESCRIPTION");
		assertNull(descCol.getTemporalType());
		assertTrue(descCol.isLob());
		assertEquals(String.class, descCol.getJavaType());

		ColumnMetadata photoCol = findColumn(columns, "PHOTO");
		assertNull(photoCol.getTemporalType());
		assertTrue(photoCol.isLob());
		assertEquals(byte[].class, photoCol.getJavaType());

		ColumnMetadata idCol = findColumn(columns, "ID");
		assertNull(idCol.getTemporalType());
		assertFalse(idCol.isLob());
	}

	@Test
	public void testCompositeId() {
		dialect.addTable("ORDER_ITEM", null, null);
		dialect.addColumn("ORDER_ITEM", "ORDER_ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("ORDER_ITEM", "PRODUCT_ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("ORDER_ITEM", "QUANTITY", java.sql.Types.INTEGER, 10, 0, true);
		dialect.addPrimaryKey("ORDER_ITEM", "ORDER_ID", 1);
		dialect.addPrimaryKey("ORDER_ITEM", "PRODUCT_ID", 2);

		ModelsDatabaseSchemaReader reader = ModelsDatabaseSchemaReader.create(
			dialect, strategy, null, null);
		List<TableMetadata> result = reader.readSchema();

		assertEquals(1, result.size());
		TableMetadata orderItem = result.get(0);

		CompositeIdMetadata compositeId = orderItem.getCompositeId();
		assertNotNull(compositeId);
		assertEquals("id", compositeId.getFieldName());
		assertEquals("OrderItemId", compositeId.getIdClassName());
		assertEquals("com.example", compositeId.getIdClassPackage());
		assertEquals(2, compositeId.getAttributeOverrides().size());
		assertEquals("orderId", compositeId.getAttributeOverrides().get(0).getFieldName());
		assertEquals("ORDER_ID", compositeId.getAttributeOverrides().get(0).getColumnName());
		assertEquals("productId", compositeId.getAttributeOverrides().get(1).getFieldName());
		assertEquals("PRODUCT_ID", compositeId.getAttributeOverrides().get(1).getColumnName());
	}

	@Test
	public void testSinglePrimaryKeyNoCompositeId() {
		dialect.addTable("EMPLOYEE", null, null);
		dialect.addColumn("EMPLOYEE", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("EMPLOYEE", "NAME", java.sql.Types.VARCHAR, 255, 0, true);
		dialect.addPrimaryKey("EMPLOYEE", "ID", 1);

		ModelsDatabaseSchemaReader reader = ModelsDatabaseSchemaReader.create(
			dialect, strategy, null, null);
		List<TableMetadata> result = reader.readSchema();

		assertEquals(1, result.size());
		assertNull(result.get(0).getCompositeId());
	}

	@Test
	public void testManyToManyRelationships() {
		// USERS table
		dialect.addTable("USERS", null, null);
		dialect.addColumn("USERS", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("USERS", "ID", 1);

		// ROLES table
		dialect.addTable("ROLES", null, null);
		dialect.addColumn("ROLES", "ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("ROLES", "ID", 1);

		// Join table USER_ROLE
		dialect.addTable("USER_ROLE", null, null);
		dialect.addColumn("USER_ROLE", "USER_ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addColumn("USER_ROLE", "ROLE_ID", java.sql.Types.BIGINT, 19, 0, false);
		dialect.addPrimaryKey("USER_ROLE", "USER_ID", 1);
		dialect.addPrimaryKey("USER_ROLE", "ROLE_ID", 2);

		// FKs from join table
		dialect.addExportedKey("USERS", "ID", "USER_ROLE", "USER_ID", "FK_UR_USER", 1);
		dialect.addExportedKey("ROLES", "ID", "USER_ROLE", "ROLE_ID", "FK_UR_ROLE", 1);

		ModelsDatabaseSchemaReader reader = ModelsDatabaseSchemaReader.create(
			dialect, strategy, null, null);
		List<TableMetadata> result = reader.readSchema();

		// Join table should be filtered out
		assertEquals(2, result.size());
		TableMetadata users = findTable(result, "USERS");
		TableMetadata roles = findTable(result, "ROLES");
		assertNotNull(users);
		assertNotNull(roles);

		// One side should be the owning side (with joinTable), the other the inverse (with mappedBy)
		int totalM2M = users.getManyToManys().size() + roles.getManyToManys().size();
		assertEquals(2, totalM2M);

		// Find owning and inverse sides
		ManyToManyMetadata owning = null;
		ManyToManyMetadata inverse = null;
		for (ManyToManyMetadata m2m : users.getManyToManys()) {
			if (m2m.getJoinTableName() != null) {
				owning = m2m;
			} else {
				inverse = m2m;
			}
		}
		for (ManyToManyMetadata m2m : roles.getManyToManys()) {
			if (m2m.getJoinTableName() != null) {
				if (owning == null) owning = m2m;
			} else {
				if (inverse == null) inverse = m2m;
			}
		}

		assertNotNull(owning, "Should have an owning side with joinTable");
		assertNotNull(inverse, "Should have an inverse side with mappedBy");
		assertEquals("USER_ROLE", owning.getJoinTableName());
		assertNotNull(inverse.getMappedBy());
	}

	@Test
	public void testEmptySchema() {
		ModelsDatabaseSchemaReader reader = ModelsDatabaseSchemaReader.create(
			dialect, strategy, null, null);
		List<TableMetadata> result = reader.readSchema();

		assertTrue(result.isEmpty());
	}

	private TableMetadata findTable(List<TableMetadata> tables, String tableName) {
		for (TableMetadata t : tables) {
			if (t.getTableName().equals(tableName)) {
				return t;
			}
		}
		return null;
	}

	private ColumnMetadata findColumn(List<ColumnMetadata> columns, String columnName) {
		for (ColumnMetadata c : columns) {
			if (c.getColumnName().equals(columnName)) {
				return c;
			}
		}
		return null;
	}

	// ---- Test RevengDialect implementation ----

	static class TestRevengDialect implements RevengDialect {

		private final List<Map<String, Object>> tables = new ArrayList<>();
		private final Map<String, List<Map<String, Object>>> columns = new HashMap<>();
		private final Map<String, List<Map<String, Object>>> primaryKeys = new HashMap<>();
		private final Map<String, List<Map<String, Object>>> exportedKeys = new HashMap<>();
		private final Map<String, List<Map<String, Object>>> suggestedStrategies = new HashMap<>();
		private final Map<String, List<Map<String, Object>>> indexInfo = new HashMap<>();

		private boolean quoteAllNames = false;
		private String lastGetTablesCatalog;
		private String lastGetTablesSchema;
		private String lastGetTablesTable;

		void setQuoteAllNames(boolean quoteAllNames) {
			this.quoteAllNames = quoteAllNames;
		}

		String getLastGetTablesCatalog() { return lastGetTablesCatalog; }
		String getLastGetTablesSchema() { return lastGetTablesSchema; }
		String getLastGetTablesTable() { return lastGetTablesTable; }

		void addTable(String name, String catalog, String schema) {
			addTable(name, catalog, schema, "TABLE", null);
		}

		void addTable(String name, String catalog, String schema, String comment) {
			addTable(name, catalog, schema, "TABLE", comment);
		}

		void addTable(String name, String catalog, String schema, String tableType, String comment) {
			Map<String, Object> row = new HashMap<>();
			row.put("TABLE_NAME", name);
			row.put("TABLE_CAT", catalog);
			row.put("TABLE_SCHEM", schema);
			row.put("TABLE_TYPE", tableType);
			if (comment != null) {
				row.put("REMARKS", comment);
			}
			tables.add(row);
		}

		void addColumn(String tableName, String columnName, int sqlType,
				int size, int decimalDigits, boolean nullable) {
			addColumn(tableName, columnName, sqlType, size, decimalDigits, nullable, null);
		}

		void addColumn(String tableName, String columnName, int sqlType,
				int size, int decimalDigits, boolean nullable, String comment) {
			Map<String, Object> row = new HashMap<>();
			row.put("TABLE_NAME", tableName);
			row.put("COLUMN_NAME", columnName);
			row.put("DATA_TYPE", sqlType);
			row.put("COLUMN_SIZE", size);
			row.put("DECIMAL_DIGITS", decimalDigits);
			row.put("NULLABLE", nullable
				? java.sql.DatabaseMetaData.columnNullable
				: java.sql.DatabaseMetaData.columnNoNulls);
			if (comment != null) {
				row.put("REMARKS", comment);
			}
			columns.computeIfAbsent(tableName, k -> new ArrayList<>()).add(row);
		}

		void addPrimaryKey(String tableName, String columnName, int keySeq) {
			Map<String, Object> row = new HashMap<>();
			row.put("TABLE_NAME", tableName);
			row.put("COLUMN_NAME", columnName);
			row.put("KEY_SEQ", (short) keySeq);
			primaryKeys.computeIfAbsent(tableName, k -> new ArrayList<>()).add(row);
		}

		void addSuggestedPrimaryKeyStrategy(String tableName, String strategyName) {
			Map<String, Object> row = new HashMap<>();
			row.put("TABLE_NAME", tableName);
			row.put("HIBERNATE_STRATEGY", strategyName);
			suggestedStrategies.computeIfAbsent(tableName, k -> new ArrayList<>()).add(row);
		}

		void addIndexInfo(String tableName, String indexName, String columnName, boolean nonUnique) {
			Map<String, Object> row = new HashMap<>();
			row.put("TABLE_NAME", tableName);
			row.put("INDEX_NAME", indexName);
			row.put("COLUMN_NAME", columnName);
			row.put("NON_UNIQUE", nonUnique);
			row.put("TYPE", (short) 3); // tableIndexOther
			indexInfo.computeIfAbsent(tableName, k -> new ArrayList<>()).add(row);
		}

		void addExportedKey(String pkTableName, String pkColumnName,
				String fkTableName, String fkColumnName, String fkName, int keySeq) {
			Map<String, Object> row = new HashMap<>();
			row.put("TABLE_NAME", pkTableName);
			row.put("PKCOLUMN_NAME", pkColumnName);
			row.put("FKTABLE_NAME", fkTableName);
			row.put("FKTABLE_CAT", null);
			row.put("FKTABLE_SCHEM", null);
			row.put("FKCOLUMN_NAME", fkColumnName);
			row.put("FK_NAME", fkName);
			row.put("KEY_SEQ", (short) keySeq);
			exportedKeys.computeIfAbsent(pkTableName, k -> new ArrayList<>()).add(row);
		}

		@Override
		public Iterator<Map<String, Object>> getTables(String catalog, String schema, String table) {
			lastGetTablesCatalog = catalog;
			lastGetTablesSchema = schema;
			lastGetTablesTable = table;
			return tables.iterator();
		}

		@Override
		public Iterator<Map<String, Object>> getColumns(String catalog, String schema,
				String table, String column) {
			return columns.getOrDefault(table, Collections.emptyList()).iterator();
		}

		@Override
		public Iterator<Map<String, Object>> getPrimaryKeys(String catalog, String schema,
				String table) {
			return primaryKeys.getOrDefault(table, Collections.emptyList()).iterator();
		}

		@Override
		public Iterator<Map<String, Object>> getExportedKeys(String catalog, String schema,
				String table) {
			return exportedKeys.getOrDefault(table, Collections.emptyList()).iterator();
		}

		@Override
		public Iterator<Map<String, Object>> getIndexInfo(String catalog, String schema,
				String table) {
			return indexInfo.getOrDefault(table, Collections.emptyList()).iterator();
		}

		@Override
		public Iterator<Map<String, Object>> getSuggestedPrimaryKeyStrategyName(
				String catalog, String schema, String table) {
			return suggestedStrategies.getOrDefault(table, Collections.emptyList()).iterator();
		}

		@Override
		public void configure(ConnectionProvider connectionProvider) {
		}

		@Override
		public void close() {
		}

		@Override
		public void close(Iterator<?> iterator) {
		}

		@Override
		public boolean needQuote(String name) {
			return quoteAllNames;
		}
	}
}
