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
/// foreign-key cycles.
///
/// The original operation inserts null for those foreign-key columns as part
/// of the cycle break. Here, we build the update which "fixes" those
/// foreign-key columns by setting them to their real value.
///
/// Builds SQL like:
///
/// ```sql
///   update <table> set <fk> = ? where <pk> = ?
/// ```
///
/// @author Steve Ebersole
public final class FkFixupUpdateFactory {

	@Nullable
	public PlannedOperation buildOperationIfNeeded(
			PlannedOperation cycleBrokenOp,
			Object entityId,
			SharedSessionContractImplementor session) {
		assert cycleBrokenOp.getKind() == MutationKind.INSERT;

		// No fixup needed if no foreign-key values were deferred during cycle breaking
		if (cycleBrokenOp.getIntendedFkValues().isEmpty()) {
			return null;
		}

		if (entityId == null) {
			throw new IllegalStateException("Foreign-key fixup requires non-null entityId");
		}

		var mutationTarget = cycleBrokenOp.getJdbcOperation().getMutationTarget();
		var persister = ( mutationTarget instanceof EntityMutationTarget emt )
				? emt.getTargetPart().getEntityPersister()
				: null;
		if ( persister == null ) {
			throw new IllegalStateException("FK fixup only valid for entities, but found - " + mutationTarget);
		}

		var tableDescriptor = (EntityTableDescriptor) cycleBrokenOp.getMutatingTableDescriptor();
		assert tableDescriptor != null;

		var tableUpdate = new FixupTableUpdate(
				tableDescriptor,
				persister,
				tableDescriptor.findColumns( cycleBrokenOp.getIntendedFkValues().keySet() ),
				tableDescriptor.keyDescriptor().columns()
		);

		var jdbcUpdate = tableUpdate.buildJdbcUpdate();

		final FixupBindPlan bindPlan = new FixupBindPlan(
				persister,
				entityId,
				cycleBrokenOp.getIntendedFkValues()
		);

		return new PlannedOperation(
				tableDescriptor,
				MutationKind.UPDATE,
				jdbcUpdate,
				bindPlan,
				cycleBrokenOp.getOrdinal() + 10_000,
				cycleBrokenOp.getOrigin() + " [cycle-break FK fixup update]"
		);
	}

}
