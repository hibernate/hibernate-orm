/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.hibernate.SessionFactory;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.type.MappingContext;

/**
 * Represents the ORM model as determined by aggregating the provided mapping sources.
 * An instance may be obtained by calling {@link MetadataSources#buildMetadata()}.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface Metadata extends MappingContext {
	/**
	 * Get the builder for {@link SessionFactory} instances based on this metamodel.
	 *
	 * @return The builder for {@link SessionFactory} instances.
	 */
	SessionFactoryBuilder getSessionFactoryBuilder();

	/**
	 * Short-hand form of building a {@link SessionFactory} through the builder without any additional
	 * option overrides.
	 *
	 * @return THe built SessionFactory.
	 */
	SessionFactory buildSessionFactory();

	/**
	 * Gets the {@link UUID} for this metamodel.
	 *
	 * @return the UUID.
	 */
	UUID getUUID();

	/**
	 * Retrieve the database model.
	 *
	 * @return The database model.
	 */
	Database getDatabase();

	/**
	 * Retrieves the PersistentClass entity metadata representation for all known entities.
	 *
	 * Returned collection is immutable
	 *
	 * @return All PersistentClass representations.
	 */
	java.util.Collection<PersistentClass> getEntityBindings();

	/**
	 * Retrieves the PersistentClass entity mapping metadata representation for
	 * the given entity name.
	 *
	 * @param entityName The entity name for which to retrieve the metadata.
	 *
	 * @return The entity mapping metadata, or {@code null} if no matching entity found.
	 */
	PersistentClass getEntityBinding(String entityName);

	/**
	 * Retrieves the Collection metadata representation for all known collections.
	 *
	 * Returned collection is immutable
	 *
	 * @return All Collection representations.
	 */
	java.util.Collection<Collection> getCollectionBindings();

	/**
	 * Retrieves the collection mapping metadata for the given collection role.
	 *
	 * @param role The collection role for which to retrieve the metadata.
	 *
	 * @return The collection mapping metadata, or {@code null} if no matching collection found.
	 */
	Collection getCollectionBinding(String role);

	/**
	 * Retrieves all defined imports (class renames).
	 *
	 * @return All imports
	 */
	Map<String,String> getImports();

	/**
	 * Retrieve named query metadata by name.
	 *
	 * @return The named query metadata, or {@code null}.
	 */
	NamedHqlQueryDefinition<?> getNamedHqlQueryMapping(String name);

	/**
	 * Visit all named HQL query definitions
	 */
	void visitNamedHqlQueryDefinitions(Consumer<NamedHqlQueryDefinition<?>> definitionConsumer);

	/**
	 * Retrieve named SQL query metadata.
	 *
	 * @return The named query metadata, or {@code null}
	 */
	NamedNativeQueryDefinition<?> getNamedNativeQueryMapping(String name);

	/**
	 * Visit all named native query definitions
	 */
	void visitNamedNativeQueryDefinitions(Consumer<NamedNativeQueryDefinition<?>> definitionConsumer);

	/**
	 * Retrieve named procedure metadata.
	 *
	 * @return The named procedure metadata, or {@code null}
	 */
	NamedProcedureCallDefinition getNamedProcedureCallMapping(String name);

	/**
	 * Visit all named callable query definitions
	 */
	void visitNamedProcedureCallDefinition(Consumer<NamedProcedureCallDefinition> definitionConsumer);

	/**
	 * Retrieve the metadata for a named SQL result set mapping.
	 *
	 * @param name The mapping name.
	 *
	 * @return The named result set mapping metadata, or {@code null} if none found.
	 */
	NamedResultSetMappingDescriptor getResultSetMapping(String name);

	/**
	 * Visit all named SQL result set mapping definitions
	 */
	void visitNamedResultSetMappingDefinition(Consumer<NamedResultSetMappingDescriptor> definitionConsumer);

	/**
	 * Retrieve a type definition by name.
	 *
	 * @return The named type definition, or {@code null}
	 */
	TypeDefinition getTypeDefinition(String typeName);

	/**
	 * Retrieves the complete map of filter definitions.
	 *
	 * Returned map is immutable
	 *
	 * @return The filter definition map.
	 */
	Map<String,FilterDefinition> getFilterDefinitions();

	/**
	 * Retrieves a filter definition by name.
	 *
	 * @param name The name of the filter definition to retrieve
	 * .
	 * @return The filter definition, or {@code null}.
	 */
	FilterDefinition getFilterDefinition(String name);

	FetchProfile getFetchProfile(String name);

	java.util.Collection<FetchProfile> getFetchProfiles();

	NamedEntityGraphDefinition getNamedEntityGraph(String name);

	Map<String, NamedEntityGraphDefinition> getNamedEntityGraphs();

	IdentifierGeneratorDefinition getIdentifierGenerator(String name);

	java.util.Collection<Table> collectTableMappings();

	Map<String, SqmFunctionDescriptor> getSqlFunctionMap();

	/**
	 * All of the known model contributors
	 */
	Set<String> getContributors();
}
