/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.reader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.tool.reveng.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.ManyToManyDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;

/**
 * Resolves many-to-many relationships by examining join tables and
 * adding {@link ManyToManyDescriptor} to both sides of the relationship.
 *
 * @author Koen Aers
 */
class ManyToManyResolver {

	private final Map<String, TableDescriptor> tablesByName;
	private final Map<String, List<RawForeignKeyInfo>> outgoingFksByTable;
	private final RevengStrategyAdapter adapter;

	static ManyToManyResolver create(
			Map<String, TableDescriptor> tablesByName,
			Map<String, List<RawForeignKeyInfo>> outgoingFksByTable,
			RevengStrategyAdapter adapter) {
		return new ManyToManyResolver(tablesByName, outgoingFksByTable, adapter);
	}

	private ManyToManyResolver(
			Map<String, TableDescriptor> tablesByName,
			Map<String, List<RawForeignKeyInfo>> outgoingFksByTable,
			RevengStrategyAdapter adapter) {
		this.tablesByName = tablesByName;
		this.outgoingFksByTable = outgoingFksByTable;
		this.adapter = adapter;
	}

	Set<String> filterManyToManyTables() {
		Set<String> manyToManyTables = new HashSet<>();
		for (Map.Entry<String, TableDescriptor> entry : tablesByName.entrySet()) {
			String tableName = entry.getKey();
			TableDescriptor table = entry.getValue();
			if (table.getColumns().isEmpty()) {
				continue;
			}
			List<RawForeignKeyInfo> outgoingFks = outgoingFksByTable.getOrDefault(
				tableName, java.util.Collections.emptyList());
			if (adapter.isManyToManyTable(table, outgoingFks)) {
				manyToManyTables.add(tableName);
			}
		}
		return manyToManyTables;
	}

	void resolveManyToManyRelationships(Set<String> manyToManyTables) {
		for (String joinTableName : manyToManyTables) {
			handleJoinTable(joinTableName, manyToManyTables);
		}
	}

	private void handleJoinTable(String joinTableName, Set<String> manyToManyTables) {
		TableDescriptor joinTable = tablesByName.get(joinTableName);
		if (joinTable == null) {
			return;
		}
		List<RawForeignKeyInfo> outgoingFks = outgoingFksByTable.getOrDefault(
			joinTableName, java.util.Collections.emptyList());

		List<RawForeignKeyInfo> firstColumnFks = filterFirstColumnFks(outgoingFks);
		if (firstColumnFks.size() != 2) {
			return;
		}

		RawForeignKeyInfo fk1 = firstColumnFks.get(0);
		RawForeignKeyInfo fk2 = firstColumnFks.get(1);

		TableDescriptor table1 = tablesByName.get(fk1.referencedTableName());
		TableDescriptor table2 = tablesByName.get(fk2.referencedTableName());
		if (table1 == null || table2 == null) {
			return;
		}

		// Determine owning/inverse side based on column ordering
		// in the join table: the FK whose column appears first
		// is the inverse side (matching Hibernate's convention).
		int fk1Pos = columnPosition(joinTable, fk1.fkColumnName());
		int fk2Pos = columnPosition(joinTable, fk2.fkColumnName());
		boolean fk1First = fk1Pos <= fk2Pos;

		RawForeignKeyInfo owningFk;
		RawForeignKeyInfo inverseFk;
		TableDescriptor owningTable;
		TableDescriptor inverseTable;

		if (fk1First) {
			// fk1's column is first → fk1's referenced table is the inverse side
			owningFk = fk2;
			inverseFk = fk1;
			owningTable = table2;
			inverseTable = table1;
		}
		else {
			owningFk = fk1;
			inverseFk = fk2;
			owningTable = table1;
			inverseTable = table2;
		}

		handleOwningSide(joinTableName, joinTable, outgoingFks,
			owningFk, inverseFk, owningTable, inverseTable);
		handleInverseSide(joinTableName, joinTable, outgoingFks,
			owningFk, inverseFk, owningTable, inverseTable);
	}

	private void handleOwningSide(String joinTableName, TableDescriptor joinTable,
			List<RawForeignKeyInfo> outgoingFks,
			RawForeignKeyInfo owningFk, RawForeignKeyInfo inverseFk,
			TableDescriptor owningTable, TableDescriptor inverseTable) {
		String owningFieldName = adapter.foreignKeyToManyToManyName(
			owningFk, joinTable, outgoingFks, inverseFk, true);
		List<String> owningColumns = collectFkColumns(owningFk.fkName(), outgoingFks);
		List<String> inverseColumns = collectFkColumns(inverseFk.fkName(), outgoingFks);
		ManyToManyDescriptor owningM2m = new ManyToManyDescriptor(
			owningFieldName,
			inverseTable.getEntityClassName(),
			inverseTable.getEntityPackage())
			.joinTable(joinTableName, owningColumns, inverseColumns)
			.joinTableSchema(joinTable.getSchema())
			.joinTableCatalog(joinTable.getCatalog());
		owningTable.addManyToMany(owningM2m);
	}

	private List<String> collectFkColumns(String fkName, List<RawForeignKeyInfo> outgoingFks) {
		List<String> columns = new ArrayList<>();
		for (RawForeignKeyInfo fk : outgoingFks) {
			if (fk.fkName().equals(fkName)) {
				columns.add(fk.fkColumnName());
			}
		}
		return columns;
	}

	private void handleInverseSide(String joinTableName, TableDescriptor joinTable,
			List<RawForeignKeyInfo> outgoingFks,
			RawForeignKeyInfo owningFk, RawForeignKeyInfo inverseFk,
			TableDescriptor owningTable, TableDescriptor inverseTable) {
		String inverseFieldName = adapter.foreignKeyToManyToManyName(
			inverseFk, joinTable, outgoingFks, owningFk, true);
		List<String> inverseColumns = collectFkColumns(inverseFk.fkName(), outgoingFks);
		List<String> owningColumns = collectFkColumns(owningFk.fkName(), outgoingFks);
		ManyToManyDescriptor inverseM2m = new ManyToManyDescriptor(
			inverseFieldName,
			owningTable.getEntityClassName(),
			owningTable.getEntityPackage())
			.joinTable(joinTableName, inverseColumns, owningColumns)
			.joinTableSchema(joinTable.getSchema())
			.joinTableCatalog(joinTable.getCatalog());
		inverseTable.addManyToMany(inverseM2m);
	}

	private List<RawForeignKeyInfo> filterFirstColumnFks(List<RawForeignKeyInfo> outgoingFks) {
		List<RawForeignKeyInfo> firstColumnFks = new ArrayList<>();
		for (RawForeignKeyInfo fk : outgoingFks) {
			if (fk.keySeq() <= 1) {
				firstColumnFks.add(fk);
			}
		}
		return firstColumnFks;
	}

	private int columnPosition(TableDescriptor table, String columnName) {
		List<ColumnDescriptor> columns = table.getColumns();
		for (int i = 0; i < columns.size(); i++) {
			if (columns.get(i).getColumnName().equals(columnName)) {
				return i;
			}
		}
		return Integer.MAX_VALUE;
	}
}
