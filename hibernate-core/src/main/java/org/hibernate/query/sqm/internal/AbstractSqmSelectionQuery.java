/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;
import org.hibernate.query.Order;
import org.hibernate.query.Page;
import org.hibernate.query.QueryLogging;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.ValueHandlingMode;
import org.hibernate.query.hql.internal.QuerySplitter;
import org.hibernate.query.spi.AbstractSelectionQuery;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.results.internal.TupleMetadata;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hibernate.cfg.QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH;
import static org.hibernate.query.KeyedPage.KeyInterpretation.KEY_OF_FIRST_ON_NEXT_PAGE;
import static org.hibernate.query.sqm.internal.KeyBasedPagination.paginate;
import static org.hibernate.query.sqm.internal.KeyedResult.collectKeys;
import static org.hibernate.query.sqm.internal.KeyedResult.collectResults;
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

	private SqmSelectStatement<KeyedResult<R>> paginateQuery(
			List<Order<? super R>> keyDefinition, List<Comparable<?>> keyValues) {
		@SuppressWarnings("unchecked")
		final SqmSelectStatement<KeyedResult<R>> sqm =
				(SqmSelectStatement<KeyedResult<R>>)
						getSqmSelectStatement().copy( noParamCopyContext() );
		final NodeBuilder builder = sqm.nodeBuilder();
		//TODO: find a better way handle parameters
		builder.setCriteriaValueHandlingMode(ValueHandlingMode.INLINE);
		return paginate( keyDefinition, keyValues, sqm, builder );
	}


	@Override
	public KeyedResultList<R> getKeyedResultList(KeyedPage<R> keyedPage) {
		if ( keyedPage == null ) {
			throw new IllegalArgumentException( "KeyedPage was null" );
		}
		final Page page = keyedPage.getPage();
		final List<Comparable<?>> key = keyedPage.getKey();
		final List<Order<? super R>> keyDefinition = keyedPage.getKeyDefinition();
		final List<Order<? super R>> appliedKeyDefinition =
				keyedPage.getKeyInterpretation() == KEY_OF_FIRST_ON_NEXT_PAGE
						? Order.reverse(keyDefinition) : keyDefinition;

		setMaxResults( page.getMaxResults() + 1 );
		if ( key == null ) {
			setFirstResult( page.getFirstResult() );
		}

//		getQueryOptions().setQueryPlanCachingEnabled( false );
		final List<KeyedResult<R>> results =
				buildConcreteQueryPlan( paginateQuery( appliedKeyDefinition, key ), getQueryOptions() )
						.performList(this);

		return new KeyedResultList<>(
				collectResults( results, page.getSize(), keyedPage.getKeyInterpretation() ),
				collectKeys( results, page.getSize() ),
				keyedPage,
				nextPage( keyedPage, results ),
				previousPage( keyedPage, results )
		);
	}

	private static <R> KeyedPage<R> nextPage(KeyedPage<R> keyedPage, List<KeyedResult<R>> results) {
		if ( keyedPage.getKeyInterpretation() == KEY_OF_FIRST_ON_NEXT_PAGE ) {
			// the results come in reverse order
			return !results.isEmpty()
					? keyedPage.nextPage( results.get(0).getKey() )
					: null;
		}
		else {
			final int pageSize = keyedPage.getPage().getSize();
			return results.size() == pageSize + 1
					? keyedPage.nextPage( results.get(pageSize - 1).getKey() )
					: null;
		}
	}

	private static <R> KeyedPage<R> previousPage(KeyedPage<R> keyedPage, List<KeyedResult<R>> results) {
		if ( keyedPage.getKeyInterpretation() == KEY_OF_FIRST_ON_NEXT_PAGE ) {
			// the results come in reverse order
			final int pageSize = keyedPage.getPage().getSize();
			return results.size() == pageSize + 1
					? keyedPage.previousPage( results.get(pageSize - 1).getKey() )
					: null;
		}
		else {
			return !results.isEmpty()
					? keyedPage.previousPage( results.get(0).getKey() )
					: null;
		}
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
