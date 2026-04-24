/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.reveng.RevengSettings;
import org.hibernate.tool.reveng.api.reveng.TableIdentifier;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;
import org.hibernate.tool.reveng.internal.strategy.DefaultStrategy;
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
		Map<String, TableDescriptor> tables = new LinkedHashMap<>();
		tables.put("EMPLOYEE", new TableDescriptor("EMPLOYEE", "Employee", "com.example"));

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

		Map<String, TableDescriptor> tables = new LinkedHashMap<>();
		tables.put("DEPARTMENT", new TableDescriptor("DEPARTMENT", "Department", "com.example"));
		tables.put("EMPLOYEE", new TableDescriptor("EMPLOYEE", "Employee", "com.example"));

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
		Map<String, TableDescriptor> tables = new LinkedHashMap<>();
		tables.put("EMPLOYEE", new TableDescriptor("EMPLOYEE", "Employee", "com.example"));

		UserDefinedForeignKeyReader reader = UserDefinedForeignKeyReader.create(
			strategy, "DEFAULT_CAT", "DEFAULT_SCHEMA");
		List<RawForeignKeyInfo> result = reader.readUserForeignKeys(tables);

		assertTrue(result.isEmpty());
	}
}
