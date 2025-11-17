/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Indicates that a transaction could not be begun, committed
 * or rolled back.
 *
 * @author Anton van Straaten
 */
public class TransactionException extends HibernateException {
	/**
	 * Constructs a {@code TransactionException} using the specified information.
	 *
	 * @param message The message explaining the exception condition
	 * @param cause The underlying cause
	 */
	public TransactionException(String message, Throwable cause) {
		super( message, cause );
	}

	/**
	 * Constructs a {@code TransactionException} using the specified information.
	 *
	 * @param message The message explaining the exception condition
	 */
	public TransactionException(String message) {
		super( message );
	}

}
