/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal.util;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

import org.hibernate.CacheMode;

/**
 * Helper to deal with conversions between {@link CacheMode} and {@link CacheRetrieveMode}/{@link CacheStoreMode}
 * .
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

		final boolean get = CacheRetrieveMode.USE == retrieveMode;

		switch ( storeMode ) {
			case USE:
				return get ? CacheMode.NORMAL : CacheMode.PUT;
			case BYPASS:
				return get ? CacheMode.GET : CacheMode.IGNORE;
			case REFRESH:
				// really (get == true) here is a bit of an invalid combo...
				return CacheMode.REFRESH;
			default:
				throw new IllegalStateException( "Unrecognized CacheStoreMode: " + storeMode );
		}
	}

	/**
	 * Given a JPA {@link CacheStoreMode} and {@link CacheRetrieveMode}, determine the corresponding
	 * legacy Hibernate {@link CacheMode}.
	 *
	 * @param storeMode The JPA shared-cache store mode.
	 * @param retrieveMode The JPA shared-cache retrieve mode.
	 *
	 * @return Corresponding {@link CacheMode}, or null if both arguments are null.
	 */
	public static CacheMode effectiveCacheMode(CacheStoreMode storeMode, CacheRetrieveMode retrieveMode) {
		return storeMode == null && retrieveMode == null ? null
				: interpretCacheMode( storeMode, retrieveMode );
	}

	public static CacheStoreMode interpretCacheStoreMode(CacheMode cacheMode) {
		if ( cacheMode == null ) {
			cacheMode = DEFAULT_LEGACY_MODE;
		}

		switch (cacheMode) {
			case NORMAL:
			case PUT:
				return CacheStoreMode.USE;
			case REFRESH:
				return CacheStoreMode.REFRESH;
			default:
				return CacheStoreMode.BYPASS;
		}
	}

	public static CacheRetrieveMode interpretCacheRetrieveMode(CacheMode cacheMode) {
		if ( cacheMode == null ) {
			cacheMode = DEFAULT_LEGACY_MODE;
		}

		return CacheMode.NORMAL == cacheMode || CacheMode.GET == cacheMode
				? CacheRetrieveMode.USE
				: CacheRetrieveMode.BYPASS;
	}
}
