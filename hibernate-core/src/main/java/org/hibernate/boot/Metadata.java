package org.hibernate.boot;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Remove;
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
	@Remove
	@Nonnull
	SessionFactoryBuilder getSessionFactoryBuilder();

	/**
	 * Shorthand form of building a {@link SessionFactory} through the builder without any additional
	 * option overrides.
	 *
	 * @return THe built SessionFactory.
	 */
	@Remove
	@Nonnull
	SessionFactory buildSessionFactory();

	/**
	 * Gets the {@link UUID} for this metamodel.
	 *
	 * @return the UUID, or {@code null} while the metadata is being collected.
	 */
	@Nullable
	UUID getUUID();

	/**
	 * Retrieve the database model.
	 *
	 * @return The database model.
	 */
	@Nonnull
	Database getDatabase();

	/**
	 * Retrieves the PersistentClass entity metadata representation for all known entities.
	 *
	 * Returned collection is immutable
	 *
	 * @return All PersistentClass representations.
	 */
	@Nonnull
	java.util.Collection<PersistentClass> getEntityBindings();

	/**
	 * Retrieves the PersistentClass entity mapping metadata representation for
	 * the given entity name.
	 *
	 * @param entityName The entity name for which to retrieve the metadata.
	 *
	 * @return The entity mapping metadata, or {@code null} if no matching entity found.
	 */
	@Nullable
	PersistentClass getEntityBinding(@Nonnull String entityName);

	/**
	 * Retrieves the Collection metadata representation for all known collections.
	 *
	 * Returned collection is immutable
	 *
	 * @return All Collection representations.
	 */
	@Nonnull
	java.util.Collection<Collection> getCollectionBindings();

	/**
	 * Retrieves the collection mapping metadata for the given collection role.
	 *
	 * @param role The collection role for which to retrieve the metadata.
	 *
	 * @return The collection mapping metadata, or {@code null} if no matching collection found.
	 */
	@Nullable
	Collection getCollectionBinding(@Nonnull String role);

	/**
	 * Retrieves all defined imports (class renames).
	 *
	 * @return All imports
	 */
	@Nonnull
	Map<String,String> getImports();

	/**
	 * Retrieve named query metadata by name.
	 *
	 * @return The named query metadata, or {@code null}.
	 */
	@Nullable
	NamedHqlQueryDefinition<?> getNamedHqlQueryMapping(@Nonnull String name);

	/**
	 * Visit all named HQL query definitions
	 */
	void visitNamedHqlQueryDefinitions(@Nonnull Consumer<NamedHqlQueryDefinition<?>> definitionConsumer);

	/**
	 * Retrieve named SQL query metadata.
	 *
	 * @return The named query metadata, or {@code null}
	 */
	@Nullable
	NamedNativeQueryDefinition<?> getNamedNativeQueryMapping(@Nonnull String name);

	/**
	 * Visit all named native query definitions
	 */
	void visitNamedNativeQueryDefinitions(@Nonnull Consumer<NamedNativeQueryDefinition<?>> definitionConsumer);

	/**
	 * Retrieve named procedure metadata.
	 *
	 * @return The named procedure metadata, or {@code null}
	 */
	@Nullable
	NamedProcedureCallDefinition getNamedProcedureCallMapping(@Nonnull String name);

	/**
	 * Visit all named callable query definitions
	 */
	void visitNamedProcedureCallDefinition(@Nonnull Consumer<NamedProcedureCallDefinition> definitionConsumer);

	/**
	 * Retrieve the metadata for a named SQL result set mapping.
	 *
	 * @param name The mapping name.
	 *
	 * @return The named result set mapping metadata, or {@code null} if none found.
	 */
	@Nullable
	NamedResultSetMappingDescriptor getResultSetMapping(@Nonnull String name);

	/**
	 * Visit all named SQL result set mapping definitions
	 */
	void visitNamedResultSetMappingDefinition(@Nonnull Consumer<NamedResultSetMappingDescriptor> definitionConsumer);

	/**
	 * Retrieve a type definition by name.
	 *
	 * @return The named type definition, or {@code null}
	 */
	@Nullable
	TypeDefinition getTypeDefinition(@Nonnull String typeName);

	/**
	 * Retrieves the complete map of filter definitions.
	 *
	 * Returned map is immutable
	 *
	 * @return The filter definition map.
	 */
	@Nonnull
	Map<String,FilterDefinition> getFilterDefinitions();

	/**
	 * Retrieves a filter definition by name.
	 *
	 * @param name The name of the filter definition to retrieve
	 * .
	 * @return The filter definition, or {@code null}.
	 */
	@Nullable
	FilterDefinition getFilterDefinition(@Nonnull String name);

	@Nullable
	FetchProfile getFetchProfile(@Nonnull String name);

	@Nonnull
	java.util.Collection<FetchProfile> getFetchProfiles();

	@Nullable
	NamedEntityGraphDefinition getNamedEntityGraph(@Nonnull String name);

	@Nonnull
	Map<String, NamedEntityGraphDefinition> getNamedEntityGraphs();

	@Remove
	@Nullable
	IdentifierGeneratorDefinition getIdentifierGenerator(@Nonnull String name);

	@Nonnull
	java.util.Collection<Table> collectTableMappings();

	@Nonnull
	Map<String, SqmFunctionDescriptor> getSqlFunctionMap();

	/**
	 * All of the known model contributors
	 */
	@Nonnull
	Set<String> getContributors();
}
