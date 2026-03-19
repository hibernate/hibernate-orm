/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.exec.ExecutionContext;
import org.hibernate.action.queue.exec.OperationResultChecker;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.sql.SQLException;

/// Bind plan for a single collection row update.
///
/// @author Steve Ebersole
public class SingleRowUpdateBindPlan implements BindPlan, OperationResultChecker {
	private final PersistentCollection<?> collection;
	private final Object key;
	private final Object entry;
	private final int entryIndex;
	private final CollectionJdbcOperations.Values updateRowValues;
	private final CollectionJdbcOperations.Restrictions updateRowRestrictions;

	public SingleRowUpdateBindPlan(
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryIndex,
			CollectionJdbcOperations.Values updateRowValues,
			CollectionJdbcOperations.Restrictions updateRowRestrictions) {
		this.collection = collection;
		this.key = key;
		this.entry = entry;
		this.entryIndex = entryIndex;
		this.updateRowValues = updateRowValues;
		this.updateRowRestrictions = updateRowRestrictions;
	}

	@Override
	public void execute(ExecutionContext context, PlannedOperation plannedOperation, SharedSessionContractImplementor session) {
		context.executeRow(
				plannedOperation,
				jdbcValueBindings -> bindValues( jdbcValueBindings, plannedOperation, session ),
				this
		);
	}

	private void bindValues(
			JdbcValueBindings valueBindings,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		updateRowValues.applyValues( collection, key, entry, entryIndex, session, valueBindings );
		updateRowRestrictions.applyRestrictions( collection, key, entry, entryIndex, session, valueBindings );
	}

	@Override
	public boolean checkResult(
			int affectedRowCount,
			int batchPosition,
			String sqlString,
			SessionFactoryImplementor sessionFactory) throws SQLException {
		// something we should check here?
		return true;
	}

	@Override
	public String toString() {
		return "SingleRowUpdateBindPlan(" + collection.getRole() + ")";
	}
}
