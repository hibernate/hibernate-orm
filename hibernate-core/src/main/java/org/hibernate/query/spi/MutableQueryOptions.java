/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import org.hibernate.CacheMode;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;

import static org.hibernate.query.internal.QueryLogging.QUERY_LOGGER;

/**
 * Extension to QueryOptions providing ability to mutate the values.
 * Generally used by the query instances to collect the options set
 * by the various API methods.
 *
 * @author Steve Ebersole
 */
public interface MutableQueryOptions extends QueryOptions {
	/**
	 * Corollary to {@link #getQueryFlushMode()}
	 */
	void setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode);

	/**
	 * Corollary to {@link #getCacheRetrieveMode}
	 */
	void setCacheRetrieveMode(@Nullable CacheRetrieveMode retrieveMode);

	/**
	 * Corollary to {@link #getCacheStoreMode()}
	 */
	void setCacheStoreMode(@Nullable CacheStoreMode storeMode);

	/**
	 * Corollary to {@link #getCacheMode()}
	 */
	default void setCacheMode(@Nullable CacheMode cacheMode) {
		if ( cacheMode == null ) {
			QUERY_LOGGER.debug( "Null CacheMode passed to #setCacheMode; falling back to 'NORMAL'" );
			cacheMode = CacheMode.NORMAL;
		}

		setCacheRetrieveMode( cacheMode.getJpaRetrieveMode() );
		setCacheStoreMode( cacheMode.getJpaStoreMode() );
	}

	/**
	 * Corollary to {@link #isResultCachingEnabled()}
	 */
	void setResultCachingEnabled(boolean cacheable);

	/**
	 * Corollary to {@link #getResultCacheRegionName()}
	 */
	void setResultCacheRegionName(@Nullable String cacheRegion);

	/**
	 * Corollary to {@link #getQueryPlanCachingEnabled()}
	 */
	void setQueryPlanCachingEnabled(@Nullable Boolean queryPlanCachingEnabled);

	/**
	 * Corollary to {@link #isLimitInMemoryEnabled()}
	 */
	void setLimitInMemory(boolean limitInMemory);

	/**
	 * Corollary to {@link #getTimeout()}
	 */
	void setTimeout(@Nullable Timeout timeout);

	/**
	 * Corollary to {@link #getTimeout()}
	 */
	void setTimeout(int timeout);

	/**
	 * Corollary to {@link #getFetchSize()}
	 */
	void setFetchSize(int fetchSize);

	/**
	 * Corollary to {@link #isReadOnly()}
	 */
	void setReadOnly(boolean readOnly);

	/**
	 * Corollary to {@link #getComment()}
	 */
	void setComment(@Nullable String comment);

	/**
	 * Corollary to {@link #getDatabaseHints()}
	 */
	void addDatabaseHint(@Nonnull String hint);

	void setTupleTransformer(@Nonnull TupleTransformer<?> transformer);

	void setResultListTransformer(@Nonnull ResultListTransformer<?> transformer);

	void applyGraph(@Nonnull RootGraphImplementor<?> rootGraph, @Nonnull GraphSemantic graphSemantic);

	void enableFetchProfile(@Nonnull String profileName);

	void disableFetchProfile(@Nonnull String profileName);

	@Nonnull
	MutableQueryOptions makeCopy();
}
