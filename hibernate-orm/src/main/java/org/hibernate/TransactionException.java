/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Indicates that a transaction could not be begun, committed
 * or rolled back.
 *
 * @author Anton van Straaten
 */
public class TransactionException extends HibernateException {
	/**
	 * Constructs a TransactionException using the specified information.
	 *
	 * @param message The message explaining the exception condition
	 * @param cause The underlying cause
	 */
	public TransactionException(String message, Throwable cause) {
		super( message, cause );
	}

	/**
	 * Constructs a TransactionException using the specified information.
	 *
	 * @param message The message explaining the exception condition
	 */
	public TransactionException(String message) {
		super( message );
	}

}
