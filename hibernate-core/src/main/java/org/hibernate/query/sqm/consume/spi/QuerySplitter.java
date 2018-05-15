/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.produce.spi.CurrentSqmFromElementSpaceCoordAccess;
import org.hibernate.query.sqm.produce.spi.ImplicitAliasGenerator;
import org.hibernate.query.sqm.produce.spi.QuerySpecProcessingState;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.produce.spi.SqmFromBuilder;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmConcat;
import org.hibernate.query.sqm.tree.expression.SqmConstantEnum;
import org.hibernate.query.sqm.tree.expression.SqmConstantFieldReference;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteralBigDecimal;
import org.hibernate.query.sqm.tree.expression.SqmLiteralBigInteger;
import org.hibernate.query.sqm.tree.expression.SqmLiteralCharacter;
import org.hibernate.query.sqm.tree.expression.SqmLiteralDouble;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.SqmLiteralFalse;
import org.hibernate.query.sqm.tree.expression.SqmLiteralFloat;
import org.hibernate.query.sqm.tree.expression.SqmLiteralInteger;
import org.hibernate.query.sqm.tree.expression.SqmLiteralLong;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.expression.SqmLiteralString;
import org.hibernate.query.sqm.tree.expression.SqmLiteralTrue;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmSubQuery;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.domain.SqmAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.expression.function.Distinctable;
import org.hibernate.query.sqm.tree.expression.function.SqmAvgFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmConcatFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountStarFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmGenericFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMaxFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMinFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSumFunction;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.internal.SqmSelectStatementImpl;
import org.hibernate.query.sqm.tree.order.SqmOrderByClause;
import org.hibernate.query.sqm.tree.order.SqmSortSpecification;
import org.hibernate.query.sqm.tree.paging.SqmLimitOffsetClause;
import org.hibernate.query.sqm.tree.predicate.AndSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.BetweenSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.EmptinessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.GroupedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InListSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InSubQuerySqmPredicate;
import org.hibernate.query.sqm.tree.predicate.LikeSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.MemberOfSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NegatedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NullnessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.OrSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.RelationalSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.set.SqmAssignment;
import org.hibernate.query.sqm.tree.set.SqmSetClause;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.PolymorphicEntityValuedExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;

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
		for ( SqmFromElementSpace fromElementSpace : statement.getQuerySpec().getFromClause().getFromElementSpaces() ) {
			if ( PolymorphicEntityValuedExpressableType.class.isInstance( fromElementSpace.getRoot().getNavigableReference().getReferencedNavigable() ) ) {
				unmappedPolymorphicReference = fromElementSpace.getRoot();
			}
		}

		if ( unmappedPolymorphicReference == null ) {
			return new SqmSelectStatement[] { statement };
		}

		final PolymorphicEntityValuedExpressableType<?> unmappedPolymorphicDescriptor = (PolymorphicEntityValuedExpressableType) unmappedPolymorphicReference.getNavigableReference().getReferencedNavigable();
		final SqmSelectStatement[] expanded = new SqmSelectStatement[ unmappedPolymorphicDescriptor.getImplementors().size() ];

		int i = -1;
		for ( EntityTypeDescriptor<?> mappedDescriptor : unmappedPolymorphicDescriptor.getImplementors() ) {
			i++;
			final UnmappedPolymorphismReplacer replacer = new UnmappedPolymorphismReplacer(
					statement,
					unmappedPolymorphicReference,
					mappedDescriptor,
					sessionFactory
			);
			expanded[i] = replacer.visitSelectStatement( statement );
		}

		return expanded;
	}

	@SuppressWarnings("unchecked")
	private static class UnmappedPolymorphismReplacer extends BaseSemanticQueryWalker implements SqmCreationContext {
		private final SqmRoot unmappedPolymorphicFromElement;
		private final EntityTypeDescriptor mappedDescriptor;

		private Map<SqmFrom,SqmFrom> sqmFromSqmCopyMap = new HashMap<>();
		private Map<SqmNavigableReference, SqmNavigableReference> navigableBindingCopyMap = new HashMap<>();
		private Map<NavigablePath, Set<SqmNavigableJoin>> fetchJoinsByParentPathCopy;

		private UnmappedPolymorphismReplacer(
				SqmSelectStatement selectStatement,
				SqmRoot unmappedPolymorphicFromElement,
				EntityTypeDescriptor mappedDescriptor,
				SessionFactoryImplementor sessionFactory) {
			super( sessionFactory );
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
			final SqmSelectStatementImpl copy = new SqmSelectStatementImpl();
			copy.applyQuerySpec( visitQuerySpec( statement.getQuerySpec() ), fetchJoinsByParentPathCopy );
			return copy;
		}

		@Override
		public SqmQuerySpec visitQuerySpec(SqmQuerySpec querySpec) {
			// NOTE : it is important that we visit the SqmFromClause first so that the
			// 		fromElementCopyMap gets built before other parts of the queryspec
			// 		are visited
			return new SqmQuerySpec(
					visitFromClause( querySpec.getFromClause() ),
					visitSelectClause( querySpec.getSelectClause() ),
					visitWhereClause( querySpec.getWhereClause() ),
					visitOrderByClause( querySpec.getOrderByClause() ),
					visitLimitOffsetClause( querySpec.getLimitOffsetClause() )
			);
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

		private SqmFromElementSpace currentFromElementSpaceCopy;

		@Override
		public SqmFromElementSpace visitFromElementSpace(SqmFromElementSpace fromElementSpace) {
			if ( currentFromClauseCopy == null ) {
				throw new ParsingException( "Current SqmFromClause copy was null" );
			}

			final SqmFromElementSpace previousCurrent = currentFromElementSpaceCopy;
			try {
				SqmFromElementSpace copy = currentFromClauseCopy.makeFromElementSpace();
				currentFromElementSpaceCopy = copy;
				super.visitFromElementSpace( fromElementSpace );
				return copy;
			}
			finally {
				currentFromElementSpaceCopy = previousCurrent;
			}
		}

		// todo : it is really the bindings we want to keep track of..

		@Override
		public SqmRoot visitRootEntityFromElement(SqmRoot rootEntityFromElement) {
			final SqmNavigableContainerReference existingCopy = (SqmNavigableContainerReference) navigableBindingCopyMap.get( rootEntityFromElement.getNavigableReference() );
			if ( existingCopy != null ) {
				return (SqmRoot) existingCopy.getExportedFromElement();
			}

			if ( currentFromElementSpaceCopy == null ) {
				throw new ParsingException( "Current FromElementSpace copy was null" );
			}
			if ( currentFromElementSpaceCopy.getRoot() != null ) {
				throw new ParsingException( "FromElementSpace copy already contains root." );
			}

			final SqmRoot copy;
			if ( rootEntityFromElement == unmappedPolymorphicFromElement ) {
				copy = new SqmRoot(
						currentFromElementSpaceCopy,
						rootEntityFromElement.getUniqueIdentifier(),
						rootEntityFromElement.getIdentificationVariable(),
						mappedDescriptor,
						this
				);
			}
			else {
				copy = new SqmRoot(
						currentFromElementSpaceCopy,
						rootEntityFromElement.getUniqueIdentifier(),
						rootEntityFromElement.getIdentificationVariable(),
						rootEntityFromElement.getNavigableReference().getReferencedNavigable().getEntityDescriptor(),
						this
				);
			}
			navigableBindingCopyMap.put( rootEntityFromElement.getNavigableReference(), copy.getNavigableReference() );
			return copy;
		}

		@Override
		public SqmCrossJoin visitCrossJoinedFromElement(SqmCrossJoin joinedFromElement) {
			final SqmNavigableContainerReference existingCopy = (SqmNavigableContainerReference) navigableBindingCopyMap.get( joinedFromElement.getNavigableReference() );
			if ( existingCopy != null ) {
				return (SqmCrossJoin) existingCopy.getExportedFromElement();
			}

			if ( currentFromElementSpaceCopy == null ) {
				throw new ParsingException( "Current FromElementSpace copy was null" );
			}

			final SqmCrossJoin copy = new SqmCrossJoin(
					currentFromElementSpaceCopy,
					joinedFromElement.getUniqueIdentifier(),
					joinedFromElement.getIdentificationVariable(),
					joinedFromElement.getNavigableReference().getReferencedNavigable().getEntityDescriptor(),
					this
			);
			navigableBindingCopyMap.put( joinedFromElement.getNavigableReference(), copy.getNavigableReference() );
			return copy;
		}

		@Override
		public SqmEntityJoin visitQualifiedEntityJoinFromElement(SqmEntityJoin joinedFromElement) {
			final SqmNavigableContainerReference existingCopy = (SqmNavigableContainerReference) navigableBindingCopyMap.get( joinedFromElement.getNavigableReference() );
			if ( existingCopy != null ) {
				return (SqmEntityJoin) existingCopy.getExportedFromElement();
			}

			if ( currentFromElementSpaceCopy == null ) {
				throw new ParsingException( "Current FromElementSpace copy was null" );
			}

			final SqmEntityJoin copy = new SqmEntityJoin(
					currentFromElementSpaceCopy,
					joinedFromElement.getUniqueIdentifier(),
					joinedFromElement.getIdentificationVariable(),
					joinedFromElement.getNavigableReference().getReferencedNavigable().getEntityDescriptor(),
					joinedFromElement.getJoinType(),
					this
			);
			navigableBindingCopyMap.put( joinedFromElement.getNavigableReference(), copy.getNavigableReference() );
			return copy;
		}

		@Override
		public SqmNavigableJoin visitQualifiedAttributeJoinFromElement(SqmNavigableJoin joinedFromElement) {
			final SqmSingularAttributeReference existingCopy = (SqmSingularAttributeReference) navigableBindingCopyMap.get( joinedFromElement.getNavigableReference() );
			if ( existingCopy != null ) {
				return (SqmNavigableJoin) existingCopy.getExportedFromElement();
			}

			if ( currentFromElementSpaceCopy == null ) {
				throw new ParsingException( "Current FromElementSpace copy was null" );
			}

			if ( joinedFromElement.getAttributeReference().getExportedFromElement() == null ) {
				throw new ParsingException( "Could not determine attribute join's LHS for copy" );
			}

			final SqmNavigableJoin copy = makeCopy( joinedFromElement );

			if ( joinedFromElement.isFetched() ) {
				registerFetch(
						joinedFromElement.getNavigableReference().getSourceReference(),
						joinedFromElement
				);
			}

			return copy;
		}


		private SqmNavigableJoin makeCopy(SqmNavigableJoin fromElement) {
			assert fromElement.getAttributeReference().getSourceReference() != null;

			final SqmNavigableContainerReference sourceBindingCopy = (SqmNavigableContainerReference) navigableBindingCopyMap.get(
					fromElement.getAttributeReference().getSourceReference()
			);

			if ( sourceBindingCopy == null ) {
				throw new ParsingException( "Could not determine attribute join's LHS for copy" );
			}

			assert sourceBindingCopy.getExportedFromElement().getContainingSpace() == currentFromElementSpaceCopy;

			final SqmAttributeReference attributeBindingCopy = (SqmAttributeReference) fromElement.getNavigableReference()
					.getReferencedNavigable()
					.createSqmExpression( sourceBindingCopy.getExportedFromElement(), sourceBindingCopy, this );

			final SqmNavigableJoin copy = new SqmNavigableJoin(
					sourceBindingCopy.getExportedFromElement(),
					attributeBindingCopy,
					fromElement.getUniqueIdentifier(),
					fromElement.getIdentificationVariable(),
					fromElement.getJoinType(),
					fromElement.isFetched()
			);
			navigableBindingCopyMap.put( fromElement.getAttributeReference(), copy.getAttributeReference() );
			return copy;
		}

		@Override
		public SqmSelectClause visitSelectClause(SqmSelectClause selectClause) {
			SqmSelectClause copy = new SqmSelectClause( selectClause.isDistinct() );
			for ( SqmSelection selection : selectClause.getSelections() ) {
				copy.addSelection(
						new SqmSelection(
								(SqmExpression) selection.getSelectableNode().accept( this ),
								selection.getAlias()
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
					copy = SqmDynamicInstantiation.forMapInstantiation( instantiationTarget.getTargetTypeDescriptor() );
					break;
				}
				case LIST: {
					copy = SqmDynamicInstantiation.forListInstantiation( instantiationTarget.getTargetTypeDescriptor() );
					break;
				}
				default: {
					copy = SqmDynamicInstantiation.forClassInstantiation( instantiationTarget.getTargetTypeDescriptor() );
				}
			}

			for ( SqmDynamicInstantiationArgument originalArgument : original.getArguments() ) {
				copy.addArgument(
						new SqmDynamicInstantiationArgument(
								( SqmSelectableNode) originalArgument.getSelectableNode().accept( this ),
								originalArgument.getAlias()
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
			return new SqmWhereClause( (SqmPredicate) whereClause.getPredicate().accept( this ) );
		}

		@Override
		public GroupedSqmPredicate visitGroupedPredicate(GroupedSqmPredicate predicate) {
			return new GroupedSqmPredicate( (SqmPredicate) predicate.accept( this ) );
		}

		@Override
		public AndSqmPredicate visitAndPredicate(AndSqmPredicate predicate) {
			return new AndSqmPredicate(
					(SqmPredicate) predicate.getLeftHandPredicate().accept( this ),
					(SqmPredicate) predicate.getRightHandPredicate().accept( this )
			);
		}

		@Override
		public OrSqmPredicate visitOrPredicate(OrSqmPredicate predicate) {
			return new OrSqmPredicate(
					(SqmPredicate) predicate.getLeftHandPredicate().accept( this ),
					(SqmPredicate) predicate.getRightHandPredicate().accept( this )
			);
		}

		@Override
		public RelationalSqmPredicate visitRelationalPredicate(RelationalSqmPredicate predicate) {
			return new RelationalSqmPredicate(
					predicate.getOperator(),
					(SqmExpression) predicate.getLeftHandExpression().accept( this ),
					(SqmExpression) predicate.getRightHandExpression().accept( this )
			);
		}

		@Override
		public EmptinessSqmPredicate visitIsEmptyPredicate(EmptinessSqmPredicate predicate) {
			return new EmptinessSqmPredicate(
					(SqmPluralAttributeReference) predicate.getExpression().accept( this ),
					predicate.isNegated()
			);
		}

		@Override
		public NullnessSqmPredicate visitIsNullPredicate(NullnessSqmPredicate predicate) {
			return new NullnessSqmPredicate(
					(SqmExpression) predicate.getExpression().accept( this ),
					predicate.isNegated()
			);
		}

		@Override
		public BetweenSqmPredicate visitBetweenPredicate(BetweenSqmPredicate predicate) {
			return new BetweenSqmPredicate(
					(SqmExpression) predicate.getExpression().accept( this ),
					(SqmExpression) predicate.getLowerBound().accept( this ),
					(SqmExpression) predicate.getUpperBound().accept( this ),
					predicate.isNegated()
			);
		}

		@Override
		public LikeSqmPredicate visitLikePredicate(LikeSqmPredicate predicate) {
			return new LikeSqmPredicate(
					(SqmExpression) predicate.getMatchExpression().accept( this ),
					(SqmExpression) predicate.getPattern().accept( this ),
					(SqmExpression) predicate.getEscapeCharacter().accept( this )
			);
		}

		@Override
		public MemberOfSqmPredicate visitMemberOfPredicate(MemberOfSqmPredicate predicate) {
			final SqmAttributeReference attributeReferenceCopy = resolveAttributeReference( predicate.getPluralAttributeReference() );
			// NOTE : no type check b4 cast as it is assumed that the initial SQM producer
			//		already verified that the path resolves to a plural attribute
			return new MemberOfSqmPredicate( (SqmPluralAttributeReference) attributeReferenceCopy );
		}

//		private DomainReferenceBinding resolveDomainReferenceBinding(DomainReferenceBinding binding) {
//			DomainReferenceBinding copy = navigableBindingCopyMap.get( binding );
//			if ( copy == null ) {
//				copy = makeDomainReferenceBindingCopy( binding );
//				navigableBindingCopyMap.put( binding, copy );
//			}
//			return copy;
//		}

//		private DomainReferenceBinding makeDomainReferenceBindingCopy(DomainReferenceBinding binding) {
//			if ( binding instanceof AttributeBinding ) {
//				final AttributeBinding attributeBinding = (AttributeBinding) binding;
//				return new AttributeBinding(
//						resolveDomainReferenceBinding( attributeBinding.getLhs() ),
//						attributeBinding.getBoundDomainReference(),
//						attributeBinding.getFromElement()
//				);
//			}
//			else if ( binding instanceof )
//		}


		// todo (6.0) : broker in SqmNavigableReference instead?

		private SqmAttributeReference resolveAttributeReference(SqmAttributeReference attributeBinding) {
			// its an attribute join... there has to be a source
			assert attributeBinding.getSourceReference() != null;

			SqmAttributeReference attributeBindingCopy = (SqmAttributeReference) navigableBindingCopyMap.get( attributeBinding );
			if ( attributeBindingCopy == null ) {
				attributeBindingCopy = makeCopy( attributeBinding );
			}

			return attributeBindingCopy;
		}

		private SqmAttributeReference makeCopy(SqmAttributeReference attributeReference) {
			// its an attribute join... there has to be a source
			assert attributeReference.getSourceReference() != null;

			assert !navigableBindingCopyMap.containsKey( attributeReference );

			final SqmNavigableJoin originalJoin = (SqmNavigableJoin) sqmFromSqmCopyMap.get( attributeReference.getExportedFromElement() );
			final SqmNavigableContainerReference sourceNavRef = (SqmNavigableContainerReference) navigableBindingCopyMap.get(
					attributeReference.getSourceReference()
			);

			if ( sourceNavRef == null ) {
				throw new ParsingException( "Could not resolve NavigableSourceBinding copy during query splitting" );
			}

			final SqmAttributeReference attributeBindingCopy = (SqmAttributeReference) sourceNavRef.getReferencedNavigable().createSqmExpression(
					sourceNavRef.getExportedFromElement(),
					sourceNavRef,
					this
			);
			navigableBindingCopyMap.put( attributeReference, attributeBindingCopy );
			return attributeBindingCopy;
		}

		@Override
		public NegatedSqmPredicate visitNegatedPredicate(NegatedSqmPredicate predicate) {
			return new NegatedSqmPredicate(
					(SqmPredicate) predicate.getWrappedPredicate().accept( this )
			);
		}

		@Override
		public InListSqmPredicate visitInListPredicate(InListSqmPredicate predicate) {
			InListSqmPredicate copy = new InListSqmPredicate(
					(SqmExpression) predicate.getTestExpression().accept( this )
			);
			for ( SqmExpression expression : predicate.getListExpressions() ) {
				copy.addExpression( (SqmExpression) expression.accept( this ) );
			}
			return copy;
		}

		@Override
		public InSubQuerySqmPredicate visitInSubQueryPredicate(InSubQuerySqmPredicate predicate) {
			return new InSubQuerySqmPredicate(
					(SqmExpression) predicate.getTestExpression().accept( this ),
					visitSubQueryExpression( predicate.getSubQueryExpression() )
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
					sortSpecification.getSortOrder()
			);
		}

		@Override
		public SqmLimitOffsetClause visitLimitOffsetClause(SqmLimitOffsetClause limitOffsetClause) {
			if ( limitOffsetClause == null ) {
				return null;
			}

			return new SqmLimitOffsetClause(
					(SqmExpression) limitOffsetClause.getLimitExpression().accept( this ),
					(SqmExpression) limitOffsetClause.getOffsetExpression().accept( this )
			);
		}

		@Override
		public SqmPositionalParameter visitPositionalParameterExpression(SqmPositionalParameter expression) {
			return new SqmPositionalParameter( expression.getPosition(), expression.allowMultiValuedBinding() );
		}

		@Override
		public SqmNamedParameter visitNamedParameterExpression(SqmNamedParameter expression) {
			return new SqmNamedParameter( expression.getName(), expression.allowMultiValuedBinding() );
		}

		@Override
		public SqmLiteralEntityType visitEntityTypeLiteralExpression(SqmLiteralEntityType expression) {
			return new SqmLiteralEntityType( expression.getExpressableType() );
		}

		@Override
		public SqmUnaryOperation visitUnaryOperationExpression(SqmUnaryOperation expression) {
			return new SqmUnaryOperation(
					expression.getOperation(),
					(SqmExpression) expression.getOperand().accept( this )
			);
		}

		@Override
		public SqmGenericFunction visitGenericFunction(SqmGenericFunction expression) {
			List<SqmExpression> argumentsCopy = new ArrayList<>();
			for ( SqmExpression argument : expression.getArguments() ) {
				argumentsCopy.add( (SqmExpression) argument.accept( this ) );
			}
			return new SqmGenericFunction(
					expression.getFunctionName(),
					expression.getExpressableType(),
					argumentsCopy
			);
		}

		@Override
		public SqlAstFunctionProducer visitSqlAstFunctionProducer(SqlAstFunctionProducer functionProducer) {
			// todo (6.0) : likely this needs a copy too
			//		how to model that?
			//		for now, return the same reference
			return functionProducer;
		}

		@Override
		public SqmAvgFunction visitAvgFunction(SqmAvgFunction expression) {
			return handleDistinct(
					new SqmAvgFunction(
							(SqmExpression) expression.getArgument().accept( this ),
							expression.getExpressableType()
					),
					expression.isDistinct()
			);
		}

		private <T extends SqmFunction> T handleDistinct(T function, boolean shouldMakeDistinction) {
			if ( function instanceof Distinctable
					&& shouldMakeDistinction ) {
				( (Distinctable) function ).makeDistinct();
			}

			return function;
		}

		@Override
		public SqmCountStarFunction visitCountStarFunction(SqmCountStarFunction expression) {
			return handleDistinct(
					new SqmCountStarFunction( expression.getExpressableType() ),
					expression.isDistinct()
			);

		}

		@Override
		public SqmCountFunction visitCountFunction(SqmCountFunction expression) {
			return handleDistinct(
					new SqmCountFunction(
							(SqmExpression) expression.getArgument().accept( this ),
							expression.getExpressableType()
					),
					expression.isDistinct()
			);
		}

		@Override
		public SqmMaxFunction visitMaxFunction(SqmMaxFunction expression) {
			return handleDistinct(
					new SqmMaxFunction(
							(SqmExpression) expression.getArgument().accept( this ),
							expression.getExpressableType()
					),
					expression.isDistinct()
			);
		}

		@Override
		public SqmMinFunction visitMinFunction(SqmMinFunction expression) {
			return handleDistinct(
					new SqmMinFunction(
							(SqmExpression) expression.getArgument().accept( this ),
							expression.getExpressableType()
					),
					expression.isDistinct()
			);
		}

		@Override
		public SqmSumFunction visitSumFunction(SqmSumFunction expression) {
			return handleDistinct(
					new SqmSumFunction(
							(SqmExpression) expression.getArgument().accept( this ),
							expression.getExpressableType()
					),
					expression.isDistinct()
			);
		}

		@Override
		public SqmLiteralString visitLiteralStringExpression(SqmLiteralString expression) {
			return new SqmLiteralString( expression.getLiteralValue(), expression.getExpressableType() );
		}

		@Override
		public SqmLiteralCharacter visitLiteralCharacterExpression(SqmLiteralCharacter expression) {
			return new SqmLiteralCharacter( expression.getLiteralValue(), expression.getExpressableType() );
		}

		@Override
		public SqmLiteralDouble visitLiteralDoubleExpression(SqmLiteralDouble expression) {
			return new SqmLiteralDouble( expression.getLiteralValue(), expression.getExpressableType() );
		}

		@Override
		public SqmLiteralInteger visitLiteralIntegerExpression(SqmLiteralInteger expression) {
			return new SqmLiteralInteger( expression.getLiteralValue(), expression.getExpressableType() );
		}

		@Override
		public SqmLiteralBigInteger visitLiteralBigIntegerExpression(SqmLiteralBigInteger expression) {
			return new SqmLiteralBigInteger( expression.getLiteralValue(), expression.getExpressableType() );
		}

		@Override
		public SqmLiteralBigDecimal visitLiteralBigDecimalExpression(SqmLiteralBigDecimal expression) {
			return new SqmLiteralBigDecimal( expression.getLiteralValue(), expression.getExpressableType() );
		}

		@Override
		public SqmLiteralFloat visitLiteralFloatExpression(SqmLiteralFloat expression) {
			return new SqmLiteralFloat( expression.getLiteralValue(), expression.getExpressableType() );
		}

		@Override
		public SqmLiteralLong visitLiteralLongExpression(SqmLiteralLong expression) {
			return new SqmLiteralLong( expression.getLiteralValue(), expression.getExpressableType() );
		}

		@Override
		public SqmLiteralTrue visitLiteralTrueExpression(SqmLiteralTrue expression) {
			return new SqmLiteralTrue( expression.getExpressableType() );
		}

		@Override
		public SqmLiteralFalse visitLiteralFalseExpression(SqmLiteralFalse expression) {
			return new SqmLiteralFalse( expression.getExpressableType() );
		}

		@Override
		public SqmLiteralNull visitLiteralNullExpression(SqmLiteralNull expression) {
			return new SqmLiteralNull();
		}

		@Override
		public SqmConcat visitConcatExpression(SqmConcat expression) {
			return new SqmConcat(
					(SqmExpression) expression.getLeftHandOperand().accept( this ),
					(SqmExpression) expression.getRightHandOperand().accept( this )
			);
		}

		@Override
		public SqmConcatFunction visitConcatFunction(SqmConcatFunction expression) {
			final List<SqmExpression> arguments = new ArrayList<>();
			for ( SqmExpression argument : expression.getExpressions() ) {
				arguments.add( (SqmExpression) argument.accept( this ) );
			}

			return new SqmConcatFunction(
					(BasicValuedExpressableType) expression.getExpressableType(),
					arguments
			);
		}

		@Override
		@SuppressWarnings("unchecked")
		public SqmConstantEnum visitConstantEnumExpression(SqmConstantEnum expression) {
			return new SqmConstantEnum( expression.getLiteralValue(), expression.getExpressableType() );
		}

		@Override
		@SuppressWarnings("unchecked")
		public SqmConstantFieldReference visitConstantFieldReference(SqmConstantFieldReference expression) {
			return new SqmConstantFieldReference( expression.getSourceField(), expression.getLiteralValue(), expression.getExpressableType() );
		}

		@Override
		public SqmBinaryArithmetic visitBinaryArithmeticExpression(SqmBinaryArithmetic expression) {
			return new SqmBinaryArithmetic(
					expression.getOperation(),
					(SqmExpression) expression.getLeftHandOperand().accept( this ),
					(SqmExpression) expression.getRightHandOperand().accept( this ),
					expression.getExpressableType()
			);
		}

		@Override
		public SqmSubQuery visitSubQueryExpression(SqmSubQuery expression) {
			// its not supported for a SubQuery to define a dynamic instantiation, so
			//		any "selectable node" will only ever be an SqmExpression
			return new SqmSubQuery(
					visitQuerySpec( expression.getQuerySpec() ),
					// assume already validated
					( (SqmExpression) expression.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode() ).getExpressableType()
			);
		}

		@Override
		public QuerySpecProcessingState getCurrentQuerySpecProcessingState() {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public SqmFromElementSpace getCurrentFromElementSpace() {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public SqmFromBuilder getCurrentFromElementBuilder() {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public CurrentSqmFromElementSpaceCoordAccess getCurrentSqmFromElementSpaceCoordAccess() {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
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

		@Override
		public void cacheNavigableReference(SqmNavigableReference reference) {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public SqmNavigableReference getCachedNavigableReference(
				SqmNavigableContainerReference source, Navigable navigable) {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public void registerFetch(SqmNavigableContainerReference sourceReference, SqmNavigableJoin navigableJoin) {
			Set<SqmNavigableJoin> joins = null;
			if ( fetchJoinsByParentPathCopy == null ) {
				fetchJoinsByParentPathCopy = new HashMap<>();
			}
			else {
				joins = fetchJoinsByParentPathCopy.get( sourceReference.getNavigablePath() );
			}

			if ( joins == null ) {
				joins = new HashSet<>();
				fetchJoinsByParentPathCopy.put( sourceReference.getNavigablePath(), joins );
			}

			joins.add( navigableJoin );
		}
	}

}
