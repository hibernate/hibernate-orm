/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.LockOptions;

/**
 * Base contract for options for multi-load operations
 */
public interface MultiLoadOptions {
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
