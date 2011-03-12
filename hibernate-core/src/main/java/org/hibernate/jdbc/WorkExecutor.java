/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
	 * @return null>.
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
