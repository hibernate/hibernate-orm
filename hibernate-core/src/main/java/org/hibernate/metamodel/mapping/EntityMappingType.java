/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.metamodel.model.domain.NavigableRole;

import jakarta.persistence.Entity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.jpa.spi.EntityCallbacks;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.internal.EntityRepresentationStrategyMap;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.persister.entity.EntityNameUse;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBase;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.ast.spi.query.from.TableReferenceJoin;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.String.format;

/**
 * Mapping of an entity
 *
 * @see jakarta.persistence.Entity
 * @see jakarta.persistence.metamodel.EntityType
 *
 * @author Steve Ebersole
 */
public interface EntityMappingType
		extends ManagedMappingType, EntityValuedModelPart, Loadable, Restrictable, Discriminable,
		SoftDeletableModelPart {

	@Nonnull
	@Override
	NavigableRole getNavigableRole();

	/**
	 * The entity name.
	 * <p>
	 * For most entities, this will be the fully-qualified name
	 * of the entity class.  The alternative is an explicit
	 * {@linkplain org.hibernate.boot.jaxb.mapping.spi.JaxbEntity#getName() entity-name} which takes precedence if provided
	 *
	 * @apiNote Different from {@link Entity#name()}, which is just a glorified
	 * SQM "import" name
	 */
	@Nonnull
	String getEntityName();

	/**
	 * Describes how the entity is represented in the application's domain model.
	 */
	@Nonnull
	default EntityRepresentationStrategy getRepresentationStrategy() {
		return getEntityPersister().getRepresentationStrategy();
	}

	/**
	 * Details for the table this entity maps.  Generally this is the
	 * same as {@link #getIdentifierTableDetails()}, though may be different
	 * for subtypes in {@linkplain jakarta.persistence.InheritanceType#JOINED joined}
	 * and{@linkplain jakarta.persistence.InheritanceType#TABLE_PER_CLASS union}
	 * inheritance hierarchies
	 *
	 * @see #getIdentifierTableDetails
	 * @see #forEachTableDetails
	 */
	@Nonnull
	TableDetails getMappedTableDetails();

	/**
	 * Details for the table that defines the identifier column(s)
	 * for an entity hierarchy.
	 *
	 * @see #forEachTableDetails
	 */
	@Nonnull
	TableDetails getIdentifierTableDetails();

	/**
	 * Access to the Jakarta Persistence style callbacks for this entity.
	 */
	@Nonnull
	default EntityCallbacks<Object> getEntityCallbacks() {
		return getEntityPersister().getEntityCallbacks();
	}

	/**
	 * Visit details for each table associated with the entity.
	 */
	void forEachTableDetails(@Nonnull Consumer<TableDetails> consumer);

	@Nonnull
	@Override
	default EntityMappingType findContainingEntityMapping() {
		return this;
	}

	@Nonnull
	@Override
	default JavaType<?> getJavaType() {
		return getMappedJavaType();
	}

	@Nonnull
	@Override
	default EntityMappingType asEntityMappingType() {
		return this;
	}

	@Nonnull
	@Override
	default MappingType getPartMappingType() {
		return this;
	}

	/**
	 * Visit each "query space" for the mapped entity.
	 *
	 * @apiNote "Query space" is simply the table expressions to
	 * which the entity is mapped; the name is historical.
	 */
	void visitQuerySpaces(@Nonnull Consumer<String> querySpaceConsumer);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Make sure we don't run into possible stack overflows

	@Nullable
	@Override
	default ModelPart findSubPart(@Nonnull String name) {
		return findSubPart( name, null );
	}

	@Nullable
	default ModelPart findSubTypesSubPart(@Nonnull String name, @Nullable EntityMappingType treatTargetType) {
		return findSubPart( name, treatTargetType );
	}

	@Override
	default int getJdbcTypeCount() {
		return forEachJdbcType( (index, jdbcMapping) -> {} );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Inheritance

	/**
	 * Whether this entity is defined as abstract using the Java {@code abstract} keyword
	 */
	default boolean isAbstract() {
		return getEntityPersister().isAbstract();
	}

	/**
	 * Whether this entity mapping has any subtype mappings
	 */
	default boolean hasSubclasses() {
		return getEntityPersister().hasSubclasses();
	}

	/**
	 * The mapping for the entity which is the supertype for this entity
	 * mapping.
	 *
	 * @return The supertype mapping, or {@code null} if there is no
	 * supertype
	 *
	 * @apiNote This need not be the direct superclass of the entity as it
	 * is driven by mapping.
	 */
	@Nullable
	default EntityMappingType getSuperMappingType() {
		return null;
	}

	/**
	 * Get the name of the entity that is the "super class" for this entity
	 *
	 * @see #getSuperMappingType
	 */
	@Nullable
	default String getMappedSuperclass() {
		final var superMappingType = getSuperMappingType();
		return superMappingType == null ? null : superMappingType.getEntityName();
	}

	/**
	 * Retrieve mappings for all subtypes
	 */
	@Nonnull
	default Collection<EntityMappingType> getSubMappingTypes() {
		final var mappingMetamodel = getEntityPersister().getFactory().getMappingMetamodel();
		final var subclassEntityNames = getSubclassEntityNames();
		final List<EntityMappingType> mappingTypes = new ArrayList<>( subclassEntityNames.size() );
		for ( String subclassEntityName : subclassEntityNames ) {
			mappingTypes.add( mappingMetamodel.getEntityDescriptor( subclassEntityName ) );
		}
		return mappingTypes;
	}

	/**
	 * Whether the passed entity mapping is the same as or is a supertype of
	 * this entity mapping
	 */
	default boolean isTypeOrSuperType(@Nullable EntityMappingType targetType) {
		return targetType == this;
	}

	/**
	 * Whether the passed mapping is (1) an entity mapping and (2) the same as or
	 * a supertype of this entity mapping
	 *
	 * @see #isTypeOrSuperType(EntityMappingType)
	 */
	default boolean isTypeOrSuperType(@Nullable ManagedMappingType targetType) {
		if ( targetType instanceof EntityMappingType entityMappingType ) {
			return isTypeOrSuperType( entityMappingType );
		}

		return false;
	}

	/**
	 * A value that uniquely identifies an entity mapping relative to its
	 * inheritance hierarchy
	 */
	default int getSubclassId() {
		return getEntityPersister().getSubclassId();
	}

	@Nonnull
	default Set<String> getSubclassEntityNames() {
		return getEntityPersister().getSubclassEntityNames();
	}

	/**
	 * The discriminator value which indicates this entity mapping
	 */
	@Nullable
	DiscriminatorValue getDiscriminatorValue();

	@Nullable
	default String getDiscriminatorSQLValue() {
		final var discriminatorValue = getDiscriminatorValue();
		if ( discriminatorValue instanceof DiscriminatorValue.Literal literal ) {
			return String.valueOf( literal.value() );
		}
		else if ( discriminatorValue instanceof DiscriminatorValue.Special special ) {
			return switch ( special ) {
				case NULL -> "null";
				case NOT_NULL -> throw new IllegalStateException( "Illegal call for NOT_NULL discriminator" );
			};
		}
		else {
			throw new AssertionFailure( "Unrecognized DiscriminatorValue" );
		}
	}

	@Nonnull
	default EntityMappingType getRootEntityDescriptor() {
		final var superMappingType = getSuperMappingType();
		return superMappingType == null
				? this
				: superMappingType.getRootEntityDescriptor();
	}

	/**
	 * Adapts the table group and its table reference as well as table reference joins
	 * in a way such that unnecessary tables or joins are omitted if possible,
	 * based on the given treated entity names.
	 * <p>
	 * The goal is to e.g. remove join inheritance "branches" or union selects that are impossible.
	 * <p>
	 * Consider the following example:
	 * <code>
	 * class BaseEntity {}
	 * class Sub1 extends BaseEntity {}
	 * class Sub1Sub1 extends Sub1 {}
	 * class Sub1Sub2 extends Sub1 {}
	 * class Sub2 extends BaseEntity {}
	 * class Sub2Sub1 extends Sub2 {}
	 * class Sub2Sub2 extends Sub2 {}
	 * </code>
	 * <p>
	 * If the <code>treatedEntityNames</code> only contains <code>Sub1</code> or any of its subtypes,
	 * this means that <code>Sub2</code> and all subtypes are impossible,
	 * thus the joins/selects for these types shall be omitted in the given table group.
	 *
	 * @param tableGroup The table group to prune subclass tables for
	 * @param entityNameUses The entity names under which a table group was used.
	 */
	default void pruneForSubclasses(@Nonnull TableGroup tableGroup, @Nonnull Map<String, EntityNameUse> entityNameUses) {
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Special model parts - identifier, discriminator, etc

	/**
	 * Mapping details for the entity's identifier.  This is shared across all
	 * entity mappings within an inheritance hierarchy.
	 */
	@Nonnull
	EntityIdentifierMapping getIdentifierMapping();

	/**
	 * Mapping details for the entity's identifier.  This is shared across all
	 * entity mappings within an inheritance hierarchy.
	 */
	@Nonnull
	default EntityIdentifierMapping getIdentifierMappingForJoin() {
		return getIdentifierMapping();
	}

	/**
	 * Mapping details for the entity's discriminator.  This is shared across all
	 * entity mappings within an inheritance hierarchy.
	 */
	@Nullable
	EntityDiscriminatorMapping getDiscriminatorMapping();

	/**
	 * Returns {@code true} if this entity type's hierarchy is configured to return
	 * {@linkplain ConcreteProxy concrete-typed} proxies.
	 *
	 * @see ConcreteProxy
	 * @since 6.6
	 */
	@Incubating(since = "5.4")
	default boolean isConcreteProxy() {
		return false;
	}

	/**
	 * If this entity is configured to return {@linkplain ConcreteProxy concrete-typed}
	 * proxies, this method queries the entity table(s) do determine the concrete entity type
	 * associated with the provided id and returns its persister. Otherwise, this method
	 * simply returns this entity persister.
	 *
	 * @see #isConcreteProxy()
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "5.4")
	default EntityMappingType resolveConcreteProxyTypeForId(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		return this;
	}

	/**
	 * Mapping details for the entity's version when using the
	 * {@linkplain OptimisticLockStyle#VERSION version strategy}.
	 * This is shared across all entity mappings within an inheritance
	 * hierarchy.
	 *
	 * @return The version mapping, or null if the entity is (1) defined
	 * with a strategy other than {@link OptimisticLockStyle#VERSION} or
	 * (2) defined without optimistic locking
	 *
	 * @see #optimisticLockStyle
	 */
	@Nullable
	EntityVersionMapping getVersionMapping();

	/**
	 * Tenant metadata, or {@code null} for entities without a tenant id.
	 */
	@Nullable
	default TenantIdMapping getTenantIdMapping() {
		return null;
	}

	/**
	 * The type of optimistic locking, if any, defined for this entity mapping
	 */
	@Nonnull
	default OptimisticLockStyle optimisticLockStyle() {
		return OptimisticLockStyle.NONE;
	}

	/**
	 * The mapping for the natural-id of the entity, if one is defined
	 */
	@Nullable NaturalIdMapping getNaturalIdMapping();

	@Nonnull
	default NaturalIdMapping requireNaturalIdMapping() {
		final var mapping = getNaturalIdMapping();
		if ( mapping == null ) {
			throw new HibernateException( format( "Entity %s does not specify a natural id", getEntityName() ) );
		}
		return mapping;
	}

	/**
	 * The mapping for the row-id of the entity, if one is defined.
	 */
	@Nullable
	EntityRowIdMapping getRowIdMapping();

	/**
	 * Mapping for soft-delete support, or {@code null} if soft-delete not defined
	 */
	@Nullable
	@Incubating(since = "5.4")
	default SoftDeleteMapping getSoftDeleteMapping() {
		return null;
	}

	/**
	 * Mapping for temporal entity support, or {@code null} if not defined.
	 */
	@Nullable
	@Incubating(since = "5.4")
	default TemporalMapping getTemporalMapping() {
		return null;
	}

	/**
	 * Mapping for audit support, or {@code null} if not defined.
	 */
	@Nullable
	@Incubating(since = "5.4")
	default AuditMapping getAuditMapping() {
		return null;
	}

	@Nullable
	default AuxiliaryMapping getAuxiliaryMapping() {
		return null;
	}

	@Nonnull
	@Override
	default TableDetails getSoftDeleteTableDetails() {
		return getIdentifierTableDetails();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Attribute mappings

	/**
	 * The total number of attributes for this entity, including those
	 * declared on supertype mappings
	 */
	@Override
	default int getNumberOfAttributeMappings() {
		return getEntityPersister().getNumberOfAttributeMappings();
	}

	/**
	 * The attributes mapping for this entity, including those
	 * declared on supertype mappings
	 */
	@Nonnull
	@Override
	default AttributeMappingsList getAttributeMappings() {
		return getEntityPersister().getAttributeMappings();
	}

	/**
	 * Visit each {@linkplain #getAttributeMappings() attribute mapping}
	 *
	 * @see #getAttributeMappings()
	 */
	@Override
	default void forEachAttributeMapping(@Nonnull Consumer<? super AttributeMapping> action) {
		getAttributeMappings().forEach( action );
	}

	/**
	 * Retrieve an attribute mapping by position, relative to
	 * {@linkplain #getAttributeMappings() all attributes}
	 */
	@Nonnull
	@Override
	default AttributeMapping getAttributeMapping(int position) {
		return getEntityPersister().getAttributeMapping( position );
	}

	/**
	 * Find an attribute-mapping, declared on this entity mapping (not super or
	 * subs), by name
	 */
	@Nullable
	AttributeMapping findDeclaredAttributeMapping(@Nonnull String name);

	/**
	 * Get the number of attributes defined on this entity mapping - do not access
	 * attributes defined on the super
	 */
	default int getNumberOfDeclaredAttributeMappings() {
		return getDeclaredAttributeMappings().size();
	}

	/**
	 * Get access to the attributes defined on this class - do not access attributes defined on the super
	 */
	@Nonnull
	AttributeMappingsMap getDeclaredAttributeMappings();

	/**
	 * Visit attributes defined on this class - do not visit attributes defined on the super
	 */
	void visitDeclaredAttributeMappings(@Nonnull Consumer<? super AttributeMapping> action);

	/**
	 * Visit the mappings, but limited to just attributes defined
	 * in the targetType or its super-type(s) if any.
	 */
	default void visitAttributeMappings(@Nonnull Consumer<? super AttributeMapping> action) {
		getAttributeMappings().forEach( action );
	}

	/**
	 * Walk this type's attributes as well as its subtypes
	 */
	default void visitSubTypeAttributeMappings(@Nonnull Consumer<? super AttributeMapping> action) {
		// by default do nothing
	}

	/**
	 * Walk this type's attributes as well as its super-type's
	 */
	default void visitSuperTypeAttributeMappings(@Nonnull Consumer<? super AttributeMapping> action) {
		// by default do nothing
	}

	void visitConstraintOrderedTables(@Nonnull ConstraintOrderedTableConsumer consumer);

	@Nonnull
	default String getImportedName() {
		return getEntityPersister().getImportedName();
	}

	@Nonnull
	default RootGraphImplementor<?> createRootGraph(@Nonnull SharedSessionContractImplementor session) {
		final var factory = session.getSessionFactory();
		return getRepresentationStrategy() instanceof EntityRepresentationStrategyMap
				? factory.createGraphForDynamicEntity( getEntityName() )
				: factory.createEntityGraph( getMappedJavaType().getJavaTypeClass() );
	}

	interface ConstraintOrderedTableConsumer {
		void consume(@Nonnull String tableExpression, @Nonnull Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Loading


	/**
	 * Access to performing natural-id database selection.  This is per-entity in the hierarchy
	 */
	@Nonnull
	NaturalIdLoader<?> getNaturalIdLoader();

	/**
	 * Access to performing multi-value natural-id database selection.  This is per-entity in the hierarchy
	 */
	@Nonnull
	MultiNaturalIdLoader<?> getMultiNaturalIdLoader();

	/**
	 * Load an instance of the persistent class, by a unique key other
	 * than the primary key.
	 */
	@Nullable
	Object loadByUniqueKey(@Nonnull String propertyName, @Nonnull Object uniqueKey, @Nonnull SharedSessionContractImplementor session);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Loadable

	@Override
	default boolean isAffectedByEnabledFilters(@Nonnull LoadQueryInfluencers influencers, boolean onlyApplyForLoadByKeyFilters) {
		return getEntityPersister().isAffectedByEnabledFilters( influencers, onlyApplyForLoadByKeyFilters );
	}

	@Override
	default boolean isAffectedByEntityGraph(@Nonnull LoadQueryInfluencers influencers) {
		return getEntityPersister().isAffectedByEntityGraph( influencers );
	}

	@Override
	default boolean isAffectedByEnabledFetchProfiles(@Nonnull LoadQueryInfluencers influencers) {
		return getEntityPersister().isAffectedByEnabledFetchProfiles( influencers );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SQM handling

	@Nullable
	default SqmMultiTableMutationStrategy getSqmMultiTableMutationStrategy(){
		return getEntityPersister().getSqmMultiTableMutationStrategy();
	}

	@Nullable
	default SqmMultiTableInsertStrategy getSqmMultiTableInsertStrategy() {
		return getEntityPersister().getSqmMultiTableInsertStrategy();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SQL AST generation

	@Nonnull
	@Override
	default String getSqlAliasStem() {
		return getEntityPersister().getSqlAliasStem();
	}

	@Nonnull
	@Override
	default TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			@Nonnull NavigablePath navigablePath,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			@Nullable SqlAstCreationState creationState) {
		return getEntityPersister().createRootTableGroup(
				canUseInnerJoins,
				navigablePath,
				explicitSourceAlias,
				explicitSqlAliasBase,
				additionalPredicateCollectorAccess,
				creationState
		);
	}

	@Nonnull
	default TableReference createPrimaryTableReference(
			@Nonnull SqlAliasBase sqlAliasBase,
			@Nonnull SqlAstCreationState creationState) {
		throw new UnsupportedMappingException(
				"Entity mapping does not support primary TableReference creation [" +
						getClass().getName() + " : " + getEntityName() + "]"
		);
	}

	@Nullable
	default TableReferenceJoin createTableReferenceJoin(
			@Nonnull String joinTableExpression,
			@Nonnull SqlAliasBase sqlAliasBase,
			@Nonnull TableReference lhs,
			@Nonnull SqlAstCreationState creationState) {
		throw new UnsupportedMappingException(
				"Entity mapping does not support primary TableReference join creation [" +
						getClass().getName() + " : " + getEntityName() + "]"
		);
	}

	@Nonnull
	@Override
	default JavaType<?> getMappedJavaType() {
		return getEntityPersister().getMappedJavaType();
	}

	@Override
	default int getNumberOfFetchables() {
		return getEntityPersister().getNumberOfFetchables();
	}

	@Nonnull
	@Override
	default Fetchable getFetchable(int position) {
		return getEntityPersister().getFetchable( position );
	}

	@Override
	default void applyDiscriminator(
			@Nullable Consumer<Predicate> predicateConsumer,
			@Nullable String alias,
			@Nonnull TableGroup tableGroup,
			@Nonnull SqlAstCreationState creationState) {
		getEntityPersister().applyDiscriminator( predicateConsumer, alias, tableGroup, creationState );
	}

	@Override
	default void applyFilterRestrictions(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nonnull Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			@Nullable SqlAstCreationState creationState) {
		getEntityPersister().applyFilterRestrictions(
				predicateConsumer,
				tableGroup,
				useQualifier,
				enabledFilters,
				onlyApplyLoadByKeyFilters,
				creationState
		);
	}

	@Override
	default void applyBaseRestrictions(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nonnull Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			@Nullable Set<String> treatAsDeclarations,
			@Nullable SqlAstCreationState creationState) {
		getEntityPersister().applyBaseRestrictions(
				predicateConsumer,
				tableGroup,
				useQualifier,
				enabledFilters,
				onlyApplyLoadByKeyFilters,
				treatAsDeclarations,
				creationState
		);
	}

	@Override
	default boolean hasWhereRestrictions() {
		return getEntityPersister().hasWhereRestrictions();
	}

	@Override
	default void applyWhereRestrictions(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nullable SqlAstCreationState creationState) {
		getEntityPersister().applyWhereRestrictions( predicateConsumer, tableGroup, useQualifier, creationState );
	}

	/**
	 * Safety-net.
	 */
	// todo (6.0) : look to remove need for this.  at the very least, move it to an SPI contract
	@Nonnull
	@Internal
	EntityPersister getEntityPersister();

	@Nonnull
	@Override
	default String getPartName() {
		return getEntityName();
	}

	@Nonnull
	@Override
	default String getRootPathName() {
		return getEntityName();
	}
}
