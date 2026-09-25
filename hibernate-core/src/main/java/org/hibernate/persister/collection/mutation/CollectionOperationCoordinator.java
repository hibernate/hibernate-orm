package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nonnull;

import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;

/**
 * Base contract for coordination of collection mutation operations
 *
 * @author Steve Ebersole
 */
public interface CollectionOperationCoordinator {
	/**
	 * The collection being mutated
	 */
	@Nonnull
	CollectionMutationTarget getMutationTarget();
}
