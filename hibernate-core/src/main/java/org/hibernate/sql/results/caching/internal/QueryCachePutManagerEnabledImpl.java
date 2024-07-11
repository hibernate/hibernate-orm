/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.caching.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.caching.QueryCachePutManager;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * QueryCachePutManager implementation for cases where we will be putting
 * Query results into the cache.
 *
 * @author Steve Ebersole
 */
public class QueryCachePutManagerEnabledImpl implements QueryCachePutManager {
	private final QueryResultsCache queryCache;
	private final StatisticsImplementor statistics;
	private final QueryKey queryKey;
	private final String queryIdentifier;
	private final List<Object> dataToCache = new ArrayList<>();

	public QueryCachePutManagerEnabledImpl(
			QueryResultsCache queryCache,
			StatisticsImplementor statistics,
			QueryKey queryKey,
			String queryIdentifier,
			JdbcValuesMetadata metadataForCache) {
		this.queryCache = queryCache;
		this.statistics = statistics;
		this.queryKey = queryKey;
		this.queryIdentifier = queryIdentifier;
		if ( metadataForCache != null ) {
			dataToCache.add( metadataForCache );
		}
	}

	@Override
	public void registerJdbcRow(Object values) {
		dataToCache.add( values );
	}

	@Override
	public void finishUp(SharedSessionContractImplementor session) {
		finishUp( dataToCache.size() - 1, session );
	}

	@Override
	public void finishUp(int resultCount, SharedSessionContractImplementor session) {
		if ( !dataToCache.isEmpty() ) {
			dataToCache.add( resultCount );
		}
		final boolean put = queryCache.put(
				queryKey,
				dataToCache,
				session
		);
		if ( put && statistics.isStatisticsEnabled() ) {
			statistics.queryCachePut( queryIdentifier, queryCache.getRegion().getName() );
		}
	}
}
