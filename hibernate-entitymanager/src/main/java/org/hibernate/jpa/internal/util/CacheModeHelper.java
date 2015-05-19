/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.util;

import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;

import org.hibernate.CacheMode;

/**
 * Helper to deal with {@link CacheMode} <-> {@link CacheRetrieveMode}/{@link CacheStoreMode}
 * conversions.
 *
 * @author Steve Ebersole
 */
public final class CacheModeHelper {
	private CacheModeHelper() {
	}

	public static final CacheMode DEFAULT_LEGACY_MODE = CacheMode.NORMAL;
	public static final CacheStoreMode DEFAULT_STORE_MODE = CacheStoreMode.USE;
	public static final CacheRetrieveMode DEFAULT_RETRIEVE_MODE = CacheRetrieveMode.USE;

	/**
	 * Given a JPA {@link CacheStoreMode} and {@link CacheRetrieveMode}, determine the corresponding
	 * legacy Hibernate {@link CacheMode}.
	 *
	 * @param storeMode The JPA shared-cache store mode.
	 * @param retrieveMode The JPA shared-cache retrieve mode.
	 *
	 * @return Corresponding {@link CacheMode}.
	 */
	public static CacheMode interpretCacheMode(CacheStoreMode storeMode, CacheRetrieveMode retrieveMode) {
		if ( storeMode == null ) {
			storeMode = DEFAULT_STORE_MODE;
		}
		if ( retrieveMode == null ) {
			retrieveMode = DEFAULT_RETRIEVE_MODE;
		}

		final boolean get = ( CacheRetrieveMode.USE == retrieveMode );

		switch ( storeMode ) {
			case USE: {
				return get ? CacheMode.NORMAL : CacheMode.PUT;
			}
			case REFRESH: {
				// really (get == true) here is a bit of an invalid combo...
				return CacheMode.REFRESH;
			}
			case BYPASS: {
				return get ? CacheMode.GET : CacheMode.IGNORE;
			}
			default: {
				throw new IllegalStateException( "huh? :)" );
			}
		}
	}

	public static CacheStoreMode interpretCacheStoreMode(CacheMode cacheMode) {
		if ( cacheMode == null ) {
			cacheMode = DEFAULT_LEGACY_MODE;
		}

		if ( CacheMode.REFRESH == cacheMode ) {
			return CacheStoreMode.REFRESH;
		}
		if ( CacheMode.NORMAL == cacheMode || CacheMode.PUT == cacheMode ) {
			return CacheStoreMode.USE;
		}
		return CacheStoreMode.BYPASS;
	}

	public static CacheRetrieveMode interpretCacheRetrieveMode(CacheMode cacheMode) {
		if ( cacheMode == null ) {
			cacheMode = DEFAULT_LEGACY_MODE;
		}

		return ( CacheMode.NORMAL == cacheMode || CacheMode.GET == cacheMode )
				? CacheRetrieveMode.USE
				: CacheRetrieveMode.BYPASS;
	}
}
