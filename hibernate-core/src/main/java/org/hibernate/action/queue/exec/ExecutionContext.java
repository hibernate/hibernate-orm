/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.op.PlannedOperation;

import java.util.function.Consumer;

/// Execution context for BindPlan execution, providing batch consolidation
/// and statement management services.
///
/// BindPlans drive their own execution by calling back to this context to
/// execute individual rows, which may be batched together with other operations.
///
/// @author Steve Ebersole
public interface ExecutionContext {
	/**
	 * Execute a single row within the context of batching.
	 * <p>
	 * The context will handle:
	 * - Starting/managing batches
	 * - Binding values via the provided binder
	 * - Adding to JDBC batch
	 * - Storing result checkers
	 * - Executing batches when full
	 *
	 * @param plannedOperation the operation being executed
	 * @param binder callback to bind JDBC values for this row
	 * @param resultChecker optional result checker for this row (may be null)
	 */
	void executeRow(
			PlannedOperation plannedOperation,
			Consumer<JdbcValueBindings> binder,
			OperationResultChecker resultChecker);
}
