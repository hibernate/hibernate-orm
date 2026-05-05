/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.queue.decompose.collection.CollectionMutationTarget;

/**
 * Base contract for coordination of collection mutation operations
 *
 * @author Steve Ebersole
 */
public interface CollectionOperationCoordinator {
	/**
	 * The collection being mutated
	 */
	CollectionMutationTarget getMutationTarget();
}
