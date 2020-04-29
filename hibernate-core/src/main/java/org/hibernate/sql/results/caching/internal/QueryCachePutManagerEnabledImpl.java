/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.caching.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.caching.QueryCachePutManager;

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

		// todo (6.0) : verify whether we really need to copy these..
		//		`RowProcessingStateStandardImpl` (see `#finishRowProcessing`) already creates new array
		//		instances for each row
//		dataToCache.add( values );
		dataToCache.add( Arrays.copyOf( values, values.length ) );
	}

	@Override
	public void finishUp(SharedSessionContractImplementor session) {
		// todo (
		queryCache.put(
				queryKey,
				dataToCache,
				session
		);
	}
}
