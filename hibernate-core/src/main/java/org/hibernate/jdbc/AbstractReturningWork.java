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
 * An abstract implementation of {@link ReturningWork} that accepts a {@link WorkExecutor}
 * visitor for executing a discrete piece of work and returning a result.
 *
 * This class is intended to be used for work that returns a value when executed.
 *
 * @author Gail Badner
 */
public abstract class AbstractReturningWork<T> implements ReturningWork<T>, WorkExecutorVisitable<T> {
	/**
	 * Accepts a {@link WorkExecutor} visitor for executing the discrete work
	 * encapsulated by this work instance using the supplied connection.
	 *
	 * @param executor The visitor that executes the work
	 * @param connection The connection on which to perform the work.
	 *
	 * @return the valued returned by {@link #execute(java.sql.Connection)}.
	 *
	 * @throws SQLException Thrown during execution of the underlying JDBC interaction.
	 * @throws org.hibernate.HibernateException Generally indicates a wrapped SQLException.
	 */
	public T accept(WorkExecutor<T> executor, Connection connection) throws SQLException {
		return executor.executeReturningWork( this, connection );
	}
}
