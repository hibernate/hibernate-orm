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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToOneMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OutgoingForeignKeyResolver}.
 *
 * @author Koen Aers
 */
public class OutgoingForeignKeyResolverTest {

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
	public void testSimpleManyToOne() {
		TableMetadata department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		TableMetadata employee = createTable("EMPLOYEE", "Employee",
			new String[]{"ID"}, new String[]{"DEPARTMENT_ID"});

		tablesByName.put("DEPARTMENT", department);
		tablesByName.put("EMPLOYEE", employee);

		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);

		Map<String, List<RawForeignKeyInfo>> outgoingFksByTable = new HashMap<>();
		outgoingFksByTable.put("EMPLOYEE", List.of(fkInfo));

		OutgoingForeignKeyResolver.create(tablesByName, outgoingFksByTable, manyToManyTables, adapter)
			.resolveOutgoingForeignKeys();

		assertEquals(1, employee.getForeignKeys().size());
		ForeignKeyMetadata fk = employee.getForeignKeys().get(0);
		assertEquals("department", fk.getFieldName());
		assertEquals("DEPARTMENT_ID", fk.getForeignKeyColumnName());
		assertEquals("Department", fk.getTargetEntityClassName());
		assertEquals("com.example", fk.getTargetEntityPackage());
	}

	@Test
	public void testNoOutgoingForeignKeys() {
		TableMetadata department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		tablesByName.put("DEPARTMENT", department);

		Map<String, List<RawForeignKeyInfo>> outgoingFksByTable = new HashMap<>();

		OutgoingForeignKeyResolver.create(tablesByName, outgoingFksByTable, manyToManyTables, adapter)
			.resolveOutgoingForeignKeys();

		assertTrue(department.getForeignKeys().isEmpty());
		assertTrue(department.getOneToOnes().isEmpty());
	}

	@Test
	public void testManyToManyTableSkipped() {
		TableMetadata joinTable = createTable("USER_ROLE", "UserRole",
			new String[]{"USER_ID", "ROLE_ID"}, new String[]{});
		tablesByName.put("USER_ROLE", joinTable);
		manyToManyTables.add("USER_ROLE");

		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_UR_USER", "USER_ROLE", null, null,
			"USER_ID", "ID", "USERS", null, null, 1);

		Map<String, List<RawForeignKeyInfo>> outgoingFksByTable = new HashMap<>();
		outgoingFksByTable.put("USER_ROLE", List.of(fkInfo));

		OutgoingForeignKeyResolver.create(tablesByName, outgoingFksByTable, manyToManyTables, adapter)
			.resolveOutgoingForeignKeys();

		assertTrue(joinTable.getForeignKeys().isEmpty());
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

		Map<String, List<RawForeignKeyInfo>> outgoingFksByTable = new HashMap<>();
		outgoingFksByTable.put("EMPLOYEE", List.of(fkSeq2));

		OutgoingForeignKeyResolver.create(tablesByName, outgoingFksByTable, manyToManyTables, adapter)
			.resolveOutgoingForeignKeys();

		assertTrue(employee.getForeignKeys().isEmpty());
	}

	@Test
	public void testReferencedTableNotInMapSkipped() {
		TableMetadata employee = createTable("EMPLOYEE", "Employee",
			new String[]{"ID"}, new String[]{"DEPARTMENT_ID"});
		tablesByName.put("EMPLOYEE", employee);
		// DEPARTMENT is NOT in tablesByName

		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);

		Map<String, List<RawForeignKeyInfo>> outgoingFksByTable = new HashMap<>();
		outgoingFksByTable.put("EMPLOYEE", List.of(fkInfo));

		OutgoingForeignKeyResolver.create(tablesByName, outgoingFksByTable, manyToManyTables, adapter)
			.resolveOutgoingForeignKeys();

		assertTrue(employee.getForeignKeys().isEmpty());
	}

	@Test
	public void testMultipleOutgoingForeignKeys() {
		TableMetadata department = createTable("DEPARTMENT", "Department",
			new String[]{"ID"}, new String[]{});
		TableMetadata company = createTable("COMPANY", "Company",
			new String[]{"ID"}, new String[]{});
		TableMetadata employee = createTable("EMPLOYEE", "Employee",
			new String[]{"ID"}, new String[]{"DEPARTMENT_ID", "COMPANY_ID"});

		tablesByName.put("DEPARTMENT", department);
		tablesByName.put("COMPANY", company);
		tablesByName.put("EMPLOYEE", employee);

		RawForeignKeyInfo fk1 = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);
		RawForeignKeyInfo fk2 = new RawForeignKeyInfo(
			"FK_EMP_COMP", "EMPLOYEE", null, null,
			"COMPANY_ID", "ID", "COMPANY", null, null, 1);

		Map<String, List<RawForeignKeyInfo>> outgoingFksByTable = new HashMap<>();
		List<RawForeignKeyInfo> fks = new ArrayList<>();
		fks.add(fk1);
		fks.add(fk2);
		outgoingFksByTable.put("EMPLOYEE", fks);

		OutgoingForeignKeyResolver.create(tablesByName, outgoingFksByTable, manyToManyTables, adapter)
			.resolveOutgoingForeignKeys();

		assertEquals(2, employee.getForeignKeys().size());
	}

	@Test
	public void testIsUniqueReferenceTrue() {
		RawForeignKeyInfo fkInfo = new RawForeignKeyInfo(
			"FK_EMP_DEPT", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);

		List<RawForeignKeyInfo> allFks = List.of(fkInfo);

		assertTrue(OutgoingForeignKeyResolver.isUniqueReference(fkInfo, allFks));
	}

	@Test
	public void testIsUniqueReferenceFalse() {
		RawForeignKeyInfo fk1 = new RawForeignKeyInfo(
			"FK_EMP_DEPT1", "EMPLOYEE", null, null,
			"DEPARTMENT_ID", "ID", "DEPARTMENT", null, null, 1);
		RawForeignKeyInfo fk2 = new RawForeignKeyInfo(
			"FK_EMP_DEPT2", "EMPLOYEE", null, null,
			"SECONDARY_DEPT_ID", "ID", "DEPARTMENT", null, null, 1);

		List<RawForeignKeyInfo> allFks = List.of(fk1, fk2);

		assertFalse(OutgoingForeignKeyResolver.isUniqueReference(fk1, allFks));
		assertFalse(OutgoingForeignKeyResolver.isUniqueReference(fk2, allFks));
	}

	@Test
	public void testIsUniqueReferenceIgnoresCompositeKeySeq() {
		RawForeignKeyInfo fk1 = new RawForeignKeyInfo(
			"FK_COMPOSITE", "EMPLOYEE", null, null,
			"COL_A", "ID_A", "DEPARTMENT", null, null, 1);
		RawForeignKeyInfo fk2 = new RawForeignKeyInfo(
			"FK_COMPOSITE", "EMPLOYEE", null, null,
			"COL_B", "ID_B", "DEPARTMENT", null, null, 2);

		List<RawForeignKeyInfo> allFks = List.of(fk1, fk2);

		assertTrue(OutgoingForeignKeyResolver.isUniqueReference(fk1, allFks));
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
