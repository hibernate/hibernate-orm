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

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.internal.SqmDmlCreationProcessingState;
import org.hibernate.query.sqm.internal.SqmQuerySpecCreationProcessingStateStandardImpl;
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
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
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
				throw new UnsupportedOperationException( "Polymorphic query group is unsupported!" );
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
		private final Stack<SqmCreationProcessingState> processingStateStack = new StandardStack<>();

		private Map<NavigablePath, SqmPath> sqmPathCopyMap = new HashMap<>();
		private Map<SqmFrom, SqmFrom> sqmFromCopyMap = new HashMap<>();

		private UnmappedPolymorphismReplacer(
				SqmRoot unmappedPolymorphicFromElement,
				EntityDomainType mappedDescriptor,
				SessionFactoryImplementor sessionFactory) {
			super( sessionFactory.getServiceRegistry() );
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
		public SqmAssignment visitAssignment(SqmAssignment assignment) {
			throw new UnsupportedOperationException( "Not valid" );
		}

		@Override
		public Object visitCteContainer(SqmCteContainer consumer) {
			final SqmCteContainer processingQuery = (SqmCteContainer) getProcessingStateStack().getCurrent()
					.getProcessingQuery();
			processingQuery.setWithRecursive( consumer.isWithRecursive() );
			for ( SqmCteStatement<?> cteStatement : consumer.getCteStatements() ) {
				processingQuery.addCteStatement( visitCteStatement( cteStatement ) );
			}
			return processingQuery;
		}

		@Override
		public SqmCteStatement<?> visitCteStatement(SqmCteStatement<?> sqmCteStatement) {
			// No need to copy anything here
			return sqmCteStatement;
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
					new SqmQuerySpecCreationProcessingStateStandardImpl(
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
				pathSource = sqmRoot.getReferencedPathSource();
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
		public SqmCrossJoin<?> visitCrossJoin(SqmCrossJoin<?> join) {
			final SqmFrom<?, ?> sqmFrom = sqmFromCopyMap.get( join );
			if ( sqmFrom != null ) {
				return (SqmCrossJoin<?>) sqmFrom;
			}
			final SqmRoot<?> sqmRoot = (SqmRoot<?>) sqmFromCopyMap.get( join.findRoot() );
			final SqmCrossJoin copy = new SqmCrossJoin<>(
					join.getReferencedPathSource(),
					join.getExplicitAlias(),
					sqmRoot
			);
			getProcessingStateStack().getCurrent().getPathRegistry().register( copy );
			sqmFromCopyMap.put( join, copy );
			sqmPathCopyMap.put( join.getNavigablePath(), copy );
			sqmRoot.addSqmJoin( copy );
			return copy;
		}

		@Override
		public SqmPluralPartJoin<?, ?> visitPluralPartJoin(SqmPluralPartJoin<?, ?> join) {
			final SqmFrom<?, ?> sqmFrom = sqmFromCopyMap.get( join );
			if ( sqmFrom != null ) {
				return (SqmPluralPartJoin<?, ?>) sqmFrom;
			}
			final SqmFrom<?, ?> newLhs = (SqmFrom<?, ?>) sqmFromCopyMap.get( join.getLhs() );
			final SqmPluralPartJoin copy = new SqmPluralPartJoin<>(
					newLhs,
					join.getReferencedPathSource(),
					join.getExplicitAlias(),
					join.getSqmJoinType(),
					join.nodeBuilder()
			);
			getProcessingStateStack().getCurrent().getPathRegistry().register( copy );
			sqmFromCopyMap.put( join, copy );
			sqmPathCopyMap.put( join.getNavigablePath(), copy );
			newLhs.addSqmJoin( copy );
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
			getProcessingStateStack().getCurrent().getPathRegistry().register( copy );
			sqmFromCopyMap.put( join, copy );
			sqmPathCopyMap.put( join.getNavigablePath(), copy );
			sqmRoot.addSqmJoin( copy );
			return copy;
		}

		@Override
		public SqmAttributeJoin<?, ?> visitQualifiedAttributeJoin(SqmAttributeJoin<?, ?> join) {
			SqmFrom<?, ?> sqmFrom = sqmFromCopyMap.get( join );
			if ( sqmFrom != null ) {
				return (SqmAttributeJoin<?, ?>) sqmFrom;
			}
			SqmAttributeJoin copy = join.makeCopy( getProcessingStateStack().getCurrent() );
			getProcessingStateStack().getCurrent().getPathRegistry().register( copy );
			sqmFromCopyMap.put( join, copy );
			sqmPathCopyMap.put( join.getNavigablePath(), copy );
			( (SqmFrom<?, ?>) copy.getParent() ).addSqmJoin( copy );
			return copy;
		}

		@Override
		public SqmBasicValuedSimplePath<?> visitBasicValuedPath(SqmBasicValuedSimplePath<?> path) {
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();

			final SqmBasicValuedSimplePath<?> copy = new SqmBasicValuedSimplePath<>(
					path.getNavigablePath(),
					path.getReferencedPathSource(),
					pathRegistry.findFromByPath( path.getLhs().getNavigablePath() ),
					path.nodeBuilder()
			);
			pathRegistry.register( copy );
			sqmPathCopyMap.put( path.getNavigablePath(), copy );
			return copy;
		}

		@Override
		public SqmEmbeddedValuedSimplePath<?> visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath<?> path) {
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();
			final SqmEmbeddedValuedSimplePath<?> copy = new SqmEmbeddedValuedSimplePath<>(
					path.getNavigablePath(),
					path.getReferencedPathSource(),
					pathRegistry.findFromByPath( path.getLhs().getNavigablePath() ),
					path.nodeBuilder()
			);
			pathRegistry.register( copy );
			sqmPathCopyMap.put( path.getNavigablePath(), copy );
			return copy;
		}

		@Override
		public SqmEntityValuedSimplePath<?> visitEntityValuedPath(SqmEntityValuedSimplePath<?> path) {
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();
			final SqmEntityValuedSimplePath<?> copy = new SqmEntityValuedSimplePath<>(
					path.getNavigablePath(),
					path.getReferencedPathSource(),
					pathRegistry.findFromByPath( path.getLhs().getNavigablePath() ),
					path.nodeBuilder()
			);
			pathRegistry.register( copy );
			sqmPathCopyMap.put( path.getNavigablePath(), copy );
			return copy;
		}

		@Override
		public SqmPluralValuedSimplePath<?> visitPluralValuedPath(SqmPluralValuedSimplePath<?> path) {
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();
			final SqmPluralValuedSimplePath<?> copy = new SqmPluralValuedSimplePath<>(
					path.getNavigablePath(),
					path.getReferencedPathSource(),
					pathRegistry.findFromByPath( path.getLhs().getNavigablePath() ),
					path.nodeBuilder()
			);
			pathRegistry.register( copy );
			sqmPathCopyMap.put( path.getNavigablePath(), copy );
			return copy;
		}

		@Override
		public SqmSelectClause visitSelectClause(SqmSelectClause selectClause) {
			SqmSelectClause copy = new SqmSelectClause( selectClause.isDistinct(), selectClause.nodeBuilder() );
			for ( SqmSelection<?> selection : selectClause.getSelections() ) {
				copy.addSelection(
						new SqmSelection<>(
								(SqmExpression<?>) selection.getSelectableNode().accept( this ),
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
			return new SqmGroupedPredicate(
					(SqmPredicate) predicate.accept( this ),
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
			return new SqmPositionalParameter(
					expression.getPosition(),
					expression.allowMultiValuedBinding(),
					expression.nodeBuilder()
			);
		}

		@Override
		public SqmNamedParameter visitNamedParameterExpression(SqmNamedParameter<?> expression) {
			return new SqmNamedParameter(
					expression.getName(),
					expression.allowMultiValuedBinding(),
					expression.getNodeType(),
					expression.nodeBuilder()
			);
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

	}

}
