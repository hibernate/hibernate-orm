package org.hibernate.tool.internal.reveng;

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
import org.hibernate.tool.api.reveng.ReverseEngineeringRuntimeInfo;
import org.hibernate.tool.api.reveng.ReverseEngineeringSettings;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.SchemaSelection;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.util.JdbcToHibernateTypeHelper;
import org.hibernate.tool.internal.util.NameConverter;
import org.hibernate.tool.internal.util.TableNameQualifier;
import org.jboss.logging.Logger;

public class DefaultReverseEngineeringStrategy implements ReverseEngineeringStrategy {

	static final private Logger log = Logger.getLogger(DefaultReverseEngineeringStrategy.class);
	
	private static Set<String> AUTO_OPTIMISTICLOCK_COLUMNS;

	private ReverseEngineeringSettings settings = new ReverseEngineeringSettings(this);

	private ReverseEngineeringRuntimeInfo runtimeInfo;
	static {
		AUTO_OPTIMISTICLOCK_COLUMNS = new HashSet<String>();
		AUTO_OPTIMISTICLOCK_COLUMNS.add("version");
		AUTO_OPTIMISTICLOCK_COLUMNS.add("timestamp");
	}
	
		
	public DefaultReverseEngineeringStrategy() {
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
		
		if(pkgName.length()>0) {			
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
		return null;
	}

	public List<String> getPrimaryKeyColumnNames(TableIdentifier identifier) {
		return null;
	}

	public String classNameToCompositeIdName(String className) {
		return className + "Id"; 
	}

	public void configure(ReverseEngineeringRuntimeInfo rti) {
		this.runtimeInfo = rti;		
	}

	public void close() {
		
	}
	
	

	/** Return explicit which column name should be used for optimistic lock */
	public String getOptimisticLockColumnName(TableIdentifier identifier) {
		return null;
	}

	public boolean useColumnForOptimisticLock(TableIdentifier identifier, String column) {
		if(settings.getDetectOptimsticLock()) {
			return AUTO_OPTIMISTICLOCK_COLUMNS.contains(column.toLowerCase())?true:false;
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

	public boolean excludeForeignKeyAsCollection(String keyname, TableIdentifier fromTable, List<Column> fromColumns, TableIdentifier referencedTable, List<Column> referencedColumns) {
		return !settings.createCollectionForForeignKey();		
	}

	public boolean excludeForeignKeyAsManytoOne(String keyname, TableIdentifier fromTable, List<?> fromColumns, TableIdentifier referencedTable, List<?> referencedColumns) {
		return !settings.createManyToOneForForeignKey();
	}

	public boolean isForeignKeyCollectionInverse(String name, TableIdentifier foreignKeyTable, List<?> columns, TableIdentifier foreignKeyReferencedTable, List<?> referencedColumns) {
		Table fkTable = getRuntimeInfo().getTable(foreignKeyTable);
		if(fkTable==null) {
			return true; // we don't know better
		}
		
		if(isManyToManyTable(fkTable)) {
		       // if the reference column is the first one then we are inverse.
			   Column column = fkTable.getColumn(0);
			   Column fkColumn = (Column) referencedColumns.get(0);
			   if(fkColumn.equals(column)) {
				   return true;   
			   } else {
				   return false;
			   }
		}
		return true;
	}

	public boolean isForeignKeyCollectionLazy(String name, TableIdentifier foreignKeyTable, List<?> columns, TableIdentifier foreignKeyReferencedTable, List<?> referencedColumns) {
		return true;
	}

	public void setSettings(ReverseEngineeringSettings settings) {
		this.settings = settings;		
	}

	public boolean isOneToOne(ForeignKey foreignKey) {
		if(settings.getDetectOneToOne()) {
			// add support for non-PK associations
			List<Column> fkColumns = foreignKey.getColumns();
			List<Column> pkForeignTableColumns = null;
			
			if (foreignKey.getTable().hasPrimaryKey())
				pkForeignTableColumns = foreignKey.getTable().getPrimaryKey().getColumns();

			boolean equals =
				fkColumns != null && pkForeignTableColumns != null
				&& fkColumns.size() == pkForeignTableColumns.size();

			Iterator<Column> columns = foreignKey.getColumnIterator();
			while (equals && columns.hasNext()) {
				Column fkColumn = (Column) columns.next();
				equals = equals && pkForeignTableColumns.contains(fkColumn);
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
			
			Iterator<?> foreignKeyIterator = table.getForeignKeyIterator();
			List<ForeignKey> foreignKeys = new ArrayList<ForeignKey>();
			
			// if we have more than 2 fk, means we have more than 2 table implied
			// in this table --> cannot be a simple many-to-many
			while ( foreignKeyIterator.hasNext() ) {
				ForeignKey fkey = (ForeignKey) foreignKeyIterator.next();
				foreignKeys.add( fkey );
				if(foreignKeys.size()>2) {
					return false; // early exit if we have more than two fk.
				}
			}
			if(foreignKeys.size()!=2) {
				return false;
			}
			
			// tests that all columns are implied in the fks
			Set<Column> columns = new HashSet<Column>();
			Iterator<?> columnIterator = table.getColumnIterator();
			while ( columnIterator.hasNext() ) {
				Column column = (Column) columnIterator.next();
				columns.add(column);
			}
			
						
			foreignKeyIterator = table.getForeignKeyIterator();
			while ( !columns.isEmpty() && foreignKeyIterator.hasNext() ) {
				ForeignKey element = (ForeignKey) foreignKeyIterator.next();				
				columns.removeAll( element.getColumns() );				
			}
			// what if one of the columns is not the primary key?
			
			return columns.isEmpty();
			

			
			
			
		} else {
			return false;
		}
	}

	protected ReverseEngineeringStrategy getRoot() {
		return settings.getRootStrategy();
	}
	
	protected ReverseEngineeringRuntimeInfo getRuntimeInfo() {
		return runtimeInfo;
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
