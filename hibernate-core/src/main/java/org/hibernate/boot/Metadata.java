/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.SessionFactory;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.model.query.spi.NamedHqlQueryDefinition;
import org.hibernate.boot.model.query.spi.NamedNativeQueryDefinition;
import org.hibernate.boot.model.query.spi.NamedQueryDefinition;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.model.resultset.spi.ResultSetMappingDefinition;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.cfg.annotations.NamedProcedureCallDefinition;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;

/**
 * Represents the ORM boot-time model as determined from all provided mapping sources.
 *
 * NOTE : for the time being this is essentially a copy of the legacy Mappings contract, split between
 * reading the mapping information exposed here and collecting it via InFlightMetadataCollector
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface Metadata {
	/**
	 * Get the builder for {@link org.hibernate.SessionFactory} instances based on this metamodel,
	 *
	 * @return The builder for {@link org.hibernate.SessionFactory} instances.
	 */
	SessionFactoryBuilder getSessionFactoryBuilder();

	/**
	 * Short-hand form of building a {@link org.hibernate.SessionFactory} through the builder without any additional
	 * option overrides.
	 *
	 * @return THe built SessionFactory.
	 */
	SessionFactory buildSessionFactory();

	/**
	 * Gets the {@link java.util.UUID} for this metamodel.
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

	java.util.Collection<EntityMappingHierarchy> getEntityHierarchies();

	/**
	 * Retrieves the PersistentClass entity metadata representation for known all entities.
	 * <p>
	 * Returned collection is immutable
	 *
	 * @return All PersistentClass representations.
	 *
	 * @deprecated Use {@link #getEntityMappings} instead.  Or depending on usage,
	 * {@link #getEntityHierarchies} is usually more appropriate
	 */
	@Deprecated
	java.util.Collection<PersistentClass> getEntityBindings();

	/**
	 * Retrieves the EntityMapping metadata representation for known all
	 * entities. The returned collection is immutable.
	 * <p/>
	 * Note that {@link #getEntityHierarchies} is usually more appropriate
	 *
	 * @return All PersistentClass representations.
	 */
	default java.util.Collection<EntityMapping> getEntityMappings() {
		return getEntityBindings().stream().collect( Collectors.toList() );
	}

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
	 * Retrieves the Collection metadata representation for known all collections.
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
	 * @param name The query name
	 *
	 * @return The named query metadata, or {@code null}.
	 */
	NamedHqlQueryDefinition getNamedHqlQueryDefinition(String name);

	java.util.Collection<NamedHqlQueryDefinition> getNamedHqlQueryDefinitions();

	/**
	 * Retrieve named query metadata by name.
	 *
	 * @param name The query name
	 *
	 * @return The named query metadata, or {@code null}.
	 *
	 * @deprecated Use {@link #getNamedHqlQueryDefinition(String)} instead
	 */
	@Deprecated
	default NamedQueryDefinition getNamedQueryDefinition(String name) {
		return getNamedHqlQueryDefinition( name );
	}

	/**
	 * @deprecated Use {@link #getNamedHqlQueryDefinitions} instead
	 */
	@Deprecated
	default java.util.Collection<NamedQueryDefinition> getNamedQueryDefinitions() {
		return new ArrayList<>( getNamedHqlQueryDefinitions() );
	}

	/**
	 * Retrieve named SQL query metadata.
	 *
	 * @param name The SQL query name.
	 *
	 * @return The named query metadata, or {@code null}
	 */
	NamedNativeQueryDefinition getNamedNativeQueryDefinition(String name);

	java.util.Collection<NamedNativeQueryDefinition> getNamedNativeQueryDefinitions();

	java.util.Collection<NamedProcedureCallDefinition> getNamedProcedureCallDefinitions();

	/**
	 * Retrieve the metadata for a named SQL result set mapping.
	 *
	 * @param name The mapping name.
	 *
	 * @return The named result set mapping metadata, or {@code null} if none found.
	 */
	ResultSetMappingDefinition getResultSetMapping(String name);

	Map<String, ResultSetMappingDefinition> getResultSetMappingDefinitions();

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
	java.util.Collection<MappedTable> collectMappedTableMappings();

	Map<String,SqmFunctionTemplate> getSqlFunctionMap();
}
