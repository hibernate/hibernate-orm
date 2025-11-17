/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.SessionCheckMode;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Encapsulation of the options for loading multiple entities by id
 */
public interface MultiIdLoadOptions extends MultiLoadOptions {
	/**
	 * Controls whether to check the current status of each identified entity
	 * within the persistence context.
	 *
	 * @since 7.2
	 */
	SessionCheckMode getSessionCheckMode();

	/**
	 * Check the first-level cache first, and only if the entity is not found in the cache
	 * should Hibernate hit the database.
	 *
	 * @return the session cache is checked first
	 * @deprecated Use {@linkplain #getSessionCheckMode()} instead.
	 */
	@Deprecated(since = "7.2", forRemoval = true)
	default boolean isSessionCheckingEnabled() {
		return getSessionCheckMode() == SessionCheckMode.ENABLED;
	}

	/**
	 * Check the second-level cache first, and only if the entity is not found in the cache
	 * should Hibernate hit the database.
	 *
	 * @return the session factory cache is checked first
	 */
	boolean isSecondLevelCacheCheckingEnabled();

	/**
	 * Should the entities be loaded in read-only mode?
	 */
	Boolean getReadOnly(SessionImplementor session);
}
