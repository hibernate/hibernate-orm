/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;

/**
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

	void setTupleTransformer(TupleTransformer transformer);

	void setResultListTransformer(ResultListTransformer transformer);
}
