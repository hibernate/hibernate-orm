/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.action.queue.op.PlannedOperation;

/// Adapter hides Hibernate "group" concepts from the planner.
///
/// @implSpec It can still use SingleOperationGroup internally.
///
/// @author Steve Ebersole
public interface PlannedOperationExecutor {
	/**
	 * Execute one planned op (single table statement).
	 * Adapter is responsible for creating the executor, wrapping operation as needed, binding, patching, and execution.
	 */
	void executePlannedOperation(PlannedOperation op);

	/**
	 * Synthesize a FK-fixup UPDATE op for a cycle-broken insert.
	 * Returns null if no fixup is needed.
	 */
	PlannedOperation synthesizeFixupUpdateIfNeeded(PlannedOperation cycleBrokenInsertOp, Object entityId);
}
