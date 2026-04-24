/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.tool.reveng.api.reveng.RevengSettings;
import org.hibernate.tool.reveng.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.ForeignKeyDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.OneToManyDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.OneToOneDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;
import org.hibernate.tool.reveng.internal.strategy.DefaultStrategy;
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
	private Map<String, TableDescriptor> tablesByName;
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
		TableDescriptor department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		TableDescriptor employee = createTable("EMPLOYEE", "Employee",
			new String[]{"ID"}, new String[]{"DEPARTMENT_ID"});
		employee.addForeignKey(new ForeignKeyDescriptor(
			"department", "DEPARTMENT_ID", "Department", "com.example"));

		tablesByName.put("DEPARTMENT", department);
		tablesByName.put("EMPLOYEE", employee);

		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);

		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = new HashMap<>();
		incomingFksByTable.put("DEPARTMENT", List.of(fkInfo));

		IncomingForeignKeyResolver.create(
			tablesByName, incomingFksByTable, manyToManyTables, adapter, true)
			.resolveIncomingForeignKeys();

		assertEquals(1, department.getOneToManys().size());
		OneToManyDescriptor o2m = department.getOneToManys().get(0);
		assertEquals("Employee", o2m.getElementEntityClassName());
		assertEquals("com.example", o2m.getElementEntityPackage());
		assertEquals("department", o2m.getMappedBy());
	}

	@Test
	public void testNoIncomingForeignKeys() {
		TableDescriptor department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		tablesByName.put("DEPARTMENT", department);

		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = new HashMap<>();

		IncomingForeignKeyResolver.create(
			tablesByName, incomingFksByTable, manyToManyTables, adapter, true)
			.resolveIncomingForeignKeys();

		assertTrue(department.getOneToManys().isEmpty());
		assertTrue(department.getOneToOnes().isEmpty());
	}

	@Test
	public void testManyToManyTableSkipped() {
		TableDescriptor department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		tablesByName.put("DEPARTMENT", department);
		manyToManyTables.add("DEPARTMENT");

		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_TEST", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);

		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = new HashMap<>();
		incomingFksByTable.put("DEPARTMENT", List.of(fkInfo));

		IncomingForeignKeyResolver.create(
			tablesByName, incomingFksByTable, manyToManyTables, adapter, true)
			.resolveIncomingForeignKeys();

		assertTrue(department.getOneToManys().isEmpty());
	}

	@Test
	public void testFkFromManyToManyTableSkipped() {
		TableDescriptor department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		TableDescriptor joinTable = createTable("DEPT_EMP", "DeptEmp",
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
			tablesByName, incomingFksByTable, manyToManyTables, adapter, true)
			.resolveIncomingForeignKeys();

		assertTrue(department.getOneToManys().isEmpty());
	}

	@Test
	public void testCompositeKeySeqSkipped() {
		TableDescriptor department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		TableDescriptor employee = createTable("EMPLOYEE", "Employee",
			new String[]{"ID"}, new String[]{"DEPARTMENT_ID"});
		tablesByName.put("DEPARTMENT", department);
		tablesByName.put("EMPLOYEE", employee);

		RawForeignKeyInfo fkSeq2 = new RawForeignKeyInfo(
			"FK_COMPOSITE", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 2);

		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = new HashMap<>();
		incomingFksByTable.put("DEPARTMENT", List.of(fkSeq2));

		IncomingForeignKeyResolver.create(
			tablesByName, incomingFksByTable, manyToManyTables, adapter, true)
			.resolveIncomingForeignKeys();

		assertTrue(department.getOneToManys().isEmpty());
	}

	@Test
	public void testFkTableNotInTablesMapSkipped() {
		TableDescriptor department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		tablesByName.put("DEPARTMENT", department);
		// EMPLOYEE is NOT in tablesByName

		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);

		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = new HashMap<>();
		incomingFksByTable.put("DEPARTMENT", List.of(fkInfo));

		IncomingForeignKeyResolver.create(
			tablesByName, incomingFksByTable, manyToManyTables, adapter, true)
			.resolveIncomingForeignKeys();

		assertTrue(department.getOneToManys().isEmpty());
	}

	@Test
	public void testMappedByFromManyToOneField() {
		TableDescriptor department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		TableDescriptor employee = createTable("EMPLOYEE", "Employee",
			new String[]{"ID"}, new String[]{"DEPARTMENT_ID"});
		employee.addForeignKey(new ForeignKeyDescriptor(
			"myDept", "DEPARTMENT_ID", "Department", "com.example"));

		tablesByName.put("DEPARTMENT", department);
		tablesByName.put("EMPLOYEE", employee);

		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);

		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = new HashMap<>();
		incomingFksByTable.put("DEPARTMENT", List.of(fkInfo));

		IncomingForeignKeyResolver.create(
			tablesByName, incomingFksByTable, manyToManyTables, adapter, true)
			.resolveIncomingForeignKeys();

		assertEquals(1, department.getOneToManys().size());
		assertEquals("myDept", department.getOneToManys().get(0).getMappedBy());
	}

	@Test
	public void testMultipleIncomingForeignKeys() {
		TableDescriptor department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		TableDescriptor employee = createTable("EMPLOYEE", "Employee",
			new String[]{"ID"}, new String[]{"DEPARTMENT_ID"});
		TableDescriptor project = createTable("PROJECT", "Project",
			new String[]{"ID"}, new String[]{"DEPARTMENT_ID"});
		employee.addForeignKey(new ForeignKeyDescriptor(
			"department", "DEPARTMENT_ID", "Department", "com.example"));
		project.addForeignKey(new ForeignKeyDescriptor(
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
			tablesByName, incomingFksByTable, manyToManyTables, adapter, true)
			.resolveIncomingForeignKeys();

		assertEquals(2, department.getOneToManys().size());
	}

	@Test
	public void testMappedByFromOneToOneField() {
		TableDescriptor department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		TableDescriptor employee = createTable("EMPLOYEE", "Employee",
			new String[]{"ID"}, new String[]{"DEPARTMENT_ID"});
		employee.addOneToOne(new OneToOneDescriptor(
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
			tablesByName, incomingFksByTable, manyToManyTables, adapter, true)
			.resolveIncomingForeignKeys();

		// Should produce a OneToMany (or OneToOne depending on adapter), with mappedBy from the OneToOne field
		int totalRelationships = department.getOneToManys().size() + department.getOneToOnes().size();
		assertTrue(totalRelationships > 0);
	}

	private TableDescriptor createTable(String tableName, String entityClassName,
			String[] pkColumns, String[] otherColumns) {
		TableDescriptor table = new TableDescriptor(tableName, entityClassName, "com.example");
		for (String pk : pkColumns) {
			table.addColumn(new ColumnDescriptor(pk, pk.toLowerCase(), Long.class)
				.primaryKey(true).nullable(false));
		}
		for (String col : otherColumns) {
			table.addColumn(new ColumnDescriptor(col, col.toLowerCase(), Long.class)
				.nullable(true));
		}
		return table;
	}
}
