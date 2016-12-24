/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal.caching;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.QueryKey;

/**
 * QueryCachePutManager implementation for cases where we will be putting
 * Query results into the cache.
 *
 * @author Steve Ebersole
 */
public class QueryCachePutManagerEnabledImpl implements QueryCachePutManager {

	private final QueryCache queryCache;
	private final QueryKey queryKey;

	private List<Object[]> dataToCache;

	public QueryCachePutManagerEnabledImpl(QueryCache queryCache, QueryKey queryKey) {
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
		// todo : see discussion regarding these arguments back on QueryCacheDataAccessEnabled
		queryCache.put(
				queryKey,
				null,
				null,
				false,
				null
		);
	}
}
