/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Marks a group of exceptions that generally indicate an internal Hibernate error or bug.
 *
 * @author Steve Ebersole
 */
public class HibernateError extends HibernateException {
	/**
	 * Constructs {@code HibernateError} with the condition message.
	 *
	 * @param message Message explaining the exception/error condition
	 */
	public HibernateError(String message) {
		super( message );
	}

	/**
	 * Constructs {@code HibernateError} with the condition message and cause.
	 *
	 * @param message Message explaining the exception/error condition
	 * @param cause The underlying cause.
	 */
	public HibernateError(String message, Throwable cause) {
		super( message, cause );
	}
}
