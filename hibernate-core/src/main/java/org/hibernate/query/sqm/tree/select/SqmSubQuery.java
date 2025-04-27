/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaCrossJoin;
import org.hibernate.query.criteria.JpaCteContainer;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaEntityJoin;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmQuery;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.cte.SqmCteContainer;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedCrossJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedEntityJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedListJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedRoot;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedSingularValuedJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularValuedJoin;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmInPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.PluralJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;
import org.checkerframework.checker.nullness.qual.Nullable;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;

import static org.hibernate.query.sqm.spi.SqmCreationHelper.combinePredicates;

/**
 * @author Steve Ebersole
 */
public class SqmSubQuery<T> extends AbstractSqmSelectQuery<T>
		implements SqmSelectQuery<T>, JpaSubQuery<T>, SqmExpression<T> {
	private final SqmQuery<?> parent;

	private SqmExpressible<T> expressibleType;
	private String alias;

	public SqmSubQuery(
			SqmQuery<?> parent,
			SqmQueryPart<T> queryPart,
			Class<T> resultType,
			NodeBuilder builder) {
		super( queryPart, resultType, builder );
		this.parent = parent;
		applyInferableType( resultType );
	}

	public SqmSubQuery(
			SqmQuery<?> parent,
			SqmQueryPart<T> queryPart,
			Class<T> resultType,
			Map<String, SqmCteStatement<?>> cteStatements,
			NodeBuilder builder) {
		super( builder, cteStatements, resultType );
		this.parent = parent;
		setQueryPart( queryPart );
		applyInferableType( resultType );
	}

	public SqmSubQuery(
			SqmQuery<?> parent,
			Class<T> resultType,
			NodeBuilder builder) {
		super( resultType, builder );
		this.parent = parent;
		applyInferableType( resultType );
	}

	public SqmSubQuery(
			SqmQuery<?> parent,
			EntityType<T> resultType,
			NodeBuilder builder) {
		super( resultType.getJavaType(), builder );
		this.parent = parent;
		applyInferableType( resultType.getJavaType() );
	}

	public SqmSubQuery(
			SqmQuery<?> parent,
			NodeBuilder builder) {
		super( null, builder );
		this.parent = parent;
	}

	private SqmSubQuery(
			NodeBuilder builder,
			Map<String, SqmCteStatement<?>> cteStatements,
			Class<T> resultType,
			SqmQuery<?> parent,
			SqmExpressible<T> expressibleType,
			String alias) {
		super( builder, cteStatements, resultType );
		this.parent = parent;
		this.expressibleType = expressibleType;
		this.alias = alias;
	}

	@Override
	public SqmSubQuery<T> copy(SqmCopyContext context) {
		final SqmSubQuery<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmSubQuery<T> statement = context.registerCopy(
				this,
				new SqmSubQuery<>(
						nodeBuilder(),
						copyCteStatements( context ),
						getResultType(),
						parent.copy( context ),
						getExpressible(),
						getAlias()
				)
		);
		statement.setQueryPart( getQueryPart().copy( context ) );
		return statement;
	}

	@Override
	public Integer getTupleLength() {
		final SqmSelectClause selectClause = getQuerySpec().getSelectClause();
		return selectClause != null ?
				getTupleLength( selectClause.getSelectionItems() ) :
				null;
	}

	private int getTupleLength(List<SqmSelectableNode<?>> selectionItems) {
		int count = 0;
		for ( SqmSelectableNode<?> selection : selectionItems ) {
			count += selection.getTupleLength();
		}
		return count;
	}

	@Override
	public SqmCteStatement<?> getCteStatement(String cteLabel) {
		final SqmCteStatement<?> cteCriteria = super.getCteStatement( cteLabel );
		return cteCriteria == null && parent instanceof SqmCteContainer cteContainer
				? cteContainer.getCteStatement( cteLabel )
				: cteCriteria;
	}

	@Override
	public <X> JpaCteCriteria<X> getCteCriteria(String cteName) {
		final JpaCteCriteria<X> cteCriteria = super.getCteCriteria( cteName );
		return cteCriteria == null && parent instanceof JpaCteContainer cteContainer
				? cteContainer.getCteCriteria( cteName )
				: cteCriteria;
	}

	@Override
	protected <X> JpaCteCriteria<X> withInternal(String name, AbstractQuery<X> criteria) {
		if ( criteria instanceof SqmSubQuery<X> sqmSubQuery && sqmSubQuery.getParent() == parent ) {
			return super.withInternal( name, criteria );
		}
		else {
			throw new IllegalArgumentException(
					"Invalid query type provided to subquery 'with' method, " +
					"expecting a subquery with the same parent to use as CTE"
			);
		}
	}

	@Override
	protected <X> JpaCteCriteria<X> withInternal(
			String name,
			AbstractQuery<X> baseCriteria,
			boolean unionDistinct,
			Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		if ( baseCriteria instanceof SqmSubQuery<X> sqmSubQuery && sqmSubQuery.getParent() == parent ) {
			return super.withInternal( name, baseCriteria, unionDistinct, recursiveCriteriaProducer );
		}
		else {
			throw new IllegalArgumentException(
					"Invalid query type provided to subquery 'with' method, " +
					"expecting a subquery with the same parent to use as CTE"
			);
		}
	}

	@Override
	public SqmQuery<?> getContainingQuery() {
		return parent;
	}

	@Override
	public SqmSelectQuery<?> getParent() {
		final SqmQuery<?> containingQuery = getContainingQuery();
		// JPA only allows sub-queries on select queries
		if ( containingQuery instanceof SqmSelectQuery<?> sqmSelectQuery ) {
			return sqmSelectQuery;
		}
		else {
			throw new IllegalStateException( "Cannot call getParent() on update/delete criteria" );
		}
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public SqmSubQuery<T> alias(String alias) {
		this.alias = alias;
		return this;
	}

	@Override
	public SqmSubQuery<T> select(Expression<T> expression) {
		final SqmQuerySpec<T> querySpec = getQuerySpec();
		if ( querySpec.getSelectClause() == null ) {
			querySpec.setSelectClause( new SqmSelectClause( false, 1, nodeBuilder() ) );
		}
		//noinspection unchecked
		querySpec.setSelection( (JpaSelection<T>) expression );
//		applyInferableType( (Class<T>) querySpec.getSelection().getJavaType() );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmSubQuery<T> multiselect(Selection<?>... selections) {
		validateComplianceMultiselect();
		final Selection<? extends T> resultSelection = getResultSelection( selections );
		final SqmQuerySpec<T> querySpec = getQuerySpec();
		if ( querySpec.getSelectClause() == null ) {
			querySpec.setSelectClause( new SqmSelectClause( false, 1, nodeBuilder() ) );
		}
		querySpec.setSelection( (JpaSelection<T>) resultSelection );
		return this;
	}

	@Override
	public SqmSubQuery<T> multiselect(List<Selection<?>> selectionList) {
		validateComplianceMultiselect();
		final SqmQuerySpec<T> querySpec = getQuerySpec();
		if ( querySpec.getSelectClause() == null ) {
			querySpec.setSelectClause( new SqmSelectClause( false, 1, nodeBuilder() ) );
		}
		querySpec.setSelection( getResultSelection( selectionList ) );
		return this;
	}

	private JpaSelection<T> getResultSelection(List<Selection<?>> selections) {
		final Class<T> resultType = getResultType();
		if ( resultType == null || resultType == Object.class ) {
			final JpaSelection<?> selection = switch ( selections.size() ) {
				case 0 -> throw new IllegalArgumentException(
						"empty selections passed to criteria query typed as Object" );
				case 1 -> (JpaSelection<?>) selections.get( 0 );
				default -> nodeBuilder().array( selections );
			};
			//noinspection unchecked
			return (JpaSelection<T>) selection;
		}
		else if ( Tuple.class.isAssignableFrom( resultType ) ) {
			//noinspection unchecked
			return (JpaSelection<T>) nodeBuilder().tuple( selections );
		}
		else if ( resultType.isArray() ) {
			return nodeBuilder().array( resultType, selections );
		}
		else {
			return nodeBuilder().construct( resultType, selections );
		}
	}

	@Override
	public SqmExpression<T> getSelection() {
		final SqmSelectClause selectClause = getQuerySpec().getSelectClause();
		return selectClause == null ? null : this;
	}

	@Override
	public boolean isCompoundSelection() {
		return getQuerySpec().getSelection().isCompoundSelection();
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		return null;
	}

	@Override
	public List<Selection<?>> getCompoundSelectionItems() {
		if ( ! isCompoundSelection() ) {
			throw new IllegalStateException( "JPA selection is not compound" );
		}
		return getQuerySpec().getSelection().getCompoundSelectionItems();
	}

	@Override
	public SqmSubQuery<T> distinct(boolean distinct) {
		return (SqmSubQuery<T>) super.distinct( distinct );
	}

	@Override
	public SqmSubQuery<T> where(Expression<Boolean> restriction) {
		return (SqmSubQuery<T>) super.where( restriction );
	}

	@Override
	public SqmSubQuery<T> where(Predicate... restrictions) {
		return (SqmSubQuery<T>) super.where( restrictions );
	}

	@Override
	public SqmSubQuery<T> groupBy(Expression<?>... expressions) {
		return (SqmSubQuery<T>) super.groupBy( expressions );
	}

	@Override
	public SqmSubQuery<T> groupBy(List<Expression<?>> grouping) {
		return (SqmSubQuery<T>) super.groupBy( grouping );
	}

	@Override
	public SqmSubQuery<T> having(Expression<Boolean> booleanExpression) {
		return (SqmSubQuery<T>) super.having( booleanExpression );
	}

	@Override
	public SqmSubQuery<T> having(Predicate... predicates) {
		return (SqmSubQuery<T>) super.having( predicates );
	}

	@Override
	public JpaExpression<Number> getOffset() {
		//noinspection unchecked
		return (JpaExpression<Number>) getQueryPart().getOffset();
	}

	@Override
	public JpaSubQuery<T> offset(JpaExpression<? extends Number> offset) {
		validateComplianceFetchOffset();
		getQueryPart().setOffset( offset );
		return this;
	}

	@Override
	public JpaSubQuery<T> offset(Number offset) {
		validateComplianceFetchOffset();
		getQueryPart().setOffset( nodeBuilder().value( offset ) );
		return this;
	}

	@Override
	public JpaExpression<Number> getFetch() {
		//noinspection unchecked
		return (JpaExpression<Number>) getQueryPart().getFetch();
	}

	@Override
	public JpaSubQuery<T> fetch(JpaExpression<? extends Number> fetch) {
		validateComplianceFetchOffset();
		getQueryPart().setFetch( fetch );
		return this;
	}

	@Override
	public JpaSubQuery<T> fetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType) {
		validateComplianceFetchOffset();
		getQueryPart().setFetch( fetch, fetchClauseType );
		return this;
	}

	@Override
	public JpaSubQuery<T> fetch(Number fetch) {
		validateComplianceFetchOffset();
		getQueryPart().setFetch( nodeBuilder().value( fetch ) );
		return this;
	}

	@Override
	public JpaSubQuery<T> fetch(Number fetch, FetchClauseType fetchClauseType) {
		validateComplianceFetchOffset();
		getQueryPart().setFetch( nodeBuilder().value( fetch ), fetchClauseType );
		return this;
	}

	@Override
	public FetchClauseType getFetchClauseType() {
		return getQueryPart().getFetchClauseType();
	}

	@Override
	public List<JpaOrder> getOrderList() {
		//noinspection rawtypes,unchecked
		return (List) getQueryPart().getSortSpecifications();
	}

	@Override
	public JpaSubQuery<T> orderBy(Order... orders) {
		validateComplianceOrderBy();
		final SqmOrderByClause sqmOrderByClause = new SqmOrderByClause( orders.length );
		for ( Order order : orders ) {
			sqmOrderByClause.addSortSpecification( (SqmSortSpecification) order );
		}
		getQueryPart().setOrderByClause( sqmOrderByClause );
		return this;
	}

	@Override
	public JpaSubQuery<T> orderBy(List<Order> orders) {
		validateComplianceOrderBy();
		final SqmOrderByClause sqmOrderByClause = new SqmOrderByClause( orders.size() );
		for ( Order order : orders ) {
			sqmOrderByClause.addSortSpecification( (SqmSortSpecification) order );
		}
		getQueryPart().setOrderByClause( sqmOrderByClause );
		return this;
	}

	private void validateComplianceMultiselect() {
		if ( nodeBuilder().isJpaQueryComplianceEnabled() ) {
			throw new IllegalStateException(
					"The JPA specification does not support subqueries having multiple select items. " +
							"Please disable the JPA query compliance if you want to use this feature." );
		}
	}

	private void validateComplianceOrderBy() {
		if ( nodeBuilder().isJpaQueryComplianceEnabled() ) {
			throw new IllegalStateException(
					"The JPA specification does not support subqueries having an order by clause. " +
							"Please disable the JPA query compliance if you want to use this feature." );
		}
	}

	private void validateComplianceFetchOffset() {
		if ( nodeBuilder().isJpaQueryComplianceEnabled() ) {
			throw new IllegalStateException(
					"The JPA specification does not support subqueries having a fetch or offset clause. " +
							"Please disable the JPA query compliance if you want to use this feature." );
		}
	}

	@Override
	public <Y> SqmRoot<Y> correlate(Root<Y> parentRoot) {
		final SqmCorrelatedRoot<Y> correlated = ( (SqmRoot<Y>) parentRoot ).createCorrelation();
		getQuerySpec().addRoot( correlated );
		return correlated;
	}

	@Override
	public <X, Y> SqmCorrelatedJoin<X, Y> correlate(Join<X, Y> join) {
		if ( join instanceof PluralJoin<?, ?, ?> pluralJoin ) {
			return switch ( pluralJoin.getModel().getCollectionType() ) {
				case COLLECTION -> correlate( (CollectionJoin<X, Y>) join );
				case LIST -> correlate( (ListJoin<X, Y>) join );
				case SET -> correlate( (SetJoin<X, Y>) join );
				case MAP -> correlate( (MapJoin<X, ?, Y>) join );
			};
		}
		final SqmCorrelatedSingularValuedJoin<X, Y> correlated =
				( (SqmSingularValuedJoin<X, Y>) join ).createCorrelation();
		getQuerySpec().addRoot( correlated.getCorrelatedRoot() );
		return correlated;
	}

	@Override
	public <X, Y> SqmCorrelatedBagJoin<X, Y> correlate(CollectionJoin<X, Y> parentCollection) {
		final SqmCorrelatedBagJoin<X, Y> correlated =
				( (SqmBagJoin<X, Y>) parentCollection ).createCorrelation();
		getQuerySpec().addRoot( correlated.getCorrelatedRoot() );
		return correlated;
	}

	@Override
	public <X, Y> SqmCorrelatedSetJoin<X, Y> correlate(SetJoin<X, Y> parentSet) {
		final SqmCorrelatedSetJoin<X, Y> correlated =
				( (SqmSetJoin<X, Y>) parentSet ).createCorrelation();
		getQuerySpec().addRoot( correlated.getCorrelatedRoot() );
		return correlated;
	}

	@Override
	public <X, Y> SqmCorrelatedListJoin<X, Y> correlate(ListJoin<X, Y> parentList) {
		final SqmCorrelatedListJoin<X, Y> correlated =
				( (SqmListJoin<X, Y>) parentList ).createCorrelation();
		getQuerySpec().addRoot( correlated.getCorrelatedRoot() );
		return correlated;
	}

	@Override
	public <X, K, V> SqmCorrelatedMapJoin<X, K, V> correlate(MapJoin<X, K, V> parentMap) {
		final SqmCorrelatedMapJoin<X, K, V> correlated =
				( (SqmMapJoin<X, K, V>) parentMap ).createCorrelation();
		getQuerySpec().addRoot( correlated.getCorrelatedRoot() );
		return correlated;
	}

	@Override
	public <X> SqmCorrelatedCrossJoin<X> correlate(JpaCrossJoin<X> parentCrossJoin) {
		final SqmCorrelatedCrossJoin<X> correlated =
				((SqmCrossJoin<X>) parentCrossJoin).createCorrelation();
		getQuerySpec().addRoot( correlated.getCorrelatedRoot() );
		return correlated;
	}

	@Override
	public <X> JpaEntityJoin<T, X> correlate(JpaEntityJoin<T, X> parentEntityJoin) {
		final SqmCorrelatedEntityJoin<T,X> correlated =
				((SqmEntityJoin<T,X>) parentEntityJoin).createCorrelation();
		getQuerySpec().addRoot( correlated.getCorrelatedRoot() );
		return correlated;
	}

	@Override
	public Set<Join<?, ?>> getCorrelatedJoins() {
		final Set<Join<?, ?>> correlatedJoins = new HashSet<>();
		final SqmFromClause fromClause = getQuerySpec().getFromClause();
		if ( fromClause != null ) {
			for ( SqmRoot<?> root : fromClause.getRoots() ) {
				if ( root instanceof SqmCorrelation<?, ?> ) {
					for ( SqmJoin<?, ?> sqmJoin : root.getSqmJoins() ) {
						if ( sqmJoin instanceof SqmCorrelation<?, ?> ) {
							correlatedJoins.add( sqmJoin );
						}
					}
				}
			}
		}
		return correlatedJoins;
	}

//	@Override
//	public Set<SqmJoin<?, ?>> getCorrelatedSqmJoins() {
//		final Set<SqmJoin<?, ?>> correlatedJoins = new HashSet<>();
//		for ( SqmRoot<?> root : getQuerySpec().getFromClause().getRoots() ) {
//			if ( root instanceof SqmCorrelation<?, ?> ) {
//				for ( SqmJoin<?, ?> sqmJoin : root.getSqmJoins() ) {
//					if ( sqmJoin instanceof SqmCorrelation<?, ?> ) {
//						correlatedJoins.add( sqmJoin );
//					}
//				}
//			}
//		}
//		return correlatedJoins;
//	}

	@Override
	public SqmPredicate isNull() {
		return nodeBuilder().isNull( this );
	}

	@Override
	public SqmPredicate isNotNull() {
		return nodeBuilder().isNotNull( this );
	}

	@Override
	public SqmPredicate equalTo(Expression<?> that) {
		return nodeBuilder().equal( this, that );
	}

	@Override
	public SqmPredicate equalTo(Object that) {
		return nodeBuilder().equal( this, that );
	}

	@Override
	public SqmInPredicate<?> in(Object... values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmInPredicate<?> in(Expression<?>... values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmInPredicate<?> in(Collection<?> values) {
		//noinspection unchecked
		return nodeBuilder().in( this, (Collection<T>) values );
	}

	@Override
	public SqmInPredicate<?> in(Expression<Collection<?>> values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public @Nullable SqmExpressible<T> getNodeType() {
		return expressibleType;
	}

	@Override
	public void applyInferableType(@Nullable SqmExpressible<?> type) {
		//noinspection unchecked
		expressibleType = (SqmExpressible<T>) type;
	}

	private void applyInferableType(Class<T> type) {
		if ( type != null ) {
			final NodeBuilder nodeBuilder = nodeBuilder();
			final EntityDomainType<T> entityDescriptor = nodeBuilder.getDomainModel().findEntityType( type );
			expressibleType =
					entityDescriptor != null
							? entityDescriptor.resolveExpressible( nodeBuilder )
							: nodeBuilder.getTypeConfiguration().getBasicTypeForJavaType( type );
		}
	}

	@Override
	public SqmExpression<Long> asLong() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( Long.class ) );
	}

	@Override
	public SqmExpression<Integer> asInteger() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( Integer.class ) );
	}

	@Override
	public SqmExpression<Float> asFloat() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( Float.class ) );
	}

	@Override
	public SqmExpression<Double> asDouble() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( Double.class ) );
	}

	@Override
	public SqmExpression<BigDecimal> asBigDecimal() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( BigDecimal.class ) );
	}

	@Override
	public SqmExpression<BigInteger> asBigInteger() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( BigInteger.class ) );
	}

	@Override
	public SqmExpression<String> asString() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( String.class ) );
	}

	@Override
	public <X> SqmExpression<X> as(Class<X> type) {
		return nodeBuilder().cast( this, type );
	}

	@Override
	public JavaType<T> getJavaTypeDescriptor() {
		if ( getNodeType() == null ) {
			return null;
		}
		return getNodeType().getExpressibleJavaType();
	}

	@Override
	public Class<? extends T> getJavaType() {
		return getResultType();
	}

	@Override
	public <U> SqmSubQuery<U> subquery(Class<U> type) {
		return new SqmSubQuery<>( this, type, nodeBuilder() );
	}

	@Override
	public <U> Subquery<U> subquery(EntityType<U> type) {
		return new SqmSubQuery<>( this, type, nodeBuilder() );
	}

	@Override
	public Subquery<T> where(List<Predicate> restrictions) {
		//noinspection rawtypes,unchecked
		getQuerySpec().getWhereClause().applyPredicates( (List) restrictions );
		return this;
	}

	@Override
	public Subquery<T> having(List<Predicate> restrictions) {
		//noinspection unchecked,rawtypes
		final SqmPredicate combined = combinePredicates( getQuerySpec().getHavingClausePredicate(), (List) restrictions );
		getQuerySpec().setHavingClausePredicate( combined );
		return this;
	}

	@Override
	public Set<ParameterExpression<?>> getParameters() {
		return Collections.emptySet();
	}

	@Override
	public JpaPredicate notEqualTo(Expression<?> value) {
		return nodeBuilder().notEqual( this, value );
	}

	@Override
	public JpaPredicate notEqualTo(Object value) {
		return nodeBuilder().notEqual( this, value );
	}

	@Override
	public <X> SqmExpression<X> cast(Class<X> targetType) {
		return nodeBuilder().cast( this, targetType );
	}

	@Override
	public String asLoggableText() {
		return "<sub-query>";
	}

	@Override
	public <T1> T1 accept(SemanticQueryWalker<T1> walker) {
		return walker.visitSubQueryExpression( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( '(' );
		super.appendHqlString( hql, context );
		hql.append( ')' );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmSubQuery<?> that
			&& Objects.equals( this.alias, that.alias )
			&& super.equals( object );
	}

	@Override
	public int hashCode() {
		return Objects.hash( super.hashCode(), alias );
	}

	@Override
	public String generateAlias() {
		return parent.generateAlias();
	}
}
