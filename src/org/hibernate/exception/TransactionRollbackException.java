// $Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.exception;

import org.hibernate.JDBCException;

import java.sql.SQLException;

/**
 * Implementation of JDBCException that indicates that the current statement
 * was automatically rolled back by the database becuase of deadlock or other
 * transaction serialization failures.
 *
 * @author Gail Badner
 */
public class TransactionRollbackException extends JDBCException {
	/**
	 * Constructor for TransactionRollbackException.
	 *
	 * @param message Optional message.
	 * @param root    The underlying exception.
	 */
	public TransactionRollbackException(String message, SQLException root) {
		super( message, root );
	}

	/**
	 * Constructor for TransactionRollbackException.
	 *
	 * @param message Optional message.
	 * @param root    The underlying exception.
	 * @param sql     the SQL statement involved in the exception.
	 */
	public TransactionRollbackException(String message, SQLException root, String sql) {
		super( message, root, sql );
	}
}