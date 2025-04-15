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
package org.hibernate.tool.internal.reveng.strategy;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.AssociationInfo;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;

public class DelegatingStrategy implements RevengStrategy {

	RevengStrategy delegate;

	public List<ForeignKey> getForeignKeys(TableIdentifier referencedTable) {
		return delegate==null?null:delegate.getForeignKeys(referencedTable);
	}

	public DelegatingStrategy(RevengStrategy delegate) {
		this.delegate = delegate;
	}

	public String columnToPropertyName(TableIdentifier table, String column) {
		return delegate==null?null:delegate.columnToPropertyName(table, column);
	}

	public boolean excludeTable(TableIdentifier ti) {
		return delegate==null?false:delegate.excludeTable(ti);
	}
	
	public boolean excludeColumn(TableIdentifier identifier, String columnName) {
		return delegate==null?false:delegate.excludeColumn(identifier, columnName);
	}

	public String foreignKeyToCollectionName(String keyname, TableIdentifier fromTable, List<?> fromColumns, TableIdentifier referencedTable, List<?> referencedColumns, boolean uniqueReference) {
		return delegate==null?null:delegate.foreignKeyToCollectionName(keyname, fromTable, fromColumns, referencedTable, referencedColumns, uniqueReference);
	}

	public String foreignKeyToEntityName(String keyname, TableIdentifier fromTable, List<?> fromColumnNames, TableIdentifier referencedTable, List<?> referencedColumnNames, boolean uniqueReference) {
		return delegate==null?null:delegate.foreignKeyToEntityName(keyname, fromTable, fromColumnNames, referencedTable, referencedColumnNames, uniqueReference);
	}

	public String columnToHibernateTypeName(TableIdentifier table, String columnName, int sqlType, int length, int precision, int scale, boolean nullable, boolean generatedIdentifier) {
		return delegate==null?null:delegate.columnToHibernateTypeName(table, columnName, sqlType, length, precision, scale, nullable, generatedIdentifier);
	}

	public String tableToClassName(TableIdentifier tableIdentifier) {
		return delegate==null?null:delegate.tableToClassName(tableIdentifier);
	}

	public String getTableIdentifierStrategyName(TableIdentifier tableIdentifier) {
		return delegate==null?null:delegate.getTableIdentifierStrategyName(tableIdentifier);
	}

	public Properties getTableIdentifierProperties(TableIdentifier identifier) {
		return delegate==null?null:delegate.getTableIdentifierProperties(identifier);
	}

	public List<String> getPrimaryKeyColumnNames(TableIdentifier identifier) {
		return delegate==null?null:delegate.getPrimaryKeyColumnNames(identifier);
	}

	public String classNameToCompositeIdName(String className) {
		return delegate==null?null:delegate.classNameToCompositeIdName(className);
	}

	public void close() {
		if(delegate!=null) delegate.close();
	}

	public String getOptimisticLockColumnName(TableIdentifier identifier) {
		return delegate==null?null:delegate.getOptimisticLockColumnName(identifier);		
	}

	public boolean useColumnForOptimisticLock(TableIdentifier identifier, String column) {
		return delegate==null?false:delegate.useColumnForOptimisticLock(identifier, column);
	}

	public List<SchemaSelection> getSchemaSelections() {
		return delegate==null?null:delegate.getSchemaSelections();
	}

	public String tableToIdentifierPropertyName(TableIdentifier tableIdentifier) {
		return delegate==null?null:delegate.tableToIdentifierPropertyName(tableIdentifier);
	}

	public String tableToCompositeIdName(TableIdentifier identifier) {
		return delegate==null?null:delegate.tableToCompositeIdName(identifier);
	}

	public boolean excludeForeignKeyAsCollection(String keyname, TableIdentifier fromTable, List<?> fromColumns, TableIdentifier referencedTable, List<?> referencedColumns) {
		return delegate==null?false:delegate.excludeForeignKeyAsCollection(keyname, fromTable, fromColumns, referencedTable, referencedColumns);
	}

	public boolean excludeForeignKeyAsManytoOne(String keyname, TableIdentifier fromTable, List<?> fromColumns, TableIdentifier referencedTable, List<?> referencedColumns) {
		return delegate==null?false:delegate.excludeForeignKeyAsManytoOne(keyname, fromTable, fromColumns, referencedTable, referencedColumns);
	}

	public boolean isForeignKeyCollectionInverse(String name, Table foreignKeyTable, List<?> columns, Table foreignKeyReferencedTable, List<?> referencedColumns) {
		return delegate==null?true:delegate.isForeignKeyCollectionInverse(name, foreignKeyTable, columns, foreignKeyReferencedTable, referencedColumns);
	}

	public boolean isForeignKeyCollectionLazy(String name, TableIdentifier foreignKeyTable, List<?> columns, TableIdentifier foreignKeyReferencedTable, List<?> referencedColumns) {
		return delegate==null?true:delegate.isForeignKeyCollectionLazy(name, foreignKeyTable, columns, foreignKeyReferencedTable, referencedColumns);
	}

	/**
	 * Initialize the settings. 
	 * 
	 * If subclasses need to use the Settings then it should keep its own reference, but still remember to initialize the delegates settings by calling super.setSettings(settings).
	 * 
	 * @see RevengStrategy.setSettings
	 */
	public void setSettings(RevengSettings settings) {
		if(delegate!=null) delegate.setSettings(settings);
	}

	public boolean isManyToManyTable(Table table) {
		return delegate==null?true:delegate.isManyToManyTable( table );
	}
	
	public boolean isOneToOne(ForeignKey foreignKey) { 
		return delegate==null?true:delegate.isOneToOne( foreignKey );
    }


	public String foreignKeyToManyToManyName(ForeignKey fromKey, TableIdentifier middleTable, ForeignKey toKey, boolean uniqueReference) {
		return delegate==null?null:delegate.foreignKeyToManyToManyName( fromKey, middleTable, toKey, uniqueReference );
	}

	public Map<String,MetaAttribute> tableToMetaAttributes(TableIdentifier tableIdentifier) {
		return delegate==null?null:delegate.tableToMetaAttributes( tableIdentifier );		
	}

	public Map<String, MetaAttribute> columnToMetaAttributes(TableIdentifier identifier, String column) {
		return delegate==null?null:delegate.columnToMetaAttributes( identifier, column );
	}

	public AssociationInfo foreignKeyToAssociationInfo(ForeignKey foreignKey) {
		return delegate==null?null:delegate.foreignKeyToAssociationInfo(foreignKey);
	}
	
	public AssociationInfo foreignKeyToInverseAssociationInfo(ForeignKey foreignKey) {
		return delegate==null?null:delegate.foreignKeyToInverseAssociationInfo(foreignKey);
	}
	
	public String foreignKeyToInverseEntityName(String keyname,
			TableIdentifier fromTable, List<?> fromColumnNames,
			TableIdentifier referencedTable, List<?> referencedColumnNames,
			boolean uniqueReference) {
		return delegate==null?null:delegate.foreignKeyToInverseEntityName(keyname, fromTable, fromColumnNames, referencedTable, referencedColumnNames, uniqueReference);
	}	
	
}
