/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.custom;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Models an entire "row" of results within a custom query
 *
 * @author Steve Ebersole
 */
public class ResultRowProcessor {
	private final boolean hasScalars;
	private ResultColumnProcessor[] columnProcessors;

	public ResultRowProcessor(boolean hasScalars, ResultColumnProcessor[] columnProcessors) {
		this.hasScalars = hasScalars || ( columnProcessors == null || columnProcessors.length == 0 );
		this.columnProcessors = columnProcessors;
	}

	public ResultColumnProcessor[] getColumnProcessors() {
		return columnProcessors;
	}

	public void prepareForAutoDiscovery(JdbcResultMetadata metadata) throws SQLException {
		if ( columnProcessors == null || columnProcessors.length == 0 ) {
			int columns = metadata.getColumnCount();
			columnProcessors = new ResultColumnProcessor[ columns ];
			for ( int i = 1; i <= columns; i++ ) {
				columnProcessors[ i - 1 ] = new ScalarResultColumnProcessor( i );
			}
		}
	}

	/**
	 * Build a logical result row.
	 * <p/>
	 * At this point, Loader has already processed all non-scalar result data.  We
	 * just need to account for scalar result data here...
	 *
	 * @param data Entity data defined as "root returns" and already handled by the
	 * normal Loader mechanism.
	 * @param resultSet The JDBC result set (positioned at the row currently being processed).
	 * @param hasTransformer Does this query have an associated {@link org.hibernate.transform.ResultTransformer}
	 * @param session The session from which the query request originated.
	 * @return The logical result row
	 * @throws java.sql.SQLException
	 * @throws org.hibernate.HibernateException
	 */
	public Object buildResultRow(Object[] data, ResultSet resultSet, boolean hasTransformer, SessionImplementor session)
			throws SQLException, HibernateException {
		final Object[] resultRow = buildResultRow( data, resultSet, session );
		if ( hasTransformer ) {
			return resultRow;
		}
		else {
			return resultRow.length == 1
					? resultRow[0]
					: resultRow;
		}
	}

	public Object[] buildResultRow(Object[] data, ResultSet resultSet, SessionImplementor session)
			throws SQLException, HibernateException {
		Object[] resultRow;
		if ( !hasScalars ) {
			resultRow = data;
		}
		else {
			// build an array with indices equal to the total number
			// of actual returns in the result Hibernate will return
			// for this query (scalars + non-scalars)
			resultRow = new Object[ columnProcessors.length ];
			for ( int i = 0; i < columnProcessors.length; i++ ) {
				resultRow[i] = columnProcessors[i].extract( data, resultSet, session );
			}
		}

		return resultRow;
	}
}
