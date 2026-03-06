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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.hibernate.tool.internal.reveng.models.reader.ModelsDatabaseSchemaReaderTest.TestRevengDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ForeignKeyReader}.
 *
 * @author Koen Aers
 */
public class ForeignKeyReaderTest {

	private TestRevengDialect dialect;

	@BeforeEach
	public void setUp() {
		dialect = new TestRevengDialect();
	}

	@Test
	public void testNoForeignKeys() {
		Map<String, TableMetadata> tables = new LinkedHashMap<>();
		tables.put("EMPLOYEE", new TableMetadata("EMPLOYEE", "Employee", "com.example"));

		ForeignKeyReader reader = ForeignKeyReader.create(dialect, null, null);
		List<RawForeignKeyInfo> result = reader.readForeignKeys(tables);

		assertTrue(result.isEmpty());
	}

	@Test
	public void testSingleForeignKey() {
		dialect.addExportedKey("DEPARTMENT", "ID", "EMPLOYEE", "DEPARTMENT_ID", "FK_EMP_DEPT", 1);

		Map<String, TableMetadata> tables = new LinkedHashMap<>();
		tables.put("DEPARTMENT", new TableMetadata("DEPARTMENT", "Department", "com.example"));

		ForeignKeyReader reader = ForeignKeyReader.create(dialect, null, null);
		List<RawForeignKeyInfo> result = reader.readForeignKeys(tables);

		assertEquals(1, result.size());
		RawForeignKeyInfo fk = result.get(0);
		assertEquals("FK_EMP_DEPT", fk.fkName());
		assertEquals("EMPLOYEE", fk.fkTableName());
		assertEquals("DEPARTMENT_ID", fk.fkColumnName());
		assertEquals("ID", fk.pkColumnName());
		assertEquals("DEPARTMENT", fk.referencedTableName());
		assertEquals(1, fk.keySeq());
	}

	@Test
	public void testMultipleForeignKeysFromDifferentTables() {
		dialect.addExportedKey("DEPARTMENT", "ID", "EMPLOYEE", "DEPARTMENT_ID", "FK_EMP_DEPT", 1);
		dialect.addExportedKey("DEPARTMENT", "ID", "PROJECT", "DEPARTMENT_ID", "FK_PROJ_DEPT", 1);

		Map<String, TableMetadata> tables = new LinkedHashMap<>();
		tables.put("DEPARTMENT", new TableMetadata("DEPARTMENT", "Department", "com.example"));

		ForeignKeyReader reader = ForeignKeyReader.create(dialect, null, null);
		List<RawForeignKeyInfo> result = reader.readForeignKeys(tables);

		assertEquals(2, result.size());
		assertEquals("FK_EMP_DEPT", result.get(0).fkName());
		assertEquals("FK_PROJ_DEPT", result.get(1).fkName());
	}

	@Test
	public void testCompositeForeignKey() {
		dialect.addExportedKey("ORDER_ITEM", "ORDER_ID", "SHIPMENT", "ORDER_ID", "FK_SHIP_OI", 1);
		dialect.addExportedKey("ORDER_ITEM", "PRODUCT_ID", "SHIPMENT", "PRODUCT_ID", "FK_SHIP_OI", 2);

		Map<String, TableMetadata> tables = new LinkedHashMap<>();
		tables.put("ORDER_ITEM", new TableMetadata("ORDER_ITEM", "OrderItem", "com.example"));

		ForeignKeyReader reader = ForeignKeyReader.create(dialect, null, null);
		List<RawForeignKeyInfo> result = reader.readForeignKeys(tables);

		assertEquals(2, result.size());
		assertEquals("FK_SHIP_OI", result.get(0).fkName());
		assertEquals("ORDER_ID", result.get(0).fkColumnName());
		assertEquals(1, result.get(0).keySeq());
		assertEquals("FK_SHIP_OI", result.get(1).fkName());
		assertEquals("PRODUCT_ID", result.get(1).fkColumnName());
		assertEquals(2, result.get(1).keySeq());
	}

	@Test
	public void testForeignKeysFromMultipleTables() {
		dialect.addExportedKey("DEPARTMENT", "ID", "EMPLOYEE", "DEPARTMENT_ID", "FK_EMP_DEPT", 1);
		dialect.addExportedKey("EMPLOYEE", "ID", "TASK", "EMPLOYEE_ID", "FK_TASK_EMP", 1);

		Map<String, TableMetadata> tables = new LinkedHashMap<>();
		tables.put("DEPARTMENT", new TableMetadata("DEPARTMENT", "Department", "com.example"));
		tables.put("EMPLOYEE", new TableMetadata("EMPLOYEE", "Employee", "com.example"));

		ForeignKeyReader reader = ForeignKeyReader.create(dialect, null, null);
		List<RawForeignKeyInfo> result = reader.readForeignKeys(tables);

		assertEquals(2, result.size());
		assertEquals("DEPARTMENT", result.get(0).referencedTableName());
		assertEquals("EMPLOYEE", result.get(1).referencedTableName());
	}

	@Test
	public void testEmptyTablesMap() {
		ForeignKeyReader reader = ForeignKeyReader.create(dialect, null, null);
		List<RawForeignKeyInfo> result = reader.readForeignKeys(Map.of());

		assertTrue(result.isEmpty());
	}

	@Test
	public void testDefaultCatalogAndSchemaUsedWhenTableHasNone() {
		dialect.addExportedKey("DEPARTMENT", "ID", "EMPLOYEE", "DEPARTMENT_ID", "FK_EMP_DEPT", 1);

		Map<String, TableMetadata> tables = new LinkedHashMap<>();
		tables.put("DEPARTMENT", new TableMetadata("DEPARTMENT", "Department", "com.example"));

		ForeignKeyReader reader = ForeignKeyReader.create(dialect, "DEFAULT_CAT", "DEFAULT_SCHEMA");
		List<RawForeignKeyInfo> result = reader.readForeignKeys(tables);

		assertEquals(1, result.size());
		assertEquals("DEFAULT_CAT", result.get(0).referencedCatalog());
		assertEquals("DEFAULT_SCHEMA", result.get(0).referencedSchema());
	}

	@Test
	public void testTableCatalogAndSchemaOverrideDefaults() {
		dialect.addExportedKey("DEPARTMENT", "ID", "EMPLOYEE", "DEPARTMENT_ID", "FK_EMP_DEPT", 1);

		Map<String, TableMetadata> tables = new LinkedHashMap<>();
		TableMetadata dept = new TableMetadata("DEPARTMENT", "Department", "com.example");
		dept.setCatalog("MY_CAT");
		dept.setSchema("MY_SCHEMA");
		tables.put("DEPARTMENT", dept);

		ForeignKeyReader reader = ForeignKeyReader.create(dialect, "DEFAULT_CAT", "DEFAULT_SCHEMA");
		List<RawForeignKeyInfo> result = reader.readForeignKeys(tables);

		assertEquals(1, result.size());
		assertEquals("MY_CAT", result.get(0).referencedCatalog());
		assertEquals("MY_SCHEMA", result.get(0).referencedSchema());
	}
}
