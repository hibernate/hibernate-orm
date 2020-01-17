/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;

/**
 * todo (6.0) : make this implement RootTableGroupProducer, etc instead of EntityPersister?
 *
 * todo (6.0) : leverage the "relational model" here?
 *
 * @author Steve Ebersole
 */
public interface EntityMappingType extends ManagedMappingType, Loadable {
	/**
	 * Safety-net.
	 *
	 * todo (6.0) : do we really need to expose?
	 */
	EntityPersister getEntityPersister();

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
	default JavaTypeDescriptor getJavaTypeDescriptor() {
		return getMappedJavaTypeDescriptor();
	}

	@Override
	default MappingType getPartMappingType() {
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Inheritance

	default AttributeMapping findDeclaredAttributeMapping(String name) {
		throw new NotYetImplementedFor6Exception( getClass() );
		// or ?
		//throw new UnsupportedOperationException();
	}

	/**
	 * Get the number of attributes defined on this class - do not access attributes defined on the super
	 */
	default int getNumberOfDeclaredAttributeMappings() {
		return getDeclaredAttributeMappings().size();
	}

	/**
	 * Get access to the attributes defined on this class - do not access attributes defined on the super
	 */
	default Collection<AttributeMapping> getDeclaredAttributeMappings() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Visit attributes defined on this class - do not visit attributes defined on the super
	 */
	default void visitDeclaredAttributeMappings(Consumer<AttributeMapping> action) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	default EntityMappingType getSuperMappingType() {
		return null;
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Special model parts - identifier, discriminator, etc

	EntityIdentifierMapping getIdentifierMapping();

	EntityVersionMapping getVersionMapping();

	default EntityDiscriminatorMapping getDiscriminatorMapping() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	NaturalIdMapping getNaturalIdMapping();

	/**
	 * Visit the mappings, but limited to just attributes defined
	 * in the targetType or its super-type(s) if any.
	 *
	 * @apiNote Passing {@code null} indicates that subclasses should be included.  This
	 * matches legacy non-TREAT behavior and meets the need for EntityGraph processing
	 */
	default void visitAttributeMappings(Consumer<AttributeMapping> action, EntityMappingType targetType) {
		getAttributeMappings().forEach( action );
	}

	/**
	 * Walk this type's attributes as well as its sub-type's
	 */
	default void visitSubTypeAttributeMappings(Consumer<AttributeMapping> action) {
		// by default do nothing
	}

	/**
	 * Walk this type's attributes as well as its super-type's
	 */
	default void visitSuperTypeAttributeMappings(Consumer<AttributeMapping> action) {
		// by default do nothing
	}

	default void visitConstraintOrderedTables(ConstraintOrderedTableConsumer consumer) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	interface ConstraintOrderedTableConsumer {
		void consume(String tableExpression, Supplier<Consumer<ColumnConsumer>> tableKeyColumnVisitationSupplier);
	}


	@Override
	default void visitAttributeMappings(Consumer<AttributeMapping> action) {
		getAttributeMappings().forEach( action );
	}

	// Customer <- DomesticCustomer <- OtherCustomer

	default Object[] extractConcreteTypeStateValues(
			Map<AttributeMapping, DomainResultAssembler> assemblerMapping,
			RowProcessingState rowProcessingState) {
		// todo (6.0) : getNumberOfAttributeMappings() needs to be fixed for this to work - bad walking of hierarchy
		final Object[] values = new Object[ getNumberOfAttributeMappings() ];

		visitStateArrayContributors(
				new Consumer<StateArrayContributorMapping>() {
					private int index;

					@Override
					public void accept(StateArrayContributorMapping attribute) {
						final DomainResultAssembler assembler = assemblerMapping.get( attribute );
						final Object value = assembler == null ? UNFETCHED_PROPERTY : assembler.assemble( rowProcessingState );

						values[index++] = value;

					}
				}
		);

		return values;
	}

	@Override
	default void visitStateArrayContributors(Consumer<StateArrayContributorMapping> mappingConsumer) {
		visitAttributeMappings(
				attributeMapping -> mappingConsumer.accept( (StateArrayContributorMapping) attributeMapping )
		);
	}


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
			NavigablePath navigablePath,
			String explicitSourceAlias,
			boolean canUseInnerJoins,
			LockMode lockMode,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAstCreationContext creationContext) {
		return getEntityPersister().createRootTableGroup(
				navigablePath,
				explicitSourceAlias,
				canUseInnerJoins,
				lockMode,
				aliasBaseGenerator,
				sqlExpressionResolver,
				additionalPredicateCollectorAccess,
				creationContext
		);
	}

	default TableReference createPrimaryTableReference(
			SqlAliasBase sqlAliasBase,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		throw new UnsupportedOperationException(
				"Entity mapping does not support primary TableReference creation [" +
						getClass().getName() + " : " + getEntityName() + "]"
		);
	}

	default TableReferenceJoin createTableReferenceJoin(
			String joinTableExpression,
			SqlAliasBase sqlAliasBase,
			TableReference lhs,
			boolean canUseInnerJoin,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		throw new UnsupportedOperationException(
				"Entity mapping does not support primary TableReference join creation [" +
						getClass().getName() + " : " + getEntityName() + "]"
		);
	}

	@Override
	default int getNumberOfAttributeMappings() {
		return getEntityPersister().getNumberOfAttributeMappings();
	}

	@Override
	default Collection<AttributeMapping> getAttributeMappings() {
		return getEntityPersister().getAttributeMappings();
	}

	@Override
	default JavaTypeDescriptor getMappedJavaTypeDescriptor() {
		return getEntityPersister().getMappedJavaTypeDescriptor();
	}

	@Override
	default String getSqlAliasStem() {
		return getEntityPersister().getSqlAliasStem();
	}

	@Override
	default int getNumberOfFetchables() {
		return getEntityPersister().getNumberOfFetchables();
	}
}
