/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Coordinates the deletion of entries removed from the collection -<ul>
 *     <li>
 *         For collections with a collection-table, deletes rows from the
 *         collection table.
 *     </li>
 *     <li>
 *         For one-to-many, unsets the collection-key for the matched row
 *         in the association table.
 *     </li>
 * </ul>
 *
 * @see org.hibernate.persister.collection.CollectionPersister#deleteRows
 * @see RowMutationOperations#getDeleteRowOperation()
 * @see RowMutationOperations#getDeleteRowRestrictions()
 *
 * @author Steve Ebersole
 */
public interface DeleteRowsCoordinator extends CollectionOperationCoordinator {
	/**
	 * Perform the deletions
	 */
	void deleteRows(PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session);
}
