/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

/**
 * @author Steve Ebersole
 */
public class MutationQueryOptions implements QueryOptions {
	public static final MutationQueryOptions INSTANCE = new MutationQueryOptions();

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
	public Boolean isResultCachingEnabled() {
		return null;
	}

	@Override
	public Boolean getQueryPlanCachingEnabled() {
		return null;
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return null;
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return null;
	}

	@Override
	public String getResultCacheRegionName() {
		return null;
	}

	@Override
	public LockOptions getLockOptions() {
		return LockOptions.NONE;
	}

	@Override
	public String getComment() {
		return null;
	}

	@Override
	public List<String> getDatabaseHints() {
		return Collections.emptyList();
	}

	@Override
	public Integer getFetchSize() {
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

	@Override
	public Limit getLimit() {
		return LimitImpl.INSTANCE;
	}

	private static class LimitImpl extends Limit {
		public static final LimitImpl INSTANCE = new LimitImpl();

		@Override
		public void setFirstRow(Integer firstRow) {
		}

		@Override
		public void setMaxRows(int maxRows) {
		}

		@Override
		public void setMaxRows(Integer maxRows) {
		}

		@Override
		public Limit makeCopy() {
			return this;
		}
	}
}
