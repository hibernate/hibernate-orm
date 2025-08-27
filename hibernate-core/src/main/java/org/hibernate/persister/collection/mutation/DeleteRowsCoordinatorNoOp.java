/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * DeleteRowsCoordinator implementation for cases where deletion is not enabled
 *
 * @author Steve Ebersole
 */
public class DeleteRowsCoordinatorNoOp implements DeleteRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;

	public DeleteRowsCoordinatorNoOp(CollectionMutationTarget mutationTarget) {
		this.mutationTarget = mutationTarget;
	}

	@Override
	public String toString() {
		return "DeleteRowsCoordinator(" + mutationTarget.getRolePath() + " [DISABLED])";
	}

	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void deleteRows(PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session) {
		// nothing to do
	}
}
