/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.decompose.collection.CollectionJdbcOperations;
import org.hibernate.action.queue.bind.OperationResultChecker;
import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.sql.SQLException;

/// Bind plan for updating only the order/index column of a join table row.
/// Used when an entity's position changes but the entity itself remains in the collection.
///
/// @author Steve Ebersole
public class OrderOnlyUpdateBindPlan implements BindPlan, OperationResultChecker {
	private final PersistentCollection<?> collection;
	private final Object key;
	private final Object entry;
	private final int oldPosition;  // Position in snapshot (for WHERE clause)
	private final int newPosition;  // Position in current collection (for SET clause)
	private final CollectionJdbcOperations.Values updateRowValues;
	private final CollectionJdbcOperations.Restrictions updateRowRestrictions;

	public OrderOnlyUpdateBindPlan(
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int oldPosition,
			int newPosition,
			CollectionJdbcOperations.Values updateRowValues,
			CollectionJdbcOperations.Restrictions updateRowRestrictions) {
		this.collection = collection;
		this.key = key;
		this.entry = entry;
		this.oldPosition = oldPosition;
		this.newPosition = newPosition;
		this.updateRowValues = updateRowValues;
		this.updateRowRestrictions = updateRowRestrictions;
	}

	@Override
	public Object getEntityInstance() {
		return null;
	}

	@Override
	public void bindValues(
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		// SET clause: use new position
		updateRowValues.applyValues( collection, key, entry, newPosition, session, valueBindings );
		// WHERE clause: use old position to find the row
		updateRowRestrictions.applyRestrictions( collection, key, entry, oldPosition, session, valueBindings );
	}

	@Override
	public boolean checkResult(
			int affectedRowCount,
			int batchPosition,
			String sqlString,
			SessionFactoryImplementor sessionFactory) throws SQLException {
		return true;
	}

	@Override
	public String toString() {
		return "OrderOnlyUpdateBindPlan(" + collection.getRole() + ", " + oldPosition + "→" + newPosition + ")";
	}
}
