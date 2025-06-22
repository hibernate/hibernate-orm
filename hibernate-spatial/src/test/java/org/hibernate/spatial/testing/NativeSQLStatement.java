/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * <code>NativeSQLStatement</code>s can instantiate a
 * database-specific  <code>PreparedStatement</code> for
 * some database query or operation.
 *
 * @author Karel Maesen, Geovise BVBA
 */
@Deprecated
public interface NativeSQLStatement {

	/**
	 * create a PreparedStatement from the specified connection
	 *
	 * @param connection Connection to the database.
	 *
	 * @return
	 *
	 * @throws SQLException
	 */
	PreparedStatement prepare(Connection connection) throws SQLException;

	String toString();
}
