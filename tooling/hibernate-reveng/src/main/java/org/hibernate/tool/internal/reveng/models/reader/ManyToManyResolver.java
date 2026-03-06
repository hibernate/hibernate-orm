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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ManyToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

/**
 * Resolves many-to-many relationships by examining join tables and
 * adding {@link ManyToManyMetadata} to both sides of the relationship.
 *
 * @author Koen Aers
 */
class ManyToManyResolver {

	private final Map<String, TableMetadata> tablesByName;
	private final Map<String, List<RawForeignKeyInfo>> outgoingFksByTable;
	private final RevengStrategyAdapter adapter;

	static ManyToManyResolver create(
			Map<String, TableMetadata> tablesByName,
			Map<String, List<RawForeignKeyInfo>> outgoingFksByTable,
			RevengStrategyAdapter adapter) {
		return new ManyToManyResolver(tablesByName, outgoingFksByTable, adapter);
	}

	private ManyToManyResolver(
			Map<String, TableMetadata> tablesByName,
			Map<String, List<RawForeignKeyInfo>> outgoingFksByTable,
			RevengStrategyAdapter adapter) {
		this.tablesByName = tablesByName;
		this.outgoingFksByTable = outgoingFksByTable;
		this.adapter = adapter;
	}

	Set<String> filterManyToManyTables() {
		Set<String> manyToManyTables = new HashSet<>();
		for (Map.Entry<String, TableMetadata> entry : tablesByName.entrySet()) {
			String tableName = entry.getKey();
			TableMetadata table = entry.getValue();
			if (table.getColumns().isEmpty()) {
				manyToManyTables.add(tableName);
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
		TableMetadata joinTable = tablesByName.get(joinTableName);
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

		TableMetadata table1 = tablesByName.get(fk1.referencedTableName());
		TableMetadata table2 = tablesByName.get(fk2.referencedTableName());
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
		TableMetadata owningTable;
		TableMetadata inverseTable;

		if (fk1First) {
			// fk1's column is first → fk1's referenced table is the inverse side
			owningFk = fk2;
			inverseFk = fk1;
			owningTable = table2;
			inverseTable = table1;
		} else {
			owningFk = fk1;
			inverseFk = fk2;
			owningTable = table1;
			inverseTable = table2;
		}

		handleOwningSide(joinTableName, joinTable, outgoingFks,
			owningFk, inverseFk, owningTable, inverseTable);
		handleInverseSide(joinTable, outgoingFks,
			owningFk, inverseFk, owningTable, inverseTable);
	}

	private void handleOwningSide(String joinTableName, TableMetadata joinTable,
			List<RawForeignKeyInfo> outgoingFks,
			RawForeignKeyInfo owningFk, RawForeignKeyInfo inverseFk,
			TableMetadata owningTable, TableMetadata inverseTable) {
		String owningFieldName = adapter.foreignKeyToManyToManyName(
			inverseFk, joinTable, outgoingFks, owningFk, true);
		ManyToManyMetadata owningM2m = new ManyToManyMetadata(
			owningFieldName,
			inverseTable.getEntityClassName(),
			inverseTable.getEntityPackage())
			.joinTable(joinTableName,
				owningFk.fkColumnName(),
				inverseFk.fkColumnName());
		owningTable.addManyToMany(owningM2m);
	}

	private void handleInverseSide(TableMetadata joinTable,
			List<RawForeignKeyInfo> outgoingFks,
			RawForeignKeyInfo owningFk, RawForeignKeyInfo inverseFk,
			TableMetadata owningTable, TableMetadata inverseTable) {
		String owningFieldName = adapter.foreignKeyToManyToManyName(
			inverseFk, joinTable, outgoingFks, owningFk, true);
		String inverseFieldName = adapter.foreignKeyToManyToManyName(
			owningFk, joinTable, outgoingFks, inverseFk, true);
		ManyToManyMetadata inverseM2m = new ManyToManyMetadata(
			inverseFieldName,
			owningTable.getEntityClassName(),
			owningTable.getEntityPackage())
			.mappedBy(owningFieldName);
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

	private int columnPosition(TableMetadata table, String columnName) {
		List<ColumnMetadata> columns = table.getColumns();
		for (int i = 0; i < columns.size(); i++) {
			if (columns.get(i).getColumnName().equals(columnName)) {
				return i;
			}
		}
		return Integer.MAX_VALUE;
	}
}
