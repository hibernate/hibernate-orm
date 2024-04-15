/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Contract for validating JDBC Connections
 *
 * @author Christian Beikov
 */
public interface ConnectionValidator {

	ConnectionValidator ALWAYS_VALID = connection -> true;

	/**
	 * Checks if the connection is still valid
	 *
	 * @return <code>true</code> if the connection is valid, <code>false</code> otherwise
	 * @throws SQLException when an error happens due to the connection usage leading to a connection close
	 */
	boolean isValid(Connection connection) throws SQLException;
}
