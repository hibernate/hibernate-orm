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
				tableName, java.util.Collections.emptyList());

			for (RawForeignKeyInfo fkInfo : outgoingFks) {
				handleOutgoingFK(fkInfo, fkTable, outgoingFks);
			}
		}
	}

	private void handleOutgoingFK(RawForeignKeyInfo fkInfo, TableMetadata fkTable,
			List<RawForeignKeyInfo> outgoingFks) {
		if (handlingIsNeeded(fkInfo)) {
			TableMetadata referencedTable = tablesByName.get(fkInfo.referencedTableName());
			boolean uniqueReference = isUniqueReference(fkInfo, outgoingFks);

			if (adapter.isOneToOne(fkInfo, fkTable)) {
				handleOneToOne(fkInfo, fkTable, referencedTable, uniqueReference);
			} else {
				handleManyToOne(fkInfo, fkTable, referencedTable, uniqueReference);
			}
		}
	}

	private void handleOneToOne(RawForeignKeyInfo fkInfo, TableMetadata fkTable,
			TableMetadata referencedTable, boolean uniqueReference) {
		OneToOneMetadata o2o = new OneToOneMetadata(
			adapter.foreignKeyToEntityName(fkInfo, uniqueReference),
			referencedTable.getEntityClassName(),
			referencedTable.getEntityPackage())
			.foreignKeyColumnName(fkInfo.fkColumnName());
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

	private boolean handlingIsNeeded(RawForeignKeyInfo fkInfo) {
		if (fkInfo.keySeq() > 1) {
			return false;
		}
		if (adapter.excludeForeignKeyAsManytoOne(fkInfo)) {
			return false;
		}
		if (tablesByName.get(fkInfo.referencedTableName()) == null) {
			return false;
		}
		return true;
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
