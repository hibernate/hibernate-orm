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
 * This interface provides a way to execute unrelated "work" objects using
 * polymorphism.
 *
 * Instances of this interface can accept a {@link WorkExecutor} visitor
 * for executing a discrete piece of work, and return an implementation-defined
 * result.
 *
 * @author Gail Badner
 */
public interface WorkExecutorVisitable<T> {
	/**
	 * Accepts a {@link WorkExecutor} visitor for executing a discrete
	 * piece of work, and returns an implementation-defined result..
	 *
	 * @param executor The visitor that executes the work.
	 * @param connection The connection on which to perform the work.
	 *
	 * @return an implementation-defined result
	 *
	 * @throws SQLException Thrown during execution of the underlying JDBC interaction.
	 * @throws org.hibernate.HibernateException Generally indicates a wrapped SQLException.
	 */
	T accept(WorkExecutor<T> executor, Connection connection) throws SQLException;
}
