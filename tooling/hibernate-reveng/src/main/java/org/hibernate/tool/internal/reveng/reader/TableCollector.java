package org.hibernate.tool.internal.reveng.reader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.dialect.MetaDataDialect;
import org.hibernate.tool.api.reveng.DatabaseCollector;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.SchemaSelection;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.jboss.logging.Logger;

public class TableCollector {

	private static final Logger log = Logger.getLogger(TableCollector.class);
	
	public static TableCollector create(
			MetaDataDialect metaDataDialect, 
			ReverseEngineeringStrategy revengStrategy, 
			DatabaseCollector databaseCollector, 
			SchemaSelection schemaSelection, 
			Set<Table> hasIndices) {
		return new TableCollector(
				metaDataDialect, 
				revengStrategy, 
				databaseCollector, 
				schemaSelection, 
				hasIndices);
	}
	
	private MetaDataDialect metaDataDialect;
	private ReverseEngineeringStrategy revengStrategy;
	private DatabaseCollector databaseCollector;
	private SchemaSelection schemaSelection;
	private Set<Table> hasIndices;
	
	private TableCollector(
			MetaDataDialect metaDataDialect, 
			ReverseEngineeringStrategy revengStrategy, 
			DatabaseCollector databaseCollector, 
			SchemaSelection schemaSelection, 
			Set<Table> hasIndices) {
		this.metaDataDialect = metaDataDialect;
		this.revengStrategy = revengStrategy;
		this.databaseCollector = databaseCollector;
		this.schemaSelection = schemaSelection;
		this.hasIndices = hasIndices;
	}

	public Collection<Table> processTables() {
		Map<String,Object> tableRs = null;
		Iterator<Map<String,Object>> tableIterator = null;
		List<Map<String,Object>> tables = new ArrayList<Map<String,Object>>();
		boolean multiSchema = false; 
		// TODO: the code below detects if the reveng is multischema'ed, but not used for anything yet. should be used to remove schema/catalog info from output if only one schema/catalog used.
		
		  try {			  
		     String matchCatalog = StringHelper.replace(schemaSelection.getMatchCatalog(),".*", "%");
		     String matchSchema = StringHelper.replace(schemaSelection.getMatchSchema(),".*", "%");
		     String matchTable = StringHelper.replace(schemaSelection.getMatchTable(),".*", "%");
		     tableIterator = metaDataDialect.getTables(matchCatalog, matchSchema, matchTable);
		     String[] lastQualifier = null;
		     String[] foundQualifier = new String[2];
		     
		     while (tableIterator.hasNext() ) {
		        tableRs = tableIterator.next();
		        String tableName = (String) tableRs.get("TABLE_NAME");
				String schemaName = (String) tableRs.get("TABLE_SCHEM");
		        String catalogName = (String) tableRs.get("TABLE_CAT");
		        TableIdentifier ti = TableIdentifier.create(catalogName, schemaName, tableName);		        
				if(revengStrategy.excludeTable(ti) ) {
					log.debug("Table " + ti + " excluded by strategy");
		        	continue;
		        }
				
				if(!multiSchema) {
					foundQualifier[0] = catalogName;
					foundQualifier[1] = schemaName;
					if(lastQualifier==null) {
						lastQualifier=new String[2];
						lastQualifier[0] = foundQualifier[0];
						lastQualifier[1] = foundQualifier[1];					
					}
					if((!safeEquals(lastQualifier[0],foundQualifier[0])) || (!safeEquals(lastQualifier[1],foundQualifier[1]))) {
						multiSchema = true;
					}
				}
				
				tables.add(new HashMap<String,Object>(tableRs));
		     }
		  } 
		  finally {
			  try {
				  if (tableIterator!=null) metaDataDialect.close(tableIterator);
			  } 
			  catch (Exception ignore) {
			  }
		  }
		  
		  List<Table> processedTables = new ArrayList<Table>();
		  tableIterator = tables.iterator();
		  while (tableIterator.hasNext() ) {
			  tableRs = tableIterator.next();
			  String tableName = (String) tableRs.get("TABLE_NAME");
			  String schemaName = (String) tableRs.get("TABLE_SCHEM");
			  String catalogName = (String) tableRs.get("TABLE_CAT");
			  
			  TableIdentifier ti = TableIdentifier.create(catalogName, schemaName, tableName);
			   if(revengStrategy.excludeTable(ti) ) {
			   log.debug("Table " + ti + " excluded by strategy");
			   continue;
			   }
			  
			  String comment = (String) tableRs.get("REMARKS");
			  String tableType = (String) tableRs.get("TABLE_TYPE");
			  
			  if(databaseCollector.getTable
					  (schemaName, 
							  catalogName, 
							  tableName)!=null) {
				  log.debug("Ignoring " + tableName + " since it has already been processed");
				  continue;
			  } else {
				  if ( ("TABLE".equalsIgnoreCase(tableType) || "VIEW".equalsIgnoreCase(tableType) || "SYNONYM".equals(tableType) ) ) { //||
					  // ("SYNONYM".equals(tableType) && isOracle() ) ) { // only on oracle ? TODO: HBX-218
					  // it's a regular table or a synonym
					  
					  // ensure schema and catalogname is truly empty (especially mysql returns null schema, "" catalog)
					  if(schemaName!=null && schemaName.trim().length()==0) {
						  schemaName = null;
					  }                     
					  if(catalogName!=null && catalogName.trim().length()==0) {
						  catalogName=null;
					  }
					  log.debug("Adding table " + tableName + " of type " + tableType);
					  Table table = databaseCollector.addTable(schemaName, catalogName, tableName);
					  table.setComment(comment);
					  if(tableType.equalsIgnoreCase("TABLE")) {
						  hasIndices.add(table);
					  }
					  processedTables.add( table );
				  }
				  else {
					  log.debug("Ignoring table " + tableName + " of type " + tableType);
				  }
			  }
		  }
		  
		  return processedTables;
	}
	
	private static boolean safeEquals(Object value, Object tf) {
		if(value==tf) return true;
		if(value==null) return false;
		return value.equals(tf);
	}

}
