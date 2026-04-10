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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToOneMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

/**
 * Resolves outgoing foreign keys (ManyToOne / OneToOne on the owning side)
 * for each non-M2M table, adding {@link ForeignKeyMetadata} or
 * {@link OneToOneMetadata} to the appropriate {@link TableMetadata}.
 *
 * @author Koen Aers
 */
class OutgoingForeignKeyResolver {

	private final Map<String, TableMetadata> tablesByName;
	private final Map<String, List<RawForeignKeyInfo>> outgoingFksByTable;
	private final Set<String> manyToManyTables;
	private final RevengStrategyAdapter adapter;

	static OutgoingForeignKeyResolver create(
			Map<String, TableMetadata> tablesByName,
			Map<String, List<RawForeignKeyInfo>> outgoingFksByTable,
			Set<String> manyToManyTables,
			RevengStrategyAdapter adapter) {
		return new OutgoingForeignKeyResolver(tablesByName, outgoingFksByTable, manyToManyTables, adapter);
	}

	private OutgoingForeignKeyResolver(
			Map<String, TableMetadata> tablesByName,
			Map<String, List<RawForeignKeyInfo>> outgoingFksByTable,
			Set<String> manyToManyTables,
			RevengStrategyAdapter adapter) {
		this.tablesByName = tablesByName;
		this.outgoingFksByTable = outgoingFksByTable;
		this.manyToManyTables = manyToManyTables;
		this.adapter = adapter;
	}

	void resolveOutgoingForeignKeys() {
		for (Map.Entry<String, TableMetadata> entry : tablesByName.entrySet()) {
			String tableName = entry.getKey();
			if (manyToManyTables.contains(tableName)) {
				continue;
			}
			TableMetadata fkTable = entry.getValue();
			List<RawForeignKeyInfo> outgoingFks = outgoingFksByTable.getOrDefault(
				tableName, Collections.emptyList());

			// Group FK entries by FK name to handle composite FKs
			Map<String, List<RawForeignKeyInfo>> fksByName = groupByFkName(outgoingFks);

			for (Map.Entry<String, List<RawForeignKeyInfo>> fkEntry : fksByName.entrySet()) {
				List<RawForeignKeyInfo> fkColumns = fkEntry.getValue();
				// Find the primary column (keySeq=1); skip if missing
				RawForeignKeyInfo primaryColumn = null;
				for (RawForeignKeyInfo col : fkColumns) {
					if (col.keySeq() <= 1) {
						primaryColumn = col;
						break;
					}
				}
				if (primaryColumn == null) continue;
				handleOutgoingFK(primaryColumn, fkColumns, fkTable, outgoingFks);
			}
		}
	}

	/**
	 * Groups FK entries by FK name, preserving order within each group by keySeq.
	 */
	private static Map<String, List<RawForeignKeyInfo>> groupByFkName(
			List<RawForeignKeyInfo> fks) {
		Map<String, List<RawForeignKeyInfo>> result = new LinkedHashMap<>();
		for (RawForeignKeyInfo fk : fks) {
			result.computeIfAbsent(fk.fkName(), k -> new ArrayList<>()).add(fk);
		}
		return result;
	}

	private void handleOutgoingFK(RawForeignKeyInfo primaryColumn,
			List<RawForeignKeyInfo> allColumns,
			TableMetadata fkTable, List<RawForeignKeyInfo> outgoingFks) {
		if (adapter.excludeForeignKeyAsManytoOne(primaryColumn)) {
			return;
		}
		if (tablesByName.get(primaryColumn.referencedTableName()) == null) {
			return;
		}
		TableMetadata referencedTable = tablesByName.get(primaryColumn.referencedTableName());
		boolean uniqueReference = isUniqueReference(primaryColumn, outgoingFks);

		if (adapter.isOneToOne(allColumns, fkTable)) {
			handleOneToOne(primaryColumn, allColumns, fkTable, referencedTable, uniqueReference);
		} else {
			handleManyToOne(primaryColumn, fkTable, referencedTable, uniqueReference);
		}
	}

	private void handleOneToOne(RawForeignKeyInfo primaryColumn,
			List<RawForeignKeyInfo> allColumns,
			TableMetadata fkTable,
			TableMetadata referencedTable, boolean uniqueReference) {
		boolean isConstrained = fkTable.getColumns().stream()
			.anyMatch(col -> col.getColumnName().equals(primaryColumn.fkColumnName()) && col.isPrimaryKey());
		OneToOneMetadata o2o = new OneToOneMetadata(
			adapter.foreignKeyToEntityName(primaryColumn, uniqueReference),
			referencedTable.getEntityClassName(),
			referencedTable.getEntityPackage())
			.constrained(isConstrained);
		// Add all join columns (supports composite FKs)
		for (RawForeignKeyInfo col : allColumns) {
			o2o.addJoinColumn(col.fkColumnName(), col.pkColumnName());
		}
		fkTable.addOneToOne(o2o);
	}

	private void handleManyToOne(RawForeignKeyInfo fkInfo, TableMetadata fkTable,
			TableMetadata referencedTable, boolean uniqueReference) {
		ForeignKeyMetadata fkMetadata = new ForeignKeyMetadata(
			adapter.foreignKeyToEntityName(fkInfo, uniqueReference),
			fkInfo.fkColumnName(),
			referencedTable.getEntityClassName(),
			referencedTable.getEntityPackage());
		fkTable.addForeignKey(fkMetadata);
	}

	static boolean isUniqueReference(
			RawForeignKeyInfo fkInfo,
			List<RawForeignKeyInfo> allOutgoingFks) {
		int count = 0;
		for (RawForeignKeyInfo other : allOutgoingFks) {
			if (other.referencedTableName().equals(fkInfo.referencedTableName())
				&& other.keySeq() <= 1) {
				count++;
			}
		}
		return count <= 1;
	}
}
