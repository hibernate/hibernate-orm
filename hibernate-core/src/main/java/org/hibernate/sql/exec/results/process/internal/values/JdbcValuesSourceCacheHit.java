/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal.values;

import java.util.List;

import org.hibernate.sql.exec.results.process.internal.caching.QueryCachePutManagerDisabledImpl;
import org.hibernate.sql.exec.results.process.spi.RowProcessingState;

/**
 * A JdbcValuesSource implementation for cases where we had a cache
 * hit.
 *
 * @author Steve Ebersole
 */
public class JdbcValuesSourceCacheHit extends AbstractJdbcValuesSource {
	private Object[][] cachedData;
	private final int numberOfRows;
	private int position = -1;

	public JdbcValuesSourceCacheHit(Object[][] cachedData) {
		// if we have a cache hit we should not be writting back to the cache.
		// its silly because the state would always be the same.
		super( QueryCachePutManagerDisabledImpl.INSTANCE );
		this.cachedData = cachedData;
		this.numberOfRows = cachedData.length;
	}

	public JdbcValuesSourceCacheHit(List<Object[]> cachedResults) {
		this( (Object[][]) cachedResults.toArray() );
	}

	@Override
	protected boolean processNext(RowProcessingState rowProcessingState) {
		// NOTE : explicitly skipping limit handling under the truth that
		//		because the cached state ought to be the same size since
		//		the cache key includes limits
		if ( isExhausted() ) {
			return false;
		}
		position++;
		return true;
	}

	private boolean isExhausted() {
		return position >= numberOfRows;
	}

	@Override
	public Object[] getCurrentRowJdbcValues() {
		if ( isExhausted() ) {
			return null;
		}
		return cachedData[position];
	}

	@Override
	protected void release() {
		cachedData = null;
	}
}
