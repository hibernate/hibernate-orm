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
 * A discrete piece of work following the lines of {@link Work} but returning a result.
 *
 * @author Steve Ebersole
 */
public interface ReturningWork<T> {
	/**
	 * Execute the discrete work encapsulated by this work instance using the supplied connection.
	 *
	 * @param connection The connection on which to perform the work.
	 *
	 * @return The work result
	 * 
	 * @throws SQLException Thrown during execution of the underlying JDBC interaction.
	 * @throws org.hibernate.HibernateException Generally indicates a wrapped SQLException.
	 */
	public T execute(Connection connection) throws SQLException;
}
