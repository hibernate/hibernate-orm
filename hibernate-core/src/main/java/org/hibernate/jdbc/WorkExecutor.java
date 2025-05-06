/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A visitor used for executing a discrete piece of work encapsulated in a
 * {@link Work} or {@link ReturningWork} instance.
 *
 * @author Gail Badner
 */
public class WorkExecutor<T> {

	/**
	 * Execute the discrete work encapsulated by a {@link Work} instance
	 * using the supplied connection.
	 * <p>
	 * Because {@link Work} does not return a value when executed via
	 * {@link Work#execute(Connection)}, this method always returns null.
	 *
	 * @param work The {@link ReturningWork} instance encapsulating the
	 *             discrete work
	 * @param connection The connection on which to perform the work.
	 *
	 * @return null.
	 *
	 * @throws SQLException Thrown during execution of the underlying JDBC interaction.
	 * @throws org.hibernate.HibernateException Generally indicates a wrapped SQLException.
	 */
	public @Nullable T executeWork(Work work, Connection connection) throws SQLException {
		work.execute( connection );
		return null;
	}

	/**
	 * Execute the discrete work encapsulated by a {@link ReturningWork}
	 * instance using the supplied connection, returning the result of
	 * {@link ReturningWork#execute(Connection)}.
	 *
	 * @param work The {@link ReturningWork} instance encapsulating the
	 *             discrete work
	 * @param connection The connection on which to perform the work.
	 *
	 * @return the valued returned by {@code work.execute(connection)}.
	 *
	 * @throws SQLException Thrown during execution of the underlying JDBC interaction.
	 * @throws org.hibernate.HibernateException Generally indicates a wrapped SQLException.
	 */
	public T executeReturningWork(ReturningWork<T> work, Connection connection) throws SQLException {
		return work.execute( connection );
	}
}
