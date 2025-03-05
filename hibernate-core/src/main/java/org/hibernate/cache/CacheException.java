/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache;

import org.hibernate.HibernateException;

/**
 * Something went wrong in the cache.
 */
public class CacheException extends HibernateException {
	/**
	 * Constructs a CacheException.
	 *
	 * @param message Message explaining the exception condition
	 */
	public CacheException(String message) {
		super( message );
	}

	/**
	 * Constructs a CacheException.
	 *
	 * @param message Message explaining the exception condition
	 * @param cause The underlying cause
	 */
	public CacheException(String message, Throwable cause) {
		super( message, cause );
	}

	/**
	 * Constructs a CacheException.
	 *
	 * @param cause The underlying cause
	 */
	public CacheException(Throwable cause) {
		super( cause );
	}

}
