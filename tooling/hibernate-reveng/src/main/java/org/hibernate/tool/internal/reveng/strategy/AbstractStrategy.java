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

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.AssociationInfo;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.util.JdbcToHibernateTypeHelper;
import org.hibernate.tool.internal.util.NameConverter;
import org.hibernate.tool.internal.util.TableNameQualifier;
import org.jboss.logging.Logger;

public abstract class AbstractStrategy implements RevengStrategy {

	static final private Logger log = Logger.getLogger(AbstractStrategy.class);
	
	private static final Set<String> AUTO_OPTIMISTICLOCK_COLUMNS;

	private RevengSettings settings = new RevengSettings(this);

	static {
		AUTO_OPTIMISTICLOCK_COLUMNS = new HashSet<String>();
		AUTO_OPTIMISTICLOCK_COLUMNS.add("version");
		AUTO_OPTIMISTICLOCK_COLUMNS.add("timestamp");
		AUTO_OPTIMISTICLOCK_COLUMNS.add("dbtimestamp");
	}
	
		
	public AbstractStrategy() {
		super();
	}
	
	public String columnToPropertyName(TableIdentifier table, String columnName) {
		String decapitalize = Introspector.decapitalize( toUpperCamelCase(columnName) );
		
		return keywordCheck( decapitalize );
	}

	private String keywordCheck(String possibleKeyword) {
		if(NameConverter.isReservedJavaKeyword(possibleKeyword)) {
			possibleKeyword = possibleKeyword + "_";
		}
		return possibleKeyword;
	}
	
	protected String toUpperCamelCase(String s) {
		return NameConverter.toUpperCamelCase(s);
	}
	
	/**
	 * Does some crude english pluralization
	 * TODO: are the from/to names correct ?
	 */
    public String foreignKeyToCollectionName(String keyname, TableIdentifier fromTable, List<?> fromColumns, TableIdentifier referencedTable, List<?> referencedColumns, boolean uniqueReference) {
		String propertyName = Introspector.decapitalize( StringHelper.unqualify( getRoot().tableToClassName(fromTable) ) );
		propertyName = pluralize( propertyName );
		
		if(!uniqueReference) {
        	if(fromColumns!=null && fromColumns.size()==1) {
        		String columnName = ( (Column) fromColumns.get(0) ).getName();
        		propertyName = propertyName + "For" + toUpperCamelCase(columnName);
        	} 
        	else { // composite key or no columns at all safeguard
        		propertyName = propertyName + "For" + toUpperCamelCase(keyname); 
        	}
        }
        return propertyName;
    }

	protected String pluralize(String singular) {
		return NameConverter.simplePluralize(singular);
	}

	public String foreignKeyToInverseEntityName(String keyname,
			TableIdentifier fromTable, List<?> fromColumnNames,
			TableIdentifier referencedTable, List<?> referencedColumnNames,
			boolean uniqueReference) {		
		return foreignKeyToEntityName(keyname, fromTable, fromColumnNames, referencedTable, referencedColumnNames, uniqueReference);
	}
	
	
    public String foreignKeyToEntityName(String keyname, TableIdentifier fromTable, List<?> fromColumnNames, TableIdentifier referencedTable, List<?> referencedColumnNames, boolean uniqueReference) {
        String propertyName = Introspector.decapitalize( StringHelper.unqualify( getRoot().tableToClassName(referencedTable) ) );
        
        if(!uniqueReference) {
        	if(fromColumnNames!=null && fromColumnNames.size()==1) {
        		String columnName = ( (Column) fromColumnNames.get(0) ).getName();
        		propertyName = propertyName + "By" + toUpperCamelCase(columnName);
        	} 
        	else { // composite key or no columns at all safeguard
        		propertyName = propertyName + "By" + toUpperCamelCase(keyname); 
        	}
        }
        
        return propertyName;
    }
	
	public String columnToHibernateTypeName(TableIdentifier table, String columnName, int sqlType, int length, int precision, int scale, boolean nullable, boolean generatedIdentifier) {
		String preferredHibernateType = JdbcToHibernateTypeHelper.getPreferredHibernateType(sqlType, length, precision, scale, nullable, generatedIdentifier);
		
		String location = "<no info>";
		if(log.isDebugEnabled()) {
			String info = " t:" + JdbcToHibernateTypeHelper.getJDBCTypeName( sqlType ) + " l:" + length + " p:" + precision + " s:" + scale + " n:" + nullable + " id:" + generatedIdentifier;
			if(table!=null) {
				location = TableNameQualifier.qualify(table.getCatalog(), table.getSchema(), table.getName() ) + "." + columnName + info;
			} else {
				
				location += " Column: " + columnName + info;
			}			
		}
		if(preferredHibernateType==null) {
			log.debug("No default type found for [" + location + "] falling back to [serializable]");
			return "serializable";
		} else {
			log.debug("Default type found for [" + location + "] to [" + preferredHibernateType + "]");		
			return preferredHibernateType;
		}		
	}

	public boolean excludeTable(TableIdentifier ti) {		
		return false;
	}
	
	public boolean excludeColumn(TableIdentifier identifier, String columnName) {
		return false;
	}

	public String tableToClassName(TableIdentifier tableIdentifier) {
		
		String pkgName = settings.getDefaultPackageName();
		String className = toUpperCamelCase( tableIdentifier.getName() );
		
		if(!pkgName.isEmpty()) {
			return StringHelper.qualify(pkgName, className);
		}
		else {
			return className;
		}
		
	}

	public List<ForeignKey> getForeignKeys(TableIdentifier referencedTable) {
		return Collections.emptyList();
	}

	public String getTableIdentifierStrategyName(TableIdentifier identifier) {
		return null;
	}

	public Properties getTableIdentifierProperties(TableIdentifier identifier) {
		return new Properties();
	}

	public List<String> getPrimaryKeyColumnNames(TableIdentifier identifier) {
		return null;
	}

	public String classNameToCompositeIdName(String className) {
		return className + "Id"; 
	}

	public void close() {
		
	}
	
	

	/** Return explicit which column name should be used for optimistic lock */
	public String getOptimisticLockColumnName(TableIdentifier identifier) {
		return null;
	}

	public boolean useColumnForOptimisticLock(TableIdentifier identifier, String column) {
		if(settings.getDetectOptimsticLock()) {
			return AUTO_OPTIMISTICLOCK_COLUMNS.contains(column.toLowerCase());
		} else {
			return false;
		}
	}

	public List<SchemaSelection> getSchemaSelections() {
		return null;
	}

	public String tableToIdentifierPropertyName(TableIdentifier tableIdentifier) {
		return null;
	}

	public String tableToCompositeIdName(TableIdentifier identifier) {
		return null;
	}

	public boolean excludeForeignKeyAsCollection(String keyname, TableIdentifier fromTable, List<?> fromColumns, TableIdentifier referencedTable, List<?> referencedColumns) {
		return !settings.createCollectionForForeignKey();		
	}

	public boolean excludeForeignKeyAsManytoOne(String keyname, TableIdentifier fromTable, List<?> fromColumns, TableIdentifier referencedTable, List<?> referencedColumns) {
		return !settings.createManyToOneForForeignKey();
	}

	public boolean isForeignKeyCollectionInverse(String name, Table foreignKeyTable, List<?> columns, Table foreignKeyReferencedTable, List<?> referencedColumns) {
		if(foreignKeyTable==null) {
			return true; // we don't know better
		}		
		if(isManyToManyTable(foreignKeyTable)) {
		       // if the reference column is the first one then we are inverse.
			   Column column = foreignKeyTable.getColumn(0);
			   Column fkColumn = (Column) referencedColumns.get(0);
            return fkColumn.equals(column);
		}
		return true;
	}

	public boolean isForeignKeyCollectionLazy(String name, TableIdentifier foreignKeyTable, List<?> columns, TableIdentifier foreignKeyReferencedTable, List<?> referencedColumns) {
		return true;
	}

	public void setSettings(RevengSettings settings) {
		this.settings = settings;		
	}

	public boolean isOneToOne(ForeignKey foreignKey) {
		if(settings.getDetectOneToOne()) {
			// add support for non-PK associations
			List<Column> fkColumns = foreignKey.getColumns();
			List<Column> pkForeignTableColumns = Collections.emptyList();

			if (foreignKey.getTable().hasPrimaryKey())
				pkForeignTableColumns = foreignKey.getTable().getPrimaryKey().getColumns();

			boolean equals = fkColumns.size() == pkForeignTableColumns.size();

			Iterator<Column> columns = foreignKey.getColumns().iterator();
			while (equals && columns.hasNext()) {
				Column fkColumn = columns.next();
				equals = pkForeignTableColumns.contains(fkColumn);
			}
			return equals;
		} else {
			return false;
		}
	}

	public boolean isManyToManyTable(Table table) {
		if(settings.getDetectManyToMany()) {
			
			// if the number of columns in the primary key is different 
			// than the total number of columns then it can't be a middle table
			PrimaryKey pk = table.getPrimaryKey();
			if ( pk==null || pk.getColumns().size() != table.getColumnSpan() )
				return false;
			
			List<ForeignKey> foreignKeys = new ArrayList<ForeignKey>();
			
			// if we have more than 2 fk, means we have more than 2 table implied
			// in this table --> cannot be a simple many-to-many
			for (ForeignKey fkey : table.getForeignKeyCollection()) {
				foreignKeys.add( fkey );
				if(foreignKeys.size()>2) {
					return false; // early exit if we have more than two fk.
				}
			}
			if(foreignKeys.size()!=2) {
				return false;
			}
			
			// tests that all columns are implied in the fks
            Set<Column> columns = new HashSet<>(table.getColumns());
			
			for (ForeignKey fkey : table.getForeignKeyCollection()) {
				if (columns.isEmpty()) break;
				fkey.getColumns().forEach(columns::remove);
			}
			
			return columns.isEmpty();

		} else {
			return false;
		}
	}

	protected RevengStrategy getRoot() {
		return settings.getRootStrategy();
	}
	
	public String foreignKeyToManyToManyName(ForeignKey fromKey, TableIdentifier middleTable, ForeignKey toKey, boolean uniqueReference) {
		String propertyName = Introspector.decapitalize( StringHelper.unqualify( getRoot().tableToClassName(TableIdentifier.create( toKey.getReferencedTable()) )) );
		propertyName = pluralize( propertyName );
		
		if(!uniqueReference) {
			//TODO: maybe use the middleTable name here ?
        	if(toKey.getColumns()!=null && toKey.getColumns().size()==1) {
        		String columnName = ( (Column) toKey.getColumns().get(0) ).getName();
        		propertyName = propertyName + "For" + toUpperCamelCase(columnName);
        	} 
        	else { // composite key or no columns at all safeguard
        		propertyName = propertyName + "For" + toUpperCamelCase(toKey.getName()); 
        	}
        }
        return propertyName;      
	}

	public Map<String,MetaAttribute> tableToMetaAttributes(TableIdentifier tableIdentifier) {
		return null;
	}

	public Map<String, MetaAttribute> columnToMetaAttributes(TableIdentifier identifier, String column) {
		return null;
	}

	public AssociationInfo foreignKeyToAssociationInfo(ForeignKey foreignKey) {
		return null;
	}

	public AssociationInfo foreignKeyToInverseAssociationInfo(ForeignKey foreignKey) {
		return null;
	}

	
	
}
