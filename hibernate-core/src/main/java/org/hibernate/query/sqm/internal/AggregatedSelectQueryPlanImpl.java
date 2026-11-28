package org.hibernate.query.sqm.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.internal.scrollable.EmptyScrollableResults;
import org.hibernate.query.internal.DelegatingDomainQueryExecutionContext;
import org.hibernate.query.spi.DelegatingQueryOptions;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.sql.results.spi.ResultsConsumer;

import static java.util.Collections.emptyList;

/**
 * @author Steve Ebersole
 */
public class AggregatedSelectQueryPlanImpl<R> implements SelectQueryPlan<R> {
	private final SelectQueryPlan<R>[] aggregatedQueryPlans;

	public AggregatedSelectQueryPlanImpl(SelectQueryPlan<R>[] aggregatedQueryPlans) {
		this.aggregatedQueryPlans = aggregatedQueryPlans;
	}

	@Override
	public <T> T executeQuery(DomainQueryExecutionContext executionContext, ResultsConsumer<T, R> resultsConsumer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<R> performList(DomainQueryExecutionContext executionContext) {
		final Limit effectiveLimit = executionContext.getQueryOptions().getEffectiveLimit();
		int remainingRows = effectiveLimit.getMaxRowsJpa();
		if ( remainingRows == 0 ) {
			return emptyList();
		}
		int elementsToSkip = effectiveLimit.getFirstRowJpa();
		final List<R> overallResults = new ArrayList<>();

		for ( var aggregatedQueryPlan : aggregatedQueryPlans ) {
			final List<R> list = aggregatedQueryPlan.performList(
					effectiveLimit.isEmpty()
							? executionContext
							: componentExecutionContext( executionContext, effectiveLimit, elementsToSkip, remainingRows )
			);
			final int skipped = Math.min( elementsToSkip, list.size() );
			elementsToSkip -= skipped;
			final int included = Math.min( remainingRows, list.size() - skipped );
			overallResults.addAll( list.subList( skipped, skipped + included ) );
			remainingRows -= included;
			if ( remainingRows == 0 ) {
				break;
			}
		}

		return overallResults;
	}

	private static DomainQueryExecutionContext componentExecutionContext(
			DomainQueryExecutionContext executionContext, Limit effectiveLimit, int elementsToSkip, int remainingRows) {
		final var queryOptions = executionContext.getQueryOptions();
		final long requiredRows = (long) elementsToSkip + remainingRows;
		// A list transformer may discard results, so a SQL row limit cannot bound its output.
		final Integer maxRows = effectiveLimit.getMaxRows() != null
				&& requiredRows <= Integer.MAX_VALUE
				&& queryOptions.isLimitInMemoryEnabled() != Boolean.TRUE
				&& queryOptions.getResultListTransformer() == null
				? (int) requiredRows : null;
		final var limit = new Limit( null, maxRows );
		final var componentOptions = new DelegatingQueryOptions( queryOptions ) {
			@Override
			public Limit getLimit() {
				return limit;
			}

			@Override
			public Limit peekOriginalLimit() {
				return limit;
			}

			@Override
			public Limit getEffectiveLimit() {
				return limit;
			}

			@Override
			public Integer getFirstRow() {
				return null;
			}

			@Override
			public Integer getMaxRows() {
				return maxRows;
			}

			@Override
			public boolean hasLimit() {
				return !limit.isEmpty();
			}
		};
		return new DelegatingDomainQueryExecutionContext( executionContext ) {
			@Override
			public QueryOptions getQueryOptions() {
				return componentOptions;
			}

			@Override
			public Class<?> getResultType() {
				return executionContext.getResultType();
			}
		};
	}

	@Override
	public ScrollableResults<R> performScroll(ScrollMode scrollMode, DomainQueryExecutionContext executionContext) {
		if ( executionContext.getQueryOptions().getEffectiveLimit().getMaxRowsJpa() == 0 ) {
			return EmptyScrollableResults.instance();
		}
		throw new UnsupportedOperationException();
	}
}
