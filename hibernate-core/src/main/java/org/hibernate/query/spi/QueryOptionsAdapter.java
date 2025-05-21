/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.List;
import java.util.Set;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;

import static java.util.Collections.emptyList;

public abstract class QueryOptionsAdapter implements QueryOptions {
	private final LockOptions lockOptions = new LockOptions();

	@Override
	public Limit getLimit() {
		return Limit.NONE;
	}

	@Override
	public Integer getFetchSize() {
		return null;
	}

	@Override
	public String getComment() {
		return null;
	}

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	public List<String> getDatabaseHints() {
		return emptyList();
	}

	@Override
	public Integer getTimeout() {
		return null;
	}

	@Override
	public FlushMode getFlushMode() {
		return null;
	}

	@Override
	public Boolean isReadOnly() {
		return null;
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return CacheRetrieveMode.BYPASS;
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return CacheStoreMode.BYPASS;
	}

	@Override
	public CacheMode getCacheMode() {
		return CacheMode.IGNORE;
	}

	@Override
	public Boolean isResultCachingEnabled() {
		return null;
	}

	@Override
	public Boolean getQueryPlanCachingEnabled() {
		return null;
	}

	@Override
	public String getResultCacheRegionName() {
		return null;
	}

	@Override
	public AppliedGraph getAppliedGraph() {
		return null;
	}

	@Override
	public TupleTransformer<?> getTupleTransformer() {
		return null;
	}

	@Override
	public ResultListTransformer<?> getResultListTransformer() {
		return null;
	}

	@Override
	public Set<String> getEnabledFetchProfiles() {
		return null;
	}

	@Override
	public Set<String> getDisabledFetchProfiles() {
		return null;
	}
}
