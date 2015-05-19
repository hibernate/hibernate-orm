/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Thrown when the user calls a method of a {@link Session} that is in an inappropriate state for the given call (for
 * example, the the session is closed or disconnected).
 *
 * @author Gavin King
 */
public class SessionException extends HibernateException {
	/**
	 * Constructs a new SessionException with the given message.
	 *
	 * @param message The message indicating the specific problem.
	 */
	public SessionException(String message) {
		super( message );
	}

	/**
	 * Constructs a new SessionException with the given message.
	 *
	 * @param message The message indicating the specific problem.
	 * @param cause An exception which caused this exception to be created.
	 */
	public SessionException(String message, Throwable cause) {
		super( message, cause );
	}
}
