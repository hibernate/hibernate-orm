/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import org.hibernate.LockOptions;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;

/**
 * @author Steve Ebersole
 */
public class MutationQueryOptions implements QueryOptions {
	public static final MutationQueryOptions INSTANCE = new MutationQueryOptions();

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
	public CacheRetrieveMode getCacheRetrieveMode() {
		return null;
	}

	@Override
	@Nullable
	public CacheStoreMode getCacheStoreMode() {
		return null;
	}

	@Override
	@Nullable
	public String getResultCacheRegionName() {
		return null;
	}

	@Override
	@Nonnull
	public LockOptions getLockOptions() {
		return LockOptions.NONE;
	}

	@Override
	@Nullable
	public String getComment() {
		return null;
	}

	@Override
	@Nonnull
	public List<String> getDatabaseHints() {
		return emptyList();
	}

	@Override
	@Nullable
	public Integer getFetchSize() {
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

	@Override
	@Nonnull
	public Limit getLimit() {
		return LimitImpl.INSTANCE;
	}

	private static class LimitImpl extends Limit {
		public static final LimitImpl INSTANCE = new LimitImpl();

		@Override
		public void setFirstRow(@Nullable Integer firstRow) {
		}

		@Override
		public void setMaxRows(int maxRows) {
		}

		@Override
		public void setMaxRows(@Nullable Integer maxRows) {
		}

		@Override
		public Limit makeCopy() {
			return this;
		}
	}
}
