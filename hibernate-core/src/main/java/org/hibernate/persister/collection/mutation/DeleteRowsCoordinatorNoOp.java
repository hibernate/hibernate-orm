package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nonnull;

import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * DeleteRowsCoordinator implementation for cases where deletion is not enabled
 *
 * @author Steve Ebersole
 */
public class DeleteRowsCoordinatorNoOp implements DeleteRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;

	public DeleteRowsCoordinatorNoOp(@Nonnull CollectionMutationTarget mutationTarget) {
		this.mutationTarget = mutationTarget;
	}

	@Nonnull
	@Override
	public String toString() {
		return "DeleteRowsCoordinator(" + mutationTarget.getRolePath() + " [DISABLED])";
	}

	@Nonnull
	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void deleteRows(@Nonnull PersistentCollection<?> collection, @Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		// nothing to do
	}
}
