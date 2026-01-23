/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

import jakarta.persistence.Timeout;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;

import static org.hibernate.query.QueryLogging.QUERY_LOGGER;

/**
 * Extension to QueryOptions providing ability to mutate the values.
 * Generally used by the query instances to collect the options set
 * by the various API methods.
 *
 * @author Steve Ebersole
 */
public interface MutableQueryOptions extends QueryOptions {
	/**
	 * Corollary to {@link #getFlushMode()}
	 */
	void setFlushMode(FlushMode flushMode);

	/**
	 * Corollary to {@link #getCacheRetrieveMode}
	 */
	void setCacheRetrieveMode(CacheRetrieveMode retrieveMode);

	/**
	 * Corollary to {@link #getCacheStoreMode()}
	 */
	void setCacheStoreMode(CacheStoreMode storeMode);

	/**
	 * Corollary to {@link #getCacheMode()}
	 */
	default void setCacheMode(CacheMode cacheMode) {
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
	void setResultCacheRegionName(String cacheRegion);

	/**
	 * Corollary to {@link #getQueryPlanCachingEnabled()}
	 */
	void setQueryPlanCachingEnabled(Boolean queryPlanCachingEnabled);

	/**
	 * Corollary to {@link #getTimeout()}
	 */
	void setTimeout(Timeout timeout);

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
	void setComment(String comment);

	/**
	 * Corollary to {@link #getDatabaseHints()}
	 */
	void addDatabaseHint(String hint);

	void setTupleTransformer(TupleTransformer<?> transformer);

	void setResultListTransformer(ResultListTransformer<?> transformer);

	void applyGraph(RootGraphImplementor<?> rootGraph, GraphSemantic graphSemantic);

	void enableFetchProfile(String profileName);

	void disableFetchProfile(String profileName);

	MutableQueryOptions makeCopy();
}
