/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nonnull;

import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class UpdateRowsCoordinatorNoOp implements UpdateRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;

	public UpdateRowsCoordinatorNoOp(@Nonnull CollectionMutationTarget mutationTarget) {
		this.mutationTarget = mutationTarget;
	}

	@Nonnull
	@Override
	public String toString() {
		return "UpdateRowsCoordinator(" + mutationTarget.getRolePath() + " (no-op))";
	}

	@Nonnull
	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void updateRows(@Nonnull Object key, @Nonnull PersistentCollection<?> collection, @Nonnull SharedSessionContractImplementor session) {
		// nothing to do
	}

}
