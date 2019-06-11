/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.consume.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.produce.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.produce.spi.SqmQuerySpecCreationProcessingState;
import org.hibernate.query.sqm.produce.spi.ImplicitAliasGenerator;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.produce.spi.SqmCreationOptions;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.function.SqmFunction;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmAndPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmEmptinessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmGroupedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmMemberOfPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNegatedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmOrPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget;
import org.hibernate.query.sqm.tree.select.SqmGroupByClause;
import org.hibernate.query.sqm.tree.select.SqmHavingClause;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
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

/**
 * Handles splitting queries containing unmapped polymorphic references.
 *
 * @author Steve Ebersole
 */
public class QuerySplitter {
	public static SqmSelectStatement[] split(
			SqmSelectStatement statement,
			SessionFactoryImplementor sessionFactory) {
		// We only allow unmapped polymorphism in a very restricted way.  Specifically,
		// the unmapped polymorphic reference can only be a root and can be the only
		// root.  Use that restriction to locate the unmapped polymorphic reference
		SqmRoot unmappedPolymorphicReference = null;
		for ( SqmRoot root : statement.getQuerySpec().getFromClause().getRoots() ) {
			if ( root.getReferencedPathSource() instanceof SqmPolymorphicRootDescriptor ) {
				unmappedPolymorphicReference = root;
			}
		}

		if ( unmappedPolymorphicReference == null ) {
			return new SqmSelectStatement[] { statement };
		}

		final SqmPolymorphicRootDescriptor<?> unmappedPolymorphicDescriptor = (SqmPolymorphicRootDescriptor) unmappedPolymorphicReference.getReferencedPathSource();
		final SqmSelectStatement[] expanded = new SqmSelectStatement[ unmappedPolymorphicDescriptor.getImplementors().size() ];

		int i = -1;
		for ( EntityDomainType<?> mappedDescriptor : unmappedPolymorphicDescriptor.getImplementors() ) {
			i++;
			final UnmappedPolymorphismReplacer replacer = new UnmappedPolymorphismReplacer(
					unmappedPolymorphicReference,
					mappedDescriptor,
					sessionFactory
			);
			expanded[i] = replacer.visitSelectStatement( statement );
		}

		return expanded;
	}

	@SuppressWarnings("unchecked")
	private static class UnmappedPolymorphismReplacer extends BaseSemanticQueryWalker implements SqmCreationState {
		private final SqmRoot unmappedPolymorphicFromElement;
		private final EntityDomainType mappedDescriptor;

		private Map<NavigablePath, SqmPath> sqmPathCopyMap = new HashMap<>();
		private Map<SqmFrom,SqmFrom> sqmFromCopyMap = new HashMap<>();

		private UnmappedPolymorphismReplacer(
				SqmRoot unmappedPolymorphicFromElement,
				EntityDomainType mappedDescriptor,
				SessionFactoryImplementor sessionFactory) {
			super( sessionFactory.getServiceRegistry() );
			this.unmappedPolymorphicFromElement = unmappedPolymorphicFromElement;
			this.mappedDescriptor = mappedDescriptor;
		}

		@Override
		public SqmUpdateStatement visitUpdateStatement(SqmUpdateStatement statement) {
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
		public SqmDeleteStatement visitDeleteStatement(SqmDeleteStatement statement) {
			throw new UnsupportedOperationException( "Not valid" );
		}

		@Override
		public SqmSelectStatement visitSelectStatement(SqmSelectStatement statement) {
			final SqmSelectStatement copy = new SqmSelectStatement( statement.nodeBuilder() );
			copy.setQuerySpec( visitQuerySpec( statement.getQuerySpec() ) );
			return copy;
		}

		@Override
		public SqmQuerySpec visitQuerySpec(SqmQuerySpec querySpec) {
			// NOTE : it is important that we visit the SqmFromClause first so that the
			// 		fromElementCopyMap gets built before other parts of the queryspec
			// 		are visited

			final SqmQuerySpec sqmQuerySpec = new SqmQuerySpec( querySpec.nodeBuilder() );
			sqmQuerySpec.setFromClause( visitFromClause( querySpec.getFromClause() ) );
			sqmQuerySpec.setSelectClause( visitSelectClause( querySpec.getSelectClause() ) );
			sqmQuerySpec.setWhereClause( visitWhereClause( querySpec.getWhereClause() ) );
			sqmQuerySpec.setGroupByClause( visitGroupByClause( querySpec.getGroupByClause() ) );
			sqmQuerySpec.setOrderByClause( visitOrderByClause( querySpec.getOrderByClause() ) );
			sqmQuerySpec.setLimitExpression( (SqmExpression) querySpec.getLimitExpression().accept( this ) );
			sqmQuerySpec.setOffsetExpression( (SqmExpression) querySpec.getOffsetExpression().accept( this ) );

			return querySpec;
		}

		private SqmFromClause currentFromClauseCopy = null;

		@Override
		public SqmFromClause visitFromClause(SqmFromClause fromClause) {
			final SqmFromClause previousCurrent = currentFromClauseCopy;

			try {
				SqmFromClause copy = new SqmFromClause();
				currentFromClauseCopy = copy;
				super.visitFromClause( fromClause );
				return copy;
			}
			finally {
				currentFromClauseCopy = previousCurrent;
			}
		}

		@Override
		public SqmGroupByClause visitGroupByClause(SqmGroupByClause clause) {
			final SqmGroupByClause result = new SqmGroupByClause();
			clause.visitGroupings(
					grouping -> result.addGrouping(
							(SqmExpression) grouping.getExpression().accept( this ),
							grouping.getCollation()
					)
			);
			return result;
		}

		@Override
		public SqmGroupByClause.SqmGrouping visitGrouping(SqmGroupByClause.SqmGrouping grouping) {
			throw new UnsupportedOperationException();
		}

		@Override
		public SqmHavingClause visitHavingClause(SqmHavingClause clause) {
			return new SqmHavingClause(
					(SqmPredicate) clause.getPredicate().accept( this )
			);
		}

		@Override
		public SqmRoot visitRootPath(SqmRoot sqmRoot) {
			return (SqmRoot) getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
					sqmRoot.getNavigablePath(),
					navigablePath -> {
						final SqmRoot copy;
						if ( sqmRoot == unmappedPolymorphicFromElement ) {
							copy = new SqmRoot(
									mappedDescriptor,
									sqmRoot.getExplicitAlias(),
									sqmRoot.nodeBuilder()
							);
						}
						else {
							copy = new SqmRoot(
									sqmRoot.getReferencedPathSource(),
									sqmRoot.getExplicitAlias(),
									sqmRoot.nodeBuilder()
							);
						}
						sqmFromCopyMap.put( sqmRoot, copy );
						sqmPathCopyMap.put( sqmRoot.getNavigablePath(), copy );
						return copy;
					}
			);
		}

		@Override
		public SqmCrossJoin visitCrossJoin(SqmCrossJoin join) {
			return (SqmCrossJoin) getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
					join.getNavigablePath(),
					navigablePath -> {
						final SqmCrossJoin copy = new SqmCrossJoin(
								join.getReferencedPathSource(),
								join.getExplicitAlias(),
								(SqmRoot) sqmFromCopyMap.get( join.findRoot() )
						);
						sqmFromCopyMap.put( join, copy );
						sqmPathCopyMap.put( join.getNavigablePath(), copy );
						return copy;
					}
			);
		}

		@Override
		public SqmEntityJoin visitQualifiedEntityJoin(SqmEntityJoin join) {
			return (SqmEntityJoin) getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
					join.getNavigablePath(),
					navigablePath -> {
						final SqmEntityJoin copy = new SqmEntityJoin(
								join.getReferencedPathSource(),
								join.getExplicitAlias(),
								join.getSqmJoinType(),
								(SqmRoot) sqmFromCopyMap.get( join.findRoot() )
						);
						sqmFromCopyMap.put( join, copy );
						sqmPathCopyMap.put( join.getNavigablePath(), copy );
						return copy;
					}
			);
		}

		@Override
		public SqmAttributeJoin visitQualifiedAttributeJoin(SqmAttributeJoin join) {
			return (SqmAttributeJoin) getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
					join.getNavigablePath(),
					navigablePath -> {
						SqmAttributeJoin copy = join.makeCopy(getProcessingStateStack().getCurrent());
						sqmFromCopyMap.put( join, copy );
						sqmPathCopyMap.put( join.getNavigablePath(), copy );
						return copy;
					}
			);
		}

		@Override
		public SqmBasicValuedSimplePath visitBasicValuedPath(SqmBasicValuedSimplePath path) {
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();

			return (SqmBasicValuedSimplePath) pathRegistry.resolvePath(
					path.getNavigablePath(),
					navigablePath -> {
						final SqmBasicValuedSimplePath copy = new SqmBasicValuedSimplePath(
								navigablePath,
								path.getReferencedPathSource(),
								pathRegistry.findFromByPath( path.getLhs().getNavigablePath() ),
								path.nodeBuilder()
						);
						sqmPathCopyMap.put( path.getNavigablePath(), copy );
						return copy;
					}
			);
		}

		@Override
		public SqmEmbeddedValuedSimplePath visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath path) {
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();

			return (SqmEmbeddedValuedSimplePath) pathRegistry.resolvePath(
					path.getNavigablePath(),
					navigablePath -> {
						final SqmEmbeddedValuedSimplePath copy = new SqmEmbeddedValuedSimplePath(
								navigablePath,
								path.getReferencedPathSource(),
								pathRegistry.findFromByPath( path.getLhs().getNavigablePath() ),
								path.nodeBuilder()
						);
						sqmPathCopyMap.put( path.getNavigablePath(), copy );
						return copy;
					}
			);
		}

		@Override
		public SqmEntityValuedSimplePath visitEntityValuedPath(SqmEntityValuedSimplePath path) {
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();

			return (SqmEntityValuedSimplePath) pathRegistry.resolvePath(
					path.getNavigablePath(),
					navigablePath -> {
						final SqmEntityValuedSimplePath copy = new SqmEntityValuedSimplePath(
								navigablePath,
								path.getReferencedPathSource(),
								pathRegistry.findFromByPath( path.getLhs().getNavigablePath() ),
								path.nodeBuilder()
						);
						sqmPathCopyMap.put( path.getNavigablePath(), copy );
						return copy;
					}
			);
		}

		@Override
		public SqmPluralValuedSimplePath visitPluralValuedPath(SqmPluralValuedSimplePath path) {
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();

			return (SqmPluralValuedSimplePath) pathRegistry.resolvePath(
					path.getNavigablePath(),
					navigablePath -> {
						final SqmPluralValuedSimplePath copy = new SqmPluralValuedSimplePath(
								navigablePath,
								path.getReferencedPathSource(),
								pathRegistry.findFromByPath( path.getLhs().getNavigablePath() ),
								path.nodeBuilder()
						);
						sqmPathCopyMap.put( path.getNavigablePath(), copy );
						return copy;
					}
			);
		}

		@Override
		public SqmSelectClause visitSelectClause(SqmSelectClause selectClause) {
			SqmSelectClause copy = new SqmSelectClause( selectClause.isDistinct(), selectClause.nodeBuilder() );
			for ( SqmSelection selection : selectClause.getSelections() ) {
				copy.addSelection(
						new SqmSelection(
								(SqmExpression) selection.getSelectableNode().accept( this ),
								selection.getAlias(),
								selectClause.nodeBuilder()
						)
				);
			}
			return copy;
		}

		@Override
		public SqmDynamicInstantiation visitDynamicInstantiation(SqmDynamicInstantiation original) {
			final SqmDynamicInstantiationTarget instantiationTarget = original.getInstantiationTarget();
			final SqmDynamicInstantiation copy;

			switch ( instantiationTarget.getNature() ) {
				case MAP: {
					copy = SqmDynamicInstantiation.forMapInstantiation(
							instantiationTarget.getTargetTypeDescriptor(),
							getCreationContext().getNodeBuilder()
					);
					break;
				}
				case LIST: {
					copy = SqmDynamicInstantiation.forListInstantiation(
							instantiationTarget.getTargetTypeDescriptor(),
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

			for ( SqmDynamicInstantiationArgument originalArgument : ( (SqmDynamicInstantiation<?>) original ).getArguments() ) {
				copy.addArgument(
						new SqmDynamicInstantiationArgument(
								( SqmSelectableNode) originalArgument.getSelectableNode().accept( this ),
								originalArgument.getAlias(),
								getCreationContext().getNodeBuilder()
						)
				);
			}

			return copy;
		}

		@Override
		public SqmWhereClause visitWhereClause(SqmWhereClause whereClause) {
			if ( whereClause == null ) {
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
		public SqmAndPredicate visitAndPredicate(SqmAndPredicate predicate) {
			return new SqmAndPredicate(
					(SqmPredicate) predicate.getLeftHandPredicate().accept( this ),
					(SqmPredicate) predicate.getRightHandPredicate().accept( this ),
					getCreationContext().getNodeBuilder()
			);
		}

		@Override
		public SqmOrPredicate visitOrPredicate(SqmOrPredicate predicate) {
			return new SqmOrPredicate(
					(SqmPredicate) predicate.getLeftHandPredicate().accept( this ),
					(SqmPredicate) predicate.getRightHandPredicate().accept( this ),
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
					(SqmPath) predicate.getPluralPath().accept( this ),
					predicate.isNegated(),
					predicate.nodeBuilder()
			);
		}

		@Override
		public SqmNullnessPredicate visitIsNullPredicate(SqmNullnessPredicate predicate) {
			return new SqmNullnessPredicate(
					(SqmExpression) predicate.getExpression().accept( this ),
					predicate.isNegated(),
					predicate.nodeBuilder()
			);
		}

		@Override
		public SqmBetweenPredicate visitBetweenPredicate(SqmBetweenPredicate predicate) {
			return new SqmBetweenPredicate(
					(SqmExpression) predicate.getExpression().accept( this ),
					(SqmExpression) predicate.getLowerBound().accept( this ),
					(SqmExpression) predicate.getUpperBound().accept( this ),
					predicate.isNegated(),
					predicate.nodeBuilder()
			);
		}

		@Override
		public SqmLikePredicate visitLikePredicate(SqmLikePredicate predicate) {
			return new SqmLikePredicate(
					(SqmExpression) predicate.getMatchExpression().accept( this ),
					(SqmExpression) predicate.getPattern().accept( this ),
					(SqmExpression) predicate.getEscapeCharacter().accept( this ),
					predicate.nodeBuilder()
			);
		}

		@Override
		public SqmMemberOfPredicate visitMemberOfPredicate(SqmMemberOfPredicate predicate) {
			final SqmPath pathCopy = sqmPathCopyMap.get( predicate.getPluralPath().getNavigablePath() );
			return new SqmMemberOfPredicate( pathCopy, predicate.nodeBuilder() );
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
			if ( orderByClause == null ) {
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
					sortSpecification.getCollation(),
					sortSpecification.getSortOrder(),
					sortSpecification.getNullPrecedence()
			);
		}



		@Override
		public SqmPositionalParameter visitPositionalParameterExpression(SqmPositionalParameter expression) {
			return new SqmPositionalParameter(
					expression.getPosition(),
					expression.allowMultiValuedBinding(),
					expression.nodeBuilder()
			);
		}

		@Override
		public SqmNamedParameter visitNamedParameterExpression(SqmNamedParameter expression) {
			return new SqmNamedParameter(
					expression.getName(),
					expression.allowMultiValuedBinding(),
					expression.nodeBuilder()
			);
		}

		@Override
		public SqmLiteralEntityType visitEntityTypeLiteralExpression(SqmLiteralEntityType expression) {
			return new SqmLiteralEntityType( expression.getNodeType(), expression.nodeBuilder() );
		}

		@Override
		public SqmUnaryOperation visitUnaryOperationExpression(SqmUnaryOperation expression) {
			return new SqmUnaryOperation(
					expression.getOperation(),
					(SqmExpression) expression.getOperand().accept( this )
			);
		}

		@Override
		public SqmFunction visitFunction(SqmFunction functionProducer) {
			// todo (6.0) : likely this needs a copy too
			//		how to model that?
			//		for now, return the same reference
			return functionProducer;
		}

		@Override
		public SqmLiteral visitLiteral(SqmLiteral literal) {
			return new SqmLiteral(
					literal.getLiteralValue(),
					literal.getNodeType(),
					literal.nodeBuilder()
			);
		}

		@Override
		public SqmBinaryArithmetic visitBinaryArithmeticExpression(SqmBinaryArithmetic expression) {
			return new SqmBinaryArithmetic(
					expression.getOperator(), (SqmExpression) expression.getLeftHandOperand().accept( this ),
					(SqmExpression) expression.getRightHandOperand().accept( this ),
					expression.getNodeType(),
					expression.nodeBuilder()
			);
		}

		@Override
		public SqmSubQuery visitSubQueryExpression(SqmSubQuery expression) {
			// its not supported for a SubQuery to define a dynamic instantiation, so
			//		any "selectable node" will only ever be an SqmExpression
			return new SqmSubQuery(
					// todo (6.0) : current?  or previous at this point?
					getProcessingStateStack().getCurrent().getProcessingQuery(),
					visitQuerySpec( expression.getQuerySpec() ),
					expression.nodeBuilder()
			);
		}

		@Override
		public SqmQuerySpecCreationProcessingState getCurrentQuerySpecProcessingState() {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public Stack<SqmCreationProcessingState> getProcessingStateStack() {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public SqmCreationContext getCreationContext() {
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public SqmCreationOptions getCreationOptions() {
			return () -> false;
		}

		@Override
		public String generateUniqueIdentifier() {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public ImplicitAliasGenerator getImplicitAliasGenerator() {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
		}
	}

}
