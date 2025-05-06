/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat;

import java.util.Set;

/**
 * Information about the first-level (session) cache for a particular
 * instance of {@link org.hibernate.Session}.
 *
 * @author Gavin King
 */
public interface SessionStatistics {
	/**
	 * The number of entity instances associated with the session.
	 */
	int getEntityCount();
	/**
	 * The number of collection instances associated with the session.
	 */
	int getCollectionCount();

	/**
	 * The set of all {@link org.hibernate.engine.spi.EntityKey}s
	 * currently held within the persistence context.
	 */
	Set<?> getEntityKeys();
	/**
	 * The set of all {@link org.hibernate.engine.spi.CollectionKey}s
	 * currently held within the persistence context.
	 */
	Set<?> getCollectionKeys();

}
