/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.util.Locale;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FindOption;

/**
 * Controls how the session interacts with the {@linkplain Cache second-level cache}
 * or {@linkplain org.hibernate.query.SelectionQuery#isCacheable() query cache}.
 * An instance of {@code CacheMode} may be viewed as packaging a JPA-defined
 * {@link CacheStoreMode} with a {@link CacheRetrieveMode}. For example,
 * {@link CacheMode#PUT} represents the combination {@code (BYPASS, USE)}.
 * <p>
 * However, this enumeration recognizes only five such combinations. In Hibernate,
 * {@link CacheStoreMode#REFRESH} always implies {@link CacheRetrieveMode#BYPASS},
 * so there's no {@code CacheMode} representing the combination
 * {@code (REFRESH, USE)}.
 *
 * @author Gavin King
 * @author Strong Liu
 *
 * @see Session#setCacheMode(CacheMode)
 * @see org.hibernate.query.SelectionQuery#setCacheMode(CacheMode)
 * @see CacheStoreMode
 * @see CacheRetrieveMode
 */
public enum CacheMode implements FindOption {

	/**
	 * The session may read items from the cache, and add items to the cache
	 * as it reads them from the database.
	 */
	NORMAL( CacheStoreMode.USE, CacheRetrieveMode.USE ),

	/**
	 * The session will never interact with the cache, except to invalidate
	 * cached items when updates occur.
	 */
	IGNORE( CacheStoreMode.BYPASS, CacheRetrieveMode.BYPASS ),

	/**
	 * The session may read items from the cache, but will not add items,
	 * except to invalidate items when updates occur.
	 */
	GET( CacheStoreMode.BYPASS, CacheRetrieveMode.USE ),

	/**
	 * The session will never read items from the cache, but will add items
	 * to the cache as it reads them from the database. In this mode, the
	 * value of the configuration setting
	 * {@value org.hibernate.cfg.AvailableSettings#USE_MINIMAL_PUTS}
	 * determines whether an item is written to the cache when the cache
	 * already contains an entry with the same key. Minimal puts should be:
	 * <ul>
	 * <li>disabled for a cache where writes and reads carry a similar cost,
	 *     as is usually the case for a local in-memory cache, and
	 * <li>enabled for a cache where writes are much more expensive than
	 *     reads, which is usually the case for a distributed cache.
	 * </ul>
	 * <p>
	 * It's not usually necessary to specify this setting explicitly because,
	 * by default, it's set to a sensible value by the second-level cache
	 * implementation.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyMinimalPutsForCaching(boolean)
	 */
	PUT( CacheStoreMode.USE, CacheRetrieveMode.BYPASS ),

	/**
	 * As with to {@link #PUT}, the session will never read items from the
	 * cache, but will add items to the cache as it reads them from the
	 * database. But in this mode, the effect of the configuration setting
	 * {@value org.hibernate.cfg.AvailableSettings#USE_MINIMAL_PUTS} is
	 * bypassed, in order to <em>force</em> a refresh of a cached item,
	 * even when an entry with the same key already exists in the cache.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyMinimalPutsForCaching(boolean)
	 */
	REFRESH( CacheStoreMode.REFRESH, CacheRetrieveMode.BYPASS );

	private final CacheStoreMode storeMode;
	private final CacheRetrieveMode retrieveMode;

	CacheMode(CacheStoreMode storeMode, CacheRetrieveMode retrieveMode) {
		this.storeMode = storeMode;
		this.retrieveMode = retrieveMode;
	}

	/**
	 * @return the JPA-defined {@link CacheStoreMode} implied by this cache mode
	 */
	public CacheStoreMode getJpaStoreMode() {
		return storeMode;
	}

	/**
	 * @return the JPA-defined {@link CacheRetrieveMode} implied by this cache mode
	 */
	public CacheRetrieveMode getJpaRetrieveMode() {
		return retrieveMode;
	}

	/**
	 * Does this cache mode indicate that reads are allowed?
	 *
	 * @return {@code true} if cache reads are allowed; {@code false} otherwise.
	 */
	public boolean isGetEnabled() {
		return retrieveMode == CacheRetrieveMode.USE;
	}

	/**
	 * Does this cache mode indicate that writes are allowed?
	 *
	 * @return {@code true} if cache writes are allowed; {@code false} otherwise.
	 */
	public boolean isPutEnabled() {
		return storeMode == CacheStoreMode.USE || storeMode == CacheStoreMode.REFRESH;
	}

	/**
	 * Interpret externalized form as an instance of this enumeration.
	 *
	 * @param setting The externalized form.
	 * @return The matching enum value.
	 *
	 * @throws MappingException Indicates the external form was not recognized as a valid enum value.
	 */
	public static CacheMode interpretExternalSetting(String setting) {
		if ( setting == null ) {
			return null;
		}

		try {
			return CacheMode.valueOf( setting.toUpperCase(Locale.ROOT) );
		}
		catch ( IllegalArgumentException e ) {
			throw new MappingException( "Unknown Cache Mode: " + setting );
		}
	}

	/**
	 * Interpret the given JPA modes as an instance of this enumeration.
	 */
	public static CacheMode fromJpaModes(CacheRetrieveMode retrieveMode, CacheStoreMode storeMode) {
		if ( retrieveMode == null && storeMode == null ) {
			return null;
		}

		if ( storeMode == null ) {
			storeMode = CacheStoreMode.BYPASS;
		}

		if ( retrieveMode == null ) {
			retrieveMode = CacheRetrieveMode.BYPASS;
		}

		return switch (storeMode) {
			case USE -> switch (retrieveMode) {
				case USE -> NORMAL;
				case BYPASS -> PUT;
			};
			case BYPASS -> switch (retrieveMode) {
				case USE -> GET;
				case BYPASS -> IGNORE;
			};
			// technically should combo CacheStoreMode#REFRESH and CacheRetrieveMode#USE be illegal?
			case REFRESH -> REFRESH;
		};
	}
}
