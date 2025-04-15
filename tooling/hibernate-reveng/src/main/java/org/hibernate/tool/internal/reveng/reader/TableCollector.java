/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2015-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.reader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.RevengDialect;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.RevengStrategy.SchemaSelection;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.RevengMetadataCollector;
import org.jboss.logging.Logger;

public class TableCollector {

	private static final Logger log = Logger.getLogger(TableCollector.class);
	
	public static TableCollector create(
			RevengDialect metaDataDialect, 
			RevengStrategy revengStrategy, 
			RevengMetadataCollector revengMetadataCollector, 
			Properties properties) {
		return new TableCollector(
				metaDataDialect, 
				revengStrategy, 
				revengMetadataCollector, 
				properties);
	}
	
	private RevengDialect metaDataDialect;
	private RevengStrategy revengStrategy;
	private RevengMetadataCollector revengMetadataCollector;
	private Properties properties;
	
	private TableCollector(
			RevengDialect metaDataDialect, 
			RevengStrategy revengStrategy, 
			RevengMetadataCollector revengMetadataCollector, 
			Properties properties) {
		this.metaDataDialect = metaDataDialect;
		this.revengStrategy = revengStrategy;
		this.revengMetadataCollector = revengMetadataCollector;
		this.properties = properties;
	}

	public Map<Table, Boolean> processTables(SchemaSelection schemaSelection) {
		  Iterator<Map<String,Object>> tableIterator = null;
		  HashMap<Table, Boolean> processedTables = new HashMap<Table, Boolean>();
		  try {			  
		     tableIterator = metaDataDialect.getTables(
		    		 StringHelper.replace(schemaSelection.getMatchCatalog(),".*", "%"), 
		    		 StringHelper.replace(schemaSelection.getMatchSchema(),".*", "%"), 
		    		 StringHelper.replace(schemaSelection.getMatchTable(),".*", "%"));
		     while (tableIterator.hasNext() ) {
		    	processTable(tableIterator.next(), processedTables);
		     }
		  } 
		  finally {
			  if (tableIterator!=null) {
				  metaDataDialect.close(tableIterator);
			  }
		  }
		  return processedTables;
	}
	
	private void processTable(Map<String, Object> tableRs, HashMap<Table, Boolean> processedTables) {
        TableIdentifier tableIdentifier = TableIdentifier.create(
        		quote((String) tableRs.get("TABLE_CAT")), 
        		quote((String) tableRs.get("TABLE_SCHEM")), 
        		quote((String) tableRs.get("TABLE_NAME")));		        
		if(revengStrategy.excludeTable(tableIdentifier) ) {
			log.debug("Table " + tableIdentifier + " excluded by strategy");
        } else if (revengMetadataCollector.getTable(tableIdentifier)!=null)	{
        	log.debug("Ignoring " + tableIdentifier + " since it has already been processed");
        } else {
        	addTable(
        			tableIdentifier, 
        			(String) tableRs.get("TABLE_TYPE"), 
        			(String) tableRs.get("REMARKS"), 
        			processedTables);
         }	
	}
	
	private void addTable(
			TableIdentifier tableIdentifier, 
			String tableType, 
			String comment, 
			HashMap<Table, Boolean> processedTables) {
    	if (isTypeToAdd(tableType)) { //||
    		log.debug("Adding table " + tableIdentifier + " of type " + tableType);
    		Table table = revengMetadataCollector.addTable(tableIdentifier);
    		table.setComment(comment);
			BasicColumnProcessor.processBasicColumns(
					metaDataDialect, 
					revengStrategy, 
					properties.getProperty(AvailableSettings.DEFAULT_SCHEMA),
					properties.getProperty(AvailableSettings.DEFAULT_CATALOG), 
					table);
			PrimaryKeyProcessor.processPrimaryKey(
					metaDataDialect, 
					revengStrategy, 
					properties.getProperty(AvailableSettings.DEFAULT_SCHEMA),
					properties.getProperty(AvailableSettings.DEFAULT_CATALOG), 
					revengMetadataCollector, 
					table);
			if (tableType.equalsIgnoreCase("TABLE")) {
				IndexProcessor.processIndices(
						metaDataDialect, 
						properties.getProperty(AvailableSettings.DEFAULT_SCHEMA),
						properties.getProperty(AvailableSettings.DEFAULT_CATALOG), 
						table);
			}
    		processedTables.put(table, tableType.equalsIgnoreCase("TABLE"));
    	}
    	else {
    		log.debug("Ignoring table " + tableIdentifier + " of type " + tableType);
    	}
	}
	
	private boolean isTypeToAdd(String tableType) {
		return "TABLE".equalsIgnoreCase(tableType) || 
				"VIEW".equalsIgnoreCase(tableType) || 
				"SYNONYM".equals(tableType);
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
