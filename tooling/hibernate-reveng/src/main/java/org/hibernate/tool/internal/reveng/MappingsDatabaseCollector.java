package org.hibernate.tool.internal.reveng;

import java.util.Iterator;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.dialect.MetaDataDialect;

public class MappingsDatabaseCollector extends AbstractDatabaseCollector {

	private final InFlightMetadataCollector metadataCollector;
	
	public MappingsDatabaseCollector(InFlightMetadataCollector metadataCollector, MetaDataDialect metaDataDialect) {
		super(metaDataDialect);
		this.metadataCollector = metadataCollector;
	}

	public Iterator<Table> iterateTables() {
		return metadataCollector.collectTableMappings().iterator();
	}

	public Table addTable(String schema, String catalog, String name) {
		return metadataCollector.addTable(quote(schema), quote(catalog), quote(name), null, false);
	}

	public Table getTable(String schema, String catalog, String name) {
		for (Table table : metadataCollector.collectTableMappings()) {
			if (equalOrBothNull(schema, table.getSchema()) && 
				equalOrBothNull(catalog, table.getCatalog()) && 
				equalOrBothNull(name, table.getName())) {
				return table;
			}
		}		
		return null;
	}
	
	private boolean equalOrBothNull(String left, String right) {
		return ((left == null) && (right == null)) || ((left != null) && left.equals(right));
	}
	
}
