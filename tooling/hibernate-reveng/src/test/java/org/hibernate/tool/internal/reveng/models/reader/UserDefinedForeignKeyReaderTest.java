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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link UserDefinedForeignKeyReader}.
 *
 * @author Koen Aers
 */
public class UserDefinedForeignKeyReaderTest {

	private DefaultStrategy strategy;

	@BeforeEach
	public void setUp() {
		strategy = new DefaultStrategy();
		RevengSettings settings = new RevengSettings(strategy);
		settings.setDefaultPackageName("com.example");
		strategy.setSettings(settings);
	}

	@Test
	public void testNoUserDefinedForeignKeys() {
		Map<String, TableMetadata> tables = new LinkedHashMap<>();
		tables.put("EMPLOYEE", new TableMetadata("EMPLOYEE", "Employee", "com.example"));

		UserDefinedForeignKeyReader reader = UserDefinedForeignKeyReader.create(
			strategy, null, null);
		List<RawForeignKeyInfo> result = reader.readUserForeignKeys(tables);

		assertTrue(result.isEmpty());
	}

	@Test
	public void testSingleUserDefinedForeignKey() {
		DefaultStrategy customStrategy = new DefaultStrategy() {
			@Override
			public List<ForeignKey> getForeignKeys(TableIdentifier referencedTable) {
				if ("DEPARTMENT".equals(referencedTable.getName())) {
					Table empTable = new Table("Hibernate Tools");
					empTable.setName("EMPLOYEE");
					Column fkCol = new Column("DEPARTMENT_ID");
					empTable.addColumn(fkCol);

					Table deptTable = new Table("Hibernate Tools");
					deptTable.setName("DEPARTMENT");
					Column pkCol = new Column("ID");
					deptTable.addColumn(pkCol);

					List<Column> fkColumns = new ArrayList<>();
					fkColumns.add(fkCol);
					List<Column> refColumns = new ArrayList<>();
					refColumns.add(pkCol);

					ForeignKey fk = empTable.createForeignKey(
						"FK_EMP_DEPT_USER", fkColumns, "DEPARTMENT", null, null, refColumns);
					fk.setReferencedTable(deptTable);

					return Arrays.asList(fk);
				}
				return super.getForeignKeys(referencedTable);
			}
		};
		RevengSettings settings = new RevengSettings(customStrategy);
		settings.setDefaultPackageName("com.example");
		customStrategy.setSettings(settings);

		Map<String, TableMetadata> tables = new LinkedHashMap<>();
		tables.put("DEPARTMENT", new TableMetadata("DEPARTMENT", "Department", "com.example"));
		tables.put("EMPLOYEE", new TableMetadata("EMPLOYEE", "Employee", "com.example"));

		UserDefinedForeignKeyReader reader = UserDefinedForeignKeyReader.create(
			customStrategy, null, null);
		List<RawForeignKeyInfo> result = reader.readUserForeignKeys(tables);

		assertEquals(1, result.size());
		RawForeignKeyInfo fk = result.get(0);
		assertEquals("FK_EMP_DEPT_USER", fk.fkName());
		assertEquals("EMPLOYEE", fk.fkTableName());
		assertEquals("DEPARTMENT_ID", fk.fkColumnName());
		assertEquals("ID", fk.pkColumnName());
		assertEquals("DEPARTMENT", fk.referencedTableName());
		assertEquals(1, fk.keySeq());
	}

	@Test
	public void testEmptyTablesMap() {
		UserDefinedForeignKeyReader reader = UserDefinedForeignKeyReader.create(
			strategy, null, null);
		List<RawForeignKeyInfo> result = reader.readUserForeignKeys(Map.of());

		assertTrue(result.isEmpty());
	}

	@Test
	public void testDefaultCatalogAndSchemaUsed() {
		Map<String, TableMetadata> tables = new LinkedHashMap<>();
		tables.put("EMPLOYEE", new TableMetadata("EMPLOYEE", "Employee", "com.example"));

		UserDefinedForeignKeyReader reader = UserDefinedForeignKeyReader.create(
			strategy, "DEFAULT_CAT", "DEFAULT_SCHEMA");
		List<RawForeignKeyInfo> result = reader.readUserForeignKeys(tables);

		assertTrue(result.isEmpty());
	}
}
