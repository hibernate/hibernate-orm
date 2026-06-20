/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.List;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import org.hibernate.CacheMode;
import org.hibernate.LockOptions;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.sql.results.spi.ListResultsConsumer;

/**
 * @author Christian Beikov
 */
public class DelegatingQueryOptions implements QueryOptions {

	private final QueryOptions queryOptions;

	public DelegatingQueryOptions(@Nonnull QueryOptions queryOptions) {
		this.queryOptions = queryOptions;
	}

	@Override
	@Nullable
	public Timeout getTimeout() {
		return queryOptions.getTimeout();
	}

	@Override
	@Nonnull
	public QueryFlushMode getQueryFlushMode() {
		return queryOptions.getQueryFlushMode();
	}

	@Override
	@Nullable
	public Boolean isReadOnly() {
		return queryOptions.isReadOnly();
	}

	@Override
	@Nullable
	public AppliedGraph getAppliedGraph() {
		return queryOptions.getAppliedGraph();
	}

	@Override
	@Nullable
	public TupleTransformer<?> getTupleTransformer() {
		return queryOptions.getTupleTransformer();
	}

	@Override
	@Nullable
	public ResultListTransformer<?> getResultListTransformer() {
		return queryOptions.getResultListTransformer();
	}

	@Override
	@Nullable
	public Boolean isResultCachingEnabled() {
		return queryOptions.isResultCachingEnabled();
	}

	@Override
	@Nullable
	public CacheRetrieveMode getCacheRetrieveMode() {
		return queryOptions.getCacheRetrieveMode();
	}

	@Override
	@Nullable
	public CacheStoreMode getCacheStoreMode() {
		return queryOptions.getCacheStoreMode();
	}

	@Override
	@Nullable
	public Boolean getQueryPlanCachingEnabled() {
		return queryOptions.getQueryPlanCachingEnabled();
	}

	@Override
	@Nullable
	public Boolean isLimitInMemoryEnabled() {
		return queryOptions.isLimitInMemoryEnabled();
	}

	@Override
	@Nullable
	public CacheMode getCacheMode() {
		return queryOptions.getCacheMode();
	}

	@Override
	@Nullable
	public String getResultCacheRegionName() {
		return queryOptions.getResultCacheRegionName();
	}

	@Override
	@Nonnull
	public LockOptions getLockOptions() {
		return queryOptions.getLockOptions();
	}

	@Override
	@Nullable
	public String getComment() {
		return queryOptions.getComment();
	}

	@Override
	@Nonnull
	public List<String> getDatabaseHints() {
		return queryOptions.getDatabaseHints();
	}

	@Override
	@Nullable
	public Integer getFetchSize() {
		return queryOptions.getFetchSize();
	}

	@Override
	@Nullable
	public Set<String> getEnabledFetchProfiles() {
		return queryOptions.getEnabledFetchProfiles();
	}

	@Override
	@Nullable
	public Set<String> getDisabledFetchProfiles() {
		return queryOptions.getDisabledFetchProfiles();
	}

	@Override
	@Nonnull
	public Limit getLimit() {
		return queryOptions.getLimit();
	}

	@Override
	@Nonnull
	public Limit peekOriginalLimit() {
		return queryOptions.peekOriginalLimit();
	}

	@Override
	@Nullable
	public Integer getFirstRow() {
		return queryOptions.getFirstRow();
	}

	@Override
	@Nullable
	public Integer getMaxRows() {
		return queryOptions.getMaxRows();
	}

	@Override
	@Nonnull
	public Limit getEffectiveLimit() {
		return queryOptions.getEffectiveLimit();
	}

	@Override
	public boolean hasLimit() {
		return queryOptions.hasLimit();
	}

	@Override
	@Nullable
	public ListResultsConsumer.UniqueSemantic getUniqueSemantic() {
		return queryOptions.getUniqueSemantic();
	}

	@Override
	public boolean isScrollExecution() {
		return queryOptions.isScrollExecution();
	}
}
