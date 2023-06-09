/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaCteCriteriaAttribute;
import org.hibernate.query.criteria.JpaSearchOrder;
import org.hibernate.query.sqm.tree.SqmQuery;
import org.hibernate.query.sqm.tree.cte.SqmCteTableColumn;
import org.hibernate.query.sqm.tree.cte.SqmSearchClauseSpecification;
import org.hibernate.query.sqm.tree.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedCrossJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedEntityJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedListJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedPluralPartJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedRoot;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedRootJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedSingularJoin;
import org.hibernate.query.sqm.tree.domain.SqmCteRoot;
import org.hibernate.query.sqm.tree.domain.SqmDerivedRoot;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.expression.SqmAny;
import org.hibernate.query.sqm.tree.expression.SqmEvery;
import org.hibernate.query.sqm.tree.from.SqmCteJoin;
import org.hibernate.query.sqm.tree.from.SqmDerivedJoin;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.select.SqmSelectQuery;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.internal.SqmDmlCreationProcessingState;
import org.hibernate.query.sqm.internal.SqmQueryPartCreationProcessingStateStandardImpl;
import org.hibernate.query.sqm.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.cte.SqmCteContainer;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralPartJoin;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmEmptinessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmGroupedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmJunctionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmMemberOfPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNegatedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.criteria.AbstractQuery;

/**
 * Handles splitting queries containing unmapped polymorphic references.
 *
 * @author Steve Ebersole
 */
public class QuerySplitter {

	public static <R> SqmSelectStatement<R>[] split(
			SqmSelectStatement<R> statement,
			SessionFactoryImplementor sessionFactory) {
		// We only allow unmapped polymorphism in a very restricted way.  Specifically,
		// the unmapped polymorphic reference can only be a root and can be the only
		// root.  Use that restriction to locate the unmapped polymorphic reference
		final SqmRoot<?> unmappedPolymorphicReference = findUnmappedPolymorphicReference( statement.getQueryPart() );

		if ( unmappedPolymorphicReference == null ) {
			return new SqmSelectStatement[] { statement };
		}

		final SqmPolymorphicRootDescriptor<?> unmappedPolymorphicDescriptor = (SqmPolymorphicRootDescriptor<?>) unmappedPolymorphicReference.getReferencedPathSource();
		final SqmSelectStatement<R>[] expanded = new SqmSelectStatement[ unmappedPolymorphicDescriptor.getImplementors().size() ];

		int i = -1;
		for ( EntityDomainType<?> mappedDescriptor : unmappedPolymorphicDescriptor.getImplementors() ) {
			i++;
			final UnmappedPolymorphismReplacer<R> replacer = new UnmappedPolymorphismReplacer<>(
					unmappedPolymorphicReference,
					mappedDescriptor,
					sessionFactory
			);
			expanded[i] = replacer.visitSelectStatement( statement );
		}

		return expanded;
	}

	private static SqmRoot<?> findUnmappedPolymorphicReference(SqmQueryPart<?> queryPart) {
		if ( queryPart instanceof SqmQuerySpec<?> ) {
			return ( (SqmQuerySpec<?>) queryPart ).getRoots()
					.stream()
					.filter( sqmRoot -> sqmRoot.getReferencedPathSource() instanceof SqmPolymorphicRootDescriptor )
					.findFirst()
					.orElse( null );
		}
		else {
			final SqmQueryGroup<?> queryGroup = (SqmQueryGroup<?>) queryPart;
			final SqmRoot<?> root = findUnmappedPolymorphicReference( queryGroup.getQueryParts().get( 0 ) );
			if ( root != null ) {
				throw new UnsupportedOperationException( "Polymorphic query group is unsupported" );
			}
			return null;
		}
	}

	public static <R> SqmDeleteStatement<R>[] split(
			SqmDeleteStatement<R> statement,
			SessionFactoryImplementor sessionFactory) {
		// We only allow unmapped polymorphism in a very restricted way.  Specifically,
		// the unmapped polymorphic reference can only be a root and can be the only
		// root.  Use that restriction to locate the unmapped polymorphic reference
		final SqmRoot<?> unmappedPolymorphicReference = findUnmappedPolymorphicReference( statement );

		if ( unmappedPolymorphicReference == null ) {
			return new SqmDeleteStatement[] { statement };
		}

		final SqmPolymorphicRootDescriptor<?> unmappedPolymorphicDescriptor = (SqmPolymorphicRootDescriptor<?>) unmappedPolymorphicReference.getReferencedPathSource();
		final SqmDeleteStatement<R>[] expanded = new SqmDeleteStatement[ unmappedPolymorphicDescriptor.getImplementors().size() ];

		int i = -1;
		for ( EntityDomainType<?> mappedDescriptor : unmappedPolymorphicDescriptor.getImplementors() ) {
			i++;
			final UnmappedPolymorphismReplacer<R> replacer = new UnmappedPolymorphismReplacer<>(
					unmappedPolymorphicReference,
					mappedDescriptor,
					sessionFactory
			);
			expanded[i] = replacer.visitDeleteStatement( statement );
		}

		return expanded;
	}

	private static SqmRoot<?> findUnmappedPolymorphicReference(SqmDeleteOrUpdateStatement<?> queryPart) {
		if ( queryPart.getTarget().getReferencedPathSource() instanceof SqmPolymorphicRootDescriptor<?> ) {
			return queryPart.getTarget();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static class UnmappedPolymorphismReplacer<R> extends BaseSemanticQueryWalker implements SqmCreationState {
		private final SqmRoot unmappedPolymorphicFromElement;
		private final EntityDomainType<R> mappedDescriptor;
		private final SqmCreationContext creationContext;
		private final Stack<SqmCreationProcessingState> processingStateStack = new StandardStack<>( SqmCreationProcessingState.class );

		private final Map<NavigablePath, SqmPath> sqmPathCopyMap = new HashMap<>();
		private final Map<SqmFrom, SqmFrom> sqmFromCopyMap = new HashMap<>();

		private UnmappedPolymorphismReplacer(
				SqmRoot unmappedPolymorphicFromElement,
				EntityDomainType mappedDescriptor,
				SessionFactoryImplementor sessionFactory) {
			this.unmappedPolymorphicFromElement = unmappedPolymorphicFromElement;
			this.mappedDescriptor = mappedDescriptor;
			this.creationContext = sessionFactory;
		}

		@Override
		public SqmInsertSelectStatement<R> visitInsertSelectStatement(SqmInsertSelectStatement<?> statement) {
			throw new UnsupportedOperationException( "Not valid" );
		}

		@Override
		public SqmUpdateStatement<R> visitUpdateStatement(SqmUpdateStatement<?> statement) {
			throw new UnsupportedOperationException( "Not valid" );
		}

		@Override
		public SqmSetClause visitSetClause(SqmSetClause setClause) {
			throw new UnsupportedOperationException( "Not valid" );
		}

		@Override
		public SqmAssignment<?> visitAssignment(SqmAssignment<?> assignment) {
			throw new UnsupportedOperationException( "Not valid" );
		}

		@Override
		public Object visitCteContainer(SqmCteContainer consumer) {
			final SqmCteContainer processingQuery = (SqmCteContainer) getProcessingStateStack().getCurrent()
					.getProcessingQuery();
			for ( SqmCteStatement<?> cteStatement : consumer.getCteStatements() ) {
				visitCteStatement( cteStatement );
			}
			return processingQuery;
		}

		@Override
		public SqmCteStatement<?> visitCteStatement(SqmCteStatement<?> sqmCteStatement) {
			final SqmCteContainer processingQuery = (SqmCteContainer) getProcessingStateStack().getCurrent()
					.getProcessingQuery();
			final SqmSelectQuery<?> cteDefinition = sqmCteStatement.getCteDefinition();
			final SqmQueryPart<?> queryPart = cteDefinition.getQueryPart();
			JpaCteCriteria<?> cteCriteria = null;
			if ( cteDefinition.getCteStatements().isEmpty()
					&& queryPart instanceof SqmQueryGroup<?> && queryPart.getSortSpecifications().isEmpty()
					&& queryPart.getFetchExpression() == null && queryPart.getOffsetExpression() == null ) {
				final SqmQueryGroup<?> queryGroup = (SqmQueryGroup<?>) queryPart;
				boolean unionDistinct = false;
				switch ( queryGroup.getSetOperator() ) {
					case UNION:
						unionDistinct = true;
					case UNION_ALL:
						if ( queryGroup.getQueryParts().size() == 2 ) {
							final SqmSelectQuery<Object> nonRecursiveStatement = visitSelectQuery(
									cteDefinition,
									queryGroup.getQueryParts().get( 0 )
							);
							final Function<JpaCteCriteria<Object>, AbstractQuery<Object>> recursiveCriteriaProducer = jpaCteCriteria -> {
								return visitSelectQuery( cteDefinition, queryGroup.getQueryParts().get( 1 ) );
							};
							if ( sqmCteStatement.getName() == null ) {
								if ( unionDistinct ) {
									cteCriteria = processingQuery.withRecursiveUnionDistinct(
											nonRecursiveStatement,
											recursiveCriteriaProducer
									);
								}
								else {
									cteCriteria = processingQuery.withRecursiveUnionAll(
											nonRecursiveStatement,
											recursiveCriteriaProducer
									);
								}
							}
							else {
								if ( unionDistinct ) {
									cteCriteria = processingQuery.withRecursiveUnionDistinct(
											sqmCteStatement.getName(),
											nonRecursiveStatement,
											recursiveCriteriaProducer
									);
								}
								else {
									cteCriteria = processingQuery.withRecursiveUnionAll(
											sqmCteStatement.getName(),
											nonRecursiveStatement,
											recursiveCriteriaProducer
									);
								}
							}

							if ( sqmCteStatement.getSearchClauseKind() != null ) {
								final List<JpaSearchOrder> searchBySpecifications = sqmCteStatement.getSearchBySpecifications();
								final List<JpaSearchOrder> newSearchBySpecifications = new ArrayList<>( searchBySpecifications.size() );
								for ( JpaSearchOrder searchBySpecification : searchBySpecifications ) {
									newSearchBySpecifications.add(
											new SqmSearchClauseSpecification(
													(SqmCteTableColumn) cteCriteria.getType().getAttribute( searchBySpecification.getAttribute().getName() ),
													searchBySpecification.getSortOrder(),
													searchBySpecification.getNullPrecedence()
											)
									);
								}
								cteCriteria.search(
										sqmCteStatement.getSearchClauseKind(),
										sqmCteStatement.getSearchAttributeName(),
										newSearchBySpecifications
								);
							}
							if ( sqmCteStatement.getCycleMarkAttributeName() != null ) {
								final List<JpaCteCriteriaAttribute> cycleAttributes = sqmCteStatement.getCycleAttributes();
								final List<JpaCteCriteriaAttribute> newCycleAttributes = new ArrayList<>( cycleAttributes.size() );
								for ( JpaCteCriteriaAttribute cycleAttribute : cycleAttributes ) {
									newCycleAttributes.add(
											cteCriteria.getType().getAttribute( cycleAttribute.getName() )
									);
								}
								cteCriteria.cycleUsing(
										sqmCteStatement.getCycleMarkAttributeName(),
										sqmCteStatement.getCyclePathAttributeName(),
										sqmCteStatement.getCycleValue(),
										sqmCteStatement.getNoCycleValue(),
										newCycleAttributes
								);
							}
						}
				}
			}
			if ( cteCriteria == null ) {
				if ( sqmCteStatement.getName() == null ) {
					cteCriteria = processingQuery.with( visitSelectQuery( cteDefinition ) );
				}
				else {
					cteCriteria = processingQuery.with( sqmCteStatement.getName(), visitSelectQuery( cteDefinition ) );
				}
			}
			if ( sqmCteStatement.getMaterialization() != null ) {
				cteCriteria.setMaterialization( sqmCteStatement.getMaterialization() );
			}
			return (SqmCteStatement<?>) cteCriteria;
		}

		@Override
		public SqmCteStatement<?> findCteStatement(final String name) {
			return processingStateStack.findCurrentFirstWithParameter( name, UnmappedPolymorphismReplacer::matchCteStatement );
		}

		private static SqmCteStatement<?> matchCteStatement(final SqmCreationProcessingState state, final String cteName) {
			final SqmQuery<?> processingQuery = state.getProcessingQuery();
			if ( processingQuery instanceof SqmCteContainer ) {
				final SqmCteContainer container = (SqmCteContainer) processingQuery;
				return container.getCteStatement( cteName );
			}
			return null;
		}

		@Override
		public SqmDeleteStatement<R> visitDeleteStatement(SqmDeleteStatement<?> statement) {
			final SqmRoot<?> sqmRoot = statement.getTarget();
			final SqmRoot<R> copy = new SqmRoot<>(
					mappedDescriptor,
					sqmRoot.getExplicitAlias(),
					sqmRoot.isAllowJoins(),
					sqmRoot.nodeBuilder()
			);
			sqmFromCopyMap.put( sqmRoot, copy );
			sqmPathCopyMap.put( sqmRoot.getNavigablePath(), copy );
			final SqmDeleteStatement<R> statementCopy = new SqmDeleteStatement<>(
					copy,
					statement.getQuerySource(),
					statement.nodeBuilder()
			);

			processingStateStack.push(
					new SqmDmlCreationProcessingState(
							statementCopy,
							this
					)
			);
			getProcessingStateStack().getCurrent().getPathRegistry().register( copy );
			try {
				visitCteContainer( statement );
				statementCopy.setWhereClause( visitWhereClause( statement.getWhereClause() ) );
			}
			finally {
				processingStateStack.pop();
			}

			return statementCopy;
		}

		@Override
		public SqmSelectStatement<R> visitSelectStatement(SqmSelectStatement<?> statement) {
			final SqmSelectStatement<R> copy = new SqmSelectStatement<>( statement.nodeBuilder() );

			processingStateStack.push(
					new SqmQueryPartCreationProcessingStateStandardImpl(
							processingStateStack.getCurrent(),
							copy,
							this
					)
			);
			try {
				visitCteContainer( statement );
				copy.setQueryPart( visitQueryPart( statement.getQueryPart() ) );
			}
			finally {
				processingStateStack.pop();
			}

			return copy;
		}

		@Override
		protected SqmSelectQuery<Object> visitSelectQuery(SqmSelectQuery<?> selectQuery) {
			if ( selectQuery instanceof SqmSelectStatement<?> ) {
				return (SqmSelectQuery<Object>) visitSelectStatement( (SqmSelectStatement<?>) selectQuery );
			}
			else {
				return visitSubQueryExpression( (SqmSubQuery<?>) selectQuery );
			}
		}

		protected SqmSelectQuery<Object> visitSelectQuery(SqmSelectQuery<?> selectQuery, SqmQueryPart<?> queryPart) {
			if ( selectQuery instanceof SqmSelectStatement<?> ) {
				final SqmSelectStatement<?> sqmSelectStatement = (SqmSelectStatement<?>) selectQuery;
				//noinspection rawtypes
				return (SqmSelectQuery<Object>) visitSelectStatement(
						new SqmSelectStatement(
								queryPart,
								sqmSelectStatement.getResultType(),
								sqmSelectStatement.getQuerySource(),
								sqmSelectStatement.nodeBuilder()
						)
				);
			}
			else {
				final SqmSubQuery<?> sqmSubQuery = (SqmSubQuery<?>) selectQuery;
				//noinspection rawtypes
				return visitSubQueryExpression(
						new SqmSubQuery(
								processingStateStack.getCurrent().getProcessingQuery(),
								queryPart,
								sqmSubQuery.getResultType(),
								sqmSubQuery.nodeBuilder()
						)
				);
			}
		}

		@Override
		public SqmQueryPart<R> visitQueryPart(SqmQueryPart<?> queryPart) {
			return (SqmQueryPart<R>) super.visitQueryPart( queryPart );
		}

		@Override
		public SqmQueryGroup<R> visitQueryGroup(SqmQueryGroup<?> queryGroup) {
			final List<? extends SqmQueryPart<?>> queryParts = queryGroup.getQueryParts();
			final int size = queryParts.size();
			final List<SqmQueryPart<R>> newQueryParts = new ArrayList<>( size );
			for ( int i = 0; i < size; i++ ) {
				newQueryParts.add( visitQueryPart( queryParts.get( i ) ) );
			}
			return new SqmQueryGroup<>( queryGroup.nodeBuilder(), queryGroup.getSetOperator(), newQueryParts );
		}

		@Override
		public SqmQuerySpec<R> visitQuerySpec(SqmQuerySpec<?> querySpec) {
			// NOTE : it is important that we visit the SqmFromClause first so that the
			// 		fromElementCopyMap gets built before other parts of the queryspec
			// 		are visited

			final SqmQuerySpec<R> sqmQuerySpec = new SqmQuerySpec<>( querySpec.nodeBuilder() );
			sqmQuerySpec.setFromClause( visitFromClause( querySpec.getFromClause() ) );
			sqmQuerySpec.setSelectClause( visitSelectClause( querySpec.getSelectClause() ) );
			sqmQuerySpec.setWhereClause( visitWhereClause( querySpec.getWhereClause() ) );
			sqmQuerySpec.setGroupByClauseExpressions( visitGroupByClause( querySpec.getGroupByClauseExpressions() ) );
			sqmQuerySpec.setHavingClausePredicate( visitHavingClause( querySpec.getHavingClausePredicate() ) );
			sqmQuerySpec.setOrderByClause( visitOrderByClause( querySpec.getOrderByClause() ) );
			if ( querySpec.getFetchExpression() != null ) {
				sqmQuerySpec.setFetchExpression(
						(SqmExpression<?>) querySpec.getFetchExpression().accept( this ),
						querySpec.getFetchClauseType()
				);
			}
			if ( querySpec.getOffsetExpression() != null ) {
				sqmQuerySpec.setOffsetExpression( (SqmExpression<?>) querySpec.getOffsetExpression().accept( this ) );
			}

			return sqmQuerySpec;
		}

		private SqmFromClause currentFromClauseCopy = null;

		@Override
		public SqmFromClause visitFromClause(SqmFromClause fromClause) {
			final SqmFromClause previousCurrent = currentFromClauseCopy;

			try {
				SqmFromClause copy = new SqmFromClause( fromClause.getNumberOfRoots() );
				currentFromClauseCopy = copy;
				super.visitFromClause( fromClause );
				return copy;
			}
			finally {
				currentFromClauseCopy = previousCurrent;
			}
		}

		@Override
		public List<SqmExpression<?>> visitGroupByClause(List<SqmExpression<?>> groupByClauseExpressions) {
			if ( groupByClauseExpressions.isEmpty() ) {
				return groupByClauseExpressions;
			}
			List<SqmExpression<?>> expressions = new ArrayList<>( groupByClauseExpressions.size() );
			for ( SqmExpression<?> groupByClauseExpression : groupByClauseExpressions ) {
				expressions.add( (SqmExpression<?>) groupByClauseExpression.accept( this ) );
			}
			return expressions;
		}

		@Override
		public SqmPredicate visitHavingClause(SqmPredicate sqmPredicate) {
			if ( sqmPredicate == null ) {
				return null;
			}
			return (SqmPredicate) sqmPredicate.accept( this );
		}

		@Override
		public SqmRoot<?> visitRootPath(SqmRoot<?> sqmRoot) {
			final SqmFrom<?, ?> sqmFrom = sqmFromCopyMap.get( sqmRoot );
			if ( sqmFrom != null ) {
				return (SqmRoot<?>) sqmFrom;
			}
			final EntityDomainType<?> pathSource;
			if ( sqmRoot == unmappedPolymorphicFromElement ) {
				pathSource = mappedDescriptor;
			}
			else {
				pathSource = sqmRoot.getModel();
			}
			final SqmRoot<?> copy = new SqmRoot<>(
					pathSource,
					sqmRoot.getExplicitAlias(),
					sqmRoot.isAllowJoins(),
					sqmRoot.nodeBuilder()
			);
			getProcessingStateStack().getCurrent().getPathRegistry().register( copy );
			sqmFromCopyMap.put( sqmRoot, copy );
			sqmPathCopyMap.put( sqmRoot.getNavigablePath(), copy );
			if ( currentFromClauseCopy != null ) {
				currentFromClauseCopy.addRoot( copy );
			}
			return copy;
		}

		@Override
		public SqmDerivedRoot<?> visitRootDerived(SqmDerivedRoot<?> sqmRoot) {
			SqmFrom<?, ?> sqmFrom = sqmFromCopyMap.get( sqmRoot );
			if ( sqmFrom != null ) {
				return (SqmDerivedRoot<?>) sqmFrom;
			}
			final SqmDerivedRoot<?> copy = new SqmDerivedRoot<>(
					(SqmSubQuery<?>) sqmRoot.getQueryPart().accept( this ),
					sqmRoot.getExplicitAlias()
			);
			getProcessingStateStack().getCurrent().getPathRegistry().register( copy );
			sqmFromCopyMap.put( sqmRoot, copy );
			sqmPathCopyMap.put( sqmRoot.getNavigablePath(), copy );
			if ( currentFromClauseCopy != null ) {
				currentFromClauseCopy.addRoot( copy );
			}
			return copy;
		}

		@Override
		public SqmCteRoot<?> visitRootCte(SqmCteRoot<?> sqmRoot) {
			SqmFrom<?, ?> sqmFrom = sqmFromCopyMap.get( sqmRoot );
			if ( sqmFrom != null ) {
				return (SqmCteRoot<?>) sqmFrom;
			}
			final SqmCteRoot<?> copy = new SqmCteRoot<>(
					(SqmCteStatement<?>) sqmRoot.getCte().accept( this ),
					sqmRoot.getExplicitAlias()
			);
			getProcessingStateStack().getCurrent().getPathRegistry().register( copy );
			sqmFromCopyMap.put( sqmRoot, copy );
			sqmPathCopyMap.put( sqmRoot.getNavigablePath(), copy );
			if ( currentFromClauseCopy != null ) {
				currentFromClauseCopy.addRoot( copy );
			}
			return copy;
		}

		@Override
		public SqmCrossJoin<?> visitCrossJoin(SqmCrossJoin<?> join) {
			final SqmFrom<?, ?> sqmFrom = sqmFromCopyMap.get( join );
			if ( sqmFrom != null ) {
				return (SqmCrossJoin<?>) sqmFrom;
			}
			final SqmCrossJoin copy = new SqmCrossJoin<>(
					join.getReferencedPathSource(),
					join.getExplicitAlias(),
					(SqmRoot<?>) sqmFromCopyMap.get( join.findRoot() )
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmCorrelatedCrossJoin<?> visitCorrelatedCrossJoin(SqmCorrelatedCrossJoin<?> join) {
			final SqmFrom<?, ?> sqmFrom = sqmFromCopyMap.get( join );
			if ( sqmFrom != null ) {
				return (SqmCorrelatedCrossJoin<?>) sqmFrom;
			}

			final SqmCorrelatedCrossJoin copy = new SqmCorrelatedCrossJoin<>(
					findSqmFromCopy( join.getCorrelationParent() )
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmPluralPartJoin<?, ?> visitPluralPartJoin(SqmPluralPartJoin<?, ?> join) {
			final SqmFrom<?, ?> sqmFrom = sqmFromCopyMap.get( join );
			if ( sqmFrom != null ) {
				return (SqmPluralPartJoin<?, ?>) sqmFrom;
			}
			final SqmPluralPartJoin copy = new SqmPluralPartJoin<>(
					(SqmFrom<?, ?>) sqmFromCopyMap.get( join.getLhs() ),
					join.getReferencedPathSource(),
					join.getExplicitAlias(),
					join.getSqmJoinType(),
					join.nodeBuilder()
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmCorrelatedPluralPartJoin<?,?> visitCorrelatedPluralPartJoin(SqmCorrelatedPluralPartJoin<?, ?> join) {
			final SqmFrom<?, ?> sqmFrom = sqmFromCopyMap.get( join );
			if ( sqmFrom != null ) {
				return (SqmCorrelatedPluralPartJoin<?, ?>) sqmFrom;
			}
			final SqmCorrelatedPluralPartJoin copy = new SqmCorrelatedPluralPartJoin<>(
					findSqmFromCopy( join.getCorrelationParent() )
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmEntityJoin<?> visitQualifiedEntityJoin(SqmEntityJoin<?> join) {
			final SqmFrom<?, ?> sqmFrom = sqmFromCopyMap.get( join );
			if ( sqmFrom != null ) {
				return (SqmEntityJoin<?>) sqmFrom;
			}
			final SqmRoot<?> sqmRoot = (SqmRoot<?>) sqmFromCopyMap.get( join.findRoot() );
			final SqmEntityJoin copy = new SqmEntityJoin<>(
					join.getReferencedPathSource(),
					join.getExplicitAlias(),
					join.getSqmJoinType(),
					sqmRoot
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmCorrelatedRootJoin<?> visitCorrelatedRootJoin(SqmCorrelatedRootJoin<?> correlatedRootJoin) {
			final SqmCorrelatedRootJoin<?> sqmFrom = (SqmCorrelatedRootJoin<?>) sqmFromCopyMap.get( correlatedRootJoin );
			if ( sqmFrom != null ) {
				return sqmFrom;
			}
			SqmCorrelatedRootJoin<?> copy = new SqmCorrelatedRootJoin<>(
					correlatedRootJoin.getNavigablePath(),
					correlatedRootJoin.getReferencedPathSource(),
					correlatedRootJoin.nodeBuilder()
			);
			correlatedRootJoin.visitReusablePaths( path -> path.accept( this ) );

			sqmFromCopyMap.put( correlatedRootJoin, copy );
			sqmPathCopyMap.put( correlatedRootJoin.getNavigablePath(), copy );
			correlatedRootJoin.visitSqmJoins(
					sqmJoin ->
							sqmJoin.accept( this )
			);

			return copy;
		}

		@Override
		public SqmCorrelatedRoot<?> visitCorrelatedRoot(SqmCorrelatedRoot<?> correlatedRoot) {
			final SqmCorrelatedRoot<?> sqmFrom = (SqmCorrelatedRoot<?>) sqmFromCopyMap.get( correlatedRoot );
			if ( sqmFrom != null ) {
				return sqmFrom;
			}
			SqmCorrelatedRoot<?> copy = new SqmCorrelatedRoot<>(
					findSqmFromCopy( correlatedRoot.getCorrelationParent() )
			);

			correlatedRoot.visitReusablePaths( path -> path.accept( this ) );

			sqmFromCopyMap.put( correlatedRoot, copy );
			sqmPathCopyMap.put( correlatedRoot.getNavigablePath(), copy );
			correlatedRoot.visitSqmJoins(
					sqmJoin ->
							sqmJoin.accept( this )
			);
			return copy;
		}

		@Override
		public SqmBagJoin<?, ?> visitBagJoin(SqmBagJoin<?, ?> join) {
			final SqmFrom<?, ?> existing = sqmFromCopyMap.get( join );
			if ( existing != null ) {
				return (SqmBagJoin<?, ?>) existing;
			}

			final SqmBagJoin copy = new SqmBagJoin<>(
					findSqmFromCopy( join.getLhs() ),
					join.getAttribute(),
					join.getExplicitAlias(),
					join.getSqmJoinType(),
					join.isFetched(),
					join.nodeBuilder()
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmCorrelatedBagJoin<?, ?> visitCorrelatedBagJoin(SqmCorrelatedBagJoin<?, ?> join) {
			final SqmFrom<?, ?> existing = sqmFromCopyMap.get( join );
			if ( existing != null ) {
				return (SqmCorrelatedBagJoin<?, ?>) existing;
			}

			final SqmCorrelatedBagJoin copy = new SqmCorrelatedBagJoin<>(
					findSqmFromCopy( join.getCorrelationParent() )
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmCorrelatedListJoin<?, ?> visitCorrelatedListJoin(SqmCorrelatedListJoin<?, ?> join) {
			final SqmFrom<?, ?> existing = sqmFromCopyMap.get( join );

			if ( existing != null ) {
				return (SqmCorrelatedListJoin<?, ?>) existing;
			}

			final SqmCorrelatedListJoin copy = new SqmCorrelatedListJoin<>(
					findSqmFromCopy( join.getCorrelationParent() )
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmCorrelatedMapJoin<?, ?, ?> visitCorrelatedMapJoin(SqmCorrelatedMapJoin<?, ?, ?> join) {
			final SqmFrom<?, ?> existing = sqmFromCopyMap.get( join );

			if ( existing != null ) {
				return (SqmCorrelatedMapJoin<?, ?, ?>) existing;
			}

			final SqmCorrelatedMapJoin copy = new SqmCorrelatedMapJoin<>(
					findSqmFromCopy( join.getCorrelationParent() )
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmCorrelatedSetJoin<?, ?> visitCorrelatedSetJoin(SqmCorrelatedSetJoin<?, ?> join) {
			final SqmFrom<?, ?> existing = sqmFromCopyMap.get( join );

			if ( existing != null ) {
				return (SqmCorrelatedSetJoin<?, ?>) existing;
			}

			final SqmCorrelatedSetJoin copy = new SqmCorrelatedSetJoin<>(
					findSqmFromCopy( join.getCorrelationParent() )
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmCorrelatedSingularJoin<?, ?> visitCorrelatedSingularJoin(SqmCorrelatedSingularJoin<?, ?> join) {
			final SqmFrom<?, ?> existing = sqmFromCopyMap.get( join );

			if ( existing != null ) {
				return (SqmCorrelatedSingularJoin<?, ?>) existing;
			}

			final SqmCorrelatedSingularJoin copy = new SqmCorrelatedSingularJoin<>(
					findSqmFromCopy( join.getCorrelationParent() )
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmListJoin<?, ?> visitListJoin(SqmListJoin<?, ?> join) {
			final SqmFrom<?, ?> existing = sqmFromCopyMap.get( join );

			if ( existing != null ) {
				return (SqmListJoin<?, ?>) existing;
			}

			final SqmListJoin copy = new SqmListJoin<>(
					findSqmFromCopy( join.getLhs() ),
					join.getAttribute(),
					join.getExplicitAlias(),
					join.getSqmJoinType(),
					join.isFetched(),
					join.nodeBuilder()
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmMapJoin<?, ?, ?> visitMapJoin(SqmMapJoin<?, ?, ?> join) {
			final SqmFrom<?, ?> existing = sqmFromCopyMap.get( join );

			if ( existing != null ) {
				return (SqmMapJoin<?, ?, ?>) existing;
			}

			final SqmMapJoin copy = new SqmMapJoin<>(
					findSqmFromCopy( join.getLhs() ),
					join.getAttribute(),
					join.getExplicitAlias(),
					join.getSqmJoinType(),
					join.isFetched(),
					join.nodeBuilder()
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmSetJoin<?, ?> visitSetJoin(SqmSetJoin<?, ?> join) {
			final SqmFrom<?, ?> existing = sqmFromCopyMap.get( join );

			if ( existing != null ) {
				return (SqmSetJoin<?, ?>) existing;
			}

			final SqmSetJoin copy = new SqmSetJoin<>(
					findSqmFromCopy( join.getLhs() ),
					join.getAttribute(),
					join.getExplicitAlias(),
					join.getSqmJoinType(),
					join.isFetched(),
					join.nodeBuilder()
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmSingularJoin<?, ?> visitSingularJoin(SqmSingularJoin<?, ?> join) {
			final SqmFrom<?, ?> existing = sqmFromCopyMap.get( join );

			if ( existing != null ) {
				return (SqmSingularJoin<?, ?>) existing;
			}

			final SqmSingularJoin copy = new SqmSingularJoin<>(
					findSqmFromCopy( join.getLhs() ),
					join.getAttribute(),
					join.getExplicitAlias(),
					join.getSqmJoinType(),
					join.isFetched(),
					join.nodeBuilder()
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmDerivedJoin<?> visitQualifiedDerivedJoin(SqmDerivedJoin<?> join) {
			SqmFrom<?, ?> sqmFrom = sqmFromCopyMap.get( join );
			if ( sqmFrom != null ) {
				return (SqmDerivedJoin<?>) sqmFrom;
			}
			final SqmRoot<?> sqmRoot = (SqmRoot<?>) sqmFromCopyMap.get( join.findRoot() );
			final SqmDerivedJoin copy = new SqmDerivedJoin(
					(SqmSubQuery<?>) join.getQueryPart().accept( this ),
					join.getExplicitAlias(),
					join.getSqmJoinType(),
					join.isLateral(),
					sqmRoot
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmCorrelatedEntityJoin<?> visitCorrelatedEntityJoin(SqmCorrelatedEntityJoin<?> join) {
			SqmFrom<?, ?> sqmFrom = sqmFromCopyMap.get( join );
			if ( sqmFrom != null ) {
				return (SqmCorrelatedEntityJoin<?>) sqmFrom;
			}

			final SqmCorrelatedEntityJoin copy = new SqmCorrelatedEntityJoin(
					findSqmFromCopy( join.getCorrelationParent() )
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmCteJoin<?> visitQualifiedCteJoin(SqmCteJoin<?> join) {
			SqmFrom<?, ?> sqmFrom = sqmFromCopyMap.get( join );
			if ( sqmFrom != null ) {
				return (SqmCteJoin<?>) sqmFrom;
			}
			final SqmRoot<?> sqmRoot = (SqmRoot<?>) sqmFromCopyMap.get( join.findRoot() );
			final SqmCteJoin copy = new SqmCteJoin(
					(SqmCteStatement<?>) join.getCte().accept( this ),
					join.getExplicitAlias(),
					join.getSqmJoinType(),
					sqmRoot
			);

			visitJoins( join, copy );
			return copy;
		}

		@Override
		public SqmBasicValuedSimplePath<?> visitBasicValuedPath(SqmBasicValuedSimplePath<?> path) {
			final SqmPath<?> existing = sqmPathCopyMap.get( path.getNavigablePath() );
			if ( existing != null ) {
				return (SqmBasicValuedSimplePath<?>) existing;
			}
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();

			final SqmPath<?> lhs = findLhs( path );

			final SqmBasicValuedSimplePath<?> copy = new SqmBasicValuedSimplePath<>(
					lhs.getNavigablePath().append( path.getNavigablePath().getLocalName() ),
					path.getReferencedPathSource(),
					lhs,
					path.nodeBuilder()
			);

			pathRegistry.register( copy );
			sqmPathCopyMap.put( path.getNavigablePath(), copy );
			return copy;
		}

		@Override
		public SqmEmbeddedValuedSimplePath<?> visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath<?> path) {
			final SqmPath<?> existing = sqmPathCopyMap.get( path.getNavigablePath() );
			if ( existing != null ) {
				return (SqmEmbeddedValuedSimplePath<?>) existing;
			}
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();
			final SqmPath<?> lhs = findLhs( path );
			final SqmEmbeddedValuedSimplePath<?> copy = new SqmEmbeddedValuedSimplePath<>(
					lhs.getNavigablePath().append( path.getNavigablePath().getLocalName() ),
					path.getReferencedPathSource(),
					lhs,
					path.nodeBuilder()
			);
			pathRegistry.register( copy );
			sqmPathCopyMap.put( path.getNavigablePath(), copy );
			return copy;
		}

		@Override
		public SqmEntityValuedSimplePath<?> visitEntityValuedPath(SqmEntityValuedSimplePath<?> path) {
			final SqmPath<?> existing = sqmPathCopyMap.get( path.getNavigablePath() );
			if ( existing != null ) {
				return (SqmEntityValuedSimplePath<?>) existing;
			}
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();
			final SqmPath<?> lhs = findLhs( path );
			final SqmEntityValuedSimplePath<?> copy = new SqmEntityValuedSimplePath<>(
					lhs.getNavigablePath().append( path.getNavigablePath().getLocalName() ),
					path.getReferencedPathSource(),
					lhs,
					path.nodeBuilder()
			);
			pathRegistry.register( copy );
			sqmPathCopyMap.put( path.getNavigablePath(), copy );
			return copy;
		}

		@Override
		public SqmPluralValuedSimplePath<?> visitPluralValuedPath(SqmPluralValuedSimplePath<?> path) {
			final SqmPath<?> existing = sqmPathCopyMap.get( path.getNavigablePath() );
			if ( existing != null ) {
				return (SqmPluralValuedSimplePath<?>) existing;
			}
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();
			SqmPath<?> lhs = findLhs( path );

			final SqmPluralValuedSimplePath<?> copy = new SqmPluralValuedSimplePath<>(
					path.getNavigablePath(),
					path.getReferencedPathSource(),
					lhs,
					path.nodeBuilder()
			);
			pathRegistry.register( copy );
			sqmPathCopyMap.put( path.getNavigablePath(), copy );
			return copy;
		}

		private SqmPath<?> findLhs(SqmPath<?> path) {
			final SqmPath<?> lhs = path.getLhs();
			final SqmPath lhsSqmPathCopy = sqmPathCopyMap.get( lhs.getNavigablePath() );
			if ( lhsSqmPathCopy != null ) {
				return lhsSqmPathCopy;
			}
			else {
				return (SqmPath<?>) lhs.accept( this );
			}
		}

		@Override
		public SqmSelectClause visitSelectClause(SqmSelectClause selectClause) {
			SqmSelectClause copy = new SqmSelectClause( selectClause.isDistinct(), selectClause.nodeBuilder() );
			for ( SqmSelection<?> selection : selectClause.getSelections() ) {
				SqmExpression<?> selectableNode = (SqmExpression<?>) selection.getSelectableNode().accept( this );
				copy.addSelection(
						new SqmSelection<>(
								selectableNode,
								selection.getAlias(),
								selectClause.nodeBuilder()
						)
				);
			}
			return copy;
		}

		@Override
		public SqmDynamicInstantiation<?> visitDynamicInstantiation(SqmDynamicInstantiation<?> original) {
			final SqmDynamicInstantiationTarget<?> instantiationTarget = original.getInstantiationTarget();
			final SqmDynamicInstantiation<?> copy;

			switch ( instantiationTarget.getNature() ) {
				case MAP: {
					copy = SqmDynamicInstantiation.forMapInstantiation(
							(JavaType<Map<?, ?>>) instantiationTarget.getTargetTypeDescriptor(),
							getCreationContext().getNodeBuilder()
					);
					break;
				}
				case LIST: {
					copy = SqmDynamicInstantiation.forListInstantiation(
							(JavaType<List<?>>) instantiationTarget.getTargetTypeDescriptor(),
							getCreationContext().getNodeBuilder()
					);
					break;
				}
				default: {
					copy = SqmDynamicInstantiation.forClassInstantiation(
							instantiationTarget.getTargetTypeDescriptor(),
							getCreationContext().getNodeBuilder()
					);
				}
			}

			for ( SqmDynamicInstantiationArgument<?> originalArgument : original.getArguments() ) {
				copy.addArgument(
						new SqmDynamicInstantiationArgument<>(
								( SqmSelectableNode<?>) originalArgument.getSelectableNode().accept( this ),
								originalArgument.getAlias(),
								getCreationContext().getNodeBuilder()
						)
				);
			}

			return copy;
		}

		@Override
		public SqmWhereClause visitWhereClause(SqmWhereClause whereClause) {
			if ( whereClause == null || whereClause.getPredicate() == null ) {
				return null;
			}
			return new SqmWhereClause(
					(SqmPredicate) whereClause.getPredicate().accept( this ),
					getCreationContext().getNodeBuilder()
			);
		}

		@Override
		public SqmGroupedPredicate visitGroupedPredicate(SqmGroupedPredicate predicate) {
			final SqmPredicate subPredicate = (SqmPredicate) predicate.getSubPredicate().accept( this );
			return new SqmGroupedPredicate(
					subPredicate,
					getCreationContext().getNodeBuilder()
			);
		}

		@Override
		public SqmJunctionPredicate visitJunctionPredicate(SqmJunctionPredicate predicate) {
			final List<SqmPredicate> predicates = new ArrayList<>( predicate.getPredicates().size() );
			for ( SqmPredicate subPredicate : predicate.getPredicates() ) {
				predicates.add( (SqmPredicate) subPredicate.accept( this ) );
			}
			return new SqmJunctionPredicate(
					predicate.getOperator(),
					predicates,
					getCreationContext().getNodeBuilder()
			);
		}

		@Override
		public SqmComparisonPredicate visitComparisonPredicate(SqmComparisonPredicate predicate) {
			return new SqmComparisonPredicate(
					(SqmExpression) predicate.getLeftHandExpression().accept( this ), predicate.getSqmOperator(),
					(SqmExpression) predicate.getRightHandExpression().accept( this ),
					getCreationContext().getNodeBuilder()
			);
		}

		@Override
		public SqmEmptinessPredicate visitIsEmptyPredicate(SqmEmptinessPredicate predicate) {
			return new SqmEmptinessPredicate(
					(SqmPluralValuedSimplePath<?>) predicate.getPluralPath().accept( this ),
					predicate.isNegated(),
					predicate.nodeBuilder()
			);
		}

		@Override
		public SqmNullnessPredicate visitIsNullPredicate(SqmNullnessPredicate predicate) {
			return new SqmNullnessPredicate(
					(SqmExpression<?>) predicate.getExpression().accept( this ),
					predicate.isNegated(),
					predicate.nodeBuilder()
			);
		}

		@Override
		public SqmBetweenPredicate visitBetweenPredicate(SqmBetweenPredicate predicate) {
			return new SqmBetweenPredicate(
					(SqmExpression<?>) predicate.getExpression().accept( this ),
					(SqmExpression<?>) predicate.getLowerBound().accept( this ),
					(SqmExpression<?>) predicate.getUpperBound().accept( this ),
					predicate.isNegated(),
					predicate.nodeBuilder()
			);
		}

		@Override
		public SqmLikePredicate visitLikePredicate(SqmLikePredicate predicate) {
			return new SqmLikePredicate(
					(SqmExpression<?>) predicate.getMatchExpression().accept( this ),
					(SqmExpression<?>) predicate.getPattern().accept( this ),
					predicate.getEscapeCharacter() == null ?
							null :
							(SqmExpression<?>) predicate.getEscapeCharacter().accept( this ),
					predicate.nodeBuilder()
			);
		}

		@Override
		public SqmMemberOfPredicate visitMemberOfPredicate(SqmMemberOfPredicate predicate) {
			final SqmPath pathCopy = sqmPathCopyMap.get( predicate.getPluralPath().getNavigablePath() );
			return new SqmMemberOfPredicate( predicate.getLeftHandExpression(), pathCopy, predicate.nodeBuilder() );
		}

		@Override
		public SqmNegatedPredicate visitNegatedPredicate(SqmNegatedPredicate predicate) {
			return new SqmNegatedPredicate(
					(SqmPredicate) predicate.getWrappedPredicate().accept( this ),
					predicate.nodeBuilder()
			);
		}

		@Override
		public SqmInSubQueryPredicate visitInSubQueryPredicate(SqmInSubQueryPredicate predicate) {
			return new SqmInSubQueryPredicate(
					(SqmExpression) predicate.getTestExpression().accept( this ),
					visitSubQueryExpression( predicate.getSubQueryExpression() ),
					predicate.nodeBuilder()
			);
		}

		@Override
		public SqmOrderByClause visitOrderByClause(SqmOrderByClause orderByClause) {
			if ( orderByClause == null || orderByClause.getSortSpecifications().isEmpty() ) {
				return null;
			}

			SqmOrderByClause copy = new SqmOrderByClause();
			for ( SqmSortSpecification sortSpecification : orderByClause.getSortSpecifications() ) {
				copy.addSortSpecification( visitSortSpecification( sortSpecification ) );
			}
			return copy;
		}

		@Override
		public SqmSortSpecification visitSortSpecification(SqmSortSpecification sortSpecification) {
			return new SqmSortSpecification(
					(SqmExpression) sortSpecification.getSortExpression().accept( this ),
					sortSpecification.getSortOrder(),
					sortSpecification.getNullPrecedence()
			);
		}

		@Override
		public SqmPositionalParameter visitPositionalParameterExpression(SqmPositionalParameter<?> expression) {
			return expression;
		}

		@Override
		public SqmNamedParameter visitNamedParameterExpression(SqmNamedParameter<?> expression) {
			return expression;
		}

		@Override
		public SqmLiteralEntityType visitEntityTypeLiteralExpression(SqmLiteralEntityType<?> expression) {
			return new SqmLiteralEntityType( expression.getNodeType(), expression.nodeBuilder() );
		}

		@Override
		public SqmUnaryOperation visitUnaryOperationExpression(SqmUnaryOperation<?> expression) {
			return new SqmUnaryOperation(
					expression.getOperation(),
					(SqmExpression) expression.getOperand().accept( this )
			);
		}

		@Override
		public SqmLiteral visitLiteral(SqmLiteral<?> literal) {
			return new SqmLiteral(
					literal.getLiteralValue(),
					literal.getNodeType(),
					literal.nodeBuilder()
			);
		}

		@Override
		public SqmBinaryArithmetic visitBinaryArithmeticExpression(SqmBinaryArithmetic<?> expression) {
			return new SqmBinaryArithmetic(
					expression.getOperator(), (SqmExpression) expression.getLeftHandOperand().accept( this ),
					(SqmExpression) expression.getRightHandOperand().accept( this ),
					expression.getNodeType(),
					expression.nodeBuilder()
			);
		}

		@Override
		public SqmSubQuery visitSubQueryExpression(SqmSubQuery<?> expression) {
			// its not supported for a SubQuery to define a dynamic instantiation, so
			//		any "selectable node" will only ever be an SqmExpression
			return new SqmSubQuery(
					// todo (6.0) : current?  or previous at this point?
					getProcessingStateStack().getCurrent().getProcessingQuery(),
					visitQueryPart( expression.getQueryPart() ),
					expression.getResultType(),
					expression.nodeBuilder()
			);
		}

		@Override
		public Object visitAny(SqmAny<?> sqmAny) {
			return new SqmAny<>(
					(SqmSubQuery<?>) sqmAny.getSubquery().accept( this ),
					creationContext.getNodeBuilder()
			);
		}

		@Override
		public Object visitTreatedPath(SqmTreatedPath<?, ?> sqmTreatedPath) {
			throw new UnsupportedOperationException("Polymorphic queries: treat operator is not supported");
		}

		@Override
		public Object visitEvery(SqmEvery<?> sqmEvery) {
			sqmEvery.getSubquery().accept( this );
			return new SqmEvery<>(
					(SqmSubQuery<?>) sqmEvery.getSubquery().accept( this ),
					creationContext.getNodeBuilder()
			);
		}

		@Override
		public Stack<SqmCreationProcessingState> getProcessingStateStack() {
			return processingStateStack;
		}

		@Override
		public SqmCreationContext getCreationContext() {
			return creationContext;
		}

		@Override
		public SqmCreationOptions getCreationOptions() {
			return () -> false;
		}

		public <X extends SqmFrom<?, ?>> X findSqmFromCopy(SqmFrom sqmFrom) {
			SqmFrom copy = sqmFromCopyMap.get( sqmFrom );
			if ( copy != null ) {
				return (X) copy;
			}
			else {
				return (X) sqmFrom.accept( this );
			}
		}

		private void visitJoins(SqmFrom<?, ?> join, SqmJoin copy) {
			getProcessingStateStack().getCurrent().getPathRegistry().register( copy );
			sqmFromCopyMap.put( join, copy );
			sqmPathCopyMap.put( join.getNavigablePath(), copy );
			join.visitSqmJoins(
					sqmJoin ->
							sqmJoin.accept( this )
			);
			SqmFrom<?, ?> lhs = (SqmFrom<?, ?>) copy.getLhs();
			if ( lhs != null ) {
				lhs.addSqmJoin( copy );
			}
		}
	}

}
