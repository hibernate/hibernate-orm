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
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.results.internal.TupleMetadata;

import java.util.List;

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

	@Override
	public SelectionQuery<R> setPage(KeyedPage<R> page) {
		setOrder( page.getKeyDefinition() );
		setMaxResults( page.getPage().getMaxResults() );
		if ( page.getKey() == null ) {
			setFirstResult( page.getPage().getFirstResult() );
		}
		else {
			addRestrictions( page.getKeyDefinition(), page.getKey() );
		}
		return this;
	}

	private void addRestrictions(List<Order<? super R>> keyDefinition, List<Comparable<?>> keyValues) {
		SqmSelectStatement<R> sqm = getSqmSelectStatement();
		sqm = sqm.copy( noParamCopyContext() );
		final SqmQuerySpec<R> querySpec = sqm.getQuerySpec();
		final SqmWhereClause whereClause = querySpec.getWhereClause();
		final List<? extends JpaSelection<?>> items = querySpec.getSelectClause().getSelectionItems();
		if ( items.size() == 1 ) {
			final JpaSelection<?> selected = items.get(0);
			for ( int i = 0; i < keyDefinition.size(); i++ ) {
				// ordering by an attribute of the returned entity
				if ( selected instanceof SqmRoot ) {
					whereClause.applyPredicate( keyPredicate(
							(SqmFrom<?,?>) selected,
							keyDefinition.get(i),
							(Comparable) keyValues.get(i))
					);
				}
				else {
					throw new IllegalQueryOperationException("Select item was not an entity type");
				}
			}
		}
		else {
			throw new IllegalQueryOperationException("Query has multiple items in the select list");
		}
		// TODO: when the QueryInterpretationCache can handle caching criteria queries,
		//       simply cache the new SQM as if it were a criteria query, and remove this:
		getQueryOptions().setQueryPlanCachingEnabled( false );
		setSqmStatement( sqm );
	}

	private <C extends Comparable<? super C>> SqmPredicate keyPredicate(
			SqmFrom<?, ?> selected, Order<? super R> key, C keyValue) {
		if ( !key.getEntityClass().isAssignableFrom( selected.getJavaType() ) ) {
			throw new IllegalQueryOperationException("Select item was of wrong entity type");
		}
		final Expression<? extends C> path = selected.get( key.getAttributeName() );
		final NodeBuilder builder = getSqmStatement().nodeBuilder();
		switch ( key.getDirection() ) {
			case ASCENDING:
				return builder.greaterThan( path, keyValue);
			case DESCENDING:
				return builder.lessThan( path, keyValue);
			default:
				throw new AssertionFailure("Unrecognized key direction");
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
}
