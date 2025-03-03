/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Thrown when the user passes a persistent instance to a {@link Session}
 * method that expects a transient instance.
 *
 * @author Gavin King
 */
public class PersistentObjectException extends HibernateException {
	/**
	 * Constructs a {@code PersistentObjectException} using the given message.
	 *
	 * @param message A message explaining the exception condition
	 */
	public PersistentObjectException(String message) {
		super( message );
	}
}
