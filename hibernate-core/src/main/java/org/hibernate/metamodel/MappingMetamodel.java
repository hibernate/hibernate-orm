package org.hibernate.metamodel;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.action.queue.internal.constraint.ConstraintModel;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.spi.SqmExpressible;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.metamodel.Metamodel;

/**
 * Access to information about the runtime relational O/R mapping model.
 *
 * @apiNote This is an incubating SPI. Its name and package may change.
 *
 * @author Steve Ebersole
 */
@Incubating(since = "5.4")
@org.hibernate.SPI(org.hibernate.SPI.Role.USE)
public interface MappingMetamodel extends Metamodel {
	/**
	 * The {@link TypeConfiguration} this metamodel is associated with
	 */
	@Nonnull
	TypeConfiguration getTypeConfiguration();

	/**
	 * Access to constraint details from the domain model.
	 */
	@Nonnull
	ConstraintModel getConstraintModel();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity descriptors

	/**
	 * Visit all entity mapping descriptors defined in the model
	 */
	void forEachEntityDescriptor(@Nonnull Consumer<EntityPersister> action);

	/**
	 * Get an entity mapping descriptor based on its Hibernate entity-name
	 *
	 * @throws IllegalArgumentException if the name does not refer to an entity
	 *
	 * @see #findEntityDescriptor
	 */
	@Nonnull
	EntityPersister getEntityDescriptor(@Nonnull String entityName);

	/**
	 * Get an entity mapping descriptor based on its NavigableRole.
	 *
	 * @throws IllegalArgumentException if the name does not refer to an entity
	 *
	 * @see #findEntityDescriptor
	 */
	@Nonnull
	EntityPersister getEntityDescriptor(@Nonnull NavigableRole name);

	/**
	 * Get an EmbeddableMappingType based on its NavigableRole.
	 *
	 * @throws IllegalArgumentException if the role does not refer to an entity
	 *
	 * @see #findEntityDescriptor
	 */
	@org.hibernate.SPI(org.hibernate.SPI.Role.USE)
	@Nonnull
	EmbeddableValuedModelPart getEmbeddableValuedModelPart(@Nonnull NavigableRole role);


	/**
	 * Get an entity mapping descriptor based on its Class.
	 *
	 * @throws IllegalArgumentException if the class is not an entity class
	 *
	 * @see #findEntityDescriptor
	 */
	@Nonnull
	EntityPersister getEntityDescriptor(@Nonnull Class<?> entityJavaType);

	/**
	 * Find an entity mapping descriptor based on its Hibernate entity name.
	 *
	 * @apiNote Returns {@code null} rather than throwing exception
	 */
	@Nullable
	EntityPersister findEntityDescriptor(@Nonnull String entityName);

	/**
	 * Find an entity mapping descriptor based on its Class.
	 *
	 * @apiNote Returns {@code null} rather than throwing exception
	 */
	@Nullable
	EntityPersister findEntityDescriptor(@Nonnull Class<?> entityJavaType);

	boolean isEntityClass(@Nonnull Class<?> entityJavaType);

	@Nonnull
	String getImportedName(@Nonnull String name);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Collection descriptors

	/**
	 * Visit the mapping descriptors for all collections defined in the model
	 */
	void forEachCollectionDescriptor(@Nonnull Consumer<CollectionPersister> action);

	/**
	 * Get a collection mapping descriptor based on its role
	 *
	 * @throws IllegalArgumentException if the role does not refer to a collection
	 *
	 * @see #findCollectionDescriptor
	 */
	@Nonnull
	CollectionPersister getCollectionDescriptor(@Nonnull String role);

	/**
	 * Get a collection mapping descriptor based on its role
	 *
	 * @throws IllegalArgumentException if the role does not refer to a collection
	 *
	 * @see #findCollectionDescriptor
	 */
	@Nonnull
	CollectionPersister getCollectionDescriptor(@Nonnull NavigableRole role);

	/**
	 * Find a collection mapping descriptor based on its role.  Returns
	 * {@code null} if the role does not refer to a collection
	 *
	 * @see #findCollectionDescriptor
	 */
	@Nullable
	CollectionPersister findCollectionDescriptor(@Nonnull NavigableRole role);

	/**
	 * Find a collection mapping descriptor based on its role.  Returns
	 * {@code null} if the role does not refer to a collection
	 *
	 * @see #findCollectionDescriptor
	 */
	@Nullable
	CollectionPersister findCollectionDescriptor(@Nonnull String role);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SQM model -> Mapping model

	// TODO Layer breaker used in SQM to SQL translation.
	//      Consider moving to QueryEngine or collaborators.
	@Internal
	@Nullable
	MappingModelExpressible<?> resolveMappingExpressible(
			@Nullable SqmExpressible<?> sqmExpressible,
			@Nonnull Function<NavigablePath, TableGroup> tableGroupLocator);
}
