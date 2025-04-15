/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
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
package org.hibernate.tool.internal.reveng;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.util.TableNameQualifier;

public class RevengMetadataCollector {

	private MetadataBuildingContext metadataBuildingContext = null;
	private final Map<TableIdentifier, Table> tables;
	private Map<String, List<ForeignKey>> oneToManyCandidates;
	private final Map<TableIdentifier, String> suggestedIdentifierStrategies;

	public RevengMetadataCollector(MetadataBuildingContext metadataBuildingContext) {
		this();
		this.metadataBuildingContext = metadataBuildingContext;
	}
	
	public RevengMetadataCollector() {
		this.tables = new HashMap<TableIdentifier, Table>();
		this.suggestedIdentifierStrategies = new HashMap<TableIdentifier, String>();
	}

	public Iterator<Table> iterateTables() {
		return tables.values().iterator();
	}
	
	// TableIdentifier's catalog, schema and name should be quoted
	public Table addTable(TableIdentifier tableIdentifier) {
		Table result = null;
		String catalog = tableIdentifier.getCatalog();
		String schema = tableIdentifier.getSchema();
		String name = tableIdentifier.getName();
		InFlightMetadataCollector metadataCollector = getMetadataCollector();
		if (metadataCollector != null) {
			result = metadataCollector.addTable(schema, catalog, name, null, false, metadataBuildingContext);
		} else {
			result = createTable(catalog, schema, name);			
		}
		if (tables.containsKey(tableIdentifier)) {
			throw new RuntimeException(
					"Attempt to add a double entry for table: " + 
					TableNameQualifier.qualify(catalog, schema, name));
		}
		tables.put(tableIdentifier, result);
		return result;
	}
	
	public Table getTable(TableIdentifier tableIdentifier) {
		return tables.get(tableIdentifier);
	}

	public Collection<Table> getTables() {
		return tables.values();
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
	
	private Table createTable(String catalog, String schema, String name) {
		Table table = new Table("Hibernate Tools");
		table.setAbstract(false);
		table.setName(name);
		table.setSchema(schema);
		table.setCatalog(catalog);	
		return table;
	}
	
	private InFlightMetadataCollector getMetadataCollector() {
		InFlightMetadataCollector result = null;
		if (metadataBuildingContext != null) {
			result = metadataBuildingContext.getMetadataCollector();
		}
		return result;
	}
	
}
