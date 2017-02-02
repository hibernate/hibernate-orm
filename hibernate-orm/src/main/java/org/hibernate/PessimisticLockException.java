/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.sql.SQLException;

/**
 * Thrown when a pessimistic locking conflict occurs.
 *
 * @author Scott Marlow
 */
public class PessimisticLockException extends JDBCException {
	/**
	 * Constructs a PessimisticLockException using the specified information.
	 *
	 * @param message A message explaining the exception condition
	 * @param sqlException The underlying SQL exception
	 * @param sql The sql that led to the exception (may be null, though usually should not be)
	 */
	public PessimisticLockException(String message, SQLException sqlException, String sql) {
		super( message, sqlException, sql );
	}
}
