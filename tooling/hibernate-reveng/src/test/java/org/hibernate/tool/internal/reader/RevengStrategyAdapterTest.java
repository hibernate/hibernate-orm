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
package org.hibernate.tool.internal.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.internal.descriptor.TableDescriptor;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RevengStrategyAdapter}, verifying that metadata objects
 * are correctly converted to mapping objects for strategy delegation.
 *
 * @author Koen Aers
 */
public class RevengStrategyAdapterTest {

	private RevengStrategyAdapter adapter;

    @BeforeEach
	public void setUp() {
        DefaultStrategy strategy = new DefaultStrategy();
		RevengSettings settings = new RevengSettings(strategy);
		settings.setDefaultPackageName("com.example");
		settings.setDetectManyToMany(true);
		settings.setDetectOneToOne(true);
		strategy.setSettings(settings);
		adapter = RevengStrategyAdapter.create(strategy);
	}

	@Test
	public void testIsManyToManyTableTrue() {
		// A join table: 2 columns, both PK, both FK
		TableDescriptor joinTable = new TableDescriptor("USER_ROLE", "UserRole", "com.example")
			.addColumn(new ColumnDescriptor("USER_ID", "userId", long.class).primaryKey(true))
			.addColumn(new ColumnDescriptor("ROLE_ID", "roleId", long.class).primaryKey(true));

		List<RawForeignKeyInfo> outgoingFks = Arrays.asList(
			new RawForeignKeyInfo("FK_UR_USER", "USER_ROLE", null, null, "USER_ID", "ID",
				"USERS", null, null, 1),
			new RawForeignKeyInfo("FK_UR_ROLE", "USER_ROLE", null, null, "ROLE_ID", "ID",
				"ROLES", null, null, 1)
		);

		assertTrue(adapter.isManyToManyTable(joinTable, outgoingFks));
	}

	@Test
	public void testIsManyToManyTableFalse() {
		// A regular table with extra non-FK columns
		TableDescriptor regularTable = new TableDescriptor("EMPLOYEE", "Employee", "com.example")
			.addColumn(new ColumnDescriptor("ID", "id", long.class).primaryKey(true))
			.addColumn(new ColumnDescriptor("NAME", "name", String.class))
			.addColumn(new ColumnDescriptor("DEPT_ID", "deptId", long.class));

		List<RawForeignKeyInfo> outgoingFks = Collections.singletonList(
			new RawForeignKeyInfo("FK_EMP_DEPT", "EMPLOYEE", null, null, "DEPT_ID", "ID",
				"DEPARTMENT", null, null, 1)
		);

		assertFalse(adapter.isManyToManyTable(regularTable, outgoingFks));
	}

	@Test
	public void testIsManyToManyTableNoFks() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example")
			.addColumn(new ColumnDescriptor("ID", "id", long.class).primaryKey(true))
			.addColumn(new ColumnDescriptor("NAME", "name", String.class));

		assertFalse(adapter.isManyToManyTable(table, Collections.emptyList()));
	}

	@Test
	public void testIsOneToOneTrue() {
		// FK columns match PK columns → one-to-one
		TableDescriptor fkTable = new TableDescriptor("EMPLOYEE_DETAIL", "EmployeeDetail", "com.example")
			.addColumn(new ColumnDescriptor("EMPLOYEE_ID", "employeeId", long.class).primaryKey(true))
			.addColumn(new ColumnDescriptor("BIO", "bio", String.class));

		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_DETAIL_EMP", "EMPLOYEE_DETAIL", null, null, "EMPLOYEE_ID", "ID",
			"EMPLOYEE", null, null, 1);

		assertTrue(adapter.isOneToOne(fkInfo, fkTable));
	}

	@Test
	public void testIsOneToOneFalse() {
		// FK column does NOT match PK column → many-to-one
		TableDescriptor fkTable = new TableDescriptor("EMPLOYEE", "Employee", "com.example")
			.addColumn(new ColumnDescriptor("ID", "id", long.class).primaryKey(true))
			.addColumn(new ColumnDescriptor("DEPT_ID", "deptId", long.class));

		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null, "DEPT_ID", "ID",
			"DEPARTMENT", null, null, 1);

		assertFalse(adapter.isOneToOne(fkInfo, fkTable));
	}

	@Test
	public void testForeignKeyToEntityName() {
		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null, "DEPARTMENT_ID", "ID",
			"DEPARTMENT", null, null, 1);

		String name = adapter.foreignKeyToEntityName(fkInfo, true);
		assertEquals("department", name);
	}

	@Test
	public void testForeignKeyToEntityNameNotUnique() {
		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null, "DEPARTMENT_ID", "ID",
			"DEPARTMENT", null, null, 1);

		String name = adapter.foreignKeyToEntityName(fkInfo, false);
		assertEquals("departmentByDepartmentId", name);
	}

	@Test
	public void testForeignKeyToCollectionName() {
		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null, "DEPARTMENT_ID", "ID",
			"DEPARTMENT", null, null, 1);

		String name = adapter.foreignKeyToCollectionName(fkInfo, true);
		assertEquals("employees", name);
	}

	@Test
	public void testForeignKeyToCollectionNameNotUnique() {
		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null, "DEPARTMENT_ID", "ID",
			"DEPARTMENT", null, null, 1);

		String name = adapter.foreignKeyToCollectionName(fkInfo, false);
		assertEquals("employeesForDepartmentId", name);
	}

	@Test
	public void testForeignKeyToManyToManyName() {
		TableDescriptor joinTable = new TableDescriptor("USER_ROLE", "UserRole", "com.example")
			.addColumn(new ColumnDescriptor("USER_ID", "userId", long.class).primaryKey(true))
			.addColumn(new ColumnDescriptor("ROLE_ID", "roleId", long.class).primaryKey(true));

		List<RawForeignKeyInfo> joinTableFks = Arrays.asList(
			new RawForeignKeyInfo("FK_UR_USER", "USER_ROLE", null, null, "USER_ID", "ID",
				"USERS", null, null, 1),
			new RawForeignKeyInfo("FK_UR_ROLE", "USER_ROLE", null, null, "ROLE_ID", "ID",
				"ROLES", null, null, 1)
		);

		// fromFk = FK to USERS, toFk = FK to ROLES → property name should be pluralized "Roles"
		String name = adapter.foreignKeyToManyToManyName(
			joinTableFks.get(0), joinTable, joinTableFks, joinTableFks.get(1), true);
		assertEquals("roleses", name);
	}

	@Test
	public void testIsForeignKeyCollectionInverse() {
		TableDescriptor joinTable = new TableDescriptor("USER_ROLE", "UserRole", "com.example")
			.addColumn(new ColumnDescriptor("USER_ID", "userId", long.class).primaryKey(true))
			.addColumn(new ColumnDescriptor("ROLE_ID", "roleId", long.class).primaryKey(true));

		List<RawForeignKeyInfo> joinTableFks = Arrays.asList(
			new RawForeignKeyInfo("FK_UR_USER", "USER_ROLE", null, null, "USER_ID", "ID",
				"USERS", null, null, 1),
			new RawForeignKeyInfo("FK_UR_ROLE", "USER_ROLE", null, null, "ROLE_ID", "ID",
				"ROLES", null, null, 1)
		);

		// Verify the method doesn't throw and returns a valid result
		boolean inverse = adapter.isForeignKeyCollectionInverse(
			joinTableFks.get(0), joinTable, joinTableFks);
		// Default strategy returns false when FK/PK column names differ
		assertFalse(inverse);
	}
}
