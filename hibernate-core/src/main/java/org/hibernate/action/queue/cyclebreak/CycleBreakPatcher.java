/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.cyclebreak;

import org.hibernate.action.queue.bind.DelayedValueAccess;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.spi.JdbcValueBindingsImplementor;

/**
 * @author Steve Ebersole
 */
public class CycleBreakPatcher {
	public static void applyNullInsertPatch(
			MutationExecutor executor,
			FlushOperation flushOperation,
			BindingPatch bindingPatch) {
		if (bindingPatch == null || bindingPatch.fkColumnsToNull().isEmpty()) {
			return;
		}

		final String table = (flushOperation.getTableExpression());
		if (!table.equals(( bindingPatch.tableName() ))) {
			return;
		}

		final JdbcValueBindingsImplementor bindings = (JdbcValueBindingsImplementor) executor.getJdbcValueBindings();

		// Determine which map to use based on cycle type
		final boolean isUniqueSwap = bindingPatch.cycleType() == BindingPatch.CycleType.UNIQUE_SWAP;

		for (var selectableMapping : bindingPatch.fkColumnsToNull()) {
			final String rawCol = selectableMapping.getSelectionExpression();
			final String col = (rawCol);

			// 1) Read the intended value that coordinator already bound
			final Object intended = bindings.getBoundValue( table, col, ParameterUsage.SET );

			if (intended == null) {
				// not bound (dynamic insert excluded) or already null
				continue;
			}
			if ( intended instanceof DelayedValueAccess
					&& (isUniqueSwap
							? flushOperation.getIntendedUniqueValues().containsKey( col )
							: flushOperation.getIntendedFkValues().containsKey( col )) ) {
				continue;
			}
			final Object intendedForFixup = intended instanceof DelayedValueAccess handle && handle.isResolved()
					? handle.get()
					: intended;

			// 2) Register a generated-value handle
			final DelayedValueAccess handle = new DelayedValueAccess(
					flushOperation.getOrigin() + "#" + col,
					intendedForFixup
			);
			bindings.replaceValue(table, col, ParameterUsage.SET, handle);

			// 3) Force NULL for INSERT/UPDATE
			handle.set( null );

			// 4) Record intended value for later fixup UPDATE
			if (isUniqueSwap) {
				flushOperation.getIntendedUniqueValues().put(col, intendedForFixup);
			}
			else {
				flushOperation.getIntendedFkValues().put(col, intendedForFixup);
			}
		}
	}

	public static void applyFixupPatch(
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation,
			BindingPatch bindingPatch) {
		if (bindingPatch == null || bindingPatch.fkColumnsToNull().isEmpty()) {
			return;
		}

		final String table = (flushOperation.getTableExpression());
		if (!table.equals(( bindingPatch.tableName() ))) {
			return;
		}

		// Determine which map to use based on cycle type
		final boolean isUniqueSwap = bindingPatch.cycleType() == BindingPatch.CycleType.UNIQUE_SWAP;

		for (var selectableMapping : bindingPatch.fkColumnsToNull()) {
			final String rawCol = selectableMapping.getSelectionExpression();
			final String col = (rawCol);

			// 1) Read the intended value that coordinator already bound
			final Object intended = valueBindings.getBoundValue( col, ParameterUsage.SET );

			if (intended == null) {
				// not bound (dynamic insert excluded) or already null
				continue;
			}
			if ( intended instanceof DelayedValueAccess
					&& (isUniqueSwap
							? flushOperation.getIntendedUniqueValues().containsKey( col )
							: flushOperation.getIntendedFkValues().containsKey( col )) ) {
				continue;
			}
			final Object intendedForFixup = intended instanceof DelayedValueAccess handle && handle.isResolved()
					? handle.get()
					: intended;

			// 2) Register a generated-value handle
			final DelayedValueAccess handle = new DelayedValueAccess(
					flushOperation.getOrigin() + "#" + col,
					intendedForFixup
			);
			valueBindings.replaceValue( col, ParameterUsage.SET, handle);

			// 3) Force NULL for INSERT/UPDATE
			handle.set( null );

			// 4) Record intended value for later fixup UPDATE
			if (isUniqueSwap) {
				flushOperation.getIntendedUniqueValues().put(col, intendedForFixup);
			}
			else {
				flushOperation.getIntendedFkValues().put(col, intendedForFixup);
			}
		}
	}
}
