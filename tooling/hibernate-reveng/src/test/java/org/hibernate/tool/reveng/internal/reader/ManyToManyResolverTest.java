/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.tool.reveng.api.reveng.RevengSettings;
import org.hibernate.tool.reveng.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.ManyToManyDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;
import org.hibernate.tool.reveng.internal.strategy.DefaultStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ManyToManyResolver}.
 *
 * @author Koen Aers
 */
public class ManyToManyResolverTest {

	private DefaultStrategy strategy;
	private RevengStrategyAdapter adapter;
	private Map<String, TableDescriptor> tablesByName;
	private Map<String, List<RawForeignKeyInfo>> outgoingFksByTable;

	@BeforeEach
	public void setUp() {
		strategy = new DefaultStrategy();
		RevengSettings settings = new RevengSettings(strategy);
		settings.setDefaultPackageName("com.example");
		settings.setDetectManyToMany(true);
		strategy.setSettings(settings);
		adapter = RevengStrategyAdapter.create(strategy);
		tablesByName = new HashMap<>();
		outgoingFksByTable = new HashMap<>();
	}

	@Test
	public void testFilterManyToManyTables() {
		TableDescriptor users = createTable("USERS", "Users", new String[]{"ID"}, new String[]{});
		TableDescriptor roles = createTable("ROLES", "Roles", new String[]{"ID"}, new String[]{});
		TableDescriptor joinTable = createTable("USER_ROLE", "UserRole",
			new String[]{"USER_ID", "ROLE_ID"}, new String[]{});

		tablesByName.put("USERS", users);
		tablesByName.put("ROLES", roles);
		tablesByName.put("USER_ROLE", joinTable);

		RawForeignKeyInfo fk1 = new RawForeignKeyInfo(
			"FK_UR_USER", "USER_ROLE", null, null,
			"USER_ID", "ID", "USERS", null, null, 1);
		RawForeignKeyInfo fk2 = new RawForeignKeyInfo(
			"FK_UR_ROLE", "USER_ROLE", null, null,
			"ROLE_ID", "ID", "ROLES", null, null, 1);
		List<RawForeignKeyInfo> joinFks = new ArrayList<>();
		joinFks.add(fk1);
		joinFks.add(fk2);
		outgoingFksByTable.put("USER_ROLE", joinFks);

		ManyToManyResolver resolver = ManyToManyResolver.create(tablesByName, outgoingFksByTable, adapter);
		Set<String> m2mTables = resolver.filterManyToManyTables();

		assertTrue(m2mTables.contains("USER_ROLE"));
		assertFalse(m2mTables.contains("USERS"));
		assertFalse(m2mTables.contains("ROLES"));
	}

	@Test
	public void testFilterNoManyToManyTables() {
		TableDescriptor employee = createTable("EMPLOYEE", "Employee",
			new String[]{"ID"}, new String[]{"NAME"});
		tablesByName.put("EMPLOYEE", employee);

		ManyToManyResolver resolver = ManyToManyResolver.create(tablesByName, outgoingFksByTable, adapter);
		Set<String> m2mTables = resolver.filterManyToManyTables();

		assertTrue(m2mTables.isEmpty());
	}

	@Test
	public void testFilterEmptyColumnTableSkipped() {
		TableDescriptor emptyTable = new TableDescriptor("EMPTY_TABLE", "EmptyTable", "com.example");
		tablesByName.put("EMPTY_TABLE", emptyTable);

		ManyToManyResolver resolver = ManyToManyResolver.create(tablesByName, outgoingFksByTable, adapter);
		Set<String> m2mTables = resolver.filterManyToManyTables();

		assertFalse(m2mTables.contains("EMPTY_TABLE"));
	}

	@Test
	public void testResolveManyToManyRelationships() {
		TableDescriptor users = createTable("USERS", "Users", new String[]{"ID"}, new String[]{});
		TableDescriptor roles = createTable("ROLES", "Roles", new String[]{"ID"}, new String[]{});
		TableDescriptor joinTable = createTable("USER_ROLE", "UserRole",
			new String[]{"USER_ID", "ROLE_ID"}, new String[]{});

		tablesByName.put("USERS", users);
		tablesByName.put("ROLES", roles);
		tablesByName.put("USER_ROLE", joinTable);

		RawForeignKeyInfo fk1 = new RawForeignKeyInfo(
			"FK_UR_USER", "USER_ROLE", null, null,
			"USER_ID", "ID", "USERS", null, null, 1);
		RawForeignKeyInfo fk2 = new RawForeignKeyInfo(
			"FK_UR_ROLE", "USER_ROLE", null, null,
			"ROLE_ID", "ID", "ROLES", null, null, 1);
		List<RawForeignKeyInfo> joinFks = new ArrayList<>();
		joinFks.add(fk1);
		joinFks.add(fk2);
		outgoingFksByTable.put("USER_ROLE", joinFks);

		ManyToManyResolver resolver = ManyToManyResolver.create(tablesByName, outgoingFksByTable, adapter);
		Set<String> m2mTables = resolver.filterManyToManyTables();
		resolver.resolveManyToManyRelationships(m2mTables);

		assertEquals(1, users.getManyToManys().size());
		assertEquals(1, roles.getManyToManys().size());

		// Both sides should have @JoinTable with swapped columns
		ManyToManyDescriptor usersM2m = users.getManyToManys().get(0);
		ManyToManyDescriptor rolesM2m = roles.getManyToManys().get(0);

		assertNotNull(usersM2m.getJoinTableName(), "Users side should have joinTable");
		assertNotNull(rolesM2m.getJoinTableName(), "Roles side should have joinTable");
		assertEquals("USER_ROLE", usersM2m.getJoinTableName());
		assertEquals("USER_ROLE", rolesM2m.getJoinTableName());
	}

	@Test
	public void testResolveSkipsJoinTableNotInMap() {
		TableDescriptor users = createTable("USERS", "Users", new String[]{"ID"}, new String[]{});
		tablesByName.put("USERS", users);
		// USER_ROLE is NOT in tablesByName

		Set<String> m2mTables = Set.of("USER_ROLE");

		ManyToManyResolver resolver = ManyToManyResolver.create(tablesByName, outgoingFksByTable, adapter);
		resolver.resolveManyToManyRelationships(m2mTables);

		assertTrue(users.getManyToManys().isEmpty());
	}

	@Test
	public void testResolveSkipsWhenNotExactlyTwoFks() {
		TableDescriptor users = createTable("USERS", "Users", new String[]{"ID"}, new String[]{});
		TableDescriptor joinTable = createTable("BAD_JOIN", "BadJoin",
			new String[]{"USER_ID"}, new String[]{});

		tablesByName.put("USERS", users);
		tablesByName.put("BAD_JOIN", joinTable);

		// Only one FK — not a valid M2M join table for resolution
		RawForeignKeyInfo fk1 = new RawForeignKeyInfo(
			"FK_BJ_USER", "BAD_JOIN", null, null,
			"USER_ID", "ID", "USERS", null, null, 1);
		outgoingFksByTable.put("BAD_JOIN", List.of(fk1));

		Set<String> m2mTables = Set.of("BAD_JOIN");

		ManyToManyResolver resolver = ManyToManyResolver.create(tablesByName, outgoingFksByTable, adapter);
		resolver.resolveManyToManyRelationships(m2mTables);

		assertTrue(users.getManyToManys().isEmpty());
	}

	@Test
	public void testResolveSkipsWhenReferencedTableMissing() {
		TableDescriptor users = createTable("USERS", "Users", new String[]{"ID"}, new String[]{});
		TableDescriptor joinTable = createTable("USER_ROLE", "UserRole",
			new String[]{"USER_ID", "ROLE_ID"}, new String[]{});

		tablesByName.put("USERS", users);
		tablesByName.put("USER_ROLE", joinTable);
		// ROLES is NOT in tablesByName

		RawForeignKeyInfo fk1 = new RawForeignKeyInfo(
			"FK_UR_USER", "USER_ROLE", null, null,
			"USER_ID", "ID", "USERS", null, null, 1);
		RawForeignKeyInfo fk2 = new RawForeignKeyInfo(
			"FK_UR_ROLE", "USER_ROLE", null, null,
			"ROLE_ID", "ID", "ROLES", null, null, 1);
		List<RawForeignKeyInfo> joinFks = new ArrayList<>();
		joinFks.add(fk1);
		joinFks.add(fk2);
		outgoingFksByTable.put("USER_ROLE", joinFks);

		Set<String> m2mTables = Set.of("USER_ROLE");

		ManyToManyResolver resolver = ManyToManyResolver.create(tablesByName, outgoingFksByTable, adapter);
		resolver.resolveManyToManyRelationships(m2mTables);

		assertTrue(users.getManyToManys().isEmpty());
	}

	@Test
	public void testCompositeKeySeqFilteredInResolve() {
		TableDescriptor users = createTable("USERS", "Users", new String[]{"ID"}, new String[]{});
		TableDescriptor roles = createTable("ROLES", "Roles", new String[]{"ID"}, new String[]{});
		TableDescriptor joinTable = createTable("USER_ROLE", "UserRole",
			new String[]{"USER_ID", "ROLE_ID"}, new String[]{});

		tablesByName.put("USERS", users);
		tablesByName.put("ROLES", roles);
		tablesByName.put("USER_ROLE", joinTable);

		RawForeignKeyInfo fk1 = new RawForeignKeyInfo(
			"FK_UR_USER", "USER_ROLE", null, null,
			"USER_ID", "ID", "USERS", null, null, 1);
		RawForeignKeyInfo fk2 = new RawForeignKeyInfo(
			"FK_UR_ROLE", "USER_ROLE", null, null,
			"ROLE_ID", "ID", "ROLES", null, null, 1);
		// Extra composite FK column — should be filtered out
		RawForeignKeyInfo fk3 = new RawForeignKeyInfo(
			"FK_UR_USER", "USER_ROLE", null, null,
			"USER_ID_2", "ID_2", "USERS", null, null, 2);

		List<RawForeignKeyInfo> joinFks = new ArrayList<>();
		joinFks.add(fk1);
		joinFks.add(fk2);
		joinFks.add(fk3);
		outgoingFksByTable.put("USER_ROLE", joinFks);

		ManyToManyResolver resolver = ManyToManyResolver.create(tablesByName, outgoingFksByTable, adapter);
		Set<String> m2mTables = Set.of("USER_ROLE");
		resolver.resolveManyToManyRelationships(m2mTables);

		// Should still resolve — fk3 with keySeq=2 is filtered out, leaving exactly 2
		assertEquals(1, users.getManyToManys().size());
		assertEquals(1, roles.getManyToManys().size());
	}

	@Test
	public void testBothSidesHaveJoinTableWithSwappedColumns() {
		TableDescriptor users = createTable("USERS", "Users", new String[]{"ID"}, new String[]{});
		TableDescriptor roles = createTable("ROLES", "Roles", new String[]{"ID"}, new String[]{});
		TableDescriptor joinTable = createTable("USER_ROLE", "UserRole",
			new String[]{"USER_ID", "ROLE_ID"}, new String[]{});

		tablesByName.put("USERS", users);
		tablesByName.put("ROLES", roles);
		tablesByName.put("USER_ROLE", joinTable);

		RawForeignKeyInfo fk1 = new RawForeignKeyInfo(
			"FK_UR_USER", "USER_ROLE", null, null,
			"USER_ID", "ID", "USERS", null, null, 1);
		RawForeignKeyInfo fk2 = new RawForeignKeyInfo(
			"FK_UR_ROLE", "USER_ROLE", null, null,
			"ROLE_ID", "ID", "ROLES", null, null, 1);
		List<RawForeignKeyInfo> joinFks = new ArrayList<>();
		joinFks.add(fk1);
		joinFks.add(fk2);
		outgoingFksByTable.put("USER_ROLE", joinFks);

		ManyToManyResolver resolver = ManyToManyResolver.create(tablesByName, outgoingFksByTable, adapter);
		Set<String> m2mTables = Set.of("USER_ROLE");
		resolver.resolveManyToManyRelationships(m2mTables);

		// Both sides should have @JoinTable
		assertEquals(1, users.getManyToManys().size());
		assertEquals(1, roles.getManyToManys().size());

		ManyToManyDescriptor usersM2m = users.getManyToManys().get(0);
		ManyToManyDescriptor rolesM2m = roles.getManyToManys().get(0);

		assertNotNull(usersM2m.getJoinTableName(), "Users side should have @JoinTable");
		assertNotNull(rolesM2m.getJoinTableName(), "Roles side should have @JoinTable");
		assertEquals("USER_ROLE", usersM2m.getJoinTableName());
		assertEquals("USER_ROLE", rolesM2m.getJoinTableName());

		// Each side should have joinColumns and inverseJoinColumns
		assertNotNull(usersM2m.getJoinColumnName());
		assertNotNull(usersM2m.getInverseJoinColumnName());
		assertNotNull(rolesM2m.getJoinColumnName());
		assertNotNull(rolesM2m.getInverseJoinColumnName());

		// The columns should be swapped between the two sides
		assertEquals(usersM2m.getJoinColumnName(), rolesM2m.getInverseJoinColumnName());
		assertEquals(usersM2m.getInverseJoinColumnName(), rolesM2m.getJoinColumnName());
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
