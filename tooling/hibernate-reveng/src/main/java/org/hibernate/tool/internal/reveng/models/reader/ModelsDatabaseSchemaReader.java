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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.tool.api.reveng.RevengDialect;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.RevengStrategy.SchemaSelection;
import org.hibernate.tool.api.reveng.TableIdentifier;
import jakarta.persistence.TemporalType;

import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.CompositeIdMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ManyToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToOneMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.hibernate.tool.internal.util.JdbcToHibernateTypeHelper;

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

	public ModelsDatabaseSchemaReader(
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
		// Step 1: Read JDBC data into metadata objects
		Map<String, TableMetadata> tablesByName = discoverTables();
		Map<String, TableMetadata> tablesByEntityName = buildEntityNameIndex(tablesByName);
		List<RawForeignKeyInfo> allFks = collectAllForeignKeys(tablesByName);

		// Build FK indexes
		Map<String, List<RawForeignKeyInfo>> outgoingFksByTable = groupByFkTable(allFks);
		Map<String, List<RawForeignKeyInfo>> incomingFksByTable = groupByReferencedTable(allFks);

		// Steps 2-3: Apply adapter to classify and resolve relationships
		RevengStrategyAdapter adapter = new RevengStrategyAdapter(strategy);

		Set<String> manyToManyTables = filterManyToManyTables(
			tablesByName, outgoingFksByTable, adapter);

		resolveOutgoingForeignKeys(
			tablesByName, tablesByEntityName, outgoingFksByTable, manyToManyTables, adapter);

		resolveIncomingForeignKeys(
			tablesByName, tablesByEntityName, incomingFksByTable, manyToManyTables, adapter);

		resolveManyToManyRelationships(
			tablesByName, outgoingFksByTable, manyToManyTables, adapter);

		// Step 4: Return result (excluding M2M tables)
		List<TableMetadata> result = new ArrayList<>();
		for (Map.Entry<String, TableMetadata> entry : tablesByName.entrySet()) {
			if (!manyToManyTables.contains(entry.getKey())) {
				result.add(entry.getValue());
			}
		}
		return result;
	}

	// ---- Step 1: Read JDBC data ----

	private Map<String, TableMetadata> discoverTables() {
		Map<String, TableMetadata> tablesByName = new LinkedHashMap<>();
		for (SchemaSelection selection : getSchemaSelections()) {
			Iterator<Map<String, Object>> tableIterator = dialect.getTables(
				selection.getMatchCatalog(),
				selection.getMatchSchema(),
				selection.getMatchTable());
			try {
				while (tableIterator.hasNext()) {
					Map<String, Object> tableRow = tableIterator.next();
					String tableName = (String) tableRow.get("TABLE_NAME");
					String catalog = (String) tableRow.get("TABLE_CAT");
					String schema = (String) tableRow.get("TABLE_SCHEM");

					TableIdentifier tableId = TableIdentifier.create(catalog, schema, tableName);
					if (strategy.excludeTable(tableId)) {
						continue;
					}

					String fullClassName = strategy.tableToClassName(tableId);
					String entityPackage = StringHelper.qualifier(fullClassName);
					String entityClassName = StringHelper.unqualify(fullClassName);

					TableMetadata tableMetadata = new TableMetadata(tableName, entityClassName, entityPackage);
					if (schema != null && !schema.equals(defaultSchema)) {
						tableMetadata.setSchema(schema);
					}
					if (catalog != null && !catalog.equals(defaultCatalog)) {
						tableMetadata.setCatalog(catalog);
					}

					readColumnsAndPrimaryKeys(tableMetadata, tableId, catalog, schema);
					detectCompositeId(tableMetadata, tableId);
					tablesByName.put(tableName, tableMetadata);
				}
			} finally {
				dialect.close(tableIterator);
			}
		}
		return tablesByName;
	}

	private void readColumnsAndPrimaryKeys(
			TableMetadata tableMetadata,
			TableIdentifier tableId,
			String catalog,
			String schema) {
		Set<String> pkColumns = readPrimaryKeyColumns(catalog, schema, tableMetadata.getTableName(), tableId);

		Iterator<Map<String, Object>> columnIterator = dialect.getColumns(
			catalog, schema, tableMetadata.getTableName(), null);
		try {
			while (columnIterator.hasNext()) {
				Map<String, Object> colRow = columnIterator.next();
				String columnName = (String) colRow.get("COLUMN_NAME");

				if (strategy.excludeColumn(tableId, columnName)) {
					continue;
				}

				int sqlType = (Integer) colRow.get("DATA_TYPE");
				int columnSize = colRow.get("COLUMN_SIZE") != null
					? (Integer) colRow.get("COLUMN_SIZE") : 0;
				int decimalDigits = colRow.get("DECIMAL_DIGITS") != null
					? (Integer) colRow.get("DECIMAL_DIGITS") : 0;
				int nullable = colRow.get("NULLABLE") != null
					? (Integer) colRow.get("NULLABLE") : 1;
				boolean isNullable = nullable != java.sql.DatabaseMetaData.columnNoNulls;
				boolean isPk = pkColumns.contains(columnName);

				String hibernateType = strategy.columnToHibernateTypeName(
					tableId, columnName, sqlType, columnSize, decimalDigits, 0, isNullable, isPk);
				if (hibernateType == null) {
					hibernateType = JdbcToHibernateTypeHelper.getPreferredHibernateType(
						sqlType, columnSize, decimalDigits, 0, isNullable, isPk);
				}

				Class<?> javaClass = HibernateTypeToJavaClass.toJavaClass(hibernateType);
				String fieldName = strategy.columnToPropertyName(tableId, columnName);

				ColumnMetadata columnMetadata = new ColumnMetadata(columnName, fieldName, javaClass)
					.nullable(isNullable)
					.length(columnSize)
					.precision(columnSize)
					.scale(decimalDigits);

				if (isPk) {
					columnMetadata.primaryKey(true);
				}

				if (isVersionColumn(tableId, columnName)) {
					columnMetadata.version(true);
				}

				TemporalType temporalType = HibernateTypeToJavaClass.toTemporalType(hibernateType);
				if (temporalType != null) {
					columnMetadata.temporal(temporalType);
				}

				if (HibernateTypeToJavaClass.isLob(hibernateType)) {
					columnMetadata.lob(true);
				}

				tableMetadata.addColumn(columnMetadata);
			}
		} finally {
			dialect.close(columnIterator);
		}
	}

	private Set<String> readPrimaryKeyColumns(
			String catalog, String schema, String tableName, TableIdentifier tableId) {
		Set<String> pkColumns = new LinkedHashMap<String, Void>() {}.keySet();
		// Use a list to preserve KEY_SEQ order
		List<String> pkList = new ArrayList<>();

		Iterator<Map<String, Object>> pkIterator = dialect.getPrimaryKeys(catalog, schema, tableName);
		try {
			while (pkIterator.hasNext()) {
				Map<String, Object> pkRow = pkIterator.next();
				pkList.add((String) pkRow.get("COLUMN_NAME"));
			}
		} finally {
			dialect.close(pkIterator);
		}

		if (pkList.isEmpty()) {
			List<String> strategyPks = strategy.getPrimaryKeyColumnNames(tableId);
			if (strategyPks != null) {
				pkList.addAll(strategyPks);
			}
		}

        return new HashSet<>(pkList);
	}

	private boolean isVersionColumn(TableIdentifier tableId, String columnName) {
		String optimisticLockColumn = strategy.getOptimisticLockColumnName(tableId);
		if (optimisticLockColumn != null) {
			return columnName.equals(optimisticLockColumn);
		}
		return strategy.useColumnForOptimisticLock(tableId, columnName);
	}

	private void detectCompositeId(TableMetadata tableMetadata, TableIdentifier tableId) {
		List<ColumnMetadata> pkColumns = new ArrayList<>();
		for (ColumnMetadata col : tableMetadata.getColumns()) {
			if (col.isPrimaryKey()) {
				pkColumns.add(col);
			}
		}
		if (pkColumns.size() <= 1) {
			return;
		}

		String idClassName = strategy.tableToCompositeIdName(tableId);
		if (idClassName == null) {
			idClassName = strategy.classNameToCompositeIdName(
				tableMetadata.getEntityClassName());
		}

		CompositeIdMetadata compositeId = new CompositeIdMetadata(
			"id", idClassName, tableMetadata.getEntityPackage());
		for (ColumnMetadata pkCol : pkColumns) {
			compositeId.addAttributeOverride(pkCol.getFieldName(), pkCol.getColumnName());
		}
		tableMetadata.compositeId(compositeId);
	}

	private List<RawForeignKeyInfo> collectAllForeignKeys(Map<String, TableMetadata> tablesByName) {
		List<RawForeignKeyInfo> allFks = new ArrayList<>();
		for (Map.Entry<String, TableMetadata> entry : tablesByName.entrySet()) {
			TableMetadata table = entry.getValue();
			String catalog = table.getCatalog() != null ? table.getCatalog() : defaultCatalog;
			String schema = table.getSchema() != null ? table.getSchema() : defaultSchema;

			Iterator<Map<String, Object>> fkIterator = dialect.getExportedKeys(
				catalog, schema, table.getTableName());
			try {
				while (fkIterator.hasNext()) {
					Map<String, Object> fkRow = fkIterator.next();
					String fkName = (String) fkRow.get("FK_NAME");
					String fkTableName = (String) fkRow.get("FKTABLE_NAME");
					String fkTableCatalog = (String) fkRow.get("FKTABLE_CAT");
					String fkTableSchema = (String) fkRow.get("FKTABLE_SCHEM");
					String fkColumnName = (String) fkRow.get("FKCOLUMN_NAME");
					String pkColumnName = (String) fkRow.get("PKCOLUMN_NAME");
					int keySeq = fkRow.get("KEY_SEQ") != null
						? ((Number) fkRow.get("KEY_SEQ")).intValue() : 1;

					allFks.add(new RawForeignKeyInfo(
						fkName,
						fkTableName, fkTableCatalog, fkTableSchema,
						fkColumnName, pkColumnName,
						table.getTableName(), catalog, schema,
						keySeq));
				}
			} finally {
				dialect.close(fkIterator);
			}
		}
		return allFks;
	}

	// ---- Steps 2-3: Apply adapter ----

	private Set<String> filterManyToManyTables(
			Map<String, TableMetadata> tablesByName,
			Map<String, List<RawForeignKeyInfo>> outgoingFksByTable,
			RevengStrategyAdapter adapter) {
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

	private void resolveOutgoingForeignKeys(
			Map<String, TableMetadata> tablesByName,
			Map<String, TableMetadata> tablesByEntityName,
			Map<String, List<RawForeignKeyInfo>> outgoingFksByTable,
			Set<String> manyToManyTables,
			RevengStrategyAdapter adapter) {
		for (Map.Entry<String, TableMetadata> entry : tablesByName.entrySet()) {
			String tableName = entry.getKey();
			if (manyToManyTables.contains(tableName)) {
				continue;
			}
			TableMetadata fkTable = entry.getValue();
			List<RawForeignKeyInfo> outgoingFks = outgoingFksByTable.getOrDefault(
				tableName, java.util.Collections.emptyList());

			for (RawForeignKeyInfo fkInfo : outgoingFks) {
				if (fkInfo.keySeq() > 1) {
					continue; // Skip composite FK columns beyond the first
				}
				if (adapter.excludeForeignKeyAsManytoOne(fkInfo)) {
					continue;
				}

				TableMetadata referencedTable = tablesByName.get(fkInfo.referencedTableName());
				if (referencedTable == null) {
					continue;
				}

				boolean uniqueReference = isUniqueReference(
					fkInfo, outgoingFks, tablesByName);
				String fullTargetClassName = referencedTable.getEntityPackage()
					+ "." + referencedTable.getEntityClassName();

				if (adapter.isOneToOne(fkInfo, fkTable)) {
					OneToOneMetadata o2o = new OneToOneMetadata(
						adapter.foreignKeyToEntityName(fkInfo, uniqueReference),
						referencedTable.getEntityClassName(),
						referencedTable.getEntityPackage())
						.foreignKeyColumnName(fkInfo.fkColumnName());
					fkTable.addOneToOne(o2o);
				} else {
					ForeignKeyMetadata fkMetadata = new ForeignKeyMetadata(
						adapter.foreignKeyToEntityName(fkInfo, uniqueReference),
						fkInfo.fkColumnName(),
						referencedTable.getEntityClassName(),
						referencedTable.getEntityPackage());
					fkTable.addForeignKey(fkMetadata);
				}
			}
		}
	}

	private void resolveIncomingForeignKeys(
			Map<String, TableMetadata> tablesByName,
			Map<String, TableMetadata> tablesByEntityName,
			Map<String, List<RawForeignKeyInfo>> incomingFksByTable,
			Set<String> manyToManyTables,
			RevengStrategyAdapter adapter) {
		for (Map.Entry<String, TableMetadata> entry : tablesByName.entrySet()) {
			String tableName = entry.getKey();
			if (manyToManyTables.contains(tableName)) {
				continue;
			}
			TableMetadata referencedTable = entry.getValue();
			List<RawForeignKeyInfo> incomingFks = incomingFksByTable.getOrDefault(
				tableName, java.util.Collections.emptyList());

			for (RawForeignKeyInfo fkInfo : incomingFks) {
				if (fkInfo.keySeq() > 1) {
					continue;
				}
				if (manyToManyTables.contains(fkInfo.fkTableName())) {
					continue;
				}
				if (adapter.excludeForeignKeyAsCollection(fkInfo)) {
					continue;
				}

				TableMetadata fkTable = tablesByName.get(fkInfo.fkTableName());
				if (fkTable == null) {
					continue;
				}

				Map<String, List<RawForeignKeyInfo>> outgoingFksForFkTable =
					groupByReferencedTable(getOutgoingFksForTable(
						fkInfo.fkTableName(), tablesByName, incomingFksByTable));
				boolean uniqueReference = isUniqueReference(
					fkInfo, getOutgoingFksForTable(
						fkInfo.fkTableName(), tablesByName,
						groupByFkTable(collectAllRawFks(tablesByName, incomingFksByTable))),
					tablesByName);

				if (adapter.isOneToOne(fkInfo, fkTable)) {
					String fieldName = adapter.foreignKeyToInverseEntityName(fkInfo, uniqueReference);
					OneToOneMetadata o2o = new OneToOneMetadata(
						fieldName,
						fkTable.getEntityClassName(),
						fkTable.getEntityPackage())
						.mappedBy(findManyToOneFieldName(fkTable, fkInfo));
					referencedTable.addOneToOne(o2o);
				} else {
					String fieldName = adapter.foreignKeyToCollectionName(fkInfo, uniqueReference);
					String mappedBy = findManyToOneFieldName(fkTable, fkInfo);
					OneToManyMetadata o2m = new OneToManyMetadata(
						fieldName,
						mappedBy != null ? mappedBy : fieldName,
						fkTable.getEntityClassName(),
						fkTable.getEntityPackage());
					referencedTable.addOneToMany(o2m);
				}
			}
		}
	}

	private void resolveManyToManyRelationships(
			Map<String, TableMetadata> tablesByName,
			Map<String, List<RawForeignKeyInfo>> outgoingFksByTable,
			Set<String> manyToManyTables,
			RevengStrategyAdapter adapter) {
		for (String joinTableName : manyToManyTables) {
			TableMetadata joinTable = tablesByName.get(joinTableName);
			if (joinTable == null) {
				continue;
			}
			List<RawForeignKeyInfo> outgoingFks = outgoingFksByTable.getOrDefault(
				joinTableName, java.util.Collections.emptyList());

			// Filter to first-column FKs only (keySeq == 1)
			List<RawForeignKeyInfo> firstColumnFks = new ArrayList<>();
			for (RawForeignKeyInfo fk : outgoingFks) {
				if (fk.keySeq() <= 1) {
					firstColumnFks.add(fk);
				}
			}

			if (firstColumnFks.size() != 2) {
				continue;
			}

			RawForeignKeyInfo fk1 = firstColumnFks.get(0);
			RawForeignKeyInfo fk2 = firstColumnFks.get(1);

			TableMetadata table1 = tablesByName.get(fk1.referencedTableName());
			TableMetadata table2 = tablesByName.get(fk2.referencedTableName());
			if (table1 == null || table2 == null) {
				continue;
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

			// Owning side: has @JoinTable
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

			// Inverse side: has @MappedBy
			String inverseFieldName = adapter.foreignKeyToManyToManyName(
				owningFk, joinTable, outgoingFks, inverseFk, true);
			ManyToManyMetadata inverseM2m = new ManyToManyMetadata(
				inverseFieldName,
				owningTable.getEntityClassName(),
				owningTable.getEntityPackage())
				.mappedBy(owningFieldName);
			inverseTable.addManyToMany(inverseM2m);
		}
	}

	// ---- Helper methods ----

	private List<SchemaSelection> getSchemaSelections() {
		List<SchemaSelection> result = strategy.getSchemaSelections();
		if (result == null) {
			result = new ArrayList<>();
			result.add(new SchemaSelection() {
				@Override
				public String getMatchCatalog() { return defaultCatalog; }
				@Override
				public String getMatchSchema() { return defaultSchema; }
				@Override
				public String getMatchTable() { return null; }
			});
		}
		return result;
	}

	private Map<String, TableMetadata> buildEntityNameIndex(Map<String, TableMetadata> tablesByName) {
		Map<String, TableMetadata> index = new HashMap<>();
		for (TableMetadata table : tablesByName.values()) {
			String entityName = table.getEntityPackage() + "." + table.getEntityClassName();
			index.put(entityName, table);
		}
		return index;
	}

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

	private boolean isUniqueReference(
			RawForeignKeyInfo fkInfo,
			List<RawForeignKeyInfo> allOutgoingFks,
			Map<String, TableMetadata> tablesByName) {
		int count = 0;
		for (RawForeignKeyInfo other : allOutgoingFks) {
			if (other.referencedTableName().equals(fkInfo.referencedTableName())
				&& other.keySeq() <= 1) {
				count++;
			}
		}
		return count <= 1;
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

	private List<RawForeignKeyInfo> getOutgoingFksForTable(
			String tableName,
			Map<String, TableMetadata> tablesByName,
			Map<String, List<RawForeignKeyInfo>> fksByTable) {
		return fksByTable.getOrDefault(tableName, java.util.Collections.emptyList());
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

	private List<RawForeignKeyInfo> collectAllRawFks(
			Map<String, TableMetadata> tablesByName,
			Map<String, List<RawForeignKeyInfo>> incomingFksByTable) {
		List<RawForeignKeyInfo> all = new ArrayList<>();
		for (List<RawForeignKeyInfo> fks : incomingFksByTable.values()) {
			all.addAll(fks);
		}
		return all;
	}
}
