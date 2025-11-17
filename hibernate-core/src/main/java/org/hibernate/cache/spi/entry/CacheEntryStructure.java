/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.entry;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Strategy for how cache entries are "structured" for storing into the cache.
 *
 * @author Gavin King
 */
public interface CacheEntryStructure {
	/**
	 * Convert the cache item into its "structured" form.  Perfectly valid to return the item as-is.
	 *
	 * @param item The item to structure.
	 *
	 * @return The structured form.
	 */
	Object structure(Object item);

	/**
	 * Convert the previous structured form of the item back into its item form.
	 *
	 * @param structured The structured form.
	 * @param factory The session factory.
	 *
	 * @return The item
	 */
	Object destructure(Object structured, SessionFactoryImplementor factory);
}
