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
package org.hibernate.tool.internal.reveng.util;

import java.util.List;

import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.AssociationInfo;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;

public class RevengUtils {

	public static List<String> getPrimaryKeyInfoInRevengStrategy(
			RevengStrategy revengStrat, 
			Table table, 
			String defaultCatalog, 
			String defaultSchema) {
		List<String> result = null;
		TableIdentifier tableIdentifier = TableIdentifier.create(table);
		result = revengStrat.getPrimaryKeyColumnNames(tableIdentifier);
		if (result == null) {
			String catalog = getCatalogForModel(table.getCatalog(), defaultCatalog);
			String schema = getSchemaForModel(table.getSchema(), defaultSchema);
			tableIdentifier = TableIdentifier.create(catalog, schema, table.getName());
			result = revengStrat.getPrimaryKeyColumnNames(tableIdentifier);
		}
		return result;
	}
	
	public static String getTableIdentifierStrategyNameInRevengStrategy(
			RevengStrategy revengStrat, 
			TableIdentifier tableIdentifier, 
			String defaultCatalog, 
			String defaultSchema) {
		String result = null;
		result = revengStrat.getTableIdentifierStrategyName(tableIdentifier);
		if (result == null) {
			String catalog = getCatalogForModel(tableIdentifier.getCatalog(), defaultCatalog);
			String schema = getSchemaForModel(tableIdentifier.getSchema(), defaultSchema);
			tableIdentifier = TableIdentifier.create(catalog, schema, tableIdentifier.getName());
			result = revengStrat.getTableIdentifierStrategyName(tableIdentifier);
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
		return TableIdentifier.create(tableCatalog, tableSchema, tableName);
	}
	
	public static AssociationInfo createAssociationInfo(			
			String cascade, 
			String fetch, 
			Boolean insert, 
			Boolean update) {
		return new AssociationInfo() {
			@Override
			public String getCascade() {
				return cascade;
			}
			@Override
			public String getFetch() {
				return fetch;
			}
			@Override
			public Boolean getUpdate() {
				return update;
			}
			@Override
			public Boolean getInsert() {
				return insert;
			}
			
		};
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
