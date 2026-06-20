/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.LockOptions;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.internal.SqlOmittingQueryOptions;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import java.sql.Statement;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates options for the execution of a query.
 *
 * @apiNote Note that not all options are relevant for every type of query.
 *
 * @author Steve Ebersole
 */
public interface QueryOptions {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query options

	/**
	 * The query execution timeout. May alternatively
	 * be specified at the transaction level using
	 * {@link org.hibernate.Transaction#getTimeout}.
	 */
	@Nullable
	Timeout getTimeout();

	/**
	 * The flush mode to use for the query execution.
	 */
	@Nonnull
	QueryFlushMode getQueryFlushMode();

	/**
	 * Are entities returned by the query marked as
	 * read-only?
	 */
	@Nullable
	Boolean isReadOnly();

	/**
	 * An {@link jakarta.persistence.EntityGraph}
	 * explicitly applied to the query.
	 */
	@Nullable
	AppliedGraph getAppliedGraph();

	/**
	 * Transformer applied to the query to transform
	 * the structure of each "row" of the results.
	 */
	@Nullable
	TupleTransformer<?> getTupleTransformer();

	/**
	 * Transformer applied to the query to transform
	 * the structure of the overall result list.
	 */
	@Nullable
	ResultListTransformer<?> getResultListTransformer();

	/**
	 * Should results from the query be cached?
	 *
	 * @see #getCacheMode
	 * @see #getResultCacheRegionName
	 */
	@Nullable
	Boolean isResultCachingEnabled();

	/**
	 * Controls whether query results are read from the cache.
	 * Has no effect unless {@link #isResultCachingEnabled}
	 * returns {@code true}
	 *
	 * @see CacheMode
	 */
	@Nullable
	CacheRetrieveMode getCacheRetrieveMode();

	/**
	 * Controls whether query results are put into the cache.
	 * Has no effect unless {@link #isResultCachingEnabled}
	 * returns {@code true}.
	 *
	 * @see CacheMode
	 */
	@Nullable
	CacheStoreMode getCacheStoreMode();

	@Nullable
	default CacheMode getCacheMode() {
		return CacheMode.fromJpaModes( getCacheRetrieveMode(), getCacheStoreMode() );
	}

	/**
	 * The query cache region in which the results should be cached.
	 * Has no effect unless {@link #isResultCachingEnabled} returns
	 * {@code true}.
	 */
	@Nullable
	String getResultCacheRegionName();

	/**
	 * Should the query plan of the query be cached?
	 */
	@Nullable
	Boolean getQueryPlanCachingEnabled();

	/**
	 * Whether top-level HQL/criteria pagination should be
	 * handled in memory instead of by the SQL query.
	 */
	@Nullable
	default Boolean isLimitInMemoryEnabled() {
		return null;
	}

	/**
	 * The explicitly enabled profiles for this query.
	 */
	@Nullable
	Set<String> getEnabledFetchProfiles();

	/**
	 * The explicitly disabled profiles for this query.
	 */
	@Nullable
	Set<String> getDisabledFetchProfiles();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JDBC / SQL options

	/**
	 * Describes the locking to apply to the query results.
	 */
	@Nonnull
	LockOptions getLockOptions();

	/**
	 * The SQL comment to apply to the interpreted SQL query,
	 * for dialects which support SQL comments.
	 */
	@Nullable
	String getComment();

	/**
	 * Hints to apply to the interpreted SQL query.
	 */
	@Nonnull
	List<String> getDatabaseHints();

	/**
	 * The fetch size to be applied to the JDBC query.
	 *
	 * @see Statement#getFetchSize
	 */
	@Nullable
	Integer getFetchSize();

	/**
	 * The limit to the query results.  May also be accessed
	 * via {@link #getFirstRow} and {@link #getMaxRows}.
	 */
	@Nonnull
	Limit getLimit();

	/**
	 * The original {@link Limit} as set by the application,
	 * before any wrapper ({@link SqlOmittingQueryOptions} or the
	 * scroll execution context) hid it from {@link #getLimit()}.
	 * Used by runtime parameter binders that the SQM-to-SQL
	 * converter pushed into the SQL AST so they can bind the
	 * original value even when {@link #getLimit()} has been
	 * intentionally suppressed for SQL rendering.
	 */
	@Nonnull
	default Limit peekOriginalLimit() {
		return getLimit();
	}

	/**
	 * The first row from the results to return.
	 *
	 * @see #getLimit
	 */
	@Nullable
	default Integer getFirstRow() {
		return getLimit().getFirstRow();
	}

	/**
	 * The maximum number of rows to return from the results.
	 *
	 * @see #getLimit
	 */
	@Nullable
	default Integer getMaxRows() {
		return getLimit().getMaxRows();
	}

	/**
	 * Determine the effective paging limit to apply to the
	 * query. If the application did not explicitly specify
	 * paging limits, {@link Limit#NONE} is returned.
	 *
	 * @see #getLimit
	 */
	@Nonnull
	default Limit getEffectiveLimit() {
		return getLimit();
	}

	/**
	 * Did the application explicitly request paging limits?
	 *
	 * @see #getLimit
	 */
	default boolean hasLimit() {
		final var limit = getLimit();
		return limit.getFirstRow() != null
			|| limit.getMaxRows() != null;
	}

	@Nullable
	default ListResultsConsumer.UniqueSemantic getUniqueSemantic() {
		return null;
	}

	/**
	 * Whether this execution goes through {@code scroll()} /
	 * {@code getResultStream()} and therefore needs SQL row
	 * ordering stable enough for scroll-style result grouping.
	 */
	default boolean isScrollExecution() {
		return false;
	}

	/**
	 * Provide singleton access for a frequently needed option.
	 */
	QueryOptions NONE = new QueryOptionsAdapter() {
	};

	QueryOptions READ_WRITE = new QueryOptionsAdapter() {
		@Override
		public Boolean isReadOnly() {
			return Boolean.FALSE;
		}
	};

	QueryOptions READ_ONLY = new QueryOptionsAdapter() {
		@Override
		public Boolean isReadOnly() {
			return Boolean.TRUE;
		}
	};

}
