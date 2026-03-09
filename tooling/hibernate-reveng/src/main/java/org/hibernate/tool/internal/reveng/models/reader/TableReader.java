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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.GenerationType;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.tool.api.reveng.RevengDialect;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.RevengStrategy.SchemaSelection;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.CompositeIdMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

/**
 * Discovers database tables via {@link RevengDialect} and populates
 * {@link TableMetadata} objects with columns, primary keys, and
 * composite ID metadata. Uses {@link RevengStrategy} for entity
 * naming, table exclusions, and composite ID class names.
 *
 * @author Koen Aers
 */
class TableReader {

	private final RevengDialect dialect;
	private final RevengStrategy strategy;
	private final String defaultCatalog;
	private final String defaultSchema;

	static TableReader create(RevengDialect dialect, RevengStrategy strategy,
			String defaultCatalog, String defaultSchema) {
		return new TableReader(dialect, strategy, defaultCatalog, defaultSchema);
	}

	private TableReader(RevengDialect dialect, RevengStrategy strategy,
			String defaultCatalog, String defaultSchema) {
		this.dialect = dialect;
		this.strategy = strategy;
		this.defaultCatalog = defaultCatalog;
		this.defaultSchema = defaultSchema;
	}

	/**
	 * Discovers all tables and returns them keyed by table name,
	 * with columns, primary keys, and composite ID metadata populated.
	 */
	Map<String, TableMetadata> readTables() {
		Map<String, TableMetadata> tablesByName = new LinkedHashMap<>();
		for (SchemaSelection selection : getSchemaSelections()) {
			readTablesForSchemaSelection(selection, tablesByName);
		}
		return tablesByName;
	}

	private void readTablesForSchemaSelection(
			SchemaSelection selection, Map<String, TableMetadata> tablesByName) {
		Iterator<Map<String, Object>> tableIterator = dialect.getTables(
			selection.getMatchCatalog(),
			selection.getMatchSchema(),
			selection.getMatchTable());
		try {
			while (tableIterator.hasNext()) {
				readTable(tableIterator.next(), tablesByName);
			}
		} finally {
			dialect.close(tableIterator);
		}
	}

	private void readTable(Map<String, Object> tableRow, Map<String, TableMetadata> tablesByName) {
		String tableName = (String) tableRow.get("TABLE_NAME");
		String catalog = (String) tableRow.get("TABLE_CAT");
		String schema = (String) tableRow.get("TABLE_SCHEM");
		String comment = (String) tableRow.get("REMARKS");

		TableIdentifier tableId = TableIdentifier.create(catalog, schema, tableName);
		if (!strategy.excludeTable(tableId)) {
			TableMetadata tableMetadata = createTableMetadata(tableName, catalog, schema, tableId);
			if (comment != null) {
				tableMetadata.comment(comment);
			}
			tablesByName.put(tableName, tableMetadata);
		}
	}

	private TableMetadata createTableMetadata(
			String tableName, String catalog, String schema, TableIdentifier tableId) {
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

		ColumnReader
				.create(dialect, strategy)
				.readColumns(tableMetadata, tableId, catalog, schema);

		detectCompositeId(tableMetadata, tableId);
		applyIdentifierStrategy(tableMetadata, tableId, catalog, schema);

		IndexReader
				.create(dialect)
				.readIndexes(tableMetadata, catalog, schema);

		return tableMetadata;
	}

	private void applyIdentifierStrategy(TableMetadata tableMetadata, TableIdentifier tableId,
			String catalog, String schema) {
		if (tableMetadata.getCompositeId() != null) {
			return;
		}

		ColumnMetadata pkColumn = null;
		for (ColumnMetadata col : tableMetadata.getColumns()) {
			if (col.isPrimaryKey()) {
				pkColumn = col;
				break;
			}
		}
		if (pkColumn == null) {
			return;
		}

		PrimaryKeyReader primaryKeyReader = PrimaryKeyReader.create(dialect, strategy);
		String strategyName = primaryKeyReader.readIdentifierStrategy(
			catalog, schema, tableMetadata.getTableName(), tableId);
		GenerationType genType = PrimaryKeyReader.toGenerationType(strategyName);
		if (genType != null) {
			pkColumn.generationType(genType);
			if (genType == GenerationType.IDENTITY) {
				pkColumn.autoIncrement(true);
			}
		}
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
}
