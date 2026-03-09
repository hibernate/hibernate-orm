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

import org.hibernate.tool.api.reveng.RevengDialect;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

/**
 * Reads a database schema via {@link RevengDialect} and produces
 * a list of {@link TableMetadata} objects. Uses {@link RevengStrategyAdapter}
 * to call {@link RevengStrategy} methods that require
 * {@code org.hibernate.mapping.*} types.
 * <p>
 * This class lives alongside the existing {@code DatabaseReader} pipeline
 * and does not modify or replace any existing code.
 *
 * @author Koen Aers
 */
public class ModelsDatabaseSchemaReader {

	private final RevengDialect dialect;
	private final RevengStrategy strategy;
	private final String defaultCatalog;
	private final String defaultSchema;

	public static ModelsDatabaseSchemaReader create(
			RevengDialect dialect,
			RevengStrategy strategy,
			String defaultCatalog,
			String defaultSchema) {
		return new ModelsDatabaseSchemaReader(dialect, strategy, defaultCatalog, defaultSchema);
	}

	private ModelsDatabaseSchemaReader(
			RevengDialect dialect,
			RevengStrategy strategy,
			String defaultCatalog,
			String defaultSchema) {
		this.dialect = dialect;
		this.strategy = strategy;
		this.defaultCatalog = defaultCatalog;
		this.defaultSchema = defaultSchema;
	}

	/**
	 * Reads the database schema and returns fully populated table metadata.
	 *
	 * @return list of {@link TableMetadata} objects
	 */
	public List<TableMetadata> readSchema() {
		// Read JDBC data into metadata objects
		Map<String, TableMetadata> tablesByName = TableReader
				.create(dialect, strategy, defaultCatalog, defaultSchema)
				.readTables();
		List<RawForeignKeyInfo> allFks = ForeignKeyReader
				.create(dialect, defaultCatalog, defaultSchema)
				.readForeignKeys(tablesByName);

		// Merge user-defined foreign keys
		List<RawForeignKeyInfo> userFks = UserDefinedForeignKeyReader
				.create(strategy, defaultCatalog, defaultSchema)
				.readUserForeignKeys(tablesByName);
		allFks.addAll(userFks);

		// Build FK indexes
		Map<String, List<RawForeignKeyInfo>> outgoingFksByTable = groupByFkTable(allFks);
		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = groupByReferencedTable(allFks);

		// Apply adapter to classify and resolve relationships
		Set<String> manyToManyTables = applyAdapterToClassifyAndResolveRelationships(
				tablesByName, outgoingFksByTable, incomingFksByTable);

		// Return result (excluding M2M tables)
		List<TableMetadata> result = new ArrayList<>();
		for (Map.Entry<String, TableMetadata> entry : tablesByName.entrySet()) {
			if (!manyToManyTables.contains(entry.getKey())) {
				result.add(entry.getValue());
			}
		}
		return result;
	}

	private Set<String> applyAdapterToClassifyAndResolveRelationships(
			Map<String, TableMetadata> tablesByName,
			Map<String, List<RawForeignKeyInfo>> outgoingFksByTable,
			Map<String, List<RawForeignKeyInfo>> incomingFksByTable) {
		RevengStrategyAdapter adapter = RevengStrategyAdapter.create(strategy);

		ManyToManyResolver manyToManyResolver = ManyToManyResolver
				.create(tablesByName, outgoingFksByTable, adapter);
		Set<String> manyToManyTables = manyToManyResolver.filterManyToManyTables();

		OutgoingForeignKeyResolver
				.create(tablesByName, outgoingFksByTable, manyToManyTables, adapter)
				.resolveOutgoingForeignKeys();

		IncomingForeignKeyResolver
				.create(tablesByName, incomingFksByTable, manyToManyTables, adapter)
				.resolveIncomingForeignKeys();

		manyToManyResolver.resolveManyToManyRelationships(manyToManyTables);

		return manyToManyTables;
	}

	// ---- Helper methods ----

	private Map<String, List<RawForeignKeyInfo>> groupByFkTable(List<RawForeignKeyInfo> fks) {
		Map<String, List<RawForeignKeyInfo>> result = new HashMap<>();
		for (RawForeignKeyInfo fk : fks) {
			result.computeIfAbsent(fk.fkTableName(), k -> new ArrayList<>()).add(fk);
		}
		return result;
	}

	private Map<String, List<RawForeignKeyInfo>> groupByReferencedTable(List<RawForeignKeyInfo> fks) {
		Map<String, List<RawForeignKeyInfo>> result = new HashMap<>();
		for (RawForeignKeyInfo fk : fks) {
			result.computeIfAbsent(fk.referencedTableName(), k -> new ArrayList<>()).add(fk);
		}
		return result;
	}

}
