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
import java.util.Properties;
import java.util.Map;
import java.util.ListIterator;

import org.hibernate.DuplicateMappingException;
import org.hibernate.MappingException;
import org.hibernate.id.factory.DefaultIdentifierGeneratorFactory;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.TypeDef;
import org.hibernate.mapping.AuxiliaryDatabaseObject;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.FetchProfile;

/**
 * A collection of mappings from classes and collections to relational database tables.  Represents a single
 * <tt>&lt;hibernate-mapping&gt;</tt> element.
 * <p/>
 * todo : the statement about this representing a single mapping element is simply not true if it was ever the case.
 * this contract actually represents 3 scopes of information: <ol>
 * <li><i>bounded</i> state : this is information which is indeed scoped by a single mapping</li>
 * <li><i>unbounded</i> state : this is information which is Configuration wide (think of metadata repository)</li>
 * <li><i>transient</i> state : state which changed at its own pace (naming strategy)</li>
 * </ol>
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface Mappings {

	/**
	 * Get the current naming strategy.
	 *
	 * @return The current naming strategy.
	 */
	public NamingStrategy getNamingStrategy();

	/**
	 * Set the current naming strategy.
	 *
	 * @param namingStrategy The naming strategy to use.
	 */
	public void setNamingStrategy(NamingStrategy namingStrategy);

	/**
	 * Returns the currently bound default schema name.
	 *
	 * @return The currently bound schema name
	 */
	public String getSchemaName();

	/**
	 * Sets the currently bound default schema name.
	 *
	 * @param schemaName The schema name to bind as the current default.
	 */
	public void setSchemaName(String schemaName);

	/**
	 * Returns the currently bound default catalog name.
	 *
	 * @return The currently bound catalog name, or null if none.
	 */
	public String getCatalogName();

    /**
     * Sets the currently bound default catalog name.
	 *
     * @param catalogName The catalog name to use as the current default.
     */
    public void setCatalogName(String catalogName);

	/**
	 * Get the currently bound default package name.
	 *
	 * @return The currently bound default package name
	 */
	public String getDefaultPackage();

	/**
	 * Set the current default package name.
	 *
	 * @param defaultPackage The package name to set as the current default.
	 */
	public void setDefaultPackage(String defaultPackage);

	/**
	 * Determine whether auto importing of entity names is currently enabled.
	 *
	 * @return True if currently enabled; false otherwise.
	 */
	public boolean isAutoImport();

	/**
	 * Set whether to enable auto importing of entity names.
	 *
	 * @param autoImport True to enable; false to diasable.
	 * @see #addImport
	 */
	public void setAutoImport(boolean autoImport);

	/**
	 * Determine whether default laziness is currently enabled.
	 *
	 * @return True if enabled, false otherwise.
	 */
	public boolean isDefaultLazy();

	/**
	 * Set whether to enable default laziness.
	 *
	 * @param defaultLazy True to enable, false to disable.
	 */
	public void setDefaultLazy(boolean defaultLazy);

	/**
	 * Get the current default cascade style.
	 *
	 * @return The current default cascade style.
	 */
	public String getDefaultCascade();

	/**
	 * Sets the current default cascade style.
	 * .
	 * @param defaultCascade The cascade style to set as the current default.
	 */
	public void setDefaultCascade(String defaultCascade);

	/**
	 * Get the current default property access style.
	 *
	 * @return The current default property access style.
	 */
	public String getDefaultAccess();

	/**
	 * Sets the current default property access style.
	 *
	 * @param defaultAccess The access style to use as the current default.
	 */
	public void setDefaultAccess(String defaultAccess);


	/**
	 * Retrieves an iterator over the entity metadata present in this repository.
	 *
	 * @return Iterator over class metadata.
	 */
	public Iterator iterateClasses();

	/**
	 * Retrieves the entity mapping metadata for the given entity name.
	 *
	 * @param entityName The entity name for which to retrieve the metadata.
	 * @return The entity mapping metadata, or null if none found matching given entity name.
	 */
	public PersistentClass getClass(String entityName);

	/**
	 * Retrieves the entity mapping metadata for the given entity name, potentially accounting
	 * for imports.
	 *
	 * @param entityName The entity name for which to retrieve the metadata.
	 * @return The entity mapping metadata, or null if none found matching given entity name.
	 */
	public PersistentClass locatePersistentClassByEntityName(String entityName);

	/**
	 * Add entity mapping metadata.
	 *
	 * @param persistentClass The entity metadata
	 * @throws DuplicateMappingException Indicates there4 was already an extry
	 * corresponding to the given entity name.
	 */
	public void addClass(PersistentClass persistentClass) throws DuplicateMappingException;

	/**
	 * Adds an import (HQL entity rename) to the repository.
	 *
	 * @param entityName The entity name being renamed.
	 * @param rename The rename
	 * @throws DuplicateMappingException If rename already is mapped to another
	 * entity name in this repository.
	 */
	public void addImport(String entityName, String rename) throws DuplicateMappingException;

	/**
	 * Retrieves the collection mapping metadata for the given collection role.
	 *
	 * @param role The collection role for which to retrieve the metadata.
	 * @return The collection mapping metadata, or null if no matching collection role found.
	 */
	public Collection getCollection(String role);

	/**
	 * Returns an iterator over collection metadata.
	 *
	 * @return Iterator over collection metadata.
	 */
	public Iterator iterateCollections();

	/**
	 * Add collection mapping metadata to this repository.
	 *
	 * @param collection The collection metadata
	 * @throws DuplicateMappingException Indicates there was already an entry
	 * corresponding to the given collection role
	 */
	public void addCollection(Collection collection) throws DuplicateMappingException;

	/**
	 * Returns the named table metadata.
	 *
	 * @param schema The named schema in which the table belongs (or null).
	 * @param catalog The named catalog in which the table belongs (or null).
	 * @param name The table name
	 * @return The table metadata, or null.
	 */
	public Table getTable(String schema, String catalog, String name);

	/**
	 * Returns an iterator over table metadata.
	 *
	 * @return Iterator over table metadata.
	 */
	public Iterator iterateTables();

	/**
	 * Adds table metedata to this repository returning the created
	 * metadata instance.
	 *
	 * @param schema The named schema in which the table belongs (or null).
	 * @param catalog The named catalog in which the table belongs (or null).
	 * @param name The table name
	 * @param subselect A select statement wwich defines a logical table, much
	 * like a DB view.
	 * @param isAbstract Is the table abstract (i.e. not really existing in the DB)?
	 * @return The created table metadata, or the existing reference.
	 */
	public Table addTable(String schema, String catalog, String name, String subselect, boolean isAbstract);

	/**
	 * Adds a 'denormalized table' to this repository.
	 *
	 * @param schema The named schema in which the table belongs (or null).
	 * @param catalog The named catalog in which the table belongs (or null).
	 * @param name The table name
	 * @param isAbstract Is the table abstract (i.e. not really existing in the DB)?
	 * @param subselect A select statement wwich defines a logical table, much
	 * like a DB view.
	 * @param includedTable ???
	 * @return The created table metadata.
	 * @throws DuplicateMappingException If such a table mapping already exists.
	 */
	public Table addDenormalizedTable(String schema, String catalog, String name, boolean isAbstract, String subselect, Table includedTable)
			throws DuplicateMappingException;

	/**
	 * Get named query metadata by name.
	 *
	 * @param name The named query name
	 * @return The query metadata, or null.
	 */
	public NamedQueryDefinition getQuery(String name);

	/**
	 * Adds metadata for a named query to this repository.
	 *
	 * @param name The name
	 * @param query The metadata
	 * @throws DuplicateMappingException If a query already exists with that name.
	 */
	public void addQuery(String name, NamedQueryDefinition query) throws DuplicateMappingException;

	/**
	 * Get named SQL query metadata.
	 *
	 * @param name The named SQL query name.
	 * @return The meatdata, or null if none found.
	 */
	public NamedSQLQueryDefinition getSQLQuery(String name);

	/**
	 * Adds metadata for a named SQL query to this repository.
	 *
	 * @param name The name
	 * @param query The metadata
	 * @throws DuplicateMappingException If a query already exists with that name.
	 */
	public void addSQLQuery(String name, NamedSQLQueryDefinition query) throws DuplicateMappingException;

	/**
	 * Get the metadata for a named SQL result set mapping.
	 *
	 * @param name The mapping name.
	 * @return The SQL result set mapping metadat, or null if none found.
	 */
	public ResultSetMappingDefinition getResultSetMapping(String name);

	/**
	 * Adds the metadata for a named SQL result set mapping to this repository.
	 *
	 * @param sqlResultSetMapping The metadata
	 * @throws DuplicateMappingException If metadata for another SQL result mapping was
	 * already found under the given name.
	 */
	public void addResultSetMapping(ResultSetMappingDefinition sqlResultSetMapping) throws DuplicateMappingException;

	/**
	 * Retrieve a type definition by name.
	 *
	 * @param typeName The name of the type definition to retrieve.
	 * @return The type definition, or null if none found.
	 */
	public TypeDef getTypeDef(String typeName);

	/**
	 * Adds a type definition to this metadata repository.
	 *
	 * @param typeName The type name.
	 * @param typeClass The class implementing the {@link org.hibernate.type.Type} contract.
	 * @param paramMap Map of parameters to be used to configure the type after instantiation.
	 */
	public void addTypeDef(String typeName, String typeClass, Properties paramMap);

	/**
	 * Retrieves the copmplete map of filter definitions.
	 *
	 * @return The filter definition map.
	 */
	public Map getFilterDefinitions();

	/**
	 * Retrieves a filter definition by name.
	 *
	 * @param name The name of the filter defintion to retrieve.
	 * @return The filter definition, or null.
	 */
	public FilterDefinition getFilterDefinition(String name);

	/**
	 * Adds a filter definition to this repository.
	 *
	 * @param definition The filter definition to add.
	 */
	public void addFilterDefinition(FilterDefinition definition);

	/**
	 * Retrieves a fetch profile by either finding one currently in this repository matching the given name
	 * or by creating one (and adding it).
	 *
	 * @param name The name of the profile.
	 * @return The fetch profile metadata.
	 */
	public FetchProfile findOrCreateFetchProfile(String name);

	/**
	 * Retrieves an iterator over the metadata pertaining to all auxilary database objects int this repository.
	 *
	 * @return Iterator over the auxilary database object metadata.
	 */
	public Iterator iterateAuxliaryDatabaseObjects();

	/**
	 * Same as {@link #iterateAuxliaryDatabaseObjects()} except that here the iterator is reversed.
	 *
	 * @return The reversed iterator.
	 */
	public ListIterator iterateAuxliaryDatabaseObjectsInReverse();

	/**
	 * Add metadata pertaining to an auxilary database object to this repository.
	 *
	 * @param auxiliaryDatabaseObject The metadata.
	 */
	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject);

	/**
	 * Get the logical table name mapped for the given physical table.
	 *
	 * @param table The table for which to determine the logical name.
	 * @return The logical name.
	 * @throws MappingException Indicates that no logical name was bound for the given physical table.
	 */
	public String getLogicalTableName(Table table) throws MappingException;

	/**
	 * Adds a table binding to this repository.
	 *
	 * @param schema The schema in which the table belongs (may be null).
	 * @param catalog The catalog in which the table belongs (may be null).
	 * @param logicalName The logical table name.
	 * @param physicalName The physical table name.
	 * @param denormalizedSuperTable ???
	 * @throws DuplicateMappingException Indicates physical table was already bound to another logical name.
	 */
	public void addTableBinding(
			String schema,
			String catalog,
			String logicalName,
			String physicalName,
			Table denormalizedSuperTable) throws DuplicateMappingException;

	/**
	 * Binds the given 'physicalColumn' to the give 'logicalName' within the given 'table'.
	 *
	 * @param logicalName The logical column name binding.
	 * @param physicalColumn The physical column metadata.
	 * @param table The table metadata.
	 * @throws DuplicateMappingException Indicates a duplicate binding for either the physical column name
	 * or the logical column name.
	 */
	public void addColumnBinding(String logicalName, Column physicalColumn, Table table) throws DuplicateMappingException;

	/**
	 * Find the physical column name for the given logical column name within the given table.
	 *
	 * @param logicalName The logical name binding.
	 * @param table The table metatdata.
	 * @return The physical column name.
	 * @throws MappingException Indicates that no such binding was found.
	 */
	public String getPhysicalColumnName(String logicalName, Table table) throws MappingException;

	/**
	 * Find the logical column name against whcih the given physical column name was bound within the given table.
	 *
	 * @param physicalName The physical column name
	 * @param table The table metadata.
	 * @return The logical column name.
	 * @throws MappingException Indicates that no such binding was found.
	 */
	public String getLogicalColumnName(String physicalName, Table table) throws MappingException;

	/**
	 * Adds a second-pass to the end of the current queue.
	 *
	 * @param sp The second pass to add.
	 */
	public void addSecondPass(SecondPass sp);

	/**
	 * Adds a second pass.
	 * @param sp The second pass to add.
	 * @param onTopOfTheQueue True to add to the beginning of the queue; false to add to the end.
	 */
	public void addSecondPass(SecondPass sp, boolean onTopOfTheQueue);

	/**
	 * Represents a property-ref mapping.
	 * <p/>
	 * TODO : currently needs to be exposed because Configuration needs access to it for second-pass processing
	 */
	public static final class PropertyReference implements Serializable {
		public final String referencedClass;
		public final String propertyName;
		public final boolean unique;

		public PropertyReference(String referencedClass, String propertyName, boolean unique) {
			this.referencedClass = referencedClass;
			this.propertyName = propertyName;
			this.unique = unique;
		}
	}

	/**
	 * Adds a property reference binding to this repository.
	 *
	 * @param referencedClass The referenced entity name.
	 * @param propertyName The referenced property name.
	 */
	public void addPropertyReference(String referencedClass, String propertyName);

	/**
	 * Adds a property reference binding to this repository where said proeprty reference is marked as unique.
	 *
	 * @param referencedClass The referenced entity name.
	 * @param propertyName The referenced property name.
	 */
	public void addUniquePropertyReference(String referencedClass, String propertyName);

	/**
	 * Adds an entry to the extends queue queue.
	 *
	 * @param entry The entry to add.
	 */
	public void addToExtendsQueue(ExtendsQueueEntry entry);

	/**
	 * Retrieve the IdentifierGeneratorFactory in effect for this mapping.
	 *
	 * @return The IdentifierGeneratorFactory
	 */
	public DefaultIdentifierGeneratorFactory getIdentifierGeneratorFactory();

	/**
	 * add a new MappedSuperclass
	 * This should not be called if the MappedSuperclass already exists
	 * (it would be erased)
	 * @param type type corresponding to the Mappedsuperclass
	 * @param mappedSuperclass MappedSuperclass
	 */
	public void addMappedSuperclass(Class type, org.hibernate.mapping.MappedSuperclass mappedSuperclass);

	/**
	 * Get a MappedSuperclass or null if not mapped
	 *
	 * @param type class corresponding to the MappedSuperclass
	 * @return the MappedSuperclass
	 */
	org.hibernate.mapping.MappedSuperclass getMappedSuperclass(Class type);

	/**
	 * Retrieve the database identifier normalizer for this context.
	 *
	 * @return The normalizer.
	 */
	public ObjectNameNormalizer getObjectNameNormalizer();

	/**
	 * Retrieve the configuration properties currently in effect.
	 *
	 * @return The configuration properties
	 */
	public Properties getConfigurationProperties();
}