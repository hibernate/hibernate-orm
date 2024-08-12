/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.sql.Statement;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.sql.results.spi.ListResultsConsumer;

/**
 * Encapsulates options for the execution of a HQL/Criteria/native query
 *
 * @author Steve Ebersole
 */
public interface QueryOptions {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query options

	/**
	 * The timeout to apply to the query.  May also be defined at the transaction
	 * level using {@link org.hibernate.Transaction#getTimeout}
	 */
	Integer getTimeout();

	/**
	 * The flush mode to use for the query execution
	 */
	FlushMode getFlushMode();

	/**
	 * Should entities returned from the query be marked read-only.
	 */
	Boolean isReadOnly();

	/**
	 * JPA {@link jakarta.persistence.EntityGraph} explicitly applied to the
	 * query.
	 */
	AppliedGraph getAppliedGraph();

	/**
	 * Transformer applied to the query to transform the structure of each "row"
	 * in the results
	 */
	@Nullable TupleTransformer<?> getTupleTransformer();

	/**
	 * Transformer applied to the query to transform the structure of the
	 * overall results
	 */
	@Nullable ResultListTransformer<?> getResultListTransformer();

	/**
	 * Should results from the query be cached?
	 *
	 * @see #getCacheMode
	 * @see #getResultCacheRegionName
	 */
	Boolean isResultCachingEnabled();

	/**
	 * Controls whether query results are read from the cache.
	 * No effect unless {@link #isResultCachingEnabled} returns
	 * {@code true}
	 *
	 * @see CacheMode
	 */
	CacheRetrieveMode getCacheRetrieveMode();

	/**
	 * Controls whether query results are put into the cache.
	 * No effect unless {@link #isResultCachingEnabled} returns
	 * {@code true}
	 *
	 * @see CacheMode
	 */
	CacheStoreMode getCacheStoreMode();

	default CacheMode getCacheMode() {
		return CacheMode.fromJpaModes( getCacheRetrieveMode(), getCacheStoreMode() );
	}

	/**
	 * The query cache region in which the results should be cached.  No
	 * effect unless {@link #isResultCachingEnabled} returns {@code true}
	 */
	String getResultCacheRegionName();

	/**
	 * Should the query plan of the query be cached?
	 */
	Boolean getQueryPlanCachingEnabled();

	/**
	 * The explicitly enabled profiles for this query
	 */
	Set<String> getEnabledFetchProfiles();

	/**
	 * The explicitly disabled profiles for this query
	 */
	Set<String> getDisabledFetchProfiles();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JDBC / SQL options

	/**
	 * Describes the locking to apply to the query results
	 */
	LockOptions getLockOptions();

	/**
	 * The SQL comment to apply to the interpreted SQL query, for dialects which
	 * support SQL comments
	 */
	String getComment();

	/**
	 * Hints to apply to the interpreted SQL query
	 */
	List<String> getDatabaseHints();

	/**
	 * The fetch size to be applied to the JDBC query.
	 *
	 * @see Statement#getFetchSize
	 */
	Integer getFetchSize();

	/**
	 * The limit to the query results.  May also be accessed via
	 * {@link #getFirstRow} and {@link #getMaxRows}
	 */
	Limit getLimit();

	/**
	 * The first row from the results to return
	 *
	 * @see #getLimit
	 */
	default Integer getFirstRow() {
		return getLimit().getFirstRow();
	}

	/**
	 * The maximum number of rows to return from the results
	 *
	 * @see #getLimit
	 */
	default Integer getMaxRows() {
		return getLimit().getMaxRows();
	}

	/**
	 * Determine the effective paging limit to apply to the
	 * query.  If the application did not explicitly specify paging
	 * limits, {@link Limit#NONE} is returned
	 *
	 * @see #getLimit
	 */
	default Limit getEffectiveLimit() {
		final Limit explicit = getLimit();
		return explicit != null ? explicit : Limit.NONE;
	}

	/**
	 * Did the application explicitly request paging limits?
	 *
	 * @see #getLimit
	 */
	default boolean hasLimit() {
		final Limit limit = getLimit();
		return limit != null
			&& (limit.getFirstRow() != null || limit.getMaxRows() != null);
	}

	default ListResultsConsumer.UniqueSemantic getUniqueSemantic(){
		return null;
	}

	/**
	 * Provide singleton access for frequently needed options:
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
