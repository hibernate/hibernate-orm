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
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
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

		ModelsDatabaseSchemaReader reader = new ModelsDatabaseSchemaReader(
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

		ModelsDatabaseSchemaReader reader = new ModelsDatabaseSchemaReader(
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

		ModelsDatabaseSchemaReader reader = new ModelsDatabaseSchemaReader(
			dialect, strategy, null, null);
		List<TableMetadata> result = reader.readSchema();

		// USER_ROLE should be filtered out as M2M join table
		assertEquals(2, result.size());
		assertNotNull(findTable(result, "USERS"));
		assertNotNull(findTable(result, "ROLES"));
		assertNull(findTable(result, "USER_ROLE"));
	}

	@Test
	public void testEmptySchema() {
		ModelsDatabaseSchemaReader reader = new ModelsDatabaseSchemaReader(
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

	// ---- Test RevengDialect implementation ----

	static class TestRevengDialect implements RevengDialect {

		private final List<Map<String, Object>> tables = new ArrayList<>();
		private final Map<String, List<Map<String, Object>>> columns = new HashMap<>();
		private final Map<String, List<Map<String, Object>>> primaryKeys = new HashMap<>();
		private final Map<String, List<Map<String, Object>>> exportedKeys = new HashMap<>();

		void addTable(String name, String catalog, String schema) {
			Map<String, Object> row = new HashMap<>();
			row.put("TABLE_NAME", name);
			row.put("TABLE_CAT", catalog);
			row.put("TABLE_SCHEM", schema);
			row.put("TABLE_TYPE", "TABLE");
			tables.add(row);
		}

		void addColumn(String tableName, String columnName, int sqlType,
				int size, int decimalDigits, boolean nullable) {
			Map<String, Object> row = new HashMap<>();
			row.put("TABLE_NAME", tableName);
			row.put("COLUMN_NAME", columnName);
			row.put("DATA_TYPE", sqlType);
			row.put("COLUMN_SIZE", size);
			row.put("DECIMAL_DIGITS", decimalDigits);
			row.put("NULLABLE", nullable
				? java.sql.DatabaseMetaData.columnNullable
				: java.sql.DatabaseMetaData.columnNoNulls);
			columns.computeIfAbsent(tableName, k -> new ArrayList<>()).add(row);
		}

		void addPrimaryKey(String tableName, String columnName, int keySeq) {
			Map<String, Object> row = new HashMap<>();
			row.put("TABLE_NAME", tableName);
			row.put("COLUMN_NAME", columnName);
			row.put("KEY_SEQ", (short) keySeq);
			primaryKeys.computeIfAbsent(tableName, k -> new ArrayList<>()).add(row);
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
			return Collections.emptyIterator();
		}

		@Override
		public Iterator<Map<String, Object>> getSuggestedPrimaryKeyStrategyName(
				String catalog, String schema, String table) {
			return Collections.emptyIterator();
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
			return false;
		}
	}
}
