/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.reader;

import org.hibernate.JDBCException;
import org.hibernate.MappingException;
import org.hibernate.mapping.Column;
import org.hibernate.tool.reveng.api.core.ForeignKeyDefinition;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.core.RevengDialect;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.reveng.internal.core.RevengMetadataCollector;
import org.hibernate.tool.reveng.internal.core.util.RevengUtils;
import org.hibernate.tool.reveng.internal.util.StringUtil;
import org.hibernate.tool.reveng.internal.util.TableNameQualifier;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ForeignKeyProcessor {

	private static final Logger log = Logger.getLogger(ForeignKeyProcessor.class);

	public static ForeignKeyProcessor create(
			RevengDialect metaDataDialect,
			RevengStrategy revengStrategy,
			String defaultCatalog,
			String defaultSchema,
			RevengMetadataCollector revengMetadataCollector) {
		return new ForeignKeyProcessor(
				metaDataDialect,
				revengStrategy,
				defaultCatalog,
				defaultSchema,
				revengMetadataCollector);
	}

	private final RevengDialect metaDataDialect;
	private final RevengStrategy revengStrategy;
	private final String defaultSchema;
	private final String defaultCatalog;
	private final RevengMetadataCollector revengMetadataCollector;

	private ForeignKeyProcessor(
			RevengDialect metaDataDialect,
			RevengStrategy revengStrategy,
			String defaultCatalog,
			String defaultSchema,
			RevengMetadataCollector revengMetadataCollector) {
		this.metaDataDialect = metaDataDialect;
		this.revengStrategy = revengStrategy;
		this.defaultCatalog = defaultCatalog;
		this.defaultSchema = defaultSchema;
		this.revengMetadataCollector = revengMetadataCollector;
	}

	public ForeignKeysInfo processForeignKeys(Table referencedTable) {
		// foreign key name to list of columns in dependent table
		Map<String, List<Column>> dependentColumns = new HashMap<>();
		// foreign key name to dependent table
		Map<String, Table> dependentTables = new HashMap<>();
		// foreign key name to list of columns in referenced table
		Map<String, List<Column>> referencedColumns = new HashMap<>();
		processExportedForeignKeys(referencedTable, dependentColumns, dependentTables, referencedColumns);
		processUserForeignKeys(referencedTable, dependentColumns, dependentTables, referencedColumns);
		return new ForeignKeysInfo(referencedTable, dependentTables, dependentColumns, referencedColumns);
	}

	private void processUserForeignKeys(
			Table referencedTable,
			Map<String, List<Column>> dependentColumns,
			Map<String, Table> dependentTables,
			Map<String, List<Column>> referencedColumns) {
		List<ForeignKeyDefinition> userForeignKeys = revengStrategy.getForeignKeys(
				RevengUtils.createTableIdentifier(
						referencedTable,
						defaultCatalog,
						defaultSchema));
		if(userForeignKeys!=null) {
			for ( ForeignKeyDefinition userForeignKey : userForeignKeys ) {
				processUserForeignKey(
						userForeignKey,
						referencedTable,
						referencedColumns,
						dependentColumns,
						dependentTables );
			}
		}
	}

	private void processExportedForeignKeys(
			Table referencedTable,
			Map<String, List<Column>> dependentColumns,
			Map<String, Table> dependentTables,
			Map<String, List<Column>> referencedColumns) {
		try {
			log.debug("Calling getExportedKeys on " + referencedTable);
			Iterator<Map<String, Object>> exportedKeyIterator = metaDataDialect.getExportedKeys(
					getCatalogForDBLookup(referencedTable.getCatalog(), defaultCatalog),
					getSchemaForDBLookup(referencedTable.getSchema(), defaultSchema),
					referencedTable.getName() );
			try {
				while (exportedKeyIterator.hasNext() ) {
					short bogusFkName = 0;
					processExportedKey(
							exportedKeyIterator.next(),
							bogusFkName,
							dependentColumns,
							dependentTables,
							referencedColumns,
							referencedTable);
				}
			}
			finally {
				try {
					if(exportedKeyIterator!=null) {
						metaDataDialect.close(exportedKeyIterator);
					}
				}
				catch(JDBCException se) {
					log.warn("Exception while closing result set for foreign key meta data",se);
				}
			}
		}
		catch(JDBCException se) {
			log.warn( "Exception while reading foreign keys for " + referencedTable + " [" + se + "]", se);
		}
	}

	private void processExportedKey(
			Map<String, Object> exportedKeyRs,
			short bogusFkName,
			Map<String, List<Column>> dependentColumns,
			Map<String, Table> dependentTables,
			Map<String, List<Column>> referencedColumns,
			Table referencedTable) {
		String fkName = determineForeignKeyName(exportedKeyRs, bogusFkName);
		Table fkTable = determineForeignKeyTable(exportedKeyRs, fkName);
		if (fkTable != null) {
			log.debug("Foreign key " + fkName);
			handleDependencies(exportedKeyRs, dependentColumns, dependentTables, fkTable, fkName);
			handleReferences(exportedKeyRs, referencedColumns, referencedTable, fkName);
		}
	}

	private Table determineForeignKeyTable(Map<String, Object> exportedKeyRs, String fkName) {
		Table fkTable = getTable(
				(String) exportedKeyRs.get("FKTABLE_CAT"),
				(String) exportedKeyRs.get("FKTABLE_SCHEM"),
				(String) exportedKeyRs.get("FKTABLE_NAME"));
		if (fkTable == null) {
			String fkCatalog = getCatalogForModel((String) exportedKeyRs.get("FKTABLE_CAT"), defaultCatalog);
			String fkSchema = getSchemaForModel((String) exportedKeyRs.get("FKTABLE_SCHEM"), defaultSchema);
			String fkTableName = (String) exportedKeyRs.get("FKTABLE_NAME");
			fkTable = getTable(fkCatalog, fkSchema, fkTableName);
			if (fkTable == null) {
				log.debug(
						"Foreign key " +
						fkName +
						" references unknown or filtered table " +
						TableNameQualifier.qualify(fkCatalog, fkSchema, fkTableName) );
			}
		}
		return fkTable;
	}

	private String determineForeignKeyName(
			Map<String, Object> exportedKeyRs,
			short bogusFkName) {
		String fkName = (String) exportedKeyRs.get("FK_NAME");
		if (fkName == null) {
			fkName = Short.toString(bogusFkName);
		}
		return fkName;
	}

	private void handleReferences(
			Map<String, Object> exportedKeyRs,
			Map<String, List<Column>> referencedColumns,
			Table referencedTable,
			String fkName) {
		List<Column> primColumns = referencedColumns.computeIfAbsent( fkName, k -> new ArrayList<>() );
		final String name = (String) exportedKeyRs.get("PKCOLUMN_NAME");
		final Column existing = revengMetadataCollector.findObservedColumn( referencedTable, name );
		primColumns.add( existing != null ? existing
				: new Column( revengMetadataCollector.observedColumnName( name, metaDataDialect.needQuote( name ) ) ) );
	}

	private void handleDependencies(
			Map<String, Object> exportedKeyRs,
			Map<String, List<Column>> dependentColumns,
			Map<String, Table> dependentTables,
			Table fkTable,
			String fkName) {
		String fkColumnName = (String) exportedKeyRs.get("FKCOLUMN_NAME");
		List<Column> depColumns =  dependentColumns.get(fkName);
		if (depColumns == null) {
			depColumns = new ArrayList<>();
			dependentColumns.put(fkName,depColumns);
			dependentTables.put(fkName, fkTable);
		}
		else {
			Object previousTable = dependentTables.get(fkName);
			if(fkTable != previousTable) {
				throw new RuntimeException("Foreign key name (" + fkName + ") mapped to different tables! previous: " + previousTable + " current:" + fkTable);
			}
		}
		final Column existing = revengMetadataCollector.findObservedColumn( fkTable, fkColumnName );
		depColumns.add( existing != null ? existing
				: new Column( revengMetadataCollector.observedColumnName( fkColumnName, metaDataDialect.needQuote( fkColumnName ) ) ) );
	}

	private void processUserForeignKey(
			ForeignKeyDefinition element,
			Table referencedTable,
			Map<String, List<Column>> referencedColumns,
			Map<String, List<Column>> dependentColumns,
			Map<String, Table> dependentTables) {

		if(!equalTable(referencedTable, element.referencedTable(), defaultSchema, defaultCatalog)) {
			log.debug("Referenced table " + element.referencedTable().getName() + " is not " +  referencedTable + ". Ignoring userdefined foreign key " + element );
			return; // skip non related foreign keys
		}
		Table deptable = determineDependentTable(dependentTables, element);
		if(deptable==null) {
			//	filter out stuff we don't have tables for!
			log.debug(
					"User defined foreign key " +
					element.name() +
					" references unknown or filtered table " +
					element.table() );
		}
		else {
			dependentTables.put(element.name(), deptable);
			referencedColumns.put(element.name(), getReferencedColums(referencedTable, element) );
			dependentColumns.put(element.name(), getDependendColumns(deptable, element) );
		}
	}

	private Table determineDependentTable(Map<String, Table> dependentTables, ForeignKeyDefinition element) {
		TableIdentifier userfkTable = element.table();
		String userfkName = element.name();
		Table deptable = dependentTables.get(userfkName);
		if(deptable!=null) { // foreign key already defined!?
			throw new MappingException("Foreign key " + userfkName + " already defined in the database!");
		}
		return getTable(
				getCatalogForDBLookup(sourceText( userfkTable.getCatalog() ), defaultCatalog),
				getSchemaForDBLookup(sourceText( userfkTable.getSchema() ), defaultSchema),
				sourceText( userfkTable.getName() ));
	}

	private List<Column> getDependendColumns(Table table, ForeignKeyDefinition definition) {
		return resolveColumns( table, definition.columns().stream().map( ForeignKeyDefinition.ColumnReference::column ).toList() );
	}

	private List<Column> getReferencedColums(Table table, ForeignKeyDefinition definition) {
		return resolveColumns( table, definition.columns().stream().map( ForeignKeyDefinition.ColumnReference::referencedColumn ).toList() );
	}

	private List<Column> resolveColumns(Table table, List<String> names) {
		final List<Column> result = new ArrayList<>( names.size() );
		for ( String name : names ) {
			// Retain the reader's existing unquoted lookup behavior in this structural migration.
			final var physicalName = revengMetadataCollector.columnName( sourceText( name ) );
			final var existing = table.getColumn( physicalName );
			result.add( existing == null ? new Column( physicalName ) : existing );
		}
		return result;
	}

	private static String sourceText(String name) {
		final var identifier = org.hibernate.boot.model.naming.Identifier.toIdentifier( name );
		return identifier == null ? null : identifier.getText();
	}

	private static String getCatalogForDBLookup(String catalog, String defaultCatalog) {
		return catalog==null?defaultCatalog:catalog;
	}

	private static String getSchemaForDBLookup(String schema, String defaultSchema) {
		return schema==null?defaultSchema:schema;
	}

	/** If catalog is equal to defaultCatalog then we return null so it will be null in the generated code. */
	private static String getCatalogForModel(String catalog, String defaultCatalog) {
		if(catalog==null) return null;
		if(catalog.equals(defaultCatalog)) return null;
		return catalog;
	}

	/** If catalog is equal to defaultSchema then we return null so it will be null in the generated code. */
	private static String getSchemaForModel(String schema, String defaultSchema) {
		if(schema==null) return null;
		if(schema.equals(defaultSchema)) return null;
		return schema;
	}

	private static boolean equalTable(
			Table table1,
			TableIdentifier table2,
			String defaultSchema,
			String defaultCatalog) {
		return  table1.getName().equals(sourceText( table2.getName() ))
				&& ( StringUtil.isEqual(
						getSchemaForModel(table1.getSchema(), defaultSchema),
						getSchemaForModel(sourceText( table2.getSchema() ), defaultSchema))
				&& ( StringUtil.isEqual(
						getCatalogForModel(table1.getCatalog(), defaultCatalog),
						getCatalogForModel(sourceText( table2.getCatalog() ), defaultCatalog))));
	}

	private Table getTable(String catalog, String schema, String name) {
		return revengMetadataCollector.getTable(
				TableIdentifier.create(
						quote(catalog),
						quote(schema),
						quote(name)));
	}

	private String quote(String name) {
		if (name == null)
			return name;
		if (metaDataDialect.needQuote(name)) {
			if (name.length() > 1 && name.charAt(0) == '`'
					&& name.charAt(name.length() - 1) == '`') {
				return name; // avoid double quoting
			}
			return "`" + name + "`";
		}
		else {
			return name;
		}
	}
}
