/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Selection;

import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.expression.ValueBindJpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.jpa.ParameterCollector;

/**
 * @author Steve Ebersole
 */
public class SqmSelectStatement<T> extends AbstractSqmSelectQuery<T> implements JpaCriteriaQuery<T>, SqmStatement<T>,
		org.hibernate.query.sqm.internal.ParameterCollector {
	private final SqmQuerySource querySource;

	private Set<SqmParameter<?>> parameters;

	public SqmSelectStatement(NodeBuilder nodeBuilder) {
		this( SqmQuerySource.HQL, nodeBuilder );
	}

	public SqmSelectStatement(SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		super( null, nodeBuilder );
		this.querySource = querySource;
	}

	public SqmSelectStatement(Class<T> resultJavaType, SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		super( resultJavaType, nodeBuilder );
		this.querySource = querySource;
	}

	public SqmSelectStatement(
			SqmQueryPart<T> queryPart,
			Class<T> resultType,
			SqmQuerySource querySource,
			NodeBuilder builder) {
		super( queryPart, resultType, builder );
		this.querySource = querySource;
	}

	public SqmSelectStatement(
			SqmQueryPart<T> queryPart,
			Class<T> resultType,
			Map<String, SqmCteStatement<?>> cteStatements,
			SqmQuerySource querySource,
			NodeBuilder builder) {
		super( builder, cteStatements, resultType );
		this.querySource = querySource;
		setQueryPart( queryPart );
	}

	/**
	 * @implNote This form is used from Hibernate's JPA criteria handling.
	 */
	public SqmSelectStatement(Class<T> resultJavaType, NodeBuilder nodeBuilder) {
		super( resultJavaType, nodeBuilder );

		this.querySource = SqmQuerySource.CRITERIA;

		getQuerySpec().setSelectClause( new SqmSelectClause( false, nodeBuilder ) );
		getQuerySpec().setFromClause( new SqmFromClause() );
	}

	private SqmSelectStatement(
			NodeBuilder builder,
			Map<String, SqmCteStatement<?>> cteStatements,
			Class<T> resultType,
			SqmQuerySource querySource,
			Set<SqmParameter<?>> parameters) {
		super( builder, cteStatements, resultType );
		this.querySource = querySource;
		this.parameters = parameters;
	}

	@Override
	public SqmSelectStatement<T> copy(SqmCopyContext context) {
		final SqmSelectStatement<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		return createCopy( context, getResultType() );
	}

	private <X> SqmSelectStatement<X> createCopy(SqmCopyContext context, Class<X> resultType) {
		final Set<SqmParameter<?>> parameters;
		if ( this.parameters == null ) {
			parameters = null;
		}
		else {
			parameters = new LinkedHashSet<>( this.parameters.size() );
			for ( SqmParameter<?> parameter : this.parameters ) {
				parameters.add( parameter.copy( context ) );
			}
		}
		//noinspection unchecked
		final SqmSelectStatement<X> statement = (SqmSelectStatement<X>) context.registerCopy(
				this,
				new SqmSelectStatement<>(
						nodeBuilder(),
						copyCteStatements( context ),
						resultType,
						getQuerySource(),
						parameters
				)
		);
		//noinspection unchecked
		statement.setQueryPart( (SqmQueryPart<X>) getQueryPart().copy( context ) );
		return statement;
	}

	@Override
	public SqmQuerySource getQuerySource() {
		return querySource;
	}

	@Override
	public SqmQuerySpec<T> getQuerySpec() {
		if ( querySource == SqmQuerySource.CRITERIA ) {
			final SqmQueryPart<T> queryPart = getQueryPart();
			if ( queryPart instanceof SqmQuerySpec<?> ) {
				return (SqmQuerySpec<T>) queryPart;
			}
			throw new IllegalStateException(
					"Query group can't be treated as query spec. Use JpaSelectCriteria#getQueryPart to access query group details"
			);
		}
		else {
			return super.getQuerySpec();
		}
	}

	public boolean producesUniqueResults() {
		return producesUniqueResults( getQueryPart() );
	}

	private boolean producesUniqueResults(SqmQueryPart<?> queryPart) {
		if ( queryPart instanceof SqmQuerySpec<?> ) {
			return ( (SqmQuerySpec<?>) queryPart ).producesUniqueResults();
		}
		else {
			// For query groups we have to assume that duplicates are possible
			return true;
		}
	}

	public boolean containsCollectionFetches() {
		return containsCollectionFetches( getQueryPart() );
	}

	private boolean containsCollectionFetches(SqmQueryPart<?> queryPart) {
		if ( queryPart instanceof SqmQuerySpec<?> ) {
			return ( (SqmQuerySpec<?>) queryPart ).containsCollectionFetches();
		}
		else {
			// We only have to check the first one
			final SqmQueryGroup<?> queryGroup = (SqmQueryGroup<?>) queryPart;
			return containsCollectionFetches( queryGroup.getQueryParts().get( 0 ) );
		}
	}

	public boolean usesDistinct() {
		return usesDistinct( getQueryPart() );
	}

	private boolean usesDistinct(SqmQueryPart<?> queryPart) {
		if ( queryPart instanceof SqmQuerySpec<?> ) {
			return ( (SqmQuerySpec<?>) queryPart ).getSelectClause().isDistinct();
		}
		else {
			// We only have to check the first one
			final SqmQueryGroup<?> queryGroup = (SqmQueryGroup<?>) queryPart;
			return usesDistinct( queryGroup.getQueryParts().get( 0 ) );
		}
	}

	@Override
	public Set<SqmParameter<?>> getSqmParameters() {
		if ( querySource == SqmQuerySource.CRITERIA ) {
			assert parameters == null : "SqmSelectStatement (as Criteria) should not have collected parameters";

			return ParameterCollector.collectParameters(
					this,
					sqmParameter -> {},
					nodeBuilder().getServiceRegistry()
			);
		}

		return parameters == null ? Collections.emptySet() : Collections.unmodifiableSet( parameters );
	}

	@Override
	public ParameterResolutions resolveParameters() {
		return SqmUtil.resolveParameters( this );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitSelectStatement( this );
	}

	@Override
	public void addParameter(SqmParameter<?> parameter) {
		if ( parameters == null ) {
			parameters = new LinkedHashSet<>();
		}

		parameters.add( parameter );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmSelectStatement<T> distinct(boolean distinct) {
		return (SqmSelectStatement<T>) super.distinct( distinct );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<ParameterExpression<?>> getParameters() {
		// At this level, the number of parameters may still be growing as
		// nodes are added to the Criteria - so we re-calculate this every
		// time.
		//
		// for a "finalized" set of parameters, use `#resolveParameters` instead
		assert querySource == SqmQuerySource.CRITERIA;
		final Set<ParameterExpression<?>> sqmParameters = (Set<ParameterExpression<?>>) (Set<?>) getSqmParameters();
		return sqmParameters.stream()
				.filter( parameterExpression -> !( parameterExpression instanceof ValueBindJpaCriteriaParameter ) )
				.collect( Collectors.toSet() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmSelectStatement<T> select(Selection<? extends T> selection) {
		if ( nodeBuilder().getDomainModel().getJpaCompliance().isJpaQueryComplianceEnabled() ) {
			checkSelectionIsJpaCompliant( selection );
		}
		getQuerySpec().setSelection( (JpaSelection<T>) selection );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmSelectStatement<T> multiselect(Selection<?>... selections) {
		if ( nodeBuilder().getDomainModel().getJpaCompliance().isJpaQueryComplianceEnabled() ) {
			for ( Selection<?> selection : selections ) {
				checkSelectionIsJpaCompliant( selection );
			}
		}

		final Selection<? extends T> resultSelection = getResultSelection( selections );
		getQuerySpec().getSelectClause().setSelection( (SqmSelectableNode<?>) resultSelection );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmSelectStatement<T> multiselect(List<Selection<?>> selectionList) {
		if ( nodeBuilder().getDomainModel().getJpaCompliance().isJpaQueryComplianceEnabled() ) {
			for ( Selection<?> selection : selectionList ) {
				checkSelectionIsJpaCompliant( selection );
			}
		}

		final Selection<? extends T> resultSelection;
		final Class<T> resultType = getResultType();
		final List<? extends JpaSelection<?>> selections = (List<? extends JpaSelection<?>>) (List<?>) selectionList;
		if ( resultType == null || resultType == Object.class ) {
			switch ( selections.size() ) {
				case 0: {
					throw new IllegalArgumentException(
							"empty selections passed to criteria query typed as Object"
					);
				}
				case 1: {
					resultSelection = ( Selection<? extends T> ) selections.get( 0 );
					break;
				}
				default: {
					resultSelection = ( Selection<? extends T> ) nodeBuilder().array( selections );
				}
			}
		}
		else if ( Tuple.class.isAssignableFrom( resultType ) ) {
			resultSelection = ( Selection<? extends T> ) nodeBuilder().tuple( selections );
		}
		else if ( resultType.isArray() ) {
			resultSelection = nodeBuilder().array( resultType, selections );
		}
		else {
			resultSelection = nodeBuilder().construct( resultType, selections );
		}

		getQuerySpec().getSelectClause().setSelection( (SqmSelectableNode<?>) resultSelection );

		return this;
	}

	private void checkSelectionIsJpaCompliant(Selection<?> selection) {
		if ( selection instanceof SqmSubQuery<?> ) {
			throw new IllegalStateException(
					"The JPA specification does not support subqueries in select clauses. " +
							"Please disable the JPA query compliance if you want to use this feature." );
		}
	}

	@Override
	public SqmSelectStatement<T> orderBy(Order... orders) {
		final SqmOrderByClause sqmOrderByClause = new SqmOrderByClause( orders.length );
		for ( Order order : orders ) {
			sqmOrderByClause.addSortSpecification( (SqmSortSpecification) order );
		}
		getQueryPart().setOrderByClause( sqmOrderByClause );
		return this;
	}

	@Override
	public SqmSelectStatement<T> orderBy(List<Order> orders) {
		final SqmOrderByClause sqmOrderByClause = new SqmOrderByClause( orders.size() );
		for ( Order order : orders ) {
			sqmOrderByClause.addSortSpecification( (SqmSortSpecification) order );
		}
		getQueryPart().setOrderByClause( sqmOrderByClause );
		return this;
	}

	@Override
	public <U> SqmSubQuery<U> subquery(Class<U> type) {
		return new SqmSubQuery<>( this, type, nodeBuilder() );
	}

	@Override
	public SqmSelectStatement<T> where(Expression<Boolean> restriction) {
		return (SqmSelectStatement<T>) super.where( restriction );
	}

	@Override
	public SqmSelectStatement<T> where(Predicate... restrictions) {
		return (SqmSelectStatement<T>) super.where( restrictions );
	}

	@Override
	public SqmSelectStatement<T> groupBy(Expression<?>... expressions) {
		return (SqmSelectStatement<T>) super.groupBy( expressions );
	}

	@Override
	public SqmSelectStatement<T> groupBy(List<Expression<?>> grouping) {
		return (SqmSelectStatement<T>) super.groupBy( grouping );
	}

	@Override
	public SqmSelectStatement<T> having(Expression<Boolean> booleanExpression) {
		return (SqmSelectStatement<T>) super.having( booleanExpression );
	}

	@Override
	public SqmSelectStatement<T> having(Predicate... predicates) {
		return (SqmSelectStatement<T>) super.having( predicates );
	}

	@Override
	public JpaExpression<Number> getOffset() {
		//noinspection unchecked
		return (JpaExpression<Number>) getQueryPart().getOffset();
	}

	@Override
	public JpaCriteriaQuery<T> offset(JpaExpression<? extends Number> offset) {
		validateComplianceFetchOffset();
		getQueryPart().setOffset( offset );
		return this;
	}

	@Override
	public JpaCriteriaQuery<T> offset(Number offset) {
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
	public JpaCriteriaQuery<T> fetch(JpaExpression<? extends Number> fetch) {
		validateComplianceFetchOffset();
		getQueryPart().setFetch( fetch );
		return this;
	}

	@Override
	public JpaCriteriaQuery<T> fetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType) {
		validateComplianceFetchOffset();
		getQueryPart().setFetch( fetch, fetchClauseType );
		return this;
	}

	@Override
	public JpaCriteriaQuery<T> fetch(Number fetch) {
		validateComplianceFetchOffset();
		getQueryPart().setFetch( nodeBuilder().value( fetch ) );
		return this;
	}

	@Override
	public JpaCriteriaQuery<T> fetch(Number fetch, FetchClauseType fetchClauseType) {
		validateComplianceFetchOffset();
		getQueryPart().setFetch( nodeBuilder().value( fetch ), fetchClauseType );
		return this;
	}

	@Override
	public FetchClauseType getFetchClauseType() {
		return getQueryPart().getFetchClauseType();
	}

	private void validateComplianceFetchOffset() {
		if ( nodeBuilder().getDomainModel().getJpaCompliance().isJpaQueryComplianceEnabled() ) {
			throw new IllegalStateException(
					"The JPA specification does not support the fetch or offset clause. " +
							"Please disable the JPA query compliance if you want to use this feature." );
		}
	}
}
