/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.sql.model.MutationOperationGroup;

/**
 * Coordinates the mutation operations of an entity.
 *
 * @see InsertCoordinator
 * @see DeleteCoordinator
 * @see UpdateCoordinator
 * @see MergeCoordinator
 *
 * @author Marco Belladelli
 */
public interface MutationCoordinator {
	/**
	 * The operation group used to perform the mutation unless some form
	 * of dynamic mutation is necessary.
	 */
	MutationOperationGroup getStaticMutationOperationGroup();
}
