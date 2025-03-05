/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception;
import java.sql.SQLException;

import org.hibernate.JDBCException;

/**
 * A {@link JDBCException} indicating a problem communicating with the
 * database (can also include incorrect JDBC setup).
 *
 * @author Steve Ebersole
 */
public class JDBCConnectionException extends JDBCException {
	public JDBCConnectionException(String string, SQLException root) {
		super( string, root );
	}

	public JDBCConnectionException(String string, SQLException root, String sql) {
		super( string, root, sql );
	}
}
