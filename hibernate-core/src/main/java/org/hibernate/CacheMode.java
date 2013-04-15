/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate;

/**
 * Controls how the session interacts with the second-level cache and query cache.
 *
 * @author Gavin King
 * @author Strong Liu
 * @see Session#setCacheMode(CacheMode)
 */
public enum CacheMode {
	/**
	 * The session may read items from the cache, and add items to the cache.
	 */
	NORMAL( true, true ),
	/**
	 * The session will never interact with the cache, except to invalidate
	 * cache items when updates occur.
	 */
	IGNORE( false, false ),
	/**
	 * The session may read items from the cache, but will not add items,
	 * except to invalidate items when updates occur.
	 */
	GET( false, true ),
	/**
	 * The session will never read items from the cache, but will add items
	 * to the cache as it reads them from the database.
	 */
	PUT( true, false ),
	/**
	 * The session will never read items from the cache, but will add items
	 * to the cache as it reads them from the database.  In this mode, the
	 * effect of <tt>hibernate.cache.use_minimal_puts</tt> is bypassed, in
	 * order to <em>force</em> a cache refresh.
	 */
	REFRESH( true, false );


	private final boolean isPutEnabled;
	private final boolean isGetEnabled;

	private CacheMode( boolean isPutEnabled, boolean isGetEnabled) {
		this.isPutEnabled = isPutEnabled;
		this.isGetEnabled = isGetEnabled;
	}

	/**
	 * Does this cache mode indicate that reads are allowed?
	 *
	 * @return {@code true} if cache reads are allowed; {@code false} otherwise.
	 */
	public boolean isGetEnabled() {
		return isGetEnabled;
	}

	/**
	 * Does this cache mode indicate that writes are allowed?
	 *
	 * @return {@code true} if cache writes are allowed; {@code false} otherwise.
	 */
	public boolean isPutEnabled() {
		return isPutEnabled;
	}

	/**
	 * Used to interpret externalized forms of this enum.
	 *
	 * @param setting The externalized form.
	 *
	 * @return The matching enum value.
	 *
	 * @throws MappingException Indicates the external form was not recognized as a valid enum value.
	 */
	public static CacheMode interpretExternalSetting(String setting) {
		if (setting == null) {
			return null;
		}

		try {
			return CacheMode.valueOf( setting.toUpperCase() );
		}
		catch ( IllegalArgumentException e ) {
			throw new MappingException( "Unknown Cache Mode: " + setting );
		}
	}
}
