/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Used when a user provided type does not match the expected one.
 *
 * @author Emmanuel Bernard
 */
public class TypeMismatchException extends HibernateException {
	/**
	 * Constructs a {@code TypeMismatchException} using the supplied message.
	 *
	 * @param message The message explaining the exception condition
	 */
	public TypeMismatchException(String message) {
		super( message );
	}
}
