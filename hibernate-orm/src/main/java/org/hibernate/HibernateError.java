/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Marks a group of exceptions that generally indicate an internal Hibernate error or bug.
 *
 * @author Steve Ebersole
 */
public class HibernateError extends HibernateException {
	/**
	 * Constructs HibernateError with the condition message.
	 *
	 * @param message Message explaining the exception/error condition
	 */
	public HibernateError(String message) {
		super( message );
	}

	/**
	 * Constructs HibernateError with the condition message and cause.
	 *
	 * @param message Message explaining the exception/error condition
	 * @param cause The underlying cause.
	 */
	public HibernateError(String message, Throwable cause) {
		super( message, cause );
	}
}
