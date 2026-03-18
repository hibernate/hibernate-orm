/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.bind;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.action.queue.exec.OperationResultChecker;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/// Represents JDBC parameter binding for graph-based operation execution
///
/// @author Steve Ebersole
public interface BindPlan {
	/// Access to the entity identifier associated with the PlannedOperation
	/// that this BindPlan is associated with.  May be null.
	@Nullable
	default Object getEntityId() {
		return null;
	}

	/**
	 * Get the entity instance for this operation.
	 * Used for tracking entities that become managed during flush (for unresolved insert resolution).
	 *
	 * @return the entity instance, or null if this operation doesn't have an associated entity instance
	 */
	@Nullable
	default Object getEntityInstance() {
		return null;
	}

	default GeneratedValuesCollector getGeneratedValuesCollector() {
		return null;
	}

	void bindValues(
			JdbcValueBindings valueBindings,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session);


	default OperationResultChecker getOperationResultChecker() {
		return null;
	}
}
