/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache;

import org.hibernate.internal.util.StringHelper;

public enum MissingCacheStrategy {

	/**
	 * Fail with an exception on missing caches.
	 */
	FAIL("fail"),

	/**
	 * Create a new cache when a cache is not found (see {@link #CREATE})
	 * and also log a warning about the missing cache.
	 */
	CREATE_WARN("create-warn"),

	/**
	 * Create a new cache when a cache is not found,
	 * without logging any warning about the missing cache.
	 *
	 * Note that caches created this way may be very badly configured (large size in particular)
	 * unless an appropriate `&lt;defaultCache&gt;` entry is added to the Ehcache configuration.
	 */
	CREATE("create");

	private final String externalRepresentation;

	MissingCacheStrategy(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	public String getExternalRepresentation() {
		return externalRepresentation;
	}

	public static MissingCacheStrategy interpretSetting(Object value) {
		if ( value instanceof MissingCacheStrategy ) {
			return (MissingCacheStrategy) value;
		}

		final String externalRepresentation = value == null ? null : value.toString().trim();

		if ( StringHelper.isEmpty( externalRepresentation ) ) {
			// Use the default
			// Default is CREATE_WARN for backward compatibility reasons; we should switch to FAIL at some point.
			return MissingCacheStrategy.CREATE_WARN;
		}

		for ( MissingCacheStrategy strategy : values() ) {
			if ( strategy.externalRepresentation.equals( externalRepresentation ) ) {
				return strategy;
			}
		}

		throw new IllegalArgumentException( "Unrecognized missing cache strategy value : `" + value + '`');
	}
}
