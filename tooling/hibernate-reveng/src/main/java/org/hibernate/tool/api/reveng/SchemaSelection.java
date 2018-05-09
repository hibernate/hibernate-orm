package org.hibernate.tool.api.reveng;

public class SchemaSelection {

	String matchCatalog;
	String matchSchema;
	String matchTable;

	public SchemaSelection(String catalog, String schema, String table) {
		matchCatalog = catalog;
		matchSchema = schema;
		matchTable = table;
	}

	public SchemaSelection(String catalog, String schema) {
		this(catalog, schema, null);
	}

	public SchemaSelection() {	}


	public String getMatchCatalog() {
		return matchCatalog;
	}
	
	public void setMatchCatalog(String catalogPattern) {
		this.matchCatalog = catalogPattern;
	}
	
	public String getMatchSchema() {
		return matchSchema;
	}
	
	public void setMatchSchema(String schemaPattern) {
		this.matchSchema = schemaPattern;
	}
	
	public String getMatchTable() {
		return matchTable;
	}
	
	public void setMatchTable(String tablePattern) {
		this.matchTable = tablePattern;
	}
	
}
