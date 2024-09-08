/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Thrown when a version number or timestamp check failed, indicating that
 * the {@link Session} contained stale data (when using long transactions
 * with versioning). Also occurs on attempts to delete or update a row that
 * does not exist.
 *
 * @author Gavin King
 */
public class StaleStateException extends HibernateException {
	/**
	 * Constructs a {@code StaleStateException} using the supplied message.
	 *
	 * @param message The message explaining the exception condition
	 */
	public StaleStateException(String message) {
		super( message );
	}

	/**
	 * Constructs a {@code StaleStateException} using the supplied message
	 * and cause.
	 *
	 * @param message The message explaining the exception condition
	 * @param cause An exception to wrap
	 */
	public StaleStateException(String message, Exception cause) {
		super( message, cause );
	}
}
