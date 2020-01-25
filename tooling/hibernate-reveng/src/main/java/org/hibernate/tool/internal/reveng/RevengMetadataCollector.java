package org.hibernate.tool.internal.reveng;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.dialect.MetaDataDialect;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.util.TableNameQualifier;

public class RevengMetadataCollector implements DatabaseCollector {

	private InFlightMetadataCollector metadataCollector = null;
	private final Map<TableIdentifier, Table> tables;
	private Map<String, List<ForeignKey>> oneToManyCandidates;
	private final Map<TableIdentifier, String> suggestedIdentifierStrategies;
	private final  MetaDataDialect metaDataDialect;
	
	public RevengMetadataCollector(InFlightMetadataCollector metadataCollector, MetaDataDialect metaDataDialect) {
		this(metaDataDialect);
		this.metadataCollector = metadataCollector;
	}
	
	public RevengMetadataCollector(MetaDataDialect metaDataDialect) {
		this.metaDataDialect = metaDataDialect;
		this.tables = new HashMap<TableIdentifier, Table>();
		this.suggestedIdentifierStrategies = new HashMap<TableIdentifier, String>();
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
	
	public void setOneToManyCandidates(Map<String, List<ForeignKey>> oneToManyCandidates) {
		this.oneToManyCandidates = oneToManyCandidates;
	}

	public Map<String, List<ForeignKey>> getOneToManyCandidates() {
		return oneToManyCandidates;
	}

	public String getSuggestedIdentifierStrategy(String catalog, String schema, String name) {
		return (String) suggestedIdentifierStrategies.get(TableIdentifier.create(catalog, schema, name));
	}

	public void addSuggestedIdentifierStrategy(String catalog, String schema, String name, String idstrategy) {
		suggestedIdentifierStrategies.put(TableIdentifier.create(catalog, schema, name), idstrategy);
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
	
	private String quote(String name) {
		if (name == null)
			return name;
		if (metaDataDialect.needQuote(name)) {
			if (name.length() > 1 && name.charAt(0) == '`'
					&& name.charAt(name.length() - 1) == '`') {
				return name; // avoid double quoting
			}
			return "`" + name + "`";
		} else {
			return name;
		}
	}

}
