/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.context;

import org.hibernate.HibernateException;

/**
 * Indicates that tenant identifiers did not match in cases where
 * {@link org.hibernate.context.spi.CurrentTenantIdentifierResolver#validateExistingCurrentSessions()}
 * returns {@code true} and there is a mismatch found.
 *
 * @author Steve Ebersole
 */
public class TenantIdentifierMismatchException extends HibernateException{
	/**
	 * Constructs a TenantIdentifierMismatchException.
	 *
	 * @param message Message explaining the exception condition
	 */
	public TenantIdentifierMismatchException(String message) {
		super( message );
	}

	/**
	 * Constructs a TenantIdentifierMismatchException.
	 *
	 * @param message Message explaining the exception condition
	 * @param cause The underlying cause
	 */
	public TenantIdentifierMismatchException(String message, Throwable cause) {
		super( message, cause );
	}
}
