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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToOneMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

/**
 * Resolves incoming foreign keys (OneToMany / OneToOne on the inverse side)
 * for each non-M2M table, adding {@link OneToManyMetadata} or
 * {@link OneToOneMetadata} to the appropriate {@link TableMetadata}.
 *
 * @author Koen Aers
 */
class IncomingForeignKeyResolver {

	private final Map<String, TableMetadata> tablesByName;
	private final Map<String, List<RawForeignKeyInfo>> incomingFksByTable;
	private final Set<String> manyToManyTables;
	private final RevengStrategyAdapter adapter;

	static IncomingForeignKeyResolver create(
			Map<String, TableMetadata> tablesByName,
			Map<String, List<RawForeignKeyInfo>> incomingFksByTable,
			Set<String> manyToManyTables,
			RevengStrategyAdapter adapter) {
		return new IncomingForeignKeyResolver(tablesByName, incomingFksByTable, manyToManyTables, adapter);
	}

	private IncomingForeignKeyResolver(
			Map<String, TableMetadata> tablesByName,
			Map<String, List<RawForeignKeyInfo>> incomingFksByTable,
			Set<String> manyToManyTables,
			RevengStrategyAdapter adapter) {
		this.tablesByName = tablesByName;
		this.incomingFksByTable = incomingFksByTable;
		this.manyToManyTables = manyToManyTables;
		this.adapter = adapter;
	}

	void resolveIncomingForeignKeys() {
		for (Map.Entry<String, TableMetadata> entry : tablesByName.entrySet()) {
			String tableName = entry.getKey();
			if (manyToManyTables.contains(tableName)) {
				continue;
			}
			TableMetadata referencedTable = entry.getValue();
			List<RawForeignKeyInfo> incomingFks = incomingFksByTable.getOrDefault(
				tableName, java.util.Collections.emptyList());

			for (RawForeignKeyInfo fkInfo : incomingFks) {
				handleIncomingFK(fkInfo, referencedTable);
			}
		}
	}

	private void handleIncomingFK(RawForeignKeyInfo fkInfo, TableMetadata referencedTable) {
		if (handlingIsNeeded(fkInfo)) {
			TableMetadata fkTable = tablesByName.get(fkInfo.fkTableName());
			List<RawForeignKeyInfo> allFks = collectAllRawFks(incomingFksByTable);
			List<RawForeignKeyInfo> outgoingFksForFkTable = groupByFkTable(allFks)
				.getOrDefault(fkInfo.fkTableName(), java.util.Collections.emptyList());
			boolean uniqueReference = OutgoingForeignKeyResolver.isUniqueReference(
				fkInfo, outgoingFksForFkTable);

			if (adapter.isOneToOne(fkInfo, fkTable)) {
				handleOneToOne(fkInfo, fkTable, referencedTable, uniqueReference);
			} else {
				handleOneToMany(fkInfo, fkTable, referencedTable, uniqueReference);
			}
		}
	}

	private void handleOneToMany(RawForeignKeyInfo fkInfo, TableMetadata fkTable,
			TableMetadata referencedTable, boolean uniqueReference) {
		String fieldName = adapter.foreignKeyToCollectionName(fkInfo, uniqueReference);
		String mappedBy = findManyToOneFieldName(fkTable, fkInfo);
		OneToManyMetadata o2m = new OneToManyMetadata(
			fieldName,
			mappedBy != null ? mappedBy : fieldName,
			fkTable.getEntityClassName(),
			fkTable.getEntityPackage());
		referencedTable.addOneToMany(o2m);
	}

	private void handleOneToOne(RawForeignKeyInfo fkInfo, TableMetadata fkTable,
			TableMetadata referencedTable, boolean uniqueReference) {
		String fieldName = adapter.foreignKeyToInverseEntityName(fkInfo, uniqueReference);
		OneToOneMetadata o2o = new OneToOneMetadata(
			fieldName,
			fkTable.getEntityClassName(),
			fkTable.getEntityPackage())
			.mappedBy(findManyToOneFieldName(fkTable, fkInfo));
		referencedTable.addOneToOne(o2o);
	}

	private boolean handlingIsNeeded(RawForeignKeyInfo fkInfo) {
		if (fkInfo.keySeq() > 1) {
			return false;
		}
		if (manyToManyTables.contains(fkInfo.fkTableName())) {
			return false;
		}
		if (adapter.excludeForeignKeyAsCollection(fkInfo)) {
			return false;
		}
		if (tablesByName.get(fkInfo.fkTableName()) == null) {
			return false;
		}
		return true;
	}

	private String findManyToOneFieldName(TableMetadata fkTable, RawForeignKeyInfo fkInfo) {
		for (ForeignKeyMetadata fk : fkTable.getForeignKeys()) {
			if (fk.getForeignKeyColumnName().equals(fkInfo.fkColumnName())) {
				return fk.getFieldName();
			}
		}
		for (OneToOneMetadata o2o : fkTable.getOneToOnes()) {
			if (fkInfo.fkColumnName().equals(o2o.getForeignKeyColumnName())) {
				return o2o.getFieldName();
			}
		}
		return null;
	}

	private Map<String, List<RawForeignKeyInfo>> groupByFkTable(List<RawForeignKeyInfo> fks) {
		Map<String, List<RawForeignKeyInfo>> result = new HashMap<>();
		for (RawForeignKeyInfo fk : fks) {
			result.computeIfAbsent(fk.fkTableName(), k -> new ArrayList<>()).add(fk);
		}
		return result;
	}

	private List<RawForeignKeyInfo> collectAllRawFks(
			Map<String, List<RawForeignKeyInfo>> incomingFksByTable) {
		List<RawForeignKeyInfo> all = new ArrayList<>();
		for (List<RawForeignKeyInfo> fks : incomingFksByTable.values()) {
			all.addAll(fks);
		}
		return all;
	}
}
