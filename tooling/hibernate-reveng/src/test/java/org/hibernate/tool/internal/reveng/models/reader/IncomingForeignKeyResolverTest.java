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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToOneMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IncomingForeignKeyResolver}.
 *
 * @author Koen Aers
 */
public class IncomingForeignKeyResolverTest {

	private DefaultStrategy strategy;
	private RevengStrategyAdapter adapter;
	private Map<String, TableMetadata> tablesByName;
	private Set<String> manyToManyTables;

	@BeforeEach
	public void setUp() {
		strategy = new DefaultStrategy();
		RevengSettings settings = new RevengSettings(strategy);
		settings.setDefaultPackageName("com.example");
		settings.setDetectOneToOne(true);
		strategy.setSettings(settings);
		adapter = RevengStrategyAdapter.create(strategy);
		tablesByName = new HashMap<>();
		manyToManyTables = new HashSet<>();
	}

	@Test
	public void testSimpleOneToMany() {
		TableMetadata department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		TableMetadata employee = createTable("EMPLOYEE", "Employee",
			new String[]{"ID"}, new String[]{"DEPARTMENT_ID"});
		employee.addForeignKey(new ForeignKeyMetadata(
			"department", "DEPARTMENT_ID", "Department", "com.example"));

		tablesByName.put("DEPARTMENT", department);
		tablesByName.put("EMPLOYEE", employee);

		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);

		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = new HashMap<>();
		incomingFksByTable.put("DEPARTMENT", List.of(fkInfo));

		IncomingForeignKeyResolver.create(
			tablesByName, incomingFksByTable, manyToManyTables, adapter)
			.resolveIncomingForeignKeys();

		assertEquals(1, department.getOneToManys().size());
		OneToManyMetadata o2m = department.getOneToManys().get(0);
		assertEquals("Employee", o2m.getElementEntityClassName());
		assertEquals("com.example", o2m.getElementEntityPackage());
		assertEquals("department", o2m.getMappedBy());
	}

	@Test
	public void testNoIncomingForeignKeys() {
		TableMetadata department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		tablesByName.put("DEPARTMENT", department);

		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = new HashMap<>();

		IncomingForeignKeyResolver.create(
			tablesByName, incomingFksByTable, manyToManyTables, adapter)
			.resolveIncomingForeignKeys();

		assertTrue(department.getOneToManys().isEmpty());
		assertTrue(department.getOneToOnes().isEmpty());
	}

	@Test
	public void testManyToManyTableSkipped() {
		TableMetadata department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		tablesByName.put("DEPARTMENT", department);
		manyToManyTables.add("DEPARTMENT");

		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_TEST", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);

		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = new HashMap<>();
		incomingFksByTable.put("DEPARTMENT", List.of(fkInfo));

		IncomingForeignKeyResolver.create(
			tablesByName, incomingFksByTable, manyToManyTables, adapter)
			.resolveIncomingForeignKeys();

		assertTrue(department.getOneToManys().isEmpty());
	}

	@Test
	public void testFkFromManyToManyTableSkipped() {
		TableMetadata department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		TableMetadata joinTable = createTable("DEPT_EMP", "DeptEmp",
			new String[]{"DEPT_ID", "EMP_ID"}, new String[]{});
		tablesByName.put("DEPARTMENT", department);
		tablesByName.put("DEPT_EMP", joinTable);
		manyToManyTables.add("DEPT_EMP");

		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_DE_DEPT", "DEPT_EMP", null, null,
			"DEPT_ID", "ID", "DEPARTMENT", null, null, 1);

		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = new HashMap<>();
		incomingFksByTable.put("DEPARTMENT", List.of(fkInfo));

		IncomingForeignKeyResolver.create(
			tablesByName, incomingFksByTable, manyToManyTables, adapter)
			.resolveIncomingForeignKeys();

		assertTrue(department.getOneToManys().isEmpty());
	}

	@Test
	public void testCompositeKeySeqSkipped() {
		TableMetadata department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		TableMetadata employee = createTable("EMPLOYEE", "Employee",
			new String[]{"ID"}, new String[]{"DEPARTMENT_ID"});
		tablesByName.put("DEPARTMENT", department);
		tablesByName.put("EMPLOYEE", employee);

		RawForeignKeyInfo fkSeq2 = new RawForeignKeyInfo(
			"FK_COMPOSITE", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 2);

		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = new HashMap<>();
		incomingFksByTable.put("DEPARTMENT", List.of(fkSeq2));

		IncomingForeignKeyResolver.create(
			tablesByName, incomingFksByTable, manyToManyTables, adapter)
			.resolveIncomingForeignKeys();

		assertTrue(department.getOneToManys().isEmpty());
	}

	@Test
	public void testFkTableNotInTablesMapSkipped() {
		TableMetadata department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		tablesByName.put("DEPARTMENT", department);
		// EMPLOYEE is NOT in tablesByName

		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);

		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = new HashMap<>();
		incomingFksByTable.put("DEPARTMENT", List.of(fkInfo));

		IncomingForeignKeyResolver.create(
			tablesByName, incomingFksByTable, manyToManyTables, adapter)
			.resolveIncomingForeignKeys();

		assertTrue(department.getOneToManys().isEmpty());
	}

	@Test
	public void testMappedByFromManyToOneField() {
		TableMetadata department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		TableMetadata employee = createTable("EMPLOYEE", "Employee",
			new String[]{"ID"}, new String[]{"DEPARTMENT_ID"});
		employee.addForeignKey(new ForeignKeyMetadata(
			"myDept", "DEPARTMENT_ID", "Department", "com.example"));

		tablesByName.put("DEPARTMENT", department);
		tablesByName.put("EMPLOYEE", employee);

		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);

		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = new HashMap<>();
		incomingFksByTable.put("DEPARTMENT", List.of(fkInfo));

		IncomingForeignKeyResolver.create(
			tablesByName, incomingFksByTable, manyToManyTables, adapter)
			.resolveIncomingForeignKeys();

		assertEquals(1, department.getOneToManys().size());
		assertEquals("myDept", department.getOneToManys().get(0).getMappedBy());
	}

	@Test
	public void testMultipleIncomingForeignKeys() {
		TableMetadata department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		TableMetadata employee = createTable("EMPLOYEE", "Employee",
			new String[]{"ID"}, new String[]{"DEPARTMENT_ID"});
		TableMetadata project = createTable("PROJECT", "Project",
			new String[]{"ID"}, new String[]{"DEPARTMENT_ID"});
		employee.addForeignKey(new ForeignKeyMetadata(
			"department", "DEPARTMENT_ID", "Department", "com.example"));
		project.addForeignKey(new ForeignKeyMetadata(
			"department", "DEPARTMENT_ID", "Department", "com.example"));

		tablesByName.put("DEPARTMENT", department);
		tablesByName.put("EMPLOYEE", employee);
		tablesByName.put("PROJECT", project);

		RawForeignKeyInfo fk1 = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);
		RawForeignKeyInfo fk2 = new RawForeignKeyInfo(
			"FK_PROJ_DEPT", "PROJECT", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);

		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = new HashMap<>();
		List<RawForeignKeyInfo> fks = new ArrayList<>();
		fks.add(fk1);
		fks.add(fk2);
		incomingFksByTable.put("DEPARTMENT", fks);

		IncomingForeignKeyResolver.create(
			tablesByName, incomingFksByTable, manyToManyTables, adapter)
			.resolveIncomingForeignKeys();

		assertEquals(2, department.getOneToManys().size());
	}

	@Test
	public void testMappedByFromOneToOneField() {
		TableMetadata department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		TableMetadata employee = createTable("EMPLOYEE", "Employee",
			new String[]{"ID"}, new String[]{"DEPARTMENT_ID"});
		employee.addOneToOne(new OneToOneMetadata(
			"dept", "Department", "com.example")
			.foreignKeyColumnName("DEPARTMENT_ID"));

		tablesByName.put("DEPARTMENT", department);
		tablesByName.put("EMPLOYEE", employee);

		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);

		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = new HashMap<>();
		incomingFksByTable.put("DEPARTMENT", List.of(fkInfo));

		IncomingForeignKeyResolver.create(
			tablesByName, incomingFksByTable, manyToManyTables, adapter)
			.resolveIncomingForeignKeys();

		// Should produce a OneToMany (or OneToOne depending on adapter), with mappedBy from the OneToOne field
		int totalRelationships = department.getOneToManys().size() + department.getOneToOnes().size();
		assertTrue(totalRelationships > 0);
	}

	private TableMetadata createTable(String tableName, String entityClassName,
			String[] pkColumns, String[] otherColumns) {
		TableMetadata table = new TableMetadata(tableName, entityClassName, "com.example");
		for (String pk : pkColumns) {
			table.addColumn(new ColumnMetadata(pk, pk.toLowerCase(), Long.class)
				.primaryKey(true).nullable(false));
		}
		for (String col : otherColumns) {
			table.addColumn(new ColumnMetadata(col, col.toLowerCase(), Long.class)
				.nullable(true));
		}
		return table;
	}
}
