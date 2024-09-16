/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
