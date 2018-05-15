/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.spi;

import org.hibernate.LockOptions;

/**
 * Encapsulation of the options for performing a load by multiple identifiers.
 *
 * @author Steve Ebersole
 */
public interface MultiLoadOptions extends MultiIdLoaderSelectors {
	@Override
	boolean isOrderReturnEnabled();

	@Override
	Integer getBatchSize();

	@Override
	LockOptions getLockOptions();

	/**
	 * Check the first-level cache first, and only if the entity is not found in the cache
	 * should Hibernate hit the database.
	 *
	 * @return the session cache is checked first
	 */
	boolean isSessionCheckingEnabled();

	/**
	 * Should we returned entities that are scheduled for deletion.
	 *
	 * @return entities that are scheduled for deletion are returned as well.
	 */
	boolean isReturnOfDeletedEntitiesEnabled();

	/**
	 * Check the second-level cache first, and only if the entity is not found in the cache
	 * should Hibernate hit the database.
	 *
	 * @return the session factory cache is checked first
	 */
	boolean isSecondLevelCacheCheckingEnabled();
}
