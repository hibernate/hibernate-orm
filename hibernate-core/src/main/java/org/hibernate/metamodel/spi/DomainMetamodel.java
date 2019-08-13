/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.spi;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.graph.RootGraph;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.mapping.spi.ValueMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Access to information about Hibernate's runtime type system
 *
 * @author Steve Ebersole
 */
@Incubating
public interface DomainMetamodel {
	/**
	 * Access to the JPA metamodel sub-set of the overall run-time metamodel
	 *
	 * @apiNote The distinction is mainly used in building SQM trees, which rely
	 * on the JPA type subset
	 */
	JpaMetamodel getJpaMetamodel();

	/**
	 * The TypeConfiguration this metamodel is associated with
	 */
	default TypeConfiguration getTypeConfiguration() {
		return getJpaMetamodel().getTypeConfiguration();
	}

	/**
	 * todo (6.0) : POC!!!  Intended for use in SQM -> SQL translation
	 *
	 * @param sqmExpressable
	 * @return
	 */
	default ValueMapping resolveValueMapping(SqmExpressable<?> sqmExpressable) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Given a Java type, determine the corresponding AllowableParameterType to
	 * use implicitly
	 */
	default <T> AllowableParameterType<T> resolveQueryParameterType(Class<T> javaType) {
		final BasicType basicType = getTypeConfiguration().getBasicTypeForJavaType( javaType );
		if ( basicType != null ) {
			//noinspection unchecked
			return basicType;
		}

		final ManagedDomainType<T> managedType = getJpaMetamodel().findManagedType( javaType );
		if ( managedType instanceof AllowableParameterType ) {
			//noinspection unchecked
			return (AllowableParameterType) managedType;
		}

		return null;
	}

	/**
	 * Given an (assumed) entity instance, determine its descriptor
	 *
	 * @see org.hibernate.EntityNameResolver
	 */
	EntityPersister determineEntityPersister(Object entity);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity descriptors

	/**
	 * Visit all entity mapping descriptors defined in the model
	 */
	void visitEntityDescriptors(Consumer<EntityPersister> action);

	/**
	 * Given a JPA entity domain type, get the associated Hibernate entity descriptor
	 */
	EntityPersister resolveEntityDescriptor(EntityDomainType<?> entityDomainType);

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
	 * Get an entity mapping descriptor based on its Class.
	 *
	 * @throws IllegalArgumentException if the name does not refer to an entity
	 *
	 * @see #findEntityDescriptor
	 */
	EntityPersister getEntityDescriptor(Class entityJavaType);

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
	EntityPersister findEntityDescriptor(Class entityJavaType);

	/**
	 * Locate an entity mapping descriptor by Class.  The passed Class might
	 * refer to either the entity Class directly, or it might name a proxy
	 * interface for the entity.  This method accounts for both, preferring the
	 * direct entity name.
	 *
	 * @throws org.hibernate.UnknownEntityTypeException If a matching EntityPersister cannot be located
	 */
	EntityPersister locateEntityDescriptor(Class byClass);

	/**
	 * @see #locateEntityDescriptor
	 *
	 * @deprecated (since 6.0) use {@link #locateEntityDescriptor(Class)} instead
	 */
	@Deprecated
	default EntityPersister locateEntityPersister(Class byClass) {
		return locateEntityDescriptor( byClass );
	}

	/**
	 * Locate the entity persister by name.
	 *
	 * @return The located EntityPersister, never {@code null}
	 *
	 * @throws org.hibernate.UnknownEntityTypeException If a matching EntityPersister cannot be located
	 *
	 * @deprecated (since 6.0) - use {@link #getEntityDescriptor(String)} instead
	 */
	@Deprecated
	EntityPersister locateEntityPersister(String byName);

	String getImportedName(String name);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Collection descriptors

	/**
	 * Visit the mapping descriptors for all collections defined in the model
	 */
	void visitCollectionDescriptors(Consumer<CollectionPersister> action);

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
	void visitNamedGraphs(Consumer<RootGraph<?>> action);

	RootGraph<?> defaultGraph(String entityName);
	RootGraph<?> defaultGraph(Class entityJavaType);
	RootGraph<?> defaultGraph(EntityPersister entityDescriptor);
	RootGraph<?> defaultGraph(EntityDomainType<?> entityDomainType);

	List<RootGraph<?>> findRootGraphsForType(Class baseEntityJavaType);
	List<RootGraph<?>> findRootGraphsForType(String baseEntityName);
	List<RootGraph<?>> findRootGraphsForType(EntityPersister baseEntityDescriptor);
}
