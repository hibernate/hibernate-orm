/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Intended to be thrown from {@link org.hibernate.classic.Lifecycle}
 * and {@link Interceptor} callbacks.
 *
 * @implNote This is a legacy exception type from back in the day before
 * Hibernate moved to an unchecked exception strategy.
 *
 * @author Gavin King
 */
public class CallbackException extends HibernateException {
	/**
	 * Creates a CallbackException using the given underlying cause.
	 *
	 * @param cause The underlying cause
	 */
	public CallbackException(Exception cause) {
		this( "An exception occurred in a callback", cause );
	}

	/**
	 * Creates a CallbackException using the given message.
	 *
	 * @param message The message explaining the reason for the exception
	 */
	public CallbackException(String message) {
		super( message );
	}

	/**
	 * Creates a CallbackException using the given message and underlying cause.
	 *
	 * @param message The message explaining the reason for the exception
	 * @param cause The underlying cause
	 */
	public CallbackException(String message, Exception cause) {
		super( message, cause );
	}

}
