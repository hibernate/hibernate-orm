/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A visitor used for executing a discrete piece of work encapsulated in a
 * {@link Work} or {@link ReturningWork} instance..
 *
 * @author Gail Badner
 */
public class WorkExecutor<T> {

	/**
	 * Execute the discrete work encapsulated by a {@link Work} instance
	 * using the supplied connection.
	 *
	 * Because {@link Work} does not return a value when executed
	 * (via {@link Work#execute(java.sql.Connection)}, this method
	 * always returns null.
	 *
	 * @param work The @link ReturningWork} instance encapsulating the discrete work
	 * @param connection The connection on which to perform the work.
	 *
	 * @return null.
	 *
	 * @throws SQLException Thrown during execution of the underlying JDBC interaction.
	 * @throws org.hibernate.HibernateException Generally indicates a wrapped SQLException.
	 */
	public <T> T executeWork(Work work, Connection connection) throws SQLException {
		work.execute( connection );
		return null;
	}

	/**
	 * Execute the discrete work encapsulated by a {@link ReturningWork} instance
	 * using the supplied connection, returning the result of
	 * {@link ReturningWork#execute(java.sql.Connection)}
	 *
	 * @param work The @link ReturningWork} instance encapsulating the discrete work
	 * @param connection The connection on which to perform the work.
	 *
	 * @return the valued returned by <code>work.execute(connection)</code>.
	 *
	 * @throws SQLException Thrown during execution of the underlying JDBC interaction.
	 * @throws org.hibernate.HibernateException Generally indicates a wrapped SQLException.
	 */
	public <T> T executeReturningWork(ReturningWork<T> work, Connection connection) throws SQLException {
		return work.execute( connection );
	}
}
