/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class InsertRowsCoordinatorNoOp implements InsertRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;

	public InsertRowsCoordinatorNoOp(CollectionMutationTarget mutationTarget) {
		this.mutationTarget = mutationTarget;
	}

	@Override
	public String toString() {
		return "InsertRowsCoordinator(" + mutationTarget.getRolePath() + " (no-op))";
	}

	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void insertRows(PersistentCollection<?> collection, Object id, EntryFilter entryChecker, SharedSessionContractImplementor session) {
		// nothing to do
	}
}
