/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import org.hibernate.LockOptions;

/**
 * Encapsulation of the options for performing a load by multiple identifiers.
 *
 * @author Steve Ebersole
 */
public interface MultiLoadOptions {
	/**
	 * Check the first-level cache first, and only if the entity is not found in the cache
	 * should Hibernate hit the database.
	 *
	 * @return the session cache is checked first
	 */
	boolean isSessionCheckingEnabled();

	/**
	 * Check the second-level cache first, and only if the entity is not found in the cache
	 * should Hibernate hit the database.
	 *
	 * @return the session factory cache is checked first
	 */
	boolean isSecondLevelCacheCheckingEnabled();

	/**
	 * Should we returned entities that are scheduled for deletion.
	 *
	 * @return entities that are scheduled for deletion are returned as well.
	 */
	boolean isReturnOfDeletedEntitiesEnabled();

	/**
	 * Should the entities be returned in the same order as their associated entity identifiers were provided.
	 *
	 * @return entities follow the provided identifier order
	 */
	boolean isOrderReturnEnabled();

	/**
	 * Specify the lock options applied during loading.
	 *
	 * @return lock options applied during loading.
	 */
	LockOptions getLockOptions();

	/**
	 * Batch size to use when loading entities from the database.
	 *
	 * @return JDBC batch size
	 */
	Integer getBatchSize();
}
