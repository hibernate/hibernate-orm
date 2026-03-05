/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.List;

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

	/**
	 * Decompose the delete rows operation into planned operation groups for the graph-based planner.
	 *
	 * @param collection The collection with deleted entries
	 * @param key The collection key
	 * @param ordinalBase Base ordinal for operation ordering
	 * @param session The session
	 * @return List of planned operation groups (maybe empty for NoOp coordinators)
	 */
	default List<PlannedOperation> decomposeDeleteRows(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		return List.of();
	}
}
