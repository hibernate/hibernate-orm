/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception;

import java.sql.SQLException;

import org.hibernate.JDBCException;

/**
 * A {@link JDBCException} indicating a problem acquiring a lock
 * on the database.
 *
 * @author Steve Ebersole
 */
public class LockAcquisitionException extends JDBCException {
	public LockAcquisitionException(String string, SQLException root) {
		super( string, root );
	}

	public LockAcquisitionException(String string, SQLException root, String sql) {
		super( string, root, sql );
	}
}
