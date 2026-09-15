/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import org.hibernate.HibernateException;

/// A concurrency configuration error which must not be hidden by the normal
/// ALLOW bootstrap-metadata fallback. Resolvers use this for contradictory
/// declarations, invalid configuration, and required probe failures.
///
/// @since 8.0
/// @author Steve Ebersole
public class TransactionConcurrencyResolutionException extends HibernateException {
	public TransactionConcurrencyResolutionException(String message) {
		super( message );
	}

	public TransactionConcurrencyResolutionException(String message, Throwable cause) {
		super( message, cause );
	}
}
