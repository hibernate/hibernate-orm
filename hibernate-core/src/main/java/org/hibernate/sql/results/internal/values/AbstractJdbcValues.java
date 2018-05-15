/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.values;

import java.sql.SQLException;

import org.hibernate.sql.results.internal.caching.QueryCachePutManager;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractJdbcValues implements JdbcValues {
	private final QueryCachePutManager queryCachePutManager;

	public AbstractJdbcValues(QueryCachePutManager queryCachePutManager) {
		if ( queryCachePutManager == null ) {
			throw new IllegalArgumentException( "QueryCachePutManager cannot be null" );
		}
		this.queryCachePutManager = queryCachePutManager;
	}

	@Override
	public final boolean next(RowProcessingState rowProcessingState) throws SQLException {
		if ( getCurrentRowValuesArray() != null ) {
			queryCachePutManager.registerJdbcRow( getCurrentRowValuesArray() );
		}
		return processNext( rowProcessingState );
	}

	protected abstract boolean processNext(RowProcessingState rowProcessingState);

	@Override
	public final void finishUp() {
		queryCachePutManager.finishUp();
		release();
	}

	protected abstract void release();
}
