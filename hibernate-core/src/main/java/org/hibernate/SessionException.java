/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Thrown when the user calls a method of a {@link Session} that is
 * in an inappropriate state for the given call (for example, the
 * session is closed or disconnected).
 *
 * @author Gavin King
 */
public class SessionException extends HibernateException {
	/**
	 * Constructs a new {@code SessionException} with the given message.
	 *
	 * @param message The message indicating the specific problem.
	 */
	public SessionException(String message) {
		super( message );
	}

	/**
	 * Constructs a new {@code SessionException} with the given message.
	 *
	 * @param message The message indicating the specific problem.
	 * @param cause An exception which caused this exception to be created.
	 */
	public SessionException(String message, Throwable cause) {
		super( message, cause );
	}
}
