/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

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
