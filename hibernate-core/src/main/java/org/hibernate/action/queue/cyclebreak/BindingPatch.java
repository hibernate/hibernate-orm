/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.cyclebreak;

import org.hibernate.action.queue.plan.CycleBreaker;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.metamodel.mapping.SelectableMapping;

import java.util.Set;

/// Handling for cycle-breaking (nullable FK null-in-insert) installed by the planner.
///
/// When the planner decides to break a cycle, it creates a `BindingPatch` and
/// "installs it" into the `PlannedOperation` where it decided to break the cycle.
/// Its presence acts as a trigger to tell later processing to do some special handling.
/// This is handled in [CycleBreaker].
///
/// When this PlannedOperation is executed, its BindPlan will bind values normally
/// (into [org.hibernate.engine.jdbc.mutation.JdbcValueBindings]).  It will then see
/// that a BindingPatch is in effect and will replace the necessary binding values with
/// [org.hibernate.action.queue.exec.DelayedValueAccess].  This is handled in
/// [org.hibernate.action.queue.cyclebreak.CycleBreakPatcher].
/// It will also capture the "real values" and store them on the
/// [PlannedOperation#getIntendedFkValues()].
///
/// In the code that executes PlannedOperations, we check for [PlannedOperation#getIntendedFkValues()]
/// and, if there are any, perform some "fix up" to apply those foreign-key values using
/// a subsequent UPDATE statement.  The update statement is created by
/// [org.hibernate.action.queue.exec.PlannedOperationExecutor#synthesizeFixupUpdateIfNeeded(PlannedOperation, Object)] and
/// is added back to the [org.hibernate.action.queue.plan.FlushPlan] as a follow-up step.  The update could be executed immediately,
/// but queueing it as a follow-up step allows for batching.
///
/// @see CycleBreaker
/// @see PlannedOperation#getBindingPatch()
/// @see PlannedOperation#setBindingPatch(BindingPatch)
/// @see org.hibernate.action.queue.cyclebreak.CycleBreakPatcher
///
/// @author Steve Ebersole
public record BindingPatch(
		String tableName,
		Set<SelectableMapping> fkColumnsToNull,
		CycleType cycleType) {

	public enum CycleType {
		/// Foreign key cycle - columns are FK columns referencing another entity
		FOREIGN_KEY,
		/// Unique constraint swap cycle - columns are unique constraint columns
		UNIQUE_SWAP
	}
}
