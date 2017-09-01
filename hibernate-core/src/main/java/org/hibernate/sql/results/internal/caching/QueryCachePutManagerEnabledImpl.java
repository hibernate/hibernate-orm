/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.caching;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.cache.spi.QueryKey;

/**
 * QueryCachePutManager implementation for cases where we will be putting
 * Query results into the cache.
 *
 * @author Steve Ebersole
 */
public class QueryCachePutManagerEnabledImpl implements QueryCachePutManager {
	private final QueryResultsCache queryCache;
	private final QueryKey queryKey;

	private List<Object[]> dataToCache;

	public QueryCachePutManagerEnabledImpl(QueryResultsCache queryCache, QueryKey queryKey) {
		this.queryCache = queryCache;
		this.queryKey = queryKey;
	}

	@Override
	public void registerJdbcRow(Object[] values) {
		if ( dataToCache == null ) {
			dataToCache = new ArrayList<>();
		}
		dataToCache.add( values );
	}

	@Override
	public void finishUp() {
		queryCache.put(
				queryKey,
				dataToCache,
				// todo (6.0) : needs access to Session to pass along to cache call
				null
		);
	}
}
