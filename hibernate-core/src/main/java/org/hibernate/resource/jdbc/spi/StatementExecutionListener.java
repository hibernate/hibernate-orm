/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc.spi;

import java.io.Serializable;

/**
 * Defines the contract for listening to SQL query execution and execution times.
 * <p>
 * There currently is a limitation that the listener is called after {@link java.sql.PreparedStatement#executeQuery()}
 * total query execution can be longer when result set processing incurs additional network round trips for example
 * loading additional data using {@link java.sql.ResultSet#next()} or {@link java.sql.ResultSet#getBlob(String)}.
 * 
 * @author Philippe Marschall
 */
public interface StatementExecutionListener extends Serializable {

	/**
	 * Inspect the given SQL, possibly returning a different SQL to be used instead.
	 * <p>
	 * Note that returning {@code null} is interpreted as returning the same SQL as was passed. The execution time has
	 * to be calculated by the implementor my subtracting the start time from the current time.
	 *
	 * @param sql The SQL query that was executed
	 * @param startTimeNanos The query start time in nanoseconds
	 */
	void statementExecuted(String sql, long startTimeNanos);

}
