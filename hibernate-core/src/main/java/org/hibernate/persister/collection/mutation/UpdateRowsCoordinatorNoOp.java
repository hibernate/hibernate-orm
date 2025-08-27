/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class UpdateRowsCoordinatorNoOp implements UpdateRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;

	public UpdateRowsCoordinatorNoOp(CollectionMutationTarget mutationTarget) {
		this.mutationTarget = mutationTarget;
	}

	@Override
	public String toString() {
		return "UpdateRowsCoordinator(" + mutationTarget.getRolePath() + " (no-op))";
	}

	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void updateRows(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session) {
		// nothing to do
	}

}
