package org.hibernate.tool.internal.reveng.reader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.dialect.MetaDataDialect;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.SchemaSelection;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.RevengMetadataCollector;
import org.jboss.logging.Logger;

public class TableCollector {

	private static final Logger log = Logger.getLogger(TableCollector.class);
	
	public static TableCollector create(
			MetaDataDialect metaDataDialect, 
			ReverseEngineeringStrategy revengStrategy, 
			RevengMetadataCollector revengMetadataCollector, 
			SchemaSelection schemaSelection) {
		return new TableCollector(
				metaDataDialect, 
				revengStrategy, 
				revengMetadataCollector, 
				schemaSelection);
	}
	
	private MetaDataDialect metaDataDialect;
	private ReverseEngineeringStrategy revengStrategy;
	private RevengMetadataCollector revengMetadataCollector;
	private SchemaSelection schemaSelection;
	
	private TableCollector(
			MetaDataDialect metaDataDialect, 
			ReverseEngineeringStrategy revengStrategy, 
			RevengMetadataCollector revengMetadataCollector, 
			SchemaSelection schemaSelection) {
		this.metaDataDialect = metaDataDialect;
		this.revengStrategy = revengStrategy;
		this.revengMetadataCollector = revengMetadataCollector;
		this.schemaSelection = schemaSelection;
	}

	public Map<Table, Boolean> processTables() {
		  Iterator<Map<String,Object>> tableIterator = null;
		  HashMap<Table, Boolean> processedTables = new HashMap<Table, Boolean>();
		  try {			  
		     String matchCatalog = StringHelper.replace(schemaSelection.getMatchCatalog(),".*", "%");
		     String matchSchema = StringHelper.replace(schemaSelection.getMatchSchema(),".*", "%");
		     String matchTable = StringHelper.replace(schemaSelection.getMatchTable(),".*", "%");
		     tableIterator = metaDataDialect.getTables(matchCatalog, matchSchema, matchTable);
		     while (tableIterator.hasNext() ) {
		    	Map<String,Object> tableRs = tableIterator.next();
		        String tableName = (String) tableRs.get("TABLE_NAME");
				String schemaName = (String) tableRs.get("TABLE_SCHEM");
		        String catalogName = (String) tableRs.get("TABLE_CAT");
		        TableIdentifier tableIdentifier = TableIdentifier.create(quote(catalogName), quote(schemaName), quote(tableName));		        
				if(revengStrategy.excludeTable(tableIdentifier) ) {
					log.debug("Table " + tableIdentifier + " excluded by strategy");
		        	continue;
		        }								
				String comment = (String) tableRs.get("REMARKS");
				String tableType = (String) tableRs.get("TABLE_TYPE");
				if(revengMetadataCollector.getTable(tableIdentifier)!=null) {
					  log.debug("Ignoring " + tableName + " since it has already been processed");
					  continue;
				  } else {
					  if ( ("TABLE".equalsIgnoreCase(tableType) || "VIEW".equalsIgnoreCase(tableType) || "SYNONYM".equals(tableType) ) ) { //||
						  log.debug("Adding table " + tableName + " of type " + tableType);
						  Table table = revengMetadataCollector.addTable(tableIdentifier);
						  table.setComment(comment);
						  if(tableType.equalsIgnoreCase("TABLE")) {
							  processedTables.put(table, true);
						  } else 
							  processedTables.put(table, false );
					  }
					  else {
						  log.debug("Ignoring table " + tableName + " of type " + tableType);
					  }
				  }
		     }
		  } 
		  finally {
			  try {
				  if (tableIterator!=null) metaDataDialect.close(tableIterator);
			  } 
			  catch (Exception ignore) {
			  }
		  }
		  return processedTables;
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
