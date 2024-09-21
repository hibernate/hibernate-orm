/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Thrown when the user passes a transient instance to a {@link Session}
 * method that expects a persistent instance.
 *
 * @author Gavin King
 */
public class TransientObjectException extends HibernateException {
	/**
	 * Constructs a {@code TransientObjectException} using the supplied message.
	 *
	 * @param message The message explaining the exception condition
	 */
	public TransientObjectException(String message) {
		super( message );
	}

}
