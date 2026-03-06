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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ManyToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
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
	private Map<String, TableMetadata> tablesByName;
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
		TableMetadata users = createTable("USERS", "Users", new String[]{"ID"}, new String[]{});
		TableMetadata roles = createTable("ROLES", "Roles", new String[]{"ID"}, new String[]{});
		TableMetadata joinTable = createTable("USER_ROLE", "UserRole",
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
		TableMetadata employee = createTable("EMPLOYEE", "Employee",
			new String[]{"ID"}, new String[]{"NAME"});
		tablesByName.put("EMPLOYEE", employee);

		ManyToManyResolver resolver = ManyToManyResolver.create(tablesByName, outgoingFksByTable, adapter);
		Set<String> m2mTables = resolver.filterManyToManyTables();

		assertTrue(m2mTables.isEmpty());
	}

	@Test
	public void testFilterEmptyColumnTableMarkedAsManyToMany() {
		TableMetadata emptyTable = new TableMetadata("EMPTY_TABLE", "EmptyTable", "com.example");
		tablesByName.put("EMPTY_TABLE", emptyTable);

		ManyToManyResolver resolver = ManyToManyResolver.create(tablesByName, outgoingFksByTable, adapter);
		Set<String> m2mTables = resolver.filterManyToManyTables();

		assertTrue(m2mTables.contains("EMPTY_TABLE"));
	}

	@Test
	public void testResolveManyToManyRelationships() {
		TableMetadata users = createTable("USERS", "Users", new String[]{"ID"}, new String[]{});
		TableMetadata roles = createTable("ROLES", "Roles", new String[]{"ID"}, new String[]{});
		TableMetadata joinTable = createTable("USER_ROLE", "UserRole",
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

		// Find owning and inverse sides
		ManyToManyMetadata owning = null;
		ManyToManyMetadata inverse = null;
		for (ManyToManyMetadata m2m : users.getManyToManys()) {
			if (m2m.getJoinTableName() != null) owning = m2m;
			else inverse = m2m;
		}
		for (ManyToManyMetadata m2m : roles.getManyToManys()) {
			if (m2m.getJoinTableName() != null && owning == null) owning = m2m;
			else if (m2m.getMappedBy() != null && inverse == null) inverse = m2m;
		}

		assertNotNull(owning);
		assertNotNull(inverse);
		assertEquals("USER_ROLE", owning.getJoinTableName());
		assertNotNull(inverse.getMappedBy());
	}

	@Test
	public void testResolveSkipsJoinTableNotInMap() {
		TableMetadata users = createTable("USERS", "Users", new String[]{"ID"}, new String[]{});
		tablesByName.put("USERS", users);
		// USER_ROLE is NOT in tablesByName

		Set<String> m2mTables = Set.of("USER_ROLE");

		ManyToManyResolver resolver = ManyToManyResolver.create(tablesByName, outgoingFksByTable, adapter);
		resolver.resolveManyToManyRelationships(m2mTables);

		assertTrue(users.getManyToManys().isEmpty());
	}

	@Test
	public void testResolveSkipsWhenNotExactlyTwoFks() {
		TableMetadata users = createTable("USERS", "Users", new String[]{"ID"}, new String[]{});
		TableMetadata joinTable = createTable("BAD_JOIN", "BadJoin",
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
		TableMetadata users = createTable("USERS", "Users", new String[]{"ID"}, new String[]{});
		TableMetadata joinTable = createTable("USER_ROLE", "UserRole",
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
		TableMetadata users = createTable("USERS", "Users", new String[]{"ID"}, new String[]{});
		TableMetadata roles = createTable("ROLES", "Roles", new String[]{"ID"}, new String[]{});
		TableMetadata joinTable = createTable("USER_ROLE", "UserRole",
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
	public void testOwningSideHasJoinTableInverseSideHasMappedBy() {
		TableMetadata users = createTable("USERS", "Users", new String[]{"ID"}, new String[]{});
		TableMetadata roles = createTable("ROLES", "Roles", new String[]{"ID"}, new String[]{});
		TableMetadata joinTable = createTable("USER_ROLE", "UserRole",
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

		// Collect all M2M metadata
		List<ManyToManyMetadata> allM2m = new ArrayList<>();
		allM2m.addAll(users.getManyToManys());
		allM2m.addAll(roles.getManyToManys());

		ManyToManyMetadata owning = null;
		ManyToManyMetadata inverse = null;
		for (ManyToManyMetadata m2m : allM2m) {
			if (m2m.getJoinTableName() != null) owning = m2m;
			if (m2m.getMappedBy() != null) inverse = m2m;
		}

		assertNotNull(owning, "One side should have @JoinTable");
		assertNotNull(inverse, "Other side should have @MappedBy");
		assertEquals("USER_ROLE", owning.getJoinTableName());
		assertNotNull(owning.getJoinColumnName());
		assertNotNull(owning.getInverseJoinColumnName());
		assertEquals(owning.getFieldName(), inverse.getMappedBy());
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
