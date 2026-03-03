/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface UpdateRowsCoordinator extends CollectionOperationCoordinator {
	void updateRows(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session);

	/**
	 * Decompose the update rows operation into planned operation groups for the graph-based planner.
	 *
	 * @param collection The collection being updated
	 * @param key The collection key
	 * @param ordinalBase Base ordinal for operation ordering
	 * @param session The session
	 * @return List of planned operation groups (may be empty for NoOp coordinators)
	 */
	default List<PlannedOperationGroup> decomposeUpdateRows(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		return List.of();
	}
}
