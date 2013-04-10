/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations;

import org.hibernate.CacheMode;

/**
 * Enumeration for the different interaction modes between the session and
 * the Level 2 Cache.
 *
 * @author Emmanuel Bernard
 * @author Carlos Gonzalez-Cadenas
 */
public enum CacheModeType {
	/**
	 * Corresponds to {@link CacheMode#GET}.
	 *
	 * @see CacheMode#GET
	 */
	GET( CacheMode.GET ),

	/**
	 * Corresponds to {@link CacheMode#IGNORE}.
	 *
	 * @see CacheMode#IGNORE
	 */
	IGNORE( CacheMode.IGNORE ),

	/**
	 * Corresponds to {@link CacheMode#NORMAL}.
	 *
	 * @see CacheMode#NORMAL
	 */
	NORMAL( CacheMode.NORMAL ),

	/**
	 * Corresponds to {@link CacheMode#PUT}.
	 *
	 * @see CacheMode#PUT
	 */
	PUT( CacheMode.PUT ),

	/**
	 * Corresponds to {@link CacheMode#REFRESH}.
	 *
	 * @see CacheMode#REFRESH
	 */
	REFRESH( CacheMode.REFRESH );

	private final CacheMode cacheMode;

	private CacheModeType(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
	}

	public CacheMode getCacheMode() {
		return cacheMode;
	}

	/**
	 * Conversion from {@link CacheMode} to {@link CacheModeType}.
	 *
	 * @param cacheMode The cache mode to convert
	 *
	 * @return The corresponding enum value.  Will be {@code null} if the given {@code accessType} is {@code null}.
	 */
	public static CacheModeType fromCacheMode(CacheMode cacheMode) {
		if ( null == cacheMode ) {
			return null;
		}

		switch ( cacheMode ) {
			case NORMAL: {
				return NORMAL;
			}
			case GET: {
				return GET;
			}
			case PUT: {
				return PUT;
			}
			case REFRESH: {
				return REFRESH;
			}
			case IGNORE: {
				return IGNORE;
			}
			default: {
				throw new IllegalArgumentException( "Unrecognized CacheMode : " + cacheMode );
			}
		}
	}
}
