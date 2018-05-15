/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.Collections;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.query.Limit;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.sql.ast.produce.spi.SqlQueryOptions;

/**
 * @author Steve Ebersole
 */
public interface QueryOptions extends SqlQueryOptions {
	Limit getLimit();
	Integer getFetchSize();
	String getComment();
	LockOptions getLockOptions();
	List<String> getDatabaseHints();

	Integer getTimeout();
	FlushMode getFlushMode();
	Boolean isReadOnly();
	CacheMode getCacheMode();
	Boolean isResultCachingEnabled();
	String getResultCacheRegionName();

	AppliedGraph getAppliedGraph();

	TupleTransformer getTupleTransformer();
	ResultListTransformer getResultListTransformer();

	default Limit getEffectiveLimit() {
		final Limit explicit = getLimit();
		return explicit != null ? explicit : Limit.NONE;
	}

	default boolean hasLimit() {
		final Limit limit = getLimit();
		if ( limit != null ) {
			if ( limit.getFirstRow() != null ) {
				return true;
			}
			if ( limit.getMaxRows() != null ) {
				return true;
			}
		}

		return false;
	}

	@Override
	default Integer getFirstRow() {
		return getLimit().getFirstRow();
	}

	@Override
	default Integer getMaxRows() {
		return getLimit().getMaxRows();
	}

	/**
	 * Singleton access
	 */
	QueryOptions NONE = new QueryOptions() {
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
			return LockOptions.NONE;
		}

		@Override
		public List<String> getDatabaseHints() {
			return Collections.emptyList();
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
		public CacheMode getCacheMode() {
			return null;
		}

		@Override
		public Boolean isResultCachingEnabled() {
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
		public TupleTransformer getTupleTransformer() {
			return null;
		}

		@Override
		public ResultListTransformer getResultListTransformer() {
			return null;
		}
	};
}
