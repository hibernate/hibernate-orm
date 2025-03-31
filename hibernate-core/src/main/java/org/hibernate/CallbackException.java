/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Intended to be thrown from {@link Interceptor} callbacks.
 *
 * @implNote This is a legacy exception type from back in the day before
 *           Hibernate moved to an unchecked exception strategy.
 * @deprecated Methods of {@link Interceptor} are no longer required to
 *             throw this exception type.
 *
 * @author Gavin King
 */
@Deprecated(since = "7")
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
