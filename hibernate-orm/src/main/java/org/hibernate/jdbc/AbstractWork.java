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
 * An abstract implementation of {@link Work} that accepts a {@link WorkExecutor}
 * visitor for executing a discrete piece of work.
 *
 * This class is intended to be used for work that does not return a value when
 * executed.
 *
 * @author Gail Badner
 */
public abstract class AbstractWork implements Work, WorkExecutorVisitable<Void> {
	/**
	 * Accepts a {@link WorkExecutor} visitor for executing the discrete work
	 * encapsulated by this work instance using the supplied connection.
	 *
	 * Because {@link Work} does not return a value when executed
	 * (via {@link Work#execute(java.sql.Connection)}, this method
	 * always returns null.
	 *
	 * @param connection The connection on which to perform the work.
	 *
	 * @return null
	 *
	 * @throws SQLException Thrown during execution of the underlying JDBC interaction.
	 * @throws org.hibernate.HibernateException Generally indicates a wrapped SQLException.
	 */
	public Void accept(WorkExecutor<Void> executor, Connection connection) throws SQLException {
		return executor.executeWork( this, connection );
	}
}
