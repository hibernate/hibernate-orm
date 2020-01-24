package org.hibernate.tool.internal.reveng;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.dialect.MetaDataDialect;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.util.TableNameQualifier;

public class RevengMetadataCollector extends AbstractDatabaseCollector {

	private InFlightMetadataCollector metadataCollector = null;
	private final Map<TableIdentifier, Table> tables;
	
	public RevengMetadataCollector(InFlightMetadataCollector metadataCollector, MetaDataDialect metaDataDialect) {
		this(metaDataDialect);
		this.metadataCollector = metadataCollector;
	}
	
	public RevengMetadataCollector(MetaDataDialect metaDataDialect) {
		super(metaDataDialect);
		this.tables = new HashMap<TableIdentifier, Table>();
	}

	public Iterator<Table> iterateTables() {
		return tables.values().iterator();
	}

	public Table addTable(String schema, String catalog, String name) {
		Table result = null;
		TableIdentifier identifier = createIdentifier(catalog, schema, name);
		if (metadataCollector != null) {
			result = metadataCollector.addTable(quote(schema), quote(catalog), quote(name), null, false);
		} else {
			result = createTable(quote(catalog), quote(schema), quote(name));			
		}
		if (tables.containsKey(identifier)) {
			throw new RuntimeException(
					"Attempt to add a double entry for table: " + 
					TableNameQualifier.qualify(quote(catalog), quote(schema), quote(name)));
		}
		tables.put(identifier, result);
		return result;
	}

	public Table getTable(String schema, String catalog, String name) {
		return tables.get(createIdentifier(catalog, schema, name));
	}
	
	private TableIdentifier createIdentifier(String catalog, String schema, String table) {
		return TableIdentifier.create(quote(catalog), quote(schema), quote(table));
	}
	
	private Table createTable(String catalog, String schema, String name) {
		Table table = new Table();
		table.setAbstract(false);
		table.setName(name);
		table.setSchema(schema);
		table.setCatalog(catalog);	
		return table;
	}
	
}
