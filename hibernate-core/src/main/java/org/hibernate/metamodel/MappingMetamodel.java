/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.graph.RootGraph;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.metamodel.Metamodel;

/**
 * Access to information about the runtime relational O/R mapping model.
 *
 * @apiNote This is an incubating SPI. Its name and package may change.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface MappingMetamodel extends Metamodel {
	/**
	 * The {@link TypeConfiguration} this metamodel is associated with
	 */
	TypeConfiguration getTypeConfiguration();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity descriptors

	/**
	 * Visit all entity mapping descriptors defined in the model
	 */
	void forEachEntityDescriptor(Consumer<EntityPersister> action);

	@Deprecated(forRemoval = true, since = "7")
	Stream<EntityPersister> streamEntityDescriptors();

	/**
	 * Get an entity mapping descriptor based on its Hibernate entity-name
	 *
	 * @throws IllegalArgumentException if the name does not refer to an entity
	 *
	 * @see #findEntityDescriptor
	 */
	EntityPersister getEntityDescriptor(String entityName);

	/**
	 * Get an entity mapping descriptor based on its NavigableRole.
	 *
	 * @throws IllegalArgumentException if the name does not refer to an entity
	 *
	 * @see #findEntityDescriptor
	 */
	EntityPersister getEntityDescriptor(NavigableRole name);

	/**
	 * Get an EmbeddableMappingType based on its NavigableRole.
	 *
	 * @throws IllegalArgumentException if the role does not refer to an entity
	 *
	 * @see #findEntityDescriptor
	 */
	EmbeddableValuedModelPart getEmbeddableValuedModelPart(NavigableRole role);


	/**
	 * Get an entity mapping descriptor based on its Class.
	 *
	 * @throws IllegalArgumentException if the name does not refer to an entity
	 *
	 * @see #findEntityDescriptor
	 */
	EntityPersister getEntityDescriptor(Class<?> entityJavaType);

	/**
	 * Find an entity mapping descriptor based on its Hibernate entity-name.
	 *
	 * @apiNote Returns {@code null} rather than throwing exception
	 */
	EntityPersister findEntityDescriptor(String entityName);

	/**
	 * Find an entity mapping descriptor based on its Class.
	 *
	 * @apiNote Returns {@code null} rather than throwing exception
	 */
	EntityPersister findEntityDescriptor(Class<?> entityJavaType);

	boolean isEntityClass(Class<?> entityJavaType);

	/**
	 * Locate an entity mapping descriptor by Class.  The passed Class might
	 * refer to either the entity Class directly, or it might name a proxy
	 * interface for the entity.  This method accounts for both, preferring the
	 * direct entity name.
	 *
	 * @throws org.hibernate.UnknownEntityTypeException If a matching EntityPersister cannot be located
	 *
	 * @deprecated No longer used
	 */
	@Deprecated(forRemoval = true, since = "7")
	EntityPersister locateEntityDescriptor(Class<?> byClass);

	String getImportedName(String name);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Collection descriptors

	/**
	 * Visit the mapping descriptors for all collections defined in the model
	 */
	void forEachCollectionDescriptor(Consumer<CollectionPersister> action);

	@Deprecated(forRemoval = true, since = "7")
	Stream<CollectionPersister> streamCollectionDescriptors();

	/**
	 * Get a collection mapping descriptor based on its role
	 *
	 * @throws IllegalArgumentException if the role does not refer to a collection
	 *
	 * @see #findCollectionDescriptor
	 */
	CollectionPersister getCollectionDescriptor(String role);

	/**
	 * Get a collection mapping descriptor based on its role
	 *
	 * @throws IllegalArgumentException if the role does not refer to a collection
	 *
	 * @see #findCollectionDescriptor
	 */
	CollectionPersister getCollectionDescriptor(NavigableRole role);

	/**
	 * Find a collection mapping descriptor based on its role.  Returns
	 * {@code null} if the role does not refer to a collection
	 *
	 * @see #findCollectionDescriptor
	 */
	CollectionPersister findCollectionDescriptor(NavigableRole role);

	/**
	 * Find a collection mapping descriptor based on its role.  Returns
	 * {@code null} if the role does not refer to a collection
	 *
	 * @see #findCollectionDescriptor
	 */
	CollectionPersister findCollectionDescriptor(String role);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA entity graphs

	RootGraph<?> findNamedGraph(String name);
	void addNamedEntityGraph(String graphName, RootGraph<?> entityGraph);
	void forEachNamedGraph(Consumer<RootGraph<?>> action);
	RootGraph<?> defaultGraph(String entityName);
	RootGraph<?> defaultGraph(Class<?> entityJavaType);
	RootGraph<?> defaultGraph(EntityPersister entityDescriptor);
	RootGraph<?> defaultGraph(EntityDomainType<?> entityDomainType);

	List<RootGraph<?>> findRootGraphsForType(Class<?> baseEntityJavaType);
	List<RootGraph<?>> findRootGraphsForType(String baseEntityName);
	List<RootGraph<?>> findRootGraphsForType(EntityPersister baseEntityDescriptor);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SQM model -> Mapping model

	// TODO Layer breaker used in SQM to SQL translation.
	//      Consider moving to QueryEngine or collaborators.
	@Internal
	MappingModelExpressible<?> resolveMappingExpressible(
			SqmExpressible<?> sqmExpressible,
			Function<NavigablePath, TableGroup> tableGroupLocator);
}
