/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.bind;

import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.function.Supplier;

/**
 * @author Steve Ebersole
 */
public interface BindPlan {
	default Supplier<Object> getEntityIdAccess() {
		return null;
	}

	/**
	 * Get the entity instance for this operation.
	 * Used for tracking entities that become managed during flush (for unresolved insert resolution).
	 *
	 * @return the entity instance, or null if this operation doesn't have an associated entity instance
	 */
	default Object getEntityInstance() {
		return null;
	}

	void bindAndMaybePatch(MutationExecutor executor, PlannedOperation operation, SharedSessionContractImplementor session);
	void execute(MutationExecutor executor, PlannedOperation operation, SharedSessionContractImplementor session);
}
