/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.cyclebreak;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;

/// Factory for building PlannedOperations which "fixup" cycle breaks due to
/// unique-key cycles.
///
/// The original operation sets null for those unique-key columns as part
/// of the cycle break. Here, we build the update which "fixes" those
/// unique-key columns by setting them to their real value.
///
/// Builds SQL like:
///
/// ```sql
///   update <table> set <uk> = ? where <pk> = ?
/// ```
///
/// @author Steve Ebersole
public final class UniqueSwapUpdateFactory {
	@Nullable
	public PlannedOperation buildOperationIfNeeded(
			PlannedOperation cycleBrokenOp,
			Object entityId,
			SharedSessionContractImplementor session) {
		assert cycleBrokenOp.getKind() == MutationKind.UPDATE;

		// No fixup needed if no unique-key values were deferred during cycle breaking
		if (cycleBrokenOp.getIntendedUniqueValues().isEmpty()) {
			return null;
		}

		if (entityId == null) {
			throw new IllegalStateException("FK fixup requires non-null entityId (identity prereq must have executed)");
		}

		var mutationTarget = cycleBrokenOp.getJdbcOperation().getMutationTarget();
		var persister = ( mutationTarget instanceof EntityMutationTarget emt )
				? emt.getTargetPart().getEntityPersister()
				: null;
		if ( persister == null ) {
			throw new IllegalStateException("Unique swap fixup only valid for entities, but found - " + mutationTarget);
		}

		var tableDescriptor = (EntityTableDescriptor) cycleBrokenOp.getMutatingTableDescriptor();
		assert tableDescriptor != null;

		var tableUpdate = new FixupTableUpdate(
				tableDescriptor,
				persister,
				tableDescriptor.findColumns( cycleBrokenOp.getIntendedUniqueValues().keySet() ),
				tableDescriptor.keyDescriptor().columns()
		);

		var jdbcUpdate = tableUpdate.buildJdbcUpdate();

		final FixupBindPlan bindPlan = new FixupBindPlan(
				persister,
				entityId,
				cycleBrokenOp.getIntendedUniqueValues()
		);

		return new PlannedOperation(
				cycleBrokenOp.getMutatingTableDescriptor(),
				MutationKind.UPDATE,
				jdbcUpdate,
				bindPlan,
				cycleBrokenOp.getOrdinal() + 10_000,
				cycleBrokenOp.getOrigin() + " [cycle-break unique swap fixup update]"
		);
	}
}
