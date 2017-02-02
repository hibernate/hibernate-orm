/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache;

import org.hibernate.HibernateException;

/**
 * Something went wrong in the cache
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
