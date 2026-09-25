package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;

import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class InsertRowsCoordinatorNoOp implements InsertRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;

	public InsertRowsCoordinatorNoOp(@Nonnull CollectionMutationTarget mutationTarget) {
		this.mutationTarget = mutationTarget;
	}

	@Nonnull
	@Override
	public String toString() {
		return "InsertRowsCoordinator(" + mutationTarget.getRolePath() + " (no-op))";
	}

	@Nonnull
	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void insertRows(@Nonnull PersistentCollection<?> collection, @Nonnull Object id, @Nullable EntryFilter entryChecker, @Nonnull SharedSessionContractImplementor session) {
		// nothing to do
	}
}
