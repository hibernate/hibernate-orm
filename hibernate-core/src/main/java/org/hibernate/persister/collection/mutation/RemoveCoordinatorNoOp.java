/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class RemoveCoordinatorNoOp implements RemoveCoordinator {
	private final CollectionMutationTarget mutationTarget;

	public RemoveCoordinatorNoOp(@Nonnull CollectionMutationTarget mutationTarget) {
		this.mutationTarget = mutationTarget;
	}

	@Nonnull
	@Override
	public String toString() {
		return "RemoveCoordinator(" + mutationTarget.getRolePath() + " [DISABLED])";
	}

	@Nonnull
	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public @Nullable String getSqlString() {
		return null;
	}

	@Override
	public void deleteAllRows(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		// nothing to do
	}
}
