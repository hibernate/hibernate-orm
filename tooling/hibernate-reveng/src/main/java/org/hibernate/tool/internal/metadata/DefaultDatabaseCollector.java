package org.hibernate.tool.internal.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.dialect.MetaDataDialect;
import org.hibernate.tool.internal.reveng.AbstractDatabaseCollector;
import org.hibernate.tool.internal.util.TableNameQualifier;

public class DefaultDatabaseCollector extends AbstractDatabaseCollector  {

	private Map<String, Table> tables;		
	private Map<String, List<Table>> qualifiers;

	public DefaultDatabaseCollector(MetaDataDialect metaDataDialect) {
		super(metaDataDialect);
		tables = new HashMap<String, Table>();
		qualifiers = new HashMap<String, List<Table>>();
	}
	
	public Iterator<Table> iterateTables() {
		return tables.values().iterator();
	}

	public Table addTable(String schema, 
			String catalog, 
			String name) {
		
        String key = TableNameQualifier.qualify(quote(catalog), quote(schema), quote(name));
		Table table = (Table) tables.get(key);
		
		if (table == null) {
			table = new Table();
			table.setAbstract(false);
			table.setName(name);
			table.setSchema(schema);
			table.setCatalog(catalog);
			tables.put(key, table);
			
			String qualifier = StringHelper.qualifier(key);
			List<Table> schemaList = qualifiers.get(qualifier);
			if(schemaList==null) {
				schemaList = new ArrayList<Table>();
				qualifiers.put(qualifier, schemaList);				
			}
			schemaList.add(table);
		}
		else {
			table.setAbstract(false);
		}
		
		return table;
	}

	public Table getTable(String schema, String catalog, String name) {
        String key = TableNameQualifier.qualify(quote(catalog), quote(schema), quote(name));
		return (Table) tables.get(key);
	}

	public Iterator<Entry<String, List<Table>>> getQualifierEntries() {
		return qualifiers.entrySet().iterator();
	}
	
	
}
