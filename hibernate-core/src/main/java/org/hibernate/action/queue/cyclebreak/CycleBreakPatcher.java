/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.cyclebreak;

import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.spi.JdbcValueBindingsImplementor;
import org.hibernate.internal.util.MutableObject;

import static org.hibernate.action.queue.Helper.normalizeColumnName;
import static org.hibernate.action.queue.Helper.normalizeTableName;

/**
 * @author Steve Ebersole
 */
public class CycleBreakPatcher {
	public static void applyNullInsertPatch(
			MutationExecutor executor,
			PlannedOperation plannedOperation,
			BindingPatch bindingPatch) {

		if (bindingPatch == null || bindingPatch.fkColumnsToNull().isEmpty()) {
			return;
		}

		final String table = normalizeTableName(plannedOperation.getTableExpression());
		if (!table.equals(normalizeTableName( bindingPatch.tableName() ))) {
			return;
		}

		final JdbcValueBindingsImplementor bindings = (JdbcValueBindingsImplementor) executor.getJdbcValueBindings();

		// Determine which map to use based on cycle type
		final boolean isUniqueSwap = bindingPatch.cycleType() == BindingPatch.CycleType.UNIQUE_SWAP;

		for (var selectableMapping : bindingPatch.fkColumnsToNull()) {
			final String rawCol = selectableMapping.getSelectionExpression();
			final String col = normalizeColumnName(rawCol);

			// 1) Read the intended value that coordinator already bound
			final Object intended = bindings.getBoundValue( table, col, ParameterUsage.SET );

			if (intended == null) {
				// not bound (dynamic insert excluded) or already null
				continue;
			}

			// 2) Register a deferred override handle
			final MutableObject<?> handle = new MutableObject<>(intended);
			bindings.replaceValue(table, col, ParameterUsage.SET, handle);

			// 3) Force NULL for INSERT/UPDATE
			handle.set( null );

			// 4) Record intended value for later fixup UPDATE
			if (isUniqueSwap) {
				plannedOperation.getIntendedUniqueValues().put(col, intended);
			} else {
				plannedOperation.getIntendedFkValues().put(col, intended);
			}
		}
	}
}
