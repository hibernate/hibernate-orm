/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.cfg;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.DuplicateMappingException;
import org.hibernate.MappingException;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.TypeDef;
import org.hibernate.mapping.AuxiliaryDatabaseObject;
import org.hibernate.mapping.Column;
import org.hibernate.util.StringHelper;

/**
 * A collection of mappings from classes and collections to
 * relational database tables. (Represents a single
 * <tt>&lt;hibernate-mapping&gt;</tt> element.)
 * @author Gavin King
 */
public class Mappings implements Serializable {

	private static final Logger log = LoggerFactory.getLogger(Mappings.class);

	protected final Map classes;
	protected final Map collections;
	protected final Map tables;
	protected final Map queries;
	protected final Map sqlqueries;
	protected final Map resultSetMappings;
	protected final Map typeDefs;
	protected final List secondPasses;
	protected final Map imports;
	protected String schemaName;
    protected String catalogName;
	protected String defaultCascade;
	protected String defaultPackage;
	protected String defaultAccess;
	protected boolean autoImport;
	protected boolean defaultLazy;
	protected final List propertyReferences;
	protected final NamingStrategy namingStrategy;
	protected final Map filterDefinitions;
	protected final List auxiliaryDatabaseObjects;

	protected final Map extendsQueue;
//	private final List extendsQueue;

	/**
	 * binding table between the logical column name and the name out of the naming strategy
	 * for each table.
	 * According that when the column name is not set, the property name is considered as such
	 * This means that while theorically possible through the naming strategy contract, it is
	 * forbidden to have 2 real columns having the same logical name
	 * <Table, ColumnNames >
	 */
	protected final Map columnNameBindingPerTable;
	/**
	 * binding between logical table name and physical one (ie after the naming strategy has been applied)
	 * <String, TableDescription>
	 */
	protected final Map tableNameBinding;


	Mappings(
			final Map classes,
			final Map collections,
			final Map tables,
			final Map queries,
			final Map sqlqueries,
			final Map sqlResultSetMappings,
			final Map imports,
			final List secondPasses,
			final List propertyReferences,
			final NamingStrategy namingStrategy,
			final Map typeDefs,
			final Map filterDefinitions,
//			final List extendsQueue,
			final Map extendsQueue,
			final List auxiliaryDatabaseObjects,
			final Map tableNamebinding,
			final Map columnNameBindingPerTable
			) {
		this.classes = classes;
		this.collections = collections;
		this.queries = queries;
		this.sqlqueries = sqlqueries;
		this.resultSetMappings = sqlResultSetMappings;
		this.tables = tables;
		this.imports = imports;
		this.secondPasses = secondPasses;
		this.propertyReferences = propertyReferences;
		this.namingStrategy = namingStrategy;
		this.typeDefs = typeDefs;
		this.filterDefinitions = filterDefinitions;
		this.extendsQueue = extendsQueue;
		this.auxiliaryDatabaseObjects = auxiliaryDatabaseObjects;
		this.tableNameBinding = tableNamebinding;
		this.columnNameBindingPerTable = columnNameBindingPerTable;
	}

	public void addClass(PersistentClass persistentClass) throws MappingException {
		Object old = classes.put( persistentClass.getEntityName(), persistentClass );
		if ( old!=null ) {
			throw new DuplicateMappingException( "class/entity", persistentClass.getEntityName() );
		}
	}
	public void addCollection(Collection collection) throws MappingException {
		Object old = collections.put( collection.getRole(), collection );
		if ( old!=null ) {
			throw new DuplicateMappingException( "collection role", collection.getRole() );
		}
	}
	public PersistentClass getClass(String className) {
		return (PersistentClass) classes.get(className);
	}
	public Collection getCollection(String role) {
		return (Collection) collections.get(role);
	}

	public void addImport(String className, String rename) throws MappingException {
		String existing = (String) imports.put(rename, className);
		if ( existing!=null ) {
			if ( existing.equals(className) ) {
				log.info( "duplicate import: " + className + "->" + rename );
			}
			else {
				throw new DuplicateMappingException(
						"duplicate import: " + rename + 
						" refers to both " + className + 
						" and " + existing + 
						" (try using auto-import=\"false\")",
						"import",
						rename
					);
			}
		}
	}

	public Table addTable(String schema, 
			String catalog, 
			String name,
			String subselect,
			boolean isAbstract
	) {
        String key = subselect==null ?
			Table.qualify(catalog, schema, name) :
			subselect;
		Table table = (Table) tables.get(key);

		if (table == null) {
			table = new Table();
			table.setAbstract(isAbstract);
			table.setName(name);
			table.setSchema(schema);
			table.setCatalog(catalog);
			table.setSubselect(subselect);
			tables.put(key, table);
		}
		else {
			if (!isAbstract) table.setAbstract(false);
		}

		return table;
	}

	public Table addDenormalizedTable(
			String schema, 
			String catalog, 
			String name,
			boolean isAbstract, 
			String subselect,
			Table includedTable)
	throws MappingException {
        String key = subselect==null ?
        		Table.qualify(catalog, schema, name) :
        		subselect;
		if ( tables.containsKey(key) ) {
			throw new DuplicateMappingException("table", name);
		}
		
		Table table = new DenormalizedTable(includedTable);
		table.setAbstract(isAbstract);
		table.setName(name);
		table.setSchema(schema);
		table.setCatalog(catalog);
		table.setSubselect(subselect);
		tables.put(key, table);
		return table;
	}

	public Table getTable(String schema, String catalog, String name) {
        String key = Table.qualify(catalog, schema, name);
		return (Table) tables.get(key);
	}

	public String getSchemaName() {
		return schemaName;
	}

    public String getCatalogName() {
        return catalogName;
    }

	public String getDefaultCascade() {
		return defaultCascade;
	}

	/**
	 * Sets the schemaName.
	 * @param schemaName The schemaName to set
	 */
	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

    /**
     * Sets the catalogName.
     * @param catalogName The catalogName to set
     */
    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

	/**
	 * Sets the defaultCascade.
	 * @param defaultCascade The defaultCascade to set
	 */
	public void setDefaultCascade(String defaultCascade) {
		this.defaultCascade = defaultCascade;
	}

	/**
	 * sets the default access strategy
	 * @param defaultAccess the default access strategy.
	 */
	public void setDefaultAccess(String defaultAccess) {
		this.defaultAccess = defaultAccess;
	}

	public String getDefaultAccess() {
		return defaultAccess;
	}

	public void addQuery(String name, NamedQueryDefinition query) throws MappingException {
		checkQueryExist(name);
		queries.put( name.intern(), query );
	}

	public void addSQLQuery(String name, NamedSQLQueryDefinition query) throws MappingException {
		checkQueryExist(name);
		sqlqueries.put( name.intern(), query );
	}

	private void checkQueryExist(String name) throws MappingException {
		if ( sqlqueries.containsKey(name) || queries.containsKey(name) ) {
			throw new DuplicateMappingException("query", name);
		}
	}

	public void addResultSetMapping(ResultSetMappingDefinition sqlResultSetMapping) {
		final String name = sqlResultSetMapping.getName();
		if ( resultSetMappings.containsKey(name) ) {
			throw new DuplicateMappingException("resultSet",  name);
		}
		resultSetMappings.put(name, sqlResultSetMapping);
	}

	public ResultSetMappingDefinition getResultSetMapping(String name) {
		return (ResultSetMappingDefinition) resultSetMappings.get(name);
	}


	public NamedQueryDefinition getQuery(String name) {
		return (NamedQueryDefinition) queries.get(name);
	}

	public void addSecondPass(SecondPass sp) {
		addSecondPass(sp, false);
	}
    
    public void addSecondPass(SecondPass sp, boolean onTopOfTheQueue) {
		if (onTopOfTheQueue) {
			secondPasses.add(0, sp);
		}
		else {
			secondPasses.add(sp);
		}
	}

	/**
	 * Returns the autoImport.
	 * @return boolean
	 */
	public boolean isAutoImport() {
		return autoImport;
	}

	/**
	 * Sets the autoImport.
	 * @param autoImport The autoImport to set
	 */
	public void setAutoImport(boolean autoImport) {
		this.autoImport = autoImport;
	}

	void addUniquePropertyReference(String referencedClass, String propertyName) {
		PropertyReference upr = new PropertyReference();
		upr.referencedClass = referencedClass;
		upr.propertyName = propertyName;
		upr.unique = true;
		propertyReferences.add(upr);
	}

	void addPropertyReference(String referencedClass, String propertyName) {
		PropertyReference upr = new PropertyReference();
		upr.referencedClass = referencedClass;
		upr.propertyName = propertyName;
		propertyReferences.add(upr);
	}

	private String buildTableNameKey(String schema, String catalog, String finalName) {
		StringBuffer keyBuilder = new StringBuffer();
		if (schema != null) keyBuilder.append( schema );
		keyBuilder.append( ".");
		if (catalog != null) keyBuilder.append( catalog );
		keyBuilder.append( ".");
		keyBuilder.append( finalName );
		return keyBuilder.toString();
	}

	static final class PropertyReference implements Serializable {
		String referencedClass;
		String propertyName;
		boolean unique;
	}

	/**
	 * @return Returns the defaultPackage.
	 */
	public String getDefaultPackage() {
		return defaultPackage;
	}

	/**
	 * @param defaultPackage The defaultPackage to set.
	 */
	public void setDefaultPackage(String defaultPackage) {
		this.defaultPackage = defaultPackage;
	}

	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

	public void addTypeDef(String typeName, String typeClass, Properties paramMap) {
		TypeDef def = new TypeDef(typeClass, paramMap);
		typeDefs.put(typeName, def);
		log.debug("Added " + typeName + " with class " + typeClass);
	}

	public TypeDef getTypeDef(String typeName) {
		return (TypeDef) typeDefs.get(typeName);
	}

    public Iterator iterateCollections() {
        return collections.values().iterator();
    }
    
    public Iterator iterateTables() {
    	return tables.values().iterator();
    }

	public Map getFilterDefinitions() {
		return filterDefinitions;
	}

	public void addFilterDefinition(FilterDefinition definition) {
		filterDefinitions.put( definition.getFilterName(), definition );
	}
	
	public FilterDefinition getFilterDefinition(String name) {
		return (FilterDefinition) filterDefinitions.get(name);
	}
	
	public boolean isDefaultLazy() {
		return defaultLazy;
	}
	public void setDefaultLazy(boolean defaultLazy) {
		this.defaultLazy = defaultLazy;
	}

    public void addToExtendsQueue(ExtendsQueueEntry entry) {
	    extendsQueue.put( entry, null );
    }

	public PersistentClass locatePersistentClassByEntityName(String entityName) {
		PersistentClass persistentClass = ( PersistentClass ) classes.get( entityName );
		if ( persistentClass == null ) {
			String actualEntityName = ( String ) imports.get( entityName );
			if ( StringHelper.isNotEmpty( actualEntityName ) ) {
				persistentClass = ( PersistentClass ) classes.get( actualEntityName );
			}
		}
		return persistentClass;
	}

	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		auxiliaryDatabaseObjects.add( auxiliaryDatabaseObject );
	}

	public void addTableBinding(
			String schema, String catalog, String logicalName, String physicalName, Table denormalizedSuperTable
	) {
		String key = buildTableNameKey( schema, catalog, physicalName );
		TableDescription tableDescription = new TableDescription(
				logicalName, denormalizedSuperTable
		);
		TableDescription oldDescriptor = (TableDescription) tableNameBinding.put( key, tableDescription );
		if ( oldDescriptor != null && ! oldDescriptor.logicalName.equals( logicalName ) ) {
			//TODO possibly relax that
			throw new MappingException("Same physical table name reference several logical table names: "
					+ physicalName + " => " + "'" + oldDescriptor.logicalName + "' and '" + logicalName + "'");
		}
	}

	public void addColumnBinding(String logicalName, Column finalColumn, Table table) {
		ColumnNames binding = (ColumnNames) columnNameBindingPerTable.get(table);
		if (binding == null) {
			binding = new ColumnNames();
			columnNameBindingPerTable.put(table, binding);
		}
		String oldFinalName = (String) binding.logicalToPhysical.put(
				logicalName.toLowerCase(),
				finalColumn.getQuotedName()
		);
		if ( oldFinalName != null &&
				! ( finalColumn.isQuoted() ?
						oldFinalName.equals( finalColumn.getQuotedName() ) :
						oldFinalName.equalsIgnoreCase( finalColumn.getQuotedName() ) ) ) {
			//TODO possibly relax that
			throw new MappingException("Same logical column name referenced by different physical ones: "
					+ table.getName() + "." + logicalName + " => '" + oldFinalName + "' and '" + finalColumn.getQuotedName() + "'" );
		}
		String oldLogicalName = (String) binding.physicalToLogical.put(
				finalColumn.getQuotedName(),
				logicalName
		);
		if ( oldLogicalName != null && ! oldLogicalName.equals( logicalName ) ) {
			//TODO possibly relax that
			throw new MappingException("Same physical column represented by different logical column names: "
					+ table.getName() + "." + finalColumn.getQuotedName() + " => '" + oldLogicalName + "' and '" + logicalName + "'");
		}
	}

	private String getLogicalTableName(String schema, String catalog, String physicalName) {
		String key = buildTableNameKey( schema, catalog, physicalName );
		TableDescription descriptor = (TableDescription) tableNameBinding.get( key );
		if (descriptor == null) {
			throw new MappingException( "Unable to find physical table: " + physicalName);
		}
		return descriptor.logicalName;
	}

	public String getPhysicalColumnName(String logicalName, Table table) {
		logicalName = logicalName.toLowerCase();
		String finalName = null;
		Table currentTable = table;
		do {
			ColumnNames binding = (ColumnNames) columnNameBindingPerTable.get(currentTable);
			if (binding != null) {
				finalName = (String) binding.logicalToPhysical.get( logicalName );
			}
			String key = buildTableNameKey( currentTable.getSchema(), currentTable.getCatalog(), currentTable.getName() );
			TableDescription description = (TableDescription) tableNameBinding.get(key);
			if (description != null) currentTable = description.denormalizedSupertable;
		}
		while (finalName == null && currentTable != null);
		if (finalName == null) {
			throw new MappingException( "Unable to find column with logical name "
					+ logicalName + " in table " + table.getName() );
		}
		return finalName;
	}

	public String getLogicalColumnName(String physicalName, Table table) {
		String logical = null;
		Table currentTable = table;
		TableDescription description = null;
		do {
			ColumnNames binding = (ColumnNames) columnNameBindingPerTable.get(currentTable);
			if (binding != null) {
				logical = (String) binding.physicalToLogical.get( physicalName );
			}
			String key = buildTableNameKey( currentTable.getSchema(), currentTable.getCatalog(), currentTable.getName() );
			description = (TableDescription) tableNameBinding.get(key);
			if (description != null) currentTable = description.denormalizedSupertable;
		}
		while (logical == null && currentTable != null && description != null);
		if (logical == null) {
			throw new MappingException( "Unable to find logical column name from physical name "
					+ physicalName + " in table " + table.getName() );
		}
		return logical;
	}

	public String getLogicalTableName(Table table) {
		return getLogicalTableName( table.getQuotedSchema(), table.getCatalog(), table.getQuotedName() );
	}

	static public class ColumnNames implements Serializable {
		//<String, String>
		public Map logicalToPhysical = new HashMap();
		//<String, String>
		public Map physicalToLogical = new HashMap();
		public ColumnNames() {
		}
	}

	static public class TableDescription implements Serializable {
		public TableDescription(String logicalName, Table denormalizedSupertable) {
			this.logicalName = logicalName;
			this.denormalizedSupertable = denormalizedSupertable;
		}

		public String logicalName;
		public Table denormalizedSupertable;
	}
}