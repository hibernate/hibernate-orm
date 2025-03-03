/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception;
import java.sql.SQLException;

import org.hibernate.JDBCException;

/**
 * Generic, non-specific flavor of {@link JDBCException}.
 *
 * @author Steve Ebersole
 */
public class GenericJDBCException extends JDBCException {
	public GenericJDBCException(String string, SQLException root) {
		super( string, root );
	}

	public GenericJDBCException(String string, SQLException root, String sql) {
		super( string, root, sql );
	}
}
