/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.Filter;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.AttributeMappingsMap;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.AttributeMappingsList;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;

/**
 * Mapping of an entity
 *
 * @see jakarta.persistence.Entity
 *
 * @author Steve Ebersole
 */
public interface EntityMappingType extends ManagedMappingType, EntityValuedModelPart, Loadable, Restrictable, Discriminatable {
	/**
	 * Safety-net.
	 *
	 * todo (6.0) : do we really need to expose?
	 */
	EntityPersister getEntityPersister();

	default String getContributor() {
		// todo (6.0) : needed for the HHH-14470 half related to HHH-14469
		return "orm";
	}

	default EntityRepresentationStrategy getRepresentationStrategy() {
		return getEntityPersister().getRepresentationStrategy();
	}

	String getEntityName();

	@Override
	default EntityMappingType findContainingEntityMapping() {
		return this;
	}

	@Override
	default String getPartName() {
		return getEntityName();
	}

	@Override
	default String getRootPathName() {
		return getEntityName();
	}

	@Override
	default JavaType<?> getJavaType() {
		return getMappedJavaType();
	}

	@Override
	default MappingType getPartMappingType() {
		return this;
	}

	void visitQuerySpaces(Consumer<String> querySpaceConsumer);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Make sure we don't run into possible stack overflows

	@Override
	default ModelPart findSubPart(String name) {
		return findSubPart( name, null );
	}

	@Override
	ModelPart findSubPart(String name, EntityMappingType targetType);

	@Override
	void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType targetType);

	@Override
	<T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState);

	@Override
	void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState);

	@Override
	void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection,JdbcMapping> selectionConsumer);

	@Override
	default int getJdbcTypeCount() {
		return forEachJdbcType( (index, jdbcMapping) -> {} );
	}

	@Override
	int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action);

	@Override
	Object disassemble(Object value, SharedSessionContractImplementor session);

	@Override
	int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session);

	@Override
	default int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer consumer,
			SharedSessionContractImplementor session) {
		return forEachDisassembledJdbcValue( disassemble( value, session ), clause, offset, consumer, session );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Inheritance

	default int getSubclassId() {
		return getEntityPersister().getEntityMetamodel().getSubclassId();
	}

	default boolean hasSubclasses() {
		return getEntityPersister().getEntityMetamodel().hasSubclasses();
	}

	default Set<String> getSubclassEntityNames() {
		return getEntityPersister().getEntityMetamodel().getSubclassEntityNames();
	}

	AttributeMapping findDeclaredAttributeMapping(String name);

	/**
	 * Get the number of attributes defined on this class - do not access attributes defined on the super
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

	default EntityMappingType getSuperMappingType() {
		return null;
	}

	default Collection<EntityMappingType> getSubMappingTypes() {
		final MappingMetamodelImplementor mappingMetamodel = getEntityPersister().getFactory().getMappingMetamodel();
		final Set<String> subclassEntityNames = getSubclassEntityNames();
		final List<EntityMappingType> mappingTypes = new ArrayList<>( subclassEntityNames.size() );
		for ( String subclassEntityName : subclassEntityNames ) {
			mappingTypes.add( mappingMetamodel.getEntityDescriptor( subclassEntityName ) );
		}
		return mappingTypes;
	}

	default boolean isTypeOrSuperType(EntityMappingType targetType) {
		return targetType == this;
	}

	default boolean isTypeOrSuperType(ManagedMappingType targetType) {
		if ( targetType instanceof EntityMappingType ) {
			return isTypeOrSuperType( (EntityMappingType) targetType );
		}

		return false;
	}

	default SqmMultiTableMutationStrategy getSqmMultiTableMutationStrategy(){
		return getEntityPersister().getSqmMultiTableMutationStrategy();
	}

	default SqmMultiTableInsertStrategy getSqmMultiTableInsertStrategy() {
		return getEntityPersister().getSqmMultiTableInsertStrategy();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Special model parts - identifier, discriminator, etc

	EntityIdentifierMapping getIdentifierMapping();

	EntityDiscriminatorMapping getDiscriminatorMapping();

	Object getDiscriminatorValue();

	String getSubclassForDiscriminatorValue(Object value);

	EntityVersionMapping getVersionMapping();

	NaturalIdMapping getNaturalIdMapping();

	EntityRowIdMapping getRowIdMapping();

	/**
	 * Visit the mappings, but limited to just attributes defined
	 * in the targetType or its super-type(s) if any.
	 *
	 * @apiNote Passing {@code null} indicates that subclasses should be included.  This
	 * matches legacy non-TREAT behavior and meets the need for EntityGraph processing
	 */
	default void visitAttributeMappings(Consumer<? super AttributeMapping> action, EntityMappingType targetType) {
		getAttributeMappings().forEachAttributeMapping( action );
	}

	/**
	 * Walk this type's attributes as well as its sub-type's
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
	 *
	 * The goal is to e.g. remove join inheritance "branches" or union selects that are impossible.
	 *
	 * Consider the following example:
	 * <code>
	 *     class BaseEntity {}
	 *     class Sub1 extends BaseEntity {}
	 *     class Sub1Sub1 extends Sub1 {}
	 *     class Sub1Sub2 extends Sub1 {}
	 *     class Sub2 extends BaseEntity {}
	 *     class Sub2Sub1 extends Sub2 {}
	 *     class Sub2Sub2 extends Sub2 {}
	 * </code>
	 *
	 * If the <code>treatedEntityNames</code> only contains <code>Sub1</code> or any of its subtypes,
	 * this means that <code>Sub2</code> and all subtypes are impossible,
	 * thus the joins/selects for these types shall be omitted in the given table group.
	 *
	 * @param tableGroup The table group to prune subclass tables for
	 * @param treatedEntityNames The entity names for which path usages were registered
	 */
	default void pruneForSubclasses(TableGroup tableGroup, Set<String> treatedEntityNames) {
	}

	default boolean isAbstract() {
		return getEntityPersister().getEntityMetamodel().isAbstract();
	}

	interface ConstraintOrderedTableConsumer {
		void consume(String tableExpression, Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier);
	}


	@Override
	default void visitAttributeMappings(Consumer<? super AttributeMapping> action) {
		getAttributeMappings().forEachAttributeMapping( action );
	}

	// Customer <- DomesticCustomer <- OtherCustomer

	@Deprecated(forRemoval = true)
	default Object[] extractConcreteTypeStateValues(
			Map<AttributeMapping, DomainResultAssembler> assemblerMapping,
			RowProcessingState rowProcessingState) {
		// todo (6.0) : getNumberOfAttributeMappings() needs to be fixed for this to work - bad walking of hierarchy
		final Object[] values = new Object[ getNumberOfAttributeMappings() ];

		visitAttributeMappings(
				attribute -> {
					final DomainResultAssembler assembler = assemblerMapping.get( attribute );
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Loadable

	@Override
	default boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers) {
		return getEntityPersister().isAffectedByEnabledFilters( influencers );
	}

	@Override
	default boolean isAffectedByEntityGraph(LoadQueryInfluencers influencers) {
		return getEntityPersister().isAffectedByEntityGraph( influencers );
	}

	@Override
	default boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers influencers) {
		return getEntityPersister().isAffectedByEnabledFetchProfiles( influencers );
	}

	@Override
	default TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAstCreationState creationState,
			SqlAstCreationContext creationContext) {
		return createRootTableGroup(
				canUseInnerJoins,
				navigablePath,
				explicitSourceAlias,
				additionalPredicateCollectorAccess,
				creationState.getSqlAliasBaseGenerator().createSqlAliasBase( getSqlAliasStem() ),
				creationState.getSqlExpressionResolver(),
				creationState.getFromClauseAccess(),
				creationContext
		);
	}

	@Override
	default TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAliasBase sqlAliasBase,
			SqlExpressionResolver expressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		return getEntityPersister().createRootTableGroup(
				canUseInnerJoins,
				navigablePath,
				explicitSourceAlias,
				additionalPredicateCollectorAccess,
				sqlAliasBase,
				expressionResolver,
				fromClauseAccess,
				creationContext
		);
	}

	default TableReference createPrimaryTableReference(
			SqlAliasBase sqlAliasBase,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		throw new UnsupportedMappingException(
				"Entity mapping does not support primary TableReference creation [" +
						getClass().getName() + " : " + getEntityName() + "]"
		);
	}

	default TableReferenceJoin createTableReferenceJoin(
			String joinTableExpression,
			SqlAliasBase sqlAliasBase,
			TableReference lhs,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		throw new UnsupportedMappingException(
				"Entity mapping does not support primary TableReference join creation [" +
						getClass().getName() + " : " + getEntityName() + "]"
		);
	}

	@Override
	default int getNumberOfAttributeMappings() {
		return getEntityPersister().getNumberOfAttributeMappings();
	}

	@Override
	default AttributeMappingsList getAttributeMappings() {
		return getEntityPersister().getAttributeMappings();
	}

	@Override
	default AttributeMapping getAttributeMapping(int position) {
		return getEntityPersister().getAttributeMapping( position );
	}

	@Override
	default JavaType getMappedJavaType() {
		return getEntityPersister().getMappedJavaType();
	}

	@Override
	default String getSqlAliasStem() {
		return getEntityPersister().getSqlAliasStem();
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
			SqlAstCreationState creationState) {
		getEntityPersister().applyFilterRestrictions( predicateConsumer, tableGroup, useQualifier, enabledFilters, creationState );
	}

	@Override
	default void applyBaseRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			Set<String> treatAsDeclarations,
			SqlAstCreationState creationState) {
		getEntityPersister().applyBaseRestrictions( predicateConsumer, tableGroup, useQualifier, enabledFilters, treatAsDeclarations, creationState );
	}

	@Override
	default void applyWhereRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			SqlAstCreationState creationState) {
		getEntityPersister().applyWhereRestrictions( predicateConsumer, tableGroup, useQualifier, creationState );
	}
}
