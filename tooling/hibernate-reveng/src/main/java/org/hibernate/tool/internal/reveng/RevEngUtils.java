package org.hibernate.tool.internal.reveng;

import java.util.List;
import java.util.Map;

import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;

public class RevEngUtils {

	public static List<String> getPrimaryKeyInfoInRevengStrategy(
			ReverseEngineeringStrategy revengStrat, 
			Table table, 
			String defaultCatalog, 
			String defaultSchema) {
		List<String> result = null;
		TableIdentifier tableIdentifier = TableIdentifier.create(table);
		result = revengStrat.getPrimaryKeyColumnNames(tableIdentifier);
		if (result == null) {
			String catalog = getCatalogForModel(table.getCatalog(), defaultCatalog);
			String schema = getSchemaForModel(table.getSchema(), defaultSchema);
			tableIdentifier = new TableIdentifier(catalog, schema, table.getName());
			result = revengStrat.getPrimaryKeyColumnNames(tableIdentifier);
		}
		return result;
	}
	
	public static String getTableIdentifierStrategyNameInRevengStrategy(
			ReverseEngineeringStrategy revengStrat, 
			Table table, 
			String defaultCatalog, 
			String defaultSchema) {
		String result = null;
		TableIdentifier tableIdentifier = TableIdentifier.create(table);
		result = revengStrat.getTableIdentifierStrategyName(tableIdentifier);
		if (result == null) {
			String catalog = getCatalogForModel(table.getCatalog(), defaultCatalog);
			String schema = getSchemaForModel(table.getSchema(), defaultSchema);
			tableIdentifier = new TableIdentifier(catalog, schema, table.getName());
			result = revengStrat.getTableIdentifierStrategyName(tableIdentifier);
		}
		return result;	
	}

	public static Map<String,MetaAttribute> getColumnToMetaAttributesInRevengStrategy(
			ReverseEngineeringStrategy revengStrat,
			Table table,
			String defaultCatalog,
			String defaultSchema,
			String column) {
		Map<String,MetaAttribute> result = null;
		TableIdentifier tableIdentifier = TableIdentifier.create(table);
		result = revengStrat.columnToMetaAttributes(tableIdentifier, column);
		if (result == null) {
			String catalog = getCatalogForModel(table.getCatalog(), defaultCatalog);
			String schema = getSchemaForModel(table.getSchema(), defaultSchema);
			tableIdentifier = new TableIdentifier(catalog, schema, table.getName());
			result = revengStrat.columnToMetaAttributes(tableIdentifier, column);
		}
		return result;
	}
	
	public static Map<String,MetaAttribute> getTableToMetaAttributesInRevengStrategy(
			ReverseEngineeringStrategy revengStrat,
			Table table,
			String defaultCatalog,
			String defaultSchema) {
		Map<String,MetaAttribute> result = null;
		TableIdentifier tableIdentifier = TableIdentifier.create(table);
		result = revengStrat.tableToMetaAttributes(tableIdentifier);
		if (result == null) {
			String catalog = getCatalogForModel(table.getCatalog(), defaultCatalog);
			String schema = getSchemaForModel(table.getSchema(), defaultSchema);
			tableIdentifier = new TableIdentifier(catalog, schema, table.getName());
			result = revengStrat.tableToMetaAttributes(tableIdentifier);
		}
		return result;
	}
	
	public static String getColumnToPropertyNameInRevengStrategy(
			ReverseEngineeringStrategy revengStrat,
			Table table,
			String defaultCatalog,
			String defaultSchema,
			String columnName) {
		String result = null;
		TableIdentifier tableIdentifier = TableIdentifier.create(table);
		result = revengStrat.columnToPropertyName(tableIdentifier, columnName);
		if (result == null) {
			String catalog = getCatalogForModel(table.getCatalog(), defaultCatalog);
			String schema = getSchemaForModel(table.getSchema(), defaultSchema);
			tableIdentifier = new TableIdentifier(catalog, schema, table.getName());
			result = revengStrat.columnToPropertyName(tableIdentifier, columnName);
		}
		return result;
	}

	public static TableIdentifier createTableIdentifier(
			Table table, 
			String defaultCatalog, 
			String defaultSchema) {
		String tableName = table.getName();
		String tableCatalog = getCatalogForModel(table.getCatalog(), defaultCatalog);
		String tableSchema = getSchemaForModel(table.getSchema(), defaultSchema);
		return new TableIdentifier(tableCatalog, tableSchema, tableName);
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
	
}
