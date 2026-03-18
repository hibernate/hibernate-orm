/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.Iterator;

/**
 * Coordinates writing index (order column) values for indexed collections.
 * <p>
 * For collections with {@code @OrderColumn}, this handles updating the position
 * values (list indices) in the database.
 *
 * @see org.hibernate.persister.collection.CollectionPersister#recreate
 * @see org.hibernate.persister.collection.CollectionPersister#insertRows
 *
 * @author Steve Ebersole
 */
public interface WriteIndexCoordinator extends CollectionOperationCoordinator {
	/**
	 * Write index values for collection entries.
	 *
	 * @param collection The collection whose indices are being written
	 * @param entries Iterator over entries to index
	 * @param key The collection key
	 * @param resetIndex Whether to reset the index to the base (true for recreate, false for incremental)
	 * @param session The session
	 */
	void writeIndex(
			PersistentCollection<?> collection,
			Iterator<?> entries,
			Object key,
			boolean resetIndex,
			SharedSessionContractImplementor session);
}
