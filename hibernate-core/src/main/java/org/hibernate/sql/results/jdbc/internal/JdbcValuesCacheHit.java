/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.jdbc.internal;

import java.util.List;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.sql.results.caching.internal.QueryCachePutManagerDisabledImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * A JdbcValuesSource implementation for cases where we had a cache hit.
 *
 * @author Steve Ebersole
 */
public class JdbcValuesCacheHit extends AbstractJdbcValues {
	private static final Object[][] NO_DATA = new Object[0][];

	private Object[][] cachedData;
	private final int numberOfRows;
	private JdbcValuesMapping resolvedMapping;
	private int position = -1;

	public JdbcValuesCacheHit(Object[][] cachedData, JdbcValuesMapping resolvedMapping) {
		// if we have a cache hit we should not be writing back to the cache.
		// its silly because the state would always be the same.
		//
		// well actually, there are times when we want to write values back to the cache even though we had a hit...
		// the case is related to the domain-data cache
		super( QueryCachePutManagerDisabledImpl.INSTANCE );
		this.cachedData = cachedData;
		this.numberOfRows = cachedData.length;
		this.resolvedMapping = resolvedMapping;
	}

	public JdbcValuesCacheHit(List<Object[]> cachedResults, JdbcValuesMapping resolvedMapping) {
		this( extractData( cachedResults ), resolvedMapping );
	}

	private static Object[][] extractData(List<Object[]> cachedResults) {
		if ( CollectionHelper.isEmpty( cachedResults ) ) {
			return NO_DATA;
		}

		final Object[][] data = new Object[cachedResults.size()][];
		for ( int i = 0; i < cachedResults.size(); i++ ) {
			final Object[] row = cachedResults.get( i );
			data[ i ] = row;
		}

		return data;
	}

	@Override
	protected boolean processNext(RowProcessingState rowProcessingState) {
		ResultsLogger.LOGGER.tracef( "JdbcValuesCacheHit#processNext : position = %i; numberOfRows = %i", position, numberOfRows );

		// NOTE : explicitly skipping limit handling because the cached state ought
		// 		already be the limited size since the cache key includes limits

		if ( position >= numberOfRows - 1 ) {
			return false;
		}

		position++;
		return true;
	}

	@Override
	public JdbcValuesMapping getValuesMapping() {
		return resolvedMapping;
	}

	@Override
	public Object[] getCurrentRowValuesArray() {
		if ( position >= numberOfRows ) {
			return null;
		}
		return cachedData[position];
	}

	@Override
	protected void release() {
		cachedData = null;
	}
}
