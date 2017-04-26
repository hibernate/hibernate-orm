/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal.criteria;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.FetchParent;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;

import org.hibernate.internal.util.collections.Stack;
import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.QueryException;
import org.hibernate.query.sqm.produce.internal.QuerySpecProcessingStateDmlImpl;
import org.hibernate.query.sqm.produce.internal.QuerySpecProcessingStateStandardImpl;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.produce.spi.QuerySpecProcessingState;
import org.hibernate.query.sqm.produce.spi.criteria.CriteriaVisitor;
import org.hibernate.query.sqm.produce.spi.criteria.JpaCriteriaDelete;
import org.hibernate.query.sqm.produce.spi.criteria.JpaCriteriaQuery;
import org.hibernate.query.sqm.produce.spi.criteria.JpaCriteriaUpdate;
import org.hibernate.query.sqm.produce.spi.criteria.JpaExpression;
import org.hibernate.query.sqm.produce.spi.criteria.JpaOrder;
import org.hibernate.query.sqm.produce.spi.criteria.JpaPredicate;
import org.hibernate.query.sqm.produce.spi.criteria.JpaQuerySpec;
import org.hibernate.query.sqm.produce.spi.criteria.JpaSubquery;
import org.hibernate.query.sqm.produce.spi.criteria.JpaUpdateAssignment;
import org.hibernate.query.sqm.produce.spi.criteria.from.JpaAttributeJoin;
import org.hibernate.query.sqm.produce.spi.criteria.from.JpaFetch;
import org.hibernate.query.sqm.produce.spi.criteria.from.JpaFrom;
import org.hibernate.query.sqm.produce.spi.criteria.from.JpaRoot;
import org.hibernate.query.sqm.produce.spi.criteria.path.JpaAttributePath;
import org.hibernate.query.sqm.produce.spi.criteria.path.JpaPath;
import org.hibernate.query.sqm.produce.spi.criteria.path.JpaPluralAttributePath;
import org.hibernate.query.sqm.produce.spi.criteria.path.JpaSingularAttributePath;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.expression.BinaryArithmeticSqmExpression;
import org.hibernate.query.sqm.tree.expression.CoalesceSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConcatSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConstantEnumSqmExpression;
import org.hibernate.query.sqm.tree.expression.EntityTypeLiteralSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralBigDecimalSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralBigIntegerSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralCharacterSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralDoubleSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralFalseSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralFloatSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralIntegerSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralLongSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralStringSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralTrueSqmExpression;
import org.hibernate.query.sqm.tree.expression.NamedParameterSqmExpression;
import org.hibernate.query.sqm.tree.expression.PositionalParameterSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SubQuerySqmExpression;
import org.hibernate.query.sqm.tree.expression.UnaryOperationSqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableSourceReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.expression.function.AvgFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.CastFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.CountFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.CountStarFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.GenericFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.MaxFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.MinFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SumFunctionSqmExpression;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.internal.SqmDeleteStatementImpl;
import org.hibernate.query.sqm.tree.internal.SqmSelectStatementImpl;
import org.hibernate.query.sqm.tree.internal.SqmUpdateStatementImpl;
import org.hibernate.query.sqm.tree.order.SqmOrderByClause;
import org.hibernate.query.sqm.tree.order.SqmSortOrder;
import org.hibernate.query.sqm.tree.order.SqmSortSpecification;
import org.hibernate.query.sqm.tree.paging.SqmLimitOffsetClause;
import org.hibernate.query.sqm.tree.predicate.AndSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.BetweenSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.BooleanExpressionSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.EmptinessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InListSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InSubQuerySqmPredicate;
import org.hibernate.query.sqm.tree.predicate.LikeSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.MemberOfSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NegatedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NullnessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.OrSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.RelationalPredicateOperator;
import org.hibernate.query.sqm.tree.predicate.RelationalSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelection;

/**
 * @author Steve Ebersole
 */
public class CriteriaInterpreter implements CriteriaVisitor {
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// top level statement visitation

	public static SqmSelectStatement interpretSelectCriteria(CriteriaQuery criteria, ParsingContext parsingContext) {
		if ( !JpaCriteriaQuery.class.isInstance( criteria ) ) {
			throw new IllegalArgumentException( "CriteriaQuery to interpret must implement JpaCriteriaQuery" );
		}

		final JpaCriteriaQuery jpaCriteriaQuery = (JpaCriteriaQuery) criteria;
		final CriteriaInterpreter interpreter = new CriteriaInterpreter( parsingContext );

		final SqmSelectStatementImpl selectStatement = new SqmSelectStatementImpl();
		selectStatement.applyQuerySpec( interpreter.visitQuerySpec( jpaCriteriaQuery.getQuerySpec() ) );

		return selectStatement;
	}

	public static <E> SqmDeleteStatement interpretDeleteCriteria(CriteriaDelete<E> criteria, ParsingContext parsingContext) {
		if ( !JpaCriteriaDelete.class.isInstance( criteria ) ) {
			throw new IllegalArgumentException( "CriteriaDelete to interpret must implement SqmDeleteStatement" );
		}

		final CriteriaInterpreter interpreter = new CriteriaInterpreter( parsingContext );
		return interpreter.visitDeleteCriteria( (JpaCriteriaDelete<E>) criteria );
	}

	public static <E> SqmUpdateStatement interpretUpdateCriteria(CriteriaUpdate<E> criteria, ParsingContext parsingContext) {
		if ( !JpaCriteriaUpdate.class.isInstance( criteria ) ) {
			throw new IllegalArgumentException( "CriteriaUpdate to interpret must implement org.hibernate.sqm.query.JpaCriteriaUpdate" );
		}

		final CriteriaInterpreter interpreter = new CriteriaInterpreter( parsingContext );
		return interpreter.visitUpdateCriteria( (JpaCriteriaUpdate<E>) criteria );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// visitation

	private final ParsingContext parsingContext;
	private final Stack<QuerySpecProcessingState> querySpecProcessingStateStack = new Stack<>();

	private CriteriaInterpreter(ParsingContext parsingContext) {
		this.parsingContext = parsingContext;
	}

	public ParsingContext getParsingContext() {
		return parsingContext;
	}

	private <E> SqmDeleteStatement visitDeleteCriteria(JpaCriteriaDelete<E> jpaCriteria) {
		final QuerySpecProcessingStateDmlImpl dmlProcessingState = new QuerySpecProcessingStateDmlImpl( parsingContext );
		querySpecProcessingStateStack.push( dmlProcessingState );

		try {
			final SqmRoot entityToDelete = dmlProcessingState.getFromElementBuilder().makeRootEntityFromElement(
					dmlProcessingState.getDmlFromElementSpace(),
					jpaCriteria.getRoot().getEntityType(),
					interpretAlias( jpaCriteria.getRoot().getAlias() )
			);

			final SqmDeleteStatementImpl sqmStatement = new SqmDeleteStatementImpl( entityToDelete );

			if ( jpaCriteria.getRestriction() != null ) {
				sqmStatement.getWhereClause().setPredicate( jpaCriteria.getRestriction().visitPredicate( this ) );
			}

			return sqmStatement;
		}
		finally {
			querySpecProcessingStateStack.pop();
		}
	}

	private <E> SqmUpdateStatement visitUpdateCriteria(JpaCriteriaUpdate<E> jpaCriteria) {
		final QuerySpecProcessingStateDmlImpl dmlProcessingState = new QuerySpecProcessingStateDmlImpl( parsingContext );
		querySpecProcessingStateStack.push( dmlProcessingState );

		try {
			final SqmRoot entityToUpdate = dmlProcessingState.getFromElementBuilder().makeRootEntityFromElement(
					dmlProcessingState.getDmlFromElementSpace(),
					jpaCriteria.getRoot().getEntityType(),
					interpretAlias( jpaCriteria.getRoot().getAlias() )
			);

			final SqmUpdateStatementImpl sqmStatement = new SqmUpdateStatementImpl( entityToUpdate );

			for ( JpaUpdateAssignment assignment : jpaCriteria.getAssignments() ) {
				sqmStatement.getSetClause().addAssignment(
						(SqmSingularAttributeReference) resolveNavigableBinding( entityToUpdate.getBinding(), assignment.getTargetAttributePath() ),
						assignment.getUpdatedValue().visitExpression( this )
				);
			}

			if ( jpaCriteria.getRestriction() != null ) {
				sqmStatement.getWhereClause().setPredicate( jpaCriteria.getRestriction().visitPredicate( this ) );
			}

			return sqmStatement;
		}
		finally {
			querySpecProcessingStateStack.pop();
		}
	}

	private final Map<JpaPath,SqmNavigableReference> jpaPathResolutionMap = new HashMap<>();

	private SqmNavigableReference resolvePath(JpaPath jpaPath) {
		final SqmNavigableReference existing = jpaPathResolutionMap.get( jpaPath );
		if ( existing != null ) {
			return existing;
		}

		if ( jpaPath instanceof JpaFrom ) {
			return resolveJpaFrom0( (JpaFrom) jpaPath );
		}

		if ( jpaPath instanceof JpaAttributePath ) {
			return resolveJpaAttributePath0( (JpaAttributePath) jpaPath );
		}

		throw new ParsingException( "Could not determine how to resolve JpaPath : " + jpaPath );
	}

	private SqmNavigableReference resolveJpaFrom(JpaFrom jpaFrom) {
		final SqmNavigableReference existing = jpaPathResolutionMap.get( jpaFrom );
		if ( existing != null ) {
			return existing;
		}

		return resolveJpaFrom0( jpaFrom );
	}

	private SqmNavigableReference resolveJpaFrom0(JpaFrom jpaFrom) {
		if ( jpaFrom instanceof JpaRoot ) {
			return makeSqmRoot(
					querySpecProcessingStateStack.getCurrent().getFromClause(),
					(JpaRoot<?>) jpaFrom
			).getBinding();
		}
		else if ( jpaFrom instanceof JpaAttributeJoin ) {
			final JpaAttributeJoin jpaAttributeJoin = (JpaAttributeJoin) jpaFrom;
			final SqmNavigableReference parentPathBinding = resolvePath( jpaAttributeJoin.getParentPath() );
			return makeSqmAttributeJoin(
					parentPathBinding.getSourceReference(),
					parentPathBinding.getSourceReference().getExportedFromElement().getContainingSpace(),
					jpaAttributeJoin
			).getBinding();
		}

		throw new ParsingException( "Could not determine how to resolve JpaFrom : " + jpaFrom );
	}

	private SqmNavigableReference resolveJpaAttributePath(JpaAttributePath jpaPath) {
		final SqmNavigableReference existing = jpaPathResolutionMap.get( jpaPath );
		if ( existing != null ) {
			return existing;
		}

		return resolveJpaAttributePath0( jpaPath );
	}

	private SqmNavigableReference resolveJpaAttributePath0(JpaAttributePath jpaPath) {
		if ( jpaPath instanceof JpaSingularAttributePath ) {
			return resolveSingularAttributePath0( (JpaSingularAttributePath) jpaPath );
		}

		if ( jpaPath instanceof JpaPluralAttributePath ) {
			return resolvePluralAttributePath0( (JpaPluralAttributePath) jpaPath );
		}

		throw new ParsingException( "Could not determine how to resolve JpaAttributePath : " + jpaPath );
	}

	private SqmSingularAttributeReference resolveSingularAttributePath(JpaSingularAttributePath jpaAttributePath) {
		final SqmNavigableReference existing = jpaPathResolutionMap.get( jpaAttributePath );
		if ( existing != null ) {
			return (SqmSingularAttributeReference) existing;
		}

		return resolveSingularAttributePath0( jpaAttributePath );
	}

	private SqmSingularAttributeReference resolveSingularAttributePath0(JpaSingularAttributePath jpaAttributePath) {
		final SqmNavigableSourceReference attributeSourceBinding = (SqmNavigableSourceReference) resolvePath( jpaAttributePath.getParentPath() );
		final SqmSingularAttributeReference attributeBinding = (SqmSingularAttributeReference) parsingContext.findOrCreateNavigableBinding(
				attributeSourceBinding,
				jpaAttributePath.getNavigable().getAttributeName()
		);
		jpaPathResolutionMap.put( jpaAttributePath, attributeBinding );
		return attributeBinding;
	}

	private SqmPluralAttributeReference resolvePluralAttributePath(JpaPluralAttributePath jpaAttributePath) {
		final SqmNavigableSourceReference existing = (SqmNavigableSourceReference) jpaPathResolutionMap.get( jpaAttributePath );
		if ( existing != null ) {
			return (SqmPluralAttributeReference) existing;
		}

		return resolvePluralAttributePath0( jpaAttributePath );
	}

	private SqmPluralAttributeReference resolvePluralAttributePath0(JpaPluralAttributePath jpaAttributePath) {
		final SqmNavigableSourceReference attributeSourceBinding = (SqmNavigableSourceReference) resolvePath( jpaAttributePath.getParentPath() );
		final SqmPluralAttributeReference attributeBinding = (SqmPluralAttributeReference) parsingContext.findOrCreateNavigableBinding(
				attributeSourceBinding,
				jpaAttributePath.getNavigable().getAttributeName()
		);
		jpaPathResolutionMap.put( jpaAttributePath, attributeBinding );
		return attributeBinding;
	}

	private SqmNavigableReference resolveNavigableBinding(SqmNavigableSourceReference sqmFrom, JpaAttributePath attributePath) {
		return parsingContext.findOrCreateNavigableBinding(
				sqmFrom,
				attributePath.getNavigable().getAttributeName()
		);
	}

	private SqmQuerySpec visitQuerySpec(JpaQuerySpec jpaQuerySpec) {
		querySpecProcessingStateStack.push(
				new QuerySpecProcessingStateStandardImpl( parsingContext, querySpecProcessingStateStack.getCurrent() )
		);

		try {
			return new SqmQuerySpec(
					visitFromClause( jpaQuerySpec ),
					visitSelectClause( jpaQuerySpec ),
					visitWhereClause( jpaQuerySpec ),
					visitOrderBy( jpaQuerySpec ),
					visitLimitOffset( jpaQuerySpec )
			);
		}
		finally {
			querySpecProcessingStateStack.pop();
		}
	}

	private SqmLimitOffsetClause visitLimitOffset(JpaQuerySpec<?> jpaQuerySpec) {
		// not yet supported in criteria
		return null;
	}

	private SqmOrderByClause visitOrderBy(JpaQuerySpec<?> jpaQuerySpec) {
		if ( jpaQuerySpec.getOrderList() == null || jpaQuerySpec.getOrderList().isEmpty() ) {
			return null;
		}

		final SqmOrderByClause sqmOrderByClause = new SqmOrderByClause();
		for ( JpaOrder jpaOrder : jpaQuerySpec.getOrderList() ) {
			sqmOrderByClause.addSortSpecification(
					new SqmSortSpecification(
							jpaOrder.getExpression().visitExpression( this ),
							jpaOrder.isAscending() ? SqmSortOrder.ASCENDING : SqmSortOrder.DESCENDING
					)
			);
		}
		return sqmOrderByClause;
	}

	private SqmFromClause visitFromClause(JpaQuerySpec<?> jpaQuerySpec) {
		final SqmFromClause fromClause = new SqmFromClause();
		for ( JpaRoot<?> jpaRoot : jpaQuerySpec.getFromClause().getRoots() ) {
			makeSqmRoot( fromClause, jpaRoot );
		}

		return fromClause;
	}

	private SqmRoot makeSqmRoot(SqmFromClause fromClause, JpaRoot<?> jpaRoot) {
		final SqmFromElementSpace space = fromClause.makeFromElementSpace();
		final SqmRoot sqmRoot = querySpecProcessingStateStack.getCurrent().getFromElementBuilder().makeRootEntityFromElement(
				space,
				jpaRoot.getEntityType(),
				interpretAlias( jpaRoot.getAlias() )
		);
		space.setRoot( sqmRoot );
		bindJoins( jpaRoot, sqmRoot.getBinding(), space );
		bindFetches( jpaRoot, sqmRoot.getBinding(), space );
		jpaPathResolutionMap.put( jpaRoot, sqmRoot.getBinding() );

		return sqmRoot;
	}

	private void bindJoins(JpaFrom<?,?> lhs, SqmNavigableReference lhsBinding, SqmFromElementSpace space) {
		if ( !SqmNavigableSourceReference.class.isInstance( lhsBinding ) ) {
			if ( !lhs.getJoins().isEmpty() ) {
				throw new ParsingException( "Attempt to bind joins against a NavigableBinding that is not also a NavigableSourceBinding " );
			}
			else {
				return;
			}
		}

		for ( Join<?, ?> join : lhs.getJoins() ) {
			makeSqmAttributeJoin( (SqmNavigableSourceReference) lhsBinding, space, join );
		}
	}

	private SqmAttributeJoin makeSqmAttributeJoin(SqmNavigableSourceReference sourceBinding, SqmFromElementSpace space, Join<?, ?> join) {
		final JpaAttributeJoin<?,?> jpaAttributeJoin = (JpaAttributeJoin<?, ?>) join;
		final String alias = jpaAttributeJoin.getAlias();

		final SqmAttributeReference attributeBinding = (SqmAttributeReference) parsingContext.findOrCreateNavigableBinding(
				sourceBinding,
				jpaAttributeJoin.getAttribute().getName()
		);

		// todo : handle treats

		final SqmAttributeJoin sqmJoin = querySpecProcessingStateStack.getCurrent().getFromElementBuilder().buildAttributeJoin(
				attributeBinding,
				alias,
				// todo : this is where treat would be applied
				null,
				convert( join.getJoinType() ),
				false,
				false
		);
		space.addJoin( sqmJoin );
		bindJoins( jpaAttributeJoin, sqmJoin.getBinding(), space );
		jpaPathResolutionMap.put( jpaAttributeJoin, sqmJoin.getBinding() );

		return sqmJoin;
	}

	private void bindFetches(FetchParent<?, ?> lhs, SqmNavigableReference lhsBinding, SqmFromElementSpace space) {
		if ( !SqmNavigableSourceReference.class.isInstance( lhsBinding ) ) {
			if ( !lhs.getFetches().isEmpty() ) {
				throw new ParsingException( "Attempt to bind fetches against a NavigableBinding that is not also a NavigableSourceBinding " );
			}
			else {
				return;
			}
		}

		final SqmNavigableSourceReference sourceBinding = (SqmNavigableSourceReference) lhsBinding;

		for ( Fetch<?, ?> fetch : lhs.getFetches() ) {
			final JpaFetch<?,?> jpaFetch = (JpaFetch<?, ?>) fetch;

			final SqmAttributeReference attrBinding = (SqmAttributeReference) parsingContext.findOrCreateNavigableBinding(
					sourceBinding,
					fetch.getAttribute().getName()
			);

			// todo : handle treats

			final SqmAttributeJoin sqmFetch = querySpecProcessingStateStack.getCurrent().getFromElementBuilder().buildAttributeJoin(
					attrBinding,
					interpretAlias( jpaFetch.getAlias() ),
					// todo : this is where treat would be applied
					null,
					convert( fetch.getJoinType() ),
					true,
					false
			);
			space.addJoin( sqmFetch );
			bindFetches( fetch, sqmFetch.getBinding(), space );
			jpaPathResolutionMap.put( jpaFetch, sqmFetch.getBinding() );
		}
	}

	private SqmJoinType convert(JoinType joinType) {
		switch ( joinType ) {
			case INNER: {
				return SqmJoinType.INNER;
			}
			case LEFT: {
				return SqmJoinType.LEFT;
			}
			case RIGHT: {
				return SqmJoinType.RIGHT;
			}
		}

		throw new ParsingException( "Unrecognized JPA JoinType : " + joinType );
	}

	private SqmSelectClause visitSelectClause(JpaQuerySpec<?> jpaQuerySpec) {
		final SqmSelectClause sqmSelectClause = new SqmSelectClause( jpaQuerySpec.getSelectClause().isDistinct() );

		jpaQuerySpec.getSelectClause().getSelection().visitSelections( this, sqmSelectClause );

		return sqmSelectClause;
	}

	@Override
	public void visitDynamicInstantiation(Class target, List<JpaExpression<?>> arguments) {
		final SqmDynamicInstantiation dynamicInstantiation;

		if ( List.class.equals( target ) ) {
			dynamicInstantiation = SqmDynamicInstantiation.forListInstantiation();
		}
		else if ( Map.class.equals( target ) ) {
			dynamicInstantiation = SqmDynamicInstantiation.forMapInstantiation();
		}
		else {
			dynamicInstantiation = SqmDynamicInstantiation.forClassInstantiation( target );
		}

		for ( JpaExpression<?> argument : arguments ) {
			dynamicInstantiation.add( argument.visitExpression( this ), argument.getAlias() );
		}
	}

	private String interpretAlias(String explicitAlias) {
		return isNotEmpty( explicitAlias )
				? explicitAlias
				: parsingContext.getImplicitAliasGenerator().buildUniqueImplicitAlias();
	}

	private static boolean isNotEmpty(String string) {
		return !isEmpty( string );
	}

	private static boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expressions

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Enum> ConstantEnumSqmExpression<T> visitEnumConstant(T value) {
		return new ConstantEnumSqmExpression<T>(
				value,
				parsingContext.getSessionFactory().getTypeConfiguration().getBasicTypeRegistry().getBasicType( value.getClass() )
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> LiteralSqmExpression<T> visitConstant(T value) {
		if ( value == null ) {
			throw new NullPointerException( "Value passed as `constant value` cannot be null" );
		}

		return visitConstant( value, (Class<T>) value.getClass() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> LiteralSqmExpression<T> visitConstant(T value, Class<T> javaType) {
		if ( Boolean.class.isAssignableFrom( javaType ) ) {
			if ( (Boolean) value ) {
				return (LiteralSqmExpression<T>) new LiteralTrueSqmExpression(
						resolveBasicExpressionType( Boolean.class )
				);
			}
			else {
				return (LiteralSqmExpression<T>) new LiteralFalseSqmExpression(
						resolveBasicExpressionType( Boolean.class )
				);
			}
		}
		else if ( Integer.class.isAssignableFrom( javaType ) ) {
			return (LiteralSqmExpression<T>) new LiteralIntegerSqmExpression(
					(Integer) value,
					resolveBasicExpressionType( Integer.class )
			);
		}
		else if ( Long.class.isAssignableFrom( javaType ) ) {
			return (LiteralSqmExpression<T>) new LiteralLongSqmExpression(
					(Long) value,
					resolveBasicExpressionType( Long.class )
			);
		}
		else if ( Float.class.isAssignableFrom( javaType ) ) {
			return (LiteralSqmExpression<T>) new LiteralFloatSqmExpression(
					(Float) value,
					resolveBasicExpressionType( Float.class )
			);
		}
		else if ( Double.class.isAssignableFrom( javaType ) ) {
			return (LiteralSqmExpression<T>) new LiteralDoubleSqmExpression(
					(Double) value,
					resolveBasicExpressionType( Double.class )
			);
		}
		else if ( BigInteger.class.isAssignableFrom( javaType ) ) {
			return (LiteralSqmExpression<T>) new LiteralBigIntegerSqmExpression(
					(BigInteger) value,
					resolveBasicExpressionType( BigInteger.class )
			);
		}
		else if ( BigDecimal.class.isAssignableFrom( javaType ) ) {
			return (LiteralSqmExpression<T>) new LiteralBigDecimalSqmExpression(
					(BigDecimal) value,
					resolveBasicExpressionType( BigDecimal.class )

			);
		}
		else if ( Character.class.isAssignableFrom( javaType ) ) {
			return (LiteralSqmExpression<T>) new LiteralCharacterSqmExpression(
					(Character) value,
					resolveBasicExpressionType( Character.class )
			);
		}
		else if ( String.class.isAssignableFrom( javaType ) ) {
			return (LiteralSqmExpression<T>) new LiteralStringSqmExpression(
					(String) value,
					resolveBasicExpressionType( String.class )
			);
		}

		throw new QueryException(
				"Unexpected literal expression [value=" + value +
						", javaType=" + javaType.getName() +
						"]; expecting boolean, int, long, float, double, BigInteger, BigDecimal, char, or String"
		);
	}

	private <T> BasicValuedExpressableType<T> resolveBasicExpressionType(Class<T> typeClass) {
		return parsingContext.getSessionFactory().getTypeConfiguration().getBasicTypeRegistry().getBasicType( typeClass );
	}

	@Override
	public UnaryOperationSqmExpression visitUnaryOperation(
			UnaryOperationSqmExpression.Operation operation,
			JpaExpression<?> expression) {
		return new UnaryOperationSqmExpression( operation, expression.visitExpression( this ) );
	}

	@Override
	public UnaryOperationSqmExpression visitUnaryOperation(
			UnaryOperationSqmExpression.Operation operation,
			JpaExpression<?> expression,
			BasicValuedExpressableType resultType) {
		return new UnaryOperationSqmExpression( operation, expression.visitExpression( this ), resultType );
	}

	@Override
	public BinaryArithmeticSqmExpression visitArithmetic(
			BinaryArithmeticSqmExpression.Operation operation,
			JpaExpression<?> expression1,
			JpaExpression<?> expression2) {
		final SqmExpression firstOperand = expression1.visitExpression( this );
		final SqmExpression secondOperand = expression2.visitExpression( this );
		return new BinaryArithmeticSqmExpression(
				operation,
				firstOperand,
				secondOperand,
				parsingContext.getSessionFactory().getTypeConfiguration().resolveArithmeticType(
						(BasicValuedExpressableType) firstOperand.getExpressionType(),
						(BasicValuedExpressableType) secondOperand.getExpressionType(),
						operation == BinaryArithmeticSqmExpression.Operation.DIVIDE
				)
		);
	}

	@Override
	public BinaryArithmeticSqmExpression visitArithmetic(
			BinaryArithmeticSqmExpression.Operation operation,
			JpaExpression<?> expression1,
			JpaExpression<?> expression2,
			BasicValuedExpressableType resultType) {
		return new BinaryArithmeticSqmExpression(
				operation,
				expression1.visitExpression( this ),
				expression2.visitExpression( this ),
				resultType
		);
	}

	@Override
	public SqmSingularAttributeReference visitAttributeReference(JpaFrom<?, ?> attributeSource, String attributeName) {
		// todo : see
		// todo : implement (especially leveraging the new pathToDomainBindingXref map)
		throw new NotYetImplementedException();

//		final DomainReferenceBinding source = currentQuerySpecProcessingState.findNavigableBindingByIdentificationVariable( attributeSource.getAlias() );
//		final Attribute attributeDescriptor = source.resolveAttribute( attributeName );
//		final Type type;
//		if ( attributeDescriptor instanceof SingularAttribute ) {
//			type = ( (SingularAttribute) attributeDescriptor ).getType();
//		}
//		else if ( attributeDescriptor instanceof PluralAttribute ) {
//			type = ( (PluralAttribute) attributeDescriptor ).getElementType();
//		}
//		else {
//			throw new ParsingException( "Resolved attribute was neither javax.persistence.metamodel.SingularAttribute nor javax.persistence.metamodel.PluralAttribute" );
//		}
//		return new AttributeReferenceSqmExpression( source, attributeDescriptor, null );
	}

	@Override
	public GenericFunctionSqmExpression visitFunction(
			String name,
			BasicValuedExpressableType resultTypeDescriptor,
			List<JpaExpression<?>> arguments) {
		final List<SqmExpression> sqmExpressions = new ArrayList<>();
		for ( JpaExpression<?> argument : arguments ) {
			sqmExpressions.add( argument.visitExpression( this ) );
		}

		return new GenericFunctionSqmExpression( name, resultTypeDescriptor, sqmExpressions );
	}

	@Override
	public GenericFunctionSqmExpression visitFunction(
			String name,
			BasicValuedExpressableType resultTypeDescriptor,
			JpaExpression<?>[] arguments) {
		// todo : handle the standard function calls specially...
		// for now always use the generic expression
		final List<SqmExpression> sqmArguments = new ArrayList<>();
		if ( arguments != null ) {
			for ( JpaExpression<?> expression : arguments ) {
				sqmArguments.add( expression.visitExpression( this ) );
			}

		}
		return new GenericFunctionSqmExpression(
				name,
				resultTypeDescriptor,
				sqmArguments
		);
	}

	@Override
	public AvgFunctionSqmExpression visitAvgFunction(JpaExpression<?> expression, boolean distinct) {
		final SqmExpression sqmExpression = expression.visitExpression( this );
		return new AvgFunctionSqmExpression(
				sqmExpression,
				distinct,
				(BasicValuedExpressableType) sqmExpression.getExpressionType()
		);
	}

	@Override
	public AvgFunctionSqmExpression visitAvgFunction(
			JpaExpression<?> expression,
			boolean distinct,
			BasicValuedExpressableType resultType) {
		return new AvgFunctionSqmExpression( expression.visitExpression( this ), distinct, resultType );
	}

	@Override
	public CountFunctionSqmExpression visitCountFunction(JpaExpression<?> expression, boolean distinct) {
		final SqmExpression sqmExpression = expression.visitExpression( this );
		return new CountFunctionSqmExpression(
				sqmExpression,
				distinct,
				(BasicValuedExpressableType) sqmExpression.getExpressionType()
		);
	}

	@Override
	public CountFunctionSqmExpression visitCountFunction(
			JpaExpression<?> expression,
			boolean distinct,
			BasicValuedExpressableType resultType) {
		return new CountFunctionSqmExpression( expression.visitExpression( this ), distinct, resultType );
	}

	@Override
	public CountStarFunctionSqmExpression visitCountStarFunction(boolean distinct) {
		return new CountStarFunctionSqmExpression(
				distinct,
				parsingContext.getSessionFactory().getTypeConfiguration().getBasicTypeRegistry().getBasicType( Long.class )
		);
	}

	@Override
	public CountStarFunctionSqmExpression visitCountStarFunction(boolean distinct, BasicValuedExpressableType resultType) {
		return new CountStarFunctionSqmExpression( distinct, resultType );
	}

	@Override
	public MaxFunctionSqmExpression visitMaxFunction(JpaExpression<?> expression, boolean distinct) {
		final SqmExpression sqmExpression = expression.visitExpression( this );
		return new MaxFunctionSqmExpression(
				sqmExpression,
				distinct,
				(BasicValuedExpressableType) sqmExpression.getExpressionType()
		);
	}

	@Override
	public MaxFunctionSqmExpression visitMaxFunction(
			JpaExpression<?> expression,
			boolean distinct,
			BasicValuedExpressableType resultType) {
		return new MaxFunctionSqmExpression( expression.visitExpression( this ), distinct, resultType );
	}

	@Override
	public MinFunctionSqmExpression visitMinFunction(JpaExpression<?> expression, boolean distinct) {
		final SqmExpression sqmExpression = expression.visitExpression( this );
		return new MinFunctionSqmExpression(
				sqmExpression,
				distinct,
				(BasicValuedExpressableType) sqmExpression.getExpressionType()
		);
	}

	@Override
	public MinFunctionSqmExpression visitMinFunction(
			JpaExpression<?> expression,
			boolean distinct,
			BasicValuedExpressableType resultType) {
		return new MinFunctionSqmExpression( expression.visitExpression( this ), distinct, resultType );
	}

	@Override
	public SumFunctionSqmExpression visitSumFunction(JpaExpression<?> expression, boolean distinct) {
		final SqmExpression sqmExpression = expression.visitExpression( this );
		return new SumFunctionSqmExpression(
				sqmExpression,
				distinct,
				parsingContext.getSessionFactory().getTypeConfiguration().resolveSumFunctionType( (BasicValuedExpressableType) sqmExpression.getExpressionType() )
		);
	}

	@Override
	public SumFunctionSqmExpression visitSumFunction(
			JpaExpression<?> expression,
			boolean distinct,
			BasicValuedExpressableType resultType) {
		return new SumFunctionSqmExpression( expression.visitExpression( this ), distinct, resultType );
	}

	@Override
	public ConcatSqmExpression visitConcat(JpaExpression<?> expression1, JpaExpression<?> expression2) {
		return new ConcatSqmExpression( expression1.visitExpression( this ), expression2.visitExpression( this ) );
	}

	@Override
	public ConcatSqmExpression visitConcat(
			JpaExpression<?> expression1,
			JpaExpression<?> expression2,
			BasicValuedExpressableType resultType) {
		return new ConcatSqmExpression(
				expression1.visitExpression( this ),
				expression2.visitExpression( this ),
				resultType
		);
	}

	@Override
	public CoalesceSqmExpression visitCoalesce(List<JpaExpression<?>> arguments) {
		final CoalesceSqmExpression coalesce = new CoalesceSqmExpression();
		arguments.forEach( argument -> coalesce.value( argument.visitExpression( this ) ) );
		return coalesce;
	}

	@Override
	public EntityTypeLiteralSqmExpression visitEntityType(String identificationVariable) {
		// todo : implement (especially leveraging the new pathToDomainBindingXref map)
		throw new NotYetImplementedException();

//		final SqmFrom fromElement = currentQuerySpecProcessingState.findNavigableBindingByIdentificationVariable( identificationVariable );
//		return new EntityTypeSqmExpression( (EntityType) fromElement.getBoundDomainReference() );
	}

	@Override
	public EntityTypeLiteralSqmExpression visitEntityType(String identificationVariable, String attributeName) {
		// todo : implement (especially leveraging the new pathToDomainBindingXref map)
		throw new NotYetImplementedException();

//		final SqmFrom fromElement = currentQuerySpecProcessingState.findNavigableBindingByIdentificationVariable( identificationVariable );
//		return new EntityTypeSqmExpression( (EntityType) fromElement.resolveAttribute( attributeName ) );
	}

	@Override
	public SubQuerySqmExpression visitSubQuery(JpaSubquery jpaSubquery) {
		final SqmQuerySpec subQuerySpec = visitQuerySpec( jpaSubquery.getQuerySpec() );

		return new SubQuerySqmExpression( subQuerySpec, determineSelectedExpressableType( subQuerySpec.getSelectClause() ) );
	}

	private static ExpressableType determineSelectedExpressableType(SqmSelectClause selectClause) {
		if ( selectClause.getSelections().size() != 0 ) {
			return null;
		}

		final SqmSelection selection = selectClause.getSelections().get( 0 );
		return selection.getExpression().getExpressionType();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates


	private SqmWhereClause visitWhereClause(JpaQuerySpec<?> jpaQuerySpec) {
		final SqmWhereClause whereClause = new SqmWhereClause();
		if ( jpaQuerySpec.getRestriction() != null ) {
			whereClause.setPredicate( jpaQuerySpec.getRestriction().visitPredicate( this ) );
		}
		return whereClause;
	}

	@Override
	public AndSqmPredicate visitAndPredicate(List<JpaPredicate> predicates) {
		final int predicateCount = predicates.size();

		if ( predicateCount < 2 ) {
			throw new QueryException(
					"Expecting 2 or more predicate expressions to form conjunction (AND), but found [" + predicateCount + "]"
			);
		}

		AndSqmPredicate result = new AndSqmPredicate(
				predicates.get( 0 ).visitPredicate( this ),
				predicates.get( 1 ).visitPredicate( this )
		);

		if ( predicateCount > 2 ) {
			for ( int i = 2; i < predicateCount; i++ ) {
				result = new AndSqmPredicate(
						result,
						predicates.get( i ).visitPredicate( this )
				);
			}
		}

		return result;
	}

	@Override
	public OrSqmPredicate visitOrPredicate(List<JpaPredicate> predicates) {
		final int predicateCount = predicates.size();

		if ( predicateCount < 2 ) {
			throw new QueryException(
					"Expecting 2 or more predicate expressions to form disjunction (OR), but found [" + predicateCount + "]"
			);
		}

		OrSqmPredicate result = new OrSqmPredicate(
				predicates.get( 0 ).visitPredicate( this ),
				predicates.get( 1 ).visitPredicate( this )
		);

		if ( predicateCount > 2 ) {
			for ( int i = 2; i < predicateCount; i++ ) {
				result = new OrSqmPredicate(
						result,
						predicates.get( i ).visitPredicate( this )
				);
			}
		}

		return result;
	}

	@Override
	public EmptinessSqmPredicate visitEmptinessPredicate(JpaPluralAttributePath pluralAttributePath, boolean negated) {
		// resolve the plural attribute binding
		final SqmNavigableSourceReference lhs = (SqmNavigableSourceReference) resolvePath( pluralAttributePath.getParentPath() );

		final SqmAttributeReference attributeBinding = (SqmAttributeReference) parsingContext.findOrCreateNavigableBinding(
				lhs,
				pluralAttributePath.getNavigable().getAttributeName()
		);
		if ( !SqmPluralAttributeReference.class.isInstance( attributeBinding ) ) {
			throw new ParsingException( "JpaPluralAttributePath resolved to non-PluralAttributeBinding : " + attributeBinding );
		}
		return new EmptinessSqmPredicate( (SqmPluralAttributeReference) attributeBinding, negated );
	}

	@Override
	public MemberOfSqmPredicate visitMemberOfPredicate(JpaPluralAttributePath pluralAttributePath, boolean negated) {
		throw new NotYetImplementedException();
	}

	@Override
	public BetweenSqmPredicate visitBetweenPredicate(
			JpaExpression<?> expression,
			JpaExpression<?> lowerBound,
			JpaExpression<?> upperBound,
			boolean negated) {
		return new BetweenSqmPredicate(
				expression.visitExpression( this ),
				lowerBound.visitExpression( this ),
				upperBound.visitExpression( this ),
				negated
		);
	}

	@Override
	public LikeSqmPredicate visitLikePredicate(
			JpaExpression<String> matchExpression,
			JpaExpression<String> pattern,
			JpaExpression<Character> escapeCharacter,
			boolean negated) {
		return new LikeSqmPredicate(
				matchExpression.visitExpression( this ),
				pattern.visitExpression( this ),
				escapeCharacter.visitExpression( this ),
				negated
		);
	}

	@Override
	public InSubQuerySqmPredicate visitInSubQueryPredicate(
			JpaExpression<?> testExpression,
			JpaSubquery<?> subquery,
			boolean negated) {
		return new InSubQuerySqmPredicate(
				testExpression.visitExpression( this ),
				visitSubQuery( subquery ),
				negated
		);
	}

	@Override
	public InListSqmPredicate visitInTupleListPredicate(
			JpaExpression<?> testExpression,
			List<JpaExpression<?>> expressionsList,
			boolean negated) {
		final List<SqmExpression> expressions = new ArrayList<>();
		for ( JpaExpression<?> expression : expressionsList ) {
			expressions.add( expression.visitExpression( this ) );
		}

		return new InListSqmPredicate(
				testExpression.visitExpression( this ),
				expressions,
				negated
		);
	}

	@Override
	public BooleanExpressionSqmPredicate visitBooleanExpressionPredicate(
			JpaExpression<Boolean> testExpression,
			Boolean assertValue) {
		// for now we only support TRUE assertions
		assert assertValue == Boolean.TRUE;
		return new BooleanExpressionSqmPredicate( testExpression.visitExpression( this ) );
	}

	@Override
	public SqmExpression visitRoot(JpaRoot root) {
		return querySpecProcessingStateStack.getCurrent().findNavigableBindingByIdentificationVariable( root.getAlias() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// New sigs


	@Override
	public SqmExpression visitParameter(String name, int position, Class javaType) {
		// todo : add hooks for knowing when we are in a context that allows "multi-valued parameter bindings"
		//		for now assume no...
		boolean canBeMultiValued = false;

		if ( isNotEmpty( name ) ) {
			return new NamedParameterSqmExpression( name, canBeMultiValued );
		}
		else {
			assert position >= 0;
			return new PositionalParameterSqmExpression( position, canBeMultiValued );
		}
	}

	@Override
	public <T, C> CastFunctionSqmExpression visitCastFunction(
			JpaExpression<T> expressionToCast,
			Class<C> castTarget) {
		return new CastFunctionSqmExpression(
				expressionToCast.visitExpression( this ),
				// ugh
				// todo : decide how we want to handle basic types in SQM
				parsingContext.getSessionFactory().getTypeConfiguration().resolveCastTargetType( castTarget.getName() )
		);
	}

	@Override
	public GenericFunctionSqmExpression visitGenericFunction(
			String functionName,
			BasicValuedExpressableType resultType,
			List<JpaExpression<?>> jpaArguments) {
		final List<SqmExpression> arguments;
		if ( jpaArguments != null && !jpaArguments.isEmpty() ) {
			arguments = new ArrayList<>();
			for ( JpaExpression<?> argument : jpaArguments ) {
				arguments.add( argument.visitExpression( this ) );
			}
		}
		else {
			arguments = Collections.emptyList();
		}

		return new GenericFunctionSqmExpression( functionName, resultType, arguments );
	}

	@Override
	public RelationalSqmPredicate visitRelationalPredicate(
			RelationalPredicateOperator operator,
			JpaExpression<?> lhs,
			JpaExpression<?> rhs) {
		return new RelationalSqmPredicate(
				operator,
				lhs.visitExpression( this ),
				rhs.visitExpression( this )
		);
	}

	@Override
	public NegatedSqmPredicate visitNegatedPredicate(JpaPredicate affirmativePredicate) {
		return new NegatedSqmPredicate( affirmativePredicate.visitPredicate( this ) );
	}

	@Override
	public NullnessSqmPredicate visitNullnessPredicate(JpaExpression<?> testExpression) {
		return new NullnessSqmPredicate( testExpression.visitExpression( this ) );
	}
}
