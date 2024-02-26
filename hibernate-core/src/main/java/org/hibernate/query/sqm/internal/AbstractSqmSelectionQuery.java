/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import jakarta.persistence.criteria.Expression;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;
import org.hibernate.query.Order;
import org.hibernate.query.Page;
import org.hibernate.query.QueryLogging;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.hql.internal.QuerySplitter;
import org.hibernate.query.spi.AbstractSelectionQuery;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.results.internal.TupleMetadata;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hibernate.cfg.QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH;
import static org.hibernate.query.sqm.internal.SqmUtil.sortSpecification;
import static org.hibernate.query.sqm.tree.SqmCopyContext.noParamCopyContext;

/**
 * @author Gavin King
 */
abstract class AbstractSqmSelectionQuery<R> extends AbstractSelectionQuery<R> {

	AbstractSqmSelectionQuery(SharedSessionContractImplementor session) {
		super(session);
	}

	protected int max(boolean hasLimit, SqmSelectStatement<?> sqmStatement, List<R> list) {
		return !hasLimit || getQueryOptions().getLimit().getMaxRows() == null
				? getMaxRows( sqmStatement, list.size() )
				: getQueryOptions().getLimit().getMaxRows();
	}

	protected int first(boolean hasLimit, SqmSelectStatement<?> sqmStatement) {
		return !hasLimit || getQueryOptions().getLimit().getFirstRow() == null
				? getIntegerLiteral( sqmStatement.getOffset(), 0 )
				: getQueryOptions().getLimit().getFirstRow();
	}

	protected static boolean hasLimit(SqmSelectStatement<?> sqm, MutableQueryOptions queryOptions) {
		return queryOptions.hasLimit() || sqm.getFetch() != null || sqm.getOffset() != null;
	}

	protected boolean needsDistinct(boolean containsCollectionFetches, boolean hasLimit, SqmSelectStatement<?> sqmStatement) {
		return containsCollectionFetches
				&& ( hasLimit || sqmStatement.usesDistinct() || hasAppliedGraph( getQueryOptions() ) );
	}

	protected static boolean hasAppliedGraph(MutableQueryOptions queryOptions) {
		final AppliedGraph appliedGraph = queryOptions.getAppliedGraph();
		return appliedGraph != null && appliedGraph.getSemantic() != null;
	}

	protected void errorOrLogForPaginationWithCollectionFetch() {
		if ( getSessionFactory().getSessionFactoryOptions().isFailOnPaginationOverCollectionFetchEnabled() ) {
			throw new HibernateException(
					"setFirstResult() or setMaxResults() specified with collection fetch join "
							+ "(in-memory pagination was about to be applied, but '"
							+ FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH
							+ "' is enabled)"
			);
		}
		else {
			QueryLogging.QUERY_MESSAGE_LOGGER.firstOrMaxResultsSpecifiedWithCollectionFetch();
		}
	}

	public abstract SqmStatement<R> getSqmStatement();
	protected abstract void setSqmStatement(SqmSelectStatement<R> statement);
	public abstract DomainParameterXref getDomainParameterXref();
	public abstract TupleMetadata getTupleMetadata();

	private SqmSelectStatement<R> getSqmSelectStatement() {
		final SqmStatement<R> sqmStatement = getSqmStatement();
		if ( sqmStatement instanceof SqmSelectStatement ) {
			return (SqmSelectStatement<R>) sqmStatement;
		}
		else {
			throw new IllegalSelectQueryException( "Not a select query" );
		}
	}

	@Override
	public SelectionQuery<R> setOrder(List<Order<? super R>> orderList) {
		SqmSelectStatement<R> sqm = getSqmSelectStatement();
		sqm = sqm.copy( noParamCopyContext() );
		final SqmSelectStatement<R> select = sqm;
		sqm.orderBy( orderList.stream().map( order -> sortSpecification( select, order ) )
				.collect( toList() ) );
		// TODO: when the QueryInterpretationCache can handle caching criteria queries,
		//       simply cache the new SQM as if it were a criteria query, and remove this:
		getQueryOptions().setQueryPlanCachingEnabled( false );
		setSqmStatement( sqm );
		return this;
	}


	@Override
	public SelectionQuery<R> setOrder(Order<? super R> order) {
		SqmSelectStatement<R> sqm = getSqmSelectStatement();
		sqm = sqm.copy( noParamCopyContext() );
		sqm.orderBy( sortSpecification( sqm, order ) );
		// TODO: when the QueryInterpretationCache can handle caching criteria queries,
		//       simply cache the new SQM as if it were a criteria query, and remove this:
		getQueryOptions().setQueryPlanCachingEnabled( false );
		setSqmStatement( sqm );
		return this;
	}

	@Override
	public SelectionQuery<R> setPage(Page page) {
		setMaxResults( page.getMaxResults() );
		setFirstResult( page.getFirstResult() );
		return this;
	}

	static class KeyedResult<R> {
		final R result;
		final List<Comparable<?>> key;
		public KeyedResult(R result, List<Comparable<?>> key) {
			this.result = result;
			this.key = key;
		}

		public R getResult() {
			return result;
		}

		public List<Comparable<?>> getKey() {
			return key;
		}
	}

	private SqmSelectStatement<KeyedResult<R>> keyedQuery(
			List<Order<? super R>> keyDefinition, List<Comparable<?>> keyValues) {
		@SuppressWarnings("unchecked")
		final SqmSelectStatement<KeyedResult<R>> sqm =
				(SqmSelectStatement<KeyedResult<R>>)
						getSqmSelectStatement().copy( noParamCopyContext() );
		final NodeBuilder builder = sqm.nodeBuilder();
		final SqmQuerySpec<?> querySpec = sqm.getQuerySpec();
		final List<? extends JpaSelection<?>> items = querySpec.getSelectClause().getSelectionItems();
		if ( items.size() == 1 ) {
			final JpaSelection<?> selected = items.get(0);
			if ( selected instanceof SqmRoot ) {
				final List<SqmPath<?>> newItems = new ArrayList<>();
				final SqmFrom<?,?> root = (SqmFrom<?,?>) selected;
				SqmPredicate restriction = querySpec.getRestriction();
				if ( restriction==null && keyValues != null ) {
					restriction = builder.disjunction();
				}
				for ( int i = 0; i < keyDefinition.size(); i++ ) {
					// ordering by an attribute of the returned entity
					final Order<? super R> key = keyDefinition.get(i);
					if ( keyValues != null ) {
						@SuppressWarnings("rawtypes")
						final Comparable keyValue = keyValues.get(i);
						restriction = builder.or( restriction,
								keyPredicate( root, key, keyValue, newItems, keyValues, builder ) );
					}
					newItems.add( root.get( key.getAttributeName() ) );
				}
				sqm.select( builder.construct((Class) KeyedResult.class,
						asList(selected, builder.construct(List.class, newItems)) ) );
				if ( keyValues != null ) {
					sqm.where( restriction );
				}
				return sqm;
			}
			else {
				throw new IllegalQueryOperationException("Select item was not an entity type");
			}
		}
		else {
			throw new IllegalQueryOperationException("Query has multiple items in the select list");
		}
	}

	private <C extends Comparable<? super C>> SqmPredicate keyPredicate(
			SqmFrom<?, ?> selected, Order<? super R> key, C keyValue,
			List<SqmPath<?>> previousKeys, List<Comparable<?>> keyValues,
			NodeBuilder builder) {
		if ( !key.getEntityClass().isAssignableFrom( selected.getJavaType() ) ) {
			throw new IllegalQueryOperationException("Select item was of wrong entity type");
		}
		final Expression<? extends C> path = selected.get( key.getAttributeName() );
		// TODO: use a parameter here and create a binding for it
//		@SuppressWarnings("unchecked")
//		final Class<C> valueClass = (Class<C>) keyValue.getClass();
//		final JpaParameterExpression<C> parameter = builder.parameter(valueClass);
//		setParameter( parameter, keyValue );
		SqmPredicate predicate;
		switch ( key.getDirection() ) {
			case ASCENDING:
				predicate = builder.greaterThan( path, keyValue );
				break;
			case DESCENDING:
				predicate = builder.lessThan( path, keyValue );
				break;
			default:
				throw new AssertionFailure("Unrecognized key direction");
		}
		for ( int i = 0; i < previousKeys.size(); i++ ) {
			final SqmPath keyPath = previousKeys.get(i);
			predicate = builder.and( predicate, keyPath.equalTo( keyValues.get(i) ) );
		}
		return predicate;
	}

	@Override
	public KeyedResultList<R> getKeyedResultList(KeyedPage<R> keyedPage) {
		if ( keyedPage == null ) {
			throw new IllegalStateException( "KeyedPage was not set" );
		}
		final Page page = keyedPage.getPage();
		final List<Order<? super R>> keyDefinition = keyedPage.getKeyDefinition();
		final List<Comparable<?>> key = keyedPage.getKey();

		setOrder( keyDefinition );
		setMaxResults( page.getMaxResults() );
		if ( key == null ) {
			setFirstResult( page.getFirstResult() );
		}

		final List<KeyedResult<R>> executed =
				buildConcreteQueryPlan( keyedQuery( keyDefinition, key ), getQueryOptions() )
						.performList(this);
		final KeyedPage<R> nextPage = keyedPage.nextPage( getKeyOfLastResult( executed, key ) );
		return new KeyedResultList<>( collectResultList( executed ), keyedPage, nextPage );
	}

	private static <R> List<R> collectResultList(List<KeyedResult<R>> executed) {
		return executed.stream()
				.map(KeyedResult::getResult)
				.collect(toList());
	}

	private static <R> List<Comparable<?>> getKeyOfLastResult(
			List<KeyedResult<R>> executed, List<Comparable<?>> key) {
		return executed.isEmpty() ? key : executed.get(executed.size() - 1).getKey();
	}

	public abstract Class<R> getExpectedResultType();

	protected SelectQueryPlan<R> buildSelectQueryPlan() {
		final SqmSelectStatement<R> statement = (SqmSelectStatement<R>) getSqmStatement();
		final SqmSelectStatement<R>[] concreteSqmStatements = QuerySplitter.split( statement );
		return concreteSqmStatements.length > 1
				? buildAggregatedQueryPlan( concreteSqmStatements )
				: buildConcreteQueryPlan( concreteSqmStatements[0] );
	}

	private SelectQueryPlan<R> buildAggregatedQueryPlan(SqmSelectStatement<R>[] concreteSqmStatements) {
		@SuppressWarnings("unchecked")
		final SelectQueryPlan<R>[] aggregatedQueryPlans = new SelectQueryPlan[ concreteSqmStatements.length ];
		// todo (6.0) : we want to make sure that certain thing (ResultListTransformer, etc) only get applied at the aggregator-level
		for ( int i = 0, length = concreteSqmStatements.length; i < length; i++ ) {
			aggregatedQueryPlans[i] = buildConcreteQueryPlan( concreteSqmStatements[i] );
		}
		return new AggregatedSelectQueryPlanImpl<>( aggregatedQueryPlans );
	}

	protected SelectQueryPlan<R> buildConcreteQueryPlan(SqmSelectStatement<R> concreteSqmStatement) {
		return buildConcreteQueryPlan(
				concreteSqmStatement,
				getExpectedResultType(),
				getTupleMetadata(),
				getQueryOptions()
		);
	}

	protected <T> ConcreteSqmSelectQueryPlan<T> buildConcreteQueryPlan(
			SqmSelectStatement<T> concreteSqmStatement,
			Class<T> expectedResultType,
			TupleMetadata tupleMetadata,
			QueryOptions queryOptions) {
		return new ConcreteSqmSelectQueryPlan<>(
				concreteSqmStatement,
				getQueryString(),
				getDomainParameterXref(),
				expectedResultType,
				tupleMetadata,
				queryOptions
		);
	}

	private <T> SelectQueryPlan<T> buildConcreteQueryPlan(SqmSelectStatement<T> sqmStatement, QueryOptions options) {
		return buildConcreteQueryPlan( sqmStatement, null, null, options );
	}
}
