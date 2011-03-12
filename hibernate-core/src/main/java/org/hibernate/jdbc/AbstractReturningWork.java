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
