/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.FindMultipleOption;
import org.hibernate.engine.spi.SessionImplementor;


/// Encapsulation of the options for loading multiple entities (of a type)
/// by [id][org.hibernate.KeyType#IDENTIFIER].
///
/// @see org.hibernate.Session#findMultiple
/// @see MultiIdEntityLoader
///
/// @author Steve Ebersole
public interface MultiIdLoadOptions extends MultiLoadOptions {
	/**
	 * Controls whether to check the current status of each identified entity
	 * within the persistence context.
	 *
	 * @since 7.2
	 */
	FindMultipleOption.SessionCheckMode getSessionCheckMode();

	/**
	 * Check the first-level cache first, and only if the entity is not found in the cache
	 * should Hibernate hit the database.
	 *
	 * @return the session cache is checked first
	 * @deprecated Use {@linkplain #getSessionCheckMode()} instead.
	 */
	@Deprecated(since = "7.2", forRemoval = true)
	default boolean isSessionCheckingEnabled() {
		return getSessionCheckMode() == FindMultipleOption.SessionCheckMode.ENABLED;
	}

	/**
	 * Check the second-level cache first, and only if the entity is not found in the cache
	 * should Hibernate hit the database.
	 *
	 * @return the session factory cache is checked first
	 */
	boolean isSecondLevelCacheCheckingEnabled();

	/**
	 * Whether the operation is configured to refresh the state of the loaded entities in the session's first level cache.
	 *
	 * @return {@code true}, if {@link org.hibernate.CacheMode#REFRESH_SESSION} is configured, {@code false} otherwise
	 * @see org.hibernate.CacheMode#REFRESH_SESSION
	 */
	boolean isRefreshSession();

	/**
	 * Should the entities be loaded in read-only mode?
	 */
	Boolean getReadOnly(SessionImplementor session);
}
