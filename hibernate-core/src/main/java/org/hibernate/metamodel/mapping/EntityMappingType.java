/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import jakarta.persistence.Entity;
import org.hibernate.Filter;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.mapping.Contributable;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.internal.EntityRepresentationStrategyMap;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityNameUse;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;

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

	/**
	 * The entity name.
	 * <p/>
	 * For most entities, this will be the fully-qualified name
	 * of the entity class.  The alternative is an explicit
	 * {@linkplain org.hibernate.boot.jaxb.mapping.spi.JaxbEntity#getName() entity-name} which takes precedence if provided
	 *
	 * @apiNote Different from {@link Entity#name()}, which is just a glorified
	 * SQM "import" name
	 */
	String getEntityName();

	/**
	 * Describes how the entity is represented in the application's domain model.
	 */
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
	TableDetails getMappedTableDetails();

	/**
	 * Details for the table that defines the identifier column(s)
	 * for an entity hierarchy.
	 *
	 * @see #forEachTableDetails
	 */
	TableDetails getIdentifierTableDetails();

	/**
	 * Visit details for each table associated with the entity.
	 */
	void forEachTableDetails(Consumer<TableDetails> consumer);

	@Override
	default EntityMappingType findContainingEntityMapping() {
		return this;
	}

	@Override
	default JavaType<?> getJavaType() {
		return getMappedJavaType();
	}

	@Override
	default EntityMappingType asEntityMappingType() {
		return this;
	}

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
	void visitQuerySpaces(Consumer<String> querySpaceConsumer);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Make sure we don't run into possible stack overflows

	@Override
	default ModelPart findSubPart(String name) {
		return findSubPart( name, null );
	}

	default ModelPart findSubTypesSubPart(String name, EntityMappingType treatTargetType) {
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
	default EntityMappingType getSuperMappingType() {
		return null;
	}

	/**
	 * Get the name of the entity that is the "super class" for this entity
	 *
	 * @see #getSuperMappingType
	 */
	default String getMappedSuperclass() {
		return getSuperMappingType().getEntityName();
	}

	/**
	 * Retrieve mappings for all subtypes
	 */
	default Collection<EntityMappingType> getSubMappingTypes() {
		final MappingMetamodelImplementor mappingMetamodel = getEntityPersister().getFactory().getMappingMetamodel();
		final Set<String> subclassEntityNames = getSubclassEntityNames();
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
	default boolean isTypeOrSuperType(EntityMappingType targetType) {
		return targetType == this;
	}

	/**
	 * Whether the passed mapping is (1) an entity mapping and (2) the same as or
	 * a supertype of this entity mapping
	 *
	 * @see #isTypeOrSuperType(EntityMappingType)
	 */
	default boolean isTypeOrSuperType(ManagedMappingType targetType) {
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

	default Set<String> getSubclassEntityNames() {
		return getEntityPersister().getSubclassEntityNames();
	}

	/**
	 * Is this class explicit polymorphism only?
	 *
	 * @deprecated No longer supported
	 */
	@Deprecated
	boolean isExplicitPolymorphism();

	/**
	 * The discriminator value which indicates this entity mapping
	 */
	Object getDiscriminatorValue();

	default String getDiscriminatorSQLValue() {
		return getDiscriminatorValue().toString();
	}

	default EntityMappingType getRootEntityDescriptor() {
		final EntityMappingType superMappingType = getSuperMappingType();
		if ( superMappingType == null ) {
			return this;
		}
		return superMappingType.getRootEntityDescriptor();
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
	default void pruneForSubclasses(TableGroup tableGroup, Map<String, EntityNameUse> entityNameUses) {
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Special model parts - identifier, discriminator, etc

	/**
	 * Mapping details for the entity's identifier.  This is shared across all
	 * entity mappings within an inheritance hierarchy.
	 */
	EntityIdentifierMapping getIdentifierMapping();

	/**
	 * Mapping details for the entity's identifier.  This is shared across all
	 * entity mappings within an inheritance hierarchy.
	 */
	default EntityIdentifierMapping getIdentifierMappingForJoin() {
		return getIdentifierMapping();
	}

	/**
	 * Mapping details for the entity's discriminator.  This is shared across all
	 * entity mappings within an inheritance hierarchy.
	 */
	EntityDiscriminatorMapping getDiscriminatorMapping();

	/**
	 * Returns {@code true} if this entity type's hierarchy is configured to return
	 * {@linkplain ConcreteProxy concrete-typed} proxies.
	 *
	 * @see ConcreteProxy
	 * @since 6.6
	 */
	@Incubating
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
	@Incubating
	default EntityMappingType resolveConcreteProxyTypeForId(Object id, SharedSessionContractImplementor session) {
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
	EntityVersionMapping getVersionMapping();

	/**
	 * The type of optimistic locking, if any, defined for this entity mapping
	 */
	default OptimisticLockStyle optimisticLockStyle() {
		return OptimisticLockStyle.NONE;
	}

	/**
	 * The mapping for the natural-id of the entity, if one is defined
	 */
	NaturalIdMapping getNaturalIdMapping();

	/**
	 * The mapping for the row-id of the entity, if one is defined.
	 */
	EntityRowIdMapping getRowIdMapping();

	/**
	 * Mapping for soft-delete support, or {@code null} if soft-delete not defined
	 */
	default SoftDeleteMapping getSoftDeleteMapping() {
		return null;
	}

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
	default void forEachAttributeMapping(Consumer<? super AttributeMapping> action) {
		getAttributeMappings().forEach( action );
	}

	/**
	 * Retrieve an attribute mapping by position, relative to
	 * {@linkplain #getAttributeMappings() all attributes}
	 */
	@Override
	default AttributeMapping getAttributeMapping(int position) {
		return getEntityPersister().getAttributeMapping( position );
	}

	/**
	 * Find an attribute-mapping, declared on this entity mapping (not super or
	 * subs), by name
	 */
	AttributeMapping findDeclaredAttributeMapping(String name);

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
	AttributeMappingsMap getDeclaredAttributeMappings();

	/**
	 * Visit attributes defined on this class - do not visit attributes defined on the super
	 */
	void visitDeclaredAttributeMappings(Consumer<? super AttributeMapping> action);

	/**
	 * Visit the mappings, but limited to just attributes defined
	 * in the targetType or its super-type(s) if any.
	 */
	default void visitAttributeMappings(Consumer<? super AttributeMapping> action) {
		getAttributeMappings().forEach( action );
	}

	/**
	 * Walk this type's attributes as well as its subtypes
	 */
	default void visitSubTypeAttributeMappings(Consumer<? super AttributeMapping> action) {
		// by default do nothing
	}

	/**
	 * Walk this type's attributes as well as its super-type's
	 */
	default void visitSuperTypeAttributeMappings(Consumer<? super AttributeMapping> action) {
		// by default do nothing
	}

	void visitConstraintOrderedTables(ConstraintOrderedTableConsumer consumer);

	default String getImportedName() {
		return getEntityPersister().getImportedName();
	}

	default RootGraphImplementor createRootGraph(SharedSessionContractImplementor session) {
		if ( getRepresentationStrategy() instanceof EntityRepresentationStrategyMap mapRep ) {
			return session.getSessionFactory().createGraphForDynamicEntity( getEntityName() );
		}
		else {
			return session.getSessionFactory().createEntityGraph( getMappedJavaType().getJavaTypeClass() );
		}
	}

	interface ConstraintOrderedTableConsumer {
		void consume(String tableExpression, Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier);
	}

	// Customer <- DomesticCustomer <- OtherCustomer

	@Deprecated(forRemoval = true)
	default Object[] extractConcreteTypeStateValues(
			Map<AttributeMapping, DomainResultAssembler> assemblerMapping,
			RowProcessingState rowProcessingState) {
		// todo (6.0) : getNumberOfAttributeMappings() needs to be fixed for this to work - bad walking of hierarchy
		final Object[] values = new Object[ getNumberOfAttributeMappings() ];

		forEachAttributeMapping(
				attribute -> {
					final DomainResultAssembler<?> assembler = assemblerMapping.get( attribute );
					final Object value;
					if ( assembler == null ) {
						value = UNFETCHED_PROPERTY;
					}
					else {
						value = assembler.assemble( rowProcessingState );
					}

					values[attribute.getStateArrayPosition()] = value;
				}
		);

		return values;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Loading


	/**
	 * Access to performing natural-id database selection.  This is per-entity in the hierarchy
	 */
	NaturalIdLoader<?> getNaturalIdLoader();

	/**
	 * Access to performing multi-value natural-id database selection.  This is per-entity in the hierarchy
	 */
	MultiNaturalIdLoader<?> getMultiNaturalIdLoader();

	/**
	 * Load an instance of the persistent class, by a unique key other
	 * than the primary key.
	 */
	Object loadByUniqueKey(String propertyName, Object uniqueKey, SharedSessionContractImplementor session);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Loadable

	@Override
	default boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers, boolean onlyApplyForLoadByKeyFilters) {
		return getEntityPersister().isAffectedByEnabledFilters( influencers, onlyApplyForLoadByKeyFilters );
	}

	@Override
	default boolean isAffectedByEntityGraph(LoadQueryInfluencers influencers) {
		return getEntityPersister().isAffectedByEntityGraph( influencers );
	}

	@Override
	default boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers influencers) {
		return getEntityPersister().isAffectedByEnabledFetchProfiles( influencers );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SQM handling

	default SqmMultiTableMutationStrategy getSqmMultiTableMutationStrategy(){
		return getEntityPersister().getSqmMultiTableMutationStrategy();
	}

	default SqmMultiTableInsertStrategy getSqmMultiTableInsertStrategy() {
		return getEntityPersister().getSqmMultiTableInsertStrategy();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SQL AST generation

	@Override
	default String getSqlAliasStem() {
		return getEntityPersister().getSqlAliasStem();
	}

	@Override
	default TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			SqlAliasBase explicitSqlAliasBase,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAstCreationState creationState) {
		return getEntityPersister().createRootTableGroup(
				canUseInnerJoins,
				navigablePath,
				explicitSourceAlias,
				explicitSqlAliasBase,
				additionalPredicateCollectorAccess,
				creationState
		);
	}

	default TableReference createPrimaryTableReference(
			SqlAliasBase sqlAliasBase,
			SqlAstCreationState creationState) {
		throw new UnsupportedMappingException(
				"Entity mapping does not support primary TableReference creation [" +
						getClass().getName() + " : " + getEntityName() + "]"
		);
	}

	default TableReferenceJoin createTableReferenceJoin(
			String joinTableExpression,
			SqlAliasBase sqlAliasBase,
			TableReference lhs,
			SqlAstCreationState creationState) {
		throw new UnsupportedMappingException(
				"Entity mapping does not support primary TableReference join creation [" +
						getClass().getName() + " : " + getEntityName() + "]"
		);
	}

	@Override
	default JavaType<?> getMappedJavaType() {
		return getEntityPersister().getMappedJavaType();
	}

	@Override
	default int getNumberOfFetchables() {
		return getEntityPersister().getNumberOfFetchables();
	}

	@Override
	default Fetchable getFetchable(int position) {
		return getEntityPersister().getFetchable( position );
	}

	@Override
	default void applyDiscriminator(
			Consumer<Predicate> predicateConsumer,
			String alias,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		getEntityPersister().applyDiscriminator( predicateConsumer, alias, tableGroup, creationState );
	}

	@Override
	default void applyFilterRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			SqlAstCreationState creationState) {
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
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			Set<String> treatAsDeclarations,
			SqlAstCreationState creationState) {
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
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			SqlAstCreationState creationState) {
		getEntityPersister().applyWhereRestrictions( predicateConsumer, tableGroup, useQualifier, creationState );
	}

	/**
	 * Safety-net.
	 */
	// todo (6.0) : look to remove need for this.  at the very least, move it to an SPI contract
	@Internal
	EntityPersister getEntityPersister();

	/**
	 * @deprecated See {@link Contributable#getContributor()}
	 */
	@Deprecated
	default String getContributor() {
		// todo (6.0) : needed for the HHH-14470 half related to HHH-14469
		return "orm";
	}

	@Override
	default String getPartName() {
		return getEntityName();
	}

	@Override
	default String getRootPathName() {
		return getEntityName();
	}
}
