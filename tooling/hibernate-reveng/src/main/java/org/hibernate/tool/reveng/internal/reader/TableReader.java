/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.reader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.GenerationType;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.tool.reveng.api.reveng.RevengDialect;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy.SchemaSelection;
import org.hibernate.tool.reveng.api.reveng.TableIdentifier;
import org.hibernate.tool.reveng.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.CompositeIdDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;

/**
 * Discovers database tables via {@link RevengDialect} and populates
 * {@link TableDescriptor} objects with columns, primary keys, and
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
	Map<String, TableDescriptor> readTables() {
		Map<String, TableDescriptor> tablesByName = new LinkedHashMap<>();
		for (SchemaSelection selection : getSchemaSelections()) {
			readTablesForSchemaSelection(selection, tablesByName);
		}
		return tablesByName;
	}

	private void readTablesForSchemaSelection(
			SchemaSelection selection, Map<String, TableDescriptor> tablesByName) {
		Iterator<Map<String, Object>> tableIterator = dialect.getTables(
			StringHelper.replace(selection.getMatchCatalog(), ".*", "%"),
			StringHelper.replace(selection.getMatchSchema(), ".*", "%"),
			StringHelper.replace(selection.getMatchTable(), ".*", "%"));
		try {
			while (tableIterator.hasNext()) {
				readTable(tableIterator.next(), tablesByName);
			}
		}
		finally {
			dialect.close(tableIterator);
		}
	}

	private void readTable(Map<String, Object> tableRow, Map<String, TableDescriptor> tablesByName) {
		String tableType = (String) tableRow.get("TABLE_TYPE");
		if (!isTypeToAdd(tableType)) {
			return;
		}

		String tableName = quote((String) tableRow.get("TABLE_NAME"), dialect);
		String catalog = quote((String) tableRow.get("TABLE_CAT"), dialect);
		String schema = quote((String) tableRow.get("TABLE_SCHEM"), dialect);
		String comment = (String) tableRow.get("REMARKS");

		TableIdentifier tableId = normalizeTableId(catalog, schema, tableName);
		if (!strategy.excludeTable(tableId)) {
			TableDescriptor tableMetadata = createTableDescriptor(
				tableName, catalog, schema, tableId, tableType);
			if (comment != null) {
				tableMetadata.comment(comment);
			}
			tablesByName.put(tableName, tableMetadata);
		}
	}

	private static boolean isTypeToAdd(String tableType) {
		return "TABLE".equalsIgnoreCase(tableType)
			|| "VIEW".equalsIgnoreCase(tableType)
			|| "SYNONYM".equals(tableType);
	}

	private TableDescriptor createTableDescriptor(
			String tableName, String catalog, String schema,
			TableIdentifier tableId, String tableType) {
		String fullClassName = strategy.tableToClassName(tableId);
		String entityPackage = StringHelper.qualifier(fullClassName);
		String entityClassName = StringHelper.unqualify(fullClassName);

		TableDescriptor tableMetadata = new TableDescriptor(tableName, entityClassName, entityPackage);
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

		if ("TABLE".equalsIgnoreCase(tableType)) {
			IndexReader
					.create(dialect)
					.readIndexes(tableMetadata, catalog, schema);
		}

		applyTableMetaAttributes(tableMetadata, tableId);

		return tableMetadata;
	}

	private void applyIdentifierStrategy(TableDescriptor tableMetadata, TableIdentifier tableId,
			String catalog, String schema) {
		if (tableMetadata.getCompositeId() != null) {
			return;
		}

		ColumnDescriptor pkColumn = null;
		for (ColumnDescriptor col : tableMetadata.getColumns()) {
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
			catalog, schema, unquote(tableMetadata.getTableName()), tableId);
		GenerationType genType = PrimaryKeyReader.toGenerationType(strategyName);
		if (genType != null) {
			pkColumn.generationType(genType);
			if (genType == GenerationType.IDENTITY) {
				pkColumn.autoIncrement(true);
			}
		}
	}

	private void detectCompositeId(TableDescriptor tableMetadata, TableIdentifier tableId) {
		List<ColumnDescriptor> pkColumns = new ArrayList<>();
		for (ColumnDescriptor col : tableMetadata.getColumns()) {
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

		String idFieldName = strategy.tableToIdentifierPropertyName(tableId);
		if (idFieldName == null) {
			idFieldName = "id";
		}
		CompositeIdDescriptor compositeId = new CompositeIdDescriptor(
			idFieldName, idClassName, tableMetadata.getEntityPackage());
		for (ColumnDescriptor pkCol : pkColumns) {
			compositeId.addAttributeOverride(pkCol.getFieldName(), pkCol.getColumnName(), pkCol.getJavaType());
		}
		tableMetadata.compositeId(compositeId);
	}

	private void applyTableMetaAttributes(TableDescriptor tableMetadata, TableIdentifier tableId) {
		Map<String, MetaAttribute> metaMap = strategy.tableToMetaAttributes(tableId);
		if (metaMap != null) {
			for (Map.Entry<String, MetaAttribute> entry : metaMap.entrySet()) {
				MetaAttribute ma = entry.getValue();
				if (ma != null && ma.getValues() != null) {
					for (Object value : ma.getValues()) {
						tableMetadata.addMetaAttribute(entry.getKey(), value.toString());
					}
				}
			}
		}
	}

	private TableIdentifier normalizeTableId(String catalog, String schema, String tableName) {
		String normCatalog = catalog != null && catalog.equals(defaultCatalog) ? null : catalog;
		String normSchema = schema != null && schema.equals(defaultSchema) ? null : schema;
		return TableIdentifier.create(normCatalog, normSchema, tableName);
	}

	private static String unquote(String name) {
		if (name != null && name.length() > 1
				&& name.charAt(0) == '`' && name.charAt(name.length() - 1) == '`') {
			return name.substring(1, name.length() - 1);
		}
		return name;
	}

	private static String quote(String name, RevengDialect dialect) {
		if (name == null) return null;
		if (dialect.needQuote(name)) {
			if (name.length() > 1 && name.charAt(0) == '`'
					&& name.charAt(name.length() - 1) == '`') {
				return name;
			}
			return "`" + name + "`";
		}
		return name;
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
