/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

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

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;

public abstract class QueryOptionsAdapter implements QueryOptions {
	private final LockOptions lockOptions = new LockOptions();

	@Override
	@Nonnull
	public Limit getLimit() {
		return Limit.NONE;
	}

	@Override
	@Nullable
	public Integer getFetchSize() {
		return null;
	}

	@Override
	@Nullable
	public String getComment() {
		return null;
	}

	@Override
	@Nonnull
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	@Nonnull
	public List<String> getDatabaseHints() {
		return emptyList();
	}

	@Override
	@Nullable
	public Timeout getTimeout() {
		return null;
	}

	@Override
	@Nonnull
	public QueryFlushMode getQueryFlushMode() {
		return QueryFlushMode.DEFAULT;
	}

	@Override
	@Nullable
	public Boolean isReadOnly() {
		return null;
	}

	@Override
	@Nullable
	public CacheRetrieveMode getCacheRetrieveMode() {
		return CacheRetrieveMode.BYPASS;
	}

	@Override
	@Nullable
	public CacheStoreMode getCacheStoreMode() {
		return CacheStoreMode.BYPASS;
	}

	@Override
	@Nullable
	public CacheMode getCacheMode() {
		return CacheMode.IGNORE;
	}

	@Override
	@Nullable
	public Boolean isResultCachingEnabled() {
		return null;
	}

	@Override
	@Nullable
	public Boolean getQueryPlanCachingEnabled() {
		return null;
	}

	@Override
	@Nullable
	public String getResultCacheRegionName() {
		return null;
	}

	@Override
	@Nullable
	public AppliedGraph getAppliedGraph() {
		return null;
	}

	@Override
	@Nullable
	public TupleTransformer<?> getTupleTransformer() {
		return null;
	}

	@Override
	@Nullable
	public ResultListTransformer<?> getResultListTransformer() {
		return null;
	}

	@Override
	@Nullable
	public Set<String> getEnabledFetchProfiles() {
		return null;
	}

	@Override
	@Nullable
	public Set<String> getDisabledFetchProfiles() {
		return null;
	}
}
