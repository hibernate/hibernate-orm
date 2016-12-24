/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal;

import java.sql.SQLException;

import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.results.process.internal.values.JdbcValuesSource;
import org.hibernate.sql.exec.results.process.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.exec.results.process.spi.RowProcessingState;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class RowProcessingStateStandardImpl implements RowProcessingState {
	private static final Logger log = Logger.getLogger( RowProcessingStateStandardImpl.class );

	private final JdbcValuesSourceProcessingStateStandardImpl resultSetProcessingState;
	private final QueryOptions queryOptions;

	private final JdbcValuesSource jdbcValuesSource;
	private Object[] currentRowJdbcValues;

	public RowProcessingStateStandardImpl(
			JdbcValuesSourceProcessingStateStandardImpl resultSetProcessingState,
			QueryOptions queryOptions,
			JdbcValuesSource jdbcValuesSource) {
		this.resultSetProcessingState = resultSetProcessingState;
		this.queryOptions = queryOptions;
		this.jdbcValuesSource = jdbcValuesSource;
	}

	@Override
	public JdbcValuesSourceProcessingState getJdbcValuesSourceProcessingState() {
		return resultSetProcessingState;
	}

//	@Override
	public boolean next() throws SQLException {
		if ( jdbcValuesSource.next( this ) ) {
			currentRowJdbcValues = jdbcValuesSource.getCurrentRowJdbcValues();
			return true;
		}
		else {
			currentRowJdbcValues = null;
			return false;
		}
	}

	@Override
	public Object[] getJdbcValues() {
		return currentRowJdbcValues;
	}

	@Override
	public void registerNonExists(EntityFetch fetch) {
	}

	@Override
	public void finishRowProcessing() {
		currentRowJdbcValues = null;
	}
}
