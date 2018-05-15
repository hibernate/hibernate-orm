/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.values;

import java.sql.SQLException;

import org.hibernate.sql.results.spi.ResultSetMapping;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * Provides unified access to query results (JDBC values - see
 * {@link RowProcessingState#getJdbcValue} whether they come from
 * query cache or ResultSet.  Implementations also manage any cache puts
 * if required.
 *
 * @author Steve Ebersole
 */
public interface JdbcValues {
	ResultSetMapping getResultSetMapping();

	// todo : ? - add ResultSet.previous() and ResultSet.absolute(int) style methods (to support ScrollableResults)?

	/**
	 * Think JDBC's {@code ResultSet#next}.  Advances the "cursor position"
	 * and return a boolean indicating whether advancing positioned the
	 * cursor beyond the set of available results.
	 *
	 * @return {@code true} indicates the call did not position the cursor beyond
	 * the available results ({@link #getCurrentRowValuesArray} will not return
	 * null); false indicates we are now beyond the end of the available results
	 * ({@link #getCurrentRowValuesArray} will return null)
	 */
	boolean next(RowProcessingState rowProcessingState) throws SQLException;

	/**
	 * Get the JDBC values for the row currently positioned at within
	 * this source.
	 *
	 * @return The current row's JDBC values, or {@code null} if the position
	 * is beyond the end of the available results.
	 */
	Object[] getCurrentRowValuesArray();

	// todo : ? - is this needed?
	//		^^ it's supposed to give impls a chance to write to the query cache
	//		or release ResultSet it.  But that could technically be handled by the
	// 		case of `#next` returning false the first time.
	void finishUp();
}
