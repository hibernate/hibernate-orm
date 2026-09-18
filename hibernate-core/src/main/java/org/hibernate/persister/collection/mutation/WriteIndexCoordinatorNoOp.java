/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nonnull;

import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.Iterator;

/**
 * WriteIndexCoordinator implementation for collections without {@code @OrderColumn}.
 *
 * @author Steve Ebersole
 */
public class WriteIndexCoordinatorNoOp implements WriteIndexCoordinator {
	private final CollectionMutationTarget mutationTarget;

	public WriteIndexCoordinatorNoOp(@Nonnull CollectionMutationTarget mutationTarget) {
		this.mutationTarget = mutationTarget;
	}

	@Nonnull
	@Override
	public String toString() {
		return "WriteIndexCoordinator(" + mutationTarget.getRolePath() + " (no-op))";
	}

	@Nonnull
	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void writeIndex(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Iterator<?> entries,
			@Nonnull Object key,
			boolean resetIndex,
			@Nonnull SharedSessionContractImplementor session) {
		// nothing to do - collection has no index column
	}
}
