/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class RemoveCoordinatorNoOp implements RemoveCoordinator {
	private final CollectionMutationTarget mutationTarget;

	public RemoveCoordinatorNoOp(CollectionMutationTarget mutationTarget) {
		this.mutationTarget = mutationTarget;
	}

	@Override
	public String toString() {
		return "RemoveCoordinator(" + mutationTarget.getRolePath() + " [DISABLED])";
	}

	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public String getSqlString() {
		return null;
	}

	@Override
	public void deleteAllRows(Object key, SharedSessionContractImplementor session) {
		// nothing to do
	}
}
