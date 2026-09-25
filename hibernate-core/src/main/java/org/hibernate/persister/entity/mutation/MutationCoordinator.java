package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nullable;

import org.hibernate.sql.model.MutationOperationGroup;

/**
 * Coordinates the mutation operations of an entity.
 *
 * @see InsertCoordinator
 * @see DeleteCoordinator
 * @see UpdateCoordinator
 * @see MergeCoordinatorStandard
 *
 * @author Marco Belladelli
 */
public interface MutationCoordinator {
	/**
	 * The operation group used to perform the mutation unless some form
	 * of dynamic mutation is necessary.
	 */
	@Nullable
	MutationOperationGroup getStaticMutationOperationGroup();
}
