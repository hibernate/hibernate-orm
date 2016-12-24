/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.spi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.Incubating;

/**
 * Encapsulates the creation of a {@link PreparedStatement} in various cases.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface PreparedStatementCreator {
	/**
	 * Create the PreparedStatement.  Note that {@link java.sql.CallableStatement} extends
	 * PreparedStatement, and so can also be returned here.
	 *
	 * @param connection The JDBC connection to use to prepare the statement
	 * @param sql The SQL string to prepare
	 *
	 * @return The created PreparedStatement
	 *
	 * @throws SQLException Indicates that the JDBC driver reported a problem
	 * preparing the statement.
	 */
	PreparedStatement create(Connection connection, String sql) throws SQLException;
}
