/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.produce.internal.NavigableBindingHelper;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.expression.BinaryArithmeticSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConcatSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConstantEnumSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConstantFieldSqmExpression;
import org.hibernate.query.sqm.tree.expression.EntityTypeLiteralSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralBigDecimalSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralBigIntegerSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralCharacterSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralDoubleSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralFalseSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralFloatSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralIntegerSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralLongSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralNullSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralStringSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralTrueSqmExpression;
import org.hibernate.query.sqm.tree.expression.NamedParameterSqmExpression;
import org.hibernate.query.sqm.tree.expression.PositionalParameterSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SubQuerySqmExpression;
import org.hibernate.query.sqm.tree.expression.UnaryOperationSqmExpression;
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
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
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
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
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
	public static SqmSelectStatement[] split(SqmSelectStatement statement) {
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
		for ( EntityDescriptor<?> mappedDescriptor : unmappedPolymorphicDescriptor.getImplementors() ) {
			i++;
			final UnmappedPolymorphismReplacer replacer = new UnmappedPolymorphismReplacer(
					statement,
					unmappedPolymorphicReference,
					mappedDescriptor
			);
			expanded[i] = replacer.visitSelectStatement( statement );
		}

		return expanded;
	}

	@SuppressWarnings("unchecked")
	private static class UnmappedPolymorphismReplacer extends BaseSemanticQueryWalker {
		private final SqmRoot unmappedPolymorphicFromElement;
		private final EntityDescriptor mappedDescriptor;

		private Map<SqmFrom,SqmFrom> sqmFromSqmCopyMap = new HashMap<>();
		private Map<SqmNavigableReference, SqmNavigableReference> navigableBindingCopyMap = new HashMap<>();

		private UnmappedPolymorphismReplacer(
				SqmSelectStatement selectStatement,
				SqmRoot unmappedPolymorphicFromElement,
				EntityDescriptor mappedDescriptor) {
			this.unmappedPolymorphicFromElement = unmappedPolymorphicFromElement;
			this.mappedDescriptor = mappedDescriptor;
		}

		@Override
		public SqmStatement visitStatement(SqmStatement statement) {
			throw new UnsupportedOperationException( "Not valid" );
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
			copy.applyQuerySpec( visitQuerySpec( statement.getQuerySpec() ) );
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
						mappedDescriptor
				);
			}
			else {
				copy = new SqmRoot(
						currentFromElementSpaceCopy,
						rootEntityFromElement.getUniqueIdentifier(),
						rootEntityFromElement.getIdentificationVariable(),
						rootEntityFromElement.getNavigableReference().getReferencedNavigable().getEntityDescriptor()
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
					joinedFromElement.getNavigableReference().getReferencedNavigable().getEntityDescriptor()
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
					joinedFromElement.getJoinType()
			);
			navigableBindingCopyMap.put( joinedFromElement.getNavigableReference(), copy.getNavigableReference() );
			return copy;
		}

		@Override
		public SqmAttributeJoin visitQualifiedAttributeJoinFromElement(SqmAttributeJoin joinedFromElement) {
			final SqmSingularAttributeReference existingCopy = (SqmSingularAttributeReference) navigableBindingCopyMap.get( joinedFromElement.getNavigableReference() );
			if ( existingCopy != null ) {
				return (SqmAttributeJoin) existingCopy.getExportedFromElement();
			}

			if ( currentFromElementSpaceCopy == null ) {
				throw new ParsingException( "Current FromElementSpace copy was null" );
			}

			if ( joinedFromElement.getAttributeReference().getExportedFromElement() == null ) {
				throw new ParsingException( "Could not determine attribute join's LHS for copy" );
			}

			return makeCopy( joinedFromElement );
		}

		private SqmAttributeJoin makeCopy(SqmAttributeJoin fromElement) {
			assert fromElement.getAttributeReference().getSourceReference() != null;

			if ( fromElement == null ) {
				return null;
			}

			final SqmNavigableContainerReference sourceBindingCopy = (SqmNavigableContainerReference) navigableBindingCopyMap.get(
					fromElement.getAttributeReference().getSourceReference()
			);

			if ( sourceBindingCopy == null ) {
				throw new ParsingException( "Could not determine attribute join's LHS for copy" );
			}

			assert NavigableBindingHelper.resolveExportedFromElement( sourceBindingCopy ).getContainingSpace() == currentFromElementSpaceCopy;

			final SqmAttributeReference attributeBindingCopy = (SqmAttributeReference) NavigableBindingHelper.createNavigableBinding(
					sourceBindingCopy,
					fromElement.getAttributeReference().getReferencedNavigable()
			);

			final SqmAttributeJoin copy = new SqmAttributeJoin(
					sourceBindingCopy.getExportedFromElement(),
					attributeBindingCopy,
					fromElement.getUniqueIdentifier(),
					fromElement.getIdentificationVariable(),
					fromElement.getIntrinsicSubclassEntityMetadata(),
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
				copy.addSelection( visitSelection( selection ) );
			}
			return copy;
		}

		@Override
		public SqmSelection visitSelection(SqmSelection selection) {
			return new SqmSelection(
					(SqmExpression) selection.getExpression().accept( this ),
					selection.getAlias()
			);
		}

		@Override
		public SqmDynamicInstantiation visitDynamicInstantiation(SqmDynamicInstantiation dynamicInstantiation) {
			SqmDynamicInstantiation copy = dynamicInstantiation.makeShallowCopy();
			for ( SqmDynamicInstantiationArgument aliasedArgument : dynamicInstantiation.getArguments() ) {
				copy.addArgument(
						new SqmDynamicInstantiationArgument(
								(SqmExpression) aliasedArgument.getExpression().accept( this ),
								aliasedArgument.getAlias()
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

			final SqmAttributeJoin originalJoin = (SqmAttributeJoin) sqmFromSqmCopyMap.get( attributeReference.getExportedFromElement() );
			final SqmNavigableContainerReference sourceBindingCopy = (SqmNavigableContainerReference) navigableBindingCopyMap.get(
					attributeReference.getSourceReference()
			);

			if ( sourceBindingCopy == null ) {
				throw new ParsingException( "Could not resolve NavigableSourceBinding copy during query splitting" );
			}

			final SqmSingularAttributeReference attributeBindingCopy = NavigableBindingHelper.createSingularAttributeBinding(
					sourceBindingCopy,
					(SingularPersistentAttribute) attributeReference.getReferencedNavigable()
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
		public PositionalParameterSqmExpression visitPositionalParameterExpression(PositionalParameterSqmExpression expression) {
			return new PositionalParameterSqmExpression( expression.getPosition(), expression.allowMultiValuedBinding() );
		}

		@Override
		public NamedParameterSqmExpression visitNamedParameterExpression(NamedParameterSqmExpression expression) {
			return new NamedParameterSqmExpression( expression.getName(), expression.allowMultiValuedBinding() );
		}

		@Override
		public EntityTypeLiteralSqmExpression visitEntityTypeLiteralExpression(EntityTypeLiteralSqmExpression expression) {
			return new EntityTypeLiteralSqmExpression( expression.getExpressionType() );
		}

		@Override
		public UnaryOperationSqmExpression visitUnaryOperationExpression(UnaryOperationSqmExpression expression) {
			return new UnaryOperationSqmExpression(
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
					expression.getExpressionType(),
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
							expression.getExpressionType()
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
					new SqmCountStarFunction( expression.getExpressionType() ),
					expression.isDistinct()
			);

		}

		@Override
		public SqmCountFunction visitCountFunction(SqmCountFunction expression) {
			return handleDistinct(
					new SqmCountFunction(
							(SqmExpression) expression.getArgument().accept( this ),
							expression.getExpressionType()
					),
					expression.isDistinct()
			);
		}

		@Override
		public SqmMaxFunction visitMaxFunction(SqmMaxFunction expression) {
			return handleDistinct(
					new SqmMaxFunction(
							(SqmExpression) expression.getArgument().accept( this ),
							expression.getExpressionType()
					),
					expression.isDistinct()
			);
		}

		@Override
		public SqmMinFunction visitMinFunction(SqmMinFunction expression) {
			return handleDistinct(
					new SqmMinFunction(
							(SqmExpression) expression.getArgument().accept( this ),
							expression.getExpressionType()
					),
					expression.isDistinct()
			);
		}

		@Override
		public SqmSumFunction visitSumFunction(SqmSumFunction expression) {
			return handleDistinct(
					new SqmSumFunction(
							(SqmExpression) expression.getArgument().accept( this ),
							expression.getExpressionType()
					),
					expression.isDistinct()
			);
		}

		@Override
		public LiteralStringSqmExpression visitLiteralStringExpression(LiteralStringSqmExpression expression) {
			return new LiteralStringSqmExpression( expression.getLiteralValue(), expression.getExpressionType() );
		}

		@Override
		public LiteralCharacterSqmExpression visitLiteralCharacterExpression(LiteralCharacterSqmExpression expression) {
			return new LiteralCharacterSqmExpression( expression.getLiteralValue(), expression.getExpressionType() );
		}

		@Override
		public LiteralDoubleSqmExpression visitLiteralDoubleExpression(LiteralDoubleSqmExpression expression) {
			return new LiteralDoubleSqmExpression( expression.getLiteralValue(), expression.getExpressionType() );
		}

		@Override
		public LiteralIntegerSqmExpression visitLiteralIntegerExpression(LiteralIntegerSqmExpression expression) {
			return new LiteralIntegerSqmExpression( expression.getLiteralValue(), expression.getExpressionType() );
		}

		@Override
		public LiteralBigIntegerSqmExpression visitLiteralBigIntegerExpression(LiteralBigIntegerSqmExpression expression) {
			return new LiteralBigIntegerSqmExpression( expression.getLiteralValue(), expression.getExpressionType() );
		}

		@Override
		public LiteralBigDecimalSqmExpression visitLiteralBigDecimalExpression(LiteralBigDecimalSqmExpression expression) {
			return new LiteralBigDecimalSqmExpression( expression.getLiteralValue(), expression.getExpressionType() );
		}

		@Override
		public LiteralFloatSqmExpression visitLiteralFloatExpression(LiteralFloatSqmExpression expression) {
			return new LiteralFloatSqmExpression( expression.getLiteralValue(), expression.getExpressionType() );
		}

		@Override
		public LiteralLongSqmExpression visitLiteralLongExpression(LiteralLongSqmExpression expression) {
			return new LiteralLongSqmExpression( expression.getLiteralValue(), expression.getExpressionType() );
		}

		@Override
		public LiteralTrueSqmExpression visitLiteralTrueExpression(LiteralTrueSqmExpression expression) {
			return new LiteralTrueSqmExpression( expression.getExpressionType() );
		}

		@Override
		public LiteralFalseSqmExpression visitLiteralFalseExpression(LiteralFalseSqmExpression expression) {
			return new LiteralFalseSqmExpression( expression.getExpressionType() );
		}

		@Override
		public LiteralNullSqmExpression visitLiteralNullExpression(LiteralNullSqmExpression expression) {
			return new LiteralNullSqmExpression();
		}

		@Override
		public ConcatSqmExpression visitConcatExpression(ConcatSqmExpression expression) {
			return new ConcatSqmExpression(
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
					(BasicValuedExpressableType) expression.getExpressionType(),
					arguments
			);
		}

		@Override
		@SuppressWarnings("unchecked")
		public ConstantEnumSqmExpression visitConstantEnumExpression(ConstantEnumSqmExpression expression) {
			return new ConstantEnumSqmExpression( expression.getValue(), expression.getExpressionType() );
		}

		@Override
		@SuppressWarnings("unchecked")
		public ConstantFieldSqmExpression visitConstantFieldExpression(ConstantFieldSqmExpression expression) {
			return new ConstantFieldSqmExpression( expression.getSourceField(), expression.getValue(), expression.getExpressionType() );
		}

		@Override
		public BinaryArithmeticSqmExpression visitBinaryArithmeticExpression(BinaryArithmeticSqmExpression expression) {
			return new BinaryArithmeticSqmExpression(
					expression.getOperation(),
					(SqmExpression) expression.getLeftHandOperand().accept( this ),
					(SqmExpression) expression.getRightHandOperand().accept( this ),
					expression.getExpressionType()
			);
		}

		@Override
		public SubQuerySqmExpression visitSubQueryExpression(SubQuerySqmExpression expression) {
			return new SubQuerySqmExpression(
					visitQuerySpec( expression.getQuerySpec() ),
					// assume already validated
					expression.getQuerySpec().getSelectClause().getSelections().get( 0 ).getExpression().getExpressionType()
			);
		}
	}

}
