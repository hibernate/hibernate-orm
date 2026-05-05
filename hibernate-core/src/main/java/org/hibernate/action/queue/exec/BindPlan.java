/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.action.queue.constraint.UniqueConstraint;
import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.ValuesAnalysis;

/// Represents JDBC parameter binding for graph-based operation execution
///
/// @author Steve Ebersole
public interface BindPlan {
	/// Access to the entity identifier associated with the FlushOperation
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

	/**
	 * Get the loaded state for this operation.
	 * Used for extracting unique constraint values from DELETE operations.
	 *
	 * @return the entity's loaded state, or null if not available
	 */
	@Nullable
	default Object[] getLoadedState() {
		return null;
	}

	/**
	 * Extract the values this operation assigns to a unique constraint.
	 * <p>
	 * Entity operations expose state through {@link #getEntityInstance()} and
	 * {@link #getLoadedState()}. Collection row operations do not represent an
	 * entity instance, but they still need to participate in unique slot ordering.
	 */
	@Nullable
	default Object[] getUniqueConstraintValues(
			UniqueConstraint constraint,
			SharedSessionContractImplementor session) {
		return null;
	}

	/**
	 * Bind JDBC values for the operation being executed.
	 *
	 * @param valueBindings the JDBC value bindings to populate
	 * @param flushOperation the operation being executed
	 * @param session the session
	 */
	void bindValues(
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session);

	default GeneratedValuesCollector getGeneratedValuesCollector() {
		return null;
	}

	default OperationResultChecker getOperationResultChecker() {
		return this instanceof OperationResultChecker checker ? checker : null;
	}

	default ValuesAnalysis getValuesAnalysis() {
		return null;
	}
}
