/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.spi;

import org.hibernate.LockOptions;

/**
 * Information that can influence the loader instance returned.
 *
 * @author Steve Ebersole
 */
public interface MultiIdLoaderSelectors {
	/**
	 * Should the entities be returned in the same order as their associated entity identifiers were provided.
	 *
	 * @return entities follow the provided identifier order
	 */
	boolean isOrderReturnEnabled();

	/**
	 * Batch size to use when loading entities from the database.
	 *
	 * @return JDBC batch size
	 */
	Integer getBatchSize();

	/**
	 * Specify the lock options applied during loading.
	 *
	 * @return lock options applied during loading.
	 */
	LockOptions getLockOptions();
}
